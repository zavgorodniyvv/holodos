import 'package:dio/dio.dart';

import '../models/dictionary_model.dart';
import '../models/notification_model.dart';
import '../models/product_model.dart';
import '../models/settings_model.dart';
import '../models/shopping_item_model.dart';
import '../models/stock_entry_model.dart';
import '../models/unit_model.dart';

class HolodosApiClient {
  HolodosApiClient(this._dio);

  final Dio _dio;

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /// Extracts a list from either a Page response `{content: [...]}` or a
  /// bare JSON array.
  List<dynamic> _pageContent(dynamic data) {
    if (data is Map<String, dynamic>) {
      return data['content'] as List<dynamic>? ?? [];
    }
    if (data is List<dynamic>) return data;
    return [];
  }

  // ---------------------------------------------------------------------------
  // Catalog — Storage Places
  // ---------------------------------------------------------------------------

  Future<List<DictionaryModel>> fetchStoragePlaces() async {
    final res = await _dio.get('/storage-places');
    return _pageContent(res.data)
        .map((e) => DictionaryModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<DictionaryModel> createStoragePlace(
      Map<String, dynamic> body) async {
    final res = await _dio.post('/storage-places', data: body);
    return DictionaryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<DictionaryModel> updateStoragePlace(
      int id, Map<String, dynamic> body) async {
    final res = await _dio.put('/storage-places/$id', data: body);
    return DictionaryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> deleteStoragePlace(int id) async {
    await _dio.delete('/storage-places/$id');
  }

  // ---------------------------------------------------------------------------
  // Catalog — Categories
  // ---------------------------------------------------------------------------

  Future<List<DictionaryModel>> fetchCategories() async {
    final res = await _dio.get('/categories');
    return _pageContent(res.data)
        .map((e) => DictionaryModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<DictionaryModel> createCategory(Map<String, dynamic> body) async {
    final res = await _dio.post('/categories', data: body);
    return DictionaryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<DictionaryModel> updateCategory(
      int id, Map<String, dynamic> body) async {
    final res = await _dio.put('/categories/$id', data: body);
    return DictionaryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> deleteCategory(int id) async {
    await _dio.delete('/categories/$id');
  }

  // ---------------------------------------------------------------------------
  // Catalog — Stores
  // ---------------------------------------------------------------------------

  Future<List<DictionaryModel>> fetchStores() async {
    final res = await _dio.get('/stores');
    return _pageContent(res.data)
        .map((e) => DictionaryModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<DictionaryModel> createStore(Map<String, dynamic> body) async {
    final res = await _dio.post('/stores', data: body);
    return DictionaryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<DictionaryModel> updateStore(
      int id, Map<String, dynamic> body) async {
    final res = await _dio.put('/stores/$id', data: body);
    return DictionaryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> deleteStore(int id) async {
    await _dio.delete('/stores/$id');
  }

  // ---------------------------------------------------------------------------
  // Catalog — Units
  // ---------------------------------------------------------------------------

  Future<List<UnitModel>> fetchUnits() async {
    final res = await _dio.get('/units');
    return _pageContent(res.data)
        .map((e) => UnitModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<UnitModel> createUnit(Map<String, dynamic> body) async {
    final res = await _dio.post('/units', data: body);
    return UnitModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<UnitModel> updateUnit(int id, Map<String, dynamic> body) async {
    final res = await _dio.put('/units/$id', data: body);
    return UnitModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> deleteUnit(int id) async {
    await _dio.delete('/units/$id');
  }

  // ---------------------------------------------------------------------------
  // Catalog — Products
  // ---------------------------------------------------------------------------

  Future<List<ProductModel>> fetchProducts({
    String? search,
    int page = 0,
    int size = 20,
  }) async {
    final res = await _dio.get('/products', queryParameters: {
      if (search != null && search.isNotEmpty) 'search': search,
      'page': page,
      'size': size,
    });
    return _pageContent(res.data)
        .map((e) => ProductModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ProductModel> createProduct(Map<String, dynamic> body) async {
    final res = await _dio.post('/products', data: body);
    return ProductModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ProductModel> updateProduct(
      int id, Map<String, dynamic> body) async {
    final res = await _dio.put('/products/$id', data: body);
    return ProductModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> deleteProduct(int id) async {
    await _dio.delete('/products/$id');
  }

  // ---------------------------------------------------------------------------
  // Inventory — Stock Entries
  // ---------------------------------------------------------------------------

  Future<List<StockEntryModel>> fetchStockEntries({
    String? status,
    int? storagePlaceId,
    String? search,
    int size = 50,
  }) async {
    final res = await _dio.get('/stock-entries', queryParameters: {
      if (status != null) 'status': status,
      if (storagePlaceId != null) 'storagePlaceId': storagePlaceId,
      if (search != null && search.isNotEmpty) 'search': search,
      'size': size,
    });
    return _pageContent(res.data)
        .map((e) => StockEntryModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<StockEntryModel> addStock(Map<String, dynamic> body) async {
    final res = await _dio.post('/stock-entries', data: body);
    return StockEntryModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> consumeStock(int id, double quantity) async {
    await _dio.post('/stock-entries/$id/consume', data: {'quantity': quantity});
  }

  Future<void> discardStock(int id) async {
    await _dio.post('/stock-entries/$id/discard');
  }

  Future<void> moveStock(int id, Map<String, dynamic> body) async {
    await _dio.post('/stock-entries/$id/move', data: body);
  }

  Future<void> adjustStock(int id, Map<String, dynamic> body) async {
    await _dio.post('/stock-entries/$id/adjust', data: body);
  }

  // ---------------------------------------------------------------------------
  // Shopping List
  // ---------------------------------------------------------------------------

  Future<List<ShoppingItemModel>> fetchShoppingItems({
    String? status,
    int? storeId,
    String? search,
    int size = 50,
  }) async {
    final res = await _dio.get('/shopping-list', queryParameters: {
      if (status != null) 'status': status,
      if (storeId != null) 'storeId': storeId,
      if (search != null && search.isNotEmpty) 'search': search,
      'size': size,
    });
    return _pageContent(res.data)
        .map((e) => ShoppingItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ShoppingItemModel> createShoppingItem(
      Map<String, dynamic> body) async {
    final res = await _dio.post('/shopping-list', data: body);
    return ShoppingItemModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ShoppingItemModel> updateShoppingItem(
      int id, Map<String, dynamic> body) async {
    final res = await _dio.put('/shopping-list/$id', data: body);
    return ShoppingItemModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> completeShoppingItem(int id) async {
    await _dio.post('/shopping-list/$id/complete');
  }

  // ---------------------------------------------------------------------------
  // Notifications
  // ---------------------------------------------------------------------------

  Future<List<NotificationModel>> fetchNotifications() async {
    final res = await _dio.get('/notifications');
    final data = res.data;
    final list = data is List<dynamic> ? data : <dynamic>[];
    return list
        .map((e) => NotificationModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> markNotificationRead(int id) async {
    await _dio.post('/notifications/$id/read');
  }

  // ---------------------------------------------------------------------------
  // Settings
  // ---------------------------------------------------------------------------

  Future<SettingsModel> fetchSettings({String userKey = 'default'}) async {
    final res = await _dio
        .get('/settings', queryParameters: {'userKey': userKey});
    return SettingsModel.fromJson(res.data as Map<String, dynamic>);
  }

  Future<SettingsModel> updateSettings(SettingsModel settings) async {
    final res = await _dio.put('/settings', data: settings.toJson());
    return SettingsModel.fromJson(res.data as Map<String, dynamic>);
  }

  // ---------------------------------------------------------------------------
  // Sync queue (legacy — kept for compatibility)
  // ---------------------------------------------------------------------------

  Future<Response<dynamic>> syncQueue(List<Map<String, dynamic>> payload) {
    return _dio.post('/sync/enqueue', data: payload);
  }
}
