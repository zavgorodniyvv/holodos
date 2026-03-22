import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

/// SQLite local cache, version 2.
///
/// Version history:
///   1 — initial: sync_queue, shopping_items (minimal), stock_entries (minimal)
///   2 — expanded: full columns on shopping_items & stock_entries; added
///       products, storage_places, categories, stores, units tables.
///       Migration from v1 drops old tables and recreates everything because
///       the cache is expendable (data is always fetched fresh from backend).
class LocalDatabase {
  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;
    final path = kIsWeb
        ? 'holodos.db'
        : join((await getApplicationDocumentsDirectory()).path, 'holodos.db');
    _db = await openDatabase(
      path,
      version: 2,
      onCreate: _createAll,
      onUpgrade: _onUpgrade,
    );
    return _db!;
  }

  Future<void> _createAll(Database db, int version) async {
    await _createSyncQueue(db);
    await _createShoppingItems(db);
    await _createStockEntries(db);
    await _createProducts(db);
    await _createStoragePlaces(db);
    await _createCategories(db);
    await _createStores(db);
    await _createUnits(db);
  }

  Future<void> _onUpgrade(Database db, int oldVersion, int newVersion) async {
    // Drop all old tables and recreate. Cache data is expendable.
    for (final table in [
      'shopping_items',
      'stock_entries',
      'products',
      'storage_places',
      'categories',
      'stores',
      'units',
    ]) {
      await db.execute('DROP TABLE IF EXISTS $table');
    }
    await _createShoppingItems(db);
    await _createStockEntries(db);
    await _createProducts(db);
    await _createStoragePlaces(db);
    await _createCategories(db);
    await _createStores(db);
    await _createUnits(db);
  }

  // ---------------------------------------------------------------------------
  // DDL helpers
  // ---------------------------------------------------------------------------

  Future<void> _createSyncQueue(Database db) => db.execute('''
    CREATE TABLE IF NOT EXISTS sync_queue (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      payload TEXT NOT NULL,
      created_at TEXT NOT NULL,
      state TEXT NOT NULL
    )
  ''');

  Future<void> _createShoppingItems(Database db) => db.execute('''
    CREATE TABLE IF NOT EXISTS shopping_items (
      id INTEGER PRIMARY KEY,
      product_id INTEGER,
      title TEXT NOT NULL,
      quantity REAL NOT NULL DEFAULT 1,
      unit_id INTEGER,
      store_id INTEGER,
      status TEXT NOT NULL DEFAULT 'ACTIVE',
      source TEXT,
      comment TEXT,
      sort_order INTEGER NOT NULL DEFAULT 0,
      created_at TEXT,
      completed_at TEXT,
      updated_at TEXT,
      version INTEGER
    )
  ''');

  Future<void> _createStockEntries(Database db) => db.execute('''
    CREATE TABLE IF NOT EXISTS stock_entries (
      id INTEGER PRIMARY KEY,
      product_id INTEGER NOT NULL DEFAULT 0,
      product_name TEXT,
      quantity REAL NOT NULL DEFAULT 0,
      unit_id INTEGER NOT NULL DEFAULT 0,
      unit_name TEXT,
      storage_place_id INTEGER NOT NULL DEFAULT 0,
      storage_place_name TEXT,
      added_at TEXT,
      purchased_at TEXT,
      expires_at TEXT,
      comment TEXT,
      status TEXT NOT NULL DEFAULT 'AVAILABLE',
      created_at TEXT,
      updated_at TEXT,
      version INTEGER
    )
  ''');

  Future<void> _createProducts(Database db) => db.execute('''
    CREATE TABLE IF NOT EXISTS products (
      id INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      category_id INTEGER NOT NULL DEFAULT 0,
      category_name TEXT,
      default_unit_id INTEGER NOT NULL DEFAULT 0,
      default_unit_name TEXT,
      default_storage_place_id INTEGER NOT NULL DEFAULT 0,
      default_storage_place_name TEXT,
      default_store_id INTEGER,
      default_store_name TEXT,
      photo_key TEXT,
      description TEXT,
      shelf_life_days INTEGER,
      minimum_quantity_threshold REAL,
      reorder_quantity REAL,
      auto_add_shopping INTEGER NOT NULL DEFAULT 0,
      barcode TEXT,
      note TEXT,
      active INTEGER NOT NULL DEFAULT 1,
      created_at TEXT,
      updated_at TEXT,
      version INTEGER
    )
  ''');

  String get _dictionaryColumns => '''
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    icon TEXT,
    color TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT,
    updated_at TEXT,
    version INTEGER
  ''';

  Future<void> _createStoragePlaces(Database db) =>
      db.execute('CREATE TABLE IF NOT EXISTS storage_places ($_dictionaryColumns)');

  Future<void> _createCategories(Database db) =>
      db.execute('CREATE TABLE IF NOT EXISTS categories ($_dictionaryColumns)');

  Future<void> _createStores(Database db) =>
      db.execute('CREATE TABLE IF NOT EXISTS stores ($_dictionaryColumns)');

  Future<void> _createUnits(Database db) => db.execute('''
    CREATE TABLE IF NOT EXISTS units (
      id INTEGER PRIMARY KEY,
      code TEXT NOT NULL,
      name TEXT NOT NULL,
      short_name TEXT NOT NULL,
      unit_type TEXT NOT NULL DEFAULT 'COUNT',
      active INTEGER NOT NULL DEFAULT 1,
      created_at TEXT,
      updated_at TEXT,
      version INTEGER
    )
  ''');

  // ---------------------------------------------------------------------------
  // Sync queue
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // Shopping items
  // ---------------------------------------------------------------------------

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

  Future<List<Map<String, dynamic>>> loadShoppingItems(
      {String? status}) async {
    final db = await database;
    return db.query(
      'shopping_items',
      where: status != null ? 'status = ?' : null,
      whereArgs: status != null ? [status] : null,
      orderBy: 'sort_order ASC, updated_at DESC',
    );
  }

  // ---------------------------------------------------------------------------
  // Stock entries
  // ---------------------------------------------------------------------------

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

  Future<List<Map<String, dynamic>>> loadStockEntries(
      {String? status, int? storagePlaceId}) async {
    final db = await database;
    final conditions = <String>[];
    final args = <dynamic>[];
    if (status != null) {
      conditions.add('status = ?');
      args.add(status);
    }
    if (storagePlaceId != null) {
      conditions.add('storage_place_id = ?');
      args.add(storagePlaceId);
    }
    return db.query(
      'stock_entries',
      where: conditions.isNotEmpty ? conditions.join(' AND ') : null,
      whereArgs: args.isNotEmpty ? args : null,
      orderBy: 'updated_at DESC',
    );
  }

  // ---------------------------------------------------------------------------
  // Generic dictionary cache (storage_places, categories, stores)
  // ---------------------------------------------------------------------------

  Future<void> replaceDictionary(
      String table, List<Map<String, dynamic>> items) async {
    final db = await database;
    final batch = db.batch();
    batch.delete(table);
    for (final item in items) {
      batch.insert(table, item,
          conflictAlgorithm: ConflictAlgorithm.replace);
    }
    await batch.commit(noResult: true);
  }

  Future<List<Map<String, dynamic>>> loadDictionary(String table) async {
    final db = await database;
    return db.query(table, orderBy: 'sort_order ASC, name ASC');
  }

  // ---------------------------------------------------------------------------
  // Units
  // ---------------------------------------------------------------------------

  Future<void> replaceUnits(List<Map<String, dynamic>> items) async {
    final db = await database;
    final batch = db.batch();
    batch.delete('units');
    for (final item in items) {
      batch.insert('units', item,
          conflictAlgorithm: ConflictAlgorithm.replace);
    }
    await batch.commit(noResult: true);
  }

  Future<List<Map<String, dynamic>>> loadUnits() async {
    final db = await database;
    return db.query('units', orderBy: 'name ASC');
  }

  // ---------------------------------------------------------------------------
  // Products
  // ---------------------------------------------------------------------------

  Future<void> replaceProducts(List<Map<String, dynamic>> items) async {
    final db = await database;
    final batch = db.batch();
    batch.delete('products');
    for (final item in items) {
      batch.insert('products', item,
          conflictAlgorithm: ConflictAlgorithm.replace);
    }
    await batch.commit(noResult: true);
  }

  Future<List<Map<String, dynamic>>> loadProducts({String? search}) async {
    final db = await database;
    return db.query(
      'products',
      where: search != null && search.isNotEmpty ? 'name LIKE ?' : null,
      whereArgs:
          search != null && search.isNotEmpty ? ['%$search%'] : null,
      orderBy: 'name ASC',
    );
  }
}
