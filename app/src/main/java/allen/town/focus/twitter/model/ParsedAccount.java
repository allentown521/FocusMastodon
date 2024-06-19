package allen.town.focus.twitter.model;

import android.text.SpannableStringBuilder;

import java.util.Collections;

import allen.town.focus.twitter.utils.CustomEmojiHelper;
import allen.town.focus.twitter.utils.HtmlParser;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ParsedAccount{
	public Account account;
	public CharSequence parsedName, parsedBio;
	public CustomEmojiHelper emojiHelper;
	public ImageLoaderRequest avatarRequest;

	public ParsedAccount(Account account, String accountID){
		this.account=account;
		parsedName= HtmlParser.parseCustomEmoji(account.displayName, account.emojis);
		parsedBio=HtmlParser.parse(account.note, null,account.emojis, Collections.emptyList(), accountID,false,true);

		emojiHelper=new CustomEmojiHelper();
		SpannableStringBuilder ssb=new SpannableStringBuilder(parsedName);
		ssb.append(parsedBio);
		emojiHelper.setText(ssb);

		avatarRequest=new UrlImageLoaderRequest(account.avatarStatic, V.dp(40), V.dp(40));
	}
}
