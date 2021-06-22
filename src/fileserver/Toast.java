package fileserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Toast extends JDialog
{

    private static final long serialVersionUID = -9201199499095469781L;

    private static final int  DURATION         = 2500;

    private JFrame            gui;
    private String            message;

    private Toast() {
    }

    private Toast(final JFrame _gui, final String _msg) {
        super(_gui, false);
        gui = _gui;
        message = _msg;
        initComponents();
    }

    public static void displayToast(final JFrame _gui, final String _msg) {
        final JDialog dialog = new Toast(_gui, _msg);
        final Timer timer = new Timer(DURATION, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        setFocusableWindowState(false);
        setUndecorated(true);
        setSize(new Dimension(600, 50));
        setLocationRelativeTo(gui);
        getContentPane().setBackground(Color.BLACK);

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        final boolean isTranslucencySupported = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);

        if (isTranslucencySupported) {
            setOpacity(0.5f);
        }

        final JLabel label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setText(message);
        add(label);
    }
}