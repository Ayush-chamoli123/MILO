package com.example.milo_2.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.R


// ============================================================
// MILO COLORS
// ============================================================

val MiloBackground = Color(0xFF05080D)
val MiloSurface = Color(0xFF0B1017)
val MiloSurface2 = Color(0xFF10161F)
val MiloBorder = Color(0xFF202934)

val MiloOrange = Color(0xFFFF995C)
val MiloWhite = Color(0xFFF5F5F5)
val MiloSecondary = Color(0xFFA7AFBA)
val MiloMuted = Color(0xFF737C88)


// ============================================================
// DATA CLASSES
// ============================================================

data class Category(
    val name: String,
    val icon: @Composable () -> Unit
)

data class ActivityItem(
    val title: String,
    val location: String,
    val date: String,
    val image: Int
)


// ============================================================
// MAIN DASHBOARD
// ============================================================

@Composable
fun Dashboard() {

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        containerColor = MiloBackground,

        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                }
            )
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiloBackground)
                .padding(innerPadding)
        ) {

            when (selectedTab) {

                // HOME
                0 -> HomeScreen()

                // EXPLORE
                1 -> ExploreScreen()

                // CREATE
                2 -> CreateScreen()

                // CHAT
                3 -> ChatScreen()

                // PROFILE
                4 -> ProfileScreen()
            }
        }
    }
}


// ============================================================
// HOME SCREEN
// ============================================================

@Composable
fun HomeScreen() {

    val categories = listOf(

        Category("Sports") {
            Icon(
                imageVector = Icons.Default.SportsTennis,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("Study") {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("Coding") {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("Gaming") {
            Icon(
                imageVector = Icons.Default.Gamepad,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("Events") {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("Football") {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("Chat") {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        },

        Category("More") {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MiloOrange,
                modifier = Modifier.size(25.dp)
            )
        }
    )


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBackground),

        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 30.dp
        ),

        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // ----------------------------------------------------
        // HEADER
        // ----------------------------------------------------

        item {

            TopHeader()
        }


        // ----------------------------------------------------
        // GREETING
        // ----------------------------------------------------

        item {

            Column {

                Text(
                    text = "Hey Ayush 👋",
                    color = MiloWhite,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Find people. Find plans. Find your vibe.",
                    color = MiloSecondary,
                    fontSize = 14.sp
                )
            }
        }


        // ----------------------------------------------------
        // SEARCH
        // ----------------------------------------------------

        item {

            SearchBar()
        }


        // ----------------------------------------------------
        // CATEGORIES TITLE
        // ----------------------------------------------------

        item {

            SectionTitle(
                title = "Explore categories"
            )
        }


        // ----------------------------------------------------
        // CATEGORY ROW
        // ----------------------------------------------------

        item {

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(categories) { category ->

                    CategoryCard(
                        category = category
                    )
                }
            }
        }


        // ----------------------------------------------------
        // RECOMMENDED TITLE
        // ----------------------------------------------------

        item {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            SectionTitle(
                title = "Recommended for you",
                showSeeAll = true
            )
        }


        // ----------------------------------------------------
        // RECOMMENDED BADMINTON
        // ----------------------------------------------------

        item {

            RecommendedCard(
                title = "Badminton",
                subtitle = "Looking for players",
                location = "Graphic Era University",
                time = "Today • 5:30 PM",
                image = R.drawable.badminton_activity
            )
        }


        // ----------------------------------------------------
        // RECOMMENDED DSA
        // ----------------------------------------------------

        item {

            RecommendedCard(
                title = "DSA Study Session",
                subtitle = "Let's solve problems together",
                location = "Central Library",
                time = "Tomorrow • 4:00 PM",
                image = R.drawable.dsa_study
            )
        }


        // ----------------------------------------------------
        // MORE ACTIVITIES
        // ----------------------------------------------------

        item {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            SectionTitle(
                title = "More activities",
                showSeeAll = true
            )
        }


        // ----------------------------------------------------
        // FOOTBALL
        // ----------------------------------------------------

        item {

            ActivityCard(
                title = "Football Evening",
                location = "University Ground",
                date = "Today • 6:00 PM",
                image = R.drawable.football
            )
        }


        // ----------------------------------------------------
        // ML STUDY
        // ----------------------------------------------------

        item {

            ActivityCard(
                title = "Machine Learning Study Group",
                location = "Computer Lab",
                date = "Tomorrow • 3:00 PM",
                image = R.drawable.ml_study
            )
        }


        // ----------------------------------------------------
        // HACKATHON
        // ----------------------------------------------------

        item {

            ActivityCard(
                title = "Hackathon Project",
                location = "Innovation Lab",
                date = "Saturday • 10:00 AM",
                image = R.drawable.hackathon
            )
        }


        // ----------------------------------------------------
        // RISHIKESH
        // ----------------------------------------------------

        item {

            ActivityCard(
                title = "Rishikesh Trip",
                location = "Rishikesh",
                date = "Sunday • 7:00 AM",
                image = R.drawable.rishikesh
            )
        }


        // ----------------------------------------------------
        // BOTTOM SPACE
        // ----------------------------------------------------

        item {

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}


// ============================================================
// TOP HEADER
// ============================================================

@Composable
fun TopHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {

            Text(
                text = "MILO",
                color = MiloWhite,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = "Dehradun",
                color = MiloSecondary,
                fontSize = 12.sp
            )
        }


        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {}
            ) {

                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = MiloWhite,
                    modifier = Modifier.size(25.dp)
                )
            }


            Spacer(
                modifier = Modifier.width(4.dp)
            )


            Image(
                painter = painterResource(
                    id = R.drawable.profile_avatar
                ),
                contentDescription = "Profile",

                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),

                contentScale = ContentScale.Crop
            )
        }
    }
}


// ============================================================
// SEARCH BAR
// ============================================================

@Composable
fun SearchBar() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(
                RoundedCornerShape(16.dp)
            )
            .background(MiloSurface2)
            .padding(horizontal = 16.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MiloSecondary,
            modifier = Modifier.size(23.dp)
        )


        Spacer(
            modifier = Modifier.width(12.dp)
        )


        Text(
            text = "Search activities, people, places...",
            color = MiloMuted,
            fontSize = 14.sp
        )
    }
}


// ============================================================
// SECTION TITLE
// ============================================================

@Composable
fun SectionTitle(
    title: String,
    showSeeAll: Boolean = false
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            color = MiloWhite,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )


        if (showSeeAll) {

            Text(
                text = "See all",
                color = MiloOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ============================================================
// CATEGORY CARD
// ============================================================

@Composable
fun CategoryCard(
    category: Category
) {

    Column(
        modifier = Modifier
            .width(82.dp)
            .height(86.dp)
            .clip(
                RoundedCornerShape(16.dp)
            )
            .background(MiloSurface)
            .padding(10.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    MiloOrange.copy(alpha = 0.12f)
                ),

            contentAlignment = Alignment.Center
        ) {

            category.icon()
        }


        Spacer(
            modifier = Modifier.height(7.dp)
        )


        Text(
            text = category.name,
            color = MiloWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ============================================================
// RECOMMENDED CARD
// ============================================================

@Composable
fun RecommendedCard(
    title: String,
    subtitle: String,
    location: String,
    time: String,
    image: Int
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(MiloSurface)
            .padding(10.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(
                id = image
            ),

            contentDescription = title,

            modifier = Modifier
                .size(100.dp)
                .clip(
                    RoundedCornerShape(14.dp)
                ),

            contentScale = ContentScale.Crop
        )


        Spacer(
            modifier = Modifier.width(13.dp)
        )


        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = MiloWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


            Spacer(
                modifier = Modifier.height(4.dp)
            )


            Text(
                text = subtitle,
                color = MiloSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(
                text = location,
                color = MiloMuted,
                fontSize = 11.sp
            )


            Spacer(
                modifier = Modifier.height(3.dp)
            )


            Text(
                text = time,
                color = MiloOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ============================================================
// ACTIVITY CARD
// ============================================================

@Composable
fun ActivityCard(
    title: String,
    location: String,
    date: String,
    image: Int
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(MiloSurface)
            .padding(10.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(
                id = image
            ),

            contentDescription = title,

            modifier = Modifier
                .size(90.dp)
                .clip(
                    RoundedCornerShape(14.dp)
                ),

            contentScale = ContentScale.Crop
        )


        Spacer(
            modifier = Modifier.width(13.dp)
        )


        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = MiloWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            Text(
                text = location,
                color = MiloSecondary,
                fontSize = 12.sp
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            Text(
                text = date,
                color = MiloOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ============================================================
// EXPLORE SCREEN
// ============================================================

@Composable
fun ExploreScreen() {

    val activities = listOf(

        ActivityItem(
            title = "Football Evening",
            location = "University Ground",
            date = "Today • 6:00 PM",
            image = R.drawable.football
        ),

        ActivityItem(
            title = "Machine Learning Study Group",
            location = "Computer Lab",
            date = "Tomorrow • 3:00 PM",
            image = R.drawable.ml_study
        ),

        ActivityItem(
            title = "Hackathon Project",
            location = "Innovation Lab",
            date = "Saturday • 10:00 AM",
            image = R.drawable.hackathon
        ),

        ActivityItem(
            title = "Rishikesh Trip",
            location = "Rishikesh",
            date = "Sunday • 7:00 AM",
            image = R.drawable.rishikesh
        )
    )


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBackground),

        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 30.dp
        ),

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                text = "Explore",
                color = MiloWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "Find something interesting near you",
                color = MiloSecondary,
                fontSize = 14.sp
            )
        }


        item {

            SearchBar()
        }


        item {

            SectionTitle(
                title = "All activities"
            )
        }


        items(activities) { activity ->

            ActivityCard(
                title = activity.title,
                location = activity.location,
                date = activity.date,
                image = activity.image
            )
        }


        item {

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}


// ============================================================
// CREATE SCREEN
// ============================================================

@Composable
fun CreateScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBackground)
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(40.dp)
        )


        Box(
            modifier = Modifier
                .size(75.dp)
                .clip(CircleShape)
                .background(
                    MiloOrange.copy(alpha = 0.12f)
                ),

            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create",
                tint = MiloOrange,
                modifier = Modifier.size(38.dp)
            )
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Text(
            text = "Create something",
            color = MiloWhite,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Text(
            text = "Create an activity and find people to join you.",
            color = MiloSecondary,
            fontSize = 14.sp
        )


        Spacer(
            modifier = Modifier.height(25.dp)
        )


        OutlinedButton(
            onClick = {},
            shape = RoundedCornerShape(14.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )


            Spacer(
                modifier = Modifier.width(8.dp)
            )


            Text(
                text = "Create Activity"
            )
        }
    }
}


// ============================================================
// CHAT SCREEN
// ============================================================

@Composable
fun ChatScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBackground)
            .padding(20.dp)
    ) {

        Text(
            text = "Chat",
            color = MiloWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier = Modifier.height(6.dp)
        )


        Text(
            text = "Connect with people you meet on MILO.",
            color = MiloSecondary,
            fontSize = 14.sp
        )


        Spacer(
            modifier = Modifier.height(40.dp)
        )


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .background(MiloSurface)
                .padding(25.dp),

            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MiloOrange,
                    modifier = Modifier.size(40.dp)
                )


                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                Text(
                    text = "No conversations yet",
                    color = MiloWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(5.dp)
                )


                Text(
                    text = "Join an activity to start meeting people.",
                    color = MiloSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}


// ============================================================
// PROFILE SCREEN
// ============================================================

@Composable
fun ProfileScreen() {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloBackground),

        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 25.dp,
            bottom = 40.dp
        ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {

            Image(
                painter = painterResource(
                    id = R.drawable.profile_avatar
                ),

                contentDescription = "Profile",

                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape),

                contentScale = ContentScale.Crop
            )


            Spacer(
                modifier = Modifier.height(15.dp)
            )


            Text(
                text = "Ayush",
                color = MiloWhite,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(4.dp)
            )


            Text(
                text = "Dehradun • Student",
                color = MiloSecondary,
                fontSize = 13.sp
            )


            Spacer(
                modifier = Modifier.height(25.dp)
            )
        }


        item {

            ProfileStats()
        }


        item {

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = "My Activities",
                modifier = Modifier.fillMaxWidth(),
                color = MiloWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(14.dp)
            )
        }


        item {

            ActivityCard(
                title = "Badminton",
                location = "Graphic Era University",
                date = "Today • 5:30 PM",
                image = R.drawable.badminton_activity
            )
        }


        item {

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            ActivityCard(
                title = "DSA Study Session",
                location = "Central Library",
                date = "Tomorrow • 4:00 PM",
                image = R.drawable.dsa_study
            )
        }
    }
}


// ============================================================
// PROFILE STATS
// ============================================================

@Composable
fun ProfileStats() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(MiloSurface)
            .padding(vertical = 18.dp),

        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        ProfileStat(
            number = "08",
            label = "Activities"
        )

        ProfileStat(
            number = "24",
            label = "Friends"
        )

        ProfileStat(
            number = "4.8",
            label = "Rating"
        )
    }
}


@Composable
fun ProfileStat(
    number: String,
    label: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = number,
            color = MiloWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier = Modifier.height(3.dp)
        )


        Text(
            text = label,
            color = MiloSecondary,
            fontSize = 11.sp
        )
    }
}


// ============================================================
// BOTTOM NAVIGATION
// ============================================================

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiloSurface)
            .navigationBarsPadding()
            .height(70.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),

            horizontalArrangement = Arrangement.SpaceEvenly,

            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomNavItem(
                icon = Icons.Default.Explore,
                label = "Home",
                selected = selectedTab == 0,
                onClick = {
                    onTabSelected(0)
                }
            )


            BottomNavItem(
                icon = Icons.Default.Explore,
                label = "Explore",
                selected = selectedTab == 1,
                onClick = {
                    onTabSelected(1)
                }
            )


            // CENTER CREATE BUTTON
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MiloOrange)
                    .clickable {
                        onTabSelected(2)
                    },

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = MiloBackground,
                    modifier = Modifier.size(28.dp)
                )
            }


            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "Chat",
                selected = selectedTab == 3,
                onClick = {
                    onTabSelected(3)
                }
            )


            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                selected = selectedTab == 4,
                onClick = {
                    onTabSelected(4)
                }
            )
        }
    }
}


// ============================================================
// BOTTOM NAV ITEM
// ============================================================

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .width(65.dp)
            .clickable {
                onClick()
            },

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,

            tint = if (selected) {
                MiloOrange
            } else {
                MiloMuted
            },

            modifier = Modifier.size(23.dp)
        )


        Spacer(
            modifier = Modifier.height(3.dp)
        )


        Text(
            text = label,

            color = if (selected) {
                MiloOrange
            } else {
                MiloMuted
            },

            fontSize = 10.sp,

            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}