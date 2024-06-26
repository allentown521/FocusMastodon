package allen.town.focus.twitter.listeners;

import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

public class MultipleImageTouchListener implements View.OnTouchListener {

    private int numberOfPictures;
    private int imageTouchPosition;

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            imageTouchPosition = calcImageTouchPosition(x, y, v.getWidth(), v.getHeight());
        }

        return false;
    }

    public MultipleImageTouchListener() {
        numberOfPictures = 0;
        imageTouchPosition = 0;
    }

    public MultipleImageTouchListener(String urls) {
        imageTouchPosition = 0;
        numberOfPictures = 1;

        if (!TextUtils.isEmpty(urls)) {
            numberOfPictures = urls.split(" ").length;
        }
    }


    public void setImageUrls(String urls) {
        numberOfPictures = 1;

        //和twitter不同url str最后多了一个" "
        if (!TextUtils.isEmpty(urls)) {
            numberOfPictures = urls.split(" ").length;
        }

    }

    public int getImageTouchPosition() {
        return imageTouchPosition;
    }

    private int calcImageTouchPosition(int x, int y, int width, int height) {

        int xThreshold = width / 2;
        int yThreshold = height / 2;
        int position;

        switch (numberOfPictures) {

            case 2:
                position = x <= xThreshold ? 0 : 1;
                break;

            case 3:
                position = x <= xThreshold ? 0
                        : y <= yThreshold ? 1
                        : 2;
                break;

            case 4:
                position = x <= xThreshold && y <= yThreshold ? 0
                        : y <= yThreshold ? 1
                        : x <= xThreshold ? 2
                        : 3;
                break;

            default:
                position = 0;
                break;
        }

        return position;
    }


}
