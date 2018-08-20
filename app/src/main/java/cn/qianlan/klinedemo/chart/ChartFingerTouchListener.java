package cn.qianlan.klinedemo.chart;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.highlight.Highlight;

/**
 * 图表长按及滑动手指监听
 */
public class ChartFingerTouchListener implements View.OnTouchListener {

    private BarLineChartBase mChart;
    private GestureDetector mDetector;
    private HighlightListener mListener;
    private boolean mIsLongPress = false;

    public ChartFingerTouchListener(BarLineChartBase chart, HighlightListener listener) {
        mChart = chart;
        mListener = listener;
        mDetector = new GestureDetector(mChart.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                mIsLongPress = true;
                if (mListener != null) {
                    mListener.enableHighlight();
                }
                Highlight h = mChart.getHighlightByTouchPoint(e.getX(), e.getY());
                if (h != null) {
                    h.setDraw(e.getX(), e.getY());
                    mChart.highlightValue(h, true);
                    mChart.disableScroll();
                }
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mIsLongPress = false;
            mChart.highlightValue(null, true);
            if (mListener != null) {
                mListener.disableHighlight();
            }
            mChart.enableScroll();
        }
        if (mIsLongPress && event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mListener != null) {
                mListener.enableHighlight();
            }
            Highlight h = mChart.getHighlightByTouchPoint(event.getX(), event.getY());
            if (h != null) {
                h.setDraw(event.getX(), event.getY());
                mChart.highlightValue(h, true);
                mChart.disableScroll();
            }
            return true;
        }
        return false;
    }

    public interface HighlightListener {
        void enableHighlight();
        void disableHighlight();
    }
}
