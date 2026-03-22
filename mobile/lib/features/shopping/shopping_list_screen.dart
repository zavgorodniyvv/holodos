import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/shopping_item_model.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/error_view.dart';
import '../../shared/widgets/shopping_item_tile.dart';
import '../catalog/catalog_providers.dart';
import 'shopping_item_detail_screen.dart';
import 'shopping_item_form_screen.dart';

// ---------------------------------------------------------------------------
// Filter state
// ---------------------------------------------------------------------------

class _ShoppingFilter {
  const _ShoppingFilter({
    this.search = '',
    this.status,
    this.storeId,
  });

  final String search;
  final String? status; // null = ACTIVE only (default view)
  final int? storeId;

  _ShoppingFilter copyWith({
    String? search,
    String? status,
    int? storeId,
    bool clearStatus = false,
    bool clearStore = false,
  }) {
    return _ShoppingFilter(
      search: search ?? this.search,
      status: clearStatus ? null : (status ?? this.status),
      storeId: clearStore ? null : (storeId ?? this.storeId),
    );
  }
}

final shoppingFilterProvider =
    StateProvider<_ShoppingFilter>((ref) => const _ShoppingFilter());

// ---------------------------------------------------------------------------
// Data provider
// ---------------------------------------------------------------------------

final shoppingItemsProvider =
    FutureProvider<List<ShoppingItemModel>>((ref) async {
  final repo = ref.watch(shoppingRepositoryProvider);
  final filter = ref.watch(shoppingFilterProvider);

  // Default: active items, cache-first.
  if (filter.search.isEmpty &&
      filter.status == null &&
      filter.storeId == null) {
    final cached = await repo.cachedItems(status: 'ACTIVE');
    if (cached.isNotEmpty) {
      unawaited(repo.refreshActiveItems());
      return cached;
    }
    return repo.refreshActiveItems();
  }

  // Filtered fetch — go directly to API.
  final remote = await repo.apiClient.fetchShoppingItems(
    status: filter.status ?? 'ACTIVE',
    storeId: filter.storeId,
    search: filter.search.isNotEmpty ? filter.search : null,
  );
  return remote;
});

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

class ShoppingListScreen extends ConsumerStatefulWidget {
  const ShoppingListScreen({super.key});

  @override
  ConsumerState<ShoppingListScreen> createState() =>
      _ShoppingListScreenState();
}

class _ShoppingListScreenState
    extends ConsumerState<ShoppingListScreen> {
  bool _searchVisible = false;
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final items = ref.watch(shoppingItemsProvider);
    final filter = ref.watch(shoppingFilterProvider);
    final storesAsync = ref.watch(storesListProvider);

    return Scaffold(
      appBar: AppBar(
        title: _searchVisible
            ? TextField(
                controller: _searchController,
                autofocus: true,
                decoration: const InputDecoration(
                  hintText: 'Search shopping list...',
                  border: InputBorder.none,
                ),
                onChanged: (v) {
                  ref
                      .read(shoppingFilterProvider.notifier)
                      .update((f) => f.copyWith(search: v));
                },
              )
            : const Text('Shopping list'),
        actions: [
          IconButton(
            icon: Icon(
                _searchVisible ? Icons.close : Icons.search_outlined),
            onPressed: () {
              setState(() => _searchVisible = !_searchVisible);
              if (!_searchVisible) {
                _searchController.clear();
                ref
                    .read(shoppingFilterProvider.notifier)
                    .update((f) => f.copyWith(search: ''));
              }
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter chips row
          _FilterChipsRow(filter: filter, storesAsync: storesAsync),

          // List
          Expanded(
            child: items.when(
              data: (data) {
                if (data.isEmpty) {
                  return const EmptyState(
                    icon: Icons.shopping_cart_outlined,
                    title: 'Shopping list is empty',
                    subtitle: 'Add items to get started.',
                  );
                }
                return RefreshIndicator(
                  onRefresh: () async =>
                      ref.invalidate(shoppingItemsProvider),
                  child: ListView.builder(
                    itemCount: data.length,
                    itemBuilder: (context, index) {
                      final item = data[index];
                      return ShoppingItemTile(
                        key: ValueKey('shop-${item.id}'),
                        item: item,
                        onTap: () =>
                            _navigateToDetail(context, item),
                        onComplete: item.isCompleted
                            ? null
                            : () => _complete(context, item),
                      );
                    },
                  ),
                );
              },
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (err, _) => ErrorView(
                message: 'Failed to load: $err',
                onRetry: () => ref.invalidate(shoppingItemsProvider),
              ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _navigateToCreate(context),
        child: const Icon(Icons.add),
      ),
    );
  }

  Future<void> _complete(
      BuildContext context, ShoppingItemModel item) async {
    try {
      await ref.read(shoppingRepositoryProvider).completeItem(item.id);
      ref.invalidate(shoppingItemsProvider);
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to complete: $e')),
        );
      }
    }
  }

  Future<void> _navigateToDetail(
      BuildContext context, ShoppingItemModel item) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => ShoppingItemDetailScreen(item: item),
      ),
    );
    ref.invalidate(shoppingItemsProvider);
  }

  Future<void> _navigateToCreate(BuildContext context) async {
    await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => const ShoppingItemFormScreen(),
      ),
    );
    ref.invalidate(shoppingItemsProvider);
  }
}

// ---------------------------------------------------------------------------
// Filter chips row
// ---------------------------------------------------------------------------

class _FilterChipsRow extends ConsumerWidget {
  const _FilterChipsRow({
    required this.filter,
    required this.storesAsync,
  });

  final _ShoppingFilter filter;
  final AsyncValue storesAsync;

  static const _statusOptions = [
    (label: 'Active', value: null),
    (label: 'Completed', value: 'COMPLETED'),
    (label: 'All', value: 'ALL'),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final stores = storesAsync.valueOrNull ?? [];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Row(
        children: [
          ..._statusOptions.map((opt) {
            final selected = filter.status == opt.value;
            return Padding(
              padding: const EdgeInsets.only(right: 6),
              child: FilterChip(
                label: Text(opt.label),
                selected: selected,
                onSelected: (_) {
                  ref
                      .read(shoppingFilterProvider.notifier)
                      .update((f) => f.copyWith(
                            clearStatus: opt.value == null,
                            status: opt.value,
                          ));
                },
              ),
            );
          }),

          if (stores.isNotEmpty) ...[
            const SizedBox(width: 4),
            DropdownButton<int?>(
              value: filter.storeId,
              hint: const Text('All stores'),
              underline: const SizedBox.shrink(),
              items: [
                const DropdownMenuItem<int?>(
                  value: null,
                  child: Text('All stores'),
                ),
                ...stores.map((s) => DropdownMenuItem<int?>(
                      value: s.id,
                      child: Text(s.name),
                    )),
              ],
              onChanged: (v) {
                ref
                    .read(shoppingFilterProvider.notifier)
                    .update((f) => f.copyWith(
                          clearStore: v == null,
                          storeId: v,
                        ));
              },
            ),
          ],
        ],
      ),
    );
  }
}
