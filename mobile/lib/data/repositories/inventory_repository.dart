import '../../core/data/local_database.dart';
import '../api/holodos_api_client.dart';
import '../models/stock_entry_model.dart';

class InventoryRepository {
  InventoryRepository({
    required this.apiClient,
    required this.localDatabase,
  });

  final HolodosApiClient apiClient;
  final LocalDatabase localDatabase;

  // ---------------------------------------------------------------------------
  // Read — cache-first
  // ---------------------------------------------------------------------------

  Future<List<StockEntryModel>> cachedEntries({
    String? status,
    int? storagePlaceId,
  }) async {
    final rows = await localDatabase.loadStockEntries(
      status: status,
      storagePlaceId: storagePlaceId,
    );
    return rows.map(StockEntryModel.fromDb).toList();
  }

  Future<List<StockEntryModel>> refreshEntries({
    String? status,
    int? storagePlaceId,
    String? search,
    int size = 50,
  }) async {
    final remote = await apiClient.fetchStockEntries(
      status: status,
      storagePlaceId: storagePlaceId,
      search: search,
      size: size,
    );
    // Only replace the full cache when fetching without filters.
    if (status == null && storagePlaceId == null && search == null) {
      await localDatabase
          .replaceStockEntries(remote.map((e) => e.toDb()).toList());
    }
    return remote;
  }

  // ---------------------------------------------------------------------------
  // Write — direct API calls
  // ---------------------------------------------------------------------------

  Future<StockEntryModel> addStock({
    required int productId,
    required double quantity,
    required int unitId,
    required int storagePlaceId,
    String? addedAt,
    String? purchasedAt,
    String? expiresAt,
    String? comment,
  }) async {
    final entry = await apiClient.addStock({
      'productId': productId,
      'quantity': quantity,
      'unitId': unitId,
      'storagePlaceId': storagePlaceId,
      if (addedAt != null) 'addedAt': addedAt,
      if (purchasedAt != null) 'purchasedAt': purchasedAt,
      if (expiresAt != null) 'expiresAt': expiresAt,
      if (comment != null) 'comment': comment,
    });
    return entry;
  }

  Future<void> consumeStock(int id, double quantity) =>
      apiClient.consumeStock(id, quantity);

  Future<void> discardStock(int id) => apiClient.discardStock(id);

  Future<void> moveStock({
    required int id,
    required int toStoragePlaceId,
    double? quantity,
    String? comment,
    String? username,
  }) =>
      apiClient.moveStock(id, {
        'toStoragePlaceId': toStoragePlaceId,
        if (quantity != null) 'quantity': quantity,
        if (comment != null) 'comment': comment,
        if (username != null) 'username': username,
      });

  Future<void> adjustStock({
    required int id,
    required double delta,
    required String reason,
    String? comment,
    String? username,
    String? adjustedAt,
  }) =>
      apiClient.adjustStock(id, {
        'delta': delta,
        'reason': reason,
        if (comment != null) 'comment': comment,
        if (username != null) 'username': username,
        if (adjustedAt != null) 'adjustedAt': adjustedAt,
      });
}
