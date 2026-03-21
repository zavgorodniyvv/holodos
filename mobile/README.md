# Holodos Mobile (Flutter)

Offline-first Flutter клиент для Holodos:
- Riverpod для управления состоянием
- локальная SQLite база (sqflite) + очереди синхронизации
- REST API слой поверх backend'а (Dio)
- модульная структура features/* (dashboard, inventory, shopping)
- экран Inventory показывает реальные остатки и позволяет поставить запись в офлайн-очередь

## Быстрый старт
1. Установите Flutter (3.22+).
2. В корне проекта выполните:
   ```bash
   cd mobile
   flutter pub get
   flutter run # выбираем целевую платформу (Android/iOS/Web)
   ```
3. Настройте URL backend (переменная окружения `HOLODOS_API`, по умолчанию `http://localhost:8080/api`).

## Основная структура
```
mobile/
  lib/
    core/      # тема, маршрутизация, DI, инфраструктура
    features/  # экранные модули
    shared/    # общие виджеты/утилиты
```

## Дальнейшие шаги
- Подключить реальную синхронизацию с backend API
- Расширить offline-слой и добавить миграции SQLite
- Реализовать UI для всех сущностей (inventory, shopping, reports)
