package dv606.gc222bz.finalproject;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import dv606.gc222bz.finalproject.database.Run;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;

public class HistoryExpandableList extends BaseExpandableListAdapter {

    private Context context;
    List<Run> runs;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
    SimpleDateFormat dayFormat;
    Calendar calendar = Calendar.getInstance();

    public HistoryExpandableList(Context context, List<Run> runs){
        this.context =context;
        this.runs = runs;
        dayFormat = new SimpleDateFormat("MM-dd-yyyy '"+context.getString(R.string.at_text)+"' HH:mm");
    }

    @Override
    public int getGroupCount() {
        return runs.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return 1;
    }

    @Override
    public Object getGroup(int i) {
        return runs.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return runs.get(i);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean b, View convertView, ViewGroup viewGroup) {
        Run headerTitle = (Run) getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
            convertView.setTag(R.id.name_text, convertView.findViewById(R.id.name_text));
            convertView.setTag(R.id.period_text, convertView.findViewById(R.id.period_text));
        }

        calendar.setTimeInMillis(headerTitle.getStartDate());


        TextView nameTextView = (TextView)convertView.getTag(R.id.name_text);
        nameTextView.setText(headerTitle.getName());


        TextView periodTextView = (TextView)convertView.getTag(R.id.period_text);

        int dayDiff = Utilities.getDiffDay(calendar.getTime(), new Date());


        if(dayDiff < 7){
            periodTextView.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT,  context.getResources().getConfiguration().locale) +","+simpleDateFormat.format(calendar.getTime()));
        }
        else {
            periodTextView.setTextSize(14);
            periodTextView.setText(dayFormat.format(calendar.getTime()));
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final Run child = (Run) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.group_item, null);

            convertView.setTag(R.id.distance_text, convertView.findViewById(R.id.distance_text));
            convertView.setTag(R.id.speed_text, convertView.findViewById(R.id.speed_text));
            convertView.setTag(R.id.time_text, convertView.findViewById(R.id.time_text));
            convertView.setTag(R.id.calories_text, convertView.findViewById(R.id.calories_text));
            convertView.setTag(R.id.map_button, convertView.findViewById(R.id.map_button));
        }

        TextView speedView = (TextView) convertView.getTag(R.id.speed_text);
        TextView caloriesView = (TextView) convertView.getTag(R.id.calories_text);
        TextView timeView = (TextView)convertView.getTag(R.id.time_text);
        TextView distanceView = (TextView)convertView.getTag(R.id.distance_text);
        ImageButton mapButton = (ImageButton)convertView.getTag(R.id.map_button);

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MapsActivity.class);
                intent.putExtra(context.getString(R.string.run_id_extra), child.getId());
                context.startActivity(intent);
            }
        });


        timeView.setText(String.format(context.getString(R.string.running_time_text), Utilities.formatLongToTime(child.getEndDate() - child.getStartDate())));
        caloriesView.setText(String.format(context.getString(R.string.burned_calories_text), PreferenceHelper.getCaloriesWithUnit(context, child.getCalories())));
        speedView.setText(String.format(context.getString(R.string.child_speed_text) , PreferenceHelper.getSpeedWithUnit(context, child.getSpeed())));
        distanceView.setText(String.format(context.getString(R.string.child_distance_text), PreferenceHelper.getDistanceWithUnit(context, (int)child.getDistance())));
        return convertView;
    }


    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false;
    }

    public void removeGroup(int group) {
        runs.remove(group);
        notifyDataSetChanged();
    }

}
