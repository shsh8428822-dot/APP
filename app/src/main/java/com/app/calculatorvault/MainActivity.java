package com.app.calculatorvault;

import android.app.Activity;
import android.app.role.RoleManager;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.Intent;

public class MainActivity extends Activity {
    private TextView display;
    private String value="0";
    private String secret="";

    @Override public void onCreate(Bundle b){ super.onCreate(b); showCalculator(); }
    private int dp(int n){ return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView key(String s){
        TextView v=new TextView(this); v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(21); v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(Color.rgb(24,35,58)); v.setClickable(true); v.setPadding(4,4,4,4); return v;
    }
    private void showCalculator(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(18)); root.setBackgroundColor(Color.rgb(6,10,20));
        TextView title=new TextView(this); title.setText("Calculator"); title.setTextColor(Color.WHITE); title.setTextSize(18); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title,new LinearLayout.LayoutParams(-1,dp(50)));
        display=new TextView(this); display.setText(value); display.setTextColor(Color.WHITE); display.setTextSize(42); display.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); display.setBackgroundColor(Color.rgb(18,27,46)); root.addView(display,new LinearLayout.LayoutParams(-1,dp(100)));
        String[][] rows={{"C","⌫","%","÷"},{"7","8","9","×"},{"4","5","6","−"},{"1","2","3","+"},{"0",".","00","="}};
        for(String[] r:rows){ LinearLayout row=new LinearLayout(this); row.setWeightSum(4); for(String s:r){ TextView k=key(s); row.addView(k,new LinearLayout.LayoutParams(0,0,1)); k.setOnClickListener(v->press(s)); } root.addView(row,new LinearLayout.LayoutParams(-1,0,1)); }
        TextView hint=new TextView(this); hint.setText("הקלד 333=333 לפתיחת האזור הפרטי"); hint.setTextColor(Color.rgb(169,179,199)); hint.setGravity(Gravity.CENTER); root.addView(hint,new LinearLayout.LayoutParams(-1,dp(42)));
        setContentView(root);
    }
    private void press(String s){
        secret += s; if(secret.length()>7) secret=secret.substring(secret.length()-7); if(secret.equals("333=333")){ secret=""; showPrivate(); return; }
        if(s.matches("[0-9]+")){ if(value.equals("0")) value=""; value+=s; }
        else if(s.equals("C")) value="0"; else if(s.equals("⌫")) value=value.length()>1?value.substring(0,value.length()-1):"0";
        display.setText(value);
    }
    private void showPrivate(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(20),dp(20),dp(20)); root.setBackgroundColor(Color.rgb(6,10,20));
        TextView t=new TextView(this); t.setText("אזור פרטי"); t.setTextColor(Color.WHITE); t.setTextSize(26); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(t,new LinearLayout.LayoutParams(-1,dp(60)));
        TextView info=new TextView(this); info.setText("זהו האזור הסודי שנפתח באמצעות 333=333.\n\nכדי להשתמש באפליקציה כ־Launcher ולהציג כאן את מגירת האפליקציות, יש להגדיר אותה כאפליקציית הבית במכשיר."); info.setTextColor(Color.rgb(169,179,199)); info.setTextSize(15); root.addView(info,new LinearLayout.LayoutParams(-1,dp(140)));
        Button home=new Button(this); home.setText("הגדר כאפליקציית בית"); home.setOnClickListener(v->requestHome()); root.addView(home,new LinearLayout.LayoutParams(-1,dp(54)));
        Button back=new Button(this); back.setText("חזרה למחשבון"); back.setOnClickListener(v->showCalculator()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));
        setContentView(root);
    }
    private void requestHome(){
        try{
            if(android.os.Build.VERSION.SDK_INT>=29){ RoleManager rm=getSystemService(RoleManager.class); if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME)){ startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),10); return; }}
            startActivity(new Intent("android.settings.HOME_SETTINGS"));
        }catch(Exception e){ startActivity(new Intent("android.settings.SETTINGS")); }
    }
}
