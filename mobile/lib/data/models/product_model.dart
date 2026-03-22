class ProductModel {
  const ProductModel({
    required this.id,
    required this.name,
    required this.categoryId,
    this.categoryName,
    required this.defaultUnitId,
    this.defaultUnitName,
    required this.defaultStoragePlaceId,
    this.defaultStoragePlaceName,
    this.defaultStoreId,
    this.defaultStoreName,
    this.photoKey,
    this.description,
    this.shelfLifeDays,
    this.minimumQuantityThreshold,
    this.reorderQuantity,
    required this.autoAddShopping,
    this.barcode,
    this.note,
    required this.active,
    this.createdAt,
    this.updatedAt,
    this.version,
  });

  final int id;
  final String name;
  final int categoryId;
  final String? categoryName;
  final int defaultUnitId;
  final String? defaultUnitName;
  final int defaultStoragePlaceId;
  final String? defaultStoragePlaceName;
  final int? defaultStoreId;
  final String? defaultStoreName;
  final String? photoKey;
  final String? description;
  final int? shelfLifeDays;
  final double? minimumQuantityThreshold;
  final double? reorderQuantity;
  final bool autoAddShopping;
  final String? barcode;
  final String? note;
  final bool active;
  final String? createdAt;
  final String? updatedAt;
  final int? version;

  factory ProductModel.fromJson(Map<String, dynamic> json) {
    return ProductModel(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      categoryId: (json['categoryId'] as int?) ?? 0,
      categoryName: json['categoryName'] as String?,
      defaultUnitId: (json['defaultUnitId'] as int?) ?? 0,
      defaultUnitName: json['defaultUnitName'] as String?,
      defaultStoragePlaceId: (json['defaultStoragePlaceId'] as int?) ?? 0,
      defaultStoragePlaceName: json['defaultStoragePlaceName'] as String?,
      defaultStoreId: json['defaultStoreId'] as int?,
      defaultStoreName: json['defaultStoreName'] as String?,
      photoKey: json['photoKey'] as String?,
      description: json['description'] as String?,
      shelfLifeDays: json['shelfLifeDays'] as int?,
      minimumQuantityThreshold:
          (json['minimumQuantityThreshold'] as num?)?.toDouble(),
      reorderQuantity: (json['reorderQuantity'] as num?)?.toDouble(),
      autoAddShopping: (json['autoAddShopping'] as bool?) ?? false,
      barcode: json['barcode'] as String?,
      note: json['note'] as String?,
      active: (json['active'] as bool?) ?? true,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      version: json['version'] as int?,
    );
  }

  factory ProductModel.fromDb(Map<String, dynamic> row) {
    return ProductModel(
      id: row['id'] as int,
      name: row['name'] as String? ?? '',
      categoryId: (row['category_id'] as int?) ?? 0,
      categoryName: row['category_name'] as String?,
      defaultUnitId: (row['default_unit_id'] as int?) ?? 0,
      defaultUnitName: row['default_unit_name'] as String?,
      defaultStoragePlaceId: (row['default_storage_place_id'] as int?) ?? 0,
      defaultStoragePlaceName: row['default_storage_place_name'] as String?,
      defaultStoreId: row['default_store_id'] as int?,
      defaultStoreName: row['default_store_name'] as String?,
      photoKey: row['photo_key'] as String?,
      description: row['description'] as String?,
      shelfLifeDays: row['shelf_life_days'] as int?,
      minimumQuantityThreshold:
          (row['minimum_quantity_threshold'] as num?)?.toDouble(),
      reorderQuantity: (row['reorder_quantity'] as num?)?.toDouble(),
      autoAddShopping: (row['auto_add_shopping'] as int?) == 1,
      barcode: row['barcode'] as String?,
      note: row['note'] as String?,
      active: (row['active'] as int?) == 1,
      createdAt: row['created_at'] as String?,
      updatedAt: row['updated_at'] as String?,
      version: row['version'] as int?,
    );
  }

  Map<String, dynamic> toDb() => {
        'id': id,
        'name': name,
        'category_id': categoryId,
        'category_name': categoryName,
        'default_unit_id': defaultUnitId,
        'default_unit_name': defaultUnitName,
        'default_storage_place_id': defaultStoragePlaceId,
        'default_storage_place_name': defaultStoragePlaceName,
        'default_store_id': defaultStoreId,
        'default_store_name': defaultStoreName,
        'photo_key': photoKey,
        'description': description,
        'shelf_life_days': shelfLifeDays,
        'minimum_quantity_threshold': minimumQuantityThreshold,
        'reorder_quantity': reorderQuantity,
        'auto_add_shopping': autoAddShopping ? 1 : 0,
        'barcode': barcode,
        'note': note,
        'active': active ? 1 : 0,
        'created_at': createdAt,
        'updated_at': updatedAt ?? DateTime.now().toIso8601String(),
        'version': version,
      };
}
