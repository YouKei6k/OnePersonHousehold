package com.household.app.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * [역할] TRASH_REGION_INFO 조회 전용 DAO (REQ-010). 데이터는 TrashRegionLoader가 최초 1회 넣는다.
 * 시/도 -> 시/군/구 -> 읍/면/동(관리구역) 순으로 좁혀가며 선택지를 제공하고,
 * 선택이 끝나면 해당 지역+쓰레기 종류의 배출요일을 찾아 "오늘 이후 가장 가까운 배출일"을 계산한다.
 * calcNearestDate()는 DB를 안 쓰는 순수 계산 로직이라 static이다.
 */
public class TrashRegionDao {

    private static final String TAG = "TrashRegionDao";

    private final DatabaseHelper dbHelper;

    public TrashRegionDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public List<String> getSidoList() {
        return queryDistinct("SELECT DISTINCT sido FROM TRASH_REGION_INFO ORDER BY sido", null);
    }

    public List<String> getSigunguList(String sido) {
        return queryDistinct(
                "SELECT DISTINCT sigungu FROM TRASH_REGION_INFO WHERE sido = ? ORDER BY sigungu",
                new String[]{sido});
    }

    public List<String> getAreaList(String sido, String sigungu) {
        return queryDistinct(
                "SELECT DISTINCT area FROM TRASH_REGION_INFO WHERE sido = ? AND sigungu = ? ORDER BY area",
                new String[]{sido, sigungu});
    }

    private List<String> queryDistinct(String sql, String[] args) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, args);
        List<String> result = new ArrayList<>();
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        return result;
    }

    /**
     * 선택한 지역+쓰레기종류의 배출요일 패턴("월+수+금" 형태)을 찾는다.
     * @return 못 찾으면 null (이 경우 화면에서는 수동 입력으로 전환)
     */
    public String getWeekdayPattern(String sido, String sigungu, String area, String trashType) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT disposal_weekday FROM TRASH_REGION_INFO " +
                        "WHERE sido = ? AND sigungu = ? AND area = ? AND trash_type = ? LIMIT 1",
                new String[]{sido, sigungu, area, trashType});
        String result = null;
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();

        if (result == null) {
            // 정상적인 상황(해당 조합의 공공데이터가 없어 수동입력으로 넘어감)일 수 있어 error가 아닌 warn으로 남긴다.
            Log.w(TAG, "getWeekdayPattern() 조회 결과 없음 : " + sido + " " + sigungu + " " + area + " / " + trashType);
        } else {
            Log.d(TAG, "getWeekdayPattern() -> " + result + " (" + sido + " " + sigungu + " " + area + " / " + trashType + ")");
        }
        return result;
    }

    /**
     * "월+수+금" 같은 배출요일 패턴을 받아 오늘(포함) 이후 가장 가까운 날짜를 계산한다.
     * @return yyyy-MM-dd 형식 날짜, 패턴이 비어있으면 null
     */
    public static String calcNearestDate(String weekdayPattern) {
        if (weekdayPattern == null || weekdayPattern.trim().isEmpty()) return null;

        List<Integer> targetWeekdays = new ArrayList<>();
        for (String token : weekdayPattern.split("\\+")) {
            Integer wd = toCalendarWeekday(token.trim());
            if (wd != null) {
                targetWeekdays.add(wd);
            } else {
                // 공공데이터의 요일 표기가 "월/수/금"처럼 "+"가 아닌 다른 구분자를 쓰거나 오타가 있는 경우를 잡아내기 위한 로그.
                Log.w(TAG, "calcNearestDate() 알 수 없는 요일 토큰 무시 : \"" + token + "\" (원본 패턴=" + weekdayPattern + ")");
            }
        }
        if (targetWeekdays.isEmpty()) {
            Log.e(TAG, "calcNearestDate() 패턴에서 유효한 요일을 하나도 못 찾음 : " + weekdayPattern);
            return null;
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

        // 오늘부터 최대 7일 안에는 반드시 해당 요일이 한 번은 걸린다
        for (int i = 0; i < 7; i++) {
            if (targetWeekdays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                String result = sdf.format(cal.getTime());
                Log.d(TAG, "calcNearestDate(" + weekdayPattern + ") -> " + result);
                return result;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        // 이론상 7일 안에 반드시 걸려야 하므로(toCalendarWeekday가 1~7 사이 값만 반환) 여기 도달하면 로직 버그다.
        Log.e(TAG, "calcNearestDate() 7일 이내에 해당 요일을 못 찾음 (로직 버그 의심) : " + weekdayPattern);
        return null;
    }

    private static Integer toCalendarWeekday(String koreanDay) {
        switch (koreanDay) {
            case "일": return Calendar.SUNDAY;
            case "월": return Calendar.MONDAY;
            case "화": return Calendar.TUESDAY;
            case "수": return Calendar.WEDNESDAY;
            case "목": return Calendar.THURSDAY;
            case "금": return Calendar.FRIDAY;
            case "토": return Calendar.SATURDAY;
            default: return null;
        }
    }

    /**
     * 저장된 지역 문자열("시도 시군구 관리구역")을 다시 [시도, 시군구, 관리구역] 으로 분해한다.
     * 관리구역명에는 공백이 있을 수 있어서, 단순히 공백으로 자르지 않고 실제 지역 데이터와 대조해 복원한다.
     * @return 복원 성공 시 길이 3 배열, 실패하면 null
     */
    public String[] parseRegionLabel(String label) {
        if (label == null || label.trim().isEmpty()) return null;
        String text = label.trim();
        Log.d(TAG, "parseRegionLabel(\"" + text + "\") 복원 시도");

        for (String sido : getSidoList()) {
            if (!text.startsWith(sido + " ")) continue;
            String rest = text.substring(sido.length() + 1);

            // 시군구가 서로 접두사인 경우를 대비해 긴 것부터 대조
            List<String> sigunguList = new ArrayList<>(getSigunguList(sido));
            java.util.Collections.sort(sigunguList, (a, b) -> b.length() - a.length());
            for (String sigungu : sigunguList) {
                if (!rest.startsWith(sigungu + " ")) continue;
                String area = rest.substring(sigungu.length() + 1);
                if (getAreaList(sido, sigungu).contains(area)) {
                    Log.d(TAG, "parseRegionLabel() 복원 성공 : " + sido + " / " + sigungu + " / " + area);
                    return new String[]{sido, sigungu, area};
                }
            }
        }
        Log.w(TAG, "parseRegionLabel(\"" + text + "\") 복원 실패 - 지역 데이터와 일치하는 조합을 못 찾음");
        return null;
    }
}
