package lucns.oblivium.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class RandomPolygonsView extends View {

    private Paint paint;
    private Random random;

    public RandomPolygonsView(Context context) {
        super(context);
        random = new Random();
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    public RandomPolygonsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        random = new Random();
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    public RandomPolygonsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        random = new Random();
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        for (int i = 0; i < 10; i++) drawCircle(canvas);
        for (int i = 0; i < 10; i++) drawTriangle(canvas);
        for (int i = 0; i < 10; i++) drawSquare(canvas);
    }

    private int generateColor() {
        int r, g, b;
        int mainColor = random.nextInt(2);
        switch (mainColor) {
            case 0:
                r = 255;
                g = random.nextInt(255);
                b = random.nextInt(255);
                break;
            case 1:
                r = random.nextInt(255);
                g = 255;
                b = random.nextInt(255);
                break;
            default:
                r = random.nextInt(255);
                g = random.nextInt(255);
                b = 255;
                break;
        }
        return Color.argb(255, r, g, b);
    }

    private void drawCircle(Canvas canvas) {
        int size = generateSize();
        int x = random.nextInt(getWidth() - size) + (size / 2);
        int y = random.nextInt(getHeight() - size) + (size / 2);

        paint.setColor(generateColor());
        canvas.drawCircle(x, y, size / 2f, paint);
    }

    private void drawSquare(Canvas canvas) {
        int width = generateWidth();
        int height = generateHeight();
        int x = random.nextInt(getWidth() - width) + (width / 2);
        int y = random.nextInt(getHeight() - height) + (height / 2);

        paint.setColor(generateColor());
        canvas.drawRect(x, y, width, height, paint);
    }

    private void drawTriangle(Canvas canvas) {
        int width = generateWidth();
        int height = generateHeight();
        int x = random.nextInt(getWidth() - width) + (width / 2);
        int y = random.nextInt(getHeight() - height) + (height / 2);

        Point p1 = new Point(x, y + (height / 2));
        Point p2 = new Point(x + width, y);
        Point p3 = new Point(x + width, y + height);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.close();

        paint.setColor(generateColor());
        canvas.drawPath(path, paint);
    }

    private int generateSize() {
        int min = Math.min(getWidth(), getHeight());
        return random.nextInt(min / 4) + min / 10;
    }

    private int generateWidth() {
        return random.nextInt(getWidth() / 10) + getWidth() / 10;
    }

    private int generateHeight() {
        return random.nextInt(getHeight() / 10) + getWidth() / 10;
    }
}
