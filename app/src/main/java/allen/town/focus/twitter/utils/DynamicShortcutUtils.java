package allen.town.focus.twitter.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.bumptech.glide.Glide;

import allen.town.focus.twitter.activities.compose.ComposeActivity;
import allen.town.focus.twitter.activities.compose.ComposeSecAccActivity;
import allen.town.focus.twitter.appshortcuts.ShortcutsDefaultList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.redirects.RedirectToMyAccount;
import allen.town.focus.twitter.utils.redirects.RedirectToSecondAccount;

import java.util.List;
import java.util.concurrent.ExecutionException;

import code.name.monkey.appthemehelper.ThemeStore;

public class DynamicShortcutUtils {

    private Context context;
    private ShortcutManager manager;

    @SuppressWarnings("WrongConstant")
    public DynamicShortcutUtils(Context context) {
        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            manager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        }
    }

    public void buildProfileShortcut() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && manager != null) {
            final AppSettings settings = AppSettings.getInstance(context);

            List<ShortcutInfo> shortcuts = new ShortcutsDefaultList(context).getDefaultShortcuts();

            Intent firstAccount = new Intent(context, ComposeActivity.class);
            firstAccount.setAction(Intent.ACTION_VIEW);
            shortcuts.add(new ShortcutInfo.Builder(context, settings.myScreenName)
                    .setIntent(firstAccount)
                    .setRank(0)
                    .setShortLabel(settings.myName)
                    .setIcon(getIcon(context, settings.myProfilePicUrl))
                    .build());

            if (settings.numberOfAccounts == 2) {
                Intent secondAccount = new Intent(context, ComposeSecAccActivity.class);
                secondAccount.setAction(Intent.ACTION_VIEW);
                shortcuts.add(new ShortcutInfo.Builder(context, settings.secondScreenName)
                        .setIntent(secondAccount)
                        .setRank(0)
                        .setShortLabel(settings.secondName)
                        .setIcon(getIcon(context, settings.secondProfilePicUrl))
                        .build());
            }

            manager.setDynamicShortcuts(shortcuts);
        }
    }

    private Icon getIcon(Context context, String url) throws InterruptedException, ExecutionException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //glide4 这里不能用-1
            Bitmap image = Glide.with(context).asBitmap().load(url)
                    .into(114,114).get();

            if (image != null) {
                return createIcon(image);
            } else {
                Bitmap color = Bitmap.createBitmap(Utils.toDP(48, context), Utils.toDP(48, context), Bitmap.Config.ARGB_8888);
                color.eraseColor(ThemeStore.accentColor(context));

                return createIcon(color);
            }
        } else {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private Icon createIcon(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Icon.createWithAdaptiveBitmap(bitmap);
        } else {
            bitmap = ImageUtils.getCircleBitmap(bitmap);
            return Icon.createWithBitmap(bitmap);
        }
    }
}
