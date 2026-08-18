package com.example.milo_2.screens.Logging1

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.R


// ============================================================
// MILO COLORS
// ============================================================

private val MiloBlack = Color(0xFF080808)
private val MiloBronze = Color(0xFFE8A77D)
private val MiloBronzeLight = Color(0xFFF0B58D)
private val MiloWhite = Color(0xFFF5F5F5)
private val ChipBorder = Color(0xFF343434)
private val DotInactive = Color(0xFF3A3A3A)


// ============================================================
// LOGGING 1 SCREEN
// ============================================================

@Preview(
    showBackground = true,
    backgroundColor = 0xFF080808
)
@Composable
fun Logging1(
    onNext: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBlack)
            .navigationBarsPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 18.dp,
                    bottom = 12.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ====================================================
            // TOP BAR
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Skip",
                    color = MiloBronze,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        end = 4.dp,
                        top = 4.dp
                    )
                )
            }


            Spacer(modifier = Modifier.height(10.dp))


            // ====================================================
            // MILO LOGO
            // ====================================================

            Image(
                painter = painterResource(
                    id = R.drawable.ic_logo
                ),
                contentDescription = "MILO Logo",
                modifier = Modifier.size(70.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "MILO",
                color = MiloWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )


            Spacer(modifier = Modifier.height(20.dp))


            // ====================================================
            // TITLE
            // ====================================================

            Text(
                text = "Life is better together",
                color = MiloWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )


            Spacer(modifier = Modifier.height(8.dp))


            // ====================================================
            // DESCRIPTION
            // ====================================================

            Text(
                text = "Find people around you for sports,\nstudy, hobbies and more.",
                color = MiloBronzeLight,
                fontSize = 14.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )


            Spacer(modifier = Modifier.height(24.dp))


            // ====================================================
            // MAIN IMAGE
            // ====================================================

            Image(
                painter = painterResource(
                    id = R.drawable.img_2
                ),
                contentDescription = "People enjoying an activity together",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF4B382D),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentScale = ContentScale.Crop
            )


            Spacer(modifier = Modifier.height(22.dp))


            // ====================================================
            // ACTIVITY CHIPS - ROW 1
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // SPORTS
                ActivityChip(
                    icon = R.drawable.sports,
                    text = "Sports",
                    selected = true
                )

                // STUDY
                ActivityChip(
                    icon = R.drawable.study,
                    text = "Study"
                )

                // WALKING
                ActivityChip(
                    icon = R.drawable.footstep,
                    text = "Walking"
                )
            }


            Spacer(modifier = Modifier.height(12.dp))


            // ====================================================
            // ACTIVITY CHIPS - ROW 2
            // ====================================================

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // CYCLING
                ActivityChip(
                    icon = R.drawable.bicycle,
                    text = "Cycling"
                )

                Spacer(modifier = Modifier.width(12.dp))

                // TECH & MORE
                ActivityChip(
                    icon = R.drawable.star,
                    text = "Tech & More"
                )
            }


            Spacer(modifier = Modifier.height(30.dp))


            // ====================================================
            // PAGE INDICATORS
            // ====================================================

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                PageDot(active = true)

                PageDot(active = false)

                PageDot(active = false)
            }


            Spacer(modifier = Modifier.height(25.dp))


            // ====================================================
            // NEXT BUTTON
            // ====================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(
                        RoundedCornerShape(17.dp)
                    )
                    .background(MiloBronzeLight)
                    .clickable {
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Next",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(14.dp))


                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


// ============================================================
// ACTIVITY CHIP
// ============================================================

@Composable
private fun ActivityChip(
    icon: Int,
    text: String,
    selected: Boolean = false
) {

    val borderColor =
        if (selected) {
            MiloBronze
        } else {
            ChipBorder
        }

    val textColor =
        if (selected) {
            MiloBronzeLight
        } else {
            MiloWhite
        }

    Row(
        modifier = Modifier
            .clip(
                RoundedCornerShape(30.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(30.dp)
            )
            .padding(
                horizontal = 15.dp,
                vertical = 9.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


// ============================================================
// PAGE DOT
// ============================================================

@Composable
private fun PageDot(
    active: Boolean
) {

    Box(
        modifier = Modifier
            .size(
                if (active) 10.dp else 9.dp
            )
            .clip(
                RoundedCornerShape(50)
            )
            .background(
                if (active) {
                    MiloBronze
                } else {
                    DotInactive
                }
            )
    )
}