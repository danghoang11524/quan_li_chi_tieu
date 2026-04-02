import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Chip,
  Typography,
  Tabs,
  Tab,
  Grid,
  FormControl,
  InputLabel,
  Select,
  Snackbar,
  Alert,
  Tooltip,
  CircularProgress,
  Card,
  CardContent,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Refresh as RefreshIcon,
  AutoAwesome as AIIcon,
} from '@mui/icons-material';
import { recurringExpenseAPI, incidentalExpenseAPI } from '../services/api';

const EXPENSE_CATEGORIES = [
  { value: 'DINING', label: 'Ăn uống' },
  { value: 'TRANSPORTATION', label: 'Di chuyển' },
  { value: 'ENTERTAINMENT', label: 'Giải trí' },
  { value: 'SHOPPING', label: 'Mua sắm' },
  { value: 'BILLS', label: 'Hóa đơn' },
  { value: 'HEALTHCARE', label: 'Y tế' },
  { value: 'EDUCATION', label: 'Giáo dục' },
  { value: 'OTHER', label: 'Khác' },
];


const RECURRENCE_PATTERNS = [
  { value: 'DAILY', label: 'Hàng ngày' },
  { value: 'WEEKLY', label: 'Hàng tuần' },
  { value: 'BI_WEEKLY', label: 'Hai tuần một lần' },
  { value: 'MONTHLY', label: 'Hàng tháng' },
  { value: 'YEARLY', label: 'Hàng năm' },
  { value: 'CUSTOM_INTERVAL', label: 'Tùy chỉnh' },
];

const INTERVAL_UNITS = [
  { value: 'DAYS', label: 'Ngày' },
  { value: 'WEEKS', label: 'Tuần' },
  { value: 'MONTHS', label: 'Tháng' },
];

const DAYS_OF_WEEK = [
  { value: 1, label: 'Thứ Hai' },
  { value: 2, label: 'Thứ Ba' },
  { value: 3, label: 'Thứ Tư' },
  { value: 4, label: 'Thứ Năm' },
  { value: 5, label: 'Thứ Sáu' },
  { value: 6, label: 'Thứ Bảy' },
  { value: 7, label: 'Chủ Nhật' },
];

// Helper function to translate confidence levels
const getConfidenceLabel = (confidence) => {
  const confidenceMap = {
    'HIGH': 'Cao',
    'MEDIUM': 'Trung bình',
    'LOW': 'Thấp'
  };
  return confidenceMap[confidence] || confidence;
};

function ExpenseManager() {
  const [tabValue, setTabValue] = useState(0);

  // Recurring Expense State
  const [recurringExpenses, setRecurringExpenses] = useState([]);
  const [recurringPage, setRecurringPage] = useState(0);
  const [recurringRowsPerPage, setRecurringRowsPerPage] = useState(10);
  const [recurringDialogOpen, setRecurringDialogOpen] = useState(false);
  const [editingRecurring, setEditingRecurring] = useState(null);

  // Incidental Expense State
  const [incidentalExpenses, setIncidentalExpenses] = useState([]);
  const [incidentalPage, setIncidentalPage] = useState(0);
  const [incidentalRowsPerPage, setIncidentalRowsPerPage] = useState(10);
  const [incidentalDialogOpen, setIncidentalDialogOpen] = useState(false);
  const [editingIncidental, setEditingIncidental] = useState(null);

  // Filter State
  const [filterCategory, setFilterCategory] = useState('ALL');
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');
  const [filterSearch, setFilterSearch] = useState('');

  // AI State
  const [aiSuggestion, setAiSuggestion] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);

  // Form State
  const [formData, setFormData] = useState(getInitialFormData());
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  useEffect(() => {
    loadData();
  }, [tabValue]);

  // Debounced AI categorization
  useEffect(() => {
    if (incidentalDialogOpen && formData.description && formData.description.length > 3) {
      const timeoutId = setTimeout(() => {
        getAISuggestion(formData.description);
      }, 800); // Debounce 800ms

      return () => clearTimeout(timeoutId);
    }
  }, [formData.description, incidentalDialogOpen]);

  function getInitialFormData() {
    return {
      amount: '',
      category: 'DINING',
      description: '',
      // For Recurring
      recurrencePattern: 'MONTHLY',
      startDate: new Date().toISOString().split('T')[0],
      endDate: '',
      daysOfWeek: [],
      daysOfMonth: '',
      dayOfMonth: '',
      monthOfYear: '',
      biWeeklyReferenceDate: '',
      customIntervalValue: '',
      customIntervalUnit: 'DAYS',
      isActive: true,
      // For Incidental
      expenseDate: new Date().toISOString().split('T')[0],
      aiSuggestedCategory: '',
      aiSuggestedType: '',
      aiSuggestionAccepted: false,
    };
  }

  const loadData = async () => {
    try {
      if (tabValue === 0) {
        const response = await incidentalExpenseAPI.getAll();
        setIncidentalExpenses(response.data.data || response.data || []);
      } else {
        const response = await recurringExpenseAPI.getAll();
        setRecurringExpenses(response.data.data || response.data || []);
      }
    } catch (error) {
      showSnackbar('Lỗi tải dữ liệu: ' + error.message, 'error');
    }
  };

  const getAISuggestion = async (description) => {
    setAiLoading(true);
    try {
      const response = await incidentalExpenseAPI.aiCategorize(description);
      const suggestion = response.data.data || response.data;
      setAiSuggestion(suggestion);

      // Tự động điền danh mục do AI đề xuất
      if (suggestion && suggestion.suggestedCategory) {
        setFormData(prev => ({
          ...prev,
          category: suggestion.suggestedCategory,
          aiSuggestedCategory: suggestion.suggestedCategory,
          aiSuggestedType: suggestion.suggestedType,
          aiSuggestionAccepted: true,
        }));
      }
    } catch (error) {
      console.error('AI categorization error:', error);
      setAiSuggestion(null);
    } finally {
      setAiLoading(false);
    }
  };

  const handleAcceptAISuggestion = () => {
    if (aiSuggestion) {
      setFormData(prev => ({
        ...prev,
        category: aiSuggestion.suggestedCategory,
        aiSuggestedCategory: aiSuggestion.suggestedCategory,
        aiSuggestedType: aiSuggestion.suggestedType,
        aiSuggestionAccepted: true,
      }));
      showSnackbar('Đã áp dụng gợi ý AI!', 'success');
    }
  };

  const handleOpenRecurringDialog = (expense = null) => {
    if (expense) {
      setEditingRecurring(expense);
      setFormData({
        ...expense,
        daysOfWeek: expense.daysOfWeek ? expense.daysOfWeek.split(',').map(Number) : [],
        startDate: expense.startDate || '',
        endDate: expense.endDate || '',
        biWeeklyReferenceDate: expense.biWeeklyReferenceDate || '',
      });
    } else {
      setEditingRecurring(null);
      setFormData(getInitialFormData());
    }
    setRecurringDialogOpen(true);
  };

  const handleOpenIncidentalDialog = (expense = null) => {
    setAiSuggestion(null);
    if (expense) {
      setEditingIncidental(expense);
      setFormData(expense);
    } else {
      setEditingIncidental(null);
      setFormData(getInitialFormData());
    }
    setIncidentalDialogOpen(true);
  };

  const handleCloseDialogs = () => {
    setRecurringDialogOpen(false);
    setIncidentalDialogOpen(false);
    setEditingRecurring(null);
    setEditingIncidental(null);
    setFormData(getInitialFormData());
    setAiSuggestion(null);
  };

  const handleSaveRecurring = async () => {
    try {
      const payload = {
        amount: formData.amount,
        category: formData.category,
        description: formData.description,
        paymentMethod: null,
        tags: null,
        recurrencePattern: formData.recurrencePattern,
        startDate: formData.startDate,
        endDate: formData.endDate || null,
        daysOfWeek: formData.daysOfWeek.length > 0 ? formData.daysOfWeek.join(',') : null,
        daysOfMonth: formData.daysOfMonth || null,
        dayOfMonth: formData.dayOfMonth ? parseInt(formData.dayOfMonth) : null,
        monthOfYear: formData.monthOfYear ? parseInt(formData.monthOfYear) : null,
        biWeeklyReferenceDate: formData.biWeeklyReferenceDate || null,
        customIntervalValue: formData.customIntervalValue ? parseInt(formData.customIntervalValue) : null,
        customIntervalUnit: formData.customIntervalUnit || null,
        isActive: formData.isActive,
      };

      if (editingRecurring) {
        await recurringExpenseAPI.update(editingRecurring.id, payload);
        showSnackbar('Cập nhật chi phí định kỳ thành công');
      } else {
        await recurringExpenseAPI.create(payload);
        showSnackbar('Tạo chi phí định kỳ thành công');
      }

      handleCloseDialogs();
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const handleSaveIncidental = async () => {
    try {
      const payload = { ...formData };

      if (editingIncidental) {
        await incidentalExpenseAPI.update(editingIncidental.id, payload);
        showSnackbar('Cập nhật chi phí phát sinh thành công');
      } else {
        await incidentalExpenseAPI.create(payload);
        showSnackbar('Tạo chi phí phát sinh thành công');
      }

      handleCloseDialogs();
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const handleDeleteRecurring = async (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa chi phí định kỳ này?')) {
      try {
        await recurringExpenseAPI.delete(id);
        showSnackbar('Xóa thành công');
        loadData();
      } catch (error) {
        showSnackbar('Lỗi xóa: ' + error.message, 'error');
      }
    }
  };

  const handleDeleteIncidental = async (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa chi phí này?')) {
      try {
        await incidentalExpenseAPI.delete(id);
        showSnackbar('Xóa thành công');
        loadData();
      } catch (error) {
        showSnackbar('Lỗi xóa: ' + error.message, 'error');
      }
    }
  };

  const handleToggleActive = async (id) => {
    try {
      await recurringExpenseAPI.toggle(id);
      showSnackbar('Đã chuyển trạng thái');
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const handleGenerateNow = async (id) => {
    try {
      await recurringExpenseAPI.generate(id);
      showSnackbar('Đã tạo chi phí phát sinh');
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const showSnackbar = (message, severity = 'success') => {
    setSnackbar({ open: true, message, severity });
  };

  const formatCurrency = (amount) => {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫';
  };

  const formatCurrencyInput = (value) => {
    // Remove all non-digit characters
    const number = value.replace(/\D/g, '');
    // Format with thousand separators
    return number.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  };

  const parseCurrencyInput = (value) => {
    // Remove all non-digit characters to get the actual number
    const numStr = value.replace(/\D/g, '');
    return numStr ? parseFloat(numStr) : '';
  };

  const formatDate = (date) => {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('vi-VN');
  };

  const getCategoryLabel = (value) => {
    const category = EXPENSE_CATEGORIES.find(c => c.value === value);
    return category ? category.label : value;
  };

  // Filter functions
  const filterIncidentalExpenses = () => {
    return incidentalExpenses.filter(expense => {
      // Filter by category
      if (filterCategory !== 'ALL' && expense.category !== filterCategory) {
        return false;
      }

      // Filter by date range
      if (filterStartDate && new Date(expense.expenseDate) < new Date(filterStartDate)) {
        return false;
      }
      if (filterEndDate && new Date(expense.expenseDate) > new Date(filterEndDate)) {
        return false;
      }

      // Filter by search text
      if (filterSearch) {
        const searchLower = filterSearch.toLowerCase();
        const descMatch = expense.description?.toLowerCase().includes(searchLower);
        const categoryMatch = getCategoryLabel(expense.category).toLowerCase().includes(searchLower);
        if (!descMatch && !categoryMatch) {
          return false;
        }
      }

      return true;
    });
  };

  const filterRecurringExpenses = () => {
    return recurringExpenses.filter(expense => {
      // Filter by category
      if (filterCategory !== 'ALL' && expense.category !== filterCategory) {
        return false;
      }

      // Filter by search text
      if (filterSearch) {
        const searchLower = filterSearch.toLowerCase();
        const descMatch = expense.description?.toLowerCase().includes(searchLower);
        const categoryMatch = getCategoryLabel(expense.category).toLowerCase().includes(searchLower);
        if (!descMatch && !categoryMatch) {
          return false;
        }
      }

      return true;
    });
  };

  const handleClearFilters = () => {
    setFilterCategory('ALL');
    setFilterStartDate('');
    setFilterEndDate('');
    setFilterSearch('');
  };

  const getPatternLabel = (value) => {
    const pattern = RECURRENCE_PATTERNS.find(p => p.value === value);
    return pattern ? pattern.label : value;
  };

  const getStatusLabel = (status) => {
    const statusMap = {
      'Scheduled': 'Đã lên lịch',
      'Paused': 'Tạm dừng',
      'Due today': 'Đến hạn hôm nay',
      'Completed': 'Hoàn thành'
    };
    return statusMap[status] || status;
  };

  const renderPatternFields = () => {
    switch (formData.recurrencePattern) {
      case 'WEEKLY':
        return (
          <FormControl fullWidth margin="normal">
            <InputLabel>Các ngày trong tuần</InputLabel>
            <Select
              multiple
              value={formData.daysOfWeek}
              onChange={(e) => setFormData({ ...formData, daysOfWeek: e.target.value })}
              renderValue={(selected) =>
                selected.map(v => DAYS_OF_WEEK.find(d => d.value === v)?.label).join(', ')
              }
            >
              {DAYS_OF_WEEK.map((day) => (
                <MenuItem key={day.value} value={day.value}>
                  {day.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        );

      case 'BI_WEEKLY':
        return (
          <TextField
            fullWidth
            margin="normal"
            label="Ngày tham chiếu"
            type="date"
            value={formData.biWeeklyReferenceDate}
            onChange={(e) => setFormData({ ...formData, biWeeklyReferenceDate: e.target.value })}
            InputLabelProps={{ shrink: true }}
            helperText="Ngày đầu tiên của lịch hai tuần một lần"
          />
        );

      case 'MONTHLY':
        return (
          <TextField
            fullWidth
            margin="normal"
            label="Các ngày trong tháng (phân cách bằng dấu phẩy)"
            value={formData.daysOfMonth}
            onChange={(e) => setFormData({ ...formData, daysOfMonth: e.target.value })}
            placeholder="Ví dụ: 1,15 cho ngày 1 và 15"
            helperText="Nhập các số từ 1-31, phân cách bằng dấu phẩy"
          />
        );

      case 'YEARLY':
        return (
          <>
            <TextField
              fullWidth
              margin="normal"
              label="Tháng (1-12)"
              type="number"
              value={formData.monthOfYear}
              onChange={(e) => setFormData({ ...formData, monthOfYear: e.target.value })}
              inputProps={{ min: 1, max: 12 }}
            />
            <TextField
              fullWidth
              margin="normal"
              label="Ngày trong tháng (1-31)"
              type="number"
              value={formData.dayOfMonth}
              onChange={(e) => setFormData({ ...formData, dayOfMonth: e.target.value })}
              inputProps={{ min: 1, max: 31 }}
            />
          </>
        );

      case 'CUSTOM_INTERVAL':
        return (
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <TextField
                fullWidth
                margin="normal"
                label="Giá trị khoảng"
                type="number"
                value={formData.customIntervalValue}
                onChange={(e) => setFormData({ ...formData, customIntervalValue: e.target.value })}
                inputProps={{ min: 1 }}
              />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth margin="normal">
                <InputLabel>Đơn vị</InputLabel>
                <Select
                  value={formData.customIntervalUnit}
                  onChange={(e) => setFormData({ ...formData, customIntervalUnit: e.target.value })}
                >
                  {INTERVAL_UNITS.map((unit) => (
                    <MenuItem key={unit.value} value={unit.value}>
                      {unit.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        );

      default:
        return null;
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Quản Lý Chi Phí
      </Typography>

      <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} sx={{ mb: 3 }}>
        <Tab label="Chi Phí Phát Sinh" />
        <Tab label="Chi Phí Định Kỳ" />
      </Tabs>

      {/* Tab 0: Incidental Expenses */}
      {tabValue === 0 && (
        <Paper sx={{ p: 2 }}>
          <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Chi Phí Phát Sinh</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpenIncidentalDialog()}
            >
              Thêm Chi Phí Phát Sinh
            </Button>
          </Box>

          {/* Bộ lọc */}
          <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  fullWidth
                  size="small"
                  label="Tìm kiếm"
                  placeholder="Mô tả hoặc danh mục..."
                  value={filterSearch}
                  onChange={(e) => setFilterSearch(e.target.value)}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth size="small">
                  <InputLabel>Danh mục</InputLabel>
                  <Select
                    value={filterCategory}
                    onChange={(e) => setFilterCategory(e.target.value)}
                    label="Danh mục"
                  >
                    <MenuItem value="ALL">Tất cả</MenuItem>
                    {EXPENSE_CATEGORIES.map((cat) => (
                      <MenuItem key={cat.value} value={cat.value}>
                        {cat.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField
                  fullWidth
                  size="small"
                  type="date"
                  label="Từ ngày"
                  value={filterStartDate}
                  onChange={(e) => setFilterStartDate(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField
                  fullWidth
                  size="small"
                  type="date"
                  label="Đến ngày"
                  value={filterEndDate}
                  onChange={(e) => setFilterEndDate(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} sm={12} md={2}>
                <Button
                  fullWidth
                  variant="outlined"
                  onClick={handleClearFilters}
                  sx={{ height: '40px' }}
                >
                  Xóa bộ lọc
                </Button>
              </Grid>
            </Grid>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Ngày</TableCell>
                  <TableCell>Danh mục</TableCell>
                  <TableCell>Mô tả</TableCell>
                  <TableCell align="right">Số tiền</TableCell>
                  <TableCell align="center">Hành động</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filterIncidentalExpenses()
                  .slice(
                    incidentalPage * incidentalRowsPerPage,
                    incidentalPage * incidentalRowsPerPage + incidentalRowsPerPage
                  )
                  .map((expense) => (
                    <TableRow key={expense.id}>
                      <TableCell>{formatDate(expense.expenseDate)}</TableCell>
                      <TableCell>{getCategoryLabel(expense.category)}</TableCell>
                      <TableCell>{expense.description || '-'}</TableCell>
                      <TableCell align="right">{formatCurrency(expense.amount)}</TableCell>
                      <TableCell align="center">
                        <IconButton onClick={() => handleOpenIncidentalDialog(expense)} size="small">
                          <EditIcon />
                        </IconButton>
                        <IconButton onClick={() => handleDeleteIncidental(expense.id)} size="small">
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>

          <TablePagination
            component="div"
            count={filterIncidentalExpenses().length}
            page={incidentalPage}
            onPageChange={(e, newPage) => setIncidentalPage(newPage)}
            rowsPerPage={incidentalRowsPerPage}
            onRowsPerPageChange={(e) => {
              setIncidentalRowsPerPage(parseInt(e.target.value, 10));
              setIncidentalPage(0);
            }}
            labelRowsPerPage="Số dòng mỗi trang:"
          />
        </Paper>
      )}

      {/* Tab 1: Recurring Expenses */}
      {tabValue === 1 && (
        <Paper sx={{ p: 2 }}>
          <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Lịch Chi Phí Định Kỳ</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpenRecurringDialog()}
            >
              Thêm Chi Phí Định Kỳ
            </Button>
          </Box>

          {/* Bộ lọc */}
          <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={5}>
                <TextField
                  fullWidth
                  size="small"
                  label="Tìm kiếm"
                  placeholder="Mô tả hoặc danh mục..."
                  value={filterSearch}
                  onChange={(e) => setFilterSearch(e.target.value)}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={5}>
                <FormControl fullWidth size="small">
                  <InputLabel>Danh mục</InputLabel>
                  <Select
                    value={filterCategory}
                    onChange={(e) => setFilterCategory(e.target.value)}
                    label="Danh mục"
                  >
                    <MenuItem value="ALL">Tất cả</MenuItem>
                    {EXPENSE_CATEGORIES.map((cat) => (
                      <MenuItem key={cat.value} value={cat.value}>
                        {cat.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={12} md={2}>
                <Button
                  fullWidth
                  variant="outlined"
                  onClick={handleClearFilters}
                  sx={{ height: '40px' }}
                >
                  Xóa bộ lọc
                </Button>
              </Grid>
            </Grid>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Danh mục</TableCell>
                  <TableCell>Mô tả</TableCell>
                  <TableCell align="right">Số tiền</TableCell>
                  <TableCell>Chu kỳ</TableCell>
                  <TableCell>Ngày bắt đầu</TableCell>
                  <TableCell>Lần tạo tiếp theo</TableCell>
                  <TableCell>Trạng thái</TableCell>
                  <TableCell align="center">Hành động</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filterRecurringExpenses()
                  .slice(
                    recurringPage * recurringRowsPerPage,
                    recurringPage * recurringRowsPerPage + recurringRowsPerPage
                  )
                  .map((expense) => (
                    <TableRow key={expense.id}>
                      <TableCell>{getCategoryLabel(expense.category)}</TableCell>
                      <TableCell>{expense.description || '-'}</TableCell>
                      <TableCell align="right">{formatCurrency(expense.amount)}</TableCell>
                      <TableCell>{getPatternLabel(expense.recurrencePattern)}</TableCell>
                      <TableCell>{formatDate(expense.startDate)}</TableCell>
                      <TableCell>{formatDate(expense.nextGenerationDate)}</TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusLabel(expense.generationStatus)}
                          color={
                            expense.generationStatus === 'Scheduled' ? 'success' :
                            expense.generationStatus === 'Paused' ? 'warning' :
                            expense.generationStatus === 'Due today' ? 'error' : 'default'
                          }
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title={expense.isActive ? 'Tạm dừng' : 'Kích hoạt'}>
                          <IconButton onClick={() => handleToggleActive(expense.id)} size="small">
                            {expense.isActive ? <PauseIcon /> : <PlayIcon />}
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Tạo ngay">
                          <IconButton onClick={() => handleGenerateNow(expense.id)} size="small">
                            <RefreshIcon />
                          </IconButton>
                        </Tooltip>
                        <IconButton onClick={() => handleOpenRecurringDialog(expense)} size="small">
                          <EditIcon />
                        </IconButton>
                        <IconButton onClick={() => handleDeleteRecurring(expense.id)} size="small">
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>

          <TablePagination
            component="div"
            count={filterRecurringExpenses().length}
            page={recurringPage}
            onPageChange={(e, newPage) => setRecurringPage(newPage)}
            rowsPerPage={recurringRowsPerPage}
            onRowsPerPageChange={(e) => {
              setRecurringRowsPerPage(parseInt(e.target.value, 10));
              setRecurringPage(0);
            }}
            labelRowsPerPage="Số dòng mỗi trang:"
          />
        </Paper>
      )}

      {/* Recurring Expense Dialog */}
      <Dialog open={recurringDialogOpen} onClose={handleCloseDialogs} maxWidth="md" fullWidth>
        <DialogTitle>{editingRecurring ? 'Sửa Chi Phí Định Kỳ' : 'Thêm Chi Phí Định Kỳ'}</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            margin="normal"
            label="Số tiền (VNĐ)"
            value={formData.amount ? formatCurrencyInput(formData.amount.toString()) : ''}
            onChange={(e) => {
              const rawValue = parseCurrencyInput(e.target.value);
              setFormData({ ...formData, amount: rawValue });
            }}
            placeholder="Ví dụ: 1,000,000"
            helperText="Nhập số tiền theo định dạng Việt Nam (VD: 1 triệu = 1,000,000)"
            required
          />

          <TextField
            fullWidth
            margin="normal"
            label="Mô tả"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          />

          <FormControl fullWidth margin="normal">
            <InputLabel>Danh mục</InputLabel>
            <Select
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value })}
            >
              {EXPENSE_CATEGORIES.map((cat) => (
                <MenuItem key={cat.value} value={cat.value}>
                  {cat.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl fullWidth margin="normal">
            <InputLabel>Chu kỳ lặp lại</InputLabel>
            <Select
              value={formData.recurrencePattern}
              onChange={(e) => setFormData({ ...formData, recurrencePattern: e.target.value })}
            >
              {RECURRENCE_PATTERNS.map((pattern) => (
                <MenuItem key={pattern.value} value={pattern.value}>
                  {pattern.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {renderPatternFields()}

          <TextField
            fullWidth
            margin="normal"
            label="Ngày bắt đầu"
            type="date"
            value={formData.startDate}
            onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
            InputLabelProps={{ shrink: true }}
            required
          />

          <TextField
            fullWidth
            margin="normal"
            label="Ngày kết thúc (Tùy chọn)"
            type="date"
            value={formData.endDate}
            onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
            InputLabelProps={{ shrink: true }}
            helperText="Để trống nếu không có ngày kết thúc"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialogs}>Hủy</Button>
          <Button onClick={handleSaveRecurring} variant="contained">
            {editingRecurring ? 'Cập nhật' : 'Tạo'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Incidental Expense Dialog */}
      <Dialog open={incidentalDialogOpen} onClose={handleCloseDialogs} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingIncidental ? 'Sửa Chi Phí Phát Sinh' : 'Thêm Chi Phí Phát Sinh'}
        </DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            margin="normal"
            label="Số tiền (VNĐ)"
            value={formData.amount ? formatCurrencyInput(formData.amount.toString()) : ''}
            onChange={(e) => {
              const rawValue = parseCurrencyInput(e.target.value);
              setFormData({ ...formData, amount: rawValue });
            }}
            placeholder="Ví dụ: 50,000"
            helperText="Nhập số tiền (VD: 50 nghìn = 50,000)"
            required
          />

          <TextField
            fullWidth
            margin="normal"
            label="Mô tả"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="Ví dụ: Cà phê Starbucks, Grab đi làm..."
            helperText="AI sẽ tự động gợi ý danh mục dựa trên mô tả"
          />

          {/* AI Suggestion Card */}
          {aiLoading && (
            <Box sx={{ display: 'flex', alignItems: 'center', my: 2 }}>
              <CircularProgress size={20} sx={{ mr: 1 }} />
              <Typography variant="body2">Đang phân tích bằng AI...</Typography>
            </Box>
          )}

          {aiSuggestion && !aiLoading && (
            <Card sx={{ my: 2, bgcolor: 'primary.light', color: 'primary.contrastText' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <AIIcon sx={{ mr: 1 }} />
                  <Typography variant="h6">Gợi ý từ AI</Typography>
                </Box>
                <Typography variant="body2">
                  <strong>Danh mục:</strong> {getCategoryLabel(aiSuggestion.suggestedCategory)}
                </Typography>
                <Typography variant="body2">
                  <strong>Loại:</strong> {aiSuggestion.suggestedType === 'RECURRING' ? 'Định kỳ' : 'Phát sinh'}
                </Typography>
                <Typography variant="body2">
                  <strong>Độ tin cậy:</strong> {getConfidenceLabel(aiSuggestion.confidence)}
                </Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>
                  <em>{aiSuggestion.reasoning}</em>
                </Typography>
                <Button
                  variant="contained"
                  size="small"
                  startIcon={<AIIcon />}
                  onClick={handleAcceptAISuggestion}
                  sx={{ mt: 2 }}
                >
                  Áp dụng gợi ý
                </Button>
              </CardContent>
            </Card>
          )}

          <FormControl fullWidth margin="normal">
            <InputLabel>Danh mục</InputLabel>
            <Select
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value })}
            >
              {EXPENSE_CATEGORIES.map((cat) => (
                <MenuItem key={cat.value} value={cat.value}>
                  {cat.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            fullWidth
            margin="normal"
            label="Ngày chi tiêu"
            type="date"
            value={formData.expenseDate}
            onChange={(e) => setFormData({ ...formData, expenseDate: e.target.value })}
            InputLabelProps={{ shrink: true }}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialogs}>Hủy</Button>
          <Button onClick={handleSaveIncidental} variant="contained">
            {editingIncidental ? 'Cập nhật' : 'Tạo'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}

export default ExpenseManager;
