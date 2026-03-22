import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/app_router.dart';
import '../../core/providers.dart';
import '../../data/models/dictionary_model.dart';
import '../../data/models/shopping_item_model.dart';
import '../../data/models/stock_entry_model.dart';
import '../../shared/widgets/error_view.dart';
import '../../shared/widgets/section_header.dart';
import '../../shared/widgets/shopping_item_tile.dart';
import '../../shared/widgets/stock_entry_tile.dart';
import 'dashboard_providers.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final stockAsync = ref.watch(dashboardStockProvider);
    final shoppingAsync = ref.watch(dashboardShoppingProvider);
    final placesAsync = ref.watch(storagePlacesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Holodos'),
        centerTitle: true,
        actions: [
          // Notification icon placeholder
          IconButton(
            icon: const Icon(Icons.notifications_outlined),
            tooltip: 'Notifications',
            onPressed: () {},
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(dashboardStockProvider);
          ref.invalidate(dashboardShoppingProvider);
          ref.invalidate(storagePlacesProvider);
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ----------------------------------------------------------------
              // Expiring Soon section
              // ----------------------------------------------------------------
              const SizedBox(height: 16),
              const SectionHeader(title: 'Expiring Soon'),
              const SizedBox(height: 8),
              stockAsync.when(
                loading: () => const _HorizontalSkeletonRow(),
                error: (err, _) => Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: ErrorView(
                    message: 'Could not load stock: $err',
                    onRetry: () => ref.invalidate(dashboardStockProvider),
                  ),
                ),
                data: (entries) {
                  final expiring = _expiringSoon(entries);
                  if (expiring.isEmpty) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 8),
                      child: Row(
                        children: [
                          Icon(Icons.check_circle_outline,
                              color: Colors.green.shade600, size: 18),
                          const SizedBox(width: 6),
                          Text(
                            'All fresh!',
                            style: Theme.of(context)
                                .textTheme
                                .bodyMedium
                                ?.copyWith(color: Colors.green.shade700),
                          ),
                        ],
                      ),
                    );
                  }
                  return SizedBox(
                    height: 148,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      itemCount: expiring.length,
                      separatorBuilder: (_, __) => const SizedBox(width: 8),
                      itemBuilder: (context, index) {
                        final entry = expiring[index];
                        return SizedBox(
                          width: 200,
                          child: StockEntryTile(
                            entry: entry,
                            onTap: () => ref
                                .read(navigationIndexProvider.notifier)
                                .state = 1,
                          ),
                        );
                      },
                    ),
                  );
                },
              ),

              // ----------------------------------------------------------------
              // Stock Summary section
              // ----------------------------------------------------------------
              const SizedBox(height: 20),
              SectionHeader(
                title: 'Inventory',
                onSeeAll: () =>
                    ref.read(navigationIndexProvider.notifier).state = 1,
              ),
              const SizedBox(height: 8),
              _StockSummaryGrid(
                stockAsync: stockAsync,
                placesAsync: placesAsync,
              ),

              // ----------------------------------------------------------------
              // Quick Actions section
              // ----------------------------------------------------------------
              const SizedBox(height: 20),
              const SectionHeader(title: 'Quick Actions'),
              const SizedBox(height: 8),
              _QuickActionsRow(),

              // ----------------------------------------------------------------
              // Shopping List section
              // ----------------------------------------------------------------
              const SizedBox(height: 20),
              SectionHeader(
                title: 'Shopping List',
                onSeeAll: () =>
                    ref.read(navigationIndexProvider.notifier).state = 2,
              ),
              const SizedBox(height: 4),
              shoppingAsync.when(
                loading: () => const Padding(
                  padding: EdgeInsets.all(24),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (err, _) => Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: ErrorView(
                    message: 'Could not load shopping list: $err',
                    onRetry: () =>
                        ref.invalidate(dashboardShoppingProvider),
                  ),
                ),
                data: (items) {
                  final active = items
                      .where((i) => !i.isCompleted)
                      .take(5)
                      .toList();
                  if (active.isEmpty) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 12),
                      child: Text(
                        'Shopping list is empty.',
                        style: Theme.of(context)
                            .textTheme
                            .bodyMedium
                            ?.copyWith(
                              color: Theme.of(context)
                                  .colorScheme
                                  .onSurface
                                  .withValues(alpha: 0.5),
                            ),
                      ),
                    );
                  }
                  return _ShoppingPreviewList(items: active);
                },
              ),

              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  List<StockEntryModel> _expiringSoon(List<StockEntryModel> entries) {
    final now = DateTime.now();
    final cutoff = now.add(const Duration(days: 7));
    return entries.where((e) {
      if (e.status != 'AVAILABLE') return false;
      final expires = e.expiresAt == null ? null : DateTime.tryParse(e.expiresAt!);
      if (expires == null) return false;
      return expires.isBefore(cutoff);
    }).toList()
      ..sort((a, b) {
        final da = DateTime.tryParse(a.expiresAt!) ?? DateTime.now();
        final db = DateTime.tryParse(b.expiresAt!) ?? DateTime.now();
        return da.compareTo(db);
      });
  }
}

// ---------------------------------------------------------------------------
// Stock Summary Grid
// ---------------------------------------------------------------------------

class _StockSummaryGrid extends ConsumerWidget {
  const _StockSummaryGrid({
    required this.stockAsync,
    required this.placesAsync,
  });

  final AsyncValue<List<StockEntryModel>> stockAsync;
  final AsyncValue<List<DictionaryModel>> placesAsync;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return stockAsync.when(
      loading: () => const Padding(
        padding: EdgeInsets.all(24),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (err, _) => const SizedBox.shrink(),
      data: (entries) {
        final places = placesAsync.valueOrNull ?? [];

        // Count available entries per storage place.
        final Map<int, int> countByPlace = {};
        for (final entry in entries) {
          if (entry.status == 'AVAILABLE') {
            countByPlace[entry.storagePlaceId] =
                (countByPlace[entry.storagePlaceId] ?? 0) + 1;
          }
        }

        // Build grid cells for places that have items + an "All" overview.
        final summaryItems = <_SummaryItem>[];

        // "All" card
        summaryItems.add(_SummaryItem(
          label: 'All items',
          count: entries.where((e) => e.status == 'AVAILABLE').length,
          icon: Icons.inventory_2_outlined,
          color: Theme.of(context).colorScheme.primary,
        ));

        for (final place in places) {
          final count = countByPlace[place.id] ?? 0;
          summaryItems.add(_SummaryItem(
            label: place.name,
            count: count,
            icon: _iconForPlace(place.icon),
            color: _colorForHex(place.color, context),
          ));
        }

        if (summaryItems.length <= 1 && places.isEmpty) {
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Text(
              'No inventory data yet.',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context)
                        .colorScheme
                        .onSurface
                        .withValues(alpha: 0.5),
                  ),
            ),
          );
        }

        return GridView.builder(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            childAspectRatio: 2.2,
            crossAxisSpacing: 8,
            mainAxisSpacing: 8,
          ),
          itemCount: summaryItems.length,
          itemBuilder: (context, index) =>
              _SummaryCard(item: summaryItems[index]),
        );
      },
    );
  }

  IconData _iconForPlace(String? iconName) {
    switch (iconName) {
      case 'kitchen':
        return Icons.kitchen;
      case 'freezer':
        return Icons.ac_unit;
      case 'pantry':
        return Icons.shelves;
      case 'fridge':
        return Icons.kitchen_outlined;
      default:
        return Icons.place_outlined;
    }
  }

  Color _colorForHex(String? hex, BuildContext context) {
    if (hex == null || hex.isEmpty) {
      return Theme.of(context).colorScheme.primaryContainer;
    }
    try {
      final clean = hex.replaceAll('#', '');
      return Color(int.parse('FF$clean', radix: 16));
    } catch (_) {
      return Theme.of(context).colorScheme.primaryContainer;
    }
  }
}

class _SummaryItem {
  const _SummaryItem({
    required this.label,
    required this.count,
    required this.icon,
    required this.color,
  });

  final String label;
  final int count;
  final IconData icon;
  final Color color;
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.item});

  final _SummaryItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(
                color: item.color.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(item.icon, size: 20, color: item.color),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${item.count}',
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    item.label,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurface
                          .withValues(alpha: 0.6),
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Quick Actions
// ---------------------------------------------------------------------------

class _QuickActionsRow extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          Expanded(
            child: _ActionCard(
              icon: Icons.add_box_outlined,
              label: 'Add Stock',
              color: Theme.of(context).colorScheme.primary,
              onTap: () {
                // Navigate to inventory tab — the FAB there opens the form.
                ref.read(navigationIndexProvider.notifier).state = 1;
              },
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _ActionCard(
              icon: Icons.playlist_add,
              label: 'Add to List',
              color: Theme.of(context).colorScheme.secondary,
              onTap: () {
                ref.read(navigationIndexProvider.notifier).state = 2;
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionCard extends StatelessWidget {
  const _ActionCard({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: color, size: 22),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  label,
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: color,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Shopping Preview List
// ---------------------------------------------------------------------------

class _ShoppingPreviewList extends ConsumerWidget {
  const _ShoppingPreviewList({required this.items});

  final List<ShoppingItemModel> items;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      children: items
          .map(
            (item) => ShoppingItemTile(
              key: ValueKey('dash-shop-${item.id}'),
              item: item,
              onTap: () =>
                  ref.read(navigationIndexProvider.notifier).state = 2,
              onComplete: () async {
                try {
                  final repo = ref.read(
                      // Using the shopping repository directly from providers
                      // avoids a separate import cycle.
                      shoppingRepositoryProvider);
                  await repo.completeItem(item.id);
                  ref.invalidate(dashboardShoppingProvider);
                } catch (e) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Failed to complete item: $e')),
                    );
                  }
                }
              },
            ),
          )
          .toList(),
    );
  }
}

// ---------------------------------------------------------------------------
// Skeleton placeholder for horizontal list while loading
// ---------------------------------------------------------------------------

class _HorizontalSkeletonRow extends StatelessWidget {
  const _HorizontalSkeletonRow();

  @override
  Widget build(BuildContext context) {
    final surface =
        Theme.of(context).colorScheme.surfaceContainerHighest;
    return SizedBox(
      height: 148,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: 3,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (_, __) => Container(
          width: 200,
          decoration: BoxDecoration(
            color: surface,
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}
