# eSalesManagerV2

App quản lý / giám sát bán hàng (Android, Kotlin + Compose), viết lại từ HQSoft eSales Manager (Vifon).

- Một module `:app`; các lớp là package trong `com.tinhcd.myesalesmanager` (`core`, `data`, `domain`, `feature`, `navigation`).
- Dùng chung database Supabase với MyeSalesSFA. Migration và Edge Function (tiền tố `mgr-`) nằm ở repo SFA, thư mục `supabase/`.
- `local.properties` (gitignored) cần: `supabase.url`, `supabase.publishableKey`, `maps.apiKey`.
