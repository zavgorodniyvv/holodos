class ShoppingItemModel {
  const ShoppingItemModel({
    required this.id,
    this.productId,
    required this.title,
    required this.quantity,
    this.unitId,
    this.storeId,
    required this.status,
    this.source,
    this.comment,
    required this.sortOrder,
    this.createdAt,
    this.completedAt,
    this.updatedAt,
    this.version,
  });

  final int id;
  final int? productId;
  final String title;
  final double quantity;
  final int? unitId;
  final int? storeId;

  /// Raw status string from backend: ACTIVE, COMPLETED
  final String status;

  /// Raw source string from backend: MANUAL, AUTO_REPLENISHMENT, GOOGLE_KEEP
  final String? source;

  final String? comment;
  final int sortOrder;
  final String? createdAt;
  final String? completedAt;
  final String? updatedAt;
  final int? version;

  bool get isCompleted => status.toUpperCase() == 'COMPLETED';

  factory ShoppingItemModel.fromJson(Map<String, dynamic> json) {
    return ShoppingItemModel(
      id: json['id'] as int,
      productId: json['productId'] as int?,
      title: json['title'] as String? ?? '',
      quantity: (json['quantity'] as num?)?.toDouble() ?? 1,
      unitId: json['unitId'] as int?,
      storeId: json['storeId'] as int?,
      status: json['status'] as String? ?? 'ACTIVE',
      source: json['source'] as String?,
      comment: json['comment'] as String?,
      sortOrder: (json['sortOrder'] as int?) ?? 0,
      createdAt: json['createdAt'] as String?,
      completedAt: json['completedAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      version: json['version'] as int?,
    );
  }

  factory ShoppingItemModel.fromDb(Map<String, dynamic> row) {
    return ShoppingItemModel(
      id: row['id'] as int,
      productId: row['product_id'] as int?,
      title: row['title'] as String? ?? '',
      quantity: (row['quantity'] as num).toDouble(),
      unitId: row['unit_id'] as int?,
      storeId: row['store_id'] as int?,
      status: row['status'] as String? ?? 'ACTIVE',
      source: row['source'] as String?,
      comment: row['comment'] as String?,
      sortOrder: (row['sort_order'] as int?) ?? 0,
      createdAt: row['created_at'] as String?,
      completedAt: row['completed_at'] as String?,
      updatedAt: row['updated_at'] as String?,
      version: row['version'] as int?,
    );
  }

  Map<String, dynamic> toDb() => {
        'id': id,
        'product_id': productId,
        'title': title,
        'quantity': quantity,
        'unit_id': unitId,
        'store_id': storeId,
        'status': status,
        'source': source,
        'comment': comment,
        'sort_order': sortOrder,
        'created_at': createdAt,
        'completed_at': completedAt,
        'updated_at': updatedAt ?? DateTime.now().toIso8601String(),
        'version': version,
      };
}
