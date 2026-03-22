import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/stock_entry_model.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/error_view.dart';
import '../../shared/widgets/status_chip.dart';
import '../catalog/catalog_providers.dart';
import 'stock_entry_detail_screen.dart';
import 'stock_entry_form_screen.dart';

// ---------------------------------------------------------------------------
// Filter state
// ---------------------------------------------------------------------------

class _InventoryFilter {
  const _InventoryFilter({
    this.search = '',
    this.status,
    this.storagePlaceId,
  });

  final String search;
  final String? status; // null = All
  final int? storagePlaceId;

  _InventoryFilter copyWith({
    String? search,
    String? status,
    int? storagePlaceId,
    bool clearStatus = false,
    bool clearPlace = false,
  }) {
    return _InventoryFilter(
      search: search ?? this.search,
      status: clearStatus ? null : (status ?? this.status),
      storagePlaceId:
          clearPlace ? null : (storagePlaceId ?? this.storagePlaceId),
    );
  }
}

final inventoryFilterProvider =
    StateProvider<_InventoryFilter>((ref) => const _InventoryFilter());

// ---------------------------------------------------------------------------
// Data provider
// ---------------------------------------------------------------------------

final stockEntriesProvider =
    FutureProvider<List<StockEntryModel>>((ref) async {
  final repo = ref.watch(inventoryRepositoryProvider);
  final filter = ref.watch(inventoryFilterProvider);

  // Cache-first only for the unfiltered case.
  if (filter.search.isEmpty &&
      filter.status == null &&
      filter.storagePlaceId == null) {
    final cached = await repo.cachedEntries();
    if (cached.isNotEmpty) {
      unawaited(repo.refreshEntries());
      return cached;
    }
    return repo.refreshEntries();
  }

  return repo.refreshEntries(
    status: filter.status,
    storagePlaceId: filter.storagePlaceId,
    search: filter.search.isNotEmpty ? filter.search : null,
  );
});

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

class InventoryScreen extends ConsumerStatefulWidget {
  const InventoryScreen({super.key});

  @override
  ConsumerState<InventoryScreen> createState() => _InventoryScreenState();
}

class _InventoryScreenState extends ConsumerState<InventoryScreen> {
  bool _searchVisible = false;
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final entries = ref.watch(stockEntriesProvider);
    final filter = ref.watch(inventoryFilterProvider);
    final placesAsync = ref.watch(storagePlacesListProvider);

    return Scaffold(
      appBar: AppBar(
        title: _searchVisible
            ? TextField(
                controller: _searchController,
                autofocus: true,
                decoration: const InputDecoration(
                  hintText: 'Search inventory...',
                  border: InputBorder.none,
                ),
                onChanged: (v) {
                  ref
                      .read(inventoryFilterProvider.notifier)
                      .update((f) => f.copyWith(search: v));
                },
              )
            : const Text('Inventory'),
        actions: [
          IconButton(
            icon: Icon(
                _searchVisible ? Icons.close : Icons.search_outlined),
            onPressed: () {
              setState(() => _searchVisible = !_searchVisible);
              if (!_searchVisible) {
                _searchController.clear();
                ref
                    .read(inventoryFilterProvider.notifier)
                    .update((f) => f.copyWith(search: ''));
              }
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter chips row
          _FilterChipsRow(filter: filter, placesAsync: placesAsync),

          // List
          Expanded(
            child: entries.when(
              data: (data) {
                if (data.isEmpty) {
                  return const EmptyState(
                    icon: Icons.inventory_2_outlined,
                    title: 'No items found',
                    subtitle:
                        'Try adjusting your filters or add new stock.',
                  );
                }
                return RefreshIndicator(
                  onRefresh: () async =>
                      ref.invalidate(stockEntriesProvider),
                  child: ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: data.length,
                    separatorBuilder: (_, __) =>
                        const Divider(height: 1),
                    itemBuilder: (ctx, index) {
                      final entry = data[index];
                      return ListTile(
                        title:
                            Text(entry.productName ?? 'Unknown product'),
                        subtitle: Text(
                          _buildSubtitle(entry),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: StatusChip(status: entry.status),
                        onTap: () => _navigateToDetail(ctx, entry),
                      );
                    },
                  ),
                );
              },
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (err, _) => ErrorView(
                message: 'Failed to load inventory: $err',
                onRetry: () => ref.invalidate(stockEntriesProvider),
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

  String _buildSubtitle(StockEntryModel entry) {
    final parts = <String>[];
    final qty =
        '${_formatQty(entry.quantity)}${entry.unitName != null ? ' ${entry.unitName}' : ''}';
    parts.add(qty);
    if (entry.storagePlaceName != null) parts.add(entry.storagePlaceName!);
    if (entry.expiresAt != null) {
      final days = entry.daysUntilExpiry;
      if (days != null) {
        if (days < 0) {
          parts.add('Expired');
        } else if (days == 0) {
          parts.add('Expires today');
        } else {
          parts.add('Expires in ${days}d');
        }
      }
    }
    return parts.join('  •  ');
  }

  Future<void> _navigateToDetail(
      BuildContext context, StockEntryModel entry) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => StockEntryDetailScreen(entry: entry),
      ),
    );
    ref.invalidate(stockEntriesProvider);
  }

  Future<void> _navigateToCreate(BuildContext context) async {
    await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => const StockEntryFormScreen(),
      ),
    );
    ref.invalidate(stockEntriesProvider);
  }

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(1);
}

// ---------------------------------------------------------------------------
// Filter chips row
// ---------------------------------------------------------------------------

class _FilterChipsRow extends ConsumerWidget {
  const _FilterChipsRow({
    required this.filter,
    required this.placesAsync,
  });

  final _InventoryFilter filter;
  final AsyncValue placesAsync;

  static const _statusOptions = [
    (label: 'All', value: null),
    (label: 'Available', value: 'AVAILABLE'),
    (label: 'Expired', value: 'EXPIRED'),
    (label: 'Discarded', value: 'DISCARDED'),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final places = placesAsync.valueOrNull ?? [];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Row(
        children: [
          // Status chips
          ..._statusOptions.map((opt) {
            final selected = filter.status == opt.value;
            return Padding(
              padding: const EdgeInsets.only(right: 6),
              child: FilterChip(
                label: Text(opt.label),
                selected: selected,
                onSelected: (_) {
                  ref
                      .read(inventoryFilterProvider.notifier)
                      .update((f) => f.copyWith(
                            clearStatus: opt.value == null,
                            status: opt.value,
                          ));
                },
              ),
            );
          }),

          // Storage place dropdown chip
          if (places.isNotEmpty) ...[
            const SizedBox(width: 4),
            DropdownButton<int?>(
              value: filter.storagePlaceId,
              hint: const Text('All places'),
              underline: const SizedBox.shrink(),
              items: [
                const DropdownMenuItem<int?>(
                  value: null,
                  child: Text('All places'),
                ),
                ...places.map((p) => DropdownMenuItem<int?>(
                      value: p.id,
                      child: Text(p.name),
                    )),
              ],
              onChanged: (v) {
                ref
                    .read(inventoryFilterProvider.notifier)
                    .update((f) => f.copyWith(
                          clearPlace: v == null,
                          storagePlaceId: v,
                        ));
              },
            ),
          ],
        ],
      ),
    );
  }
}
