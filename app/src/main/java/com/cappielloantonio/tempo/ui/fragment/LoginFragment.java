package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.App;
import com.cappielloantonio.tempo.databinding.FragmentLoginBinding;
import com.cappielloantonio.tempo.interfaces.SystemCallback;
import com.cappielloantonio.tempo.repository.SystemRepository;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.util.Preferences;

import java.util.UUID;

@UnstableApi
public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";

    private static final String HARDCODED_SERVER_URL = "https://music.tropikeau.fr";
    private static final String HARDCODED_SERVER_NAME = "Peach";

    private FragmentLoginBinding bind;
    private MainActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        bind = FragmentLoginBinding.inflate(inflater, container, false);
        View view = bind.getRoot();

        initAppBar();
        initLoginForm();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void initAppBar() {
        activity.setSupportActionBar(bind.toolbar);
    }

    private void initLoginForm() {
        bind.loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = bind.usernameEditText.getText().toString().trim();
        String password = bind.passwordEditText.getText().toString().trim();
        boolean lowSecurity = false;

        if (TextUtils.isEmpty(username)) {
            bind.usernameInputLayout.setError(getString(R.string.error_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            bind.passwordInputLayout.setError(getString(R.string.error_required));
            return;
        }

        bind.usernameInputLayout.setError(null);
        bind.passwordInputLayout.setError(null);
        bind.loginProgressBar.setVisibility(View.VISIBLE);
        bind.loginButton.setEnabled(false);
        bind.errorTextView.setVisibility(View.GONE);

        String serverId = UUID.randomUUID().toString();
        String encodedPassword = lowSecurity ? MusicUtil.passwordHexEncoding(password) : password;

        saveServerPreference(serverId, HARDCODED_SERVER_URL, null, username, encodedPassword, lowSecurity);

        SystemRepository systemRepository = new SystemRepository();
        systemRepository.checkUserCredential(new SystemCallback() {
            @Override
            public void onError(Exception exception) {
                if (bind != null) {
                    bind.loginProgressBar.setVisibility(View.GONE);
                    bind.loginButton.setEnabled(true);
                    bind.errorTextView.setText(exception.getMessage());
                    bind.errorTextView.setVisibility(View.VISIBLE);
                    Preferences.setServerId(null);
                    Preferences.setServer(null);
                    Preferences.setUser(null);
                    Preferences.setPassword(null);
                    Preferences.setToken(null);
                    Preferences.setSalt(null);
                    Preferences.setLowSecurity(false);
                    App.getSubsonicClientInstance(true);
                }
            }

            @Override
            public void onSuccess(String returnedPassword, String token, String salt) {
                if (bind != null) {
                    bind.loginProgressBar.setVisibility(View.GONE);
                    bind.loginButton.setEnabled(true);
                }
                activity.goFromLogin();
            }
        });
    }

    private void saveServerPreference(String serverId, String server, String localAddress, String user, String password, boolean isLowSecurity) {
        Preferences.setServerId(serverId);
        Preferences.setServer(server);
        Preferences.setLocalAddress(localAddress);
        Preferences.setUser(user);
        Preferences.setPassword(password);
        Preferences.setLowSecurity(isLowSecurity);

        App.getSubsonicClientInstance(true);
    }
}
