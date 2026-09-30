package com.household.app.model;

/**
 * [역할] SCHEDULE_NOTI 테이블 한 행을 표현하는 모델(POJO). "등록한 일정 알림시간 수정,추가,삭제" (REQ-005)에서 사용.
 * 일정 하나에 "00일 전 알림"을 여러 개 등록할 수 있다.
 */
public class ScheduleNoti {

    private int notiId;
    private int scheduleId;
    private int dday; // 며칠 전 알림인지 (0 = 당일)

    public ScheduleNoti() {
    }

    public ScheduleNoti(int dday) {
        this.dday = dday;
    }

    public int getNotiId() {
        return notiId;
    }

    public void setNotiId(int notiId) {
        this.notiId = notiId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getDday() {
        return dday;
    }

    public void setDday(int dday) {
        this.dday = dday;
    }
}
