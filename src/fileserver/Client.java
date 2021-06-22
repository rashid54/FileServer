package fileserver;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    Callback callback;
    private int serverPort = 3599;
    private InetAddress serverIp;

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
        if(value && getLoading()){
            return false;
        }
        LOADING = value;
        return true;
    }

    public boolean getLoading(){
        return LOADING;
    }

//    public boolean connectToServer() throws IOException {
//        socket = new Socket(serverIp,serverPort);
//        dataInputStream = new DataInputStream(socket.getInputStream());
//        dataOutputStream = new DataOutputStream(socket.getOutputStream());
//        String received = dataInputStream.readUTF();
//        if(received.equals(Server.CONNECTED)){
//            System.out.println("connection: true");
//            return true;
//        }
//        return false;
//    }
//
//    public boolean disconnectFromServer(){
//        try {
//            dataOutputStream.writeUTF(Server.DISCONNECTED);
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }

    public String getFilelist() throws IOException {
        Socket socket = new Socket(serverIp,serverPort);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        dataOutputStream.writeUTF(Server.FILE_LIST);
        dataOutputStream.writeUTF(currentDirectory);
        String received = dataInputStream.readUTF();
        System.out.println(received);

        return received;
    }

    public String changeDirectory(String foldername) throws IOException {
        Socket socket = new Socket(serverIp,serverPort);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        dataOutputStream.writeUTF(Server.CHANGE_DIRECTORY);

        if(foldername == null){
            foldername = "";
        }
        else{
            foldername = currentDirectory + File.separator + foldername;
        }
        dataOutputStream.writeUTF(foldername);
        String received = dataInputStream.readUTF();

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
        Thread downloadTask = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(serverIp,serverPort);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                    FileOutputStream fileOutputStream = new FileOutputStream(fileDestination);

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
                            return ;
                        }

                        if(callback!=null){
                            callback.onDownloadStart(filename);
                        }

                        while((read = dataInputStream.read(buffer,0, Math.toIntExact(Math.min(buffer.length, remaining)))) > 0){
                            remaining -= read;
                            fileOutputStream.write(buffer,0,read);
                            System.out.println("Downloaded ===> "+((fileSize-remaining)*100/fileSize)+"%");
                            if(callback!=null){
                                callback.updateProgress((fileSize-remaining)*100/fileSize);
                            }
                        }
                        received = dataInputStream.readUTF();
                        if(received.equals(Server.YES)){
                            System.out.println("Download Complete.");
                            if(callback!=null){
                                callback.onDownloadComplete(filename);
                            }
                            return ;
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        downloadTask.start();

        return true;
    }

    public void uploadFile(File loadFile) throws IOException {
        Socket socket = new Socket(serverIp,serverPort);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        dataOutputStream.writeUTF(Server.UPLOAD_FILE);
        dataOutputStream.writeUTF(currentDirectory+File.separator+loadFile.getName());
        dataOutputStream.writeUTF(String.valueOf(loadFile.length()));

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

                        long totalRead = 0;
                        int read = 0;
                        while((read = fileInputStream.read(buffer))>0){
                            dataOutputStream.write(buffer,0,read);
                            totalRead += read;
                            if(callback!=null){
                                callback.updateProgress((totalRead*100)/filesize);
                            }
                            System.out.println("uploading "+(totalRead*100)/filesize);
                        }
                        dataOutputStream.writeUTF(Server.YES);
                        if(callback != null){
                            callback.onUploadComplete(loadFile.getName());
                        }
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

        void onDownloadStart(String filename);

        void onDownloadComplete(String filename);

        void onDownloadError(String errorMsg);
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public InetAddress getServerIp() {
        return serverIp;
    }

    public void setServerIp(InetAddress serverIp) {
        this.serverIp = serverIp;
    }
}
