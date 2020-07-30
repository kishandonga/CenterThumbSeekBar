package ui.widget.seekbar;

public class Const {
    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;
    public static final float DEFAULT_MIN_VALUE = -100f;
    public static final float DEFAULT_MAX_VALUE = +100f;
    public static final float DEFAULT_PROGRESS_VALUE = 0f;
    public static int DEFAULT_THUMB_COLOR;
    public static int DEFAULT_TRACK_PROGRESS_COLOR;
    public static int DEFAULT_TRACK_COLOR;
    public static float DEFAULT_WIDTH = Utils.dpToPx(28);
    public static float DEFAULT_TRACK_HEIGHT = Utils.dpToPx(2);
    public static float DEFAULT_THUMB_RADIUS = Utils.dpToPx(6);
    public static float DEFAULT_THUMB_PRESSED_RADIUS = Utils.dpToPx(7);
}
