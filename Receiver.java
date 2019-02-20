import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import javax.swing.*;
import java.awt.FlowLayout;
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
    static JTextField IPField = new JTextField("");
    static JTextField ackPortField = new JTextField("");
    static JTextField dataPortField = new JTextField("");
    static JTextField fileNameField = new JTextField("");
    static JTextArea dataArea = new JTextArea("");

    static DatagramSocket socket = null;
    static InetAddress address;
    static DatagramPacket packet;
    static boolean connected = false;
    static Thread receiving = new Thread(){
        public void run(){
            try{

            }catch(Exception e){
                
            }
        }
    };

    public Receiver() {
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

        dataArea.setBounds(10, 200, 350, 150);
        dataArea.setVisible(true);
        dataArea.setEditable(false);
        dataArea.setLineWrap(true);

        disconnectButton.setBounds(380, 320, 170, 30);
        disconnectButton.setVisible(true);

        disconnectButton.addActionListener(this);
    }

    public void receive(Datagrampacket p) throws IOException{
        byte[] data = p.getData();
        String message = "";
        for(int i = 0; i < p.getLength(); i++){
           
        }
    }
    public void actionPerformed(ActionEvent ae) {
        String action = ae.getActionCommand();
        try {
            if (action.equals("CONNECT")) {
                socket = new DatagramSocket(Integer.parseInt(this.dataPortField.getText()));
                address =  InetAddress.getByName(ackPortField.getText());
                socket.connect(address, Integer.parseInt(this.dataPortField.getText()));
                connectPanel.setVisible(false);
                connected = true;
                clientPanelInit();
                receiving.start();
            } else if (action.equals("DISCONNECT")) {
                socket.close();
                clientPanel.setVisible(false);
                connected = false;
                connectPanelInit();
            }
        } catch (Exception e) {
            System.out.println("eror");
            dataArea.setText("Input error");
        }
    }


    public static void main(final String[] args) throws Exception {
        Receiver mainView = new Receiver();
    }
}