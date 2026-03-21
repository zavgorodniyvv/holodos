import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

class LocalDatabase {
  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;
    final dir = await getApplicationDocumentsDirectory();
    final path = join(dir.path, 'holodos.db');
    _db = await openDatabase(path, version: 1, onCreate: (db, version) async {
      await db.execute('''
        CREATE TABLE IF NOT EXISTS sync_queue (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          payload TEXT NOT NULL,
          created_at TEXT NOT NULL,
          state TEXT NOT NULL
        );
      ''');
      await db.execute('''
        CREATE TABLE IF NOT EXISTS shopping_items (
          id INTEGER PRIMARY KEY,
          title TEXT NOT NULL,
          quantity REAL NOT NULL,
          status TEXT NOT NULL,
          updated_at TEXT
        );
      ''');
      await db.execute('''
        CREATE TABLE IF NOT EXISTS stock_entries (
          id INTEGER PRIMARY KEY,
          product_name TEXT NOT NULL,
          quantity REAL NOT NULL,
          storage_place TEXT,
          status TEXT NOT NULL,
          updated_at TEXT
        );
      ''');
    });
    return _db!;
  }

  Future<void> insertQueueItem(String payload) async {
    final db = await database;
    await db.insert('sync_queue', {
      'payload': payload,
      'created_at': DateTime.now().toIso8601String(),
      'state': 'pending',
    });
  }

  Future<List<Map<String, dynamic>>> pendingQueueItems() async {
    final db = await database;
    return db.query('sync_queue',
        where: 'state = ?', whereArgs: ['pending'], orderBy: 'created_at ASC');
  }

  Future<void> markProcessed(int id) async {
    final db = await database;
    await db.update('sync_queue', {'state': 'processed'},
        where: 'id = ?', whereArgs: [id]);
  }

  Future<void> replaceShoppingItems(List<Map<String, dynamic>> items) async {
    final db = await database;
    final batch = db.batch();
    batch.delete('shopping_items');
    for (final item in items) {
      batch.insert('shopping_items', item,
          conflictAlgorithm: ConflictAlgorithm.replace);
    }
    await batch.commit(noResult: true);
  }

  Future<List<Map<String, dynamic>>> loadShoppingItems() async {
    final db = await database;
    return db.query('shopping_items', orderBy: 'updated_at DESC');
  }

  Future<void> replaceStockEntries(List<Map<String, dynamic>> items) async {
    final db = await database;
    final batch = db.batch();
    batch.delete('stock_entries');
    for (final item in items) {
      batch.insert('stock_entries', item,
          conflictAlgorithm: ConflictAlgorithm.replace);
    }
    await batch.commit(noResult: true);
  }

  Future<List<Map<String, dynamic>>> loadStockEntries() async {
    final db = await database;
    return db.query('stock_entries', orderBy: 'updated_at DESC');
  }
}
