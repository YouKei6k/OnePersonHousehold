package com.household.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.household.app.adapter.ManageRowAdapter;
import com.household.app.dao.BillDao;
import com.household.app.dao.IngredientDao;
import com.household.app.dao.TrashDao;
import com.household.app.model.Bill;
import com.household.app.model.Ingredient;
import com.household.app.model.ManageRow;
import com.household.app.model.TrashItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * [역할] 통합관리 화면 (REQ-007)
 * 공과금(REQ-008) / 식재료(REQ-009) / 쓰레기 배출(REQ-010) 3개 섹션을 한 화면에서 관리.
 * 3개 DAO(BillDao/IngredientDao/TrashDao)를 각각 조회해 공통 모델(ManageRow)로 변환하고,
 * 3개 섹션이 ManageRowAdapter 하나를 공유한다. 각 섹션 비고 : 완료 버튼을 누르면 취소선+맨 아래로 이동,
 * 마감일이 가장 가까운 것부터 표시.
 */
public class ManageActivity extends BaseActivity {

    private static final String TAG = "ManageActivity";

    private BillDao billDao;
    private IngredientDao ingredientDao;
    private TrashDao trashDao;
    private int userSeq;

    private RecyclerView rvBillList, rvIngredientList, rvTrashList;
    private TextView tvEmptyBill, tvEmptyIngredient, tvEmptyTrash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        billDao = new BillDao(this);
        ingredientDao = new IngredientDao(this);
        trashDao = new TrashDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq);

        rvBillList = findViewById(R.id.rvBillList);
        rvIngredientList = findViewById(R.id.rvIngredientList);
        rvTrashList = findViewById(R.id.rvTrashList);
        tvEmptyBill = findViewById(R.id.tvEmptyBill);
        tvEmptyIngredient = findViewById(R.id.tvEmptyIngredient);
        tvEmptyTrash = findViewById(R.id.tvEmptyTrash);

        rvBillList.setLayoutManager(new LinearLayoutManager(this));
        rvIngredientList.setLayoutManager(new LinearLayoutManager(this));
        rvTrashList.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAddBill).setOnClickListener(v ->
                startActivity(new Intent(this, BillEditActivity.class).putExtra("userSeq", userSeq).putExtra("billId", -1)));
        findViewById(R.id.btnAddIngredient).setOnClickListener(v ->
                startActivity(new Intent(this, IngredientEditActivity.class).putExtra("userSeq", userSeq).putExtra("ingredientId", -1)));
        findViewById(R.id.btnAddTrash).setOnClickListener(v ->
                startActivity(new Intent(this, TrashEditActivity.class).putExtra("userSeq", userSeq).putExtra("trashId", -1)));

        // REQ-007 비고 : 공과금 섹션의 "사이트로 이동" -> 공과금 통합 조회/납부 사이트(위택스)로 이동
        findViewById(R.id.btnGoToSite).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.wetax.go.kr"));
            startActivity(intent);
        });

        BottomNavHelper.setActive(this, BottomNavHelper.Tab.MANAGE);
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBills();
        loadIngredients();
        loadTrash();
    }

    // ===================== 공과금 (REQ-008) =====================

    private void loadBills() {
        List<Bill> bills = billDao.getAllByUser(userSeq);
        Log.d(TAG, "loadBills() -> " + bills.size() + "건");
        if (bills.isEmpty()) {
            rvBillList.setVisibility(View.GONE);
            tvEmptyBill.setVisibility(View.VISIBLE);
            return;
        }
        rvBillList.setVisibility(View.VISIBLE);
        tvEmptyBill.setVisibility(View.GONE);

        List<ManageRow> rows = new ArrayList<>();
        for (Bill b : bills) {
            rows.add(new ManageRow(b.getBillId(), b.getTitle(),
                    b.getPaymentDate() + " · " + ddayText(b.getPaymentDate()), b.isPaid()));
        }

        rvBillList.setAdapter(new ManageRowAdapter(rows, new ManageRowAdapter.OnRowActionListener() {
            @Override
            public void onCheckChanged(ManageRow row, boolean checked) {
                Log.d(TAG, "공과금 완료 체크 변경 : billId=" + row.getId() + ", paid=" + checked);
                billDao.setPaid(row.getId(), checked);
                rvBillList.postDelayed(ManageActivity.this::loadBills, 200);
            }

            @Override
            public void onItemClick(ManageRow row) {
                startActivity(new Intent(ManageActivity.this, BillEditActivity.class)
                        .putExtra("userSeq", userSeq).putExtra("billId", row.getId()));
            }
        }));
    }

    // ===================== 식재료 (REQ-009) =====================

    private void loadIngredients() {
        List<Ingredient> ingredients = ingredientDao.getAllByUser(userSeq);
        Log.d(TAG, "loadIngredients() -> " + ingredients.size() + "건");
        if (ingredients.isEmpty()) {
            rvIngredientList.setVisibility(View.GONE);
            tvEmptyIngredient.setVisibility(View.VISIBLE);
            return;
        }
        rvIngredientList.setVisibility(View.VISIBLE);
        tvEmptyIngredient.setVisibility(View.GONE);

        List<ManageRow> rows = new ArrayList<>();
        for (Ingredient item : ingredients) {
            rows.add(new ManageRow(item.getIngredientId(), item.getTitle(),
                    "소비기한 " + item.getExpireDate() + " · " + ddayText(item.getExpireDate()), item.isDone()));
        }

        rvIngredientList.setAdapter(new ManageRowAdapter(rows, new ManageRowAdapter.OnRowActionListener() {
            @Override
            public void onCheckChanged(ManageRow row, boolean checked) {
                Log.d(TAG, "식재료 완료 체크 변경 : ingredientId=" + row.getId() + ", done=" + checked);
                ingredientDao.setDone(row.getId(), checked);
                rvIngredientList.postDelayed(ManageActivity.this::loadIngredients, 200);
            }

            @Override
            public void onItemClick(ManageRow row) {
                startActivity(new Intent(ManageActivity.this, IngredientEditActivity.class)
                        .putExtra("userSeq", userSeq).putExtra("ingredientId", row.getId()));
            }
        }));
    }

    // ===================== 쓰레기 배출 (REQ-010) =====================

    private void loadTrash() {
        List<TrashItem> items = trashDao.getAllByUser(userSeq);
        Log.d(TAG, "loadTrash() -> " + items.size() + "건");
        if (items.isEmpty()) {
            rvTrashList.setVisibility(View.GONE);
            tvEmptyTrash.setVisibility(View.VISIBLE);
            return;
        }
        rvTrashList.setVisibility(View.VISIBLE);
        tvEmptyTrash.setVisibility(View.GONE);

        List<ManageRow> rows = new ArrayList<>();
        for (TrashItem item : items) {
            // 제목 = 쓰레기 종류(항상 값이 있음), 부제 = 배출일 · D-day · (지정했다면) 지역
            String subtitle = item.getDisposalDate() + " · " + ddayText(item.getDisposalDate())
                    + (item.isAuto() ? " · 자동" : " · 수동");
            if (item.getArea() != null && !item.getArea().isEmpty()) {
                subtitle += "\n" + item.getArea();
            }
            rows.add(new ManageRow(item.getTrashId(), item.getTrashType(), subtitle, item.isDone()));
        }

        rvTrashList.setAdapter(new ManageRowAdapter(rows, new ManageRowAdapter.OnRowActionListener() {
            @Override
            public void onCheckChanged(ManageRow row, boolean checked) {
                Log.d(TAG, "쓰레기 배출 완료 체크 변경 : trashId=" + row.getId() + ", done=" + checked);
                trashDao.setDone(row.getId(), checked);
                rvTrashList.postDelayed(ManageActivity.this::loadTrash, 200);
            }

            @Override
            public void onItemClick(ManageRow row) {
                startActivity(new Intent(ManageActivity.this, TrashEditActivity.class)
                        .putExtra("userSeq", userSeq).putExtra("trashId", row.getId()));
            }
        }));
    }

    // ===================== 공통 =====================

    /** yyyy-MM-dd 문자열을 받아 "D-3" / "D-DAY" / "D+2" 형태로 변환 */
    private String ddayText(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date target = sdf.parse(dateStr);
            Date today = sdf.parse(sdf.format(new Date()));
            long diffDays = (target.getTime() - today.getTime()) / (24 * 60 * 60 * 1000);
            if (diffDays == 0) return "D-DAY";
            return diffDays > 0 ? "D-" + diffDays : "D+" + Math.abs(diffDays);
        } catch (Exception e) {
            Log.e(TAG, "ddayText() 날짜 파싱 실패 : " + dateStr, e);
            return "";
        }
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class).putExtra("userSeq", userSeq));
            finish();
        });
        findViewById(R.id.navSchedule).setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class).putExtra("userSeq", userSeq)));
        findViewById(R.id.navTip).setOnClickListener(v ->
                startActivity(new Intent(this, TipCategoryActivity.class).putExtra("userSeq", userSeq)));
        findViewById(R.id.navMy).setOnClickListener(v ->
                startActivity(new Intent(this, MyPageActivity.class).putExtra("userSeq", userSeq)));
        // navManage 는 현재 화면이므로 별도 동작 없음
    }
}
