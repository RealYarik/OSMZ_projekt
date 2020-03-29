package com.vsb.kru13.osmzhttpserver;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;


public class SocketServer extends Thread {
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    private ServerSocket serverSocket;
    public final int port = 12345;
    boolean bRunning;
    private Handler messageHandler;
    private Semaphore lock;

    public SocketServer(Handler messageHandler,int threadCount) {
        this.messageHandler = messageHandler;
        this.lock = new Semaphore(threadCount);
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {

                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                try{
                    lock.acquire();
                    Log.d("THREADS", "Get LOCK " + lock.availablePermits());
                    ClientSocket clientSocket = new ClientSocket(s,messageHandler,lock);
                    clientSocket.start();
                }catch(InterruptedException e){
                    Log.d("SERVER", "No free slots");
                }
            }
        }
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        }finally {
            serverSocket = null;
            bRunning = false;
        }
    }
}

