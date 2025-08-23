package com.mojang.minecraft.level.tile;

import java.util.Random;

import com.mojang.minecraft.level.Level;

public class GrassTile extends Tile {
	protected GrassTile(int id) {
		super(id);
		this.tex = 3;
	}

	protected int getTexture(int face) {
		return face == 1 ? 0 : (face == 0 ? 2 : 3);
	}

	public void tick(Level level, int x, int y, int z, Random random) {
		if(!level.isLit(x, y, z)) {
			level.setTile(x, y, z, Tile.dirt.id);
		} else {
			for(int i = 0; i < 4; ++i) {
				int xt = x + random.nextInt(3) - 1;
				int yt = y + random.nextInt(5) - 3;
				int zt = z + random.nextInt(3) - 1;
				if(level.getTile(xt, yt, zt) == Tile.dirt.id && level.isLit(xt, yt, zt)) {
					level.setTile(xt, yt, zt, Tile.grass.id);
				}
			}
		}

	}
}
