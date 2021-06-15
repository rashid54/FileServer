package fileserver.ui;

import fileserver.Server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerUI implements Server.Callback{
    private final Server socketServer;

    private JPanel contentPanel;
    private JButton btnStart;
    private JButton btnStop;
    private JButton btnClose;
    private JButton btnChangePort;
    private JButton btnChangeRoot;
    private JTextPane msgBox;
    private JTextField textField1;
    private JTextField textField2;

    public ServerUI() {
        socketServer = new Server(this);

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
        btnChangePort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //todo Change port
            }
        });
        btnChangeRoot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //todo Change root directory
            }
        });
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });
        btnChangePort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                socketServer.stopServer();
                socketServer.setServerPort(Integer.parseInt(textField1.getText()));
            }
        });
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame();
        //todo set title
        jFrame.setContentPane(new ServerUI().contentPanel);
        jFrame.setResizable(false);
        jFrame.setSize(600,600);//todo set size
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }

    @Override
    public void printMsg(String msg) {
//todo msgbox update
    }
}
