package com.bbg.client;

public class User {

	GameScreen screen;
	public int index = 0;
	public String name = "";
	//public int listIndex = 0;
	
	public User(GameScreen screen, int index, String name) {
		this.screen = screen;
		this.index = index;
		this.name = name;
	}
	
	public static class UserListData {
		public User user;
	}

}
