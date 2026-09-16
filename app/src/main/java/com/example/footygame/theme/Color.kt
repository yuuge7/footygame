package com.example.footygame.theme

import androidx.compose.ui.graphics.Color

// Night pitch
val Pitch = Color(0xFF0B3B2C)
val PitchStripe = Color(0xFF0E4332)
val Dugout = Color(0xFF06261B)
val DugoutRaised = Color(0xFF0D3427)

// Chalk markings and type
val Chalk = Color(0xFFEEF3EF)
val ChalkMuted = Color(0xFF9DB8AC)
val ChalkLine = Color(0x33EEF3EF)

// Scoreboard floodlight
val Floodlight = Color(0xFFFFC247)

// Sticker album
val StickerPaper = Color(0xFFF6F1E3)
val Ink = Color(0xFF14201A)
val InkMuted = Color(0xFF56625B)

val ResultWin = Color(0xFF5BE49B)
val ResultDraw = Color(0xFFD9CFAE)
val ResultLoss = Color(0xFFFF6B5E)

val FoilGold = listOf(Color(0xFFFAE08A), Color(0xFFC08A22), Color(0xFFF6D57C), Color(0xFFB37F1E))
val FoilSilver = listOf(Color(0xFFF1F3F5), Color(0xFF98A2AA), Color(0xFFE1E5E8), Color(0xFF8C959D))
val FoilBronze = listOf(Color(0xFFEDB891), Color(0xFF9A5A31), Color(0xFFDDA47D), Color(0xFF8A4F2A))

/** Plain matte frame for blind drafts, so the border can't give the rating away. */
val FoilHidden = listOf(Color(0xFFC9D2CD), Color(0xFF7F8C85), Color(0xFFB8C2BC), Color(0xFF6F7C75))

/** Sticker border tier: gold 90+, silver 85+, bronze below. */
fun foilFor(rating: Int): List<Color> = when {
    rating >= 90 -> FoilGold
    rating >= 85 -> FoilSilver
    else -> FoilBronze
}
