import 'package:flutter/material.dart';

import '../../data/models/dictionary_model.dart';

/// AlertDialog form for creating or editing a dictionary entity
/// (StoragePlace, Category, or Store).
///
/// Returns the submitted [Map<String, dynamic>] body ready for the API,
/// or null if the user cancelled.
class DictionaryFormDialog extends StatefulWidget {
  const DictionaryFormDialog({
    super.key,
    required this.title,
    this.existing,
  });

  final String title;
  final DictionaryModel? existing;

  @override
  State<DictionaryFormDialog> createState() => _DictionaryFormDialogState();

  static Future<Map<String, dynamic>?> show(
    BuildContext context, {
    required String title,
    DictionaryModel? existing,
  }) {
    return showDialog<Map<String, dynamic>>(
      context: context,
      builder: (_) =>
          DictionaryFormDialog(title: title, existing: existing),
    );
  }
}

class _DictionaryFormDialogState extends State<DictionaryFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _iconController;
  late final TextEditingController _colorController;
  late final TextEditingController _sortOrderController;
  late bool _active;

  @override
  void initState() {
    super.initState();
    final e = widget.existing;
    _nameController = TextEditingController(text: e?.name ?? '');
    _descriptionController =
        TextEditingController(text: e?.description ?? '');
    _iconController = TextEditingController(text: e?.icon ?? '');
    _colorController = TextEditingController(text: e?.color ?? '');
    _sortOrderController =
        TextEditingController(text: e?.sortOrder.toString() ?? '0');
    _active = e?.active ?? true;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _iconController.dispose();
    _colorController.dispose();
    _sortOrderController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.title),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Name *'),
                autofocus: true,
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _descriptionController,
                decoration:
                    const InputDecoration(labelText: 'Description'),
                maxLines: 2,
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _iconController,
                decoration: const InputDecoration(
                  labelText: 'Icon name',
                  hintText: 'e.g. kitchen, freezer',
                ),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _colorController,
                decoration: const InputDecoration(
                  labelText: 'Color (hex)',
                  hintText: 'e.g. #4CAF50',
                ),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _sortOrderController,
                decoration:
                    const InputDecoration(labelText: 'Sort order'),
                keyboardType: TextInputType.number,
                validator: (v) {
                  if (int.tryParse(v ?? '') == null) {
                    return 'Enter a number';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 8),
              SwitchListTile(
                title: const Text('Active'),
                value: _active,
                onChanged: (v) => setState(() => _active = v),
                contentPadding: EdgeInsets.zero,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _save,
          child: Text(widget.existing == null ? 'Create' : 'Save'),
        ),
      ],
    );
  }

  void _save() {
    if (!_formKey.currentState!.validate()) return;
    Navigator.of(context).pop({
      'name': _nameController.text.trim(),
      'description': _descriptionController.text.isEmpty
          ? null
          : _descriptionController.text.trim(),
      'icon': _iconController.text.isEmpty
          ? null
          : _iconController.text.trim(),
      'color': _colorController.text.isEmpty
          ? null
          : _colorController.text.trim(),
      'sortOrder': int.tryParse(_sortOrderController.text) ?? 0,
      'active': _active,
    });
  }
}
