package com.household.app.model;

/** [역할] TRASH_DISPOSAL 테이블 한 행을 표현하는 모델(POJO). 통합관리(REQ-007) / 쓰레기배출등록(REQ-010)에서 사용. */
public class TrashItem {
    public static final String TYPE_GENERAL = "일반";
    public static final String TYPE_FOOD = "음식물";
    public static final String TYPE_RECYCLE = "분리수거";
    public static final String TYPE_CLOTHING = "의류"; // 공공데이터 없음 -> 항상 수동입력

    private int trashId;
    private int userSeq;
    private String trashType;    // 일반/음식물/분리수거/의류
    private String area;         // 사는 지역/구 (표시용 : "시도 시군구 관리구역")
    private String disposalDate; // 배출일
    private boolean auto;        // 자동 계산 여부 (false면 수동 입력/수정)
    private Integer dday;
    private boolean done;        // 완료 여부

    public int getTrashId() { return trashId; }
    public void setTrashId(int trashId) { this.trashId = trashId; }
    public int getUserSeq() { return userSeq; }
    public void setUserSeq(int userSeq) { this.userSeq = userSeq; }
    public String getTrashType() { return trashType; }
    public void setTrashType(String trashType) { this.trashType = trashType; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getDisposalDate() { return disposalDate; }
    public void setDisposalDate(String disposalDate) { this.disposalDate = disposalDate; }
    public boolean isAuto() { return auto; }
    public void setAuto(boolean auto) { this.auto = auto; }
    public Integer getDday() { return dday; }
    public void setDday(Integer dday) { this.dday = dday; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
