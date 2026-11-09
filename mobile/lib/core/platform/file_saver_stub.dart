import 'dart:typed_data';

/// Non-web platforms save to a temp file and open it (see openProtectedFile).
Future<bool> saveBytesToBrowser(Uint8List bytes, String name, String? mime) async => false;
