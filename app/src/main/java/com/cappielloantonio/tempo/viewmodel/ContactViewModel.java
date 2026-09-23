package com.cappielloantonio.tempo.viewmodel;

import android.os.Build;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.repository.peach.PeachRepository;
import com.cappielloantonio.tempo.repository.peach.models.ContactDiagnostics;
import com.cappielloantonio.tempo.repository.peach.models.ContactErrorResponse;
import com.cappielloantonio.tempo.repository.peach.models.ContactRequest;
import com.cappielloantonio.tempo.repository.peach.models.ContactResponse;
import com.cappielloantonio.tempo.util.Preferences;

public class ContactViewModel extends ViewModel {

    private final PeachRepository repository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ContactResponse> successResponse = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> validationError = new MutableLiveData<>();

    public ContactViewModel() {
        this.repository = new PeachRepository();
    }

    public ContactViewModel(PeachRepository repository) {
        this.repository = repository;
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<ContactResponse> getSuccessResponse() { return successResponse; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getValidationError() { return validationError; }

    public void sendContactMessage(String category, String name, String email, String subject, String message, boolean includeDiagnostics) {
        // App-side validation
        if (category == null || category.trim().isEmpty()) {
            validationError.setValue("Veuillez choisir un type de message.");
            return;
        }

        if (name == null || name.trim().isEmpty()) {
            validationError.setValue("Le nom est obligatoire.");
            return;
        }

        if (name.length() > 120) {
            validationError.setValue("Le nom ne doit pas dépasser 120 caractères.");
            return;
        }

        String trimmedEmail = (email != null) ? email.trim() : "";
        if (!trimmedEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            validationError.setValue("L'adresse email n'est pas valide.");
            return;
        }

        if (subject == null || subject.trim().isEmpty()) {
            validationError.setValue("Le sujet est obligatoire.");
            return;
        }

        if (subject.length() > 160) {
            validationError.setValue("Le sujet ne doit pas dépasser 160 caractères.");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            validationError.setValue("Le message est obligatoire.");
            return;
        }

        if (message.length() > 10000) {
            validationError.setValue("Le message ne doit pas dépasser 10 000 caractères.");
            return;
        }

        validationError.setValue(null);
        errorMessage.setValue(null);
        isLoading.setValue(true);

        ContactDiagnostics diagnostics = null;
        if (includeDiagnostics) {
            diagnostics = new ContactDiagnostics(
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.FLAVOR,
                    Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : String.valueOf(Build.VERSION.SDK_INT),
                    Build.MANUFACTURER + " " + Build.MODEL
            );
        }

        ContactRequest request = new ContactRequest(
                category,
                name.trim(),
                trimmedEmail.isEmpty() ? null : trimmedEmail,
                subject.trim(),
                message.trim(),
                diagnostics
        );

        repository.sendContactMessage(request, new PeachRepository.ContactCallback() {
            @Override
            public void onSuccess(ContactResponse response) {
                isLoading.postValue(false);
                Preferences.clearContactDraft();
                successResponse.postValue(response);
            }

            @Override
            public void onValidationError(String msg, ContactErrorResponse errorResponse) {
                isLoading.postValue(false);
                validationError.postValue(msg);
            }

            @Override
            public void onError(int code, String msg) {
                isLoading.postValue(false);
                errorMessage.postValue(msg);
            }
        });
    }

    public void saveDraft(String category, String name, String email, String subject, String message, boolean includeDiag) {
        Preferences.saveContactDraft(category, name, email, subject, message, includeDiag);
    }
}
