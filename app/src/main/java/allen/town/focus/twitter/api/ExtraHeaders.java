package allen.town.focus.twitter.api;

import androidx.annotation.NonNull;

import org.mariotaku.restfu.http.MultiValueMap;

import java.util.List;

import kotlin.Pair;

public interface ExtraHeaders {
    @NonNull
    List<Pair<String, String>> get();
}