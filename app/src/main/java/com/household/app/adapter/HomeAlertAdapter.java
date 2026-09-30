package com.household.app.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.household.app.R;
import com.household.app.model.HomeAlertItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * [역할] 홈 화면(REQ-014) "가장 근접한 기한 알림" RecyclerView 어댑터.
 * HomeActivity가 HomeAlertDao로 읽어온 목록을 화면에 그리고, 항목의 별 아이콘을 누르면
 * 즐겨찾기 토글 콜백(OnFavoriteClickListener)을 호출해 실제 DB 처리는 HomeActivity에게 맡긴다.
 */
public class HomeAlertAdapter extends RecyclerView.Adapter<HomeAlertAdapter.ViewHolder> {

    private static final String TAG = "HomeAlertAdapter";

    /** 별 아이콘 클릭 시 액티비티로 전달할 콜백 */
    public interface OnFavoriteClickListener {
        void onFavoriteClick(HomeAlertItem item, int position);
    }

    private final List<HomeAlertItem> items;
    private final OnFavoriteClickListener listener;

    public HomeAlertAdapter(List<HomeAlertItem> items, OnFavoriteClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeAlertItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(formatDday(item.getTargetDate()) + " · " + item.getTargetDate());

        holder.ivFavorite.setImageResource(
                item.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
        );

        holder.ivFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(item, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * target_date(YYYY-MM-DD) 를 "D-3" / "D-DAY" 형태 문구로 변환.
     * java.time(LocalDate)는 API 26부터 지원되므로 minSdk 24 호환을 위해
     * SimpleDateFormat + Calendar 로 날짜 차이를 계산한다.
     */
    private String formatDday(String targetDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date target = sdf.parse(targetDate);
            Date today = sdf.parse(sdf.format(new Date())); // 시:분:초 제거하고 자정 기준으로 비교

            long diffMillis = target.getTime() - today.getTime();
            long diffDays = diffMillis / (24 * 60 * 60 * 1000);

            if (diffDays == 0) return "D-DAY";
            return diffDays > 0 ? "D-" + diffDays : "D+" + Math.abs(diffDays);
        } catch (ParseException e) {
            // 화면에는 그냥 빈 D-day 문구만 보이고 왜 빈지는 알 수 없으므로, 원인(어떤 날짜 문자열이 문제였는지)을 남긴다.
            Log.e(TAG, "formatDday() 날짜 파싱 실패 : targetDate=" + targetDate, e);
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        ImageView ivFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
        }
    }
}
