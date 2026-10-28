plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2.x"

stonecutter parameters {
    if (current.parsed >= "26.3") replacements {
        string(true) {
            replace(".mulPose(", ".rotate(")
            replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
            replace("com.mojang.blaze3d.pipeline.DepthStencilState", "com.mojang.renderpearl.api.pipeline.DepthStencilState")
            replace("com.mojang.blaze3d.pipeline.ColorTargetState", "com.mojang.renderpearl.api.pipeline.ColorTargetState")
            replace("com.mojang.blaze3d.platform.CompareOp", "com.mojang.renderpearl.api.pipeline.CompareOp")
        }
    }

    if (current.parsed < "26.2") replacements {
        string(true) {
            replace("net.minecraft.world.entity.monster.cubemob.Slime", "net.minecraft.world.entity.monster.Slime")
            replace("AbstractCubeMob", "Slime")
            replace(".gui.screen()", ".screen")
            replace(".gui.setScreen(", ".setScreen(")
            replace("mc.gui.hud.isHidden()", "mc.options.hideGui")
            replace("\"submitArmWithItem\"", "\"renderArmWithItem\"")
        }
    }
}
