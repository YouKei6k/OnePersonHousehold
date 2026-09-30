package com.household.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.household.app.dao.TipDao;

import java.util.List;

/**
 * [역할] 팁 화면 (REQ-011) : 각종 팁 종류를 버튼으로 나눠 보여주고, 누르면 상세(서브카테고리,
 * TipSubCategoryActivity) 화면으로 이동. 검색어 입력 즉시 TipDao로 다시 조회해서 필터링한다.
 */
public class TipCategoryActivity extends BaseActivity {

    private static final String TAG = "TipCategoryActivity";

    private TipDao tipDao;
    private int userSeq;

    private EditText etSearch;
    private LinearLayout gridContainer;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_list);
        Log.d(TAG, "onCreate()");

        tipDao = new TipDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);

        etSearch = findViewById(R.id.etTipSearch);
        gridContainer = findViewById(R.id.tipGridContainer);
        tvEmpty = findViewById(R.id.tvEmptyTip);

        // 입력할 때마다 즉시 필터링
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCategories();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        BottomNavHelper.setActive(this, BottomNavHelper.Tab.TIP);
        setupBottomNav();
        loadCategories();
    }

    private void loadCategories() {
        List<String> categories = tipDao.getCategories(etSearch.getText().toString());
        Log.d(TAG, "loadCategories() -> " + categories.size() + "건");

        if (categories.isEmpty()) {
            gridContainer.removeAllViews();
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        TipGridHelper.fill(this, gridContainer, categories, (index, label) -> {
            Log.d(TAG, "카테고리 선택 : " + label);
            Intent intent = new Intent(TipCategoryActivity.this, TipSubCategoryActivity.class);
            intent.putExtra("userSeq", userSeq);
            intent.putExtra("category", label);
            startActivity(intent);
        });
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navSchedule).setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navManage).setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navMy).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyPageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        // navTip 은 현재 화면이므로 별도 동작 없음
    }
}
