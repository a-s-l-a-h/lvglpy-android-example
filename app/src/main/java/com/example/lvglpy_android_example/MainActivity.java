package com.example.lvglpy_android_example;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.mylibrary.NativeBridge;

public class MainActivity extends AppCompatActivity {

    private LvglPyGLView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Step 1 — start Python (same as working example)
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Step 2 — import main.py and call get_so_path()
        //          Python returns lvglpy.__file__ — the exact path
        //          Chaquopy extracted the .so to on this device
        String soPath = null;
        try {
            Python   py     = Python.getInstance();
            PyObject module = py.getModule("main");

            // same pattern as working example:
            //   PyObject pathObj = module.callAttr("get_so_path");
            //   soPath = pathObj.toString();
            PyObject pathObj = module.callAttr("get_so_path");
            soPath = pathObj.toString();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to get so path from Python get_so_path(): "
                            + e.getMessage(), e);
        }

        // Step 3 — System.load() so the JVM native linker sees JNI symbols
        //          UnsatisfiedLinkError = already loaded by Chaquopy — fine
        try {
            System.load(soPath);
        } catch (UnsatisfiedLinkError e) {
            // already loaded in same classloader — safe to continue
        }

        // Step 4 — register native methods
        //          Pass Class objects so C++ never needs a package string
        NativeBridge.registerNatives(
                LvglPyRenderer.class,
                LvglPyGLView.class
        );

        // Step 5 — GL view takes over
        glView = new LvglPyGLView(this);
        setContentView(glView);
    }

    @Override protected void onPause()   { super.onPause();   glView.onPause();  }
    @Override protected void onResume()  { super.onResume();  glView.onResume(); }
    @Override protected void onDestroy() { super.onDestroy(); glView = null;     }
}