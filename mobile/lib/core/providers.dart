import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';

import 'config/app_config.dart';
import 'data/local_database.dart';
import 'data/sync_queue.dart';
import '../data/api/holodos_api_client.dart';
import '../data/repositories/catalog_repository.dart';
import '../data/repositories/inventory_repository.dart';
import '../data/repositories/notification_repository.dart';
import '../data/repositories/settings_repository.dart';
import '../data/repositories/shopping_repository.dart';

// ---------------------------------------------------------------------------
// Infrastructure
// ---------------------------------------------------------------------------

final appConfigProvider = Provider<AppConfig>((ref) => AppConfig.fromEnv());

final dioProvider = Provider<Dio>((ref) {
  final config = ref.watch(appConfigProvider);
  return Dio(BaseOptions(
    baseUrl: config.baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 15),
  ));
});

final localDatabaseProvider =
    Provider<LocalDatabase>((ref) => LocalDatabase());

final holodosApiProvider = Provider<HolodosApiClient>((ref) {
  final dio = ref.watch(dioProvider);
  return HolodosApiClient(dio);
});

// ---------------------------------------------------------------------------
// Legacy sync queue (kept for compatibility with existing inventory screen)
// ---------------------------------------------------------------------------

final syncQueueProvider = Provider<SyncQueue>((ref) {
  final api = ref.watch(holodosApiProvider);
  final db = ref.watch(localDatabaseProvider);
  return SyncQueue(api: api, database: db);
});

// ---------------------------------------------------------------------------
// Repositories
// ---------------------------------------------------------------------------

final catalogRepositoryProvider = Provider<CatalogRepository>((ref) {
  final api = ref.watch(holodosApiProvider);
  final db = ref.watch(localDatabaseProvider);
  return CatalogRepository(apiClient: api, localDatabase: db);
});

final inventoryRepositoryProvider = Provider<InventoryRepository>((ref) {
  final api = ref.watch(holodosApiProvider);
  final db = ref.watch(localDatabaseProvider);
  return InventoryRepository(apiClient: api, localDatabase: db);
});

final shoppingRepositoryProvider = Provider<ShoppingRepository>((ref) {
  final api = ref.watch(holodosApiProvider);
  final db = ref.watch(localDatabaseProvider);
  return ShoppingRepository(apiClient: api, localDatabase: db);
});

final notificationRepositoryProvider =
    Provider<NotificationRepository>((ref) {
  final api = ref.watch(holodosApiProvider);
  return NotificationRepository(apiClient: api);
});

final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  final api = ref.watch(holodosApiProvider);
  return SettingsRepository(apiClient: api);
});
