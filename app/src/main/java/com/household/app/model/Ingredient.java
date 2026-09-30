package com.household.app.model;

/** [역할] INGREDIENT 테이블 한 행을 표현하는 모델(POJO). 통합관리(REQ-007) / 식재료등록(REQ-009)에서 사용. */
public class Ingredient {
    private int ingredientId;
    private int userSeq;
    private String title;
    private String purchaseDate; // 구매일
    private String expireDate;   // 소비기한
    private Integer dday;        // 몇일전 알림
    private boolean done;

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public int getUserSeq() { return userSeq; }
    public void setUserSeq(int userSeq) { this.userSeq = userSeq; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getExpireDate() { return expireDate; }
    public void setExpireDate(String expireDate) { this.expireDate = expireDate; }
    public Integer getDday() { return dday; }
    public void setDday(Integer dday) { this.dday = dday; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
