package com.cappielloantonio.tempo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.service.PeachRadioPlayerManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PeachRadioAdapter
        extends RecyclerView.Adapter<PeachRadioAdapter.ViewHolder> {

    public interface OnPeachRadioClickListener {
        void onPeachRadioClick(PeachRadioStation station);
    }

    private final List<PeachRadioStation> items =
            new ArrayList<>();

    private final OnPeachRadioClickListener listener;
    private String activeSlug;

    public PeachRadioAdapter(
            OnPeachRadioClickListener listener
    ) {
        this.listener = listener;
        this.activeSlug =
                PeachRadioPlayerManager.getActiveRadioSlug();
        setHasStableIds(true);
    }

    public void setItems(List<PeachRadioStation> stations) {
        items.clear();

        if (stations != null) {
            items.addAll(stations);
        }

        notifyDataSetChanged();
    }

    public void setActiveSlug(String slug) {
        if (Objects.equals(activeSlug, slug)) {
            return;
        }

        activeSlug = slug;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        String slug = items.get(position).getSlug();
        return slug != null
                ? slug.hashCode()
                : position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_peach_radio_station,
                                parent,
                                false
                        )
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView icon;
        private final TextView name;
        private final TextView liveBadge;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(
                    R.id.peach_radio_card
            );
            icon = itemView.findViewById(
                    R.id.peach_radio_icon
            );
            name = itemView.findViewById(
                    R.id.peach_radio_name
            );
            liveBadge = itemView.findViewById(
                    R.id.peach_radio_live_badge
            );
        }

        void bind(PeachRadioStation station) {
            name.setText(station.getName());

            String cover = station.getCoverImageUrl();

            Glide.with(itemView)
                    .load(
                            cover != null && !cover.trim().isEmpty()
                                    ? cover
                                    : R.drawable.ic_radio_premium
                    )
                    .placeholder(R.drawable.ic_radio_premium)
                    .centerCrop()
                    .into(icon);

            boolean active = Objects.equals(
                    activeSlug,
                    station.getSlug()
            );

            liveBadge.setVisibility(
                    active ? View.VISIBLE : View.GONE
            );

            card.setStrokeWidth(
                    active
                            ? (int) (
                            2
                            * itemView.getResources()
                            .getDisplayMetrics().density
                    )
                            : (int) (
                            itemView.getResources()
                                    .getDisplayMetrics().density
                    )
            );

            View.OnClickListener clickListener = view -> {
                activeSlug = station.getSlug();
                notifyDataSetChanged();

                if (listener != null) {
                    listener.onPeachRadioClick(station);
                }
            };

            card.setOnClickListener(clickListener);
            itemView.setOnClickListener(clickListener);
        }
    }
}
