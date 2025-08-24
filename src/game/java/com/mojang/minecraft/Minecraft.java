package com.mojang.minecraft;

import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.gui.Font;
import com.mojang.minecraft.level.Chunk;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelRenderer;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.renderer.Frustum;
import com.mojang.minecraft.renderer.Tesselator;
import com.mojang.minecraft.renderer.Textures;
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

public class Minecraft implements Runnable {
	public static final String VERSION_STRING = "0.0.11a";
	private boolean fullscreen = false;
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
	private ArrayList<Entity> entities = new ArrayList();
	private int yMouseAxis = 1;
	public Textures textures;
	private Font font;
	private int editMode = 0;
	private volatile boolean running = false;
	private String fpsString = "";
	private boolean mouseGrabbed = false;
	private IntBuffer viewportBuffer = GLAllocation.createIntBuffer(16);
	private IntBuffer selectBuffer = GLAllocation.createIntBuffer(2000);
	private HitResult hitResult = null;
	FloatBuffer lb = GLAllocation.createFloatBuffer(16);
	
	public Minecraft(int width, int height, boolean fullscreen) {
		this.width = width;
		this.height = height;
		this.fullscreen = fullscreen;
		this.textures = new Textures();
	}

	public void init() throws LWJGLException, IOException {
		int col0 = 16710650;
		int col1 = 920330;
		float fr = 0.5F;
		float fg = 0.8F;
		float fb = 1.0F;
		this.fogColor0.put(new float[]{(float)(col0 >> 16 & 255) / 255.0F, (float)(col0 >> 8 & 255) / 255.0F, (float)(col0 & 255) / 255.0F, 1.0F}).flip();
		this.fogColor1.put(new float[]{(float)(col1 >> 16 & 255) / 255.0F, (float)(col1 >> 8 & 255) / 255.0F, (float)(col1 & 255) / 255.0F, 1.0F}).flip();
		if(this.fullscreen) {
			Display.toggleFullscreen();
			this.width = Display.getWidth();
			this.height = Display.getHeight();
		} else {
			this.width = Display.getWidth();
			this.height = Display.getHeight();
		}

		Display.setTitle("Minecraft 0.0.11a");

		Display.create();
		Keyboard.create();
		Mouse.create();
		this.checkGlError("Pre startup");
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glClearColor(fr, fg, fb, 0.0F);
		GL11.glClearDepth(1.0D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		this.checkGlError("Startup");
		this.level = new Level(256, 256, 64);
		this.levelRenderer = new LevelRenderer(this.level, this.textures);
		this.player = new Player(this.level);
		this.particleEngine = new ParticleEngine(this.level, this.textures);
		this.font = new Font("/default.gif", this.textures);
		for(int imgData = 0; imgData < 10; ++imgData) {
			Zombie e = new Zombie(this.level, this.textures, 128.0F, 0.0F, 128.0F);
			e.resetPos();
			this.entities.add(e);
		}
		
		IntBuffer var11 = GLAllocation.createIntBuffer(256);
		var11.clear().limit(256);
		this.grabMouse();

		this.checkGlError("Post startup");
	}
	
	private void checkGlError(String string) {
		int errorCode = GL11.glGetError();
		if(errorCode != 0) {
			String errorString = GLU.gluErrorString(errorCode);
			System.out.println("########## GL ERROR ##########");
			System.out.println("@ " + string);
			System.out.println(errorCode + ": " + errorString);
			throw new RuntimeException(errorCode + ": " + errorString);

		}

	}

	public void destroy() {
		try {
			this.level.save();
		} catch (Exception var2) {
		}
		EagRuntime.destroy();
	}

	public void run() {
		this.running = true;
		try {
			this.init();
		} catch (Exception var9) {
			System.out.println("Failed to start Minecraft");
			return;
		}

		long lastTime = System.currentTimeMillis();
		int frames = 0;

		try {
			while(this.running) {
					if(Display.isCloseRequested()) {
						this.stop();
					}

					this.timer.advanceTime();

					for(int e = 0; e < this.timer.ticks; ++e) {
						this.tick();
					}

					this.checkGlError("Pre render");
					this.render(this.timer.a);
					this.checkGlError("Post render");
					++frames;

					while(System.currentTimeMillis() >= lastTime + 1000L) {
						this.fpsString = frames + " fps, " + Chunk.updates + " chunk updates";
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
	
	public void stop() {
		this.running = false;
	}

	public void grabMouse() {
		if(!this.mouseGrabbed) {
			this.mouseGrabbed = true;
			Mouse.setGrabbed(true);

		}
	}
	
	public void releaseMouse() {
		if(this.mouseGrabbed) {
			this.mouseGrabbed = false;
			Mouse.setGrabbed(false);

		}
	}
	
	private void handleMouseClick() {
		if(this.editMode == 0) {
			if(this.hitResult != null) {
				Tile x = Tile.tiles[this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z)];
				boolean y = this.level.setTile(this.hitResult.x, this.hitResult.y, this.hitResult.z, 0);
				if(x != null && y) {
					x.destroy(this.level, this.hitResult.x, this.hitResult.y, this.hitResult.z, this.particleEngine);
				}
			}
		} else if(this.hitResult != null) {
			int var5 = this.hitResult.x;
			int var6 = this.hitResult.y;
			int z = this.hitResult.z;
			if(this.hitResult.f == 0) {
				--var6;
			}

			if(this.hitResult.f == 1) {
				++var6;
			}

			if(this.hitResult.f == 2) {
				--z;
			}

			if(this.hitResult.f == 3) {
				++z;
			}

			if(this.hitResult.f == 4) {
				--var5;
			}

			if(this.hitResult.f == 5) {
				++var5;
			}

			AABB aabb = Tile.tiles[this.paintTexture].getAABB(var5, var6, z);
			if(aabb == null || this.isFree(aabb)) {
				this.level.setTile(var5, var6, z, this.paintTexture);
			}
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
		while(Mouse.next()) {
			if(!this.mouseGrabbed && Mouse.getEventButtonState()) {
				this.grabMouse();
			} else {
				if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
					this.handleMouseClick();
				}

				if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
					this.editMode = (this.editMode + 1) % 2;
				}
			}
		}

		while(true) {
			do {
				if(!Keyboard.next()) {
					this.level.tick();
					this.particleEngine.tick();

					for(int i = 0; i < this.entities.size(); ++i) {
						((Entity)this.entities.get(i)).tick();
						if(((Entity)this.entities.get(i)).removed) {
							this.entities.remove(i--);
						}
					}

					this.player.tick();
		            levelSave();
					return;
				}
			} while(!Keyboard.getEventKeyState());

			if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE && (!this.fullscreen)) {
				this.releaseMouse();
			}

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

			if(Keyboard.getEventKey() == Keyboard.KEY_Y) {
				this.yMouseAxis *= -1;
			}

			if(Keyboard.getEventKey() == Keyboard.KEY_G) {
				this.entities.add(new Zombie(this.level, this.textures, this.player.x, this.player.y, this.player.z));
			}
		}
	}
	
	private boolean isFree(AABB aabb) {
		if(this.player.bb.intersects(aabb)) {
			return false;
		} else {
			for(int i = 0; i < this.entities.size(); ++i) {
				if(((Entity)this.entities.get(i)).bb.intersects(aabb)) {
					return false;
				}
			}

			return true;
		}
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
		if(!Display.isActive()) {
			this.releaseMouse();
		}
		if (Display.wasResized()) {
			this.width = Display.getWidth();
			this.height = Display.getHeight();
		}
		GL11.glViewport(0, 0, this.width, this.height);
		if(this.mouseGrabbed) {
			float frustum = 0.0F;
			float i = 0.0F;
			frustum = (float)Mouse.getDX();
			i = (float)Mouse.getDY();

			this.player.turn(frustum, i * (float)this.yMouseAxis);
		}

		this.checkGlError("Set viewport");
		this.pick(a);
		this.checkGlError("Picked");
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
		this.setupCamera(a);
		this.checkGlError("Set up camera");
		GL11.glEnable(GL11.GL_CULL_FACE);
		Frustum var5 = Frustum.getFrustum();
		this.levelRenderer.updateDirtyChunks(this.player);
		this.checkGlError("Update chunks");
		this.setupFog(0);
		GL11.glEnable(GL11.GL_FOG);
		this.levelRenderer.render(this.player, 0);
		this.checkGlError("Rendered level");

		Entity zombie;
		int var6;
		for(var6 = 0; var6 < this.entities.size(); ++var6) {
			zombie = (Entity)this.entities.get(var6);
			if(zombie.isLit() && var5.isVisible(zombie.bb)) {
				((Entity)this.entities.get(var6)).render(a);
			}
		}

		this.checkGlError("Rendered entities");
		this.particleEngine.render(this.player, a, 0);
		this.checkGlError("Rendered particles");
		this.setupFog(1);
		this.levelRenderer.render(this.player, 1);

		for(var6 = 0; var6 < this.entities.size(); ++var6) {
			zombie = (Entity)this.entities.get(var6);
			if(!zombie.isLit() && var5.isVisible(zombie.bb)) {
				((Entity)this.entities.get(var6)).render(a);
			}
		}

		this.particleEngine.render(this.player, a, 1);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_FOG);
		this.checkGlError("Rendered rest");
		if(this.hitResult != null) {
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			this.levelRenderer.renderHit(this.hitResult, this.editMode, this.paintTexture);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		}

		this.checkGlError("Rendered hit");
		this.drawGui(a);
		this.checkGlError("Rendered gui");
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
		this.checkGlError("GUI: Init");
		GL11.glPushMatrix();
		GL11.glTranslatef((float)(screenWidth - 16), 16.0F, 0.0F);
		Tesselator t = Tesselator.instance;
		GL11.glScalef(16.0F, 16.0F, 16.0F);
		GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(-1.5F, 0.5F, -0.5F);
		GL11.glScalef(-1.0F, -1.0F, 1.0F);
		int id = this.textures.loadTexture("/terrain.png", 9728);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		t.init();
		Tile.tiles[this.paintTexture].render(t, this.level, 0, -2, 0, 0);
		t.flush();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
		this.checkGlError("GUI: Draw selected");
		this.font.drawShadow("0.0.11a", 2, 2, 16777215);
		this.font.drawShadow(this.fpsString, 2, 12, 16777215);
		this.checkGlError("GUI: Draw text");
		int wc = screenWidth / 2;
		int hc = screenHeight / 2;
		GL11.color(1.0F, 1.0F, 1.0F, 1.0F);
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
		this.checkGlError("GUI: Draw crosshair");
	}
	private void setupFog(int i) {
		if(i == 0) {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			GL11.glFogf(GL11.GL_FOG_DENSITY, 0.001F);
			GL11.glFog(GL11.GL_FOG_COLOR, this.fogColor0);
			GL11.glDisable(GL11.GL_LIGHTING);
		} else if(i == 1) {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			GL11.glFogf(GL11.GL_FOG_DENSITY, 0.01F);
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
