import 'package:flutter/material.dart';

import '../../data/models/unit_model.dart';

/// AlertDialog form for creating or editing a [UnitModel].
///
/// Returns the submitted [Map<String, dynamic>] body ready for the API,
/// or null if the user cancelled.
class UnitFormDialog extends StatefulWidget {
  const UnitFormDialog({super.key, this.existing});

  final UnitModel? existing;

  @override
  State<UnitFormDialog> createState() => _UnitFormDialogState();

  static Future<Map<String, dynamic>?> show(
    BuildContext context, {
    UnitModel? existing,
  }) {
    return showDialog<Map<String, dynamic>>(
      context: context,
      builder: (_) => UnitFormDialog(existing: existing),
    );
  }
}

class _UnitFormDialogState extends State<UnitFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _codeController;
  late final TextEditingController _nameController;
  late final TextEditingController _shortNameController;
  late String _unitType;
  late bool _active;

  static const _unitTypes = ['COUNT', 'WEIGHT', 'VOLUME', 'PACKAGING'];

  @override
  void initState() {
    super.initState();
    final e = widget.existing;
    _codeController = TextEditingController(text: e?.code ?? '');
    _nameController = TextEditingController(text: e?.name ?? '');
    _shortNameController = TextEditingController(text: e?.shortName ?? '');
    _unitType = e?.unitType ?? 'COUNT';
    _active = e?.active ?? true;
  }

  @override
  void dispose() {
    _codeController.dispose();
    _nameController.dispose();
    _shortNameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title:
          Text(widget.existing == null ? 'Create unit' : 'Edit unit'),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _codeController,
                decoration: const InputDecoration(
                  labelText: 'Code *',
                  hintText: 'e.g. KG, L, PCS',
                ),
                autofocus: widget.existing == null,
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Full name *',
                  hintText: 'e.g. Kilogram',
                ),
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _shortNameController,
                decoration: const InputDecoration(
                  labelText: 'Short name *',
                  hintText: 'e.g. kg',
                ),
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                decoration:
                    const InputDecoration(labelText: 'Type'),
                value: _unitType,
                items: _unitTypes
                    .map((t) => DropdownMenuItem(
                          value: t,
                          child: Text(_formatType(t)),
                        ))
                    .toList(),
                onChanged: (v) {
                  if (v != null) setState(() => _unitType = v);
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
      'code': _codeController.text.trim().toUpperCase(),
      'name': _nameController.text.trim(),
      'shortName': _shortNameController.text.trim(),
      'unitType': _unitType,
      'active': _active,
    });
  }

  String _formatType(String t) {
    switch (t) {
      case 'COUNT':
        return 'Count (pieces)';
      case 'WEIGHT':
        return 'Weight';
      case 'VOLUME':
        return 'Volume';
      case 'PACKAGING':
        return 'Packaging';
      default:
        return t;
    }
  }
}
