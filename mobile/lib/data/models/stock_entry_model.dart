class StockEntryModel {
  const StockEntryModel({
    required this.id,
    required this.productName,
    required this.quantity,
    required this.storagePlace,
    required this.status,
  });

  final int id;
  final String productName;
  final double quantity;
  final String? storagePlace;
  final String status;

  factory StockEntryModel.fromJson(Map<String, dynamic> json) {
    return StockEntryModel(
      id: json['id'] as int,
      productName: json['productName'] as String? ?? 'Unknown',
      quantity: (json['quantity'] as num?)?.toDouble() ?? 0,
      storagePlace: json['storagePlace']?.toString(),
      status: json['status'] as String? ?? 'AVAILABLE',
    );
  }

  factory StockEntryModel.fromDb(Map<String, dynamic> row) {
    return StockEntryModel(
      id: row['id'] as int,
      productName: row['product_name'] as String,
      quantity: (row['quantity'] as num).toDouble(),
      storagePlace: row['storage_place'] as String?,
      status: row['status'] as String,
    );
  }

  Map<String, dynamic> toDb() => {
        'id': id,
        'product_name': productName,
        'quantity': quantity,
        'storage_place': storagePlace,
        'status': status,
        'updated_at': DateTime.now().toIso8601String(),
      };
}
