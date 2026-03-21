import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/dashboard/dashboard_screen.dart';
import '../features/inventory/inventory_screen.dart';
import '../features/shopping/shopping_list_screen.dart';

final navigationIndexProvider = StateProvider<int>((ref) => 0);

final appRouterProvider = Provider<Widget>((ref) {
  final index = ref.watch(navigationIndexProvider);
  return _HolodosShell(currentIndex: index);
});

class _HolodosShell extends ConsumerWidget {
  const _HolodosShell({required this.currentIndex});

  final int currentIndex;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pages = const [
      DashboardScreen(),
      InventoryScreen(),
      ShoppingListScreen()
    ];
    return Scaffold(
      body: pages[currentIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: currentIndex,
        destinations: const [
          NavigationDestination(
              icon: Icon(Icons.dashboard_outlined), label: 'Dashboard'),
          NavigationDestination(
              icon: Icon(Icons.inventory_2_outlined), label: 'Inventory'),
          NavigationDestination(
              icon: Icon(Icons.shopping_cart_outlined), label: 'Shopping'),
        ],
        onDestinationSelected: (idx) =>
            ref.read(navigationIndexProvider.notifier).state = idx,
      ),
    );
  }
}
