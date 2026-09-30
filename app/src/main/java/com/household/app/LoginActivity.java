package com.household.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;


import com.household.app.dao.UserDao;
import com.household.app.model.User;

/**
 * [역할] 로그인 화면 (REQ-001). 앱을 실행하면 가장 먼저 뜨는 화면(AndroidManifest LAUNCHER)이다.
 * 분석서 작동방법 : 로그인 -> 회원 정보 확인(UserDao.login) -> 성공 시 홈(REQ-014) 이동
 *                아이디/비밀번호 틀리면 "ID/PW가 틀립니다." 메시지 출력
 * "로그인 유지" 체크박스를 켜면 SharedPreferences에 저장해서, 다음 실행부터는 이 화면을 건너뛰고 바로 홈으로 간다.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    // 로그인 유지 기능을 위한 SharedPreferences 키
    public static final String PREF_NAME = "auto_login_pref";
    public static final String KEY_AUTO_LOGIN = "auto_login";
    public static final String KEY_LOGIN_USER_SEQ = "login_user_seq";
    public static final String KEY_LOGIN_USER_NAME = "login_user_name";

    private EditText etLoginId, etLoginPw;
    private CheckBox cbKeepLogin;
    private TextView tvLoginError;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate()");
        userDao = new UserDao(this);

        // 이미 "로그인 유지"가 되어있다면 로그인 화면을 건너뛰고 바로 홈으로 이동
        if (isAutoLoginEnabled()) {
            Log.d(TAG, "로그인 유지 활성화됨 - 자동으로 홈 화면으로 이동");
            goToHome(getSavedUserSeq(), getSavedUserName());
            return;
        }

        etLoginId = findViewById(R.id.etLoginId);
        etLoginPw = findViewById(R.id.etLoginPw);
        cbKeepLogin = findViewById(R.id.cbKeepLogin);
        tvLoginError = findViewById(R.id.tvLoginError);

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoSignup = findViewById(R.id.tvGoSignup);
        TextView tvFindId = findViewById(R.id.tvFindId);
        TextView tvFindPw = findViewById(R.id.tvFindPw);

        btnLogin.setOnClickListener(v -> attemptLogin());

        // 회원가입 창(REQ-002)으로 이동
        tvGoSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        // 아이디/비밀번호 찾기 (REQ-001)
        tvFindId.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, FindIdActivity.class)));
        tvFindPw.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, FindPasswordActivity.class)));
    }

    /** 입력값 확인 후 DB에서 로그인 시도 */
    private void attemptLogin() {
        String id = etLoginId.getText().toString().trim();
        String pw = etLoginPw.getText().toString().trim();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw)) {
            Log.d(TAG, "attemptLogin() 입력값 비어있음 - 로그인 취소");
            showError(getString(R.string.error_empty_field));
            return;
        }

        User user = userDao.login(id, pw);

        if (user == null) {
            // 분석서 예외처리 : "ID/PW가 틀립니다." 출력 (UserDao.login()에서도 실패 로그를 남기지만
            // 화면에서 실제로 에러 메시지를 띄운 시점도 함께 남겨 흐름을 추적하기 쉽게 한다)
            Log.w(TAG, "attemptLogin() 실패 : id=" + id);
            showError(getString(R.string.error_login_fail));
            return;
        }

        // 로그인 유지 체크 시 SharedPreferences에 로그인 상태 저장
        if (cbKeepLogin.isChecked()) {
            saveAutoLogin(user);
            Log.d(TAG, "로그인 유지 체크됨 - SharedPreferences에 저장");
        }

        Log.i(TAG, "attemptLogin() 성공 : id=" + id + " (user_seq=" + user.getUserSeq() + ")");
        tvLoginError.setVisibility(View.GONE);
        goToHome(user.getUserSeq(), user.getUserName());
    }

    private void showError(String message) {
        tvLoginError.setText(message);
        tvLoginError.setVisibility(View.VISIBLE);
    }

    /** 로그인 성공 -> 홈(REQ-014)으로 이동 */
    private void goToHome(int userSeq, String userName) {
        Log.d(TAG, "goToHome(userSeq=" + userSeq + ")");
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("userSeq", userSeq);
        intent.putExtra("userName", userName);
        // 로그인 화면으로 뒤로가기 되지 않도록 이전 스택 정리
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ===================== 로그인 유지(자동 로그인) =====================

    private boolean isAutoLoginEnabled() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return pref.getBoolean(KEY_AUTO_LOGIN, false);
    }

    private String getSavedUserName() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return pref.getString(KEY_LOGIN_USER_NAME, "");
    }

    private int getSavedUserSeq() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return pref.getInt(KEY_LOGIN_USER_SEQ, -1);
    }

    private void saveAutoLogin(User user) {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_AUTO_LOGIN, true);
        editor.putInt(KEY_LOGIN_USER_SEQ, user.getUserSeq());
        editor.putString(KEY_LOGIN_USER_NAME, user.getUserName());
        editor.apply();
    }
}
