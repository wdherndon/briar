package net.sf.briar.util;

public class ByteUtils {

	/**
	 * The maximum value that can be represented as an unsigned 16-bit integer.
	 */
	public static final int MAX_16_BIT_UNSIGNED = 65535; // 2^16 - 1

	/**
	 * The maximum value that can be represented as an unsigned 32-bit integer.
	 */
	public static final long MAX_32_BIT_UNSIGNED = 4294967295L; // 2^32 - 1

	public static void writeUint16(int i, byte[] b, int offset) {
		if(i < 0) throw new IllegalArgumentException();
		if(i > MAX_16_BIT_UNSIGNED) throw new IllegalArgumentException();
		assert b.length >= offset + 2;
		b[offset] = (byte) (i >> 8);
		b[offset + 1] = (byte) (i & 0xFF);
	}

	public static void writeUint32(long i, byte[] b, int offset) {
		if(i < 0L) throw new IllegalArgumentException();
		if(i > MAX_32_BIT_UNSIGNED) throw new IllegalArgumentException();
		assert b.length >= offset + 4;
		b[offset] = (byte) (i >> 24);
		b[offset + 1] = (byte) (i >> 16 & 0xFF);
		b[offset + 2] = (byte) (i >> 8 & 0xFF);
		b[offset + 3] = (byte) (i & 0xFF);
	}

	public static int readUint16(byte[] b, int offset) {
		if(b.length < offset + 2) throw new IllegalArgumentException();
		return ((b[offset] & 0xFF) << 8) | (b[offset + 1] & 0xFF);
	}

	public static long readUint32(byte[] b, int offset) {
		if(b.length < offset + 4) throw new IllegalArgumentException();
		return ((b[offset] & 0xFFL) << 24) | ((b[offset + 1] & 0xFFL) << 16)
		| ((b[offset + 2] & 0xFFL) << 8) | (b[offset + 3] & 0xFFL);
	}

	public static void erase(byte[] b) {
		for(int i = 0; i < b.length; i++) b[i] = 0;
	}
}
