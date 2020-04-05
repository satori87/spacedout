package com.bbg.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class AutoVersion {

    public static void autoVersion() {
        if (Gdx.files.local("update.dat").exists()) {
            Gdx.files.local("update.dat").delete();
        }
        FileHandle upd = Gdx.files.local("update.dat");
        List<String> fileList = getFileList("assets");
        fileList.add("game.exe");
        fileList.add("game.jar");
        upd.writeString("52.15.137.51" + "\n", false);
        
        for (String s : fileList) {
            String md5 = calcMD5Local(s);
            upd.writeString(s + "," + md5 + "\n", true);
        }
    }

    public static List<String> getFileList(String dir) {
        //This is run on the folder containing the assets. it produces update.dat to be downloaded for comparison
        List<String> list = new LinkedList<String>();
        FileHandle[] files = Gdx.files.local(dir).list();
        for (FileHandle file : files) {
            if (file.isDirectory()) {
                List<String> subList = new LinkedList<String>();
                subList = getFileList(dir + "/" + file.name());
                for (String s : subList) {
                    list.add(s);
                }
            } else {
                list.add(dir + "/" + file.name());
            }
        }
        return list;
    }

    public static String calcMD5Local(String fileName) {
        InputStream fis = Gdx.files.local(fileName).read();
        boolean success = true;
        String md5 = "";
        do {
            try {
                md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                success = true;
            } catch (IOException e) {
                success = false;
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } while (success == false);
        return md5;
    }

    public static String calcMD5(String fileName) {
        InputStream fis = Gdx.files.local(fileName).read();
        boolean success = true;
        String md5 = "";
        do {
            try {
                md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                success = true;
            } catch (IOException e) {
                success = false;
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } while (success == false);
        return md5;
    }

}
