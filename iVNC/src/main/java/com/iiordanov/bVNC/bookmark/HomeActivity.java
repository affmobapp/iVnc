package com.iiordanov.bVNC.bookmark;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.R;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.iiordanov.bVNC.Utils;
import com.iiordanov.bVNC.bVNC;
import com.iiordanov.billingUtil.CheckTrial;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Mithun on 2/28/2015.
 */
public class HomeActivity extends FragmentActivity {
//    private final static String ADD_BOOKMARK_PLACEHOLDER = "add_bookmark";
    private static final String TAG = "HomeActivity";
    private static final String PARAM_SUPERBAR_TEXT = "superbar_text";
    public static final String PARAM_CONNECTION_REFERENCE = "ConnectionInfo";
    private ListView listViewBookmarks;
//    private WebView webViewGetStarted;
    private Button clearTextButton;
    private EditText superBarEditText;
    private ConnectionArrayAdapter connectionAdapter;
//    private DifferentListAdapter differentListAdapter;
//    private PlaceholderBookmark addBookmarkPlaceholder;
    private String sectionLabelBookmarks;
    protected Database database;
    private ConnectionBean selectedConnection = null;
    private ArrayList<ConnectionBean> connections;
    private boolean isConnecting = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            CheckTrial.saveStartTimeStamp(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        setTitle(com.freerdp.freerdpcore.R.string.title_home);
        super.onCreate(savedInstanceState);
        Utils.showMenu(this);
        setContentView(R.layout.bookmark_home);

        database = new Database(this);


        long heapSize = Runtime.getRuntime().maxMemory();
        Log.i(TAG, "Max HeapSize: " + heapSize);
        Log.i(TAG, "App data folder: " + getFilesDir().toString());

        // load views
        View superView = (View) findViewById(R.id.homeSuperBar);
        superView.setVisibility(View.GONE); // Mithun

        clearTextButton = (Button) findViewById(R.id.home_clear_search_btn);
        superBarEditText = (EditText) findViewById(R.id.homesuperBarEditText);

        listViewBookmarks = (ListView) findViewById(R.id.listViewHomeBookmarks);

        // set listeners for the list view
        listViewBookmarks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedConnection = connections.get(position);
                selectedConnection.setContext(getApplicationContext());
                startConnection();
            }
        });

        listViewBookmarks.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                getMenuInflater().inflate(R.menu.bookmark_home_context_menu, menu);
                menu.setHeaderTitle(getResources().getString(com.freerdp.freerdpcore.R.string.menu_title_bookmark));
            }
        });

        superBarEditText.addTextChangedListener(new SuperBarTextWatcher());

        clearTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                superBarEditText.setText("");
            }
        });
    }

    protected void startConnection() {
        if (selectedConnection == null) return;
        ActivityManager.MemoryInfo info = Utils.getMemoryInfo(this);
        if (info.lowMemory)
            System.gc();
        startConnectionActivity();
    }

    /**
     * Starts the activity which makes a VNC connection and displays the remote desktop.
     */
    private void startConnectionActivity() {
        isConnecting = true;
        //updateSelectedFromView();
        selectedConnection.saveAndWriteRecent(false);
        Intent intent = new Intent(this, RemoteCanvasActivity.class);
        intent.putExtra(Constants.CONNECTION, selectedConnection.Gen_getValues());
        startActivity(intent);
    }

//    private void trialExpired(final Context context, final Activity self) {
//        AlertDialog.Builder ad = new AlertDialog.Builder(this);
//        ad.setTitle(context.getString(com.freerdp.freerdpcore.R.string.iPlay_Audio_Trial_Expired));
//        ad.setIcon(com.freerdp.freerdpcore.R.drawable.market);
//        ad.setMessage(context.getString(com.freerdp.freerdpcore.R.string.download_iPlay_Audio_License_from_Android_Market));
//        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                finish();
//            }
//        });
//        ad.setPositiveButton(context.getString(com.freerdp.freerdpcore.R.string.Get_from_Market), new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int whichButton) {
//                com.mk.iRdp.billingUtil.CheckTrial.startInAppBilling(context, self, com.mk.iRdp.billingUtil.CheckTrial.PURCHASE_LICENSE, true);
//            }
//        }).show();
//
//    }


//    @Override
//    public boolean onSearchRequested() {
//        superBarEditText.requestFocus();
//        return true;
//    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {

        // get connection reference
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) aItem.getMenuInfo();
         // refer to http://tools.android.com/tips/non-constant-fields why we can't use switch/case here ..
        selectedConnection = connections.get(menuInfo.position);
        final long refStr = selectedConnection.get_Id();
        int itemId = aItem.getItemId();
        if (itemId == R.id.bookmark_connect) {
            selectedConnection.setContext(getApplicationContext());
            startConnection();
            return true;
        } else if (itemId == R.id.bookmark_edit) {
            Intent bookmarkIntent = new Intent(this.getApplicationContext(), bVNC.class);
            bookmarkIntent.putExtra(PARAM_CONNECTION_REFERENCE, refStr);
            startActivity(bookmarkIntent);
            return true;
        } else if (itemId == R.id.bookmark_delete) {
            Utils.showYesNoPrompt(this, "Delete?", "Delete " + selectedConnection.getNickname() + "?",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i)
                        {
                            selectedConnection.Gen_delete(database.getWritableDatabase());
                            database.close();
                            connectionAdapter.remove(refStr);
                            refreshListView(true);
                        }
                    }, null);
//            // clear super bar text
//            superBarEditText.setText("");
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if (com.mk.iRdp.billingUtil.CheckTrial.isTrialExpired(getApplicationContext()) &&
                !com.mk.iRdp.billingUtil.CheckTrial.isLicensePurchased(com.mk.iRdp.billingUtil.CheckTrial.LICENSE_URI, getApplicationContext())) {
            this.trialExpired(getApplicationContext(), (Activity) this);
            com.mk.iRdp.billingUtil.CheckTrial.notifyTrialExpired(getApplicationContext());
        } else if (!com.mk.iRdp.billingUtil.CheckTrial.isLicensePurchased(com.mk.iRdp.billingUtil.CheckTrial.LICENSE_URI, getApplicationContext())) {
            Toast.makeText(getApplicationContext(), com.mk.iRdp.billingUtil.CheckTrial.timeRemaining(getApplicationContext()), Toast.LENGTH_LONG).show();
        } else {
            com.mk.iRdp.billingUtil.CheckTrial.CancelNotification(getApplicationContext(), com.mk.iRdp.billingUtil.CheckTrial.TRIAL_EXPIRED_NOTIFICATION_ID);
        }*/
        Log.v(TAG, "HomeActivity.onResume");
        if(CheckTrial.shouldLaunch(getApplicationContext(), CheckTrial.LICENSE_URI)) {
            refreshListView(false);
            listViewBookmarks.setAdapter(connectionAdapter);

            // show welcome screen in case we have a first-time user
            showWelcomeScreenOrBookmarkList();
            // if we have a filter text entered cause an update to be caused here
            //        String filter = superBarEditText.getText().toString();
            //        if (filter.length() > 0)
            //            superBarEditText.setText(filter);
        }
        else{
            CheckTrial.trialExpired(getApplicationContext(), (Activity)this);
            CheckTrial.notifyTrialExpired(getApplicationContext());
        }
    }

    public Database getDatabaseHelper() {
        return database;
    }

    public void refreshListView(boolean checkConnectionLength) {
        Log.i(TAG, "refreshListView called");
        SQLiteDatabase db = database.getReadableDatabase();
        connections = new ArrayList<ConnectionBean>();
        ConnectionBean.getAll(db,
                ConnectionBean.GEN_TABLE_NAME, connections,
                ConnectionBean.newInstance);
        Collections.sort(connections);
//        int connectionIndex = 0;
        if (connections.size() > 1) {
//            MostRecentBean mostRecent = ConnectionBean.getMostRecent(db);
//            if (mostRecent != null) {
//                for (int i = 1; i < connections.size(); ++i) {
//                    if (connections.get(i).get_Id() == mostRecent.getConnectionId()) {
//                        connectionIndex = i;
//                        break;
//                    }
//                }
//            }
        }
        else if(connections.size() == 0){
            database.close();
            Intent bookmarkIntent = new Intent(this.getApplicationContext(), bVNC.class);
            startActivity(bookmarkIntent);
        }

        database.close();

        if(!checkConnectionLength)
            connectionAdapter = new ConnectionArrayAdapter(this,
                R.layout.bookmark_home_list_item,
                connections);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "HomeActivity.onPause");

        // reset adapters
        listViewBookmarks.setAdapter(null);
        connectionAdapter = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PARAM_SUPERBAR_TEXT, superBarEditText.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
        superBarEditText.setText(inState.getString(PARAM_SUPERBAR_TEXT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bookmark_home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // refer to http://tools.android.com/tips/non-constant-fields why we can't use switch/case here ..
        int itemId = item.getItemId();
        switch (item.getItemId()) {
            case R.id.inewBookmark:
                Intent bookmarkIntent = new Intent(this, bVNC.class);
                startActivity(bookmarkIntent);
                break;
            case R.id.itemExportImport:
                showDialog(R.layout.importexport);
                break;
            case R.id.ihelp:
                Intent helpIntent = new Intent(this, Help.class);
                startActivity(helpIntent);
                break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case R.layout.importexport:
                return new HomeImportExportDialog(this);
            case R.id.itemMainScreenHelp:
                return createHelpDialog();
        }
        return null;
    }

    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.main_screen_help_text)
                .setPositiveButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // We don't have to do anything.
                            }
                        });
        Dialog d = adb.setView(new ListView (this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }

    private void showWelcomeScreenOrBookmarkList() {
        listViewBookmarks.setVisibility(View.VISIBLE);
//        webViewGetStarted.setVisibility(View.GONE);
    }

    private class SuperBarTextWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            /*if (differentListAdapter != null) {
                String text = s.toString();
                if (text.length() > 0) {
                    ArrayList<BookmarkBase> computers_list = GlobalApp.getQuickConnectHistoryGateway().findHistory(text);
                    computers_list.addAll(GlobalApp.getManualBookmarkGateway().findByLabelOrHostnameLike(text));
                    connectionAdapter.replaceItems(computers_list);
                    QuickConnectBookmark qcBm = new QuickConnectBookmark();
                    qcBm.setLabel(text);
                    qcBm.setHostname(text);
                    connectionAdapter.insert(qcBm, 0);
                } else {
                    connectionAdapter.replaceItems(GlobalApp.getManualBookmarkGateway().findAll());
                    connectionAdapter.insert(addBookmarkPlaceholder, 0);
                }

                differentListAdapter.notifyDataSetChanged();
            }*/
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        /*
        // Pass on the activity result to the helper for handling
        if (!com.mk.iRdp.billingUtil.CheckTrial.mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }*/
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }

    @Override
    protected void onDestroy() {
        if (database != null)
            database.close();
        System.gc();
        super.onDestroy();
    }
}
