import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/data/database_factory_native.dart'
    if (dart.library.html) 'core/data/database_factory_web.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  initializeDatabaseFactory();
  runApp(const ProviderScope(child: HolodosApp()));
}
