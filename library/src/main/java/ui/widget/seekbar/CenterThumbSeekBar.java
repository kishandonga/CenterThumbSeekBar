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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.core.content.ContextCompat;

@SuppressLint("ClickableViewAccessibility")
public class CenterThumbSeekBar extends View {

    private final int trackProgressColor;
    private final int trackColor;
    private final int thumbColor;
    private final int thumbPressColor;
    private final boolean hasRoundedCorners;
    private final float fromValue;
    private final float toValue;
    private final double absoluteMinValue;
    private final double absoluteMaxValue;
    private final Paint paint;
    private float padding;
    private Bitmap thumbImage = null;
    private Bitmap thumbPressedImage = null;
    private float thumbRadius;
    private float thumbPressedRadius;
    private float trackHeight;
    private int scaledTouchSlop;
    private boolean isDragging;
    private boolean isThumbPressed;
    private double normalizedThumbValue = 0d;
    private OnFromValueChangeListener fromListener;
    private OnToValueChangeListener toListener;
    private float mDownMotionX;
    private int mActivePointerId = Const.INVALID_POINTER_ID;
    private ThumbDirection thumbDirection = ThumbDirection.NONE;
    private float newHeight;

    public CenterThumbSeekBar(Context context) {
        this(context, null);
    }

    public CenterThumbSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenterThumbSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.trackHeight = Const.DEFAULT_TRACK_HEIGHT;
        this.thumbRadius = Const.DEFAULT_THUMB_RADIUS;
        this.thumbPressedRadius = Const.DEFAULT_THUMB_PRESSED_RADIUS;

        Const.DEFAULT_THUMB_COLOR = ContextCompat.getColor(context, R.color.colorAccent);
        Const.DEFAULT_TRACK_PROGRESS_COLOR = ContextCompat.getColor(context, R.color.colorAccent);
        Const.DEFAULT_TRACK_COLOR = ContextCompat.getColor(context, R.color.default_track_color);

        this.absoluteMinValue = Const.DEFAULT_MIN_VALUE;
        this.absoluteMaxValue = Const.DEFAULT_MAX_VALUE;
        setProgress(Const.DEFAULT_PROGRESS_VALUE);

        // Attribute initialization
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CenterThumbSeekBar, defStyleAttr, 0);

        this.fromValue = a.getFloat(R.styleable.CenterThumbSeekBar_fromValue, Const.DEFAULT_MIN_VALUE);
        this.toValue = a.getFloat(R.styleable.CenterThumbSeekBar_toValue, Const.DEFAULT_MAX_VALUE);

        float fromProgress = a.getFloat(R.styleable.CenterThumbSeekBar_fromProgress, Const.DEFAULT_PROGRESS_VALUE);
        if ((fromProgress != Const.DEFAULT_PROGRESS_VALUE) && (fromProgress <= fromValue)) {
            setProgress(getFromProgress(fromProgress));
        }

        float toProgress = a.getFloat(R.styleable.CenterThumbSeekBar_toProgress, Const.DEFAULT_PROGRESS_VALUE);
        if ((toProgress != Const.DEFAULT_PROGRESS_VALUE) && (toProgress <= toValue)) {
            setProgress(getToProgress(toProgress));
        }

        this.thumbColor = a.getColor(R.styleable.CenterThumbSeekBar_thumbColor, Const.DEFAULT_THUMB_COLOR);
        this.thumbPressColor = a.getColor(R.styleable.CenterThumbSeekBar_thumbPressedColor, Const.DEFAULT_THUMB_COLOR);

        this.trackProgressColor = a.getColor(R.styleable.CenterThumbSeekBar_trackProgressColor, Const.DEFAULT_TRACK_PROGRESS_COLOR);
        this.trackColor = a.getColor(R.styleable.CenterThumbSeekBar_trackColor, Const.DEFAULT_TRACK_COLOR);

        this.hasRoundedCorners = a.getBoolean(R.styleable.CenterThumbSeekBar_trackRoundedCorners, false);
        this.trackHeight = a.getDimension(R.styleable.CenterThumbSeekBar_trackHeight, trackHeight);
        this.thumbRadius = a.getDimension(R.styleable.CenterThumbSeekBar_thumbRadius, thumbRadius);
        this.thumbPressedRadius = a.getDimension(R.styleable.CenterThumbSeekBar_thumbPressedRadius, thumbPressedRadius);

        newHeight = Math.max(thumbRadius * 2, thumbPressedRadius * 2);

        Drawable thumbImageDrawable = a.getDrawable(R.styleable.CenterThumbSeekBar_thumbDrawable);
        Drawable thumbImagePressedDrawable = a.getDrawable(R.styleable.CenterThumbSeekBar_thumbPressedDrawable);

        if (thumbImageDrawable != null && thumbImagePressedDrawable != null) {
            this.thumbImage = Utils.getBitmapFromDrawable(thumbImageDrawable);
            this.thumbPressedImage = Utils.getBitmapFromDrawable(thumbImagePressedDrawable);
            //thumb drawable
            thumbRadius = Math.max(thumbImage.getWidth(), thumbImage.getHeight());
            thumbPressedRadius = Math.max(thumbPressedImage.getWidth(), thumbPressedImage.getHeight());
            newHeight = Math.max(thumbRadius, thumbPressedRadius);
        }

        a.recycle();

        padding = newHeight;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        setForeground(new ColorDrawable(Color.TRANSPARENT));
        scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private float getFromProgress(float fromProgress) {
        return (fromProgress * Const.DEFAULT_MIN_VALUE) / fromValue;
    }

    private float getToProgress(float toProgress) {
        return (toProgress * Const.DEFAULT_MAX_VALUE) / toValue;
    }

    public void setOnFromValueChangeListener(OnFromValueChangeListener listener) {
        this.fromListener = listener;
    }

    public void setOnToValueChangeListener(OnToValueChangeListener listener) {
        this.toListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) (Const.DEFAULT_WIDTH);
        int height = (int) (newHeight + (padding / 4f));
        setMeasuredDimension(Utils.measureDim(width, widthMeasureSpec), Utils.measureDim(height, heightMeasureSpec));
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

                    notifyValueChange();
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
                notifyValueChange();
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

    private void notifyValueChange() {
        if (thumbDirection == ThumbDirection.LEFT) {
            if (fromListener != null) {
                fromListener.onValueChange(getFromProgressValue(normalizedValue(normalizedThumbValue)));
            }
        }
        if (thumbDirection == ThumbDirection.RIGHT) {
            if (toListener != null) {
                toListener.onValueChange(getToProgressValue(normalizedValue(normalizedThumbValue)));
            }
        }
    }

    private double getFromProgressValue(double progress) {
        return Math.abs((fromValue * progress) / Const.DEFAULT_MIN_VALUE);
    }

    private double getToProgressValue(double progress) {
        return Math.abs((toValue * progress) / Const.DEFAULT_MAX_VALUE);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & Const.ACTION_POINTER_INDEX_MASK) >> Const.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose
            // a new active pointer and adjust accordingly.
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
     * @param screenCoordinate The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoordinate) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            double result = (screenCoordinate - padding) / (width - 2 * padding);
            return Math.min(1d, Math.max(0d, result));
        }
    }

    private double normalizedValue(double normalized) {
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

    private void setProgress(double value) {
        normalizedThumbValue = valueToNormalized(value);
        invalidate();
    }

    /**
     * This is called when the user has started touching this widget.
     */
    private void onStartTrackingTouch() {
        isDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    private void onStopTrackingTouch() {
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
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbRadius;
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

        paint.setColor(trackColor);
        if (hasRoundedCorners) {
            canvas.drawRoundRect(rect, trackHeight, trackHeight, paint);
        } else {
            canvas.drawRect(rect, paint);
        }

        // draw seek bar active range line
        if (normalizedToScreen(valueToNormalized(0.0d)) < normalizedToScreen(normalizedThumbValue)) {
            thumbDirection = ThumbDirection.RIGHT;
            rect.left = normalizedToScreen(valueToNormalized(0.0d));
            rect.right = normalizedToScreen(normalizedThumbValue);
        } else {
            thumbDirection = ThumbDirection.LEFT;
            rect.right = normalizedToScreen(valueToNormalized(0.0d));
            rect.left = normalizedToScreen(normalizedThumbValue);
        }

        paint.setColor(trackProgressColor);
        if (hasRoundedCorners) {
            canvas.drawRoundRect(rect, trackHeight, trackHeight, paint);
        } else {
            canvas.drawRect(rect, paint);
        }

        drawThumb(normalizedToScreen(normalizedThumbValue), isThumbPressed, canvas);
    }

    /**
     * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
     *
     * @param screenCoordinate The x-coordinate in screen space where to draw the image.
     * @param pressed          Is the thumb currently in "pressed" state?
     * @param canvas           The canvas to draw upon.
     */
    private void drawThumb(float screenCoordinate, boolean pressed, Canvas canvas) {
        if (thumbImage != null && thumbPressedImage != null) {
            if (pressed) {
                canvas.drawBitmap(thumbPressedImage, screenCoordinate - (thumbPressedRadius / 2f), (0.5f * getHeight()) - (thumbPressedRadius / 2f), paint);
            } else {
                canvas.drawBitmap(thumbImage, screenCoordinate - (thumbRadius / 2f), (0.5f * getHeight()) - (thumbRadius / 2f), paint);
            }
        } else {
            if (pressed) {
                paint.setColor(thumbPressColor);
                canvas.drawCircle(screenCoordinate, (0.5f * getHeight()), thumbPressedRadius, paint);
            } else {
                paint.setColor(thumbColor);
                canvas.drawCircle(screenCoordinate, (0.5f * getHeight()), thumbRadius, paint);
            }
        }
    }

    /**
     * Callback listener interface to notify about changed from values.
     */
    public interface OnFromValueChangeListener {
        void onValueChange(double value);
    }

    /**
     * Callback listener interface to notify about changed to values.
     */
    public interface OnToValueChangeListener {
        void onValueChange(double value);
    }
}