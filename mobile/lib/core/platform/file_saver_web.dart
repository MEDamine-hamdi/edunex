import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

/// Triggers a browser download for an already-fetched (authorized) file.
Future<bool> saveBytesToBrowser(Uint8List bytes, String name, String? mime) async {
  final blob = web.Blob(
    [bytes.toJS].toJS,
    web.BlobPropertyBag(type: mime ?? 'application/octet-stream'),
  );
  final url = web.URL.createObjectURL(blob);
  final a = web.HTMLAnchorElement()
    ..href = url
    ..download = name;
  a.click();
  web.URL.revokeObjectURL(url);
  return true;
}
