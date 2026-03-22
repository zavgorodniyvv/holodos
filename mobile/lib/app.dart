import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/app_router.dart';
import 'core/theme.dart';

class HolodosApp extends ConsumerWidget {
  const HolodosApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    return MaterialApp(
      title: 'Holodos',
      theme: buildTheme(),
      home: router,
    );
  }
}
