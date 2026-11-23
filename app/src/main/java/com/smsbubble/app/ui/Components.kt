package com.smsbubble.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
    val initials = remember(name, style) {
        // iPhone hiện hình người cho tên một từ (vd: tên đầu số "ADCBAlert" hay số điện thoại).
        if (style == BubbleStyle.IOS && !name.trim().contains(' ')) "" else initialsOf(name)
    }
    val brush = if (style == BubbleStyle.IOS) {
        Brush.verticalGradient(listOf(Color(0xFFA9B2DC), Color(0xFF7C85BD)))
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
                modifier = Modifier
                    .size(size * 0.8f)
                    .offset(y = size * 0.12f),
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
 * Hình bong bóng kiểu iPhone (đo từ ảnh chụp iOS): bo góc 21dp; tin cuối mỗi nhóm có "đuôi"
 * thòng xuống dưới đáy, lùi vào ~9dp so với mép (trái với tin nhận, phải với tin gửi),
 * đầu đuôi thấp hơn đáy ~7.5dp. Đuôi vẽ tràn xuống dưới khung nên cần chừa ~8dp bên dưới.
 */
class IosBubbleShape(
    private val outgoing: Boolean,
    private val tail: Boolean,
    private val radius: Dp = 21.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = with(density) { radius.toPx() }.coerceAtMost(h / 2f)
        val body = Path().apply {
            addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r, r)))
        }
        if (!tail) return Outline.Generic(body)

        val u = density.density // 1dp tính bằng px
        // Toạ độ theo dp, gốc ở góc dưới-trái của bong bóng; lật ngang cho tin gửi đi.
        fun x(v: Float) = if (outgoing) w - v * u else v * u
        fun y(v: Float) = h + v * u

        val tailPath = Path().apply {
            moveTo(x(11.5f), y(-6f))
            lineTo(x(11f), y(0f))
            cubicTo(x(11f), y(3f), x(10.2f), y(5.5f), x(9f), y(7f))
            cubicTo(x(8.6f), y(7.6f), x(9.3f), y(7.9f), x(10f), y(7.6f))
            cubicTo(x(14.5f), y(6f), x(19.5f), y(3f), x(24.5f), y(0f))
            lineTo(x(24.5f), y(-6f))
            close()
        }
        val result = Path()
        result.op(body, tailPath, PathOperation.Union)
        return Outline.Generic(result)
    }
}
