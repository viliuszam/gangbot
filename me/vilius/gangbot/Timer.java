package me.vilius.gangbot;

/*
 *   Written by yours truly - Nya (Vilius)
 *   Created 2019-09-13
 *   Inspired by Flex Seal™
 */

//Gross

public class Timer {
	private long currentTime;

	public Timer() {
		this.reset();
	}
	
	public void reset() {
		this.currentTime = System.nanoTime() / 1000000;
	}

	public boolean hasTimePassed(final long time) {
		return (System.nanoTime() / 1000000 - currentTime) >= time;
	}

	public long getCurrentTime(){
		return this.currentTime;
	}
}
