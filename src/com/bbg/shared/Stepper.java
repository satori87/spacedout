package com.bbg.shared;

public class Stepper {

	private final int NUMSTEPS = 10;

	public int interval = 0;
	public long stepTick = 0;

	public int[] step;

	public Stepper(int interval) {
		step = new int[NUMSTEPS];
		this.interval = interval;
	}

	public void update(long tick) {
		// step[2] will be 0 or 1, step[3] will be 0,1,or 2
		if (tick > stepTick) {
			stepTick = tick + interval;
			for (int i = 2; i < NUMSTEPS; i++) {
				step[i] += 1;
				if (step[i] >= i) {
					step[i] = 0;
				}
			}
		}

	}

}
