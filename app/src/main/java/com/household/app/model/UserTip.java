package com.household.app.model;

/** [역할] USER_TIP 테이블 한 행을 표현하는 모델(POJO). 사용자가 특정 팁(subCategory)에 직접 추가한 팁. 수정/삭제 가능 (REQ-013). */
public class UserTip {
    private int userTipId;
    private int tipId;
    private int userSeq;
    private String content;

    public int getUserTipId() { return userTipId; }
    public void setUserTipId(int userTipId) { this.userTipId = userTipId; }
    public int getTipId() { return tipId; }
    public void setTipId(int tipId) { this.tipId = tipId; }
    public int getUserSeq() { return userSeq; }
    public void setUserSeq(int userSeq) { this.userSeq = userSeq; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
