package com.household.app.model;

/** [역할] UTILITY_BILL 테이블 한 행을 표현하는 모델(POJO). 통합관리(REQ-007) / 공과금등록(REQ-008)에서 사용. */
public class Bill {
    private int billId;
    private int userSeq;
    private String title;
    private String paymentDate; // 납부날짜 yyyy-MM-dd
    private Integer dday;       // 몇일전 알림 (null 허용)
    private boolean paid;       // 완료 여부

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }
    public int getUserSeq() { return userSeq; }
    public void setUserSeq(int userSeq) { this.userSeq = userSeq; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public Integer getDday() { return dday; }
    public void setDday(Integer dday) { this.dday = dday; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
}
