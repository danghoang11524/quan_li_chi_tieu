import React, { useState, useEffect } from 'react';
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
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { fixedIncomeAPI, supplementaryIncomeAPI, aiAPI } from '../services/api';

const INCOME_CATEGORIES = [
  { value: 'SALARY', label: 'Lương' },
  { value: 'BONUS', label: 'Thưởng' },
  { value: 'INVESTMENT', label: 'Đầu tư' },
  { value: 'BUSINESS', label: 'Kinh doanh' },
  { value: 'OTHER', label: 'Thu nhập khác' },
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

function FixedIncome() {
  const [tabValue, setTabValue] = useState(0);

  // Fixed Income State
  const [fixedIncomes, setFixedIncomes] = useState([]);
  const [fixedIncomePage, setFixedIncomePage] = useState(0);
  const [fixedIncomeRowsPerPage, setFixedIncomeRowsPerPage] = useState(10);
  const [fixedIncomeDialogOpen, setFixedIncomeDialogOpen] = useState(false);
  const [editingFixedIncome, setEditingFixedIncome] = useState(null);

  // Supplementary Income State
  const [supplementaryIncomes, setSupplementaryIncomes] = useState([]);
  const [suppIncomePage, setSuppIncomePage] = useState(0);
  const [suppIncomeRowsPerPage, setSuppIncomeRowsPerPage] = useState(10);
  const [suppIncomeDialogOpen, setSuppIncomeDialogOpen] = useState(false);
  const [editingSupplementaryIncome, setEditingSupplementaryIncome] = useState(null);

  // Filter State
  const [filterCategory, setFilterCategory] = useState('ALL');
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');
  const [filterSearch, setFilterSearch] = useState('');

  // Form State
  const [formData, setFormData] = useState(getInitialFormData());
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  useEffect(() => {
    loadData();
  }, [tabValue]);

  function getInitialFormData() {
    return {
      amount: '',
      category: 'SALARY',
      description: '',
      // For Fixed Income
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
      // For Supplementary Income
      incomeDate: new Date().toISOString().split('T')[0],
    };
  }

  const loadData = async () => {
    try {
      if (tabValue === 0) {
        const response = await supplementaryIncomeAPI.getAll();
        setSupplementaryIncomes(response.data.data || response.data);
      } else {
        const response = await fixedIncomeAPI.getAll();
        setFixedIncomes(response.data.data || response.data);
      }
    } catch (error) {
      showSnackbar('Lỗi tải dữ liệu: ' + error.message, 'error');
    }
  };

  const handleOpenFixedIncomeDialog = (income = null) => {
    if (income) {
      setEditingFixedIncome(income);
      setFormData({
        ...income,
        daysOfWeek: income.daysOfWeek ? income.daysOfWeek.split(',').map(Number) : [],
        startDate: income.startDate || '',
        endDate: income.endDate || '',
        biWeeklyReferenceDate: income.biWeeklyReferenceDate || '',
      });
    } else {
      setEditingFixedIncome(null);
      setFormData(getInitialFormData());
    }
    setFixedIncomeDialogOpen(true);
  };

  const handleOpenSuppIncomeDialog = (income = null) => {
    if (income) {
      setEditingSupplementaryIncome(income);
      setFormData(income);
    } else {
      setEditingSupplementaryIncome(null);
      setFormData(getInitialFormData());
    }
    setSuppIncomeDialogOpen(true);
  };

  const handleCloseDialogs = () => {
    setFixedIncomeDialogOpen(false);
    setSuppIncomeDialogOpen(false);
    setEditingFixedIncome(null);
    setEditingSupplementaryIncome(null);
    setFormData(getInitialFormData());
  };

  const handleSaveFixedIncome = async () => {
    try {
      const payload = {
        amount: formData.amount,
        category: formData.category,
        description: formData.description,
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

      if (editingFixedIncome) {
        await fixedIncomeAPI.update(editingFixedIncome.id, payload);
        showSnackbar('Cập nhật nguồn thu định kỳ thành công');
      } else {
        await fixedIncomeAPI.create(payload);
        showSnackbar('Tạo nguồn thu định kỳ thành công');
      }

      handleCloseDialogs();
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const handleSaveSupplementaryIncome = async () => {
    try {
      const payload = {
        amount: formData.amount,
        category: formData.category,
        description: formData.description,
        incomeDate: formData.incomeDate,
      };

      if (editingSupplementaryIncome) {
        await supplementaryIncomeAPI.update(editingSupplementaryIncome.id, payload);
        showSnackbar('Cập nhật thu nhập phát sinh thành công');
      } else {
        await supplementaryIncomeAPI.create(payload);
        showSnackbar('Tạo thu nhập phát sinh thành công');
      }

      handleCloseDialogs();
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const handleDeleteFixedIncome = async (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa nguồn thu định kỳ này?')) {
      try {
        await fixedIncomeAPI.delete(id);
        showSnackbar('Xóa thành công');
        loadData();
      } catch (error) {
        showSnackbar('Lỗi: ' + error.message, 'error');
      }
    }
  };

  const handleDeleteSupplementaryIncome = async (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa thu nhập phát sinh này?')) {
      try {
        await supplementaryIncomeAPI.delete(id);
        showSnackbar('Xóa thành công');
        loadData();
      } catch (error) {
        showSnackbar('Lỗi: ' + error.message, 'error');
      }
    }
  };

  const handleToggleActive = async (id) => {
    try {
      await fixedIncomeAPI.toggle(id);
      showSnackbar('Đã chuyển trạng thái');
      loadData();
    } catch (error) {
      showSnackbar('Lỗi: ' + error.message, 'error');
    }
  };

  const handleGenerateNow = async (id) => {
    try {
      await fixedIncomeAPI.generate(id);
      showSnackbar('Đã tạo thu nhập');
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
    const category = INCOME_CATEGORIES.find(c => c.value === value);
    return category ? category.label : value;
  };

  // Filter functions
  const filterSupplementaryIncomes = () => {
    return supplementaryIncomes.filter(income => {
      // Filter by category
      if (filterCategory !== 'ALL' && income.category !== filterCategory) {
        return false;
      }

      // Filter by date range
      if (filterStartDate && new Date(income.incomeDate) < new Date(filterStartDate)) {
        return false;
      }
      if (filterEndDate && new Date(income.incomeDate) > new Date(filterEndDate)) {
        return false;
      }

      // Filter by search text
      if (filterSearch) {
        const searchLower = filterSearch.toLowerCase();
        const descMatch = income.description?.toLowerCase().includes(searchLower);
        const categoryMatch = getCategoryLabel(income.category).toLowerCase().includes(searchLower);
        if (!descMatch && !categoryMatch) {
          return false;
        }
      }

      return true;
    });
  };

  const filterFixedIncomes = () => {
    return fixedIncomes.filter(income => {
      // Filter by category
      if (filterCategory !== 'ALL' && income.category !== filterCategory) {
        return false;
      }

      // Filter by search text
      if (filterSearch) {
        const searchLower = filterSearch.toLowerCase();
        const sourceMatch = income.source?.toLowerCase().includes(searchLower);
        const categoryMatch = getCategoryLabel(income.category).toLowerCase().includes(searchLower);
        if (!sourceMatch && !categoryMatch) {
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
        Quản Lý Thu Nhập
      </Typography>

      <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} sx={{ mb: 3 }}>
        <Tab label="Thu Nhập Phát Sinh" />
        <Tab label="Thu Nhập Định Kỳ" />
      </Tabs>

      {/* Tab 0: Supplementary Income */}
      {tabValue === 0 && (
        <Paper sx={{ p: 2 }}>
          <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Thu Nhập Phát Sinh</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpenSuppIncomeDialog()}
            >
              Thêm Thu Nhập Phát Sinh
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
                    {INCOME_CATEGORIES.map((cat) => (
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
                {filterSupplementaryIncomes()
                  .slice(
                    suppIncomePage * suppIncomeRowsPerPage,
                    suppIncomePage * suppIncomeRowsPerPage + suppIncomeRowsPerPage
                  )
                  .map((income) => (
                    <TableRow key={income.id}>
                      <TableCell>{formatDate(income.incomeDate)}</TableCell>
                      <TableCell>{getCategoryLabel(income.category)}</TableCell>
                      <TableCell>{income.description || '-'}</TableCell>
                      <TableCell align="right">{formatCurrency(income.amount)}</TableCell>
                      <TableCell align="center">
                        <IconButton onClick={() => handleOpenSuppIncomeDialog(income)} size="small">
                          <EditIcon />
                        </IconButton>
                        <IconButton onClick={() => handleDeleteSupplementaryIncome(income.id)} size="small">
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
            count={filterSupplementaryIncomes().length}
            page={suppIncomePage}
            onPageChange={(e, newPage) => setSuppIncomePage(newPage)}
            rowsPerPage={suppIncomeRowsPerPage}
            onRowsPerPageChange={(e) => {
              setSuppIncomeRowsPerPage(parseInt(e.target.value, 10));
              setSuppIncomePage(0);
            }}
            labelRowsPerPage="Số dòng mỗi trang:"
          />
        </Paper>
      )}

      {/* Tab 1: Fixed Income */}
      {tabValue === 1 && (
        <Paper sx={{ p: 2 }}>
          <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Lịch Thu Nhập Định Kỳ</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpenFixedIncomeDialog()}
            >
              Thêm Thu Nhập Định Kỳ
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
                  placeholder="Nguồn thu hoặc danh mục..."
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
                    {INCOME_CATEGORIES.map((cat) => (
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
                {filterFixedIncomes()
                  .slice(
                    fixedIncomePage * fixedIncomeRowsPerPage,
                    fixedIncomePage * fixedIncomeRowsPerPage + fixedIncomeRowsPerPage
                  )
                  .map((income) => (
                    <TableRow key={income.id}>
                      <TableCell>{getCategoryLabel(income.category)}</TableCell>
                      <TableCell>{income.description || '-'}</TableCell>
                      <TableCell align="right">{formatCurrency(income.amount)}</TableCell>
                      <TableCell>{getPatternLabel(income.recurrencePattern)}</TableCell>
                      <TableCell>{formatDate(income.startDate)}</TableCell>
                      <TableCell>{formatDate(income.nextGenerationDate)}</TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusLabel(income.generationStatus)}
                          color={
                            income.generationStatus === 'Scheduled' ? 'success' :
                            income.generationStatus === 'Paused' ? 'warning' :
                            income.generationStatus === 'Due today' ? 'error' : 'default'
                          }
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title={income.isActive ? 'Tạm dừng' : 'Kích hoạt'}>
                          <IconButton onClick={() => handleToggleActive(income.id)} size="small">
                            {income.isActive ? <PauseIcon /> : <PlayIcon />}
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Tạo ngay">
                          <IconButton onClick={() => handleGenerateNow(income.id)} size="small">
                            <RefreshIcon />
                          </IconButton>
                        </Tooltip>
                        <IconButton onClick={() => handleOpenFixedIncomeDialog(income)} size="small">
                          <EditIcon />
                        </IconButton>
                        <IconButton onClick={() => handleDeleteFixedIncome(income.id)} size="small">
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
            count={filterFixedIncomes().length}
            page={fixedIncomePage}
            onPageChange={(e, newPage) => setFixedIncomePage(newPage)}
            rowsPerPage={fixedIncomeRowsPerPage}
            onRowsPerPageChange={(e) => {
              setFixedIncomeRowsPerPage(parseInt(e.target.value, 10));
              setFixedIncomePage(0);
            }}
            labelRowsPerPage="Số dòng mỗi trang:"
          />
        </Paper>
      )}

      {/* Fixed Income Dialog */}
      <Dialog open={fixedIncomeDialogOpen} onClose={handleCloseDialogs} maxWidth="md" fullWidth>
        <DialogTitle>{editingFixedIncome ? 'Sửa Thu Nhập Định Kỳ' : 'Thêm Thu Nhập Định Kỳ'}</DialogTitle>
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
              {INCOME_CATEGORIES.map((cat) => (
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
          <Button onClick={handleSaveFixedIncome} variant="contained">
            {editingFixedIncome ? 'Cập nhật' : 'Tạo'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Supplementary Income Dialog */}
      <Dialog open={suppIncomeDialogOpen} onClose={handleCloseDialogs} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingSupplementaryIncome ? 'Sửa Thu Nhập Phát Sinh' : 'Thêm Thu Nhập Phát Sinh'}
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
              {INCOME_CATEGORIES.map((cat) => (
                <MenuItem key={cat.value} value={cat.value}>
                  {cat.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            fullWidth
            margin="normal"
            label="Ngày thu nhập"
            type="date"
            value={formData.incomeDate}
            onChange={(e) => setFormData({ ...formData, incomeDate: e.target.value })}
            InputLabelProps={{ shrink: true }}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialogs}>Hủy</Button>
          <Button onClick={handleSaveSupplementaryIncome} variant="contained">
            {editingSupplementaryIncome ? 'Cập nhật' : 'Tạo'}
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

export default FixedIncome;
