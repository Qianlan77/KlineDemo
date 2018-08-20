package cn.qianlan.klinedemo.chart;

import android.graphics.Canvas;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * 自定义Y轴标签渲染器，使其不出界
 * 只修改了第31行 使最后一个标签处于刻度下方 其余标签处于刻度上方
 */
public class InBoundYAxisRenderer extends YAxisRenderer {

    public InBoundYAxisRenderer(ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
        super(viewPortHandler, yAxis, trans);
    }

    @Override
    protected void drawYLabels(Canvas c, float fixedPosition, float[] positions, float offset) {
        final int from = mYAxis.isDrawBottomYLabelEntryEnabled() ? 0 : 1;
        final int to = mYAxis.isDrawTopYLabelEntryEnabled() ? mYAxis.mEntryCount : (mYAxis.mEntryCount - 1);

        // draw
        int labelHeight = Utils.calcTextHeight(mAxisLabelPaint, "A");
        for (int i = from; i < to; i++) {
            String text = mYAxis.getFormattedLabel(i);
            float os = i == mYAxis.mEntryCount - 1 ? -0.9F * labelHeight : 0.7F * labelHeight;
            c.drawText(text, fixedPosition, positions[i * 2 + 1] + offset - os, mAxisLabelPaint);
        }
    }
}
