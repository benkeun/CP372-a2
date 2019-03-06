import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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

public class Sender extends JFrame implements ActionListener {
    static JPanel connectPanel = new JPanel();
    static JPanel clientPanel = new JPanel();
    static JButton connectButton = new JButton("CONNECT");
    static JButton disconnectButton = new JButton("DISCONNECT");
    static JButton transferButton = new JButton("TRANSFER");
    static JLabel IPLabel = new JLabel("IP Address");
    static JLabel ackPortLabel = new JLabel("ACK Port Number");
    static JLabel dataPortLabel = new JLabel("Data Port Number");
    static JLabel fileNameLabel = new JLabel("File Name");
    static JLabel mdsLabel = new JLabel("Max Datagram Size");
    static JLabel timeoutLabel = new JLabel("Timeout (ms)");
    static JTextField IPField = new JTextField("127.0.0.1");
    static JTextField ackPortField = new JTextField("400");
    static JTextField dataPortField = new JTextField("401");
    static JTextField fileNameField = new JTextField("Sender.java");
    static JTextField mdsField = new JTextField("1024");
    static JTextField timeoutField = new JTextField("500");
    static JTextArea dataArea = new JTextArea("");
    static File file_name;
    static double num_byte;
    static int mds;
    static int num_seq;
    static byte[][] packages;
    static int acknowledged[];
    static int portIn;
    static int endPort;
    static InetAddress endIP;
    static DatagramSocket sock;
    static FileInputStream fileIn;
    static int timeout;
    static long totalTime = 0;
    static Thread out;
    static boolean fileTransfering = false;
    static boolean connected=false;


    public Sender() {
        this.setLocation(200, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        connectPanelInit();
        connectButton.addActionListener(this);
        
        disconnectButton.addActionListener(this);
        transferButton.addActionListener(this);
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
        clientPanel.add(fileNameLabel);

        clientPanel.setVisible(true);

        dataArea.setBounds(10, 100, 250, 150);
        dataArea.setVisible(true);
        dataArea.setEditable(false);
        dataArea.setLineWrap(true);

        transferButton.setVisible(true);
        transferButton.setBounds(280, 180, 170, 30);

        fileNameLabel.setVisible(true);
        fileNameLabel.setBounds(10, 10, 110, 20);

        fileNameField.setVisible(true);
        fileNameField.setBounds(10, 30, 110, 20);

        mdsLabel.setVisible(true);
        mdsLabel.setBounds(280, 10, 110, 20);

        mdsField.setVisible(true);
        mdsField.setBounds(280, 30, 110, 20);

        timeoutLabel.setVisible(true);
        timeoutLabel.setBounds(280, 60, 110, 20);

        timeoutField.setVisible(true);
        timeoutField.setBounds(280, 80, 110, 20);

        disconnectButton.setVisible(true);
        disconnectButton.setBounds(280, 220, 170, 30);

        
    }

    public void outThread() {
        out = new Thread() {
            public void run() {
                try {
                    if (connected){
                    byte[] handshake = (Integer.toString(mds + 4) + " " + Integer.toString(num_seq) + " "
                            + Integer.toString((int) (((num_seq) * (mds + 4)) - num_byte)) + " ").getBytes();
                    DatagramPacket hand = new DatagramPacket(handshake, handshake.length);
                    sock.send(hand);
                    System.out.println(new String(handshake));
                    byte[] bufr = new byte[4]; // 2^8
                    DatagramPacket p = new DatagramPacket(bufr, 4);
                    sock.receive(p);
                    fileTransfering = true;
                    int i = 0;
                    while (fileTransfering) {
                        fileTransfering = false;
                        try {
                            byte[] send = new byte[4 + mds];
                            byte[] result = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8),
                                    (byte) i };

                            System.arraycopy(result, 0, send, 0, 4);
                            System.arraycopy(packages[i], 0, send, 4, packages[i].length);

                            DatagramPacket n = new DatagramPacket(send, send.length);
                            sock.send(n);

                            byte[] buf = new byte[mds];
                            DatagramPacket ack = new DatagramPacket(buf, 4);
                            sock.receive(ack);
                            int seq = java.nio.ByteBuffer.wrap(buf).getInt();
                            if (i == seq) {
                                acknowledged[seq] = 1;
                                i++;
                            }
                        } catch (SocketTimeoutException h) {
                            System.out.println(h);
                        }
                        for (int j = 0; j < acknowledged.length; j++) {
                            if (acknowledged[j] == 0) {
                                fileTransfering = true;
                            }
                        }

                    }
                    byte b[] = new byte[4];

                    ByteBuffer buf = ByteBuffer.wrap(b);
                    buf.putInt(-1);
                    DatagramPacket done = new DatagramPacket(b, 4);
                    sock.send(done);
                    totalTime = System.currentTimeMillis() - totalTime;
                    dataArea.setText(String.format("It took %d ms to transfer the file.", totalTime));

                }
                } catch (Exception e) {
                    dataArea.setText(e.getMessage());
                }
                
                return;
            }
        };
    }

    public static void main(String[] args) {
        new Sender();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String action = e.getActionCommand();
            if (action.equals("CONNECT")) {
                if (!connected){
                portIn = Integer.parseInt(dataPortField.getText());
                endPort = Integer.parseInt(ackPortField.getText());
                endIP = InetAddress.getByName(IPField.getText());
                connected=true;
                sock = new DatagramSocket(portIn);
                sock.connect(endIP, endPort);
                connectPanel.setVisible(false);
                clientPanelInit();
                }

            } else if (action.equals("TRANSFER")) {
                if (!fileTransfering){
                totalTime = System.currentTimeMillis();
                file_name = new File(fileNameField.getText());
                fileIn = new FileInputStream(file_name);

                num_byte = file_name.length();
                mds = Integer.parseInt(mdsField.getText());
                num_seq = (int) Math.ceil(num_byte / mds);
                timeout = Integer.parseInt(timeoutField.getText());
                sock.setSoTimeout(timeout);

                packages = new byte[num_seq][mds];
                acknowledged = new int[num_seq];
                for (int i = 0; i < num_seq; i++) {
                    acknowledged[i] = 0;
                    fileIn.read(packages[i]);
                }
                outThread();
                out.start();
            }
            } else if (action.equals("DISCONNECT")) {
                sock.close();
                clientPanel.setVisible(false);
                connectPanelInit();
                connected=false;
            }
        } catch (Exception ae) {
            dataArea.setText(ae.getMessage());
        }
    }
}