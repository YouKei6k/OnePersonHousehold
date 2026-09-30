package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.TrashItem;

import java.util.ArrayList;
import java.util.List;

/**
 * [역할] TRASH_DISPOSAL 테이블 DAO. 통합관리(REQ-007) / 쓰레기배출등록(REQ-010)에서 쓰는 쓰레기 배출 CRUD를 담당한다.
 * 지역 자동계산 로직 자체는 TrashRegionDao가 맡고, 이 클래스는 계산된 결과(배출일, 자동/수동 여부)를 저장·조회만 한다.
 */
public class TrashDao {

    private static final String TAG = "TrashDao";
    private final DatabaseHelper dbHelper;

    public TrashDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long insert(TrashItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("TRASH_DISPOSAL", null, toValues(item));
        if (id == -1) {
            Log.e(TAG, "insert() 실패 : trashType=" + item.getTrashType());
        } else {
            Log.d(TAG, "insert() 성공 : trash_id=" + id + ", trashType=" + item.getTrashType()
                    + ", disposalDate=" + item.getDisposalDate() + ", auto=" + item.isAuto());
        }
        return id;
    }

    public int update(TrashItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updated = db.update("TRASH_DISPOSAL", toValues(item), "trash_id = ?",
                new String[]{String.valueOf(item.getTrashId())});
        Log.d(TAG, "update(trash_id=" + item.getTrashId() + ") -> " + updated + "행 변경");
        return updated;
    }

    public void setDone(int trashId, boolean done) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_done", done ? 1 : 0);
        int updated = db.update("TRASH_DISPOSAL", values, "trash_id = ?", new String[]{String.valueOf(trashId)});
        Log.d(TAG, "setDone(trash_id=" + trashId + ", done=" + done + ") -> " + updated + "행 변경");
    }

    public void delete(int trashId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete("TRASH_DISPOSAL", "trash_id = ?", new String[]{String.valueOf(trashId)});
        Log.d(TAG, "delete(trash_id=" + trashId + ") -> " + deleted + "행 삭제");
    }

    public TrashItem getById(int trashId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM TRASH_DISPOSAL WHERE trash_id = ?",
                new String[]{String.valueOf(trashId)});
        TrashItem item = null;
        if (cursor.moveToFirst()) item = cursorToItem(cursor);
        cursor.close();
        return item;
    }

    public List<TrashItem> getAllByUser(int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM TRASH_DISPOSAL WHERE user_seq = ? ORDER BY is_done ASC, disposal_date ASC",
                new String[]{String.valueOf(userSeq)});

        List<TrashItem> list = new ArrayList<>();
        while (cursor.moveToNext()) list.add(cursorToItem(cursor));
        cursor.close();
        Log.d(TAG, "getAllByUser(userSeq=" + userSeq + ") -> " + list.size() + "건");
        return list;
    }

    /** 이 사용자가 가장 최근에 쓰레기 배출을 등록할 때 지정한 지역 문자열 (없으면 null) */
    public String getLastAreaByUser(int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT area FROM TRASH_DISPOSAL WHERE user_seq = ? AND area IS NOT NULL AND area != '' " +
                        "ORDER BY trash_id DESC LIMIT 1",
                new String[]{String.valueOf(userSeq)});
        String area = null;
        if (cursor.moveToFirst()) area = cursor.getString(0);
        cursor.close();
        return area;
    }

    private ContentValues toValues(TrashItem item) {
        ContentValues values = new ContentValues();
        values.put("user_seq", item.getUserSeq());
        values.put("trash_type", item.getTrashType());
        values.put("area", item.getArea());
        values.put("disposal_date", item.getDisposalDate());
        values.put("is_auto", item.isAuto() ? 1 : 0);
        values.put("dday", item.getDday());
        values.put("is_done", item.isDone() ? 1 : 0);
        return values;
    }

    private TrashItem cursorToItem(Cursor cursor) {
        TrashItem item = new TrashItem();
        item.setTrashId(cursor.getInt(cursor.getColumnIndexOrThrow("trash_id")));
        item.setUserSeq(cursor.getInt(cursor.getColumnIndexOrThrow("user_seq")));
        item.setTrashType(cursor.getString(cursor.getColumnIndexOrThrow("trash_type")));
        item.setArea(cursor.getString(cursor.getColumnIndexOrThrow("area")));
        item.setDisposalDate(cursor.getString(cursor.getColumnIndexOrThrow("disposal_date")));
        item.setAuto(cursor.getInt(cursor.getColumnIndexOrThrow("is_auto")) == 1);
        int ddayIdx = cursor.getColumnIndexOrThrow("dday");
        item.setDday(cursor.isNull(ddayIdx) ? null : cursor.getInt(ddayIdx));
        item.setDone(cursor.getInt(cursor.getColumnIndexOrThrow("is_done")) == 1);
        return item;
    }
}
