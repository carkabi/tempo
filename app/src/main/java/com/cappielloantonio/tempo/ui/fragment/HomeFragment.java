package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentHomeBinding;

@UnstableApi
public class HomeFragment extends Fragment {

    private static final String TAG = "PEACH_NAV";
    private static final String TAG_MUSIC = "home_music";
    private static final String TAG_RADIO = "home_radio";

    private FragmentHomeBinding bind;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        bind = FragmentHomeBinding.inflate(
                inflater,
                container,
                false
        );

        return bind.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        if (
                getChildFragmentManager()
                        .findFragmentById(
                                R.id.home_container
                        ) == null
        ) {
            showMusicTab();
        }
    }

    public void showMusicTab() {
        showTab(
                TAG_MUSIC,
                HomeTabMusicFragment::new
        );
    }

    public void showRadioTab() {
        showTab(
                TAG_RADIO,
                HomeTabRadioFragment::new
        );
    }

    private void showTab(
            String tag,
            FragmentFactory factory
    ) {
        Fragment current =
                getChildFragmentManager()
                        .findFragmentById(
                                R.id.home_container
                        );

        Fragment target =
                getChildFragmentManager()
                        .findFragmentByTag(tag);

        if (target == null) {
            target = factory.create();
        }

        if (current == target) {
            return;
        }

        FragmentTransaction transaction =
                getChildFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true);

        if (current != null) {
            transaction.hide(current);
            transaction.setMaxLifecycle(
                    current,
                    Lifecycle.State.STARTED
            );
        }

        if (!target.isAdded()) {
            transaction.add(
                    R.id.home_container,
                    target,
                    tag
            );
        } else {
            transaction.show(target);
        }

        transaction.setMaxLifecycle(
                target,
                Lifecycle.State.RESUMED
        );

        transaction.commit();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "HomeFragment.onDestroyView()");
        super.onDestroyView();
        bind = null;
    }

    private interface FragmentFactory {
        Fragment create();
    }
}
