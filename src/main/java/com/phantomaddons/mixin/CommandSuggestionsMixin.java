package com.phantomaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.phantomaddons.utils.CommandCase;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow @org.spongepowered.asm.mixin.Final EditBox input;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "updateCommandInfo",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;",
                    remap = false),
            require = 0)
    private ParseResults phantomaddons$parseCaseFixed(CommandDispatcher dispatcher, StringReader reader, Object source,
                                                      Operation<ParseResults> original) {
        String text = reader.getString();
        String fixed = CommandCase.fixForDisplay(text, dispatcher.getRoot());
        if (fixed.equals(text)) return original.call(dispatcher, reader, source);

        StringReader fixedReader = new StringReader(fixed);
        fixedReader.setCursor(reader.getCursor());
        return original.call(dispatcher, fixedReader, source);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "updateCommandInfo",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;",
                    remap = false),
            require = 0)
    private CompletableFuture phantomaddons$keepTypedCase(CommandDispatcher dispatcher, ParseResults parse, int cursor,
                                                          Operation<CompletableFuture> original) {
        String typed = input.getValue();
        CompletableFuture<Suggestions> future = original.call(dispatcher, parse, cursor);
        return future.thenApply(suggestions -> CommandCase.keepTypedCase(typed, suggestions, dispatcher.getRoot()));
    }
}
