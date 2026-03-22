import 'package:flutter/material.dart';

/// Simple two-button confirmation dialog.
///
/// Usage:
/// ```dart
/// final confirmed = await ConfirmDialog.show(
///   context,
///   title: 'Delete item',
///   message: 'This action cannot be undone.',
/// );
/// if (confirmed) { /* proceed */ }
/// ```
class ConfirmDialog {
  ConfirmDialog._();

  static Future<bool> show(
    BuildContext context, {
    required String title,
    required String message,
    String cancelLabel = 'Cancel',
    String confirmLabel = 'Confirm',
    bool destructive = false,
  }) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: Text(cancelLabel),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: destructive
                ? TextButton.styleFrom(
                    foregroundColor: Theme.of(ctx).colorScheme.error,
                  )
                : null,
            child: Text(confirmLabel),
          ),
        ],
      ),
    );
    return result ?? false;
  }
}
