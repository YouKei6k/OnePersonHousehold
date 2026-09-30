package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.Ingredient;

import java.util.ArrayList;
import java.util.List;

/**
 * [역할] INGREDIENT 테이블 DAO. 통합관리(REQ-007) / 식재료등록(REQ-009)에서 쓰는 식재료 CRUD를 담당한다.
 * 소비기한이 가까운 순으로 정렬해서 보여준다.
 */
public class IngredientDao {

    private static final String TAG = "IngredientDao";
    private final DatabaseHelper dbHelper;

    public IngredientDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long insert(Ingredient item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("INGREDIENT", null, toValues(item));
        if (id == -1) {
            Log.e(TAG, "insert() 실패 : title=" + item.getTitle());
        } else {
            Log.d(TAG, "insert() 성공 : ingredient_id=" + id + ", title=" + item.getTitle());
        }
        return id;
    }

    public int update(Ingredient item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updated = db.update("INGREDIENT", toValues(item), "ingredient_id = ?",
                new String[]{String.valueOf(item.getIngredientId())});
        Log.d(TAG, "update(ingredient_id=" + item.getIngredientId() + ") -> " + updated + "행 변경");
        return updated;
    }

    public void setDone(int ingredientId, boolean done) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_done", done ? 1 : 0);
        int updated = db.update("INGREDIENT", values, "ingredient_id = ?", new String[]{String.valueOf(ingredientId)});
        Log.d(TAG, "setDone(ingredient_id=" + ingredientId + ", done=" + done + ") -> " + updated + "행 변경");
    }

    public void delete(int ingredientId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete("INGREDIENT", "ingredient_id = ?", new String[]{String.valueOf(ingredientId)});
        Log.d(TAG, "delete(ingredient_id=" + ingredientId + ") -> " + deleted + "행 삭제");
    }

    public Ingredient getById(int ingredientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM INGREDIENT WHERE ingredient_id = ?",
                new String[]{String.valueOf(ingredientId)});
        Ingredient item = null;
        if (cursor.moveToFirst()) item = cursorToIngredient(cursor);
        cursor.close();
        return item;
    }

    public List<Ingredient> getAllByUser(int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM INGREDIENT WHERE user_seq = ? ORDER BY is_done ASC, expire_date ASC",
                new String[]{String.valueOf(userSeq)});

        List<Ingredient> list = new ArrayList<>();
        while (cursor.moveToNext()) list.add(cursorToIngredient(cursor));
        cursor.close();
        Log.d(TAG, "getAllByUser(userSeq=" + userSeq + ") -> " + list.size() + "건");
        return list;
    }

    private ContentValues toValues(Ingredient item) {
        ContentValues values = new ContentValues();
        values.put("user_seq", item.getUserSeq());
        values.put("title", item.getTitle());
        values.put("purchase_date", item.getPurchaseDate());
        values.put("expire_date", item.getExpireDate());
        values.put("dday", item.getDday());
        values.put("is_done", item.isDone() ? 1 : 0);
        return values;
    }

    private Ingredient cursorToIngredient(Cursor cursor) {
        Ingredient item = new Ingredient();
        item.setIngredientId(cursor.getInt(cursor.getColumnIndexOrThrow("ingredient_id")));
        item.setUserSeq(cursor.getInt(cursor.getColumnIndexOrThrow("user_seq")));
        item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        item.setPurchaseDate(cursor.getString(cursor.getColumnIndexOrThrow("purchase_date")));
        item.setExpireDate(cursor.getString(cursor.getColumnIndexOrThrow("expire_date")));
        int ddayIdx = cursor.getColumnIndexOrThrow("dday");
        item.setDday(cursor.isNull(ddayIdx) ? null : cursor.getInt(ddayIdx));
        item.setDone(cursor.getInt(cursor.getColumnIndexOrThrow("is_done")) == 1);
        return item;
    }
}
