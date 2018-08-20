package cn.qianlan.klinedemo.chart;

import android.graphics.Canvas;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * 自定义X轴标签渲染器，使其不出界
 * {@link #interval}  -- 左右两侧边缘处的标签 距离边缘的间隔
 * 修改了第55行 使第一个标签向右偏移一半、最后一个标签向左偏移一半
 */
public class InBoundXAxisRenderer extends XAxisRenderer {

    protected int interval;

    public InBoundXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
        this(viewPortHandler, xAxis, trans, 0);
    }

    public InBoundXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans,
                                int interval) {
        super(viewPortHandler, xAxis, trans);
        this.interval = interval;
    }

    @Override
    protected void drawLabels(Canvas c, float pos, MPPointF anchor) {
        final float labelRotationAngleDegrees = mXAxis.getLabelRotationAngle();
        boolean centeringEnabled = mXAxis.isCenterAxisLabelsEnabled();

        float[] positions = new float[mXAxis.mEntryCount * 2];
        for (int i = 0; i < positions.length; i += 2) {
            // only fill x values
            if (centeringEnabled) {
                positions[i] = mXAxis.mCenteredEntries[i / 2];
            } else {
                positions[i] = mXAxis.mEntries[i / 2];
            }
        }
        mTrans.pointValuesToPixel(positions);

        for (int i = 0; i < positions.length; i += 2) {
            float x = positions[i];
            if (mViewPortHandler.isInBoundsX(x)) {
                String label = mXAxis.getValueFormatter().getFormattedValue(mXAxis.mEntries[i / 2], mXAxis);
                if (mXAxis.isAvoidFirstLastClippingEnabled()) {
                    // avoid clipping of the last
                    float width = Utils.calcTextWidth(mAxisLabelPaint, label);
                    if (i == mXAxis.mEntryCount * 2 - 2 && mXAxis.mEntryCount > 1) {
                        x -= width / 2 + interval;
                        // avoid clipping of the first
                    } else if (i == 0) {
                        x += width / 2 + interval;
                    }
                }

                drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees);
            }
        }
    }
}
