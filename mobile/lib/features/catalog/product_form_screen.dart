import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/dictionary_model.dart';
import '../../data/models/product_model.dart';
import '../../data/models/unit_model.dart';
import 'catalog_providers.dart';

class ProductFormScreen extends ConsumerStatefulWidget {
  const ProductFormScreen({super.key, this.existing});

  final ProductModel? existing;

  @override
  ConsumerState<ProductFormScreen> createState() =>
      _ProductFormScreenState();
}

class _ProductFormScreenState extends ConsumerState<ProductFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _shelfLifeDaysController;
  late final TextEditingController _minQtyController;
  late final TextEditingController _reorderQtyController;
  late final TextEditingController _barcodeController;
  late final TextEditingController _noteController;

  int? _selectedCategoryId;
  int? _selectedUnitId;
  int? _selectedStoragePlaceId;
  int? _selectedStoreId;
  late bool _autoAddShopping;
  late bool _active;
  bool _submitting = false;

  bool get _isEditing => widget.existing != null;

  @override
  void initState() {
    super.initState();
    final p = widget.existing;
    _nameController = TextEditingController(text: p?.name ?? '');
    _descriptionController =
        TextEditingController(text: p?.description ?? '');
    _shelfLifeDaysController = TextEditingController(
        text: p?.shelfLifeDays?.toString() ?? '');
    _minQtyController = TextEditingController(
        text: p?.minimumQuantityThreshold != null
            ? _fmtQty(p!.minimumQuantityThreshold!)
            : '');
    _reorderQtyController = TextEditingController(
        text: p?.reorderQuantity != null
            ? _fmtQty(p!.reorderQuantity!)
            : '');
    _barcodeController = TextEditingController(text: p?.barcode ?? '');
    _noteController = TextEditingController(text: p?.note ?? '');
    _selectedCategoryId = p?.categoryId;
    _selectedUnitId = p?.defaultUnitId;
    _selectedStoragePlaceId = p?.defaultStoragePlaceId;
    _selectedStoreId = p?.defaultStoreId;
    _autoAddShopping = p?.autoAddShopping ?? false;
    _active = p?.active ?? true;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _shelfLifeDaysController.dispose();
    _minQtyController.dispose();
    _reorderQtyController.dispose();
    _barcodeController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final categoriesAsync = ref.watch(categoriesListProvider);
    final unitsAsync = ref.watch(unitsListProvider);
    final placesAsync = ref.watch(storagePlacesListProvider);
    final storesAsync = ref.watch(storesListProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit Product' : 'New Product'),
        actions: [
          _submitting
              ? const Padding(
                  padding: EdgeInsets.all(12),
                  child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2)))
              : TextButton(
                  onPressed: () => _submit(context),
                  child: const Text('Save'),
                ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // --- Basic info ---
            _SectionLabel(label: 'Basic Info'),
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Product name *',
                border: OutlineInputBorder(),
              ),
              autofocus: !_isEditing,
              validator: (v) =>
                  v == null || v.trim().isEmpty ? 'Required' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _descriptionController,
              decoration: const InputDecoration(
                labelText: 'Description',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 12),

            // Category
            _DropdownField<DictionaryModel>(
              label: 'Category *',
              asyncValue: categoriesAsync,
              items: categoriesAsync.valueOrNull ?? [],
              itemLabel: (c) => c.name,
              itemValue: (c) => c.id,
              selectedValue: _selectedCategoryId,
              onChanged: (id) =>
                  setState(() => _selectedCategoryId = id),
              required: true,
            ),
            const SizedBox(height: 12),

            // Unit
            _DropdownField<UnitModel>(
              label: 'Default unit *',
              asyncValue: unitsAsync,
              items: unitsAsync.valueOrNull ?? [],
              itemLabel: (u) => '${u.name} (${u.shortName})',
              itemValue: (u) => u.id,
              selectedValue: _selectedUnitId,
              onChanged: (id) => setState(() => _selectedUnitId = id),
              required: true,
            ),
            const SizedBox(height: 12),

            // Storage place
            _DropdownField<DictionaryModel>(
              label: 'Default storage place *',
              asyncValue: placesAsync,
              items: placesAsync.valueOrNull ?? [],
              itemLabel: (p) => p.name,
              itemValue: (p) => p.id,
              selectedValue: _selectedStoragePlaceId,
              onChanged: (id) =>
                  setState(() => _selectedStoragePlaceId = id),
              required: true,
            ),
            const SizedBox(height: 12),

            // Store (optional)
            _DropdownField<DictionaryModel>(
              label: 'Default store (optional)',
              asyncValue: storesAsync,
              items: storesAsync.valueOrNull ?? [],
              itemLabel: (s) => s.name,
              itemValue: (s) => s.id,
              selectedValue: _selectedStoreId,
              onChanged: (id) => setState(() => _selectedStoreId = id),
              required: false,
            ),
            const SizedBox(height: 20),

            // --- Lifecycle & thresholds ---
            _SectionLabel(label: 'Lifecycle & Thresholds'),
            TextFormField(
              controller: _shelfLifeDaysController,
              decoration: const InputDecoration(
                labelText: 'Shelf life (days)',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              validator: (v) {
                if (v == null || v.isEmpty) return null;
                if (int.tryParse(v) == null || int.parse(v) < 0) {
                  return 'Enter a positive integer';
                }
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _minQtyController,
              decoration: const InputDecoration(
                labelText: 'Minimum quantity threshold',
                border: OutlineInputBorder(),
              ),
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.isEmpty) return null;
                if (double.tryParse(v) == null) return 'Enter a number';
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _reorderQtyController,
              decoration: const InputDecoration(
                labelText: 'Reorder quantity',
                border: OutlineInputBorder(),
              ),
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.isEmpty) return null;
                if (double.tryParse(v) == null) return 'Enter a number';
                return null;
              },
            ),
            const SizedBox(height: 12),
            SwitchListTile(
              title: const Text('Auto-add to shopping list'),
              subtitle: const Text(
                  'Add automatically when stock falls below threshold'),
              value: _autoAddShopping,
              onChanged: (v) => setState(() => _autoAddShopping = v),
            ),
            const SizedBox(height: 20),

            // --- Additional info ---
            _SectionLabel(label: 'Additional Info'),
            TextFormField(
              controller: _barcodeController,
              decoration: const InputDecoration(
                labelText: 'Barcode',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.qr_code_outlined),
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _noteController,
              decoration: const InputDecoration(
                labelText: 'Note',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 12),
            SwitchListTile(
              title: const Text('Active'),
              subtitle:
                  const Text('Inactive products are hidden from forms'),
              value: _active,
              onChanged: (v) => setState(() => _active = v),
            ),
            const SizedBox(height: 24),

            FilledButton(
              onPressed: _submitting ? null : () => _submit(context),
              child: Text(
                  _isEditing ? 'Update Product' : 'Create Product'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _submit(BuildContext context) async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    final body = <String, dynamic>{
      'name': _nameController.text.trim(),
      'categoryId': _selectedCategoryId,
      'defaultUnitId': _selectedUnitId,
      'defaultStoragePlaceId': _selectedStoragePlaceId,
      if (_selectedStoreId != null) 'defaultStoreId': _selectedStoreId,
      if (_descriptionController.text.isNotEmpty)
        'description': _descriptionController.text.trim(),
      if (_shelfLifeDaysController.text.isNotEmpty)
        'shelfLifeDays': int.parse(_shelfLifeDaysController.text),
      if (_minQtyController.text.isNotEmpty)
        'minimumQuantityThreshold':
            double.parse(_minQtyController.text),
      if (_reorderQtyController.text.isNotEmpty)
        'reorderQuantity': double.parse(_reorderQtyController.text),
      'autoAddShopping': _autoAddShopping,
      if (_barcodeController.text.isNotEmpty)
        'barcode': _barcodeController.text.trim(),
      if (_noteController.text.isNotEmpty)
        'note': _noteController.text.trim(),
      'active': _active,
    };

    try {
      final repo = ref.read(catalogRepositoryProvider);
      if (_isEditing) {
        await repo.updateProduct(widget.existing!.id, body);
      } else {
        await repo.createProduct(body);
      }
      ref.invalidate(productsListProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(_isEditing
                  ? 'Product updated'
                  : 'Product created')),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String _fmtQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(2);
}

// ---------------------------------------------------------------------------
// Section label
// ---------------------------------------------------------------------------

class _SectionLabel extends StatelessWidget {
  const _SectionLabel({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Text(
        label,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          fontWeight: FontWeight.w700,
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Reusable dropdown field
// ---------------------------------------------------------------------------

class _DropdownField<T> extends StatelessWidget {
  const _DropdownField({
    required this.label,
    required this.asyncValue,
    required this.items,
    required this.itemLabel,
    required this.itemValue,
    required this.selectedValue,
    required this.onChanged,
    required this.required,
  });

  final String label;
  final AsyncValue<List<T>> asyncValue;
  final List<T> items;
  final String Function(T) itemLabel;
  final int Function(T) itemValue;
  final int? selectedValue;
  final void Function(int?) onChanged;
  final bool required;

  @override
  Widget build(BuildContext context) {
    return asyncValue.when(
      loading: () => InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
        child: const SizedBox(
            height: 20,
            child:
                Center(child: CircularProgressIndicator(strokeWidth: 2))),
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
        items: [
          if (!required)
            const DropdownMenuItem<int>(
              value: null,
              child: Text('— None —'),
            ),
          ...items.map((item) => DropdownMenuItem<int>(
                value: itemValue(item),
                child: Text(itemLabel(item)),
              )),
        ],
        onChanged: onChanged,
        validator: required
            ? (v) => v == null ? 'Select one' : null
            : null,
      ),
    );
  }
}
