import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/dictionary_model.dart';
import '../../data/models/product_model.dart';
import '../../data/models/unit_model.dart';
import '../catalog/catalog_providers.dart';
import 'inventory_screen.dart';

class StockEntryFormScreen extends ConsumerStatefulWidget {
  const StockEntryFormScreen({super.key});

  @override
  ConsumerState<StockEntryFormScreen> createState() =>
      _StockEntryFormScreenState();
}

class _StockEntryFormScreenState extends ConsumerState<StockEntryFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _quantityController = TextEditingController(text: '1');
  final _commentController = TextEditingController();

  int? _selectedProductId;
  int? _selectedUnitId;
  int? _selectedStoragePlaceId;
  DateTime? _addedAt;
  DateTime? _purchasedAt;
  DateTime? _expiresAt;
  bool _submitting = false;

  @override
  void dispose() {
    _quantityController.dispose();
    _commentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final productsAsync = ref.watch(productsListProvider);
    final placesAsync = ref.watch(storagePlacesListProvider);
    final unitsAsync = ref.watch(unitsListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Add Stock Entry'),
        actions: [
          _submitting
              ? const Padding(
                  padding: EdgeInsets.all(12),
                  child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2)))
              : TextButton(
                  onPressed: () => _submit(
                      context, productsAsync.valueOrNull ?? []),
                  child: const Text('Save'),
                ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Product dropdown
            _buildDropdownField<ProductModel>(
              label: 'Product',
              asyncValue: productsAsync,
              items: productsAsync.valueOrNull ?? [],
              itemLabel: (p) => p.name,
              itemValue: (p) => p.id,
              selectedValue: _selectedProductId,
              onChanged: (id) {
                setState(() => _selectedProductId = id);
                // Auto-fill unit and storage place from product defaults
                final products = productsAsync.valueOrNull ?? [];
                final product =
                    products.where((p) => p.id == id).firstOrNull;
                if (product != null) {
                  setState(() {
                    _selectedUnitId = product.defaultUnitId;
                    _selectedStoragePlaceId =
                        product.defaultStoragePlaceId;
                  });
                }
              },
              validator: (v) => v == null ? 'Select a product' : null,
            ),
            const SizedBox(height: 12),

            // Quantity
            TextFormField(
              controller: _quantityController,
              decoration: const InputDecoration(
                labelText: 'Quantity',
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

            // Unit dropdown
            _buildDropdownField<UnitModel>(
              label: 'Unit',
              asyncValue: unitsAsync,
              items: unitsAsync.valueOrNull ?? [],
              itemLabel: (u) => '${u.name} (${u.shortName})',
              itemValue: (u) => u.id,
              selectedValue: _selectedUnitId,
              onChanged: (id) => setState(() => _selectedUnitId = id),
              validator: (v) => v == null ? 'Select a unit' : null,
            ),
            const SizedBox(height: 12),

            // Storage place dropdown
            _buildDropdownField<DictionaryModel>(
              label: 'Storage place',
              asyncValue: placesAsync,
              items: placesAsync.valueOrNull ?? [],
              itemLabel: (p) => p.name,
              itemValue: (p) => p.id,
              selectedValue: _selectedStoragePlaceId,
              onChanged: (id) =>
                  setState(() => _selectedStoragePlaceId = id),
              validator: (v) => v == null ? 'Select a storage place' : null,
            ),
            const SizedBox(height: 12),

            // Date pickers
            _DatePickerField(
              label: 'Added at',
              value: _addedAt,
              onChanged: (d) => setState(() => _addedAt = d),
            ),
            const SizedBox(height: 12),
            _DatePickerField(
              label: 'Purchased at',
              value: _purchasedAt,
              onChanged: (d) => setState(() => _purchasedAt = d),
            ),
            const SizedBox(height: 12),
            _DatePickerField(
              label: 'Expires at',
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

            FilledButton(
              onPressed: _submitting
                  ? null
                  : () => _submit(
                      context, productsAsync.valueOrNull ?? []),
              child: const Text('Add Stock Entry'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDropdownField<T>({
    required String label,
    required AsyncValue<List<T>> asyncValue,
    required List<T> items,
    required String Function(T) itemLabel,
    required int Function(T) itemValue,
    required int? selectedValue,
    required void Function(int?) onChanged,
    String? Function(int?)? validator,
  }) {
    return asyncValue.when(
      loading: () => InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
        child: const SizedBox(
          height: 20,
          child: Center(
              child: CircularProgressIndicator(strokeWidth: 2)),
        ),
      ),
      error: (e, _) => InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
          errorText: 'Failed to load',
        ),
        child: const SizedBox.shrink(),
      ),
      data: (_) => DropdownButtonFormField<int>(
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
        value: selectedValue,
        items: items
            .map((item) => DropdownMenuItem<int>(
                  value: itemValue(item),
                  child: Text(itemLabel(item)),
                ))
            .toList(),
        onChanged: onChanged,
        validator: validator,
      ),
    );
  }

  Future<void> _submit(
      BuildContext context, List<ProductModel> products) async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    try {
      await ref.read(inventoryRepositoryProvider).addStock(
            productId: _selectedProductId!,
            quantity: double.parse(_quantityController.text),
            unitId: _selectedUnitId!,
            storagePlaceId: _selectedStoragePlaceId!,
            addedAt: _addedAt?.toIso8601String(),
            purchasedAt: _purchasedAt?.toIso8601String(),
            expiresAt: _expiresAt?.toIso8601String(),
            comment: _commentController.text.isEmpty
                ? null
                : _commentController.text,
          );
      ref.invalidate(stockEntriesProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Stock entry added')),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to add stock: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }
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
