package com.shenhan.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenhan.app.R
import com.shenhan.app.model.SubscriptionTier
import com.shenhan.app.ui.theme.*

@Composable
fun ShenHanTopAppBar(
    currentLevel: Int,
    streakDays: Int,
    isTraditional: Boolean,
    subscriptionTier: SubscriptionTier,
    onToggleScript: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenHumanGates: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FrostedGlassWhite80,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ฝั่งซ้าย: โลโก้ + ชื่อแอป (ยืดหยุ่นตามพื้นที่จอ ไม่ดันปุ่มขวาตกขอบ)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon_1788077123859),
                    contentDescription = "ShenHan App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clipToBounds()
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "神漢",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FrostedPurplePrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ShenHan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HSK LEVEL $currentLevel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrostedPurplePrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "  • Live-Talk",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostedSlateMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ฝั่งขวา: ปุ่มสลับอักษรย่อ/เต็ม + Streak + ข้อมูล Roadmap
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(10.dp))
                    .clickable { onToggleScript() }
                    .testTag("script_toggle_button"),
                color = FrostedPurpleContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Toggle Script",
                        tint = FrostedPurplePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTraditional) "繁⇄简" else "简⇄繁",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FrostedPurplePrimary,
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(10.dp))
                    .clickable { onOpenSubscription() },
                color = FrostedGlassWhite80
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Daily Streak",
                        tint = FrostedPitchAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$streakDays",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = FrostedPitchAmber,
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onOpenHumanGates,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, FrostedGlassBorder, CircleShape)
                    .background(FrostedGlassWhite80)
                    .testTag("human_gates_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "Project TOR Roadmap & Human Gates",
                    tint = FrostedPurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
