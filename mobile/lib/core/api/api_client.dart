import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:path_provider/path_provider.dart';

/// Error raised for any non-2xx answer. Mirrors the backend format:
/// `{"error": "..."}` or `{"error":"Validation failed","fields":{...}}`.
class ApiException implements Exception {
  final int status;
  final String message;
  final Map<String, String> fields;
  final int? retryAfterSeconds;
  ApiException(this.status, this.message, {this.fields = const {}, this.retryAfterSeconds});

  bool get isForbidden => status == 403;
  bool get isUnauthorized => status == 401;

  @override
  String toString() {
    if (fields.isNotEmpty) {
      return fields.entries.map((e) => '${e.key} : ${e.value}').join('\n');
    }
    return message;
  }
}

class UploadFile {
  final String field;
  final String name;
  final List<int> bytes;
  final String? contentType;
  UploadFile(this.field, this.name, this.bytes, {this.contentType});
}

/// Thin HTTP layer over the existing EduNex REST API.
/// Every request carries `Authorization: Bearer <jwt>` (the backend's mobile path);
/// login additionally sends `X-Client: mobile` so the token is returned in the body.
class ApiClient {
  String baseUrl;
  String? token;
  void Function()? onUnauthorized;

  ApiClient({this.baseUrl = ''});

  static const _timeout = Duration(seconds: 30);

  Uri _uri(String path, [Map<String, dynamic>? query]) {
    final q = <String, String>{};
    query?.forEach((k, v) {
      if (v != null && v.toString().isNotEmpty) q[k] = v.toString();
    });
    final base = baseUrl.endsWith('/') ? baseUrl.substring(0, baseUrl.length - 1) : baseUrl;
    return Uri.parse('$base$path').replace(queryParameters: q.isEmpty ? null : q);
  }

  Map<String, String> _headers({bool json = true, bool mobile = false}) => {
        if (json) 'Content-Type': 'application/json',
        'Accept': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
        if (mobile) 'X-Client': 'mobile',
      };

  Future<dynamic> get(String path, {Map<String, dynamic>? query, Duration? timeout}) =>
      _run(() => http.get(_uri(path, query), headers: _headers()), timeout: timeout);

  Future<dynamic> post(String path, {Object? body, bool mobile = false, bool auth = true, Duration? timeout}) =>
      _run(() => http.post(_uri(path), headers: _headers(mobile: mobile), body: body == null ? null : jsonEncode(body)),
          auth: auth, timeout: timeout);

  Future<dynamic> put(String path, {Object? body}) =>
      _run(() => http.put(_uri(path), headers: _headers(), body: body == null ? null : jsonEncode(body)));

  Future<dynamic> patch(String path, {Object? body}) =>
      _run(() => http.patch(_uri(path), headers: _headers(), body: body == null ? null : jsonEncode(body)));

  Future<dynamic> delete(String path) => _run(() => http.delete(_uri(path), headers: _headers()));

  /// Multipart upload. [jsonParts] are sent as `application/json` parts (e.g. announcement "data").
  Future<dynamic> multipart(
    String path, {
    String method = 'POST',
    Map<String, String> fields = const {},
    Map<String, Object> jsonParts = const {},
    List<UploadFile> files = const [],
  }) {
    return _run(() async {
      final req = http.MultipartRequest(method, _uri(path));
      req.headers.addAll(_headers(json: false));
      req.fields.addAll(fields);
      jsonParts.forEach((name, value) {
        req.files.add(http.MultipartFile.fromString(name, jsonEncode(value),
            contentType: MediaType('application', 'json')));
      });
      for (final f in files) {
        req.files.add(http.MultipartFile.fromBytes(f.field, f.bytes,
            filename: f.name, contentType: f.contentType == null ? null : MediaType.parse(f.contentType!)));
      }
      final streamed = await req.send();
      return http.Response.fromStream(streamed);
    }, timeout: const Duration(minutes: 2));
  }

  /// Authorized binary read (photo, previews).
  Future<Uint8List> bytes(String path) async {
    final res = await _raw(() => http.get(_uri(path), headers: _headers(json: false)));
    return res.bodyBytes;
  }

  /// Downloads a protected file (course document, attachment, request file...) to a temp file.
  Future<File> download(String path, {String fallbackName = 'document'}) async {
    final res = await _raw(() => http.get(_uri(path), headers: _headers(json: false)));
    var name = fallbackName;
    final cd = res.headers['content-disposition'];
    if (cd != null) {
      final star = RegExp(r"filename\*=UTF-8''([^;]+)", caseSensitive: false).firstMatch(cd);
      final plain = RegExp(r'filename="?([^";]+)"?', caseSensitive: false).firstMatch(cd);
      if (star != null) {
        name = Uri.decodeComponent(star.group(1)!);
      } else if (plain != null) {
        name = plain.group(1)!;
      }
    }
    name = name.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
    final dir = await getTemporaryDirectory();
    final file = File('${dir.path}/$name');
    await file.writeAsBytes(res.bodyBytes, flush: true);
    return file;
  }

  Future<http.Response> _raw(Future<http.Response> Function() call, {Duration? timeout}) async {
    http.Response res;
    try {
      res = await call().timeout(timeout ?? _timeout);
    } on TimeoutException {
      throw ApiException(0, 'Le serveur ne rÃ©pond pas. RÃ©essayez.');
    } on SocketException {
      throw ApiException(0, 'Connexion impossible. VÃ©rifiez votre rÃ©seau et l\'adresse du serveur.');
    } on http.ClientException {
      throw ApiException(0, 'Connexion impossible. VÃ©rifiez votre rÃ©seau et l\'adresse du serveur.');
    }
    if (res.statusCode >= 200 && res.statusCode < 300) return res;
    throw _error(res);
  }

  Future<dynamic> _run(Future<http.Response> Function() call, {bool auth = true, Duration? timeout}) async {
    try {
      final res = await _raw(call, timeout: timeout);
      if (res.bodyBytes.isEmpty) return null;
      return jsonDecode(utf8.decode(res.bodyBytes));
    } on ApiException catch (e) {
      if (e.status == 401 && auth && token != null) onUnauthorized?.call();
      rethrow;
    }
  }

  ApiException _error(http.Response res) {
    String msg = '';
    Map<String, String> fields = {};
    int? retry;
    try {
      final body = jsonDecode(utf8.decode(res.bodyBytes));
      if (body is Map) {
        msg = (body['error'] ?? body['message'] ?? '').toString();
        final f = body['fields'];
        if (f is Map) fields = f.map((k, v) => MapEntry(k.toString(), v.toString()));
        final r = body['retryAfterSeconds'];
        if (r is num) retry = r.toInt();
      }
    } catch (_) {}
    retry ??= int.tryParse(res.headers['retry-after'] ?? '');
    if (msg.isEmpty) {
      msg = switch (res.statusCode) {
        401 => 'Session expirÃ©e ou identifiants incorrects.',
        403 => 'AccÃ¨s refusÃ© : vous n\'avez pas la permission pour cette action.',
        404 => 'Ressource introuvable.',
        409 => 'Conflit : cette opÃ©ration n\'est pas possible.',
        413 => 'Fichier trop volumineux.',
        429 => 'Trop de tentatives. RÃ©essayez plus tard.',
        503 => 'Service momentanÃ©ment indisponible.',
        _ => 'Erreur serveur (${res.statusCode}).',
      };
    }
    return ApiException(res.statusCode, msg, fields: fields, retryAfterSeconds: retry);
  }
}