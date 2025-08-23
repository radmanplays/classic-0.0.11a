package com.mojang.minecraft.level;

import com.mojang.minecraft.level.tile.Tile;
import java.util.Random;

public class LevelGen {
	private int width;
	private int height;
	private int depth;
	private Random random = new Random();

	public LevelGen(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public byte[] generateMap() {
		int w = this.width;
		int h = this.height;
		int d = this.depth;
		int[] heightmap1 = (new NoiseMap(0)).read(w, h);
		int[] heightmap2 = (new NoiseMap(0)).read(w, h);
		int[] cf = (new NoiseMap(1)).read(w, h);
		int[] rockMap = (new NoiseMap(1)).read(w, h);
		byte[] blocks = new byte[this.width * this.height * this.depth];

		int count;
		int i;
		int length;
		for(count = 0; count < w; ++count) {
			for(i = 0; i < d; ++i) {
				for(int x = 0; x < h; ++x) {
					int y = heightmap1[count + x * this.width];
					int z = heightmap2[count + x * this.width];
					length = cf[count + x * this.width];
					if(length < 128) {
						z = y;
					}

					int dir1 = y;
					if(z > y) {
						dir1 = z;
					}

					dir1 = dir1 / 8 + d / 3;
					int dira1 = rockMap[count + x * this.width] / 8 + d / 3;
					if(dira1 > dir1 - 2) {
						dira1 = dir1 - 2;
					}

					int dir2 = (i * this.height + x) * this.width + count;
					int dira2 = 0;
					if(i == dir1) {
						dira2 = Tile.grass.id;
					}

					if(i < dir1) {
						dira2 = Tile.dirt.id;
					}

					if(i <= dira1) {
						dira2 = Tile.rock.id;
					}

					blocks[dir2] = (byte)dira2;
				}
			}
		}

		count = w * h * d / 256 / 64;

		for(i = 0; i < count; ++i) {
			float var29 = this.random.nextFloat() * (float)w;
			float var30 = this.random.nextFloat() * (float)d;
			float var31 = this.random.nextFloat() * (float)h;
			length = (int)(this.random.nextFloat() + this.random.nextFloat() * 150.0F);
			float var32 = (float)((double)this.random.nextFloat() * Math.PI * 2.0D);
			float var33 = 0.0F;
			float var34 = (float)((double)this.random.nextFloat() * Math.PI * 2.0D);
			float var35 = 0.0F;

			for(int l = 0; l < length; ++l) {
				var29 = (float)((double)var29 + Math.sin((double)var32) * Math.cos((double)var34));
				var31 = (float)((double)var31 + Math.cos((double)var32) * Math.cos((double)var34));
				var30 = (float)((double)var30 + Math.sin((double)var34));
				var32 += var33 * 0.2F;
				var33 *= 0.9F;
				var33 += this.random.nextFloat() - this.random.nextFloat();
				var34 += var35 * 0.5F;
				var34 *= 0.5F;
				var35 *= 0.9F;
				var35 += this.random.nextFloat() - this.random.nextFloat();
				float size = (float)(Math.sin((double)l * Math.PI / (double)length) * 2.5D + 1.0D);

				for(int xx = (int)(var29 - size); xx <= (int)(var29 + size); ++xx) {
					for(int yy = (int)(var30 - size); yy <= (int)(var30 + size); ++yy) {
						for(int zz = (int)(var31 - size); zz <= (int)(var31 + size); ++zz) {
							float xd = (float)xx - var29;
							float yd = (float)yy - var30;
							float zd = (float)zz - var31;
							float dd = xd * xd + yd * yd * 2.0F + zd * zd;
							if(dd < size * size && xx >= 1 && yy >= 1 && zz >= 1 && xx < this.width - 1 && yy < this.depth - 1 && zz < this.height - 1) {
								int ii = (yy * this.height + zz) * this.width + xx;
								if(blocks[ii] == Tile.rock.id) {
									blocks[ii] = 0;
								}
							}
						}
					}
				}
			}
		}

		return blocks;
	}
}
