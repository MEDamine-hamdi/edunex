import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ThemeController extends ChangeNotifier {
  final SharedPreferences prefs;
  ThemeMode mode;
  ThemeController(this.prefs)
      : mode = switch (prefs.getString('theme')) {
          'dark' => ThemeMode.dark,
          'light' => ThemeMode.light,
          _ => ThemeMode.system,
        };
  Future<void> set(ThemeMode m) async {
    mode = m;
    await prefs.setString('theme', m.name);
    notifyListeners();
  }
}