package com.phantomaddons.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

public final class RecordedVertices implements VertexConsumer {

    private static final int COLOR = 1, UV = 2, UV1 = 4, UV2 = 8, UV3 = 16, NORMAL = 32, LINE_WIDTH = 64;

    private static final class Vertex {
        float x, y, z;
        int flags;
        int r, g, b, a;
        float u, v, u3, v3;
        int u1, v1, u2, v2;
        float nx, ny, nz;
        float lineWidth;
    }

    private final List<Vertex> vertices = new ArrayList<>();
    private Vertex current;

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        current = new Vertex();
        current.x = x; current.y = y; current.z = z;
        vertices.add(current);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        current.flags |= COLOR;
        current.r = r; current.g = g; current.b = b; current.a = a;
        return this;
    }

    @Override
    public VertexConsumer setColor(int argb) {
        return setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        current.flags |= UV;
        current.u = u; current.v = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        current.flags |= UV1;
        current.u1 = u; current.v1 = v;
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        current.flags |= UV2;
        current.u2 = u; current.v2 = v;
        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        current.flags |= UV3;
        current.u3 = u; current.v3 = v;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        current.flags |= NORMAL;
        current.nx = x; current.ny = y; current.nz = z;
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        current.flags |= LINE_WIDTH;
        current.lineWidth = width;
        return this;
    }

    public void replayInto(VertexConsumer out) {
        for (Vertex v : vertices) {
            out.addVertex(v.x, v.y, v.z);
            if ((v.flags & COLOR) != 0) out.setColor(v.r, v.g, v.b, v.a);
            if ((v.flags & UV) != 0) out.setUv(v.u, v.v);
            if ((v.flags & UV1) != 0) out.setUv1(v.u1, v.v1);
            if ((v.flags & UV2) != 0) out.setUv2(v.u2, v.v2);
            if ((v.flags & UV3) != 0) out.setUv3(v.u3, v.v3);
            if ((v.flags & NORMAL) != 0) out.setNormal(v.nx, v.ny, v.nz);
            if ((v.flags & LINE_WIDTH) != 0) out.setLineWidth(v.lineWidth);
        }
    }
}
