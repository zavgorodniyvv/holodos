import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/stock_entry_model.dart';
import '../../shared/widgets/confirm_dialog.dart';
import '../../shared/widgets/status_chip.dart';
import '../inventory/inventory_screen.dart';

class StockEntryDetailScreen extends ConsumerWidget {
  const StockEntryDetailScreen({super.key, required this.entry});

  final StockEntryModel entry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final days = entry.daysUntilExpiry;

    return Scaffold(
      appBar: AppBar(
        title: Text(entry.productName ?? 'Stock Entry'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Main info card
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
                            entry.productName ?? 'Unknown product',
                            style: theme.textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        StatusChip(status: entry.status),
                      ],
                    ),
                    const SizedBox(height: 16),
                    _InfoRow(
                      icon: Icons.scale_outlined,
                      label: 'Quantity',
                      value:
                          '${_formatQty(entry.quantity)}${entry.unitName != null ? ' ${entry.unitName}' : ''}',
                    ),
                    if (entry.storagePlaceName != null)
                      _InfoRow(
                        icon: Icons.location_on_outlined,
                        label: 'Storage place',
                        value: entry.storagePlaceName!,
                      ),
                    if (entry.addedAt != null)
                      _InfoRow(
                        icon: Icons.add_circle_outline,
                        label: 'Added',
                        value: _formatDate(entry.addedAt),
                      ),
                    if (entry.purchasedAt != null)
                      _InfoRow(
                        icon: Icons.shopping_bag_outlined,
                        label: 'Purchased',
                        value: _formatDate(entry.purchasedAt),
                      ),
                    if (entry.expiresAt != null)
                      _InfoRow(
                        icon: Icons.schedule,
                        label: 'Expires',
                        value: _expiryText(entry.expiresAt, days),
                        valueColor: _expiryColor(days),
                      ),
                    if (entry.comment != null && entry.comment!.isNotEmpty)
                      _InfoRow(
                        icon: Icons.comment_outlined,
                        label: 'Comment',
                        value: entry.comment!,
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: _ActionBar(entry: entry),
    );
  }

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(2);

  String _formatDate(String? iso) {
    if (iso == null) return '';
    final dt = DateTime.tryParse(iso);
    if (dt == null) return iso;
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}';
  }

  String _expiryText(String? iso, int? days) {
    final date = _formatDate(iso);
    if (days == null) return date;
    if (days < 0) return '$date (expired ${-days}d ago)';
    if (days == 0) return '$date (expires today)';
    return '$date (in ${days}d)';
  }

  Color? _expiryColor(int? days) {
    if (days == null) return null;
    if (days < 0) return Colors.red.shade700;
    if (days <= 3) return Colors.red.shade600;
    if (days <= 7) return Colors.amber.shade700;
    return Colors.green.shade700;
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
    this.valueColor,
  });

  final IconData icon;
  final String label;
  final String value;
  final Color? valueColor;

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
                Text(
                  value,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: valueColor,
                    fontWeight: valueColor != null ? FontWeight.w600 : null,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Bottom action bar
// ---------------------------------------------------------------------------

class _ActionBar extends ConsumerWidget {
  const _ActionBar({required this.entry});

  final StockEntryModel entry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isActive = entry.status == 'AVAILABLE';
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                icon: const Icon(Icons.remove_circle_outline, size: 18),
                label: const Text('Consume'),
                onPressed: isActive
                    ? () => _showConsumeDialog(context, ref)
                    : null,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                icon: const Icon(Icons.move_down, size: 18),
                label: const Text('Move'),
                onPressed:
                    isActive ? () => _showMoveDialog(context, ref) : null,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                icon: Icon(Icons.delete_outline,
                    size: 18,
                    color: isActive
                        ? Theme.of(context).colorScheme.error
                        : null),
                label: Text(
                  'Discard',
                  style: TextStyle(
                    color:
                        isActive ? Theme.of(context).colorScheme.error : null,
                  ),
                ),
                onPressed: isActive
                    ? () => _discard(context, ref)
                    : null,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                icon: const Icon(Icons.tune, size: 18),
                label: const Text('Adjust'),
                onPressed: isActive
                    ? () => _showAdjustDialog(context, ref)
                    : null,
              ),
            ),
          ],
        ),
      ),
    );
  }

  // -- Consume dialog --------------------------------------------------------

  Future<void> _showConsumeDialog(
      BuildContext context, WidgetRef ref) async {
    final controller = TextEditingController(
        text: _formatQty(entry.quantity));
    final formKey = GlobalKey<FormState>();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Consume stock'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller: controller,
            decoration: InputDecoration(
              labelText: 'Quantity to consume',
              suffixText: entry.unitName,
            ),
            keyboardType:
                const TextInputType.numberWithOptions(decimal: true),
            autofocus: true,
            validator: (v) {
              final q = double.tryParse(v ?? '');
              if (q == null || q <= 0) return 'Enter a positive number';
              if (q > entry.quantity) return 'Exceeds available quantity';
              return null;
            },
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              if (formKey.currentState!.validate()) {
                Navigator.of(ctx).pop(true);
              }
            },
            child: const Text('Consume'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;
    final qty = double.tryParse(controller.text);
    if (qty == null) return;

    try {
      await ref
          .read(inventoryRepositoryProvider)
          .consumeStock(entry.id, qty);
      ref.invalidate(stockEntriesProvider);
      if (context.mounted) Navigator.of(context).pop();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to consume: $e')),
        );
      }
    }
  }

  // -- Move dialog -----------------------------------------------------------

  Future<void> _showMoveDialog(BuildContext context, WidgetRef ref) async {
    final formKey = GlobalKey<FormState>();
    int? selectedPlaceId;
    final qtyController =
        TextEditingController(text: _formatQty(entry.quantity));

    final places = await ref
        .read(catalogRepositoryProvider)
        .cachedStoragePlaces();

    if (!context.mounted) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          title: const Text('Move stock'),
          content: Form(
            key: formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<int>(
                  decoration:
                      const InputDecoration(labelText: 'Move to'),
                  items: places
                      .where((p) => p.id != entry.storagePlaceId)
                      .map((p) => DropdownMenuItem(
                            value: p.id,
                            child: Text(p.name),
                          ))
                      .toList(),
                  onChanged: (v) => setState(() => selectedPlaceId = v),
                  validator: (v) =>
                      v == null ? 'Select a storage place' : null,
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: qtyController,
                  decoration: InputDecoration(
                    labelText: 'Quantity',
                    suffixText: entry.unitName,
                  ),
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  validator: (v) {
                    final q = double.tryParse(v ?? '');
                    if (q == null || q <= 0) return 'Enter a positive number';
                    return null;
                  },
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.of(ctx).pop(false),
                child: const Text('Cancel')),
            FilledButton(
              onPressed: () {
                if (formKey.currentState!.validate()) {
                  Navigator.of(ctx).pop(true);
                }
              },
              child: const Text('Move'),
            ),
          ],
        ),
      ),
    );

    if (confirmed != true || selectedPlaceId == null) return;
    final qty = double.tryParse(qtyController.text);

    try {
      await ref.read(inventoryRepositoryProvider).moveStock(
            id: entry.id,
            toStoragePlaceId: selectedPlaceId!,
            quantity: qty,
          );
      ref.invalidate(stockEntriesProvider);
      if (context.mounted) Navigator.of(context).pop();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to move: $e')),
        );
      }
    }
  }

  // -- Discard ---------------------------------------------------------------

  Future<void> _discard(BuildContext context, WidgetRef ref) async {
    final confirmed = await ConfirmDialog.show(
      context,
      title: 'Discard stock',
      message:
          'Mark "${entry.productName ?? 'this entry'}" as discarded? This cannot be undone.',
      confirmLabel: 'Discard',
      destructive: true,
    );
    if (!confirmed) return;

    try {
      await ref.read(inventoryRepositoryProvider).discardStock(entry.id);
      ref.invalidate(stockEntriesProvider);
      if (context.mounted) Navigator.of(context).pop();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to discard: $e')),
        );
      }
    }
  }

  // -- Adjust dialog ---------------------------------------------------------

  Future<void> _showAdjustDialog(
      BuildContext context, WidgetRef ref) async {
    final formKey = GlobalKey<FormState>();
    final deltaController = TextEditingController();
    final reasonController = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Adjust quantity'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: deltaController,
                decoration: InputDecoration(
                  labelText: 'Delta (positive or negative)',
                  suffixText: entry.unitName,
                  hintText: 'e.g. -0.5 or 2',
                ),
                keyboardType: const TextInputType.numberWithOptions(
                    decimal: true, signed: true),
                autofocus: true,
                validator: (v) {
                  if (double.tryParse(v ?? '') == null) {
                    return 'Enter a number (e.g. -1 or 2)';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: reasonController,
                decoration: const InputDecoration(
                  labelText: 'Reason',
                  hintText: 'e.g. Correction, spillage',
                ),
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Required' : null,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              if (formKey.currentState!.validate()) {
                Navigator.of(ctx).pop(true);
              }
            },
            child: const Text('Adjust'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;
    final delta = double.tryParse(deltaController.text);
    if (delta == null) return;

    try {
      await ref.read(inventoryRepositoryProvider).adjustStock(
            id: entry.id,
            delta: delta,
            reason: reasonController.text.trim(),
          );
      ref.invalidate(stockEntriesProvider);
      if (context.mounted) Navigator.of(context).pop();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to adjust: $e')),
        );
      }
    }
  }

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(2);
}
