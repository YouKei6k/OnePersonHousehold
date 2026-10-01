package com.household.app;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.household.app.dao.ScheduleDao;
import com.household.app.dao.TrashDao;
import com.household.app.dao.TrashRegionDao;
import com.household.app.model.Schedule;
import com.household.app.model.TrashItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * [역할] 쓰레기 배출 등록/수정 화면 (REQ-010)
 * 쓰레기 종류 + 사는 지역(시도->시군구->관리구역) 선택 시 공공데이터 기반으로 배출일을 자동 계산한다.
 * 자동 계산이 안 되면(공공데이터 없음, 의류 등) 사용자가 직접 날짜를 선택해야 하고,
 * 자동으로 계산된 경우에도 "수동 변경"으로 언제든 덮어쓸 수 있다.
 * 지역 3단계 스피너는 시도->시군구->관리구역 순으로 연쇄 갱신되며, 수정 모드에서는 저장된 지역 문자열을
 * TrashRegionDao.parseRegionLabel()로 되돌려 복원한다.
 */
public class TrashEditActivity extends BaseActivity {

    private static final String TAG = "TrashEditActivity";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String HINT = "선택해주세요"; // 스피너 0번째 placeholder

    private TrashDao trashDao;
    private TrashRegionDao regionDao;
    private ScheduleDao scheduleDao;
    private int userSeq;
    private int trashId; // -1 = 신규

    private Spinner spTrashType, spSido, spSigungu, spArea;
    private TextView tvScreenTitle, tvDisposalDate, tvAutoBadge, tvEditDateManually, tvRecalcAuto, tvDelete;
    private EditText etDday;

    private String disposalDate; // yyyy-MM-dd
    private boolean isAuto;
    private boolean suppressAutoCalc; // (예비) 자동계산을 잠시 막아야 할 때 사용
    private boolean isEditMode;       // 수정 모드에서는 사용자가 스피너를 직접 건드리기 전까지 저장된 배출일을 덮어쓰지 않는다
    private boolean userInteracted;   // 사용자가 스피너를 한 번이라도 터치했는지

    // 지역 스피너 연쇄 선택 상태 : 이 값과 같은 선택이면 하위 목록을 다시 채우지 않는다
    // (저장된 지역을 복원할 때 리스너가 뒤늦게 호출되며 복원한 선택을 지워버리는 문제 방지)
    private String loadedSido;
    private String loadedSigungu;

    private static final String PREF_NAME = "trash_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_edit);

        trashDao = new TrashDao(this);
        regionDao = new TrashRegionDao(this);
        scheduleDao = new ScheduleDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        trashId = getIntent().getIntExtra("trashId", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq + ", trashId=" + trashId
                + (trashId == -1 ? " (신규)" : " (수정)"));

        bindViews();
        setupTrashTypeSpinner();
        setupRegionSpinners();

        tvRecalcAuto.setOnClickListener(v -> tryAutoCalc(true));
        tvEditDateManually.setOnClickListener(v -> pickDateManually());
        findViewById(R.id.btnRegister).setOnClickListener(v -> save());
        tvDelete.setOnClickListener(v -> confirmDelete());

        if (trashId != -1) {
            isEditMode = true;
            tvScreenTitle.setText(R.string.title_trash_edit);
            tvDelete.setVisibility(View.VISIBLE);
            loadExisting();
        } else {
            tvScreenTitle.setText(R.string.title_trash_add);
            updateDisposalDateDisplay();
            prefillLastRegion(); // 지난번에 등록한 사는 지역을 미리 채워둔다
        }
    }

    private void bindViews() {
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        spTrashType = findViewById(R.id.spTrashType);
        spSido = findViewById(R.id.spSido);
        spSigungu = findViewById(R.id.spSigungu);
        spArea = findViewById(R.id.spArea);
        tvDisposalDate = findViewById(R.id.tvDisposalDate);
        tvAutoBadge = findViewById(R.id.tvAutoBadge);
        tvEditDateManually = findViewById(R.id.tvEditDateManually);
        tvRecalcAuto = findViewById(R.id.tvRecalcAuto);
        etDday = findViewById(R.id.etDday);
        tvDelete = findViewById(R.id.tvDelete);
    }

    // ===================== 쓰레기 종류 스피너 =====================

    private void setupTrashTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.trash_type_array, android.R.layout.simple_spinner_dropdown_item);
        spTrashType.setAdapter(adapter);
        spTrashType.setOnTouchListener(touchTracker);
        spTrashType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tryAutoCalc(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // ===================== 지역 3단계 스피너 (시도 -> 시군구 -> 관리구역) =====================

    /** 스피너를 사용자가 직접 터치했는지 추적 (수정 모드에서 저장된 배출일을 함부로 덮어쓰지 않기 위해) */
    private final View.OnTouchListener touchTracker = (v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_DOWN) userInteracted = true;
        return false; // 터치 동작(드롭다운 열기)은 그대로 진행
    };

    private void setupRegionSpinners() {
        setSpinnerItems(spSido, withHint(regionDao.getSidoList()));
        setSpinnerItems(spSigungu, withHint(new ArrayList<>()));
        setSpinnerItems(spArea, withHint(new ArrayList<>()));

        spSido.setOnTouchListener(touchTracker);
        spSigungu.setOnTouchListener(touchTracker);
        spArea.setOnTouchListener(touchTracker);

        spSido.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sido = selectedOrNull(spSido);
                if (!Objects.equals(sido, loadedSido)) { // 시도가 실제로 바뀐 경우에만 하위 목록 초기화
                    loadedSido = sido;
                    loadedSigungu = null;
                    setSpinnerItems(spSigungu, withHint(sido != null
                            ? regionDao.getSigunguList(sido) : new ArrayList<>()));
                    setSpinnerItems(spArea, withHint(new ArrayList<>()));
                }
                tryAutoCalc(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spSigungu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sigungu = selectedOrNull(spSigungu);
                if (!Objects.equals(sigungu, loadedSigungu)) { // 시군구가 실제로 바뀐 경우에만 관리구역 목록 갱신
                    loadedSigungu = sigungu;
                    String sido = selectedOrNull(spSido);
                    setSpinnerItems(spArea, withHint(sido != null && sigungu != null
                            ? regionDao.getAreaList(sido, sigungu) : new ArrayList<>()));
                }
                tryAutoCalc(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tryAutoCalc(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /** 저장돼 있던 지역(시도/시군구/관리구역)을 세 스피너에 그대로 복원한다 */
    private void applyRegion(String[] region) {
        setSpinnerSelection(spSido, region[0]);
        loadedSido = region[0];

        setSpinnerItems(spSigungu, withHint(regionDao.getSigunguList(region[0])));
        setSpinnerSelection(spSigungu, region[1]);
        loadedSigungu = region[1];

        setSpinnerItems(spArea, withHint(regionDao.getAreaList(region[0], region[1])));
        setSpinnerSelection(spArea, region[2]);
    }

    /** 새로 등록할 때 : 이 사용자가 마지막으로 쓴 지역을 미리 채운다 (SharedPreferences 우선, 없으면 마지막 등록 내역) */
    private void prefillLastRegion() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String label = pref.getString("region_" + userSeq, null);
        if (label == null) label = trashDao.getLastAreaByUser(userSeq);

        Log.d(TAG, "prefillLastRegion() 후보 지역 : " + label);
        String[] region = regionDao.parseRegionLabel(label);
        if (region != null) {
            applyRegion(region);
        } else if (label != null) {
            Log.w(TAG, "prefillLastRegion() 지역 복원 실패, 새로 선택해야 함 : " + label);
        }
    }

    private List<String> withHint(List<String> list) {
        List<String> result = new ArrayList<>();
        result.add(HINT);
        result.addAll(list);
        return result;
    }

    private void setSpinnerItems(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
    }

    /** 스피너에서 placeholder("선택해주세요")가 아닌 실제 선택값만 반환 */
    private String selectedOrNull(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        if (selected == null) return null;
        String value = selected.toString();
        return HINT.equals(value) ? null : value;
    }

    // ===================== 배출일 자동 계산 (REQ-010) =====================

    /**
     * 쓰레기종류 + 시도/시군구/관리구역이 모두 선택되면 공공데이터에서 배출요일을 찾아
     * 가장 가까운 배출일을 자동 계산한다. 못 찾으면(공공데이터 없음, '의류' 선택 등) 수동 입력으로 안내한다.
     * @param force true 이면 "자동으로 다시 계산" 버튼을 눌렀을 때처럼 조건과 상관없이 즉시 계산하고 결과를 안내한다.
     */
    private void tryAutoCalc(boolean force) {
        if (!force && (suppressAutoCalc || (isEditMode && !userInteracted))) return;

        String trashType = spTrashType.getSelectedItem() != null ? spTrashType.getSelectedItem().toString() : null;
        String sido = selectedOrNull(spSido);
        String sigungu = selectedOrNull(spSigungu);
        String area = selectedOrNull(spArea);

        if (trashType == null) return;

        if (sido == null || sigungu == null || area == null) {
            if (force) {
                Log.d(TAG, "tryAutoCalc(force=true) 지역 선택 미완료 - 안내만 표시");
                Toast.makeText(this, R.string.msg_select_region_first, Toast.LENGTH_SHORT).show();
            }
            return; // 아직 선택이 다 안 끝남 -> 계산하지 않고 대기
        }

        if (TrashItem.TYPE_CLOTHING.equals(trashType)) {
            // 의류는 공공데이터가 없어 항상 수동 (REQ-010 비고)
            isAuto = false;
            tvAutoBadge.setText(R.string.badge_manual);
            Toast.makeText(this, R.string.msg_auto_calc_fail, Toast.LENGTH_SHORT).show();
            return;
        }

        String weekdayPattern = regionDao.getWeekdayPattern(sido, sigungu, area, trashType);
        String calculated = TrashRegionDao.calcNearestDate(weekdayPattern);

        if (calculated == null) {
            Log.d(TAG, "tryAutoCalc() 자동계산 실패 -> 수동 전환 (" + sido + " " + sigungu + " " + area + " / " + trashType + ")");
            isAuto = false;
            tvAutoBadge.setText(R.string.badge_manual);
            Toast.makeText(this, R.string.msg_auto_calc_fail, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "tryAutoCalc() 자동계산 성공 -> " + calculated);
            disposalDate = calculated;
            isAuto = true;
            tvAutoBadge.setText(R.string.badge_auto);
            updateDisposalDateDisplay();
            if (force) Toast.makeText(this, R.string.msg_auto_calc_done, Toast.LENGTH_SHORT).show();
        }
    }

    private void pickDateManually() {
        Calendar cal = Calendar.getInstance();
        try {
            if (disposalDate != null) {
                cal.setTime(new SimpleDateFormat(DATE_FORMAT, Locale.KOREA).parse(disposalDate));
            }
        } catch (Exception e) {
            Log.w(TAG, "pickDateManually() 기존 배출일 파싱 실패, 오늘 날짜로 대체 : " + disposalDate, e);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            disposalDate = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Log.d(TAG, "pickDateManually() 수동 지정 : " + disposalDate);
            isAuto = false; // 사용자가 직접 고르면 수동으로 전환 (REQ-010 : 언제든 수동 입력 가능)
            tvAutoBadge.setText(R.string.badge_manual);
            updateDisposalDateDisplay();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDisposalDateDisplay() {
        tvDisposalDate.setText(disposalDate != null ? disposalDate : "");
    }

    // ===================== 기존 데이터 불러오기 (수정 모드) =====================

    private void loadExisting() {
        TrashItem item = trashDao.getById(trashId);
        if (item == null) {
            Log.e(TAG, "loadExisting() trashId=" + trashId + " 를 찾을 수 없어 화면을 닫습니다.");
            finish();
            return;
        }

        setSpinnerSelection(spTrashType, item.getTrashType());

        // 저장해둔 지역을 세 스피너에 복원 (복원 실패 시에는 새로 선택하도록 비워둠)
        String[] region = regionDao.parseRegionLabel(item.getArea());
        if (region != null) {
            applyRegion(region);
        } else if (item.getArea() != null) {
            Log.w(TAG, "loadExisting() 저장된 지역 복원 실패 : " + item.getArea());
        }

        disposalDate = item.getDisposalDate();
        isAuto = item.isAuto();
        tvAutoBadge.setText(isAuto ? R.string.badge_auto : R.string.badge_manual);
        updateDisposalDateDisplay();

        if (item.getDday() != null) {
            etDday.setText(String.valueOf(item.getDday()));
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null || spinner.getAdapter() == null) return;
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (value.equals(spinner.getAdapter().getItem(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    // ===================== 저장 =====================

    private void save() {
        String trashType = spTrashType.getSelectedItem() != null ? spTrashType.getSelectedItem().toString() : null;
        String sido = selectedOrNull(spSido);
        String sigungu = selectedOrNull(spSigungu);
        String area = selectedOrNull(spArea);

        if (TextUtils.isEmpty(disposalDate)) {
            Log.d(TAG, "save() 배출일 비어있음 - 저장 취소");
            Toast.makeText(this, R.string.error_date_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String regionLabel = (sido != null && sigungu != null && area != null)
                ? (sido + " " + sigungu + " " + area) : "";

        Integer dday = null;
        String ddayStr = etDday.getText().toString().trim();
        if (!TextUtils.isEmpty(ddayStr)) {
            try {
                dday = Integer.parseInt(ddayStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "save() dday 파싱 실패, 알림 없이 저장 : \"" + ddayStr + "\"", e);
            }
        }

        // 다음에 새로 등록할 때 같은 지역이 미리 채워지도록 기억해둔다
        if (!regionLabel.isEmpty()) {
            getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                    .putString("region_" + userSeq, regionLabel).apply();
        }

        TrashItem item = new TrashItem();
        item.setUserSeq(userSeq);
        item.setTrashType(trashType);
        item.setArea(regionLabel.isEmpty() ? null : regionLabel);
        item.setDisposalDate(disposalDate);
        item.setAuto(isAuto);
        item.setDday(dday);

        if (trashId == -1) {
            long newId = trashDao.insert(item);
            Log.i(TAG, "save() 신규 등록 : trash_id=" + newId + ", type=" + trashType
                    + ", date=" + disposalDate + ", auto=" + isAuto);

            // REQ-010 : "자동 계산하여 생활일정에 등록한다" -> SCHEDULE 에도 가볍게 함께 등록
            // (수정 시에는 중복 등록을 막기 위해 최초 등록 때만 수행)
            Schedule schedule = new Schedule();
            schedule.setUserSeq(userSeq);
            schedule.setTitle(trashType + " 배출");
            schedule.setDateStart(disposalDate);
            schedule.setDateEnd(disposalDate);
            long scheduleId = scheduleDao.insertSchedule(schedule);
            Log.d(TAG, "save() 생활일정에도 함께 등록 : schedule_id=" + scheduleId);
        } else {
            item.setTrashId(trashId);
            trashDao.update(item);
            Log.i(TAG, "save() 수정 : trash_id=" + trashId + ", date=" + disposalDate + ", auto=" + isAuto);
        }

        // REQ-010 : 등록 -> 통합관리로 돌아감
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_schedule_title)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    Log.i(TAG, "confirmDelete() 삭제 확정 : trashId=" + trashId);
                    trashDao.delete(trashId);
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
