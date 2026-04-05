package com.windowvanish.client;

public class OperatingSytemChecker {
    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}
