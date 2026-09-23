package com.cappielloantonio.tempo.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.ActivityMainBinding;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.ui.activity.base.BaseActivity;
import com.cappielloantonio.tempo.ui.fragment.PlayerBottomSheetFragment;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.viewmodel.MainViewModel;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@UnstableApi
public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    public ActivityMainBinding bind;
    public NavController navController;

    private MainViewModel mainViewModel;
    private BottomNavigationView bottomNavigationView;

    private View playerBottomSheetView;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private final ActivityResultLauncher<Intent> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        Log.d(TAG, "Storage permission denied");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        initPlayerBottomSheet();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initService();
    }

    private void initViews() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        bottomNavigationView = bind.bottomNavigation;

        if (Preferences.getUser() == null || Preferences.getUser().isEmpty()) {
            goToLogin();
        } else {
            goToHome();
        }
    }

    private void initPlayerBottomSheet() {
        playerBottomSheetView = bind.playerBottomSheet;
        bottomSheetBehavior = BottomSheetBehavior.from(playerBottomSheetView);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.player_bottom_sheet, new PlayerBottomSheetFragment(), "PLAYER_TAG")
                .commit();

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    setBottomSheetVisibility(false);
                } else {
                    setBottomSheetVisibility(true);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.loginFragment || destId == R.id.landingFragment) {
                    setBottomSheetVisibility(false);
                    setBottomNavigationBarVisibility(false);
                    if (bottomSheetBehavior != null) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }
                } else if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && (
                        destId == R.id.homeFragment ||
                                destId == R.id.searchFragment ||
                                destId == R.id.playlistCatalogueFragment ||
                                destId == R.id.addMusicFragment ||
                                destId == R.id.settingsCategoryFragment)
                ) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        }

        if (bottomNavigationView != null && navController != null) {
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        }
    }

    public void setBottomNavigationBarVisibility(boolean visibility) {
        if (visibility) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    private void initService() {
        ListenableFuture<MediaBrowser> future = getMediaBrowserListenableFuture();
        if (future != null) {
            MediaManager.check(future);

            future.addListener(() -> {
                try {
                    MediaBrowser browser = future.get();
                    if (browser != null) {
                        browser.addListener(new Player.Listener() {
                            @Override
                            public void onIsPlayingChanged(boolean isPlaying) {
                                if (isPlaying && bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                                    setBottomSheetInPeek(true);
                                }
                            }
                        });
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private void goToLogin() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        setBottomNavigationBarVisibility(false);
        setBottomSheetVisibility(false);

        if (navController != null && Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.landingFragment) {
            navController.navigate(R.id.action_landingFragment_to_loginFragment);
        } else if (navController != null && Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.settingsFragment) {
            navController.navigate(R.id.action_settingsFragment_to_loginFragment);
        } else if (navController != null && Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.homeFragment) {
            navController.navigate(R.id.action_homeFragment_to_loginFragment);
        }
    }

    private void goToHome() {
        bottomNavigationView.setVisibility(View.VISIBLE);

        if (navController != null && Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.landingFragment) {
            navController.navigate(R.id.action_landingFragment_to_homeFragment);
        } else if (navController != null && Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.loginFragment) {
            navController.navigate(R.id.action_loginFragment_to_homeFragment);
        }
    }

    public void goFromLogin() {
        setBottomSheetInPeek(mainViewModel.isQueueLoaded());
        goToHome();
    }

    public void quit() {
        finishAndRemoveTask();
    }

    public void expandBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public void collapseBottomSheetDelayed() {
        new Handler().postDelayed(() -> {
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }, 300);
    }

    public void setBottomSheetDraggableState(boolean draggable) {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setDraggable(draggable);
        }
    }

    public void setBottomSheetVisibility(boolean visibility) {
        if (playerBottomSheetView != null) {
            if (visibility) {
                playerBottomSheetView.setVisibility(View.VISIBLE);
            } else {
                playerBottomSheetView.setVisibility(View.GONE);
            }
        }
    }

    public void setBottomSheetInPeek(boolean setInPeek) {
        if (bottomSheetBehavior == null) return;
        if (setInPeek) {
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    public NavController getNavController() {
        return navController;
    }
}
