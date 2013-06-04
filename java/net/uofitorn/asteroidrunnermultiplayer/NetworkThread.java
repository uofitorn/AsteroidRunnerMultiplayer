package net.uofitorn.asteroidrunnermultiplayer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class NetworkThread extends Thread {

    private final static String TAG = "NetworkThread";
    InetAddress serverAddr;
    Socket socket;
    BufferedReader in;
    boolean connected = false;
    Handler handler;

    public NetworkThread(Handler handler) {
        this.handler = handler;
    }

    public void sendMessage(int message) {
        Log.i(TAG, "Sending message: " + message + " to server.");
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch(Exception e) {
            Log.e(TAG, "Exception caught sending");
        }
    }

    public void run() {
        try {
            while(true) {
                if (connected) {
                    final String msgFromServer = in.readLine();
                    Log.i(TAG, "Message received: " + msgFromServer);
                    switch (Integer.parseInt(msgFromServer)) {
                        case 99:
                            disconnectFromServer();
                            break;
                    }
                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("command", Integer.parseInt(msgFromServer));
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    connectToServer();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Caught IOException");
        }
    }

    public void connectToServer() {
        Log.i(TAG, "Connecting to server");
        try {
            serverAddr = InetAddress.getByName("208.78.100.124");
            socket = new Socket(serverAddr, 3000);
            Log.i(TAG, "Connected to server");
            connected = true;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            Log.e(TAG, "Caught exception connecting to server: " + e.toString());
        }
    }

    public void disconnectFromServer() {
        try {
            socket.close();
            connected = false;
            Log.i(TAG, "Disconnecting from server");
        } catch (Exception e) {
            Log.e(TAG, "Exception closing connection");
        }
    }
}
