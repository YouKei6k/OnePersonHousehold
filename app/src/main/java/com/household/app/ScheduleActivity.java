package com.household.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.household.app.adapter.ScheduleAdapter;
import com.household.app.dao.ScheduleDao;
import com.household.app.dao.StickerDao;
import com.household.app.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * [역할] 생활일정관리 화면 (REQ-004 : 캘린더+스티커, REQ-006 : 할일목록)
 * 캘린더는 별도 라이브러리 없이 LinearLayout에 행(주)마다 7칸(요일)을 직접 그려 넣는 방식으로 구현했다.
 * 날짜를 탭하면 selectedDate가 바뀌면서 하단 할일목록(ScheduleDao.getSchedulesByDate)이 다시 그려진다.
 * "+ 일정 추가" / 기존 항목 클릭은 모두 ScheduleEditActivity로 이동한다.
 */
public class ScheduleActivity extends BaseActivity {

    private static final String TAG = "ScheduleActivity";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    // 디자인 참고 : 캘린더 요일이 월요일부터 시작
    private static final String[] WEEKDAY_LABELS = {"월", "화", "수", "목", "금", "토", "일"};

    private ScheduleDao scheduleDao;
    private StickerDao stickerDao;
    private int userSeq;

    private Calendar currentMonth = Calendar.getInstance(); // 현재 보고 있는 달
    private String selectedDate;                            // yyyy-MM-dd, 기본값 오늘

    private TextView tvMonthTitle;
    private LinearLayout weekdayHeader;
    private LinearLayout calendarContainer;
    private TextView tvSelectedDate;
    private TextView btnToggleSticker;
    private RecyclerView rvScheduleList;
    private TextView tvEmptySchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        scheduleDao = new ScheduleDao(this);
        stickerDao = new StickerDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq);

        selectedDate = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).format(new Date());

        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        weekdayHeader = findViewById(R.id.weekdayHeader);
        calendarContainer = findViewById(R.id.calendarContainer);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnToggleSticker = findViewById(R.id.btnToggleSticker);
        rvScheduleList = findViewById(R.id.rvScheduleList);
        tvEmptySchedule = findViewById(R.id.tvEmptySchedule);

        rvScheduleList.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> changeMonth(1));
        btnToggleSticker.setOnClickListener(v -> toggleSticker());
        findViewById(R.id.btnAddSchedule).setOnClickListener(v -> openScheduleEdit(-1));

        buildWeekdayHeader();
        BottomNavHelper.setActive(this, BottomNavHelper.Tab.SCHEDULE);
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 등록/수정 화면에서 돌아왔을 때 최신 데이터로 다시 그린다
        renderCalendar();
        loadScheduleList();
    }

    private void changeMonth(int amount) {
        currentMonth.add(Calendar.MONTH, amount);
        renderCalendar();
    }

    // ===================== 캘린더 렌더링 (REQ-004) =====================

    private void buildWeekdayHeader() {
        weekdayHeader.removeAllViews();
        for (String label : WEEKDAY_LABELS) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tv.setGravity(Gravity.CENTER);
            tv.setText(label);
            tv.setTextColor(getResources().getColor(R.color.text_gray));
            tv.setTextSize(12);
            weekdayHeader.addView(tv);
        }
    }

    private void renderCalendar() {
        SimpleDateFormat sdfMonthTitle = new SimpleDateFormat("yyyy년 M월", Locale.KOREA);
        tvMonthTitle.setText(sdfMonthTitle.format(currentMonth.getTime()));
        tvSelectedDate.setText(selectedDate);

        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(currentMonth.getTime());
        String todayStr = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).format(new Date());

        Set<String> scheduleDates = scheduleDao.getDatesWithScheduleInMonth(userSeq, yearMonth);
        Set<String> stickerDates = stickerDao.getStickerDatesInMonth(userSeq, yearMonth);

        Calendar firstOfMonth = (Calendar) currentMonth.clone();
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        // Calendar.DAY_OF_WEEK : 일=1 ~ 토=7. 월요일 시작 그리드로 바꾸려면 (dayOfWeek+5)%7 로 옮긴다 (월=0 ... 일=6).
        int startOffset = (firstOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        calendarContainer.removeAllViews();

        int dayCounter = 1;
        int totalCells = startOffset + daysInMonth;
        int totalRows = (int) Math.ceil(totalCells / 7.0);

        for (int row = 0; row < totalRows; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < 7; col++) {
                int cellIndex = row * 7 + col;

                if (cellIndex < startOffset || dayCounter > daysInMonth) {
                    rowLayout.addView(makeEmptyCell());
                } else {
                    String dateStr = yearMonth + "-" + String.format(Locale.KOREA, "%02d", dayCounter);
                    boolean isToday = dateStr.equals(todayStr);
                    boolean isSelected = dateStr.equals(selectedDate);
                    boolean hasSchedule = scheduleDates.contains(dateStr);
                    boolean hasSticker = stickerDates.contains(dateStr);
                    rowLayout.addView(makeDayCell(dayCounter, dateStr, isToday, isSelected, hasSchedule, hasSticker));
                    dayCounter++;
                }
            }
            calendarContainer.addView(rowLayout);
        }
    }

    private View makeEmptyCell() {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));
        return tv;
    }

    /** 날짜 셀 하나 : 숫자(원형 배경) + 아래 작은 점(일정/스티커 유무) */
    private View makeDayCell(int day, String dateStr, boolean isToday, boolean isSelected,
                              boolean hasSchedule, boolean hasSticker) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));

        TextView tvDay = new TextView(this);
        tvDay.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setText(String.valueOf(day));
        tvDay.setTextSize(13);

        if (isSelected) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_selected);
            tvDay.setTextColor(Color.WHITE);
        } else if (isToday) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_today);
            tvDay.setTextColor(getResources().getColor(R.color.accent_orange));
        } else {
            tvDay.setTextColor(getResources().getColor(R.color.text_dark));
        }

        cell.addView(tvDay);

        // 일정/스티커 표시 : 둘 다 있으면 점 두 개를 나란히 보여준다 (일정=오렌지, 스티커=네이비)
        LinearLayout marks = new LinearLayout(this);
        marks.setOrientation(LinearLayout.HORIZONTAL);
        marks.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams marksParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(5));
        marksParams.topMargin = dp(3);
        marks.setLayoutParams(marksParams);

        if (hasSchedule) marks.addView(makeDot(R.drawable.dot_orange));
        if (hasSticker) marks.addView(makeDot(R.drawable.bg_calendar_selected));
        marks.setVisibility((hasSchedule || hasSticker) ? View.VISIBLE : View.INVISIBLE);
        cell.addView(marks);

        cell.setOnClickListener(v -> {
            selectedDate = dateStr;
            renderCalendar();
            loadScheduleList();
        });

        return cell;
    }

    /** 5dp 짜리 원형 점 하나 (좌우 1dp 간격) */
    private View makeDot(int drawableRes) {
        View dot = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(5), dp(5));
        lp.leftMargin = dp(1);
        lp.rightMargin = dp(1);
        dot.setLayoutParams(lp);
        dot.setBackgroundResource(drawableRes);
        return dot;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    // ===================== 스티커 =====================

    private void toggleSticker() {
        Log.d(TAG, "toggleSticker() date=" + selectedDate);
        boolean nowHasSticker = stickerDao.toggleSticker(userSeq, selectedDate);
        Toast.makeText(this,
                nowHasSticker ? R.string.msg_sticker_added : R.string.msg_sticker_removed,
                Toast.LENGTH_SHORT).show();
        renderCalendar();
    }

    // ===================== 할일목록 (REQ-006) =====================

    private void loadScheduleList() {
        List<Schedule> list = scheduleDao.getSchedulesByDate(userSeq, selectedDate);
        Log.d(TAG, "loadScheduleList(date=" + selectedDate + ") -> " + list.size() + "건");

        if (list.isEmpty()) {
            rvScheduleList.setVisibility(View.GONE);
            tvEmptySchedule.setVisibility(View.VISIBLE);
            return;
        }

        rvScheduleList.setVisibility(View.VISIBLE);
        tvEmptySchedule.setVisibility(View.GONE);

        ScheduleAdapter adapter = new ScheduleAdapter(list, new ScheduleAdapter.OnScheduleActionListener() {
            @Override
            public void onCheckChanged(Schedule schedule, boolean checked) {
                Log.d(TAG, "일정 완료 체크 변경 : schedule_id=" + schedule.getScheduleId() + ", done=" + checked);
                scheduleDao.setDone(schedule.getScheduleId(), checked);
                // 완료 상태가 바뀌면 정렬 순서(맨 아래로 이동)도 바뀌어야 하므로 목록을 다시 읽어온다
                rvScheduleList.postDelayed(() -> {
                    loadScheduleList();
                    renderCalendar();
                }, 200); // 체크 애니메이션이 보이도록 살짝 지연 후 재정렬
            }

            @Override
            public void onItemClick(Schedule schedule) {
                openScheduleEdit(schedule.getScheduleId());
            }
        });
        rvScheduleList.setAdapter(adapter);
    }

    private void openScheduleEdit(int scheduleId) {
        Log.d(TAG, "openScheduleEdit(scheduleId=" + scheduleId + (scheduleId == -1 ? ", 신규" : ", 수정") + ")");
        Intent intent = new Intent(ScheduleActivity.this, ScheduleEditActivity.class);
        intent.putExtra("userSeq", userSeq);
        intent.putExtra("selectedDate", selectedDate);
        intent.putExtra("scheduleId", scheduleId); // -1이면 신규 등록
        startActivity(intent);
    }

    // ===================== 하단 네비게이션 =====================

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleActivity.this, HomeActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navManage).setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleActivity.this, ManageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navTip).setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleActivity.this, TipCategoryActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        findViewById(R.id.navMy).setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleActivity.this, MyPageActivity.class);
            intent.putExtra("userSeq", userSeq);
            startActivity(intent);
        });
        // navSchedule 은 현재 화면이므로 별도 동작 없음
    }

}
