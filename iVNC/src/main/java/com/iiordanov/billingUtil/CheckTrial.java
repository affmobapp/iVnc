package com.iiordanov.billingUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;

import com.iiordanov.bVNC.BuildConfig;
import com.iiordanov.bVNC.R;
import com.iiordanov.bVNC.bookmark.HomeActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import dalvik.system.PathClassLoader;


public class CheckTrial {

    public final static String GOOGLE_LICENSE_LINK = BuildConfig.GOOGLE_LICENSE_LINK;
    public static final String LICENSE_URI = BuildConfig.LICENSE_URI;
    //Type of operation on inAppBilling
    public static final String PURCHASE_LICENSE = "purchase_license";

    public static final String IS_LICENSE_PURCHASED = "isLicensePurchase";
    public static final int TRIAL_EXPIRED_NOTIFICATION_ID = BuildConfig.ID_NO;
    final static long TrialSec = BuildConfig.TRIAL_EXP_DAYS * 86400; 
    final static boolean isFull = BuildConfig.IS_FULL;
    static final String SKU_PREMIUM = BuildConfig.LICENSE_URI;

    static final int RC_REQUEST = BuildConfig.ID_NO;
    final private static String base64EncodedPublicKey = BuildConfig.IN_APP_PUB_KEY;
    // The helper object
    public static IabHelper mHelper;
    // Does the user have the premium upgrade?
    static boolean mIsPremium = false;
    static Activity calledActivity = null;
    static private String StorageFileName = "iVnc";
    static private Context appContext = null;

    private static String OperationType = null;
    private static SharedPreferences sp = null;
    private static boolean shouldFinishActivity = false;
    // Listener that's called when we finish querying the items we own
    static IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d("iVnc License", "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                //complain("Failed to query inventory: " + result);
                Log.d("iVnc License", "Failed to query inventory: " + result);
                if(isTrialExpired(appContext)) {
                    notifyTrialExpired(appContext);

                    StoreToSharedPreference(mIsPremium);
                    String Title = "Failed to query inventory!!!";
                    String Message = result.toString();
                    CancelNotification(appContext, TRIAL_EXPIRED_NOTIFICATION_ID);
                    showDialogue(calledActivity, Title, Message, true);
                }
                return;
            }

            Log.d("iVnc License", "Query inventory was successful.");

            // Do we have the premium upgrade?
            mIsPremium = inventory.hasPurchase(SKU_PREMIUM);
            Log.d("iVnc License", "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
            if (!mIsPremium) {
                if (OperationType.equalsIgnoreCase(PURCHASE_LICENSE))
                    PurchaseLicense(calledActivity);
            } else {
                StoreToSharedPreference(true);
                String Title = "You have already purchased license";
                String Message = "Your iVnc License has been retrived. Please restart the Rdp Remote Desktop.";
                CancelNotification(appContext, TRIAL_EXPIRED_NOTIFICATION_ID);
                showDialogue(calledActivity, Title, Message, false);
            }
        }
    };

    // Callback for when a purchase is finished
    static IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d("iVnc License", "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                StoreToSharedPreference(false);
                String Title = "Error purchasing license";
                String Message = result.mMessage;
                notifyTrialExpired(appContext);
                showDialogue(calledActivity, Title, Message, shouldFinishActivity);
                Log.d("iVnc License", "Error purchasing: " + result);
                return;
            }

            Log.d("iVnc License", "Purchase successful.");

            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d("iVnc License", "Purchase is premium upgrade. Congratulating user.");
                mIsPremium = true;
                StoreToSharedPreference(mIsPremium);
                String Title = "Congratulations!!!";
                String Message = "License purchased successfully. Press ok to restart.";
                CancelNotification(appContext, TRIAL_EXPIRED_NOTIFICATION_ID);
                showDialogue(calledActivity, Title, Message, true);
            }
        }
    };

    public static Boolean isTrialExpired(Context context) {
        appContext = context;
        Boolean ret = true;
        appContext = context;

        long currentTimeMsec = System.currentTimeMillis();
        long startTimeMsec = getStartTimeStamp();
        long runningTime = ((currentTimeMsec - startTimeMsec) / 1000);

        if (runningTime < TrialSec)
            ret = false;

        return ret;
    }

    public static String timeRemaining(Context context) {
        String ret = context.getString(R.string.Trial_expired);
        appContext = context;

        long currentTimeMsec = System.currentTimeMillis();
        long startTimeMsec = getStartTimeStamp();
        long runningTime = ((currentTimeMsec - startTimeMsec) / 1000);

        if (runningTime < TrialSec) {
            long secsRemaining = TrialSec - runningTime;

            int days = (int) (secsRemaining / 86400);
            int secsStill = (int) (secsRemaining - (days * 86400));
            int hours = (int) (secsStill / 3600);
            secsStill -= (hours * 3600);
            int minutes = (int) (secsStill / 60);
            int seconds = (int) (secsRemaining % 60);

            if (days > 0)
                ret = context.getString(R.string.Trial_expires_in) + " " + days + " " + context.getString(R.string.days);
            else if (hours > 0)
                ret = context.getString(R.string.Trial_expires_in) + " " + hours + " " + context.getString(R.string.hours);
            else if (minutes > 0)
                ret = context.getString(R.string.Trial_expires_in) + " " + minutes + " " + context.getString(R.string.minutes);
            else if (seconds > 0)
                ret = context.getString(R.string.Trial_expires_in) + " " + seconds + " " + context.getString(R.string.seconds);
        } else {
            ret = context.getString(R.string.Trial_expired);
        }

        return ret;
    }

    public static void saveStartTimeStamp(Context context) throws IOException {
        appContext = context;
        long currentTime = System.currentTimeMillis();
        String string = String.valueOf(currentTime);

		/* Write to internal storage */
        String IntRoot = appContext.getFilesDir().getAbsolutePath();
        File intFile = new File(IntRoot, StorageFileName);
		/*Write to internal storage*/
        if (!intFile.exists() && !file.exists()) {
            try {
                FileOutputStream out = new FileOutputStream(intFile);
                out.write(string.getBytes());
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }

    private static long getStartTimeStamp() {
        long timestampFromStorage = getInternalFileTime();

        return timestampFromStorage;
    }

    private static long getInternalFileTime() {
        long ret = 0;
        File file = new File(appContext.getFilesDir().getAbsolutePath(), StorageFileName);
        final StringBuffer storedString = new StringBuffer();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                storedString.append(line);
                //storedString.append('\n');
            }

            String longString = storedString.toString();
            ret = Long.parseLong(longString);
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static Boolean shouldLaunch(Context context, String uri) {
        if (isLicensePurchased(uri, context))
            return true;
        else if (!isTrialExpired(context))
            return true;
        else
            return false;
    }

    public static boolean isLicensePurchased(String uri, Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean app_installed = false;
        if (!isFull) {
            if (inAppPurchased()) {
                app_installed = true;
            }
        } else {
            app_installed = true;
        }
        if (app_installed == true)
            CancelNotification(appContext, TRIAL_EXPIRED_NOTIFICATION_ID);
        return app_installed;
    }

    private static boolean inAppPurchased() {
        boolean ret = sp.getBoolean(IS_LICENSE_PURCHASED, false);
        return ret;
    }
    public static boolean startInAppBilling(Context context, final Activity self, String opType, boolean shouldFinish) {

		calledActivity = self;
		OperationType = opType;
		shouldFinishActivity = shouldFinish;
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		Log.d("iVnc License", "Creating IAB helper.");
        mHelper = new IabHelper(context, base64EncodedPublicKey);

        mHelper.enableDebugLogging(false);

        Log.d("iVnc License", "Starting setup.");

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d("iVnc License", "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                	Log.d("iVnc License", "Problem setting up in-app billing: " + result);
                	// cant goto inApp billing... launch license app purchase
                	Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_LICENSE_LINK));
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					self.startActivity(i);
					self.finish();
                	return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d("iVnc License", "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
		return false;
    }

    public static void PurchaseLicense(Activity context) {
        if (context != null) {
            String payload = "";
            mHelper.launchPurchaseFlow(context, SKU_PREMIUM, RC_REQUEST, mPurchaseFinishedListener , payload);
        } else {
            Log.d("iVnc License", "launchPurchaseFlow. Context null.");
        }

    }

    public static boolean receivedActivityResult(int requestCode, int resultCode, Intent data) {
        return mHelper.handleActivityResult(requestCode, resultCode, data);

    }

    public static void destroyHelper() {
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    protected static void StoreToSharedPreference(boolean state) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(IS_LICENSE_PURCHASED, state);
        editor.commit();
    }

    private static void showDialogue(final Context context, String Title, String Message, final boolean shouldFinish) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setTitle(Title);
        ad.setIcon(R.mipmap.market);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (shouldFinish)
                    ((Activity) context).finish();
            }
        });
        ad.setMessage(Message);
        ad.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (shouldFinish)
                    ((Activity) context).finish();
            }
        }).show();
    }

    public static void notifyTrialExpired(Context context) {
        if (context != null)
            appContext = context;

        if (appContext != null) {
            //String logText = (String) getApplicationContext().getText(R.string.select_trial_expired_body);
            Notification notification = new Notification(R.drawable.notification_error_icon, appContext.getString(R.string.iVnc_Audio_Trial_Expired), System.currentTimeMillis());
            Intent notificationIntent = new Intent(appContext, HomeActivity.class);

            //notificationIntent.putExtra(SendBugReportUiActivity.CRASH_REPORT_TEXT, logText);

            PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, notificationIntent, 0);

            notification.setLatestEventInfo(appContext,
                    appContext.getString(R.string.Trial_expired),
                    appContext.getString(R.string.Please_purchase_iVnc_license_from_market),
                    contentIntent);
            notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_LIGHTS;
            notification.defaults |= Notification.DEFAULT_VIBRATE;
            // notifying
            NotificationManager notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            //cancel previous notification
            notificationManager.notify(TRIAL_EXPIRED_NOTIFICATION_ID, notification);
        }
    }

    public static void trialExpired(final Context context, final Activity self) {
        AlertDialog.Builder ad = new AlertDialog.Builder(self);
        ad.setTitle(context.getString(R.string.iVnc_Audio_Trial_Expired));
        ad.setIcon(R.mipmap.market);
        ad.setMessage(context.getString(R.string.download_iVnc_Audio_License_from_Android_Market));
        ad.setCancelable(false);
        ad.setPositiveButton(context.getString(R.string.Get_from_Market), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
             /*Intent i = new Intent(Intent.ACTION_VIEW,
                                           Uri.parse(marketLink));
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             startActivity(i);*/
                //finish();
                startInAppBilling(context,self,PURCHASE_LICENSE,true);
            }
        }).show();

    }

    public static void CancelNotification(Context context, int NotificationID) {
        if (context != null)
            appContext = context;

        if (appContext != null) {
            if (appContext.NOTIFICATION_SERVICE != null) {
                String ns = Context.NOTIFICATION_SERVICE;
                NotificationManager nMgr = (NotificationManager) appContext.getSystemService(ns);
                nMgr.cancel(NotificationID);
            }
        }
    }
}
