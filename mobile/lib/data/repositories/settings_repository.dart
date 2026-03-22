import '../api/holodos_api_client.dart';
import '../models/settings_model.dart';

/// Settings are not cached locally — they are always fetched from the backend.
class SettingsRepository {
  SettingsRepository({required this.apiClient});

  final HolodosApiClient apiClient;

  Future<SettingsModel> fetch({String userKey = 'default'}) =>
      apiClient.fetchSettings(userKey: userKey);

  Future<SettingsModel> save(SettingsModel settings) =>
      apiClient.updateSettings(settings);
}
