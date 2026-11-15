package app.veyra;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.IntConsumer;

final class ActionSheet {
    static Dialog show(Activity activity,String title,String[] labels,IntConsumer action) {
        return show(activity,title,"",labels,-1,action);
    }

    static Dialog show(Activity activity,String title,String summary,String[] labels,int selected,IntConsumer action) {
        return show(activity,title,summary,labels,null,selected,action);
    }
    static Dialog show(Activity activity,String title,String summary,String[] labels,String[] icons,int selected,IntConsumer action) {
        Dialog dialog=new Dialog(activity);
        LinearLayout panel=Ui.column(activity);
        GradientDrawable background=new GradientDrawable();
        background.setColor(Ui.BG);
        background.setCornerRadius(Ui.dp(activity,24));
        background.setStroke(Ui.dp(activity,1),Ui.SURFACE);
        panel.setBackground(background);
        panel.setPadding(Ui.dp(activity,16),Ui.dp(activity,8),Ui.dp(activity,16),Ui.dp(activity,12));

        LinearLayout heading=new LinearLayout(activity);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=Ui.text(activity,title,20,Ui.INK);
        Ui.display(name);
        heading.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        Button close=Ui.icon(activity,"close","Close "+title);
        close.setOnClickListener(v->dialog.dismiss());
        heading.addView(close,new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));
        panel.addView(heading);
        if(!summary.isEmpty()){
            TextView description=Ui.text(activity,summary,13,Ui.MUTED);
            description.setPadding(0,0,0,Ui.dp(activity,9));
            panel.addView(description);
        }

        ScrollView scroll=new ScrollView(activity);
        LinearLayout rows=Ui.column(activity);
        rows.setBackgroundColor(Color.TRANSPARENT);
        for(int i=0;i<labels.length;i++){
            final int index=i;
            Button row=icons==null?Ui.chip(activity,labels[i]):Ui.playerButton(activity,icons[i],labels[i],true);
            row.setSelected(i==selected);
            row.setSingleLine(false);
            row.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            row.setTextSize(15);
            row.setOnClickListener(v->{dialog.dismiss();action.accept(index);});
            row.setMinimumHeight(Ui.dp(activity,52));
            rows.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        scroll.addView(rows);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        dialog.setContentView(panel);
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();
            params.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
            params.dimAmount=.52f;
            window.setAttributes(params);
        }
        dialog.show();
        if(window!=null){
            int width=Math.min(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,16),Ui.dp(activity,520));
            int maxHeight=(int)(activity.getResources().getDisplayMetrics().heightPixels*.84f);
            panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST));
            window.setLayout(width,panel.getMeasuredHeight());
        }
        return dialog;
    }
}
