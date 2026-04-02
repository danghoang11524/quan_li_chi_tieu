package com.expense.transaction.service;

import com.expense.transaction.dto.FixedIncomeRequest;
import com.expense.transaction.dto.FixedIncomeResponse;
import com.expense.transaction.dto.SupplementaryIncomeRequest;
import com.expense.transaction.entity.*;
import com.expense.common.exception.BadRequestException;
import com.expense.common.exception.ResourceNotFoundException;
import com.expense.transaction.repository.FixedIncomeRepository;
import com.expense.transaction.repository.SupplementaryIncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedIncomeService {

    private final FixedIncomeRepository fixedIncomeRepository;
    private final SupplementaryIncomeRepository supplementaryIncomeRepository;

    @Transactional
    public FixedIncomeResponse createFixedIncome(Long userId, FixedIncomeRequest request) {
        validateFixedIncomeRequest(request);

        FixedIncome fixedIncome = new FixedIncome();
        fixedIncome.setUserId(userId);
        fixedIncome.setAmount(request.getAmount());
        fixedIncome.setCategory(request.getCategory());
        fixedIncome.setDescription(request.getDescription());
        fixedIncome.setPaymentMethod(request.getPaymentMethod());
        fixedIncome.setTags(request.getTags());
        fixedIncome.setRecurrencePattern(request.getRecurrencePattern());
        fixedIncome.setStartDate(request.getStartDate());
        fixedIncome.setEndDate(request.getEndDate());
        fixedIncome.setDaysOfWeek(request.getDaysOfWeek());
        fixedIncome.setDaysOfMonth(request.getDaysOfMonth());
        fixedIncome.setDayOfMonth(request.getDayOfMonth());
        fixedIncome.setMonthOfYear(request.getMonthOfYear());
        fixedIncome.setBiWeeklyReferenceDate(request.getBiWeeklyReferenceDate());
        fixedIncome.setCustomIntervalValue(request.getCustomIntervalValue());
        fixedIncome.setCustomIntervalUnit(request.getCustomIntervalUnit());
        fixedIncome.setIsActive(request.getIsActive());

        FixedIncome saved = fixedIncomeRepository.save(fixedIncome);
        return mapToResponse(saved);
    }

    public List<FixedIncomeResponse> getAllFixedIncome(Long userId) {
        List<FixedIncome> incomes = fixedIncomeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<FixedIncomeResponse> getActiveFixedIncome(Long userId) {
        List<FixedIncome> incomes = fixedIncomeRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true);
        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FixedIncomeResponse getFixedIncomeById(Long userId, Long id) {
        FixedIncome fixedIncome = fixedIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed income not found"));
        return mapToResponse(fixedIncome);
    }

    @Transactional
    public FixedIncomeResponse updateFixedIncome(Long userId, Long id, FixedIncomeRequest request) {
        validateFixedIncomeRequest(request);

        FixedIncome fixedIncome = fixedIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed income not found"));

        fixedIncome.setAmount(request.getAmount());
        fixedIncome.setCategory(request.getCategory());
        fixedIncome.setDescription(request.getDescription());
        fixedIncome.setPaymentMethod(request.getPaymentMethod());
        fixedIncome.setTags(request.getTags());
        fixedIncome.setRecurrencePattern(request.getRecurrencePattern());
        fixedIncome.setStartDate(request.getStartDate());
        fixedIncome.setEndDate(request.getEndDate());
        fixedIncome.setDaysOfWeek(request.getDaysOfWeek());
        fixedIncome.setDaysOfMonth(request.getDaysOfMonth());
        fixedIncome.setDayOfMonth(request.getDayOfMonth());
        fixedIncome.setMonthOfYear(request.getMonthOfYear());
        fixedIncome.setBiWeeklyReferenceDate(request.getBiWeeklyReferenceDate());
        fixedIncome.setCustomIntervalValue(request.getCustomIntervalValue());
        fixedIncome.setCustomIntervalUnit(request.getCustomIntervalUnit());
        fixedIncome.setIsActive(request.getIsActive());

        FixedIncome updated = fixedIncomeRepository.save(fixedIncome);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteFixedIncome(Long userId, Long id) {
        if (!fixedIncomeRepository.findByIdAndUserId(id, userId).isPresent()) {
            throw new ResourceNotFoundException("Fixed income not found");
        }
        fixedIncomeRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public FixedIncomeResponse toggleActiveStatus(Long userId, Long id) {
        FixedIncome fixedIncome = fixedIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed income not found"));

        fixedIncome.setIsActive(!fixedIncome.getIsActive());
        FixedIncome updated = fixedIncomeRepository.save(fixedIncome);
        return mapToResponse(updated);
    }

    @Transactional
    public void generateSupplementaryIncomeFromFixed(Long userId, Long fixedIncomeId) {
        FixedIncome fixedIncome = fixedIncomeRepository.findByIdAndUserId(fixedIncomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed income not found"));

        if (!fixedIncome.getIsActive()) {
            throw new BadRequestException("Cannot generate from inactive fixed income");
        }

        LocalDate generationDate = LocalDate.now();

        if (fixedIncome.getEndDate() != null && generationDate.isAfter(fixedIncome.getEndDate())) {
            throw new BadRequestException("Fixed income has ended");
        }

        createSupplementaryIncomeFromFixed(fixedIncome, generationDate);

        fixedIncome.setLastGeneratedDate(generationDate);
        fixedIncomeRepository.save(fixedIncome);
    }

    @Transactional
    public void generateAllDueFixedIncome() {
        LocalDate today = LocalDate.now();
        List<FixedIncome> dueIncomes = fixedIncomeRepository.findActiveDueForGeneration(today);

        log.info("Found {} fixed income entries due for generation", dueIncomes.size());

        for (FixedIncome fixedIncome : dueIncomes) {
            try {
                List<LocalDate> datesToGenerate = calculateGenerationDates(fixedIncome, today);

                for (LocalDate date : datesToGenerate) {
                    createSupplementaryIncomeFromFixed(fixedIncome, date);
                }

                fixedIncome.setLastGeneratedDate(today);
                fixedIncomeRepository.save(fixedIncome);

                log.info("Generated {} supplementary income entries from fixed income ID: {}",
                         datesToGenerate.size(), fixedIncome.getId());
            } catch (Exception e) {
                log.error("Error generating from fixed income ID: {}", fixedIncome.getId(), e);
            }
        }
    }

    private List<LocalDate> calculateGenerationDates(FixedIncome fixedIncome, LocalDate currentDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate lastGenerated = fixedIncome.getLastGeneratedDate();
        LocalDate startDate = lastGenerated != null ? lastGenerated.plusDays(1) : fixedIncome.getStartDate();

        if (startDate.isAfter(currentDate)) {
            return dates;
        }

        switch (fixedIncome.getRecurrencePattern()) {
            case DAILY:
                dates.addAll(generateDailyDates(startDate, currentDate));
                break;
            case WEEKLY:
                dates.addAll(generateWeeklyDates(startDate, currentDate, fixedIncome.getDaysOfWeek()));
                break;
            case BI_WEEKLY:
                dates.addAll(generateBiWeeklyDates(startDate, currentDate, fixedIncome.getBiWeeklyReferenceDate()));
                break;
            case MONTHLY:
                dates.addAll(generateMonthlyDates(startDate, currentDate, fixedIncome.getDaysOfMonth()));
                break;
            case YEARLY:
                dates.addAll(generateYearlyDates(startDate, currentDate,
                        fixedIncome.getMonthOfYear(), fixedIncome.getDayOfMonth()));
                break;
            case CUSTOM_INTERVAL:
                dates.addAll(generateCustomIntervalDates(startDate, currentDate,
                        fixedIncome.getCustomIntervalValue(), fixedIncome.getCustomIntervalUnit()));
                break;
        }

        return dates.stream()
                .filter(date -> !date.isAfter(currentDate))
                .filter(date -> fixedIncome.getEndDate() == null || !date.isAfter(fixedIncome.getEndDate()))
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

    private void createSupplementaryIncomeFromFixed(FixedIncome fixedIncome, LocalDate date) {
        SupplementaryIncome supplementaryIncome = new SupplementaryIncome();
        supplementaryIncome.setUserId(fixedIncome.getUserId());
        supplementaryIncome.setAmount(fixedIncome.getAmount());
        supplementaryIncome.setCategory(fixedIncome.getCategory());
        supplementaryIncome.setDescription(fixedIncome.getDescription());
        supplementaryIncome.setIncomeDate(date);
        supplementaryIncome.setPaymentMethod(fixedIncome.getPaymentMethod());
        supplementaryIncome.setTags(fixedIncome.getTags());

        supplementaryIncomeRepository.save(supplementaryIncome);
    }

    private FixedIncomeResponse mapToResponse(FixedIncome fixedIncome) {
        FixedIncomeResponse response = new FixedIncomeResponse();
        response.setId(fixedIncome.getId());
        response.setUserId(fixedIncome.getUserId());
        response.setAmount(fixedIncome.getAmount());
        response.setCategory(fixedIncome.getCategory());
        response.setDescription(fixedIncome.getDescription());
        response.setPaymentMethod(fixedIncome.getPaymentMethod());
        response.setTags(fixedIncome.getTags());
        response.setRecurrencePattern(fixedIncome.getRecurrencePattern());
        response.setStartDate(fixedIncome.getStartDate());
        response.setEndDate(fixedIncome.getEndDate());
        response.setDaysOfWeek(fixedIncome.getDaysOfWeek());
        response.setDaysOfMonth(fixedIncome.getDaysOfMonth());
        response.setDayOfMonth(fixedIncome.getDayOfMonth());
        response.setMonthOfYear(fixedIncome.getMonthOfYear());
        response.setBiWeeklyReferenceDate(fixedIncome.getBiWeeklyReferenceDate());
        response.setCustomIntervalValue(fixedIncome.getCustomIntervalValue());
        response.setCustomIntervalUnit(fixedIncome.getCustomIntervalUnit());
        response.setIsActive(fixedIncome.getIsActive());
        response.setLastGeneratedDate(fixedIncome.getLastGeneratedDate());
        response.setCreatedAt(fixedIncome.getCreatedAt());
        response.setUpdatedAt(fixedIncome.getUpdatedAt());

        // Calculate next generation date and status
        LocalDate nextDate = calculateNextGenerationDate(fixedIncome);
        response.setNextGenerationDate(nextDate);
        response.setGenerationStatus(calculateGenerationStatus(fixedIncome, nextDate));

        return response;
    }

    private LocalDate calculateNextGenerationDate(FixedIncome fixedIncome) {
        if (!fixedIncome.getIsActive()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastGenerated = fixedIncome.getLastGeneratedDate();
        LocalDate startFrom = lastGenerated != null ? lastGenerated.plusDays(1) : fixedIncome.getStartDate();

        if (fixedIncome.getEndDate() != null && today.isAfter(fixedIncome.getEndDate())) {
            return null;
        }

        List<LocalDate> futureDates = calculateGenerationDates(fixedIncome, startFrom.plusYears(1));
        return futureDates.stream()
                .filter(date -> date.isAfter(today) || date.isEqual(today))
                .findFirst()
                .orElse(null);
    }

    private String calculateGenerationStatus(FixedIncome fixedIncome, LocalDate nextDate) {
        if (!fixedIncome.getIsActive()) {
            return "Paused";
        }
        if (fixedIncome.getEndDate() != null && LocalDate.now().isAfter(fixedIncome.getEndDate())) {
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

    private void validateFixedIncomeRequest(FixedIncomeRequest request) {
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
