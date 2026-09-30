package com.household.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.household.app.dao.UserDao;
import com.household.app.model.User;

import java.util.regex.Pattern;

/**
 * [역할] 회원가입 화면 (REQ-002). 입력값 유효성 검사(정규식 3종) + 아이디 중복확인 + UserDao.insertUser() 로
 * 실제 DB 저장까지 이 화면 하나에서 처리한다.
 * 분석서 예외처리 규칙 :
 *  - 아이디 : 8글자 이내
 *  - 비밀번호 : 8~10글자 이내, 숫자 포함
 *  - 연락처 : 전화번호 11자리 또는 '@naver.com', '@gmail.com' 등으로 끝나는 이메일
 */
public class SignupActivity extends BaseActivity {

    private static final String TAG = "SignupActivity";

    // 숫자 1개 이상 포함 여부 확인용 정규식
    private static final Pattern PW_HAS_DIGIT = Pattern.compile(".*[0-9].*");
    // 전화번호 11자리(숫자만)
    private static final Pattern PHONE_11 = Pattern.compile("^[0-9]{11}$");
    // 이메일 형식 (일반적인 형식 검사)
    private static final Pattern EMAIL_FORMAT =
            Pattern.compile("^[\\w.-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private EditText etId, etPw, etName, etContact;
    private TextView tvIdError, tvPwError, tvContactError;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Log.d(TAG, "onCreate()");

        userDao = new UserDao(this);

        etId = findViewById(R.id.etSignupId);
        etPw = findViewById(R.id.etSignupPw);
        etName = findViewById(R.id.etSignupName);
        etContact = findViewById(R.id.etSignupContact);

        tvIdError = findViewById(R.id.tvIdError);
        tvPwError = findViewById(R.id.tvPwError);
        tvContactError = findViewById(R.id.tvContactError);

        Button btnConfirm = findViewById(R.id.btnSignupConfirm);
        btnConfirm.setOnClickListener(v -> attemptSignup());
    }

    /** 입력값 전체 유효성 검사 후 DB 저장 */
    private void attemptSignup() {
        String id = etId.getText().toString().trim();
        String pw = etPw.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        // 모든 에러 문구 우선 숨김 처리
        tvIdError.setVisibility(View.GONE);
        tvPwError.setVisibility(View.GONE);
        tvContactError.setVisibility(View.GONE);

        boolean isValid = true;

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw)
                || TextUtils.isEmpty(name) || TextUtils.isEmpty(contact)) {
            Log.d(TAG, "attemptSignup() 필수값 비어있음 - 가입 취소");
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) 아이디 : 8자 이내
        if (id.length() > 8) {
            tvIdError.setVisibility(View.VISIBLE);
            isValid = false;
        }

        // 2) 비밀번호 : 8~10자 + 숫자 포함
        boolean pwLengthOk = pw.length() >= 8 && pw.length() <= 10;
        boolean pwHasDigit = PW_HAS_DIGIT.matcher(pw).matches();
        if (!pwLengthOk || !pwHasDigit) {
            tvPwError.setVisibility(View.VISIBLE);
            isValid = false;
        }

        // 3) 연락처 : 숫자 11자리 또는 이메일 형식
        boolean contactOk = PHONE_11.matcher(contact).matches()
                || EMAIL_FORMAT.matcher(contact).matches();
        if (!contactOk) {
            tvContactError.setVisibility(View.VISIBLE);
            isValid = false;
        }

        if (!isValid) {
            Log.d(TAG, "attemptSignup() 유효성 검사 실패 (아이디/비밀번호/연락처 형식 확인 필요)");
            return;
        }

        // 4) 아이디 중복 확인
        if (userDao.isUserIdDuplicated(id)) {
            Log.d(TAG, "attemptSignup() 아이디 중복 : " + id);
            tvIdError.setText(R.string.error_id_duplicated);
            tvIdError.setVisibility(View.VISIBLE);
            return;
        }

        // 5) DB 저장
        User newUser = new User(id, pw, name, contact);
        long resultSeq = userDao.insertUser(newUser);

        if (resultSeq == -1) {
            // UserDao.insertUser() 쪽에서도 Log.e로 원인을 남기지만, 화면에서 어떤 시점에 실패를 인지했는지도 남긴다.
            Log.e(TAG, "attemptSignup() 회원가입 실패 : id=" + id);
            Toast.makeText(this, "회원가입에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "attemptSignup() 회원가입 성공 : id=" + id + " -> user_seq=" + resultSeq);
        // 분석서 작동방법 : 계정 생성 -> 로그인창(REQ-001)으로 복귀
        Toast.makeText(this, R.string.msg_signup_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}
