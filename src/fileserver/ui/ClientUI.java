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
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;

public class ClientUI implements Client.Callback {
    private JPanel contentPanel;
    private JTable tblFileList;
    private JButton btnHome;
    private JButton btnBack;
    private JLabel lblCurrentPath;
    private JProgressBar progressBarLoading;
    private JLabel lblLoading;
    private JButton btnCngIP;
    private JTextField textFieldIP;
    private JTextField textFieldPort;
    private JButton btnCngPort;

    private DefaultTableModel tableModel;
    private Client socketClient;

    public ClientUI() {
        try {
            socketClient = new Client(this,3599, InetAddress.getByName("localhost"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println("Failed to create Client");
            //todo msg
            System.exit(1);
        }

        lblLoading.setText("Not Transfer in progress.");
        progressBarLoading.setMinimum(0);
        progressBarLoading.setMaximum(100);
        progressBarLoading.setValue(0);

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
            }
        });
        btnCngIP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    socketClient.setServerIp(InetAddress.getByAddress(textFieldIP.getText().getBytes(StandardCharsets.UTF_8)));
                    updateFileList();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        btnCngPort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                socketClient.setServerPort(Integer.parseInt(textFieldPort.getText()));
                updateFileList();
            }
        });
    }

    private void showPopup(MouseEvent mouseEvent) {
        if(mouseEvent.isPopupTrigger()){
            JTable jTable = (JTable) mouseEvent.getSource();
            int row = jTable.rowAtPoint(mouseEvent.getPoint());
            jTable.setRowSelectionInterval(row,row);

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem item ;
            if(jTable.getValueAt(row,0).equals("true")){
                item = new JMenuItem("Open");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            socketClient.changeDirectory((String) jTable.getValueAt(row,1));
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Error occurred while changing directory.");
                            //todo msg
                        }
                        updateFileList();
                    }
                });
                popupMenu.add(item);
            }
            else{
                item = new JMenuItem("Download");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {

                        downloadFile((String)jTable.getValueAt(row,1));
                    }
                });
                popupMenu.add(item);
            }
            item = new JMenuItem("Upload");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    uploadFile();
                }
            });
            popupMenu.add(item);

            item = new JMenuItem("Refresh");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    updateFileList();
                }
            });
            popupMenu.add(item);

            if(!socketClient.getCurrentDirectory().equals("")){
                item = new JMenuItem("Back");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            socketClient.changeDirectory("../");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Error occurred while changing directory.");
                            //todo msg
                        }
                        updateFileList();
                    }
                });
                popupMenu.add(item);
            }

            popupMenu.show(mouseEvent.getComponent(),mouseEvent.getX(),mouseEvent.getY());
        }
    }

    private void downloadFile(String filename) {
        if(socketClient.setLoading(true)){
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Select save location");
            int userSelection = jFileChooser.showSaveDialog(tblFileList);
            if(userSelection==JFileChooser.APPROVE_OPTION){
                File file =  jFileChooser.getSelectedFile();

                try {
                    socketClient.downloadFile(file,filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            System.out.println("A file is already being downloaded or uploaded");
        }
    }

    private void uploadFile(){
        if(socketClient.setLoading(true)){
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Select a File to upload");
            int userSelection = jFileChooser.showDialog(tblFileList,"Upload");
            if(userSelection==JFileChooser.APPROVE_OPTION) {
                File file = jFileChooser.getSelectedFile();

                try {
                    socketClient.uploadFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    //todo failed file uploading
                }
            }
        }
        else{
            System.out.println("A file is already being downloaded or uploaded");
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

    @Override
    public void onUploadStart(String filename) {
        lblLoading.setText("Uploading: "+ filename);
        progressBarLoading.setValue(0);
    }

    @Override
    public void onDownloadStart(String filename) {
        lblLoading.setText("Downloading: "+ filename);
        progressBarLoading.setValue(0);
    }

    @Override
    public void onDownloadComplete(String filename) {
        lblLoading.setText("Download Complete: "+ filename);
        socketClient.setLoading(false);
    }

    @Override
    public void onDownloadError(String errorMsg) {

    }

    @Override
    public void updateProgress(long progress) {
        progressBarLoading.setValue(Math.toIntExact(progress));
    }

    @Override
    public void onUploadComplete(String filename) {
        lblLoading.setText("Upload Complete: "+ filename);
        socketClient.setLoading(false);
        updateFileList();
    }

    @Override
    public void onUploadError(String errorMsg) {

    }
}
