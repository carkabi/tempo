package com.cappielloantonio.tempo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.ItemHorizontalTrackBinding;
import com.cappielloantonio.tempo.glide.CustomGlideRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistItem;
import com.cappielloantonio.tempo.util.MusicUtil;

import java.util.ArrayList;
import java.util.List;

public class SharedPlaylistAdapter extends RecyclerView.Adapter<SharedPlaylistAdapter.ViewHolder> {
    public interface Callback {
        void onPlay(int position);
        void onRemove(SharedPlaylistItem item);
    }

    private final Callback callback;
    private final List<SharedPlaylistItem> items = new ArrayList<>();

    public SharedPlaylistAdapter(Callback callback) {
        this.callback = callback;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemHorizontalTrackBinding binding =
                ItemHorizontalTrackBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        SharedPlaylistItem item = items.get(position);

        holder.binding.searchResultSongTitleTextView
                .setText(item.getTitle());

        String artist = item.getArtist() != null
                ? item.getArtist()
                : holder.itemView.getContext()
                    .getString(R.string.shared_playlist_unknown_artist);
        String addedBy = item.getAddedBy() != null
                ? item.getAddedBy()
                : holder.itemView.getContext()
                    .getString(R.string.shared_playlist_member);

        String duration = MusicUtil.getReadableDurationString(
                item.getDuration(),
                false
        );

        holder.binding.searchResultSongSubtitleTextView.setText(
                holder.itemView.getContext().getString(
                        R.string.shared_playlist_item_subtitle,
                        artist,
                        duration,
                        addedBy
                )
        );

        holder.binding.trackNumberTextView.setVisibility(View.INVISIBLE);
        holder.binding.songCoverImageView.setVisibility(View.VISIBLE);
        holder.binding.searchResultDownloadIndicatorImageView
                .setVisibility(View.GONE);
        holder.binding.ratingIndicatorImageView.setVisibility(View.GONE);

        CustomGlideRequest.Builder
                .from(
                        holder.itemView.getContext(),
                        item.getCoverArtId(),
                        CustomGlideRequest.ResourceType.Song
                )
                .build()
                .into(holder.binding.songCoverImageView);

        holder.binding.searchResultSongMoreButton.setVisibility(
                item.canRemove() ? View.VISIBLE : View.INVISIBLE
        );
    }
    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<SharedPlaylistItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<SharedPlaylistItem> getItems() {
        return new ArrayList<>(items);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHorizontalTrackBinding binding;

        ViewHolder(ItemHorizontalTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    callback.onPlay(position);
                }
            });

            binding.searchResultSongMoreButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    SharedPlaylistItem item = items.get(position);
                    if (item.canRemove()) {
                        callback.onRemove(item);
                    }
                }
            });
        }
    }
}
