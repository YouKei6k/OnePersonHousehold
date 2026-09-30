package com.household.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.household.app.dao.ScheduleDao;
import com.household.app.model.Schedule;
import com.household.app.model.ScheduleNoti;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * [역할] 생활일정 등록/수정 화면 (REQ-005)
 * scheduleId == -1 이면 신규 등록, 아니면 해당 일정 수정 모드로 진입한다.
 * 반복일정 체크박스, 여러 개의 "며칠 전" 알림 추가/삭제, 반복일정 전체/부분 삭제 선택까지 이 화면에서 처리하고
 * 실제 DB 반영은 ScheduleDao에 위임한다.
 */
public class ScheduleEditActivity extends BaseActivity {

    private static final String TAG = "ScheduleEditActivity";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private ScheduleDao scheduleDao;
    private int userSeq;
    private int scheduleId; // -1 = 신규

    private EditText etTitle, etDetail, etNotiDday;
    private TextView tvDateStart, tvDateEnd, tvDateError, tvScreenTitle, tvDeleteSchedule;
    private CheckBox cbRecurring;
    private RadioGroup rgRecurType;
    private LinearLayout notiListContainer;

    private boolean existingRecurring; // 수정 모드에서 불러온 일정이 반복일정인지 (삭제 방식 선택용)
    private String dateStart;
    private String dateEnd; // null 허용 (선택 안 함)
    private final List<Integer> notiDdayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);

        scheduleDao = new ScheduleDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        scheduleId = getIntent().getIntExtra("scheduleId", -1);
        String selectedDate = getIntent().getStringExtra("selectedDate");
        Log.d(TAG, "onCreate() userSeq=" + userSeq + ", scheduleId=" + scheduleId
                + (scheduleId == -1 ? " (신규, selectedDate=" + selectedDate + ")" : " (수정)"));

        bindViews();
        setupListeners();

        if (scheduleId != -1) {
            tvScreenTitle.setText(R.string.title_schedule_edit);
            tvDeleteSchedule.setVisibility(View.VISIBLE);
            loadExistingSchedule();
        } else {
            tvScreenTitle.setText(R.string.title_schedule_add);
            dateStart = (selectedDate != null) ? selectedDate
                    : new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).format(new java.util.Date());
            dateEnd = null;
            renderDates();
        }
    }

    private void bindViews() {
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        etTitle = findViewById(R.id.etTitle);
        etDetail = findViewById(R.id.etDetail);
        tvDateStart = findViewById(R.id.tvDateStart);
        tvDateEnd = findViewById(R.id.tvDateEnd);
        tvDateError = findViewById(R.id.tvDateError);
        cbRecurring = findViewById(R.id.cbRecurring);
        rgRecurType = findViewById(R.id.rgRecurType);
        notiListContainer = findViewById(R.id.notiListContainer);
        etNotiDday = findViewById(R.id.etNotiDday);
        tvDeleteSchedule = findViewById(R.id.tvDeleteSchedule);
    }

    private void setupListeners() {
        tvDateStart.setOnClickListener(v -> pickDate(true));
        tvDateEnd.setOnClickListener(v -> pickDate(false));

        // 반복일정 체크 시에만 주/월/년 선택 노출 (REQ-005 비고)
        cbRecurring.setOnCheckedChangeListener((buttonView, isChecked) ->
                rgRecurType.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        findViewById(R.id.btnAddNoti).setOnClickListener(v -> addNoti());
        findViewById(R.id.btnRegister).setOnClickListener(v -> saveSchedule());
        tvDeleteSchedule.setOnClickListener(v -> confirmDelete());
    }

    /** 기존 일정 정보를 불러와 화면에 채운다 (수정 모드) */
    private void loadExistingSchedule() {
        Schedule schedule = scheduleDao.getScheduleById(scheduleId);
        if (schedule == null) {
            Log.e(TAG, "loadExistingSchedule() scheduleId=" + scheduleId + " 를 찾을 수 없어 화면을 닫습니다.");
            finish();
            return;
        }

        etTitle.setText(schedule.getTitle());
        etDetail.setText(schedule.getDetail());
        dateStart = schedule.getDateStart();
        dateEnd = schedule.getDateEnd();
        renderDates();

        existingRecurring = schedule.isRecurring();
        cbRecurring.setChecked(schedule.isRecurring());
        rgRecurType.setVisibility(schedule.isRecurring() ? View.VISIBLE : View.GONE);
        if (Schedule.RECUR_MONTH.equals(schedule.getRecurringType())) {
            ((android.widget.RadioButton) findViewById(R.id.rbMonth)).setChecked(true);
        } else if (Schedule.RECUR_YEAR.equals(schedule.getRecurringType())) {
            ((android.widget.RadioButton) findViewById(R.id.rbYear)).setChecked(true);
        } else {
            ((android.widget.RadioButton) findViewById(R.id.rbWeek)).setChecked(true);
        }

        List<ScheduleNoti> notiList = scheduleDao.getNotiList(scheduleId);
        for (ScheduleNoti n : notiList) {
            notiDdayList.add(n.getDday());
        }
        renderNotiList();
    }

    // ===================== 날짜 선택 =====================

    private void pickDate(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        try {
            String base = isStart ? dateStart : dateEnd;
            if (base != null) {
                cal.setTime(new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).parse(base));
            }
        } catch (Exception e) {
            Log.w(TAG, "pickDate() 기존 날짜 파싱 실패, 오늘 날짜로 대체 (isStart=" + isStart + ")", e);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String picked = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            if (isStart) {
                dateStart = picked;
            } else {
                dateEnd = picked;
            }
            renderDates();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void renderDates() {
        tvDateStart.setText(dateStart != null ? dateStart : "");
        tvDateEnd.setText(dateEnd != null ? dateEnd : "선택 안 함");
    }

    // ===================== 알림(며칠 전) 목록 =====================

    private void addNoti() {
        String value = etNotiDday.getText().toString().trim();
        if (TextUtils.isEmpty(value)) return;

        int dday;
        try {
            dday = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.w(TAG, "addNoti() 숫자 파싱 실패, 추가 취소 : \"" + value + "\"", e);
            return;
        }
        if (dday < 0) {
            Log.d(TAG, "addNoti() 음수 dday는 무시 : " + dday);
            return;
        }

        notiDdayList.add(dday);
        etNotiDday.setText("");
        renderNotiList();
    }

    /** 알림 목록을 화면에 다시 그린다 (각 줄 : "n일 전 알림" + 삭제 버튼) */
    private void renderNotiList() {
        notiListContainer.removeAllViews();

        for (int i = 0; i < notiDdayList.size(); i++) {
            int dday = notiDdayList.get(i);
            int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(8), dp(4), dp(8));

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tv.setText(getString(R.string.format_dday_before, dday));
            tv.setTextColor(getResources().getColor(R.color.text_dark));

            TextView tvDelete = new TextView(this);
            tvDelete.setText("✕");
            tvDelete.setTextColor(getResources().getColor(R.color.text_gray));
            tvDelete.setPadding(dp(8), 0, dp(8), 0);
            tvDelete.setOnClickListener(v -> {
                notiDdayList.remove(index);
                renderNotiList();
            });

            row.addView(tv);
            row.addView(tvDelete);
            notiListContainer.addView(row);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    // ===================== 저장 =====================

    private void saveSchedule() {
        String title = etTitle.getText().toString().trim();
        String detail = etDetail.getText().toString().trim();

        tvDateError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(title)) {
            Log.d(TAG, "saveSchedule() 제목 비어있음 - 저장 취소");
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(dateStart)) {
            Log.d(TAG, "saveSchedule() 시작 날짜 비어있음 - 저장 취소");
            tvDateError.setVisibility(View.VISIBLE);
            return;
        }

        boolean recurring = cbRecurring.isChecked();
        String recurringType = null;
        if (recurring) {
            int checkedId = rgRecurType.getCheckedRadioButtonId();
            if (checkedId == R.id.rbMonth) recurringType = Schedule.RECUR_MONTH;
            else if (checkedId == R.id.rbYear) recurringType = Schedule.RECUR_YEAR;
            else recurringType = Schedule.RECUR_WEEK;
        }

        Schedule schedule = new Schedule();
        schedule.setUserSeq(userSeq);
        schedule.setTitle(title);
        schedule.setDetail(detail);
        schedule.setDateStart(dateStart);
        schedule.setDateEnd(dateEnd);
        schedule.setRecurring(recurring);
        schedule.setRecurringType(recurringType);

        if (scheduleId == -1) {
            // 신규 등록 : 반복이면 원본+발생분이 함께 생성된다 (ScheduleDao.insertSchedule 참고)
            long newId = scheduleDao.insertSchedule(schedule);
            if (newId == -1) {
                Log.e(TAG, "saveSchedule() 신규 등록 실패 : title=" + title);
                Toast.makeText(this, R.string.msg_update_fail, Toast.LENGTH_SHORT).show();
                return;
            }
            scheduleDao.replaceNotiList((int) newId, notiDdayList);
            Log.i(TAG, "saveSchedule() 신규 등록 성공 : schedule_id=" + newId + ", 알림 " + notiDdayList.size() + "건");
        } else {
            schedule.setScheduleId(scheduleId);
            scheduleDao.updateSchedule(schedule);
            scheduleDao.replaceNotiList(scheduleId, notiDdayList);
            Log.i(TAG, "saveSchedule() 수정 성공 : schedule_id=" + scheduleId + ", 알림 " + notiDdayList.size() + "건");
        }

        // REQ-005 : 등록 -> 생활일정관리로 돌아감
        finish();
    }

    private void confirmDelete() {
        if (!existingRecurring) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_delete_schedule_title)
                    .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                        Log.i(TAG, "confirmDelete() 단일 삭제 확정 : schedule_id=" + scheduleId);
                        scheduleDao.deleteSchedule(scheduleId);
                        finish();
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            return;
        }

        // 반복일정 : 전체를 지울지, 지금 보고 있는 날짜의 일정만 지울지 선택
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_recurring_title)
                .setMessage(R.string.dialog_delete_recurring_desc)
                .setPositiveButton(R.string.btn_delete_all_recurring, (dialog, which) -> {
                    Log.i(TAG, "confirmDelete() 반복 전체 삭제 확정 : schedule_id=" + scheduleId);
                    scheduleDao.deleteRecurringGroup(scheduleId);
                    finish();
                })
                .setNeutralButton(R.string.btn_delete_this_only, (dialog, which) -> {
                    Log.i(TAG, "confirmDelete() 이 일정만 삭제 확정 : schedule_id=" + scheduleId);
                    scheduleDao.deleteSchedule(scheduleId);
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
