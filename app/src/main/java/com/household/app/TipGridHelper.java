package com.household.app;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * [역할] 팁 카테고리(REQ-011) / 서브카테고리(REQ-012) 화면이 공유하는 버튼 그리드 그리기 헬퍼.
 * TipCategoryActivity와 TipSubCategoryActivity가 검색 결과(카테고리 또는 서브카테고리 이름 목록)를
 * 받아 한 줄에 3개씩 둥근 버튼으로 그려 넣을 때 이 클래스 하나를 공용으로 쓴다. DB 접근은 하지 않는다.
 */
public class TipGridHelper {

    private static final String TAG = "TipGridHelper";

    public interface OnItemClick {
        void onClick(int index, String label);
    }

    private static final int COLUMNS = 3;

    public static void fill(Context context, LinearLayout container, List<String> labels, OnItemClick listener) {
        Log.d(TAG, "fill() 버튼 " + labels.size() + "개 그리기 : " + labels);
        container.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        int margin = Math.round(6 * density);
        int height = Math.round(76 * density);

        for (int start = 0; start < labels.size(); start += COLUMNS) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < COLUMNS; col++) {
                int index = start + col;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
                lp.setMargins(margin, margin, margin, margin);

                TextView tv = new TextView(context);
                tv.setLayoutParams(lp);

                if (index < labels.size()) {
                    String label = labels.get(index);
                    tv.setText(label);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextColor(context.getResources().getColor(R.color.text_white));
                    tv.setTextSize(14);
                    tv.setBackgroundResource(R.drawable.bg_card_navy);
                    tv.setOnClickListener(v -> listener.onClick(index, label));
                } else {
                    tv.setVisibility(View.INVISIBLE); // 마지막 줄 빈칸 (칸 너비 유지용)
                }
                row.addView(tv);
            }
            container.addView(row);
        }
    }
}
