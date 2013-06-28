package net.uofitorn.asteroidrunnermultiplayer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkThread extends Thread {

    private final static String TAG = "NetworkThread";
    InetAddress serverAddr;
    Socket socket;
    BufferedReader in;
    boolean connected = false;
    PrintWriter out;
    AsteroidRunner asteroidRunner;

    public NetworkThread(AsteroidRunner asteroidRunner) {
        this.asteroidRunner = asteroidRunner;
    }

    public void sendMessage(String message) {
        Log.i(TAG, "Sending message: " + message + " to server.");
        try {
            out.println(message);
        } catch(Exception e) {
            Log.e(TAG, "Exception caught sending");
        }
    }

    public void run() {
        connectToServer();
        try {
            while(true) {
                if (connected) {
                    final String msgFromServer = in.readLine();
                    /*if (msgFromServer == null) {
                        Log.e(TAG, "WARNING: RECEIVED NULL");
                        continue;
                    } */
                    Log.i(TAG, "Message received: " + msgFromServer);
                    if (msgFromServer.contains("STARTGAME")) {
                            asteroidRunner.startNewGame();
                    } else {
                        try {
                            JSONObject jObject = new JSONObject(msgFromServer);
                            if (jObject.getString("command").equalsIgnoreCase("move")) {
                                int x = Integer.parseInt(jObject.getString("x"));
                                int y = Integer.parseInt(jObject.getString("y"));
                                asteroidRunner.setOtherPlayerLocation(x, y);
                            } else if (jObject.getString("command").equalsIgnoreCase("crashed")){
                                int x = Integer.parseInt(jObject.getString("x"));
                                int y = Integer.parseInt(jObject.getString("y"));
                                asteroidRunner.otherPlayerCrashed(x, y);
                            } else if (jObject.getString("command").equalsIgnoreCase("otherPlayerWon")) {
                                asteroidRunner.handleYouLost();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Caught JSONException in run: " + e.toString());
                        }
                    }
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
        } catch (Exception e) {
            Log.e(TAG, "Caught exception connecting to server: " + e.toString());
        }
    }

    public void closeConnection() {
        try {
            out.println("99");
            socket.close();
            connected = false;
            Log.i(TAG, "Disconnecting from server");
        } catch (Exception e) {
            Log.e(TAG, "Exception closing connection");
        }
    }

    public void sendUpdatedLocation(int x, int y) {
        try {
            JSONObject command = new JSONObject();
            command.put("x", x);
            command.put("y", y);
            command.put("command", "move");
            sendMessage(command.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Caught JSONException in sendUpdatedLocation: " + e.toString());
        }
    }

    public void sendCrashed(int x, int y) {
        try {
            JSONObject command = new JSONObject();
            command.put("x", x);
            command.put("y", y);
            command.put("command", "crashed");
            sendMessage(command.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Caught JSONException in sendCrashed: " + e.toString());
        }
    }
}
