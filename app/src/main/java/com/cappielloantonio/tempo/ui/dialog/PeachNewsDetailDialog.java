package com.cappielloantonio.tempo.ui.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.repository.peach.models.PeachNews;
import com.cappielloantonio.tempo.util.PeachUpdateDownloader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

@UnstableApi
public class PeachNewsDetailDialog extends DialogFragment {
    private final PeachNews news;

    public PeachNewsDetailDialog(PeachNews news) {
        this.news = news;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_peach_news_detail, null);

        TextView titleView = view.findViewById(R.id.news_title);
        TextView contentView = view.findViewById(R.id.news_content);
        Button openUrlBtn = view.findViewById(R.id.news_open_url_button);
        Button closeBtn = view.findViewById(R.id.news_close_button);

        if (news != null) {
            if (titleView != null) titleView.setText(news.getTitle());

            String rawContent = (news.getContent() != null && !news.getContent().trim().isEmpty())
                    ? news.getContent()
                    : news.getSummary();

            if (contentView != null) {
                if (rawContent != null) {
                    contentView.setText(HtmlCompat.fromHtml(rawContent, HtmlCompat.FROM_HTML_MODE_COMPACT));
                } else {
                    contentView.setText(news.getTitle());
                }
            }

            if (openUrlBtn != null) {
                if (news.getUrl() != null && PeachUpdateDownloader.isValidTropikeauUrl(news.getUrl())) {
                    openUrlBtn.setVisibility(View.VISIBLE);
                    openUrlBtn.setOnClickListener(v -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getUrl()));
                            startActivity(intent);
                        } catch (Exception ignored) {}
                    });
                } else {
                    openUrlBtn.setVisibility(View.GONE);
                }
            }
        }

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dismiss());
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
                .setView(view);

        return builder.create();
    }
}
