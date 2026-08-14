package com.faceaccess.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Chu co lon hon mac dinh Material3 vi doi tuong dung chinh la nguoi han che
 * van dong tay, can doc ro tu xa khi khong the cham/zoom man hinh.
 */
val FaceAccessTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
)
