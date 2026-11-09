import 'package:flutter/widgets.dart' as w;

import 'i18n.dart';

/// Drop-in replacement for Flutter's Text that translates its French source string
/// with the current language (see [tr]). Dynamic strings with no dictionary entry stay as written.
class Text extends w.StatelessWidget {
  final String data;
  final w.TextStyle? style;
  final w.TextAlign? textAlign;
  final int? maxLines;
  final w.TextOverflow? overflow;
  final bool? softWrap;
  const Text(this.data, {super.key, this.style, this.textAlign, this.maxLines, this.overflow, this.softWrap});

  @override
  w.Widget build(w.BuildContext context) {
    LangScope.of(context); // registers the dependency
    return w.Text(tr(data), style: style, textAlign: textAlign, maxLines: maxLines, overflow: overflow, softWrap: softWrap);
  }
}
