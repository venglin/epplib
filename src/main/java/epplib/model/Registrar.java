package epplib.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Registrar {
    private BigDecimal amountAvailable;
    private Integer hitPoints;
    private Integer maxHitPoints;
    private Map<String, Long> credits;

    public Registrar() {
        amountAvailable = null;
        hitPoints = null;
        maxHitPoints = null;
        credits = new HashMap<>();
    }

    @Override
    public String toString() {
        String res = "Registrar " +
                "amountAvailable=" + amountAvailable +
                ", hitPoints=" + hitPoints +
                ", maxHitPoints=" + maxHitPoints;
        if (null != credits)
            res += ", creditsSize=" + credits.size();
        return res;
    }

    public BigDecimal getAmountAvailable() {
        return amountAvailable;
    }

    public void setAmountAvailable(BigDecimal amountAvailable) {
        this.amountAvailable = amountAvailable;
    }

    public Integer getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(Integer hitPoints) {
        this.hitPoints = hitPoints;
    }

    public Integer getMaxHitPoints() {
        return maxHitPoints;
    }

    public void setMaxHitPoints(Integer maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }

    public Map<String, Long> getCredits() {
        return credits;
    }

    public void setCredits(Map<String, Long> credits) {
        this.credits = credits;
    }

    public void setCredit(String name, long value) {
        if (this.credits.containsKey(name))
            this.credits.remove(name);

        this.credits.put(name, value);
    }

    public Long getCredit(String name) {
        return this.credits.get(name);
    }
}
