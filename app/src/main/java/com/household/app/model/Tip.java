package com.household.app.model;

/** [역할] TIP 테이블 한 행을 표현하는 모델(POJO). 관리자가 미리 제공하는 팁(청소/쓰레기/공과금/생활정책 등). */
public class Tip {
    private int tipId;
    private String category;
    private String subCategory;
    private String title;
    private String content;

    public int getTipId() { return tipId; }
    public void setTipId(int tipId) { this.tipId = tipId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
