package com.example.lvglpy_android_example;

import android.opengl.GLSurfaceView;
import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GLSurfaceView.Renderer — GL thread only.
 *
 * Correct lifecycle order:
 *
 *   onSurfaceCreated
 *     → get Python module ref only — LVGL not ready yet
 *
 *   onSurfaceChanged(w, h)
 *     → nativeInit(w, h)           C++: LVGL init + GL objects ready
 *     → pyModule.on_init()         Python: safe to call lv.* now
 *     → pyModule.on_resize(w, h)   Python: align widgets to real size
 *
 *   onDrawFrame (~60fps)
 *     → pyModule.on_frame()        Python: update state
 *     → nativeDrawFrame()          C++: drain queues → lv_timer_handler
 *                                       → flush_cb → GL upload → draw
 */
public class LvglPyRenderer implements GLSurfaceView.Renderer {

    // registered by NativeBridge.registerNatives() in MainActivity
    // signatures match g_renderer_methods[] in android_input_sw.cpp
    private native void nativeInit(int width, int height);
    private native void nativeDrawFrame();
    private native void nativeResize(int width, int height);
    private native void nativeDestroy();

    private PyObject pyModule  = null;
    private boolean  lvglReady = false;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // GL thread.
        // Only grab module reference — do NOT call on_init() here.
        // LVGL is not ready until nativeInit() runs in onSurfaceChanged.
        pyModule  = Python.getInstance().getModule("main");
        lvglReady = false;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // GL thread.

        // 1. C++ — LVGL init + GL objects
        //    After this returns lv.screen_active() etc are safe to call
        nativeInit(width, height);
        lvglReady = true;

        // 2. Python — build UI widgets
        //    LVGL is fully ready here
        pyModule.callAttr("on_init");

        // 3. Python — align widgets to actual pixel dimensions
        pyModule.callAttr("on_resize", width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // GL thread — ~60fps
        if (!lvglReady) return;

        // 1. Python updates widget state via lv.* calls
        //    Safe — same GL thread as lv_timer_handler
        pyModule.callAttr("on_frame");

        // 2. C++:
        //    drain_text_queue()  → inject buffered text into textarea
        //    lv_timer_handler()  → fires callbacks, calls flush_cb
        //    glTexSubImage2D()   → upload g_fb to GPU
        //    glDrawArrays()      → draw fullscreen quad
        nativeDrawFrame();
    }

    public void destroy() {
        if (lvglReady) {
            nativeDestroy();
            lvglReady = false;
        }
    }
}