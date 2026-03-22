import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/product_model.dart';
import '../../shared/widgets/confirm_dialog.dart';
import 'catalog_providers.dart';
import 'product_form_screen.dart';

class ProductDetailScreen extends ConsumerWidget {
  const ProductDetailScreen({super.key, required this.product});

  final ProductModel product;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(product.name),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            tooltip: 'Edit',
            onPressed: () => _navigateToEdit(context, ref),
          ),
          IconButton(
            icon: Icon(Icons.delete_outline,
                color: theme.colorScheme.error),
            tooltip: 'Delete',
            onPressed: () => _delete(context, ref),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Main card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Text(
                            product.name,
                            style: theme.textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        _StatusBadge(active: product.active),
                      ],
                    ),
                    if (product.description != null &&
                        product.description!.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        product.description!,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurface
                              .withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                    const Divider(height: 24),
                    _InfoRow(
                      icon: Icons.category_outlined,
                      label: 'Category',
                      value: product.categoryName ?? 'Unknown',
                    ),
                    _InfoRow(
                      icon: Icons.straighten,
                      label: 'Default unit',
                      value: product.defaultUnitName ?? 'Unknown',
                    ),
                    _InfoRow(
                      icon: Icons.location_on_outlined,
                      label: 'Default storage place',
                      value: product.defaultStoragePlaceName ?? 'Unknown',
                    ),
                    if (product.defaultStoreName != null)
                      _InfoRow(
                        icon: Icons.store_outlined,
                        label: 'Default store',
                        value: product.defaultStoreName!,
                      ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 8),

            // Thresholds & lifecycle card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Lifecycle & Thresholds',
                        style: theme.textTheme.titleMedium
                            ?.copyWith(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 12),
                    if (product.shelfLifeDays != null)
                      _InfoRow(
                        icon: Icons.schedule,
                        label: 'Shelf life',
                        value: '${product.shelfLifeDays} days',
                      ),
                    if (product.minimumQuantityThreshold != null)
                      _InfoRow(
                        icon: Icons.arrow_downward,
                        label: 'Minimum quantity threshold',
                        value: _formatQty(product.minimumQuantityThreshold!),
                      ),
                    if (product.reorderQuantity != null)
                      _InfoRow(
                        icon: Icons.shopping_cart_outlined,
                        label: 'Reorder quantity',
                        value: _formatQty(product.reorderQuantity!),
                      ),
                    _InfoRow(
                      icon: Icons.autorenew,
                      label: 'Auto-add to shopping',
                      value: product.autoAddShopping ? 'Yes' : 'No',
                    ),
                  ],
                ),
              ),
            ),

            // Additional info card
            if (product.barcode != null ||
                product.note != null ||
                product.photoKey != null) ...[
              const SizedBox(height: 8),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Additional Info',
                          style: theme.textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600)),
                      const SizedBox(height: 12),
                      if (product.barcode != null)
                        _InfoRow(
                          icon: Icons.qr_code_outlined,
                          label: 'Barcode',
                          value: product.barcode!,
                        ),
                      if (product.note != null && product.note!.isNotEmpty)
                        _InfoRow(
                          icon: Icons.note_outlined,
                          label: 'Note',
                          value: product.note!,
                        ),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _navigateToEdit(BuildContext context, WidgetRef ref) async {
    final result = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ProductFormScreen(existing: product),
      ),
    );
    if (result == true) {
      ref.invalidate(productsListProvider);
      if (context.mounted) Navigator.of(context).pop();
    }
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final confirmed = await ConfirmDialog.show(
      context,
      title: 'Delete product',
      message:
          'Delete "${product.name}"? This cannot be undone.',
      confirmLabel: 'Delete',
      destructive: true,
    );
    if (!confirmed) return;

    try {
      await ref.read(catalogRepositoryProvider).deleteProduct(product.id);
      ref.invalidate(productsListProvider);
      if (context.mounted) Navigator.of(context).pop();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to delete: $e')),
        );
      }
    }
  }

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(2);
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.active});

  final bool active;

  @override
  Widget build(BuildContext context) {
    return Chip(
      label: Text(
        active ? 'Active' : 'Inactive',
        style: TextStyle(
          color: active ? Colors.green.shade800 : Colors.grey.shade700,
          fontWeight: FontWeight.w600,
          fontSize: 12,
        ),
      ),
      backgroundColor: active
          ? Colors.green.shade100
          : Colors.grey.shade200,
      side: BorderSide.none,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      visualDensity: VisualDensity.compact,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon,
              size: 18,
              color: theme.colorScheme.onSurface.withValues(alpha: 0.5)),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
                  ),
                ),
                Text(value, style: theme.textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
