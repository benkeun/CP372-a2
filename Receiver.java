import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Receiver extends JFrame implements ActionListener {
    static JPanel connectPanel = new JPanel();
    static JPanel clientPanel = new JPanel();
    static JButton connectButton = new JButton("CONNECT");
    static JButton disconnectButton = new JButton("DISCONNECT");
    static JLabel IPLabel = new JLabel("IP Address");
    static JLabel ackPortLabel = new JLabel("ACK Port Number");
    static JLabel dataPortLabel = new JLabel("Data Port Number");
    static JLabel fileNameLabel = new JLabel("File Name");
    static JTextField IPField = new JTextField("127.0.0.1");
    static JTextField ackPortField = new JTextField("400");
    static JTextField dataPortField = new JTextField("401");
    static JTextField fileNameField = new JTextField("file.txt");
    static JTextArea dataArea = new JTextArea("");
    static JCheckBox reliableBox = new JCheckBox("RELIABLE");
    static boolean reliable = false;
    static int packetSize;
    static int numPackets;
    static int leftOverByte;
    static boolean receivingStop = false;
    static JScrollPane scrollPane = new JScrollPane(dataArea);

    static DatagramSocket socket = null;
    static InetAddress address;

    static boolean connected = false;
    static boolean acknowledged[];
    static Thread receiving;
    static boolean transmitting = true;

    public void receivingThread() {
        receiving = new Thread() {
            public void run() {
                try {
                    if (connected) {
                        int counter = 1;
                        transmitting = true;
                        String packetInfo[] = handshake().split(" ");
                        packetSize = Integer.parseInt(packetInfo[0]);
                        numPackets = Integer.parseInt(packetInfo[1]);
                        leftOverByte = Integer.parseInt(packetInfo[2]);
                        byte[][] file = new byte[numPackets][packetSize];
                        acknowledged = new boolean[numPackets];
                        dataArea.setText("");
                        byte[] buffer = new byte[packetSize];
                        DatagramPacket packet = new DatagramPacket(buffer, packetSize);
                        while (transmitting) {
                            counter++;
                            socket.receive(packet);
                            byte[] sequenceNumberByte = Arrays.copyOfRange(buffer, 0, 4);
                            byte[] filePortionByte;
                            int sequenceNumber = java.nio.ByteBuffer.wrap(sequenceNumberByte).getInt();
                            if (sequenceNumber == (numPackets)) {
                                filePortionByte = Arrays.copyOfRange(buffer, 4, leftOverByte);
                            } else {
                                filePortionByte = Arrays.copyOfRange(buffer, 4, buffer.length);
                            }
                            if (sequenceNumber == -1) {
                                transmitting = false;
                                FileOutputStream out = new FileOutputStream(fileNameField.getText());
                                for (int i = 0; i < numPackets; i++) {
                                    out.write(file[i]);

                                }
                                dataArea.setText(dataArea.getText() + "File Received\n");
                            } else if (counter % 10 != 0 || reliable) {
                                dataArea.setText(dataArea.getText() + "There were " + (sequenceNumber + 1) + " of "
                                        + numPackets + " packets Received in order\n");
                                file[sequenceNumber] = filePortionByte;
                                acknowledged[sequenceNumber] = true;

                                if (sequenceNumber == numPackets) {
                                }
                                DatagramPacket pSend = new DatagramPacket(sequenceNumberByte, 4);
                                socket.send(pSend);

                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
                if (receivingStop) {
                    return;
                }
                if (!transmitting) {
                    receivingThread();
                    receiving.start();
                }
            }

        };

    }

    public Receiver() {
        this.setLocation(700, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        connectPanelInit();
        connectButton.addActionListener(this);
        disconnectButton.addActionListener(this);
        reliableBox.addActionListener(this);
    }

    public String handshake() {
        try {

            byte[] buf = new byte[64]; // 2^8
            DatagramPacket p = new DatagramPacket(buf, 64);
            socket.receive(p);
            String handshake = new String(buf);

            buf = new byte[4];
            ByteBuffer buffer = ByteBuffer.wrap(buf);
            buffer.putInt(-1);
            DatagramPacket done = new DatagramPacket(buf, 4);
            socket.send(done);

            return handshake;

        } catch (Exception e) {
        }
        return "";
    }

    public void actionPerformed(ActionEvent ae) {
        String action = ae.getActionCommand();
        try {
            if (action.equals("CONNECT")) {
                connectionInit();
                clientPanelInit();
                receivingStop = false;
                receivingThread();
                receiving.start();
            } else if (action.equals("DISCONNECT")) {
                socket.close();
                clientPanel.setVisible(false);
                connected = false;
                transmitting = false;
                connectPanelInit();
                receivingStop = true;
            } else if (action.equals("RELIABLE")) {
                reliable = reliableBox.isSelected();
            }
        } catch (Exception e) {
            dataArea.setText("Input error");
        }
    }

    public void connectionInit() {
        try {
            if (!connected) {
                connected = true;
                socket = new DatagramSocket(Integer.parseInt(ackPortField.getText()));
                address = InetAddress.getByName(IPField.getText());
                socket.connect(address, Integer.parseInt(dataPortField.getText()));
                connectPanel.setVisible(false);
            }
        } catch (Exception e) {

        }
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
        this.setSize(575, 220);
        this.setTitle("Client");

        clientPanel.setLayout(null);
        clientPanel.add(disconnectButton);
        clientPanel.add(reliableBox);
        clientPanel.add(scrollPane);
        clientPanel.add(fileNameField);
        clientPanel.add(fileNameLabel);
        clientPanel.setVisible(true);

        dataArea.setVisible(true);
        dataArea.setEditable(false);
        dataArea.setLineWrap(true);
        dataArea.setText("");

        reliableBox.setBounds(380, 80, 100, 30);
        reliableBox.setVisible(true);

        fileNameLabel.setBounds(380, 10, 110, 20);
        fileNameLabel.setVisible(true);

        fileNameField.setBounds(380, 40, 110, 20);
        fileNameField.setVisible(true);

        disconnectButton.setBounds(380, 130, 170, 30);
        disconnectButton.setVisible(true);

        scrollPane.setBounds(10, 10, 350, 150);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setVisible(true);

    }

    public static void main(final String[] args) throws Exception {
        Receiver mainView = new Receiver();
    }
}