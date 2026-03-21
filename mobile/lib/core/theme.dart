import 'package:flutter/material.dart';

ThemeData buildTheme() {
  final base = ThemeData.light(useMaterial3: true);
  return base.copyWith(
    colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
    appBarTheme: const AppBarTheme(centerTitle: true),
    navigationBarTheme:
        const NavigationBarThemeData(indicatorColor: Colors.tealAccent),
  );
}
