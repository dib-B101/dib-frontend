package com.ssafy.dib.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors

enum class DibMainTab(val label: String, @param:DrawableRes val icon: Int) {
    Home("홈", R.drawable.nav_home_full),
    Feed("피드", R.drawable.nav_feed_full),
    Register("등록", R.drawable.nav_register_full),
    Trades("내 거래", R.drawable.nav_trades_full),
    My("마이", R.drawable.nav_my_full)
}

/** Figma 00_Components / Bottom Navigation / Main (626:242). */
@Composable
fun DibBottomNavigation(selectedTab: DibMainTab, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(WireframeColors.Background)) {
        HorizontalDivider(color = WireframeColors.Border, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(63.dp).padding(horizontal = 12.dp, vertical = 4.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            DibMainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val color = if (selected) WireframeColors.Navy else WireframeColors.Muted
                Column(Modifier.weight(1f).height(56.dp).selectable(selected, role = Role.Tab, onClick = { onTabSelected(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(tab.icon), null, Modifier.size(22.dp, 24.dp), colorFilter = ColorFilter.tint(color))
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label, color = color, fontSize = 11.sp, lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = (-0.22).sp)
                }
            }
        }
    }
}
