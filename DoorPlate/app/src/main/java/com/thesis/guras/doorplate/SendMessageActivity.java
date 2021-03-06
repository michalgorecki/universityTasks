package com.thesis.guras.doorplate;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.ScanResult;
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

import java.util.Comparator;

public class SendMessageActivity extends AppCompatActivity {
    public Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
        @Override
        public int compare(ScanResult firstResult, ScanResult secondResult) {
            return (firstResult.level > secondResult.level ? -1 :
                    (firstResult.level == secondResult.level ? 0 : 1));
        }
    };
    private String currentMessage = "";
    private String DEBUG_TAG = "SendMessageActivity";
    private MessageCursorAdapter mMessageCursorAdapter = null;
    private Cursor messagesCursor = null;
    private MDBHandler mdbHandler = new MDBHandler(this);

    //MDBHandler mdbHandler = new MDBHandler(this);
    EditText messageEditText;
    Button chooseLocationButton;
    Button addMessageButton;
    ListView templatesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        messageEditText = (EditText) findViewById(R.id.sendToLocationEditText);
        chooseLocationButton = (Button) findViewById(R.id.chooseLocationButton);
        templatesListView = (ListView) findViewById(R.id.templatesListView);
        addMessageButton = (Button) findViewById(R.id.addMessageButton);

        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring previous instance state");
            currentMessage = savedInstanceState.getString("CurrentMessage");
            messageEditText.setText(currentMessage);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

        }
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            Log.d(DEBUG_TAG, "Intent extras was not null");
            String selectedLocationExtra = intentExtras.getString("LocationName");
            String eventData = intentExtras.getString("EventData");
            String previousMessage = intentExtras.getString("PreviousMessage");
            if (eventData == null) {
                eventData = "";
            }
            if (selectedLocationExtra == null) {
                selectedLocationExtra = "";
            }
            if (previousMessage == null) {
                previousMessage = "";
            }
            messageEditText.setText(previousMessage + " " + selectedLocationExtra + " " + eventData);
        }
        mdbHandler.open();
        messagesCursor = mdbHandler.getAllMessages();
        mMessageCursorAdapter = new MessageCursorAdapter(this, messagesCursor, 0);
        templatesListView.setAdapter(mMessageCursorAdapter);
        mdbHandler.close();
        templatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(DEBUG_TAG, "templatesListView onClick()");
                //item is selected from the cursor to get necessary data
                Log.d(DEBUG_TAG, "ListView count: " + templatesListView.getCount());
                Log.d(DEBUG_TAG, "messagesCursor count: " + messagesCursor.getCount());
                if (position >= messagesCursor.getCount()) {
                    Log.d(DEBUG_TAG, "Unable to access element " + position + ", it does not exist in the messagesCursor. Cursor count: " + messagesCursor.getCount());
                }
                final int selectedTemplateId = messagesCursor.getInt(0);
                messagesCursor.moveToPosition(position);
                final String selectedItemName = messagesCursor.getString(1);
                AlertDialog.Builder builder = new AlertDialog.Builder(SendMessageActivity.this);
                builder.setTitle(selectedItemName).setMessage("Do you want to use template: " + selectedItemName + "?");

                //Use template onClick
                builder.setPositiveButton("Use", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int x) {
                        messageEditText.setText(selectedItemName);
                    }
                });

                builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int x) {
                        mdbHandler.open();
                        Log.d(DEBUG_TAG, "Trying to delete message template no." + selectedTemplateId);
                        if (!mdbHandler.deleteMessageTemplate(selectedTemplateId)) {
                            Log.d(DEBUG_TAG, "Failed to delete pattern no. " + messagesCursor.getInt(0));
                        } else {
                            Log.d(DEBUG_TAG, "Message template no. " + selectedTemplateId + " has been sucessfully deleted!");
                        }
                        refreshTemplatesListView();
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

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        currentMessage = savedInstanceState.getString("CurrentMessage"); //retrieve your data using its corresponding key
        Log.d(DEBUG_TAG, "CurrentMessage restored from savedInstanceState: " + savedInstanceState.getString("CurrentMessage"));
        Log.d(DEBUG_TAG, "onRestoreInstanceState()");
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState()");
        String savedString = messageEditText.getText().toString();
        savedInstanceState.putString("CurrentMessage", savedString);

        super.onSaveInstanceState(savedInstanceState);
        Log.d(DEBUG_TAG, "onSaveInstanceState()");
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

    public void addMessageOnClick(View v) {
        Log.d(DEBUG_TAG, "addMessageOnClick()");
        final String message = messageEditText.getText().toString();
        Log.d(DEBUG_TAG, "Message: " + message + ".");
        if (message != null && !message.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SendMessageActivity.this);
            builder.setTitle(message).setMessage("Do you want to save: " + message + " as template?");
            builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int x) {
                    mdbHandler.open();
                    long recordId = mdbHandler.insertMessageTemplate(message);
                    if (recordId > 0) {
                        Log.d(DEBUG_TAG, "Message sucessfully inserted with record id: " + recordId);
                    } else {
                        Log.d(DEBUG_TAG, "Failed to insert message template to database");
                    }
                    mdbHandler.close();
                    refreshTemplatesListView();

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int x) {

                }
            });
            builder.show();
        }


        Log.d(DEBUG_TAG, "addMessageOnClick()");
    }

    public void goToLocationsOnClick(View v) {
        Intent intent = new Intent(this, ChooseLocationActivity.class);
        String message = messageEditText.getText().toString();
        if (message != null) {
            intent.putExtra("CurrentMessage", message);
        }

        SendMessageActivity.this.startActivity(intent);
    }

    public void sendMessageOnClick(View v) {
        Log.d(DEBUG_TAG, "sendMessageOnClick");
        String message = messageEditText.getText().toString();
        if (message != null && !message.isEmpty()) {
            Log.d(DEBUG_TAG, "Message to be sent: " + message);
        }
        messageEditText.setText("");
        Log.d(DEBUG_TAG, "sendMessageOnClick");
    }

    public boolean refreshTemplatesListView() {
        mdbHandler.open();
        messagesCursor = mdbHandler.getAllMessages();
        if (messagesCursor != null) {
            Log.d(DEBUG_TAG, "MessagesCursor was not null");
            mMessageCursorAdapter = new MessageCursorAdapter(SendMessageActivity.this, messagesCursor, 0);
            mMessageCursorAdapter.notifyDataSetChanged();
            templatesListView.setAdapter(mMessageCursorAdapter);
            mdbHandler.close();
            return true;
        } else {
            Log.d(DEBUG_TAG, "MessagesCursor was null");
            mMessageCursorAdapter = null;
            templatesListView.setAdapter(mMessageCursorAdapter);
            mdbHandler.close();
            return false;
        }
    }


}


