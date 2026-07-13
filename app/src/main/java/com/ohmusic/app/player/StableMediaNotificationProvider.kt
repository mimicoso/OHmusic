package com.ohmusic.app.player

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.ohmusic.app.R

/**
 * 向厂商 MediaStyle 模板稳定提供五枚等距、可用的播放动作。
 *
 * vivo 新系统会直接按动作数量排版：只有三枚动作时，卡片会明显区别于
 * 原生 i音乐。两侧动作使用 OHMusic 已有能力，不伪装歌词等未实现功能。
 */
@OptIn(UnstableApi::class)
class StableMediaNotificationProvider(
    context: Context,
    private val onToggleFavorite: (Long) -> Unit
) : MediaNotification.Provider {

    private val appContext = context.applicationContext

    private val delegate = object : DefaultMediaNotificationProvider(appContext) {
        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean
        ): ImmutableList<CommandButton> = ImmutableList.of(
            sessionButton(
                icon = CommandButton.ICON_REPEAT_ONE,
                action = ACTION_RESTART,
                label = appContext.getString(R.string.action_restart),
                slot = CommandButton.SLOT_BACK_SECONDARY
            ),
            mediaButton(
                icon = CommandButton.ICON_PREVIOUS,
                playerCommand = Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                label = appContext.getString(R.string.action_previous),
                slot = CommandButton.SLOT_BACK
            ),
            mediaButton(
                icon = if (showPauseButton) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY,
                playerCommand = Player.COMMAND_PLAY_PAUSE,
                label = appContext.getString(
                    if (showPauseButton) R.string.action_pause else R.string.action_play
                ),
                slot = CommandButton.SLOT_CENTRAL
            ),
            mediaButton(
                icon = CommandButton.ICON_NEXT,
                playerCommand = Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                label = appContext.getString(R.string.action_next),
                slot = CommandButton.SLOT_FORWARD
            ),
            sessionButton(
                icon = CommandButton.ICON_HEART_UNFILLED,
                action = ACTION_TOGGLE_FAVORITE,
                label = appContext.getString(R.string.action_favorite),
                slot = CommandButton.SLOT_FORWARD_SECONDARY
            )
        )
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification = delegate.createNotification(
        mediaSession,
        customLayout,
        actionFactory,
        onNotificationChangedCallback
    )

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.notificationChannelInfo

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = when (action) {
        ACTION_RESTART -> {
            session.player.seekTo(0L)
            true
        }

        ACTION_TOGGLE_FAVORITE -> {
            session.player.currentMediaItem?.mediaId?.toLongOrNull()?.let(onToggleFavorite)
            true
        }

        else -> false
    }

    private fun mediaButton(
        icon: Int,
        playerCommand: Int,
        label: String,
        slot: Int
    ): CommandButton = CommandButton.Builder(icon)
        .setPlayerCommand(playerCommand)
        .setDisplayName(label)
        .setEnabled(true)
        .setSlots(slot)
        .build()

    private fun sessionButton(
        icon: Int,
        action: String,
        label: String,
        slot: Int
    ): CommandButton = CommandButton.Builder(icon)
        .setSessionCommand(SessionCommand(action, Bundle.EMPTY))
        .setDisplayName(label)
        .setEnabled(true)
        .setSlots(slot)
        .build()

    private companion object {
        const val ACTION_RESTART = "com.ohmusic.app.notification.RESTART"
        const val ACTION_TOGGLE_FAVORITE = "com.ohmusic.app.notification.TOGGLE_FAVORITE"
    }
}
