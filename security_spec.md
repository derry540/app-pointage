# Security Specification — ThermoPoint Pro (Équipements Thermiques)

## 1. Data Invariants
1. **Authentication & Ownership Isolation**: Every document in `/employees`, `/attendance_logs`, `/advances`, `/companies`, `/stock_items`, `/daily_procurements`, `/suppliers`, and `/audit_logs` MUST belong to an authenticated user (`request.auth != null`) and have `userId == request.auth.uid`.
2. **Immutable Ownership & Creation Timestamp**: `userId` and `createdAt` can never be mutated on `update` (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Append-Only Audit Logs**: Documents in `/audit_logs/{auditId}` can only be created and read by their owner; updates and deletes are strictly forbidden (`allow update, delete: if false;`).
4. **Domain Bounds**:
   - `AttendanceLog`: `totalHours >= 0 && totalHours <= 24`, `regularHours >= 0 && regularHours <= 9`, `overtimeHours >= 0 && overtimeHours <= 15`, `performanceRating >= 1 && performanceRating <= 5`.
   - `EmployeeProfile`: `role in ['ADMIN', 'MANAGER', 'WORKER']`, `hourlyRate >= 0`, `overtimeBonusRate >= 0`, `commissionPercent >= 0 && commissionPercent <= 100`.
   - `StockItem`: `currentQuantity >= 0`, `reorderPoint >= 0`, `unitPrice >= 0`.
   - `DailyProcurement`: `quantity > 0`, `status in ['NEEDED', 'ORDERED', 'RECEIVED']`.
   - `SupplierRecord`: `amountDue >= 0`, `amountPaid >= 0`, `status in ['PENDING', 'PARTIAL', 'PAID', 'OVERDUE']`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: Caller with `request.auth == null` attempts to list `/employees`.
2. **Cross-User Read (PII Leak)**: User `alice_123` attempts to `get` or `list` `/employees` owned by `bob_456`.
3. **Identity Spoofing on Create**: User `alice_123` attempts to create an `AttendanceLog` with `userId: "bob_456"`.
4. **Ownership Hijack on Update**: User `alice_123` attempts to update `userId` on an existing `StockItem`.
5. **Timestamp Tampering (Future Clock)**: Client submits `createdAt` in the future (`> request.time`) or mutates `createdAt` during an update.
6. **Shadow Field Injection**: Client sends an undocumented field `{"isSuperAdmin": true}` in `/employees`.
7. **Invalid Enum Injection**: Client creates an `EmployeeProfile` with `role: "SUPERUSER"` or `SupplierRecord` with `status: "FORGIVEN"`.
8. **Out-of-Bounds Hours**: Client creates an `AttendanceLog` with `regularHours: 14` (violating the 9h/day regular cap) or `totalHours: 30`.
9. **Negative Financial Injection**: Client creates a `SalaryAdvance` with `amount: -500`.
10. **Audit Log Tampering**: Authenticated owner attempts to `update` or `delete` a record in `/audit_logs`.
11. **Denial-of-Wallet Oversized String**: Client injects a 10,000-character string into `dailyNote` or `details`.
12. **Unfiltered Collection List Query**: Authenticated user executes `db.collection("employees").get()` without `.where("userId", "==", uid)`.
