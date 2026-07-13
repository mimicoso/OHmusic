package com.ohmusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.ohmusic.app.ui.components.SimpleBottomTabs
import com.ohmusic.app.ui.player.PlayerScreen
import com.ohmusic.app.ui.playlist.PlaylistScreen
import com.ohmusic.app.ui.theme.OHMusicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val hasAudioPermission = MutableStateFlow(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAudioPermission.value = checkAudioPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        hasAudioPermission.value = checkAudioPermission()

        setContent {
            OHMusicTheme {
                val backdrop = rememberLayerBackdrop()
                val saveableStateHolder = rememberSaveableStateHolder()

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var visibleTab by rememberSaveable { mutableIntStateOf(selectedTab) }
                val composedTabs = remember { mutableStateListOf(selectedTab) }
                val canReadAudio by hasAudioPermission.collectAsState()

                // Keep a screen composed after its first visit. A newly requested screen is
                // rendered behind the current one for two frames so its artwork and backdrop
                // have a valid first frame before it becomes visible.
                LaunchedEffect(selectedTab) {
                    if (selectedTab !in composedTabs) {
                        composedTabs += selectedTab
                        withFrameNanos { }
                        withFrameNanos { }
                    }
                    visibleTab = selectedTab
                }

                // 先提交首帧，再请求运行时权限，避免权限窗口阻塞冷启动绘制。
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    requestAudioPermission()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                    ) {
                        composedTabs.forEach { tab ->
                            val isVisible = visibleTab == tab
                            val isPrewarming = selectedTab == tab && visibleTab != selectedTab
                            val semanticsModifier = if (isVisible) {
                                Modifier
                            } else {
                                Modifier.clearAndSetSemantics { }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = if (isVisible || isPrewarming) 1f else 0f
                                    }
                                    .zIndex(if (isVisible) 1f else 0f)
                                    .then(semanticsModifier)
                            ) {
                                saveableStateHolder.SaveableStateProvider(tab) {
                                    when (tab) {
                                        0 -> PlayerScreen(modifier = Modifier.fillMaxSize())
                                        else -> PlaylistScreen(
                                            hasAudioPermission = canReadAudio,
                                            onRequestPermission = ::requestAudioPermission,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 底部导航栏
                    SimpleBottomTabs(
                        backdrop = backdrop,
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        tabLabels = listOf(
                            stringResource(R.string.tab_player),
                            stringResource(R.string.tab_library)
                        ),
                        tabContent = { index, isSelected ->
                            val icon = when (index) {
                                0 -> Icons.Default.MusicNote
                                1 -> Icons.AutoMirrored.Filled.QueueMusic
                                else -> Icons.Default.MusicNote
                            }
                            val size = if (index == 1 && isSelected) 30.dp else 26.dp
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(size)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.5f)
                            .padding(bottom = 32.dp)
                            .zIndex(2f)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasAudioPermission.value = checkAudioPermission()
    }

    private fun requestAudioPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionLauncher.launch(permissions)
        }
    }

    private fun checkAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
