package com.mojang.minecraft.renderer;

import org.lwjgl.opengl.GL11;

public class Tesselator {
    private float u, v;
    private float r, g, b;
    private boolean hasColor = false;
    private boolean hasTexture = false;
    private boolean drawing = false;
	public static Tesselator instance = new Tesselator();
	private boolean noColor = false;

    public void init() {
        if (drawing) {
            flush();
        }
        hasColor = false;
        hasTexture = false;
        drawing = true;

        GL11.glBegin(GL11.GL_QUADS);
    }

    public void tex(float u, float v) {
        this.u = u;
        this.v = v;
        this.hasTexture = true;
        GL11.glTexCoord2f(u, v);
    }

    public void color(float r, float g, float b) {
	        this.r = r;
	        this.g = g;
	        this.b = b;
	        this.hasColor = true;
	        GL11.glColor3f(r, g, b);
    }
    
	public void vertexUV(float x, float y, float z, float u, float v) {
		this.tex(u, v);
		this.vertex(x, y, z);
	}

    public void vertex(float x, float y, float z) {
        if (hasColor) {
            GL11.glColor3f(r, g, b);
        }

        if (hasTexture) {
            GL11.glTexCoord2f(u, v);
        }

        GL11.glVertex3f(x, y, z);

        hasColor = false;
        hasTexture = false;
    }

    public void flush() {
        if (!drawing) return;
        GL11.glEnd();
        drawing = false;
        hasColor = false;
        hasTexture = false;
    }
    
	public void color(int c) {
		float r = (float)(c >> 16 & 255) / 255.0F;
		float g = (float)(c >> 8 & 255) / 255.0F;
		float b = (float)(c & 255) / 255.0F;
		this.color(r, g, b);
	}
}