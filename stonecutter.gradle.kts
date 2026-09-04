plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2.x"

stonecutter parameters {
    replacements {
        string(current.parsed < "26.2") {
            replace("net.minecraft.world.entity.monster.cubemob.Slime", "net.minecraft.world.entity.monster.Slime")
            replace("AbstractCubeMob", "Slime")
            replace(".gui.screen()", ".screen")
            replace(".gui.setScreen(", ".setScreen(")
            replace("mc.gui.hud.isHidden()", "mc.options.hideGui")
            replace("\"submitArmWithItem\"", "\"renderArmWithItem\"")
        }
    }
}
