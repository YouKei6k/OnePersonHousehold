package com.household.app.model;

/**
 * [역할] USER 테이블 한 행(row)을 표현하는 모델(POJO) 클래스.
 * REQ-001(로그인), REQ-002(회원가입), REQ-003(마이페이지)에서 공통으로 사용.
 */
public class User {

    private int userSeq;        // 내부 관리용 고유번호 (PK)
    private String userId;      // 로그인 아이디
    private String userPw;      // 비밀번호
    private String userName;    // 이름
    private String userMail;    // 연락처 또는 이메일
    private String userProfile; // 프로필 이미지 경로 (없으면 null)
    private String createdAt;   // 가입일시

    public User() {
    }

    public User(String userId, String userPw, String userName, String userMail) {
        this.userId = userId;
        this.userPw = userPw;
        this.userName = userName;
        this.userMail = userMail;
    }

    public int getUserSeq() {
        return userSeq;
    }

    public void setUserSeq(int userSeq) {
        this.userSeq = userSeq;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPw() {
        return userPw;
    }

    public void setUserPw(String userPw) {
        this.userPw = userPw;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMail() {
        return userMail;
    }

    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    public String getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
