package com.cappielloantonio.tempo.ui.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.ItemMusicRequestBinding;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MusicRequestAdapter extends RecyclerView.Adapter<MusicRequestAdapter.ViewHolder> {

    private List<MusicRequestData> items = new ArrayList<>();
    private final Context context;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MusicRequestAdapter(Context context) {
        this.context = context;
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void setItems(List<MusicRequestData> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMusicRequestBinding bind = ItemMusicRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(bind);
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
        private final ItemMusicRequestBinding bind;

        ViewHolder(ItemMusicRequestBinding bind) {
            super(bind.getRoot());
            this.bind = bind;
        }

        void bind(MusicRequestData data) {
            String title = data.getTitle();
            bind.musicTitle.setText(title != null && !title.isEmpty() ? title : "Titre en cours d'identification");
            
            if (data.getArtist() != null && !data.getArtist().isEmpty()) {
                bind.musicArtist.setText(data.getArtist());
                bind.musicArtist.setVisibility(View.VISIBLE);
            } else {
                bind.musicArtist.setVisibility(View.GONE);
            }

            String platform = data.getSourcePlatform();
            if (platform == null) platform = "INCONNUE";
            bind.platformLabel.setText(platform.toUpperCase());

            try {
                if (data.getCreatedAt() != null) {
                    Date date = inputFormat.parse(data.getCreatedAt());
                    if (date != null) bind.requestDate.setText(outputFormat.format(date));
                }
            } catch (Exception e) {
                bind.requestDate.setText("");
            }
            
            String status = data.getStatus();
            if (status == null) status = "unknown";

            int statusColor;
            switch (status) {
                case "queued":
                    bind.statusLabel.setText("EN ATTENTE");
                    bind.statusIcon.setImageResource(R.drawable.ic_history);
                    statusColor = ContextCompat.getColor(context, R.color.warning_orange);
                    bind.extraInfo.setText(data.getQueuePosition() != null ? "Position : " + data.getQueuePosition() : "");
                    bind.progressIndicator.setVisibility(View.GONE);
                    break;
                case "processing":
                    bind.statusLabel.setText("TÉLÉCHARGEMENT EN COURS");
                    bind.statusIcon.setImageResource(R.drawable.ic_transcode);
                    statusColor = ContextCompat.getColor(context, R.color.accent_cyan);
                    
                    String info = data.getCurrentTitle() != null ? data.getCurrentTitle() : (data.getProgressStage() != null ? data.getProgressStage() : "");
                    if (data.getTotalItems() != null && data.getTotalItems() > 0) {
                        int current = data.getCompletedItems() != null ? data.getCompletedItems() : 0;
                        info = current + " / " + data.getTotalItems() + " - " + info;
                        bind.progressIndicator.setIndeterminate(false);
                        bind.progressIndicator.setProgress((int) (((float) current / data.getTotalItems()) * 100));
                        bind.progressIndicator.setVisibility(View.VISIBLE);
                    } else {
                        bind.progressIndicator.setIndeterminate(true);
                        bind.progressIndicator.setVisibility(View.VISIBLE);
                    }
                    bind.extraInfo.setText(info);
                    break;
                case "completed":
                    bind.statusLabel.setText("FAIT");
                    bind.statusIcon.setImageResource(R.drawable.ic_check_circle);
                    statusColor = ContextCompat.getColor(context, R.color.success_green);
                    bind.extraInfo.setText("");
                    bind.progressIndicator.setVisibility(View.GONE);
                    break;
                case "failed":
                    bind.statusLabel.setText("ÉCHEC");
                    bind.statusIcon.setImageResource(R.drawable.ic_error);
                    statusColor = ContextCompat.getColor(context, R.color.status_red);
                    bind.extraInfo.setText(data.getErrorMessage() != null ? data.getErrorMessage() : "Erreur inconnue");
                    bind.progressIndicator.setVisibility(View.GONE);
                    break;
                default:
                    bind.statusLabel.setText("STATUT INCONNU");
                    bind.statusIcon.setImageResource(R.drawable.ic_info_stream);
                    statusColor = ContextCompat.getColor(context, R.color.md_theme_dark_surfaceVariant);
                    bind.extraInfo.setText("");
                    bind.progressIndicator.setVisibility(View.GONE);
                    break;
            }
            bind.statusIndicatorBar.setBackgroundColor(statusColor);
            bind.statusIcon.setColorFilter(statusColor, PorterDuff.Mode.SRC_IN);
            bind.statusLabel.setTextColor(statusColor);
        }
    }
}