import 'package:holodos_mobile/core/config/platform_env_stub.dart'
    if (dart.library.io) 'package:holodos_mobile/core/config/platform_env_native.dart';

class AppConfig {
  const AppConfig({required this.baseUrl});

  final String baseUrl;

  factory AppConfig.fromEnv() {
    final baseUrl =
        getEnvVariable('HOLODOS_API') ?? 'http://localhost:8080/api';
    return AppConfig(baseUrl: baseUrl);
  }
}
