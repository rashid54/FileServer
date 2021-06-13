package fileserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final String FILE_LIST = "201";
    public static final String DOWNLOAD_FILE = "202";
    public static final String UPLOAD_FILE = "203";
    public static final String CHANGE_DIRECTORY = "204";
    public static final String CONNECTED = "251";
    public static final String DISCONNECTED = "252";
    public static final String YES = "253";
    public static final String NO = "254";

    private int serverPort;

    private ServerSocket serverSocket;

    private File rootDirectory;

    Thread serverMain;

    public Server() {
        serverPort = 3599;
        setRootDirectory(null);
    }

    /**
     * Sets root directory for the server to "ProgramFolder/ServerFiles"
     */
    private boolean setRootDirectory(File root){
        if(root == null){
            root = new File(System.getProperty("user.dir")+File.separator+"ServerFiles");
        }
        if(!root.exists()){
            rootDirectory.mkdirs();
        }
        try {
            rootDirectory = root.getCanonicalFile();
        } catch (IOException e) {
            System.out.println("Failed to set root directory.");
            e.printStackTrace();
        }
        System.out.println("Root Directory: "+rootDirectory.getAbsolutePath());
        return true;
    }

    public void startServer(){
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Started server at port: "+ serverPort);
        } catch (IOException e) {
            //todo handle exception
            e.printStackTrace();
        }

        serverMain = new Thread(new Runnable() {
            @Override
            public void run() {
                listenToScoket();
            }
        });

        serverMain.start();

    }

    public void stopServer(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileList(String path){
        File currentDirectory = new File(rootDirectory.getAbsolutePath()+File.separator+path);
        System.out.println(path);

        StringBuilder stringBuilder = new StringBuilder();
        for(File file:currentDirectory.listFiles()){
            stringBuilder
                    .append(file.getName())
                    .append("\n");
        }
        return stringBuilder.toString();
    }

    private String isDirectory(String path){
        File currentDirectory = new File(rootDirectory.getAbsolutePath()+File.separator+path);
        try {
            currentDirectory = currentDirectory.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(currentDirectory.getAbsolutePath().equals(rootDirectory.getAbsolutePath())){
            return "";
        }
        else if(currentDirectory.isDirectory()
                && currentDirectory.exists()
                && currentDirectory.getAbsolutePath().length()>rootDirectory.getAbsolutePath().length()
        ){
            System.out.println(currentDirectory.getPath());
            return currentDirectory.getAbsolutePath().substring(rootDirectory.getAbsolutePath().length()+1);
        }
        return Server.NO;
    }

    private void listenToScoket(){
        Socket socket = null;
        while(!serverSocket.isClosed()){
            try {
                socket = serverSocket.accept();
                System.out.println("Connected to a new client: "+ socket);

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(Server.CONNECTED);
                System.out.println("Connection established.");

                while(!socket.isClosed()){
                    String received = dis.readUTF();

                    File file;
                    switch (received){
                        case Server.FILE_LIST:
                            received = dis.readUTF();
                            dos.writeUTF(getFileList(received));
                            break;
                        case Server.CHANGE_DIRECTORY:
                            received = dis.readUTF();
                            dos.writeUTF(isDirectory(received));
                            break;
                        case Server.DOWNLOAD_FILE:
                            received = dis.readUTF();
                            file = new File(
                                    rootDirectory.getAbsolutePath()
                                            + File.separator
                                            + received
                            );
                            if(file.exists()&& !file.isDirectory()){
                                dos.writeUTF(Server.YES);
                                dos.writeUTF(String.valueOf(file.length()));
                                received = dis.readUTF();
                                if(received.equals(Server.YES)){
                                    FileInputStream fileInputStream = new FileInputStream(file);
                                    byte[] buffer = new byte[4096];

                                    int read = 0;
                                    while((read = fileInputStream.read(buffer))>0){
                                        dos.write(buffer,0,read);
                                    }
                                    dos.writeUTF(Server.YES);
                                }
                            }
                            break;
                        case Server.UPLOAD_FILE:
                            String filepath = rootDirectory.getAbsolutePath()+ File.separator+dis.readUTF();
                            long fileSize = Long.parseLong(dis.readUTF());
                            file = new File(filepath);
                            if((!file.exists())&& rootDirectory.getFreeSpace()>fileSize){
                                dos.writeUTF(Server.YES);

                                int read = 0;
                                long remaining = fileSize;
                                byte[] buffer = new byte[4096];
                                FileOutputStream fileOutputStream = new FileOutputStream(file);

                                while((read = dis.read(buffer,0,Math.toIntExact(Math.min(buffer.length,remaining)))) > 0){
                                    remaining -= read;
                                    fileOutputStream.write(buffer,0,read);
                                    System.out.println("Upload complete ===> "+((fileSize-remaining)*100/fileSize)+"%");
                                }
                                received = dis.readUTF();
                                if(received.equals(Server.YES)){
                                    System.out.println("Upload Complete.");
                                }
                            }
                            else{
                                dos.writeUTF(Server.NO);
                            }
                            break;
                        case Server.DISCONNECTED:
                            dis.close();
                            dos.close();
                            System.out.println("Disconnected from "+socket);
                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
