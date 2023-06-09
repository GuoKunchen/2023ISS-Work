package RegionManagers.SocketManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import lombok.SneakyThrows;
import miniSQL.API;
import miniSQL.Interpreter;
import RegionManagers.DataBaseManager;

// 负责和主节点进行通信的类
public class MasterSocketManager implements Runnable {
    private Socket socket;
    private BufferedReader input = null;
    private PrintWriter output = null;
    private FtpUtils ftpUtils;
    private DataBaseManager dataBaseManager;
    private boolean isRunning = false;

    private int SERVER_PORT = 12345;
    private String MASTER = "192.168.43.125";

    public MasterSocketManager(String Master,int Server_Port) throws IOException {
		this.MASTER=Master;
		this.SERVER_PORT=Server_Port;
		System.out.println("Socket服务启动,当前连接的Master服务器ip:port为:"+this.MASTER+":"+this.SERVER_PORT);
        this.socket = new Socket(MASTER, SERVER_PORT);
        this.ftpUtils = new FtpUtils();
        this.dataBaseManager = new DataBaseManager();
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        isRunning = true;

		//FTP登录测试
		if(!ftpUtils.login_test())
			System.out.println("测试环节>>>FTP连接测试失败,请检查IP和端口是否正确");
    }

    public void sendToMaster(String modified_info) {
        output.println(modified_info);
    }

    public void sendTableInfoToMaster(String table_info) {
        output.println("<region>[1]" + table_info);
    }

    public void receiveFromMaster() throws IOException {
        String line = null;
        if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
            System.out.println("新消息>>>Socket已经关闭!");
        } else {
            line = input.readLine();
			System.out.println(line);
        }
        if (line != null) {
            if (line.startsWith("<master>[3]")) {
                String info = line.substring(11);
                if(line.length()==11) return;
                String[] tables = info.split("#")[1].split("@");
                // <master[3]>ip#name@name@...
				boolean success=false;
                for(String table : tables) {
                    delFile(table);
                    delFile(table + "_index.index");
                    success=ftpUtils.downLoadFile("table", table, "");
                    System.out.println(success + table);
                    success=ftpUtils.downLoadFile("index", table + "_index.index", "");
                    System.out.println(success + table + "_index.index");
                }
                String ip = info.split("#")[0];
                success=ftpUtils.additionalDownloadFile("catalog", ip + "#table_catalog");
				System.out.println(success + " " + ip + "#table_catalog");
                success=ftpUtils.additionalDownloadFile("catalog", ip + "#index_catalog");
				System.out.println(success + " " + ip + "#index_catalog");
                try {
                    API.initial();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("here");
                output.println("<region>[3]Complete disaster recovery");
            }
            else if (line.equals("<master>[4]recover")) {
                String tableName = dataBaseManager.getMetaInfo();
                String[] tableNames = tableName.split(" ");
                for(String table: tableNames) {
                    Interpreter.interpret("drop table " + table + " ;");
                    try {
                        API.store();
                        API.initial();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                output.println("<master>[4]Online");
            }
        }
    }

    public void delFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) file.delete();
    }

    @Override
    public void run() {
        System.out.println("新消息>>>从节点的主服务器监听线程启动！");
        while (isRunning) {
            if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
                isRunning = false;
                break;
            }
            try {
                receiveFromMaster();
            }catch (SocketException e){
				System.out.println("与Master的连接终止");
				break;
			}
			 catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException | NullPointerException e) {
                e.printStackTrace();
            }
        }
		System.out.println("主服务器监听线程停止运行");
    }
}
