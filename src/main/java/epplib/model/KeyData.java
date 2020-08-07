package epplib.model;

/**
 * Created by trou on 3/31/14.
 */
public class KeyData {
    private Integer flags;
    private Short protocol;
    private Short alg;
    private byte[] pubKey;

    public KeyData() {
    }

    public Integer getFlags() {

        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public Short getProtocol() {
        return protocol;
    }

    public void setProtocol(Short protocol) {
        this.protocol = protocol;
    }

    public Short getAlg() {
        return alg;
    }

    public void setAlg(Short alg) {
        this.alg = alg;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }
}
