import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/catalog/catalog_screen.dart';
import '../features/dashboard/dashboard_screen.dart';
import '../features/inventory/inventory_screen.dart';
import '../features/settings/settings_screen.dart';
import '../features/shopping/shopping_list_screen.dart';

final navigationIndexProvider = StateProvider<int>((ref) => 0);

final appRouterProvider = Provider<Widget>((ref) {
  final index = ref.watch(navigationIndexProvider);
  return _HolodosShell(currentIndex: index);
});

class _HolodosShell extends ConsumerWidget {
  const _HolodosShell({required this.currentIndex});

  final int currentIndex;

  static const _pages = [
    DashboardScreen(),
    InventoryScreen(),
    ShoppingListScreen(),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      drawer: _AppDrawer(),
      body: _pages[currentIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: currentIndex,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            selectedIcon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          NavigationDestination(
            icon: Icon(Icons.inventory_2_outlined),
            selectedIcon: Icon(Icons.inventory_2),
            label: 'Inventory',
          ),
          NavigationDestination(
            icon: Icon(Icons.shopping_cart_outlined),
            selectedIcon: Icon(Icons.shopping_cart),
            label: 'Shopping',
          ),
        ],
        onDestinationSelected: (idx) =>
            ref.read(navigationIndexProvider.notifier).state = idx,
      ),
    );
  }
}

class _AppDrawer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Drawer(
      child: Column(
        children: [
          DrawerHeader(
            decoration: BoxDecoration(
              color: theme.colorScheme.primaryContainer,
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(
                  Icons.kitchen_outlined,
                  size: 40,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
                const SizedBox(width: 12),
                Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Holodos',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: theme.colorScheme.onPrimaryContainer,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Text(
                      'Home inventory',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onPrimaryContainer
                            .withValues(alpha: 0.7),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Catalog
          ListTile(
            leading: const Icon(Icons.category_outlined),
            title: const Text('Catalog'),
            onTap: () {
              Navigator.of(context).pop(); // close drawer
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                    builder: (_) => const CatalogScreen()),
              );
            },
          ),

          // Settings
          ListTile(
            leading: const Icon(Icons.settings_outlined),
            title: const Text('Settings'),
            onTap: () {
              Navigator.of(context).pop();
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                    builder: (_) => const SettingsScreen()),
              );
            },
          ),

          const Divider(),

          // About
          ListTile(
            leading: const Icon(Icons.info_outline),
            title: const Text('About'),
            onTap: () {
              Navigator.of(context).pop();
              showAboutDialog(
                context: context,
                applicationName: 'Holodos',
                applicationVersion: '0.1.0',
                applicationIcon: const Icon(Icons.kitchen_outlined, size: 48),
                children: const [
                  Text(
                    'Home inventory and shopping management platform.\n'
                    'Built with Flutter and Spring Boot.',
                  ),
                ],
              );
            },
          ),

          const Spacer(),

          // Version footer
          Padding(
            padding: const EdgeInsets.all(16),
            child: Text(
              'v0.1.0',
              style: theme.textTheme.bodySmall?.copyWith(
                color:
                    theme.colorScheme.onSurface.withValues(alpha: 0.4),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
