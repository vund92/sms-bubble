package com.smsbubble.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsbubble.app.data.BubbleStyle
import com.smsbubble.app.data.ContactUtils
import com.smsbubble.app.data.Conversation

private val AvatarColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF6C00), Color(0xFFAB47BC),
    Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFF8D6E63),
)

/** Avatar tròn với chữ cái đầu. Kiểu iPhone: nền xám chuyển màu; kiểu Android: màu theo tên. */
@Composable
fun Avatar(name: String, size: Dp, style: BubbleStyle, modifier: Modifier = Modifier) {
    val initials = remember(name) { initialsOf(name) }
    val brush = if (style == BubbleStyle.IOS) {
        Brush.verticalGradient(listOf(Color(0xFFA5ABB8), Color(0xFF858994)))
    } else {
        SolidColor(AvatarColors[Math.floorMod(name.hashCode(), AvatarColors.size)])
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isEmpty()) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.62f),
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Tên hiển thị: tên đã lưu → tên tra trong danh bạ theo số → số điện thoại. */
@Composable
fun rememberDisplayName(c: Conversation): String {
    val context = LocalContext.current
    return remember(c.name, c.phone) {
        c.name.ifBlank {
            ContactUtils.lookupName(context, c.phone) ?: c.phone.ifBlank { "Không tên" }
        }
    }
}

/** Mục menu chuyển giao diện iPhone / Android. */
@Composable
fun StyleMenuItems(current: BubbleStyle, onSelect: (BubbleStyle) -> Unit) {
    DropdownMenuItem(
        text = { Text("Giao diện iPhone") },
        onClick = { onSelect(BubbleStyle.IOS) },
        trailingIcon = {
            if (current == BubbleStyle.IOS) Icon(Icons.Filled.Check, contentDescription = null)
        },
    )
    DropdownMenuItem(
        text = { Text("Giao diện Android") },
        onClick = { onSelect(BubbleStyle.ANDROID) },
        trailingIcon = {
            if (current == BubbleStyle.ANDROID) Icon(Icons.Filled.Check, contentDescription = null)
        },
    )
}

/**
 * Hình bong bóng kiểu iPhone: bo tròn 18dp, tin cuối mỗi nhóm có "đuôi" cong
 * ở góc dưới (bên phải với tin gửi đi, bên trái với tin nhận).
 * Đuôi vẽ tràn ra ngoài khung một chút nên cần chừa khoảng trống bên cạnh bong bóng.
 */
class IosBubbleShape(
    private val outgoing: Boolean,
    private val tail: Boolean,
    private val radius: Dp = 18.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = with(density) { radius.toPx() }.coerceAtMost(h / 2f)
        val body = Path().apply {
            addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r, r)))
        }
        if (!tail) return Outline.Generic(body)

        val t = with(density) { 6.dp.toPx() }
        // Toạ độ x được lật đối xứng cho tin nhận (đuôi bên trái).
        fun x(v: Float) = if (outgoing) v else w - v

        val tailPath = Path().apply {
            moveTo(x(w - r), h - r)
            lineTo(x(w), h - r)
            cubicTo(x(w), h - r * 0.35f, x(w + t * 0.35f), h - t * 0.15f, x(w + t), h)
            cubicTo(x(w - t * 0.4f), h + 0.5f, x(w - r * 0.55f), h - 0.5f, x(w - r), h - r * 0.12f)
            close()
        }
        val result = Path()
        result.op(body, tailPath, PathOperation.Union)
        return Outline.Generic(result)
    }
}
