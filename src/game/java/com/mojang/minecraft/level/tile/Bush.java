package com.mojang.minecraft.level.tile;

import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.renderer.Tesselator;

import java.util.Random;

import org.lwjgl.opengl.GL11;

public class Bush extends Tile {
	protected Bush(int id) {
		super(id);
		this.tex = 15;
	}

	public void tick(Level level, int x, int y, int z, Random random) {
		int below = level.getTile(x, y - 1, z);
		if(!level.isLit(x, y, z) || below != Tile.dirt.id && below != Tile.grass.id) {
			level.setTile(x, y, z, 0);
		}

	}

	public void render(Tesselator t, Level level, int layer, int x, int y, int z) {
		if(!(level.isLit(x, y, z) ^ layer != 1)) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			int tex = this.getTexture(15);
			float u0 = (float)(tex % 16) / 16.0F;
			float u1 = u0 + 0.999F / 16.0F;
			float v0 = (float)(tex / 16) / 16.0F;
			float v1 = v0 + 0.999F / 16.0F;
			byte rots = 2;
			t.color(1.0F, 1.0F, 1.0F);

			for(int r = 0; r < rots; ++r) {
				float xa = (float)(Math.sin((double)r * Math.PI / (double)rots + Math.PI * 0.25D) * 0.5D);
				float za = (float)(Math.cos((double)r * Math.PI / (double)rots + Math.PI * 0.25D) * 0.5D);
				float x0 = (float)x + 0.5F - xa;
				float x1 = (float)x + 0.5F + xa;
				float y0 = (float)y + 0.0F;
				float y1 = (float)y + 1.0F;
				float z0 = (float)z + 0.5F - za;
				float z1 = (float)z + 0.5F + za;
				t.vertexUV(x0, y1, z0, u1, v0);
				t.vertexUV(x1, y1, z1, u0, v0);
				t.vertexUV(x1, y0, z1, u0, v1);
				t.vertexUV(x0, y0, z0, u1, v1);
				t.vertexUV(x1, y1, z1, u0, v0);
				t.vertexUV(x0, y1, z0, u1, v0);
				t.vertexUV(x0, y0, z0, u1, v1);
				t.vertexUV(x1, y0, z1, u0, v1);
			}

		}
	}

	public AABB getAABB(int x, int y, int z) {
		return null;
	}

	public boolean blocksLight() {
		return false;
	}

	public boolean isSolid() {
		return false;
	}
}
