import java.io.File;
import java.io.FileInputStream;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.*;

public class Server extends JFrame implements ActionListener {
    static JPanel connectPanel = new JPanel();
    static JPanel clientPanel = new JPanel();
    static JButton connectButton = new JButton("CONNECT");
    static JButton disconnectButton = new JButton("DISCONNECT");
    static JLabel IPLabel = new JLabel("IP Address");
    static JLabel ackPortLabel = new JLabel("ACK Port Number");
    static JLabel dataPortLabel = new JLabel("Data Port Number");
    static JLabel fileNameLabel = new JLabel("File Name");
    static JTextField IPField = new JTextField("");
    static JTextField ackPortField = new JTextField("");
    static JTextField dataPortField = new JTextField("");
    static JTextField fileNameField = new JTextField("");
    static JTextArea dataArea = new JTextArea("");
    static File file_name;
    static double num_byte;
    static int mds = 1024;
    static int num_seq;
    static byte[][] packages;
    static int acknowleged[];
    static int portIn;
    static int endPort;
    static InetAddress endIP;
    static DatagramSocket sock;
    static FileInputStream fileIn;
    static int timeout;

    public Server(){
        this.setLocation(200, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        connectPanelInit();
    }
    public void connectPanelInit() {
        add(connectPanel);
        connectPanel.setLayout(null);
        this.setSize(270, 170);
        this.setTitle("Connect");

        connectPanel.add(connectButton);
        connectPanel.add(IPLabel);
        connectPanel.add(ackPortLabel);
        connectPanel.add(dataPortLabel);
        connectPanel.add(IPField);
        connectPanel.add(ackPortField);
        connectPanel.add(dataPortField);
        connectPanel.setVisible(true);

        connectButton.setBounds(130, 80, 100, 25);
        connectButton.setVisible(true);

        IPField.setBounds(130, 30, 100, 25);
        IPField.setVisible(true);

        ackPortField.setBounds(20, 30, 100, 25);
        ackPortField.setVisible(true);

        dataPortField.setBounds(20, 80, 100, 25);
        dataPortField.setVisible(true);

        IPLabel.setBounds(130, 10, 80, 20);
        IPLabel.setVisible(true);

        ackPortLabel.setBounds(20, 10, 80, 20);
        ackPortLabel.setVisible(true);

        dataPortLabel.setBounds(20, 60, 80, 20);
        dataPortLabel.setVisible(true);

        connectButton.addActionListener(this);
    }

    public void clientPanelInit() throws Exception {
        add(clientPanel);
        clientPanel.setLayout(null);
        this.setSize(575, 400);
        this.setTitle("Client");

        clientPanel.setLayout(null);
        clientPanel.add(disconnectButton);
        clientPanel.setVisible(true);

        clientPanel.add(dataArea);
        dataArea.setBounds(10, 200, 350, 150);
        dataArea.setVisible(true);
        dataArea.setEditable(false);
        dataArea.setLineWrap(true);

        clientPanel.add(fileNameField);
        fileNameField.setVisible(true);
        fileNameField.setBounds();
        
        disconnectButton.setBounds(380, 320, 170, 30);
        disconnectButton.setVisible(true);

        disconnectButton.addActionListener(this);
    }

    static Thread in = new Thread() {
        public void run() {
            try {
                boolean fileTransfering = true;
                while (fileTransfering) {
                    fileTransfering = false;
                    for (int i = 0; i < num_seq; i++) {
                        if (acknowleged[i] != 1) {

                            byte[] send = new byte[4 + mds];
                            byte[] result = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8),
                                    (byte) i };

                            System.arraycopy(result, 0, send, 0, 4);
                            System.arraycopy(packages[i], 0, send, 4, mds);
                            DatagramPacket n = new DatagramPacket(send, send.length);
                            sock.send(n);

                            ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(send, 0, 4)); // big-endian by
                                                                                                  // default
                            int num = wrapped.getInt();
                            System.out.println(num);
                            System.out.println(new String(Arrays.copyOfRange(send, 4, send.length)));
                            fileTransfering = true;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e + "here");
            }
        }
    };
    static Thread out = new Thread() {
        public void run() {
            try {
                while (true) {
                    byte[] buf = new byte[mds];
                    DatagramPacket n = new DatagramPacket(buf, mds);
                    sock.receive(n);
                    int seq = java.nio.ByteBuffer.wrap(buf).getInt();
                    acknowleged[seq] = 1;
                    System.out.println(new String(buf));
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }

    };

    public static void main(String[] args) {
        new Server();
        try {
            for (int i = 0; i < num_seq; i++) {
                acknowleged[i] = 0;
                fileIn.read(packages[i]);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try{
        String action = e.getActionCommand();
        if (action.equals("CONNECT")){
        portIn = Integer.parseInt(dataPortField.getText());
        endPort = Integer.parseInt(ackPortField.getText());
        endIP = InetAddress.getByName(IPField.getText());
        sock = new DatagramSocket(portIn);
        fileIn = new FileInputStream(file_name);
        sock.connect(endIP, endPort);
        connectPanel.setVisible(false);
        clientPanelInit();
        }
        else if (action.equals("TRANSFER")){
            file_name = new File("Server.java");
            num_byte = file_name.length();
            mds = 1024;
            num_seq = (int) Math.ceil(num_byte / mds);
            packages = new byte[num_seq][mds];
            acknowleged = new int[num_seq];
            in.run();
            out.run();
        }
    }catch(Exception ae){
        System.out.println(ae);
    }
    }
}
