package com.household.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.imageview.ShapeableImageView;
import com.household.app.dao.UserDao;
import com.household.app.model.User;

import java.util.regex.Pattern;

/**
 * [역할] 마이페이지 (REQ-003)
 * 요구사항리스트 : 내 정보 확인/수정(이름, 비밀번호, 연락처), 프로필 수정, 탈퇴 및 로그아웃.
 * 이름/비밀번호/연락처는 각각 독립된 저장 버튼으로 개별 반영되고(UserDao), 프로필 사진은
 * ACTION_OPEN_DOCUMENT로 고른 뒤 영구 URI 권한을 얻어 앱 재시작 후에도 보이게 한다.
 */
public class MyPageActivity extends BaseActivity {

    private static final String TAG = "MyPageActivity";

    // SignupActivity와 동일한 검사 규칙 재사용
    private static final Pattern PW_HAS_DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern PHONE_11 = Pattern.compile("^[0-9]{11}$");
    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[\\w.-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final int REQUEST_PICK_IMAGE = 1001;

    private UserDao userDao;
    private int userSeq;

    private ShapeableImageView ivProfile;
    private EditText etName, etPw, etContact;
    private TextView tvPwError, tvContactError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        userDao = new UserDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq);

        ivProfile = findViewById(R.id.ivProfile);
        etName = findViewById(R.id.etName);
        etPw = findViewById(R.id.etPw);
        etContact = findViewById(R.id.etContact);
        tvPwError = findViewById(R.id.tvPwError);
        tvContactError = findViewById(R.id.tvContactError);

        findViewById(R.id.ivProfile).setOnClickListener(v -> pickProfileImage());
        findViewById(R.id.tvEditProfile).setOnClickListener(v -> pickProfileImage());

        findViewById(R.id.btnSaveName).setOnClickListener(v -> saveName());
        findViewById(R.id.btnSavePw).setOnClickListener(v -> savePassword());
        findViewById(R.id.btnSaveContact).setOnClickListener(v -> saveContact());

        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
        findViewById(R.id.tvWithdraw).setOnClickListener(v -> confirmWithdraw());

        BottomNavHelper.setActive(this, BottomNavHelper.Tab.MY);
        setupBottomNav();
        loadUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }

    /** 현재 회원 정보를 불러와 화면에 채운다 */
    private void loadUserInfo() {
        User user = userDao.getUserBySeq(userSeq);
        if (user == null) {
            // 탈퇴 등으로 더 이상 존재하지 않는 회원 -> 로그인 화면으로
            goToLogin();
            return;
        }

        etName.setText(user.getUserName());
        etContact.setText(user.getUserMail());
        etPw.setText(""); // 비밀번호는 보여주지 않고 새로 입력받는 방식

        if (user.getUserProfile() != null) {
            try {
                ivProfile.setImageURI(Uri.parse(user.getUserProfile()));
            } catch (Exception e) {
                // 예전에 골랐던 사진의 Uri 권한이 만료됐거나 파일이 지워진 경우 등에 발생할 수 있다.
                // 화면에는 그냥 기본 아이콘만 보이므로, 왜 사진이 안 뜨는지는 로그로만 확인 가능하다.
                Log.w(TAG, "프로필 이미지 로드 실패, 기본 아이콘으로 대체 : " + user.getUserProfile(), e);
                ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    // ===================== 정보 수정 =====================

    private void saveName() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "saveName() 시도");
        int updated = userDao.updateUserName(userSeq, name);
        showSaveResult(updated, getString(R.string.label_name));
    }

    private void savePassword() {
        String pw = etPw.getText().toString().trim();
        tvPwError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(pw)) {
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean pwLengthOk = pw.length() >= 8 && pw.length() <= 10;
        boolean pwHasDigit = PW_HAS_DIGIT.matcher(pw).matches();
        if (!pwLengthOk || !pwHasDigit) {
            tvPwError.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "savePassword() 시도");
        int updated = userDao.updateUserPassword(userSeq, pw);
        etPw.setText("");
        showSaveResult(updated, getString(R.string.label_pw));
    }

    private void saveContact() {
        String contact = etContact.getText().toString().trim();
        tvContactError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(contact)) {
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean contactOk = PHONE_11.matcher(contact).matches()
                || EMAIL_FORMAT.matcher(contact).matches();
        if (!contactOk) {
            tvContactError.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "saveContact() 시도");
        int updated = userDao.updateUserContact(userSeq, contact);
        showSaveResult(updated, getString(R.string.label_contact));
    }

    private void showSaveResult(int updatedRows, String fieldLabel) {
        if (updatedRows > 0) {
            Toast.makeText(this, getString(R.string.msg_update_success, fieldLabel), Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "showSaveResult() 저장 실패 : field=" + fieldLabel + ", userSeq=" + userSeq);
            Toast.makeText(this, R.string.msg_update_fail, Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== 프로필 이미지 (REQ-003 "프로필 수정") =====================

    /** 갤러리(문서 선택기)에서 이미지 한 장을 고르도록 연다 */
    private void pickProfileImage() {
        Log.d(TAG, "pickProfileImage() 갤러리 열기");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri == null) {
                Log.w(TAG, "onActivityResult() 이미지 선택 결과의 data가 null");
                return;
            }
            Log.d(TAG, "onActivityResult() 이미지 선택됨 : " + imageUri);

            // 앱을 재시작해도 이 이미지에 계속 접근할 수 있도록 권한을 영구 저장
            try {
                getContentResolver().takePersistableUriPermission(
                        imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                // 일부 문서 제공자는 영구 권한을 지원하지 않을 수 있음 -> 이번 세션에서만 사용
                // (앱을 재시작하면 이 사진이 다시 안 보일 수 있다는 뜻이라 원인 확인용으로 남긴다)
                Log.w(TAG, "takePersistableUriPermission() 실패 - 이번 세션에서만 사진이 보일 수 있음 : " + imageUri, e);
            }

            ivProfile.setImageURI(imageUri);
            int updated = userDao.updateUserProfile(userSeq, imageUri.toString());
            showSaveResult(updated, getString(R.string.desc_profile_image));
        }
    }

    // ===================== 로그아웃 / 탈퇴 =====================

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> logout())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void logout() {
        Log.i(TAG, "logout() userSeq=" + userSeq);
        // 로그인 유지 상태 해제
        SharedPreferences pref = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        pref.edit().clear().apply();
        goToLogin();
    }

    private void confirmWithdraw() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_withdraw_title)
                .setMessage(R.string.dialog_withdraw_desc)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> withdraw())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void withdraw() {
        Log.i(TAG, "withdraw() userSeq=" + userSeq);
        userDao.deleteUser(userSeq);
        SharedPreferences pref = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        pref.edit().clear().apply();
        goToLogin();
    }

    /** 로그인 화면으로 이동하며 이전 화면 스택(홈 등) 모두 정리 */
    private void goToLogin() {
        Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ===================== 하단 네비게이션 =====================

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, HomeActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navSchedule).setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, ScheduleActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navManage).setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, ManageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navTip).setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, TipCategoryActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        // navMy 는 현재 화면이므로 별도 동작 없음
    }

    private void showComingSoon() {
        Toast.makeText(this, R.string.msg_coming_soon, Toast.LENGTH_SHORT).show();
    }
}
