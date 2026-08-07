package com.phantomaddons.utils;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.phantomaddons.PhantomAddons;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

// Several highlight/waypoint renderers draw with depth testing forced to ALWAYS (via
// GL11.glDepthFunc) so they're visible through walls/terrain. Raw OpenGL calls stop functioning
// once a version adds a Vulkan-capable backend (26.2+) — the correct replacement is a RenderPipeline
// whose own DepthStencilState uses CompareOp.ALWAYS_PASS. The catch: turning a RenderPipeline into
// something MultiBufferSource.getBuffer(...) accepts requires RenderType.create(String, RenderSetup),
// which Mojang left package-private — not part of the public modding API, and never guaranteed to
// keep this exact shape. This reflects into that one method to build always-on-top variants of the
// two RenderTypes these features actually use, cloning every other property of the original
// RenderSetup so nothing about how they draw changes besides the depth test.
//
// If the reflection ever breaks (a future Minecraft version restructures these classes), every
// method here falls back to the normal, depth-tested RenderType instead of throwing — the affected
// features simply stop rendering through walls rather than the mod breaking.
public final class AlwaysOnTopRenderTypes {

    private static volatile RenderType linesAlwaysOnTop;
    private static volatile RenderType debugQuadsAlwaysOnTop;
    private static volatile boolean reflectionFailed = false;

    private AlwaysOnTopRenderTypes() {}

    public static RenderType lines() {
        if (reflectionFailed) return RenderTypes.lines();
        RenderType cached = linesAlwaysOnTop;
        if (cached != null) return cached;
        synchronized (AlwaysOnTopRenderTypes.class) {
            if (linesAlwaysOnTop == null) {
                linesAlwaysOnTop = build(RenderTypes.lines(),
                        Identifier.fromNamespaceAndPath("phantomaddons", "lines_always_on_top"));
            }
            return linesAlwaysOnTop != null ? linesAlwaysOnTop : RenderTypes.lines();
        }
    }

    public static RenderType debugQuads() {
        if (reflectionFailed) return RenderTypes.debugQuads();
        RenderType cached = debugQuadsAlwaysOnTop;
        if (cached != null) return cached;
        synchronized (AlwaysOnTopRenderTypes.class) {
            if (debugQuadsAlwaysOnTop == null) {
                debugQuadsAlwaysOnTop = build(RenderTypes.debugQuads(),
                        Identifier.fromNamespaceAndPath("phantomaddons", "debug_quads_always_on_top"));
            }
            return debugQuadsAlwaysOnTop != null ? debugQuadsAlwaysOnTop : RenderTypes.debugQuads();
        }
    }

    private static RenderType build(RenderType base, Identifier newLocation) {
        try {
            RenderPipeline basePipeline = base.pipeline();
            RenderPipeline alwaysOnTopPipeline = withAlwaysDepth(basePipeline, newLocation);

            Object baseSetup = readField(base, "state");
            Object newSetup = cloneSetupWithPipeline(baseSetup, alwaysOnTopPipeline);

            Method create = RenderType.class.getDeclaredMethod("create", String.class,
                    Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup"));
            create.setAccessible(true);
            RenderType result = (RenderType) create.invoke(null, newLocation.toString(), newSetup);

            PhantomAddons.LOGGER.info("[AlwaysOnTopRenderTypes] Built always-on-top variant of {}", newLocation);
            return result;
        } catch (Throwable t) {
            reflectionFailed = true;
            PhantomAddons.LOGGER.warn(
                    "[AlwaysOnTopRenderTypes] Could not build always-on-top render type — " +
                    "falling back to normal depth-tested rendering for this and any other always-on-top " +
                    "render type. This only affects whether highlights draw through walls; nothing else " +
                    "is impacted. Cause: {}", String.valueOf(t));
            return null;
        }
    }

    private static RenderPipeline withAlwaysDepth(RenderPipeline base, Identifier newLocation) {
        DepthStencilState original = base.getDepthStencilState();
        DepthStencilState alwaysOn = new DepthStencilState(CompareOp.ALWAYS_PASS, original.writeDepth());

        com.mojang.blaze3d.pipeline.ColorTargetState[] colorTargets = base.getColorTargetStates();

        RenderPipeline.Snippet snippet = new RenderPipeline.Snippet(
                Optional.of(base.getVertexShader()),
                Optional.of(base.getFragmentShader()),
                Optional.of(base.getShaderDefines()),
                Optional.of(base.getBindGroupLayouts()),
                colorTargets,
                colorTargets.length,
                Optional.of(alwaysOn),
                Optional.of(base.getPolygonMode()),
                Optional.of(base.isCull()),
                base.getVertexFormatBindings(),
                Optional.of(base.getPrimitiveTopology()));

        return RenderPipeline.builder(snippet).withLocation(newLocation).build();
    }

    private static Object cloneSetupWithPipeline(Object baseSetup, RenderPipeline newPipeline) throws Exception {
        Class<?> renderSetupClass = baseSetup.getClass();
        Method builderMethod = renderSetupClass.getMethod("builder", RenderPipeline.class);
        Object setupBuilder = builderMethod.invoke(null, newPipeline);
        Class<?> builderClass = setupBuilder.getClass();

        if ((boolean) readField(baseSetup, "useLightmap")) {
            builderClass.getMethod("useLightmap").invoke(setupBuilder);
        }
        if ((boolean) readField(baseSetup, "useOverlay")) {
            builderClass.getMethod("useOverlay").invoke(setupBuilder);
        }
        if ((boolean) readField(baseSetup, "affectsCrumbling")) {
            builderClass.getMethod("affectsCrumbling").invoke(setupBuilder);
        }
        if ((boolean) readField(baseSetup, "sortOnUpload")) {
            builderClass.getMethod("sortOnUpload").invoke(setupBuilder);
        }

        Object layeringTransform = readField(baseSetup, "layeringTransform");
        builderClass.getMethod("setLayeringTransform", Class.forName("net.minecraft.client.renderer.rendertype.LayeringTransform"))
                .invoke(setupBuilder, layeringTransform);

        Object outputTarget = readField(baseSetup, "outputTarget");
        builderClass.getMethod("setOutputTarget", Class.forName("net.minecraft.client.renderer.rendertype.OutputTarget"))
                .invoke(setupBuilder, outputTarget);

        Object textureTransform = readField(baseSetup, "textureTransform");
        builderClass.getMethod("setTextureTransform", Class.forName("net.minecraft.client.renderer.rendertype.TextureTransform"))
                .invoke(setupBuilder, textureTransform);

        Object outlineProperty = readField(baseSetup, "outlineProperty");
        builderClass.getMethod("setOutline", Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup$OutlineProperty"))
                .invoke(setupBuilder, outlineProperty);

        @SuppressWarnings("unchecked")
        Map<String, ?> textures = (Map<String, ?>) readField(baseSetup, "textures");
        if (!textures.isEmpty()) {
            // lines()/debugQuads() are untextured in every version this was written against — if a
            // future version adds textures to them, bail out to the safe fallback rather than risk
            // silently dropping texture bindings.
            throw new IllegalStateException("Unexpected textures on base RenderSetup: " + textures.keySet());
        }

        return builderClass.getMethod("createRenderSetup").invoke(setupBuilder);
    }

    private static Object readField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
