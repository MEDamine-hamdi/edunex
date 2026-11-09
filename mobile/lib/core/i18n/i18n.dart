import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'dictionary.dart';
import 'extra_dictionary.dart';

/// Language codes supported by the app (same three as the web app).
const supportedLangs = ['fr', 'en', 'ar'];

class I18n {
  /// Current language, readable from anywhere (non-widget code such as helpers).
  static String code = 'fr';
  static bool get isRtl => code == 'ar';
}

/// Translates a French source text (the same keys as the web dictionary).
/// Unknown texts are returned unchanged (French), exactly like the web app.
String tr(String text) {
  if (I18n.code == 'fr' || text.isEmpty) return text;
  final whole = _lookup(text);
  if (whole != null) return whole;
  // Composed texts such as "Étudiant : Ali" or "Absences : 2 · Retards : 1":
  // translate each " · " part, and the label before " : " in each part.
  if (text.contains(' · ')) {
    return text.split(' · ').map(_trPart).join(' · ');
  }
  return _trPart(text);
}

String _trPart(String part) {
  final whole = _lookup(part);
  if (whole != null) return whole;
  final i = part.indexOf(' : ');
  if (i > 0) {
    final label = _lookup(part.substring(0, i)) ?? part.substring(0, i);
    final rest = part.substring(i + 3);
    return '$label : ${_lookup(rest) ?? rest}';
  }
  return part;
}

String? _lookup(String text) {
  final trimmed = text.trim();
  final entry = kMobileDict[trimmed] ?? kWebDict[trimmed];
  if (entry == null) return null;
  final out = I18n.code == 'en' ? entry[0] : entry[1];
  return text.replaceFirst(trimmed, out);
}

/// Locale code used for dates and number formats.
String dateLocale() => switch (I18n.code) {
      'en' => 'en',
      'ar' => 'ar',
      _ => 'fr',
    };

class AppLang extends ChangeNotifier {
  final SharedPreferences prefs;
  String code;
  AppLang(this.prefs) : code = _initial(prefs) {
    I18n.code = code;
  }

  static String _initial(SharedPreferences p) {
    final v = p.getString('lang');
    return supportedLangs.contains(v) ? v! : 'fr';
  }

  Future<void> set(String c) async {
    if (!supportedLangs.contains(c) || c == code) return;
    code = c;
    I18n.code = c;
    await prefs.setString('lang', c);
    notifyListeners();
  }
}

/// Rebuilds every [Text] / helper widget that depends on it when the language changes.
class LangScope extends InheritedWidget {
  final String code;
  const LangScope({super.key, required this.code, required super.child});

  static String of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<LangScope>()?.code ?? I18n.code;

  @override
  bool updateShouldNotify(LangScope old) => old.code != code;
}
