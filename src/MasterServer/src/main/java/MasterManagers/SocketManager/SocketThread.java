package MasterManagers.SocketManager;

import MasterManagers.TableManger;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * 客户端socket线程，负责和客户端进行通信
 */
@Slf4j
public class SocketThread implements Runnable  {

    private boolean isRunning = false;
    private ClientProcessor clientProcessor;
    private RegionProcessor regionProcessor;
	private Socket socket;
    public BufferedReader input = null;
    public PrintWriter output = null;

    public SocketThread(Socket socket, TableManger tableManger) throws IOException {
        this.clientProcessor = new ClientProcessor(tableManger,socket);
        this.regionProcessor = new RegionProcessor(tableManger,socket);
        this.isRunning = true;
        // 基于Socket建立输入输出流
		this.socket=socket;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("MasterServer建立Socket连接,连接信息IP与Port为:" + socket.getInetAddress() +":"+ socket.getPort());
    }

    @Override
    public void run() {
        String line;
        try {
            while (isRunning) {
                Thread.sleep(Long.parseLong("1000"));
                line = input.readLine();
                if (line != null) {
                    this.commandProcess(line);
                }
            }
        }catch(SocketException e){
			System.out.println("Socket连接已终止,IP:"+this.socket.getInetAddress()+" Port:"+this.socket.getPort());
		}catch (InterruptedException | IOException e) {
		 	e.printStackTrace();
        }

    }

    public void sendToRegion(String info) {
        output.println("<master>"+info);
    }

    public void commandProcess(String cmd) {
        log.warn(cmd);
        String result = "";
        if (cmd.startsWith("<client>")) {
            // 去掉前缀之后开始处理
            result = clientProcessor.processClientCommand(cmd.substring(8));
        } else if (cmd.startsWith("<region>")) {
            result = regionProcessor.processRegionCommand(cmd.substring(8));
        }
        if(!result.equals(""))
            this.sendToRegion(result);
    }
}