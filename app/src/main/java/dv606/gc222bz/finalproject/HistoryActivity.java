package dv606.gc222bz.finalproject;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.List;

import dv606.gc222bz.finalproject.database.Run;
import dv606.gc222bz.finalproject.database.RunsDataSource;
import dv606.gc222bz.finalproject.utilities.Costants;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;

public class HistoryActivity extends AppCompatActivity {

    private RunsDataSource runsDataSource;
    private ExpandableListView mList;
    private HistoryExpandableList adapter;
    AlertDialog dialog = null;

    private final int MENU_DELETE = 0, MENU_RENAME = 1;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        runsDataSource = new RunsDataSource(HistoryActivity.this);
        runsDataSource.open();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mList = (ExpandableListView) findViewById(R.id.history_list);
        registerForContextMenu(mList);
    }

    @Override
    public void onResume(){
        super.onResume();
        adapter = new HistoryExpandableList(HistoryActivity.this, new ArrayList<Run>());
        mList.setAdapter(adapter);
        refreshList(PreferenceHelper.getOrderType(this));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.action_context_title);
        menu.add(0, MENU_DELETE, 0, R.string.context_delete_message);
        menu.add(0, MENU_RENAME, 0, R.string.context_rename_message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        this.menu = menu;
        checkExclusive(PreferenceHelper.getOrderType(this));
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId =item.getItemId();
        switch (itemId){
            case R.id.option_settings:{
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.sort_speed:{
                PreferenceHelper.setOrderType(this, itemId);
                refreshList(itemId);
                checkExclusive(itemId);
                return true;
            }case R.id.sort_distance:{
                PreferenceHelper.setOrderType(this, itemId);
                refreshList(itemId);
                checkExclusive(itemId);
                return true;
            }case R.id.sort_calories:{
                PreferenceHelper.setOrderType(this, itemId);
                refreshList(itemId);
                checkExclusive(itemId);
                return true;
            }case R.id.sort_time:{
                PreferenceHelper.setOrderType(this, itemId);
                refreshList(itemId);
                checkExclusive(itemId);
                return true;
            }case R.id.sort_date:{
                PreferenceHelper.setOrderType(this, itemId);
                refreshList(itemId);
                checkExclusive(itemId);
                return true;
            }
            default: return false;
        }
    }

    public void checkExclusive(int checkedId){
        for(int i = 0; i < menu.size(); i++){
            MenuItem item = menu.getItem(i);
            if(item.isCheckable()){

                if(item.getItemId() == checkedId){
                    item.setChecked(true);
                }
                else{
                    item.setChecked(false);
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        Run run = (Run)mList.getItemAtPosition(groupPosition);
        switch (item.getItemId()){
            case MENU_DELETE:{
                runsDataSource.deleteRun(run.getId());
                adapter.removeGroup(groupPosition);
                return  true;
            }
            case MENU_RENAME:{
                dialog = makeNewNameDialog(groupPosition, run.getId()).show();
                return true;
            }
        }

        return  false;
    }

    public void refreshList(int orderType){
        List<Run> runsList ;
        switch(orderType){
            case R.id.sort_speed:{
                runsList= runsDataSource.getAllRun(RunsDataSource.SPEED_SORT);
                break;
            }case R.id.sort_distance:{
                runsList= runsDataSource.getAllRun(RunsDataSource.DISTANCE_SORT);
                break;
            }case R.id.sort_calories:{
                runsList= runsDataSource.getAllRun(RunsDataSource.CALORIES_SORT);
                break;
            }case R.id.sort_time:{
                runsList= runsDataSource.getAllRun(RunsDataSource.TIME_SORT);
                break;
            }case R.id.sort_date:{
                runsList= runsDataSource.getAllRun(RunsDataSource.DATE_SORT);
                break;
            }
            default:{
                runsList = new ArrayList<>();
                break;
            }

        }
        adapter.addAll(runsList);
    }

    public AlertDialog.Builder makeNewNameDialog(final int position, final long runId) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setCancelable(true);

        final EditText edittext = new EditText(this);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(Costants.RUN_TITLE_LENGTH);
        edittext.setFilters(FilterArray);
        edittext.setHint(R.string.run_name_default_text);
        alert.setMessage(getString(R.string.change_name_title));
        alert.setTitle(getString(R.string.change_name_message));
        alert.setView(edittext, 90, 0, 90, 0);


        alert.setPositiveButton(getString(R.string.confirm_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                final String activityName = edittext.getText().toString();

                if(!TextUtils.isEmpty(activityName.trim())) {
                    adapter.renameGroup( position,activityName );
                    runsDataSource.renameRun(runId, activityName);
                }
                else{
                    adapter.renameGroup( position,getString(R.string.run_name_default_text) );
                    runsDataSource.renameRun(runId, getString(R.string.run_name_default_text));
                }


            }
        });

        alert.setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });


        return alert;
    }
}
