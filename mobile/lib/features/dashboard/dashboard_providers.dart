import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/dictionary_model.dart';
import '../../data/models/shopping_item_model.dart';
import '../../data/models/stock_entry_model.dart';

/// All available stock entries (AVAILABLE status), cache-first.
///
/// Loads from SQLite immediately, then triggers a background refresh so the
/// next rebuild shows fresh data without a loading spinner.
final dashboardStockProvider =
    FutureProvider<List<StockEntryModel>>((ref) async {
  final repo = ref.watch(inventoryRepositoryProvider);
  final cached = await repo.cachedEntries(status: 'AVAILABLE');
  if (cached.isNotEmpty) {
    // Return cached immediately; refresh runs in background.
    repo
        .refreshEntries(status: 'AVAILABLE')
        // ignore: invalid_return_type_for_catch_error
        .catchError((_) => <StockEntryModel>[]);
    return cached;
  }
  return repo.refreshEntries(status: 'AVAILABLE');
});

/// Active shopping items, cache-first.
final dashboardShoppingProvider =
    FutureProvider<List<ShoppingItemModel>>((ref) async {
  final repo = ref.watch(shoppingRepositoryProvider);
  final cached = await repo.cachedItems(status: 'ACTIVE');
  if (cached.isNotEmpty) {
    repo
        .refreshActiveItems()
        // ignore: invalid_return_type_for_catch_error
        .catchError((_) => <ShoppingItemModel>[]);
    return cached;
  }
  return repo.refreshActiveItems();
});

/// Storage places from catalog, cache-first.
final storagePlacesProvider =
    FutureProvider<List<DictionaryModel>>((ref) async {
  final repo = ref.watch(catalogRepositoryProvider);
  final cached = await repo.cachedStoragePlaces();
  if (cached.isNotEmpty) {
    repo
        .refreshStoragePlaces()
        // ignore: invalid_return_type_for_catch_error
        .catchError((_) => <DictionaryModel>[]);
    return cached;
  }
  return repo.refreshStoragePlaces();
});
