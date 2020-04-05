package com.bbg.shared;

public class ItemDef {
	
	public String name = "Jawn";
	public int type = 0;
	
	public ItemDef(int type) {
		this.type = type;
		name = getItemPackName(type);
	}
	
	public static String getItemPackName(int t) {
		switch(t) {
		default:
			return "Jawn";
		case 0:
			return "Jawn";			
		case 1:
			return "1 Repair Kit";			
		case 2:
			return "2 Shields";			
		case 3:
			return "5 Turbos";		
		}
	}
	
}
