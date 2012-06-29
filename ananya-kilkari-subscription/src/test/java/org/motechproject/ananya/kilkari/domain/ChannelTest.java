package org.motechproject.ananya.kilkari.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ChannelTest {

    @Test
    public void shouldConvertStringToEnum() {
        assertEquals(Channel.from("ivr"), Channel.IVR);
        assertEquals(Channel.from("IVR"), Channel.IVR);
        assertEquals(Channel.from("IVr"), Channel.IVR);
        assertEquals(Channel.from("call_center"), Channel.CALL_CENTER);
    }

    @Test
    public void shouldReturnFalseIfChannelIsInvalid() {
        assertFalse(Channel.isValid("abc"));
        assertFalse(Channel.isValid(""));
        assertFalse(Channel.isValid(" "));
        assertFalse(Channel.isValid(null));
    }

    @Test
    public void shouldReturnTrueIfChannelIsValid() {
        assertTrue(Channel.isValid("ivr"));
        assertTrue(Channel.isValid("IVR"));
        assertTrue(Channel.isValid("IVr"));
        assertTrue(Channel.isValid(" IVr "));
    }

    @Test
    public void shouldReturnTrueIfChannelIsIVR() {
        assertTrue(Channel.isIVR("ivr"));
        assertTrue(Channel.isIVR("IVR"));
        assertTrue(Channel.isIVR("IVr"));
        assertTrue(Channel.isIVR(" IVr "));
    }

    @Test
    public void shouldReturnFalseIfChannelIsNotIVR() {
        assertFalse(Channel.isIVR(""));
        assertFalse(Channel.isIVR(" "));
        assertFalse(Channel.isIVR(null));
        assertFalse(Channel.isIVR("abc"));
        assertFalse(Channel.isIVR("CALL_CENTER"));
    }
}
