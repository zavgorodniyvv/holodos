import 'dart:io';

class AppConfig {
  const AppConfig({required this.baseUrl});

  final String baseUrl;

  factory AppConfig.fromEnv() {
    final envBase =
        Platform.environment['HOLODOS_API'] ?? 'http://localhost:8080/api';
    return AppConfig(baseUrl: envBase);
  }
}
