package com.cappielloantonio.tempo.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.repository.peach.models.RadioProgramItem;
import com.cappielloantonio.tempo.util.PeachRadioCache;

import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class PeachRadioAdapter extends RecyclerView.Adapter<PeachRadioAdapter.ViewHolder> {

    private static final String TAG = "PEACH_RADIO";

    public interface OnPeachRadioClickListener {
        void onPeachRadioClick(PeachRadioStation station);
    }

    private List<PeachRadioStation> items = new ArrayList<>();
    private final OnPeachRadioClickListener listener;

    public PeachRadioAdapter(OnPeachRadioClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PeachRadioStation> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peach_radio_station, parent, false);
        return new ViewHolder(view);
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
        private final TextView radioName;
        private final TextView radioDesc;
        private final ImageView radioIcon;
        private final TextView liveBadge;
        private final View trackInfoContainer;
        private final TextView currentTrackText;
        private final TextView nextTrackText;
        private final TextView historyText;
        private final TextView upcomingText;
        private final View radioCard;

        ViewHolder(View itemView) {
            super(itemView);
            this.radioCard = itemView.findViewById(R.id.peach_radio_card);
            this.radioName = itemView.findViewById(R.id.peach_radio_name);
            this.radioDesc = itemView.findViewById(R.id.peach_radio_desc);
            this.radioIcon = itemView.findViewById(R.id.peach_radio_icon);
            this.liveBadge = itemView.findViewById(R.id.peach_radio_live_badge);
            this.trackInfoContainer = itemView.findViewById(R.id.peach_radio_track_info_container);
            this.currentTrackText = itemView.findViewById(R.id.peach_radio_current_track);
            this.nextTrackText = itemView.findViewById(R.id.peach_radio_next_track);
            this.historyText = itemView.findViewById(R.id.peach_radio_history);
            this.upcomingText = itemView.findViewById(R.id.peach_radio_upcoming);
        }

        void bind(PeachRadioStation station) {
            if (radioName != null) radioName.setText(station.getName());
            if (radioDesc != null) {
                radioDesc.setText(
                        station.getDescription() != null
                                ? station.getDescription()
                                : itemView.getContext().getString(
                                        R.string.peach_radio_default_description
                                )
                );
            }

            if (radioIcon != null) {
                if (station.getCoverImageUrl() != null && !station.getCoverImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(station.getCoverImageUrl())
                            .placeholder(R.drawable.ic_radio_premium)
                            .into(radioIcon);
                } else {
                    radioIcon.setImageResource(R.drawable.ic_radio_premium);
                }
            }

            RadioProgramItem activeItem = PeachRadioCache.findActiveProgramItem(station.getSlug());
            RadioProgramItem nextItem = PeachRadioCache.findNextProgramItem(station.getSlug(), activeItem);

            if (trackInfoContainer != null) {
                if (activeItem != null) {
                    trackInfoContainer.setVisibility(View.VISIBLE);
                    if (liveBadge != null) {
                        liveBadge.setVisibility(View.VISIBLE);
                    }

                    String currentTitle = activeItem.getTitle() != null
                            ? activeItem.getTitle()
                            : station.getName();
                    String currentArtist = activeItem.getArtist() != null
                            ? activeItem.getArtist()
                            : station.getName();

                    if (currentTrackText != null) {
                        currentTrackText.setText(
                                itemView.getContext().getString(
                                        R.string.peach_radio_now_format,
                                        currentTitle,
                                        currentArtist
                                )
                        );
                    }

                    if (nextTrackText != null) {
                        if (nextItem != null && nextItem.getTitle() != null) {
                            nextTrackText.setVisibility(View.VISIBLE);
                            nextTrackText.setText(
                                    itemView.getContext().getString(
                                            R.string.peach_radio_next_format,
                                            nextItem.getTitle(),
                                            nextItem.getArtist() != null
                                                    ? nextItem.getArtist()
                                                    : station.getName()
                                    )
                            );
                        } else {
                            nextTrackText.setVisibility(View.GONE);
                        }
                    }

                    List<RadioProgramItem> previous =
                            PeachRadioCache.getPreviousProgramItems(
                                    station.getSlug(),
                                    3
                            );
                    List<RadioProgramItem> upcoming =
                            PeachRadioCache.getUpcomingProgramItems(
                                    station.getSlug(),
                                    3
                            );

                    bindProgramSummary(
                            historyText,
                            R.string.peach_radio_history_format,
                            previous
                    );
                    bindProgramSummary(
                            upcomingText,
                            R.string.peach_radio_upcoming_format,
                            upcoming
                    );
                } else {
                    trackInfoContainer.setVisibility(View.GONE);
                    if (liveBadge != null) {
                        liveBadge.setVisibility(View.GONE);
                    }
                }
            }

            View.OnClickListener clickListener = v -> {
                Log.i(TAG, "Step 1: PeachRadioAdapter receives click for station = " + station.getSlug());
                if (listener != null) {
                    listener.onPeachRadioClick(station);
                }
            };

            itemView.setOnClickListener(clickListener);
            if (radioCard != null) {
                radioCard.setOnClickListener(clickListener);
            }
        }

        private void bindProgramSummary(
                TextView view,
                int formatRes,
                List<RadioProgramItem> program
        ) {
            if (view == null) return;

            if (program == null || program.isEmpty()) {
                view.setVisibility(View.GONE);
                return;
            }

            StringBuilder titles = new StringBuilder();

            for (RadioProgramItem item : program) {
                if (item.getTitle() == null
                        || item.getTitle().trim().isEmpty()) {
                    continue;
                }

                if (titles.length() > 0) {
                    titles.append(" · ");
                }

                titles.append(item.getTitle());
            }

            if (titles.length() == 0) {
                view.setVisibility(View.GONE);
                return;
            }

            view.setText(
                    itemView.getContext().getString(
                            formatRes,
                            titles.toString()
                    )
            );
            view.setVisibility(View.VISIBLE);
        }

    }
}
