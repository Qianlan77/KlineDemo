package cn.qianlan.klinedemo.chart;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.renderer.DataRenderer;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

/**
 * 自定义BarChart渲染器 使Bar的颜色根据取值来实现 自定义高亮
 * 只修改 {@link #drawDataSet(Canvas, IBarDataSet, int)} 中设置多种颜色的情况
 * 使用方法: 1.先设置渲染器 {@link BarChart#setRenderer(DataRenderer)} 传入此渲染器
 *           2.再调用 {@link BarDataSet#setColors(int...)} 设置多种颜色;
 *           3.设置数据时 调用 {@link BarEntry#BarEntry(float, float, Object)} 传入Integer类型的data指明第几种颜色.
 */
public class OffsetBarRenderer extends BarChartRenderer {

    protected float barOffset;//BarChart绘制时偏移多少个单位 --小于0时向左偏移
    protected float highlightWidth, highlightSize;//高亮线宽度 单位:dp  /  高亮文字大小 单位:px
    private RectF mBarShadowRectBuffer = new RectF();
    private DecimalFormat format = new DecimalFormat("0.0000");//保留小数点后四位

    public OffsetBarRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        this(chart, animator, viewPortHandler, 0);
    }

    public OffsetBarRenderer(BarDataProvider chart, ChartAnimator animator,
                             ViewPortHandler viewPortHandler, float barOffsetCount) {
        super(chart, animator, viewPortHandler);
        barOffset = barOffsetCount;
    }

    @Override
    public void initBuffers() {
        BarData barData = mChart.getBarData();
        mBarBuffers = new OffsetBarBuffer[barData.getDataSetCount()];

        for (int i = 0; i < mBarBuffers.length; i++) {
            IBarDataSet set = barData.getDataSetByIndex(i);
            mBarBuffers[i] = new OffsetBarBuffer(set.getEntryCount() * 4 *
                    (set.isStacked() ? set.getStackSize() : 1), barData.getDataSetCount(),
                    set.isStacked(), barOffset);
        }
    }

    @Override
    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
        mBarBorderPaint.setColor(dataSet.getBarBorderColor());
        mBarBorderPaint.setStrokeWidth(Utils.convertDpToPixel(dataSet.getBarBorderWidth()));

        final boolean drawBorder = dataSet.getBarBorderWidth() > 0.f;
        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        // draw the bar shadow before the values
        if (mChart.isDrawBarShadowEnabled()) {
            mShadowPaint.setColor(dataSet.getBarShadowColor());

            BarData barData = mChart.getBarData();
            final float barWidth = barData.getBarWidth();
            final float barWidthHalf = barWidth / 2.0f;
            float x;

            for (int i = 0, count = Math.min((int)(Math.ceil((float)(dataSet.getEntryCount()) * phaseX)),
                    dataSet.getEntryCount()); i < count; i++) {

                BarEntry e = dataSet.getEntryForIndex(i);
                x = e.getX();
                mBarShadowRectBuffer.left = x - barWidthHalf;
                mBarShadowRectBuffer.right = x + barWidthHalf;
                trans.rectValueToPixel(mBarShadowRectBuffer);

                if (!mViewPortHandler.isInBoundsLeft(mBarShadowRectBuffer.right))
                    continue;
                if (!mViewPortHandler.isInBoundsRight(mBarShadowRectBuffer.left))
                    break;

                mBarShadowRectBuffer.top = mViewPortHandler.contentTop();
                mBarShadowRectBuffer.bottom = mViewPortHandler.contentBottom();
                c.drawRect(mBarShadowRectBuffer, mShadowPaint);
            }
        }

        // initialize the buffer
        BarBuffer buffer = mBarBuffers[index];
        buffer.setPhases(phaseX, phaseY);
        buffer.setDataSet(index);
        buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
        buffer.setBarWidth(mChart.getBarData().getBarWidth());

        buffer.feed(dataSet);
        trans.pointValuesToPixel(buffer.buffer);

        int size = dataSet.getColors().size();
        final boolean isSingleColor = size == 1;
        if (isSingleColor) {
            mRenderPaint.setColor(dataSet.getColor());
        }

        for (int j = 0; j < buffer.size(); j += 4) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2]))
                continue;
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j]))
                break;
            if (!isSingleColor) {
                // Set the color for the currently drawn value. If the index
                // is out of bounds, reuse colors.
                BarEntry entry = dataSet.getEntryForIndex(j / 4);
                Object data = entry.getData();
                if (data == null || !(data instanceof Integer)) {
                    mRenderPaint.setColor(dataSet.getColor(j / 4));
                } else {
                    int i = (int) data;
                    mRenderPaint.setColor(size > 1 ? dataSet.getColors().get(i % size) : Color.BLACK);
                }
            }

            c.drawRect(buffer.buffer[j], buffer.buffer[j + 1], buffer.buffer[j + 2],
                    buffer.buffer[j + 3], mRenderPaint);
            if (drawBorder) {
                c.drawRect(buffer.buffer[j], buffer.buffer[j + 1], buffer.buffer[j + 2],
                        buffer.buffer[j + 3], mBarBorderPaint);
            }
        }
    }

    public OffsetBarRenderer setHighlightWidthSize(float width, float textSize) {
        highlightWidth = Utils.convertDpToPixel(width);
        highlightSize = textSize;
        return this;
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {
        BarData barData = mChart.getBarData();
        for (Highlight high : indices) {
            IBarDataSet set = barData.getDataSetByIndex(high.getDataSetIndex());
            if (set == null || !set.isHighlightEnabled())
                continue;
            BarEntry e = set.getEntryForXValue(high.getX(), high.getY());
            if (!isInBoundsX(e, set))
                continue;

            mHighlightPaint.setColor(set.getHighLightColor());
            mHighlightPaint.setStrokeWidth(highlightWidth);
            mHighlightPaint.setTextSize(highlightSize);

            //画竖线
            float barWidth = barData.getBarWidth();
            Transformer trans = mChart.getTransformer(set.getAxisDependency());
            prepareBarHighlight(e.getX() + barOffset, 0, 0, barWidth / 2, trans);

            float xp = mBarRect.centerX();
            c.drawLine(xp, mViewPortHandler.getContentRect().bottom, xp, 0, mHighlightPaint);

            //判断是否画横线
            float y = high.getDrawY();
            float yMaxValue = mChart.getYChartMax();
            float yMin = getYPixelForValues(xp, yMaxValue);
            float yMax = getYPixelForValues(xp, 0);
            if (y >= 0 && y <= yMax) {//在区域内即绘制横线
                float xMax = mChart.getWidth();
                int halfPaddingVer = 5;//竖向半个padding
                int halfPaddingHor = 8;
                //先绘制文字框
                mHighlightPaint.setStyle(Paint.Style.STROKE);
                float yValue = (yMax - y) / (yMax - yMin) * yMaxValue;
                String text = format.format(yValue);
                int width = Utils.calcTextWidth(mHighlightPaint, text);
                int height = Utils.calcTextHeight(mHighlightPaint, text);
                float top = Math.max(0, y - height / 2F - halfPaddingVer);//考虑间隙
                float bottom = top + height + halfPaddingVer * 2;
                if (bottom > yMax) {
                    bottom = yMax;
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
    }

    protected float getYPixelForValues(float x, float y) {
        MPPointD pixels = mChart.getTransformer(YAxis.AxisDependency.LEFT).getPixelForValues(x, y);
        return (float) pixels.y;
    }
}
