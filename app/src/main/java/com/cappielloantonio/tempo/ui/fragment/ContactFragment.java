package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.viewmodel.ContactViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

@UnstableApi
public class ContactFragment extends Fragment {

    private ContactViewModel viewModel;

    private RadioGroup categoryGroup;
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText subjectEditText;
    private TextInputEditText messageEditText;
    private MaterialSwitch diagnosticsSwitch;
    private MaterialButton sendButton;
    private MaterialButton retryButton;
    private View progressBar;
    private View formContainer;
    private MaterialCardView successCard;
    private TextView successMessageText;
    private MaterialCardView errorCard;
    private TextView errorText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        categoryGroup = view.findViewById(R.id.contact_category_group);
        nameEditText = view.findViewById(R.id.contact_name_edit_text);
        emailEditText = view.findViewById(R.id.contact_email_edit_text);
        subjectEditText = view.findViewById(R.id.contact_subject_edit_text);
        messageEditText = view.findViewById(R.id.contact_message_edit_text);
        diagnosticsSwitch = view.findViewById(R.id.contact_diagnostics_switch);
        sendButton = view.findViewById(R.id.contact_send_button);
        retryButton = view.findViewById(R.id.contact_retry_button);
        progressBar = view.findViewById(R.id.contact_progress_bar);
        formContainer = view.findViewById(R.id.contact_form_container);
        successCard = view.findViewById(R.id.contact_success_card);
        successMessageText = view.findViewById(R.id.contact_success_message);
        MaterialButton newMessageButton = view.findViewById(R.id.contact_new_message_button);
        errorCard = view.findViewById(R.id.contact_error_card);
        errorText = view.findViewById(R.id.contact_error_text);

        restoreDraft();
        setupDraftListeners();
        setupObservers();

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> submitForm());
        }

        if (retryButton != null) {
            retryButton.setOnClickListener(v -> submitForm());
        }

        if (newMessageButton != null) {
            newMessageButton.setOnClickListener(v -> resetForm());
        }
    }

    private String getSelectedCategory() {
        int checkedId = categoryGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.category_suggestion) return "suggestion";
        if (checkedId == R.id.category_question) return "question";
        if (checkedId == R.id.category_other) return "other";
        return "bug";
    }

    private void setSelectedCategory(String category) {
        if ("suggestion".equals(category)) categoryGroup.check(R.id.category_suggestion);
        else if ("question".equals(category)) categoryGroup.check(R.id.category_question);
        else if ("other".equals(category)) categoryGroup.check(R.id.category_other);
        else categoryGroup.check(R.id.category_bug);
    }

    private void restoreDraft() {
        String draftCategory = Preferences.getContactDraftCategory();
        String draftName = Preferences.getContactDraftName();
        String draftEmail = Preferences.getContactDraftEmail();
        String draftSubject = Preferences.getContactDraftSubject();
        String draftMessage = Preferences.getContactDraftMessage();
        boolean draftIncludeDiag = Preferences.getContactDraftIncludeDiag();

        setSelectedCategory(draftCategory);

        if (draftName != null && !draftName.isEmpty()) {
            nameEditText.setText(draftName);
        } else if (Preferences.getUser() != null) {
            nameEditText.setText(Preferences.getUser());
        }

        if (draftEmail != null) emailEditText.setText(draftEmail);
        if (draftSubject != null) subjectEditText.setText(draftSubject);
        if (draftMessage != null) messageEditText.setText(draftMessage);
        diagnosticsSwitch.setChecked(draftIncludeDiag);
    }

    private void setupDraftListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { saveDraft(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        nameEditText.addTextChangedListener(watcher);
        emailEditText.addTextChangedListener(watcher);
        subjectEditText.addTextChangedListener(watcher);
        messageEditText.addTextChangedListener(watcher);

        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> saveDraft());
        diagnosticsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveDraft());
    }

    private void saveDraft() {
        if (getContext() == null) return;
        viewModel.saveDraft(
                getSelectedCategory(),
                nameEditText.getText() != null ? nameEditText.getText().toString() : "",
                emailEditText.getText() != null ? emailEditText.getText().toString() : "",
                subjectEditText.getText() != null ? subjectEditText.getText().toString() : "",
                messageEditText.getText() != null ? messageEditText.getText().toString() : "",
                diagnosticsSwitch.isChecked()
        );
    }

    private void submitForm() {
        if (errorCard != null) errorCard.setVisibility(View.GONE);
        if (retryButton != null) retryButton.setVisibility(View.GONE);

        viewModel.sendContactMessage(
                getSelectedCategory(),
                nameEditText.getText() != null ? nameEditText.getText().toString() : "",
                emailEditText.getText() != null ? emailEditText.getText().toString() : "",
                subjectEditText.getText() != null ? subjectEditText.getText().toString() : "",
                messageEditText.getText() != null ? messageEditText.getText().toString() : "",
                diagnosticsSwitch.isChecked()
        );
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (sendButton != null) sendButton.setEnabled(!loading);
            if (retryButton != null) retryButton.setEnabled(!loading);
        });

        viewModel.getValidationError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                if (errorCard != null && errorText != null) {
                    errorText.setText(err);
                    errorCard.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                if (errorCard != null && errorText != null) {
                    errorText.setText(err);
                    errorCard.setVisibility(View.VISIBLE);
                }
                if (retryButton != null) retryButton.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSuccessResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (formContainer != null) formContainer.setVisibility(View.GONE);
                if (successCard != null) successCard.setVisibility(View.VISIBLE);

                String msg = (response.getMessage() != null ? response.getMessage() : "Ton message a bien été envoyé.");
                if (response.getContact() != null) {
                    msg += " Référence : #" + response.getContact().getId();
                }
                if (successMessageText != null) successMessageText.setText(msg);
            }
        });
    }

    private void resetForm() {
        Preferences.clearContactDraft();
        if (nameEditText != null) nameEditText.setText(Preferences.getUser() != null ? Preferences.getUser() : "");
        if (emailEditText != null) emailEditText.setText("");
        if (subjectEditText != null) subjectEditText.setText("");
        if (messageEditText != null) messageEditText.setText("");
        if (categoryGroup != null) categoryGroup.check(R.id.category_bug);
        if (diagnosticsSwitch != null) diagnosticsSwitch.setChecked(true);

        if (successCard != null) successCard.setVisibility(View.GONE);
        if (errorCard != null) errorCard.setVisibility(View.GONE);
        if (retryButton != null) retryButton.setVisibility(View.GONE);
        if (formContainer != null) formContainer.setVisibility(View.VISIBLE);
    }
}
