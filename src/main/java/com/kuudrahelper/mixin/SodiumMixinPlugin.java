package com.kuudrahelper.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SodiumMixinPlugin implements IMixinConfigPlugin {

    private static final boolean SODIUM_LOADED =
            FabricLoader.getInstance().isModLoaded("sodium");
    private static final boolean IRIS_LOADED =
            FabricLoader.getInstance().isModLoaded("iris");

    @Override
    public void onLoad(String mixinPackage) {
        if (!SODIUM_LOADED) {
            System.out.println("[KuudraHelper] Sodium not found — Sodium fluid mixins will be skipped.");
        }
        if (IRIS_LOADED) {
            System.out.println("[KuudraHelper] Iris detected — fluid vertex color mixin will operate in Iris-compatible mode.");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!SODIUM_LOADED) return false;

        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}