package org.etieskrill.engine.time;

import org.lwjgl.system.JNI;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.WindowsLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeResolutionUtils {

    private static final Logger logger = LoggerFactory.getLogger(TimeResolutionUtils.class);

    public static void setSystemTimeResolution(int resolution) {
        switch (Platform.get()) {
            case WINDOWS -> {
                try (WindowsLibrary lib = new WindowsLibrary("winmm.dll")) {
                    long beginFunctionPointer = lib.getFunctionAddress("timeBeginPeriod");
                    int ret = JNI.invokeI(resolution, beginFunctionPointer);
                    if (ret != 0) {
                        logger.warn("Unsuccessfully tried to set system time resolution to {}, return value: {}", resolution, ret);
                    } else {
                        logger.debug("Successfully set system time resolution to {}", resolution);
                    }
                }
            }
        }
    }

    public static void resetSystemTimeResolution(int resolution) {
        switch (Platform.get()) {
            case WINDOWS -> {
                try (WindowsLibrary lib = new WindowsLibrary("winmm.dll")) {
                    long endFunctionPointer = lib.getFunctionAddress("timeEndPeriod");
                    int ret = JNI.invokeI(resolution, endFunctionPointer);
                    if (ret != 0) {
                        logger.warn("Unsuccessfully tried to reset system time resolution, return value: {}", ret);
                    } else {
                        logger.debug("Successfully reset system time resolution");
                    }
                }
            }
        }
    }

    private TimeResolutionUtils() {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

}
