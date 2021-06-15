package fileserver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainClient {
    public static void main(String[] args) {
        try {
            Client client = new Client(null,3599, InetAddress.getByName("localhost"));
            System.out.println(client.getCurrentDirectory());

            File file1 = new File("/media/MEDIA/Music/Sea Shanty - Wellerman _ 9D AUDIO \uD83C\uDFA7 - YouTube.mkv");

            client.uploadFile(file1);
            System.out.println(client.getFilelist());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
