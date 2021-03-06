package org.motechproject.ananya.kilkari.obd.service.validator;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

public class ErrorsTest {

    private Errors errors;

    @Before
    public void setUp() {
        errors = new Errors();
    }

    @Test
    public void shouldCheckForThePresenceOfErrorMessages() {
        assertFalse(errors.hasErrors());

        errors.add("An error message");

        assertTrue(errors.hasErrors());
    }

    @Test
    public void shouldCheckForTheAbsenceOfErrorMessages() {
        assertTrue(errors.hasNoErrors());

        errors.add("An error message");

        assertFalse(errors.hasNoErrors());
    }

    @Test
    public void shouldGetAllErrors() {
        String errorMessage1 = "An error message";
        String errorMessage2 = "Another error message";
        errors.add(errorMessage1);
        errors.add(errorMessage2);
        String expectedErrorMessage = errorMessage1 + "," + errorMessage2;

        String actualErrorMessage = errors.allMessages();

        assertEquals(expectedErrorMessage, actualErrorMessage);
    }

    @Test
    public void shouldAddACollectionOfErrorMessages() {
        Errors newErrors = new Errors();
        newErrors.add("Error message 1");
        newErrors.add("Error message 2");
        String expectedErrorMessage = "Error message 1" + "," + "Error message 2";

        errors.addAll(newErrors);

        String actualErrorMessage = errors.allMessages();
        assertEquals(expectedErrorMessage, actualErrorMessage);
    }

    @Test
    public void shouldReturnTheNumberOfErrorMessages() {
        Errors newErrors = new Errors();
        newErrors.add("Error message 1");
        newErrors.add("Error message 2");
        errors.addAll(newErrors);

        int errorsCount = errors.getCount();
        assertEquals(2, errorsCount);
    }
}
