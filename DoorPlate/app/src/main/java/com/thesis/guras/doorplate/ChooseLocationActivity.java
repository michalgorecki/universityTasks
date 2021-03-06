package com.thesis.guras.doorplate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChooseLocationActivity extends AppCompatActivity {
    private ArrayList<ScanResult> currentWifiList = new ArrayList<ScanResult>();

    //create variables corresponding to GUI elements
    private EditText editText;
    private Button refreshButton;
    private ListView chooseLocationListView;
    private PatternCursorAdapter mAdapter = null;
    private Cursor foundPatternsCursor = null;
    private String messageIntentExtra = null;
    private String DEBUG_TAG = "ChooseLocationActivity";
    private MDBHandler mDbHandler = new MDBHandler(this);
    public Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
        @Override
        public int compare(ScanResult firstResult, ScanResult secondResult) {
            return (firstResult.level > secondResult.level ? -1 :
                    (firstResult.level == secondResult.level ? 0 : 1));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_location);
        final WifiManager myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        //check if wifi is enabled
        if (!myWifiManager.isWifiEnabled()) {
            Toast.makeText(ChooseLocationActivity.this, "Please enable Wifi to enable scanning...", Toast.LENGTH_SHORT).show();
        } else {
            //enter the scan results to a list and sort them using the comparator defined before
            currentWifiList = (ArrayList<ScanResult>) myWifiManager.getScanResults();
            Collections.sort(currentWifiList, comparator);
            editText = (EditText) findViewById(R.id.locationNameEditText);
            refreshButton = (Button) findViewById(R.id.refreshChooseLocationButton);
            chooseLocationListView = (ListView) findViewById(R.id.chooseLocationListView);
        }
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            Log.d(DEBUG_TAG, "Intent extras was not null");
            messageIntentExtra = intentExtras.getString("CurrentMessage");
        }

        //suggest similar patterns based on available Wifis
        mDbHandler.open();
        DatabaseDataModel mCurrentDataModel = mDbHandler.setupInsertContent(currentWifiList, "current");
        foundPatternsCursor = mDbHandler.getSimilarPatterns(mCurrentDataModel, "none");
        populateListViewWithPatterns(foundPatternsCursor, chooseLocationListView);
        mDbHandler.close();

        //refreshButton onClick
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(DEBUG_TAG, "refreshButton OnClick()");

                mDbHandler.open();
                currentWifiList = (ArrayList<ScanResult>) myWifiManager.getScanResults();
                Collections.sort(currentWifiList, comparator);
                DatabaseDataModel ddm = mDbHandler.setupInsertContent(currentWifiList, "none");
                if (ddm != null) {
                    currentWifiList = (ArrayList<ScanResult>) myWifiManager.getScanResults();
                    DatabaseDataModel mCurrentDataModel = mDbHandler.setupInsertContent(currentWifiList, "none");
                    foundPatternsCursor = mDbHandler.getSimilarPatterns(mCurrentDataModel, "none");
                    if(foundPatternsCursor != null){
                        Log.d(DEBUG_TAG, "foundPatternsCursor new count: " + foundPatternsCursor.getCount());
                        populateListViewWithPatterns(foundPatternsCursor, chooseLocationListView);
                    }
                }
                mDbHandler.close();
                Log.d(DEBUG_TAG, "refreshButton OnClick()");
            }
        });

        //OnClick for listview
        chooseLocationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(DEBUG_TAG, "onItemClick()");
                //item is selected from the cursor to get necessary data
                Log.d(DEBUG_TAG, "ListView count: " + chooseLocationListView.getCount());
                Log.d(DEBUG_TAG, "foundPatternsCursor count: " + foundPatternsCursor.getCount());
                if (position >= foundPatternsCursor.getCount()) {
                    Log.d(DEBUG_TAG, "Unable to access element " + position + ", it does not exist in the foundPatternsCursor. Cursor count: " + foundPatternsCursor.getCount());
                }
                foundPatternsCursor.moveToPosition(position);
                final String selectedItemName = foundPatternsCursor.getString(1);
                AlertDialog.Builder builder = new AlertDialog.Builder(ChooseLocationActivity.this);
                builder.setTitle("Selected location").setMessage("Do you want to update location data or use it?:");
                builder.setMessage(selectedItemName);

                //Use location onClick
                builder.setPositiveButton("Use", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int x) {
                        onLaunchMessageSender(selectedItemName);
                    }
                });

                //Cancel onClick
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int x) {
                    }
                });
                builder.show();
            }
        });


    }
    private void populateListViewWithPatterns(Cursor listviewInput, ListView listView){
        Log.d(DEBUG_TAG,"populateListViewWithPatterns()");
        if(listviewInput != null && listviewInput.getCount() > 0){
            mAdapter = new PatternCursorAdapter(this,listviewInput,0);
            mAdapter.notifyDataSetChanged();
            listView.setAdapter(mAdapter);
            Log.d(DEBUG_TAG,"ListView count: "+ chooseLocationListView.getCount());
        }
        Log.d(DEBUG_TAG,"populateListViewWithPatterns()");
    }

    //this method is called when the SendMessageActivity is launched from the dialog
    public void onLaunchMessageSender(String locationName){
        mDbHandler.open();
        mDbHandler.removeOldestSimilarPattern(locationName);
        mDbHandler.close();
        Intent intent = new Intent(this, SendMessageActivity.class);
        intent.putExtra("LocationName",locationName);
        if(messageIntentExtra != null){
            intent.putExtra("PreviousMessage",messageIntentExtra);
        }
        ChooseLocationActivity.this.startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onDestroy(){
        mDbHandler.close();
        super.onDestroy();
    }
}
