package com.cappielloantonio.tempo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.ItemSharedPlaylistTrackBinding;
import com.cappielloantonio.tempo.glide.CustomGlideRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistItem;
import com.cappielloantonio.tempo.util.MusicUtil;

import java.util.ArrayList;
import java.util.List;

public class SharedPlaylistAdapter
        extends RecyclerView.Adapter<SharedPlaylistAdapter.ViewHolder> {

    public interface Callback {
        void onPlay(int position);
        void onRemove(SharedPlaylistItem item);
        void onVote(SharedPlaylistItem item, int vote);
    }

    private final Callback callback;
    private final List<SharedPlaylistItem> items =
            new ArrayList<>();

    public SharedPlaylistAdapter(Callback callback) {
        this.callback = callback;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSharedPlaylistTrackBinding binding =
                ItemSharedPlaylistTrackBinding.inflate(
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

        holder.binding.title.setText(item.getTitle());

        String artist = item.getArtist() != null
                ? item.getArtist()
                : holder.itemView.getContext()
                    .getString(
                            R.string.shared_playlist_unknown_artist
                    );
        String addedBy = item.getAddedBy() != null
                ? item.getAddedBy()
                : holder.itemView.getContext()
                    .getString(R.string.shared_playlist_member);
        String duration = MusicUtil.getReadableDurationString(
                item.getDuration(),
                false
        );
        holder.binding.subtitle.setText(
                holder.itemView.getContext().getString(
                        R.string.shared_playlist_item_subtitle,
                        artist,
                        duration,
                        addedBy
                )
        );

        holder.binding.score.setText(
                String.valueOf(item.getScore())
        );

        holder.binding.voteUp.setAlpha(
                item.getUserVote() == 1 ? 1f : 0.52f
        );
        holder.binding.voteDown.setAlpha(
                item.getUserVote() == -1 ? 1f : 0.52f
        );

        holder.binding.remove.setVisibility(
                item.canRemove() ? View.VISIBLE : View.GONE
        );

        CustomGlideRequest.Builder
                .from(
                        holder.itemView.getContext(),
                        item.getCoverArtId(),
                        CustomGlideRequest.ResourceType.Song
                )
                .build()
                .into(holder.binding.cover);
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
        final ItemSharedPlaylistTrackBinding binding;

        ViewHolder(ItemSharedPlaylistTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    callback.onPlay(position);
                }
            });

            binding.voteUp.setOnClickListener(v ->
                    dispatchVote(1)
            );

            binding.voteDown.setOnClickListener(v ->
                    dispatchVote(-1)
            );

            binding.remove.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    callback.onRemove(items.get(position));
                }
            });
        }
        private void dispatchVote(int requestedVote) {
            int position = getBindingAdapterPosition();

            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            SharedPlaylistItem item = items.get(position);
            int vote = item.getUserVote() == requestedVote
                    ? 0
                    : requestedVote;

            callback.onVote(item, vote);
        }
    }
}
