import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models/dictionary_model.dart';
import '../../data/models/product_model.dart';
import '../../data/models/unit_model.dart';

// ---------------------------------------------------------------------------
// Products
// ---------------------------------------------------------------------------

final productsListProvider =
    FutureProvider<List<ProductModel>>((ref) async {
  final repo = ref.watch(catalogRepositoryProvider);
  final cached = await repo.cachedProducts();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshProducts());
    return cached;
  }
  return repo.refreshProducts();
});

// ---------------------------------------------------------------------------
// Storage Places
// ---------------------------------------------------------------------------

final storagePlacesListProvider =
    FutureProvider<List<DictionaryModel>>((ref) async {
  final repo = ref.watch(catalogRepositoryProvider);
  final cached = await repo.cachedStoragePlaces();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshStoragePlaces());
    return cached;
  }
  return repo.refreshStoragePlaces();
});

// ---------------------------------------------------------------------------
// Categories
// ---------------------------------------------------------------------------

final categoriesListProvider =
    FutureProvider<List<DictionaryModel>>((ref) async {
  final repo = ref.watch(catalogRepositoryProvider);
  final cached = await repo.cachedCategories();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshCategories());
    return cached;
  }
  return repo.refreshCategories();
});

// ---------------------------------------------------------------------------
// Stores
// ---------------------------------------------------------------------------

final storesListProvider =
    FutureProvider<List<DictionaryModel>>((ref) async {
  final repo = ref.watch(catalogRepositoryProvider);
  final cached = await repo.cachedStores();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshStores());
    return cached;
  }
  return repo.refreshStores();
});

// ---------------------------------------------------------------------------
// Units
// ---------------------------------------------------------------------------

final unitsListProvider =
    FutureProvider<List<UnitModel>>((ref) async {
  final repo = ref.watch(catalogRepositoryProvider);
  final cached = await repo.cachedUnits();
  if (cached.isNotEmpty) {
    unawaited(repo.refreshUnits());
    return cached;
  }
  return repo.refreshUnits();
});
