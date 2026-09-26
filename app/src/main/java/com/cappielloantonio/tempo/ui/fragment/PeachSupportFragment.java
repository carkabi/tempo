package com.cappielloantonio.tempo.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentPeachSupportBinding;
import com.cappielloantonio.tempo.repository.peach.PeachSupportBillingManager;

import java.util.List;

public class PeachSupportFragment extends Fragment
        implements PeachSupportBillingManager.Callback {

    private FragmentPeachSupportBinding binding;
    private PeachSupportBillingManager billingManager;

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
        billingManager = new PeachSupportBillingManager(
                requireContext(),
                this
        );

        binding.toolbar.setNavigationOnClickListener(
                v -> requireActivity()
                        .getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.supportSmallButton.setEnabled(false);
        binding.supportMediumButton.setEnabled(false);
        binding.supportLargeButton.setEnabled(false);

        binding.supportSmallButton.setOnClickListener(
                v -> billingManager.purchase(
                        requireActivity(),
                        PeachSupportBillingManager.SUPPORT_1
                )
        );
        binding.supportMediumButton.setOnClickListener(
                v -> billingManager.purchase(
                        requireActivity(),
                        PeachSupportBillingManager.SUPPORT_3
                )
        );
        binding.supportLargeButton.setOnClickListener(
                v -> billingManager.purchase(
                        requireActivity(),
                        PeachSupportBillingManager.SUPPORT_5
                )
        );

        binding.manageSupportButton.setOnClickListener(
                v -> openSubscriptionCenter()
        );

        return binding.getRoot();
    }
    @Override
    public void onStart() {
        super.onStart();
        billingManager.start();
    }

    @Override
    public void onDestroyView() {
        if (billingManager != null) {
            billingManager.close();
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onProducts(
            List<PeachSupportBillingManager.SupportProduct> products
    ) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;

            boolean any = false;

            for (PeachSupportBillingManager.SupportProduct product
                    : products) {
                String label = getString(
                        R.string.peach_support_price_format,
                        product.getPrice()
                );

                switch (product.getProductId()) {
                    case PeachSupportBillingManager.SUPPORT_1:
                        binding.supportSmallButton.setText(
                                getString(
                                        R.string.peach_support_small,
                                        label
                                )
                        );
                        binding.supportSmallButton.setEnabled(true);
                        any = true;
                        break;
                    case PeachSupportBillingManager.SUPPORT_3:
                        binding.supportMediumButton.setText(
                                getString(
                                        R.string.peach_support_medium,
                                        label
                                )
                        );
                        binding.supportMediumButton.setEnabled(true);
                        any = true;
                        break;
                    case PeachSupportBillingManager.SUPPORT_5:
                        binding.supportLargeButton.setText(
                                getString(
                                        R.string.peach_support_large,
                                        label
                                )
                        );
                        binding.supportLargeButton.setEnabled(true);
                        any = true;
                        break;
                }
            }

            binding.supportStatusMessage.setText(
                    any
                            ? R.string.peach_support_ready
                            : R.string.peach_support_not_configured
            );
        });
    }

    @Override
    public void onSupportState(
            boolean active,
            String productId
    ) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;
            if (active) {
                binding.supportState.setText(
                        R.string.peach_support_active
                );
                binding.manageSupportButton.setVisibility(
                        View.VISIBLE
                );
            } else {
                binding.supportState.setText(
                        R.string.peach_support_free
                );
                binding.manageSupportButton.setVisibility(
                        View.GONE
                );
            }
        });
    }

    @Override
    public void onMessage(String message) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() ->
                Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void openSubscriptionCenter() {
        Uri uri = Uri.parse(
                "https://play.google.com/store/account/subscriptions"
                        + "?package=fr.tropikeau.peach"
        );

        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }
}
