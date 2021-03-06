// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package edu.cmu.cs.gabriel.network;

import java.io.DataInputStream;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AccControlThread extends Thread {

    private static final String LOG_TAG = "krha";

    private Handler networkHandler;
    private DataInputStream networkReader;
    private boolean is_running = true;


    public AccControlThread(DataInputStream dataInputStream, Handler networkHandler) {
        this.networkReader = dataInputStream;
        this.networkHandler = networkHandler;
    }

    @Override
    public void run() {
        // Recv initial simulation information
        while(is_running == true){
            try {
                String recvMsg = this.receiveMsg(networkReader);
                this.notifyReceivedData(recvMsg);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
                // Do not send error to handler, Streaming thread already sent it.
//              this.notifyError(e.getMessage());
                break;
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.toString());
                this.notifyError(e.getMessage());
            }
        }
    }

    private String receiveMsg(DataInputStream reader) throws IOException {
        int retLength = reader.readInt();
        byte[] recvByte = new byte[retLength];
        int readSize = 0;
        while(readSize < retLength){
            int ret = reader.read(recvByte, readSize, retLength-readSize);
            if(ret <= 0){
                break;
            }
            readSize += ret;
        }
        String receivedString = new String(recvByte);
        return receivedString;
    }

    private void notifyReceivedData(String recvData) throws JSONException {
        // convert the message to JSON
        JSONObject obj;
        String controlMsg = null;
        obj = new JSONObject(recvData);

        try{
            controlMsg = obj.getString(NetworkProtocol.HEADER_MESSAGE_CONTROL);
        } catch(JSONException e){}
        if (controlMsg != null){
            Message msg = Message.obtain();
            msg.what = NetworkProtocol.NETWORK_RET_CONFIG;
            msg.obj = controlMsg;
            this.networkHandler.sendMessage(msg);
        }
    }

    private void notifyError(String errorMessage) {
        Message msg = Message.obtain();
        msg.what = NetworkProtocol.NETWORK_RET_FAILED;
        msg.obj = errorMessage;
        this.networkHandler.sendMessage(msg);
    }

    public void close() {
        this.is_running = false;
        try {
            if(this.networkReader != null)
                this.networkReader.close();
        } catch (IOException e) {
        }
    }
}
