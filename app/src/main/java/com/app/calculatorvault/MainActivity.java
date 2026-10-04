package com.app.calculatorvault;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "calculator_vault";
    private static final String HIDDEN_APPS = "hidden_apps";
    private static final String SECRET = "333=333";

    private final int BG = Color.rgb(7, 12, 22);
    private final int SURFACE = Color.rgb(16, 25, 42);
    private final int SURFACE_2 = Color.rgb(20, 31, 51);
    private final int KEY = Color.rgb(24, 37, 60);
    private final int OPERATOR = Color.rgb(42, 67, 109);
    private final int ACCENT = Color.rgb(76, 122, 220);
    private final int EQUALS = Color.rgb(55, 137, 94);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(159, 174, 198);

    private TextView expressionView;
    private TextView resultView;

    private String current = "0";
    private String first = "";
    private String operator = "";
    private boolean justCalculated = false;
    private String secret = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (isHomeIntent(getIntent())) {
            showLauncherHome();
        } else {
            showCalculator();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (isHomeIntent(intent)) {
            showLauncherHome();
        }
    }

    @Override
    public void onBackPressed() {
        // When CalculatorVault is the device Home app, Back from any internal screen
        // returns to the app's main launcher surface instead of closing the launcher.
        if (isHomeApp()) {
            showLauncherHome();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isHomeIntent(Intent intent) {
        return intent != null
                && Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_HOME);
    }

    private boolean isHomeApp() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager rm = getSystemService(RoleManager.class);
                return rm != null && rm.isRoleHeld(RoleManager.ROLE_HOME);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView label(String text, float size, int color, int gravity) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setGravity(gravity);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable bg(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView key(String text, boolean operatorKey, boolean equalsKey) {
        TextView v = label(text, 22, TEXT, Gravity.CENTER);
        v.setBackground(bg(equalsKey ? EQUALS : operatorKey ? OPERATOR : KEY, 18));
        v.setClickable(true);
        v.setFocusable(true);
        v.setPadding(dp(2), dp(2), dp(2), dp(2));
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    break;
                default:
                    break;
            }
            return false;
        });
        return v;
    }

    private void showCalculator() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("Calculator", 20, TEXT, Gravity.CENTER_VERTICAL);
        TextView privateHint = label("Private", 12, MUTED, Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1f));
        top.addView(privateHint, new LinearLayout.LayoutParams(-2, dp(46)));
        root.addView(top);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        screen.setPadding(dp(18), dp(12), dp(18), dp(12));
        screen.setBackground(bg(SURFACE, 22));
        screen.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        expressionView = label("", 17, MUTED, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        resultView = label(current, 40, TEXT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        expressionView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        screen.addView(expressionView, new LinearLayout.LayoutParams(-1, 0, 1f));
        screen.addView(resultView, new LinearLayout.LayoutParams(-1, 0, 1.25f));
        root.addView(screen, new LinearLayout.LayoutParams(-1, dp(128)));

        Space spacer = new Space(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(10)));

        String[][] rows = {
                {"7", "8", "9", "÷"},
                {"4", "5", "6", "×"},
                {"1", "2", "3", "−"},
                {"00", "0", ".", "+"},
                {"C", "%", "⌫", "="}
        };

        for (String[] rowValues : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

            for (int i = 0; i < rowValues.length; i++) {
                String token = rowValues[i];
                boolean isOperator = i == 3;
                boolean isEquals = "=".equals(token);

                TextView k = key(token, isOperator, isEquals);
                k.setOnClickListener(v -> press(token));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(60), 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(k, lp);
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, dp(68)));
        }

        TextView footer = label("333=333  •  פתיחת האזור הפרטי", 12, MUTED, Gravity.CENTER);
        footer.setPadding(0, dp(4), 0, 0);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(30)));

        setContentView(root);
        updateScreen();
    }

    private void press(String s) {
        secret += s;
        if (secret.length() > SECRET.length()) {
            secret = secret.substring(secret.length() - SECRET.length());
        }

        if (SECRET.equals(secret)) {
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
                expressionView.setText("");
            }
            if ("0".equals(current)) {
                current = "";
            }
            if (current.length() < 14) {
                current += s;
            }
        } else if (".".equals(s)) {
            if (justCalculated) {
                current = "0";
                first = "";
                operator = "";
                justCalculated = false;
                expressionView.setText("");
            }
            if (!current.contains(".")) {
                current += ".";
            }
        } else if ("C".equals(s)) {
            current = "0";
            first = "";
            operator = "";
            justCalculated = false;
            expressionView.setText("");
        } else if ("⌫".equals(s)) {
            if (!justCalculated) {
                current = current.length() > 1 ? current.substring(0, current.length() - 1) : "0";
            }
        } else if ("%".equals(s)) {
            try {
                double n = Double.parseDouble(current) / 100d;
                current = formatNumber(n);
            } catch (Exception ignored) {
            }
        } else if ("+".equals(s) || "−".equals(s) || "×".equals(s) || "÷".equals(s)) {
            if ("שגיאה".equals(current)) {
                current = "0";
            }

            if (!operator.isEmpty() && !justCalculated) {
                calculate();
            }

            first = current;
            operator = s;
            justCalculated = false;
            expressionView.setText(first + " " + operator);
        } else if ("=".equals(s)) {
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

            if ("+".equals(operator)) {
                result = a + b;
            } else if ("−".equals(operator)) {
                result = a - b;
            } else if ("×".equals(operator)) {
                result = a * b;
            } else if ("÷".equals(operator)) {
                if (Math.abs(b) < 0.000000000001d) {
                    current = "שגיאה";
                    first = "";
                    operator = "";
                    return;
                }
                result = a / b;
            } else {
                return;
            }

            current = formatNumber(result);
            first = current;
            operator = "";
        } catch (Exception e) {
            current = "שגיאה";
            first = "";
            operator = "";
        }
    }

    private String formatNumber(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) {
            return "שגיאה";
        }
        if (Math.abs(n - Math.rint(n)) < 0.000000001d) {
            return String.format(Locale.US, "%.0f", n);
        }
        DecimalFormat df = new DecimalFormat("0.##########");
        return df.format(n);
    }

    private void updateScreen() {
        if (resultView != null) {
            resultView.setText(current);
        }
        if (expressionView != null && !justCalculated && operator.isEmpty()) {
            expressionView.setText("");
        }
        if (expressionView != null && !operator.isEmpty() && !justCalculated) {
            expressionView.setText(first + " " + operator);
        }
    }

    private LinearLayout basePage() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(16));
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        return root;
    }

    private TextView sectionTitle(String title, String subtitle) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(18), dp(16), dp(18), dp(16));
        block.setBackground(bg(SURFACE, 22));

        TextView t = label(title, 24, TEXT, Gravity.LEFT);
        TextView s = label(subtitle, 14, MUTED, Gravity.LEFT);
        s.setTypeface(Typeface.DEFAULT);
        block.addView(t, new LinearLayout.LayoutParams(-1, dp(36)));
        block.addView(s, new LinearLayout.LayoutParams(-1, dp(48)));
        return block;
    }

    private Button actionButton(String text, int color, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bg(color, 18));
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        b.setOnClickListener(listener);
        return b;
    }

    private void showPrivate() {
        LinearLayout root = basePage();

        TextView title = label("אזור פרטי", 28, TEXT, Gravity.LEFT);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView subtitle = label(
                "גישה מהירה לקבצים ולהגדרות, עם ממשק נקי ומינימליסטי.",
                14, MUTED, Gravity.LEFT);
        subtitle.setTypeface(Typeface.DEFAULT);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(44)));

        TextView card = new TextView(this);
        card.setText("🔒  ההסתרה פועלת בתוך מסך הבית של CalculatorVault.\nהגדר אותו כאפליקציית הבית כדי לשלוט במה שמופיע במסך הבית.");
        card.setTextColor(MUTED);
        card.setTextSize(14);
        card.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackground(bg(SURFACE, 20));
        root.addView(card, new LinearLayout.LayoutParams(-1, dp(94)));

        Space sp = new Space(this);
        root.addView(sp, new LinearLayout.LayoutParams(1, dp(12)));

        Button manage = actionButton("ניהול הסתרת אפליקציות", ACCENT, v -> showAppHider());
        root.addView(manage, new LinearLayout.LayoutParams(-1, dp(54)));

        Space sp2 = new Space(this);
        root.addView(sp2, new LinearLayout.LayoutParams(1, dp(10)));

        Button home = actionButton("הגדר כאפליקציית הבית", SURFACE_2, v -> requestHome());
        root.addView(home, new LinearLayout.LayoutParams(-1, dp(54)));

        Space sp3 = new Space(this);
        root.addView(sp3, new LinearLayout.LayoutParams(1, dp(10)));

        Button back = actionButton("חזרה למחשבון", SURFACE_2, v -> showCalculator());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(54)));

        setContentView(root);
    }

    private void showAppHider() {
        LinearLayout root = basePage();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("הסתרת אפליקציות", 25, TEXT, Gravity.CENTER_VERTICAL);
        TextView count = label("", 13, MUTED, Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        header.addView(count, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(header);

        TextView info = label(
                "לחיצה על "הסתר" מסירה את האפליקציה מהמסך הראשי של CalculatorVault. "
                        + "אפליקציה מוסתרת לא נמחקת מהמכשיר.",
                13, MUTED, Gravity.LEFT);
        info.setTypeface(Typeface.DEFAULT);
        info.setPadding(dp(4), 0, dp(4), dp(12));
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(62)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        List<ResolveInfo> apps = getLaunchableApps();
        Set<String> hidden = getHiddenPackages();
        int hiddenCount = 0;

        for (ResolveInfo infoItem : apps) {
            String pkg = infoItem.activityInfo.packageName;
            if (pkg.equals(getPackageName())) {
                continue;
            }

            String appName = infoItem.loadLabel(getPackageManager()).toString();
            ImageView icon = new ImageView(this);
            try {
                icon.setImageDrawable(infoItem.loadIcon(getPackageManager()));
            } catch (Exception ignored) {
            }

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            row.setBackground(bg(SURFACE, 18));

            row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

            TextView appLabel = label(appName, 15, TEXT, Gravity.LEFT | Gravity.CENTER_VERTICAL);
            appLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, dp(56), 1f);
            nameLp.setMargins(dp(10), 0, dp(8), 0);
            row.addView(appLabel, nameLp);

            boolean isHidden = hidden.contains(pkg);
            if (isHidden) {
                hiddenCount++;
            }

            Button hide = actionButton(isHidden ? "הצג" : "הסתר",
                    isHidden ? SURFACE_2 : ACCENT,
                    v -> {
                        setHidden(pkg, !getHiddenPackages().contains(pkg));
                        showAppHider();
                    });
            row.addView(hide, new LinearLayout.LayoutParams(dp(78), dp(48)));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(68));
            rowLp.setMargins(0, dp(4), 0, dp(4));
            list.addView(row, rowLp);
        }

        count.setText(hiddenCount + " מוסתרות");
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button back = actionButton("חזרה לאזור הפרטי", SURFACE_2, v -> showPrivate());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(52)));

        setContentView(root);
    }

    private List<ResolveInfo> getLaunchableApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);

        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                String aa = a.loadLabel(getPackageManager()).toString();
                String bb = b.loadLabel(getPackageManager()).toString();
                return aa.toLowerCase(Locale.getDefault()).compareTo(bb.toLowerCase(Locale.getDefault()));
            }
        });

        List<ResolveInfo> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : apps) {
            String pkg = ri.activityInfo.packageName;
            if (seen.add(pkg)) {
                unique.add(ri);
            }
        }
        return unique;
    }

    private Set<String> getHiddenPackages() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(HIDDEN_APPS, new HashSet<>()));
    }

    private void setHidden(String packageName, boolean hidden) {
        Set<String> set = getHiddenPackages();
        if (hidden) {
            set.add(packageName);
        } else {
            set.remove(packageName);
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(HIDDEN_APPS, set)
                .apply();
    }

    private void showLauncherHome() {
        LinearLayout root = basePage();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("מסך הבית", 26, TEXT, Gravity.CENTER_VERTICAL);
        Button calc = actionButton("מחשבון", SURFACE_2, v -> showCalculator());
        header.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1f));
        header.addView(calc, new LinearLayout.LayoutParams(dp(100), dp(46)));
        root.addView(header);

        TextView subtitle = label("האפליקציות שלך", 14, MUTED, Gravity.LEFT);
        subtitle.setTypeface(Typeface.DEFAULT);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(34)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        Set<String> hidden = getHiddenPackages();
        int shown = 0;

        for (ResolveInfo ri : getLaunchableApps()) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || hidden.contains(pkg)) {
                continue;
            }

            String appName = ri.loadLabel(getPackageManager()).toString();
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(14), dp(8), dp(14), dp(8));
            item.setBackground(bg(SURFACE, 20));

            ImageView icon = new ImageView(this);
            try {
                icon.setImageDrawable(ri.loadIcon(getPackageManager()));
            } catch (Exception ignored) {
            }
            item.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

            TextView appLabel = label(appName, 16, TEXT, Gravity.LEFT | Gravity.CENTER_VERTICAL);
            appLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(60), 1f);
            lp.setMargins(dp(12), 0, 0, 0);
            item.addView(appLabel, lp);

            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                item.setOnClickListener(v -> startActivity(launch));
            }

            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(-1, dp(70));
            itemLp.setMargins(0, dp(5), 0, dp(5));
            list.addView(item, itemLp);
            shown++;
        }

        if (shown == 0) {
            TextView empty = label(
                    "אין כרגע אפליקציות להצגה.\nפתח "ניהול הסתרת אפליקציות" כדי לנהל את הרשימה.",
                    15, MUTED, Gravity.CENTER);
            empty.setTypeface(Typeface.DEFAULT);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(-1, dp(180)));
        }

        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button settings = actionButton("ניהול הסתרת אפליקציות", SURFACE_2, v -> showAppHider());
        root.addView(settings, new LinearLayout.LayoutParams(-1, dp(52)));

        setContentView(root);
    }

    private void requestHome() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)
                        && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivityForResult(
                            rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 10);
                    return;
                }
            }
            startActivity(new Intent("android.settings.HOME_SETTINGS"));
        } catch (Exception e) {
            startActivity(new Intent("android.settings.SETTINGS"));
        }
    }
}
