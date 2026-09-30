package com.household.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.household.app.dao.TipDao;
import com.household.app.dao.UserTipDao;
import com.household.app.model.Tip;
import com.household.app.model.UserTip;

import java.util.List;

/**
 * [역할] 팁 상세 화면 (REQ-013) : 예) 팁 > 청소 > 세탁기.
 * 관리자 제공 팁 내용(TipDao)과 "내가 쓴 팁"(UserTipDao, 추가/수정/삭제 가능)을 함께 보여준다.
 * TipSubCategoryActivity에서 tipId를 intent로 넘겨받아 진입한다.
 */
public class TipDetailActivity extends BaseActivity {

    private static final String TAG = "TipDetailActivity";

    private TipDao tipDao;
    private UserTipDao userTipDao;
    private int userSeq;
    private int tipId;

    private LinearLayout myTipContainer;
    private TextView tvNoMyTip;
    private EditText etMyTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_detail);

        tipDao = new TipDao(this);
        userTipDao = new UserTipDao(this);
        userSeq = getIntent().getIntExtra("userSeq", -1);
        tipId = getIntent().getIntExtra("tipId", -1);
        Log.d(TAG, "onCreate() userSeq=" + userSeq + ", tipId=" + tipId);

        myTipContainer = findViewById(R.id.myTipContainer);
        tvNoMyTip = findViewById(R.id.tvNoMyTip);
        etMyTip = findViewById(R.id.etMyTip);

        Tip tip = tipDao.getTipById(tipId);
        if (tip == null) {
            // TipSubCategoryActivity에서 넘겨준 tipId가 DB에 없는 상태. 정상 흐름에서는 발생하지 않아야 한다.
            Log.e(TAG, "tipId=" + tipId + " 에 해당하는 팁을 찾을 수 없어 화면을 닫습니다.");
            finish();
            return;
        }
        ((TextView) findViewById(R.id.tvTipPath)).setText(tip.getCategory() + " › " + tip.getSubCategory());
        ((TextView) findViewById(R.id.tvTipTitle)).setText(tip.getTitle());
        ((TextView) findViewById(R.id.tvTipContent)).setText(tip.getContent());

        findViewById(R.id.btnAddMyTip).setOnClickListener(v -> addMyTip());
        renderMyTips();
    }

    private void addMyTip() {
        String content = etMyTip.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, R.string.msg_my_tip_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        long newId = userTipDao.insert(tipId, userSeq, content);
        if (newId == -1) {
            Log.e(TAG, "addMyTip() 저장 실패 : tipId=" + tipId);
            Toast.makeText(this, R.string.msg_update_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        etMyTip.setText("");
        renderMyTips();
    }

    /** 내가 쓴 팁 목록을 다시 그린다 (각 줄 : 내용 + 수정 + 삭제) */
    private void renderMyTips() {
        myTipContainer.removeAllViews();
        List<UserTip> myTips = userTipDao.getMyTips(tipId, userSeq);

        tvNoMyTip.setVisibility(myTips.isEmpty() ? View.VISIBLE : View.GONE);

        for (UserTip tip : myTips) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_card_white);
            row.setPadding(dp(14), dp(12), dp(8), dp(12));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(8);
            row.setLayoutParams(rowParams);

            TextView tvContent = new TextView(this);
            tvContent.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvContent.setText(tip.getContent());
            tvContent.setTextColor(getResources().getColor(R.color.text_dark));
            tvContent.setTextSize(14);

            TextView tvEdit = makeActionText(R.string.btn_edit_my_tip, R.color.primary_navy);
            tvEdit.setOnClickListener(v -> showEditDialog(tip));

            TextView tvDelete = makeActionText(R.string.btn_delete_my_tip, R.color.error_red);
            tvDelete.setOnClickListener(v -> {
                Log.d(TAG, "내가 쓴 팁 삭제 : user_tip_id=" + tip.getUserTipId());
                userTipDao.delete(tip.getUserTipId());
                renderMyTips();
            });

            row.addView(tvContent);
            row.addView(tvEdit);
            row.addView(tvDelete);
            myTipContainer.addView(row);
        }
    }

    private TextView makeActionText(int textRes, int colorRes) {
        TextView tv = new TextView(this);
        tv.setText(textRes);
        tv.setTextColor(getResources().getColor(colorRes));
        tv.setTextSize(12);
        tv.setPadding(dp(8), dp(4), dp(8), dp(4));
        return tv;
    }

    private void showEditDialog(UserTip tip) {
        final EditText input = new EditText(this);
        input.setText(tip.getContent());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_my_tip_title)
                .setView(input)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    String newContent = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newContent)) {
                        Toast.makeText(this, R.string.msg_my_tip_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.d(TAG, "내가 쓴 팁 수정 : user_tip_id=" + tip.getUserTipId());
                    userTipDao.update(tip.getUserTipId(), newContent);
                    renderMyTips();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
