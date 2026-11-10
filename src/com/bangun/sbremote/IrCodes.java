package com.bangun.sbremote;

/**
 * Samsung36 IR frames for the Samsung HW-B650 soundbar.
 *
 * Frame (LSB-first, 38 kHz):
 *   header 4500/4500 | custom 0x30, 0x0F (16 bit) | 500 mark + 4500 gap |
 *   0000 + data (8 bit) + ~data (8 bit) | 500 stop mark
 * bit 0 = 500/500 us, bit 1 = 500/1500 us
 */
public final class IrCodes {
    public static final int CARRIER_HZ = 38000;

    private static final int HDR_MARK = 4500, HDR_SPACE = 4500;
    private static final int MARK = 500, ZERO = 500, ONE = 1500, GAP = 4500;
    private static final int CUSTOM_LO = 0x30, CUSTOM_HI = 0x0F;

    private IrCodes() {}

    /** Build the on/off pattern in microseconds for a soundbar data byte. */
    public static int[] pattern(int data) {
        int[] p = new int[2 + 16 * 2 + 2 + 20 * 2 + 1];
        int i = 0;
        p[i++] = HDR_MARK;
        p[i++] = HDR_SPACE;
        i = putBits(p, i, CUSTOM_LO, 8);
        i = putBits(p, i, CUSTOM_HI, 8);
        p[i++] = MARK;
        p[i++] = GAP;
        i = putBits(p, i, 0, 4);
        i = putBits(p, i, data & 0xFF, 8);
        i = putBits(p, i, (~data) & 0xFF, 8);
        p[i++] = MARK;
        return p;
    }

    private static int putBits(int[] p, int i, int value, int n) {
        for (int b = 0; b < n; b++) {
            p[i++] = MARK;
            p[i++] = ((value >> b) & 1) != 0 ? ONE : ZERO;
        }
        return i;
    }
}
