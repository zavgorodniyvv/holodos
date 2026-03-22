import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/shopping_item_model.dart';
import '../../shared/widgets/confirm_dialog.dart';
import '../../shared/widgets/status_chip.dart';
import '../shopping/shopping_list_screen.dart';
import '../shopping/shopping_item_form_screen.dart';

class ShoppingItemDetailScreen extends ConsumerWidget {
  const ShoppingItemDetailScreen({super.key, required this.item});

  final ShoppingItemModel item;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(item.title),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            tooltip: 'Edit',
            onPressed: () => _navigateToEdit(context, ref),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Card(
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
                        item.title,
                        style: theme.textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                          decoration: item.isCompleted
                              ? TextDecoration.lineThrough
                              : null,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    StatusChip(status: item.status),
                  ],
                ),
                const SizedBox(height: 16),
                _InfoRow(
                  icon: Icons.numbers,
                  label: 'Quantity',
                  value: _formatQty(item.quantity),
                ),
                if (item.source != null)
                  _InfoRow(
                    icon: Icons.source_outlined,
                    label: 'Source',
                    value: _formatSource(item.source!),
                  ),
                if (item.comment != null && item.comment!.isNotEmpty)
                  _InfoRow(
                    icon: Icons.comment_outlined,
                    label: 'Comment',
                    value: item.comment!,
                  ),
                if (item.createdAt != null)
                  _InfoRow(
                    icon: Icons.calendar_today_outlined,
                    label: 'Created',
                    value: _formatDate(item.createdAt),
                  ),
                if (item.completedAt != null)
                  _InfoRow(
                    icon: Icons.check_circle_outline,
                    label: 'Completed',
                    value: _formatDate(item.completedAt),
                  ),
              ],
            ),
          ),
        ),
      ),
      bottomNavigationBar: item.isCompleted
          ? null
          : _ActionBar(item: item),
    );
  }

  Future<void> _navigateToEdit(BuildContext context, WidgetRef ref) async {
    final result = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ShoppingItemFormScreen(existing: item),
      ),
    );
    if (result == true) {
      ref.invalidate(shoppingItemsProvider);
      if (context.mounted) Navigator.of(context).pop();
    }
  }

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(1);

  String _formatDate(String? iso) {
    if (iso == null) return '';
    final dt = DateTime.tryParse(iso);
    if (dt == null) return iso;
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}';
  }

  String _formatSource(String source) {
    switch (source.toUpperCase()) {
      case 'MANUAL':
        return 'Manual';
      case 'AUTO_REPLENISHMENT':
        return 'Auto-replenishment';
      case 'GOOGLE_KEEP':
        return 'Google Keep';
      default:
        return source;
    }
  }
}

// ---------------------------------------------------------------------------
// Info row helper
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Bottom action bar (active items only)
// ---------------------------------------------------------------------------

class _ActionBar extends ConsumerWidget {
  const _ActionBar({required this.item});

  final ShoppingItemModel item;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
        child: SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            icon: const Icon(Icons.check_circle_outline),
            label: const Text('Mark as Completed'),
            onPressed: () => _complete(context, ref),
          ),
        ),
      ),
    );
  }

  Future<void> _complete(BuildContext context, WidgetRef ref) async {
    final confirmed = await ConfirmDialog.show(
      context,
      title: 'Complete item',
      message: 'Mark "${item.title}" as completed?',
      confirmLabel: 'Complete',
    );
    if (!confirmed) return;

    try {
      await ref.read(shoppingRepositoryProvider).completeItem(item.id);
      ref.invalidate(shoppingItemsProvider);
      if (context.mounted) Navigator.of(context).pop();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to complete: $e')),
        );
      }
    }
  }
}
