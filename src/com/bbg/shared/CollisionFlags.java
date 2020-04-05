package com.bbg.shared;

public class CollisionFlags {
	static public final short CAT_SOLID = 0x0001;
	static public final short CAT_T1FLAG = 0x0002;
	static public final short CAT_T2FLAG = 0x0004;
	static public final short CAT_T1PLAYER = 0x0008;
	static public final short CAT_T2PLAYER = 0x0010;
	static public final short MASKSOLID = CAT_SOLID | CAT_T1FLAG | CAT_T2FLAG | CAT_T1PLAYER | CAT_T2PLAYER;
	static public final short MASK_T1FLAG = CAT_SOLID | CAT_T2FLAG | CAT_T1PLAYER;
	static public final short MASK_T2FLAG = CAT_SOLID | CAT_T1FLAG | CAT_T2PLAYER;
	static public final short MASK_T1PLAYER = CAT_SOLID | CAT_T1FLAG | CAT_T1PLAYER | CAT_T2PLAYER;
	static public final short MASK_T2PLAYER = CAT_SOLID | CAT_T2FLAG | CAT_T1PLAYER | CAT_T2PLAYER;
}
