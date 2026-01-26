package com.denis.shaormafinder.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import com.denis.shaormafinder.R
import com.denis.shaormafinder.viewModel.DashboardViewModel
import java.io.File
import java.util.Calendar

@Composable
fun TopBar(
    userName: String,
    userImagePath: String?,
    wishlistCount: Int,
    points: Int ,
    viewModel: DashboardViewModel
) {
    val userRankTitle = viewModel.getUserRank()

    val greetingText = remember {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        when (hour) {
            in 6..10 -> "Neaţa"
            in 11..17 -> "Bună ziua"
            else -> "Bună seara"
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (title1, title2, profile, box) = createRefs()

        if (userImagePath != null) {
            AsyncImage(
                model = File(userImagePath),
                contentDescription = "Poza de profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .constrainAs(profile) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
            )
        } else {
            Image(
                painter = painterResource(R.drawable.profile),
                contentDescription = "Poza de profil implicită",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .constrainAs(profile) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
            )
        }

        Text(
            text = "$greetingText, $userName",
            fontSize = 20.sp,
            color = colorResource(R.color.gold),
            modifier = Modifier
                .constrainAs(title1) {
                    top.linkTo(profile.top)
                    start.linkTo(parent.start, margin = 16.dp)
                    bottom.linkTo(profile.bottom)
                    end.linkTo(profile.start, margin = 8.dp)
                    width = Dimension.fillToConstraints
                }
        )

        Text(
            text = "Ce mănânci azi bun?",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.gold),
            modifier = Modifier
                .padding(top = 24.dp)
                .constrainAs(title2) {
                    top.linkTo(profile.bottom)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        ConstraintLayout(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp)
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    color = colorResource(R.color.black3),
                    shape = RoundedCornerShape(10.dp)
                )
                .constrainAs(box) {
                    bottom.linkTo(parent.bottom)
                    top.linkTo(title2.bottom)
                }
                .clip(RoundedCornerShape(10.dp))
        ) {
            val (icon1, icon2, wishTitle, wishCount, reward, wallet, pointsValue, line1, line2) = createRefs()

            Image(
                painter = painterResource(R.drawable.wallet),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .size(20.dp)
                    .constrainAs(icon1) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
            )

            Text(
                text = "Shaormițe",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .constrainAs(wallet) {
                        top.linkTo(icon1.top)
                        bottom.linkTo(icon1.bottom)
                        start.linkTo(icon1.end)
                    }
            )

            Text(
                text = "$points XP",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.gold),
                modifier = Modifier
                    .padding(start = 8.dp, top = 2.dp)
                    .constrainAs(pointsValue) {
                        top.linkTo(wallet.bottom)
                        start.linkTo(wallet.start)
                    }
            )

            Image(
                painter = painterResource(R.drawable.medal),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 16.dp)
                    .constrainAs(icon2) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    }
            )

            Text(
                text = userRankTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.gold),
                style = TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier
                    .padding(start = 8.dp,bottom=16.dp)
                    .constrainAs(reward) {
                        top.linkTo(icon2.top)
                        bottom.linkTo(icon2.bottom)
                        start.linkTo(icon2.end)
                    }
            )

            Box(modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 16.dp)
                .background(colorResource(R.color.grey))
                .constrainAs(line1) {
                    centerTo(parent)
                }
            )

            Box(Modifier
                .height(1.dp)
                .background(colorResource(R.color.grey))
                .constrainAs(line2) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)

                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(line1.start, margin = 16.dp)

                    width = Dimension.fillToConstraints
                }
            )

            Text(
                text = "Preferate",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(textDecoration = TextDecoration.Underline),
                color = Color.White,
                modifier = Modifier
                    .padding(start = 16.dp, top = 32.dp)
                    .constrainAs(wishTitle) {
                        top.linkTo(parent.top)
                        start.linkTo(line1.end)
                    }
            )

            Text(
                text = "$wishlistCount ${if (wishlistCount == 1) "Local" else "Localuri"}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
                    .constrainAs(wishCount) {
                        top.linkTo(wishTitle.bottom)
                        start.linkTo(wishTitle.start)
                    }
            )
        }
    }
}