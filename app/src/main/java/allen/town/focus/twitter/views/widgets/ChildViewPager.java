package allen.town.focus.twitter.views.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 作为子viewpager解决滑动嵌套问题，但是好像不需要
 */
public class ChildViewPager extends HackyViewPager{
    private int downX;
    private int downY;

    public ChildViewPager(Context context) {
        super(context);
    }

    public ChildViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                downX = (int) ev.getX();
                downY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) ev.getX();
                int moveY = (int) ev.getY();

                int diffX = downX - moveX;
                int diffY = downY - moveY;

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // 当前是横向滑动
                    if (getCurrentItem() == 0 && diffX < 0) {
                        // 当前页面等于第一个页面, 并且是从左向右滑动, 可以拦截
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else if (getCurrentItem() == (getAdapter().getCount() - 1)
                            && diffX > 0) {
                        // 当前页面等于最后一个, 并且是从右向左滑动, 可以拦截
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        // 自己处理
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                } else {
                    // 竖着滑动, 可以拦截
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
