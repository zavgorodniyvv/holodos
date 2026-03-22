import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/product_model.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/error_view.dart';
import 'catalog_providers.dart';
import 'product_detail_screen.dart';
import 'product_form_screen.dart';

class ProductsListScreen extends ConsumerStatefulWidget {
  const ProductsListScreen({super.key});

  @override
  ConsumerState<ProductsListScreen> createState() =>
      _ProductsListScreenState();
}

class _ProductsListScreenState
    extends ConsumerState<ProductsListScreen> {
  bool _searchVisible = false;
  String _searchQuery = '';
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final productsAsync = ref.watch(productsListProvider);

    return Scaffold(
      appBar: AppBar(
        title: _searchVisible
            ? TextField(
                controller: _searchController,
                autofocus: true,
                decoration: const InputDecoration(
                  hintText: 'Search products...',
                  border: InputBorder.none,
                ),
                onChanged: (v) => setState(() => _searchQuery = v),
              )
            : const Text('Products'),
        actions: [
          IconButton(
            icon: Icon(
                _searchVisible ? Icons.close : Icons.search_outlined),
            onPressed: () {
              setState(() {
                _searchVisible = !_searchVisible;
                if (!_searchVisible) {
                  _searchQuery = '';
                  _searchController.clear();
                }
              });
            },
          ),
        ],
      ),
      body: productsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => ErrorView(
          message: 'Failed to load products: $e',
          onRetry: () => ref.invalidate(productsListProvider),
        ),
        data: (products) {
          final filtered = _applyFilter(products, _searchQuery);
          if (filtered.isEmpty) {
            return EmptyState(
              icon: Icons.inventory_2_outlined,
              title: _searchQuery.isNotEmpty
                  ? 'No products match "$_searchQuery"'
                  : 'No products yet',
              subtitle: _searchQuery.isEmpty
                  ? 'Tap + to add your first product.'
                  : 'Try a different search term.',
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(productsListProvider),
            child: ListView.separated(
              itemCount: filtered.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (ctx, index) {
                final product = filtered[index];
                return _ProductListTile(
                  product: product,
                  onTap: () => _navigateToDetail(ctx, product),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _navigateToCreate(context),
        child: const Icon(Icons.add),
      ),
    );
  }

  List<ProductModel> _applyFilter(
      List<ProductModel> products, String query) {
    if (query.isEmpty) return products;
    final q = query.toLowerCase();
    return products
        .where((p) =>
            p.name.toLowerCase().contains(q) ||
            (p.categoryName?.toLowerCase().contains(q) ?? false))
        .toList();
  }

  Future<void> _navigateToDetail(
      BuildContext context, ProductModel product) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => ProductDetailScreen(product: product),
      ),
    );
    ref.invalidate(productsListProvider);
  }

  Future<void> _navigateToCreate(BuildContext context) async {
    await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => const ProductFormScreen(),
      ),
    );
    ref.invalidate(productsListProvider);
  }
}

class _ProductListTile extends StatelessWidget {
  const _ProductListTile({
    required this.product,
    required this.onTap,
  });

  final ProductModel product;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: product.active
            ? theme.colorScheme.primaryContainer
            : theme.colorScheme.surfaceContainerHighest,
        child: Text(
          product.name.substring(0, 1).toUpperCase(),
          style: TextStyle(
            color: product.active
                ? theme.colorScheme.onPrimaryContainer
                : theme.colorScheme.onSurface.withValues(alpha: 0.5),
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      title: Text(
        product.name,
        style: TextStyle(
          color: product.active
              ? null
              : theme.colorScheme.onSurface.withValues(alpha: 0.5),
        ),
      ),
      subtitle: Text(
        [
          if (product.categoryName != null) product.categoryName!,
          if (product.defaultUnitName != null) product.defaultUnitName!,
        ].join(' · '),
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
        ),
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }
}
