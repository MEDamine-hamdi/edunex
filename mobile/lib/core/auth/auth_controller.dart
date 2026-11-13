import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../api/api_client.dart';
import '../config.dart';

class SessionUser {
  final int id;
  final String email;
  final String firstName;
  final String lastName;
  final String role; // ROLE_STUDENT | ROLE_PROFESSOR | ROLE_ADMIN_AGENT | ROLE_PLATFORM_ADMIN
  /// True while the account still has its e-mailed temporary password.
  bool mustChangePassword;
  SessionUser(this.id, this.email, this.firstName, this.lastName, this.role, {this.mustChangePassword = false});

  String get fullName => '$firstName $lastName'.trim();
  String get initials =>
      ((firstName.isNotEmpty ? firstName[0] : '') + (lastName.isNotEmpty ? lastName[0] : '')).toUpperCase();

  factory SessionUser.fromJson(Map<String, dynamic> j) => SessionUser(
        (j['id'] as num?)?.toInt() ?? 0,
        (j['email'] ?? '').toString(),
        (j['firstName'] ?? '').toString(),
        (j['lastName'] ?? '').toString(),
        (j['role'] ?? '').toString(),
        mustChangePassword: j['mustChangePassword'] == true,
      );
}

/// Owns the JWT, the signed-in user and the base URL.
/// Token lives in the platform keystore (flutter_secure_storage), never in plain preferences.
class AuthController extends ChangeNotifier {
  final ApiClient api;
  AuthController(this.api) {
    api.onUnauthorized = _expired;
    api.onPasswordChangeRequired = () {
      if (user != null && !user!.mustChangePassword) {
        user!.mustChangePassword = true;
        notifyListeners();
      }
    };
  }

  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  SessionUser? user;
  bool booting = true;
  String? notice;

  bool get signedIn => user != null;

  Future<void> boot() async {
    api.baseUrl = AppConfig.baseUrl;
    try {
      final token = await _storage.read(key: 'jwt');
      if (token != null) {
        api.token = token;
        final me = await api.get('/api/users/me') as Map<String, dynamic>;
        user = SessionUser.fromJson(me);
      }
    } on ApiException catch (e) {
      // 401 => token expired/revoked; network errors keep the stored token for the next try.
      if (e.status == 401 || e.status == 403) await _clear();
      if (e.status == 0) {
        api.token = null;
        notice = e.message;
      }
    } catch (_) {
      api.token = null;
    }
    booting = false;
    notifyListeners();
  }

  Future<void> login(String email, String password, {required bool remember}) async {
    notice = null;
    final res = await api.post(
      '/api/auth/login',
      body: {'email': email.trim(), 'password': password, 'remember': remember},
      mobile: true, // X-Client: mobile => token returned in the body
      auth: false,
    ) as Map<String, dynamic>;
    final token = res['token'] as String?;
    if (token == null || token.isEmpty) {
      throw ApiException(0, 'Le serveur n\'a pas renvoyé de jeton (X-Client: mobile non pris en charge ?).');
    }
    api.token = token;
    if (remember) {
      await _storage.write(key: 'jwt', value: token);
    } else {
      await _storage.delete(key: 'jwt');
    }
    user = SessionUser.fromJson(res);
    notifyListeners();
  }

  Future<void> logout() async {
    try {
      await api.post('/api/auth/logout');
    } catch (_) {}
    await _clear();
    notifyListeners();
  }

  /// After a password change every token is revoked by the backend: sign out cleanly.
  Future<void> forceSignOut(String message) async {
    await _clear();
    notice = message;
    notifyListeners();
  }

  void passwordChanged() {
    user?.mustChangePassword = false;
    notifyListeners();
  }

  void _expired() {
    if (user == null) return;
    forceSignOut('Votre session a expiré. Veuillez vous reconnecter.');
  }

  Future<void> _clear() async {
    api.token = null;
    user = null;
    await _storage.delete(key: 'jwt');
  }

  void updateName(String first, String last) {
    final u = user;
    if (u == null) return;
    user = SessionUser(u.id, u.email, first, last, u.role, mustChangePassword: u.mustChangePassword);
    notifyListeners();
  }
}
