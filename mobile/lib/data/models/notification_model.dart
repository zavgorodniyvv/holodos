class NotificationModel {
  const NotificationModel({
    required this.id,
    required this.type,
    required this.title,
    this.message,
    this.entityType,
    this.entityId,
    required this.status,
    this.createdAt,
    this.readAt,
  });

  final int id;
  final String type;
  final String title;
  final String? message;
  final String? entityType;
  final String? entityId;

  /// Raw status string from backend: UNREAD, READ
  final String status;

  final String? createdAt;
  final String? readAt;

  bool get isUnread => status.toUpperCase() == 'UNREAD';

  factory NotificationModel.fromJson(Map<String, dynamic> json) {
    return NotificationModel(
      id: json['id'] as int,
      type: json['type'] as String? ?? '',
      title: json['title'] as String? ?? '',
      message: json['message'] as String?,
      entityType: json['entityType'] as String?,
      entityId: json['entityId']?.toString(),
      status: json['status'] as String? ?? 'UNREAD',
      createdAt: json['createdAt'] as String?,
      readAt: json['readAt'] as String?,
    );
  }
}
