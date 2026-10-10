package epplib.model;

import java.util.Locale;

/**
 * A DS (Delegation Signer) record of a domain — DNSSEC.
 *
 * The NASK registry implements only the DS Data Interface of RFC 5910, not the
 * Key Data Interface. The caller therefore supplies a digest of the key rather
 * than the public key itself: the four fields of {@code <secDNS:dsData>}.
 *
 * The digest is held as a hexadecimal string because that is the form a DNS
 * operator hands out and the form the registry returns. Converting it to the
 * bytes JAXB expects is a serialisation detail and has no reason to leak to
 * the caller.
 */
public class DsRecord {

	/** Key identifier, 1–65535. The schema forbids zero. */
	private final int keyTag;

	/** Signing algorithm, per the IANA DNSSEC registry. */
	private final short alg;

	/** Digest type: 1 = SHA-1, 2 = SHA-256, 4 = SHA-384. */
	private final short digestType;

	/** Key digest in hexadecimal. */
	private final String digest;

	public DsRecord(int keyTag, short alg, short digestType, String digest) {
		if (digest == null)
			throw new IllegalArgumentException("digest must not be null");
		this.keyTag = keyTag;
		this.alg = alg;
		this.digestType = digestType;
		this.digest = digest.trim().toUpperCase(Locale.ROOT);
	}

	public int getKeyTag() {
		return keyTag;
	}

	public short getAlg() {
		return alg;
	}

	public short getDigestType() {
		return digestType;
	}

	public String getDigest() {
		return digest;
	}

	/**
	 * The digest as bytes, in the form {@code xs:hexBinary} requires.
	 *
	 * An odd number of characters, or anything outside hexadecimal, describes a
	 * record the registry would reject anyway. Stopping it here with a clear
	 * message beats sending it and decoding the refusal.
	 */
	public byte[] getDigestBytes() {
		int len = digest.length();
		if (len == 0 || len % 2 != 0)
			throw new IllegalArgumentException("digest must have an even number of hex characters");

		byte[] out = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			int hi = Character.digit(digest.charAt(i), 16);
			int lo = Character.digit(digest.charAt(i + 1), 16);
			if (hi < 0 || lo < 0)
				throw new IllegalArgumentException("digest must be hexadecimal");
			out[i / 2] = (byte) ((hi << 4) | lo);
		}
		return out;
	}

	/** Rebuilds a record from the byte form the registry returns. */
	public static DsRecord fromBytes(int keyTag, short alg, short digestType, byte[] digest) {
		StringBuilder hex = new StringBuilder(digest == null ? 0 : digest.length * 2);
		if (digest != null)
			for (byte b : digest)
				hex.append(String.format("%02X", b));
		return new DsRecord(keyTag, alg, digestType, hex.toString());
	}

	@Override
	public String toString() {
		return "DsRecord[keyTag=" + keyTag + ", alg=" + alg + ", digestType=" + digestType
				+ ", digest=" + digest + "]";
	}
}
