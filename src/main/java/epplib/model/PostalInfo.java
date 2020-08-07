package epplib.model;

public class PostalInfo {
	private String name;
	private String org;
	private PostalAddress addr;

	public PostalInfo() {
		addr = new PostalAddress();
	}

	public PostalAddress getAddr() {
		return addr;
	}

	public String getName() {
		return name;
	}

	public String getOrg() {
		return org;
	}

	public void setAddr(PostalAddress addr) {
		this.addr = addr;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOrg(String org) {
		this.org = org;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostalInfo that = (PostalInfo) o;

        if (addr != null ? !addr.equals(that.addr) : that.addr != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (org != null ? !org.equals(that.org) : that.org != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (org != null ? org.hashCode() : 0);
        result = 31 * result + (addr != null ? addr.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "PostalInfo [name=" + name + ", org=" + org + ", addr=" + addr + "]";
	}
}
