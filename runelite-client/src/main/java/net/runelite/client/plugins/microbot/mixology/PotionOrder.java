package net.runelite.client.plugins.microbot.mixology;

import lombok.Setter;

public class PotionOrder {
    private final int idx;
    private final PotionType potionType;
    private final PotionModifier potionModifier;
    @Setter
    private boolean fulfilled;

    public PotionOrder(int idx, PotionType potionType, PotionModifier potionModifier) {
        this.idx = idx;
        this.potionType = potionType;
        this.potionModifier = potionModifier;
    }

    public int idx() {
        return this.idx;
    }

    public PotionType potionType() {
        return this.potionType;
    }

    public PotionModifier potionModifier() {
        return this.potionModifier;
    }

    public boolean fulfilled() {
        return this.fulfilled;
    }

    public String toString() {
        return "PotionOrder{idx=" + this.idx + ", potionType=" + this.potionType + ", potionModifier=" + this.potionModifier + "}";
    }
}