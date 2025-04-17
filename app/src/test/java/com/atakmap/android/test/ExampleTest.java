
package com.atakmap.android.test;

import static org.junit.Assert.*;

import com.atakmap.android.trackingplugin.plugin.BuildConfig;

import org.junit.Test;

public class ExampleTest {
    @Test
    public void basicArithmetic() {
        assertEquals(4, 2 + 2);
    }

    /// @noinspection ConstantValue
    @Test(expected=org.junit.ComparisonFailure.class)
    public void shouldFail() {
        if (BuildConfig.BUILD_TYPE.equals("debug"))
            assertEquals(BuildConfig.BUILD_TYPE, "release");
        else
            assertEquals(BuildConfig.BUILD_TYPE, "debug");
    }
}
