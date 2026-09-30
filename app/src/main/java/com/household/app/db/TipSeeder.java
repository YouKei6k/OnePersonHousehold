package com.household.app.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * [역할] TIP 테이블에 들어갈 기본(관리자 제공) 팁 데이터를 앱 최초 설치 시 한 번 넣는 "1회성 시드 데이터 적재기".
 * DatabaseHelper.onCreate()에서만 호출된다.
 * 와이어프레임 구조 참고 : 팁(카테고리, REQ-011) -> 상세(서브카테고리, REQ-012) -> 상세2(내용, REQ-013)
 */
public class TipSeeder {

    private static final String TAG = "TipSeeder";

    // {category, sub_category, title, content}
    private static final String[][] SEED_DATA = {
            {"청소", "세탁기", "세탁기 냄새 관리", "세탁조는 두 달에 한 번 세탁조 클리너로 청소하세요. 통돌이는 고온세탁 코스로 헹구면 곰팡이 냄새를 줄일 수 있습니다."},
            {"청소", "냉장고", "냉장고 정리 요령", "냉장실은 60%, 냉동실은 80% 채웠을 때 냉기 순환이 가장 효율적입니다. 소비기한이 짧은 식재료는 앞쪽에 두세요."},
            {"청소", "화장실", "배수구 냄새 제거", "배수구에 베이킹소다와 식초를 부어 30분 두었다가 물을 내리면 냄새와 물때 제거에 효과적입니다."},
            {"청소", "창문", "물자국 없이 창문 닦기", "신문지에 유리세정제를 뿌려 닦으면 물자국 없이 깨끗하게 마무리됩니다."},
            {"청소", "베란다", "장마철 배수구 관리", "배수구 거름망을 주 1회 비워주면 장마철 역류를 예방할 수 있습니다."},

            {"쓰레기", "분리수거", "투명 페트병 배출법", "투명 페트병은 라벨을 제거하고 압착해서 별도로 배출해야 재활용 대상이 됩니다."},
            {"쓰레기", "음식물쓰레기", "음식물쓰레기 줄이는 법", "물기를 최대한 제거하고 배출하면 냄새와 무게를 줄일 수 있어요. 뼈·씨앗·껍질류는 일반쓰레기입니다."},

            {"공과금", "전기요금", "누진구간 관리", "한전 '스마트 알림 서비스'를 신청하면 누진구간을 넘기기 전에 사용량을 확인할 수 있습니다."},
            {"공과금", "수도요금", "변기 물 절약", "변기 물탱크에 절수형 부속을 달면 벽돌을 넣는 것보다 안전하게 한 번에 쓰는 물의 양을 줄일 수 있어요."},

            {"생활정책", "전입신고", "전입신고 온라인 신청", "이사 후 14일 이내 정부24에서 온라인으로 전입신고를 하면 확정일자도 함께 신청할 수 있습니다."},
            {"생활정책", "확정일자", "확정일자 받는 법", "임대차계약서 원본을 지참해 주민센터 또는 정부24에서 받을 수 있고, 보증금 보호를 위해 전입 직후 바로 받는 게 좋습니다."},
    };

    public static void seed(SQLiteDatabase db) {
        Log.d(TAG, "seed() 시작 - 기본 팁 " + SEED_DATA.length + "건 삽입");
        String sql = "INSERT INTO TIP (category, sub_category, title, content) VALUES (?, ?, ?, ?)";
        SQLiteStatement stmt = db.compileStatement(sql);

        try {
            for (String[] row : SEED_DATA) {
                stmt.clearBindings();
                stmt.bindString(1, row[0]);
                stmt.bindString(2, row[1]);
                stmt.bindString(3, row[2]);
                stmt.bindString(4, row[3]);
                stmt.executeInsert();
            }
            Log.i(TAG, "seed() 완료 - TIP " + SEED_DATA.length + "건 삽입 성공");
        } catch (Exception e) {
            // SEED_DATA는 코드에 고정된 값이라 실패 가능성은 낮지만,
            // DB 스키마가 바뀌었는데 이 파일을 안 고친 경우 등을 대비해 원인을 남긴다.
            Log.e(TAG, "seed() 실패 - TIP 기본 데이터 삽입 중 오류", e);
        }
    }
}
