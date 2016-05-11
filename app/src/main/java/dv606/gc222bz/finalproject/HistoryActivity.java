package dv606.gc222bz.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import java.util.List;

import dv606.gc222bz.finalproject.database.Run;
import dv606.gc222bz.finalproject.database.RunsDataSource;

public class HistoryActivity extends AppCompatActivity {

    private RunsDataSource runsDataSource;
    private ExpandableListView mList;
    private HistoryExpandableList adapter;

    private final int MENU_DELETE = 0;

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

        List<Run> runsList = runsDataSource.getAllRun();
        adapter = new HistoryExpandableList(HistoryActivity.this, runsList);
        mList.setAdapter(adapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.action_context_title);
        menu.add(0, MENU_DELETE, 0, R.string.context_delete_message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_settings:{
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default: return false;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();

        switch (item.getItemId()){
            case MENU_DELETE:{
                Run run = (Run)mList.getItemAtPosition((int)info.packedPosition);
                runsDataSource.deleteRun(run.getId());
                adapter.removeGroup((int)info.packedPosition);
                return  true;
            }
        }

        return  false;
    }
}
