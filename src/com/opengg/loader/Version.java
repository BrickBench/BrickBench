package com.opengg.loader;

/**
 * A semantically-comparable version number container.
 */
public record Version(String version) implements Comparable<Version> {
    public Version {
        if (version == null)
            throw new IllegalArgumentException("Version can not be null");
        if (!version.matches("v?[0-9]+(\\.[0-9a-z]+)*"))
            throw new IllegalArgumentException("Invalid version format");
    }

    @Override
    public int compareTo(Version other) {
        if (this.version.equals(other.version)) return 0;
        String[] thisParts = this.version.replace("v", "").split("\\.");
        String[] thatParts = other.version.replace("v", "").split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart;
            if (!(i < thisParts.length)) thisPart = 0;
            else if (!isInteger(thisParts[i])) thisPart = -1;
            else thisPart = Integer.parseInt(thisParts[i]);

            int thatPart;
            if (!(i < thatParts.length)) thatPart = 0;
            else if (!isInteger(thatParts[i])) thatPart = -1;
            else thatPart = Integer.parseInt(thatParts[i]);

            if (thisPart < thatPart)
                return -1;
            if (thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (that == null)
            return false;
        if (this.getClass() != that.getClass())
            return false;
        return this.compareTo((Version) that) == 0;
    }

    private static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }


}
