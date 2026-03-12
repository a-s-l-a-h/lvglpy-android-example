package com.example.lvglpy_android_example;

import android.opengl.GLSurfaceView;
import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GLSurfaceView.Renderer — GL thread only.
 */
public class LvglPyRenderer implements GLSurfaceView.Renderer {

    // JNI bindings
    private native void nativeSurfaceCreated();
    private native void nativeInit(int width, int height);
    private native void nativeDrawFrame();
    private native void nativeResize(int width, int height);
    private native void nativeDestroy();

    private PyObject pyModule    = null;
    private boolean  lvglReady   = false;
    private boolean  pythonReady = false; // FIX: Prevents widget duplication on resume

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (pyModule == null) {
            pyModule = Python.getInstance().getModule("main");
        }
        // FIX: Force C++ to recreate GL Shaders/Textures for the new or restored EGL context
        nativeSurfaceCreated();
        lvglReady = false;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // C++ side handles both Initializing and Resizing safely now
        nativeInit(width, height);
        lvglReady = true;

        // Build UI widgets ONLY ONCE
        if (!pythonReady) {
            pyModule.callAttr("on_init");
            pythonReady = true;
        }

        // Always align widgets to actual pixel dimensions
        pyModule.callAttr("on_resize", width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!lvglReady) return;

        pyModule.callAttr("on_frame");
        nativeDrawFrame();
    }

    public void destroy() {
        if (lvglReady) {
            nativeDestroy();
            lvglReady = false;
        }
    }
}