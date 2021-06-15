package fileserver.ui;

import com.google.gson.Gson;
import fileserver.Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;

public class ClientUI {
    private JPanel contentPanel;
    private JTable tblFileList;
    private JButton btnHome;
    private JButton btnBack;
    private JLabel lblCurrentPath;

    private DefaultTableModel tableModel;
    private Client socketClient;

    public ClientUI() {
        try {
            socketClient = new Client(3599, InetAddress.getByName("localhost"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println("Failed to create Client");
            //todo msg
            System.exit(1);
        }

        tableModel = new DefaultTableModel(new String[]{"Folder", "Name", "Size", "Last Modified"},0){
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };

        tblFileList.setModel(tableModel);
        tblFileList.setRowSelectionAllowed(true);
        tblFileList.setColumnSelectionAllowed(false);
        tblFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblFileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if(mouseEvent.getButton()==MouseEvent.BUTTON1 && mouseEvent.getClickCount() == 2){
                    JTable jTable = (JTable) mouseEvent.getSource();
                    int row = jTable.getSelectedRow();
                    try {
                        socketClient.changeDirectory((String) jTable.getValueAt(row,1));
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error occurred while changing directory.");
                        //todo msg
                    }
                    updateFileList();
                }
                showPopup(mouseEvent);
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                showPopup(mouseEvent);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                showPopup(mouseEvent);
            }
        });

        btnHome.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    socketClient.changeDirectory(null);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error occurred while changing directory to home.");
                }
                updateFileList();
//todo home button
            }
        });
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    socketClient.changeDirectory("../");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error occurred while changing to parent directory.");
                }
                updateFileList();
//todo back button
            }
        });
    }

    private void showPopup(MouseEvent mouseEvent) {
        if(mouseEvent.isPopupTrigger()){
            JTable jTable = (JTable) mouseEvent.getSource();
            int row = jTable.rowAtPoint(mouseEvent.getPoint());
            jTable.setRowSelectionInterval(row,row);

            JPopupMenu popupMenu = new JPopupMenu();
            if(jTable.getValueAt(row,0).equals("true")){
                popupMenu.add(new JMenuItem("Open"));
            }
            else{
                popupMenu.add(new JMenuItem("Download"));
            }
            popupMenu.add(new JMenuItem("Upload"));
            popupMenu.add(new JMenuItem("Refresh"));
            popupMenu.addSeparator();
            popupMenu.add(new JMenuItem("Back"));
            //todo only when not in home
            popupMenu.show(mouseEvent.getComponent(),mouseEvent.getX(),mouseEvent.getY());
        }
    }

    private void updateFileList(){
        String received = null;
        try {
            received = socketClient.getFilelist();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to get File List");
            //todo msg
        }
        File[] fileList = new Gson().fromJson(received, File[].class);

        tableModel = new DefaultTableModel(new String[]{"Folder", "Name", "Size", "Last Modified"},0){
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };

        tblFileList.setModel(tableModel);
        for(File file: fileList){
            tableModel.addRow(new String[]{
                    String.valueOf(file.isDirectory()),
                    file.getName(),
                    bytesIntoHumanReadable(file.length()),
                    DateFormat.getDateTimeInstance().format(new Date(file.lastModified()))
            });
        }
        lblCurrentPath.setText("Path: Home/"+socketClient.getCurrentDirectory());
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame();
        //todo set title
        jFrame.setContentPane(new ClientUI().contentPanel);
        jFrame.setResizable(false);
        jFrame.setSize(600,600);//todo set size
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }

    private String bytesIntoHumanReadable(long bytes) {
        long kilobyte = 1024;
        long megabyte = kilobyte * 1024;
        long gigabyte = megabyte * 1024;
        long terabyte = gigabyte * 1024;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + " B";

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return (bytes / kilobyte) + " KB";

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return (bytes / megabyte) + " MB";

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            String value = String.valueOf((bytes / gigabyte));
            bytes = bytes % gigabyte;
            if(bytes>=1024){
                value = value +"."+ (bytes/1024);
            }
            return value + " GB";

        } else if (bytes >= terabyte) {
            String value = String.valueOf((bytes / terabyte));
            bytes = bytes % terabyte;
            if(bytes>=1024){
                value = value +"."+ (bytes/1024);
            }
            return value + " TB";

        } else {
            return bytes + " Bytes";
        }
    }
}
