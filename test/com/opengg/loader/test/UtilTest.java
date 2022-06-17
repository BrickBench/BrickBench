package com.opengg.loader.test;

import com.opengg.loader.Util;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilTest {

    @Test
    void testShortAngleConversions(){
        assertEquals(0, Util.shortAngleToFloat((short) 0));
        assertEquals(180, Util.shortAngleToFloat(Short.MAX_VALUE));
        assertEquals(0, Util.floatToShortAngle(0));
        assertEquals(Short.MAX_VALUE, Util.floatToShortAngle(180));
    }
}
