package cn.qianlan.klinedemo.chart;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.renderer.BubbleChartRenderer;
import com.github.mikephil.charting.renderer.CombinedChartRenderer;
import com.github.mikephil.charting.renderer.ScatterChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * 自定义CombinedChartRenderer 把Candle图、Line图 的渲染器替换成自定义渲染器
 */
public class HighlightCombinedRenderer extends CombinedChartRenderer {

    private float highlightSize;//图表高亮文字大小 单位:px

    public HighlightCombinedRenderer(CombinedChart chart, ChartAnimator animator,
                                     ViewPortHandler viewPortHandler, float highlightSize) {
        super(chart, animator, viewPortHandler);
        this.highlightSize = highlightSize;
    }

    @Override
    public void createRenderers() {
        mRenderers.clear();
        CombinedChart chart = (CombinedChart)mChart.get();
        if (chart == null)
            return;
        DrawOrder[] orders = chart.getDrawOrder();
        for (DrawOrder order : orders) {
            switch (order) {
                case BAR:
                    if (chart.getBarData() != null)
                        mRenderers.add(new BarChartRenderer(chart, mAnimator, mViewPortHandler));
                    break;
                case BUBBLE:
                    if (chart.getBubbleData() != null)
                        mRenderers.add(new BubbleChartRenderer(chart, mAnimator, mViewPortHandler));
                    break;
                case LINE:
                    if (chart.getLineData() != null)
                        mRenderers.add(new HighlightLineRenderer(chart, mAnimator, mViewPortHandler)
                                .setHighlightSize(highlightSize));
                    break;
                case CANDLE:
                    if (chart.getCandleData() != null)
                        mRenderers.add(new HighlightCandleRenderer(chart, mAnimator, mViewPortHandler)
                                .setHighlightSize(highlightSize));
                    break;
                case SCATTER:
                    if (chart.getScatterData() != null)
                        mRenderers.add(new ScatterChartRenderer(chart, mAnimator, mViewPortHandler));
                    break;
            }
        }
    }
}
