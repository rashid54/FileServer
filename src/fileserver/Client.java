package fileserver;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private int serverPort = 3599;
    private InetAddress serverIp;
    private Socket socket;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private String currentDirectory;

    public Client(int serverPort, InetAddress serverIp) {
        this.serverPort = serverPort;
        this.serverIp = serverIp;
        currentDirectory = "";
    }

    public boolean connectToServer() throws IOException {
        socket = new Socket(serverIp,serverPort);
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        String received = dataInputStream.readUTF();
        if(received.equals(Server.CONNECTED)){
            System.out.println("connection: true");
            return true;
        }
        return false;
    }

    public boolean disconnectFromServer(){
        try {
            dataOutputStream.writeUTF(Server.DISCONNECTED);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getFilelist() throws IOException {
        connectToServer();
        dataOutputStream.writeUTF(Server.FILE_LIST);
        dataOutputStream.writeUTF(currentDirectory);
        String received = dataInputStream.readUTF();
        disconnectFromServer();
        return received;
    }

    public String changeDirectory(String foldername) throws IOException {
        connectToServer();
        dataOutputStream.writeUTF(Server.CHANGE_DIRECTORY);
        dataOutputStream.writeUTF(currentDirectory + File.separator + foldername);
        String received = dataInputStream.readUTF();
        disconnectFromServer();
        if(received.equals(Server.NO)){
            System.out.println("Failed to change directory.");
        }
        else{
            currentDirectory = received;
        }
        return getFilelist();
    }

    public String getCurrentDirectory(){
        return currentDirectory;
    }

    public boolean downloadFile(File fileDestination, String filename) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(fileDestination);
        connectToServer();
        dataOutputStream.writeUTF(Server.DOWNLOAD_FILE);
        dataOutputStream.writeUTF(currentDirectory+File.separator+filename);
        String received = dataInputStream.readUTF();
        if(received.equals(Server.YES)){
            long fileSize = Long.parseLong(dataInputStream.readUTF());
            int read = 0;
            long remaining = fileSize;
            byte[] buffer = new byte[4096];

            if((fileDestination.getFreeSpace()-fileSize)>10240){
                dataOutputStream.writeUTF(Server.YES);
            }
            else{
                dataOutputStream.writeUTF(Server.NO);
                disconnectFromServer();
                return false;
            }

            while((read = dataInputStream.read(buffer,0, Math.toIntExact(Math.min(buffer.length, remaining)))) > 0){
                remaining -= read;
                fileOutputStream.write(buffer,0,read);
                System.out.println("Downloaded ===> "+((fileSize-remaining)*100/fileSize)+"%");
            }
            received = dataInputStream.readUTF();
            if(received.equals(Server.YES)){
                System.out.println("Download Complete.");
                disconnectFromServer();
                return true;
            }
        }
        disconnectFromServer();
        return false;
    }

    public boolean uploadFile(File file) throws IOException {
        connectToServer();
        dataOutputStream.writeUTF(Server.UPLOAD_FILE);
        dataOutputStream.writeUTF(currentDirectory+File.separator+file.getName());
        dataOutputStream.writeUTF(String.valueOf(file.length()));

        String received = dataInputStream.readUTF();
        if(received.equals(Server.YES)){
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];

            int read = 0;
            while((read = fileInputStream.read(buffer))>0){
                dataOutputStream.write(buffer,0,read);
            }
            dataOutputStream.writeUTF(Server.YES);
            disconnectFromServer();
            return true;
        }
        disconnectFromServer();
        return false;
    }
}
