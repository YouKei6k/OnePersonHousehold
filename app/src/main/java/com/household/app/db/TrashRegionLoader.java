package com.household.app.db;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * [역할] 공공데이터 CSV를 SQLite로 옮겨 담는 "1회성 데이터 적재기".
 * assets/trash_region.csv (전국 생활쓰레기 배출정보 공공데이터, 시도/시군구/관리구역 단위)를
 * 앱 최초 설치 시 한 번 읽어서 TRASH_REGION_INFO 테이블에 넣는다. DatabaseHelper.onCreate()에서만 호출된다.
 *
 * CSV 컬럼 : sido, sigungu, area, general(생활쓰레기), food(음식물쓰레기), recycle(재활용품)
 * -> trash_type 3종("일반","음식물","분리수거")으로 각각 나눠서 저장한다.
 * "의류"는 이 공공데이터에 항목이 없어 자동조회 대상에서 제외하고 화면에서 항상 수동입력으로 처리한다 (REQ-010 비고).
 */
public class TrashRegionLoader {

    private static final String TAG = "TrashRegionLoader";
    private static final String ASSET_FILE_NAME = "trash_region.csv";

    public static void loadFromAssets(Context context, SQLiteDatabase db) {
        Log.d(TAG, "loadFromAssets() 시작 - assets/" + ASSET_FILE_NAME + " 읽기");
        AssetManager assetManager = context.getAssets();

        db.beginTransaction();
        // INSERT를 매번 새로 컴파일하지 않도록 SQLiteStatement로 미리 준비해 대량 삽입 속도를 높인다.
        String sql = "INSERT INTO TRASH_REGION_INFO (sido, sigungu, area, trash_type, disposal_weekday) VALUES (?, ?, ?, ?, ?)";
        SQLiteStatement stmt = db.compileStatement(sql);

        int insertedCount = 0;

        try (InputStream is = assetManager.open(ASSET_FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // 헤더 줄(sido,sigungu,area,general,food,recycle) 건너뜀
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] cols = parseCsvLine(line);
                if (cols.length < 6) {
                    Log.w(TAG, "컬럼 수가 6개 미만인 줄을 건너뜁니다 : " + line);
                    continue;
                }

                String sido = cols[0];
                String sigungu = cols[1];
                String area = cols[2];
                String general = cols[3];
                String food = cols[4];
                String recycle = cols[5];

                insertedCount += insertIfPresent(stmt, sido, sigungu, area, "일반", general);
                insertedCount += insertIfPresent(stmt, sido, sigungu, area, "음식물", food);
                insertedCount += insertIfPresent(stmt, sido, sigungu, area, "분리수거", recycle);
            }

            db.setTransactionSuccessful();
            Log.i(TAG, "TRASH_REGION_INFO 적재 완료 : " + insertedCount + "건");
        } catch (IOException e) {
            Log.e(TAG, "trash_region.csv 로딩 실패", e);
        } finally {
            db.endTransaction();
        }
    }

    private static int insertIfPresent(SQLiteStatement stmt, String sido, String sigungu, String area,
                                        String trashType, String weekday) {
        if (weekday == null || weekday.trim().isEmpty()) return 0;

        stmt.clearBindings();
        stmt.bindString(1, sido);
        stmt.bindString(2, sigungu);
        stmt.bindString(3, area);
        stmt.bindString(4, trashType);
        stmt.bindString(5, weekday.trim());
        stmt.executeInsert();
        return 1;
    }

    /**
     * 아주 단순한 CSV 한 줄 파서. 큰따옴표로 묶인 필드 안의 콤마(,)를 필드 구분자로 오인하지 않도록 처리한다.
     * (예 : "신설동,용두동,회기동" 처럼 지역명 자체에 콤마가 들어있는 행이 있어서 String.split(",")로는 깨짐)
     */
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"'); // 이스케이프된 큰따옴표("")
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(field.toString());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
        }
        result.add(field.toString());
        return result.toArray(new String[0]);
    }
}
