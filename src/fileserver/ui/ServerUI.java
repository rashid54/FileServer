package fileserver.ui;

import fileserver.Server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerUI {
    private final Server socketServer;

    private JPanel contentPanel;
    private JButton btnStart;
    private JButton btnStop;

    public ServerUI() {
        socketServer = new Server();

        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                socketServer.startServer();
            }
        });
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                socketServer.stopServer();
            }
        });
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame();
        jFrame.setContentPane(new ServerUI().contentPanel);
        jFrame.setSize(600,600);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }
}
