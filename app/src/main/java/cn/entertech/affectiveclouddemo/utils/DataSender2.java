package cn.entertech.affectiveclouddemo.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class DataSender2 {
    private Socket socket;
    private OutputStream outputStream;

    public DataSender2(String ip, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void send(String data) {
        if (outputStream != null) {
            try {
                outputStream.write(data.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
