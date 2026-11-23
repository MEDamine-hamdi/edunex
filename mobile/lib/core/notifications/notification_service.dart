import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// System notifications (Android). New server notifications and new messages are
/// detected by the app's regular refresh and shown in the phone's notification bar.
/// On other platforms (web, desktop) every call is a safe no-op.
class NotificationService {
  NotificationService._();

  static final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  static bool _ready = false;

  static bool get supported => !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  static Future<void> init() async {
    if (!supported || _ready) return;
    try {
      await _plugin.initialize(const InitializationSettings(android: AndroidInitializationSettings('@mipmap/ic_launcher')));
      final android = _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
      await android?.requestNotificationsPermission();
      _ready = true;
    } catch (e) {
      debugPrint('Notifications unavailable: $e');
    }
  }

  static Future<void> show(int id, String title, String body) async {
    if (!_ready) return;
    try {
      await _plugin.show(
        id,
        title,
        body,
        const NotificationDetails(
          android: AndroidNotificationDetails(
            'edunex_main',
            'EduNex',
            channelDescription: 'Notifications and messages',
            importance: Importance.high,
            priority: Priority.high,
          ),
        ),
      );
    } catch (e) {
      debugPrint('Notification not shown: $e');
    }
  }
}
