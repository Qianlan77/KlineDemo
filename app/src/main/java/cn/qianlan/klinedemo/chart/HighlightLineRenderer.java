package cn.qianlan.klinedemo.chart;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.DataRenderer;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

/**
 * 自定义LineChart渲染器 绘制高亮  -- 绘制方式和自定义CandleStickChart渲染器相同
 * 使用方法: 1.先设置渲染器 {@link CombinedChart#setRenderer(DataRenderer)}
 *              传入自定义渲染器 将其中Line图的渲染器替换成此渲染器
 *           2.设置数据时 调用 {@link Entry#Entry(float, float, Object)}
 *              传入String类型的data 以绘制x的值  -- 如未设置 则只绘制竖线
 */
public class HighlightLineRenderer extends LineChartRenderer {

    private float highlightSize;//图表高亮文字大小 单位:px
    private DecimalFormat format = new DecimalFormat("0.0000");
    private Highlight[] indices;

    public HighlightLineRenderer(LineDataProvider chart, ChartAnimator animator,
                                 ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    public HighlightLineRenderer setHighlightSize(float textSize) {
        highlightSize = textSize;
        return this;
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {
        this.indices = indices;
    }

    protected float getYPixelForValues(float x, float y) {
        MPPointD pixels = mChart.getTransformer(YAxis.AxisDependency.LEFT).getPixelForValues(x, y);
        return (float) pixels.y;
    }

    @Override
    public void drawExtras(Canvas c) {
        if (indices == null) {
            return;
        }

        LineData lineData = mChart.getLineData();
        for (Highlight high : indices) {
            ILineDataSet set = lineData.getDataSetByIndex(high.getDataSetIndex());
            if (set == null || !set.isHighlightEnabled())
                continue;

            Entry e = set.getEntryForXValue(high.getX(), high.getY());
            if (!isInBoundsX(e, set))
                continue;

            MPPointD pix = mChart.getTransformer(set.getAxisDependency()).getPixelForValues(e.getX(),
                    e.getY() * mAnimator.getPhaseY());
            float xp = (float) pix.x;

            mHighlightPaint.setColor(set.getHighLightColor());
            mHighlightPaint.setStrokeWidth(set.getHighlightLineWidth());
            mHighlightPaint.setTextSize(highlightSize);

            float xMin = mViewPortHandler.contentLeft();
            float xMax = mViewPortHandler.contentRight();
            float contentBottom = mViewPortHandler.contentBottom();
            //画竖线
            int halfPaddingVer = 5;//竖向半个padding
            int halfPaddingHor = 8;
            float textXHeight = 0;

            String textX;//高亮点的X显示文字
            Object data = e.getData();
            if (data != null && data instanceof String) {
                textX = (String) data;
            } else {
                textX = e.getX() + "";
            }
            if (!TextUtils.isEmpty(textX)) {//绘制x的值
                //先绘制文字框
                mHighlightPaint.setStyle(Paint.Style.STROKE);
                int width = Utils.calcTextWidth(mHighlightPaint, textX);
                int height = Utils.calcTextHeight(mHighlightPaint, textX);
                float left = Math.max(xMin, xp - width / 2F - halfPaddingVer);//考虑间隙
                float right = left + width + halfPaddingHor * 2;
                if (right > xMax) {
                    right = xMax;
                    left = right - width - halfPaddingHor * 2;
                }
                textXHeight = height + halfPaddingVer * 2;
                RectF rect = new RectF(left, 0, right, textXHeight);
                c.drawRect(rect, mHighlightPaint);
                //再绘制文字
                mHighlightPaint.setStyle(Paint.Style.FILL);
                Paint.FontMetrics metrics = mHighlightPaint.getFontMetrics();
                float baseY = (height + halfPaddingVer * 2 - metrics.top - metrics.bottom) / 2;
                c.drawText(textX, left + halfPaddingHor, baseY, mHighlightPaint);
            }
            //绘制竖线
            c.drawLine(xp, textXHeight, xp, mChart.getHeight(), mHighlightPaint);

            //判断是否画横线
            float y = high.getDrawY();
            float yMaxValue = mChart.getYChartMax();
            float yMinValue = mChart.getYChartMin();
            float yMin = getYPixelForValues(xp, yMaxValue);
            float yMax = getYPixelForValues(xp, yMinValue);
            if (y >= 0 && y <= contentBottom) {//在区域内即绘制横线
                //先绘制文字框
                mHighlightPaint.setStyle(Paint.Style.STROKE);
                float yValue = (yMax - y) / (yMax - yMin) * (yMaxValue - yMinValue) + yMinValue;
                String text = format.format(yValue);
                int width = Utils.calcTextWidth(mHighlightPaint, text);
                int height = Utils.calcTextHeight(mHighlightPaint, text);
                float top = Math.max(0, y - height / 2F - halfPaddingVer);//考虑间隙
                float bottom = top + height + halfPaddingVer * 2;
                if (bottom > contentBottom) {
                    bottom = contentBottom;
                    top = bottom - height - halfPaddingVer * 2;
                }
                RectF rect = new RectF(xMax - width - halfPaddingHor * 2, top, xMax, bottom);
                c.drawRect(rect, mHighlightPaint);
                //再绘制文字
                mHighlightPaint.setStyle(Paint.Style.FILL);
                Paint.FontMetrics metrics = mHighlightPaint.getFontMetrics();
                float baseY = (top + bottom - metrics.top - metrics.bottom) / 2;
                c.drawText(text, xMax - width - halfPaddingHor, baseY, mHighlightPaint);
                //绘制横线
                c.drawLine(0, y, xMax - width - halfPaddingHor * 2, y, mHighlightPaint);
            }
        }
        indices = null;
    }
}
