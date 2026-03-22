import 'dart:convert';

import '../../data/api/holodos_api_client.dart';
import 'local_database.dart';

class SyncQueue {
  SyncQueue({required this.api, required this.database});

  final HolodosApiClient api;
  final LocalDatabase database;

  Future<void> enqueue(Map<String, dynamic> payload) async {
    await database.insertQueueItem(jsonEncode(payload));
  }

  Future<void> flush() async {
    final items = await database.pendingQueueItems();
    if (items.isEmpty) return;
    final payloads = items
        .map((row) =>
            jsonDecode(row['payload'] as String) as Map<String, dynamic>)
        .toList();
    final response = await api.syncQueue(payloads);
    if (response.statusCode == 200) {
      for (final row in items) {
        await database.markProcessed(row['id'] as int);
      }
    }
  }
}
