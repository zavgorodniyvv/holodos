class StockEntryModel {
  const StockEntryModel({
    required this.id,
    required this.productId,
    this.productName,
    required this.quantity,
    required this.unitId,
    this.unitName,
    required this.storagePlaceId,
    this.storagePlaceName,
    this.addedAt,
    this.purchasedAt,
    this.expiresAt,
    this.comment,
    required this.status,
    this.createdAt,
    this.updatedAt,
    this.version,
  });

  final int id;
  final int productId;
  final String? productName;
  final double quantity;
  final int unitId;
  final String? unitName;
  final int storagePlaceId;
  final String? storagePlaceName;
  final String? addedAt;
  final String? purchasedAt;
  final String? expiresAt;
  final String? comment;

  /// Raw status string from backend: AVAILABLE, EXPIRED, DISCARDED
  final String status;

  final String? createdAt;
  final String? updatedAt;
  final int? version;

  factory StockEntryModel.fromJson(Map<String, dynamic> json) {
    return StockEntryModel(
      id: json['id'] as int,
      productId: (json['productId'] as int?) ?? 0,
      productName: json['productName'] as String?,
      quantity: (json['quantity'] as num?)?.toDouble() ?? 0,
      unitId: (json['unitId'] as int?) ?? 0,
      unitName: json['unitName'] as String?,
      storagePlaceId: (json['storagePlaceId'] as int?) ?? 0,
      storagePlaceName: json['storagePlaceName'] as String?,
      addedAt: json['addedAt'] as String?,
      purchasedAt: json['purchasedAt'] as String?,
      expiresAt: json['expiresAt'] as String?,
      comment: json['comment'] as String?,
      status: json['status'] as String? ?? 'AVAILABLE',
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      version: json['version'] as int?,
    );
  }

  factory StockEntryModel.fromDb(Map<String, dynamic> row) {
    return StockEntryModel(
      id: row['id'] as int,
      productId: (row['product_id'] as int?) ?? 0,
      productName: row['product_name'] as String?,
      quantity: (row['quantity'] as num).toDouble(),
      unitId: (row['unit_id'] as int?) ?? 0,
      unitName: row['unit_name'] as String?,
      storagePlaceId: (row['storage_place_id'] as int?) ?? 0,
      storagePlaceName: row['storage_place_name'] as String?,
      addedAt: row['added_at'] as String?,
      purchasedAt: row['purchased_at'] as String?,
      expiresAt: row['expires_at'] as String?,
      comment: row['comment'] as String?,
      status: row['status'] as String? ?? 'AVAILABLE',
      createdAt: row['created_at'] as String?,
      updatedAt: row['updated_at'] as String?,
      version: row['version'] as int?,
    );
  }

  Map<String, dynamic> toDb() => {
        'id': id,
        'product_id': productId,
        'product_name': productName,
        'quantity': quantity,
        'unit_id': unitId,
        'unit_name': unitName,
        'storage_place_id': storagePlaceId,
        'storage_place_name': storagePlaceName,
        'added_at': addedAt,
        'purchased_at': purchasedAt,
        'expires_at': expiresAt,
        'comment': comment,
        'status': status,
        'created_at': createdAt,
        'updated_at': updatedAt ?? DateTime.now().toIso8601String(),
        'version': version,
      };

  /// Returns days until expiry. Negative means already expired.
  /// Returns null when no expiry date is set.
  int? get daysUntilExpiry {
    if (expiresAt == null) return null;
    final expiry = DateTime.tryParse(expiresAt!);
    if (expiry == null) return null;
    return expiry.difference(DateTime.now()).inDays;
  }
}
