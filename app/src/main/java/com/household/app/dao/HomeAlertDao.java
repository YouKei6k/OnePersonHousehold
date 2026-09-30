package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.HomeAlertItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [역할] 홈 화면(REQ-014) 전용 DAO.
 * "가장 근접한 기한 알림 다섯개" 를 DB_설계서.md 4절에서 정한 방식대로
 * SCHEDULE / UTILITY_BILL / INGREDIENT / TRASH_DISPOSAL 를 UNION ALL 로 합쳐 조회하고,
 * HOME_FAVORITE 테이블로 즐겨찾기 여부를 표시한다.
 *
 * 참고 : 각 도메인 테이블의 등록 화면은 09.24~09.25에 개발 예정이라
 * 지금은 데이터가 없을 수 있다. 그 경우 홈 화면에는 빈 목록이 뜬다.
 */
public class HomeAlertDao {

    private static final String TAG = "HomeAlertDao";

    // HOME_FAVORITE.source_type 에 저장되는 값 (다른 화면 개발 시에도 동일하게 사용)
    public static final String SOURCE_SCHEDULE = "SCHEDULE";
    public static final String SOURCE_UTILITY_BILL = "UTILITY_BILL";
    public static final String SOURCE_INGREDIENT = "INGREDIENT";
    public static final String SOURCE_TRASH = "TRASH_DISPOSAL";

    private static final int MAX_ALERT_COUNT = 5; // REQ-014 : 다섯개

    private final DatabaseHelper dbHelper;

    public HomeAlertDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * 오늘 날짜 이후 알림을 가져온다. 즐겨찾기한 항목이 맨 위, 그 아래로 가까운 날짜 순이며 기본 5개를 채운다.
     * 각 항목의 즐겨찾기 여부도 함께 채워서 반환한다.
     */
    public List<HomeAlertItem> getNearestAlerts(int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new java.util.Date());

        // 4개 테이블을 같은 모양(source_type, source_id, title, target_date)으로 맞춰 UNION ALL
        String sql =
                "SELECT '" + SOURCE_SCHEDULE + "' AS source_type, schedule_id AS source_id, title, date_start AS target_date " +
                        "FROM SCHEDULE WHERE user_seq = ? AND is_done = 0 AND date_start >= ? " +
                        "UNION ALL " +
                        "SELECT '" + SOURCE_UTILITY_BILL + "' AS source_type, bill_id AS source_id, title, payment_date AS target_date " +
                        "FROM UTILITY_BILL WHERE user_seq = ? AND is_paid = 0 AND payment_date >= ? " +
                        "UNION ALL " +
                        "SELECT '" + SOURCE_INGREDIENT + "' AS source_type, ingredient_id AS source_id, title, expire_date AS target_date " +
                        "FROM INGREDIENT WHERE user_seq = ? AND is_done = 0 AND expire_date >= ? " +
                        "UNION ALL " +
                        "SELECT '" + SOURCE_TRASH + "' AS source_type, trash_id AS source_id, trash_type AS title, disposal_date AS target_date " +
                        "FROM TRASH_DISPOSAL WHERE user_seq = ? AND is_done = 0 AND disposal_date >= ? " +
                        "ORDER BY target_date ASC";

        String[] args = new String[]{
                String.valueOf(userSeq), today,
                String.valueOf(userSeq), today,
                String.valueOf(userSeq), today,
                String.valueOf(userSeq), today,
        };

        List<HomeAlertItem> result = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                HomeAlertItem item = new HomeAlertItem(
                        cursor.getString(cursor.getColumnIndexOrThrow("source_type")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("source_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("target_date"))
                );
                item.setFavorite(isFavorite(userSeq, item.getSourceType(), item.getSourceId()));
                result.add(item);
            }
        } catch (Exception e) {
            // 홈 화면은 앱 실행 시 가장 먼저 보이는 화면이라, 여기서 예외가 나면 원인을 바로 알아야 한다.
            // (예 : 4개 테이블 중 하나라도 컬럼명이 바뀌었는데 이 쿼리를 안 고친 경우)
            Log.e(TAG, "getNearestAlerts() UNION ALL 쿼리 실행 중 오류", e);
            return new ArrayList<>();
        }

        // 즐겨찾기 항목을 맨 위로 (각 그룹 안에서는 SQL에서 이미 날짜 오름차순)
        // - 즐겨찾기는 날짜가 멀어도 전부 보여주고, 남는 자리를 가까운 일반 항목으로 채워 최소 5개를 맞춘다.
        List<HomeAlertItem> favorites = new ArrayList<>();
        List<HomeAlertItem> others = new ArrayList<>();
        for (HomeAlertItem item : result) {
            if (item.isFavorite()) favorites.add(item);
            else others.add(item);
        }

        List<HomeAlertItem> arranged = new ArrayList<>(favorites);
        for (HomeAlertItem item : others) {
            if (arranged.size() >= MAX_ALERT_COUNT) break;
            arranged.add(item);
        }
        Log.d(TAG, "getNearestAlerts(userSeq=" + userSeq + ") -> 전체 " + result.size()
                + "건 중 즐겨찾기 " + favorites.size() + "건 포함 " + arranged.size() + "건 표시");
        return arranged;
    }

    /** 해당 항목이 이미 즐겨찾기 되어 있는지 확인 */
    public boolean isFavorite(int userSeq, String sourceType, int sourceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_HOME_FAVORITE,
                new String[]{DatabaseHelper.COL_FAV_SOURCE_ID},
                DatabaseHelper.COL_FAV_USER_SEQ + " = ? AND " +
                        DatabaseHelper.COL_FAV_SOURCE_TYPE + " = ? AND " +
                        DatabaseHelper.COL_FAV_SOURCE_ID + " = ?",
                new String[]{String.valueOf(userSeq), sourceType, String.valueOf(sourceId)},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 즐겨찾기 토글 : 이미 있으면 삭제(해제), 없으면 추가.
     * @return 토글 후의 즐겨찾기 상태 (true = 즐겨찾기 됨)
     */
    public boolean toggleFavorite(int userSeq, String sourceType, int sourceId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (isFavorite(userSeq, sourceType, sourceId)) {
            db.delete(
                    DatabaseHelper.TABLE_HOME_FAVORITE,
                    DatabaseHelper.COL_FAV_USER_SEQ + " = ? AND " +
                            DatabaseHelper.COL_FAV_SOURCE_TYPE + " = ? AND " +
                            DatabaseHelper.COL_FAV_SOURCE_ID + " = ?",
                    new String[]{String.valueOf(userSeq), sourceType, String.valueOf(sourceId)}
            );
            Log.d(TAG, "toggleFavorite() 해제 : " + sourceType + "#" + sourceId);
            return false;
        } else {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_FAV_USER_SEQ, userSeq);
            values.put(DatabaseHelper.COL_FAV_SOURCE_TYPE, sourceType);
            values.put(DatabaseHelper.COL_FAV_SOURCE_ID, sourceId);
            long rowId = db.insertWithOnConflict(
                    DatabaseHelper.TABLE_HOME_FAVORITE, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            if (rowId == -1) {
                Log.e(TAG, "toggleFavorite() 추가 실패 : " + sourceType + "#" + sourceId);
            } else {
                Log.d(TAG, "toggleFavorite() 추가 : " + sourceType + "#" + sourceId);
            }
            return true;
        }
    }
}
