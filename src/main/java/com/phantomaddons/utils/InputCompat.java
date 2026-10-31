package com.phantomaddons.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.phantomaddons.gui.PhantomScreen;
import net.minecraft.client.Minecraft;
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?} else {
/*import org.lwjgl.sdl.SDLMouse;
*///?}

public final class InputCompat {

    private InputCompat() {}

    public static boolean isKeyDown(int key) {
        //? if <26.3 {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        //?} else {
        /*return InputConstants.isKeyDown(key);
        *///?}
    }

    public static boolean isMouseDown(int button) {
        //? if <26.3 {
        long handle = Minecraft.getInstance().getWindow().handle();
        return handle != 0 && GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
        //?} else {
        /*if (button < 1 || button > 31) return false;
        int held = SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, (java.nio.FloatBuffer) null);
        return (held & (1 << (button - 1))) != 0;
        *///?}
    }

    public static boolean isBoundDown(int bound) {
        if (bound <= 0) return false;
        if (bound >= PhantomScreen.MOUSE_OFFSET) return isMouseDown(bound - PhantomScreen.MOUSE_OFFSET);
        return isKeyDown(bound);
    }

    public static boolean isCtrlDown() {
        return isKeyDown(InputConstants.KEY_LCONTROL) || isKeyDown(InputConstants.KEY_RCONTROL);
    }

    public static boolean isShiftDown() {
        return isKeyDown(InputConstants.KEY_LSHIFT) || isKeyDown(InputConstants.KEY_RSHIFT);
    }

    public static String keyName(int code) {
        try {
            //? if <26.3 {
            return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString().toUpperCase();
            //?} else {
            /*return InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName().getString().toUpperCase();
            *///?}
        } catch (RuntimeException e) {
            return "Key " + code;
        }
    }

    public static int mouseButtonNumber(int button) {
        //? if <26.3 {
        return button + 1;
        //?} else {
        /*return button;
        *///?}
    }
}
