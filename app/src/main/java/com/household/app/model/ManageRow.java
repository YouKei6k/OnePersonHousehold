package com.household.app.model;

/**
 * [역할] 통합관리(REQ-007) 화면의 3개 섹션(공과금/식재료/쓰레기)이 공유하는 표시용 모델(POJO).
 * 각 DAO가 반환하는 Bill/Ingredient/TrashItem을 화면에 그리기 직전에 이 형태로 변환해서 쓴다.
 */
public class ManageRow {
    private final int id;
    private final String title;
    private final String subtitle; // "yyyy-MM-dd · D-n" 형태
    private final boolean done;

    public ManageRow(int id, String title, String subtitle, boolean done) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.done = done;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public boolean isDone() { return done; }
}
