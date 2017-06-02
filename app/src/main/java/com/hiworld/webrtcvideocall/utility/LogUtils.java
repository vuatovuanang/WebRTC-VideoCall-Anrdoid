package com.hiworld.webrtcvideocall.utility;

import android.util.Log;


public class LogUtils {
    public static boolean DEBUG = true;

    public LogUtils() {
    }

    public static void d(String message) {
        StackTraceElement stackTraceElement = (new Throwable()).getStackTrace()[1];
        if (DEBUG) {
            Log.d(stackTraceElement.getFileName() + " in " + stackTraceElement.getMethodName() +
                    " at line: " + stackTraceElement.getLineNumber(), message);
        }

    }

    public static void w(String message) {
        StackTraceElement stackTraceElement = (new Throwable()).getStackTrace()[1];
        if (DEBUG) {
            Log.w(stackTraceElement.getFileName() + " in " + stackTraceElement.getMethodName() +
                    " at line: " + stackTraceElement.getLineNumber(), message);
        }

    }

    public static void i(String message) {
        StackTraceElement stackTraceElement = (new Throwable()).getStackTrace()[1];
        if (DEBUG) {
            Log.i(stackTraceElement.getFileName() + " in " + stackTraceElement.getMethodName() +
                    " at line: " + stackTraceElement.getLineNumber(), message);
        }

    }

    public static void e(String message) {
        StackTraceElement stackTraceElement = (new Throwable()).getStackTrace()[1];
        if (DEBUG) {
            Log.e(stackTraceElement.getFileName() + " in " + stackTraceElement.getMethodName() +
                    " at line: " + stackTraceElement.getLineNumber(), message);
        }

    }
}
