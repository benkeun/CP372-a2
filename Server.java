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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.*;

public class Server extends JFrame implements ActionListener {
    static JPanel connectPanel = new JPanel();
    static JPanel clientPanel = new JPanel();
    static JButton connectButton = new JButton("CONNECT");
    static JButton disconnectButton = new JButton("DISCONNECT");
    static JButton transferButton = new JButton("TRANSFER");
    static JLabel IPLabel = new JLabel("IP Address");
    static JLabel ackPortLabel = new JLabel("ACK Port Number");
    static JLabel dataPortLabel = new JLabel("Data Port Number");
    static JLabel fileNameLabel = new JLabel("File Name");
    static JTextField IPField = new JTextField("127.0.0.1");
    static JTextField ackPortField = new JTextField("400");
    static JTextField dataPortField = new JTextField("401");
    static JTextField fileNameField = new JTextField("Server.java");
    static JTextArea dataArea = new JTextArea("");
    static JTextField mdsField = new JTextField("1024");
    static JLabel mdsLabel = new JLabel("Max Datagram Size");
    static JTextField timeoutField = new JTextField("500");
    static JLabel timeoutLabel = new JLabel("Timeout (ms)");
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

    public Server() {
        this.setLocation(200, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        connectPanelInit();
    }

    public void connectPanelInit() {
        add(connectPanel);
        connectPanel.setLayout(null);
        this.setSize(300, 200);
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

        IPLabel.setBounds(130, 10, 110, 20);
        IPLabel.setVisible(true);

        ackPortLabel.setBounds(20, 10, 110, 20);
        ackPortLabel.setVisible(true);

        dataPortLabel.setBounds(20, 60, 110, 20);
        dataPortLabel.setVisible(true);

        connectButton.addActionListener(this);
    }

    public void clientPanelInit() throws Exception {
        add(clientPanel);
        clientPanel.setLayout(null);
        this.setSize(475, 300);
        clientPanel.setSize(475, 300);
        this.setTitle("Client");

        clientPanel.setLayout(null);
        clientPanel.add(disconnectButton);
        clientPanel.add(transferButton);
        clientPanel.add(dataArea);
        clientPanel.add(fileNameField);
        clientPanel.add(mdsField);
        clientPanel.add(mdsLabel);
        clientPanel.add(timeoutField);
        clientPanel.add(timeoutLabel);

        clientPanel.setVisible(true);

        dataArea.setBounds(10, 100, 250, 150);
        dataArea.setVisible(true);
        dataArea.setEditable(false);
        dataArea.setLineWrap(true);

        fileNameField.setVisible(true);
        fileNameField.setBounds(10, 10, 80, 30);

        transferButton.setVisible(true);
        transferButton.setBounds(100, 10, 160, 30);

        mdsLabel.setVisible(true);
        mdsLabel.setBounds(280, 10, 110, 20);

        mdsField.setVisible(true);
        mdsField.setBounds(280, 30, 110, 20);

        timeoutLabel.setVisible(true);
        timeoutLabel.setBounds(280, 60, 110, 20);

        timeoutField.setVisible(true);
        timeoutField.setBounds(280, 80, 110, 20);



        disconnectButton.setBounds(280, 120, 170, 30);
        disconnectButton.setVisible(true);

        disconnectButton.addActionListener(this);
        transferButton.addActionListener(this);
    }

    static Thread out = new Thread() {
        public void run() {
            try {
                byte[] handshake = (Integer.toString(mds + 4) + " " + Integer.toString(num_seq)).getBytes();
                DatagramPacket hand = new DatagramPacket(handshake, handshake.length);
                sock.send(hand);
                boolean fileTransfering = true;
                while (acknowleged[num_seq - 1] != 1) {
                    try{
                    int i = 0;

                    byte[] send = new byte[4 + mds];
                    byte[] result = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i };

                    System.arraycopy(result, 0, send, 0, 4);
                    System.arraycopy(packages[i], 0, send, 4, mds);
                    DatagramPacket n = new DatagramPacket(send, send.length);
                    sock.send(n);

                    fileTransfering = true;

                    byte[] buf = new byte[mds];
                    DatagramPacket ack = new DatagramPacket(buf, 4);
                    sock.receive(ack);
                    int seq = java.nio.ByteBuffer.wrap(buf).getInt();
                    acknowleged[seq] = 1;
                    i++;
                    }
                    catch(SocketTimeoutException h){
                        System.out.println(h);
                    }

                }

            } catch (Exception e) {
                System.out.println(e );
            }
        }
    };

    public static void main(String[] args) {
        new Server();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String action = e.getActionCommand();
            if (action.equals("CONNECT")) {
                portIn = Integer.parseInt(dataPortField.getText());
                endPort = Integer.parseInt(ackPortField.getText());
                endIP = InetAddress.getByName(IPField.getText());

                sock = new DatagramSocket(portIn);
                sock.connect(endIP, endPort);
                connectPanel.setVisible(false);
                clientPanelInit();

            } else if (action.equals("TRANSFER")) {
                file_name = new File(fileNameField.getText());
                fileIn = new FileInputStream(file_name);
                num_byte = file_name.length();
                mds = Integer.parseInt(mdsField.getText());
                num_seq = (int) Math.ceil(num_byte / mds);
                timeout = Integer.parseInt(timeoutField.getText());
                sock.setSoTimeout(timeout);
                packages = new byte[num_seq][mds];
                acknowleged = new int[num_seq];
                for (int i = 0; i < num_seq; i++) {
                    acknowleged[i] = 0;
                    fileIn.read(packages[i]);
                }
                out.start();

            }
        } catch (Exception ae) {
            System.out.println(ae);
        }
    }
}
