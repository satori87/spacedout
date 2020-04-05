package com.bbg.patch;

import com.bbg.patch.SpacePatch;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.bbg.patch.Prefs;

public class DesktopPatch {

    public static void main(String[] arg) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = Prefs.WINDOWNAME;
        cfg.width = Prefs.WINDOWWIDTH;
        cfg.height = Prefs.WINDOWHEIGHT;
        cfg.resizable = false;
        SpacePatch sp = new SpacePatch();
        new LwjglApplication(sp, cfg);
    }

}
