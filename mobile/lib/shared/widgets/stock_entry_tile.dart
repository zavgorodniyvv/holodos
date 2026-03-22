import 'package:flutter/material.dart';

import '../../data/models/stock_entry_model.dart';
import 'status_chip.dart';

/// Card tile representing a single stock entry in lists or grids.
///
/// Shows: product name, quantity + unit, storage place, expiry countdown
/// colour-coded by urgency (green → amber → red), and a status chip.
class StockEntryTile extends StatelessWidget {
  const StockEntryTile({
    super.key,
    required this.entry,
    required this.onTap,
  });

  final StockEntryModel entry;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Product name + status chip
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      entry.productName ?? 'Unknown product',
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  StatusChip(status: entry.status),
                ],
              ),
              const SizedBox(height: 4),

              // Quantity + unit
              Text(
                '${_formatQuantity(entry.quantity)} ${entry.unitName ?? ''}',
                style: theme.textTheme.bodyMedium,
              ),

              // Storage place
              if (entry.storagePlaceName != null) ...[
                const SizedBox(height: 2),
                Row(
                  children: [
                    Icon(
                      Icons.location_on_outlined,
                      size: 14,
                      color: theme.colorScheme.onSurface.withValues(alpha: 0.5),
                    ),
                    const SizedBox(width: 2),
                    Expanded(
                      child: Text(
                        entry.storagePlaceName!,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color:
                              theme.colorScheme.onSurface.withValues(alpha: 0.6),
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ],

              // Expiry countdown
              if (entry.expiresAt != null) ...[
                const SizedBox(height: 6),
                _ExpiryBadge(daysUntilExpiry: entry.daysUntilExpiry),
              ],
            ],
          ),
        ),
      ),
    );
  }

  String _formatQuantity(double q) {
    return q == q.roundToDouble() ? q.toInt().toString() : q.toStringAsFixed(1);
  }
}

class _ExpiryBadge extends StatelessWidget {
  const _ExpiryBadge({required this.daysUntilExpiry});

  final int? daysUntilExpiry;

  @override
  Widget build(BuildContext context) {
    final days = daysUntilExpiry;
    if (days == null) return const SizedBox.shrink();

    final Color color;
    final String label;

    if (days < 0) {
      color = Colors.red.shade700;
      label = 'Expired ${(-days)} day${(-days) == 1 ? '' : 's'} ago';
    } else if (days == 0) {
      color = Colors.red.shade700;
      label = 'Expires today';
    } else if (days <= 3) {
      color = Colors.red.shade600;
      label = 'Expires in $days day${days == 1 ? '' : 's'}';
    } else if (days <= 7) {
      color = Colors.amber.shade700;
      label = 'Expires in $days days';
    } else {
      color = Colors.green.shade700;
      label = 'Expires in $days days';
    }

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(Icons.schedule, size: 13, color: color),
        const SizedBox(width: 3),
        Text(
          label,
          style: Theme.of(context).textTheme.labelSmall?.copyWith(color: color),
        ),
      ],
    );
  }
}
