import '../../core/data/local_database.dart';
import '../api/holodos_api_client.dart';
import '../models/dictionary_model.dart';
import '../models/product_model.dart';
import '../models/unit_model.dart';

/// Cache-first repository for catalog dictionaries and products.
///
/// Read pattern: return cached data immediately if present, then refresh in the
/// background.  Write operations (create / update / delete) go directly to the
/// API and invalidate the local cache for the affected collection.
class CatalogRepository {
  CatalogRepository({
    required this.apiClient,
    required this.localDatabase,
  });

  final HolodosApiClient apiClient;
  final LocalDatabase localDatabase;

  // ---------------------------------------------------------------------------
  // Storage Places
  // ---------------------------------------------------------------------------

  Future<List<DictionaryModel>> cachedStoragePlaces() async {
    final rows = await localDatabase.loadDictionary('storage_places');
    return rows.map(DictionaryModel.fromDb).toList();
  }

  Future<List<DictionaryModel>> refreshStoragePlaces() async {
    final remote = await apiClient.fetchStoragePlaces();
    await localDatabase.replaceDictionary(
      'storage_places',
      remote.map((e) => e.toDb('storage_places')).toList(),
    );
    return remote;
  }

  Future<DictionaryModel> createStoragePlace(
      Map<String, dynamic> body) async {
    final result = await apiClient.createStoragePlace(body);
    await refreshStoragePlaces();
    return result;
  }

  Future<DictionaryModel> updateStoragePlace(
      int id, Map<String, dynamic> body) async {
    final result = await apiClient.updateStoragePlace(id, body);
    await refreshStoragePlaces();
    return result;
  }

  Future<void> deleteStoragePlace(int id) async {
    await apiClient.deleteStoragePlace(id);
    await refreshStoragePlaces();
  }

  // ---------------------------------------------------------------------------
  // Categories
  // ---------------------------------------------------------------------------

  Future<List<DictionaryModel>> cachedCategories() async {
    final rows = await localDatabase.loadDictionary('categories');
    return rows.map(DictionaryModel.fromDb).toList();
  }

  Future<List<DictionaryModel>> refreshCategories() async {
    final remote = await apiClient.fetchCategories();
    await localDatabase.replaceDictionary(
      'categories',
      remote.map((e) => e.toDb('categories')).toList(),
    );
    return remote;
  }

  Future<DictionaryModel> createCategory(Map<String, dynamic> body) async {
    final result = await apiClient.createCategory(body);
    await refreshCategories();
    return result;
  }

  Future<DictionaryModel> updateCategory(
      int id, Map<String, dynamic> body) async {
    final result = await apiClient.updateCategory(id, body);
    await refreshCategories();
    return result;
  }

  Future<void> deleteCategory(int id) async {
    await apiClient.deleteCategory(id);
    await refreshCategories();
  }

  // ---------------------------------------------------------------------------
  // Stores
  // ---------------------------------------------------------------------------

  Future<List<DictionaryModel>> cachedStores() async {
    final rows = await localDatabase.loadDictionary('stores');
    return rows.map(DictionaryModel.fromDb).toList();
  }

  Future<List<DictionaryModel>> refreshStores() async {
    final remote = await apiClient.fetchStores();
    await localDatabase.replaceDictionary(
      'stores',
      remote.map((e) => e.toDb('stores')).toList(),
    );
    return remote;
  }

  Future<DictionaryModel> createStore(Map<String, dynamic> body) async {
    final result = await apiClient.createStore(body);
    await refreshStores();
    return result;
  }

  Future<DictionaryModel> updateStore(
      int id, Map<String, dynamic> body) async {
    final result = await apiClient.updateStore(id, body);
    await refreshStores();
    return result;
  }

  Future<void> deleteStore(int id) async {
    await apiClient.deleteStore(id);
    await refreshStores();
  }

  // ---------------------------------------------------------------------------
  // Units
  // ---------------------------------------------------------------------------

  Future<List<UnitModel>> cachedUnits() async {
    final rows = await localDatabase.loadUnits();
    return rows.map(UnitModel.fromDb).toList();
  }

  Future<List<UnitModel>> refreshUnits() async {
    final remote = await apiClient.fetchUnits();
    await localDatabase.replaceUnits(remote.map((e) => e.toDb()).toList());
    return remote;
  }

  Future<UnitModel> createUnit(Map<String, dynamic> body) async {
    final result = await apiClient.createUnit(body);
    await refreshUnits();
    return result;
  }

  Future<UnitModel> updateUnit(int id, Map<String, dynamic> body) async {
    final result = await apiClient.updateUnit(id, body);
    await refreshUnits();
    return result;
  }

  Future<void> deleteUnit(int id) async {
    await apiClient.deleteUnit(id);
    await refreshUnits();
  }

  // ---------------------------------------------------------------------------
  // Products
  // ---------------------------------------------------------------------------

  Future<List<ProductModel>> cachedProducts({String? search}) async {
    final rows = await localDatabase.loadProducts(search: search);
    return rows.map(ProductModel.fromDb).toList();
  }

  Future<List<ProductModel>> refreshProducts({
    String? search,
    int page = 0,
    int size = 100,
  }) async {
    final remote =
        await apiClient.fetchProducts(search: search, page: page, size: size);
    // Only replace the full cache when fetching without a search filter.
    if (search == null || search.isEmpty) {
      await localDatabase
          .replaceProducts(remote.map((e) => e.toDb()).toList());
    }
    return remote;
  }

  Future<ProductModel> createProduct(Map<String, dynamic> body) async {
    final result = await apiClient.createProduct(body);
    await refreshProducts();
    return result;
  }

  Future<ProductModel> updateProduct(
      int id, Map<String, dynamic> body) async {
    final result = await apiClient.updateProduct(id, body);
    await refreshProducts();
    return result;
  }

  Future<void> deleteProduct(int id) async {
    await apiClient.deleteProduct(id);
    await refreshProducts();
  }
}
