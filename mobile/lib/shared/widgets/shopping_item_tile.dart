import 'package:flutter/material.dart';

import '../../data/models/shopping_item_model.dart';

/// Dismissible list tile for a shopping item.
///
/// - Leading checkbox: filled green when completed.
/// - Swipe right to complete (Dismissible).
/// - Tap to open detail / edit.
class ShoppingItemTile extends StatelessWidget {
  const ShoppingItemTile({
    super.key,
    required this.item,
    this.onTap,
    this.onComplete,
  });

  final ShoppingItemModel item;
  final VoidCallback? onTap;
  final VoidCallback? onComplete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isCompleted = item.isCompleted;

    Widget tile = ListTile(
      leading: GestureDetector(
        onTap: isCompleted ? null : onComplete,
        child: Icon(
          isCompleted ? Icons.check_circle : Icons.radio_button_unchecked,
          color: isCompleted
              ? Colors.green.shade600
              : theme.colorScheme.onSurface.withValues(alpha: 0.4),
          size: 26,
        ),
      ),
      title: Text(
        item.title,
        style: theme.textTheme.bodyLarge?.copyWith(
          decoration:
              isCompleted ? TextDecoration.lineThrough : null,
          color: isCompleted
              ? theme.colorScheme.onSurface.withValues(alpha: 0.4)
              : null,
        ),
      ),
      subtitle: Text(
        _subtitleText(),
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
        ),
      ),
      onTap: onTap,
      contentPadding:
          const EdgeInsets.symmetric(horizontal: 16, vertical: 2),
    );

    // Only add swipe-to-complete for active items.
    if (!isCompleted && onComplete != null) {
      tile = Dismissible(
        key: ValueKey('shopping-${item.id}'),
        direction: DismissDirection.startToEnd,
        onDismissed: (_) => onComplete!(),
        background: Container(
          alignment: Alignment.centerLeft,
          padding: const EdgeInsets.only(left: 24),
          color: Colors.green.shade400,
          child: const Icon(Icons.check, color: Colors.white),
        ),
        child: tile,
      );
    }

    return tile;
  }

  String _subtitleText() {
    final qty = item.quantity == item.quantity.roundToDouble()
        ? item.quantity.toInt().toString()
        : item.quantity.toStringAsFixed(1);
    return 'Qty: $qty';
  }
}
