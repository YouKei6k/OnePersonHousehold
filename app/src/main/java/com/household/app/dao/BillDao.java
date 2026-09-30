package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.Bill;

import java.util.ArrayList;
import java.util.List;

/**
 * [역할] UTILITY_BILL 테이블 DAO. 통합관리(REQ-007) / 공과금등록(REQ-008)에서 쓰는 공과금 CRUD를 담당한다.
 * 목록 정렬 규칙(REQ-007 비고) : 미완료는 마감일이 가까운 순, 완료는 맨 아래로.
 */
public class BillDao {

    private static final String TAG = "BillDao";
    private final DatabaseHelper dbHelper;

    public BillDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long insert(Bill bill) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("UTILITY_BILL", null, toValues(bill));
        if (id == -1) {
            Log.e(TAG, "insert() 실패 : title=" + bill.getTitle());
        } else {
            Log.d(TAG, "insert() 성공 : bill_id=" + id + ", title=" + bill.getTitle());
        }
        return id;
    }

    public int update(Bill bill) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updated = db.update("UTILITY_BILL", toValues(bill), "bill_id = ?",
                new String[]{String.valueOf(bill.getBillId())});
        Log.d(TAG, "update(bill_id=" + bill.getBillId() + ") -> " + updated + "행 변경");
        return updated;
    }

    public void setPaid(int billId, boolean paid) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_paid", paid ? 1 : 0);
        int updated = db.update("UTILITY_BILL", values, "bill_id = ?", new String[]{String.valueOf(billId)});
        Log.d(TAG, "setPaid(bill_id=" + billId + ", paid=" + paid + ") -> " + updated + "행 변경");
    }

    public void delete(int billId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete("UTILITY_BILL", "bill_id = ?", new String[]{String.valueOf(billId)});
        Log.d(TAG, "delete(bill_id=" + billId + ") -> " + deleted + "행 삭제");
    }

    public Bill getById(int billId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM UTILITY_BILL WHERE bill_id = ?",
                new String[]{String.valueOf(billId)});
        Bill bill = null;
        if (cursor.moveToFirst()) bill = cursorToBill(cursor);
        cursor.close();
        return bill;
    }

    /** 미완료(마감 임박순) 먼저, 완료는 아래로 */
    public List<Bill> getAllByUser(int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM UTILITY_BILL WHERE user_seq = ? ORDER BY is_paid ASC, payment_date ASC",
                new String[]{String.valueOf(userSeq)});

        List<Bill> list = new ArrayList<>();
        while (cursor.moveToNext()) list.add(cursorToBill(cursor));
        cursor.close();
        Log.d(TAG, "getAllByUser(userSeq=" + userSeq + ") -> " + list.size() + "건");
        return list;
    }

    private ContentValues toValues(Bill bill) {
        ContentValues values = new ContentValues();
        values.put("user_seq", bill.getUserSeq());
        values.put("title", bill.getTitle());
        values.put("payment_date", bill.getPaymentDate());
        values.put("dday", bill.getDday());
        values.put("is_paid", bill.isPaid() ? 1 : 0);
        return values;
    }

    private Bill cursorToBill(Cursor cursor) {
        Bill bill = new Bill();
        bill.setBillId(cursor.getInt(cursor.getColumnIndexOrThrow("bill_id")));
        bill.setUserSeq(cursor.getInt(cursor.getColumnIndexOrThrow("user_seq")));
        bill.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        bill.setPaymentDate(cursor.getString(cursor.getColumnIndexOrThrow("payment_date")));
        int ddayIdx = cursor.getColumnIndexOrThrow("dday");
        bill.setDday(cursor.isNull(ddayIdx) ? null : cursor.getInt(ddayIdx));
        bill.setPaid(cursor.getInt(cursor.getColumnIndexOrThrow("is_paid")) == 1);
        return bill;
    }
}
