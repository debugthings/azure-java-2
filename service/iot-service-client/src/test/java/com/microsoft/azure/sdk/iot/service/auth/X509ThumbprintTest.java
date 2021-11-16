/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.service.auth;

import mockit.Deencapsulation;
import mockit.integration.junit4.JMockit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Code coverage:
 * 100% Methods
 * 100% lines
 */
@RunWith(JMockit.class)
public class X509ThumbprintTest
{
    String expectedPrimaryThumbprint;
    String expectedSecondaryThumbprint;

    @BeforeEach
    public void setUp()
    {
        X509Thumbprint thumbprint = Deencapsulation.newInstance(X509Thumbprint.class);
        expectedPrimaryThumbprint = Deencapsulation.invoke(thumbprint, "getPrimaryThumbprint");
        expectedSecondaryThumbprint = Deencapsulation.invoke(thumbprint, "getSecondaryThumbprint");
    }

    //Tests_SRS_X509THUMBPRINT_34_001: [The function shall return the primary thumbprint value of this.]
    //Tests_SRS_X509THUMBPRINT_34_002: [The function shall return the secondary thumbprint value of this.]
    //Tests_SRS_X509THUMBPRINT_34_003: [The function shall set the primary thumbprint to the given value.]
    //Tests_SRS_X509THUMBPRINT_34_004: [The function shall set the secondary thumbprint to the given value.]
    @Test
    public void gettersAndSettersWork()
    {
        //arrange
        X509Thumbprint thumbprint = createTestThumbprint(expectedPrimaryThumbprint.toLowerCase(), expectedSecondaryThumbprint.toLowerCase());

        //act
        Deencapsulation.invoke(thumbprint, "setPrimaryThumbprint", new Class[] { String.class }, expectedPrimaryThumbprint);
        Deencapsulation.invoke(thumbprint, "setSecondaryThumbprint", new Class[] { String.class }, expectedSecondaryThumbprint);

        //assert
        assertEquals(expectedPrimaryThumbprint, Deencapsulation.invoke(thumbprint, "getPrimaryThumbprint", new Class[] {}));
        assertEquals(expectedSecondaryThumbprint, Deencapsulation.invoke(thumbprint, "getSecondaryThumbprint", new Class[] {}));
    }

    //Tests_SRS_X509THUMBPRINT_34_006: [This constructor shall create an X509Thumbprint with the provided primary thumbprint and the provided secondary thumbprint.]
    @Test
    public void constructorSetsPrimaryAndSecondaryThumbprints()
    {
        //act
        X509Thumbprint thumbprint = createTestThumbprint(expectedPrimaryThumbprint, expectedSecondaryThumbprint);

        //assert
        assertEquals(expectedPrimaryThumbprint, Deencapsulation.invoke(thumbprint, "getPrimaryThumbprint", new Class[] {}));
        assertEquals(expectedSecondaryThumbprint, Deencapsulation.invoke(thumbprint, "getSecondaryThumbprint", new Class[] {}));
    }

    //Tests_SRS_X509THUMBPRINT_34_010: [This constructor shall throw an IllegalArgumentException if the provided thumbprints are null, empty, or not a valid format.]
    @Test
    public void illegalArgumentExceptionThrownByConstructorForEmptyPrimaryThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            createTestThumbprint("", expectedSecondaryThumbprint);
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_010: [This constructor shall throw an IllegalArgumentException if the provided thumbprints are null, empty, or not a valid format.]
    @Test
    public void illegalArgumentExceptionThrownByConstructorForEmptySecondaryThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            createTestThumbprint(expectedPrimaryThumbprint, "");
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_010: [This constructor shall throw an IllegalArgumentException if the provided thumbprints are null, empty, or not a valid format.]
    @Test
    public void illegalArgumentExceptionThrownByConstructorForNullPrimaryThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            createTestThumbprint(null, expectedSecondaryThumbprint);
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_010: [This constructor shall throw an IllegalArgumentException if the provided thumbprints are null, empty, or not a valid format.]
    @Test
    public void illegalArgumentExceptionThrownByConstructorForNullSecondaryThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            createTestThumbprint(expectedPrimaryThumbprint, null);
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_010: [This constructor shall throw an IllegalArgumentException if the provided thumbprints are null, empty, or not a valid format.]
    @Test
    public void illegalArgumentExceptionThrownByConstructorForInvalidPrimaryThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            createTestThumbprint("NOT_A_THUMBPRINT", expectedSecondaryThumbprint);
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_010: [This constructor shall throw an IllegalArgumentException if the provided thumbprints are null, empty, or not a valid format.]
    @Test
    public void illegalArgumentExceptionThrownByConstructorForInvalidSecondaryThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //act
            createTestThumbprint(expectedPrimaryThumbprint, "NOT_A_THUMBPRINT");
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_007: [If the provided thumbprint string is not the proper format, an IllegalArgumentException shall be thrown.]
    @Test
    public void illegalArgumentExceptionThrownBySetPrimaryThumbprintForIllegalThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //arrange
            X509Thumbprint thumbprint = createTestThumbprint(null, null);

            //act
            Deencapsulation.invoke(thumbprint, "setPrimaryThumbprint", new Class[] { String.class }, "");
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_008: [If the provided thumbprint string is not the proper format, an IllegalArgumentException shall be thrown.]
    @Test
    public void illegalArgumentExceptionThrownBySetSecondaryThumbprintForIllegalThumbprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            //arrange
            X509Thumbprint thumbprint = createTestThumbprint(null, null);

            //act
            Deencapsulation.invoke(thumbprint, "setSecondaryThumbprint", new Class[] { String.class }, "");
        });
    }

    //Tests_SRS_X509THUMBPRINT_34_011: [This constructor shall generate a random primary and secondary thumbprint.]
    @Test
    public void emptyConstructorGeneratesThumbprints()
    {
        //act
        X509Thumbprint thumbprint = Deencapsulation.newInstance(X509Thumbprint.class, new Class[]{});

        //assert
        assertNotNull(Deencapsulation.getField(thumbprint, "primaryThumbprint"));
        assertNotNull(Deencapsulation.getField(thumbprint, "secondaryThumbprint"));
    }

    @Test
    public void equalsWorks()
    {
        // Arrange
        X509Thumbprint s1 = createTestThumbprintWithInvalidThumbprintValues(null, null);
        X509Thumbprint s2 = createTestThumbprintWithInvalidThumbprintValues("", null);
        X509Thumbprint s3 = createTestThumbprintWithInvalidThumbprintValues(null, "");
        X509Thumbprint s4 = createTestThumbprintWithInvalidThumbprintValues("", "");
        X509Thumbprint s5 = createTestThumbprintWithInvalidThumbprintValues("", "secondaryThumbprint");
        X509Thumbprint s6 = createTestThumbprintWithInvalidThumbprintValues("primaryThumbprint", "secondaryThumbprint2");
        X509Thumbprint s7 = createTestThumbprintWithInvalidThumbprintValues("primaryThumbprint2", "secondaryThumbprint");
        X509Thumbprint s8 = createTestThumbprintWithInvalidThumbprintValues("primaryThumbprint", "secondaryThumbprint");
        X509Thumbprint s9 = createTestThumbprintWithInvalidThumbprintValues("primaryThumbprint", "secondaryThumbprint");

        // Assert
        assertNotEquals(s1, s2);
        assertNotEquals(s1, s3);
        assertNotEquals(s1, s4);
        assertNotEquals(s1, s5);
        assertNotEquals(s1, s6);
        assertNotEquals(s1, s7);
        assertNotEquals(s1, s8);

        assertNotEquals(s2, s3);
        assertNotEquals(s2, s4);
        assertNotEquals(s2, s5);
        assertNotEquals(s2, s6);
        assertNotEquals(s2, s7);
        assertNotEquals(s2, s8);

        assertNotEquals(s3, s4);
        assertNotEquals(s3, s5);
        assertNotEquals(s3, s6);
        assertNotEquals(s3, s7);
        assertNotEquals(s3, s8);

        assertNotEquals(s4, s5);
        assertNotEquals(s4, s6);
        assertNotEquals(s4, s7);
        assertNotEquals(s4, s8);

        assertNotEquals(s5, s6);
        assertNotEquals(s5, s7);
        assertNotEquals(s5, s8);

        assertNotEquals(s6, s7);
        assertNotEquals(s6, s8);

        assertNotEquals(s7, s8);

        assertEquals(s8, s9);

        assertNotEquals(s1, "not a thumbprint object");
    }

    /**
     * Creates a thumbprint using the constructor X509Thumbprint(String primaryThumbprint, String secondaryThumbprint)
     * @param primaryThumbprint the primary thumbprint value to set
     * @param secondaryThumbprint the secondary thumbprint value to set
     * @return the created thumbprint object
     */
    private X509Thumbprint createTestThumbprint(String primaryThumbprint, String secondaryThumbprint)
    {
        return Deencapsulation.newInstance(X509Thumbprint.class, new Class[] { String.class, String.class }, primaryThumbprint, secondaryThumbprint);
    }

    /**
     * Creates a thumbprint using the empty constructor for X509Thumbprint and then sets the primary and secondary thumbprints using reflection.
     * This avoids some checks in the constructor and allows for creating thumbprints in illegal states.
     * @param primaryThumbprint the primary thumbprint value to set
     * @param secondaryThumbprint the secondary thumbprint value to set
     * @return the created thumbprint object
     */
    private X509Thumbprint createTestThumbprintWithInvalidThumbprintValues(String primaryThumbprint, String secondaryThumbprint)
    {
        X509Thumbprint thumbprint = Deencapsulation.newInstance(X509Thumbprint.class, new Class[] { String.class, String.class }, expectedPrimaryThumbprint, expectedSecondaryThumbprint);
        Deencapsulation.setField(thumbprint, "primaryThumbprint", primaryThumbprint);
        Deencapsulation.setField(thumbprint, "secondaryThumbprint", secondaryThumbprint);
        return thumbprint;
    }
}
