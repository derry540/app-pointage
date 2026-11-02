package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.LocalOfflineStore
import com.example.data.repository.ThermoPointRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.LocalPinSession
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AiAssistantTabScreen
import com.example.ui.screens.DashboardTabScreen
import com.example.ui.screens.PayrollAdvancesTabScreen
import com.example.ui.screens.PointageAndEmployeesTabScreen
import com.example.ui.screens.StockAndCompaniesTabScreen
import com.example.ui.screens.SuppliersErpAuditTabScreen
import com.example.ui.screens.WorkerSelfPortalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThermalCopper
import com.example.util.configureAppCheck
import com.example.viewmodel.AppPortalMode
import com.example.viewmodel.GeminiAiViewModel
import com.example.viewmodel.ThermoPointViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration before setContent, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        LocalOfflineStore.init(this)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ThermoPointRootApp()
            }
        }
    }
}

@Composable
fun ThermoPointRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var localPinSession by remember { mutableStateOf<LocalPinSession?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    val pinSession = localPinSession

    if (user == null && pinSession == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onLocalPinLoginSuccess = { session -> localPinSession = session }
        )
    } else {
        val effectiveUserId = user?.uid ?: pinSession?.userId ?: "equipements_thermiques_shared_org"
        val effectiveDisplayName = user?.displayName
            ?: user?.email
            ?: pinSession?.displayName
            ?: "Admin"
        val isWorkerRoleLocked = pinSession?.isWorkerRole == true
        val initialWorkerId = pinSession?.workerId

        AuthenticatedMainContent(
            currentUserId = effectiveUserId,
            userDisplayName = effectiveDisplayName,
            isWorkerLockedSession = isWorkerRoleLocked,
            initialWorkerId = initialWorkerId,
            onSignOutLocalAndCloud = {
                localPinSession = null
                currentUser = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainContent(
    currentUserId: String,
    userDisplayName: String,
    isWorkerLockedSession: Boolean = false,
    initialWorkerId: String? = null,
    onSignOutLocalAndCloud: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val sessionKey = "${currentUserId}_${isWorkerLockedSession}_${initialWorkerId.orEmpty()}"

    val thermoViewModel: ThermoPointViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                LocalOfflineStore.init(app)
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ThermoPointViewModel(
                    repository = ThermoPointRepository(db),
                    currentUserId = currentUserId,
                    initialPortalMode = if (isWorkerLockedSession) AppPortalMode.WORKER_PORTAL else AppPortalMode.ADMIN_PORTAL,
                    initialWorkerId = initialWorkerId,
                    isWorkerLockedSession = isWorkerLockedSession
                )
            }
        }
    )

    val aiViewModel: GeminiAiViewModel = viewModel(key = "ai_$currentUserId")

    val portalMode by thermoViewModel.portalMode.collectAsStateWithLifecycle()
    val statusBanner by thermoViewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val employees by thermoViewModel.employees.collectAsStateWithLifecycle()
    val stockItems by thermoViewModel.stockItems.collectAsStateWithLifecycle()
    val suppliers by thermoViewModel.suppliers.collectAsStateWithLifecycle()
    val lowStockCount = stockItems.count { it.isLowStock }
    val pendingSuppliersCount = suppliers.count { it.remainingBalance > 0 }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    if (selectedTab != 0 && portalMode == AppPortalMode.ADMIN_PORTAL && !isWorkerLockedSession) {
        BackHandler {
            selectedTab = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = ThermalCopper
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ThermoPoint Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isWorkerLockedSession) {
                                    "Compte Salarié (Sans Permissions Admin) • $userDisplayName"
                                } else {
                                    "Équipements Thermiques • $userDisplayName"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (!isWorkerLockedSession) {
                        FilledTonalButton(
                            onClick = {
                                val nextMode = if (portalMode == AppPortalMode.ADMIN_PORTAL) {
                                    AppPortalMode.WORKER_PORTAL
                                } else {
                                    AppPortalMode.ADMIN_PORTAL
                                }
                                thermoViewModel.setPortalMode(nextMode)
                            },
                            modifier = Modifier.testTag("top_bar_portal_mode_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Basculer mode Admin / Salarié"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (portalMode == AppPortalMode.ADMIN_PORTAL) "Admin" else "Salarié",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOutLocalAndCloud,
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Se déconnecter"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!isWorkerLockedSession && portalMode == AppPortalMode.ADMIN_PORTAL && selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddTask, contentDescription = "Nouveau Pointage") },
                    text = { Text("Nouveau Pointage") },
                    containerColor = ThermalCopper,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("scaffold_fab_quick_pointage")
                )
            }
        },
        bottomBar = {
            if (!isWorkerLockedSession && portalMode == AppPortalMode.ADMIN_PORTAL) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (employees.isNotEmpty()) {
                                        Badge { Text("${employees.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = "Employés & Pointage")
                            }
                        },
                        label = { Text("Employés", maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_pointage")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Paie & Avances") },
                        label = { Text("Paie", maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_paie")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (lowStockCount > 0) {
                                        Badge { Text("$lowStockCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = "Suivi Stock")
                            }
                        },
                        label = { Text("Stock", maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_stock")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingSuppliersCount > 0) {
                                        Badge { Text("$pendingSuppliersCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = "Fournisseurs & Audit")
                            }
                        },
                        label = { Text("Fourn.", maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_erp")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "IA Gemini") },
                        label = { Text("IA Gemini", maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_ai")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = statusBanner != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { thermoViewModel.clearStatusBanner() }) {
                            Text("OK")
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isWorkerLockedSession || portalMode == AppPortalMode.WORKER_PORTAL) {
                    WorkerSelfPortalScreen(viewModel = thermoViewModel)
                } else {
                    when (selectedTab) {
                        0 -> DashboardTabScreen(
                            viewModel = thermoViewModel,
                            onNavigateToTab = { tabIdx -> selectedTab = tabIdx }
                        )
                        1 -> PointageAndEmployeesTabScreen(viewModel = thermoViewModel)
                        2 -> PayrollAdvancesTabScreen(viewModel = thermoViewModel)
                        3 -> StockAndCompaniesTabScreen(viewModel = thermoViewModel)
                        4 -> SuppliersErpAuditTabScreen(viewModel = thermoViewModel)
                        5 -> AiAssistantTabScreen(
                            thermoViewModel = thermoViewModel,
                            aiViewModel = aiViewModel
                        )
                    }
                }
            }
        }
    }
}
