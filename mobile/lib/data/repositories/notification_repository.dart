import '../api/holodos_api_client.dart';
import '../models/notification_model.dart';

/// Notifications are not cached locally — they are always fetched fresh.
class NotificationRepository {
  NotificationRepository({required this.apiClient});

  final HolodosApiClient apiClient;

  Future<List<NotificationModel>> fetchAll() =>
      apiClient.fetchNotifications();

  Future<void> markRead(int id) => apiClient.markNotificationRead(id);
}
