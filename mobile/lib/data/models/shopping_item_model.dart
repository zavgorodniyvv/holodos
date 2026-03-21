class ShoppingItemModel {
  const ShoppingItemModel({
    required this.id,
    required this.title,
    required this.quantity,
    required this.status,
  });

  final int id;
  final String title;
  final double quantity;
  final String status;

  factory ShoppingItemModel.fromJson(Map<String, dynamic> json) {
    return ShoppingItemModel(
      id: json['id'] as int,
      title: json['title'] as String,
      quantity: (json['quantity'] as num?)?.toDouble() ?? 0,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  factory ShoppingItemModel.fromDb(Map<String, dynamic> row) {
    return ShoppingItemModel(
      id: row['id'] as int,
      title: row['title'] as String,
      quantity: (row['quantity'] as num).toDouble(),
      status: row['status'] as String,
    );
  }

  Map<String, dynamic> toDb() => {
        'id': id,
        'title': title,
        'quantity': quantity,
        'status': status,
        'updated_at': DateTime.now().toIso8601String(),
      };
}
