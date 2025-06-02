package com.example.jarm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.util.Locale;

public class RoundHistoryAdapter extends RecyclerView.Adapter<RoundHistoryAdapter.ViewHolder> {

    private List<RoundHistoryItem> historyItems;
    private Context context;

    public RoundHistoryAdapter(List<RoundHistoryItem> historyItems, Context context) {
        this.historyItems = historyItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_round_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoundHistoryItem item = historyItems.get(position);

        holder.roundLabel.setText(String.format(Locale.getDefault(), context.getString(R.string.history_item_round_label_format), item.roundNumber));
        holder.problemText.setText(String.format(Locale.getDefault(), context.getString(R.string.history_item_problem_format), item.problemExpression, item.correctAnswer));

        String userAnswerDisplay;
        if (item.wasTimeUp) {
            userAnswerDisplay = context.getString(R.string.history_item_time_up);
        } else if (item.userAnswer.equals(context.getString(R.string.history_item_no_answer))) {
            userAnswerDisplay = context.getString(R.string.history_item_no_answer);
        }
        else {
            userAnswerDisplay = item.userAnswer;
        }
        holder.userAnswerText.setText(String.format(Locale.getDefault(), context.getString(R.string.history_item_user_answer_format), userAnswerDisplay));


        if (item.wasCorrect) {
            holder.cardBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.feedback_panel_correct_bg));
        } else {
            holder.cardBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.feedback_panel_incorrect_bg));
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardBackground;
        TextView roundLabel;
        TextView problemText;
        TextView userAnswerText;

        ViewHolder(View itemView) {
            super(itemView);
            cardBackground = itemView.findViewById(R.id.card_history_item_background);
            roundLabel = itemView.findViewById(R.id.text_view_history_round_label);
            problemText = itemView.findViewById(R.id.text_view_history_problem);
            userAnswerText = itemView.findViewById(R.id.text_view_history_user_answer);
        }
    }
}