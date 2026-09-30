package com.household.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.household.app.adapter.HomeAlertAdapter;
import com.household.app.dao.HomeAlertDao;
import com.household.app.dao.UserDao;
import com.household.app.model.HomeAlertItem;
import com.household.app.model.User;

import java.util.List;

/**
 * [역할] 홈 화면 (REQ-014). 로그인/자동로그인 이후 가장 먼저 보이는 화면.
 * 상단 인사말(이름은 매번 DB에서 새로 읽어와 마이페이지 수정을 즉시 반영), 가장 근접한 기한 알림 5개
 * (HomeAlertDao, 즐겨찾기 가능), 하단 네비게이션을 담당한다.
 * ('생활기록 그래프'는 요구사항리스트에서 취소선 처리되어 제외)
 */
public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity";

    private HomeAlertDao homeAlertDao;
    private UserDao userDao;
    private HomeAlertAdapter adapter;
    private int userSeq;

    private TextView tvWelcome;
    private RecyclerView rvHomeAlerts;
    private TextView tvEmptyAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        homeAlertDao = new HomeAlertDao(this);
        userDao = new UserDao(this);
        // 방금 로그인해서 들어온 경우 intent extra 우선 사용, 로그인 유지로 바로 들어온 경우 SharedPreferences 사용
        userSeq = getIntent().getIntExtra("userSeq", getSavedUserSeq());
        Log.d(TAG, "onCreate() userSeq=" + userSeq);
        if (userSeq == -1) {
            // 로그인도 안 했는데(또는 로그인 유지 값도 없는데) 이 화면에 온 상태. 정상 흐름에서는 발생하면 안 된다.
            Log.e(TAG, "userSeq를 확인할 수 없습니다 (intent extra도, 저장된 로그인 유지 값도 없음). 로그인 화면으로 돌아갑니다.");
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        rvHomeAlerts = findViewById(R.id.rvHomeAlerts);
        tvEmptyAlerts = findViewById(R.id.tvEmptyAlerts);
        rvHomeAlerts.setLayoutManager(new LinearLayoutManager(this));

        BottomNavHelper.setActive(this, BottomNavHelper.Tab.HOME);
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 마이페이지에서 이름을 바꿨을 수도, 다른 화면에서 일정 데이터가 바뀌었을 수도 있으므로
        // 화면에 돌아올 때마다 이름/알림 목록을 DB에서 새로 읽어온다.
        loadWelcomeName();
        loadAlerts();
    }

    /** 상단 인사말의 이름을 DB에서 최신 값으로 읽어와 표시 (마이페이지에서 수정한 이름 즉시 반영) */
    private void loadWelcomeName() {
        User user = userDao.getUserBySeq(userSeq);
        String userName = (user != null) ? user.getUserName() : "";
        tvWelcome.setText(userName + "님, 환영합니다!");
    }

    /** 가장 근접한 기한 알림 최대 5개를 DB에서 읽어와 리스트에 표시 */
    private void loadAlerts() {
        List<HomeAlertItem> alerts = homeAlertDao.getNearestAlerts(userSeq);
        Log.d(TAG, "loadAlerts() -> " + alerts.size() + "건");

        if (alerts.isEmpty()) {
            rvHomeAlerts.setVisibility(View.GONE);
            tvEmptyAlerts.setVisibility(View.VISIBLE);
            return;
        }

        rvHomeAlerts.setVisibility(View.VISIBLE);
        tvEmptyAlerts.setVisibility(View.GONE);

        adapter = new HomeAlertAdapter(alerts, (item, position) -> {
            // 즐겨찾기 별 아이콘 클릭 -> DB 토글 후 목록을 다시 읽어 즐겨찾기가 맨 위로 오도록 재정렬
            homeAlertDao.toggleFavorite(userSeq, item.getSourceType(), item.getSourceId());
            loadAlerts();
        });
        rvHomeAlerts.setAdapter(adapter);
    }

    /** 와이어프레임 공통 하단 네비게이션. **/
    private void setupBottomNav() {
        findViewById(R.id.navSchedule).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ScheduleActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navManage).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ManageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navTip).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TipCategoryActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navMy).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MyPageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        // navHome 은 현재 화면이므로 별도 동작 없음
    }

    private void showComingSoon() {
        Toast.makeText(this, R.string.msg_coming_soon, Toast.LENGTH_SHORT).show();
    }

    /** 로그인 유지(자동 로그인)로 들어온 경우 SharedPreferences에서 회원 정보 조회 */
    private int getSavedUserSeq() {
        SharedPreferences pref = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        return pref.getInt(LoginActivity.KEY_LOGIN_USER_SEQ, -1);
    }
}
