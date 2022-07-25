package space.taran.arkretouch

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.RelativeLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getCompressionFormat
import com.simplemobiletools.commons.extensions.getCurrentFormattedDateTime
import com.simplemobiletools.commons.extensions.getFilenameExtension
import com.simplemobiletools.commons.extensions.getFilenameFromContentUri
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getParentPath
import com.simplemobiletools.commons.extensions.getRealPathFromURI
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.extensions.isGone
import com.simplemobiletools.commons.extensions.isInvisible
import com.simplemobiletools.commons.extensions.isPathOnOTG
import com.simplemobiletools.commons.extensions.isVisible
import com.simplemobiletools.commons.extensions.onGlobalLayout
import com.simplemobiletools.commons.extensions.onSeekBarChangeListener
import com.simplemobiletools.commons.extensions.openEditorIntent
import com.simplemobiletools.commons.extensions.rescanPath
import com.simplemobiletools.commons.extensions.sharePathIntent
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.commons.models.FileDirItem
import com.theartofdev.edmodo.cropper.CropImageView
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlinx.android.synthetic.main.activity_edit.bottomButtonEditorDisable
import kotlinx.android.synthetic.main.activity_edit.bottomRelativeEditor
import kotlinx.android.synthetic.main.activity_edit.bottom_aspect_ratios
import kotlinx.android.synthetic.main.activity_edit.bottom_editor_crop_rotate_actions
import kotlinx.android.synthetic.main.activity_edit.bottom_editor_draw_actions
import kotlinx.android.synthetic.main.activity_edit.bottom_editor_filter_actions
import kotlinx.android.synthetic.main.activity_edit.bottom_editor_primary_actions
import kotlinx.android.synthetic.main.activity_edit.crop_image_view
import kotlinx.android.synthetic.main.activity_edit.default_image_view
import kotlinx.android.synthetic.main.activity_edit.editor_draw_canvas
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.bottom_aspect_ratio_four_three
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.bottom_aspect_ratio_free
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.bottom_aspect_ratio_one_one
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.bottom_aspect_ratio_other
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.bottom_aspect_ratio_sixteen_nine
import kotlinx.android.synthetic.main.bottom_editor_actions_filter.bottom_actions_filter_list
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.bottom_aspect_ratio
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.bottom_flip_horizontally
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.bottom_flip_vertically
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.bottom_resize
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.bottom_rotate
import kotlinx.android.synthetic.main.bottom_editor_draw_actions.bottom_draw_color
import kotlinx.android.synthetic.main.bottom_editor_draw_actions.bottom_draw_color_clickable
import kotlinx.android.synthetic.main.bottom_editor_draw_actions.bottom_draw_undo
import kotlinx.android.synthetic.main.bottom_editor_draw_actions.bottom_draw_width
import kotlinx.android.synthetic.main.bottom_editor_primary_actions.bottom_primary_crop_rotate
import kotlinx.android.synthetic.main.bottom_editor_primary_actions.bottom_primary_draw
import kotlinx.android.synthetic.main.bottom_editor_primary_actions.bottom_primary_filter
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.ArkFilePickerMode
import space.taran.arkfilepicker.onArkPathPicked
import space.taran.arkretouch.dialog.OtherAspectRatioDialog
import space.taran.arkretouch.dialog.ResizeDialog
import space.taran.arkretouch.dialog.SaveAsDialogFragment
import space.taran.arkretouch.filter.FilterItem
import space.taran.arkretouch.filter.FilterThumbnailsManager
import space.taran.arkretouch.filter.FiltersAdapter
import space.taran.arkretouch.internal.BaseActivity
import space.taran.arkretouch.internal.getFileOutputStream
import space.taran.arkretouch.utils.ASPECT_RATIO_FOUR_THREE
import space.taran.arkretouch.utils.ASPECT_RATIO_FREE
import space.taran.arkretouch.utils.ASPECT_RATIO_ONE_ONE
import space.taran.arkretouch.utils.ASPECT_RATIO_OTHER
import space.taran.arkretouch.utils.ASPECT_RATIO_SIXTEEN_NINE
import space.taran.arkretouch.utils.Config
import space.taran.arkretouch.utils.copyNonDimensionAttributesTo

class EditActivity : BaseActivity(), CropImageView.OnCropImageCompleteListener {

    companion object {
        init {
            System.loadLibrary("NativeImageProcessor")
        }
    }

    private var heightPortrait: Int = 0
    private var heightLandscape: Int = 0
    private var rect: Rect? = null
    private var afterCroppedSaveClickBitmap: Bitmap? = null
    private val TEMP_FOLDER_NAME = "images"
    private val ASPECT_X = "aspectX"
    private val ASPECT_Y = "aspectY"
    private val CROP = "crop"

    private val PATH = "SAVE_FOLDER_PATH"
    private val RESULT_ORIGINAL_URI = "RESULT_ORIGINAL_URI"
    private val RESULT_SAVE_URI = "RESULT_SAVE_URI"

    // constants for bottom primary action groups
    private val PRIMARY_ACTION_NONE = 0
    private val PRIMARY_ACTION_FILTER = 1
    private val PRIMARY_ACTION_CROP_ROTATE = 2
    private val PRIMARY_ACTION_DRAW = 3

    private val CROP_ROTATE_NONE = 0
    private val CROP_ROTATE_ASPECT_RATIO = 1

    private lateinit var saveUri: Uri
    private var uri: Uri? = null
    private var resizeWidth = 0
    private var resizeHeight = 0
    private var drawColor = 0
    private var lastOtherAspectRatio: Pair<Float, Float>? = null
    private var currPrimaryAction = PRIMARY_ACTION_NONE
    private var currCropRotateAction = CROP_ROTATE_ASPECT_RATIO
    private var currAspectRatio = ASPECT_RATIO_FREE
    private var isCropIntent = false
    private var isEditingWithThirdParty = false
    private var isSharingBitmap = false
    private var wasDrawCanvasPositioned = false
    private var oldExif: ExifInterface? = null
    private var filterInitialBitmap: Bitmap? = null
    private var originalUri: Uri? = null
    private var savePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        setupPrimaryActionButtons()
        setupDrawButtons()
        savePath = intent?.getStringExtra(PATH)
        intent.data?.let { initEditActivity(it) }

        bottomRelativeEditor.setOnClickListener {
            // bottomRelativeEditor.beInvisible()
            bottomRelativeEditor.beInvisible()
            bottomButtonEditorDisable.beVisible()
        }
        bottomButtonEditorDisable.setOnClickListener {
            // bottomRelativeEditor.beInvisible()
            bottomRelativeEditor.beVisible()
            bottomButtonEditorDisable.beInvisible()
        }
    }

    override fun onSaveInstanceState(b: Bundle) {
        super.onSaveInstanceState(b)
        if (isChangingConfigurations) {
            if (default_image_view.isVisible()) {
                b.putParcelable(
                    "imageDefault",
                    default_image_view.drawable?.toBitmap()
                )
            }
            if (crop_image_view.isVisible()) {
                if (afterCroppedSaveClickBitmap != null) { // called when user click save button
                    b.putParcelable("imageCrop", afterCroppedSaveClickBitmap)
                }
                if (rect != null) { //called when user crop image
                    b.putParcelable("imageCropRect", rect)
                }
            }
            if (editor_draw_canvas.isVisible()) {
                // b.putParcelable("imageEdit", editor_draw_canvas.getBitmap())
                b.putSerializable("imageEditDetails", editor_draw_canvas.getDrawingDetails())
                b.putInt(
                    "imageEditHeightPortrait",
                    heightPortrait
                )
                b.putInt(
                    "imageEditHeightLandscape",
                    heightLandscape
                )
            }
            b.putParcelable("imageUri", uri)
        }
    }

    override fun onRestoreInstanceState(b: Bundle) {
        //you need to handle NullPointerException here.
        (b.getParcelable<Parcelable>("imageUri") as Uri?)?.let { imageUri ->
            uri = imageUri
        }
        (b.getParcelable<Parcelable>("imageDefault") as Bitmap?)?.let { imageDefault ->
            loadDefaultImageView(imageDefault)
        }
        if (b.getParcelable<Parcelable>("imageCrop") != null) { // called when user click save button
            val imageCrop = b.getParcelable<Parcelable>("imageCrop") as Bitmap
            loadCropImageView(imageCrop)
            bottom_aspect_ratios.beVisible()
            bottomCropRotateClicked()
            setupCropRotateActionButtons()
            setupAspectRatioButtons()
        } else if (b.getParcelable<Parcelable>("imageCropRect") != null) { //called when user crop image
            val imageRect = b.getParcelable<Parcelable>("imageCropRect")
            loadCropImageView(rectNew = imageRect as Rect)
            bottom_aspect_ratios.beVisible()
            bottomCropRotateClicked()
            setupCropRotateActionButtons()
            setupAspectRatioButtons()
        } else { // called when user just rotate the screen
            loadCropImageView()
            bottom_aspect_ratios.beVisible()
            bottomCropRotateClicked()
            setupCropRotateActionButtons()
            setupAspectRatioButtons()
        }
        (b.getSerializable("imageEditDetails") as LinkedHashMap<*, *>?)?.let {
            loadDrawCanvas(imageEditDetails = it)
            bottomDrawClicked()
            heightPortrait = b.getInt("imageEditHeightPortrait")
            heightLandscape = b.getInt("imageEditHeightLandscape")
        }
        setupPrimaryActionButtons()
    }

    override fun onResume() {
        super.onResume()
        isEditingWithThirdParty = false
        bottom_draw_width.setColors(
            config.textColor,
            getAdjustedPrimaryColor(),
            config.backgroundColor
        )
    }

    override fun onStop() {
        super.onStop()
        if (isEditingWithThirdParty) {
            finish()
        }
    }

    override fun onBackPressed() {
        if ((crop_image_view.isVisible() && crop_image_view.isCropAreaChanged())
                || (editor_draw_canvas.isVisible() && editor_draw_canvas.isCanvasChanged())
                || (default_image_view.isVisible() && filterInitialBitmap != default_image_view.drawable.toBitmap())
        ){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setMessage("Do you want to save the changes?")
            builder.setPositiveButton(
                "Yes"
            ) { dialog, it -> //if user pressed "yes", then he is allowed to save changes
                saveImage()
            }
            builder.setNegativeButton(
                "No"
            ) { dialog, it -> //if user select "No", just cancel this dialog and continue with app
                super.onBackPressed()
                //dialog.cancel()
            }
            val alert: AlertDialog = builder.create()
            alert.show()
        }else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)

        // disable the + icon if app open from other intent
        if (intent.data != null) {
            menu.findItem(R.id.open).isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.open -> {
                handlePermission(PERMISSION_WRITE_STORAGE) {
                    ArkFilePickerFragment
                        .newInstance(getFilePickerConfig())
                        .show(supportFragmentManager, null)

                    supportFragmentManager.onArkPathPicked(this) {
                        initEditActivity(Uri.fromFile(it.toFile()))
                    }
                }
            }
            R.id.save_as -> {
                handlePermission(PERMISSION_WRITE_STORAGE) {
                    saveImage()
                }
            }
            R.id.edit -> editWith()
            R.id.share -> shareImage()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initEditActivity(imageUri: Uri) {
        uri = imageUri
        originalUri = uri
        rect = null
        if (uri!!.scheme != "file" && uri!!.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras!!.getString(REAL_FILE_PATH)
            uri = when {
                isPathOnOTG(realPath!!) -> uri
                realPath.startsWith("file:/") -> Uri.parse(realPath)
                else -> Uri.fromFile(File(realPath))
            }
        } else {
            (getRealPathFromURI(uri!!))?.apply {
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true -> intent.extras!!.get(
                MediaStore.EXTRA_OUTPUT
            ) as Uri
            else -> uri!!
        }

        isCropIntent = intent.extras?.get(CROP) == "true"
        if (isCropIntent) {
            bottom_editor_primary_actions.beGone()
            (bottom_editor_crop_rotate_actions.layoutParams as RelativeLayout.LayoutParams).addRule(
                RelativeLayout.ALIGN_PARENT_BOTTOM,
                1
            )
        }

        wasDrawCanvasPositioned = false
        currPrimaryAction = PRIMARY_ACTION_NONE
        editor_draw_canvas.clear()
        updatePrimaryActionButtons()
        loadDefaultImageView()
        setupBottomActions()

        if (config.lastEditorCropAspectRatio == ASPECT_RATIO_OTHER) {
            if (config.lastEditorCropOtherAspectRatioX == 0f) {
                config.lastEditorCropOtherAspectRatioX = 1f
            }

            if (config.lastEditorCropOtherAspectRatioY == 0f) {
                config.lastEditorCropOtherAspectRatioY = 1f
            }

            lastOtherAspectRatio =
                Pair(config.lastEditorCropOtherAspectRatioX, config.lastEditorCropOtherAspectRatioY)
        }
        updateAspectRatio(config.lastEditorCropAspectRatio)
        crop_image_view.guidelines = CropImageView.Guidelines.ON
        bottom_aspect_ratios.beVisible()
    }

    private fun loadDefaultImageView(bitmap: Bitmap? = null) {
        bottomRelativeEditor.beVisible()
        default_image_view.beVisible()
        crop_image_view.beGone()
        editor_draw_canvas.beInvisible()

        val options = RequestOptions()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        Glide.with(this)
            .asBitmap()
            .apply {
                if (bitmap != null) {
                    load(bitmap)
                } else {
                    load(uri)
                }
            }
            .apply(options)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (uri != originalUri) {
                        uri = originalUri
                        Handler().post {
                            loadDefaultImageView()
                        }
                    }
                    return false
                }

                override fun onResourceReady(
                    bitmap: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (filterInitialBitmap == null) {
                        loadCropImageView(bitmap = bitmap)
                        bottomCropRotateClicked()
                    }

                    if (filterInitialBitmap != null && currentFilter != null && currentFilter.filter.name != getString(
                            R.string.none
                        )
                    ) {
                        default_image_view.onGlobalLayout {
                            applyFilter(currentFilter)
                        }
                    } else {
                        filterInitialBitmap = bitmap
                    }

                    if (isCropIntent) {
                        bottom_primary_filter.beGone()
                        bottom_primary_draw.beGone()
                    }

                    return false
                }
            }).into(default_image_view)
    }

    private fun loadCropImageView(
        bitmap: Bitmap? = null, rectNew: Rect? = null
    ) {
        bottomRelativeEditor.beVisible()
        default_image_view.beGone()
        editor_draw_canvas.beInvisible()
        crop_image_view.apply {
            beVisible()
            setOnCropImageCompleteListener(this@EditActivity)
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else {
                setImageUriAsync(uri)
            }
            isAutoZoomEnabled = false
            if (rectNew != null) {
                cropRect = rectNew
                rect = rectNew
            }
            setOnSetCropOverlayReleasedListener {
                Log.d("CropRelease", "setOnSetCropOverlayReleasedListener $it")
                rect = it
            }
            guidelines = CropImageView.Guidelines.ON

            if (isCropIntent && shouldCropSquare()) {
                currAspectRatio = ASPECT_RATIO_ONE_ONE
                setFixedAspectRatio(true)
                bottom_aspect_ratio.beGone()
            }
        }
    }

    private fun loadDrawCanvas(
        bitmap: Bitmap? = null,
        imageEditDetails: java.util.LinkedHashMap<*, *>? = null
    ) {
        bottomRelativeEditor.beVisible()
        default_image_view.beGone()
        crop_image_view.beGone()
        editor_draw_canvas.beVisible()
        if ((!wasDrawCanvasPositioned && uri != null) || bitmap != null) {
            wasDrawCanvasPositioned = true
            editor_draw_canvas.onGlobalLayout {
                ensureBackgroundThread {
                    fillCanvasBackground(bitmap, imageEditDetails)
                }
            }
        } else {
            editor_draw_canvas.drawRect(if (crop_image_view.isCropAreaChanged()) crop_image_view.cropRect else null)
            editor_draw_canvas.updateDrawingDetails(imageEditDetails)
        }
    }

    private fun fillCanvasBackground(
        bitmap: Bitmap? = null,
        imageEditDetails: java.util.LinkedHashMap<*, *>? = null
    ) {
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_ARGB_8888)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()

        try {
            val updatedBitmap = bitmap
                ?: Glide.with(applicationContext)
                    .asBitmap()
                    .load(uri)
                    .apply(options)
                    .submit(editor_draw_canvas.width, editor_draw_canvas.height).get()
            runOnUiThread {
                editor_draw_canvas.apply {
                    updateBackgroundBitmap(updatedBitmap)
                    layoutParams.width = updatedBitmap.width
                    layoutParams.height = updatedBitmap.height
                    //y = (height - updatedBitmap.height) / 2f
                    requestLayout()
                    editor_draw_canvas.drawRect(if (crop_image_view.isCropAreaChanged()) crop_image_view.cropRect else null)
                    editor_draw_canvas.updateDrawingDetails(imageEditDetails)
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveImage() {
        setOldExif()

        if (crop_image_view.isVisible()) {
            crop_image_view.getCroppedImageAsync()
        } else if (editor_draw_canvas.isVisible()) {
            val bitmap = if (!crop_image_view.isCropAreaChanged())
                editor_draw_canvas.getBitmap()
            else
                editor_draw_canvas.getCropImage()
            if (!::saveUri.isInitialized) {
                saveUri =
                    Uri.fromFile(File("$internalStoragePath/${getCurrentFormattedDateTime()}.jpg"))
            }
            if (bitmap != null) {
                if (saveUri.scheme == "file") {
                    SaveAsDialogFragment.newInstanceAndShow(this, savePath, saveUri.path!!, true) {
                        saveBitmapToFile(bitmap, it, true)
                    }
                } else if (saveUri.scheme == "content") {
                    val filePathGetter = getNewFilePath()
                    SaveAsDialogFragment.newInstanceAndShow(
                        this,
                        savePath,
                        filePathGetter.first,
                        filePathGetter.second
                    ) {
                        saveBitmapToFile(bitmap, it, true)
                    }
                }
            }
        } else {
            val currentFilter = getFiltersAdapter()?.getCurrentFilter() ?: return
            val filePathGetter = getNewFilePath()
            SaveAsDialogFragment.newInstanceAndShow(
                this,
                savePath,
                filePathGetter.first,
                filePathGetter.second
            ) {
                toast(R.string.saving)

                // clean up everything to free as much memory as possible
                default_image_view.setImageResource(0)
                crop_image_view.setImageBitmap(null)
                bottom_actions_filter_list.adapter = null
                bottom_actions_filter_list.beGone()

                ensureBackgroundThread {
                    try {
                        val originalBitmap =
                            Glide.with(applicationContext).asBitmap().load(uri)
                                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .get()
                        currentFilter.filter.processFilter(originalBitmap)
                        saveBitmapToFile(originalBitmap, it, false)
                    } catch (e: OutOfMemoryError) {
                        toast(R.string.out_of_memory_error)
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setOldExif() {
        var inputStream: InputStream? = null
        try {
            if (isNougatPlus()) {
                inputStream = contentResolver.openInputStream(uri!!)
                oldExif = ExifInterface(inputStream!!)
            }
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }
    }

    private fun shareImage() {
        ensureBackgroundThread {
            when {
                default_image_view.isVisible() -> {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                        ?: return@ensureBackgroundThread
                    val originalBitmap =
                        Glide.with(applicationContext).asBitmap().load(uri)
                            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
                    currentFilter.filter.processFilter(originalBitmap)
                    shareBitmap(originalBitmap)
                }
                crop_image_view.isVisible() -> {
                    isSharingBitmap = true
                    runOnUiThread {
                        crop_image_view.getCroppedImageAsync()
                    }
                }
                editor_draw_canvas.isVisible() -> editor_draw_canvas.getBitmap()
                    ?.let { shareBitmap(it) }
            }
        }
    }

    private fun getTempImagePath(bitmap: Bitmap, callback: (path: String?) -> Unit) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 0, bytes)

        val folder = File(cacheDir, TEMP_FOLDER_NAME)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                callback(null)
                return
            }
        }
        if (!::saveUri.isInitialized) {
            saveUri = Uri.fromFile(File("$internalStoragePath/${getCurrentFormattedDateTime()}.jpg"))
        }
        val filename = applicationContext.getFilenameFromContentUri(saveUri) ?: "tmp.jpg"
        val newPath = "$folder/$filename"
        val fileDirItem = FileDirItem(newPath, filename)
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                try {
                    it.write(bytes.toByteArray())
                    callback(newPath)
                } catch (e: Exception) {
                } finally {
                    it.close()
                }
            } else {
                callback("")
            }
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        getTempImagePath(bitmap) {
            if (it != null) {
                sharePathIntent(it, BuildConfig.APPLICATION_ID)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun getFiltersAdapter() = bottom_actions_filter_list.adapter as? FiltersAdapter

    private fun setupBottomActions() {
        setupPrimaryActionButtons()
        setupCropRotateActionButtons()
        setupAspectRatioButtons()
        setupDrawButtons()
    }

    private fun setupPrimaryActionButtons() {
        bottom_primary_filter.setOnClickListener {
            if (uri == null) return@setOnClickListener
            bottomFilterClicked()
        }

        bottom_primary_crop_rotate.setOnClickListener {
            if (uri == null) return@setOnClickListener
            bottomCropRotateClicked()
        }

        bottom_primary_draw.setOnClickListener {
            bottomDrawClicked()
        }
    }

    private fun bottomFilterClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_FILTER
        }
        updatePrimaryActionButtons()
    }

    private fun bottomCropRotateClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_CROP_ROTATE
        }
        updatePrimaryActionButtons()
    }

    private fun bottomDrawClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_DRAW) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_DRAW
        }
        updatePrimaryActionButtons()
    }

    private fun setupCropRotateActionButtons() {
        bottom_rotate.setOnClickListener {
            crop_image_view.rotateImage(90)
        }

        bottom_resize.beGoneIf(isCropIntent)
        bottom_resize.setOnClickListener {
            resizeImage()
        }

        bottom_flip_horizontally.setOnClickListener {
            crop_image_view.flipImageHorizontally()
        }

        bottom_flip_vertically.setOnClickListener {
            crop_image_view.flipImageVertically()
        }

        bottom_aspect_ratios.beVisible()
        bottom_aspect_ratio.setOnClickListener {
            currCropRotateAction =
                if (currCropRotateAction == CROP_ROTATE_ASPECT_RATIO) {
                    crop_image_view.guidelines = CropImageView.Guidelines.OFF
                    bottom_aspect_ratios.beGone()
                    CROP_ROTATE_NONE
                } else {
                    crop_image_view.guidelines = CropImageView.Guidelines.ON
                    bottom_aspect_ratios.beVisible()
                    CROP_ROTATE_ASPECT_RATIO
                }
            updateCropRotateActionButtons()
        }
    }

    private fun setupAspectRatioButtons() {
        bottom_aspect_ratio_free.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FREE)
        }

        bottom_aspect_ratio_one_one.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_ONE_ONE)
        }

        bottom_aspect_ratio_four_three.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FOUR_THREE)
        }

        bottom_aspect_ratio_sixteen_nine.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_SIXTEEN_NINE)
        }

        bottom_aspect_ratio_other.setOnClickListener {
            OtherAspectRatioDialog(this, lastOtherAspectRatio) {
                lastOtherAspectRatio = it
                config.lastEditorCropOtherAspectRatioX = it.first
                config.lastEditorCropOtherAspectRatioY = it.second
                updateAspectRatio(ASPECT_RATIO_OTHER)
            }
        }

        updateAspectRatioButtons()
    }

    private fun setupDrawButtons() {
        updateDrawColor(config.lastEditorDrawColor)
        bottom_draw_width.progress = config.lastEditorBrushSize
        updateBrushSize(config.lastEditorBrushSize)

        bottom_draw_color_clickable.setOnClickListener {
            ColorPickerDialog(this, drawColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    updateDrawColor(color)
                }
            }
        }

        bottom_draw_width.onSeekBarChangeListener {
            config.lastEditorBrushSize = it
            updateBrushSize(it)
        }

        bottom_draw_undo.setOnClickListener {
            editor_draw_canvas.undo()
        }
    }

    private fun updateBrushSize(percent: Int) {
        if (editor_draw_canvas.measuredHeight == 0) {
            val vto: ViewTreeObserver = editor_draw_canvas.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (editor_draw_canvas.measuredHeight > 0) {
                        val orientation = resources.configuration.orientation
                        if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
                            heightLandscape = editor_draw_canvas.measuredHeight
                            editor_draw_canvas.viewTreeObserver.removeOnGlobalLayoutListener(
                                this
                            )
                            if (heightPortrait > 0) {
                                editor_draw_canvas.updateBrushSize((heightLandscape * percent) / heightPortrait)
                                Log.d(
                                    "changePaint",
                                    "changePaint updateBrushSize2 ${(heightLandscape * percent) / heightPortrait}"
                                )
                            } else
                                editor_draw_canvas.updateBrushSize(percent)
                        } else {
                            heightPortrait = editor_draw_canvas.measuredHeight
                            editor_draw_canvas.viewTreeObserver.removeOnGlobalLayoutListener(
                                this
                            )
                            editor_draw_canvas.updateBrushSize(percent)
                            Log.d(
                                "changePaint",
                                "changePaint updateBrushSize2 $percent"
                            )
                        }
                    }
                }
            })
        } else {
            val orientation = resources.configuration.orientation
            if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
                heightLandscape = editor_draw_canvas.measuredHeight
                if (heightPortrait > 0) {
                    editor_draw_canvas.updateBrushSize((heightLandscape * percent) / heightPortrait)
                    Log.d(
                        "changePaint",
                        "changePaint updateBrushSize2 ${(heightLandscape * percent) / heightPortrait}"
                    )
                }else
                    editor_draw_canvas.updateBrushSize(percent)
            } else {
                heightPortrait = editor_draw_canvas.measuredHeight
                editor_draw_canvas.updateBrushSize(percent)
                Log.d(
                    "changePaint",
                    "changePaint updateBrushSize2 $percent"
                )
            }
        }
        val scale = max(0.03f, percent / 100f)
        bottom_draw_color.scaleX = scale
        bottom_draw_color.scaleY = scale
    }

    private fun updatePrimaryActionButtons() {
        if (crop_image_view.isGone() && currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            loadCropImageView(bitmap = editor_draw_canvas.getBitmap(),rectNew = rect)
        } else if (default_image_view.isGone() && currPrimaryAction == PRIMARY_ACTION_FILTER) {
            loadDefaultImageView()
        } else if (editor_draw_canvas.isInvisible() && currPrimaryAction == PRIMARY_ACTION_DRAW) {
            loadDrawCanvas()
        }

        arrayOf(bottom_primary_filter, bottom_primary_crop_rotate, bottom_primary_draw).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val currentPrimaryActionButton = when (currPrimaryAction) {
            PRIMARY_ACTION_FILTER -> bottom_primary_filter
            PRIMARY_ACTION_CROP_ROTATE -> bottom_primary_crop_rotate
            PRIMARY_ACTION_DRAW -> bottom_primary_draw
            else -> null
        }

        currentPrimaryActionButton?.applyColorFilter(getAdjustedPrimaryColor())
        bottom_editor_filter_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_FILTER)
        bottom_editor_crop_rotate_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE)
        bottom_editor_draw_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_DRAW)

        if (currPrimaryAction == PRIMARY_ACTION_FILTER && bottom_actions_filter_list.adapter == null) {
            ensureBackgroundThread {
                val thumbnailSize =
                    resources.getDimension(R.dimen.bottom_filters_thumbnail_size).toInt()

                val bitmap = try {
                    Glide.with(this)
                        .asBitmap()
                        .load(uri).listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                showErrorToast(e.toString())
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ) = false
                        })
                        .submit(thumbnailSize, thumbnailSize)
                        .get()
                } catch (e: GlideException) {
                    showErrorToast(e)
                    finish()
                    return@ensureBackgroundThread
                }

                runOnUiThread {
                    val filterThumbnailsManager = FilterThumbnailsManager()
                    filterThumbnailsManager.clearThumbs()

                    val noFilter = Filter(getString(R.string.none))
                    filterThumbnailsManager.addThumb(FilterItem(bitmap, noFilter))

                    FilterPack.getFilterPack(this).forEach {
                        val filterItem = FilterItem(bitmap, it)
                        filterThumbnailsManager.addThumb(filterItem)
                    }

                    val filterItems = filterThumbnailsManager.processThumbs()
                    val adapter = FiltersAdapter(applicationContext, filterItems) {
                        val layoutManager =
                            bottom_actions_filter_list.layoutManager as LinearLayoutManager
                        getFiltersAdapter()?.getCurrentFilter()?.let { currentFilter ->
                            if (currentFilter.filter.name != getString(R.string.none)) {
                                applyFilter(currentFilter)
                            }else{
                                default_image_view.setImageBitmap(filterInitialBitmap)
                            }
                        }
                        if (it == layoutManager.findLastCompletelyVisibleItemPosition() || it == layoutManager.findLastVisibleItemPosition()) {
                            bottom_actions_filter_list.smoothScrollBy(thumbnailSize, 0)
                        } else if (it == layoutManager.findFirstCompletelyVisibleItemPosition() || it == layoutManager.findFirstVisibleItemPosition()) {
                            bottom_actions_filter_list.smoothScrollBy(-thumbnailSize, 0)
                        }
                    }

                    bottom_actions_filter_list.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }
        }

        if (currPrimaryAction != PRIMARY_ACTION_CROP_ROTATE) {
            bottom_aspect_ratios.beGone()
            currCropRotateAction = CROP_ROTATE_NONE
        }
        updateCropRotateActionButtons()
    }

    private fun applyFilter(filterItem: FilterItem) {
        val newBitmap = Bitmap.createBitmap(filterInitialBitmap!!)
        default_image_view.setImageBitmap(filterItem.filter.processFilter(newBitmap))
    }

    private fun updateAspectRatio(aspectRatio: Int) {
        currAspectRatio = aspectRatio
        config.lastEditorCropAspectRatio = aspectRatio
        updateAspectRatioButtons()

        crop_image_view.apply {
            if (aspectRatio == ASPECT_RATIO_FREE) {
                setFixedAspectRatio(false)
            } else {
                val newAspectRatio = when (aspectRatio) {
                    ASPECT_RATIO_ONE_ONE -> Pair(1f, 1f)
                    ASPECT_RATIO_FOUR_THREE -> Pair(4f, 3f)
                    ASPECT_RATIO_SIXTEEN_NINE -> Pair(16f, 9f)
                    else -> Pair(lastOtherAspectRatio!!.first, lastOtherAspectRatio!!.second)
                }

                setAspectRatio(newAspectRatio.first.toInt(), newAspectRatio.second.toInt())
            }
        }
    }

    private fun updateAspectRatioButtons() {
        arrayOf(
            bottom_aspect_ratio_free,
            bottom_aspect_ratio_one_one,
            bottom_aspect_ratio_four_three,
            bottom_aspect_ratio_sixteen_nine,
            bottom_aspect_ratio_other
        ).forEach {
            it.setTextColor(Color.WHITE)
        }

        val currentAspectRatioButton = when (currAspectRatio) {
            ASPECT_RATIO_FREE -> bottom_aspect_ratio_free
            ASPECT_RATIO_ONE_ONE -> bottom_aspect_ratio_one_one
            ASPECT_RATIO_FOUR_THREE -> bottom_aspect_ratio_four_three
            ASPECT_RATIO_SIXTEEN_NINE -> bottom_aspect_ratio_sixteen_nine
            else -> bottom_aspect_ratio_other
        }

        currentAspectRatioButton.setTextColor(getAdjustedPrimaryColor())
    }

    private fun updateCropRotateActionButtons() {
        arrayOf(bottom_aspect_ratio).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val primaryActionView = when (currCropRotateAction) {
            CROP_ROTATE_ASPECT_RATIO -> bottom_aspect_ratio
            else -> null
        }

        primaryActionView?.applyColorFilter(getAdjustedPrimaryColor())
    }

    private fun updateDrawColor(color: Int) {
        drawColor = color
        bottom_draw_color.applyColorFilter(color)
        config.lastEditorDrawColor = color
        editor_draw_canvas.updateColor(color)
    }

    private fun resizeImage() {
        val point = getAreaSize()
        if (point == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ResizeDialog(this, point) {
            resizeWidth = it.x
            resizeHeight = it.y
            crop_image_view.getCroppedImageAsync()
        }
    }

    private fun shouldCropSquare(): Boolean {
        val extras = intent.extras
        return if (extras != null && extras.containsKey(ASPECT_X) && extras.containsKey(ASPECT_Y)) {
            extras.getInt(ASPECT_X) == extras.getInt(ASPECT_Y)
        } else {
            false
        }
    }

    private fun getAreaSize(): Point? {
        val rect = crop_image_view.cropRect ?: return null
        val rotation = crop_image_view.rotatedDegrees
        return if (rotation == 0 || rotation == 180) {
            Point(rect.width(), rect.height())
        } else {
            Point(rect.height(), rect.width())
        }
    }

    override fun onCropImageComplete(
        view: CropImageView,
        result: CropImageView.CropResult
    ) {
        if (result.error == null) {
            setOldExif()

            afterCroppedSaveClickBitmap = result.bitmap
            afterCroppedSaveClickBitmap?.let { bitmap ->
                if (isSharingBitmap) {
                    isSharingBitmap = false
                    shareBitmap(bitmap)
                    return
                }

                if (isCropIntent) {
                    if (saveUri.scheme == "file") {
                        saveBitmapToFile(bitmap, saveUri.path!!, true)
                    } else {
                        var inputStream: InputStream? = null
                        var outputStream: OutputStream? = null
                        try {
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(CompressFormat.JPEG, 100, stream)
                            inputStream = ByteArrayInputStream(stream.toByteArray())
                            outputStream = contentResolver.openOutputStream(saveUri)
                            inputStream.copyTo(outputStream!!)
                        } finally {
                            inputStream?.close()
                            outputStream?.close()
                        }

                        Intent().apply {
                            data = saveUri
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setResult(RESULT_OK, this)
                        }
                        finish()
                    }
                } else if (saveUri.scheme == "file") {
                    SaveAsDialogFragment.newInstanceAndShow(this, savePath, saveUri.path!!, true) {
                        saveBitmapToFile(bitmap, it, true)
                    }
                } else if (saveUri.scheme == "content") {
                    val filePathGetter = getNewFilePath()
                    SaveAsDialogFragment.newInstanceAndShow(
                        this,
                        savePath,
                        filePathGetter.first,
                        filePathGetter.second
                    ) {
                        saveBitmapToFile(bitmap, it, true)
                    }
                } else {
                    toast(R.string.unknown_file_location)
                }
            }

        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    private fun getNewFilePath(): Pair<String, Boolean> {
        var newPath = applicationContext.getRealPathFromURI(saveUri) ?: ""
        if (newPath.startsWith("/mnt/")) {
            newPath = ""
        }

        var shouldAppendFilename = true
        if (newPath.isEmpty()) {
            val filename = applicationContext.getFilenameFromContentUri(saveUri) ?: ""
            if (filename.isNotEmpty()) {
                val path =
                    if (intent.extras?.containsKey(REAL_FILE_PATH) == true) intent.getStringExtra(
                        REAL_FILE_PATH
                    )?.getParentPath() else internalStoragePath
                newPath = "$path/$filename"
                shouldAppendFilename = false
            }
        }

        if (newPath.isEmpty()) {
            newPath = "$internalStoragePath/${getCurrentFormattedDateTime()}.${
                saveUri.toString().getFilenameExtension()
            }"
            shouldAppendFilename = false
        }

        return Pair(newPath, shouldAppendFilename)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, path: String, showSavingToast: Boolean) {
        try {
            ensureBackgroundThread {
                val file = File(path)
                val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    if (it != null) {
                        saveBitmap(file, bitmap, it, showSavingToast)
                    } else {
                        toast(R.string.image_editing_failed)
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveBitmap(
        file: File,
        bitmap: Bitmap,
        out: OutputStream,
        showSavingToast: Boolean
    ) {
        if (showSavingToast) {
            toast(R.string.saving)
        }

        if (resizeWidth > 0 && resizeHeight > 0) {
            val resized = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
            resized.compress(file.absolutePath.getCompressionFormat(), 90, out)
        } else {
            bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)
        }

        try {
            if (isNougatPlus()) {
                val newExif = ExifInterface(file.absolutePath)
                oldExif?.copyNonDimensionAttributesTo(newExif)
            }
        } catch (e: Exception) {
        }

        out.close()
        scanFinalPath(file.absolutePath)
    }

    private fun editWith() {
        openEditor(uri.toString(), true)
        isEditingWithThirdParty = true
    }

    private fun scanFinalPath(path: String) {
        rescanPath(path) {
            toast(R.string.file_saved)
            val result = Intent(intent).apply {
                putExtra(RESULT_ORIGINAL_URI, originalUri?.toString())
                putExtra(RESULT_SAVE_URI, Uri.fromFile(File(path)).toString())
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun getFilePickerConfig() = ArkFilePickerConfig(
        mode = ArkFilePickerMode.FILE,
        titleStringId = R.string.pick_image
    )

    private fun Activity.openEditor(path: String, forceChooser: Boolean = false) {
        val newPath = path.removePrefix("file://")
        openEditorIntent(newPath, forceChooser, BuildConfig.APPLICATION_ID)
    }

    private val Context.config: Config get() = Config.newInstance(applicationContext)
}

fun CropImageView.isCropAreaChanged(): Boolean {
    return (cropRect?.width() != 0 && cropRect?.height() != 0)
        && (cropRect?.width() != wholeImageRect.width()
        || cropRect?.height() != wholeImageRect.height())
}
