import 'package:flutter/material.dart' hide Text;
import 'package:flutter/material.dart' as m show Text;
import 'package:provider/provider.dart';

import 'i18n.dart';

/// FR / EN / AR selector. Labels are native names and are never translated.
class LanguageSwitcher extends StatelessWidget {
  final bool compact;
  const LanguageSwitcher({super.key, this.compact = false});

  @override
  Widget build(BuildContext context) {
    final lang = context.watch<AppLang>();
    if (compact) {
      return PopupMenuButton<String>(
        tooltip: 'Language',
        icon: const Icon(Icons.translate),
        onSelected: lang.set,
        itemBuilder: (_) => const [
          PopupMenuItem(value: 'fr', child: m.Text('Français')),
          PopupMenuItem(value: 'en', child: m.Text('English')),
          PopupMenuItem(value: 'ar', child: m.Text('العربية')),
        ],
      );
    }
    return SegmentedButton<String>(
      showSelectedIcon: false,
      segments: const [
        ButtonSegment(value: 'fr', label: m.Text('FR')),
        ButtonSegment(value: 'en', label: m.Text('EN')),
        ButtonSegment(value: 'ar', label: m.Text('AR')),
      ],
      selected: {lang.code},
      onSelectionChanged: (s) => lang.set(s.first),
    );
  }
}
