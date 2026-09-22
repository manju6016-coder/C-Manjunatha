package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.ILedgerRepository
import com.example.data.repository.LedgerRepository
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminSetPasswordTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ILedgerRepository
    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepository.fromDatabase(db)
        repository.seedInitialDataIfEmpty()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testAdminCanSetPasswordForRicInCharge() = runBlocking {
        // 1. Create RIC In-Charge user
        val ricUser = UserEntity(
            outletCode = "RIC99",
            name = "Vikram Singh",
            mobileNumber = "9876543211",
            password = "",
            role = UserRole.RIC.name,
            assignedShopCodes = "OUT-101"
        )
        val ricId = repository.addUser(ricUser)
        assertTrue(ricId > 0)

        // 2. Initial login with mobile number succeeds
        val initialLogin = repository.login("RIC99", "9876543211")
        assertNotNull("RIC should log in with mobile number initially", initialLogin)

        // 3. Admin sets custom password for RIC
        repository.updateUserPassword(ricId, "ricSecret2026")

        // 4. Verify RIC can now log in with the new password
        val loginWithPassword = repository.login("RIC99", "ricSecret2026")
        assertNotNull("RIC should log in with new password", loginWithPassword)
        assertEquals("Vikram Singh", loginWithPassword?.name)

        // 5. Wrong password fails
        val loginWithWrongPass = repository.login("RIC99", "wrongPassword")
        assertNull("Wrong password should fail", loginWithWrongPass)
    }

    @Test
    fun testAdminCanSetPasswordForShopEmployee() = runBlocking {
        // 1. Create Shop Employee user with initial password
        val employeeUser = UserEntity(
            outletCode = "EMP99",
            name = "Deepak Kumar",
            mobileNumber = "9876543212",
            password = "initialEmpPass",
            role = UserRole.SHOP_EMPLOYEE.name,
            assignedShopCodes = "OUT-101"
        )
        val empId = repository.addUser(employeeUser)
        assertTrue(empId > 0)

        // 2. Login with initial password succeeds
        val initialLogin = repository.login("EMP99", "initialEmpPass")
        assertNotNull("Employee should log in with initial password", initialLogin)

        // 3. Admin updates password by outlet code
        repository.updateUserPasswordByOutletCode("EMP99", "newEmpSecret456")

        // 4. Verify login with updated password succeeds
        val loginWithUpdatedPass = repository.login("EMP99", "newEmpSecret456")
        assertNotNull("Employee should log in with updated password", loginWithUpdatedPass)

        // 5. Old password fails
        val loginWithOldPass = repository.login("EMP99", "initialEmpPass")
        assertNull("Old password should no longer work", loginWithOldPass)
    }

    @Test
    fun testLedgerViewModelPasswordUpdateFlow() = runBlocking {
        val app = ApplicationProvider.getApplicationContext() as Application
        val viewModel = LedgerViewModel(app, repository)

        val empUser = repository.getUserByOutletCode("EMP101")
        assertNotNull("EMP101 should exist in repository", empUser)

        // 1. Verify initial login with seeded password works
        val initialLoginSuccess = viewModel.loginSuspend("EMP101", "emp123")
        assertTrue("Initial login should succeed with seeded password", initialLoginSuccess)
        assertEquals("EMP101", viewModel.currentUser.value?.outletCode)

        // 2. Admin sets a new password for employee via ViewModel
        viewModel.updateUserPasswordSuspend(empUser!!.id, "adminSetSecret789")

        // 3. Verify new password logs in via ViewModel
        val newLoginSuccess = viewModel.loginSuspend("EMP101", "adminSetSecret789")
        assertTrue("Login with admin-set password should succeed", newLoginSuccess)
        assertEquals("EMP101", viewModel.currentUser.value?.outletCode)

        // 4. Verify old password no longer works
        val oldLoginSuccess = viewModel.loginSuspend("EMP101", "emp123")
        assertFalse("Old password should not work", oldLoginSuccess)
    }
}
