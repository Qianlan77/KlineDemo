package cn.qianlan.klinedemo.chart;

import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

/**
 * 自定义BarChart的Bar数据 绘制Bar时添加偏移
 */
public class OffsetBarBuffer extends BarBuffer {

    protected float barOffset;//BarChart绘制时偏移多少个单位 --小于0时向左偏移

    public OffsetBarBuffer(int size, int dataSetCount, boolean containsStacks, float barOffset) {
        super(size, dataSetCount, containsStacks);
        this.barOffset = barOffset;
    }

    @Override
    public void feed(IBarDataSet data) {
        float size = data.getEntryCount() * phaseX;
        float barWidthHalf = mBarWidth / 2f;
        for (int i = 0; i < size; i++) {
            BarEntry e = data.getEntryForIndex(i);
            if(e == null)
                continue;
            float x = e.getX();
            float y = e.getY();
            float[] vals = e.getYVals();

            if (!mContainsStacks || vals == null) {
                float left = x - barWidthHalf + barOffset;
                float right = x + barWidthHalf + barOffset;
                float bottom, top;

                if (mInverted) {
                    bottom = y >= 0 ? y : 0;
                    top = y <= 0 ? y : 0;
                } else {
                    top = y >= 0 ? y : 0;
                    bottom = y <= 0 ? y : 0;
                }

                // multiply the height of the rect with the phase
                if (top > 0)
                    top *= phaseY;
                else
                    bottom *= phaseY;

                addBar(left, top, right, bottom);
            } else {
                float posY = 0f;
                float negY = -e.getNegativeSum();
                float yStart = 0f;

                // fill the stack
                for (int k = 0; k < vals.length; k++) {
                    float value = vals[k];
                    if (value == 0.0f && (posY == 0.0f || negY == 0.0f)) {
                        // Take care of the situation of a 0.0 value, which overlaps a non-zero bar
                        y = value;
                        yStart = y;
                    } else if (value >= 0.0f) {
                        y = posY;
                        yStart = posY + value;
                        posY = yStart;
                    } else {
                        y = negY;
                        yStart = negY + Math.abs(value);
                        negY += Math.abs(value);
                    }

                    float left = x - barWidthHalf + barOffset;
                    float right = x + barWidthHalf + barOffset;
                    float bottom, top;

                    if (mInverted) {
                        bottom = y >= yStart ? y : yStart;
                        top = y <= yStart ? y : yStart;
                    } else {
                        top = y >= yStart ? y : yStart;
                        bottom = y <= yStart ? y : yStart;
                    }

                    // multiply the height of the rect with the phase
                    top *= phaseY;
                    bottom *= phaseY;
                    addBar(left, top, right, bottom);
                }
            }
        }
        reset();
    }
}
