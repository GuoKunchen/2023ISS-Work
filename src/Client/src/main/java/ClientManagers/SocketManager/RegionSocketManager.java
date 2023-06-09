package ClientManagers.SocketManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class RegionSocketManager {
    public Socket socket = null;
    private BufferedReader input = null;
    private PrintWriter output = null;
    private boolean isRunning = false;
    private Thread infoListener;

    private String region = "localhost";

    public RegionSocketManager() {

    }

    public void setRegionIP(String ip) {
        this.region = ip;
    }

    // 与Region建立连接
    public void connectRegionServer(int PORT) throws IOException {
        socket = new Socket(region, PORT);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        isRunning = true;
        this.listenToRegion();
        System.out.println("新消息>>>与从节点" + PORT + "建立Socket连接");
    }

    public boolean connectRegionServer(String ip) throws IOException {
		if(ip.equals("null")){
			System.out.println("服务器登记列表中不存在该表单，请检查表名后重试");
			return false;
		}
        System.out.println("connectRegionServer : "+ip);
        socket = new Socket(ip, 22222);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        isRunning = true;
        this.listenToRegion();
        System.out.println("新消息>>>与从节点" + ip + "建立Socket连接");
		return true;
    }


    public void receiveFromRegion() throws IOException {
        String line = new String("");
        if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
            System.out.println("新消息>>>Socket已经关闭!");
        } else {
            line = input.readLine();
        }
        if (line != null) {
            System.out.println("新消息>>>从region服务器收到的信息是：" + line);
        }
    }

    public void listenToRegion() {
        infoListener = new InfoListener();
        infoListener.start();
    }

    public void closeRegionSocket() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
        infoListener.stop();
    }

    // 像从服务器发送信息的api
    public void sendToRegion(String info) {
//        System.out.println("发送给从节点的消息是：" + info);
        output.println(info);
    }


    class InfoListener extends Thread {
        @Override
        public void run() {
            System.out.println("新消息>>>客户端的从服务器监听线程启动！");
            while (isRunning) {
                if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
                    isRunning = false;
                    break;
                }

                try {
                    receiveFromRegion();
                }catch(SocketException e){
					System.out.println("Socket连接出现问题:"+socket);
					try {
						System.out.println("尝试关闭socket");
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} 
				catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    sleep(100);
                } catch (InterruptedException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
