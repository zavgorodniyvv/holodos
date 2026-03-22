import 'package:flutter/material.dart';

/// Small colour-coded chip representing a stock or shopping item status.
///
/// Colour mapping:
///   AVAILABLE / ACTIVE  → green
///   EXPIRED / DISCARDED → red
///   COMPLETED           → grey
class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final upper = status.toUpperCase();

    final Color backgroundColor;
    final Color foregroundColor;
    final String label;

    switch (upper) {
      case 'AVAILABLE':
        backgroundColor = Colors.green.shade100;
        foregroundColor = Colors.green.shade800;
        label = 'Available';
      case 'ACTIVE':
        backgroundColor = Colors.green.shade100;
        foregroundColor = Colors.green.shade800;
        label = 'Active';
      case 'EXPIRED':
        backgroundColor = Colors.red.shade100;
        foregroundColor = Colors.red.shade800;
        label = 'Expired';
      case 'DISCARDED':
        backgroundColor = Colors.red.shade100;
        foregroundColor = Colors.red.shade800;
        label = 'Discarded';
      case 'COMPLETED':
        backgroundColor = theme.colorScheme.surfaceContainerHighest;
        foregroundColor =
            theme.colorScheme.onSurface.withValues(alpha: 0.6);
        label = 'Completed';
      default:
        backgroundColor = theme.colorScheme.surfaceContainerHighest;
        foregroundColor = theme.colorScheme.onSurface;
        label = status;
    }

    return Chip(
      label: Text(
        label,
        style: theme.textTheme.labelSmall?.copyWith(
          color: foregroundColor,
          fontWeight: FontWeight.w600,
        ),
      ),
      backgroundColor: backgroundColor,
      side: BorderSide.none,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      visualDensity: VisualDensity.compact,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }
}
