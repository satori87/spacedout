package com.bbg.patch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;

public class Patch {

    GameScreen screen;
    UpdFile curUpdFile = new UpdFile();
    boolean first = true;
    boolean firstPass = true;
    public long tick = 0;
    public String curFile = "";
    public int progress = 0;
    public boolean downloading = false;
    InputStream is;
    OutputStream os;
    Queue<UpdFile> files = new LinkedList<UpdFile>();
    public int curTask = 0;
    
    
    public Patch(GameScreen screen) {
        this.screen = screen;
    }

    public void update(long tick) {
        this.tick = tick;
        boolean keepGoing = true;
        if (first) {
        	//AutoVersion.autoVersion();
        	//System.exit(0);
            curTask = 1;
            first = false;
            //downloadFile(Prefs.hostName,"lobbyip.txt");
            downloadFile(Prefs.hostName, "update.dat");
        } else {
            if (curTask == 1) {
                if (!downloading) {
                    curTask = 2;
                    readUpdateFile();
                }
            } else if (curTask == 2) {
                //process a file from queue
                if (files.isEmpty()) {
                    //nope, we're done!
                    //but we're gonna check everything one more time
                    if (firstPass) {
                        curTask = 1;
                        firstPass = false;
                    } else {
                        curTask = 3;
                    }
                } else {
                    if (!downloading()) {
                        do {
                            curUpdFile = files.remove();
                            FileHandle fh = Gdx.files.local(curUpdFile.name);
                            System.out.println(curUpdFile.name);
                            if (fh.exists()) {
                                String md5 = AutoVersion.calcMD5(curUpdFile.name);
                                
                                if (!md5.equals(curUpdFile.version)) {
                                	System.out.println(md5 + " not same as " + curUpdFile.version);
                                    downloadFile(Prefs.hostName, curUpdFile.name);
                                }
                            } else {
                                downloadFile(Prefs.hostName, curUpdFile.name);
                            }
                            if (downloading() || getTick() - tick >= 10) {
                                keepGoing = false;
                            }
                        } while (!files.isEmpty() && keepGoing);
                    }
                }
            } else if (curTask == 3) {
                //done, launch the game
            	
				//FileHandle fh = Gdx.files.local("java -jar game.jar");				
				 //ProcessBuilder pb = new ProcessBuilder(fh.file().getAbsolutePath());
				 try {
					 Runtime.getRuntime().exec("java -jar game.jar");
					//Process p = pb.start();
					//System.out.println(p.toString());
				} catch (IOException e) {
					e.printStackTrace();
				} 
				 /*
                FileHandle fh = Gdx.files.local("jre1.8.0_111/bin/javaw.exe");
                FileHandle fh2 = Gdx.files.local("game.jar");

                try {
                    System.out.println(fh.file().getAbsolutePath() + " -jar " + fh2.file().getAbsolutePath());
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
                System.exit(0);
                curTask = 4;
            }
        }

    }

    void readUpdateFile() {
        FileHandle fh = Gdx.files.local("update.dat");
        String text = fh.readString();
        List<String> lines = Arrays.asList(text.split("\\r?\\n"));
        int c = 0;
        for (String curLine : lines) {
            if (c == 0) {
                //serverIP = curLine;
                c = 1;
            } else {
                String[] words = curLine.split(",");
                if (words.length == 2) {
                    files.add(new UpdFile(words[0], words[1]));
                }
            }
        }

    }

    public void render() {
        String prog = Integer.toString(getProgress()) + "%";
        int barX = (int) (238f * ((float) getProgress() / 100f));
        screen.drawFrame(32, 16, 256, 128, true);
        if (curTask < 3) {
            screen.drawFont(0, 160, 54, "Downloading", true, 2f, Color.WHITE);
            String[] words = curFile.split("/");
            int c = 0;
            //for (String s : words) {
            //    c++;
            //}
            c = words.length;
            screen.drawFont(0, 160, 110, words[c - 1], true, 1.0f, Color.WHITE);
            screen.drawRegion(AssetLoader.wall[1], 41, 160, false, 0, 1);
            screen.batcher.setColor(Color.BLUE);
            screen.batcher.draw(AssetLoader.wallTex, 41, 160, barX, 63, 0, 63, barX, 63, false, true);
            screen.batcher.setColor(Color.WHITE);
            screen.drawFont(0, 160, 195, prog, true, 1, Color.WHITE);
        }
    }

    public long getTick() {
        return System.currentTimeMillis();
    }

    public int getProgress() {
        return accessProgress(false, 0);
    }

    public void setProgress(int p) {
        accessProgress(true, p);
    }

    public synchronized int accessProgress(boolean set, int p) {
        if (set) {
            progress = p;
        }
        return progress;
    }

    public boolean downloading() {
        return accessDownloading(false, false);
    }

    public void setDownloading(boolean d) {
        accessDownloading(true, d);
    }

    public synchronized boolean accessDownloading(boolean set, boolean d) {
        if (set) {
            downloading = d;
        }
        return downloading;
    }

    public void downloadFile(String serverIP, String fileName) {
    	System.out.printf(serverIP);
        progress = 0;
        accessDownloading(true, true);
        HttpRequest request = new HttpRequest(HttpMethods.GET);
        request.setTimeOut(Prefs.HTTPTIMEOUT);
        request.setUrl("http://" + serverIP + "/" + fileName);
        curFile = fileName;
        // Send the request, listen for the response						
        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                // Determine how much we have to download
                long length = Long.parseLong(httpResponse.getHeader("Content-Length"));

                // We're going to download the file to local storage, create the streams
                is = httpResponse.getResultAsStream();
                os = Gdx.files.local(curFile).write(false);
                byte[] bytes = new byte[1024];
                int count = -1;
                long read = 0;
                try {
                    // Keep reading bytes and storing them until there are no more.
                    while ((count = is.read(bytes, 0, bytes.length)) != -1) {
                        os.write(bytes, 0, count);
                        read += count;

                        // Update the UI with the download progress
                        final int prog = ((int) (((double) read / (double) length) * 100));
                        accessProgress(true, prog);
                        // Since we are downloading on a background thread, post a runnable to touch ui											
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                if (prog == 100) {
                                    //download done
                                    accessDownloading(true, false);
                                    boolean fine = true;
                                    do {
                                        try {
                                            fine = true;
                                            os.close();
                                        } catch (IOException e) {
                                            fine = false;
                                            e.printStackTrace();
                                        }
                                    } while (fine == false);
                                    do {
                                        try {
                                            fine = true;
                                            is.close();
                                        } catch (IOException e) {
                                            fine = false;
                                            e.printStackTrace();
                                        }
                                    } while (fine == false);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    screen.debug("sucks3");
                }
            }

            @Override
            public void failed(Throwable t) {
                screen.debug("sucks2");
            }

            @Override
            public void cancelled() {
                screen.debug("sucks1");
            }
        });
    }

}
