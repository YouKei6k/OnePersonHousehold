package com.household.app.model;

/**
 * [역할] 홈 화면(REQ-014)에 뜨는 "가장 근접한 기한 알림" 한 건을 표현하는 모델(POJO).
 * SCHEDULE / UTILITY_BILL / INGREDIENT / TRASH_DISPOSAL 네 테이블을
 * UNION ALL 로 합친 결과 한 행을 표현한다. DB 테이블 하나에 대응되지 않는 유일한 모델이다.
 */
public class HomeAlertItem {

    // 출처 테이블 구분값 (HomeAlertDao.SOURCE_* 상수와 매칭)
    private String sourceType;
    private int sourceId;      // 출처 테이블의 PK
    private String title;      // 제목
    private String targetDate; // 기준이 되는 날짜 (일정 시작일 / 납부일 / 소비기한 / 배출일)
    private boolean favorite;  // 즐겨찾기 여부 (HOME_FAVORITE 테이블 조회 결과)

    public HomeAlertItem(String sourceType, int sourceId, String title, String targetDate) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.targetDate = targetDate;
    }

    public String getSourceType() {
        return sourceType;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /** 같은 항목인지 비교할 때 쓰는 고유키 (source_type + source_id 조합) */
    public String getUniqueKey() {
        return sourceType + "_" + sourceId;
    }
}
