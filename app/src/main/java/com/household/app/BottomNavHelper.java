package com.household.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * [역할] <include layout="@layout/bottom_nav.xml" /> 를 쓰는 모든 화면이 공유하는 탭 강조 헬퍼.
 * 이제 6개 화면(홈/일정관리/관리/마이페이지/팁 카테고리/팁 서브카테고리)이 레이아웃 파일 하나를
 * 같이 쓰기 때문에 활성 탭 표시는 코드에서 정한다. DB나 네트워크를 쓰지 않는 순수 화면 헬퍼다.
 */
public class BottomNavHelper {

    private static final String TAG = "BottomNavHelper";

    public enum Tab { HOME, SCHEDULE, MANAGE, TIP, MY }

    public static void setActive(Activity activity, Tab active) {
        apply(activity, R.id.ivNavHome, R.id.tvNavHome, active == Tab.HOME);
        apply(activity, R.id.ivNavSchedule, R.id.tvNavSchedule, active == Tab.SCHEDULE);
        apply(activity, R.id.ivNavManage, R.id.tvNavManage, active == Tab.MANAGE);
        apply(activity, R.id.ivNavTip, R.id.tvNavTip, active == Tab.TIP);
        apply(activity, R.id.ivNavMy, R.id.tvNavMy, active == Tab.MY);
    }

    private static void apply(Activity activity, int iconId, int labelId, boolean active) {
        ImageView icon = activity.findViewById(iconId);
        TextView label = activity.findViewById(labelId);
        if (icon == null || label == null) {
            // 이 화면의 레이아웃에 <include layout="@layout/bottom_nav.xml" />이 빠졌거나
            // id가 bottom_nav.xml과 어긋난 경우에만 발생한다. 하단 탭이 안 보이는데 원인을 모를 때 확인할 로그.
            Log.w(TAG, activity.getClass().getSimpleName()
                    + " 에서 하단 네비 View를 찾지 못했습니다 (bottom_nav.xml include 여부 확인 필요).");
            return;
        }

        int color = activity.getResources().getColor(
                active ? R.color.primary_navy : R.color.text_gray);

        icon.setColorFilter(color);
        label.setTextColor(color);
        label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }
}
