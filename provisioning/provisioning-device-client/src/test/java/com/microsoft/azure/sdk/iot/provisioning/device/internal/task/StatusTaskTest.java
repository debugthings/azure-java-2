/*
 *
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 *
 */

package com.microsoft.azure.sdk.iot.provisioning.device.internal.task;

import com.microsoft.azure.sdk.iot.provisioning.device.internal.ProvisioningDeviceClientConfig;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.contract.ProvisioningDeviceClientContract;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.contract.ResponseCallback;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.contract.UrlPathBuilder;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceSecurityException;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.parser.DeviceRegistrationParser;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.parser.ProvisioningErrorParser;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.parser.RegistrationOperationStatusParser;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProvider;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.microsoft.azure.sdk.iot.provisioning.device.internal.task.ContractState.DPS_REGISTRATION_RECEIVED;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
    Unit tests for Status task
    Coverage : 80% Method, 82% Line
 */
@RunWith(JMockit.class)
public class StatusTaskTest
{
    private static final String TEST_OPERATION_ID = "testOperationId";
    private static final String TEST_REGISTRATION_ID = "testRegistrationId";

    @Mocked
    SecurityProvider mockedSecurityProvider;
    @Mocked
    ProvisioningDeviceClientContract mockedProvisioningDeviceClientContract;
    @Mocked
    ProvisioningDeviceClientConfig mockedProvisioningDeviceClientConfig;
    @Mocked
    Authorization mockedAuthorization;
    @Mocked
    DeviceRegistrationParser mockedDeviceRegistrationParser;
    @Mocked
    UrlPathBuilder mockedUrlPathBuilder;
    @Mocked
    SSLContext mockedSslContext;
    @Mocked
    RegistrationOperationStatusParser mockedRegistrationOperationStatusParser;
    @Mocked
    ResponseCallback mockedResponseCallback;
    @Mocked
    ResponseData mockedResponseData;
    @Mocked
    RequestData mockedRequestData;

    //Tests_SRS_StatusTask_25_001: [ Constructor shall save operationId , dpsSecurityProvider, provisioningDeviceClientContract and authorization. ]
    @Test
    public void constructorSucceeds() throws ProvisioningDeviceClientException
    {
        //arrange
        //act
        StatusTask statusTask = Deencapsulation.newInstance(
                StatusTask.class,
                new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                },
                mockedSecurityProvider,
                mockedProvisioningDeviceClientContract,
                mockedProvisioningDeviceClientConfig,
                TEST_OPERATION_ID,
                mockedAuthorization);
        //assert
        assertNotNull(Deencapsulation.getField(statusTask, "securityProvider"));
        assertNotNull(Deencapsulation.getField(statusTask, "provisioningDeviceClientContract"));
        assertNotNull(Deencapsulation.getField(statusTask, "authorization"));
        assertEquals(TEST_OPERATION_ID, Deencapsulation.getField(statusTask, "operationId"));
    }

    //Tests_SRS_StatusTask_25_002: [ Constructor shall throw ProvisioningDeviceClientException if operationId , dpsSecurityProvider, authorization or provisioningDeviceClientContract is null. ]
    @Test
    public void constructorThrowsOnNullSecurityProvider() throws ProvisioningDeviceClientException {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //act
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                            SecurityProvider.class,
                            ProvisioningDeviceClientContract.class,
                            ProvisioningDeviceClientConfig.class,
                            String.class,
                            Authorization.class
                    },
                    null,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
        });
    }

    @Test
    public void constructorThrowsOnNullContract() throws ProvisioningDeviceClientException {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //act
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                            SecurityProvider.class,
                            ProvisioningDeviceClientContract.class,
                            ProvisioningDeviceClientConfig.class,
                            String.class,
                            Authorization.class
                    },
                    mockedSecurityProvider,
                    null,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
        });
    }

    @Test
    public void constructorThrowsOnNullOperationId() throws ProvisioningDeviceClientException {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //act
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    null,
                    mockedAuthorization);
        });
    }

    @Test
    public void constructorThrowsOnEmptyOperationId() throws ProvisioningDeviceClientException {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //act
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    "",
                    mockedAuthorization);
        });
    }

    @Test
    public void constructorThrowsOnNullAuth() throws ProvisioningDeviceClientException {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //act
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    null);
        });
    }

    @Test
    public void getRegistrationStatusSucceeds() throws Exception
    {
        //arrange
        StatusTask statusTask = Deencapsulation.newInstance(
                StatusTask.class,
                new Class[] {
                    SecurityProvider.class,
                    ProvisioningDeviceClientContract.class,
                    ProvisioningDeviceClientConfig.class,
                    String.class,
                    Authorization.class
                },
                mockedSecurityProvider,
                mockedProvisioningDeviceClientContract,
                mockedProvisioningDeviceClientConfig,
                TEST_OPERATION_ID,
                mockedAuthorization);
        new NonStrictExpectations()
        {
            {
                mockedSecurityProvider.getRegistrationId();
                result = TEST_REGISTRATION_ID;
                Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                result = mockedSslContext;
                Deencapsulation.invoke(mockedResponseData, "getResponseData");
                result = "NonNullValue".getBytes(StandardCharsets.UTF_8);
                Deencapsulation.invoke(mockedResponseData, "getContractState");
                result = DPS_REGISTRATION_RECEIVED;
                RegistrationOperationStatusParser.createFromJson(anyString);
                result = mockedRegistrationOperationStatusParser;
            }
        };

        //act
        statusTask.call();
        //assert
        new Verifications()
        {
            {
                mockedProvisioningDeviceClientContract.getRegistrationStatus((RequestData) any,
                                                                             (ResponseCallback)any, any);
                times = 1;
            }
        };
    }

    //Tests_SRS_StatusTask_25_003: [ This method shall throw ProvisioningDeviceClientException if registration id is null or empty. ]
    @Test
    public void getRegistrationStatusThrowsOnNullRegId() throws Exception {
        assertThrows(ProvisioningDeviceSecurityException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = null;
                }
            };

            //act
            statusTask.call();
        });
    }

    @Test
    public void getRegistrationStatusThrowsOnEmptyRegId() throws Exception {
        assertThrows(ProvisioningDeviceSecurityException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = "";
                }
            };

            //act
            statusTask.call();
        });
    }

    //Tests_SRS_StatusTask_25_004: [ This method shall retrieve the SSL context from Authorization and throw ProvisioningDeviceClientException if it is null. ]
    @Test
    public void getRegistrationStatusThrowsOnNullSslContext() throws Exception {
        assertThrows(ProvisioningDeviceSecurityException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = TEST_REGISTRATION_ID;
                    Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                    result = null;
                }
            };

            //act
            statusTask.call();
        });
    }

    //Tests_SRS_StatusTask_25_005: [ This method shall trigger getRegistrationState on the contract API and wait for response and return it. ]
    @Test
    public void getRegistrationStatusThrowsOnContractGetStatusFails() throws Exception {
        assertThrows(IOException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = TEST_REGISTRATION_ID;
                    Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                    result = mockedSslContext;
                    mockedProvisioningDeviceClientContract.getRegistrationStatus((RequestData) any,
                                                                                 (ResponseCallback)any, any);
                    result = new IOException("testException");
                }
            };

            //act
            statusTask.call();
        });
    }

    //Tests_SRS_StatusTask_25_006: [ This method shall throw ProvisioningDeviceClientException if null response or no response is received in maximum time of 90 seconds. ]
    @Test
    public void getRegistrationStatusThrowsIfNoResponseReceivedInMaxTime() throws Exception {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = TEST_REGISTRATION_ID;
                    Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                    result = mockedSslContext;
                }
            };

            //act
            statusTask.call();
        });
    }

    @Test
    public void getRegistrationStatusThrowsOnNullResponseReceived() throws Exception {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class, new Class[] {
                            SecurityProvider.class,
                            ProvisioningDeviceClientContract.class,
                            ProvisioningDeviceClientConfig.class,
                            String.class,
                            Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID, mockedAuthorization);

            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = TEST_REGISTRATION_ID;
                    Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                    result = mockedSslContext;
                    Deencapsulation.invoke(mockedResponseData, "getResponseData");
                    result = null;
                }
            };

            //act
            statusTask.call();
        });
    }

    @Test
    public void getRegistrationStatusThrowsOnInterruptedException() throws Exception {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class, new Class[] {
                        SecurityProvider.class,
                        ProvisioningDeviceClientContract.class,
                        ProvisioningDeviceClientConfig.class,
                        String.class,
                        Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);

            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = TEST_REGISTRATION_ID;
                    Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                    result = mockedSslContext;
                    Deencapsulation.invoke(mockedResponseData, "getResponseData");
                    result = "NonNullValue".getBytes(StandardCharsets.UTF_8);
                    Deencapsulation.invoke(mockedResponseData, "getContractState");
                    result = DPS_REGISTRATION_RECEIVED;
                    RegistrationOperationStatusParser.createFromJson(anyString);
                    result = new InterruptedException();
                }
            };

            //act
            statusTask.call();
        });
    }

    //Tests_SRS_StatusTask_34_007: [ If the response data cannot be parsed into a RegistrationOperationStatusParser,
    // this function shall parse it into a ProvisioningErrorParser and throw a ProvisioningDeviceClientException with the parsed message. ]
    @Test
    public void getRegistrationStatusFallsBackToErrorParserIfRegistrationOperationStatusParsingFails(@Mocked final ProvisioningErrorParser mockedProvisioningErrorParser) throws Exception {
        assertThrows(ProvisioningDeviceClientException.class, () -> {
            //arrange
            StatusTask statusTask = Deencapsulation.newInstance(
                    StatusTask.class,
                    new Class[] {
                            SecurityProvider.class,
                            ProvisioningDeviceClientContract.class,
                            ProvisioningDeviceClientConfig.class,
                            String.class,
                            Authorization.class
                    },
                    mockedSecurityProvider,
                    mockedProvisioningDeviceClientContract,
                    mockedProvisioningDeviceClientConfig,
                    TEST_OPERATION_ID,
                    mockedAuthorization);
            new NonStrictExpectations()
            {
                {
                    mockedSecurityProvider.getRegistrationId();
                    result = TEST_REGISTRATION_ID;
                    Deencapsulation.invoke(mockedAuthorization, "getSslContext");
                    result = mockedSslContext;
                    Deencapsulation.invoke(mockedResponseData, "getResponseData");
                    result = "NonNullValue".getBytes(StandardCharsets.UTF_8);
                    Deencapsulation.invoke(mockedResponseData, "getContractState");
                    result = DPS_REGISTRATION_RECEIVED;
                    RegistrationOperationStatusParser.createFromJson(anyString);
                    result = new IllegalArgumentException("Some illegal argumentException");
                    ProvisioningErrorParser.createFromJson(anyString);
                    result = mockedProvisioningErrorParser;
                }
            };

            //act
            statusTask.call();
            //assert
            new Verifications()
            {
                {
                    mockedProvisioningErrorParser.getExceptionMessage();
                    times = 1;
                }
            };
        });
    }
}
