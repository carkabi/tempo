package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentHomeBinding;

@UnstableApi
public class HomeFragment extends Fragment {

    private static final String TAG = "PEACH_NAV";

    private FragmentHomeBinding bind;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "HomeFragment.onCreateView()");
        bind = FragmentHomeBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "HomeFragment.onViewCreated(), childFragments = " + getChildFragmentManager().getFragments());

        if (getChildFragmentManager().findFragmentById(R.id.home_container) == null) {
            showMusicTab();
        }
    }

    public void showMusicTab() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.home_container, new HomeTabMusicFragment(), "home_music")
                .commit();
    }

    public void showRadioTab() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.home_container, new HomeTabRadioFragment(), "home_radio")
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "HomeFragment.onResume(), childFragments = " + getChildFragmentManager().getFragments());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "HomeFragment.onPause()");
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "HomeFragment.onDestroyView()");
        super.onDestroyView();
        bind = null;
    }
}
