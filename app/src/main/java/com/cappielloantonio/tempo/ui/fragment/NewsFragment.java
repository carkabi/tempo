package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.databinding.FragmentNewsBinding;
import com.cappielloantonio.tempo.repository.peach.PeachRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachNews;
import com.cappielloantonio.tempo.ui.adapter.NewsAdapter;
import com.cappielloantonio.tempo.ui.dialog.PeachNewsDetailDialog;
import com.cappielloantonio.tempo.util.Preferences;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

@UnstableApi
public class NewsFragment extends Fragment implements NewsAdapter.OnNewsClickListener {

    private FragmentNewsBinding bind;
    private final PeachRepository repository = new PeachRepository();
    private NewsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentNewsBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRecyclerView();
        initRefresh();
        loadNews();

        bind.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void initRecyclerView() {
        adapter = new NewsAdapter(this);
        bind.newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.newsRecyclerView.setAdapter(adapter);
    }

    private void initRefresh() {
        bind.swipeRefresh.setOnRefreshListener(this::loadNews);
    }

    private void loadNews() {
        if (bind == null) return;
        bind.loadingProgressBar.setVisibility(View.VISIBLE);
        bind.emptyNewsLayout.setVisibility(View.GONE);

        repository.bootstrap(new PeachRepository.PeachCallback() {
            @Override
            public void onSuccess(PeachBootstrapResponse response) {
                if (bind == null) return;
                bind.loadingProgressBar.setVisibility(View.GONE);
                bind.swipeRefresh.setRefreshing(false);

                List<PeachNews> news = response.getNews();
                if (news != null && !news.isEmpty()) {
                    adapter.setItems(news);
                    bind.newsRecyclerView.setVisibility(View.VISIBLE);
                    bind.emptyNewsLayout.setVisibility(View.GONE);

                    int maxId = 0;
                    for (PeachNews item : news) {
                        if (item.getId() > maxId) {
                            maxId = item.getId();
                        }
                    }
                    Preferences.setLastSeenNewsId(maxId);
                } else {
                    bind.newsRecyclerView.setVisibility(View.GONE);
                    bind.emptyNewsLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(int code, String message) {
                if (bind == null) return;
                bind.loadingProgressBar.setVisibility(View.GONE);
                bind.swipeRefresh.setRefreshing(false);
                Snackbar.make(bind.getRoot(), message, Snackbar.LENGTH_LONG)
                        .setAction("Réessayer", v -> loadNews())
                        .show();
            }
        });
    }

    @Override
    public void onNewsClick(PeachNews news) {
        if (news != null) {
            new PeachNewsDetailDialog(news).show(getParentFragmentManager(), "PeachNewsDetailDialog");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }
}
