/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package tests.integration.com.microsoft.azure.sdk.iot.iothub;


import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.ModuleClientException;
import com.microsoft.azure.sdk.iot.device.exceptions.MultiplexingClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.service.DeviceStatus;
import com.microsoft.azure.sdk.iot.service.Module;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.RegistryManagerOptions;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationType;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.rules.Timeout;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import tests.integration.com.microsoft.azure.sdk.iot.helpers.*;
import tests.integration.com.microsoft.azure.sdk.iot.helpers.annotations.IotHubTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.microsoft.azure.sdk.iot.device.IotHubClientProtocol.*;
import static junit.framework.TestCase.assertTrue;

@Slf4j
@IotHubTest
public class TokenRenewalTests extends IntegrationTest
{
    protected static String iotHubConnectionString;
    private static RegistryManager registryManager;
    protected static HttpProxyServer proxyServer;
    private static String iotHubHostName;
    protected static String testProxyHostname = "127.0.0.1";
    protected static int testProxyPort = 8898;

    // Semmle flags this as a security issue, but this is a test username so the warning can be suppressed
    protected static final String testProxyUser = "proxyUsername"; // lgtm

    // Semmle flags this as a security issue, but this is a test password so the warning can be suppressed
    protected static final char[] testProxyPass = "1234".toCharArray(); // lgtm


    final long SECONDS_FOR_SAS_TOKEN_TO_LIVE_BEFORE_RENEWAL = 30;
    final long EXPIRED_SAS_TOKEN_GRACE_PERIOD_SECONDS = 600; //service extends 10 minute grace period after a token has expired
    final long EXTRA_BUFFER_TO_ENSURE_TOKEN_EXPIRED_SECONDS = 120; //wait time beyond the expected grace period, just in case

    private static final Integer SEND_TIMEOUT_MILLISECONDS = 60000;
    private static final Integer RETRY_MILLISECONDS = 100;

    private final List<TestIdentity> testIdentities = new ArrayList<>();

    private static final int MULTIPLEX_COUNT = 5; // number of multiplexed devices to have per multiplexed connection

    private static final int TOKEN_RENEWAL_TEST_TIMEOUT_MILLISECONDS = 17 * 60 * 1000;

    public TokenRenewalTests()
    {
        // This overrides the IntegrationTest level timeout that is too restrictive for this particular test
        timeout = new Timeout(TOKEN_RENEWAL_TEST_TIMEOUT_MILLISECONDS);
    }

    @BeforeAll
    public static void setUp() throws IOException
    {
        iotHubConnectionString = Tools.retrieveEnvironmentVariableValue(TestConstants.IOT_HUB_CONNECTION_STRING_ENV_VAR_NAME);
        isBasicTierHub = Boolean.parseBoolean(Tools.retrieveEnvironmentVariableValue(TestConstants.IS_BASIC_TIER_HUB_ENV_VAR_NAME));
        isPullRequest = Boolean.parseBoolean(Tools.retrieveEnvironmentVariableValue(TestConstants.IS_PULL_REQUEST));
        iotHubHostName = com.microsoft.azure.sdk.iot.service.IotHubConnectionString.createConnectionString(iotHubConnectionString).getHostName();
        registryManager = RegistryManager.createFromConnectionString(iotHubConnectionString, RegistryManagerOptions.builder().httpReadTimeout(HTTP_READ_TIMEOUT).build());
    }

    @BeforeAll
    public static void startProxy()
    {
        proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(testProxyPort)
                .withProxyAuthenticator(new BasicProxyAuthenticator(testProxyUser, testProxyPass))
                .start();
    }

    @AfterAll
    public static void stopProxy()
    {
        proxyServer.stop();
    }

    /**
     * Note, this test takes at least 10 minutes to run as a token has a 10 minute grace period from the service to stay alive after it has technically expired
     * This test behaves a bit differently across protocol due to amqp proactively renewing sas tokens, and http not having connection status callback
     * Amqps/Amqps_ws : Expect the cbs link to send a new sas token before the old token expires
     * Mqtt/Mqtt_ws   : Expect the connection to be lost briefly, but re-established with a new sas token
     * Http           : No connection status callback, but should be able to send a message after the first generated sas token has expired
     * @throws Exception if the test fails in any way
     */
    @Test
    // While this test is long enough to be considered a continuous integration test, it is the only test that truly validates
    // that the device client's token renewal logic works. Because of that, it needs to be run at the gate.
    //@ContinuousIntegrationTest
    public void tokenRenewalWorks() throws Exception
    {
        List<InternalClient> clients = createClientsToTest();
        String hostname = clients.get(0).getConfig().getIotHubHostname();
        ArrayList<DeviceClient> amqpMultiplexedClients = new ArrayList<>();
        MultiplexingClient amqpMultiplexingClient = createMultiplexedClientToTest(AMQPS, amqpMultiplexedClients, hostname);
        ArrayList<DeviceClient> amqpwsMultiplexedClients = new ArrayList<>();
        MultiplexingClient amqpWsMultiplexingClient = createMultiplexedClientToTest(AMQPS_WS, amqpwsMultiplexedClients, hostname);

        // Allow registry operations some buffer time before attempting to open connections for them
        Thread.sleep(2000);

        //service grants a 10 minute grace period beyond when sas token expires, this test attempts to send a message after that grace period
        // to ensure that the first sas token has expired, and that the sas token was renewed successfully.
        final long WAIT_BUFFER_FOR_TOKEN_TO_EXPIRE = EXPIRED_SAS_TOKEN_GRACE_PERIOD_SECONDS + EXTRA_BUFFER_TO_ENSURE_TOKEN_EXPIRED_SECONDS;

        clients.addAll(amqpMultiplexedClients);
        clients.addAll(amqpwsMultiplexedClients);

        // Multiplexed clients have this sas token expiry set already
        for (InternalClient client : clients)
        {
            try
            {
                //set it so a newly generated sas token only lasts for a small amount of time
                client.setOption("SetSASTokenExpiryTime", SECONDS_FOR_SAS_TOKEN_TO_LIVE_BEFORE_RENEWAL);
            }
            catch (UnsupportedOperationException e)
            {
                // This will throw for clients with custom sas token providers since you cannot configure this value at the SDK
                // level when the user controls all aspects of SAS token generation.
                log.debug("Ignoring UnsupportedOperationException because it was expected");
            }
        }

        Success[] amqpDisconnectDidNotHappenSuccesses = new Success[clients.size()];
        Success[] mqttDisconnectDidHappenSuccesses = new Success[clients.size()];
        Success[] shutdownWasGracefulSuccesses = new Success[clients.size()];
        Success[] mqttDisconnectHadTokenExpiredReasonSuccesses = new Success[clients.size()];
        for (int clientIndex = 0; clientIndex < clients.size(); clientIndex++)
        {
            amqpDisconnectDidNotHappenSuccesses[clientIndex] = new Success();
            mqttDisconnectDidHappenSuccesses[clientIndex] = new Success();
            shutdownWasGracefulSuccesses[clientIndex] = new Success();
            mqttDisconnectHadTokenExpiredReasonSuccesses[clientIndex] = new Success();
            
            amqpDisconnectDidNotHappenSuccesses[clientIndex].setResult(true); //assume success until unexpected DISCONNECTED_RETRYING
            mqttDisconnectDidHappenSuccesses[clientIndex].setResult(false); //assume failure until DISCONNECTED_RETRYING is triggered by token expiring
            shutdownWasGracefulSuccesses[clientIndex].setResult(true); //assume success until DISCONNECTED callback without CLIENT_CLOSE
            mqttDisconnectHadTokenExpiredReasonSuccesses[clientIndex].setResult(false); //assume failure until first disconnected_retrying executes with reason EXPIRED_SAS_TOKEN

            clients.get(clientIndex).registerConnectionStatusChangeCallback(
                new IotHubConnectionStatusChangeTokenRenewalCallbackVerifier(
                    clients.get(clientIndex).getConfig().getProtocol(),
                    amqpDisconnectDidNotHappenSuccesses[clientIndex],
                    mqttDisconnectDidHappenSuccesses[clientIndex],
                    shutdownWasGracefulSuccesses[clientIndex],
                    mqttDisconnectHadTokenExpiredReasonSuccesses[clientIndex]),
                clients.get(clientIndex));
        }

        openEachClient(clients);
        amqpMultiplexingClient.open();
        amqpWsMultiplexingClient.open();

        //wait until old sas token has expired, this should force the config to generate a new one from the device key
        System.out.println("Sleeping..." + System.currentTimeMillis());
        Thread.sleep((SECONDS_FOR_SAS_TOKEN_TO_LIVE_BEFORE_RENEWAL + WAIT_BUFFER_FOR_TOKEN_TO_EXPIRE) * 1000);
        System.out.println("Awake!" + System.currentTimeMillis());

        sendMessageFromEachClient(clients);

        closeClients(clients);
        amqpMultiplexingClient.close();
        amqpWsMultiplexingClient.close();

        Tools.disposeTestIdentities(testIdentities, iotHubConnectionString);

        testIdentities.clear();

        verifyClientsConnectivityBehavedCorrectly(clients, amqpDisconnectDidNotHappenSuccesses, mqttDisconnectDidHappenSuccesses, shutdownWasGracefulSuccesses, mqttDisconnectHadTokenExpiredReasonSuccesses);
    }

    private void closeClients(List<InternalClient> clients) throws IOException
    {
        for (InternalClient client : clients)
        {
            try
            {
                client.closeNow();
            }
            catch (UnsupportedOperationException ex)
            {
                // Multiplexed clients will throw this exception when closed through the individual client itself.
                // Can ignore this error since this class will close the multiplexing client itself to close these individual clients
            }
        }
    }

    private void verifyClientsConnectivityBehavedCorrectly(
        List<InternalClient> clients,
        Success[] amqpDisconnectDidNotHappenSuccesses,
        Success[] mqttDisconnectDidHappenSuccesses,
        Success[] shutdownWasGracefulSuccesses,
        Success[] mqttDisconnectHadTokenExpiredReasonSuccesses)
    {
        for (int clientIndex = 0; clientIndex < clients.size(); clientIndex++)
        {
            InternalClient client = clients.get(clientIndex);
            IotHubClientProtocol protocol = client.getConfig().getProtocol();

            if (protocol == MQTT || protocol == MQTT_WS)
            {
                assertTrue(CorrelationDetailsLoggingAssert.buildExceptionMessage("MQTT connection was never lost, token renewal was not tested", client), mqttDisconnectDidHappenSuccesses[clientIndex].result);
                assertTrue(CorrelationDetailsLoggingAssert.buildExceptionMessage("Connection was lost during test, or shutdown was not graceful", client), shutdownWasGracefulSuccesses[clientIndex].result);
                assertTrue(CorrelationDetailsLoggingAssert.buildExceptionMessage("MQTT connection was lost, but for an unexpected reason", client), mqttDisconnectHadTokenExpiredReasonSuccesses[clientIndex].result);
            }
            else if (protocol == AMQPS || protocol == AMQPS_WS)
            {
                assertTrue(CorrelationDetailsLoggingAssert.buildExceptionMessage("AMQPS connection was lost at least once, token renewal must have failed", client), amqpDisconnectDidNotHappenSuccesses[clientIndex].result);
                assertTrue(CorrelationDetailsLoggingAssert.buildExceptionMessage("Connection was lost during test, or shutdown was not graceful", client), shutdownWasGracefulSuccesses[clientIndex].result);
            }
        }
    }

    private List<InternalClient> createClientsToTest() throws IotHubException, IOException, URISyntaxException, ModuleClientException, InterruptedException, GeneralSecurityException
    {
        List<InternalClient> clients = new ArrayList<>();
        Proxy testProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(testProxyHostname, testProxyPort));
        for (IotHubClientProtocol protocol: IotHubClientProtocol.values())
        {
            clients.add(createDeviceClient(protocol));
            if (protocol == HTTPS || protocol == MQTT_WS || protocol == AMQPS_WS)
            {
                InternalClient client = createDeviceClient(protocol);
                ProxySettings proxySettings = new ProxySettings(testProxy, testProxyUser, testProxyPass);
                client.setProxySettings(proxySettings);
                clients.add(client);
            }

            if (protocol != IotHubClientProtocol.HTTPS && !isBasicTierHub)
            {
                clients.add(createModuleClient(protocol));
            }

            // Add another client with a custom sas token provider. This is important to test
            // because we want to make sure that the sas token provider is called to get the new sas token and that it mqtt/amqp connections
            // with it behave the same way as mqtt/amqp connections without it.
            UUID uuid = UUID.randomUUID();
            String deviceId = "token-renewal-test-device-with-custom-sas-token-provider-" + protocol + "-" + uuid.toString();
            com.microsoft.azure.sdk.iot.service.Device device = com.microsoft.azure.sdk.iot.service.Device.createFromId(deviceId, DeviceStatus.Enabled, null);
            device = registryManager.addDevice(device);
            SasTokenProvider sasTokenProvider = new SasTokenProviderImpl(registryManager.getDeviceConnectionString(device), (int) (SECONDS_FOR_SAS_TOKEN_TO_LIVE_BEFORE_RENEWAL));
            clients.add(new DeviceClient(iotHubHostName, deviceId, sasTokenProvider, protocol));
        }

        return clients;
    }

    private MultiplexingClient createMultiplexedClientToTest(IotHubClientProtocol protocol, List<DeviceClient> clientsToCreate, String hostname) throws IotHubException, IOException, URISyntaxException, MultiplexingClientException, InterruptedException, GeneralSecurityException
    {
        MultiplexingClient multiplexingClient = new MultiplexingClient(hostname, protocol);

        for (int i = 0; i < MULTIPLEX_COUNT; i++)
        {
            DeviceClient deviceClientToMultiplex = (DeviceClient) createDeviceClient(protocol);
            clientsToCreate.add(deviceClientToMultiplex);
        }

        multiplexingClient.registerDeviceClients(clientsToCreate);

        return multiplexingClient;
    }

    private void sendMessageFromEachClient(List<InternalClient> clients)
    {
        for (InternalClient client : clients)
        {
            System.out.println("Sending test message for client " + client.getConfig().getDeviceId());
            IotHubServicesCommon.sendMessageAndWaitForResponse(client, new MessageAndResult(new Message("some message"), IotHubStatusCode.OK_EMPTY), RETRY_MILLISECONDS, SEND_TIMEOUT_MILLISECONDS, client.getConfig().getProtocol());
        }
    }

    private void openEachClient(List<InternalClient> clients) throws IOException
    {
        for (InternalClient client : clients)
        {
            try
            {
                client.open();
            } catch (UnsupportedOperationException ex)
            {
                //client was a multiplexing client which was already opened, safe to ignore
            }
        }
    }

    /**
     * Expected callback order by protocol:
     * amqps/amqps_ws : Connected -> Disconnected (Client_closed)
     * mqtt/mqtt_ws   : Connected -> Disconnected_Retrying -> Connected -> Disconnected (Client_closed)
     */
    private static class IotHubConnectionStatusChangeTokenRenewalCallbackVerifier implements IotHubConnectionStatusChangeCallback
    {
        IotHubClientProtocol protocol;
        Success amqpDisconnectDidNotHappen;
        Success mqttDisconnectDidHappen;
        Success shutdownWasGraceful;
        Success mqttDisconnectHadTokenExpiredReason;

        public IotHubConnectionStatusChangeTokenRenewalCallbackVerifier(IotHubClientProtocol protocol, Success amqpDisconnectDidNotHappen, Success mqttDisconnectDidHappen, Success shutdownWasGraceful, Success mqttDisconnectHadTokenExpiredReason)
        {
            this.protocol = protocol;
            this.mqttDisconnectDidHappen = mqttDisconnectDidHappen;
            this.amqpDisconnectDidNotHappen = amqpDisconnectDidNotHappen;
            this.shutdownWasGraceful = shutdownWasGraceful;
            this.mqttDisconnectHadTokenExpiredReason = mqttDisconnectHadTokenExpiredReason;
        }

        @Override
        public void execute(IotHubConnectionStatus status, IotHubConnectionStatusChangeReason statusChangeReason, Throwable throwable, Object callbackContext)
        {
            System.out.println();
            System.out.println("CONNECTION STATUS UPDATE: " + status);
            System.out.println("CONNECTION STATUS REASON: " + statusChangeReason);
            System.out.println("CONNECTION STATUS THROWABLE: " + (throwable == null ? "null" : throwable.getMessage()));
            if (callbackContext instanceof InternalClient)
            {
                System.out.println("CONNECTION BELONGS TO: " + ((InternalClient) callbackContext).getConfig().getDeviceId());
            }
            System.out.println("CONNECTION PROTOCOL: " + this.protocol);
            System.out.println();

            if (status == IotHubConnectionStatus.DISCONNECTED)
            {
                if (statusChangeReason != IotHubConnectionStatusChangeReason.CLIENT_CLOSE)
                {
                    shutdownWasGraceful.setResult(false);
                }
            }
            else if (status == IotHubConnectionStatus.DISCONNECTED_RETRYING)
            {
                // AMQPS/AMQPS_WS is expected to not lose connection at any point except for when the client is closed
                // MQTT does need to tear down the expired connection in order to send the new sas token, so this
                // DISCONNECTED RETRYING is expected at least once.
                mqttDisconnectDidHappen.setResult(true);

                if (protocol == AMQPS || protocol == AMQPS_WS)
                {
                    amqpDisconnectDidNotHappen.setResult(false);
                }

                if (protocol == MQTT || protocol == MQTT_WS)
                {
                    boolean reasonIsSasTokenExpired = statusChangeReason == IotHubConnectionStatusChangeReason.EXPIRED_SAS_TOKEN;
                    mqttDisconnectHadTokenExpiredReason.setResult(reasonIsSasTokenExpired);
                }
            }
        }
    }

    private InternalClient createModuleClient(IotHubClientProtocol protocol) throws IOException, IotHubException, ModuleClientException, URISyntaxException, InterruptedException {
        UUID uuid = UUID.randomUUID();
        String deviceId = "token-renewal-test-device-" + protocol + "-" + uuid.toString();
        String moduleId = "token-renewal-test-module-" + protocol + "-" + uuid.toString();
        com.microsoft.azure.sdk.iot.service.Device device = com.microsoft.azure.sdk.iot.service.Device.createFromId(deviceId, DeviceStatus.Enabled, null);
        device = Tools.addDeviceWithRetry(registryManager, device);
        Module module = Module.createModule(deviceId, moduleId, AuthenticationType.SAS);
        module = Tools.addModuleWithRetry(registryManager, module);
        ModuleClient moduleClient = new ModuleClient(DeviceConnectionString.get(iotHubConnectionString, device, module), protocol);
        testIdentities.add(new TestModuleIdentity(moduleClient, device, module));
        return moduleClient;
    }

    private InternalClient createDeviceClient(IotHubClientProtocol protocol) throws URISyntaxException, IOException, IotHubException, GeneralSecurityException
    {
        TestDeviceIdentity testDeviceIdentity = Tools.getTestDevice(iotHubConnectionString, protocol, AuthenticationType.SAS, false);
        com.microsoft.azure.sdk.iot.service.Device device = testDeviceIdentity.getDevice();
        testIdentities.add(testDeviceIdentity);
        return new DeviceClient(DeviceConnectionString.get(iotHubConnectionString, device), protocol);
    }
}
