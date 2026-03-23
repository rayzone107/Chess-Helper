package com.rachitgoyal.chesshelper.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the overlay colour palette.
 *
 * Palette is based on Tailwind CSS Slate scale, shifted up two
 * stops from the original (900/800/700 → 800/700/600) so that
 * dark-coloured pieces remain clearly visible on every surface.
 *
 * ── Base surfaces ──────────────────────────────────────────────
 *   CardBackground  – the main overlay card
 *   Surface         – secondary panels / buttons inside the card
 *   SurfaceHigh     – elevated surfaces that need more contrast
 *   Divider         – separators between sections
 *
 * ── Text ───────────────────────────────────────────────────────
 *   PrimaryText     – main readable text
 *   SecondaryText   – muted / caption text
 *
 * ── Pieces ─────────────────────────────────────────────────────
 *   WhitePiece      – colour used to render white chess pieces
 *   BlackPiece      – colour used to render black chess pieces
 *
 * ── Board indicators ───────────────────────────────────────────
 *   SelectedSquare, LegalTarget, LastMoveHighlight,
 *   CheckBorder, RecommendedMoveArrow
 *
 * ── Config / Board-setup mode ──────────────────────────────────
 *   ConfigAccent, ConfigCardBackground
 *
 * ── Semantic / status ──────────────────────────────────────────
 *   Error, ErrorDark, Warning, StalemateBackground, InfoText,
 *   ErrorTextLight, DiscardText
 */
object OverlayColors {

    // ── Base surfaces (Tailwind Slate shifted +2) ──────────────
    val CardBackground   = Color(0xFF1E293B)   // slate-800
    val Surface          = Color(0xFF3A5273)   // slate-700
    val SurfaceHigh      = Color(0xFF475569)   // slate-600
    val Divider          = Color(0xFF64748B)   // slate-500

    // ── Text ───────────────────────────────────────────────────
    val PrimaryText      = Color.White
    val SecondaryText    = Color(0xFFCBD5E1)   // slate-300
    val DisabledText     = Color(0xFF94A3B8)   // slate-400

    // ── Pieces ─────────────────────────────────────────────────
    val WhitePiece       = Color.White
    val BlackPiece       = Color(0xFF0F172A)   // slate-900

    // ── Board indicators ───────────────────────────────────────
    val SelectedSquare       = Color(0xFF2563EB)
    val LegalTargetSquare    = Color(0xFFB8E1FF)
    val LegalTargetDot       = Color(0xCC0F172A)
    val LegalTargetRing      = Color(0xFF0F172A)
    val LastMoveHighlight    = Color(0xFFFACC15)
    val CheckBorder          = Color(0xFFEF4444)
    val RecommendedMoveArrow = Color(0xFF22C55E)
    val CoordinateLabel      = Color(0xAA0F172A)

    // ── Config / board-setup mode ──────────────────────────────
    val ConfigAccent         = Color(0xFFF59E0B)   // amber-500
    val ConfigCardBackground = Color(0xFF4A3314)   // warm dark amber
    val ConfigDoneText       = Color(0xFF1A1200)

    // ── Semantic / status ──────────────────────────────────────
    val Error            = Color(0xFFDC2626)
    val ErrorDark        = Color(0xFF7F1D1D)
    val WarningDark      = Color(0xFF7C2D12)
    val StalemateBackground = Color(0xFF475569)
    val InfoText         = Color(0xFFE2E8F0)
    val ErrorTextLight   = Color(0xFFFCA5A5)
    val DiscardText      = Color(0xFFFCA5A5)
    val SuccessGreen     = Color(0xFF22C55E)

    // ── Home screen gradient bottom ────────────────────────────
    val HomeGradientEnd  = Color(0xFF1E293B)   // matches CardBackground
}

