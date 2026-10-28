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

    private static RenderPipeline withAlwaysDepth(RenderPipeline base, Identifier newLocation) throws Exception {
        DepthStencilState alwaysOn = new DepthStencilState(CompareOp.ALWAYS_PASS, false);

        //? if <26.3 {
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
        //?} else {
        /*java.lang.reflect.Constructor<?> ctor = RenderPipeline.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Class<?>[] types = ctor.getParameterTypes();

        Object colorTargets = toArray(readField(base, "colorTargetStates"), types[4].getComponentType());
        Object vertexFormats = toArray(readField(base, "vertexFormatPerBuffer"), types[8].getComponentType());

        return (RenderPipeline) ctor.newInstance(
                newLocation,
                readField(base, "shaders"),
                readField(base, "shaderDefines"),
                readField(base, "bindGroupLayouts"),
                colorTargets,
                alwaysOn,
                readField(base, "polygonMode"),
                readField(base, "cull"),
                vertexFormats,
                readField(base, "primitiveTopology"),
                readField(base, "pushConstantSize"),
                readField(base, "sortKey"));
        *///?}
    }

    //? if >=26.3 {
    /*private static Object toArray(Object list, Class<?> component) {
        java.util.List<?> items = (java.util.List<?>) list;
        Object array = java.lang.reflect.Array.newInstance(component, items.size());
        for (int i = 0; i < items.size(); i++) java.lang.reflect.Array.set(array, i, items.get(i));
        return array;
    }
    *///?}

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

        if (hasField(baseSetup, "outputTarget")) {
            Object outputTarget = readField(baseSetup, "outputTarget");
            builderClass.getMethod("setOutputTarget", Class.forName("net.minecraft.client.renderer.rendertype.OutputTarget"))
                    .invoke(setupBuilder, outputTarget);
        }
        if (hasField(baseSetup, "oitPipelineSet") && readField(baseSetup, "oitPipelineSet") != null) {
            builderClass.getMethod("setOitPipelines", Class.forName("net.minecraft.client.renderer.oit.OitPipelineSet"))
                    .invoke(setupBuilder, readField(baseSetup, "oitPipelineSet"));
        }
        if (hasField(baseSetup, "forceSolidModelPhase") && (boolean) readField(baseSetup, "forceSolidModelPhase")) {
            builderClass.getMethod("withForcedSolidModelPhase").invoke(setupBuilder);
        }

        Object textureTransform = readField(baseSetup, "textureTransform");
        builderClass.getMethod("setTextureTransform", Class.forName("net.minecraft.client.renderer.rendertype.TextureTransform"))
                .invoke(setupBuilder, textureTransform);

        Object outlineProperty = readField(baseSetup, "outlineProperty");
        builderClass.getMethod("setOutline", Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup$OutlineProperty"))
                .invoke(setupBuilder, outlineProperty);

        @SuppressWarnings("unchecked")
        Map<String, ?> textures = (Map<String, ?>) readField(baseSetup, "textures");
        if (!textures.isEmpty()) {
            throw new IllegalStateException("Unexpected textures on base RenderSetup: " + textures.keySet());
        }

        return builderClass.getMethod("createRenderSetup").invoke(setupBuilder);
    }

    private static boolean hasField(Object target, String name) {
        try {
            target.getClass().getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static Object readField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
