package cn.qianlan.klinedemo.chart;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

/**
 * 图表联动高亮监听
 */
public class CoupleChartValueSelectedListener implements OnChartValueSelectedListener {

    private BarLineChartBase srcChart;
    private BarLineChartBase[] dstCharts;
    private ValueSelectedListener mListener;

    public CoupleChartValueSelectedListener(BarLineChartBase srcChart, BarLineChartBase... dstCharts) {
        this(null, srcChart, dstCharts);
    }

    public CoupleChartValueSelectedListener(ValueSelectedListener mListener,
                                            BarLineChartBase srcChart, BarLineChartBase... dstCharts) {
        this.mListener = mListener;
        this.srcChart = srcChart;
        this.dstCharts = dstCharts;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        if (dstCharts != null) {
            for (BarLineChartBase chart : dstCharts) {
                float touchY = h.getDrawY();//手指接触点在srcChart上的Y坐标，即手势监听器中保存数据
                float y = h.getY();
                if (chart instanceof BarChart) {
                    y = touchY - srcChart.getHeight();
                } else if (chart instanceof CombinedChart) {
                    y = touchY + chart.getHeight();
                }
                Highlight hl = new Highlight(h.getX(), Float.NaN, h.getDataSetIndex());
                hl.setDraw(h.getX(), y);
                chart.highlightValues(new Highlight[]{hl});
            }
        }
        if (mListener != null) {
            mListener.valueSelected(e);
        }
    }

    @Override
    public void onNothingSelected() {
        if (dstCharts != null) {
            for (BarLineChartBase chart : dstCharts) {
                chart.highlightValues(null);
            }
        }
        if (mListener != null) {
            mListener.nothingSelected();
        }
    }

    public interface ValueSelectedListener {
        void valueSelected(Entry e);
        void nothingSelected();
    }
}
