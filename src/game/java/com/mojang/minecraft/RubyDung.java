package com.mojang.minecraft;

import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Chunk;
import com.mojang.minecraft.level.Frustum;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelRenderer;
import com.mojang.minecraft.level.Tesselator;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.util.GLAllocation;
import net.lax1dude.eaglercraft.EagRuntime;

import java.io.IOException;
import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import net.lax1dude.eaglercraft.internal.EnumPlatformType;
import net.lax1dude.eaglercraft.internal.buffer.FloatBuffer;
import net.lax1dude.eaglercraft.internal.buffer.IntBuffer;

public class RubyDung implements Runnable {
	private static final boolean FULLSCREEN_MODE = false;
	private int width;
	private int height;
	private int lastWidth;
	private int lastHeight;
	private FloatBuffer fogColor0 = GLAllocation.createFloatBuffer(4);
	private FloatBuffer fogColor1 = GLAllocation.createFloatBuffer(4);
	private Timer timer = new Timer(20.0F);
	private Level level;
	private LevelRenderer levelRenderer;
	private Player player;
	private int paintTexture = 1;
	private ParticleEngine particleEngine;
    private ArrayList<Zombie> zombies = new ArrayList();
	private IntBuffer viewportBuffer = GLAllocation.createIntBuffer(16);
	private IntBuffer selectBuffer = GLAllocation.createIntBuffer(2000);
	private HitResult hitResult = null;
	FloatBuffer lb = GLAllocation.createFloatBuffer(16);

	public void init() throws LWJGLException, IOException {
		int col0 = 16710650;
		int col1 = 920330;
		float fr = 0.5F;
		float fg = 0.8F;
		float fb = 1.0F;
		this.fogColor0.put(new float[]{(float)(col0 >> 16 & 255) / 255.0F, (float)(col0 >> 8 & 255) / 255.0F, (float)(col0 & 255) / 255.0F, 1.0F}).flip();
		this.fogColor1.put(new float[]{(float)(col1 >> 16 & 255) / 255.0F, (float)(col1 >> 8 & 255) / 255.0F, (float)(col1 & 255) / 255.0F, 1.0F}).flip();
		Display.create();
		Keyboard.create();
		Mouse.create();
		this.width = Display.getWidth();
		this.height = Display.getHeight();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glClearColor(fr, fg, fb, 0.0F);
		GL11.glClearDepth(1.0D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
//		GL11.glEnable(GL11.GL_ALPHA_TEST);
//		GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		this.level = new Level(256, 256, 64);
		this.levelRenderer = new LevelRenderer(this.level);
		this.player = new Player(this.level);
		this.particleEngine = new ParticleEngine(this.level);
        Mouse.setGrabbed(true);
		for(int i = 0; i < 10; ++i) {
			Zombie zombie = new Zombie(this.level, 128.0F, 0.0F, 128.0F);
			zombie.resetPos();
			this.zombies.add(zombie);
		}
	}

	public void destroy() {
		this.level.save();
		EagRuntime.destroy();
	}

	public void run() {
		try {
			this.init();
		} catch (Exception var9) {
			System.out.println("Failed to start RubyDung");
			throw new RuntimeException(var9);
		}

		long lastTime = System.currentTimeMillis();
		int frames = 0;

		try {
			while(!Display.isCloseRequested() && (EagRuntime.getPlatformType() != EnumPlatformType.DESKTOP || !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))) {
				this.timer.advanceTime();

				for(int e = 0; e < this.timer.ticks; ++e) {
					this.tick();
				}

				this.render(this.timer.a);
				++frames;

				while(System.currentTimeMillis() >= lastTime + 1000L) {
					System.out.println(frames + " fps, " + Chunk.updates);
					Chunk.updates = 0;
					lastTime += 1000L;
					frames = 0;
				}
			}
		} catch (Exception var10) {
			var10.printStackTrace();
		} finally {
			this.destroy();
		}

	}
	private int saveCountdown = 600;

	private void levelSave() {
	    if (level == null) return;

	    saveCountdown--;
	    if (saveCountdown <= 0) {
	        level.save();
	        saveCountdown = 600;
	    }
	}
	public void tick() {
		while(Keyboard.next()) {
			if(Keyboard.getEventKeyState()) {
				if(Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
					this.level.save();
				}

				if(Keyboard.getEventKey() == Keyboard.KEY_1) {
					this.paintTexture = 1;
				}

				if(Keyboard.getEventKey() == Keyboard.KEY_2) {
					this.paintTexture = 3;
				}

				if(Keyboard.getEventKey() == Keyboard.KEY_3) {
					this.paintTexture = 4;
				}

				if(Keyboard.getEventKey() == Keyboard.KEY_4) {
					this.paintTexture = 5;
				}
				
				if(Keyboard.getEventKey() == Keyboard.KEY_6) {
					this.paintTexture = 6;
				}

				if(Keyboard.getEventKey() == Keyboard.KEY_G) {
					this.zombies.add(new Zombie(this.level, this.player.x, this.player.y, this.player.z));
				}
			}
		}

		this.level.tick();
		this.particleEngine.tick();

		for(int i = 0; i < this.zombies.size(); ++i) {
			((Zombie)this.zombies.get(i)).tick();
			if(((Zombie)this.zombies.get(i)).removed) {
				this.zombies.remove(i--);
			}
		}

		this.player.tick();
		levelSave();
	}

	private void moveCameraToPlayer(float a) {
		GL11.glTranslatef(0.0F, 0.0F, -0.3F);
		GL11.glRotatef(this.player.xRot, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(this.player.yRot, 0.0F, 1.0F, 0.0F);
		float x = this.player.xo + (this.player.x - this.player.xo) * a;
		float y = this.player.yo + (this.player.y - this.player.yo) * a;
		float z = this.player.zo + (this.player.z - this.player.zo) * a;
		GL11.glTranslatef(-x, -y, -z);
	}

	private void setupCamera(float a) {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluPerspective(70.0F, (float)this.width / (float)this.height, 0.05F, 1000.0F);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		this.moveCameraToPlayer(a);
	}

	private void pick(float a) {
	    double px = player.x;
	    double py = player.y;
	    double pz = player.z;

	    float yaw = (float) Math.toRadians(player.yRot);
	    float pitch = (float) Math.toRadians(player.xRot);

	    double dx = Math.sin(yaw) * Math.cos(pitch);
	    double dy = -Math.sin(pitch);
	    double dz = -Math.cos(yaw) * Math.cos(pitch);

	    double reach = 3.0;
	    double step = 0.05;

	    HitResult closestHit = null;

	    double closestT = reach + 1;

	    for (int x = (int) Math.floor(px - reach); x <= (int) Math.floor(px + reach); x++) {
	        for (int y = (int) Math.floor(py - (reach + 1)); y <= (int) Math.floor(py + (reach + 1)); y++) {
	            for (int z = (int) Math.floor(pz - reach); z <= (int) Math.floor(pz + reach); z++) {
	                int block = level.getTile(x, y, z);
	                if (block != 0) {

	                    double txmin = (x - px) / dx;
	                    double txmax = (x + 1 - px) / dx;
	                    if (txmin > txmax) { double temp = txmin; txmin = txmax; txmax = temp; }

	                    double tymin = (y - py) / dy;
	                    double tymax = (y + 1 - py) / dy;
	                    if (tymin > tymax) { double temp = tymin; tymin = tymax; tymax = temp; }

	                    double tzmin = (z - pz) / dz;
	                    double tzmax = (z + 1 - pz) / dz;
	                    if (tzmin > tzmax) { double temp = tzmin; tzmin = tzmax; tzmax = temp; }

	                    double tEnter = Math.max(Math.max(txmin, tymin), tzmin);
	                    double tExit = Math.min(Math.min(txmax, tymax), tzmax);

	                    if (tEnter <= tExit && tEnter < closestT && tEnter >= 0 && tEnter <= reach + 1) {
	                        closestT = tEnter;

	                        int face;
	                        if (tEnter == txmin) {
	                            face = dx > 0 ? 4 : 5;
	                        } else if (tEnter == tymin) {
	                            face = dy > 0 ? 0 : 1;
	                        } else {
	                            face = dz > 0 ? 2 : 3;
	                        }

	                        closestHit = new HitResult(block, x, y, z, face);
	                    }
	                }
	            }
	        }
	    }

	    this.hitResult = closestHit;
	}

	public void render(float a) {
		float xo = (float)Mouse.getDX();
		float yo = (float)Mouse.getDY();
		this.player.turn(xo, yo);
		this.pick(a);
		int var8;
		while(Mouse.next()) {
			if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && this.hitResult != null) {
				Tile frustum = Tile.tiles[this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z)];
				boolean i = this.level.setTile(this.hitResult.x, this.hitResult.y, this.hitResult.z, 0);
				if(frustum != null && i) {
					frustum.destroy(this.level, this.hitResult.x, this.hitResult.y, this.hitResult.z, this.particleEngine);
				}
			}

			if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && this.hitResult != null) {
				int var7 = this.hitResult.x;
				var8 = this.hitResult.y;
				int zombie = this.hitResult.z;
				if(this.hitResult.f == 0) {
					--var8;
				}

				if(this.hitResult.f == 1) {
					++var8;
				}

				if(this.hitResult.f == 2) {
					--zombie;
				}

				if(this.hitResult.f == 3) {
					++zombie;
				}

				if(this.hitResult.f == 4) {
					--var7;
				}

				if(this.hitResult.f == 5) {
					++var7;
				}

				this.level.setTile(var7, var8, zombie, this.paintTexture);
			}
		}


		if (Display.wasResized()) {
			this.width = Display.getWidth();
			this.height = Display.getHeight();
			GL11.glViewport(0, 0, this.width, this.height);
		}

		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
		this.setupCamera(a);
		GL11.glEnable(GL11.GL_CULL_FACE);
		Frustum var9 = Frustum.getFrustum();
		this.levelRenderer.updateDirtyChunks(this.player);
		this.setupFog(0);
		GL11.glEnable(GL11.GL_FOG);
		this.levelRenderer.render(this.player, 0);

		Zombie var10;
		for(var8 = 0; var8 < this.zombies.size(); ++var8) {
			var10 = (Zombie)this.zombies.get(var8);
			if(var10.isLit() && var9.isVisible(var10.bb)) {
				((Zombie)this.zombies.get(var8)).render(a);
			}
		}

		this.particleEngine.render(this.player, a, 0);
		this.setupFog(1);
		this.levelRenderer.render(this.player, 1);

		for(var8 = 0; var8 < this.zombies.size(); ++var8) {
			var10 = (Zombie)this.zombies.get(var8);
			if(!var10.isLit() && var9.isVisible(var10.bb)) {
				((Zombie)this.zombies.get(var8)).render(a);
			}
		}

		this.particleEngine.render(this.player, a, 1);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_FOG);
		if(this.hitResult != null) {
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			this.levelRenderer.renderHit(this.hitResult);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		}

		this.drawGui(a);
		Display.update();
	}
	
	private void drawGui(float a) {
		int screenWidth = this.width * 240 / this.height;
		int screenHeight = this.height * 240 / this.height;
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0D, (double)screenWidth, (double)screenHeight, 0.0D, 100.0D, 300.0D);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glTranslatef(0.0F, 0.0F, -200.0F);
		GL11.glPushMatrix();
		GL11.glTranslatef((float)(screenWidth - 16), 16.0F, 0.0F);
		Tesselator t = Tesselator.instance;
		GL11.glScalef(16.0F, 16.0F, 16.0F);
		GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(-1.5F, 0.5F, -0.5F);
		GL11.glScalef(-1.0F, -1.0F, 1.0F);
		int id = Textures.loadTexture("/terrain.png", 9728);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		t.init();
		Tile.tiles[this.paintTexture].render(t, this.level, 0, -2, 0, 0);
		t.flush();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
		int wc = screenWidth / 2;
		int hc = screenHeight / 2;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		t.init();
		t.vertex((float)(wc + 1), (float)(hc - 4), 0.0F);
		t.vertex((float)(wc - 0), (float)(hc - 4), 0.0F);
		t.vertex((float)(wc - 0), (float)(hc + 5), 0.0F);
		t.vertex((float)(wc + 1), (float)(hc + 5), 0.0F);
		t.vertex((float)(wc + 5), (float)(hc - 0), 0.0F);
		t.vertex((float)(wc - 4), (float)(hc - 0), 0.0F);
		t.vertex((float)(wc - 4), (float)(hc + 1), 0.0F);
		t.vertex((float)(wc + 5), (float)(hc + 1), 0.0F);
		t.flush();
	}

	private void setupFog(int i) {
		if(i == 0) {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			GL11.glFogf(GL11.GL_FOG_DENSITY, 0.001F);
			GL11.glFog(GL11.GL_FOG_COLOR, this.fogColor0);
			GL11.glDisable(GL11.GL_LIGHTING);
		} else if(i == 1) {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			GL11.glFogf(GL11.GL_FOG_DENSITY, 0.06F);
			GL11.glFog(GL11.GL_FOG_COLOR, this.fogColor1);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_COLOR_MATERIAL);
			float br = 0.6F;
//			GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, this.getBuffer(br, br, br, 1.0F));
		}

	}

	private FloatBuffer getBuffer(float a, float b, float c, float d) {
		this.lb.clear();
		this.lb.put(a).put(b).put(c).put(d);
		this.lb.flip();
		return this.lb;
	}

	public static void checkError() {
		int e = GL11.glGetError();
		if(e != 0) {
			throw new IllegalStateException(GLU.gluErrorString(e));
		}
	}
}