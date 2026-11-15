package app.veyra;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.json.JSONException;
import org.json.JSONObject;

final class ViewingOverview {
    private final Activity owner;

    ViewingOverview(Activity owner) { this.owner = owner; }

    void show(LinearLayout body, Consumer<JSONObject> resume) {
        LinearLayout activity = card(body);
        TextView heading = Ui.text(owner,"Viewing activity",22,Ui.INK);
        Ui.display(heading);
        activity.addView(heading);
        LinearLayout ranges = new LinearLayout(owner);
        Button week = Ui.chip(owner,"7 days"), month = Ui.chip(owner,"30 days");
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0,-2,1);
        first.setMarginEnd(dp(8));
        ranges.addView(week,first);
        ranges.addView(month,new LinearLayout.LayoutParams(0,-2,1));
        activity.addView(ranges);
        LinearLayout graph = Ui.column(owner);
        graph.setBackgroundColor(Color.TRANSPARENT);
        activity.addView(graph);
        Calendar today = Calendar.getInstance();
        long[] days = ViewingActivityLog.recent(owner,30,today);
        week.setOnClickListener(v -> renderRange(graph,today,days,7,week,month));
        month.setOnClickListener(v -> renderRange(graph,today,days,30,week,month));
        renderRange(graph,today,days,7,week,month);

        List<JSONObject> recent = new ArrayList<>();
        int completed = 0, inProgress = 0;
        for (Object raw : ProfileStore.prefs(owner,"watch-history").getAll().values()) {
            if (!(raw instanceof String)) continue;
            try {
                JSONObject record = new JSONObject((String) raw);
                if (record.optBoolean("completed")) completed++;
                else if (record.optLong("position") > 0) inProgress++;
                if (!record.optString("title").isEmpty()) recent.add(record);
            } catch (JSONException ignored) {}
        }
        int marked = 0;
        for (java.util.Map.Entry<String,?> entry : PersonalStore.prefs(owner).getAll().entrySet()) {
            if (entry.getKey().startsWith("watched:") && Boolean.TRUE.equals(entry.getValue())) marked++;
        }
        LinearLayout totals = card(body);
        metric(totals,"Total watch time",duration(PersonalStore.prefs(owner).getLong("stat:watchMs",0)));
        metric(totals,"Titles in progress",String.valueOf(inProgress));
        metric(totals,"Completed history entries",String.valueOf(completed));
        metric(totals,"Marked watched",String.valueOf(marked));

        TextView recentHeading = Ui.text(owner,"Recently watched",22,Ui.INK);
        Ui.display(recentHeading);
        body.addView(recentHeading);
        recent.sort(Comparator.comparingLong((JSONObject entry)->entry.optLong("updatedAt")).reversed());
        if (recent.isEmpty()) body.addView(Ui.text(owner,"Your titles will appear here after you watch them.",14,Ui.MUTED));
        for (int i=0;i<Math.min(6,recent.size());i++) recentCard(body,recent.get(i),resume);
    }

    private void renderRange(LinearLayout target, Calendar today, long[] days, int range,
                             Button week, Button month) {
        week.setSelected(range==7);
        month.setSelected(range==30);
        target.removeAllViews();
        long[] values = Arrays.copyOfRange(days,days.length-range,days.length);
        long total = Arrays.stream(values).sum();
        long active = Arrays.stream(values).filter(value->value>0).count();
        TextView totalLabel = Ui.text(owner,duration(total),34,Ui.ACCENT);
        Ui.display(totalLabel);
        target.addView(totalLabel);
        target.addView(Ui.text(owner,active+(active==1?" active day":" active days")+" in the last "+range+" days",14,Ui.MUTED));
        if (total==0) {
            target.addView(Ui.text(owner,"Daily totals begin with new playback. Pauses and private viewing are excluded.",14,Ui.MUTED));
            return;
        }
        int window = range==7?1:5;
        long[] bins = new long[range/window];
        for (int i=0;i<values.length;i++) bins[i/window]+=values[i];
        long maximum = Arrays.stream(bins).max().orElse(1);
        LinearLayout chart = new LinearLayout(owner);
        chart.setPadding(0,dp(8),0,0);
        Calendar day = (Calendar) today.clone();
        day.add(Calendar.DAY_OF_YEAR,1-range);
        SimpleDateFormat label = new SimpleDateFormat(range==7?"EEE":"MMM d",Locale.getDefault());
        for (long bin : bins) {
            String start = label.format(day.getTime());
            Calendar end = (Calendar) day.clone();
            end.add(Calendar.DAY_OF_YEAR,window-1);
            String period = window==1?start:start+" to "+label.format(end.getTime());
            LinearLayout column = Ui.column(owner);
            column.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
            column.setBackgroundColor(Color.TRANSPARENT);
            column.setContentDescription(period+", "+duration(bin));
            column.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            View bar = new View(owner);
            GradientDrawable paint = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{bin>0?Ui.ACCENT:Ui.MUTED,Ui.SURFACE});
            paint.setCornerRadius(dp(5));
            bar.setBackground(paint);
            column.addView(bar,new LinearLayout.LayoutParams(dp(15),dp(bin==0?2:Math.max(5,(int)(68*bin/(double)maximum)))));
            TextView caption = Ui.text(owner,start,10,Ui.MUTED);
            caption.setSingleLine();
            caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            column.addView(caption);
            chart.addView(column,new LinearLayout.LayoutParams(0,dp(96),1));
            day.add(Calendar.DAY_OF_YEAR,window);
        }
        target.addView(chart);
    }

    private void recentCard(LinearLayout parent, JSONObject record, Consumer<JSONObject> resume) {
        LinearLayout card = card(parent);
        boolean completed = record.optBoolean("completed");
        long position = record.optLong("position"), runtime = record.optLong("duration");
        TextView title = Ui.text(owner,record.optString("title"),16,Ui.INK);
        Ui.emphasis(title);
        card.addView(title);
        String detail = completed?"Completed":runtime>position?duration(runtime-position)+" left":duration(position)+" watched";
        card.addView(Ui.text(owner,detail,13,Ui.MUTED));
        if (!completed && runtime>0) {
            ProgressBar bar = new ProgressBar(owner,null,android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress((int)Math.max(0,Math.min(100,100.0*position/runtime)));
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
            card.addView(bar,new LinearLayout.LayoutParams(-1,dp(4)));
        }
        if (!completed && position>0 && !record.optString("collection").isEmpty()) {
            Button action = Ui.playerButton(owner,"play","Resume",true);
            action.setContentDescription("Resume "+record.optString("title"));
            action.setOnClickListener(v->resume.accept(record));
            card.addView(action);
        }
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = Ui.card(owner);
        LinearLayout.LayoutParams position = new LinearLayout.LayoutParams(-1,-2);
        position.bottomMargin = dp(12);
        parent.addView(card,position);
        return card;
    }

    private void metric(LinearLayout parent, String title, String value) {
        LinearLayout row = new LinearLayout(owner);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.text(owner,title,14,Ui.MUTED),new LinearLayout.LayoutParams(0,-2,1));
        TextView amount = Ui.text(owner,value,18,Ui.INK);
        Ui.emphasis(amount);
        row.addView(amount);
        parent.addView(row);
    }

    private static String duration(long millis) {
        long seconds = Math.max(0,millis/1000);
        if (seconds<60) return seconds+" sec";
        long minutes = seconds/60;
        return minutes<60?minutes+" min":minutes/60+" hr "+minutes%60+" min";
    }

    private int dp(int value) { return Ui.dp(owner,value); }
}
