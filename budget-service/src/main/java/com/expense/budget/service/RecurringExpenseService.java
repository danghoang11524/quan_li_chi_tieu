package com.expense.budget.service;

import com.expense.budget.dto.RecurringExpenseRequest;
import com.expense.budget.dto.RecurringExpenseResponse;
import com.expense.budget.dto.IncidentalExpenseRequest;
import com.expense.budget.entity.*;
import com.expense.common.exception.BadRequestException;
import com.expense.common.exception.ResourceNotFoundException;
import com.expense.budget.repository.RecurringExpenseRepository;
import com.expense.budget.repository.IncidentalExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final IncidentalExpenseRepository incidentalExpenseRepository;

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(Long userId, RecurringExpenseRequest request) {
        validateRecurringExpenseRequest(request);

        RecurringExpense expense = new RecurringExpense();
        expense.setUserId(userId);
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setTags(request.getTags());
        expense.setRecurrencePattern(request.getRecurrencePattern());
        expense.setStartDate(request.getStartDate());
        expense.setEndDate(request.getEndDate());
        expense.setDaysOfWeek(request.getDaysOfWeek());
        expense.setDaysOfMonth(request.getDaysOfMonth());
        expense.setDayOfMonth(request.getDayOfMonth());
        expense.setMonthOfYear(request.getMonthOfYear());
        expense.setBiWeeklyReferenceDate(request.getBiWeeklyReferenceDate());
        expense.setCustomIntervalValue(request.getCustomIntervalValue());
        expense.setCustomIntervalUnit(request.getCustomIntervalUnit());
        expense.setIsActive(request.getIsActive());

        RecurringExpense saved = recurringExpenseRepository.save(expense);
        return mapToResponse(saved);
    }

    public List<RecurringExpenseResponse> getAllRecurringExpenses(Long userId) {
        List<RecurringExpense> expenses = recurringExpenseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RecurringExpenseResponse> getActiveRecurringExpenses(Long userId) {
        List<RecurringExpense> expenses = recurringExpenseRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true);
        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RecurringExpenseResponse getRecurringExpenseById(Long userId, Long id) {
        RecurringExpense expense = recurringExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));
        return mapToResponse(expense);
    }

    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(Long userId, Long id, RecurringExpenseRequest request) {
        validateRecurringExpenseRequest(request);

        RecurringExpense expense = recurringExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));

        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setTags(request.getTags());
        expense.setRecurrencePattern(request.getRecurrencePattern());
        expense.setStartDate(request.getStartDate());
        expense.setEndDate(request.getEndDate());
        expense.setDaysOfWeek(request.getDaysOfWeek());
        expense.setDaysOfMonth(request.getDaysOfMonth());
        expense.setDayOfMonth(request.getDayOfMonth());
        expense.setMonthOfYear(request.getMonthOfYear());
        expense.setBiWeeklyReferenceDate(request.getBiWeeklyReferenceDate());
        expense.setCustomIntervalValue(request.getCustomIntervalValue());
        expense.setCustomIntervalUnit(request.getCustomIntervalUnit());
        expense.setIsActive(request.getIsActive());

        RecurringExpense updated = recurringExpenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteRecurringExpense(Long userId, Long id) {
        if (!recurringExpenseRepository.findByIdAndUserId(id, userId).isPresent()) {
            throw new ResourceNotFoundException("Recurring expense not found");
        }
        recurringExpenseRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public RecurringExpenseResponse toggleActiveStatus(Long userId, Long id) {
        RecurringExpense expense = recurringExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));

        expense.setIsActive(!expense.getIsActive());
        RecurringExpense updated = recurringExpenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Transactional
    public void generateIncidentalExpenseFromRecurring(Long userId, Long recurringExpenseId) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findByIdAndUserId(recurringExpenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));

        if (!recurringExpense.getIsActive()) {
            throw new BadRequestException("Cannot generate from inactive recurring expense");
        }

        LocalDate generationDate = LocalDate.now();

        if (recurringExpense.getEndDate() != null && generationDate.isAfter(recurringExpense.getEndDate())) {
            throw new BadRequestException("Recurring expense has ended");
        }

        createIncidentalExpenseFromRecurring(recurringExpense, generationDate);

        recurringExpense.setLastGeneratedDate(generationDate);
        recurringExpenseRepository.save(recurringExpense);
    }

    @Transactional
    public void generateAllDueRecurringExpenses() {
        LocalDate today = LocalDate.now();
        List<RecurringExpense> dueExpenses = recurringExpenseRepository.findActiveDueForGeneration(today);

        log.info("Found {} recurring expenses due for generation", dueExpenses.size());

        for (RecurringExpense recurringExpense : dueExpenses) {
            try {
                List<LocalDate> datesToGenerate = calculateGenerationDates(recurringExpense, today);

                for (LocalDate date : datesToGenerate) {
                    createIncidentalExpenseFromRecurring(recurringExpense, date);
                }

                recurringExpense.setLastGeneratedDate(today);
                recurringExpenseRepository.save(recurringExpense);

                log.info("Generated {} incidental expenses from recurring expense ID: {}",
                         datesToGenerate.size(), recurringExpense.getId());
            } catch (Exception e) {
                log.error("Error generating from recurring expense ID: {}", recurringExpense.getId(), e);
            }
        }
    }

    private List<LocalDate> calculateGenerationDates(RecurringExpense expense, LocalDate currentDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate lastGenerated = expense.getLastGeneratedDate();
        LocalDate startFrom = lastGenerated != null ? lastGenerated.plusDays(1) : expense.getStartDate();

        if (startFrom.isAfter(currentDate)) {
            return dates;
        }

        switch (expense.getRecurrencePattern()) {
            case DAILY:
                dates.addAll(generateDailyDates(startFrom, currentDate));
                break;
            case WEEKLY:
                dates.addAll(generateWeeklyDates(startFrom, currentDate, expense.getDaysOfWeek()));
                break;
            case BI_WEEKLY:
                dates.addAll(generateBiWeeklyDates(startFrom, currentDate, expense.getBiWeeklyReferenceDate()));
                break;
            case MONTHLY:
                dates.addAll(generateMonthlyDates(startFrom, currentDate, expense.getDaysOfMonth()));
                break;
            case YEARLY:
                dates.addAll(generateYearlyDates(startFrom, currentDate,
                        expense.getMonthOfYear(), expense.getDayOfMonth()));
                break;
            case CUSTOM_INTERVAL:
                dates.addAll(generateCustomIntervalDates(startFrom, currentDate,
                        expense.getCustomIntervalValue(), expense.getCustomIntervalUnit()));
                break;
        }

        return dates.stream()
                .filter(date -> !date.isAfter(currentDate))
                .filter(date -> expense.getEndDate() == null || !date.isAfter(expense.getEndDate()))
                .collect(Collectors.toList());
    }

    private List<LocalDate> generateDailyDates(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private List<LocalDate> generateWeeklyDates(LocalDate start, LocalDate end, String daysOfWeekStr) {
        List<LocalDate> dates = new ArrayList<>();
        if (daysOfWeekStr == null || daysOfWeekStr.isEmpty()) {
            return dates;
        }

        List<Integer> daysOfWeek = Arrays.stream(daysOfWeekStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        LocalDate current = start;
        while (!current.isAfter(end)) {
            int dayOfWeek = current.getDayOfWeek().getValue();
            if (daysOfWeek.contains(dayOfWeek)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private List<LocalDate> generateBiWeeklyDates(LocalDate start, LocalDate end, LocalDate referenceDate) {
        List<LocalDate> dates = new ArrayList<>();
        if (referenceDate == null) {
            referenceDate = start;
        }

        LocalDate current = start;
        while (!current.isAfter(end)) {
            long weeksDiff = ChronoUnit.WEEKS.between(referenceDate, current);
            if (weeksDiff % 2 == 0 && current.getDayOfWeek().equals(referenceDate.getDayOfWeek())) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private List<LocalDate> generateMonthlyDates(LocalDate start, LocalDate end, String daysOfMonthStr) {
        List<LocalDate> dates = new ArrayList<>();
        if (daysOfMonthStr == null || daysOfMonthStr.isEmpty()) {
            return dates;
        }

        List<Integer> daysOfMonth = Arrays.stream(daysOfMonthStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (daysOfMonth.contains(current.getDayOfMonth())) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private List<LocalDate> generateYearlyDates(LocalDate start, LocalDate end, Integer month, Integer day) {
        List<LocalDate> dates = new ArrayList<>();
        if (month == null || day == null) {
            return dates;
        }

        int startYear = start.getYear();
        int endYear = end.getYear();

        for (int year = startYear; year <= endYear; year++) {
            try {
                LocalDate date = LocalDate.of(year, month, day);
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    dates.add(date);
                }
            } catch (Exception e) {
                log.warn("Invalid date: year={}, month={}, day={}", year, month, day);
            }
        }
        return dates;
    }

    private List<LocalDate> generateCustomIntervalDates(LocalDate start, LocalDate end,
                                                         Integer intervalValue, IntervalUnit intervalUnit) {
        List<LocalDate> dates = new ArrayList<>();
        if (intervalValue == null || intervalUnit == null) {
            return dates;
        }

        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            switch (intervalUnit) {
                case DAYS:
                    current = current.plusDays(intervalValue);
                    break;
                case WEEKS:
                    current = current.plusWeeks(intervalValue);
                    break;
                case MONTHS:
                    current = current.plusMonths(intervalValue);
                    break;
            }
        }
        return dates;
    }

    private void createIncidentalExpenseFromRecurring(RecurringExpense recurringExpense, LocalDate date) {
        IncidentalExpense incidentalExpense = new IncidentalExpense();
        incidentalExpense.setUserId(recurringExpense.getUserId());
        incidentalExpense.setAmount(recurringExpense.getAmount());
        incidentalExpense.setCategory(recurringExpense.getCategory());
        incidentalExpense.setDescription(recurringExpense.getDescription());
        incidentalExpense.setExpenseDate(date);
        incidentalExpense.setPaymentMethod(recurringExpense.getPaymentMethod());
        incidentalExpense.setTags(recurringExpense.getTags());
        incidentalExpense.setAiSuggestionAccepted(false); // Auto-generated, no AI involved

        incidentalExpenseRepository.save(incidentalExpense);
    }

    private RecurringExpenseResponse mapToResponse(RecurringExpense expense) {
        RecurringExpenseResponse response = new RecurringExpenseResponse();
        response.setId(expense.getId());
        response.setUserId(expense.getUserId());
        response.setAmount(expense.getAmount());
        response.setCategory(expense.getCategory());
        response.setDescription(expense.getDescription());
        response.setPaymentMethod(expense.getPaymentMethod());
        response.setTags(expense.getTags());
        response.setRecurrencePattern(expense.getRecurrencePattern());
        response.setStartDate(expense.getStartDate());
        response.setEndDate(expense.getEndDate());
        response.setDaysOfWeek(expense.getDaysOfWeek());
        response.setDaysOfMonth(expense.getDaysOfMonth());
        response.setDayOfMonth(expense.getDayOfMonth());
        response.setMonthOfYear(expense.getMonthOfYear());
        response.setBiWeeklyReferenceDate(expense.getBiWeeklyReferenceDate());
        response.setCustomIntervalValue(expense.getCustomIntervalValue());
        response.setCustomIntervalUnit(expense.getCustomIntervalUnit());
        response.setIsActive(expense.getIsActive());
        response.setLastGeneratedDate(expense.getLastGeneratedDate());
        response.setCreatedAt(expense.getCreatedAt());
        response.setUpdatedAt(expense.getUpdatedAt());

        // Calculate next generation date and status
        LocalDate nextDate = calculateNextGenerationDate(expense);
        response.setNextGenerationDate(nextDate);
        response.setGenerationStatus(calculateGenerationStatus(expense, nextDate));

        return response;
    }

    private LocalDate calculateNextGenerationDate(RecurringExpense expense) {
        if (!expense.getIsActive()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastGenerated = expense.getLastGeneratedDate();
        LocalDate startFrom = lastGenerated != null ? lastGenerated.plusDays(1) : expense.getStartDate();

        if (expense.getEndDate() != null && today.isAfter(expense.getEndDate())) {
            return null;
        }

        List<LocalDate> futureDates = calculateGenerationDates(expense, startFrom.plusYears(1));
        return futureDates.stream()
                .filter(date -> date.isAfter(today) || date.isEqual(today))
                .findFirst()
                .orElse(null);
    }

    private String calculateGenerationStatus(RecurringExpense expense, LocalDate nextDate) {
        if (!expense.getIsActive()) {
            return "Paused";
        }
        if (expense.getEndDate() != null && LocalDate.now().isAfter(expense.getEndDate())) {
            return "Ended";
        }
        if (nextDate == null) {
            return "No upcoming";
        }
        if (nextDate.isEqual(LocalDate.now())) {
            return "Due today";
        }
        return "Scheduled";
    }

    private void validateRecurringExpenseRequest(RecurringExpenseRequest request) {
        RecurrencePattern pattern = request.getRecurrencePattern();

        if (pattern == RecurrencePattern.WEEKLY &&
            (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty())) {
            throw new BadRequestException("Days of week required for WEEKLY pattern");
        }

        if (pattern == RecurrencePattern.MONTHLY &&
            (request.getDaysOfMonth() == null || request.getDaysOfMonth().isEmpty())) {
            throw new BadRequestException("Days of month required for MONTHLY pattern");
        }

        if (pattern == RecurrencePattern.YEARLY &&
            (request.getDayOfMonth() == null || request.getMonthOfYear() == null)) {
            throw new BadRequestException("Day and month required for YEARLY pattern");
        }

        if (pattern == RecurrencePattern.BI_WEEKLY && request.getBiWeeklyReferenceDate() == null) {
            throw new BadRequestException("Reference date required for BI_WEEKLY pattern");
        }

        if (pattern == RecurrencePattern.CUSTOM_INTERVAL &&
            (request.getCustomIntervalValue() == null || request.getCustomIntervalUnit() == null)) {
            throw new BadRequestException("Interval value and unit required for CUSTOM_INTERVAL pattern");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }
}
