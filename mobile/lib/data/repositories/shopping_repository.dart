import '../../core/data/local_database.dart';
import '../api/holodos_api_client.dart';
import '../models/shopping_item_model.dart';

class ShoppingRepository {
  ShoppingRepository({
    required this.apiClient,
    required this.localDatabase,
  });

  final HolodosApiClient apiClient;
  final LocalDatabase localDatabase;

  // ---------------------------------------------------------------------------
  // Read — cache-first
  // ---------------------------------------------------------------------------

  Future<List<ShoppingItemModel>> cachedItems({String? status}) async {
    final rows = await localDatabase.loadShoppingItems(status: status);
    return rows.map(ShoppingItemModel.fromDb).toList();
  }

  Future<List<ShoppingItemModel>> refreshActiveItems({
    String? storeId,
    String? search,
    int size = 50,
  }) async {
    final remote = await apiClient.fetchShoppingItems(
      status: 'ACTIVE',
      search: search,
      size: size,
    );
    // Only replace the cache when fetching the plain active list.
    if (search == null) {
      await localDatabase
          .replaceShoppingItems(remote.map((e) => e.toDb()).toList());
    }
    return remote;
  }

  // ---------------------------------------------------------------------------
  // Write — direct API calls
  // ---------------------------------------------------------------------------

  Future<ShoppingItemModel> createItem({
    int? productId,
    required String title,
    required double quantity,
    int? unitId,
    int? storeId,
    String? source,
    String? comment,
    int sortOrder = 0,
  }) async {
    return apiClient.createShoppingItem({
      if (productId != null) 'productId': productId,
      'title': title,
      'quantity': quantity,
      if (unitId != null) 'unitId': unitId,
      if (storeId != null) 'storeId': storeId,
      'source': source ?? 'MANUAL',
      if (comment != null) 'comment': comment,
      'sortOrder': sortOrder,
    });
  }

  Future<ShoppingItemModel> updateItem(
      int id, Map<String, dynamic> body) async {
    return apiClient.updateShoppingItem(id, body);
  }

  Future<void> completeItem(int id) => apiClient.completeShoppingItem(id);
}
