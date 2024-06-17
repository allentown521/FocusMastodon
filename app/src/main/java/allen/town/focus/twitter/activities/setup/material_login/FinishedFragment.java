package allen.town.focus.twitter.activities.setup.material_login;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import allen.town.focus.twitter.R;

public class FinishedFragment extends Fragment {

    private MaterialLogin activity;
    private CheckBox followMeTwitterCb;

    public static FinishedFragment getInstance() {
        return new FinishedFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public boolean isFollowMe() {
        return followMeTwitterCb.isChecked();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_intro_finished, container, false);
        followMeTwitterCb = root.findViewById(R.id.follow_me_twitter);
        ((ImageView) root.findViewById(R.id.image)).setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));

        return root;
    }
}