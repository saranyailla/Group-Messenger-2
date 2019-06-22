package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class one extends Activity {

    static final String TAG = one.class.getSimpleName();
    static ArrayList<Integer> portnumbers=new ArrayList<Integer>();



    static List<Message> multicastedMessages= new ArrayList<Message>();
    static HashMap<Integer,ArrayList<Message>> proposedList= new HashMap<Integer,ArrayList<Message>>();
    ArrayList<Integer> client_msgs=new ArrayList<Integer>();
    HashMap<String,String> hj=new HashMap<String, String>();
    static int self_incrementer=0;
    static int incrementer=0;
    static List<ArrayList<String>> buffer= new ArrayList<ArrayList<String>>();

    static final int SERVER_PORT = 10000;
    static int port=0;
    static String  currentport="";
    static String cp = "00000";
    static int mg=0;
    String h="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);;


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        final EditText textedit = (EditText) findViewById(R.id.editText1);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);


        portnumbers.add(11108);
        portnumbers.add(11112);
        portnumbers.add(11116);
        portnumbers.add(11120);
        portnumbers.add(11124);


        hj.put("11108",cp);
        hj.put("11112",cp);
        hj.put("11116",cp);
        hj.put("11120",cp);
        hj.put("11124",cp);
        final  String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        port=Integer.parseInt(myPort);
        Log.e(TAG,myPort);
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=textedit.getText().toString();
                textedit.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String senderPort = msgs[1];
            try {
                String msgToSend = msgs[0];
                self_incrementer = self_incrementer + 1;
                String mid = senderPort + self_incrementer;
                int msgId = Integer.parseInt(mid);
                client_msgs.add(msgId);

                Message message = new Message();
                message.setMessageId(msgId);
                message.setMessage(msgToSend);
                message.setSenderPort(Integer.parseInt(senderPort));
                message.setFinalAgreedPort(Integer.parseInt(senderPort));
                message.setProposedSequenceNumber(self_incrementer);
                message.setAgreedSequenceNumber(self_incrementer);
                message.setAgreement(false);
                message.setDeliverable(false);
                message.setSendStatus(false);
                message.setAgreementReceived(false);

                multicastedMessages.add(message);

                for (int i = 0; i < portnumbers.size(); i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portnumbers.get(i));
                    if(message.agreement==false) {
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        oos.writeObject(message);
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        Message propMsg = (Message) ois.readObject();

                        if (proposedList.get(propMsg.messageId) == null) {
                            ArrayList<Message> a = new ArrayList<Message>();
                            a.add(propMsg);
                            proposedList.put(propMsg.messageId, a);
                        } else {
                            ArrayList<Message> a = proposedList.get(propMsg.messageId);
                            a.add(propMsg);
                            proposedList.put(propMsg.messageId, a);
                        }
//                    socket.close();
                    }

                }
                if(client_msgs.size()!=0)
                for(Integer messId:client_msgs){
                    int max = 0;
                    int port = 0;
                    if (proposedList.get(messId).size() == portnumbers.size()) {
                        for (Message mess : proposedList.get(messId)) {
                            if (mess.getProposedSequenceNumber() >= max) {
                                if (mess.getProposedSequenceNumber() == max) {
                                    if (mess.getFinalAgreedPort() > port) {
                                        port = mess.getFinalAgreedPort();
                                    }
                                } else {
                                    max = mess.getProposedSequenceNumber();
                                    port = mess.getFinalAgreedPort();
                                }
                            }
                        }
                        proposedList.remove(messId);
                        for(Message mess:multicastedMessages){
                            if(mess.messageId==messId){

                                mess.setAgreement(true);
                                mess.setAgreedSequenceNumber(max);
                                mess.setFinalAgreedPort(port);
                                proposedList.put(messId,null);
                                int c=0;
                                for(int i=0;i<portnumbers.size();i++) {
                                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portnumbers.get(i));

                                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                    oos.writeObject(mess);
                                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                                    Message agrMsg = (Message) ois.readObject();
                                    if(agrMsg.getAgreementReceived()==true){
                                        mess.setAgreementReceived(true);
                                        if(proposedList.get(agrMsg.messageId)==null){
                                            ArrayList<Message> a = new ArrayList<Message>();
                                             a.add(agrMsg);
                                             proposedList.put(agrMsg.messageId, a);
                                        } else {
                                            ArrayList<Message> a = proposedList.get(agrMsg.messageId);
                                            a.add(agrMsg);
                                            proposedList.put(agrMsg.messageId, a);
                                        }
                                        if(proposedList.get(agrMsg.messageId).size()==portnumbers.size()){
                                            c=1;
                                            mess.setSendStatus(true);
                                        }
                                    }
//                                    socket.close();
                                }

                                if(c==1) {
                                    proposedList.get(messId).clear();
                                    for (int i = 0; i < portnumbers.size(); i++) {

                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portnumbers.get(i));
                                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                        oos.writeObject(mess);
                                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                                        String done = (String) ois.readObject();
//                                        if(done.equals("done")){
//                                            socket.close();
//                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //Implementing server task
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri AUri = null;

        private Uri buildUri(String scheme, String authority)
        {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            String acktestmsg= null;
            AUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            Socket client = null;
            try {
                while (true) {
                    if (multicastedMessages.size() != 0) {
                        Thread.sleep(400);

                        Collections.sort(multicastedMessages, new Comparator<Message>() {
                            @Override
                            public int compare(Message p, Message q) {
                                if (Integer.compare(p.getAgreedSequenceNumber(), q.getAgreedSequenceNumber()) == 0) {
                                    if (Integer.compare(p.getFinalAgreedPort(), q.getFinalAgreedPort()) == 0) {
                                        return Integer.compare(p.messageId, q.getMessageId());
                                    } else {
                                        return Integer.compare(p.getFinalAgreedPort(), q.getFinalAgreedPort());
                                    }
                                } else {
                                    return Integer.compare(p.getAgreedSequenceNumber(),q.getAgreedSequenceNumber());
                                }
                            }
                        });

                        while (multicastedMessages.size()!=0 && multicastedMessages.get(0).getDeliverable()==true) {

                            ContentValues keyValueToInsert = new ContentValues();
                            keyValueToInsert.put("key", incrementer++);
                            keyValueToInsert.put("value", multicastedMessages.get(0).message);
                            Uri newUri = getContentResolver().insert(AUri, keyValueToInsert);

                            publishProgress(multicastedMessages.get(0).message);
                            multicastedMessages.remove(0);
                        }
                    }


                    client = serverSocket.accept();
                    ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                    Message serverMsg = (Message) ois.readObject();
                    if(serverMsg!=null){
                        if( serverMsg.agreement==false) {
                            if(serverMsg.senderPort!=port ) {
                                serverMsg.setProposedSequenceNumber(self_incrementer + 1);
                                serverMsg.setFinalAgreedPort(port);
                                multicastedMessages.add(serverMsg);
                            }
                            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                            oos.writeObject(serverMsg);
                        }
                        else if (serverMsg.agreement==true){
                           for(Message m:multicastedMessages){
                               if(m.getMessageId()==serverMsg.getMessageId()){
                                   m.setAgreement(serverMsg.getAgreement());
                                   m.setFinalAgreedPort(serverMsg.getFinalAgreedPort());
                                   m.setAgreedSequenceNumber(serverMsg.getAgreedSequenceNumber());
                                   m.setAgreementReceived(true);
                                   ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                                   oos.writeObject(m);
                               }
                           }

                        }
                        else if(serverMsg.getSendStatus()==true){
                            for(Message m:multicastedMessages){
                                if(m.getMessageId()==serverMsg.getMessageId()){
                                   m.setSendStatus(true);
                                   m.setDeliverable(true);
                                }
                            }
                            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                            oos.writeObject("done");
                        }
                    }

                }
            }
            catch(Exception f){
                    f.printStackTrace();
                }
            finally{
                    try {
                        if (client != null)
                            client.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return null;

            }

    }

    public class Message{
        public Boolean agreement;
        public String message;
        public int messageId;
        public int senderPort;
        public int proposedSequenceNumber;
        public int agreedSequenceNumber;
        public int finalAgreedPort;
        public Boolean deliverable;
        public Boolean agreementReceived;
        public Boolean sendStatus;

        public int getSenderPort() {
            return senderPort;
        }

        public void setSenderPort(int senderPort) {
            this.senderPort = senderPort;
        }

        public Boolean getSendStatus() {
            return sendStatus;
        }

        public void setSendStatus(Boolean sendStatus) {
            this.sendStatus = sendStatus;
        }

        public Boolean getAgreementReceived() {
            return agreementReceived;
        }

        public void setAgreementReceived(Boolean agreementReceived) {
            this.agreementReceived = agreementReceived;
        }



        public Boolean getDeliverable() {
            return deliverable;
        }

        public void setDeliverable(Boolean deliverable) {
            this.deliverable = deliverable;
        }

        public Boolean getAgreement() {
            return agreement;
        }

        public void setAgreement(Boolean agreement) {
            this.agreement = agreement;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getMessageId() {
            return messageId;
        }

        public void setMessageId(int messageId) {
            this.messageId = messageId;
        }



        public int getProposedSequenceNumber() {
            return proposedSequenceNumber;
        }

        public void setProposedSequenceNumber(int proposedSequenceNumber) {
            this.proposedSequenceNumber = proposedSequenceNumber;
        }

        public int getAgreedSequenceNumber() {
            return agreedSequenceNumber;
        }

        public void setAgreedSequenceNumber(int agreedSequenceNumber) {
            this.agreedSequenceNumber = agreedSequenceNumber;
        }

        public int getFinalAgreedPort() {
            return finalAgreedPort;
        }

        public void setFinalAgreedPort(int finalAgreedPort) {
            this.finalAgreedPort = finalAgreedPort;
        }
    }
}