package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermoPointRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createEmployeeAndObserve_authenticatedOwner_emitsCreatedEmployee() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ThermoPointRepository(firestore)

        val empId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createEmployee(
                fullName = "Karim Tazi",
                workerEmail = "karim@thermique.ma",
                role = "WORKER",
                specialty = "Installation Chaudières",
                hourlyRate = 50.0,
                overtimeBonusRate = 75.0,
                commissionPercent = 4.0,
                lastMonthPaidTotal = 9500.0
            ).getOrThrow()
        }
        assertTrue(empId.isNotEmpty())

        val employees = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeEmployees().first { list -> list.any { it.id == empId } }
        }
        assertTrue(employees.any { it.id == empId && it.fullName == "Karim Tazi" })
    }

    @Test
    fun getEmployeeById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = ThermoPointRepository(firestore)
        val empId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createEmployee(
                fullName = "Alice Worker",
                workerEmail = "worker@alice.ma",
                role = "WORKER",
                specialty = "Brûleurs",
                hourlyRate = 40.0,
                overtimeBonusRate = 60.0,
                commissionPercent = 2.0,
                lastMonthPaidTotal = 7000.0
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ThermoPointRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getEmployeeById(empId)
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeEmployees_unauthenticatedUser_failsOnMissingAuth() = runBlocking {
        auth.signOut()
        val repository = ThermoPointRepository(firestore)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeEmployees().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@thermique.ma"
        const val BOB_EMAIL = "bob@thermique.ma"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
