package com.sample.sayan.sampledialer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;

    public static boolean isRunning;
    private TelephonyManager mTelephonyManager;
    private MyPhoneCallListener mListener;

    @Override
    protected void onStart() {
        super.onStart();
        MainActivity.isRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.isRunning = false;
    }

    /**
     * Creates the activity, sets the view, and checks if Telephony is enabled.
     * Telephony enabled:
     *     Checks for phone permission.
     *     Sets the PhoneStateListener.
     * Telephony not enabled: Disables the call button and shows the Retry button.
     *
     * @param savedInstanceState Instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create a telephony manager.
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        // Check to see if Telephony is enabled.
        if (isTelephonyEnabled()) {
            // Check for phone permission.
            checkForPhonePermission();
            // Register the PhoneStateListener to monitor phone activity.
            if (getIntent().getBooleanExtra("calling", false)) {
                mListener = new MyPhoneCallListener();
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } else {
            // Disable the call button.
            //  disableCallButton();
        }
    }

    /**
     * Checks whether Telephony is enabled.
     *
     * @return true if enabled, otherwise false
     */
    private boolean isTelephonyEnabled() {
        if (mTelephonyManager != null) {
            if (mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the app has phone-calling permission.
     */
    private void checkForPhonePermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Permission not yet granted. Use requestPermissions().
            // MY_PERMISSIONS_REQUEST_CALL_PHONE is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
        } else {
            // Permission already granted. Enable the call button.
            //   enableCallButton();
        }
    }

    /**
     * Processes permission request codes.
     *
     * @param requestCode  The request code passed in requestPermissions()
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        // Check if permission is granted or not for the request.
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.CALL_PHONE)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted. Enable call button.
                    //   enableCallButton();
                } else {
                    // Permission denied.
                    // Disable the call button.
                    //   disableCallButton();
                }
            }
        }
    }

    /**
     * Uses an implicit intent to make the phone call.
     * Before calling, checks to see if permission is granted.
     *
     * @param view View (phone_icon) that was clicked.
     */
    public void callNumber(View view) {
//        EditText editText = (EditText) findViewById(R.id.editText_main);
        // Use format with "tel:" and phone number to create phoneNumber.
        String phoneNumber = String.format("tel: %s", "8900032859");
        // Log the concatenated phone number for dialing.
        // Create the intent.
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        // Set the data for the intent as the phone number.
        callIntent.setData(Uri.parse(phoneNumber));
        // If package resolves to an app, check for phone permission,
        // and send intent.
        if (callIntent.resolveActivity(getPackageManager()) != null) {
            checkForPhonePermission();
            startActivity(callIntent);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    killCall(getApplicationContext());
                }
            },8000);
        } else {
            Log.e(TAG, "Can't resolve app for ACTION_CALL Intent.");
        }
    }

    public boolean killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d(TAG,"PhoneStateReceiver **" + ex.toString());
            return false;
        }
        return true;
    }

    /**
     * Monitors and logs phone call activities, and shows the phone state
     * in a toast message.
     */
    private class MyPhoneCallListener extends PhoneStateListener {
        private boolean returningFromOffHook = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // Define a string for the message to use in a toast.
            String message = "";
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // Incoming call is ringing (not used for outgoing call).
                    message = message +
                            "Ringing: " + incomingNumber;
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Phone call is active -- off the hook.
                    message = message + "Off Hook";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    returningFromOffHook = true;
                    Intent intentPhoneCall = new Intent(MainActivity.this, MainActivity.class);
                    intentPhoneCall.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intentPhoneCall.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    intentPhoneCall.putExtra("calling", true);
                    startActivity(intentPhoneCall);
                    overridePendingTransition(0,0);
                    finish();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // Phone is idle before and after phone call.
                    // If running on version older than 19 (KitKat),
                    // restart activity when phone call ends.
                    message = message + "Idle";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    if (returningFromOffHook) {
                        // No need to do anything if >= version KitKat.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            // Restart the app.
                            Intent intent1 = getPackageManager()
                                    .getLaunchIntentForPackage(getPackageName());
                            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent1);
                        }
                    }
                    break;
                default:
                    message = message + "Phone off";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    break;
            }
        }
    }

    /**
     * Makes the call button (phone_icon) invisible so that it can't be used,
     * and makes the Retry button visible.
     */
//    private void disableCallButton() {
//        Toast.makeText(this, R.string.phone_disabled, Toast.LENGTH_LONG).show();
//        ImageButton callButton = (ImageButton) findViewById(R.id.phone_icon);
//        callButton.setVisibility(View.INVISIBLE);
//        if (isTelephonyEnabled()) {
//            Button retryButton = (Button) findViewById(R.id.button_retry);
//            retryButton.setVisibility(View.VISIBLE);
//        }
//    }
//
//    /**
//     * Makes the call button (phone_icon) visible so that it can be used.
//     */
//    private void enableCallButton() {
//        ImageButton callButton = (ImageButton) findViewById(R.id.phone_icon);
//        callButton.setVisibility(View.VISIBLE);
//    }
//
//    /**
//     * Enables the call button, and sends an intent to start the activity.
//     *
//     * @param view View (Retry button) that was clicked.
//     */
//    public void retryApp(View view) {
//        enableCallButton();
//        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
//        startActivity(intent);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTelephonyEnabled()) {
            mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
        }
    }
}
