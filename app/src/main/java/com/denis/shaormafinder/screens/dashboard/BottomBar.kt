package com.denis.shaormafinder.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denis.shaormafinder.R



data class BottomMenuItem(
    val label: String, val icon: Painter
)

@Composable
fun prepareBottomMenu(): List<BottomMenuItem> {
    val bottomMenuItemList = arrayListOf<BottomMenuItem>()

    bottomMenuItemList.add(
        BottomMenuItem(label = "Acasă", icon = painterResource(R.drawable.btn_1))
    )
    bottomMenuItemList.add(
        BottomMenuItem(label = "Suport", icon = painterResource(R.drawable.btn_2))
    )
    bottomMenuItemList.add(
        BottomMenuItem(label = "Favorite", icon = painterResource(R.drawable.btn_3))
    )
    bottomMenuItemList.add(
        BottomMenuItem(label = "Profil", icon = painterResource(R.drawable.btn_4))
    )

    return bottomMenuItemList
}

@Composable
fun BottomBar(
    selected: String,
    onItemClick: (String) -> Unit
) {
    val bottomMenuItemList = prepareBottomMenu()

    BottomAppBar(
        backgroundColor = colorResource(R.color.black3),
        elevation = 3.dp
    ) {
        bottomMenuItemList.forEach { bottomMenuItem ->
            BottomNavigationItem(
                selected = (selected == bottomMenuItem.label),
                onClick = {
                    onItemClick(bottomMenuItem.label)
                },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally)
                    {
                        Icon(
                            painter = bottomMenuItem.icon,
                            contentDescription = null,
                            tint = if (selected == bottomMenuItem.label) colorResource(R.color.gold) else colorResource(R.color.white),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(20.dp)
                        )
                        Text(
                            text = bottomMenuItem.label,
                            fontSize = 12.sp,
                            color = if (selected == bottomMenuItem.label) colorResource(R.color.gold) else colorResource(R.color.white),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    BottomBar(selected = "Acasă", onItemClick = {})
}