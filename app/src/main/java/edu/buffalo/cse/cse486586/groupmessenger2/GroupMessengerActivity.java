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
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    static ArrayList<Integer> portnumbers=new ArrayList<Integer>();

    static List<Message> multicastedMessages= new ArrayList<Message>();

    static HashMap<Integer,ArrayList<Message>> proposedList= new HashMap<Integer,ArrayList<Message>>();

    ArrayList<Integer> client_msgs=new ArrayList<Integer>();

    static int avd_counter=0;

    static int seq_no=0;

    static final int SERVER_PORT = 10000;

    static int cp=0;

    static int currentport=0;

    static int port=0;

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

        final  String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        port=Integer.parseInt(myPort);

        OnPTestClickListener optc= new OnPTestClickListener(tv, getContentResolver());
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket,optc);
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
                avd_counter = avd_counter + 1;
                String mid = senderPort + avd_counter;
                int msgId = Integer.parseInt(mid);
                client_msgs.add(msgId);

                Message message = new Message();
                message.setMessageId(msgId);
                message.setMessage(msgToSend);
                message.setSenderPort(Integer.parseInt(senderPort));
                message.setFinalAgreedPort(Integer.parseInt(senderPort));
                message.setProposedSequenceNumber(avd_counter);
                message.setAgreedSequenceNumber(avd_counter);
                message.setAgreement(false);
                message.setDeliverable(false);
                message.setSendStatus(false);
                message.setAgreementReceived(false);

                multicastedMessages.add(message);

                for (int i = 0; i < portnumbers.size(); i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portnumbers.get(i));
                    currentport=portnumbers.get(i);
                    try {

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
                        socket.close();
                    } catch (Exception e) {
                        socket.close();
                        cp = currentport;
                        portnumbers.remove(i);
                        i = i - 1;

                    }

                }
                if (client_msgs.size() != 0)
                    for (Integer messId : client_msgs) {
                        int max = 0;
                        int maxport = 0;
                        if (proposedList.get(messId) != null)
                            if (proposedList.get(messId).size() >= portnumbers.size()) {
                                for (Message mess : proposedList.get(messId)) {
                                    if (mess.getProposedSequenceNumber() >= max) {
                                        if (mess.getProposedSequenceNumber() == max) {
                                            if (mess.getFinalAgreedPort() > maxport) {
                                                maxport = mess.getFinalAgreedPort();
                                            }
                                        } else {
                                            max = mess.getProposedSequenceNumber();
                                            maxport = mess.getFinalAgreedPort();
                                        }
                                    }
                                }
                                proposedList.get(messId).clear();

                                ArrayList<Message> al = new ArrayList<Message>(multicastedMessages);
                                ListIterator<Message> it = al.listIterator();
                                int size= 0;
                                while (it.hasNext()) {

                                    Message mess = it.next();
                                    if (mess.messageId == messId) {
                                        mess.setAgreement(true);
                                        mess.setAgreedSequenceNumber(max);
                                        mess.setFinalAgreedPort(maxport);
                                        for (int i = 0; i < portnumbers.size(); i++) {
                                            currentport=portnumbers.get(i);
                                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portnumbers.get(i));
                                            Message agrMsg= new Message();

                                            try {
                                                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                                oos.writeObject(mess);
                                                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                                                agrMsg = (Message) ois.readObject();
                                                if (agrMsg.getAgreementReceived() == true) {
                                                    mess.setAgreementReceived(true);
                                                    if (proposedList.get(agrMsg.messageId) == null) {
                                                        ArrayList<Message> a = new ArrayList<Message>();
                                                        a.add(agrMsg);
                                                        proposedList.put(agrMsg.messageId, a);
                                                    } else {
                                                        ArrayList<Message> a = proposedList.get(agrMsg.messageId);
                                                        a.add(agrMsg);
                                                        proposedList.put(agrMsg.messageId, a);
                                                    }

                                                    if (proposedList.get(agrMsg.messageId).size() >= portnumbers.size()) {
                                                        size = 1;
                                                        mess.setSendStatus(true);

                                                    }
                                                }
                                                socket.close();
                                            } catch (Exception e) {
                                                socket.close();
                                                cp=currentport;
                                                portnumbers.remove(i);
                                                i=i-1;
                                                if(proposedList.get(agrMsg.messageId)!=null) {
                                                    if (proposedList.get(agrMsg.messageId).size() >= portnumbers.size()) {
                                                        size = 1;
                                                        break;
                                                    }
                                                }
                                            }

                                        }
                                        if (size == 1) {
                                            proposedList.get(messId).clear();
                                            for (int i = 0; i < portnumbers.size(); i++) {
                                                currentport=portnumbers.get(i);
                                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portnumbers.get(i));
                                                try {
                                                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                                    oos.writeObject(mess);
                                                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                                                    String propMsg = (String) ois.readObject();
                                                    if (propMsg.equals("done")) {
                                                        socket.close();
                                                    }
                                                }
                                                catch (Exception e) {
                                                    socket.close();
                                                    cp = currentport;
                                                    portnumbers.remove(i);
                                                    i = i - 1;

                                                }

                                            }
                                        }
                                    }
                                }
                            }

                    }
            }catch(Exception e)

            {
                e.printStackTrace();
            }
            return null;
        }

    }

    //Implementing server task
    private class ServerTask extends AsyncTask<Object, String, Void> {

        private Uri AUri = null;

        private Uri buildUri(String scheme, String authority)
        {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        @Override
        protected Void doInBackground(Object... sockets) {
            ServerSocket serverSocket =(ServerSocket)sockets[0];
            OnPTestClickListener optc=(OnPTestClickListener)sockets[1];
            Message serverMsg= new Message();
            Socket client = null;

            while (true) {
                try {
                    if (multicastedMessages.size() != 0) {
                        Thread.sleep(400);
                        ArrayList<Message> al= new ArrayList<Message>(multicastedMessages);
                        if(cp!=0)
                            for(Message a:al){
                                if(a.senderPort==cp && a.getDeliverable()==false){
                                    multicastedMessages.remove(a);
                                }
                            }
                        Collections.sort(multicastedMessages, Message.sortMessages());
                        while (multicastedMessages.size()!=0 && multicastedMessages.get(0).getDeliverable()==true) {

                            ContentValues keyValueToInsert = new ContentValues();
                            keyValueToInsert.put("key", seq_no++);
                            keyValueToInsert.put("value", multicastedMessages.get(0).message);
                            getContentResolver().insert(optc.mUri, keyValueToInsert);

                            publishProgress(multicastedMessages.get(0).message);
                            multicastedMessages.remove(0);
                        }
                    }


                    client = serverSocket.accept();
                    ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                    serverMsg = (Message) ois.readObject();
                    if(serverMsg!=null){
                        if( serverMsg.agreement==false) {
                            if(serverMsg.senderPort!=port ) {
                                avd_counter=avd_counter+1;
                                serverMsg.setProposedSequenceNumber(avd_counter);
                                serverMsg.setFinalAgreedPort(port);
                                multicastedMessages.add(serverMsg);
                            }
                            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                            oos.writeObject(serverMsg);
                        }
                        else if (serverMsg.agreement==true && serverMsg.getSendStatus()==false){
                            for(Message m:multicastedMessages){
                                if(m.getMessageId()==serverMsg.getMessageId()){
                                    m.setAgreement(serverMsg.getAgreement());
                                    m.setFinalAgreedPort(serverMsg.getFinalAgreedPort());
                                    m.setAgreedSequenceNumber(serverMsg.getAgreedSequenceNumber());
                                    m.setAgreementReceived(true);
                                    ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                                    oos.writeObject(m);
                                    break;
                                }
                            }

                        }
                        else if(serverMsg.getSendStatus()==true){
                            for(Message m:multicastedMessages){
                                if(m.getMessageId()==serverMsg.getMessageId()){
                                    m.setSendStatus(true);
                                    m.setDeliverable(true);
                                    break;
                                }
                            }
                            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                            oos.writeObject("done");
                        }
                    }

                }
                catch(Exception f){
                    if(cp==0 && portnumbers.size()==5){
                        cp=serverMsg.senderPort;
                        portnumbers.remove(serverMsg.senderPort);
                    }
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
            }



        }
        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived+"\n");
            return;

        }

    }


}