package com.mojang.minecraft.level;

import java.util.Comparator;

import com.mojang.minecraft.Player;

public class DirtyChunkSorter implements Comparator<Chunk> {
	private Player player;
	private Frustum frustum;
	private long now = System.currentTimeMillis();

	public DirtyChunkSorter(Player player, Frustum frustum) {
		this.player = player;
		this.frustum = frustum;
	}

	public int compare(Chunk c0, Chunk c1) {
		boolean i0 = this.frustum.isVisible(c0.aabb);
		boolean i1 = this.frustum.isVisible(c1.aabb);
		if(i0 && !i1) {
			return -1;
		} else if(i1 && !i0) {
			return 1;
		} else {
			int t0 = (int)((this.now - c0.dirtiedTime) / 2000L);
			int t1 = (int)((this.now - c1.dirtiedTime) / 2000L);
			return t0 < t1 ? -1 : (t0 > t1 ? 1 : (c0.distanceToSqr(this.player) < c1.distanceToSqr(this.player) ? -1 : 1));
		}
	}

	public int Compare(Object var1, Object var2) {
		return this.compare((Chunk)var1, (Chunk)var2);
	}
}
