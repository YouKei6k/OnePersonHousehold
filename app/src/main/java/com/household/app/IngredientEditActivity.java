package com.household.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.household.app.dao.IngredientDao;
import com.household.app.model.Ingredient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * [역할] 식재료 등록/수정 화면 (REQ-009). ManageActivity에서 "+ 등록"(ingredientId=-1) 또는
 * 기존 항목 클릭(ingredientId 지정)으로 진입한다. 제목 / 구매일 / 소비기한 / 몇일전 알림 -> 등록 시 통합관리로 돌아감.
 */
public class IngredientEditActivity extends BaseActivity {

    private static final String TAG = "IngredientEditActivity";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private IngredientDao ingredientDao;
    private int userSeq;
    private int ingredientId; // -1 = 신규

    private EditText etTitle, etDday;
    private TextView tvScreenTitle, tvPurchaseDate, tvExpireDate, tvDateError, tvDelete;
    private String purchaseDate;
    private String expireDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_edit);

        ingredientDao = new IngredientDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        ingredientId = getIntent().getIntExtra("ingredientId", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq + ", ingredientId=" + ingredientId
                + (ingredientId == -1 ? " (신규)" : " (수정)"));

        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        etTitle = findViewById(R.id.etTitle);
        tvPurchaseDate = findViewById(R.id.tvPurchaseDate);
        tvExpireDate = findViewById(R.id.tvExpireDate);
        tvDateError = findViewById(R.id.tvDateError);
        etDday = findViewById(R.id.etDday);
        tvDelete = findViewById(R.id.tvDelete);

        tvPurchaseDate.setOnClickListener(v -> pickDate(true));
        tvExpireDate.setOnClickListener(v -> pickDate(false));
        findViewById(R.id.btnRegister).setOnClickListener(v -> save());
        tvDelete.setOnClickListener(v -> confirmDelete());

        if (ingredientId != -1) {
            tvScreenTitle.setText(R.string.title_ingredient_edit);
            tvDelete.setVisibility(View.VISIBLE);
            loadExisting();
        } else {
            tvScreenTitle.setText(R.string.title_ingredient_add);
            purchaseDate = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).format(new java.util.Date());
            tvPurchaseDate.setText(purchaseDate);
        }
    }

    private void loadExisting() {
        Ingredient item = ingredientDao.getById(ingredientId);
        if (item == null) {
            Log.e(TAG, "loadExisting() ingredientId=" + ingredientId + " 를 찾을 수 없어 화면을 닫습니다.");
            finish();
            return;
        }
        etTitle.setText(item.getTitle());
        purchaseDate = item.getPurchaseDate();
        expireDate = item.getExpireDate();
        tvPurchaseDate.setText(purchaseDate);
        tvExpireDate.setText(expireDate);
        if (item.getDday() != null) {
            etDday.setText(String.valueOf(item.getDday()));
        }
    }

    private void pickDate(boolean isPurchase) {
        Calendar cal = Calendar.getInstance();
        try {
            String base = isPurchase ? purchaseDate : expireDate;
            if (base != null) {
                cal.setTime(new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).parse(base));
            }
        } catch (Exception e) {
            Log.w(TAG, "pickDate() 기존 날짜 파싱 실패, 오늘 날짜로 대체 (isPurchase=" + isPurchase + ")", e);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String picked = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            if (isPurchase) {
                purchaseDate = picked;
                tvPurchaseDate.setText(picked);
            } else {
                expireDate = picked;
                tvExpireDate.setText(picked);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void save() {
        String title = etTitle.getText().toString().trim();
        tvDateError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(title)) {
            Log.d(TAG, "save() 제목 비어있음 - 저장 취소");
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(expireDate)) {
            Log.d(TAG, "save() 소비기한 비어있음 - 저장 취소");
            tvDateError.setVisibility(View.VISIBLE);
            return;
        }

        Integer dday = null;
        String ddayStr = etDday.getText().toString().trim();
        if (!TextUtils.isEmpty(ddayStr)) {
            try {
                dday = Integer.parseInt(ddayStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "save() dday 파싱 실패, 알림 없이 저장 : \"" + ddayStr + "\"", e);
            }
        }

        Ingredient item = new Ingredient();
        item.setUserSeq(userSeq);
        item.setTitle(title);
        item.setPurchaseDate(purchaseDate);
        item.setExpireDate(expireDate);
        item.setDday(dday);

        if (ingredientId == -1) {
            ingredientDao.insert(item);
            Log.i(TAG, "save() 신규 등록 : title=" + title);
        } else {
            item.setIngredientId(ingredientId);
            ingredientDao.update(item);
            Log.i(TAG, "save() 수정 : ingredientId=" + ingredientId);
        }

        // REQ-009 : 등록 -> 통합관리로 돌아감
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_schedule_title)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    Log.i(TAG, "confirmDelete() 삭제 확정 : ingredientId=" + ingredientId);
                    ingredientDao.delete(ingredientId);
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
