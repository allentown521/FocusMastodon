package allen.town.focus.twitter.activities.media_viewer.image

import allen.town.focus.twitter.BuildConfig
import allen.town.focus.twitter.R
import allen.town.focus.twitter.utils.*
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import xyz.klinker.android.drag_dismiss.util.AndroidVersionUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class ImageFragment : Fragment() {

    private var attacher: DraggablePhotoViewAttacher? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_image, container, false)

        val imageLink = getLink(arguments)
        val imageView = root.findViewById<View>(R.id.imageView) as ImageView

        val args = arguments
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && args != null && args.getInt(
                EXTRA_INDEX,
                0
            ) != 0
        ) {
            imageView.transitionName = ""
        }

        Glide.with(this).load(imageLink)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    imageView.setImageDrawable(resource)

                    imageView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    imageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    imageView.invalidate()

                    if (activity != null) activity?.supportStartPostponedEnterTransition()
                }
            })

        Handler().postDelayed(
            { if (activity != null) activity?.supportStartPostponedEnterTransition() },
            1500
        )
        attacher = DraggablePhotoViewAttacher(activity as AppCompatActivity, imageView)

        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            attacher?.cleanup()
        } catch (e: Exception) {
        }
    }

    companion object {
        private val EXTRA_URL = "extra_url"
        private val EXTRA_INDEX = "extra_index"

        fun getInstance(index: Int, imageLink: String?): ImageFragment {
            val b = Bundle()
            b.putString(EXTRA_URL, imageLink)
            b.putInt(EXTRA_INDEX, index)

            val fragment = ImageFragment()
            fragment.arguments = b

            return fragment
        }

        private fun getLink(args: Bundle?): String {
            if (args == null) {
                return ""
            }

            val url = args.getString(EXTRA_URL)

            try {
                if (url != null && url.contains("imgur")) return url.replace("t.jpg", ".jpg")
            } catch (e: Exception) {
            }

            try {
                if (url != null && url.contains("insta")) return url.substring(
                    0,
                    url.length - 1
                ) + "l"
            } catch (e: Exception) {
            }


            return url ?: ""
        }
    }


    //
    //
    // Long methods for downloading and sharing images in the fragment
    //
    //


    fun downloadImage() {
        TimeoutThread {
            Looper.prepare()
            val url = getLink(arguments)

            val activity = activity ?: return@TimeoutThread

            try {
                val mBuilder = NotificationCompat.Builder(
                    activity,
                    NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL
                )
                    .setSmallIcon(R.drawable.ic_stat_icon)
                    .setTicker(resources.getString(R.string.downloading) + "...")
                    .setContentTitle(resources.getString(R.string.app_name))
                    .setContentText(resources.getString(R.string.saving_picture) + "...")
                    .setProgress(100, 100, true)

                val mNotificationManager =
                    activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(6, mBuilder.build())

                var urlString = url

                val inputStream = try {
                    val conn = URL(urlString).openConnection() as HttpURLConnection
                    BufferedInputStream(conn.inputStream)
                } catch (e: FileNotFoundException) {
                    val conn = URL(
                        urlString.replace(
                            ":orig",
                            ""
                        )
                    ).openConnection() as HttpURLConnection
                    BufferedInputStream(conn.inputStream)
                }

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()


                val name = "Image-" + Random().nextInt(1000000) + ".jpg"

                if (AndroidVersionUtils.isAndroidQ()) {
                    val relativeLocation = Environment.DIRECTORY_DOWNLOADS
                    val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

                    val contentValues = ContentValues()
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

                    val uri = context?.contentResolver?.insert(contentUri, contentValues)
                    val outputStream = context?.contentResolver?.openOutputStream(uri!!)

                    val bos = ByteArrayOutputStream()
                    bitmap.compress(CompressFormat.JPEG, 100, bos)
                    val data = bos.toByteArray()
                    val `in` = ByteArrayInputStream(data)

                    `in`.writeToOutputAndCleanup(outputStream as FileOutputStream)

                    mNotificationManager.cancel(6)
                } else {
                    var uri = IOUtils.saveImage(bitmap, name, activity)
                    val root =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toString()
                    val myDir = File("$root/Focus_for_Mastodon")
                    val file = File(myDir, name)

                    try {
                        uri = FileProvider.getUriForFile(
                            activity,
                            BuildConfig.APPLICATION_ID + ".provider", file
                        )
                    } catch (err: Exception) {
                    }

                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(uri, "image/*")

                    val randomId = NotificationUtils.generateRandomId()
                    val pending = PendingIntent.getActivity(
                        activity,
                        randomId,
                        intent,
                        Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT)
                    )

                    val builder2 = NotificationCompat.Builder(
                        activity,
                        NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL
                    )
                        //                                    .setContentIntent(pending)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(resources.getString(R.string.saved_picture) + "...")
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setAutoCancel(true)
                        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                        .setContentText(resources.getString(R.string.saved_picture) + "!")

                    mNotificationManager.cancel(6)
                    mNotificationManager.notify(randomId, builder2.build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread {
                    try {
                        PermissionModelUtils(activity).showStorageIssue(e)
                    } catch (x: Exception) {
                        e.printStackTrace()
                    }
                }

                try {
                    val builder2 = NotificationCompat.Builder(
                        activity,
                        NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL
                    )
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(resources.getString(R.string.error) + "...")
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setContentText(resources.getString(R.string.error) + "...")
                        .setProgress(0, 100, true)

                    val mNotificationManager =
                        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mNotificationManager.notify(6, builder2.build())
                } catch (x: IllegalStateException) {
                }
            }
        }.start()
    }

    fun shareImage() {
        TimeoutThread(Runnable {
            val activity = activity ?: return@Runnable

            try {
                val bitmap = Glide.with(this@ImageFragment)
                    .asBitmap()
                    .load(getLink(arguments))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get()

                activity.runOnUiThread {
                    // create the intent
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    sharingIntent.type = "image/*"

                    // add the bitmap uri to the intent
                    val uri = getImageUri(activity, bitmap)
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)

                    sharingIntent.clipData = ClipData.newRawUri(null, uri)
                    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    // start the chooser
                    activity.startActivity(
                        Intent.createChooser(
                            sharingIntent,
                            activity.getString(R.string.menu_share) + ": "
                        )
                    )
                }
            } catch (e: Exception) {

            }
        }).start()
    }

    private fun getImageUri(context: Context, inImage: Bitmap): Uri? {
        return if (AndroidVersionUtils.isAndroidQ()) {
            val relativeLocation = Environment.DIRECTORY_DOWNLOADS
            val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

            val contentValues = ContentValues()
            contentValues.put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "share_" + System.currentTimeMillis() + ".jpg"
            )
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

            val uri = context.contentResolver?.insert(contentUri, contentValues)
            val outputStream = context.contentResolver?.openOutputStream(uri!!)

            val bos = ByteArrayOutputStream()
            inImage.compress(CompressFormat.JPEG, 100, bos)
            val data = bos.toByteArray()
            val `in` = ByteArrayInputStream(data)

            `in`.writeToOutputAndCleanup(outputStream as FileOutputStream)

            uri
        } else {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val f =
                File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/Focus_for_Mastodon/share_" + System.currentTimeMillis() + ".jpg")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Focus_for_Mastodon"
            )

            try {
                if (!dir.exists()) dir.mkdirs()
                f.createNewFile()

                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())

                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", f)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }


}

fun InputStream.writeToOutputAndCleanup(out: FileOutputStream) {
    // Transfer bytes from in to out
    val buf = ByteArray(1024)
    var len = this.read(buf)
    while (len > 0) {
        out.write(buf, 0, len)
        len = this.read(buf)
    }

    this.closeSilent()
    out.closeSilent()
}

fun InputStream.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) {
    }
}

fun OutputStream.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) {
    }
}