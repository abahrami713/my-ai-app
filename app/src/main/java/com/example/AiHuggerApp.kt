package com.example

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiHuggerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State bindings
    val showSplash by viewModel.showSplash.collectAsState()
    val imageUri1 by viewModel.imageUri1.collectAsState()
    val imageUri2 by viewModel.imageUri2.collectAsState()
    val bitmap1 by viewModel.bitmap1.collectAsState()
    val bitmap2 by viewModel.bitmap2.collectAsState()

    // Filter bindings for Photo 1 & 2
    val b1 by viewModel.brightness1.collectAsState()
    val c1 by viewModel.contrast1.collectAsState()
    val s1 by viewModel.saturation1.collectAsState()
    val w1 by viewModel.warmth1.collectAsState()
    val preset1 by viewModel.preset1.collectAsState()

    val b2 by viewModel.brightness2.collectAsState()
    val c2 by viewModel.contrast2.collectAsState()
    val s2 by viewModel.saturation2.collectAsState()
    val w2 by viewModel.warmth2.collectAsState()
    val preset2 by viewModel.preset2.collectAsState()

    // Blend State bindings
    val blendStyle by viewModel.blendStyle.collectAsState()
    val blendMix by viewModel.blendMix.collectAsState()
    val customPrompt by viewModel.customPrompt.collectAsState()

    // API state bindings
    val apiState by viewModel.apiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val aiStoryTitle by viewModel.aiStoryTitle.collectAsState()
    val aiStoryContent by viewModel.aiStoryContent.collectAsState()
    val aiPoem by viewModel.aiPoem.collectAsState()

    val exportState by viewModel.exportState.collectAsState()

    // UI Interactive Selection tab - "خانه" (Home) or "تنظیمات فیلتر" (Filters) or "آغوش هوش مصنوعی" (AI Hug Page)
    var currentTab by remember { mutableStateOf(0) } // 0: Home, 1: Edit Photo 1, 2: Edit Photo 2, 3: Canvas / AI Result
    var showPresetDialog by remember { mutableStateOf(false) }
    var activeEditingPhoto by remember { mutableStateOf(1) } // 1 or 2

    // Toast triggered when export or save status updates
    LaunchedEffect(exportState) {
        exportState?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.setRatingStatus(null)
        }
    }

    // Modern Photo Picker Launchers
    val pickerLauncher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.loadPhoto(context, it, true) }
    }

    val pickerLauncher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.loadPhoto(context, it, false) }
    }

    // Material 3 Color Theme (Warm Night Rose Vibe)
    val darkBackground = Color(0xFF0F0B13)
    val cardBackground = Color(0xFF1B1424)
    val accentPink = Color(0xFFFF4D80)
    val accentGold = Color(0xFFFFD166)
    val textLight = Color(0xFFF0E6F5)
    val textMuted = Color(0xFFB5A8C0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        // Main Screen Interface Scaffolding
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                1 -> "تنظیمات عکس اول"
                                2 -> "تنظیمات عکس دوم"
                                3 -> "خلق آغوش هوش مصنوعی 💖"
                                else -> "آغوش هوش مصنوعی"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textLight,
                            fontFamily = FontFamily.SansSerif
                        )
                    },
                    navigationIcon = {
                        if (currentTab != 0) {
                            IconButton(onClick = { currentTab = 0 }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت",
                                    tint = textLight
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = darkBackground,
                        titleContentColor = textLight
                    )
                )
            },
            bottomBar = {
                if (currentTab == 0 && !showSplash) {
                    NavigationBar(
                        containerColor = cardBackground,
                        tonalElevation = 8.dp,
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        NavigationBarItem(
                            selected = true,
                            onClick = { },
                            icon = { Icon(Icons.Default.Home, contentDescription = "خانه", tint = accentPink) },
                            label = { Text("خانه", color = textLight) }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                if (bitmap1 != null && bitmap2 != null) {
                                    currentTab = 3
                                } else {
                                    Toast.makeText(context, "لطفاً ابتدا هر دو عکس را اضافه کنید", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "آغوش") },
                            label = { Text("بوم آغوش", color = textMuted) }
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentTab) {
                    0 -> { // --- MAIN HUD SCREEN ---
                        // Title / Greeting Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val heartAnimate = rememberInfiniteTransition()
                                val scaleHeart by heartAnimate.animateFloat(
                                    initialValue = 0.95f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "ضربان عشق",
                                    tint = accentPink,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .scale(scaleHeart)
                                        .padding(bottom = 12.dp)
                                )

                                Text(
                                    text = "پیوند احساسی تصاویر با جادوی هنر",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textLight,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "عکس‌های گالری خود را با افکت‌های رنگی فوق‌العاده تنظیم کنید و با هوش مصنوعی در آغوش هم بگذارید.",
                                    fontSize = 13.sp,
                                    color = textMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Left/Right Photo Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Slot 1
                            Box(modifier = Modifier.weight(1f)) {
                                PhotoPickCard(
                                    label = "عکس اول 📸",
                                    bitmap = bitmap1,
                                    colorMatrix = createAdjustedColorMatrix(b1, c1, s1, w1, preset1),
                                    onPickClick = {
                                        pickerLauncher1.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onEditClick = {
                                        activeEditingPhoto = 1
                                        currentTab = 1
                                    },
                                    accentColor = accentPink,
                                    cardBg = cardBackground,
                                    textLight = textLight,
                                    textMuted = textMuted,
                                    testTag = "photo_slot_1"
                                )
                            }

                            // Slot 2
                            Box(modifier = Modifier.weight(1f)) {
                                PhotoPickCard(
                                    label = "عکس دوم 📸",
                                    bitmap = bitmap2,
                                    colorMatrix = createAdjustedColorMatrix(b2, c2, s2, w2, preset2),
                                    onPickClick = {
                                        pickerLauncher2.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onEditClick = {
                                        activeEditingPhoto = 2
                                        currentTab = 2
                                    },
                                    accentColor = accentGold,
                                    cardBg = cardBackground,
                                    textLight = textLight,
                                    textMuted = textMuted,
                                    testTag = "photo_slot_2"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Special Prompt Area
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        "افزودن پیام خاص به پیوند هوش مصنوعی",
                                        fontWeight = FontWeight.Bold,
                                        color = textLight,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = accentPink, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customPrompt,
                                    onValueChange = { viewModel.setCustomPrompt(it) },
                                    placeholder = {
                                        Text(
                                            "مثال: با تم ساحل غروب، رمانتیک، غلیظ...",
                                            color = textMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_prompt_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textLight,
                                        unfocusedTextColor = textLight,
                                        focusedBorderColor = accentPink,
                                        unfocusedBorderColor = textMuted.copy(alpha = 0.5f)
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Start AI Bonding Button
                        Button(
                            onClick = {
                                if (bitmap1 != null && bitmap2 != null) {
                                    currentTab = 3
                                    viewModel.generateAiHugDescription()
                                } else {
                                    Toast.makeText(context, "ابتدا باید هر دو عکس را از گالری اضافه کنید", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(28.dp))
                                .testTag("start_hug_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentPink,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "✨ در آغوش کشیدن با هوش مصنوعی ✨",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    1, 2 -> { // --- FILTER AND ENHANCEMENT EDIT LAYOUTS ---
                        val isPhoto1 = currentTab == 1
                        val bitmap = if (isPhoto1) bitmap1 else bitmap2
                        
                        // Active color Matrix properties
                        val bState = if (isPhoto1) b1 else b2
                        val cState = if (isPhoto1) c1 else c2
                        val sState = if (isPhoto1) s1 else s2
                        val wState = if (isPhoto1) w1 else w2
                        val pState = if (isPhoto1) preset1 else preset2

                        val activeAccent = if (isPhoto1) accentPink else accentGold

                        if (bitmap != null) {
                            // Render modified live Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(cardBackground)
                                    .border(1.dp, activeAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // GPU Matrix color transformation in real-time
                                Image(
                                    painter = rememberAsyncImagePainter(bitmap),
                                    contentDescription = "پیش‌نمایش افکت‌ها",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.colorMatrix(
                                        createAdjustedColorMatrix(bState, cState, sState, wState, pState)
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .clickable { viewModel.resetFilters(isPhoto1) }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "بازنشانی فیلترها",
                                        tint = textLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Color Preset Selection Pill
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        "فیلتر پیش‌فرض زیبایی 🎨",
                                        fontWeight = FontWeight.Bold,
                                        color = textLight,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "رنگ‌بندی آماده خودکار برای جذاب‌تر کردن بک‌گراند یا چهره‌ها:",
                                        fontSize = 11.sp,
                                        color = textMuted,
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val presets = listOf(
                                        "هیچ‌کدام (None)",
                                        "سپیایی (Sepia)",
                                        "رویایی صورتی (Dreamy Rose)",
                                        "سایبرپانک (Cyberpunk)",
                                        "غم‌انگیز (Noir)"
                                    )

                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presets.forEach { presetItem ->
                                            val isSelected = pState == presetItem
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(if (isSelected) activeAccent else cardBackground.copy(alpha = 0.6f))
                                                    .border(1.dp, if (isSelected) Color.Transparent else textMuted.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                                    .clickable {
                                                        if (isPhoto1) viewModel.preset1.value = presetItem
                                                        else viewModel.preset2.value = presetItem
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = presetItem,
                                                    color = if (isSelected) Color.White else textLight,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Manual Filter Sliders
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    // Header
                                    Text(
                                        text = "تنظیمات پیشرفته رنگ و نور 🎛️",
                                        fontWeight = FontWeight.Bold,
                                        color = textLight,
                                        fontSize = 15.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Brightness Slider
                                    SliderWithLabel(
                                        label = "روشنایی (Brightness)",
                                        value = bState,
                                        valueRange = -0.5f..0.5f,
                                        onValueChange = { newVal ->
                                            if (isPhoto1) viewModel.brightness1.value = newVal
                                            else viewModel.brightness2.value = newVal
                                        },
                                        activeColor = activeAccent,
                                        textLight = textLight
                                    )

                                    // Contrast Slider
                                    SliderWithLabel(
                                        label = "کنتراست (Contrast)",
                                        value = cState,
                                        valueRange = 0.5f..2.0f,
                                        onValueChange = { newVal ->
                                            if (isPhoto1) viewModel.contrast1.value = newVal
                                            else viewModel.contrast2.value = newVal
                                        },
                                        activeColor = activeAccent,
                                        textLight = textLight
                                    )

                                    // Saturation Slider
                                    SliderWithLabel(
                                        label = "غلظت رنگ (Saturation)",
                                        value = sState,
                                        valueRange = 0.0f..2.0f,
                                        onValueChange = { newVal ->
                                            if (isPhoto1) viewModel.saturation1.value = newVal
                                            else viewModel.saturation2.value = newVal
                                        },
                                        activeColor = activeAccent,
                                        textLight = textLight
                                    )

                                    // Warmth Slider
                                    SliderWithLabel(
                                        label = "گرمای رنگ (Warmth)",
                                        value = wState,
                                        valueRange = -0.5f..0.5f,
                                        onValueChange = { newVal ->
                                            if (isPhoto1) viewModel.warmth1.value = newVal
                                            else viewModel.warmth2.value = newVal
                                        },
                                        activeColor = activeAccent,
                                        textLight = textLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { currentTab = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = activeAccent),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("اعمال و اتمام کار 👌", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("عکسی یافت نشد به صفحه خانه برگردید.", color = textMuted, modifier = Modifier.padding(20.dp))
                        }
                    }

                    3 -> { // --- AI CANVAS & HUG RESULT VIEW SCREEN ---
                        if (bitmap1 != null && bitmap2 != null) {
                            // Styles choices
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        "۱. انتخاب سبک پیوند و آغوش 🔮",
                                        fontWeight = FontWeight.Bold,
                                        color = textLight,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            BlendStyle.DOUBLE_EXPOSURE to "آغوش رویایی 🌌",
                                            BlendStyle.HEART_MASK to "پیوند قلبی ❤️",
                                            BlendStyle.SOFT_GRADIENT to "ادغام بیانی 🌊",
                                            BlendStyle.POLAROID to "قاب پولاروید 🎞️"
                                        ).forEach { (style, name) ->
                                            val isSelected = blendStyle == style
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(if (isSelected) accentPink else cardBackground)
                                                    .border(1.dp, if (isSelected) Color.Transparent else textMuted.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                                    .clickable { viewModel.setBlendStyle(style) }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    name,
                                                    color = if (isSelected) Color.White else textLight,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Blend Mix opacity level
                                    if (blendStyle != BlendStyle.POLAROID) {
                                        Text(
                                            "میزان ادغام تصاویر (${(blendMix*100).toInt()}%):",
                                            fontSize = 11.sp,
                                            color = textMuted,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Slider(
                                            value = blendMix,
                                            onValueChange = { viewModel.setBlendMix(it) },
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentPink,
                                                activeTrackColor = accentPink,
                                                inactiveTrackColor = textMuted.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Rendering dynamic blended graphics Canvas
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val matrix1 = createAdjustedColorMatrix(b1, c1, s1, w1, preset1)
                                    val matrix2 = createAdjustedColorMatrix(b2, c2, s2, w2, preset2)

                                    // Beautiful Custom Draw Canvas doing CPU-Accelerated Blending
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("composite_image")
                                    ) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height

                                        when (blendStyle) {
                                            BlendStyle.DOUBLE_EXPOSURE -> {
                                                // Photo 1 layer base
                                                drawImage(
                                                    image = bitmap1!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix1),
                                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                                )
                                                // Photo 2 layer merged via blend Screen mode
                                                drawImage(
                                                    image = bitmap2!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix2),
                                                    blendMode = BlendMode.Screen,
                                                    alpha = blendMix,
                                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                                )
                                            }
                                            BlendStyle.HEART_MASK -> {
                                                // Layer base Photo 1
                                                drawImage(
                                                    image = bitmap1!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix1),
                                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                                )

                                                // Complex heart path outline
                                                val heartPath = Path().apply {
                                                    val size = canvasWidth
                                                    moveTo(size / 2, size * 0.25f)
                                                    cubicTo(size * 0.1f, size * 0.05f, size * 0.02f, size * 0.5f, size / 2, size * 0.95f)
                                                    cubicTo(size * 0.98f, size * 0.5f, size * 0.9f, size * 0.05f, size / 2, size * 0.25f)
                                                    close()
                                                }

                                                // Draw masked second picture using heart mask clipping
                                                drawContext.canvas.save()
                                                drawContext.canvas.clipPath(heartPath)
                                                drawImage(
                                                    image = bitmap2!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix2),
                                                    alpha = blendMix,
                                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                                )
                                                drawContext.canvas.restore()

                                                // Glow border
                                                drawPath(
                                                    path = heartPath,
                                                    color = Color(0xFFFF2D55).copy(alpha = 0.8f),
                                                    style = Stroke(width = 8f)
                                                )
                                            }
                                            BlendStyle.SOFT_GRADIENT -> {
                                                // Base photo 1
                                                drawImage(
                                                    image = bitmap1!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix1),
                                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                                )

                                                // Blended overlaying photo 2 using simple custom alpha layer gradient
                                                drawImage(
                                                    image = bitmap2!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix2),
                                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt()),
                                                    alpha = blendMix // uses mix slider
                                                )
                                            }
                                            BlendStyle.POLAROID -> {
                                                // Two classic side by side frames with a beautiful hand-connecting line
                                                val separation = 20f
                                                val padding = 15f
                                                
                                                // Left Frame
                                                drawContext.canvas.save()
                                                val leftWidth = (canvasWidth / 2) - (separation)
                                                val leftHeight = canvasHeight - 60f
                                                
                                                // White polaroid base rect
                                                drawRect(
                                                    color = Color.White,
                                                    topLeft = Offset(separation, 30f),
                                                    size = androidx.compose.ui.geometry.Size(leftWidth, leftHeight)
                                                )
                                                // Render Image A
                                                drawImage(
                                                    image = bitmap1!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix1),
                                                    dstOffset = IntOffset(
                                                        (separation + padding).toInt(),
                                                        (30f + padding).toInt()
                                                    ),
                                                    dstSize = IntSize(
                                                        (leftWidth - (padding * 2)).toInt(),
                                                        (leftHeight - (padding * 4)).toInt()
                                                    )
                                                )
                                                drawContext.canvas.restore()

                                                // Right Frame
                                                drawContext.canvas.save()
                                                val rightLeftOffset = (canvasWidth / 2) + separation
                                                drawRect(
                                                    color = Color.White,
                                                    topLeft = Offset(rightLeftOffset, 30f),
                                                    size = androidx.compose.ui.geometry.Size(leftWidth, leftHeight)
                                                )
                                                drawImage(
                                                    image = bitmap2!!.asImageBitmap(),
                                                    colorFilter = ColorFilter.colorMatrix(matrix2),
                                                    dstOffset = IntOffset(
                                                        (rightLeftOffset + padding).toInt(),
                                                        (30f + padding).toInt()
                                                    ),
                                                    dstSize = IntSize(
                                                        (leftWidth - (padding * 2)).toInt(),
                                                        (leftHeight - (padding * 4)).toInt()
                                                    )
                                                )
                                                drawContext.canvas.restore()

                                                // Linking heart sign in the middle
                                                drawCircle(
                                                    color = Color.Red,
                                                    center = Offset(canvasWidth / 2, canvasHeight / 2),
                                                    radius = 24f
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // AI Story Card
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        "۲. داستان و شعر پیوند با هوش مصنوعی 🖋️",
                                        fontWeight = FontWeight.Bold,
                                        color = textLight,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (apiState) {
                                        ApiState.LOADING -> {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator(color = accentPink)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "در حال تحلیل تصاویر و ترانه‌سرایی عشق توسط جیو پیوندی...",
                                                    color = textMuted,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        ApiState.ERROR -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    errorMessage ?: "خطایی در تحلیل تصاویر رخ داد",
                                                    color = Color.Red,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { viewModel.generateAiHugDescription() },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentPink)
                                            ) {
                                                Text("کوشش دوباره 🔄", color = Color.White)
                                            }
                                        }
                                        else -> {
                                            // Title
                                            Text(
                                                text = aiStoryTitle,
                                                fontWeight = FontWeight.Bold,
                                                color = accentGold,
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Description Story
                                            Text(
                                                text = aiStoryContent,
                                                color = textLight,
                                                fontSize = 13.sp,
                                                lineHeight = 24.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Divider(color = textMuted.copy(alpha = 0.2f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Poetic verse
                                            Text(
                                                text = aiPoem,
                                                color = accentPink,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 26.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // Save Export Button
                            Button(
                                onClick = {
                                    val matrix1 = createAdjustedColorMatrix(b1, c1, s1, w1, preset1)
                                    val matrix2 = createAdjustedColorMatrix(b2, c2, s2, w2, preset2)
                                    viewModel.exportCompositeImage(
                                        context = context,
                                        blendStyle = blendStyle,
                                        mix = blendMix,
                                        bitmap1 = bitmap1!!,
                                        bitmap2 = bitmap2!!,
                                        matrix1 = matrix1,
                                        matrix2 = matrix2
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .shadow(6.dp, RoundedCornerShape(27.dp))
                                    .testTag("export_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = accentPink),
                                shape = RoundedCornerShape(27.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ذخیره در گالری گوشی 💾", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Retry text trigger
                            TextButton(
                                onClick = { viewModel.generateAiHugDescription() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تولید داستان عاشقانه جدید با هوش مصنوعی 💫", color = accentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        // --- AUTH SPLASH OVERLAY POPUP (Made by Ali Bahrami) ---
        // Disappears after 5 seconds gracefully using self-contained launch effect
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(1200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE60A060E)) // Translucent dark
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Blocks taps beneath */ },
                contentAlignment = Alignment.Center
            ) {
                // Heart beat canvas background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val size = size.width
                    // Draw glowing ambient lights
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3DFF2D55), Color.Transparent),
                            center = Offset(size / 2, size / 1.5f),
                            radius = size
                        )
                    )
                }

                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp))
                        .testTag("splash_overlay"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Glowing animated love logo
                        val pulseTransition = rememberInfiniteTransition()
                        val pulseScale by pulseTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "آیکون قلب",
                            tint = accentPink,
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .shadow(8.dp, CircleShape)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "به برنامه آغوش خوش آمدید",
                            fontSize = 16.sp,
                            color = textMuted,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "ساخته شده توسط علی بهرامی",
                            fontSize = 24.sp,
                            color = accentGold,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "پیوند احساسی دو تصویر در کمال لطافت و زیبایی...",
                            fontSize = 12.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        LinearProgressIndicator(
                            color = accentGold,
                            trackColor = textMuted.copy(alpha = 0.15f),
                            modifier = Modifier
                                .width(120.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// Reuseable selector card wrapper
@Composable
fun PhotoPickCard(
    label: String,
    bitmap: Bitmap?,
    colorMatrix: ColorMatrix,
    onPickClick: () -> Unit,
    onEditClick: () -> Unit,
    accentColor: Color,
    cardBg: Color,
    textLight: Color,
    textMuted: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = textLight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            if (bitmap != null) {
                // Display thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(bitmap),
                        contentDescription = "تصویر بارگذاری شده",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = onPickClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = textMuted)
                    ) {
                        Text("تعویض عکس 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("افکت رنگی 🎨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                // Empty Picker state
                val dashedBrush = Brush.sweepGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f)))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg.copy(alpha = 0.5f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onPickClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن عکس",
                            tint = accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "انتخاب عکس از گالری",
                            color = textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Pre-configured slider row component
@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    activeColor: Color,
    textLight: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                String.format("%.1f", value),
                color = activeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                color = textLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = textLight.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
