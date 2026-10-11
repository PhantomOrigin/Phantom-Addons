package com.phantomaddons.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.phantomaddons.features.pv.PvCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.command.client.ClientCommandInternals", remap = false)
public abstract class ClientCommandInternalsMixin {

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void phantomaddons$routePv(CallbackInfoReturnable<Boolean> cir,
                                              @Local(argsOnly = true, ordinal = 0) String command) {
        if (PvCommand.intercept(command)) cir.setReturnValue(true);
    }
}
