package allen.town.focus.twitter.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import allen.town.focus.twitter.utils.UiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import allen.town.focus.twitter.R;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class ElevationOnScrollListener extends RecyclerView.OnScrollListener{
	private boolean isAtTop;
	private Animator currentPanelsAnim;
	private View[] views;
	private FragmentRootLinearLayout fragmentRootLayout;

	public ElevationOnScrollListener(FragmentRootLinearLayout fragmentRootLayout, View... views){
	}

	public void setViews(View... views){
	}

}
