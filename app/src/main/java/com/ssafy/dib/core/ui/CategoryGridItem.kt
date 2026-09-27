package com.ssafy.dib.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private data class CategoryStyle(
    val order: Int,
    @param:DrawableRes val icon: Int,
    val tint: Color,
    val surface: Color
)

private val techInk = Colors.Navy
private val techSoft = Colors.NavySoft
private val homeInk = Color(0xFF8A5A2B)
private val homeSoft = Color(0xFFF6EFE7)
private val fashionInk = Color(0xFFB53A6E)
private val fashionSoft = Color(0xFFFBEAF1)
private val activeInk = Colors.MintInk
private val activeSoft = Colors.MintSoft
private val hobbyInk = Color(0xFF6D4AC2)
private val hobbySoft = Color(0xFFF0EBFA)
private val kidsInk = Color(0xFFD98A16)
private val kidsSoft = Color(0xFFFFF4E0)
private val foodInk = Colors.Urgent
private val foodSoft = Colors.UrgentBackground
private val natureInk = Color(0xFF2E8B57)
private val natureSoft = Color(0xFFE9F6EE)
private val ticketInk = Color(0xFF1F7AB8)
private val ticketSoft = Color(0xFFE8F3FB)

private fun String.containsAny(vararg keywords: String) = keywords.any { contains(it) }

// API의 이름과 ID는 그대로 사용한다. 두 화면이 같은 아이콘과 의미 그룹 순서를 공유한다.
private fun categoryStyle(name: String): CategoryStyle = when {
    name.containsAny("디지털", "전자", "기기") -> CategoryStyle(0, R.drawable.category_digital, techInk, techSoft)
    name.containsAny("가전") -> CategoryStyle(1, R.drawable.category_home, techInk, techSoft)
    name.containsAny("가구", "인테리어") -> CategoryStyle(2, R.drawable.category_furniture, homeInk, homeSoft)
    name.containsAny("주방", "생활") -> CategoryStyle(3, R.drawable.category_kitchen, homeInk, homeSoft)
    name.containsAny("의류", "패션") -> CategoryStyle(4, R.drawable.category_fashion, fashionInk, fashionSoft)
    name.containsAny("잡화", "가방", "신발") -> CategoryStyle(5, R.drawable.category_bag, fashionInk, fashionSoft)
    name.containsAny("뷰티", "미용") -> CategoryStyle(6, R.drawable.category_beauty, fashionInk, fashionSoft)
    name.containsAny("스포츠", "레저") -> CategoryStyle(7, R.drawable.category_sports, activeInk, activeSoft)
    name.containsAny("취미", "게임", "음반") -> CategoryStyle(8, R.drawable.category_game, hobbyInk, hobbySoft)
    name.containsAny("예술", "창작") -> CategoryStyle(9, R.drawable.category_art, hobbyInk, hobbySoft)
    name.containsAny("유아도서", "아동도서", "어린이책") -> CategoryStyle(11, R.drawable.category_book, kidsInk, kidsSoft)
    name.containsAny("유아", "아동", "키즈") -> CategoryStyle(10, R.drawable.category_kids, kidsInk, kidsSoft)
    name.containsAny("도서", "책") -> CategoryStyle(11, R.drawable.category_book, kidsInk, kidsSoft)
    name.containsAny("건강") -> CategoryStyle(12, R.drawable.category_health, foodInk, foodSoft)
    name.containsAny("식품", "음식") -> CategoryStyle(13, R.drawable.category_food, foodInk, foodSoft)
    name.containsAny("반려", "펫") -> CategoryStyle(14, R.drawable.category_pet, natureInk, natureSoft)
    name.containsAny("식물", "플랜트") -> CategoryStyle(15, R.drawable.category_plant, natureInk, natureSoft)
    name.containsAny("티켓", "교환권") -> CategoryStyle(16, R.drawable.category_ticket, ticketInk, ticketSoft)
    name.containsAny("쿠폰") -> CategoryStyle(17, R.drawable.category_coupon, ticketInk, ticketSoft)
    else -> CategoryStyle(99, R.drawable.category_etc, Colors.Muted, Colors.Surface)
}

fun categoryOrder(name: String): Int = categoryStyle(name).order

fun categoryDisplayName(name: String): String = when (name) {
    "유아동" -> "유아·아동 용품"
    "유아도서" -> "유아·아동 도서"
    else -> name
}

@Composable
fun CategoryIcon(name: String, modifier: Modifier = Modifier, size: Dp = 36.dp, selected: Boolean = false) {
    val style = categoryStyle(name)
    Box(
        modifier.size(size)
            .background(style.surface, CircleShape)
            .then(if (selected) Modifier.border(2.dp, style.tint, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(style.icon),
            contentDescription = null,
            modifier = Modifier.size(size * 0.45f),
            colorFilter = ColorFilter.tint(style.tint)
        )
    }
}

@Composable
fun CategoryGridItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CategoryIcon(name, size = 56.dp, selected = selected)
        Text(
            categoryDisplayName(name),
            Modifier.padding(top = 8.dp),
            color = Colors.Text,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
