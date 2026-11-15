package app.veyra;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Recent daily totals for this profile, with no title or playback URL stored. */
final class ViewingActivityLog {
    private static final long DAY_LIMIT_MS = 86_400_000L;
    private static final int RETAINED_DAYS = 35;

    private ViewingActivityLog() {}

    private static SharedPreferences prefs(Context context) {
        return ProfileStore.prefs(context, "watch-activity");
    }

    static synchronized void record(Context context, long elapsedMs, long endedAt) {
        if (elapsedMs <= 0) return;
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(endedAt);
        Calendar midnight = (Calendar) day.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long today = Math.min(elapsedMs, Math.max(0, endedAt - midnight.getTimeInMillis()));
        SharedPreferences totals = prefs(context);
        SharedPreferences.Editor edit = totals.edit();
        List<String> dates = new ArrayList<>(totals.getAll().keySet());
        if (today > 0) add(totals, edit, dates, date(day), today);
        if (elapsedMs > today) {
            day.add(Calendar.DAY_OF_YEAR, -1);
            add(totals, edit, dates, date(day), elapsedMs - today);
        }
        if (dates.size() > RETAINED_DAYS) {
            Collections.sort(dates);
            for (int i = 0; i < dates.size() - RETAINED_DAYS; i++) edit.remove(dates.get(i));
        }
        edit.apply();
        SharedPreferences lifetime = PersonalStore.prefs(context);
        long previous = Math.max(0,lifetime.getLong("stat:watchMs",0));
        lifetime.edit().putLong("stat:watchMs",Math.min(3_600_000_000_000L,previous+elapsedMs)).apply();
    }

    private static void add(SharedPreferences totals, SharedPreferences.Editor edit,
            List<String> dates, String key, long elapsedMs) {
        long previous = totals.getLong(key, 0);
        if (previous == 0) dates.add(key);
        edit.putLong(key, Math.min(DAY_LIMIT_MS, previous + elapsedMs));
    }

    private static String date(Calendar day) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(day.getTime());
    }

    static long[] recent(Context context, int count, Calendar last) {
        long[] values = new long[count];
        Calendar day = (Calendar) last.clone();
        SharedPreferences totals = prefs(context);
        for (int i = count - 1; i >= 0; i--) {
            values[i] = totals.getLong(date(day), 0);
            day.add(Calendar.DAY_OF_YEAR, -1);
        }
        return values;
    }

    static synchronized void clear(Context context) {
        prefs(context).edit().clear().apply();
        PersonalStore.prefs(context).edit().remove("stat:watchMs").apply();
    }
}
