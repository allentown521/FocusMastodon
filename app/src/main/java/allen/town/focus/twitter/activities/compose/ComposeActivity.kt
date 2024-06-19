package allen.town.focus.twitter.activities.compose

import allen.town.focus.twitter.BuildConfig
import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.scheduled_tweets.ViewScheduledTweets
import allen.town.focus.twitter.api.requests.instance.GetCustomEmojis
import allen.town.focus.twitter.api.requests.statuses.CreateStatus
import allen.town.focus.twitter.api.requests.statuses.GetAttachmentByID
import allen.town.focus.twitter.data.sq_lite.HashtagDataSource
import allen.town.focus.twitter.data.sq_lite.QueuedDataSource
import allen.town.focus.twitter.model.Emoji
import allen.town.focus.twitter.model.StatusPrivacy
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.PermissionModelUtils
import allen.town.focus.twitter.utils.SimpleTextWatcher
import allen.town.focus.twitter.utils.UserAutoCompleteHelper
import allen.town.focus.twitter.utils.Utils.hasInternetConnection
import allen.town.focus.twitter.utils.Utils.uploadAttachment
import allen.town.focus.twitter.utils.Utils.uploadImage
import allen.town.focus.twitter.utils.Utils.uploadVideo
import allen.town.focus.twitter.views.ReorderableLinearLayout
import allen.town.focus.twitter.views.widgets.text.FontPrefEditText
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView
import allen.town.focus_common.common.views.ATESwitch
import allen.town.focus_common.util.ImageUtils.isPhotoPickerAvailable
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.views.AccentMaterialDialog
import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.bumptech.glide.Glide
import com.github.ajalt.reprint.core.Reprint
import kotlinx.coroutines.*
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import twitter4j.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ open class ComposeActivity() : Compose() {
    private class DraftPollOption {
        lateinit var edit: EditText
        lateinit var view: View
        lateinit var dragger: View
    }

    var tryingAgainTimes = 0
    private var pollChanged = false
    lateinit var visibilityImageButton: ImageButton
    private lateinit var pollOptionsView: ReorderableLinearLayout
    private lateinit var pollWrap: View
    private lateinit var addPollOptionBtn: View
    private lateinit var pollDurationView: TextView
    private lateinit var pollMultipleSwitch: ATESwitch
    private val pollOptions: ArrayList<DraftPollOption> =
        ArrayList<DraftPollOption>()
    private var pollDuration = 24 * 3600
    private var pollDurationStr: String? = null
    override fun setUpLayout() {
        setContentView(R.layout.compose_activity)
        setUpSimilar()
        var count = 0 // number of accounts logged in
        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++
        }
        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++
        }
        if (count == 2) {
            findViewById<View>(R.id.accounts).setOnClickListener { v: View? ->
                val options: Array<String?> = arrayOfNulls(2)
                //                    String[] options = new String[3];
                options[0] = "@" + settings.myScreenName
                options[1] = "@" + settings.secondScreenName
                //                    options[2] = getString(R.string.both_accounts);
                val builder: AlertDialog.Builder = AccentMaterialDialog(
                    context,
                    R.style.MaterialAlertDialogTheme
                )
                builder.setItems(options, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, item: Int) {
                        val pic: ImageView = findViewById<View>(R.id.profile_pic) as ImageView
                        val currentName: FontPrefTextView =
                            findViewById<View>(R.id.current_name) as FontPrefTextView
                        var tweetText: String
                        when (item) {
                            0 -> {
                                useAccOne = true
                                useAccTwo = false
                                Glide.with(this@ComposeActivity).load(settings.myProfilePicUrl)
                                    .into(pic)
                                currentName.setText("@" + settings.myScreenName)
                                tweetText = reply.getText().toString()
                                tweetText = tweetText.replace("@" + settings.myScreenName + " ", "")
                                    .replace("@" + settings.myScreenName, "")
                                reply.setText(tweetText)
                                reply.setSelection(tweetText.length)
                            }
                            1 -> {
                                useAccOne = false
                                useAccTwo = true
                                Glide.with(this@ComposeActivity).load(settings.secondProfilePicUrl)
                                    .into(pic)
                                currentName.setText("@" + settings.secondScreenName)
                                tweetText = reply.getText().toString()
                                tweetText =
                                    tweetText.replace("@" + settings.secondScreenName + " ", "")
                                        .replace("@" + settings.secondScreenName, "")
                                reply.setText(tweetText)
                                reply.setSelection(tweetText.length)
                            }
                            2 -> {
                                useAccOne = true
                                useAccTwo = true
                                pic.setImageResource(R.drawable.ic_both_accounts)
                                currentName.setText(getString(R.string.both_accounts))
                            }
                        }
                    }
                })
                val alert: AlertDialog = builder.create()
                alert.show()
            }
        }
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val userAutoCompleteHelper = UserAutoCompleteHelper.applyTo(this, reply)
        overflow = findViewById<View>(R.id.overflow_button) as ImageButton
        overflow.setOnClickListener(View.OnClickListener { })
        attachButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                attachImage()
            }
        })
        captureButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                attachCapture()
            }
        })
        val at = findViewById<View>(R.id.at_button) as ImageButton
        at.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                val start = reply.selectionStart
                reply.text!!.insert(start, "@")
                reply.setSelection(start + 1)
                val window = userAutoCompleteHelper.userAutoComplete
                try {
                    if (!window.isShowing) {
                        window.show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        val hashtag = findViewById<View>(R.id.hashtag_button) as ImageButton
        hashtag.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                val start = reply.selectionStart
                reply.text!!.insert(start, "#")
                reply.setSelection(start + 1)
                val window = userAutoCompleteHelper.hashtagAutoComplete
                try {
                    if (AppSettings.getInstance(context).autoCompleteHashtags && !window.isShowing) {
                        window.show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        val SAVE_DRAFT = 0
        val VIEW_DRAFTS = 1
        val VIEW_QUEUE = 2
        val SCHEDULE = 3
        val ENABLE_FINGERPRINT_LOCK = 4
        val DISABLE_FINGERPRINT_LOCK = 5
        val overflow = findViewById<View>(R.id.overflow_button) as ImageButton
        overflow.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val menu = PopupMenu(context, findViewById(R.id.overflow_button))
                menu.menu.add(
                    Menu.NONE,
                    SAVE_DRAFT,
                    Menu.NONE,
                    context.getString(R.string.menu_save_draft)
                )
                menu.menu.add(
                    Menu.NONE,
                    VIEW_DRAFTS,
                    Menu.NONE,
                    context.getString(R.string.menu_view_drafts)
                )
                menu.menu.add(
                    Menu.NONE,
                    VIEW_QUEUE,
                    Menu.NONE,
                    context.getString(R.string.menu_view_queued)
                )
                //                menu.getMenu().add(Menu.NONE, SCHEDULE, Menu.NONE, context.getString(R.string.menu_schedule_tweet));
                if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered()) {
                    if (!AppSettings.getInstance(this@ComposeActivity).fingerprintLock) {
                        menu.menu.add(
                            Menu.NONE,
                            ENABLE_FINGERPRINT_LOCK,
                            Menu.NONE,
                            context.getString(R.string.enable_fingerprint_lock)
                        )
                    } else {
                        menu.menu.add(
                            Menu.NONE,
                            DISABLE_FINGERPRINT_LOCK,
                            Menu.NONE,
                            context.getString(R.string.disable_fingerprint_lock)
                        )
                    }
                }
                menu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                        when (menuItem.itemId) {
                            DISABLE_FINGERPRINT_LOCK -> {
                                sharedPrefs.edit().putBoolean("fingerprint_lock", false).commit()
                                AppSettings.invalidate()
                            }
                            ENABLE_FINGERPRINT_LOCK -> {
                                sharedPrefs.edit().putBoolean("fingerprint_lock", true).commit()
                                AppSettings.invalidate()
                                finish()
                            }
                            SAVE_DRAFT -> if (reply.text!!.length > 0) {
                                QueuedDataSource.getInstance(context)
                                    .createDraft(reply.text.toString(), currentAccount)
                                showSnack(
                                    context,
                                    resources.getString(R.string.saved_draft),
                                    Toast.LENGTH_SHORT
                                )
                                reply.setText("")
                                finish()
                            } else {
                                showSnack(
                                    context,
                                    resources.getString(R.string.no_tweet),
                                    Toast.LENGTH_SHORT
                                )
                            }
                            VIEW_DRAFTS -> {
                                val drafts = QueuedDataSource.getInstance(context).drafts
                                if (drafts.size > 0) {
                                    val draftsAndDelete = arrayOfNulls<String>(drafts.size + 1)
                                    draftsAndDelete[0] = getString(R.string.delete_all)
                                    var i = 1
                                    while (i < draftsAndDelete.size) {
                                        draftsAndDelete[i] = drafts[i - 1]
                                        i++
                                    }
                                    val builder: AlertDialog.Builder = AccentMaterialDialog(
                                        context,
                                        R.style.MaterialAlertDialogTheme
                                    )
                                    builder.setItems(
                                        draftsAndDelete,
                                        object : DialogInterface.OnClickListener {
                                            override fun onClick(
                                                dialog: DialogInterface,
                                                item: Int
                                            ) {
                                                if (item == 0) {
                                                    // clicked the delete all item
                                                    AccentMaterialDialog(
                                                        context,
                                                        R.style.MaterialAlertDialogTheme
                                                    )
                                                        .setMessage(getString(R.string.delete_all) + "?")
                                                        .setPositiveButton(
                                                            R.string.ok,
                                                            object :
                                                                DialogInterface.OnClickListener {
                                                                override fun onClick(
                                                                    dialogInterface: DialogInterface,
                                                                    i: Int
                                                                ) {
                                                                    QueuedDataSource.getInstance(
                                                                        context
                                                                    ).deleteAllDrafts()
                                                                    dialogInterface.dismiss()
                                                                }
                                                            })
                                                        .setNegativeButton(
                                                            R.string.cancel,
                                                            object :
                                                                DialogInterface.OnClickListener {
                                                                override fun onClick(
                                                                    dialogInterface: DialogInterface,
                                                                    i: Int
                                                                ) {
                                                                    dialogInterface.dismiss()
                                                                }
                                                            })
                                                        .create()
                                                        .show()
                                                    dialog.dismiss()
                                                } else {
                                                    AccentMaterialDialog(
                                                        context,
                                                        R.style.MaterialAlertDialogTheme
                                                    )
                                                        .setTitle(context.resources.getString(R.string.apply))
                                                        .setMessage(draftsAndDelete[item])
                                                        .setPositiveButton(
                                                            R.string.ok,
                                                            object :
                                                                DialogInterface.OnClickListener {
                                                                override fun onClick(
                                                                    dialogInterface: DialogInterface,
                                                                    i: Int
                                                                ) {
                                                                    reply.setText(draftsAndDelete[item])
                                                                    reply.setSelection(reply.text!!.length)
                                                                    QueuedDataSource.getInstance(
                                                                        context
                                                                    ).deleteDraft(
                                                                        draftsAndDelete[item]
                                                                    )
                                                                    dialogInterface.dismiss()
                                                                }
                                                            })
                                                        .setNegativeButton(
                                                            R.string.delete_draft,
                                                            object :
                                                                DialogInterface.OnClickListener {
                                                                override fun onClick(
                                                                    dialogInterface: DialogInterface,
                                                                    i: Int
                                                                ) {
                                                                    QueuedDataSource.getInstance(
                                                                        context
                                                                    ).deleteDraft(
                                                                        draftsAndDelete[item]
                                                                    )
                                                                    dialogInterface.dismiss()
                                                                }
                                                            })
                                                        .create()
                                                        .show()
                                                    dialog.dismiss()
                                                }
                                            }
                                        })
                                    val alert = builder.create()
                                    alert.show()
                                } else {
                                    showSnack(context, R.string.no_drafts, Toast.LENGTH_SHORT)
                                }
                            }
                            SCHEDULE -> {
                                val schedule = Intent(context, ViewScheduledTweets::class.java)
                                if (!reply.text.toString().isEmpty()) {
                                    schedule.putExtra("has_text", true)
                                    schedule.putExtra("text", reply.text.toString())
                                }
                                startActivity(schedule)
                                finish()
                            }
                            VIEW_QUEUE -> {
                                val queued = QueuedDataSource.getInstance(context)
                                    .getQueuedTweets(currentAccount)
                                if (queued.size > 0) {
                                    val builder: AlertDialog.Builder = AccentMaterialDialog(
                                        context,
                                        R.style.MaterialAlertDialogTheme
                                    )
                                    builder.setItems(
                                        queued,
                                        object : DialogInterface.OnClickListener {
                                            override fun onClick(
                                                dialog: DialogInterface,
                                                item: Int
                                            ) {
                                                AccentMaterialDialog(
                                                    context,
                                                    R.style.MaterialAlertDialogTheme
                                                )
                                                    .setTitle(context.resources.getString(R.string.keep_queued_tweet))
                                                    .setMessage(queued[item])
                                                    .setPositiveButton(
                                                        R.string.ok,
                                                        object : DialogInterface.OnClickListener {
                                                            override fun onClick(
                                                                dialogInterface: DialogInterface,
                                                                i: Int
                                                            ) {
                                                                dialogInterface.dismiss()
                                                            }
                                                        })
                                                    .setNegativeButton(
                                                        R.string.delete_draft,
                                                        object : DialogInterface.OnClickListener {
                                                            override fun onClick(
                                                                dialogInterface: DialogInterface,
                                                                i: Int
                                                            ) {
                                                                QueuedDataSource.getInstance(context)
                                                                    .deleteQueuedTweet(
                                                                        queued[item]
                                                                    )
                                                                dialogInterface.dismiss()
                                                            }
                                                        })
                                                    .create()
                                                    .show()
                                                dialog.dismiss()
                                            }
                                        })
                                    val alert = builder.create()
                                    alert.show()
                                } else {
                                    showSnack(context, R.string.no_queued, Toast.LENGTH_SHORT)
                                }
                            }
                        }
                        return false
                    }
                })
                menu.show()
            }
        })
        visibilityImageButton = findViewById<View>(R.id.visibility) as ImageButton
        visibilityImageButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                selectVisibility()
            }
        })
        if (!settings.useEmoji) {
            emojiButton.visibility = View.GONE
        } else {
            GetCustomEmojis().setCallback(object : Callback<List<Emoji>> {
                override fun onSuccess(result: List<Emoji>) {
                    emojiKeyboard.setEmojiList(result)
                }

                override fun onError(error: ErrorResponse?) {
                }

            }).exec()
            emojiKeyboard.setAttached(reply as FontPrefEditText)
            reply.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    if (emojiKeyboard.isShowing) {
                        emojiKeyboard.setVisibility(false)
                        emojiButton.setImageResource(R.drawable.ic_round_emoji_emotions_24)
                    }
                }
            })
            emojiButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    if (emojiKeyboard.isShowing) {
                        emojiKeyboard.setVisibility(false)
                        Handler().postDelayed(object : Runnable {
                            override fun run() {
                                val imm = getSystemService(
                                    INPUT_METHOD_SERVICE
                                ) as InputMethodManager
                                imm.showSoftInput(reply, 0)
                            }
                        }, 250)
                        emojiButton.setImageResource(R.drawable.ic_round_emoji_emotions_24)
                    } else {
                        val imm = getSystemService(
                            INPUT_METHOD_SERVICE
                        ) as InputMethodManager
                        imm.hideSoftInputFromWindow(reply.windowToken, 0)
                        Handler().postDelayed(object : Runnable {
                            override fun run() {
                                emojiKeyboard.setVisibility(true)
                            }
                        }, 250)
                        emojiButton.setImageResource(R.drawable.ic_round_keyboard_24)
                    }
                }
            })
        }

        pollButton.setOnClickListener(View.OnClickListener { v: View? -> togglePoll() })
        pollOptionsView = findViewById(R.id.poll_options)
        pollWrap = findViewById<View>(R.id.poll_wrap)
        addPollOptionBtn = findViewById<View>(R.id.add_poll_option)

        addPollOptionBtn.setOnClickListener(View.OnClickListener { v: View? ->
            createDraftPollOption().edit.requestFocus()
            updatePollOptionHints()
        })
        pollOptionsView.setDragListener { oldIndex: Int, newIndex: Int ->
            this.onSwapPollOptions(
                oldIndex,
                newIndex
            )
        }
        pollDurationView = findViewById<TextView>(R.id.poll_duration)
        pollMultipleSwitch = findViewById(R.id.poll_multiple)
        pollDurationView.text = getString(
            R.string.compose_poll_duration,
            resources.getQuantityString(R.plurals.x_days, 1, 1).also {
                pollDurationStr = it
            })
        pollDurationView.setOnClickListener(View.OnClickListener { v: View? -> showPollDurationMenu() })

        pollOptions.clear()
    }

    private fun togglePoll() {
        if (pollOptions.isEmpty()) {
            pollButton.setSelected(true)
            captureButton.setEnabled(false)
            attachButton.setEnabled(false)
            gifButton.setEnabled(false)
            pollWrap.visibility = View.VISIBLE
            for (i in 0..1) createDraftPollOption()
            updatePollOptionHints()
        } else {
            pollButton.setSelected(false)
            captureButton.setEnabled(true)
            attachButton.setEnabled(true)
            gifButton.setEnabled(true)
            pollWrap.visibility = View.GONE
            addPollOptionBtn.visibility = View.VISIBLE
            pollOptionsView.removeAllViews()
            pollOptions.clear()
            pollDuration = 24 * 3600
        }
//        updatePublishButtonState()
    }

    private val creatingView = false
    private fun createDraftPollOption(): DraftPollOption {
        val option = DraftPollOption()
        option.view = LayoutInflater.from(this)
            .inflate(R.layout.compose_poll_option, pollOptionsView, false)
        option.edit = option.view.findViewById(R.id.edit)
        option.dragger = option.view.findViewById(R.id.dragger_thingy)
        option.dragger.setOnLongClickListener(View.OnLongClickListener { v: View? ->
            pollOptionsView.startDragging(option.view)
            true
        })
        option.edit.addTextChangedListener(SimpleTextWatcher { e ->
            if (!creatingView) pollChanged = true
//            updatePublishButtonState()
        })
        option.edit.setFilters(arrayOf<InputFilter>(LengthFilter(50)))
        pollOptionsView.addView(option.view)
        pollOptions.add(option)
        if (pollOptions.size == 4) addPollOptionBtn.visibility =
            View.GONE
        return option
    }

    private fun updatePollOptionHints() {
        var i = 0
        for (option in pollOptions) {
            option.edit.hint = getString(R.string.poll_option_hint, ++i)
        }
    }

    private fun onSwapPollOptions(oldIndex: Int, newIndex: Int) {
        pollOptions.add(newIndex, pollOptions.removeAt(oldIndex))
        updatePollOptionHints()
        pollChanged = true
    }

    private fun showPollDurationMenu() {
        val menu = PopupMenu(this, pollDurationView)
        menu.menu.add(0, 1, 0, resources.getQuantityString(R.plurals.x_minutes, 5, 5))
        menu.menu.add(0, 2, 0, resources.getQuantityString(R.plurals.x_minutes, 30, 30))
        menu.menu.add(0, 3, 0, resources.getQuantityString(R.plurals.x_hours, 1, 1))
        menu.menu.add(0, 4, 0, resources.getQuantityString(R.plurals.x_hours, 6, 6))
        menu.menu.add(0, 5, 0, resources.getQuantityString(R.plurals.x_days, 1, 1))
        menu.menu.add(0, 6, 0, resources.getQuantityString(R.plurals.x_days, 3, 3))
        menu.menu.add(0, 7, 0, resources.getQuantityString(R.plurals.x_days, 7, 7))
        menu.setOnMenuItemClickListener { item: MenuItem ->
            pollDuration = when (item.itemId) {
                1 -> 5 * 60
                2 -> 30 * 60
                3 -> 3600
                4 -> 6 * 3600
                5 -> 24 * 3600
                6 -> 3 * 24 * 3600
                7 -> 7 * 24 * 3600
                else -> throw IllegalStateException("Unexpected value: " + item.itemId)
            }
            pollDurationView.text = getString(
                R.string.compose_poll_duration,
                item.title.toString().also {
                    pollDurationStr = it
                })
            pollChanged = true
            true
        }
        menu.show()
    }

    fun attachCapture() {
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setItems(R.array.attach_capture, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, item: Int) {
                if (item == 0) { // take picture
                    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val f = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toString() + "/Focus_for_Mastodon/", "photoToTweet.jpg"
                    )
                    if (!f.exists()) {
                        try {
                            f.parentFile.mkdirs()
                            f.createNewFile()
                        } catch (e: IOException) {
                        }
                    }
                    captureIntent.addFlags(
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    )
                    try {
                        val photoURI = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + ".provider", f
                        )
                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(captureIntent, CAPTURE_IMAGE)
                    } catch (e: Exception) {
                        var permission = ContextCompat.checkSelfPermission(
                            this@ComposeActivity,
                            Manifest.permission.CAMERA
                        )
                        if (permission == PackageManager.PERMISSION_DENIED) {
                            val utils = PermissionModelUtils(context)
                            utils.requestCameraPermission()
                        }
                        permission = ContextCompat.checkSelfPermission(
                            this@ComposeActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        if (permission == PackageManager.PERMISSION_DENIED) {
                            val utils = PermissionModelUtils(context)
                            utils.requestStoragePermission()
                        }
                    }
                } else if (item == 1) { // video
                    val permissionsToRequest: MutableList<String> = ArrayList()
                    val cameraPermission = ContextCompat.checkSelfPermission(
                        this@ComposeActivity,
                        Manifest.permission.CAMERA
                    )
                    if (cameraPermission == PackageManager.PERMISSION_DENIED) {
                        permissionsToRequest.add(Manifest.permission.CAMERA)
                    }
                    val audioPermission = ContextCompat.checkSelfPermission(
                        this@ComposeActivity,
                        Manifest.permission.RECORD_AUDIO
                    )
                    if (audioPermission == PackageManager.PERMISSION_DENIED) {
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (permissionsToRequest.size > 0) {
                        ActivityCompat.requestPermissions(
                            this@ComposeActivity,
                            permissionsToRequest.toTypedArray(),
                            VIDEO_PERMISSION_REQUEST_CODE
                        )
                        return
                    }
                    try {
                        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                        //好使
                        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 512 * 1024 * 1024)
                        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 140)
                        startActivityForResult(intent, CAPTURE_VIDEO)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
        builder.create().show()
    }

    fun attachImage() {
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setItems(R.array.attach_options, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, item: Int) {
                if (item == 0) { // attach picture
                    if (isPhotoPickerAvailable()) {
                        val photoPickerIntent = Intent(MediaStore.ACTION_PICK_IMAGES)
                        photoPickerIntent.type = "image/*"
                        startActivityForResult(
                            Intent.createChooser(
                                photoPickerIntent,
                                "Select Picture"
                            ), SELECT_PHOTO
                        )
                    } else {
                        try {
                            val intent = Intent()
                            intent.type = "image/*"
                            intent.action = Intent.ACTION_GET_CONTENT
                            startActivityForResult(
                                Intent.createChooser(intent, "Select Picture"),
                                SELECT_PHOTO
                            )
                        } catch (e: Exception) {
                            val photoPickerIntent = Intent(Intent.ACTION_PICK)
                            photoPickerIntent.type = "image/*"
                            startActivityForResult(
                                Intent.createChooser(
                                    photoPickerIntent,
                                    "Select Picture"
                                ), SELECT_PHOTO
                            )
                        }
                    }
                } else if (item == 1) { // attach video
                    if (isPhotoPickerAvailable()) {
                        val photoPickerIntent = Intent(MediaStore.ACTION_PICK_IMAGES)
                        photoPickerIntent.type = "video/mp4"
                        startActivityForResult(
                            Intent.createChooser(
                                photoPickerIntent,
                                "Select Picture"
                            ), SELECT_VIDEO
                        )
                    } else {
                        try {
                            val gifIntent = Intent()
                            gifIntent.type = "video/mp4"
                            gifIntent.action = Intent.ACTION_GET_CONTENT
                            startActivityForResult(gifIntent, SELECT_VIDEO)
                        } catch (e: Exception) {
                            val gifIntent = Intent()
                            gifIntent.type = "video/mp4"
                            gifIntent.action = Intent.ACTION_PICK
                            startActivityForResult(gifIntent, SELECT_VIDEO)
                        }
                    }
                }
            }
        })
        builder.create().show()
    }

    private var currentVisibility: StatusPrivacy = StatusPrivacy.PUBLIC
    fun selectVisibility() {
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setItems(R.array.visibility_options, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, item: Int) {
                currentVisibility =
                    StatusPrivacy.valueOf(getStringArray(R.array.visibility_values)[item])
                setStatusVisibility(currentVisibility)
            }
        })
        builder.create().show()
    }

    private fun setStatusVisibility(visibility: StatusPrivacy) {

        val iconRes = when (visibility) {
            StatusPrivacy.PUBLIC -> R.drawable.ic_public_24dp
            StatusPrivacy.PRIVATE -> R.drawable.ic_lock_outline_24dp
            StatusPrivacy.DIRECT -> R.drawable.ic_email_24dp
            StatusPrivacy.UNLISTED -> R.drawable.ic_lock_open_24dp
            else -> R.drawable.ic_lock_open_24dp
        }
        visibilityImageButton.setImageResource(iconRes)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val results: MutableList<Int> = ArrayList()
        for (result: Int in grantResults) {
            results.add(result)
        }
        if (requestCode == VIDEO_PERMISSION_REQUEST_CODE && results.contains(PackageManager.PERMISSION_DENIED)) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ||
                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            ) {
                PermissionModelUtils(this).showVideoRecorderPermissions()
            } else {
                AccentMaterialDialog(
                    context,
                    R.style.MaterialAlertDialogTheme
                )
                    .setTitle(R.string.video_permissions)
                    .setMessage(R.string.no_video_permission_first_time)
                    .setPositiveButton(R.string.ok, { dialog: DialogInterface?, which: Int ->
                        ActivityCompat.requestPermissions(
                            this@ComposeActivity, arrayOf(
                                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                            ),
                            VIDEO_PERMISSION_REQUEST_CODE
                        )
                    })
                    .create().show()
            }
        }
    }

    override fun setUpReplyText() {
        // for failed notification
        if (intent.getStringExtra("failed_notification_text") != null) {
            reply.setText(intent.getStringExtra("failed_notification_text"))
            reply.setSelection(reply.text!!.length)
        }
        to = intent.getStringExtra("user") + (if (isDM) "" else " ")
        to = to.trim { it <= ' ' } + " "
        if ((to != "null " && !isDM) || (isDM && to != "null")) {
            if (!isDM) {
                Log.v("username_for_noti", "to place: $to")
                if (to.contains("/status/")) {
                    // quoting a tweet
                    quotingAStatus = to
                    attachmentUrl = to
                    reply.setText("")
                    reply.setSelection(0)
                } else {
                    reply.setText(to)
                    reply.setSelection(reply.text.toString().length)
                }
            } else {
                contactEntry.setText(to)
                reply.requestFocus()
            }
            sharedPrefs.edit().putString("draft", "").commit()
        }
        notiId = intent.getLongExtra("id", 0)
        replyText = intent.getStringExtra("reply_to_text")
        replyStatus = intent.getSerializableExtra("reply_to_status") as? Status

        // Get intent, action and MIME type
        val intent = intent
        val action = intent.action
        val type = intent.type
        if ((Intent.ACTION_SEND == action) && type != null) {
            sharingSomething = true
            if (("text/plain" == type)) {
                handleSendText(intent) // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent) // Handle single image being sent
            }
            Handler().postDelayed(object : Runnable {
                override fun run() {
                    replyText = ""
                }
            }, 1000)
        }
    }

    override fun doneClick(): Boolean {
        if (emojiKeyboard.isShowing) {
            emojiKeyboard.setVisibilityImmediately(false)
            emojiButton.setImageResource(R.drawable.ic_round_emoji_emotions_24)
        }
        val editText = findViewById<View>(R.id.tweet_content) as EditText
        val status = editText.text.toString()
        if (!hasInternetConnection(context) && !status.isEmpty() && (imagesAttached == 0)) {
            // we are going to queue this tweet to send for when they get a connection
            QueuedDataSource.getInstance(context).createQueuedTweet(status, currentAccount)
            showSnack(context, R.string.tweet_queued, Toast.LENGTH_SHORT)
            return true
        } else if (!hasInternetConnection(context) && imagesAttached > 0) {
            // we only queue tweets without pictures
            showSnack(context, R.string.only_queue_no_pic, Toast.LENGTH_SHORT)
            return false
        }

        if (editText.text.isEmpty() && imagesAttached == 0) {
            //没有文本内容也没有附件
            showSnack(
                context,
                context.resources.getString(R.string.error_empty),
                Toast.LENGTH_SHORT
            )
            return false

        }

        if (editText.text.length > AppSettings.getInstance(
                this
            ).tweetCharacterCount
        ) {
            //太长
            showSnack(
                context,
                context.resources.getString(R.string.tweet_to_long),
                Toast.LENGTH_SHORT
            )
            return false

        }

        // update status
        doneClicked = true
        sendStatus(status, charRemaining.text.toString().toInt())
        return true

    }

    fun sendStatus(status: String?, length: Int) {
        UpdateTwitterStatus(reply.text.toString(), length).execute(status)
    }

    override fun onPause() {
        sharedPrefs.edit().putString("draft", "").commit()
        try {
            if (!(doneClicked || discardClicked)) {
                QueuedDataSource.getInstance(context)
                    .createDraft(reply.text.toString(), currentAccount)
            }
        } catch (e: Exception) {
            // it is a direct message
        }
        super.onPause()
    }

    internal inner class UpdateTwitterStatus {
        var text: String?
        var status: String? = null
        private var secondTry: Boolean
        private var remaining: Int

        constructor(text: String?, length: Int) {
            var text = text
            if (quotingAStatus != null) {
                text += " $quotingAStatus"
            }
            this.text = text
            remaining = length
            secondTry = false
            Handler().postDelayed(object : Runnable {
                override fun run() {
                    makeTweetingNotification()
                }
            }, 50)
        }

        constructor(text: String?, length: Int, secondTry: Boolean) {
            var text = text
            if (quotingAStatus != null) {
                text += " $quotingAStatus"
            }
            this.text = text
            remaining = length
            this.secondTry = secondTry
            Handler().postDelayed(object : Runnable {
                override fun run() {
                    makeTweetingNotification()
                }
            }, 50)
        }

        /**
         * Helper function to tweet updates without attaching images. Set the tweet to
         * scheduled as a workaround for the rate-limit from twitter. Provide a time if the
         * tweet is scheduled.
         *
         * @param scheduled True if tweet needs to be scheduled
         */
        /**
         * Helper function to tweet the updates without attaching images.
         *
         */
        @Throws(Exception::class)
        private fun tweetWithoutImages(
            useSecondAccount: Boolean,
            scheduled: Boolean = false,
            time: Long = 0
        ) {
            if (scheduled) {
                // some guy wanted this for the future I guess. The one the did the multi tweet PR
//                ScheduledTweet tweet = new ScheduledTweet(getApplicationContext(), context, status, time, 0);
//                tweet.createScheduledTweet();
            } else {
                var autoPopulateMetadata = false
                if (shouldReplaceTo(text)) {
                    val replaceable = to.replace("#[a-zA-Z]+ ".toRegex(), "")
                    if (replaceable != " ") {
                        status = status!!.replace(replaceable.toRegex(), "")
                        autoPopulateMetadata = true
                    }
                }
                val media = StatusUpdate(status, currentVisibility)
                media.isAutoPopulateReplyMetadata = autoPopulateMetadata
                if (notiId != 0L) {
                    media.setInReplyToStatusId(notiId)
                }

                if (pollOptions.isNotEmpty()) {
                    media.poll = CreateStatus.Request.Poll()
                    media.poll.expiresIn = pollDuration
                    media.poll.multiple = pollMultipleSwitch.isChecked
                    for (opt: DraftPollOption in pollOptions)
                        media.poll.options.add(opt.edit.text.toString())
                }

                // Update status
                val status: twitter4j.Status
                if (useSecondAccount) {
                    status =
                        StatusJSONImplMastodon(CreateStatus(CreateStatus.parseStatusUpdate(media)).execSecondAccountSync())
                } else {
                    status =
                        StatusJSONImplMastodon(CreateStatus(CreateStatus.parseStatusUpdate(media)).execSync())
                }
                notiId = status.getId()
            }
        }

        private val supervisorJob = SupervisorJob()
        private val serviceScope = CoroutineScope(Dispatchers.Main + supervisorJob)
        fun execute(content: String?) {
            status = content
            if (quotingAStatus != null) {
                status += " $quotingAStatus"
            }
            var success: Boolean = false
            serviceScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        if (imagesAttached == 0) {
                            if (useAccOne) {
                                tweetWithoutImages(false)
                            }
                            if (useAccTwo) {
                                tweetWithoutImages(true)
                            }
                            success = true
                        } else {
                            // status with picture(s)
                            var autoPopulateMetadata = false
                            if (shouldReplaceTo(text)) {
                                val replaceable = to.replace("#[a-zA-Z]+ ".toRegex(), "")
                                if (replaceable != " ") {
                                    status = status!!.replace(replaceable.toRegex(), "")
                                    autoPopulateMetadata = true
                                }
                            }
                            val media = StatusUpdate(status, currentVisibility)
                            val media2 = StatusUpdate(status, currentVisibility)
                            if (autoPopulateMetadata) {
                                media.isAutoPopulateReplyMetadata = autoPopulateMetadata
                                media2.isAutoPopulateReplyMetadata = autoPopulateMetadata
                            }
                            if (notiId != 0L) {
                                media.setInReplyToStatusId(notiId)
                                media2.setInReplyToStatusId(notiId)
                            }
                            val files = arrayOfNulls<File>(imagesAttached)
                            val outputDir = context.cacheDir

                            // use twitter4j's because it is easier
                            if (attachButtonEnabled) {
                                if (imagesAttached == 1) {
                                    //media.setMedia(files[0]);
                                    if (useAccOne) {
                                        val upload: UploadedMedia =
                                            uploadImage(this@ComposeActivity, false, attachedUri[0])
                                        if (upload != null) {
                                            val mediaId = upload.mediaId
                                            media.setMediaIds(mediaId)
                                        }
                                    }
                                    if (useAccTwo) {
                                        val upload: UploadedMedia =
                                            uploadImage(this@ComposeActivity, true, attachedUri[0])
                                        if (upload != null) {
                                            val mediaId = upload.mediaId
                                            media2.setMediaIds(mediaId)
                                        }
                                    }
                                } else {
                                    // has multiple images and should be done through twitters service
                                    if (useAccOne) {
                                        val mediaIds = LongArray(files.size)
                                        for (i in files.indices) {
                                            val upload: UploadedMedia =
                                                uploadImage(
                                                    this@ComposeActivity,
                                                    false,
                                                    attachedUri[i]
                                                )
                                            if (upload != null) {
                                                mediaIds[i] = upload.mediaId
                                            }
                                        }
                                        media.setMediaIds(*mediaIds)
                                    }
                                    if (useAccTwo) {
                                        val mediaIds = LongArray(files.size)
                                        for (i in files.indices) {
                                            val upload: UploadedMedia =
                                                uploadImage(
                                                    this@ComposeActivity,
                                                    true,
                                                    attachedUri[i]
                                                )
                                            if (upload != null) {
                                                mediaIds[i] = upload.mediaId
                                            }
                                        }
                                        media2.setMediaIds(*mediaIds)
                                    }
                                }
                            } else {
                                // animated gif or video
                                Timber.v("attaching: $attachmentType")
                                Timber.v("media: " + attachedUri[0])
                                if ((attachmentType == "animated_gif")) {
                                    files[0] = File.createTempFile("compose", ".gif", outputDir)
                                    val stream = contentResolver.openInputStream(
                                        Uri.parse(
                                            attachedUri[0]
                                        )
                                    )
                                    val fos = FileOutputStream(files[0])
                                    var read = 0
                                    val bytes = ByteArray(1024)
                                    while ((stream!!.read(bytes).also { read = it }) != -1) {
                                        fos.write(bytes, 0, read)
                                    }
                                    stream.close()
                                    fos.close()
                                    if (useAccOne) {
                                        val upload =
                                            uploadAttachment(
                                                this@ComposeActivity,
                                                false,
                                                files[0]!!
                                            )
                                        val mediaId = upload.mediaId
                                        media.setMediaIds(mediaId)
                                    }
                                    if (useAccTwo) {
                                        val upload =
                                            uploadAttachment(this@ComposeActivity, true, files[0]!!)
                                        val mediaId = upload.mediaId
                                        media2.setMediaIds(mediaId)
                                    }
                                } else {

                                    if (useAccOne) {
                                        val upload = uploadVideo(context, false, attachedUri[0])
                                        val mediaId = upload.mediaId
                                        media.setMediaIds(mediaId)
                                    }
                                    if (useAccTwo) {
                                        val upload = uploadVideo(context, true, attachedUri[0])
                                        val mediaId = upload.mediaId
                                        media2.setMediaIds(mediaId)
                                    }
                                }
                            }
                            var s: twitter4j.Status? = null
                            var finalMedia: StatusUpdate
                            if (useAccOne) {
                                finalMedia = media
                            } else {
                                finalMedia = media2
                            }

                            // then wait until server finished processing the media
                            var mediaCheckRetries = 0
                            while (mediaCheckRetries < 5 && finalMedia.media.any { mediaItem -> !mediaItem.processed }) {
                                delay(1000L * mediaCheckRetries)
                                finalMedia.media.forEach { mediaItem ->
                                    if (!mediaItem.processed) {
                                        val attachment = GetAttachmentByID(mediaItem.id).execSync()
                                        if (attachment != null && !TextUtils.isEmpty(attachment.url)) {
                                            //根据tusky写法，206 -> { } // media is still being processed, continue checking，200才是处理完了
                                            mediaItem.processed = true // success
                                        }
                                    }
                                }
                                mediaCheckRetries++
                            }

                            if (useAccOne) {
                                s =
                                    StatusJSONImplMastodon(
                                        CreateStatus(
                                            CreateStatus.parseStatusUpdate(
                                                media
                                            )
                                        ).execSync()
                                    )
                            }
                            if (useAccTwo) {
                                s = StatusJSONImplMastodon(
                                    CreateStatus(
                                        CreateStatus.parseStatusUpdate(media2)
                                    ).execSecondAccountSync()
                                )
                            }
                            if (status != null) {
                                val text =
                                    status!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                        .toTypedArray()
                                Thread(object : Runnable {
                                    override fun run() {
                                        val tags = ArrayList<String>()
                                        for (split: String in text) {
                                            if (split.contains("#")) {
                                                tags.add(split)
                                            }
                                        }
                                        val source = HashtagDataSource.getInstance(context)
                                        for (s: String? in tags) {
                                            source.deleteTag(s)
                                            source.createTag(s)
                                        }
                                    }
                                }).start()
                            }
                            success = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread(Runnable { displayErrorNotification(e) })
                        //重试一次
                        success = false
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                        outofmem = true
                    }
                }

                // dismiss the dialog after getting all products
                if (tryingAgainTimes > 0 || success) {
                    if (success) {
                        finishedTweetingNotification()
                    } else if (outofmem) {
                        showSnack(
                            this@ComposeActivity,
                            getString(R.string.error_attaching_image),
                            Toast.LENGTH_SHORT
                        )
                    } else {
                        makeFailedNotification(text)
                    }
                } else {
                    //只重试一次
                    tryingAgainTimes++
                    UpdateTwitterStatus(text, remaining, true).execute(status)
                }
            }
        }

        var outofmem = false
    }

    companion object {
        private val VIDEO_PERMISSION_REQUEST_CODE = 1
    }
}