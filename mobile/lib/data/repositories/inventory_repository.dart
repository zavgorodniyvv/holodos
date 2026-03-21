import '../../core/data/local_database.dart';
import '../api/holodos_api_client.dart';
import '../models/stock_entry_model.dart';

class InventoryRepository {
  InventoryRepository({required this.apiClient, required this.localDatabase});

  final HolodosApiClient apiClient;
  final LocalDatabase localDatabase;

  Future<List<StockEntryModel>> cachedEntries() async {
    final rows = await localDatabase.loadStockEntries();
    return rows.map(StockEntryModel.fromDb).toList();
  }

  Future<List<StockEntryModel>> refreshEntries() async {
    final remote = await apiClient.fetchStockEntries();
    await localDatabase
        .replaceStockEntries(remote.map((e) => e.toDb()).toList());
    return remote;
  }
}
