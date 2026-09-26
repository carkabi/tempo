package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentPeachSupportBinding;

public class PeachSupportFragment extends Fragment {

    private FragmentPeachSupportBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPeachSupportBinding.inflate(
                inflater,
                container,
                false
        );

        binding.toolbar.setNavigationOnClickListener(
                view -> requireActivity()
                        .getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        SpannableString title = new SpannableString(
                getString(R.string.peach_support_title)
        );
        title.setSpan(
                new StrikethroughSpan(),
                0,
                title.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        binding.toolbar.setTitle(title);

        binding.supportContent.setEnabled(false);
        binding.supportSmallButton.setEnabled(false);
        binding.supportMediumButton.setEnabled(false);
        binding.supportLargeButton.setEnabled(false);
        binding.manageSupportButton.setEnabled(false);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
