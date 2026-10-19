-- Shopping list is read by status ordered by sort_order (UI, Keep/Tasks sync, reports)
CREATE INDEX IF NOT EXISTS ix_shopping_list_items_status_sort ON shopping_list_items (status, sort_order);
