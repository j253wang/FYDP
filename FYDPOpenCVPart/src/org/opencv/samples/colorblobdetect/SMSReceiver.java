package org.opencv.samples.colorblobdetect;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
//Dont know yet what else i should do
public class SMSReceiver extends BroadcastReceiver {
	/*
	@Override
	public void onReceive(Context context, Intent intent) {
		//—get the SMS message passed in—
		Log.e("wolololololol", "Received a message");
		Bundle bundle = intent.getExtras(); 
		SmsMessage[] msgs = null;
		String str = ""; 
		if (bundle != null){
			//—retrieve the SMS message received—
			Object[] pdus = (Object[]) bundle.get("pdus");
			msgs = new SmsMessage[pdus.length]; 
			for (int i=0; i<msgs.length; i++){
			msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]); 
			str += "SMS from " + msgs[i].getOriginatingAddress(); 
			str += " :";
			str += msgs[i].getMessageBody().toString();
			str += "\n"; 
		}
		//—display the new SMS message—
		Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
		} 
	}*/
	// Get the object of SmsManager
	
	
    final SmsManager sms = SmsManager.getDefault();
    
    public void onReceive(Context context, Intent intent) {
    	Log.e("wolololololol", "Received a message");
        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {
            
            if (bundle != null) {
                
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                
                for (int i = 0; i < pdusObj.length; i++) {
                    
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();
                    
                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReceiver", "senderNum: "+ senderNum + "; message: " + message);
                    

                   // Show Alert
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, 
                                 "senderNum: "+ senderNum + ", message: " + message, duration);
                    toast.show();
                    
                } // end for loop
              } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);
            
        }
    }   
    /*
	private final String DEBUG_TAG = getClass().getSimpleName().toString();
    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private Context mContext;
    private Intent mIntent;

    // Retrieve SMS
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        Log.e("wolololololol", "Received a message");
        
        String action = intent.getAction();

        if(action.equals(ACTION_SMS_RECEIVED)){

            String address, str = "";
            int contactId = -1;

            SmsMessage[] msgs = getMessagesFromIntent(mIntent);
            if (msgs != null) {
                for (int i = 0; i < msgs.length; i++) {
                    address = msgs[i].getOriginatingAddress();
                    str += msgs[i].getMessageBody().toString();
                    str += "\n";
                }
            }   

            if(contactId != -1){
                showNotification(contactId, str);
            }

            // ---send a broadcast intent to update the SMS received in the
            // activity---
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("SMS_RECEIVED_ACTION");
            broadcastIntent.putExtra("sms", str);
            context.sendBroadcast(broadcastIntent);
        }

    }

    public static SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        byte[][] pduObjs = new byte[messages.length][];

        for (int i = 0; i < messages.length; i++) {
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            pdus[i] = pduObjs[i];
            msgs[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return msgs;
    }
    protected void showNotification(int contactId, String message) {
        //Display notification...
    }
    */
}