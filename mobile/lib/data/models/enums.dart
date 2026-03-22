/// Domain enums used across models and UI layers.
/// String values match the backend's serialized form (uppercase).

enum StockStatus {
  available,
  expired,
  discarded;

  static StockStatus fromString(String value) {
    switch (value.toUpperCase()) {
      case 'EXPIRED':
        return StockStatus.expired;
      case 'DISCARDED':
        return StockStatus.discarded;
      default:
        return StockStatus.available;
    }
  }

  String toApiString() {
    switch (this) {
      case StockStatus.available:
        return 'AVAILABLE';
      case StockStatus.expired:
        return 'EXPIRED';
      case StockStatus.discarded:
        return 'DISCARDED';
    }
  }
}

enum ShoppingItemStatus {
  active,
  completed;

  static ShoppingItemStatus fromString(String value) {
    switch (value.toUpperCase()) {
      case 'COMPLETED':
        return ShoppingItemStatus.completed;
      default:
        return ShoppingItemStatus.active;
    }
  }

  String toApiString() {
    switch (this) {
      case ShoppingItemStatus.active:
        return 'ACTIVE';
      case ShoppingItemStatus.completed:
        return 'COMPLETED';
    }
  }
}

enum ShoppingItemSource {
  manual,
  autoReplenishment,
  googleKeep;

  static ShoppingItemSource fromString(String value) {
    switch (value.toUpperCase()) {
      case 'AUTO_REPLENISHMENT':
        return ShoppingItemSource.autoReplenishment;
      case 'GOOGLE_KEEP':
        return ShoppingItemSource.googleKeep;
      default:
        return ShoppingItemSource.manual;
    }
  }

  String toApiString() {
    switch (this) {
      case ShoppingItemSource.manual:
        return 'MANUAL';
      case ShoppingItemSource.autoReplenishment:
        return 'AUTO_REPLENISHMENT';
      case ShoppingItemSource.googleKeep:
        return 'GOOGLE_KEEP';
    }
  }
}

enum UnitType {
  count,
  weight,
  volume,
  packaging;

  static UnitType fromString(String value) {
    switch (value.toUpperCase()) {
      case 'WEIGHT':
        return UnitType.weight;
      case 'VOLUME':
        return UnitType.volume;
      case 'PACKAGING':
        return UnitType.packaging;
      default:
        return UnitType.count;
    }
  }

  String toApiString() {
    switch (this) {
      case UnitType.count:
        return 'COUNT';
      case UnitType.weight:
        return 'WEIGHT';
      case UnitType.volume:
        return 'VOLUME';
      case UnitType.packaging:
        return 'PACKAGING';
    }
  }
}
