import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/dictionary_model.dart';
import '../../data/models/shopping_item_model.dart';
import '../../data/models/unit_model.dart';
import '../catalog/catalog_providers.dart';
import 'shopping_list_screen.dart';

class ShoppingItemFormScreen extends ConsumerStatefulWidget {
  const ShoppingItemFormScreen({super.key, this.existing});

  /// Pass an existing item for edit mode; null for create mode.
  final ShoppingItemModel? existing;

  @override
  ConsumerState<ShoppingItemFormScreen> createState() =>
      _ShoppingItemFormScreenState();
}

class _ShoppingItemFormScreenState
    extends ConsumerState<ShoppingItemFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleController;
  late final TextEditingController _quantityController;
  late final TextEditingController _commentController;
  late final TextEditingController _sortOrderController;

  int? _selectedUnitId;
  int? _selectedStoreId;
  bool _submitting = false;

  bool get _isEditing => widget.existing != null;

  @override
  void initState() {
    super.initState();
    final item = widget.existing;
    _titleController = TextEditingController(text: item?.title ?? '');
    _quantityController = TextEditingController(
        text: item != null ? _formatQty(item.quantity) : '1');
    _commentController = TextEditingController(text: item?.comment ?? '');
    _sortOrderController = TextEditingController(
        text: item?.sortOrder.toString() ?? '0');
    _selectedUnitId = item?.unitId;
    _selectedStoreId = item?.storeId;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _quantityController.dispose();
    _commentController.dispose();
    _sortOrderController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final storesAsync = ref.watch(storesListProvider);
    final unitsAsync = ref.watch(unitsListProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit Item' : 'Add Shopping Item'),
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
            // Title
            TextFormField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: 'Item name',
                border: OutlineInputBorder(),
              ),
              textCapitalization: TextCapitalization.sentences,
              autofocus: !_isEditing,
              validator: (v) =>
                  v == null || v.trim().isEmpty ? 'Required' : null,
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
              label: 'Unit (optional)',
              asyncValue: unitsAsync,
              items: unitsAsync.valueOrNull ?? [],
              itemLabel: (u) => '${u.name} (${u.shortName})',
              itemValue: (u) => u.id,
              selectedValue: _selectedUnitId,
              onChanged: (id) => setState(() => _selectedUnitId = id),
              required: false,
            ),
            const SizedBox(height: 12),

            // Store dropdown
            _buildDropdownField<DictionaryModel>(
              label: 'Store (optional)',
              asyncValue: storesAsync,
              items: storesAsync.valueOrNull ?? [],
              itemLabel: (s) => s.name,
              itemValue: (s) => s.id,
              selectedValue: _selectedStoreId,
              onChanged: (id) => setState(() => _selectedStoreId = id),
              required: false,
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
            const SizedBox(height: 12),

            // Sort order
            TextFormField(
              controller: _sortOrderController,
              decoration: const InputDecoration(
                labelText: 'Sort order',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              validator: (v) {
                if (int.tryParse(v ?? '') == null) return 'Enter a number';
                return null;
              },
            ),
            const SizedBox(height: 24),

            FilledButton(
              onPressed: _submitting ? null : () => _submit(context),
              child: Text(_isEditing ? 'Update Item' : 'Add to List'),
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
    bool required = true,
  }) {
    return asyncValue.when(
      loading: () => InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
        child: const SizedBox(
          height: 20,
          child:
              Center(child: CircularProgressIndicator(strokeWidth: 2)),
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

  Future<void> _submit(BuildContext context) async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    final qty = double.parse(_quantityController.text);
    final sortOrder = int.tryParse(_sortOrderController.text) ?? 0;
    final comment = _commentController.text.isEmpty
        ? null
        : _commentController.text;

    try {
      final repo = ref.read(shoppingRepositoryProvider);
      if (_isEditing) {
        await repo.updateItem(widget.existing!.id, {
          'title': _titleController.text.trim(),
          'quantity': qty,
          if (_selectedUnitId != null) 'unitId': _selectedUnitId,
          if (_selectedStoreId != null) 'storeId': _selectedStoreId,
          if (comment != null) 'comment': comment,
          'sortOrder': sortOrder,
        });
      } else {
        await repo.createItem(
          title: _titleController.text.trim(),
          quantity: qty,
          unitId: _selectedUnitId,
          storeId: _selectedStoreId,
          comment: comment,
          sortOrder: sortOrder,
          source: 'MANUAL',
        );
      }
      ref.invalidate(shoppingItemsProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(
                  _isEditing ? 'Item updated' : 'Item added to list')),
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

  String _formatQty(double q) =>
      q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(1);
}
