package hu.rycus.watchface.commons;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.Time;

public class DateTimeUI {

    protected final Rect bounds = new Rect();

    protected final TimeField field;
    protected String format;

    protected int value = -1;
    protected String text;

    private boolean formatChanged = false;

    protected DateTimeUI(final TimeField field, final String format) {
        this.field = field;
        this.format = format;
    }

    public String text() {
        return text;
    }

    public int width() {
        return bounds.width();
    }

    public int height() {
        return bounds.height();
    }

    public void changeFormat(final String format) {
        this.formatChanged = this.format != format;
        this.format = format;
    }

    public void update(final Time time, final Paint paint) {
        if (hasChanged(time)) {
            onUpdate(time);
            measureText(paint);
        }
    }

    protected boolean hasChanged(final Time time) {
        if (formatChanged) {
            formatChanged = false;
            return true;
        } else {
            switch (field) {
                case DATE:
                    return time.yearDay != value;
                case HOUR:
                    return time.hour != value;
                case MINUTE:
                    return time.minute != value;
                case SECOND:
                    return time.second != value;
                default:
                    return false;
            }
        }
    }

    protected void onUpdate(final Time time) {
        switch (field) {
            case DATE:
                value = time.yearDay;
                break;
            case HOUR:
                value = time.hour;
                break;
            case MINUTE:
                value = time.minute;
                break;
            case SECOND:
                value = time.second;
                break;
            default:
                break;
        }

        text = time.format(format);
    }

    protected void measureText(final Paint paint) {
        paint.getTextBounds(text, 0, text.length(), bounds);
    }

    public static class Builder {

        private TimeField field;
        private String format;

        public Builder field(final TimeField field) {
            this.field = field;
            return this;
        }

        public Builder format(final String format) {
            this.format = format;
            return this;
        }

        public DateTimeUI build() {
            assert field != null;
            assert format != null;

            return new DateTimeUI(field, format);
        }

    }

}
