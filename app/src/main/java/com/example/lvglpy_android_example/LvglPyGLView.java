package com.example.lvglpy_android_example;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * GLSurfaceView subclass — Main thread only.
 *
 * Input events are pushed into the C++ mutex-protected queues via JNI.
 * The .so is already loaded by Chaquopy + System.load() in MainActivity
 * before this view is created — no System.loadLibrary needed here.
 *
 * Thread model:
 *   onTouchEvent / onKeyDown / onKeyUp  →  Main thread
 *   nativePushTouch / nativePushKey     →  JNI → C++ queue (mutex)
 *   nativePushText                      →  JNI → C++ text queue (mutex)
 *   impl_draw_frame drains queues       →  GL thread
 */
public class LvglPyGLView extends GLSurfaceView {

    /*
     * JNI — input goes directly to the C++ queue.
     * Registered by NativeBridge.registerNatives() in MainActivity.
     * Signatures must match g_view_methods[] in android_input_sw.cpp:
     *   nativePushTouch  (IFF)V
     *   nativePushKey    (II)V
     *   nativePushText   (I)V
     */
    private native void nativePushTouch(int action, float x, float y);
    private native void nativePushKey(int keyCode, int action);
    private native void nativePushText(int codepoint);   /* commitText path */

    public LvglPyGLView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(new LvglPyRenderer());
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /* ── Touch ─────────────────────────────────────────────────── */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*
         * Extract coordinates HERE on the main thread before Android
         * recycles the MotionEvent object.
         * JNI push is ~1µs — never blocks the main thread.
         */
        nativePushTouch(
                event.getActionMasked(),   /* 0=DOWN 1=UP 2=MOVE */
                event.getX(),
                event.getY()
        );
        return true;
    }

    /* ── Hardware keys ─────────────────────────────────────────── */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        nativePushKey(keyCode, 1);   /* 1 = down */
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        nativePushKey(keyCode, 0);   /* 0 = up */
        return true;
    }

    /* ── Soft keyboard — InputConnection ───────────────────────── */
    @Override
    public boolean onCheckIsTextEditor() { return true; }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo info) {
        info.inputType = android.text.InputType.TYPE_CLASS_TEXT;
        return new BaseInputConnection(this, false) {

            /*
             * commitText fires for every key on the soft keyboard.
             * Send each Unicode codepoint to C++ individually so the
             * UTF-8 encoder in push_char() handles all scripts correctly.
             */
            @Override
            public boolean commitText(CharSequence text, int newCursorPos) {
                for (int i = 0; i < text.length(); ) {
                    int cp = Character.codePointAt(text, i);
                    nativePushText(cp);
                    i += Character.charCount(cp);
                }
                return true;
            }

            /*
             * deleteSurroundingText fires for backspace on the soft keyboard.
             * before = number of chars to delete before cursor.
             */
            @Override
            public boolean deleteSurroundingText(int before, int after) {
                /* map to KEYCODE_DEL (67) so C++ handles it uniformly */
                for (int i = 0; i < before; i++)
                    nativePushKey(67, 1);
                return true;
            }
        };
    }
}