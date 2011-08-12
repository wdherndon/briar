package net.sf.briar.transport;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

class PacketEncrypterImpl extends FilterOutputStream
implements PacketEncrypter {

	private final Cipher tagCipher, packetCipher;
	private final SecretKey packetKey;

	PacketEncrypterImpl(OutputStream out, Cipher tagCipher,
			Cipher packetCipher, SecretKey tagKey, SecretKey packetKey) {
		super(out);
		this.tagCipher = tagCipher;
		this.packetCipher = packetCipher;
		this.packetKey = packetKey;
		try {
			tagCipher.init(Cipher.ENCRYPT_MODE, tagKey);
		} catch(InvalidKeyException e) {
			throw new IllegalArgumentException(e);
		}
		if(tagCipher.getOutputSize(Constants.TAG_BYTES) != Constants.TAG_BYTES)
			throw new IllegalArgumentException();
	}

	public OutputStream getOutputStream() {
		return this;
	}

	public void writeTag(byte[] tag) throws IOException {
		if(tag.length != Constants.TAG_BYTES)
			throw new IllegalArgumentException();
		IvParameterSpec iv = new IvParameterSpec(tag);
		try {
			out.write(tagCipher.doFinal(tag));
			packetCipher.init(Cipher.ENCRYPT_MODE, packetKey, iv);
		} catch(BadPaddingException badCipher) {
			throw new IOException(badCipher);
		} catch(IllegalBlockSizeException badCipher) {
			throw new RuntimeException(badCipher);
		} catch(InvalidAlgorithmParameterException badIv) {
			throw new RuntimeException(badIv);
		} catch(InvalidKeyException badKey) {
			throw new RuntimeException(badKey);
		}
	}

	public void finishPacket() throws IOException {
		try {
			out.write(packetCipher.doFinal());
		} catch(BadPaddingException badCipher) {
			throw new IOException(badCipher);
		} catch(IllegalBlockSizeException badCipher) {
			throw new RuntimeException(badCipher);
		}
	}

	@Override
	public void write(int b) throws IOException {
		byte[] buf = new byte[] {(byte) b};
		try {
			int i = packetCipher.update(buf, 0, buf.length, buf);
			assert i <= 1;
			if(i == 1) out.write(b);
		} catch(ShortBufferException badCipher) {
			throw new RuntimeException(badCipher);
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		try {
			int i = packetCipher.update(b, 0, b.length, b);
			assert i <= b.length;
			out.write(b, 0, i);
		} catch(ShortBufferException badCipher) {
			throw new RuntimeException(badCipher);
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		try {
			int i = packetCipher.update(b, off, len, b, off);
			assert i <= len;
			out.write(b, off, i);
		} catch(ShortBufferException badCipher) {
			throw new RuntimeException(badCipher);
		}
	}
}
