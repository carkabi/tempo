package com.cappielloantonio.tempo.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import com.cappielloantonio.tempo.App;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentLoginBinding;
import com.cappielloantonio.tempo.interfaces.SystemCallback;
import com.cappielloantonio.tempo.repository.SystemRepository;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.util.Preferences;

@UnstableApi
public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";

    private FragmentLoginBinding bind;
    private MainActivity activity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = FragmentLoginBinding.inflate(inflater, container, false);
        View view = bind.getRoot();

        initServerListView();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void initServerListView() {
        bind.loginButton.setOnClickListener(v -> attemptLogin());
        bind.registerLink.setOnClickListener(v -> openUrl("https://tropikeau.fr/register"));
        bind.forgotPasswordLink.setOnClickListener(v -> openUrl("https://tropikeau.fr/contact"));
        activity.setSupportActionBar(bind.toolbar);
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void attemptLogin() {
        if (bind.usernameEditText.getText() == null || bind.passwordEditText.getText() == null) return;
        String username = bind.usernameEditText.getText().toString().trim();
        String password = bind.passwordEditText.getText().toString();

        if (username.isEmpty()) {
            bind.usernameInputLayout.setError(getString(R.string.error_required));
            return;
        }

        if (password.isEmpty()) {
            bind.passwordInputLayout.setError(getString(R.string.error_required));
            return;
        }

        bind.usernameInputLayout.setError(null);
        bind.passwordInputLayout.setError(null);
        bind.loginButton.setEnabled(false);
        bind.loginProgressBar.setVisibility(View.VISIBLE);

        saveServerPreference("tropikeau_fixed", com.cappielloantonio.tempo.util.Constants.NAVIDROME_SERVER_URL, null, username, password, false);

        SystemRepository systemRepository = new SystemRepository();
        systemRepository.checkUserCredential(new SystemCallback() {
            @Override
            public void onError(Exception exception) {
                if (isAdded()) {
                    bind.loginButton.setEnabled(true);
                    bind.loginProgressBar.setVisibility(View.GONE);
                    String errorMsg = exception.getMessage() != null ? exception.getMessage() : "Erreur inconnue";
                    Toast.makeText(requireContext(), "Échec de connexion : " + errorMsg, Toast.LENGTH_LONG).show();
                    resetServerPreference();
                }
            }

            @Override
            public void onSuccess(String password, String token, String salt) {
                if (isAdded()) {
                    activity.goFromLogin();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return false;
    }

    private void saveServerPreference(String serverId, String server, String localAddress, String user, String password, boolean isLowSecurity) {
        Preferences.setInUseServerAddress(null);
        Preferences.setServerId(serverId);
        Preferences.setServer(server);
        Preferences.setLocalAddress(localAddress);
        Preferences.setUser(user);
        Preferences.setPassword(password);
        Preferences.setLowSecurity(isLowSecurity);

        App.getSubsonicClientInstance(true);
    }

    private void resetServerPreference() {
        Preferences.setInUseServerAddress(null);
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
