package com.mojang.minecraft.level;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.phys.AABB;

public class Level {
	private static final int TILE_UPDATE_INTERVAL = 400;
	public final int width;
	public final int height;
	public final int depth;
	private byte[] blocks;
	private int[] lightDepths;
	private ArrayList<LevelListener> levelListeners = new ArrayList();
	private Random random = new Random();
	int unprocessed = 0;

	public Level(int w, int h, int d) {
		this.width = w;
		this.height = h;
		this.depth = d;
		this.blocks = new byte[w * h * d];
		this.lightDepths = new int[w * h];
		boolean mapLoaded = this.load();
		if(!mapLoaded) {
			this.generateMap();
		}

		this.calcLightDepths(0, 0, w, h);
	}

	private void generateMap() {
		int w = this.width;
		int h = this.height;
		int d = this.depth;
		int[] heightmap1 = (new PerlinNoiseFilter(0)).read(w, h);
		int[] heightmap2 = (new PerlinNoiseFilter(0)).read(w, h);
		int[] cf = (new PerlinNoiseFilter(1)).read(w, h);
		int[] rockMap = (new PerlinNoiseFilter(1)).read(w, h);

		for(int x = 0; x < w; ++x) {
			for(int y = 0; y < d; ++y) {
				for(int z = 0; z < h; ++z) {
					int dh1 = heightmap1[x + z * this.width];
					int dh2 = heightmap2[x + z * this.width];
					int cfh = cf[x + z * this.width];
					if(cfh < 128) {
						dh2 = dh1;
					}

					int dh = dh1;
					if(dh2 > dh1) {
						dh = dh2;
					}

					dh = dh / 8 + d / 3;
					int rh = rockMap[x + z * this.width] / 8 + d / 3;
					if(rh > dh - 2) {
						rh = dh - 2;
					}

					int i = (y * this.height + z) * this.width + x;
					int id = 0;
					if(y == dh) {
						id = Tile.grass.id;
					}

					if(y < dh) {
						id = Tile.dirt.id;
					}

					if(y <= rh) {
						id = Tile.rock.id;
					}

					this.blocks[i] = (byte)id;
				}
			}
		}

	}

	public boolean load() {
		try {
			VFile2 file = new VFile2("level.dat");
			if (!file.exists()) {
				return false;
			}
			DataInputStream e = new DataInputStream(new GZIPInputStream(file.getInputStream()));
			e.readFully(this.blocks);
			this.calcLightDepths(0, 0, this.width, this.height);

			for(int i = 0; i < this.levelListeners.size(); ++i) {
				((LevelListener)this.levelListeners.get(i)).allChanged();
			}

			e.close();
			return true;
		} catch (Exception var3) {
			var3.printStackTrace();
			return false;
		}
	}

	public void save() {
		try {
			VFile2 file = new VFile2("level.dat");
			DataOutputStream e = new DataOutputStream(new GZIPOutputStream(file.getOutputStream()));
			e.write(this.blocks);
			e.close();
		} catch (Exception var2) {
			var2.printStackTrace();
		}

	}

	public void calcLightDepths(int x0, int y0, int x1, int y1) {
		for(int x = x0; x < x0 + x1; ++x) {
			for(int z = y0; z < y0 + y1; ++z) {
				int oldDepth = this.lightDepths[x + z * this.width];

				int y;
				for(y = this.depth - 1; y > 0 && !this.isLightBlocker(x, y, z); --y) {
				}

				this.lightDepths[x + z * this.width] = y;
				if(oldDepth != y) {
					int yl0 = oldDepth < y ? oldDepth : y;
					int yl1 = oldDepth > y ? oldDepth : y;

					for(int i = 0; i < this.levelListeners.size(); ++i) {
						((LevelListener)this.levelListeners.get(i)).lightColumnChanged(x, z, yl0, yl1);
					}
				}
			}
		}

	}

	public void addListener(LevelListener levelListener) {
		this.levelListeners.add(levelListener);
	}

	public void removeListener(LevelListener levelListener) {
		this.levelListeners.remove(levelListener);
	}

	public boolean isLightBlocker(int x, int y, int z) {
		Tile tile = Tile.tiles[this.getTile(x, y, z)];
		return tile == null ? false : tile.blocksLight();
	}

	public ArrayList<AABB> getCubes(AABB aABB) {
		ArrayList aABBs = new ArrayList();
		int x0 = (int)aABB.x0;
		int x1 = (int)(aABB.x1 + 1.0F);
		int y0 = (int)aABB.y0;
		int y1 = (int)(aABB.y1 + 1.0F);
		int z0 = (int)aABB.z0;
		int z1 = (int)(aABB.z1 + 1.0F);
		if(x0 < 0) {
			x0 = 0;
		}

		if(y0 < 0) {
			y0 = 0;
		}

		if(z0 < 0) {
			z0 = 0;
		}

		if(x1 > this.width) {
			x1 = this.width;
		}

		if(y1 > this.depth) {
			y1 = this.depth;
		}

		if(z1 > this.height) {
			z1 = this.height;
		}

		for(int x = x0; x < x1; ++x) {
			for(int y = y0; y < y1; ++y) {
				for(int z = z0; z < z1; ++z) {
					Tile tile = Tile.tiles[this.getTile(x, y, z)];
					if(tile != null) {
						AABB aabb = tile.getAABB(x, y, z);
						if(aabb != null) {
							aABBs.add(aabb);
						}
					}
				}
			}
		}

		return aABBs;
	}

	public boolean setTile(int x, int y, int z, int type) {
		if(x >= 0 && y >= 0 && z >= 0 && x < this.width && y < this.depth && z < this.height) {
			if(type == this.blocks[(y * this.height + z) * this.width + x]) {
				return false;
			} else {
				this.blocks[(y * this.height + z) * this.width + x] = (byte)type;
				this.calcLightDepths(x, z, 1, 1);

				for(int i = 0; i < this.levelListeners.size(); ++i) {
					((LevelListener)this.levelListeners.get(i)).tileChanged(x, y, z);
				}

				return true;
			}
		} else {
			return false;
		}
	}

	public boolean isLit(int x, int y, int z) {
		return x >= 0 && y >= 0 && z >= 0 && x < this.width && y < this.depth && z < this.height ? y >= this.lightDepths[x + z * this.width] : true;
	}

	public int getTile(int x, int y, int z) {
		return x >= 0 && y >= 0 && z >= 0 && x < this.width && y < this.depth && z < this.height ? this.blocks[(y * this.height + z) * this.width + x] : 0;
	}

	public boolean isSolidTile(int x, int y, int z) {
		Tile tile = Tile.tiles[this.getTile(x, y, z)];
		return tile == null ? false : tile.isSolid();
	}

	public void tick() {
		this.unprocessed += this.width * this.height * this.depth;
		int ticks = this.unprocessed / 400;
		this.unprocessed -= ticks * 400;

		for(int i = 0; i < ticks; ++i) {
			int x = this.random.nextInt(this.width);
			int y = this.random.nextInt(this.depth);
			int z = this.random.nextInt(this.height);
			Tile tile = Tile.tiles[this.getTile(x, y, z)];
			if(tile != null) {
				tile.tick(this, x, y, z, this.random);
			}
		}

	}
}
