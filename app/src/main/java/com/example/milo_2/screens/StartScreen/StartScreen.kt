package com.example.milo_2.screens.StartScreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic Netflix-Style Convergence Intro for MILO
 * ==================================================
 * Theme: "Find People. Do More Together."
 *
 * Visual & Sonic Choreography:
 * 1. Solitary Sparks (0.0s - 1.2s):
 *    Characters representing activities (Badminton, Cycling, Football, Hackathon,
 *    Study, Hiking) emerge at outer orbital positions with individual breathing auras.
 *    Atmospheric whoosh audio begins to rise in pitch and volume.
 * 2. Magnetic Convergence / Joining Up (1.2s - 2.2s):
 *    The characters lock together and accelerate inward toward the center,
 *    leaving speed trails and light ribbons behind them (Netflix signature convergence).
 * 3. The Fusion Impact & Sonic Theme (2.2s - 2.8s):
 *    Characters slam together at (0, 0)! The signature MILO sonic chord plays
 *    (sub-bass thud + warm major chord + crystalline chimes). A shockwave ring
 *    explodes outward accompanied by a burst of glowing particles.
 * 4. Brand Assembly & Shimmer (2.5s - 4.2s):
 *    The MILO emblem emerges with spring bounce physics. Metallic horizontal
 *    light sheen sweeps across the letters "M I L O".
 *    Tagline "Find People. Do More Together." fades in with letter tracking.
 */

private data class ActivityCharacter(
    val name: String,
    val iconRes: Int,
    val auraColor: Color,
    val angleDegrees: Float,
    val initialDistanceDp: Dp,
)

private data class ParticleSpark(
    val angleRad: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
)

@Preview(showBackground = true)
@Composable
fun StartScreen(
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioPlayer = remember { MiloAudioPlayer() }

    // Cleanup audio resources on exit
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    // Master Animation Clock (0.0f -> 1.0f over ~4.2 seconds)
    val masterProgress = remember { Animatable(0.0f) }

    // Spark Particles for Fusion Burst
    val sparkParticles = remember {
        val colors = listOf(
            Color(0xFF00E5FF), // Cyan
            Color(0xFFFFB078), // Milo Orange
            Color(0xFF10B981), // Lime Green
            Color(0xFFA855F7), // Purple
            Color(0xFFF43F5E), // Rose
            Color(0xFFFFFFFF)  // White
        )
        List(32) { i ->
            val angle = (i * (360f / 32f)) * (Math.PI / 180f).toFloat()
            val speed = 250f + (i % 5) * 60f
            val color = colors[i % colors.size]
            val size = 3f + (i % 4) * 2f
            ParticleSpark(angle, speed, color, size)
        }
    }

    // Six Activity Characters representing MILO's activities
    val characters = remember {
        listOf(
            ActivityCharacter("Badminton", R.drawable.badminton_icon, Color(0xFF00E5FF), 215f, 210.dp),
            ActivityCharacter("Cycling", R.drawable.bicycle, Color(0xFF10B981), 335f, 220.dp),
            ActivityCharacter("Football", R.drawable.football, Color(0xFFFF9100), 90f, 195.dp),
            ActivityCharacter("Hackathon", R.drawable.hackathon, Color(0xFFA855F7), 270f, 205.dp),
            ActivityCharacter("Study", R.drawable.study, Color(0xFFF59E0B), 25f, 215.dp),
            ActivityCharacter("Trek", R.drawable.footstep, Color(0xFFF43F5E), 150f, 225.dp),
        )
    }

    // Continuous floating ambient pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ambientPulse = infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPulse"
    )

    // Trigger choreographed timeline
    LaunchedEffect(Unit) {
        // Trigger Sonic Theme at the exact fusion sync point (600ms + 2400ms audio impact = 3000ms fusion)
        launch {
            delay(600)
            audioPlayer.playIntroSound(context)
        }

        // Animate master timeline to completion with a smooth, slower cinematic duration (5.6 seconds)
        masterProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 5600, easing = LinearEasing)
        )

        // Seamless transition to next screen
        delay(300)
        onFinished()
    }

    val progress = masterProgress.value

    // Animation Milestones (Smooth 5.6s timeline)
    // 0.00 -> 0.24 (0 - 1350ms): Gentle emergence & organic floating orbit
    // 0.24 -> 0.54 (1350 - 3020ms): Silky smooth magnetic convergence with glowing trails
    // 0.54 (approx 3020ms): Fusion collision sync with sonic sub-bass impact
    // 0.53 -> 0.82 (2970 - 4600ms): Decelerating shockwave ripple & particle fireworks
    // 0.53 -> 0.76 (2970 - 4250ms): Elegant logo spring emergence & ambient halo
    // 0.66 -> 0.92 (3700 - 5150ms): Slow metallic shimmer sweep
    // 0.70 -> 0.92 (3920 - 5150ms): Tagline upward glide & fade-in

    val convergenceProgress = when {
        progress < 0.24f -> 0f
        progress in 0.24f..0.54f -> {
            val localT = (progress - 0.24f) / (0.54f - 0.24f)
            // Silky smooth ease-in-out curve
            CubicBezierEasing(0.38f, 0.0f, 0.20f, 1.0f).transform(localT)
        }
        else -> 1f
    }

    // Characters opacity: smooth fade in, stay bright, then gracefully dissolve into the core
    val characterAlpha = when {
        progress < 0.18f -> (progress / 0.15f).coerceIn(0f, 1f)
        progress in 0.18f..0.51f -> 1f
        progress in 0.51f..0.56f -> {
            val localT = (progress - 0.51f) / 0.05f
            (1f - localT * localT).coerceIn(0f, 1f)
        }
        else -> 0f
    }

    // Decelerating Shockwave expansion
    val shockwaveProgress = when {
        progress < 0.53f -> 0f
        progress in 0.53f..0.82f -> {
            val localT = (progress - 0.53f) / 0.29f
            CubicBezierEasing(0.12f, 0.82f, 0.25f, 1.0f).transform(localT)
        }
        else -> 1f
    }
    val shockwaveAlpha = when {
        progress < 0.53f -> 0f
        progress in 0.53f..0.78f -> (1f - (progress - 0.53f) / 0.25f).coerceIn(0f, 1f)
        else -> 0f
    }

    // Logo & Brand emergence: gentle dampened curve without harsh jarring bounce
    val logoScale = when {
        progress < 0.53f -> 0.0f
        progress in 0.53f..0.75f -> {
            val localT = (progress - 0.53f) / 0.22f
            val eased = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f).transform(localT)
            eased * 1.0f
        }
        else -> 1.0f + (progress - 0.75f) * 0.025f // subtle slow push
    }
    val logoAlpha = when {
        progress < 0.53f -> 0f
        progress in 0.53f..0.66f -> ((progress - 0.53f) / 0.13f).coerceIn(0f, 1f)
        else -> 1f
    }

    // Shimmer sweep across the MILO letters: slow and luxurious
    val shimmerOffset = when {
        progress < 0.66f -> -1.0f
        progress in 0.66f..0.92f -> ((progress - 0.66f) / 0.26f) * 2.2f - 1.0f
        else -> 1.2f
    }

    // Tagline fade-in and vertical glide
    val taglineProgress = when {
        progress < 0.70f -> 0f
        else -> ((progress - 0.70f) / 0.20f).coerceIn(0f, 1f)
    }
    val taglineAlpha = taglineProgress

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1A30), // Deep cinematic navy
                        Color(0xFF070D18),
                        Color(0xFF020408)  // True midnight black
                    ),
                    radius = 1200f
                )
            )
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx / 2f)

        // ---------------------------------------------------------------------
        // 1. DYNAMIC LIGHT TRAILS & FUSION SHOCKWAVE CANVAS
        // ---------------------------------------------------------------------
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw speed light ribbons connecting converging characters to center
            if (convergenceProgress > 0.05f && characterAlpha > 0.01f) {
                for (char in characters) {
                    val angleRad = Math.toRadians(char.angleDegrees.toDouble()).toFloat()
                    val totalDistPx = char.initialDistanceDp.toPx()
                    val currentDistPx = totalDistPx * (1f - convergenceProgress)

                    val charPos = Offset(
                        center.x + currentDistPx * cos(angleRad),
                        center.y + currentDistPx * sin(angleRad)
                    )

                    // Netflix-style colored light trail behind character
                    val trailLength = (totalDistPx - currentDistPx).coerceAtMost(160f)
                    val trailStart = Offset(
                        charPos.x + trailLength * cos(angleRad),
                        charPos.y + trailLength * sin(angleRad)
                    )

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(char.auraColor.copy(alpha = 0f), char.auraColor.copy(alpha = 0.6f * characterAlpha)),
                            start = trailStart,
                            end = charPos
                        ),
                        start = trailStart,
                        end = charPos,
                        strokeWidth = 5f * (1f - convergenceProgress * 0.5f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Expanding Shockwave Ring upon collision
            if (shockwaveProgress in 0.01f..0.99f && shockwaveAlpha > 0.01f) {
                val radius = shockwaveProgress * (widthPx * 0.65f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFB078).copy(alpha = shockwaveAlpha * 0.85f),
                            Color(0xFF00E5FF).copy(alpha = shockwaveAlpha * 0.40f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius + 20f
                    ),
                    center = center,
                    radius = radius,
                    style = Stroke(width = 8f * (1f - shockwaveProgress))
                )
            }

            // 32 Radial Fusion Sparks Exploding Outward with smooth deceleration
            if (progress in 0.53f..0.85f) {
                val sparkRaw = ((progress - 0.53f) / 0.32f).coerceIn(0f, 1f)
                val sparkProgress = CubicBezierEasing(0.10f, 0.75f, 0.20f, 1.0f).transform(sparkRaw)
                val sparkAlpha = (1f - sparkRaw).coerceIn(0f, 1f)

                for (spark in sparkParticles) {
                    val dist = spark.speed * sparkProgress
                    val sparkPos = Offset(
                        center.x + dist * cos(spark.angleRad),
                        center.y + dist * sin(spark.angleRad)
                    )
                    drawCircle(
                        color = spark.color.copy(alpha = sparkAlpha * 0.90f),
                        radius = spark.size * (1f - sparkProgress * 0.5f),
                        center = sparkPos
                    )
                }
            }
        }

        // ---------------------------------------------------------------------
        // 2. CONVERGING ACTIVITY CHARACTERS
        // ---------------------------------------------------------------------
        if (characterAlpha > 0.01f) {
            for (char in characters) {
                val angleRad = Math.toRadians(char.angleDegrees.toDouble()).toFloat()
                val currentDistDp = char.initialDistanceDp * (1f - convergenceProgress)

                // Individual subtle organic hovering & drifting offset
                val hoverPhase = (progress * 5f * Math.PI.toFloat()) + (char.angleDegrees * (Math.PI / 180f)).toFloat()
                val hoverOffsetDp = (sin(hoverPhase) * 6f * (1f - convergenceProgress)).dp
                val hoverSideDp = (cos(hoverPhase * 0.7f) * 4f * (1f - convergenceProgress)).dp

                val xOffset = currentDistDp * cos(angleRad) + hoverSideDp
                val yOffset = currentDistDp * sin(angleRad) + hoverOffsetDp

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = xOffset, y = yOffset)
                        .alpha(characterAlpha)
                        .scale((1f - convergenceProgress * 0.35f) * (if (convergenceProgress < 0.1f) ambientPulse.value else 1f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing outer aura
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .blur(14.dp)
                            .background(char.auraColor.copy(alpha = 0.55f), CircleShape)
                    )

                    // Frosted circular avatar container
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131D33).copy(alpha = 0.90f))
                            .border(1.8.dp, char.auraColor.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = char.iconRes),
                            contentDescription = char.name,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 3. CENTRAL HERO BRAND & SHIMMERING MILO LOGO
        // ---------------------------------------------------------------------
        if (logoAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-15).dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Central Glowing MILO Emblem
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient radial glow behind logo
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .blur(26.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFB078).copy(alpha = 0.50f),
                                        Color(0xFF00E5FF).copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // MILO Icon Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "MILO Logo",
                        modifier = Modifier
                            .size(135.dp)
                            .scale(if (progress > 0.72f) ambientPulse.value else 1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cinematic Metallic Shimmer Text "M I L O"
                val textShimmerBrush = remember(shimmerOffset) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            Color(0xFFFFB078), // Warm Milo Gold
                            Color.White,
                            Color(0xFF00E5FF), // Electric cyan highlight
                            Color.White.copy(alpha = 0.88f)
                        ),
                        start = Offset(shimmerOffset * 300f, 0f),
                        end = Offset((shimmerOffset + 1.0f) * 300f, 80f)
                    )
                }

                Text(
                    text = "MILO",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = textShimmerBrush,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline with smooth letter tracking expansion and gentle upward slide
                val taglineSlideY = ((1f - taglineAlpha) * 10f).dp
                Text(
                    text = "Find People. Do More Together.",
                    color = Color(0xFFF0F4F8).copy(alpha = 0.92f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (0.5 + progress * 0.8).sp,
                    modifier = Modifier
                        .offset(y = taglineSlideY)
                        .alpha(taglineAlpha)
                )
            }
        }

        // ---------------------------------------------------------------------
        // 4. ELEGANT SKIP BUTTON (Top-Right)
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .clickable { onFinished() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Skip",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}