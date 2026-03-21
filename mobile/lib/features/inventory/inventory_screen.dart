import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/stock_entry_model.dart';

final stockEntriesProvider = FutureProvider<List<StockEntryModel>>((ref) async {
  final repo = ref.watch(inventoryRepositoryProvider);
  final cached = await repo.cachedEntries();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshEntries());
    return cached;
  }
  return repo.refreshEntries();
});

class InventoryScreen extends ConsumerWidget {
  const InventoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final entries = ref.watch(stockEntriesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Inventory')),
      body: entries.when(
        data: (data) => RefreshIndicator(
          onRefresh: () async => ref.invalidate(stockEntriesProvider),
          child: ListView.separated(
            padding: const EdgeInsets.all(16),
            itemBuilder: (context, index) {
              final entry = data[index];
              return ListTile(
                title: Text(entry.productName),
                subtitle:
                    Text('Qty: ${entry.quantity} ${entry.storagePlace ?? ''}'),
                trailing: Text(entry.status),
              );
            },
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemCount: data.length,
          ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, _) =>
            Center(child: Text('Failed to load inventory: $err')),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => showModalBottomSheet<void>(
          context: context,
          isScrollControlled: true,
          builder: (_) => const _AddStockSheet(),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}

class _AddStockSheet extends ConsumerStatefulWidget {
  const _AddStockSheet();

  @override
  ConsumerState<_AddStockSheet> createState() => _AddStockSheetState();
}

class _AddStockSheetState extends ConsumerState<_AddStockSheet> {
  final _formKey = GlobalKey<FormState>();
  final _productController = TextEditingController();
  final _quantityController = TextEditingController(text: '1');
  final _storageController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _productController.dispose();
    _quantityController.dispose();
    _storageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 16,
        right: 16,
        top: 24,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Add stock entry',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            TextFormField(
              controller: _productController,
              decoration: const InputDecoration(labelText: 'Product name'),
              validator: (value) =>
                  value == null || value.isEmpty ? 'Required' : null,
            ),
            TextFormField(
              controller: _quantityController,
              decoration: const InputDecoration(labelText: 'Quantity'),
              keyboardType: TextInputType.numberWithOptions(decimal: true),
              validator: (value) =>
                  value == null || double.tryParse(value) == null
                      ? 'Enter a number'
                      : null,
            ),
            TextFormField(
              controller: _storageController,
              decoration:
                  const InputDecoration(labelText: 'Storage place (optional)'),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _submitting ? null : () => _submit(context),
                child: _submitting
                    ? const CircularProgressIndicator()
                    : const Text('Add to queue'),
              ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Future<void> _submit(BuildContext context) async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    final syncQueue = ref.read(syncQueueProvider);
    await syncQueue.enqueue({
      'type': 'STOCK_ADD',
      'payload': {
        'productName': _productController.text,
        'quantity': double.parse(_quantityController.text),
        'storagePlace':
            _storageController.text.isEmpty ? null : _storageController.text,
      },
      'createdAt': DateTime.now().toIso8601String(),
    });
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Queued stock entry. Will sync when online.')));
      Navigator.of(context).pop();
      ref.invalidate(stockEntriesProvider);
    }
  }
}
