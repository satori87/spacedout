package com.bbg.mapeditor;

import com.bbg.mapeditor.MapEditor;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.bbg.mapeditor.Prefs;

public class DesktopEditor {

    public static void main(String[] arg) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = Prefs.WINDOWNAME;
        cfg.width = Prefs.WINDOWWIDTH;
        cfg.height = Prefs.WINDOWHEIGHT;
        cfg.resizable = false;
        MapEditor sp = new MapEditor();
        new LwjglApplication(sp, cfg);
    }

}
