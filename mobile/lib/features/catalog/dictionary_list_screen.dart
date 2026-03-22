import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/dictionary_model.dart';
import '../../shared/widgets/confirm_dialog.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/error_view.dart';
import 'dictionary_form_dialog.dart';

/// Generic list screen for any dictionary entity (StoragePlace, Category, Store).
///
/// Accepts async-aware callbacks for CRUD so it can be reused for all three
/// entity types without knowing which repository method to call.
class DictionaryListScreen extends ConsumerWidget {
  const DictionaryListScreen({
    super.key,
    required this.title,
    required this.provider,
    required this.onRefresh,
    required this.onCreate,
    required this.onUpdate,
    required this.onDelete,
  });

  final String title;

  /// Riverpod provider that yields the list.
  final FutureProvider<List<DictionaryModel>> provider;

  /// Called to force-refresh the list (e.g. after a mutation).
  final Future<void> Function() onRefresh;

  final Future<void> Function(Map<String, dynamic> body) onCreate;
  final Future<void> Function(int id, Map<String, dynamic> body) onUpdate;
  final Future<void> Function(int id) onDelete;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final itemsAsync = ref.watch(provider);

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: itemsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => ErrorView(
          message: 'Failed to load $title: $e',
          onRetry: () => ref.invalidate(provider),
        ),
        data: (items) {
          if (items.isEmpty) {
            return EmptyState(
              icon: Icons.category_outlined,
              title: 'No $title yet',
              subtitle: 'Tap + to add one.',
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(provider),
            child: ListView.separated(
              itemCount: items.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (ctx, index) {
                final item = items[index];
                return Dismissible(
                  key: ValueKey('dict-${item.id}'),
                  direction: DismissDirection.endToStart,
                  background: Container(
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.only(right: 24),
                    color: Theme.of(ctx).colorScheme.error,
                    child: const Icon(Icons.delete, color: Colors.white),
                  ),
                  confirmDismiss: (_) async {
                    return ConfirmDialog.show(
                      ctx,
                      title: 'Delete ${item.name}?',
                      message: 'This cannot be undone.',
                      confirmLabel: 'Delete',
                      destructive: true,
                    );
                  },
                  onDismissed: (_) async {
                    try {
                      await onDelete(item.id);
                      ref.invalidate(provider);
                    } catch (e) {
                      if (ctx.mounted) {
                        ScaffoldMessenger.of(ctx).showSnackBar(
                          SnackBar(content: Text('Failed to delete: $e')),
                        );
                      }
                    }
                  },
                  child: ListTile(
                    leading: _DictIcon(icon: item.icon, color: item.color),
                    title: Text(item.name),
                    subtitle: item.description != null &&
                            item.description!.isNotEmpty
                        ? Text(
                            item.description!,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          )
                        : null,
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (!item.active)
                          Chip(
                            label: const Text('Inactive',
                                style: TextStyle(fontSize: 11)),
                            visualDensity: VisualDensity.compact,
                            materialTapTargetSize:
                                MaterialTapTargetSize.shrinkWrap,
                          ),
                        IconButton(
                          icon: const Icon(Icons.edit_outlined, size: 20),
                          onPressed: () =>
                              _openEditDialog(ctx, ref, item),
                        ),
                      ],
                    ),
                    onTap: () => _openEditDialog(ctx, ref, item),
                  ),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _openCreateDialog(context, ref),
        child: const Icon(Icons.add),
      ),
    );
  }

  Future<void> _openCreateDialog(
      BuildContext context, WidgetRef ref) async {
    final body = await DictionaryFormDialog.show(
      context,
      title: 'New $title',
    );
    if (body == null) return;
    try {
      await onCreate(body);
      ref.invalidate(provider);
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to create: $e')),
        );
      }
    }
  }

  Future<void> _openEditDialog(
      BuildContext context, WidgetRef ref, DictionaryModel item) async {
    final body = await DictionaryFormDialog.show(
      context,
      title: 'Edit ${item.name}',
      existing: item,
    );
    if (body == null) return;
    try {
      await onUpdate(item.id, body);
      ref.invalidate(provider);
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to update: $e')),
        );
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Icon helper
// ---------------------------------------------------------------------------

class _DictIcon extends StatelessWidget {
  const _DictIcon({this.icon, this.color});

  final String? icon;
  final String? color;

  @override
  Widget build(BuildContext context) {
    final bg = _parseColor(color, context);
    return CircleAvatar(
      backgroundColor: bg.withValues(alpha: 0.2),
      child: Icon(
        _iconData(icon),
        size: 20,
        color: bg,
      ),
    );
  }

  IconData _iconData(String? name) {
    switch (name) {
      case 'kitchen':
        return Icons.kitchen;
      case 'freezer':
        return Icons.ac_unit;
      case 'pantry':
        return Icons.shelves;
      case 'fridge':
        return Icons.kitchen_outlined;
      case 'store':
        return Icons.store;
      case 'category':
        return Icons.category;
      default:
        return Icons.label_outline;
    }
  }

  Color _parseColor(String? hex, BuildContext ctx) {
    if (hex == null || hex.isEmpty) {
      return Theme.of(ctx).colorScheme.primary;
    }
    try {
      final clean = hex.replaceAll('#', '');
      return Color(int.parse('FF$clean', radix: 16));
    } catch (_) {
      return Theme.of(ctx).colorScheme.primary;
    }
  }
}
