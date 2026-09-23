package com.ssafy.dib.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private val SearchBarShape = RoundedCornerShape(14.dp)
private val SearchBarHeight = 50.dp

/** 홈과 같은 검색 상자. 누르면 검색 화면으로 가는 정적 상자다. 카테고리 등 다른 화면도 이걸 써서 생김새를 맞춘다. */
@Composable
fun DibSearchBar(hint: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(SearchBarHeight)
            .background(Colors.Search, SearchBarShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(painterResource(R.drawable.search_full), null, Modifier.size(19.dp), colorFilter = ColorFilter.tint(Colors.MintInk))
        Text(hint, color = Colors.Muted, fontSize = 14.sp)
    }
}

/** 검색 화면의 입력 상자. DibSearchBar 와 같은 높이·모서리·색을 쓴다. */
@Composable
fun DibSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    // Material TextField 는 위아래 16dp 여백이 고정이라 50dp 안에 넣으면 글자 아래가 잘렸다(글자 크기 배율을 키운 뒤 더 심해짐).
    // 여백을 직접 정하는 BasicTextField 로 그려 DibSearchBar 와 같은 모양에 글자를 세로 가운데 둔다
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = SearchBarHeight),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp, color = Colors.Text),
        cursorBrush = SolidColor(Colors.Navy),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = SearchBarHeight)
                    .background(Colors.Search, SearchBarShape)
                    .padding(start = 16.dp, end = if (value.isNotBlank() && onClear != null) 4.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(painterResource(R.drawable.search_full), null, Modifier.size(19.dp), colorFilter = ColorFilter.tint(Colors.MintInk))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(hint, color = Colors.Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1)
                    innerTextField()
                }
                if (value.isNotBlank() && onClear != null) {
                    IconButton(onClick = onClear) { Image(painterResource(R.drawable.close), "검색어 지우기", Modifier.size(18.dp), colorFilter = ColorFilter.tint(Colors.Muted)) }
                }
            }
        }
    )
}
