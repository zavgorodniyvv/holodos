class SettingsModel {
  const SettingsModel({
    required this.userKey,
    required this.expiryDaysBeforeNotify,
    required this.notifyExpiring,
    required this.notifyExpired,
    required this.notifyOldItems,
    required this.notifyOutOfStock,
    this.quietHoursStart,
    this.quietHoursEnd,
    required this.maxFrequencyMinutes,
  });

  final String userKey;
  final int expiryDaysBeforeNotify;
  final bool notifyExpiring;
  final bool notifyExpired;
  final bool notifyOldItems;
  final bool notifyOutOfStock;
  final String? quietHoursStart;
  final String? quietHoursEnd;
  final int maxFrequencyMinutes;

  factory SettingsModel.fromJson(Map<String, dynamic> json) {
    return SettingsModel(
      userKey: json['userKey'] as String? ?? 'default',
      expiryDaysBeforeNotify: (json['expiryDaysBeforeNotify'] as int?) ?? 3,
      notifyExpiring: (json['notifyExpiring'] as bool?) ?? true,
      notifyExpired: (json['notifyExpired'] as bool?) ?? true,
      notifyOldItems: (json['notifyOldItems'] as bool?) ?? false,
      notifyOutOfStock: (json['notifyOutOfStock'] as bool?) ?? true,
      quietHoursStart: json['quietHoursStart'] as String?,
      quietHoursEnd: json['quietHoursEnd'] as String?,
      maxFrequencyMinutes: (json['maxFrequencyMinutes'] as int?) ?? 60,
    );
  }

  Map<String, dynamic> toJson() => {
        'userKey': userKey,
        'expiryDaysBeforeNotify': expiryDaysBeforeNotify,
        'notifyExpiring': notifyExpiring,
        'notifyExpired': notifyExpired,
        'notifyOldItems': notifyOldItems,
        'notifyOutOfStock': notifyOutOfStock,
        'quietHoursStart': quietHoursStart,
        'quietHoursEnd': quietHoursEnd,
        'maxFrequencyMinutes': maxFrequencyMinutes,
      };

  SettingsModel copyWith({
    String? userKey,
    int? expiryDaysBeforeNotify,
    bool? notifyExpiring,
    bool? notifyExpired,
    bool? notifyOldItems,
    bool? notifyOutOfStock,
    String? quietHoursStart,
    String? quietHoursEnd,
    int? maxFrequencyMinutes,
  }) {
    return SettingsModel(
      userKey: userKey ?? this.userKey,
      expiryDaysBeforeNotify:
          expiryDaysBeforeNotify ?? this.expiryDaysBeforeNotify,
      notifyExpiring: notifyExpiring ?? this.notifyExpiring,
      notifyExpired: notifyExpired ?? this.notifyExpired,
      notifyOldItems: notifyOldItems ?? this.notifyOldItems,
      notifyOutOfStock: notifyOutOfStock ?? this.notifyOutOfStock,
      quietHoursStart: quietHoursStart ?? this.quietHoursStart,
      quietHoursEnd: quietHoursEnd ?? this.quietHoursEnd,
      maxFrequencyMinutes: maxFrequencyMinutes ?? this.maxFrequencyMinutes,
    );
  }
}
