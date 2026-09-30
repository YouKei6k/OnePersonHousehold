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

import com.household.app.dao.BillDao;
import com.household.app.model.Bill;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * [역할] 공과금 등록/수정 화면 (REQ-008). ManageActivity에서 "+ 등록"(billId=-1) 또는
 * 기존 항목 클릭(billId 지정)으로 진입한다. 제목 / 납부날짜 / 몇일전 알림 -> 등록 시 통합관리로 돌아감.
 */
public class BillEditActivity extends BaseActivity {

    private static final String TAG = "BillEditActivity";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private BillDao billDao;
    private int userSeq;
    private int billId; // -1 = 신규

    private EditText etTitle, etDday;
    private TextView tvScreenTitle, tvPaymentDate, tvDelete;
    private String paymentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_edit);

        billDao = new BillDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        billId = getIntent().getIntExtra("billId", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq + ", billId=" + billId + (billId == -1 ? " (신규)" : " (수정)"));

        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        etTitle = findViewById(R.id.etTitle);
        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        etDday = findViewById(R.id.etDday);
        tvDelete = findViewById(R.id.tvDelete);

        tvPaymentDate.setOnClickListener(v -> pickDate());
        findViewById(R.id.btnRegister).setOnClickListener(v -> save());
        tvDelete.setOnClickListener(v -> confirmDelete());

        if (billId != -1) {
            tvScreenTitle.setText(R.string.title_bill_edit);
            tvDelete.setVisibility(View.VISIBLE);
            loadExisting();
        } else {
            tvScreenTitle.setText(R.string.title_bill_add);
            paymentDate = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).format(new java.util.Date());
            tvPaymentDate.setText(paymentDate);
        }
    }

    private void loadExisting() {
        Bill bill = billDao.getById(billId);
        if (bill == null) {
            // 통합관리 목록에는 있었는데 눌러서 들어와보니 없는 상황(다른 곳에서 이미 삭제됨 등). 원인 파악용으로 남긴다.
            Log.e(TAG, "loadExisting() billId=" + billId + " 를 찾을 수 없어 화면을 닫습니다.");
            finish();
            return;
        }
        etTitle.setText(bill.getTitle());
        paymentDate = bill.getPaymentDate();
        tvPaymentDate.setText(paymentDate);
        if (bill.getDday() != null) {
            etDday.setText(String.valueOf(bill.getDday()));
        }
    }

    private void pickDate() {
        Calendar cal = Calendar.getInstance();
        try {
            if (paymentDate != null) {
                cal.setTime(new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).parse(paymentDate));
            }
        } catch (Exception e) {
            // DatePickerDialog가 기본값(오늘)으로 열리는 정도라 기능은 계속되지만, paymentDate 값이
            // 이 화면이 아닌 다른 경로(DB 등)에서 잘못 들어왔을 가능성을 알 수 있도록 남긴다.
            Log.w(TAG, "pickDate() paymentDate 파싱 실패, 오늘 날짜로 대체 : " + paymentDate, e);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            paymentDate = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            tvPaymentDate.setText(paymentDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void save() {
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Log.d(TAG, "save() 제목 비어있음 - 저장 취소");
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(paymentDate)) {
            Log.d(TAG, "save() 납부날짜 비어있음 - 저장 취소");
            Toast.makeText(this, R.string.error_date_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Integer dday = null;
        String ddayStr = etDday.getText().toString().trim();
        if (!TextUtils.isEmpty(ddayStr)) {
            try {
                dday = Integer.parseInt(ddayStr);
            } catch (NumberFormatException e) {
                // EditText가 inputType="number"라 정상적으로는 숫자만 들어오지만, 방어적으로 로그를 남긴다.
                Log.w(TAG, "save() dday 파싱 실패, 알림 없이 저장 : \"" + ddayStr + "\"", e);
            }
        }

        Bill bill = new Bill();
        bill.setUserSeq(userSeq);
        bill.setTitle(title);
        bill.setPaymentDate(paymentDate);
        bill.setDday(dday);

        if (billId == -1) {
            billDao.insert(bill);
            Log.i(TAG, "save() 신규 등록 : title=" + title);
        } else {
            bill.setBillId(billId);
            billDao.update(bill);
            Log.i(TAG, "save() 수정 : billId=" + billId);
        }

        // REQ-008 : 등록 -> 통합관리로 돌아감
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_schedule_title)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    Log.i(TAG, "confirmDelete() 삭제 확정 : billId=" + billId);
                    billDao.delete(billId);
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
