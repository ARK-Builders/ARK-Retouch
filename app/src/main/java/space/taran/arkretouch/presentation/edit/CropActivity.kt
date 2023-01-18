package space.taran.arkretouch.presentation.edit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.parcelable
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import space.taran.arkretouch.R
import space.taran.arkretouch.databinding.ActivityCropBinding

class CropActivity : CropImageActivity() {

    private lateinit var binding: ActivityCropBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        binding.apply {
            setContentView(root)
            setSupportActionBar(toolbar)
            setCropImageView(ivCrop)
            ivCrop.apply {
                val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
                val imageUri: Uri? = bundle
                    ?.parcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
                val cropImageOptions: CropImageOptions = bundle
                    ?.parcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)!!
                setImageUriAsync(imageUri)
                setImageCropOptions(cropImageOptions)
            }
        }
        initCropRatioListeners()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initCropRatioListeners() {

        fun changeAspectRatioButtonColor(view: View) {
            @ColorInt
            val activeAspectRatioColor = getColor(R.color.purple_200)
            @ColorInt
            val textColor = getColor(R.color.white)
            val textColorStateList = ColorStateList.valueOf(textColor)
            val activeAspectRatioColorStateList = ColorStateList.valueOf(
                activeAspectRatioColor
            )
            val tv = view as TextView
            val aspectRatioButtons = arrayOf(
                binding.tvCropFree,
                binding.tvCropSquare,
                binding.tvCrop169,
                binding.tvCrop32,
                binding.tvCrop54
            )
            val tintList = tv.compoundDrawableTintList
            if (tintList != activeAspectRatioColorStateList) {
                tv.compoundDrawableTintList = activeAspectRatioColorStateList
                tv.setTextColor(activeAspectRatioColor)
            }
            aspectRatioButtons.filter {
                it != tv
            }.forEach {
                it.compoundDrawableTintList = null
                it.setTextColor(textColorStateList)
            }
        }

        with(binding) {
            tvCropFree.setOnClickListener {
                ivCrop.clearAspectRatio()
                changeAspectRatioButtonColor(it)
            }

            tvCropSquare.setOnClickListener {
                ivCrop.setAspectRatio(
                    AspectRatio.SQUARE.first,
                    AspectRatio.SQUARE.second
                )
                changeAspectRatioButtonColor(it)
            }

            tvCrop169.setOnClickListener {
                ivCrop.setAspectRatio(
                    AspectRatio.SIXTEEN_TO_NINE.first,
                    AspectRatio.SIXTEEN_TO_NINE.second
                )
                changeAspectRatioButtonColor(it)
            }

            tvCrop32.setOnClickListener {
                ivCrop.setAspectRatio(
                    AspectRatio.THREE_TO_TWO.first,
                    AspectRatio.THREE_TO_TWO.second
                )
                changeAspectRatioButtonColor(it)
            }

            tvCrop54.setOnClickListener {
                ivCrop.setAspectRatio(
                    AspectRatio.FIVE_TO_FOUR.first,
                    AspectRatio.FIVE_TO_FOUR.second
                )
                changeAspectRatioButtonColor(it)
            }
        }
    }
}

private object AspectRatio {
    val SQUARE = Pair(1, 1)
    val SIXTEEN_TO_NINE = Pair(16, 9)
    val THREE_TO_TWO = Pair(3, 2)
    val FIVE_TO_FOUR = Pair(5, 4)
}

object CropHelper {
    fun launchCropActivity(
        cropLauncher: ActivityResultLauncher<CropImageContractOptions>,
        uri: Uri
    ) {
        cropLauncher.launch(
            CropImageContractOptions(
                uri,
                CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    outputCompressFormat = Bitmap.CompressFormat.PNG,
                    outputCompressQuality = 100,
                    cropMenuCropButtonIcon = R.drawable.ic_check,
                    activityMenuIconColor = R.color.white,
                    allowRotation = false,
                    allowFlipping = false,
                    showProgressBar = false
                )
            )
        )
    }
}

class CropContract :
    ActivityResultContract
    <CropImageContractOptions, CropImageView.CropResult>() {
    override fun createIntent(
        context: Context,
        input: CropImageContractOptions
    ): Intent {
        val intent = Intent(context, CropActivity::class.java).apply {
            putExtra(
                CropImage.CROP_IMAGE_EXTRA_BUNDLE,
                Bundle().apply {
                    putParcelable(
                        CropImage.CROP_IMAGE_EXTRA_SOURCE,
                        input.uri
                    )
                    putParcelable(
                        CropImage.CROP_IMAGE_EXTRA_OPTIONS,
                        input.cropImageOptions
                    )
                }
            )
        }
        return intent
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): CropImageView.CropResult {
        val result = intent?.parcelable<CropImage.ActivityResult>(
            CropImage.CROP_IMAGE_EXTRA_RESULT
        )

        return if (result == null || resultCode == Activity.RESULT_CANCELED) {
            CropImage.CancelledResult
        } else {
            result
        }
    }
}
