package fileserver;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    Callback callback;
    private int serverPort = 3599;
    private InetAddress serverIp;
    private Socket socket;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private String currentDirectory;

    private boolean LOADING = false;
    private File loadFile;

    public Client(Callback callback, int serverPort, InetAddress serverIp) {
        this.callback = callback;
        this.serverPort = serverPort;
        this.serverIp = serverIp;
        currentDirectory = "";
    }

    public synchronized boolean setLoading(boolean value){
        if(LOADING == true){
            return false;
        }
        LOADING = value;
        return true;
    }

    public boolean getLoading(){
        return LOADING;
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

        if(foldername == null){
            foldername = "";
        }
        else{
            foldername = currentDirectory + File.separator + foldername;
        }
        dataOutputStream.writeUTF(foldername);
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

    public void uploadFile(File file) throws IOException {
        loadFile = file;
        connectToServer();
        dataOutputStream.writeUTF(Server.UPLOAD_FILE);
        dataOutputStream.writeUTF(currentDirectory+File.separator+file.getName());
        dataOutputStream.writeUTF(String.valueOf(file.length()));

        String received = dataInputStream.readUTF();
        if(received.equals(Server.YES)){
            Thread uploadTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(callback!=null){
                            callback.onUploadStart(loadFile.getName());
                        }
                        FileInputStream fileInputStream = new FileInputStream(loadFile);
                        long filesize = loadFile.length();
                        byte[] buffer = new byte[4096];

                        int read = 0;
                        while((read = fileInputStream.read(buffer))>0){
                            dataOutputStream.write(buffer,0,read);
                            if(callback!=null){
                                callback.updateProgress((read*100)/filesize);
                            }
                            System.out.println("uploading "+(read*100)/filesize);
                        }
                        dataOutputStream.writeUTF(Server.YES);
                        if(callback != null){
                            callback.onUploadComplete(loadFile.getName());
                        }
                        disconnectFromServer();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        if(callback!=null){
                            callback.onUploadError("File not found");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(callback!=null){
                            callback.onUploadError("Uploading file failed");
                        }
                    }
                }
            });
            uploadTask.start();
        }
        else{
            disconnectFromServer();
            if(callback!=null){
                callback.onUploadError("Failed to upload file.");
            }
        }
        return;
    }

    public interface Callback{

        void updateProgress(long progress);

        void onUploadComplete(String filename);

        void onUploadError(String errorMsg);

        void onUploadStart(String filename);
    }



}
