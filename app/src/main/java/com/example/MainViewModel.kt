package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.ColorMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

enum class BlendStyle {
    DOUBLE_EXPOSURE, // آغوش رویایی
    HEART_MASK,      // پیوند قلبی
    SOFT_GRADIENT,   // ادغام بیانی
    POLAROID         // مهر پولاروید
}

enum class ApiState {
    IDLE, LOADING, SUCCESS, ERROR
}

class MainViewModel : ViewModel() {

    private val _showSplash = MutableStateFlow(true)
    val showSplash = _showSplash.asStateFlow()

    // Photo States
    private val _imageUri1 = MutableStateFlow<Uri?>(null)
    val imageUri1 = _imageUri1.asStateFlow()

    private val _imageUri2 = MutableStateFlow<Uri?>(null)
    val imageUri2 = _imageUri2.asStateFlow()

    private val _bitmap1 = MutableStateFlow<Bitmap?>(null)
    val bitmap1 = _bitmap1.asStateFlow()

    private val _bitmap2 = MutableStateFlow<Bitmap?>(null)
    val bitmap2 = _bitmap2.asStateFlow()

    // Filter Stats - Photo 1
    val brightness1 = MutableStateFlow(0f)      // -0.5f to 0.5f
    val contrast1 = MutableStateFlow(1f)        // 0.5f to 2.0f
    val saturation1 = MutableStateFlow(1f)      // 0.0f to 2.0f
    val warmth1 = MutableStateFlow(0f)          // -0.5f to 0.5f
    val preset1 = MutableStateFlow("هیچ‌کدام (None)")

    // Filter Stats - Photo 2
    val brightness2 = MutableStateFlow(0f)
    val contrast2 = MutableStateFlow(1f)
    val saturation2 = MutableStateFlow(1f)
    val warmth2 = MutableStateFlow(0f)
    val preset2 = MutableStateFlow("هیچ‌کدام (None)")

    // Composite Settings
    private val _blendStyle = MutableStateFlow(BlendStyle.DOUBLE_EXPOSURE)
    val blendStyle = _blendStyle.asStateFlow()

    private val _blendMix = MutableStateFlow(0.5f) // 0f to 1f
    val blendMix = _blendMix.asStateFlow()

    private val _customPrompt = MutableStateFlow("")
    val customPrompt = _customPrompt.asStateFlow()

    // API & Generation State
    private val _apiState = MutableStateFlow(ApiState.IDLE)
    val apiState = _apiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _aiStoryTitle = MutableStateFlow("پیوند دو جهان")
    val aiStoryTitle = _aiStoryTitle.asStateFlow()

    private val _aiStoryContent = MutableStateFlow("منتظر کلیک کردن روی دکمه شروع آغوش هوش مصنوعی...")
    val aiStoryContent = _aiStoryContent.asStateFlow()

    private val _aiPoem = MutableStateFlow("دو تصویر، دو آوای جداگانه...\nدر آغوش هم، یک نغمه همیشگی...")
    val aiPoem = _aiPoem.asStateFlow()

    // Saved export state
    private val _exportState = MutableStateFlow<String?>(null)
    val exportState = _exportState.asStateFlow()

    init {
        // Automatically hide the author splash after exactly 5 seconds
        viewModelScope.launch {
            delay(5000)
            _showSplash.value = false
        }
    }

    fun setBlendStyle(style: BlendStyle) {
        _blendStyle.value = style
    }

    fun setBlendMix(mix: Float) {
        _blendMix.value = mix
    }

    fun setCustomPrompt(prompt: String) {
        _customPrompt.value = prompt
    }

    fun setRatingStatus(message: String?) {
        _exportState.value = message
    }

    fun loadPhoto(context: Context, uri: Uri, isPhoto1: Boolean) {
        viewModelScope.launch {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                // Scale down bitmap to save memory and avoid issues
                val scaled = scaleBitmap(bitmap, 800)
                if (isPhoto1) {
                    _imageUri1.value = uri
                    _bitmap1.value = scaled
                } else {
                    _imageUri2.value = uri
                    _bitmap2.value = scaled
                }
            }
        }
    }

    fun resetFilters(isPhoto1: Boolean) {
        if (isPhoto1) {
            brightness1.value = 0f
            contrast1.value = 1f
            saturation1.value = 1f
            warmth1.value = 0f
            preset1.value = "هیچ‌کدام (None)"
        } else {
            brightness2.value = 0f
            contrast2.value = 1f
            saturation2.value = 1f
            warmth2.value = 0f
            preset2.value = "هیچ‌کدام (None)"
        }
    }

    // Call Gemini API to generate an elegant Persian poetic text and details
    fun generateAiHugDescription() {
        val b1 = _bitmap1.value
        val b2 = _bitmap2.value

        if (b1 == null || b2 == null) {
            _errorMessage.value = "لطفاً ابتدا هر دو عکس را در برنامه اضافه کنید."
            _apiState.value = ApiState.ERROR
            return
        }

        _apiState.value = ApiState.LOADING
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Encode scaled photos to base64
                val base64_1 = withContext(Dispatchers.Default) { b1.toBase64() }
                val base64_2 = withContext(Dispatchers.Default) { b2.toBase64() }

                val geminiKey = BuildConfig.GEMINI_API_KEY
                if (geminiKey.isEmpty() || geminiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("کلید هوش مصنوعی در برنامه تنظیم نشده است. لطفاً GEMINI_API_KEY را در تنظیمات Secrets وارد کنید.")
                }

                val extraPrompt = _customPrompt.value.trim()
                val promptText = """
                    شما یک هوش مصنوعی بسیار هنرمند و توانا با احساسات عمیق رمانتیک و ادبی هستید.
                    کاربر دو تصویر بارگذاری کرده است تا آنها را با یکدیگر پیوند داده و در آغوش هم بگذارد.
                    لطفاً تصاویر را تحلیل کنید و یک داستان احساسی، ادبی و فوق‌العاده شاعرانه به زبان فارسی بنویسید که چگونه این دو سوژه یکدیگر را عمیقاً در آغوش کشیده و یکی شده‌اند.
                    همچنین یک شعر کوتاه (۲ الی ۴ مصراع) بسیار احساسی درباره این پیوند بنویسید.
                    عنوان زیبایی هم به فارسی برای این پیوند انتخاب کنید.
                    ${if (extraPrompt.isNotEmpty()) "درخواست ویژه کاربر برای حال و هوای داستان: $extraPrompt" else ""}
                    
                    باید خروجی شما فقط و فقط به صورت یک متن معتبر با فرمت JSON به شکل زیر باشد و هیچ توضیحی خارج از این JSON نوشته نشود:
                    {
                      "title": "عنوان رمانتیک اثر هنری به فارسی",
                      "story": "داستان ادبی و بسیار عمیق در آغوش گرفتن این دو تصویر به زبان فارسی در چند خط دلنشین",
                      "poem": "شعر کوتاه فارسی متناسب با این آغوش"
                    }
                """.trimIndent()

                val responseJson = callGeminiApi(geminiKey, promptText, base64_1, base64_2)
                
                // Parse results
                parseGeminiResponse(responseJson)

            } catch (e: Exception) {
                Log.e("AiHugger", "Error calling Gemini", e)
                _errorMessage.value = e.localizedMessage ?: "خطایی در برقراری ارتباط با سرور هوش مصنوعی رخ داد."
                _apiState.value = ApiState.ERROR
            }
        }
    }

    private suspend fun callGeminiApi(
        apiKey: String,
        prompt: String,
        base64Image1: String,
        base64Image2: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        // Construct manual JSON body
        val inlineData1 = JSONObject().apply {
            put("mimeType", "image/jpeg")
            put("data", base64Image1)
        }
        val part1 = JSONObject().apply {
            put("inlineData", inlineData1)
        }

        val inlineData2 = JSONObject().apply {
            put("mimeType", "image/jpeg")
            put("data", base64Image2)
        }
        val part2 = JSONObject().apply {
            put("inlineData", inlineData2)
        }

        val textPart = JSONObject().apply {
            put("text", prompt)
        }

        val partsArray = JSONArray().apply {
            put(textPart)
            put(part1)
            put(part2)
        }

        val contentObject = JSONObject().apply {
            put("parts", partsArray)
        }

        val contentsArray = JSONArray().apply {
            put(contentObject)
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("API Error ($response.code): $errorBody")
            }
            response.body?.string() ?: throw IllegalStateException("Empty response from AI")
        }
    }

    private fun parseGeminiResponse(rawResponse: String) {
        try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val partText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // We need to clean potential markdown triple backticks "```json ... ```" from the response text
            var cleanedJson = partText.trim()
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substringAfter("```").substringBeforeLast("```").trim()
            }

            // Parse final inner JSON
            val innerJson = JSONObject(cleanedJson)
            _aiStoryTitle.value = innerJson.optString("title", "پیوند عمیق عاشقانه")
            _aiStoryContent.value = innerJson.optString("story", "داستان دو تصویر که با احساس در آغوش کشیده شدند...")
            _aiPoem.value = innerJson.optString("poem", "در هم آمیخته و یکی گشتند...")
            _apiState.value = ApiState.SUCCESS
        } catch (e: Exception) {
            Log.e("AiHugger", "Error parsing JSON", e)
            // Fallback parsing or use direct text if it wasn't perfect JSON
            _errorMessage.value = "هوش مصنوعی پاسخی نوشت اما فرمت آن استاندارد نبود. پاسخ ذخیره شد."
            _aiStoryTitle.value = "پیوند احساسی دل‌ها"
            _aiStoryContent.value = rawResponse.substringBefore("\n\n")
            _aiPoem.value = "دو قلب، یک آغوش بی‌پایان"
            _apiState.value = ApiState.SUCCESS // Treat as success with graceful recovery
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Export composite image using Canvas rendering offscreen
    fun exportCompositeImage(context: Context, blendStyle: BlendStyle, mix: Float, bitmap1: Bitmap, bitmap2: Bitmap, matrix1: ColorMatrix, matrix2: ColorMatrix) {
        viewModelScope.launch {
            _exportState.value = "در حال ایجاد عکس ترکیبی..."
            try {
                val exportedUri = withContext(Dispatchers.IO) {
                    val width = 1000
                    val height = 1000
                    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(resultBitmap)

                    // Draw Background Base
                    val paintBg = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        this.style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

                    // Helper to create Android ColorMatrix from Compose color matrix values
                    val androidMatrix1 = android.graphics.ColorMatrix(matrix1.values)
                    val androidMatrix2 = android.graphics.ColorMatrix(matrix2.values)

                    val paintImage1 = android.graphics.Paint().apply {
                        colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix1)
                        isAntiAlias = true
                    }
                    val paintImage2 = android.graphics.Paint().apply {
                        colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix2)
                        isAntiAlias = true
                    }

                    // Fit bitmaps to 1000x1000 rectangles
                    val srcRect1 = android.graphics.Rect(0, 0, bitmap1.width, bitmap1.height)
                    val srcRect2 = android.graphics.Rect(0, 0, bitmap2.width, bitmap2.height)

                    when (blendStyle) {
                        BlendStyle.DOUBLE_EXPOSURE -> {
                            val destRect = android.graphics.Rect(0, 0, width, height)
                            canvas.drawBitmap(bitmap1, srcRect1, destRect, paintImage1)
                            
                            // Composite bitmap 2 using blend alpha
                            val doublePaint = android.graphics.Paint().apply {
                                colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix2)
                                isAntiAlias = true
                                alpha = (mix * 255f).toInt()
                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
                            }
                            canvas.drawBitmap(bitmap2, srcRect2, destRect, doublePaint)
                        }
                        BlendStyle.HEART_MASK -> {
                            // Blends both images in a heart shape mask
                            val destRect1 = android.graphics.Rect(0, 0, width, height)
                            canvas.drawBitmap(bitmap1, srcRect1, destRect1, paintImage1)

                            // Apply a beautiful Heart overlay
                            val heartPath = android.graphics.Path().apply {
                                val size = width.toFloat()
                                moveTo(size / 2, size * 0.25f)
                                cubicTo(size * 0.1f, size * 0.05f, size * 0.02f, size * 0.5f, size / 2, size * 0.95f)
                                cubicTo(size * 0.98f, size * 0.5f, size * 0.9f, size * 0.05f, size / 2, size * 0.25f)
                                close()
                            }

                            // Use clipping or path to render Mask
                            canvas.save()
                            canvas.clipPath(heartPath)
                            val heartPaint = android.graphics.Paint().apply {
                                colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix2)
                                isAntiAlias = true
                                alpha = (mix * 255f).toInt()
                            }
                            canvas.drawBitmap(bitmap2, srcRect2, destRect1, heartPaint)
                            canvas.restore()

                            // Draw red glowing border for the heart
                            val borderPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(180, 255, 60, 110)
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 12f
                                isAntiAlias = true
                            }
                            canvas.drawPath(heartPath, borderPaint)
                        }
                        BlendStyle.SOFT_GRADIENT -> {
                            // Draw left half Image 1, right half Image 2 with middle soft gradient
                            val destRect1 = android.graphics.Rect(0, 0, width / 2, height)
                            val destRect2 = android.graphics.Rect(width / 2, 0, width, height)
                            
                            // Let's render both full screen but overlay image 2 with linear shade
                            val fullDest = android.graphics.Rect(0, 0, width, height)
                            canvas.drawBitmap(bitmap1, srcRect1, fullDest, paintImage1)

                            // Use custom gradient mask for bitmap2
                            val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val maskCanvas = Canvas(maskBitmap)
                            val gradPaint = android.graphics.Paint().apply {
                                shader = android.graphics.LinearGradient(
                                    0f, 0f, width.toFloat() * mix, 0f,
                                    android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                                isAntiAlias = true
                            }
                            maskCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradPaint)

                            // Render second image masked
                            val maskedPaint = android.graphics.Paint().apply {
                                colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix2)
                                isAntiAlias = true
                            }
                            canvas.drawBitmap(bitmap2, srcRect2, fullDest, maskedPaint)

                            // Apply composite overlay blend
                            val blendPaint = android.graphics.Paint().apply {
                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                            }
                            canvas.drawBitmap(maskBitmap, 0f, 0f, blendPaint)
                            maskBitmap.recycle()
                        }
                        BlendStyle.POLAROID -> {
                            val border = 40f
                            val polaroidBg = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                style = android.graphics.Paint.Style.FILL
                            }
                            
                            // Image 1 Box
                            val box1 = android.graphics.RectF(border, border, (width / 2f) - (border / 2), height - (border * 3))
                            canvas.drawRect(box1, polaroidBg)
                            val inner1 = android.graphics.Rect(
                                (box1.left + 15).toInt(), 
                                (box1.top + 15).toInt(), 
                                (box1.right - 15).toInt(), 
                                (box1.bottom - 45).toInt()
                            )
                            canvas.drawBitmap(bitmap1, srcRect1, inner1, paintImage1)

                            // Image 2 Box
                            val box2 = android.graphics.RectF((width / 2f) + (border / 2), border, width - border, height - (border * 3))
                            canvas.drawRect(box2, polaroidBg)
                            val inner2 = android.graphics.Rect(
                                (box2.left + 15).toInt(), 
                                (box2.top + 15).toInt(), 
                                (box2.right - 15).toInt(), 
                                (box2.bottom - 45).toInt()
                            )
                            canvas.drawBitmap(bitmap2, srcRect2, inner2, paintImage2)

                            // Draw a love string connecting them
                            val heartPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.RED
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                            }
                            canvas.drawCircle(width / 2f, height / 2.5f, 30f, heartPaint)
                        }
                    }

                    // Save resultBitmap to Gallery
                    saveBitmapToGallery(context, resultBitmap)
                }

                if (exportedUri != null) {
                    _exportState.value = "عکس با موفقیت در گالری ذخیره شد!"
                } else {
                    _exportState.value = "عدم امکان ذخیره عکس در گالری."
                }
            } catch (e: Exception) {
                _exportState.value = "خطا در خروجی گرفتن: ${e.localizedMessage}"
            }
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "AI_HUG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        val resolver = context.contentResolver

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AiHugging")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).toString()
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
                imageUri = Uri.fromFile(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            return imageUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

// Math Matrix Helper for Compose custom filter operations
fun createAdjustedColorMatrix(
    brightness: Float, // -1f to 1f
    contrast: Float,   // 0.5f to 2f
    saturation: Float, // 0f to 2f
    warmth: Float,     // -0.5f to 0.5f
    preset: String     // Filter Preset name
): ColorMatrix {
    val bVal = brightness * 128f // Scale up offset for colors
    val c = contrast

    // ColorMatrix array representation (Row-major 4x5)
    val base = floatArrayOf(
        c, 0f, 0f, 0f, bVal,
        0f, c, 0f, 0f, bVal,
        0f, 0f, c, 0f, bVal,
        0f, 0f, 0f, 1f, 0f
    )

    // Apply Saturation properties
    val lr = 0.213f
    val lg = 0.715f
    val lb = 0.072f
    val s = saturation
    val invS = 1f - s

    val r1 = invS * lr + s
    val g1 = invS * lg
    val b1 = invS * lb

    val r2 = invS * lr
    val g2 = invS * lg + s
    val b2 = invS * lb

    val r3 = invS * lr
    val g3 = invS * lg
    val b3 = invS * lb + s

    // Overlay saturation on top of contrast matrix elements
    val finalMatrix = floatArrayOf(
        r1 * c,      g1 * c,      b1 * c,      0f, bVal,
        r2 * c,      g2 * c,      b2 * c,      0f, bVal,
        r3 * c,      g3 * c,      b3 * c,      0f, bVal,
        0f,          0f,          0f,          1f, 0f
    )

    // Warmth additions
    if (warmth > 0) {
        finalMatrix[4] += warmth * 50f   // Boost Red
        finalMatrix[9] += warmth * 25f   // Boost Green slightly
    } else if (warmth < 0) {
        finalMatrix[14] += (-warmth) * 50f // Boost Blue (cooling)
    }

    // Advanced Filter Presets
    when {
        preset.contains("سپیایی") -> { // Sepia
            val sepia = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            return multiplyFloatMatrices(sepia, finalMatrix)
        }
        preset.contains("صورتی") -> { // Dreamy Rose
            finalMatrix[4] += 50f
            finalMatrix[14] += 50f
        }
        preset.contains("سایبرپانک") -> { // Cyberpunk
            finalMatrix[4] += 60f
            finalMatrix[14] += 120f
            finalMatrix[9] -= 30f
        }
        preset.contains("غم‌انگیز") -> { // Noir Dramatic
            val mono = floatArrayOf(
                0.213f, 0.715f, 0.072f, 0f, 0f,
                0.213f, 0.715f, 0.072f, 0f, 0f,
                0.213f, 0.715f, 0.072f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            val baseMono = multiplyFloatMatrices(mono, finalMatrix)
            // dark filter boost
            baseMono.values[4] -= 30f
            baseMono.values[9] -= 30f
            baseMono.values[14] -= 30f
            return baseMono
        }
    }

    return ColorMatrix(finalMatrix)
}

private fun multiplyFloatMatrices(a: FloatArray, b: FloatArray): ColorMatrix {
    val result = FloatArray(20)
    for (row in 0..3) {
        for (col in 0..4) {
            val idx = row * 5 + col
            if (col == 4) {
                result[idx] = a[row * 5 + 0] * b[0 * 5 + 4] +
                             a[row * 5 + 1] * b[1 * 5 + 4] +
                             a[row * 5 + 2] * b[2 * 5 + 4] +
                             a[row * 5 + 3] * b[3 * 5 + 4] +
                             a[row * 5 + 4]
            } else {
                result[idx] = a[row * 5 + 0] * b[0 * 5 + col] +
                             a[row * 5 + 1] * b[1 * 5 + col] +
                             a[row * 5 + 2] * b[2 * 5 + col] +
                             a[row * 5 + 3] * b[3 * 5 + col]
            }
        }
    }
    return ColorMatrix(result)
}
