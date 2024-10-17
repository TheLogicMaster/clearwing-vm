package java.security;

import java.util.Properties;

public class Provider extends Properties {
    private final double version;
    private final String name;
    private final String versionStr;
    private final String info;

    protected Provider(String name, double version, String info) {
        this.name = name;
        this.version = version;
        this.versionStr = Double.toString(version);
        this.info = info;
    }

    protected Provider(String name, String versionStr, String info) {
        this.name = name;
        this.versionStr = versionStr;
        this.version = parseVersionStr(versionStr);
        this.info = info;
    }

    public String getName() {
        return this.name;
    }

    public double getVersion() {
        return this.version;
    }

    public String getVersionStr() {
        return this.versionStr;
    }

    public String getInfo() {
        return this.info;
    }

    private static double parseVersionStr(String s) {
        try {
            int firstDotIdx = s.indexOf(46);
            int nextDotIdx = s.indexOf(46, firstDotIdx + 1);
            if (nextDotIdx != -1) {
                s = s.substring(0, nextDotIdx);
            }

            int endIdx = s.indexOf(45);
            if (endIdx > 0) {
                s = s.substring(0, endIdx);
            }

            endIdx = s.indexOf(43);
            if (endIdx > 0) {
                s = s.substring(0, endIdx);
            }

            return Double.parseDouble(s);
        } catch (NumberFormatException | NullPointerException var4) {
            return 0.0;
        }
    }
}
