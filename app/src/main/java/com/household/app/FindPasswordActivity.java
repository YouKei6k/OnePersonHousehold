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

import java.util.regex.Pattern;

/**
 * [역할] 비밀번호 찾기 화면 (REQ-001 비고 : "비밀번호 찾기" 버튼)
 * 1단계 : 아이디 + 연락처(또는 이메일)로 본인 확인 (UserDao.verifyUserForReset)
 * 2단계 : 새 비밀번호 입력 (REQ-002와 동일한 8~10자, 숫자 포함 규칙 재사용) 후 UserDao.resetPassword로 반영.
 * 한 화면에 두 단계를 layoutStep1/layoutStep2 표시 전환으로 구현했다 (화면 전환 없음).
 */
public class FindPasswordActivity extends BaseActivity {

    private static final String TAG = "FindPasswordActivity";

    // SignupActivity와 동일한 비밀번호 규칙(8~10자, 숫자 포함)
    private static final Pattern PW_HAS_DIGIT = Pattern.compile(".*[0-9].*");

    private TextView tvStepDesc;
    private View layoutStep1, layoutStep2;

    private EditText etVerifyId, etVerifyContact;
    private EditText etNewPw;
    private TextView tvNewPwError;

    private UserDao userDao;
    private String verifiedUserId; // 1단계 통과 후 확정된 아이디

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pw);
        Log.d(TAG, "onCreate()");

        userDao = new UserDao(this);

        tvStepDesc = findViewById(R.id.tvStepDesc);
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);

        etVerifyId = findViewById(R.id.etVerifyId);
        etVerifyContact = findViewById(R.id.etVerifyContact);
        etNewPw = findViewById(R.id.etNewPw);
        tvNewPwError = findViewById(R.id.tvNewPwError);

        Button btnVerify = findViewById(R.id.btnVerify);
        Button btnResetPw = findViewById(R.id.btnResetPw);

        btnVerify.setOnClickListener(v -> attemptVerify());
        btnResetPw.setOnClickListener(v -> attemptResetPassword());
    }

    /** 1단계 : 아이디 + 연락처 본인 확인 */
    private void attemptVerify() {
        String id = etVerifyId.getText().toString().trim();
        String contact = etVerifyContact.getText().toString().trim();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(contact)) {
            Log.d(TAG, "attemptVerify() 입력값 비어있음 - 검증 취소");
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean verified = userDao.verifyUserForReset(id, contact);
        Log.d(TAG, "attemptVerify(userId=" + id + ") -> " + verified);

        if (!verified) {
            Toast.makeText(this, R.string.error_find_pw_fail, Toast.LENGTH_SHORT).show();
            return;
        }

        // 본인 확인 성공 -> 2단계(새 비밀번호 입력) 화면으로 전환
        verifiedUserId = id;
        tvStepDesc.setText(R.string.step2_desc);
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);
    }

    /** 2단계 : 새 비밀번호 유효성 검사 후 DB 업데이트 */
    private void attemptResetPassword() {
        String newPw = etNewPw.getText().toString().trim();
        tvNewPwError.setVisibility(View.GONE);

        boolean pwLengthOk = newPw.length() >= 8 && newPw.length() <= 10;
        boolean pwHasDigit = PW_HAS_DIGIT.matcher(newPw).matches();

        if (!pwLengthOk || !pwHasDigit) {
            Log.d(TAG, "attemptResetPassword() 새 비밀번호가 규칙에 안 맞음 (길이 또는 숫자 포함 여부)");
            tvNewPwError.setVisibility(View.VISIBLE);
            return;
        }

        int updatedRows = userDao.resetPassword(verifiedUserId, newPw);

        if (updatedRows > 0) {
            Log.i(TAG, "attemptResetPassword() 성공 : userId=" + verifiedUserId);
            Toast.makeText(this, R.string.msg_reset_pw_success, Toast.LENGTH_SHORT).show();
            finish(); // 로그인 화면으로 복귀
        } else {
            // resetPassword가 0을 반환하는 건 verifiedUserId가 DB에서 사라진 경우(예: 그 사이 탈퇴)뿐이라 흔치 않은 케이스다.
            Log.e(TAG, "attemptResetPassword() 실패 : userId=" + verifiedUserId + " - resetPassword()가 0행을 반환");
            Toast.makeText(this, "비밀번호 변경에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
    }
}
