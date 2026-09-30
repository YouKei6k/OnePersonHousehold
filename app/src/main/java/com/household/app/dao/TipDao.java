package com.household.app.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.Tip;

import java.util.ArrayList;
import java.util.List;

/**
 * [역할] TIP 테이블 조회 전용 DAO (읽기만 함, 기본 데이터는 TipSeeder가 넣음).
 * REQ-011(카테고리 목록) / REQ-012(서브카테고리 목록) / REQ-013(상세 내용) 화면에서 사용.
 * 두 목록 화면 모두 검색 기능이 있어야 해서 검색어 유무에 따라 분기하는 메서드를 함께 둔다.
 */
public class TipDao {

    private static final String TAG = "TipDao";

    private final DatabaseHelper dbHelper;

    public TipDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /** 카테고리 목록 (REQ-011). 검색어가 있으면 카테고리명에 포함된 것만. */
    public List<String> getCategories(String keyword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if (keyword == null || keyword.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT DISTINCT category FROM TIP ORDER BY category", null);
        } else {
            cursor = db.rawQuery("SELECT DISTINCT category FROM TIP WHERE category LIKE ? ORDER BY category",
                    new String[]{"%" + keyword.trim() + "%"});
        }
        List<String> result = new ArrayList<>();
        while (cursor.moveToNext()) result.add(cursor.getString(0));
        cursor.close();

        Log.d(TAG, "getCategories(keyword=" + keyword + ") -> " + result.size() + "건");
        if (result.isEmpty()) {
            // TipSeeder가 실패했거나(로그는 TipSeeder 쪽에 남음) DB가 비어있는 상태를 화면단에서 바로 의심할 수 있게 남긴다.
            Log.w(TAG, "getCategories() 결과가 비어있습니다. TipSeeder가 정상 실행됐는지 확인이 필요할 수 있습니다.");
        }
        return result;
    }

    /** 특정 카테고리의 서브카테고리(=팁) 목록 (REQ-012). 검색어가 있으면 서브카테고리/제목에 포함된 것만. */
    public List<Tip> getTipsByCategory(String category, String keyword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if (keyword == null || keyword.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM TIP WHERE category = ? ORDER BY sub_category",
                    new String[]{category});
        } else {
            String like = "%" + keyword.trim() + "%";
            cursor = db.rawQuery(
                    "SELECT * FROM TIP WHERE category = ? AND (sub_category LIKE ? OR title LIKE ?) ORDER BY sub_category",
                    new String[]{category, like, like});
        }
        List<Tip> result = new ArrayList<>();
        while (cursor.moveToNext()) result.add(cursorToTip(cursor));
        cursor.close();
        Log.d(TAG, "getTipsByCategory(category=" + category + ", keyword=" + keyword + ") -> " + result.size() + "건");
        return result;
    }

    public Tip getTipById(int tipId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM TIP WHERE tip_id = ?", new String[]{String.valueOf(tipId)});
        Tip tip = null;
        if (cursor.moveToFirst()) tip = cursorToTip(cursor);
        cursor.close();

        if (tip == null) {
            Log.w(TAG, "getTipById(" + tipId + ") -> 없음");
        }
        return tip;
    }

    private Tip cursorToTip(Cursor cursor) {
        Tip tip = new Tip();
        tip.setTipId(cursor.getInt(cursor.getColumnIndexOrThrow("tip_id")));
        tip.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        tip.setSubCategory(cursor.getString(cursor.getColumnIndexOrThrow("sub_category")));
        tip.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        tip.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        return tip;
    }
}
