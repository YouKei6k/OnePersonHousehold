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

/**
 * [역할] 아이디 찾기 화면 (REQ-001 비고 : "아이디 찾기" 버튼)
 * 이름 + 연락처(또는 이메일)가 모두 일치하는 회원의 아이디를 UserDao.findUserId()로 조회해 보여준다.
 * LoginActivity의 "아이디 찾기" 링크에서 진입한다.
 */
public class FindIdActivity extends BaseActivity {

    private static final String TAG = "FindIdActivity";

    private EditText etName, etContact;
    private LinearLayoutResult layoutResult; // 아래 View 묶음
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);
        Log.d(TAG, "onCreate()");

        userDao = new UserDao(this);

        etName = findViewById(R.id.etFindIdName);
        etContact = findViewById(R.id.etFindIdContact);
        Button btnConfirm = findViewById(R.id.btnFindIdConfirm);

        layoutResult = new LinearLayoutResult(
                findViewById(R.id.layoutFindIdResult),
                findViewById(R.id.tvFindIdResult),
                findViewById(R.id.tvGoLogin)
        );

        btnConfirm.setOnClickListener(v -> attemptFindId());
        layoutResult.tvGoLogin.setOnClickListener(v -> finish()); // 로그인 화면으로 돌아감
    }

    private void attemptFindId() {
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(contact)) {
            Log.d(TAG, "attemptFindId() 입력값 비어있음 - 조회 취소");
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        String foundId = userDao.findUserId(name, contact);
        Log.d(TAG, "attemptFindId(name=" + name + ") -> " + (foundId != null ? "찾음" : "못 찾음"));

        layoutResult.container.setVisibility(View.VISIBLE);

        if (foundId == null) {
            layoutResult.tvResult.setText(R.string.error_find_id_fail);
            layoutResult.tvGoLogin.setVisibility(View.GONE);
        } else {
            layoutResult.tvResult.setText(getString(R.string.msg_find_id_result, foundId));
            layoutResult.tvGoLogin.setVisibility(View.VISIBLE);
        }
    }

    /** 결과 영역 View 3개를 한번에 들고 다니기 위한 간단한 묶음 클래스 */
    private static class LinearLayoutResult {
        final View container;
        final TextView tvResult;
        final TextView tvGoLogin;

        LinearLayoutResult(View container, TextView tvResult, TextView tvGoLogin) {
            this.container = container;
            this.tvResult = tvResult;
            this.tvGoLogin = tvGoLogin;
        }
    }
}
