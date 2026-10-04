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
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView expressionView, resultView;
    private String current = "0";
    private String first = "";
    private String operator = "";
    private boolean justCalculated = false;
    private String secret = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showCalculator();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView key(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(21);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setBackgroundColor(s.equals("=") ? Color.rgb(35, 115, 76) : Color.rgb(24, 35, 58));
        v.setClickable(true);
        v.setPadding(dp(2), dp(2), dp(2), dp(2));
        return v;
    }

    private void showCalculator() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(Color.rgb(6, 10, 20));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("Calculator");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        screen.setPadding(dp(14), dp(8), dp(14), dp(8));
        screen.setBackgroundColor(Color.rgb(18, 27, 46));
        screen.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        expressionView = new TextView(this);
        expressionView.setText("");
        expressionView.setTextColor(Color.rgb(169, 179, 199));
        expressionView.setTextSize(18);
        expressionView.setGravity(Gravity.RIGHT);
        screen.addView(expressionView, new LinearLayout.LayoutParams(-1, 0, 1));

        resultView = new TextView(this);
        resultView.setText(current);
        resultView.setTextColor(Color.WHITE);
        resultView.setTextSize(38);
        resultView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screen.addView(resultView, new LinearLayout.LayoutParams(-1, 0, 1));

        root.addView(screen, new LinearLayout.LayoutParams(-1, dp(108)));

        // Operators are kept in a dedicated column on the LEFT side for a clean RTL layout.
        String[][] rows = {
            {"÷", "%", "⌫", "C"},
            {"×", "9", "8", "7"},
            {"−", "6", "5", "4"},
            {"+", "3", "2", "1"},
            {"=", "00", ".", "0"}
        };

        for (String[] values : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            row.setPadding(0, dp(3), 0, dp(3));

            for (String s : values) {
                TextView k = key(s);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(k, lp);
                k.setOnClickListener(v -> press(s));
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
        }

        TextView hint = new TextView(this);
        hint.setText("333=333  •  פתיחת האזור הפרטי");
        hint.setTextColor(Color.rgb(130, 143, 166));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(34)));

        setContentView(root);
        updateScreen();
    }

    private void press(String s) {
        secret += s;
        if (secret.length() > 7) secret = secret.substring(secret.length() - 7);
        if (secret.equals("333=333")) {
            secret = "";
            showPrivate();
            return;
        }

        if (s.matches("[0-9]+")) {
            if (justCalculated) {
                current = "0";
                first = "";
                operator = "";
                justCalculated = false;
            }
            if (current.equals("0")) current = "";
            if (current.length() < 14) current += s;
        } else if (s.equals(".")) {
            if (justCalculated) {
                current = "0";
                first = "";
                operator = "";
                justCalculated = false;
            }
            if (!current.contains(".")) current += ".";
        } else if (s.equals("C")) {
            current = "0";
            first = "";
            operator = "";
            justCalculated = false;
        } else if (s.equals("⌫")) {
            if (!justCalculated) {
                current = current.length() > 1 ? current.substring(0, current.length() - 1) : "0";
            }
        } else if (s.equals("%")) {
            try {
                double n = Double.parseDouble(current) / 100.0;
                current = formatNumber(n);
            } catch (Exception ignored) {}
        } else if (s.equals("+") || s.equals("−") || s.equals("×") || s.equals("÷")) {
            if (operator.isEmpty()) {
                first = current;
            } else if (!justCalculated) {
                calculate();
                first = current;
            }
            operator = s;
            justCalculated = false;
        } else if (s.equals("=")) {
            if (!operator.isEmpty()) {
                String left = first;
                String op = operator;
                String right = current;
                calculate();
                expressionView.setText(left + " " + op + " " + right + " =");
                justCalculated = true;
            }
        }
        updateScreen();
    }

    private void calculate() {
        try {
            double a = Double.parseDouble(first);
            double b = Double.parseDouble(current);
            double result;
            if (operator.equals("+")) result = a + b;
            else if (operator.equals("−")) result = a - b;
            else if (operator.equals("×")) result = a * b;
            else if (operator.equals("÷")) {
                if (b == 0) {
                    current = "שגיאה";
                    operator = "";
                    first = "";
                    return;
                }
                result = a / b;
            } else return;

            current = formatNumber(result);
            first = current;
            operator = "";
        } catch (Exception e) {
            current = "שגיאה";
            operator = "";
            first = "";
        }
    }

    private String formatNumber(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) return "שגיאה";
        if (Math.abs(n - Math.rint(n)) < 0.000000001) {
            return String.format(Locale.US, "%.0f", n);
        }
        return String.format(Locale.US, "%.10f", n).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void updateScreen() {
        if (resultView != null) resultView.setText(current);
        if (expressionView != null && !justCalculated) {
            expressionView.setText(operator.isEmpty() ? "" : first + " " + operator);
        }
    }

    private void showPrivate() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(6, 10, 20));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView t = new TextView(this);
        t.setText("אזור פרטי");
        t.setTextColor(Color.WHITE);
        t.setTextSize(26);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.RIGHT);
        root.addView(t, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView info = new TextView(this);
        info.setText("זהו האזור הסודי שנפתח באמצעות 333=333.\\n\\nכדי להשתמש באפליקציה כ־Launcher ולהציג כאן את מגירת האפליקציות, יש להגדיר אותה כאפליקציית הבית במכשיר.");
        info.setTextColor(Color.rgb(169, 179, 199));
        info.setTextSize(15);
        info.setGravity(Gravity.RIGHT);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(140)));

        Button home = new Button(this);
        home.setText("הגדר כאפליקציית בית");
        home.setOnClickListener(v -> requestHome());
        root.addView(home, new LinearLayout.LayoutParams(-1, dp(54)));

        Button back = new Button(this);
        back.setText("חזרה למחשבון");
        back.setOnClickListener(v -> showCalculator());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(54)));

        setContentView(root);
    }

    private void requestHome() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 10);
                    return;
                }
            }
            startActivity(new Intent("android.settings.HOME_SETTINGS"));
        } catch (Exception e) {
            startActivity(new Intent("android.settings.SETTINGS"));
        }
    }
}
