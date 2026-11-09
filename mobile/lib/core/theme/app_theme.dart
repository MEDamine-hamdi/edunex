import 'package:flutter/material.dart';

/// Palette lifted from the EduNex web app (Tailwind: blue-600, indigo-600, slate neutrals, violet logo gradient).
class EduColors {
  static const primary = Color(0xFF2563EB);
  static const indigo = Color(0xFF4F46E5);
  static const violet = Color(0xFF7C3AED);
  static const slate50 = Color(0xFFF8FAFC);
  static const slate100 = Color(0xFFF1F5F9);
  static const slate200 = Color(0xFFE2E8F0);
  static const slate400 = Color(0xFF94A3B8);
  static const slate500 = Color(0xFF64748B);
  static const slate700 = Color(0xFF334155);
  static const slate900 = Color(0xFF0F172A);
  static const emerald = Color(0xFF059669);
  static const orange = Color(0xFFEA580C);
  static const red = Color(0xFFDC2626);
  static const sky = Color(0xFF0284C7);
}

ThemeData buildTheme(Brightness b) {
  final dark = b == Brightness.dark;
  final bg = dark ? const Color(0xFF000000) : EduColors.slate50;
  final card = dark ? const Color(0xFF0B0B0C) : Colors.white;
  final border = dark ? const Color(0xFF27272A) : EduColors.slate200;
  final scheme = ColorScheme.fromSeed(seedColor: EduColors.primary, brightness: b).copyWith(
    primary: EduColors.primary,
    surface: card,
    error: EduColors.red,
  );
  return ThemeData(
    useMaterial3: true,
    brightness: b,
    colorScheme: scheme,
    scaffoldBackgroundColor: bg,
    dividerColor: border,
    appBarTheme: AppBarTheme(
      backgroundColor: card,
      foregroundColor: dark ? Colors.white : EduColors.slate900,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w800,
        color: dark ? Colors.white : EduColors.slate900,
      ),
      shape: Border(bottom: BorderSide(color: border)),
    ),
    cardTheme: CardThemeData(
      color: card,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: BorderSide(color: border)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: card,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: border)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: border)),
      focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: EduColors.primary, width: 1.6)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: EduColors.primary,
        foregroundColor: Colors.white,
        minimumSize: const Size(0, 48),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        textStyle: const TextStyle(fontWeight: FontWeight.w700),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(0, 44),
        side: BorderSide(color: border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        textStyle: const TextStyle(fontWeight: FontWeight.w600),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: card,
      indicatorColor: EduColors.primary.withValues(alpha: dark ? 0.25 : 0.12),
      labelTextStyle: WidgetStateProperty.all(const TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
    ),
    chipTheme: ChipThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      side: BorderSide(color: border),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
  );
}
