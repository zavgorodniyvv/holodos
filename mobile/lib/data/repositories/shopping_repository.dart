import '../../core/data/local_database.dart';
import '../api/holodos_api_client.dart';
import '../models/shopping_item_model.dart';

class ShoppingRepository {
  ShoppingRepository({required this.apiClient, required this.localDatabase});

  final HolodosApiClient apiClient;
  final LocalDatabase localDatabase;

  Future<List<ShoppingItemModel>> cachedItems() async {
    final rows = await localDatabase.loadShoppingItems();
    return rows.map(ShoppingItemModel.fromDb).toList();
  }

  Future<List<ShoppingItemModel>> refreshActiveItems() async {
    final remote = await apiClient.fetchShoppingItems();
    await localDatabase
        .replaceShoppingItems(remote.map((e) => e.toDb()).toList());
    return remote;
  }
}
