import java.io.File;
import java.io.FileInputStream;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import javax.swing.*;

public class Sender extends JFrame implements ActionListener {
    static JPanel connectPanel = new JPanel();
    static JPanel clientPanel = new JPanel();
    static JButton connectButton = new JButton("CONNECT");
    static JButton disconnectButton = new JButton("DISCONNECT");
    static JButton transferButton = new JButton("TRANSFER");
    static JButton cancelButton = new JButton("CANCEL");
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
    static int lastPack = 0;
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
    static int packLost = 0;
    static long totalTime = 0;
    static Thread out;
    static boolean fileTransfering = false;
    static boolean connected = false;
    static boolean cancel= false;

    public void outThread() {
        out = new Thread() {
            public void run() {
                try {
                    if (connected) {
                        
                        byte[] handshake = (Integer.toString(mds + 4) + " " + Integer.toString(num_seq) + " ")
                                .getBytes();
                        DatagramPacket hand = new DatagramPacket(handshake, handshake.length);
                        sock.send(hand);
                        byte[] bufr = new byte[4]; // 2^8
                        DatagramPacket p = new DatagramPacket(bufr, 4);
                        sock.receive(p);
                        fileTransfering = true;
                        int i = 0;
                        dataArea.setText("File is transferring");
                        while (fileTransfering&&!cancel) {
                            fileTransfering = false;
                            try {
                                DatagramPacket n;
                                if (i != num_seq - 1) {
                                    byte[] send = new byte[4 + mds];
                                    byte[] result = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8),
                                            (byte) i };

                                    System.arraycopy(result, 0, send, 0, 4);
                                    System.arraycopy(packages[i], 0, send, 4, packages[i].length);
                                    n = new DatagramPacket(send, send.length);
                                } else {
                                    byte[] send = new byte[4 + lastPack];
                                    byte[] result = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8),
                                            (byte) i };

                                    System.arraycopy(result, 0, send, 0, 4);
                                    System.arraycopy(packages[i], 0, send, 4, lastPack);
                                    n = new DatagramPacket(send, send.length);
                                }
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
                                packLost++;
                            }
                            for (int j = 0; j < acknowledged.length; j++) {
                                if (acknowledged[j] == 0 &&!cancel) {
                                    fileTransfering = true;
                                }
                            }

                        }
                        if(!cancel){
                        byte b[] = new byte[4];

                        ByteBuffer buf = ByteBuffer.wrap(b);
                        buf.putInt(-1);
                        DatagramPacket done = new DatagramPacket(b, 4);
                        sock.send(done);
                        totalTime = System.currentTimeMillis() - totalTime;
                        dataArea.setText(String.format("There were %d packets resent.\n", packLost)
                                + String.format("It took %d ms to succesfully transfer the file.\n", totalTime)
                                + String.format("The file was %.2f bytes or %.2fKB", num_byte, num_byte / 1024));
                        transferButton.setEnabled(true);
                        cancelButton.setEnabled(false);
                    }
                }
                } catch (Exception e) {
                    dataArea.setText(e.getMessage());
                    transferButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                }

                return;
            }
        };
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            dataArea.setText("");
            String action = e.getActionCommand();
            if (action.equals("CONNECT")) {
                if (!connected) {
                    portIn = Integer.parseInt(dataPortField.getText());
                    endPort = Integer.parseInt(ackPortField.getText());
                    endIP = InetAddress.getByName(IPField.getText());
                    connected = true;
                    sock = new DatagramSocket(portIn);
                    sock.connect(endIP, endPort);
                    connectPanel.setVisible(false);
                    clientPanelInit();
                    dataArea.setText("For best Results:\nSet MDS to at least .05% of your file size");
                }

            } else if (action.equals("TRANSFER")) {
                if (Integer.parseInt(mdsField.getText()) < 4) {
                    dataArea.setText("MDS must be at least 4 bytes.");
                } else if (Integer.parseInt(mdsField.getText()) > 65500) {
                    dataArea.setText("MDS cannot be more than 65500 bytes.");
                } else if (Integer.parseInt(timeoutField.getText()) < 25) {
                    dataArea.setText("Timeout must be at least 25ms.");
                } else if (!fileTransfering) {
                    
                    packLost = 0;
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
                        lastPack = fileIn.read(packages[i]);
                    }
                    transferButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    cancel=false;
                    outThread();
                    out.start();
                }
            } else if (action.equals("DISCONNECT")) {
                sock.close();
                clientPanel.setVisible(false);
                connectPanelInit();
                connected = false;
                transferButton.setEnabled(true);
            } else if (action.equals("CANCEL")){
                dataArea.setText("Transfer Canceled");
                byte b[] = new byte[4];

                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.putInt(-5);
                DatagramPacket done = new DatagramPacket(b, 4);
                sock.send(done);
                fileTransfering = false;
                cancel=true;
                transferButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        } catch (Exception ae) {
            dataArea.setText(ae.getMessage());
            transferButton.setEnabled(true);
            cancelButton.setEnabled(false);
            System.out.println(ae.getMessage());
        }
    }

    public Sender() {
        this.setLocation(200, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        connectPanelInit();
        connectButton.addActionListener(this);

        disconnectButton.addActionListener(this);
        transferButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    public void connectPanelInit() {

        add(connectPanel);
        connectPanel.setLayout(null);
        this.setSize(280, 180);
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
        clientPanel.add(cancelButton);
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
        transferButton.setBounds(280, 140, 170, 30);

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
        disconnectButton.setBounds(280, 180, 170, 30);

        cancelButton.setVisible(true);
        cancelButton.setEnabled(false);
        cancelButton.setBounds(280,220, 170, 30);

    }

    public static void main(String[] args) {
        new Sender();

    }
}