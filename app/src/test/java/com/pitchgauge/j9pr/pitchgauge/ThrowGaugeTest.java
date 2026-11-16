package com.pitchgauge.j9pr.pitchgauge;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ThrowGaugeTest pins the angle and travel that ThrowGauge reports for a set of sensor poses.
 *
 * <p>The expected values were recorded with JOML 1.9.3-SNAPSHOT, the math library the app
 * shipped with, so any library upgrade or code change that alters a measurement fails here.
 */
@RunWith(Parameterized.class)
public class ThrowGaugeTest {

    private static final MainPrefs.throwCalcMethodT ORTHO = MainPrefs.throwCalcMethodT.ORTHO;
    private static final MainPrefs.throwCalcMethodT CHORD = MainPrefs.throwCalcMethodT.CHORD;

    // Recorded values are rounded to 9 decimals; the clock-based low-pass filter's first
    // sample adds less than 1e-9, so 1e-8 still catches any real change.
    private static final double TOLERANCE = 1e-8;

    @Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
                // name, neutral {roll, pitch, yaw}, current {roll, pitch, yaw}, ignoreZ,
                // method, chord, expected angle (deg), expected travel
                {"pitchUp20_ortho", pose(0, 0, 0), pose(0, 20, 0), true, ORTHO, 50.0,
                        19.999999999, 17.101007166},
                {"pitchDown15_ortho", pose(0, 0, 0), pose(0, -15, 0), true, ORTHO, 50.0,
                        -14.999999999, -12.940952255},
                {"pitchUp20_chord", pose(0, 0, 0), pose(0, 20, 0), true, CHORD, 50.0,
                        19.999999999, 17.364817766},
                {"roll10pitch30_ortho", pose(0, 0, 0), pose(10, 30, 0), true, ORTHO, 40.0,
                        31.586448301, 20.951377575},
                {"yaw45_counted", pose(0, 0, 0), pose(0, 20, 45), false, ORTHO, 50.0,
                        49.032470773, 37.754063102},
                {"yaw45_ignored", pose(0, 0, 0), pose(0, 20, 45), true, ORTHO, 50.0,
                        19.999999999, 17.101007166},
                {"offsetNeutral_chord", pose(5, -3, 10), pose(5, 25, 10), false, CHORD, 60.0,
                        27.999999999, 29.030627471},
                {"beyond90_chord", pose(0, 0, 0), pose(0, 100, 0), true, CHORD, 50.0,
                        99.999999996, 0.0},
                {"zeroChord_angleOnly", pose(0, 0, 0), pose(0, 35, 0), true, ORTHO, 0.0,
                        34.999999999, 0.0},
                {"noMovement", pose(0, 0, 0), pose(0, 0, 0), true, ORTHO, 50.0,
                        0.0, 0.0},
                {"rollAcross180", pose(-170, 0, 0), pose(170, 0, 0), true, ORTHO, 50.0,
                        19.999999999, 17.101007166},
        });
    }

    @Parameter(0)
    public String name;

    @Parameter(1)
    public float[] neutral;

    @Parameter(2)
    public float[] current;

    @Parameter(3)
    public boolean ignoreZ;

    @Parameter(4)
    public MainPrefs.throwCalcMethodT method;

    @Parameter(5)
    public double chord;

    @Parameter(6)
    public double expectedAngle;

    @Parameter(7)
    public double expectedTravel;

    @Test
    public void resolveQuatsThrow_matchesJoml193() {
        ThrowGauge gauge = new ThrowGauge();
        gauge.setIgnoreZ(ignoreZ);
        gauge.setThrowCalcMethod(method);
        gauge.SetChord(chord);
        gauge.SetAngles(neutral[0], neutral[1], neutral[2]);
        gauge.SetNeutral();
        gauge.SetAngles(current[0], current[1], current[2]);

        double travel = gauge.GetThrow();

        assertEquals("angle", expectedAngle, gauge.GetAngle(), TOLERANCE);
        assertEquals("travel", expectedTravel, travel, TOLERANCE);
    }

    private static float[] pose(float roll, float pitch, float yaw) {
        return new float[] {roll, pitch, yaw};
    }
}
