package org.etieskrill.games.particles;

import java.util.Arrays;

public class ApproxExpFunction {

    private static final int APPROX_NUM_MULTS_IN_POW = 9; //reasonable limit for glsl impls - https://stackoverflow.com/a/68793086 - funny this experiment is for java then _facepalm_
    private static final float ITERATIONS = 10_000_000;

    private final float endRange = 4;
    private final int base = 4;

    public static void main(String[] args) {
        var approx = new ApproxExpFunction();

        double[] powResults = new double[(int) ITERATIONS];
        long powTime = approx.runPow(powResults);

        double[] polyResults = new double[(int) ITERATIONS];
        double[] avgDeviation = new double[APPROX_NUM_MULTS_IN_POW], maxDeviation = new double[APPROX_NUM_MULTS_IN_POW];
        long[] polyTime = new long[APPROX_NUM_MULTS_IN_POW];
        for (int i = 0; i < APPROX_NUM_MULTS_IN_POW; i++) {
            Arrays.fill(polyResults, 0);
            polyTime[i] = approx.runPoly(powResults, polyResults, i, avgDeviation, maxDeviation);
        }

        int approxExponentIndex = 0;
        for (int i = 1; i < avgDeviation.length; i++) {
            if (avgDeviation[approxExponentIndex] > avgDeviation[i]) {
                approxExponentIndex = i;
            }
        }

        System.out.println("Exponent '" + approxExponentIndex + "' produces most accurate result");
        System.out.println("Averaged deviation: %.2f%%".formatted(100 * avgDeviation[approxExponentIndex]));
        System.out.println("Max deviation: %.2f%%".formatted((float) 100 * maxDeviation[approxExponentIndex]));

        System.out.println();
        System.out.println("Time pow: " + powTime / 1_000 + "ys");
        System.out.println("Time poly: " + polyTime[approxExponentIndex] / 1_000 + "ys");
        System.out.println("Speedup pow vs. poly: %.3f".formatted((float) powTime / polyTime[approxExponentIndex]));
    }

    private long runPow(double[] results) {
        long time = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            results[i] = Math.pow(base, (i * endRange / ITERATIONS) - endRange);
        }
        long powTime = System.nanoTime();
        powTime -= time;
        return powTime;
    }

    private long runPoly(double[] powResults, double[] polyResults, int approxExponent, double[] avgDeviation, double[] maxDeviation) {
        double factor = 1 / Math.pow(endRange, approxExponent);
        long time = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {

            double x = endRange * i / ITERATIONS;
            double result = factor;
            for (int j = 0; j < approxExponent; j++) result *= x;

            polyResults[i] = result;
        }
        long polyTime = System.nanoTime() - time;

        double deviationSum = 0;
        maxDeviation[approxExponent] = 0;
        for (int i = 0; i < powResults.length; i++) {
            double deviation = Math.abs(powResults[i] - polyResults[i]);

            deviationSum += deviation; //range is [0,1) already, so no normalisation is needed
            maxDeviation[approxExponent] = Math.max(maxDeviation[approxExponent], deviation);
        }
        avgDeviation[approxExponent] = (float) (deviationSum / powResults.length);

        return polyTime;
    }

}
