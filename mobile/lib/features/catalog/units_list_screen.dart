import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/unit_model.dart';
import '../../shared/widgets/confirm_dialog.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/error_view.dart';
import 'catalog_providers.dart';
import 'unit_form_dialog.dart';

class UnitsListScreen extends ConsumerWidget {
  const UnitsListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final unitsAsync = ref.watch(unitsListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Units')),
      body: unitsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => ErrorView(
          message: 'Failed to load units: $e',
          onRetry: () => ref.invalidate(unitsListProvider),
        ),
        data: (units) {
          if (units.isEmpty) {
            return const EmptyState(
              icon: Icons.straighten,
              title: 'No units yet',
              subtitle: 'Tap + to add a unit.',
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(unitsListProvider),
            child: ListView.separated(
              itemCount: units.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (ctx, index) {
                final unit = units[index];
                return Dismissible(
                  key: ValueKey('unit-${unit.id}'),
                  direction: DismissDirection.endToStart,
                  background: Container(
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.only(right: 24),
                    color: Theme.of(ctx).colorScheme.error,
                    child: const Icon(Icons.delete, color: Colors.white),
                  ),
                  confirmDismiss: (_) async => ConfirmDialog.show(
                    ctx,
                    title: 'Delete unit "${unit.name}"?',
                    message: 'This cannot be undone.',
                    confirmLabel: 'Delete',
                    destructive: true,
                  ),
                  onDismissed: (_) async {
                    try {
                      await ref
                          .read(catalogRepositoryProvider)
                          .deleteUnit(unit.id);
                      ref.invalidate(unitsListProvider);
                    } catch (e) {
                      if (ctx.mounted) {
                        ScaffoldMessenger.of(ctx).showSnackBar(
                          SnackBar(
                              content: Text('Failed to delete: $e')),
                        );
                      }
                    }
                  },
                  child: ListTile(
                    leading: _UnitTypeBadge(type: unit.unitType),
                    title: Text(unit.name),
                    subtitle: Text('${unit.code} · ${unit.shortName}'),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (!unit.active)
                          const Chip(
                            label: Text('Inactive',
                                style: TextStyle(fontSize: 11)),
                            visualDensity: VisualDensity.compact,
                            materialTapTargetSize:
                                MaterialTapTargetSize.shrinkWrap,
                          ),
                        IconButton(
                          icon: const Icon(Icons.edit_outlined, size: 20),
                          onPressed: () =>
                              _openEditDialog(ctx, ref, unit),
                        ),
                      ],
                    ),
                    onTap: () => _openEditDialog(ctx, ref, unit),
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
    final body = await UnitFormDialog.show(context);
    if (body == null) return;
    try {
      await ref.read(catalogRepositoryProvider).createUnit(body);
      ref.invalidate(unitsListProvider);
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to create unit: $e')),
        );
      }
    }
  }

  Future<void> _openEditDialog(
      BuildContext context, WidgetRef ref, UnitModel unit) async {
    final body = await UnitFormDialog.show(context, existing: unit);
    if (body == null) return;
    try {
      await ref
          .read(catalogRepositoryProvider)
          .updateUnit(unit.id, body);
      ref.invalidate(unitsListProvider);
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to update unit: $e')),
        );
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Type badge
// ---------------------------------------------------------------------------

class _UnitTypeBadge extends StatelessWidget {
  const _UnitTypeBadge({required this.type});

  final String type;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return CircleAvatar(
      backgroundColor:
          theme.colorScheme.primaryContainer.withValues(alpha: 0.5),
      child: Text(
        _abbr(type),
        style: theme.textTheme.labelSmall?.copyWith(
          fontWeight: FontWeight.w700,
          color: theme.colorScheme.primary,
        ),
      ),
    );
  }

  String _abbr(String t) {
    switch (t) {
      case 'COUNT':
        return 'Cnt';
      case 'WEIGHT':
        return 'Wt';
      case 'VOLUME':
        return 'Vol';
      case 'PACKAGING':
        return 'Pkg';
      default:
        return t.substring(0, 1);
    }
  }
}
