package allen.town.focus.twitter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.appcompat.widget.AppCompatButton;

public class ProgressBarButton extends AppCompatButton {
	private boolean textVisible=true;

	public ProgressBarButton(Context context){
		super(context);
	}

	public ProgressBarButton(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public ProgressBarButton(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	public void setTextVisible(boolean textVisible){
		this.textVisible=textVisible;
		invalidate();
	}

	public boolean isTextVisible(){
		return textVisible;
	}

	@Override
	protected void onDraw(Canvas canvas){
		if(textVisible){
			super.onDraw(canvas);
		}
	}
}
