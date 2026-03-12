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
 */
public class LvglPyGLView extends GLSurfaceView {

    private native void nativePushTouch(int action, float x, float y);
    private native void nativePushKey(int keyCode, int action);
    private native void nativePushText(int codepoint);

    public LvglPyGLView(Context context) {
        super(context);
        setEGLContextClientVersion(2);

        // FIX: Tell Android to keep the EGL context alive when backgrounded
        setPreserveEGLContextOnPause(true);

        setRenderer(new LvglPyRenderer());
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /* ── Touch ─────────────────────────────────────────────────── */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

            @Override
            public boolean commitText(CharSequence text, int newCursorPos) {
                for (int i = 0; i < text.length(); ) {
                    int cp = Character.codePointAt(text, i);
                    nativePushText(cp);
                    i += Character.charCount(cp);
                }
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int before, int after) {
                for (int i = 0; i < before; i++)
                    nativePushKey(67, 1);
                return true;
            }
        };
    }
}