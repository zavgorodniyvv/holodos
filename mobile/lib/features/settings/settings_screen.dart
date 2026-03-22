import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/settings_model.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  SettingsModel? _settings;
  bool _loading = true;
  bool _saving = false;
  String? _errorMessage;

  // Local mutable state mirrors _settings for form editing.
  bool _notifyExpiring = true;
  bool _notifyExpired = true;
  bool _notifyOldItems = false;
  bool _notifyOutOfStock = true;
  int _expiryDays = 3;
  int _maxFrequencyMinutes = 60;
  TimeOfDay? _quietStart;
  TimeOfDay? _quietEnd;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
    });
    try {
      final settings =
          await ref.read(settingsRepositoryProvider).fetch();
      _applySettings(settings);
    } catch (e) {
      if (mounted) {
        setState(() => _errorMessage = 'Failed to load settings: $e');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _applySettings(SettingsModel s) {
    setState(() {
      _settings = s;
      _notifyExpiring = s.notifyExpiring;
      _notifyExpired = s.notifyExpired;
      _notifyOldItems = s.notifyOldItems;
      _notifyOutOfStock = s.notifyOutOfStock;
      _expiryDays = s.expiryDaysBeforeNotify;
      _maxFrequencyMinutes = s.maxFrequencyMinutes;
      _quietStart = _parseTime(s.quietHoursStart);
      _quietEnd = _parseTime(s.quietHoursEnd);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        actions: [
          _saving
              ? const Padding(
                  padding: EdgeInsets.all(12),
                  child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2)))
              : TextButton(
                  onPressed: _settings != null ? _save : null,
                  child: const Text('Save'),
                ),
        ],
      ),
      body: _buildBody(context),
    );
  }

  Widget _buildBody(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_errorMessage!),
            const SizedBox(height: 16),
            FilledButton(
                onPressed: _loadSettings,
                child: const Text('Retry')),
          ],
        ),
      );
    }

    return ListView(
      children: [
        // ---------------------------------------------------------------
        // Notifications
        // ---------------------------------------------------------------
        _SectionHeader(label: 'Notifications'),
        SwitchListTile(
          title: const Text('Notify expiring items'),
          subtitle:
              const Text('Alert when items are close to expiry date'),
          value: _notifyExpiring,
          onChanged: (v) => setState(() => _notifyExpiring = v),
        ),
        SwitchListTile(
          title: const Text('Notify expired items'),
          subtitle: const Text('Alert when items have already expired'),
          value: _notifyExpired,
          onChanged: (v) => setState(() => _notifyExpired = v),
        ),
        SwitchListTile(
          title: const Text('Notify old items'),
          subtitle: const Text(
              'Alert when items have been in stock for a long time'),
          value: _notifyOldItems,
          onChanged: (v) => setState(() => _notifyOldItems = v),
        ),
        SwitchListTile(
          title: const Text('Notify out of stock'),
          subtitle:
              const Text('Alert when quantity falls below threshold'),
          value: _notifyOutOfStock,
          onChanged: (v) => setState(() => _notifyOutOfStock = v),
        ),

        // ---------------------------------------------------------------
        // Expiry alert threshold
        // ---------------------------------------------------------------
        _SectionHeader(label: 'Expiry Alert'),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
          child: _NumberStepperField(
            label: 'Days before expiry to notify',
            value: _expiryDays,
            min: 1,
            max: 30,
            onChanged: (v) => setState(() => _expiryDays = v),
          ),
        ),

        // ---------------------------------------------------------------
        // Quiet hours
        // ---------------------------------------------------------------
        _SectionHeader(label: 'Quiet Hours'),
        ListTile(
          leading: const Icon(Icons.bedtime_outlined),
          title: const Text('Start'),
          subtitle: Text(_quietStart != null
              ? _quietStart!.format(context)
              : 'Not set'),
          trailing: _quietStart != null
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 18),
                  onPressed: () =>
                      setState(() => _quietStart = null),
                )
              : const Icon(Icons.chevron_right),
          onTap: () async {
            final picked = await showTimePicker(
              context: context,
              initialTime: _quietStart ?? const TimeOfDay(hour: 22, minute: 0),
            );
            if (picked != null) {
              setState(() => _quietStart = picked);
            }
          },
        ),
        ListTile(
          leading: const Icon(Icons.wb_sunny_outlined),
          title: const Text('End'),
          subtitle: Text(
              _quietEnd != null ? _quietEnd!.format(context) : 'Not set'),
          trailing: _quietEnd != null
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 18),
                  onPressed: () => setState(() => _quietEnd = null),
                )
              : const Icon(Icons.chevron_right),
          onTap: () async {
            final picked = await showTimePicker(
              context: context,
              initialTime: _quietEnd ?? const TimeOfDay(hour: 8, minute: 0),
            );
            if (picked != null) {
              setState(() => _quietEnd = picked);
            }
          },
        ),

        // ---------------------------------------------------------------
        // Frequency
        // ---------------------------------------------------------------
        _SectionHeader(label: 'Frequency'),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
          child: _NumberStepperField(
            label: 'Max notification frequency (minutes)',
            value: _maxFrequencyMinutes,
            min: 5,
            max: 1440,
            step: 15,
            onChanged: (v) => setState(() => _maxFrequencyMinutes = v),
          ),
        ),
      ],
    );
  }

  Future<void> _save() async {
    if (_settings == null) return;
    setState(() => _saving = true);
    try {
      final updated = _settings!.copyWith(
        notifyExpiring: _notifyExpiring,
        notifyExpired: _notifyExpired,
        notifyOldItems: _notifyOldItems,
        notifyOutOfStock: _notifyOutOfStock,
        expiryDaysBeforeNotify: _expiryDays,
        maxFrequencyMinutes: _maxFrequencyMinutes,
        quietHoursStart:
            _quietStart != null ? _formatTime(_quietStart!) : null,
        quietHoursEnd:
            _quietEnd != null ? _formatTime(_quietEnd!) : null,
      );
      final saved =
          await ref.read(settingsRepositoryProvider).save(updated);
      _applySettings(saved);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Settings saved')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to save: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  TimeOfDay? _parseTime(String? s) {
    if (s == null || s.isEmpty) return null;
    final parts = s.split(':');
    if (parts.length < 2) return null;
    final h = int.tryParse(parts[0]);
    final m = int.tryParse(parts[1]);
    if (h == null || m == null) return null;
    return TimeOfDay(hour: h, minute: m);
  }

  String _formatTime(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
      child: Text(
        label,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          fontWeight: FontWeight.w700,
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Number stepper field
// ---------------------------------------------------------------------------

class _NumberStepperField extends StatelessWidget {
  const _NumberStepperField({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.onChanged,
    this.step = 1,
  });

  final String label;
  final int value;
  final int min;
  final int max;
  final int step;
  final void Function(int) onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context)
                  .colorScheme
                  .onSurface
                  .withValues(alpha: 0.7),
            )),
        const SizedBox(height: 8),
        Row(
          children: [
            IconButton(
              icon: const Icon(Icons.remove_circle_outline),
              onPressed:
                  value > min ? () => onChanged(value - step) : null,
            ),
            Expanded(
              child: Center(
                child: Text(
                  '$value',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.add_circle_outline),
              onPressed:
                  value < max ? () => onChanged(value + step) : null,
            ),
          ],
        ),
      ],
    );
  }
}
