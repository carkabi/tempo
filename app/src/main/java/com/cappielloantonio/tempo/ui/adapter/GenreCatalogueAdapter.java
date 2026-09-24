package com.cappielloantonio.tempo.ui.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.databinding.ItemLibraryCatalogueGenreBinding;
import com.cappielloantonio.tempo.interfaces.ClickCallback;
import com.cappielloantonio.tempo.subsonic.models.Genre;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.PeachGenreCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GenreCatalogueAdapter
        extends RecyclerView.Adapter<GenreCatalogueAdapter.ViewHolder>
        implements Filterable {

    private final ClickCallback click;
    private final boolean peachMode =
            "peach".equals(BuildConfig.FLAVOR);
    private final Filter filtering = new Filter() {
        @Override
        protected FilterResults performFiltering(
                CharSequence constraint
        ) {
            List<GenreItem> filtered = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(itemsFull);
            } else {
                String pattern = constraint.toString()
                        .toLowerCase(Locale.ROOT)
                        .trim();

                for (GenreItem item : itemsFull) {
                    if (item.title
                            .toLowerCase(Locale.ROOT)
                            .contains(pattern)) {
                        filtered.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            results.count = filtered.size();
            return results;
        }
        @Override
        protected void publishResults(
                CharSequence constraint,
                FilterResults results
        ) {
            items.clear();

            if (results.values != null) {
                items.addAll((List<GenreItem>) results.values);
            }

            notifyDataSetChanged();
        }
    };

    private final List<GenreItem> items = new ArrayList<>();
    private final List<GenreItem> itemsFull = new ArrayList<>();

    public GenreCatalogueAdapter(ClickCallback click) {
        this.click = click;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemLibraryCatalogueGenreBinding binding =
                ItemLibraryCatalogueGenreBinding.inflate(
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
        GenreItem item = items.get(position);
        holder.binding.genreLabel.setText(item.title);

        if (peachMode && item.songCount > 0) {
            holder.binding.genreLabel.setContentDescription(
                    item.title + ", " + item.songCount + " titres"
            );
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Genre> genres) {
        items.clear();
        itemsFull.clear();

        if (peachMode) {
            for (PeachGenreCatalog.Group group
                    : PeachGenreCatalog.build(genres)) {
                items.add(GenreItem.fromGroup(group));
            }
        } else if (genres != null) {
            for (Genre genre : genres) {
                items.add(GenreItem.fromGenre(genre));
            }
        }

        itemsFull.addAll(items);
        notifyDataSetChanged();
    }
    @Override
    public Filter getFilter() {
        return filtering;
    }

    public void sort(String order) {
        switch (order) {
            case Constants.GENRE_ORDER_BY_NAME:
                items.sort(
                        Comparator.comparing(
                                item -> item.title,
                                String.CASE_INSENSITIVE_ORDER
                        )
                );
                break;
            case Constants.GENRE_ORDER_BY_RANDOM:
                Collections.shuffle(items);
                break;
        }

        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final ItemLibraryCatalogueGenreBinding binding;

        ViewHolder(ItemLibraryCatalogueGenreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> onClick());
            binding.genreLabel.setOnClickListener(v -> onClick());
        }
        private void onClick() {
            int position = getBindingAdapterPosition();

            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            GenreItem item = items.get(position);
            Bundle bundle = new Bundle();

            if (peachMode && item.group != null) {
                bundle.putString(
                        Constants.MEDIA_BY_GENRES,
                        Constants.MEDIA_BY_GENRES
                );
                bundle.putStringArrayList(
                        "filters_list",
                        item.group.getRawGenres()
                );

                ArrayList<String> names = new ArrayList<>();
                names.add(item.group.getTitle());
                bundle.putStringArrayList(
                        "filter_name_list",
                        names
                );
            } else {
                bundle.putString(
                        Constants.MEDIA_BY_GENRE,
                        Constants.MEDIA_BY_GENRE
                );
                bundle.putParcelable(
                        Constants.GENRE_OBJECT,
                        item.genre
                );
            }

            click.onGenreClick(bundle);
        }
    }
    private static final class GenreItem {
        final String title;
        final Genre genre;
        final PeachGenreCatalog.Group group;
        final int songCount;

        private GenreItem(
                String title,
                Genre genre,
                PeachGenreCatalog.Group group,
                int songCount
        ) {
            this.title = title;
            this.genre = genre;
            this.group = group;
            this.songCount = songCount;
        }

        static GenreItem fromGenre(Genre genre) {
            return new GenreItem(
                    genre.getGenre(),
                    genre,
                    null,
                    genre.getSongCount()
            );
        }

        static GenreItem fromGroup(
                PeachGenreCatalog.Group group
        ) {
            return new GenreItem(
                    group.getTitle(),
                    null,
                    group,
                    group.getSongCount()
            );
        }
    }
}
