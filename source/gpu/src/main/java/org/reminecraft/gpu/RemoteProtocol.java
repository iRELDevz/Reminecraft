package org.reminecraft.gpu;

public final class RemoteProtocol {

    public static final int MAGIC = 0x524D4750;
    public static final byte OP_NOISE = 1;
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_ERR = 1;
    public static final long MAX_POINTS = 16_000_000L;

    private RemoteProtocol() {
    }
}
