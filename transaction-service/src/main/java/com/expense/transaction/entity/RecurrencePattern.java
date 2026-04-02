package com.expense.transaction.entity;

public enum RecurrencePattern {
    DAILY,              // Daily - Every day
    WEEKLY,             // Weekly - Every week on specific day(s)
    BI_WEEKLY,          // Bi-weekly - Every 2 weeks
    MONTHLY,            // Monthly - Specific day(s) of month
    YEARLY,             // Yearly - Specific date each year
    CUSTOM_INTERVAL     // Custom - Every N days/weeks/months
}
