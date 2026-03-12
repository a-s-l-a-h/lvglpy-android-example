"""
main.py — lvglpy Android bridge

Same pattern as working mylibrary example:
  get_so_path() returns lvglpy.__file__
  Java calls get_so_path() first → System.load(soPath) → RegisterNatives works
"""
import lvglpy as lv

# ── so path helper — Java calls this FIRST before anything else ──
def get_so_path():
    return lv.__file__      # Chaquopy sets __file__ = extracted .so abs path

# ── state ────────────────────────────────────────────────────────
_label       = None
_btn         = None
_click_count = [0]

# ── lifecycle — called from LvglPyRenderer on GL thread ──────────
def on_init():
    global _label, _btn
    print(f"on_init: backend={lv.backend_name()}", flush=True)

    scr = lv.screen_active()
    scr.set_style_bg_color(0x0D0D0D)
    scr.set_scrollbar_mode(lv.SCROLLBAR.OFF)

    _label = lv.label_create(scr)
    lv.label_set_text(_label, "Hello from Python + LVGL!")
    _label.set_style_text_color(0xFFFFFF)
    _label.align(lv.ALIGN.CENTER, 0, -50)

    _btn = lv.btn_create(scr)
    _btn.set_width(lv.pct(70))
    _btn.set_height(50)
    _btn.set_style_bg_color(0x6C63FF)
    _btn.set_style_radius(12)
    _btn.set_style_border_width(0)
    _btn.align(lv.ALIGN.BOTTOM_MID, 0, -80)

    btn_label = lv.label_create(_btn)
    lv.label_set_text(btn_label, "Click Me!")
    btn_label.set_style_text_color(0xFFFFFF)
    btn_label.center()

    _btn.add_event_cb(_on_click,   lv.EVENT.CLICKED)
    _btn.add_event_cb(_on_press,   lv.EVENT.PRESSED)
    _btn.add_event_cb(_on_release, lv.EVENT.RELEASED)

    print("on_init: UI ready", flush=True)

def on_resize(w, h):
    if _label is None:
        return
    print(f"on_resize: {w}x{h}", flush=True)
    _label.align(lv.ALIGN.CENTER, 0, -50)
    _btn.align(lv.ALIGN.BOTTOM_MID, 0, -80)

def on_frame():
    pass    # all updates handled reactively via callbacks

# ── callbacks ────────────────────────────────────────────────────
def _on_click():
    _click_count[0] += 1
    lv.label_set_text(_label, f"Clicked {_click_count[0]} times!")
    print(f"clicked: {_click_count[0]}", flush=True)

def _on_press():
    _btn.set_style_bg_color(0x4a43cc)
    print("pressed", flush=True)

def _on_release():
    _btn.set_style_bg_color(0x6C63FF)
    print("released", flush=True)