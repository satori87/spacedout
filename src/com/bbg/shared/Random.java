package com.bbg.shared;

public class Random {

	public static int getInt(int n) {

		return (int) (Math.random() * n);
	}

	public static float frnd() {
		return (float) Math.random();
	}

	public static float dir() {
		return (float)(Math.random() * Math.PI * 2);
	}
}
