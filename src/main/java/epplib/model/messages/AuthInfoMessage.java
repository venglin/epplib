package epplib.model.messages;

public class AuthInfoMessage extends DomainMessage {
	private String authInfo;

	public AuthInfoMessage() {
		super();
	}

	public String getAuthInfo() {
		return authInfo;
	}

	public void setAuthInfo(String authInfo) {
		this.authInfo = authInfo;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthInfoMessage that = (AuthInfoMessage) o;

        if (authInfo != null ? !authInfo.equals(that.authInfo) : that.authInfo != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return authInfo != null ? authInfo.hashCode() : 0;
    }

    @Override
	public String toString() {
		return "AuthInfoMessage [authInfo=" + authInfo + ", name=" + name + ", message=" + message + "]";
	}
}
