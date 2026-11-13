class AppConfig {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://x7fgtsn35nmy0tzh1l2bclwr.15.235.92.116.sslip.io',
  );
}