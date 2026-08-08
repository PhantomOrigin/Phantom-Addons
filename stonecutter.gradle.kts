plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2.x"

// Canonical (checked-in) source is written for 26.2.x. These are pure textual renames with no
// structural difference between versions, so a blanket search-and-replace on 26.1.x output is
// simpler than littering the source with //? if comments at every call site. Anything with a
// genuinely different shape (different arg counts, different surrounding logic) is handled with
// explicit //? if blocks in the affected files instead.
stonecutter parameters {
    replacements {
        string(current.parsed < "26.2") {
            replace("net.minecraft.world.entity.monster.cubemob.Slime", "net.minecraft.world.entity.monster.Slime")
            // Kuudra is rendered client-side as a giant MagmaCube. Pre-26.2, MagmaCube extends
            // Slime, so `instanceof Slime` matched it. In 26.2, Slime and MagmaCube became sibling
            // subclasses of AbstractCubeMob (neither extends the other), so Kuudra detection has to
            // target AbstractCubeMob there instead — replaced back to the old Slime-based check here
            // since that's what correctly matches MagmaCube on the pre-26.2 hierarchy. The import
            // itself is handled by an explicit //? if block in each file (not here), since a blanket
            // FQN replacement would overlap/conflict with this bare-name replacement.
            replace("AbstractCubeMob", "Slime")
            replace(".gui.screen()", ".screen")
            replace(".gui.setScreen(", ".setScreen(")
            replace("mc.gui.hud.isHidden()", "mc.options.hideGui")
            // ItemInHandRenderer.submitArmWithItem was renderArmWithItem pre-26.2 — the
            // MultiBufferSource->SubmitNodeCollector param swap doesn't affect this mixin at all
            // since it only ever captures PoseStack/float/ItemStack locals by ordinal, and those
            // ordinals are unchanged between the two signatures, so the method name is the only
            // thing that needs to differ.
            replace("\"submitArmWithItem\"", "\"renderArmWithItem\"")
        }
    }
}
