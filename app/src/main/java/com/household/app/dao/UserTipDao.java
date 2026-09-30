package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.UserTip;

import java.util.ArrayList;
import java.util.List;

/**
 * [역할] USER_TIP 테이블 DAO. "내가 쓴 팁(수정, 삭제 가능)" (REQ-013)을 담당한다.
 * TipDao와 달리 이 테이블은 앱 사용자가 직접 쓰고 지우는 데이터라 insert/update/delete가 모두 있다.
 */
public class UserTipDao {

    private static final String TAG = "UserTipDao";

    private final DatabaseHelper dbHelper;

    public UserTipDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /** 특정 팁에 내가 쓴 팁 목록 (다른 사용자가 쓴 것은 제외) */
    public List<UserTip> getMyTips(int tipId, int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM USER_TIP WHERE tip_id = ? AND user_seq = ? ORDER BY user_tip_id ASC",
                new String[]{String.valueOf(tipId), String.valueOf(userSeq)});

        List<UserTip> result = new ArrayList<>();
        while (cursor.moveToNext()) {
            UserTip t = new UserTip();
            t.setUserTipId(cursor.getInt(cursor.getColumnIndexOrThrow("user_tip_id")));
            t.setTipId(cursor.getInt(cursor.getColumnIndexOrThrow("tip_id")));
            t.setUserSeq(cursor.getInt(cursor.getColumnIndexOrThrow("user_seq")));
            t.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            result.add(t);
        }
        cursor.close();
        Log.d(TAG, "getMyTips(tipId=" + tipId + ", userSeq=" + userSeq + ") -> " + result.size() + "건");
        return result;
    }

    public long insert(int tipId, int userSeq, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tip_id", tipId);
        values.put("user_seq", userSeq);
        values.put("content", content);
        long id = db.insert("USER_TIP", null, values);
        if (id == -1) {
            Log.e(TAG, "insert() 실패 : tipId=" + tipId + ", userSeq=" + userSeq);
        } else {
            Log.d(TAG, "insert() 성공 : user_tip_id=" + id + " (tipId=" + tipId + ", userSeq=" + userSeq + ")");
        }
        return id;
    }

    public int update(int userTipId, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("content", content);
        int updated = db.update("USER_TIP", values, "user_tip_id = ?", new String[]{String.valueOf(userTipId)});
        if (updated == 0) {
            Log.w(TAG, "update() 변경된 행이 없음 : user_tip_id=" + userTipId);
        } else {
            Log.d(TAG, "update() 성공 : user_tip_id=" + userTipId);
        }
        return updated;
    }

    public int delete(int userTipId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete("USER_TIP", "user_tip_id = ?", new String[]{String.valueOf(userTipId)});
        Log.d(TAG, "delete(user_tip_id=" + userTipId + ") -> " + deleted + "행 삭제");
        return deleted;
    }
}
