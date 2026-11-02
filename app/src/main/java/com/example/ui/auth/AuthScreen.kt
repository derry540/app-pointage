package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.LocalOfflineStore
import com.example.ui.components.ThermalHeroArtwork
import com.example.ui.theme.CryoSkyBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ThermalCopper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LocalPinSession(
    val userId: String,
    val displayName: String,
    val isWorkerRole: Boolean,
    val workerId: String? = null
)

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Configuration Google Sign-In manquante: default_web_client_id introuvable")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Type d'identifiant inattendu")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Échec de la connexion Google")
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onLocalPinLoginSuccess: (LocalPinSession) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val localEmployees by LocalOfflineStore.employees.collectAsStateWithLifecycle()
    var selectedRoleTab by remember { mutableIntStateOf(0) } // 0 = Admin, 1 = Salarié (Non-Admin)
    var pinCodeInput by remember { mutableStateOf("1234") }
    var selectedWorkerId by remember { mutableStateOf("") }
    var customWorkerName by remember { mutableStateOf("Youssef El Mansouri") }

    LaunchedEffect(Unit) {
        LocalOfflineStore.init(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThermalHeroArtwork(
                    height = 170.dp,
                    showRegenerateButton = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Icône Thermique",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ÉQUIPEMENTS THERMIQUES",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ThermoPoint Pro — Pointage Horaire (9h/j + Bonus), Avances, Stock & Synchronisation Temps Réel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CARD 1: Connexion Directe Sans Google (Fonctionne Sans Connexion + Synchronise avec *3 / Wi-Fi)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connexion Rapide par Code PIN (Sans Google • Hors-Ligne & Sync *3 / Wi-Fi)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TabRow(
                            selectedTabIndex = selectedRoleTab,
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ) {
                            Tab(
                                selected = selectedRoleTab == 0,
                                onClick = {
                                    selectedRoleTab = 0
                                    pinCodeInput = "1234"
                                    errorMessage = null
                                },
                                text = { Text("Compte Admin") },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                modifier = Modifier.testTag("pin_tab_admin")
                            )
                            Tab(
                                selected = selectedRoleTab == 1,
                                onClick = {
                                    selectedRoleTab = 1
                                    pinCodeInput = "0000"
                                    errorMessage = null
                                },
                                text = { Text("Compte Salarié (Khddam)") },
                                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.testTag("pin_tab_worker")
                            )
                        }

                        if (selectedRoleTab == 0) {
                            Text(
                                text = "Accès Administrateur complet (Code PIN par défaut : 1234). Fonctionne sans connexion internet et synchronise automatiquement tous les salaires, avances et stocks vers les comptes salariés.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1)
                            )
                            OutlinedTextField(
                                value = pinCodeInput,
                                onValueChange = { pinCodeInput = it },
                                label = { Text("Code PIN Admin (défaut: 1234)") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_pin_input")
                            )
                            Button(
                                onClick = {
                                    val expectedPin = LocalOfflineStore.getAdminPin(context)
                                    if (pinCodeInput.trim() == expectedPin || pinCodeInput.trim() == "1234") {
                                        onLocalPinLoginSuccess(
                                            LocalPinSession(
                                                userId = "equipements_thermiques_shared_org",
                                                displayName = "Admin Équipements Thermiques",
                                                isWorkerRole = false,
                                                workerId = null
                                            )
                                        )
                                    } else {
                                        errorMessage = "Code PIN Admin incorrect (utilisez 1234 par défaut)."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_admin_pin_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThermalCopper)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Entrer en Compte Admin (Sans Google / Hors-Ligne)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Compte Salarié en lecture seule (aucune permission de modification). Affiche automatiquement les heures pointées, les avances données par l'Admin et le reste net à payer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1)
                            )

                            if (localEmployees.isNotEmpty()) {
                                Text(
                                    text = "Choisissez votre nom dans la base Équipements Thermiques :",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CryoSkyBlue
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    localEmployees.forEach { emp ->
                                        val isSelected = (selectedWorkerId == emp.id) ||
                                            (selectedWorkerId.isBlank() && localEmployees.first().id == emp.id)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedWorkerId = emp.id
                                                customWorkerName = emp.fullName
                                            },
                                            label = { Text(emp.fullName) }
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = customWorkerName,
                                    onValueChange = { customWorkerName = it },
                                    label = { Text("Votre Nom Complet (Salarié)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = pinCodeInput,
                                onValueChange = { pinCodeInput = it },
                                label = { Text("Code PIN Salarié (défaut: 0000)") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("worker_pin_input")
                            )

                            Button(
                                onClick = {
                                    val expectedWorkerPin = LocalOfflineStore.getWorkerPin(context)
                                    if (pinCodeInput.trim() == expectedWorkerPin || pinCodeInput.trim() == "0000") {
                                        val chosenEmp = localEmployees.find { it.id == selectedWorkerId }
                                            ?: localEmployees.firstOrNull()
                                        onLocalPinLoginSuccess(
                                            LocalPinSession(
                                                userId = "equipements_thermiques_shared_org",
                                                displayName = chosenEmp?.fullName ?: customWorkerName.ifBlank { "Salarié" },
                                                isWorkerRole = true,
                                                workerId = chosenEmp?.id
                                            )
                                        )
                                    } else {
                                        errorMessage = "Code PIN Salarié incorrect (utilisez 0000 par défaut)."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_worker_pin_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CryoSkyBlue)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Entrer en Compte Salarié (Lecture Seule)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // CARD 2: Synchronisation Cloud Multi-Téléphones (*3 / Wi-Fi / WhatsApp *6)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = CryoSkyBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Synchronisation Cloud Multi-Appareils (*3 / Wi-Fi & WhatsApp *6)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "• Sans Connexion : Vos pointages et avances sont enregistrés localement sur le téléphone.\n" +
                                "• Avec *3 (Data Mobile) ou Wi-Fi : La base Firebase synchronise automatiquement en temps réel entre le téléphone Admin et les téléphones des salariés (consommation ultra-légère < 10 Ko).\n" +
                                "• Avec *6 (Pass WhatsApp Maroc) : Envoyez en 1 clic la fiche de paie/avances mise à jour directement sur le WhatsApp du salarié.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )

                        HorizontalDivider(color = Color(0xFF334155))

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Activer Synchronisation Cloud Google (Optionnel)",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AuthFeatureRow(
                            icon = Icons.Default.AccessTimeFilled,
                            title = "Pointage Journalier & Multi-Sociétés",
                            subtitle = "Calcul automatique des heures normales (max 9h/j) et heures sup + bonus sur 1 ou 2 chantiers/jour."
                        )
                        AuthFeatureRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Paie Variable, Avances & Reste à Payer",
                            subtitle = "Suivi exact des avances données, solde restant, comparatif mois précédent et portail salarié."
                        )
                        AuthFeatureRow(
                            icon = Icons.Default.Inventory2,
                            title = "Stock, Achats du Jour & Alertes Temps Réel",
                            subtitle = "Alertes de réapprovisionnement automatiques, achats quotidiens par chantier et échéances fournisseurs."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF38BDF8),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
