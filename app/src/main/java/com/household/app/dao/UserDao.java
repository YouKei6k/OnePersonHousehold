package com.household.app.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.household.app.db.DatabaseHelper;
import com.household.app.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * [역할] USER 테이블 전용 DAO(Data Access Object).
 * 회원가입(REQ-002), 로그인(REQ-001), 아이디/비밀번호 찾기(REQ-001), 마이페이지 수정/탈퇴(REQ-003)에서
 * 필요한 모든 회원 관련 SQL을 이 클래스 하나에 모아뒀다. Activity는 SQL을 직접 다루지 않고
 * 이 DAO의 메서드만 호출한다.
 */
public class UserDao {

    // Logcat 필터에 "UserDao"를 입력하면 회원 관련(가입/로그인/수정/탈퇴) 로그만 볼 수 있다.
    private static final String TAG = "UserDao";

    private final DatabaseHelper dbHelper;

    public UserDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * 아이디 중복 여부 확인.
     * @return true = 이미 존재하는 아이디, false = 사용 가능
     */
    public boolean isUserIdDuplicated(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USER,
                new String[]{DatabaseHelper.COL_USER_SEQ},
                DatabaseHelper.COL_USER_ID + " = ?",
                new String[]{userId},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        Log.d(TAG, "isUserIdDuplicated(" + userId + ") -> " + exists);
        return exists;
    }

    /**
     * 회원가입 : USER 테이블에 새 행 저장.
     * @return 성공 시 새로 생성된 user_seq, 실패 시 -1
     */
    public long insertUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_ID, user.getUserId());
        values.put(DatabaseHelper.COL_USER_PW, user.getUserPw());
        values.put(DatabaseHelper.COL_USER_NAME, user.getUserName());
        values.put(DatabaseHelper.COL_USER_MAIL, user.getUserMail());

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date());
        values.put(DatabaseHelper.COL_CREATED_AT, now);

        long newSeq = db.insert(DatabaseHelper.TABLE_USER, null, values);
        if (newSeq == -1) {
            // db.insert()는 실패해도 예외를 던지지 않고 조용히 -1만 반환하기 때문에,
            // 이 로그가 없으면 "왜 회원가입이 실패했는지" 원인을 추적할 방법이 없다.
            Log.e(TAG, "insertUser() 실패 : userId=" + user.getUserId() + " - db.insert()가 -1을 반환했습니다.");
        } else {
            Log.i(TAG, "insertUser() 성공 : userId=" + user.getUserId() + " -> user_seq=" + newSeq);
        }
        return newSeq;
    }

    /**
     * 로그인 : 아이디+비밀번호가 일치하는 회원을 조회.
     * @return 일치하는 회원이 있으면 User 객체, 없으면 null (REQ-001 "ID/PW가 틀립니다" 처리용)
     */
    public User login(String userId, String userPw) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USER,
                null,
                DatabaseHelper.COL_USER_ID + " = ? AND " + DatabaseHelper.COL_USER_PW + " = ?",
                new String[]{userId, userPw},
                null, null, null
        );

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();

        if (user == null) {
            Log.w(TAG, "login() 실패 : userId=" + userId + " - 아이디/비밀번호 불일치");
        } else {
            Log.i(TAG, "login() 성공 : userId=" + userId + " (user_seq=" + user.getUserSeq() + ")");
        }
        return user;
    }

    /** Cursor 한 행 -> User 객체 변환 (공통 사용) */
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setUserSeq(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_SEQ)));
        user.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)));
        user.setUserPw(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PW)));
        user.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME)));
        user.setUserMail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_MAIL)));
        int profileIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PROFILE);
        user.setUserProfile(cursor.isNull(profileIdx) ? null : cursor.getString(profileIdx));
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT)));
        return user;
    }

    /**
     * 아이디 찾기 (REQ-001) : 이름 + 연락처(또는 이메일)가 모두 일치하는 회원의 아이디를 조회.
     * @return 일치하는 회원이 있으면 그 아이디, 없으면 null
     */
    public String findUserId(String userName, String userMail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USER,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_NAME + " = ? AND " + DatabaseHelper.COL_USER_MAIL + " = ?",
                new String[]{userName, userMail},
                null, null, null
        );

        String foundId = null;
        if (cursor.moveToFirst()) {
            foundId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
        }
        cursor.close();
        Log.d(TAG, "findUserId(name=" + userName + ") -> " + (foundId != null ? "찾음" : "없음"));
        return foundId;
    }

    /**
     * 비밀번호 찾기 1단계 (REQ-001) : 아이디 + 연락처(또는 이메일)가 모두 일치하는지 확인.
     * 본인 확인이 끝나면 FindPasswordActivity에서 resetPassword를 호출해 새 비밀번호로 바꾼다.
     * @return 본인 확인 성공 여부
     */
    public boolean verifyUserForReset(String userId, String userMail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USER,
                new String[]{DatabaseHelper.COL_USER_SEQ},
                DatabaseHelper.COL_USER_ID + " = ? AND " + DatabaseHelper.COL_USER_MAIL + " = ?",
                new String[]{userId, userMail},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        Log.d(TAG, "verifyUserForReset(userId=" + userId + ") -> " + exists);
        return exists;
    }

    /**
     * 비밀번호 찾기 2단계 (REQ-001) : 본인 확인된 아이디의 비밀번호를 새 값으로 변경.
     * 새 비밀번호는 SignupActivity와 동일한 규칙(8~10자, 숫자 포함)을 통과한 값이어야 한다.
     * @return 변경된 행 수 (1이면 성공)
     */
    public int resetPassword(String userId, String newPw) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_PW, newPw);
        int updated = db.update(
                DatabaseHelper.TABLE_USER,
                values,
                DatabaseHelper.COL_USER_ID + " = ?",
                new String[]{userId}
        );
        if (updated == 0) {
            Log.e(TAG, "resetPassword() 실패 : userId=" + userId + " - 일치하는 행이 없습니다.");
        } else {
            Log.i(TAG, "resetPassword() 성공 : userId=" + userId);
        }
        return updated;
    }

    // ===================== 마이페이지 (REQ-003) =====================

    /** user_seq로 회원 한 명 조회. 마이페이지 진입 시 현재 정보 표시용 */
    public User getUserBySeq(int userSeq) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USER,
                null,
                DatabaseHelper.COL_USER_SEQ + " = ?",
                new String[]{String.valueOf(userSeq)},
                null, null, null
        );

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();

        if (user == null) {
            // 탈퇴한 회원의 세션이 남아있는 상태로 호출된 경우 등에 원인 파악용으로 남긴다.
            Log.w(TAG, "getUserBySeq(" + userSeq + ") -> 없음 (탈퇴했거나 잘못된 user_seq일 수 있음)");
        }
        return user;
    }

    /** 이름 수정 (REQ-003) */
    public int updateUserName(int userSeq, String newName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_NAME, newName);
        int updated = db.update(
                DatabaseHelper.TABLE_USER, values,
                DatabaseHelper.COL_USER_SEQ + " = ?", new String[]{String.valueOf(userSeq)}
        );
        Log.d(TAG, "updateUserName(userSeq=" + userSeq + ") -> " + updated + "행 변경");
        return updated;
    }

    /** 비밀번호 수정 (REQ-003, SignupActivity와 동일한 8~10자·숫자포함 규칙은 화면단에서 검사) */
    public int updateUserPassword(int userSeq, String newPw) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_PW, newPw);
        int updated = db.update(
                DatabaseHelper.TABLE_USER, values,
                DatabaseHelper.COL_USER_SEQ + " = ?", new String[]{String.valueOf(userSeq)}
        );
        // 비밀번호 값 자체는 로그에 남기지 않는다 (Logcat은 기기에 저장되므로 평문 노출을 피한다).
        Log.d(TAG, "updateUserPassword(userSeq=" + userSeq + ") -> " + updated + "행 변경");
        return updated;
    }

    /** 연락처(또는 이메일) 수정 (REQ-003) */
    public int updateUserContact(int userSeq, String newContact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_MAIL, newContact);
        int updated = db.update(
                DatabaseHelper.TABLE_USER, values,
                DatabaseHelper.COL_USER_SEQ + " = ?", new String[]{String.valueOf(userSeq)}
        );
        Log.d(TAG, "updateUserContact(userSeq=" + userSeq + ") -> " + updated + "행 변경");
        return updated;
    }

    /** 프로필 이미지 경로(Uri 문자열) 수정 (REQ-003 "프로필 수정") */
    public int updateUserProfile(int userSeq, String profileUri) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_PROFILE, profileUri);
        int updated = db.update(
                DatabaseHelper.TABLE_USER, values,
                DatabaseHelper.COL_USER_SEQ + " = ?", new String[]{String.valueOf(userSeq)}
        );
        Log.d(TAG, "updateUserProfile(userSeq=" + userSeq + ") -> " + updated + "행 변경, uri=" + profileUri);
        return updated;
    }

    /** 회원 탈퇴 (REQ-003) : USER 행 삭제. 도메인 데이터(일정 등)는 09.24 이후 화면에서 함께 정리 예정 */
    public int deleteUser(int userSeq) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(
                DatabaseHelper.TABLE_USER,
                DatabaseHelper.COL_USER_SEQ + " = ?",
                new String[]{String.valueOf(userSeq)}
        );
        Log.i(TAG, "deleteUser(userSeq=" + userSeq + ") -> " + deleted + "행 삭제");
        return deleted;
    }
}
