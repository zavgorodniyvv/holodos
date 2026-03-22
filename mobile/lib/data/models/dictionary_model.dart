/// Generic model for catalog dictionaries: StoragePlace, Category, Store.
/// All three share the same shape from the backend.
class DictionaryModel {
  const DictionaryModel({
    required this.id,
    required this.name,
    this.description,
    this.icon,
    this.color,
    required this.sortOrder,
    required this.active,
    this.createdAt,
    this.updatedAt,
    this.version,
  });

  final int id;
  final String name;
  final String? description;
  final String? icon;
  final String? color;
  final int sortOrder;
  final bool active;
  final String? createdAt;
  final String? updatedAt;
  final int? version;

  factory DictionaryModel.fromJson(Map<String, dynamic> json) {
    return DictionaryModel(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      icon: json['icon'] as String?,
      color: json['color'] as String?,
      sortOrder: (json['sortOrder'] as int?) ?? 0,
      active: (json['active'] as bool?) ?? true,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      version: json['version'] as int?,
    );
  }

  factory DictionaryModel.fromDb(Map<String, dynamic> row) {
    return DictionaryModel(
      id: row['id'] as int,
      name: row['name'] as String? ?? '',
      description: row['description'] as String?,
      icon: row['icon'] as String?,
      color: row['color'] as String?,
      sortOrder: (row['sort_order'] as int?) ?? 0,
      active: (row['active'] as int?) == 1,
      createdAt: row['created_at'] as String?,
      updatedAt: row['updated_at'] as String?,
      version: row['version'] as int?,
    );
  }

  Map<String, dynamic> toDb(String tableName) => {
        'id': id,
        'name': name,
        'description': description,
        'icon': icon,
        'color': color,
        'sort_order': sortOrder,
        'active': active ? 1 : 0,
        'created_at': createdAt,
        'updated_at': updatedAt ?? DateTime.now().toIso8601String(),
        'version': version,
      };
}
