package com.household.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

/**
 * [역할] 앱의 모든 화면(15개 Activity)이 상속하는 공통 부모 클래스.
 *    startActivity() / finish() 를 가로채서 슬라이드 전환 애니메이션을 자동으로 붙여준다.
 *    화면마다 startActivity 호출 뒤에 overridePendingTransition()을 일일이 추가하지 않아도
 *    전체 화면에 똑같이 적용된다.
 */
public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    @Override
    public void startActivity(Intent intent) {
        try {
            super.startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (ActivityNotFoundException e) {
            // 보통 Manifest에 Activity 등록을 빼먹었거나 클래스 이름을 잘못 넣었을 때 발생한다.
            Log.e(TAG, getClass().getSimpleName() + " -> " + intent.getComponent()
                    + " 화면 전환 실패 (Activity를 찾을 수 없음, AndroidManifest.xml 등록 여부를 확인)", e);
            throw e; // 개발 중에는 원인을 바로 알 수 있도록 그대로 다시 던진다.
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
