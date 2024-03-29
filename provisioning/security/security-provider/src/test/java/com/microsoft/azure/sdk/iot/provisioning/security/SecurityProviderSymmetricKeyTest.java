/*
 *
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 *
 */

package com.microsoft.azure.sdk.iot.provisioning.security;

import com.microsoft.azure.sdk.iot.provisioning.security.exceptions.SecurityProviderException;
import mockit.*;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
     Unit tests for SecurityProviderTpm and SecurityProvider
     Coverage :
     SecurityProvider : 100% lines, 100% methods
     SecurityProviderTpm : 95% lines, 100% methods

 */
public class SecurityProviderSymmetricKeyTest
{
    private static final byte[] testSymKey = "symmkey".getBytes(StandardCharsets.UTF_8);
    private static final String testRegId = "regId";

    private static final String testPrimaryKey = "12345";
    private static final String testSecondaryKey = "6789";

    @Mocked
    SecretKeySpec mockedSecretKeySpec;

    @Mocked
    Mac mockedMac;

    @Mocked
    MessageDigest mockedMessageDigest;

    @Mocked
    Base32 mockedBase32;

    @Mocked
    SSLContext mockedSslContext;

    @Mocked
    KeyStore mockedKeyStore;

    @Mocked
    TrustManagerFactory mockedTrustManagerFactory;

    @Mocked
    UUID mockedUUID;

    @Test
    public void testConstructorSucceeds() throws SecurityProviderException
    {
        //act
        SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
        //assert
        assertEquals(securityProviderSymmetricKey.getSymmetricKey(), testSymKey);
        assertEquals(securityProviderSymmetricKey.getRegistrationId(), testRegId);
    }

    @Test
    public void testConstructorWithBothKeysSucceeds() throws SecurityProviderException
    {
        //act
        SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testPrimaryKey, testSecondaryKey, testRegId);

        //assert
        assertEquals(testPrimaryKey, new String(securityProviderSymmetricKey.getSymmetricKey(), StandardCharsets.UTF_8));
        assertEquals(testSecondaryKey, new String(securityProviderSymmetricKey.getSecondaryKey(), StandardCharsets.UTF_8));
        assertEquals(testRegId, securityProviderSymmetricKey.getRegistrationId());
    }


    @Test
    public void testSymmetrickeyNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(null, testRegId);
            //assert
        });
    }

    @Test
    public void constructorWithBothKeysThrowsIfPrimaryNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(null, testSecondaryKey, testRegId);
        });
    }

    @Test
    public void constructorWithBothKeysThrowsIfPrimaryEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey("", testSecondaryKey, testRegId);
        });
    }

    @Test
    public void constructorWithBothKeysThrowsIfSecondaryNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testPrimaryKey, null, testRegId);
        });
    }

    @Test
    public void constructorWithBothKeysThrowsIfSecondaryEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testPrimaryKey, "", testRegId);
        });
    }

    @Test
    public void testRegIdNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, null);
            //assert
        });
    }

    @Test
    public void testSignData() throws SecurityProviderException
    {
        final String TEST_SIGNATURE = "testSignature";

        // Semmle flags this as sensitive call, but it is a false positive since it is for test purposes
        final String TEST_BASE64_DECODED_KEY = "base64DecodedKey"; //lgtm

        final String HMAC_SHA_256 = "HmacSHA256";

        //arrange
        SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
        //act
        securityProviderSymmetricKey.HMACSignData(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8), TEST_BASE64_DECODED_KEY.getBytes(StandardCharsets.UTF_8));
        //assert
        new Verifications()
        {
            {
                new SecretKeySpec((byte[]) any, HMAC_SHA_256);
                times = 1;
                mockedMac.doFinal(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8));
                times = 1;
            }
        };

    }

    @Test
    public void testSignDataThrowsSecurityProviderExceptionOnNullKey() throws SecurityProviderException {
        assertThrows(SecurityProviderException.class, () -> {
            final String TEST_SIGNATURE = "testSignature";
            final String TEST_BASE64_DECODED_KEY = "";
            final String HMAC_SHA_256 = "HmacSHA256";
            //arrange
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
            //act
            securityProviderSymmetricKey.HMACSignData(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8), null);
            //assert
            new Verifications()
            {
                {
                    new SecretKeySpec((byte[]) any, HMAC_SHA_256);
                    times = 1;
                    mockedMac.doFinal(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8));
                    times = 1;
                }
            };
        });
    }

    @Test
    public void testSignDataThrowsSecurityProviderExceptionOnEmptyKey() throws SecurityProviderException {
        assertThrows(SecurityProviderException.class, () -> {
            final String TEST_SIGNATURE = "testSignature";
            final String TEST_BASE64_DECODED_KEY = "";
            final String HMAC_SHA_256 = "HmacSHA256";
            //arrange
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
            //act
            securityProviderSymmetricKey.HMACSignData(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8), "".getBytes(StandardCharsets.UTF_8));
            //assert
            new Verifications()
            {
                {
                    new SecretKeySpec((byte[]) any, HMAC_SHA_256);
                    times = 1;
                    mockedMac.doFinal(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8));
                    times = 1;
                }
            };
        });
    }

    @Test
    public void testSignDataThrowsSecurityProviderExceptionOnNullSignature() throws SecurityProviderException {
        assertThrows(SecurityProviderException.class, () -> {
            final String TEST_SIGNATURE = "testSignature";
            final String TEST_BASE64_DECODED_KEY = "";
            final String HMAC_SHA_256 = "HmacSHA256";
            //arrange
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
            //act
            securityProviderSymmetricKey.HMACSignData(null, TEST_BASE64_DECODED_KEY.getBytes(StandardCharsets.UTF_8));
            //assert
            new Verifications()
            {
                {
                    new SecretKeySpec((byte[]) any, HMAC_SHA_256);
                    times = 1;
                    mockedMac.doFinal(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8));
                    times = 1;
                }
            };
        });
    }

    @Test
    public void testSignDataThrowsSecurityProviderExceptionOnEmptySignature() throws SecurityProviderException {
        assertThrows(SecurityProviderException.class, () -> {
            final String TEST_SIGNATURE = "testSignature";
            final String TEST_BASE64_DECODED_KEY = "";
            final String HMAC_SHA_256 = "HmacSHA256";
            //arrange
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
            //act
            securityProviderSymmetricKey.HMACSignData("".getBytes(StandardCharsets.UTF_8), TEST_BASE64_DECODED_KEY.getBytes(StandardCharsets.UTF_8));
            //assert
            new Verifications()
            {
                {
                    new SecretKeySpec((byte[]) any, HMAC_SHA_256);
                    times = 1;
                    mockedMac.doFinal(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8));
                    times = 1;
                }
            };
        });
    }

    @Test
    public void testSignDataThrowsSecurityProviderExceptionOnInvalidKey() throws SecurityProviderException, InvalidKeyException {
        assertThrows(SecurityProviderException.class, () -> {
            final String TEST_SIGNATURE = "testSignature";

            // Semmle flags this as sensitive call, but it is a false positive since it is for test purposes
            final String TEST_BASE64_DECODED_KEY = "InvalidKey"; // lgtm

            final String HMAC_SHA_256 = "HmacSHA256";
            new Expectations()
            {
                {
                    mockedMac.init((Key) any);
                    result = new InvalidKeyException();
                }
            };
            //arrange
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
            //act
            securityProviderSymmetricKey.HMACSignData(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8), TEST_BASE64_DECODED_KEY.getBytes(StandardCharsets.UTF_8));
            //assert
            new Verifications()
            {
                {
                    new SecretKeySpec((byte[]) any, HMAC_SHA_256);
                    times = 1;
                    mockedMac.doFinal(TEST_SIGNATURE.getBytes(StandardCharsets.UTF_8));
                    times = 0;
                }
            };
        });
    }

    @Test
    public void getSSLContextSucceeds() throws SecurityProviderException, KeyManagementException, KeyStoreException, CertificateException
    {
        //arrange
        SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);

        //act
        securityProviderSymmetricKey.getSSLContext();

        new Verifications()
        {
            {
                mockedKeyStore.setCertificateEntry(anyString, (Certificate) any);
                times = 4;
                mockedSslContext.init((KeyManager[]) any, (TrustManager[]) any, (SecureRandom) any);
                times = 1;
            }
        };
    }

    //SRS_SecurityClientSymmetricKey_25_005: [ This method shall throw SecurityProviderException if any of the underlying API's in generating SSL context fails. ]
    @Test
    public void getSSLContextThrowsUnderlyingException() throws SecurityProviderException, KeyStoreException {
        assertThrows(SecurityProviderException.class, () -> {
            //arrange
            SecurityProviderSymmetricKey securityProviderSymmetricKey = new SecurityProviderSymmetricKey(testSymKey, testRegId);
            new NonStrictExpectations()
            {
                {
                    mockedKeyStore.setCertificateEntry(anyString, (Certificate) any);
                    result = new KeyStoreException();
                }
            };

            //act
            securityProviderSymmetricKey.getSSLContext();
        });
    }

}
