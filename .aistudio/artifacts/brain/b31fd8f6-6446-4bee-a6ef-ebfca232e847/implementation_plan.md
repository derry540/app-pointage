# ThermoPoint Pro — Gestion de Pointage, Paie, Stock & ERP (Équipements Thermiques)

ThermoPoint Pro est une application Android complète conçue sur mesure pour **Équipements Thermiques**. Elle automatise le pointage journalier par intervalles d'heures (norme de 9h/jour avec calcul automatique des heures supplémentaires et bonus), gère la paie horaire variable et les avances sans risque d'erreur, centralise les chantiers/sociétés, pilote les stocks avec alertes de réapprovisionnement en temps réel, suit les échéanciers fournisseurs, et synchronise les écritures avec **Memesoft Oxygène ERP**.

## User Review & Critical Decisions

> [!IMPORTANT]
> Les choix d'architecture suivants ont été retenus par défaut pour garantir une expérience fluide, sécurisée et immédiatement opérationnelle :

- **Langue & Devise par défaut** : Interface en **Français** adaptée au contexte marocain (montants en **MAD / DH**, terminologie métier claire pour le pointage, les avances et la gestion thermique).
- **Contrôle d'accès Multi-Rôles (Admin vs. Employé)** : Authentification sécurisée via **Google Sign-In (Firebase Auth & Firestore)**. Le créateur et les comptes promus disposent du rôle **Admin** (gestion des tarifs horaires, validation du pointage, avances, sociétés, stock, fournisseurs, historique d'audit). Les travailleurs disposent d'une **Vue Employé** en lecture seule affichant uniquement leurs statistiques personnelles (heures du mois en cours vs. mois précédent, avances reçues, reste à percevoir).
- **Passerelle Memesoft Oxygène ERP** : Double intégration comprenant **(1)** un connecteur API REST configurable (synchronisation JSON en temps réel vers une passerelle Oxygène Cloud/SQL) et **(2)** un générateur natif de fichiers d'import comptable et paie au standard **Memsoft Oxygène** (CSV structuré : *Pièces, Mouvements, Échéances, Ventilations analytiques*) ainsi que des exports **PDF** complets.

---

## 1. Overview & Core Concept

- **What It Does** :
  1. **Pointage Journalier Anti-Fraude & Multi-Sociétés** : Enregistrement précis des intervalles d'heures travaillées par jour. Si un employé travaille dans deux sociétés/chantiers le même jour, chaque créneau horaire est associé à la société correspondante (sélectionnée depuis la base de données), avec une tâche optionnelle et une note journalière.
  2. **Calcul Automatique de Paie Horaire (Base 9h/j), Bonus & Avances** : Chaque employé possède son propre tarif horaire variable. Au-delà de **9 heures par jour**, l'application calcule automatiquement les heures supplémentaires et applique le bonus/pourcentage configuré. Le suivi déduit les **avances (`avances`)** versées pour afficher instantanément le **reste à payer**, le comparatif avec le salaire du mois précédent, et des graphiques de performance.
  3. **Sociétés, Approvisionnement du Jour & Alertes Stock Temps Réel** : Base de données des sociétés clientes/chantiers permettant de noter chaque jour la marchandise à acheter (`sl3a li khasaha njibha`) et son coût, reliée au stock central avec alertes automatiques de réapprovisionnement dès qu'un équipement thermique passe sous le seuil critique.
  4. **Fournisseurs, Échéanciers & Audit Complet** : Gestion des fournisseurs avec rappels de paiement, historique d'accès et de modifications (`qui a modifié quoi et quand`), notifications d'alerte à chaque enregistrement, envoi automatisé des rapports mensuels par e-mail, exports PDF/Excel et synchronisation **Oxygène ERP**.
  5. **Suite IA Gemini Intégrée** : Analyse photo/vidéo de fiches de pointage, bons de livraison et chantiers thermiques (`gemini-4-argon`), assistant de gestion et comptabilité Oxygène avec mode réflexion approfondie (`ThinkingLevel.HIGH`), et génération d'images techniques en 1K, 2K ou 4K (`gemini-3-pro-image`).
- **Target Audience / Persona** : Dirigeants, administrateurs de chantier et techniciens de la société **Équipements Thermiques**.
- **Key Value** : Élimine les erreurs de déclaration d'heures en fin de semaine, automatise le calcul complexe des avances et heures supplémentaires (>9h), et unifie le terrain (pointage, achats du jour, stock) avec la comptabilité Oxygène ERP.

---

## 2. User Experience & Visual Design

- **Key User Flows** :
  1. **Connexion & Routage par Rôle** : Connexion Google en un clic. Si l'utilisateur est **Admin**, ouverture du **Tableau de Bord Global**. Si l'utilisateur est **Employé**, ouverture du **Portail Salarié** (résumé des heures travaillées, heures sup >9h, avances perçues, net à payer pour le mois actuel et le mois dernier).
  2. **Catégories d'Employés & Tarifs (Admin)** : L'Admin ajoute un employé, définit son taux horaire (DH/h), son taux/bonus d'heures supplémentaires (>9h/j) et son pourcentage de commission.
  3. **Pointage Journalier & Multi-Chantiers** : Saisie rapide par intervalle (`08:00 - 13:00` chez *Société A*, `14:00 - 19:30` chez *Société B*). Calcul immédiat : `10.5h total = 9h normales + 1.5h supplémentaires + bonus`. Ajout d'une note quotidienne et d'une tâche (facultative).
  4. **Suivi Paie, Avances & Graphiques** : Onglet récapitulatif affichant pour chaque employé le total gagné sur la période, les avances données, le solde restant dû, le salaire du mois précédent, et un graphique comparatif des performances.
  5. **Chantiers, Achats du Jour & Stock Automatisé** : Sélection d'une société pré-enregistrée, saisie des besoins d'approvisionnement du jour (articles thermiques, quantités, prix), mise à jour du stock et affichage des alertes de réapprovisionnement critiques sur le Dashboard.
  6. **Fournisseurs, Audit, Exports PDF/Excel & Oxygène ERP** : Consultation des échéanciers fournisseurs avec rappels, vérification du journal d'audit des modifications, génération de rapports PDF/CSV, envoi par e-mail et synchronisation vers Memesoft Oxygène.

- **Visual Identity & Theme** :
  - *Aesthetic Direction* : **Precision Thermal Industrial** — une esthétique professionnelle haut de gamme alliant le bleu acier profond de l'ingénierie industrielle à des accents cuivre/orange thermique vifs et une lisibilité tabulaire irréprochable.
  - *Color Palette & Mood* :
    - Background / Surface principale : Ardoise Carbone `#0F172A` (Mode Sombre industriel) et Blanc Technique `#F8FAFC` (Mode Clair), cartes étagées `#1E293B` / `#FFFFFF`.
    - Primary (`--primary`) : Orange Thermique Cuivré `#EA580C` / `#F97316` (énergie, chaleur, actions principales).
    - Secondary (`--secondary`) : Bleu Acier Cryo `#0284C7` (précision, équipements, graphiques).
    - Semantic Accents : Vert Émeraude `#10B981` (pointage validé / stock OK), Ambre `#F59E0B` (heures sup >9h / échéance proche), Rouge Alerte `#EF4444` (rupture de stock / retard fournisseur).
  - *Typography & Hierarchy* :
    - Titres & En-têtes : **Space Grotesk** (installée localement via `font-util`) pour un caractère technique et moderne.
    - Corps & Tableaux : **Plus Jakarta Sans** & **JetBrains Mono** pour l'alignement parfait des heures (`09h30`), des tarifs horaires et des soldes en Dirhams (MAD).
  - *Component Styling & Layout* : Material Design 3 avec cartes asymétriques à bordures subtiles, indicateurs de progression circulaires pour le seuil des 9h/jour, badges de statut colorés et barre de navigation adaptative respectant `WindowInsets`.
- **Interactive Feedback & Motion** : Animations fluides (`AnimatedVisibility` avec `spring()`) lors de l'ajout d'intervalles horaires, alertes visuelles pulsées pour les stocks critiques, et indicateurs de progression détaillés lors des exports PDF et appels Gemini.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 : Calcul Horaire par Intervalles & Seuil Légal Interne de 9h/Jour**
  - *Chosen Approach* : Chaque journée de pointage accepte un ou plusieurs intervalles horaires (`heureDébut`, `heureFin`, `sociétéId`, `tâcheOptionnelle`, `note`). Le moteur additionne les heures de la journée pour un employé : les 9 premières heures sont facturées au tarif normal (`tauxHoraire`), et toute heure au-delà de 9h est automatiquement basculée en heure supplémentaire avec bonus.
  - *Why* : Empêche les fausses déclarations forfaitaires (ex. réclamer 9h × 7j sans justificatif) et permet de répartir une même journée sur deux entreprises clientes.
- **Decision 2 : Persistance Cloud Temps Réel & Traçabilité d'Audit (Firebase Firestore)**
  - *Chosen Approach* : Base de données **Firebase Firestore** couplée à **Firebase Auth (Google Sign-In)**, avec écriture automatique dans une collection `audit_logs` à chaque création ou modification (pointage, avance, stock, rôle).
  - *Why* : Garantit qu'aucune donnée de paie ou d'avance n'est perdue en cas de changement de téléphone, permet aux employés de consulter leurs propres compteurs sans pouvoir les modifier, et assure une transparence totale entre administrateurs.
- **Decision 3 : Synchronisation Hybride Memesoft Oxygène ERP + Rapports PDF/CSV**
  - *Chosen Approach* : Génération native de fichiers CSV au format d'import comptable/paie **Oxygène ERP** + connecteur HTTP REST configurable (Retrofit) pour les passerelles Oxygène Cloud/API + génération de documents PDF multi-pages via `android.graphics.pdf.PdfDocument` et envoi d'e-mails pré-formatés.
  - *Why* : Memesoft Oxygène fonctionne aussi bien par import structuré de fichiers plats (Pièces/Mouvements/Échéances) que via des connecteurs API d'intégrateurs ; offrir les deux garantit une compatibilité à 100%.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram** :

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   MainActivity (App Check + EdgeToEdge)                    │
│               AuthGate (Google Sign-In via CredentialManager)              │
└───────────────────┬────────────────────────────────────────┬───────────────┘
                    │ (Role == ADMIN / MANAGER)              │ (Role == WORKER)
                    ▼                                        ▼
┌───────────────────────────────────────────┐  ┌─────────────────────────────┐
│        Admin Navigation Scaffold          │  │    Worker Self-Portal       │
├───────────────────────────────────────────┤  ├─────────────────────────────┤
│ • DashboardTab (KPIs, Achats Jour, Alertes│  │ • Stats Mois Actuel vs Préc.│
│   Stock, Échéances, Accès Audit & ERP)    │  │ • Détail Heures (Norm + >9h)│
│ • PointageTab (Saisie Intervalles, 9h+    │  │ • Avances Reçues & Solde Dû │
│   Bonus, Multi-Sociétés, Tâches & Notes)  │  │ • Historique Personnel      │
│ • PaieAvancesTab (Calcul Auto, Avances,   │  └─────────────────────────────┘
│   Reste à Payer, Mois Préc., Graphiques)  │
│ • ChantiersStockTab (Sociétés BD, Achats  │
│   Journaliers, Stock & Réappro Auto)      │
│ • FournisseursErpTab (Échéanciers, Audit, │
│   Export PDF/Excel, Sync Oxygène, Rôles)  │
│ • AiStudioHubTab (Chat, Scan Facture/     │
│   Pointage, Analyse Vidéo, Studio 1K-4K)  │
└───────────────────┬───────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                ViewModels (StateFlow + Coroutines + Timeouts)              │
│   • WorkforceViewModel (Employés, Pointage 9h+, Avances, Stats, Audit)     │
│   • OperationsViewModel (Sociétés, Besoins du Jour, Stock, Fournisseurs)   │
│   • ErpExportViewModel (PDF Generator, CSV/Excel, Email, Oxygène API)      │
│   • GeminiAiViewModel (Chat, High-Thinking, Image/Video Scan, Image Gen)   │
└───────────────────┬────────────────────────────────────────┬───────────────┘
                    │                                        │
                    ▼                                        ▼
┌───────────────────────────────────────────┐  ┌─────────────────────────────┐
│     FirestoreRepository (Custom DB ID)    │  │   Firebase AI Logic SDK     │
│ • employees, attendance_logs, advances    │  │ • gemini-3.8-flash (Chat)   │
│ • companies, daily_procurements, stock    │  │ • gemini-4-argon (Thinking, │
│ • suppliers, audit_logs, user_roles       │  │   Image & Video Analysis)   │
│ • Local Notification & Alert Manager      │  │ • gemini-3-pro-image (1-4K) │
└───────────────────────────────────────────┘  └─────────────────────────────┘
```

- **Data Model & State** :
  - `EmployeeCategory` : `id`, `fullName`, `workerEmail`, `role` (`ADMIN`, `MANAGER`, `WORKER`), `hourlyRate` (MAD/h), `overtimeBonusRate` (MAD/h sup au-delà de 9h), `commissionPercent` (%), `lastMonthPaidTotal` (MAD).
  - `AttendanceRecord` (Pointage Journalier) : `id`, `employeeId`, `employeeName`, `date` (`YYYY-MM-DD`), `intervals` (liste de créneaux `{startTime, endTime, hours, companyId, companyName, optionalTask}`), `totalHours`, `regularHours` (`min(total, 9.0)`), `overtimeHours` (`max(0.0, total - 9.0)`), `dailyPay` (calculé auto), `dailyNote`, `recordedByAdminName`.
  - `AdvanceRecord` (Avances) : `id`, `employeeId`, `employeeName`, `amount` (MAD), `date`, `monthKey` (`YYYY-MM`), `note`, `recordedBy`.
  - `ClientCompany` (Sociétés / Chantiers) : `id`, `name`, `location`, `contactPhone`, `specialtyNote`.
  - `DailyProcurement` (Achats du Jour par Société) : `id`, `date`, `companyId`, `companyName`, `itemName`, `quantity`, `unitCost`, `totalCost`, `isPurchased`, `linkedStockItemId`.
  - `StockItem` (Inventaire Thermique) : `id`, `name`, `category` (Chaudières, Brûleurs, Tuyauterie, Régulation, Pièces), `currentQuantity`, `minThreshold`, `unit`, `unitPrice`, `autoRestockAlertActive`.
  - `SupplierSchedule` (Fournisseurs & Échéanciers) : `id`, `supplierName`, `phone`, `email`, `invoiceRef`, `amountDue`, `amountPaid`, `dueDate`, `status` (`PENDING`, `PARTIAL`, `PAID`, `OVERDUE`), `reminderEnabled`.
  - `AuditLogEntry` (Historique des Accès & Modifications) : `id`, `actorUid`, `actorName`, `actionType` (`CREATE`, `UPDATE`, `DELETE`, `EXPORT`, `ERP_SYNC`), `targetModule`, `description`, `createdAt`.

- **Interactive Component & State Mapping** :
  - **Moteur de Pointage 9h+** : Lorsqu'un Admin ajoute un deuxième intervalle dans la même journée pour un employé (ex. `5h` chez *Société A* + `5.5h` chez *Société B* = `10.5h`), le `WorkforceViewModel` recalcule instantanément `regularHours = 9.0`, `overtimeHours = 1.5`, et `dailyPay = (9.0 * hourlyRate) + (1.5 * overtimeBonusRate)`, crée une entrée dans `audit_logs`, et déclenche une notification locale si le système d'alertes est activé.
  - **Calculateur d'Avances & Solde Net** : Agrège en temps réel `Somme(dailyPay du mois) + Commission(%) - Somme(avances du mois) = Reste à Payer`, mis en regard du salaire du mois précédent et représenté sur un graphique Canvas interactif.
  - **Système d'Alertes de Réapprovisionnement & Achats du Jour** : Toute sortie de stock qui fait passer `currentQuantity <= minThreshold` génère automatiquement une alerte prioritaire sur le Dashboard et propose d'ajouter l'article à la liste des achats du jour (`DailyProcurement`).
  - **Exports PDF / CSV & Intégration Oxygène ERP** : Génère des fichiers PDF (fiches de paie, relevé de pointage, état des stocks) et CSV compatibles Excel/Oxygène dans le stockage partagé avec partage direct (`FileProvider` + `Intent.ACTION_SEND` pour l'automatisation e-mail mensuelle) et test/envoi API vers le point de terminaison Oxygène configuré.
  - **Outils IA Gemini** :
    - *Scan Intelligent (`gemini-4-argon`)* : Sélection d'une photo (via Android Photo Picker) ou d'une vidéo de chantier pour extraire automatiquement des lignes de pointage ou des articles de stock.
    - *Assistant & High Thinking (`gemini-4-argon` avec `ThinkingLevel.HIGH` & `gemini-3.8-flash`)* : Chat multi-tours avec interrupteur "Réflexion Approfondie" pour l'analyse financière et technique.
    - *Générateur d'Images Studio (`gemini-3-pro-image`)* : Création de visuels techniques avec sélecteur de résolution **1K, 2K, 4K**.
