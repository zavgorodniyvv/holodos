import 'package:dio/dio.dart';

import '../models/shopping_item_model.dart';
import '../models/stock_entry_model.dart';

class HolodosApiClient {
  HolodosApiClient(this._dio);

  final Dio _dio;

  Future<List<ShoppingItemModel>> fetchShoppingItems() async {
    final response = await _dio.get('/shopping-list',
        queryParameters: {'status': 'ACTIVE', 'size': 100});
    final data = response.data;
    final List<dynamic> content = data is Map<String, dynamic>
        ? (data['content'] as List<dynamic>? ?? [])
        : (data as List<dynamic>);
    return content
        .map((item) => ShoppingItemModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<StockEntryModel>> fetchStockEntries() async {
    final response =
        await _dio.get('/stock-entries', queryParameters: {'size': 100});
    final data = response.data as Map<String, dynamic>;
    final List<dynamic> content = data['content'] as List<dynamic>? ?? [];
    return content
        .map((item) => StockEntryModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<Response<dynamic>> syncQueue(List<Map<String, dynamic>> payload) {
    return _dio.post('/sync/enqueue', data: payload);
  }
}
