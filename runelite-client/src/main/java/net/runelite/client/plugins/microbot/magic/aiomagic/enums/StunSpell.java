package net.runelite.client.plugins.microbot.magic.aiomagic.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

@Getter
@RequiredArgsConstructor
public enum StunSpell {
    CONFUSE(MagicAction.CONFUSE),
    WEAKEN(MagicAction.WEAKEN),
    CURSE(MagicAction.CURSE),
    VULNERABILITY(MagicAction.VULNERABILITY),
    ENFEEBLE(MagicAction.ENFEEBLE),
    STUN(MagicAction.STUN);

    private final MagicAction spell;

    @Override
    public String toString() {
        return spell != null ? spell.getName() : "None";
    }
}
