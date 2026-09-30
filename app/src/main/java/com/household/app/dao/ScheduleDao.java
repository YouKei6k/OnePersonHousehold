package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.Schedule;
import com.household.app.model.ScheduleNoti;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * [역할] SCHEDULE / SCHEDULE_NOTI 테이블 DAO.
 * 생활일정관리(REQ-004, 006), 생활일정등록/수정(REQ-005) 화면에서 사용하는 모든 일정 관련 SQL을 담당한다.
 * 반복일정 생성, 반복 그룹 전체 삭제, 캘린더 점 표시용 날짜 집합 계산처럼 이 앱에서 가장 로직이 복잡한 DAO다.
 */
public class ScheduleDao {

    private static final String TAG = "ScheduleDao";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final DatabaseHelper dbHelper;

    public ScheduleDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // ===================== 등록 / 수정 / 삭제 =====================

    /**
     * 일정 등록 (REQ-005).
     * 반복일정 체크박스가 켜져 있으면 "주/월/년에 따른 날짜마다 체크박스 추가 생성" 요구사항에 맞춰
     * 원본 1건 + 이후 반복 발생분(RECUR_OCCURRENCE_COUNT개)을 함께 만든다.
     * 각 발생분은 독립된 행이라 개별적으로 완료 체크가 가능하다.
     * @return 원본(첫 번째) 일정의 schedule_id
     */
    private static final int RECUR_OCCURRENCE_COUNT = 8; // 반복일정 미리 생성해둘 개수 (원본 포함하지 않은 추가분)

    public long insertSchedule(Schedule schedule) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long firstId = insertOne(db, schedule);
        if (firstId == -1) {
            Log.e(TAG, "insertSchedule() 실패 : title=" + schedule.getTitle() + " - db.insert()가 -1을 반환했습니다.");
            return -1;
        }
        Log.d(TAG, "insertSchedule() 원본 저장 : schedule_id=" + firstId + ", title=" + schedule.getTitle());

        if (schedule.isRecurring() && schedule.getRecurringType() != null) {
            // 첫 행의 id 를 그룹 번호로 삼아 이후 발생분과 함께 묶는다 (반복 전체 삭제 시 사용)
            ContentValues groupValues = new ContentValues();
            groupValues.put("recur_group_id", firstId);
            db.update("SCHEDULE", groupValues, "schedule_id = ?", new String[]{String.valueOf(firstId)});

            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA);
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(schedule.getDateStart()));

                Calendar calEnd = null;
                if (schedule.getDateEnd() != null) {
                    calEnd = Calendar.getInstance();
                    calEnd.setTime(sdf.parse(schedule.getDateEnd()));
                }

                for (int i = 1; i <= RECUR_OCCURRENCE_COUNT; i++) {
                    addInterval(cal, schedule.getRecurringType());
                    if (calEnd != null) {
                        addInterval(calEnd, schedule.getRecurringType());
                    }

                    Schedule occurrence = new Schedule();
                    occurrence.setUserSeq(schedule.getUserSeq());
                    occurrence.setTitle(schedule.getTitle());
                    occurrence.setDetail(schedule.getDetail());
                    occurrence.setDateStart(sdf.format(cal.getTime()));
                    occurrence.setDateEnd(calEnd != null ? sdf.format(calEnd.getTime()) : null);
                    occurrence.setRecurring(true);
                    occurrence.setRecurringType(schedule.getRecurringType());
                    occurrence.setRecurGroupId((int) firstId);

                    insertOne(db, occurrence);
                }
                Log.d(TAG, "insertSchedule() 반복 발생분 " + RECUR_OCCURRENCE_COUNT
                        + "건 추가 생성 완료 (주기=" + schedule.getRecurringType() + ", group_id=" + firstId + ")");
            } catch (ParseException e) {
                // 날짜 형식이 잘못된 경우 반복분 생성은 건너뛰고 원본만 저장.
                // 화면(ScheduleEditActivity)에서 DatePickerDialog로만 날짜를 입력받기 때문에 정상적으로는 발생하지 않아야 하는 상황이라,
                // 원본은 저장됐는데 반복분만 조용히 안 만들어지는 걸 놓치지 않도록 경고 로그를 남긴다.
                Log.e(TAG, "insertSchedule() 반복 발생분 생성 실패 : 날짜 파싱 오류 (date_start=" + schedule.getDateStart() + ")", e);
            }
        }

        return firstId;
    }

    private long insertOne(SQLiteDatabase db, Schedule s) {
        ContentValues values = new ContentValues();
        values.put("user_seq", s.getUserSeq());
        values.put("title", s.getTitle());
        values.put("detail", s.getDetail());
        values.put("date_start", s.getDateStart());
        values.put("date_end", s.getDateEnd());
        values.put("is_recurring", s.isRecurring() ? 1 : 0);
        values.put("recurring_type", s.getRecurringType());
        values.put("is_done", s.isDone() ? 1 : 0);
        values.put("recur_group_id", s.getRecurGroupId());
        long id = db.insert("SCHEDULE", null, values);
        if (id == -1) {
            Log.e(TAG, "insertOne() 실패 : title=" + s.getTitle() + ", date_start=" + s.getDateStart());
        }
        return id;
    }

    /** 반복 주기만큼 날짜를 한 칸 이동 (주 -> +7일, 월 -> +1개월, 년 -> +1년) */
    private void addInterval(Calendar cal, String recurringType) {
        switch (recurringType) {
            case Schedule.RECUR_WEEK:
                cal.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case Schedule.RECUR_MONTH:
                cal.add(Calendar.MONTH, 1);
                break;
            case Schedule.RECUR_YEAR:
                cal.add(Calendar.YEAR, 1);
                break;
            default:
                break;
        }
    }

    /** 일정 수정 (REQ-005). 반복 발생분은 각각 독립된 행이라 이 항목 1건만 수정된다. */
    public int updateSchedule(Schedule schedule) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", schedule.getTitle());
        values.put("detail", schedule.getDetail());
        values.put("date_start", schedule.getDateStart());
        values.put("date_end", schedule.getDateEnd());
        int updated = db.update("SCHEDULE", values, "schedule_id = ?",
                new String[]{String.valueOf(schedule.getScheduleId())});
        if (updated == 0) {
            Log.w(TAG, "updateSchedule() 변경된 행이 없음 : schedule_id=" + schedule.getScheduleId());
        } else {
            Log.d(TAG, "updateSchedule() 성공 : schedule_id=" + schedule.getScheduleId());
        }
        return updated;
    }

    /** 완료 체크 토글 (REQ-006 비고 : 체크하면 맨 아래로 이동) */
    public void setDone(int scheduleId, boolean done) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_done", done ? 1 : 0);
        int updated = db.update("SCHEDULE", values, "schedule_id = ?", new String[]{String.valueOf(scheduleId)});
        Log.d(TAG, "setDone(schedule_id=" + scheduleId + ", done=" + done + ") -> " + updated + "행 변경");
    }

    /** 일정 삭제 (딸린 알림도 함께 삭제) */
    public void deleteSchedule(int scheduleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int notiDeleted = db.delete("SCHEDULE_NOTI", "schedule_id = ?", new String[]{String.valueOf(scheduleId)});
        int scheduleDeleted = db.delete("SCHEDULE", "schedule_id = ?", new String[]{String.valueOf(scheduleId)});
        Log.d(TAG, "deleteSchedule(schedule_id=" + scheduleId + ") -> 일정 " + scheduleDeleted
                + "건, 알림 " + notiDeleted + "건 삭제");
    }

    /**
     * 반복일정 전체 삭제 : 같은 반복 그룹(recur_group_id)으로 만들어진 모든 일정과 그 알림을 함께 지운다.
     * 그룹 번호가 없는 예전 데이터는 같은 사용자/제목/반복주기의 반복일정을 같은 묶음으로 보고 지운다.
     * @return 삭제한 일정 개수
     */
    public int deleteRecurringGroup(int scheduleId) {
        Schedule target = getScheduleById(scheduleId);
        if (target == null) {
            Log.w(TAG, "deleteRecurringGroup(schedule_id=" + scheduleId + ") -> 대상을 찾을 수 없어 취소");
            return 0;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        List<Integer> ids = new ArrayList<>();
        Cursor cursor;
        if (target.getRecurGroupId() != null) {
            Log.d(TAG, "deleteRecurringGroup() : recur_group_id=" + target.getRecurGroupId() + " 기준으로 묶어서 삭제");
            cursor = db.rawQuery("SELECT schedule_id FROM SCHEDULE WHERE recur_group_id = ?",
                    new String[]{String.valueOf(target.getRecurGroupId())});
        } else if (target.isRecurring()) {
            // 그룹 번호가 없는 예전 데이터 : 같은 제목/주기로 유추해서 묶는다 (완벽하지 않은 방식이라 로그로 남겨둔다)
            Log.w(TAG, "deleteRecurringGroup() : recur_group_id가 없는 예전 반복일정이라 title="
                    + target.getTitle() + " 로 추정 삭제합니다.");
            cursor = db.rawQuery(
                    "SELECT schedule_id FROM SCHEDULE WHERE user_seq = ? AND title = ? AND is_recurring = 1 " +
                            "AND recurring_type = ? AND recur_group_id IS NULL",
                    new String[]{String.valueOf(target.getUserSeq()), target.getTitle(), target.getRecurringType()});
        } else {
            cursor = db.rawQuery("SELECT schedule_id FROM SCHEDULE WHERE schedule_id = ?",
                    new String[]{String.valueOf(scheduleId)});
        }
        while (cursor.moveToNext()) ids.add(cursor.getInt(0));
        cursor.close();

        db.beginTransaction();
        try {
            for (int id : ids) {
                db.delete("SCHEDULE_NOTI", "schedule_id = ?", new String[]{String.valueOf(id)});
                db.delete("SCHEDULE", "schedule_id = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            Log.i(TAG, "deleteRecurringGroup() 완료 : " + ids.size() + "건 삭제");
        } catch (Exception e) {
            // 트랜잭션 도중 오류가 나면 setTransactionSuccessful()이 호출되지 않아 전체 롤백되는데,
            // 그 상태에서 이유 없이 "삭제가 안 됐다"고 헷갈리지 않도록 원인을 남긴다.
            Log.e(TAG, "deleteRecurringGroup() 중 오류 발생 - 트랜잭션이 롤백됩니다.", e);
        } finally {
            db.endTransaction();
        }
        return ids.size();
    }

    // ===================== 조회 =====================

    /** 특정 날짜에 해당하는(시작~끝 범위 포함) 일정 목록. 미완료가 먼저, 완료는 아래로 (REQ-006) */
    public List<Schedule> getSchedulesByDate(int userSeq, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM SCHEDULE WHERE user_seq = ? " +
                        "AND date_start <= ? AND COALESCE(NULLIF(date_end, ''), date_start) >= ? " +
                        "ORDER BY is_done ASC, date_start ASC",
                new String[]{String.valueOf(userSeq), date, date}
        );

        List<Schedule> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(cursorToSchedule(cursor));
        }
        cursor.close();
        Log.d(TAG, "getSchedulesByDate(date=" + date + ") -> " + list.size() + "건");
        return list;
    }

    public Schedule getScheduleById(int scheduleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM SCHEDULE WHERE schedule_id = ?",
                new String[]{String.valueOf(scheduleId)});
        Schedule s = null;
        if (cursor.moveToFirst()) {
            s = cursorToSchedule(cursor);
        }
        cursor.close();
        return s;
    }

    /**
     * yyyy-MM 월에 일정이 하나라도 있는 날짜들의 집합. 캘린더에 점(dot) 표시용 (REQ-004 "한눈에 보기").
     * 시작~끝 기간이 있는 일정은 그 기간에 걸친 모든 날짜를 포함한다 (해당 월 범위로만 잘라서 계산).
     */
    public Set<String> getDatesWithScheduleInMonth(int userSeq, String yearMonth) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Set<String> result = new HashSet<>();

        String monthStart = yearMonth + "-01";
        String monthEnd = yearMonth + "-31";

        Cursor cursor = db.rawQuery(
                "SELECT date_start, COALESCE(NULLIF(date_end, ''), date_start) AS eff_end FROM SCHEDULE " +
                        "WHERE user_seq = ? AND date_start <= ? AND COALESCE(NULLIF(date_end, ''), date_start) >= ?",
                new String[]{String.valueOf(userSeq), monthEnd, monthStart}
        );

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA);
        while (cursor.moveToNext()) {
            String from = cursor.getString(0);
            String to = cursor.getString(1);
            // 해당 월 밖의 날짜는 잘라낸다
            if (from.compareTo(monthStart) < 0) from = monthStart;
            if (to.compareTo(monthEnd) > 0) to = monthEnd;

            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(from));
                // 한 달 안에서만 돌므로 최대 31번 반복
                for (int i = 0; i < 31; i++) {
                    String day = sdf.format(cal.getTime());
                    if (day.compareTo(to) > 0 || !day.startsWith(yearMonth)) break;
                    result.add(day);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
            } catch (ParseException e) {
                // 캘린더 점 표시 하나가 부정확해지는 정도라 화면을 막지는 않지만,
                // 어떤 일정의 날짜 형식이 깨졌는지는 알아야 데이터를 고칠 수 있어서 로그를 남긴다.
                Log.w(TAG, "getDatesWithScheduleInMonth() 날짜 파싱 실패, 시작일만 표시 : " + from, e);
                result.add(cursor.getString(0)); // 파싱 실패 시 시작일만이라도 표시
            }
        }
        cursor.close();
        return result;
    }

    private Schedule cursorToSchedule(Cursor cursor) {
        Schedule s = new Schedule();
        s.setScheduleId(cursor.getInt(cursor.getColumnIndexOrThrow("schedule_id")));
        s.setUserSeq(cursor.getInt(cursor.getColumnIndexOrThrow("user_seq")));
        s.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        s.setDetail(cursor.getString(cursor.getColumnIndexOrThrow("detail")));
        s.setDateStart(cursor.getString(cursor.getColumnIndexOrThrow("date_start")));
        s.setDateEnd(cursor.getString(cursor.getColumnIndexOrThrow("date_end")));
        s.setRecurring(cursor.getInt(cursor.getColumnIndexOrThrow("is_recurring")) == 1);
        s.setRecurringType(cursor.getString(cursor.getColumnIndexOrThrow("recurring_type")));
        s.setDone(cursor.getInt(cursor.getColumnIndexOrThrow("is_done")) == 1);
        int groupIdx = cursor.getColumnIndex("recur_group_id");
        s.setRecurGroupId((groupIdx >= 0 && !cursor.isNull(groupIdx)) ? cursor.getInt(groupIdx) : null);
        return s;
    }

    // ===================== 알림(며칠 전) CRUD (REQ-005) =====================

    public List<ScheduleNoti> getNotiList(int scheduleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("SCHEDULE_NOTI", null, "schedule_id = ?",
                new String[]{String.valueOf(scheduleId)}, null, null, "dday ASC");

        List<ScheduleNoti> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            ScheduleNoti n = new ScheduleNoti();
            n.setNotiId(cursor.getInt(cursor.getColumnIndexOrThrow("noti_id")));
            n.setScheduleId(cursor.getInt(cursor.getColumnIndexOrThrow("schedule_id")));
            n.setDday(cursor.getInt(cursor.getColumnIndexOrThrow("dday")));
            list.add(n);
        }
        cursor.close();
        return list;
    }

    public void addNoti(int scheduleId, int dday) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("schedule_id", scheduleId);
        values.put("dday", dday);
        long id = db.insert("SCHEDULE_NOTI", null, values);
        if (id == -1) {
            Log.e(TAG, "addNoti() 실패 : schedule_id=" + scheduleId + ", dday=" + dday);
        }
    }

    public void deleteNoti(int notiId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("SCHEDULE_NOTI", "noti_id = ?", new String[]{String.valueOf(notiId)});
    }

    /** 일정 수정 시 알림 목록을 통째로 교체(삭제 후 재삽입)하는 헬퍼 */
    public void replaceNotiList(int scheduleId, List<Integer> ddayList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete("SCHEDULE_NOTI", "schedule_id = ?", new String[]{String.valueOf(scheduleId)});
        for (int dday : ddayList) {
            addNoti(scheduleId, dday);
        }
        Log.d(TAG, "replaceNotiList(schedule_id=" + scheduleId + ") : 기존 " + deleted
                + "건 삭제 후 " + ddayList.size() + "건 재삽입");
    }
}
