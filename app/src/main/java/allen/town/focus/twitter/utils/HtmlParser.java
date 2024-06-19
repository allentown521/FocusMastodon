package allen.town.focus.twitter.utils;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import allen.town.focus.twitter.model.Emoji;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.text.CustomEmojiSpan;
import allen.town.focus.twitter.text.DeleteWhenCopiedSpan;
import allen.town.focus.twitter.text.InvisibleSpan;
import allen.town.focus.twitter.text.LinkSpan;
import allen.town.focus.twitter.twittertext.CustomEmojiHelper;
import allen.town.focus.twitter.twittertext.Regex;
import allen.town.focus_common.util.Timber;
import twitter4j.UserMentionEntity;

public class HtmlParser {
    private static final String TAG = "HtmlParser";
    private static final String VALID_URL_PATTERN_STRING =
            "(" +                                                            //  $1 total match
                    "(" + Regex.URL_VALID_PRECEDING_CHARS + ")" +                        //  $2 Preceding character
                    "(" +                                                          //  $3 URL
                    "(https?://)" +                                             //  $4 Protocol (optional)
                    "(" + Regex.URL_VALID_DOMAIN + ")" +                               //  $5 Domain(s)
                    "(?::(" + Regex.URL_VALID_PORT_NUMBER + "))?" +                    //  $6 Port number (optional)
                    "(/" +
                    Regex.URL_VALID_PATH + "*+" +
                    ")?" +                                                       //  $7 URL Path and anchor
                    "(\\?" + Regex.URL_VALID_URL_QUERY_CHARS + "*" +                   //  $8 Query String
                    Regex.URL_VALID_URL_QUERY_ENDING_CHARS + ")?" +
                    ")" +
                    ")";
    public static final Pattern URL_PATTERN = Pattern.compile(VALID_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
    private static Pattern EMOJI_CODE_PATTERN = Pattern.compile(":([\\w]+):");

    private HtmlParser() {
    }

    /**
     * @param textView
     * @param emojis
     * @param mentions   从数据库序列化传它的子类，需要构造函数
     * @param extBrowser
     */
    public static void linkifyText(TextView textView, List<Emoji> emojis, List<UserMentionEntity> mentions, boolean extBrowser) {
        String accountID = AppSettings.getInstance(textView.getContext()).mySessionId;
        textView.setText(parse(textView.getText().toString(), textView, emojis, mentions, accountID, extBrowser, false));
    }

    /**
     * Parse HTML and custom emoji into a spanned string for display.
     * Supported tags: <ul>
     * <li>&lt;a class="hashtag | mention | (none)"></li>
     * <li>&lt;span class="invisible | ellipsis"></li>
     * <li>&lt;br/></li>
     * <li>&lt;p></li>
     * </ul>
     *
     * @param source Source HTML
     * @param emojis Custom emojis that are present in source as <code>:code:</code>
     * @return a spanned string
     */
    public static SpannableStringBuilder parse(String source, TextView textView, List<Emoji> emojis, List<UserMentionEntity> mentions, String accountID, boolean extBrowser, boolean useMethod1) {
        class SpanInfo {
            public Object span;
            public int start;
            public Element element;

            public SpanInfo(Object span, int start, Element element) {
                this.span = span;
                this.start = start;
                this.element = element;
            }
        }
        if (mentions == null) {
            mentions = Collections.EMPTY_LIST;
        }
        if (emojis == null) {
            emojis = Collections.EMPTY_LIST;
        }
        Map<String, String> idsByUrl = null;
        try {
            idsByUrl = mentions.stream().collect(Collectors.toMap(m -> m.getUrl(), m -> m.getId() + ""));
        } catch (Exception e) {
            //如果url是一样会抛异常，虽然不应该出现
            Timber.e("mentions parse failed", e);
        }
        // Hashtags in remote posts have remote URLs, these have local URLs so they don't match.
//		Map<String, String> tagsByUrl=tags.stream().collect(Collectors.toMap(t->t.url, t->t.name));

        final SpannableStringBuilder ssb = new SpannableStringBuilder();
        Map<String, String> finalIdsByUrl = idsByUrl;
        Jsoup.parseBodyFragment(source).body().traverse(new NodeVisitor() {
            private final ArrayList<SpanInfo> openSpans = new ArrayList<>();

            @Override
            public void head(@NonNull Node node, int depth) {
                if (node instanceof TextNode textNode) {
                    ssb.append(textNode.text());
                } else if (node instanceof Element el) {
                    switch (el.nodeName()) {
                        case "a" -> {
                            String href = el.attr("href");
                            final String originalHref = href;
                            final String content = el.text();
                            LinkSpan.Type linkType;
                            if (el.hasClass("hashtag")) {
                                String text = el.text();
                                if (text.startsWith("#")) {
                                    linkType = LinkSpan.Type.HASHTAG;
                                    href = text.substring(1);
                                } else {
                                    linkType = LinkSpan.Type.URL;
                                }
                            } else if (el.hasClass("mention")) {
                                String id = finalIdsByUrl.get(href);
                                if (TextUtils.isEmpty(id)) {
                                    //有id属性的也可以，比如转推
                                    id = el.attr("id");
                                }
                                if (!TextUtils.isEmpty(id)) {
                                    linkType = LinkSpan.Type.MENTION;
                                    href = id;
                                } else {
                                    linkType = LinkSpan.Type.URL;
                                }
                            } else {
                                linkType = LinkSpan.Type.URL;
                            }
                            openSpans.add(new SpanInfo(new LinkSpan(href, null, linkType, accountID, extBrowser, originalHref, content), ssb.length(), el));
                        }
                        case "br" -> ssb.append('\n');
                        case "span" -> {
                            if (el.hasClass("invisible")) {
                                openSpans.add(new SpanInfo(new InvisibleSpan(), ssb.length(), el));
                            }
                        }
                    }
                }
            }

            @Override
            public void tail(@NonNull Node node, int depth) {
                if (node instanceof Element el) {
                    if ("span".equals(el.nodeName()) && el.hasClass("ellipsis")) {
                        ssb.append("…", new DeleteWhenCopiedSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if ("p".equals(el.nodeName())) {
                        if (node.nextSibling() != null)
                            ssb.append("\n\n");
                    } else if (!openSpans.isEmpty()) {
                        SpanInfo si = openSpans.get(openSpans.size() - 1);
                        if (si.element == el) {
                            ssb.setSpan(si.span, si.start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            openSpans.remove(openSpans.size() - 1);
                        }
                    }
                }
            }
        });
        if (!emojis.isEmpty()) {
            if (useMethod1) {
                parseCustomEmoji(ssb, emojis);
            } else {
                CustomEmojiHelper.emojify(ssb, emojis, textView, false);
            }
        }
        return ssb;
    }

    public static SpannableStringBuilder parseCustomEmoji(String text, List<Emoji> emojis) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        parseCustomEmoji(ssb, emojis);
        return ssb;
    }

    public static void parseCustomEmoji(SpannableStringBuilder ssb, List<Emoji> emojis) {
        Map<String, Emoji> emojiByCode =
                emojis.stream()
                        .collect(
                                Collectors.toMap(e -> e.shortcode, Function.identity(), (emoji1, emoji2) -> {
                                    // Ignore duplicate shortcodes and just take the first, it will be
                                    // the same emoji anyway
                                    return emoji1;
                                })
                        );

        Matcher matcher = EMOJI_CODE_PATTERN.matcher(ssb);
        int spanCount = 0;
        CustomEmojiSpan lastSpan = null;
        while (matcher.find()) {
            Emoji emoji = emojiByCode.get(matcher.group(1));
            if (emoji == null)
                continue;
            ssb.setSpan(lastSpan = new CustomEmojiSpan(emoji), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanCount++;
        }
        if (spanCount == 1 && ssb.getSpanStart(lastSpan) == 0 && ssb.getSpanEnd(lastSpan) == ssb.length()) {
            ssb.append(' '); // To fix line height
        }
    }

    public static CharSequence parseLinks(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) // Return the original string if there are no URLs
            return text;
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        do {
            String url = matcher.group(3);
            if (TextUtils.isEmpty(matcher.group(4)))
                url = "http://" + url;
            ssb.setSpan(new LinkSpan(url, null, LinkSpan.Type.URL, null, false, url, url), matcher.start(3), matcher.end(3), 0);
        } while (matcher.find()); // Find more URLs
        return ssb;
    }

    public static String strip(String html) {
        return Jsoup.clean(html, Safelist.none());
    }
}
