class UnitModel {
  const UnitModel({
    required this.id,
    required this.code,
    required this.name,
    required this.shortName,
    required this.unitType,
    required this.active,
    this.createdAt,
    this.updatedAt,
    this.version,
  });

  final int id;
  final String code;
  final String name;
  final String shortName;

  /// Raw unit type string from backend: COUNT, WEIGHT, VOLUME, PACKAGING
  final String unitType;

  final bool active;
  final String? createdAt;
  final String? updatedAt;
  final int? version;

  factory UnitModel.fromJson(Map<String, dynamic> json) {
    return UnitModel(
      id: json['id'] as int,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      shortName: json['shortName'] as String? ?? '',
      unitType: json['unitType'] as String? ?? 'COUNT',
      active: (json['active'] as bool?) ?? true,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      version: json['version'] as int?,
    );
  }

  factory UnitModel.fromDb(Map<String, dynamic> row) {
    return UnitModel(
      id: row['id'] as int,
      code: row['code'] as String? ?? '',
      name: row['name'] as String? ?? '',
      shortName: row['short_name'] as String? ?? '',
      unitType: row['unit_type'] as String? ?? 'COUNT',
      active: (row['active'] as int?) == 1,
      createdAt: row['created_at'] as String?,
      updatedAt: row['updated_at'] as String?,
      version: row['version'] as int?,
    );
  }

  Map<String, dynamic> toDb() => {
        'id': id,
        'code': code,
        'name': name,
        'short_name': shortName,
        'unit_type': unitType,
        'active': active ? 1 : 0,
        'created_at': createdAt,
        'updated_at': updatedAt ?? DateTime.now().toIso8601String(),
        'version': version,
      };
}
