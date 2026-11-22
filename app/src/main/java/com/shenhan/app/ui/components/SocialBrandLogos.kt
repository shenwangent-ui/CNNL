package com.shenhan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GoogleBrandLogo(size: Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.21f
        val inset = strokeWidth / 2f
        val arcSize = Size(w - strokeWidth, h - strokeWidth)

        // น้ำเงิน (ขวา + แถบขวางตัว G)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -15f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // เขียว (ล่าง)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // เหลือง (ซ้าย)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // แดง (บน)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 215f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // ขีดกลางตัว G สีน้ำเงิน
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(w * 0.50f, h * 0.41f),
            size = Size(w * 0.45f, strokeWidth * 0.92f)
        )
    }
}

@Composable
fun LineBrandLogo(size: Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // กรอบสีเขียว LINE
        drawRoundRect(
            color = Color(0xFF06C755),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
        )
        // วงรีแชทสีขาว
        drawOval(
            color = Color.White,
            topLeft = Offset(w * 0.14f, h * 0.18f),
            size = Size(w * 0.72f, h * 0.54f)
        )
        // หางแชทสีขาว
        val tail = Path().apply {
            moveTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.36f, h * 0.84f)
            lineTo(w * 0.58f, h * 0.68f)
            close()
        }
        drawPath(tail, color = Color.White)

        // ตัวอักษร L I N E สีเขียวด้านใน
        val c = Color(0xFF06C755)
        val t = w * 0.045f
        val topY = h * 0.36f
        val botY = h * 0.54f
        // L
        drawLine(c, Offset(w * 0.25f, topY), Offset(w * 0.25f, botY), strokeWidth = t)
        drawLine(c, Offset(w * 0.25f - t / 2, botY), Offset(w * 0.34f, botY), strokeWidth = t)
        // I
        drawLine(c, Offset(w * 0.40f, topY), Offset(w * 0.40f, botY), strokeWidth = t)
        // N
        drawLine(c, Offset(w * 0.47f, topY), Offset(w * 0.47f, botY), strokeWidth = t)
        drawLine(c, Offset(w * 0.47f, topY), Offset(w * 0.58f, botY), strokeWidth = t)
        drawLine(c, Offset(w * 0.58f, topY), Offset(w * 0.58f, botY), strokeWidth = t)
        // E
        drawLine(c, Offset(w * 0.65f, topY), Offset(w * 0.65f, botY), strokeWidth = t)
        drawLine(c, Offset(w * 0.65f, topY), Offset(w * 0.75f, topY), strokeWidth = t)
        drawLine(c, Offset(w * 0.65f, (topY + botY) / 2f), Offset(w * 0.73f, (topY + botY) / 2f), strokeWidth = t)
        drawLine(c, Offset(w * 0.65f, botY), Offset(w * 0.75f, botY), strokeWidth = t)
    }
}

@Composable
fun WeChatBrandLogo(size: Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // กรอบสีเขียว WeChat
        drawRoundRect(
            color = Color(0xFF07C160),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
        )
        // ฟองแชทใหญ่ด้านซ้ายบน
        drawOval(
            color = Color.White,
            topLeft = Offset(w * 0.13f, h * 0.18f),
            size = Size(w * 0.52f, h * 0.42f)
        )
        val tail1 = Path().apply {
            moveTo(w * 0.23f, h * 0.55f)
            lineTo(w * 0.18f, h * 0.68f)
            lineTo(w * 0.34f, h * 0.57f)
            close()
        }
        drawPath(tail1, color = Color.White)

        // ฟองแชทเล็กด้านขวาล่าง
        drawOval(
            color = Color.White,
            topLeft = Offset(w * 0.42f, h * 0.42f),
            size = Size(w * 0.45f, h * 0.36f)
        )
        val tail2 = Path().apply {
            moveTo(w * 0.72f, h * 0.74f)
            lineTo(w * 0.79f, h * 0.85f)
            lineTo(w * 0.62f, h * 0.75f)
            close()
        }
        drawPath(tail2, color = Color.White)

        // จุดตา 2 จุดในแต่ละฟองแชท
        val eyeColor = Color(0xFF07C160)
        drawCircle(eyeColor, radius = w * 0.032f, center = Offset(w * 0.31f, h * 0.34f))
        drawCircle(eyeColor, radius = w * 0.032f, center = Offset(w * 0.46f, h * 0.34f))
        drawCircle(eyeColor, radius = w * 0.026f, center = Offset(w * 0.57f, h * 0.55f))
        drawCircle(eyeColor, radius = w * 0.026f, center = Offset(w * 0.70f, h * 0.55f))
    }
}
