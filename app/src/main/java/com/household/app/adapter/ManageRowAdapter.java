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
import com.household.app.model.ManageRow;

import java.util.List;

/**
 * [역할] 통합관리(REQ-007) 공과금/식재료/쓰레기 3개 섹션이 공유하는 RecyclerView 어댑터.
 * ManageActivity가 각 섹션마다 이 어댑터를 하나씩(총 3개) 만들어 쓴다.
 * 완료 체크 시(REQ-007 비고 : 밑줄+아래로 이동) 취소선 표시 + 콜백으로 DB 반영, 재정렬은 액티비티가 재조회로 처리.
 */
public class ManageRowAdapter extends RecyclerView.Adapter<ManageRowAdapter.ViewHolder> {

    private static final String TAG = "ManageRowAdapter";

    public interface OnRowActionListener {
        void onCheckChanged(ManageRow row, boolean checked);
        void onItemClick(ManageRow row);
    }

    private final List<ManageRow> items;
    private final OnRowActionListener listener;

    public ManageRowAdapter(List<ManageRow> items, OnRowActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ManageRow row = items.get(position);

        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(row.isDone());
        holder.tvTitle.setText(row.getTitle());
        holder.tvSubtitle.setText(row.getSubtitle());
        applyDoneStyle(holder.tvTitle, row.isDone());

        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "체크 변경 : id=" + row.getId() + ", title=" + row.getTitle() + " -> done=" + isChecked);
            applyDoneStyle(holder.tvTitle, isChecked);
            if (listener != null) {
                listener.onCheckChanged(row, isChecked);
            } else {
                Log.e(TAG, "listener가 null이라 onCheckChanged() 콜백을 호출하지 못했습니다. DB에 반영되지 않습니다.");
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(row);
        });
    }

    private void applyDoneStyle(TextView tv, boolean done) {
        if (done) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.5f);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbDone;
        TextView tvTitle;
        TextView tvSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbRowDone);
            tvTitle = itemView.findViewById(R.id.tvRowTitle);
            tvSubtitle = itemView.findViewById(R.id.tvRowSubtitle);
        }
    }
}
