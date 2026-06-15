package com.ghartur.springosgi.osgi;

public record BundleInfo(long id, String symbolicName, String version, String state) {

    static String stateLabel(int state) {
        return switch (state) {
            case 1  -> "UNINSTALLED";
            case 2  -> "INSTALLED";
            case 4  -> "RESOLVED";
            case 8  -> "STARTING";
            case 16 -> "STOPPING";
            case 32 -> "ACTIVE";
            default -> "UNKNOWN";
        };
    }
}
