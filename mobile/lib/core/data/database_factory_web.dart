import 'package:sqflite_common_ffi_web/sqflite_ffi_web.dart';
import 'package:sqflite/sqflite.dart';

/// Web platform — redirect sqflite to IndexedDB-backed FFI implementation.
void initializeDatabaseFactory() {
  databaseFactory = databaseFactoryFfiWeb;
}
