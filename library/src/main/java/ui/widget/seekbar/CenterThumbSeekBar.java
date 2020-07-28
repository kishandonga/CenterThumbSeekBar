package ui.widget.seekbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.core.content.ContextCompat;

@SuppressLint("ClickableViewAccessibility")
public class CenterThumbSeekBar extends View {

    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int DEFAULT_RANGE_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);
    private static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#CECECE");
    private static final float DEFAULT_MIN_VALUE = -100f;
    private static final float DEFAULT_MAX_VALUE = +100f;
    private static final float DEFAULT_PROGRESS_VALUE = 0f;
    private final Bitmap thumbImage;
    private final Bitmap thumbPressedImage;
    private final int defaultRangeColor;
    private final int defaultBackgroundColor;
    private final boolean hasRoundedCorners;

    private final float thumbHalfWidth;
    private final float thumbHalfHeight;
    private final float trackHeight;
    private final float padding;
    private double absoluteMinValue;
    private double absoluteMaxValue;
    private int scaledTouchSlop;
    private boolean isDragging;
    private boolean isThumbPressed;
    private double normalizedThumbValue = 0d;
    private OnSeekBarChangeListener listener;
    private float mDownMotionX;
    private int mActivePointerId = INVALID_POINTER_ID;

    public CenterThumbSeekBar(Context context) {
        this(context, null);
    }

    public CenterThumbSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenterThumbSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Attribute initialization
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CenterThumbSeekBar, defStyleAttr, 0);

        Drawable thumbImageDrawable = a.getDrawable(R.styleable.CenterThumbSeekBar_thumbDrawable);
        if (thumbImageDrawable == null) {
            thumbImageDrawable = ContextCompat.getDrawable(context, R.drawable.seek_thumb_normal);
        }
        this.thumbImage = getBitmapFromDrawable(thumbImageDrawable);

        Drawable thumbImagePressedDrawable = a.getDrawable(R.styleable.CenterThumbSeekBar_thumbPressedDrawable);
        if (thumbImagePressedDrawable == null) {
            thumbImagePressedDrawable = ContextCompat.getDrawable(context, R.drawable.seek_thumb_pressed);
        }
        this.thumbPressedImage = getBitmapFromDrawable(thumbImagePressedDrawable);

        this.absoluteMinValue = a.getFloat(R.styleable.CenterThumbSeekBar_minValue, DEFAULT_MIN_VALUE);
        this.absoluteMaxValue = a.getFloat(R.styleable.CenterThumbSeekBar_maxValue, DEFAULT_MAX_VALUE);
        float progress = a.getFloat(R.styleable.CenterThumbSeekBar_progress, DEFAULT_PROGRESS_VALUE);
        setProgress(progress);

        this.defaultBackgroundColor = a.getColor(R.styleable.CenterThumbSeekBar_defaultBackgroundColor, DEFAULT_BACKGROUND_COLOR);
        this.defaultRangeColor = a.getColor(R.styleable.CenterThumbSeekBar_defaultBackgroundRangeColor, DEFAULT_RANGE_COLOR);

        this.hasRoundedCorners = a.getBoolean(R.styleable.CenterThumbSeekBar_trackRoundedCorners, false);

        a.recycle();

        float thumbWidth = thumbImage.getWidth();
        thumbHalfWidth = 0.5f * thumbWidth;
        thumbHalfHeight = 0.5f * thumbImage.getHeight();
        trackHeight = 6f;
        padding = thumbHalfWidth;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        setForeground(new ColorDrawable(Color.TRANSPARENT));
        scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public void setAbsoluteMinMaxValue(double absoluteMinValue, double absoluteMaxValue) {
        this.absoluteMinValue = absoluteMinValue;
        this.absoluteMaxValue = absoluteMaxValue;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = thumbImage.getHeight() + 12;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int pointerIndex;

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                isThumbPressed = evalPressedThumb(mDownMotionX);

                // Only handle thumb presses.
                if (!isThumbPressed) {
                    return super.onTouchEvent(event);
                }

                setPressed(true);
                invalidate();
                onStartTrackingTouch();
                trackTouchEvent(event);
                attemptClaimDrag();
                break;

            case MotionEvent.ACTION_MOVE:
                if (isThumbPressed) {

                    if (isDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);

                        if (Math.abs(x - mDownMotionX) > scaledTouchSlop) {
                            setPressed(true);
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }

                    if (listener != null) {
                        listener.onOnSeekBarValueChange(this, normalizedToValue(normalizedThumbValue));
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                isThumbPressed = false;
                invalidate();
                if (listener != null) {
                    listener.onOnSeekBarValueChange(this, normalizedToValue(normalizedThumbValue));
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose
            // a new active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        final float x = event.getX(pointerIndex);
        setNormalizedValue(screenToNormalized(x));
    }

    /**
     * Converts screen space x-coordinates into normalized values.
     *
     * @param screenCoord The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoord) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            double result = (screenCoord - padding) / (width - 2 * padding);
            return Math.min(1d, Math.max(0d, result));
        }
    }

    private double normalizedToValue(double normalized) {
        return absoluteMinValue + normalized * (absoluteMaxValue - absoluteMinValue);
    }

    private double valueToNormalized(double value) {
        if (0 == absoluteMaxValue - absoluteMinValue) {
            // prevent division by zero, simply return 0.
            return 0d;
        }
        return (value - absoluteMinValue) / (absoluteMaxValue - absoluteMinValue);
    }

    /**
     * Sets normalized max value to value so that 0 <= normalized min value <=
     * value <= 1. The View will get invalidated when calling this method.
     *
     * @param value The new normalized max value to set.
     */

    private void setNormalizedValue(double value) {
        normalizedThumbValue = Math.max(0d, value);
        invalidate();
    }

    /**
     * Sets value of seek bar to the given value
     *
     * @param value The new value to set
     */
    public void setProgress(double value) {
        double newThumbValue = valueToNormalized(value);
        if (newThumbValue > absoluteMaxValue || newThumbValue < absoluteMinValue) {
            throw new IllegalArgumentException("Value should be in the middle of max and min value");
        }
        normalizedThumbValue = newThumbValue;
        invalidate();
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        isDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch() {
        isDragging = false;
    }

    /**
     * Decides which (if any) thumb is touched by the given x-coordinate.
     *
     * @param touchX The x-coordinate of a touch event in screen space.
     * @return The pressed thumb or null if none has been touched.
     */
    private boolean evalPressedThumb(float touchX) {
        return isInThumbRange(touchX, normalizedThumbValue);
    }

    /**
     * Decides if given x-coordinate in screen space needs to be interpreted as
     * "within" the normalized thumb x-coordinate.
     *
     * @param touchX               The x-coordinate in screen space to check.
     * @param normalizedThumbValue The normalized x-coordinate of the thumb to check.
     * @return true if x-coordinate is in thumb range, false otherwise.
     */
    private boolean isInThumbRange(float touchX, double normalizedThumbValue) {
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth;
    }

    /**
     * Converts a normalized value into screen space.
     *
     * @param normalizedCoordinates The normalized value to convert.
     * @return The converted value in screen space.
     */
    private float normalizedToScreen(double normalizedCoordinates) {
        return (float) (padding + normalizedCoordinates * (getWidth() - 2 * padding));
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw seek bar background line
        final RectF rect = new RectF(padding, 0.5f * (getHeight() - trackHeight), getWidth() - padding, 0.5f * (getHeight() + trackHeight));

        paint.setColor(defaultBackgroundColor);
        if (hasRoundedCorners) {
            canvas.drawRoundRect(rect, trackHeight, trackHeight, paint);
        } else {
            canvas.drawRect(rect, paint);
        }

        // draw seek bar active range line

        if (normalizedToScreen(valueToNormalized(0.0d)) < normalizedToScreen(normalizedThumbValue)) {
            Log.d(VIEW_LOG_TAG, "thumb: right");
            rect.left = normalizedToScreen(valueToNormalized(0.0d));
            rect.right = normalizedToScreen(normalizedThumbValue);
        } else {
            Log.d(VIEW_LOG_TAG, "thumb: left");
            rect.right = normalizedToScreen(valueToNormalized(0.0d));
            rect.left = normalizedToScreen(normalizedThumbValue);
        }

        paint.setColor(defaultRangeColor);
        if (hasRoundedCorners) {
            canvas.drawRoundRect(rect, trackHeight, trackHeight, paint);
        } else {
            canvas.drawRect(rect, paint);
        }

        drawThumb(normalizedToScreen(normalizedThumbValue), isThumbPressed, canvas);
        Log.d(VIEW_LOG_TAG, "thumb: " + normalizedToValue(normalizedThumbValue));
    }

    /**
     * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
     *
     * @param screenCoordinate The x-coordinate in screen space where to draw the image.
     * @param pressed          Is the thumb currently in "pressed" state?
     * @param canvas           The canvas to draw upon.
     */
    private void drawThumb(float screenCoordinate, boolean pressed, Canvas canvas) {
        if (pressed) {
            canvas.drawCircle(screenCoordinate, (0.5f * getHeight()), thumbHalfHeight + 5, paint);
        } else {
            canvas.drawCircle(screenCoordinate, (0.5f * getHeight()), thumbHalfHeight, paint);
        }
        //canvas.drawBitmap(pressed ? thumbPressedImage : thumbImage, screenCoordinate - thumbHalfWidth, (0.5f * getHeight()) - thumbHalfHeight, paint);
    }

    /**
     * Callback listener interface to notify about changed range values.
     */
    public interface OnSeekBarChangeListener {
        void onOnSeekBarValueChange(CenterThumbSeekBar bar, double value);
    }
}