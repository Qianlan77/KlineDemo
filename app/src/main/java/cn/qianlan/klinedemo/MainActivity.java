package cn.qianlan.klinedemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.Transformer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.qianlan.klinedemo.chart.ChartFingerTouchListener;
import cn.qianlan.klinedemo.chart.CoupleChartGestureListener;
import cn.qianlan.klinedemo.chart.CoupleChartValueSelectedListener;
import cn.qianlan.klinedemo.chart.HighlightCombinedRenderer;
import cn.qianlan.klinedemo.chart.InBoundXAxisRenderer;
import cn.qianlan.klinedemo.chart.InBoundYAxisRenderer;
import cn.qianlan.klinedemo.chart.OffsetBarRenderer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        TabLayout.OnTabSelectedListener, OkHttpUtil.OnDataListener, CoupleChartGestureListener.OnEdgeListener,
        CoupleChartValueSelectedListener.ValueSelectedListener, ChartFingerTouchListener.HighlightListener {

    private ImageView ivBack;
    private ImageView ivOri;

    private TabLayout tabLayout;
    private ConstraintLayout clHl;

    private TextView tvOpen;
    private TextView tvClose;
    private TextView tvHigh;
    private TextView tvLow;
    private TextView tvVol;
    private TextView tvLine;

    private CombinedChart cc;
    private BarChart bc;

    private boolean toLeft;
    private int range = 52;//一屏显示Candle个数
    private int index = 2;//TabLayout选中下标
    private float highVisX;//切屏时X轴的最大值

    private List<List<String>> dataList;
    private Map<Integer, String> xValues;
    private LineDataSet lineSetMin;//分时线
    private LineDataSet lineSet5;
    private LineDataSet lineSet10;
    private CandleDataSet candleSet;
    private CombinedData combinedData;
    private BarDataSet barSet;
    private final float barOffset = -0.5F;//BarChart偏移量
    private CoupleChartGestureListener ccGesture;
    private CoupleChartGestureListener bcGesture;

    //K线接口 参考文档:https://github.com/Dragonexio/OpenApi/blob/master/docs/%E4%B8%AD%E6%96%87/1.%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3_v1.md
    private String URL = "https://openapi.dragonex.im/api/v1/market/kline/?symbol_id=103&st=%s&direction=%s&count=100&kline_type=%s";
    private final String[] KLINE = {"1m", "5m", "15m", "30m", "1h", "1d"};
    private int[] KL_TYPE = {1, 2, 3, 4, 5, 6};
    private int[] KL_INTERVAL = {1, 5, 15, 30, 60, 1440};//单位: Min
    private final long M1 = 60 * 1000L;//1 Min的毫秒数

    //5日均线、10日均线
    private String Kline5_10 = "<font color=#B230ED>MA5:%s</font>　<font color=#EFBB40>MA10:%s</font>";

    private LoadingDialog loadingDialog;
    private Gson gson = new GsonBuilder().create();

    private DecimalFormat format4p = new DecimalFormat("0.0000");//格式化数字，保留小数点后4位
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//默认竖屏
        initView();
        loadData();
    }

    private void initView() {
        ivBack = findViewById(R.id.iv_klBack);
        ivOri = findViewById(R.id.iv_klOrientation);

        tabLayout = findViewById(R.id.tl_kl);
        clHl = findViewById(R.id.cl_klHighlight);

        tvOpen = findViewById(R.id.tv_klOpen);
        tvClose = findViewById(R.id.tv_klClose);
        tvHigh = findViewById(R.id.tv_klHigh);
        tvLow = findViewById(R.id.tv_klLow);
        tvVol = findViewById(R.id.tv_klVol);
        tvLine = findViewById(R.id.tv_klLineInfo);

        cc = findViewById(R.id.cc_kl);
        bc = findViewById(R.id.bc_kl);

        ivBack.setOnClickListener(this);
        ivOri.setOnClickListener(this);

        for (int i = 0; i < KLINE.length; i++) {
            TextView v = (TextView) LayoutInflater.from(this).inflate(R.layout.item_tab_kline, null);
            v.setText(KLINE[i]);
            tabLayout.addTab(tabLayout.newTab().setCustomView(v), i == index);
        }
        tabLayout.addOnTabSelectedListener(this);

        initChart();
    }

    private void initChart() {
        int black = getColorById(R.color.black3B);
        int gray = getColorById(R.color.gray8B);
        int red = getColorById(R.color.redEB);
        int green = getColorById(R.color.green4C);
        int highlightColor = getColorById(R.color.brown);
        float highlightWidth = 0.5F;//高亮线的线宽
        float sp8 = sp2px(8);
        //K线
        cc.setNoDataTextColor(gray);//无数据时提示文字的颜色
        cc.setDescription(null);//取消描述
        cc.getLegend().setEnabled(false);//取消图例
        cc.setDragDecelerationEnabled(false);//不允许甩动惯性滑动  和moveView方法有冲突 设置为false
        cc.setMinOffset(0);//设置外边缘偏移量
        cc.setExtraBottomOffset(6);//设置底部外边缘偏移量 便于显示X轴

        cc.setScaleEnabled(false);//不可缩放
        cc.setAutoScaleMinMaxEnabled(true);//自适应最大最小值
        cc.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.CANDLE,
                CombinedChart.DrawOrder.LINE});
        Transformer trans = cc.getTransformer(YAxis.AxisDependency.LEFT);
        //自定义X轴标签位置
        cc.setXAxisRenderer(new InBoundXAxisRenderer(cc.getViewPortHandler(), cc.getXAxis(), trans, 10));
        //自定义Y轴标签位置
        cc.setRendererLeftYAxis(new InBoundYAxisRenderer(cc.getViewPortHandler(), cc.getAxisLeft(), trans));
        //自定义渲染器 重绘高亮
        cc.setRenderer(new HighlightCombinedRenderer(cc, cc.getAnimator(), cc.getViewPortHandler(), sp8));

        //X轴
        XAxis xac = cc.getXAxis();
        xac.setPosition(XAxis.XAxisPosition.BOTTOM);
        xac.setGridColor(black);//网格线颜色
        xac.setTextColor(gray);//标签颜色
        xac.setTextSize(8);//标签字体大小
        xac.setAxisLineColor(black);//轴线颜色
        xac.disableAxisLineDashedLine();//取消轴线虚线设置
        xac.setAvoidFirstLastClipping(true);//避免首尾端标签被裁剪
        xac.setLabelCount(2, true);//强制显示2个标签
        xac.setValueFormatter(new IAxisValueFormatter() {//转换X轴的数字为文字
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int v = (int) value;
                if (!xValues.containsKey(v) && xValues.containsKey(v - 1)) {
                    v = v - 1;
                }
                String x = xValues.get(v);
                return TextUtils.isEmpty(x) ? "" : x;
            }
        });

        //左Y轴
        YAxis yac = cc.getAxisLeft();
        yac.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);//标签显示在内侧
        yac.setGridColor(black);
        yac.setTextColor(gray);
        yac.setTextSize(8);
        yac.setLabelCount(5, true);
        yac.enableGridDashedLine(5, 4, 0);//横向网格线设置为虚线
        yac.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {//只显示部分标签
                int index = getIndexY(value, axis.getAxisMinimum(), axis.getAxisMaximum());
                return index == 0 || index == 2 ? format4p.format(value) : "";//不显示的标签不能返回null
            }
        });
        //右Y轴
        cc.getAxisRight().setEnabled(false);

        //蜡烛图
        candleSet = new CandleDataSet(new ArrayList<CandleEntry>(), "Kline");
        candleSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        candleSet.setDrawHorizontalHighlightIndicator(false);
        candleSet.setHighlightLineWidth(highlightWidth);
        candleSet.setHighLightColor(highlightColor);
        candleSet.setShadowWidth(0.7f);
        candleSet.setIncreasingColor(red);//上涨为红色
        candleSet.setIncreasingPaintStyle(Paint.Style.FILL);
        candleSet.setDecreasingColor(green);//下跌为绿色
        candleSet.setDecreasingPaintStyle(Paint.Style.STROKE);
        candleSet.setNeutralColor(red);
        candleSet.setShadowColorSameAsCandle(true);
        candleSet.setDrawValues(false);
        candleSet.setHighlightEnabled(false);
        //均线
        lineSet5 = new LineDataSet(new ArrayList<Entry>(), "MA5");
        lineSet5.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineSet5.setColor(getColorById(R.color.purple));
        lineSet5.setDrawCircles(false);
        lineSet5.setDrawValues(false);
        lineSet5.setHighlightEnabled(false);
        lineSet10 = new LineDataSet(new ArrayList<Entry>(), "MA10");
        lineSet10.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineSet10.setColor(getColorById(R.color.yellow));
        lineSet10.setDrawCircles(false);
        lineSet10.setDrawValues(false);
        lineSet10.setHighlightEnabled(false);
        //分时线
        lineSetMin = new LineDataSet(new ArrayList<Entry>(), "Minutes");
        lineSetMin.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineSetMin.setColor(Color.WHITE);
        lineSetMin.setDrawCircles(false);
        lineSetMin.setDrawValues(false);
        lineSetMin.setDrawFilled(true);
        lineSetMin.setHighlightEnabled(false);
        lineSetMin.setFillColor(gray);
        lineSetMin.setFillAlpha(60);


        //成交量
        bc.setNoDataTextColor(gray);
        bc.setDescription(null);
        bc.getLegend().setEnabled(false);
        bc.setDragDecelerationEnabled(false);//不允许甩动惯性滑动
        bc.setMinOffset(0);//设置外边缘偏移量

        bc.setScaleEnabled(false);//不可缩放
        bc.setAutoScaleMinMaxEnabled(true);//自适应最大最小值
        //自定义Y轴标签位置
        bc.setRendererLeftYAxis(new InBoundYAxisRenderer(bc.getViewPortHandler(), bc.getAxisLeft(),
                bc.getTransformer(YAxis.AxisDependency.LEFT)));
        //设置渲染器控制颜色、偏移，以及高亮
        bc.setRenderer(new OffsetBarRenderer(bc, bc.getAnimator(), bc.getViewPortHandler(), barOffset)
                .setHighlightWidthSize(highlightWidth, sp8));

        bc.getXAxis().setEnabled(false);
        YAxis yab = bc.getAxisLeft();
        yab.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);//标签显示在内侧
        yab.setDrawAxisLine(false);
        yab.setGridColor(black);
        yab.setTextColor(gray);
        yab.setTextSize(8);
        yab.setLabelCount(2, true);
        yab.setAxisMinimum(0);
        yab.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return value == 0 ? "" : value + "";
            }
        });
        bc.getAxisRight().setEnabled(false);

        barSet = new BarDataSet(new ArrayList<BarEntry>(), "VOL");
        barSet.setHighLightColor(highlightColor);
        barSet.setColors(red, green);
        barSet.setDrawValues(false);
        barSet.setHighlightEnabled(false);

        ccGesture = new CoupleChartGestureListener(this, cc, bc) {//设置成全局变量，后续要用到
            @Override
            public void chartDoubleTapped(MotionEvent me) {
                doubleTapped();
            }
        };
        cc.setOnChartGestureListener(ccGesture);//设置手势联动监听
        bcGesture = new CoupleChartGestureListener(this, bc, cc) {
            @Override
            public void chartDoubleTapped(MotionEvent me) {
                doubleTapped();
            }
        };
        bc.setOnChartGestureListener(bcGesture);

        cc.setOnChartValueSelectedListener(new CoupleChartValueSelectedListener(this, cc, bc));//设置高亮联动监听
        bc.setOnChartValueSelectedListener(new CoupleChartValueSelectedListener(this, bc, cc));
        cc.setOnTouchListener(new ChartFingerTouchListener(cc, this));//手指长按滑动高亮
        bc.setOnTouchListener(new ChartFingerTouchListener(bc, this));
    }

    /**
     * 计算value是当前Y轴的第几个
     */
    private int getIndexY(float value, float min, float max) {
        float piece = (max - min) / 4;
        return Math.round((value - min) / piece);
    }

    protected void loadData() {
        clearChart();
        toLeft = true;
        getData("0");
    }

    private void clearChart() {
        if (dataList == null) {
            dataList = new ArrayList<>();
        } else {
            dataList.clear();
        }
        if (xValues == null) {
            xValues = new HashMap<>();
        } else {
            xValues.clear();
        }
        cc.setNoDataText("加载中...");
        cc.clear();
        bc.setNoDataText("加载中...");
        bc.clear();
    }

    private void getData(String time) {
        String url = String.format(URL, time, toLeft ? "2" : "1", KL_TYPE[index]);
        OkHttpUtil.getJSON(url, this);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        index = tab.getPosition();
        loadData();
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {}

    @Override
    public void onResponse(String url, String json) {
        Log.e("loge", "Kline: " + json);
        KlineEntity kl = gson.fromJson(json, KlineEntity.class);
        if (kl.isOk()) {
            int size = xValues.size();
            List<List<String>> lists = kl.getData().getLists();
            if (lists.size() <= 0) {
                dismissLoading();
                return;
            }
            if (lists.size() == 1) {
                long time = Long.parseLong(lists.get(0).get(6)) * 1000;
                String x = sdf.format(new Date(time));
                if (!xValues.containsValue(x)) {
                    handleData(lists, size);
                }
            } else {
                handleData(lists, size);
            }
        }
        dismissLoading();
    }

    /**
     * size是指追加数据之前，已有的数据个数
     */
    private void handleData(List<List<String>> lists, int size) {
        if (toLeft) {
            dataList.addAll(0, lists);//添加到左侧
        } else {
            dataList.addAll(lists);
        }

        configData();
        if (xValues.size() > 0) {
            int x = xValues.size() - (toLeft ? size : 0);
            //如果设置了惯性甩动 move方法将会无效
            if (!toLeft && size > 0) {
                cc.moveViewToAnimated(x, 0, YAxis.AxisDependency.LEFT, 200);
                bc.moveViewToAnimated(x + barOffset, 0, YAxis.AxisDependency.LEFT, 200);
            } else {
                cc.moveViewToX(x);
                bc.moveViewToX(x + barOffset);
            }
            cc.notifyDataSetChanged();
            bc.notifyDataSetChanged();
        }
    }

    private void configData() {
        if (dataList.size() == 0) {
            cc.setNoDataText("暂无相关数据");
            cc.clear();
            bc.setNoDataText("暂无相关数据");
            bc.clear();
        } else {
            if (combinedData == null) {
                combinedData = new CombinedData();
            }
            xValues.clear();
            List<CandleEntry> candleValues = candleSet.getValues();
            candleValues.clear();
            List<Entry> ma5Values = lineSet5.getValues();
            ma5Values.clear();
            List<Entry> ma10Values = lineSet10.getValues();
            ma10Values.clear();
            List<Entry> minValues = lineSetMin.getValues();
            minValues.clear();
            List<BarEntry> barValues = barSet.getValues();
            barValues.clear();
            for (int i = 0; i < dataList.size(); i++) {
                List<String> k = dataList.get(i);
                Date d = new Date(Long.parseLong(k.get(6)) * 1000);//毫秒
                String x = sdf.format(d);//显示日期
                if (xValues.containsValue(x)) {//x重复
                    dataList.remove(i);
                    i--;
                } else {
                    xValues.put(i, x);
                    float open = Float.parseFloat(k.get(4));
                    float close = Float.parseFloat(k.get(1));
                    candleValues.add(new CandleEntry(i, Float.parseFloat(k.get(2)),
                            Float.parseFloat(k.get(3)), open, close, x));
                    minValues.add(new Entry(i, close, x));
                    barValues.add(new BarEntry(i, Float.parseFloat(k.get(8)), close >= open ? 0 : 1));
                    if (i >=4) {
                        ma5Values.add(new Entry(i, getMA(i, 5)));
                        if (i >= 9) {
                            ma10Values.add(new Entry(i, getMA(i, 10)));
                        }
                    }
                }
            }
            candleSet.setValues(candleValues);
            lineSet5.setValues(ma5Values);
            lineSet10.setValues(ma10Values);
            lineSetMin.setValues(minValues);
            if (tabLayout.getSelectedTabPosition() == 0) {
                combinedData.removeDataSet(candleSet);//分时图时移除蜡烛图
                combinedData.setData(new LineData(lineSetMin));
            } else {
                combinedData.setData(new CandleData(candleSet));
                combinedData.setData(new LineData(lineSet5, lineSet10));
            }

            cc.setData(combinedData);
            float xMax = xValues.size() - 0.5F;//默认X轴最大值是 xValues.size() - 1
            cc.getXAxis().setAxisMaximum(xMax);//使最后一个显示完整

            barSet.setValues(barValues);
            BarData barData = new BarData(barSet);
            barData.setBarWidth(1 - candleSet.getBarSpace() * 2);//使Candle和Bar宽度一致
            bc.setData(barData);
            bc.getXAxis().setAxisMaximum(xMax + barOffset);//保持边缘对齐

            cc.setVisibleXRange(range, range);//设置显示X轴个数的上下限，竖屏固定52个
            bc.setVisibleXRange(range, range);
        }
    }

    private void dismissLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onFailure(String url, String error) {
        dismissLoading();
        cc.setNoDataText("加载失败 点击标签重试");
        cc.invalidate();
        bc.setNoDataText("加载失败");
        bc.invalidate();
    }

    private float getMA(int index, int maxCount) {
        int count = 1;
        float sum = Float.parseFloat(dataList.get(index).get(1));
        while (count < maxCount) {
            if (--index < 0) {
                break;
            }
            sum += Float.parseFloat(dataList.get(index).get(1));
            count++;
        }
        return sum / count;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_klBack://后退键
                onBackPressed();
                break;
            case R.id.iv_klOrientation://切换横竖屏
                highVisX = cc.getHighestVisibleX();
                setRequestedOrientation(isPort() ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    /**
     * 滑动到边缘后加载更多
     */
    @Override
    public void edgeLoad(float x, boolean left) {
        int v = (int) x;
        if (!left && !xValues.containsKey(v) && xValues.containsKey(v - 1)) {
            v = v - 1;
        }
        String time = xValues.get(v);
        if (!TextUtils.isEmpty(time)) {
            try {
                long t = sdf.parse(time).getTime();
                if (!left) {//向右获取数据时判断时间间隔
                    long interval = KL_INTERVAL[tabLayout.getSelectedTabPosition()] * M1;
                    if (System.currentTimeMillis() - t < interval) {//不会有新数据
                        return;
                    }
                }
                loadingDialog = LoadingDialog.newInstance();
                loadingDialog.show(this);
                toLeft = left;
                getData(t * 1000000L + "");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 双击图表
     */
    private void doubleTapped() {
        if (isPort()) {
            highVisX = cc.getHighestVisibleX();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void valueSelected(Entry e) {
        float x = e.getX();
        clHl.setVisibility(View.VISIBLE);
        CandleEntry candle = candleSet.getEntryForXValue(x, 0);
        if (candle != null) {
            tvOpen.setText(format4p.format(candle.getOpen()));
            tvOpen.setSelected(candle.getOpen() < candle.getClose());
            tvClose.setText(format4p.format(candle.getClose()));
            tvClose.setSelected(candle.getOpen() >= candle.getClose());

            tvHigh.setText(format4p.format(candle.getHigh()));
            tvHigh.setSelected(false);
            tvLow.setText(format4p.format(candle.getLow()));
            tvLow.setSelected(true);
        }
        BarEntry bar = barSet.getEntryForXValue(x, 0);
        if (bar != null) {
            tvVol.setText(format4p.format(bar.getY()));
        }

        if (tabLayout.getSelectedTabPosition() != 0) {
            Entry line5 = lineSet5.getEntryForXValue(x, 0);
            Entry line10 = lineSet10.getEntryForXValue(x, 0);
            if (line5 != null && line10 != null) {
                tvLine.setVisibility(View.VISIBLE);
                String line = String.format(Kline5_10, format4p.format(line5.getY()),
                        format4p.format(line10.getY()));
                tvLine.setText(fromHtml(line));
            }
        }
    }

    @Override
    public void nothingSelected() {
        clHl.setVisibility(View.GONE);
        tvLine.setVisibility(View.GONE);
    }

    @Override
    public void enableHighlight() {
        if (!barSet.isHighlightEnabled()) {
            candleSet.setHighlightEnabled(true);
            lineSetMin.setHighlightEnabled(true);
            barSet.setHighlightEnabled(true);
        }
    }

    @Override
    public void disableHighlight() {
        if (barSet.isHighlightEnabled()) {
            candleSet.setHighlightEnabled(false);
            lineSetMin.setHighlightEnabled(false);
            barSet.setHighlightEnabled(false);
            if (ccGesture != null) {
                ccGesture.setHighlight(true);
            }
            if (bcGesture != null) {
                bcGesture.setHighlight(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isPort()) {
            super.onBackPressed();
        } else {
            highVisX = cc.getHighestVisibleX();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        float rightX = cc.getHighestVisibleX();
        if (rightX == cc.getXChartMax()) {//停留在最右端
            edgeLoad(rightX, false);
        }
    }

    /**
     * 横竖屏切换
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        initView();
        range = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? 52 : 86;//竖屏显示52个 横屏显示86个
        configData();
        if (xValues.size() > 0) {
            cc.post(new Runnable() {
                @Override
                public void run() {
                    float x = highVisX - range;
                    cc.moveViewToX(x);
                    bc.moveViewToX(x + barOffset);
                    cc.notifyDataSetChanged();
                    bc.notifyDataSetChanged();
                }
            });
        }
    }

    /**
     * 当前是否是竖屏
     */
    public boolean isPort() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }


    public int getColorById(int colorId) {
        return ContextCompat.getColor(this, colorId);
    }

    public int sp2px(float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue,
                getResources().getDisplayMetrics());
    }

    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }
}
