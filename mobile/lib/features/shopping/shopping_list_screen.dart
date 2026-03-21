import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/shopping_item_model.dart';

final shoppingItemsProvider =
    FutureProvider<List<ShoppingItemModel>>((ref) async {
  final repo = ref.watch(shoppingRepositoryProvider);
  final cached = await repo.cachedItems();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshActiveItems());
    return cached;
  }
  return repo.refreshActiveItems();
});

class ShoppingListScreen extends ConsumerWidget {
  const ShoppingListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final items = ref.watch(shoppingItemsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Shopping list')),
      body: items.when(
        data: (data) => RefreshIndicator(
          onRefresh: () async => ref.invalidate(shoppingItemsProvider),
          child: ListView.builder(
            itemCount: data.length,
            itemBuilder: (context, index) {
              final item = data[index];
              final statusIcon = item.status == 'COMPLETED'
                  ? const Icon(Icons.check_box, color: Colors.green)
                  : const Icon(Icons.check_box_outline_blank);
              return ListTile(
                leading: statusIcon,
                title: Text(item.title),
                subtitle: Text('Qty: ${item.quantity} • ${item.status}'),
              );
            },
          ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, _) => Center(child: Text('Failed to load: $err')),
      ),
    );
  }
}
