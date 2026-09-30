package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * [역할] CALENDAR_STICKER 테이블 DAO. "캘린더 스티커 추가 / 삭제" (REQ-004)
 * 지금 범위에서는 스티커 종류를 하나(기본 별표)로 단순화해서, 날짜별로 있음/없음만 토글한다.
 */
public class StickerDao {

    private static final String TAG = "StickerDao";
    public static final String DEFAULT_STICKER_TYPE = "STAR";

    private final DatabaseHelper dbHelper;

    public StickerDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /** yyyy-MM 월에 스티커가 있는 날짜들의 집합 (캘린더에 표시용) */
    public Set<String> getStickerDatesInMonth(int userSeq, String yearMonth) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Set<String> result = new HashSet<>();
        Cursor cursor = db.query(
                "CALENDAR_STICKER",
                new String[]{"date"},
                "user_seq = ? AND date LIKE ?",
                new String[]{String.valueOf(userSeq), yearMonth + "%"},
                null, null, null
        );
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        return result;
    }

    private boolean hasSticker(int userSeq, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "CALENDAR_STICKER",
                new String[]{"sticker_id"},
                "user_seq = ? AND date = ?",
                new String[]{String.valueOf(userSeq), date},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 스티커 추가/삭제 토글 (REQ-004).
     * @return 토글 후 상태 (true = 스티커 있음)
     */
    public boolean toggleSticker(int userSeq, String date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (hasSticker(userSeq, date)) {
            int deleted = db.delete("CALENDAR_STICKER",
                    "user_seq = ? AND date = ?",
                    new String[]{String.valueOf(userSeq), date});
            Log.d(TAG, "toggleSticker(date=" + date + ") 해제 -> " + deleted + "행 삭제");
            return false;
        } else {
            ContentValues values = new ContentValues();
            values.put("user_seq", userSeq);
            values.put("date", date);
            values.put("sticker_type", DEFAULT_STICKER_TYPE);
            long id = db.insert("CALENDAR_STICKER", null, values);
            if (id == -1) {
                Log.e(TAG, "toggleSticker(date=" + date + ") 추가 실패");
            } else {
                Log.d(TAG, "toggleSticker(date=" + date + ") 추가 -> sticker_id=" + id);
            }
            return true;
        }
    }
}
