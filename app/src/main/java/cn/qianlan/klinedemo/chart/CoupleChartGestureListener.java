package cn.qianlan.klinedemo.chart;

import android.graphics.Matrix;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

/**
 * 图表联动交互监听
 */
public class CoupleChartGestureListener implements OnChartGestureListener {

    private BarLineChartBase srcChart;
    private Chart[] dstCharts;

    private OnEdgeListener edgeListener;//滑动到边缘的监听器
    private boolean isLoadMore, isHighlight;//是否加载更多、是否高亮  -- 高亮时不再加载更多
    private boolean canLoad;//K线图手指交互已停止，正在惯性滑动

    public CoupleChartGestureListener(BarLineChartBase srcChart, Chart... dstCharts) {
        this.srcChart = srcChart;
        this.dstCharts = dstCharts;
        isLoadMore = false;
    }

    public CoupleChartGestureListener(OnEdgeListener edgeListener, BarLineChartBase srcChart,
                                      Chart... dstCharts) {
        this.edgeListener = edgeListener;
        this.srcChart = srcChart;
        this.dstCharts = dstCharts;
        isLoadMore = true;
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        canLoad = false;
        syncCharts();
        chartGestureStart(me, lastPerformedGesture);
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        if (isHighlight) {
            isHighlight = false;
        } else {
            if (isLoadMore) {
                float leftX = srcChart.getLowestVisibleX();
                float rightX = srcChart.getHighestVisibleX();
                if (leftX == srcChart.getXChartMin()) {//滑到最左端
                    canLoad = false;
                    if (edgeListener != null) {
                        edgeListener.edgeLoad(leftX, true);
                    }
                } else if (rightX == srcChart.getXChartMax()) {//滑到最右端
                    canLoad = false;
                    if (edgeListener != null) {
                        edgeListener.edgeLoad(rightX, false);
                    }
                } else {
                    canLoad = true;
                }
            }
        }
        syncCharts();
        chartGestureEnd(me, lastPerformedGesture);
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        syncCharts();
        chartLongPressed(me);
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        syncCharts();
        chartDoubleTapped(me);
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        syncCharts();
        chartSingleTapped(me);
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        syncCharts();
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        syncCharts();
    }

    /**
     * 由于在外部设置了禁止惯性甩动(因为和Chart的move方法有冲突)，
     * if中的语句实际上不会执行(整个手势交互结束后，最后回调的方法是onChartGestureEnd，而不是onChartTranslate)，
     * 这样写是为了统一允许惯性甩动的情况
     */
    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        if (canLoad) {
            float leftX = srcChart.getLowestVisibleX();
            float rightX = srcChart.getHighestVisibleX();
            if (leftX == srcChart.getXChartMin()) {//滑到最左端
                canLoad = false;
                if (edgeListener != null) {
                    edgeListener.edgeLoad(leftX, true);
                }
            } else if (rightX == srcChart.getXChartMax()) {//滑到最右端
                canLoad = false;
                if (edgeListener != null) {
                    edgeListener.edgeLoad(rightX, false);
                }
            }
        }
        syncCharts();
        chartTranslate(me, dX, dY);
    }

    //以下5个方法仅为了：方便在外部根据需要自行重写
    public void chartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
    public void chartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
    public void chartLongPressed(MotionEvent me) {}
    public void chartDoubleTapped(MotionEvent me) {}
    public void chartSingleTapped(MotionEvent me) {}
    public void chartTranslate(MotionEvent me, float dX, float dY) {}

    private void syncCharts() {
        Matrix srcMatrix;
        float[] srcVals = new float[9];
        Matrix dstMatrix;
        float[] dstVals = new float[9];
        // get src chart translation matrix:
        srcMatrix = srcChart.getViewPortHandler().getMatrixTouch();
        srcMatrix.getValues(srcVals);
        // apply X axis scaling and position to dst charts:
        for (Chart dstChart : dstCharts) {
            dstMatrix = dstChart.getViewPortHandler().getMatrixTouch();
            dstMatrix.getValues(dstVals);

            dstVals[Matrix.MSCALE_X] = srcVals[Matrix.MSCALE_X];
            dstVals[Matrix.MSKEW_X] = srcVals[Matrix.MSKEW_X];
            dstVals[Matrix.MTRANS_X] = srcVals[Matrix.MTRANS_X];
            dstVals[Matrix.MSKEW_Y] = srcVals[Matrix.MSKEW_Y];
            dstVals[Matrix.MSCALE_Y] = srcVals[Matrix.MSCALE_Y];
            dstVals[Matrix.MTRANS_Y] = srcVals[Matrix.MTRANS_Y];
            dstVals[Matrix.MPERSP_0] = srcVals[Matrix.MPERSP_0];
            dstVals[Matrix.MPERSP_1] = srcVals[Matrix.MPERSP_1];
            dstVals[Matrix.MPERSP_2] = srcVals[Matrix.MPERSP_2];

            dstMatrix.setValues(dstVals);
            dstChart.getViewPortHandler().refresh(dstMatrix, dstChart, true);
        }
    }

    public void setHighlight(boolean highlight) {
        isHighlight = highlight;
    }

    public interface OnEdgeListener {
        void edgeLoad(float x, boolean left);
    }
}