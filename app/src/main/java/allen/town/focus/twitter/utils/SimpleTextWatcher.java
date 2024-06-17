package allen.town.focus.twitter.utils;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

import java.util.function.Consumer;

public class SimpleTextWatcher implements TextWatcher{
	private final Consumer<Editable> delegate;

	public SimpleTextWatcher(@NonNull Consumer<Editable> delegate){
		this.delegate=delegate;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after){

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count){

	}

	@Override
	public void afterTextChanged(Editable s){
		delegate.accept(s);
	}
}
