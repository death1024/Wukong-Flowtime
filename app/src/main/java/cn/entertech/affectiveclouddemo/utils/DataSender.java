package cn.entertech.affectiveclouddemo.utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class DataSender {
    private WebSocket webSocket;
    private OkHttpClient client;

    public DataSender(String ip, int port) {
        client = new OkHttpClient();
        String url = "ws://" + ip + ":" + port;  // 格式化为 WebSocket URI
        Request request = new Request.Builder().url(url).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket opened");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("Receiving : " + text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                System.out.println("Receiving bytes : " + bytes.hex());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                System.out.println("Closing : " + code + " / " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.out.println("Error : " + t.getMessage());
            }
        });
        // Trigger asynchronous connection
        client.dispatcher().executorService().shutdown();
    }

    public void send(String data) {
        if (webSocket != null) {
            webSocket.send(data);
            System.out.println("Sending data: " + data);
        } else {
            System.out.println("WebSocket is not connected or not open.");
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing Connection");
            System.out.println("WebSocket closed");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            System.out.println("Client shutdown");
        }
    }
}
