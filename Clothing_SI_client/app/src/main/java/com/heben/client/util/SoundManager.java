package com.heben.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

import com.heben.client.R;

/**
 * 音效管理器 — 统一播放 UI 操作音效
 * 受 ProgramSettings 中 "音效开关" 控制 (SharedPreferences key: sound_enabled)
 */
public class SoundManager {

    private static SoundManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    public static final int CLICK          = R.raw.click;
    public static final int CONFIRM_POP    = R.raw.confirm_pop;
    public static final int SALE_SUCCESS   = R.raw.sale_success;
    public static final int PURCHASE_SUCCESS = R.raw.purchase_success;
    public static final int REFUND_SUCCESS = R.raw.refund_success;
    public static final int DELETE_SUCCESS = R.raw.delete_success;
    public static final int GENERAL_SUCCESS = R.raw.general_success;
    public static final int ERROR          = R.raw.error;
    public static final int CANCEL         = R.raw.cancel;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    /** 音效是否开启 */
    public boolean isEnabled() {
        return prefs.getBoolean("sound_enabled", true);
    }

    /** 播放音效（受开关控制） */
    public void play(int rawResId) {
        if (!isEnabled()) return;
        try {
            MediaPlayer mp = MediaPlayer.create(context, rawResId);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        } catch (Exception ignored) {
            // 音效文件损坏或不存在时静默忽略
        }
    }

    /** 强制播放音效（忽略开关） */
    public void playForce(int rawResId) {
        try {
            MediaPlayer mp = MediaPlayer.create(context, rawResId);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        } catch (Exception ignored) {}
    }

    // ===== 便捷方法 =====

    public void playClick()       { play(CLICK); }
    public void playConfirmPop()  { play(CONFIRM_POP); }
    public void playSaleSuccess() { play(SALE_SUCCESS); }
    public void playPurchaseSuccess() { play(PURCHASE_SUCCESS); }
    public void playRefundSuccess() { play(REFUND_SUCCESS); }
    public void playDeleteSuccess() { play(DELETE_SUCCESS); }
    public void playSuccess()     { play(GENERAL_SUCCESS); }
    public void playError()       { play(ERROR); }
    public void playCancel()      { play(CANCEL); }
}
