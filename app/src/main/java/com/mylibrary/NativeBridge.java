package com.mylibrary;

/**
 * Thin JNI trampoline — the only exported JNI symbol in mylibrary.so.
 *
 * We pass the Class objects from Java so the C++ side never needs to call
 * FindClass with a hardcoded package string.  The .so is fully portable —
 * drop it into any app package without touching a single line of C++.
 *
 * Usage (MainActivity):
 *   NativeBridge.registerNatives(LvglPyRenderer.class, LvglPyGLView.class);
 */
public class NativeBridge {

    /**
     * @param rendererClass   pass LvglPyRenderer.class
     * @param glViewClass     pass LvglPyGLView.class
     */
    public static native void registerNatives(Class<?> rendererClass,
                                              Class<?> glViewClass);
}