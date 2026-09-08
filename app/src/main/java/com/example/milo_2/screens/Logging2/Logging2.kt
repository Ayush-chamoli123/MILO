package com.example.milo_2.screens.Logging2

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
private val MiloGray = Color(0xFFBDBDBD)
private val CardBlack = Color(0xFF101010)
private val ChipBorder = Color(0xFF343434)
private val DotInactive = Color(0xFF3A3A3A)
private val ActiveGreen = Color(0xFF49C77B)


// ============================================================
// LOGGING 2
// ============================================================

@Preview(
    showBackground = true,
    backgroundColor = 0xFF080808
)
@Composable
fun Logging2(
    onNext: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBlack)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ====================================================
            // TOP BAR
            // ====================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(
                    modifier = Modifier.width(1.dp)
                )

                Text(
                    text = "Skip",
                    color = MiloBronze,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }


            Spacer(modifier = Modifier.height(5.dp))


            // ====================================================
            // MILO LOGO
            // ====================================================

            Image(
                painter = painterResource(
                    id = R.drawable.ic_logo
                ),
                contentDescription = "MILO Logo",
                modifier = Modifier.size(62.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "MILO",
                color = MiloWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )


            Spacer(modifier = Modifier.height(30.dp))


            // ====================================================
            // TITLE
            // ====================================================

            Text(
                text = "Discover activities near you",
                color = MiloWhite,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )


            Spacer(modifier = Modifier.height(8.dp))


            // ====================================================
            // DESCRIPTION
            // ====================================================

            Text(
                text = "Explore and join activities happening\naround your locality.",
                color = MiloBronzeLight,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center
            )


            Spacer(modifier = Modifier.height(24.dp))


            // ====================================================
            // MAP + ACTIVITY CARD
            // ====================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(
                        RoundedCornerShape(25.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF4B382D),
                        shape = RoundedCornerShape(25.dp)
                    )
            ) {

                // ------------------------------------------------
                // MAP BACKGROUND
                // ------------------------------------------------

                Image(
                    painter = painterResource(
                        id = R.drawable.imagee
                    ),
                    contentDescription = "Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )


                // ------------------------------------------------
                // ACTIVITY CARD
                // ------------------------------------------------

                ActivityCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            bottom = 20.dp
                        )
                )
            }


            Spacer(modifier = Modifier.height(20.dp))


            // ====================================================
            // ACTIVITY FILTERS
            // ====================================================

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                CategoryChip(
                    icon = R.drawable.sports,
                    text = "Football",
                    selected = true
                )

                Spacer(modifier = Modifier.width(10.dp))

                CategoryChip(
                    icon = R.drawable.badminton_icon,
                    text = "Badminton"
                )
            }


            Spacer(modifier = Modifier.height(10.dp))


            CategoryChip(
                icon = R.drawable.photo,
                text = "Photography"
            )


            Spacer(modifier = Modifier.height(25.dp))


            // ====================================================
            // PAGE INDICATORS
            // ====================================================

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                PageDot(active = false)

                PageDot(active = true)

                PageDot(active = false)
            }


            Spacer(modifier = Modifier.height(20.dp))


            // ====================================================
            // NEXT BUTTON
            // ====================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(MiloBronzeLight)
                    .clickable {
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ){
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Next",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Text(
                        text = "→",
                        color = Color.Black,
                        fontSize = 28.sp
                    )
                }
            }
        }
    }
}


// ============================================================
// ACTIVITY CARD
// ============================================================

@Composable
private fun ActivityCard(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardBlack)
            .border(
                width = 1.dp,
                color = Color(0xFF654A3A),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(
                horizontal = 15.dp,
                vertical = 12.dp
            )
    ) {

        // ------------------------------------------------
        // CATEGORY + ACTIVE
        // ------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF211B17))
                    .padding(
                        horizontal = 12.dp,
                        vertical = 5.dp
                    )
            ) {

                Text(
                    text = "Sports",
                    color = MiloBronzeLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Active",
                color = ActiveGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ------------------------------------------------
        // TITLE
        // ------------------------------------------------

        Text(
            text = "Evening Football (5v5)",
            color = MiloWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ------------------------------------------------
        // LOCATION
        // ------------------------------------------------

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "⌖",
                color = MiloBronzeLight,
                fontSize = 21.sp
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "Koramangala, Bengaluru (1.2 km)",
                color = MiloGray,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // ------------------------------------------------
        // TIME
        // ------------------------------------------------

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "◷",
                color = MiloBronzeLight,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "Today, 6:30 PM",
                color = MiloGray,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        // ------------------------------------------------
        // PEOPLE + JOIN
        // ------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(
                    id = R.drawable.people3
                ),
                contentDescription = "People joined",
                modifier = Modifier
                    .width(115.dp)
                    .height(38.dp),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .width(95.dp)
                    .height(45.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiloBronzeLight),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Join",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================================
// CATEGORY CHIP
// ============================================================

@Composable
private fun CategoryChip(
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
                horizontal = 16.dp,
                vertical = 9.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(
                id = icon
            ),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

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