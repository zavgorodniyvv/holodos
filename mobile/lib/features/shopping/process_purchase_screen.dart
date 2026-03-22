import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/shopping_item_model.dart';
import '../catalog/catalog_providers.dart';
import '../inventory/inventory_screen.dart';
import 'shopping_list_screen.dart';

/// Screen to convert a completed shopping item into a stock entry
/// by calling POST /purchases/process on the backend.
class ProcessPurchaseScreen extends ConsumerStatefulWidget {
  const ProcessPurchaseScreen({super.key, required this.item});

  final ShoppingItemModel item;

  @override
  ConsumerState<ProcessPurchaseScreen> createState() =>
      _ProcessPurchaseScreenState();
}

class _ProcessPurchaseScreenState
    extends ConsumerState<ProcessPurchaseScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _quantityController;
  final _commentController = TextEditingController();

  int? _selectedStoragePlaceId;
  DateTime? _purchasedAt;
  DateTime? _expiresAt;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _quantityController = TextEditingController(
        text: _formatQty(widget.item.quantity));
    _purchasedAt = DateTime.now();
  }

  @override
  void dispose() {
    _quantityController.dispose();
    _commentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final placesAsync = ref.watch(storagePlacesListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Process Purchase'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Info card about the shopping item
            Card(
              color: Theme.of(context)
                  .colorScheme
                  .surfaceContainerHighest
                  .withValues(alpha: 0.5),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    const Icon(Icons.shopping_cart_outlined),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(widget.item.title,
                              style: const TextStyle(
                                  fontWeight: FontWeight.w600)),
                          Text(
                            'Shopping list item',
                            style: Theme.of(context)
                                .textTheme
                                .bodySmall
                                ?.copyWith(
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onSurface
                                      .withValues(alpha: 0.6),
                                ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Actual quantity purchased
            TextFormField(
              controller: _quantityController,
              decoration: const InputDecoration(
                labelText: 'Actual quantity',
                border: OutlineInputBorder(),
              ),
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                final q = double.tryParse(v ?? '');
                if (q == null || q <= 0) return 'Enter a positive number';
                return null;
              },
            ),
            const SizedBox(height: 12),

            // Storage place dropdown
            placesAsync.when(
              loading: () => const InputDecorator(
                decoration: InputDecoration(
                  labelText: 'Storage place',
                  border: OutlineInputBorder(),
                ),
                child: SizedBox(
                  height: 20,
                  child: Center(
                      child: CircularProgressIndicator(strokeWidth: 2)),
                ),
              ),
              error: (e, _) => const InputDecorator(
                decoration: InputDecoration(
                  labelText: 'Storage place',
                  border: OutlineInputBorder(),
                  errorText: 'Failed to load',
                ),
                child: SizedBox.shrink(),
              ),
              data: (places) => DropdownButtonFormField<int>(
                decoration: const InputDecoration(
                  labelText: 'Storage place',
                  border: OutlineInputBorder(),
                ),
                value: _selectedStoragePlaceId,
                items: places
                    .map((p) => DropdownMenuItem<int>(
                          value: p.id,
                          child: Text(p.name),
                        ))
                    .toList(),
                onChanged: (id) =>
                    setState(() => _selectedStoragePlaceId = id),
                validator: (v) =>
                    v == null ? 'Select a storage place' : null,
              ),
            ),
            const SizedBox(height: 12),

            // Purchased date
            _DatePickerField(
              label: 'Purchased date',
              value: _purchasedAt,
              onChanged: (d) => setState(() => _purchasedAt = d),
            ),
            const SizedBox(height: 12),

            // Expiry date
            _DatePickerField(
              label: 'Expires at (optional)',
              value: _expiresAt,
              onChanged: (d) => setState(() => _expiresAt = d),
            ),
            const SizedBox(height: 12),

            // Comment
            TextFormField(
              controller: _commentController,
              decoration: const InputDecoration(
                labelText: 'Comment (optional)',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 24),

            FilledButton.icon(
              icon: const Icon(Icons.inventory_2_outlined),
              label: const Text('Add to Inventory'),
              onPressed: _submitting ? null : () => _submit(context),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _submit(BuildContext context) async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    final qty = double.parse(_quantityController.text);

    try {
      final api = ref.read(holodosApiProvider);
      await api.syncQueue([
        {
          'type': 'PURCHASE_PROCESS',
          'payload': {
            'shoppingItemId': widget.item.id,
            'actualQuantity': qty,
            'storagePlaceId': _selectedStoragePlaceId,
            if (_purchasedAt != null)
              'purchasedAt': _purchasedAt!.toIso8601String(),
            if (_expiresAt != null)
              'expiresAt': _expiresAt!.toIso8601String(),
            if (_commentController.text.isNotEmpty)
              'comment': _commentController.text,
          },
        }
      ]);
      ref.invalidate(stockEntriesProvider);
      ref.invalidate(shoppingItemsProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('Purchase processed — added to inventory')),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to process purchase: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(1);
}

// ---------------------------------------------------------------------------
// Date picker helper widget
// ---------------------------------------------------------------------------

class _DatePickerField extends StatelessWidget {
  const _DatePickerField({
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final DateTime? value;
  final void Function(DateTime?) onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: () async {
        final picked = await showDatePicker(
          context: context,
          initialDate: value ?? DateTime.now(),
          firstDate: DateTime(2000),
          lastDate: DateTime(2100),
        );
        onChanged(picked);
      },
      borderRadius: BorderRadius.circular(4),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
          suffixIcon: value != null
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 18),
                  onPressed: () => onChanged(null),
                )
              : const Icon(Icons.calendar_today_outlined, size: 18),
        ),
        child: Text(
          value != null ? _format(value!) : 'Not set',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: value != null
                ? null
                : theme.colorScheme.onSurface.withValues(alpha: 0.5),
          ),
        ),
      ),
    );
  }

  String _format(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}
