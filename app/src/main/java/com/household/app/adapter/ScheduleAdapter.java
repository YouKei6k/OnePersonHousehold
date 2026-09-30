package com.household.app.adapter;

import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.household.app.R;
import com.household.app.model.Schedule;

import java.util.List;

/**
 * [역할] 생활일정관리(REQ-006) 할일목록 RecyclerView 어댑터.
 * 체크박스를 누르면 완료 처리 콜백을 호출하고(목록 갱신은 액티비티가 재조회해서 처리),
 * 항목 자체를 누르면 수정 화면으로 이동하는 콜백을 호출한다. DB 접근은 하지 않고 콜백만 던진다.
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private static final String TAG = "ScheduleAdapter";

    public interface OnScheduleActionListener {
        void onCheckChanged(Schedule schedule, boolean checked);
        void onItemClick(Schedule schedule);
    }

    private final List<Schedule> items;
    private final OnScheduleActionListener listener;

    public ScheduleAdapter(List<Schedule> items, OnScheduleActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule schedule = items.get(position);

        // 체크리스너를 먼저 해제하고 세팅해야 재사용(recycle) 시 이전 리스너가 중복 호출되지 않는다
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(schedule.isDone());
        holder.tvTitle.setText(schedule.getTitle());
        applyDoneStyle(holder.tvTitle, schedule.isDone());

        if (schedule.isRecurring()) {
            holder.tvRecurBadge.setVisibility(View.VISIBLE);
            holder.tvRecurBadge.setText(recurLabel(schedule.getRecurringType()));
        } else {
            holder.tvRecurBadge.setVisibility(View.GONE);
        }

        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "체크 변경 : schedule_id=" + schedule.getScheduleId() + " -> done=" + isChecked);
            applyDoneStyle(holder.tvTitle, isChecked);
            if (listener != null) {
                listener.onCheckChanged(schedule, isChecked);
            } else {
                // 콜백이 없으면 체크만 되고 DB에는 전혀 반영되지 않으므로, 이 조합은 반드시 버그다.
                Log.e(TAG, "listener가 null이라 onCheckChanged() 콜백을 호출하지 못했습니다. DB에 반영되지 않습니다.");
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(schedule);
            }
        });
    }

    /** 완료된 항목은 취소선으로 표시 (REQ-006 : 체크하면 맨 아래로 이동 + 완료 표시) */
    private void applyDoneStyle(TextView tv, boolean done) {
        if (done) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.5f);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setAlpha(1f);
        }
    }

    private String recurLabel(String recurringType) {
        if (Schedule.RECUR_WEEK.equals(recurringType)) return "매주";
        if (Schedule.RECUR_MONTH.equals(recurringType)) return "매월";
        if (Schedule.RECUR_YEAR.equals(recurringType)) return "매년";
        return "";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbDone;
        TextView tvTitle;
        TextView tvRecurBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbDone);
            tvTitle = itemView.findViewById(R.id.tvScheduleTitle);
            tvRecurBadge = itemView.findViewById(R.id.tvRecurBadge);
        }
    }
}
