package com.example.util

import com.example.data.model.UserRole

/**
 * Access Control and Business Rules for Retail Operations:
 *
 * 1. Admin & RIC can modify past date entries.
 * 2. Shop Employee can only view past date records (read-only mode).
 * 3. Shop Employee can only record or modify transactions for the current business date (Today).
 * 4. Admin has exclusive permission to add outlets, assign Regional In-Charge (RIC) for outlets,
 *    and set passwords for RICs and Shop Employees.
 */
object TransactionPermissionRules {

    /**
     * Determines whether a given timestamp falls before today's start of day (00:00:00).
     */
    fun isPastDate(dateMillis: Long): Boolean {
        val todayStart = IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis())
        return dateMillis < todayStart
    }

    /**
     * Determines whether a user with [userRole] can modify an existing transaction on [transactionDate].
     * - ADMIN: Allowed to modify any record (past or present).
     * - RIC: Allowed to modify any record (past or present).
     * - SHOP_EMPLOYEE: Only allowed to modify records for TODAY; past records are strictly view-only!
     */
    fun canModifyEntry(userRole: UserRole, transactionDate: Long): Boolean {
        return when (userRole) {
            UserRole.ADMIN -> true
            UserRole.RIC -> true
            UserRole.SHOP_EMPLOYEE -> !isPastDate(transactionDate)
        }
    }

    fun canModifyEntry(userRoleCode: String, transactionDate: Long): Boolean {
        return canModifyEntry(UserRole.fromCode(userRoleCode), transactionDate)
    }

    /**
     * Determines whether a user can select past dates when creating a new transaction.
     * - ADMIN & RIC: Can back-date or select past dates.
     * - SHOP_EMPLOYEE: Restricted to Today's date only.
     */
    fun canSelectPastDateForNewEntry(userRole: UserRole): Boolean {
        return when (userRole) {
            UserRole.ADMIN, UserRole.RIC -> true
            UserRole.SHOP_EMPLOYEE -> false
        }
    }

    fun canSelectPastDateForNewEntry(userRoleCode: String): Boolean {
        return canSelectPastDateForNewEntry(UserRole.fromCode(userRoleCode))
    }

    /**
     * Determines whether the user can add outlets.
     * Rule: Only Admin can add outlets.
     */
    fun canAddOutlet(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canAddOutlet(userRoleCode: String): Boolean = canAddOutlet(UserRole.fromCode(userRoleCode))

    /**
     * Determines whether the user can assign RIC to an outlet.
     * Rule: Only Admin can assign RIC for outlets.
     */
    fun canAssignRic(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canAssignRic(userRoleCode: String): Boolean = canAssignRic(UserRole.fromCode(userRoleCode))

    /**
     * Determines whether the user can set passwords for RIC and Shop Employees.
     * Rule: Only Admin can set passwords for RIC and Shop Employees.
     */
    fun canSetPassword(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canSetPassword(userRoleCode: String): Boolean = canSetPassword(UserRole.fromCode(userRoleCode))

    /**
     * Determines whether the user can delete an outlet/shop.
     * Rule: Only Admin can delete outlets.
     */
    fun canDeleteOutlet(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canDeleteOutlet(userRoleCode: String): Boolean = canDeleteOutlet(UserRole.fromCode(userRoleCode))

    /**
     * Determines whether the user can delete a staff member (RIC or Employee).
     * Rule: Only Admin can delete staff.
     */
    fun canDeleteStaff(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canDeleteStaff(userRoleCode: String): Boolean = canDeleteStaff(UserRole.fromCode(userRoleCode))

    /**
     * Determines whether the user can reassign staff and outlets.
     * Rule: Only Admin can reassign staff.
     */
    fun canReassignStaff(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canReassignStaff(userRoleCode: String): Boolean = canReassignStaff(UserRole.fromCode(userRoleCode))

    /**
     * Returns a user-friendly explanatory message when an operation is restricted.
     */
    fun getPastRecordExplanation(userRole: UserRole): String {
        return if (userRole == UserRole.SHOP_EMPLOYEE) {
            "Shop employees can only view past records. To modify past date entries, please contact your Regional In-Charge (RIC) or Administrator."
        } else {
            "Authorized: As an ${userRole.displayName}, you have permission to modify past date entries."
        }
    }
}
