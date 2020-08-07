package epplib.model;

import java.util.List;

/**
 * Created by trou on 3/31/14.
 */
public class Keygroup {
    private String name;
    private List<KeyData> keyDataList;
    private String reason;
    private Boolean isAvailable;

    public Keygroup() {
    }

    public Keygroup(String name, List<KeyData> keyDataList) {
        this.name = name;
        this.keyDataList = keyDataList;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<KeyData> getKeyDataList() {
        return keyDataList;
    }

    public void setKeyDataList(List<KeyData> keyDataList) {
        this.keyDataList = keyDataList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
