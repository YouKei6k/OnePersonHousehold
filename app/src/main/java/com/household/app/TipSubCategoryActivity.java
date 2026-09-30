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
import com.household.app.model.Tip;

import java.util.ArrayList;
import java.util.List;

/**
 * [역할] 팁 상세 화면 (REQ-012) : 선택한 카테고리(예: 청소)의 세부 항목(세탁기, 냉장고 ...)을 버튼으로 보여준다. 검색 가능.
 * 항목을 누르면 팁 내용 + 내가 쓴 팁 화면(REQ-013, TipDetailActivity)으로 이동한다.
 * TipCategoryActivity에서 category를 intent로 넘겨받아 진입한다.
 */
public class TipSubCategoryActivity extends BaseActivity {

    private static final String TAG = "TipSubCategoryActivity";

    private TipDao tipDao;
    private int userSeq;
    private String category;

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
        category = getIntent().getStringExtra("category");
        Log.d(TAG, "진입 category=" + category);

        ((TextView) findViewById(R.id.tvTipTitle)).setText(getString(R.string.title_tip) + " · " + category);

        etSearch = findViewById(R.id.etTipSearch);
        gridContainer = findViewById(R.id.tipGridContainer);
        tvEmpty = findViewById(R.id.tvEmptyTip);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadSubCategories();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        BottomNavHelper.setActive(this, BottomNavHelper.Tab.TIP);
        setupBottomNav();
        loadSubCategories();
    }

    private void loadSubCategories() {
        final List<Tip> tips = tipDao.getTipsByCategory(category, etSearch.getText().toString());
        Log.d(TAG, "loadSubCategories(category=" + category + ") -> " + tips.size() + "건");

        if (tips.isEmpty()) {
            gridContainer.removeAllViews();
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        List<String> labels = new ArrayList<>();
        for (Tip t : tips) labels.add(t.getSubCategory());

        TipGridHelper.fill(this, gridContainer, labels, (index, label) -> {
            Log.d(TAG, "서브카테고리 선택 : " + label + " (tipId=" + tips.get(index).getTipId() + ")");
            Intent intent = new Intent(TipSubCategoryActivity.this, TipDetailActivity.class);
            intent.putExtra("userSeq", userSeq);
            intent.putExtra("tipId", tips.get(index).getTipId());
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
        // navTip: 카테고리 목록으로 돌아가기
        findViewById(R.id.navTip).setOnClickListener(v -> finish());
    }
}
