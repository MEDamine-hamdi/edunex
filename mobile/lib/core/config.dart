class AppConfig {
  /// Android emulator -> host machine. Users can change the server address on the login screen.
  static const defaultBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');
}
