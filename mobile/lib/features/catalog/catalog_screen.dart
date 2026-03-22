import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import 'catalog_providers.dart';
import 'dictionary_list_screen.dart';
import 'products_list_screen.dart';
import 'units_list_screen.dart';

class CatalogScreen extends ConsumerWidget {
  const CatalogScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Pre-fetch counts for subtitle display
    final productsAsync = ref.watch(productsListProvider);
    final placesAsync = ref.watch(storagePlacesListProvider);
    final categoriesAsync = ref.watch(categoriesListProvider);
    final storesAsync = ref.watch(storesListProvider);
    final unitsAsync = ref.watch(unitsListProvider);

    final items = [
      _CatalogEntry(
        icon: Icons.inventory_2_outlined,
        title: 'Products',
        subtitle: _countLabel(productsAsync.valueOrNull?.length, 'product'),
        color: Theme.of(context).colorScheme.primary,
        onTap: () => Navigator.of(context).push<void>(
          MaterialPageRoute<void>(
              builder: (_) => const ProductsListScreen()),
        ),
      ),
      _CatalogEntry(
        icon: Icons.location_on_outlined,
        title: 'Storage Places',
        subtitle:
            _countLabel(placesAsync.valueOrNull?.length, 'location'),
        color: Colors.teal,
        onTap: () => Navigator.of(context).push<void>(
          MaterialPageRoute<void>(
            builder: (_) => DictionaryListScreen(
              title: 'Storage Places',
              provider: storagePlacesListProvider,
              onRefresh: () => ref
                  .read(catalogRepositoryProvider)
                  .refreshStoragePlaces()
                  .then((_) {}),
              onCreate: (body) =>
                  ref.read(catalogRepositoryProvider).createStoragePlace(body),
              onUpdate: (id, body) => ref
                  .read(catalogRepositoryProvider)
                  .updateStoragePlace(id, body),
              onDelete: (id) =>
                  ref.read(catalogRepositoryProvider).deleteStoragePlace(id),
            ),
          ),
        ),
      ),
      _CatalogEntry(
        icon: Icons.category_outlined,
        title: 'Categories',
        subtitle:
            _countLabel(categoriesAsync.valueOrNull?.length, 'category'),
        color: Colors.indigo,
        onTap: () => Navigator.of(context).push<void>(
          MaterialPageRoute<void>(
            builder: (_) => DictionaryListScreen(
              title: 'Categories',
              provider: categoriesListProvider,
              onRefresh: () => ref
                  .read(catalogRepositoryProvider)
                  .refreshCategories()
                  .then((_) {}),
              onCreate: (body) =>
                  ref.read(catalogRepositoryProvider).createCategory(body),
              onUpdate: (id, body) =>
                  ref.read(catalogRepositoryProvider).updateCategory(id, body),
              onDelete: (id) =>
                  ref.read(catalogRepositoryProvider).deleteCategory(id),
            ),
          ),
        ),
      ),
      _CatalogEntry(
        icon: Icons.store_outlined,
        title: 'Stores',
        subtitle: _countLabel(storesAsync.valueOrNull?.length, 'store'),
        color: Colors.orange,
        onTap: () => Navigator.of(context).push<void>(
          MaterialPageRoute<void>(
            builder: (_) => DictionaryListScreen(
              title: 'Stores',
              provider: storesListProvider,
              onRefresh: () => ref
                  .read(catalogRepositoryProvider)
                  .refreshStores()
                  .then((_) {}),
              onCreate: (body) =>
                  ref.read(catalogRepositoryProvider).createStore(body),
              onUpdate: (id, body) =>
                  ref.read(catalogRepositoryProvider).updateStore(id, body),
              onDelete: (id) =>
                  ref.read(catalogRepositoryProvider).deleteStore(id),
            ),
          ),
        ),
      ),
      _CatalogEntry(
        icon: Icons.straighten,
        title: 'Units',
        subtitle: _countLabel(unitsAsync.valueOrNull?.length, 'unit'),
        color: Colors.deepPurple,
        onTap: () => Navigator.of(context).push<void>(
          MaterialPageRoute<void>(
              builder: (_) => const UnitsListScreen()),
        ),
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Catalog'),
      ),
      body: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: items.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (ctx, index) {
          final entry = items[index];
          return ListTile(
            leading: CircleAvatar(
              backgroundColor: entry.color.withValues(alpha: 0.15),
              child: Icon(entry.icon, color: entry.color, size: 22),
            ),
            title: Text(entry.title,
                style:
                    const TextStyle(fontWeight: FontWeight.w600)),
            subtitle: Text(entry.subtitle),
            trailing: const Icon(Icons.chevron_right),
            onTap: entry.onTap,
          );
        },
      ),
    );
  }

  static String _countLabel(int? count, String noun) {
    if (count == null) return 'Loading...';
    final plural = count == 1 ? noun : '${noun}s';
    return '$count $plural';
  }
}

class _CatalogEntry {
  const _CatalogEntry({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Color color;
  final VoidCallback onTap;
}
