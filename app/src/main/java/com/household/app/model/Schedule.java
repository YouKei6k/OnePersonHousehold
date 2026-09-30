package com.household.app.model;

/**
 * [역할] SCHEDULE 테이블 한 행을 표현하는 모델(POJO). 생활일정관리(REQ-004,006)와 등록/수정(REQ-005) 화면에서 공용으로 사용.
 */
public class Schedule {

    public static final String RECUR_NONE = null;
    public static final String RECUR_WEEK = "WEEK";   // 매주
    public static final String RECUR_MONTH = "MONTH"; // 매월
    public static final String RECUR_YEAR = "YEAR";   // 매년

    private int scheduleId;
    private int userSeq;
    private String title;        // 제목 (생활일정관리 목록에 보이는 글자)
    private String detail;       // 내용
    private String dateStart;    // 시작 날짜 (yyyy-MM-dd)
    private String dateEnd;      // 끝나는 날짜 (yyyy-MM-dd)
    private boolean recurring;   // 반복일정 체크박스
    private String recurringType; // WEEK / MONTH / YEAR (recurring == true일 때만 의미 있음)
    private boolean done;        // 완료 여부 (체크 시 목록 맨 아래로 이동)
    private Integer recurGroupId; // 같은 반복일정에서 만들어진 행들의 묶음 번호 (반복 전체 삭제용, 반복 아니면 null)

    public Schedule() {
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getUserSeq() {
        return userSeq;
    }

    public void setUserSeq(int userSeq) {
        this.userSeq = userSeq;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getDateStart() {
        return dateStart;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getRecurringType() {
        return recurringType;
    }

    public void setRecurringType(String recurringType) {
        this.recurringType = recurringType;
    }

    public Integer getRecurGroupId() {
        return recurGroupId;
    }

    public void setRecurGroupId(Integer recurGroupId) {
        this.recurGroupId = recurGroupId;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
