package cn.qianlan.klinedemo;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * 加载中对话框
 */
public class LoadingDialog extends DialogFragment {

    private ImageView iv;

    public static LoadingDialog newInstance() {
        return new LoadingDialog();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (iv == null) {
            iv = new ImageView(getActivity());
            iv.setImageResource(R.mipmap.loading);
        }
        iv.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.rotate));

        getDialog().setCanceledOnTouchOutside(false);//对话框外不可取消
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);//取消标题显示
        Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.CENTER);

        return iv;
    }

    public LoadingDialog show(FragmentActivity context) {
        return show(context.getFragmentManager());
    }

    public LoadingDialog show(FragmentManager manager) {
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            show(manager, "dialog");
        }
        return this;
    }

    @Override
    public void dismiss() {
        if (iv != null && iv.getAnimation() != null) {
            iv.clearAnimation();
        }
        super.dismiss();
    }
}
