package com.cappielloantonio.tempo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cappielloantonio.tempo.databinding.ItemNewsBinding;
import com.cappielloantonio.tempo.repository.peach.models.PeachNews;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    private List<PeachNews> items = new ArrayList<>();
    private final OnNewsClickListener listener;

    public interface OnNewsClickListener {
        void onNewsClick(PeachNews news);
    }

    public NewsAdapter(OnNewsClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PeachNews> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding bind;

        ViewHolder(ItemNewsBinding bind) {
            super(bind.getRoot());
            this.bind = bind;
        }

        void bind(PeachNews news) {
            bind.newsTitle.setText(news.getTitle());
            bind.newsSummary.setText(news.getSummary());
            bind.newsCategory.setText(news.getCategoryLabel() != null ? news.getCategoryLabel().toUpperCase() : "ACTUALITÉ");
            bind.newsDate.setText(news.getPublishedAt()); // Formattage date à améliorer si besoin

            if (news.getCoverImageUrl() != null && !news.getCoverImageUrl().isEmpty()) {
                bind.newsCoverImage.setVisibility(View.VISIBLE);
                Glide.with(bind.getRoot().getContext())
                        .load(news.getCoverImageUrl())
                        .into(bind.newsCoverImage);
            } else {
                bind.newsCoverImage.setVisibility(View.GONE);
            }

            View.OnClickListener click = v -> listener.onNewsClick(news);
            bind.newsCard.setOnClickListener(click);
            bind.newsReadMore.setOnClickListener(click);
            
            if (news.isPinned()) {
                bind.newsCard.setCardBackgroundColor(0x4DFFD700); // Overlay or teinté
            } else {
                bind.newsCard.setCardBackgroundColor(0x26FFFFFF);
            }
        }
    }
}