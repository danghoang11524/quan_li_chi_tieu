import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Chip,
  Alert,
  Snackbar,
  Divider,
  Collapse,
  CircularProgress,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Cancel as CancelIcon,
  Savings as SavingsIcon,
  TrendingUp as TrendingUpIcon,
  EmojiEvents as TrophyIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  Psychology as PsychologyIcon,
} from '@mui/icons-material';
import { savingsAPI, recurringTransactionAPI, aiAPI, transactionAPI, fixedIncomeAPI, supplementaryIncomeAPI, recurringExpenseAPI, incidentalExpenseAPI } from '../services/api';

const STATUS_COLORS = {
  ACTIVE: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'error',
  OVERDUE: 'warning',
};

const STATUS_LABELS = {
  ACTIVE: 'Đang tiết kiệm',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  OVERDUE: 'Quá hạn',
};

const SavingsGoals = () => {
  const [goals, setGoals] = useState([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [openContributeDialog, setOpenContributeDialog] = useState(false);
  const [editingGoal, setEditingGoal] = useState(null);
  const [selectedGoal, setSelectedGoal] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    targetAmount: '',
    targetDate: '',
  });

  const [contributeAmount, setContributeAmount] = useState('');

  // UI collapse/expand state
  const [expandedGoals, setExpandedGoals] = useState({});

  // AI analysis state
  const [aiAnalysis, setAiAnalysis] = useState(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  // Tab state cho dialog
  const [dialogTab, setDialogTab] = useState(0);

  // Tab state cho trang chính
  const [mainTab, setMainTab] = useState(0);

  useEffect(() => {
    loadGoals();
  }, []);

  // Auto-trigger AI analysis when user switches to AI tab
  useEffect(() => {
    // Only auto-analyze for new goals (not editing)
    if (editingGoal) return;

    // Check if user is on AI tab and all required fields are filled
    if (dialogTab !== 1 || !formData.name || !formData.targetAmount || !formData.targetDate || !openDialog) {
      return;
    }

    // Tự động reset và phân tích lại MỖI KHI:
    // 1. Chuyển sang tab AI (dialogTab thay đổi)
    // 2. Thông tin mục tiêu thay đổi (name, targetAmount, targetDate)
    // Điều này đảm bảo luôn có dữ liệu mới nhất sau khi user sửa thông tin
    if (!isAnalyzing) {
      setAiAnalysis(null); // Reset để trigger phân tích mới
      analyzeGoalWithAI();
    }
  }, [dialogTab, formData.name, formData.targetAmount, formData.targetDate]);

  const loadGoals = async () => {
    try {
      const response = await savingsAPI.getAll();
      // Sắp xếp mục tiêu mới nhất lên đầu (theo createdAt giảm dần)
      const sortedGoals = (response.data.data || []).sort((a, b) => {
        const dateA = new Date(a.createdAt || 0);
        const dateB = new Date(b.createdAt || 0);
        return dateB - dateA; // Mới nhất trước
      });
      setGoals(sortedGoals);
    } catch (error) {
      showSnackbar('Lỗi khi tải mục tiêu', 'error');
    }
  };

  const handleOpenDialog = (goal = null) => {
    if (goal) {
      setEditingGoal(goal);
      setFormData({
        name: goal.name,
        description: goal.description || '',
        targetAmount: goal.targetAmount,
        targetDate: goal.targetDate,
      });
    } else {
      setEditingGoal(null);
      const nextMonth = new Date();
      nextMonth.setMonth(nextMonth.getMonth() + 1);
      setFormData({
        name: '',
        description: '',
        targetAmount: '',
        targetDate: nextMonth.toISOString().split('T')[0],
      });
    }
    setDialogTab(0); // Reset về tab đầu tiên
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingGoal(null);
    setAiAnalysis(null); // Reset AI analysis khi đóng dialog
  };

  const handleSubmit = async () => {
    // Kiểm tra tính khả thi TRƯỚC KHI thực hiện bất kỳ thao tác nào
    if (!editingGoal && aiAnalysis && aiAnalysis.feasibility === 'NOT_FEASIBLE') {
      showSnackbar('❌ Không thể thêm mục tiêu: Mục tiêu này nằm ngoài khả năng tài chính của bạn!', 'error');
      return; // Dừng lại ngay, không thực hiện API call
    }

    try {
      const data = {
        ...formData,
        targetAmount: parseFloat(formData.targetAmount),
      };

      if (editingGoal) {
        await savingsAPI.update(editingGoal.id, data);
        showSnackbar('Cập nhật mục tiêu thành công!', 'success');
      } else {
        await savingsAPI.create(data);
        showSnackbar('Thêm mục tiêu thành công!', 'success');
      }

      handleCloseDialog();
      loadGoals();
    } catch (error) {
      showSnackbar('Lỗi khi lưu mục tiêu', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa mục tiêu này?')) {
      try {
        await savingsAPI.delete(id);
        showSnackbar('Xóa mục tiêu thành công!', 'success');
        loadGoals();
      } catch (error) {
        showSnackbar('Lỗi khi xóa mục tiêu', 'error');
      }
    }
  };

  const handleCancel = async (id) => {
    if (window.confirm('Bạn có chắc muốn hủy mục tiêu này?')) {
      try {
        await savingsAPI.cancel(id);
        showSnackbar('Đã hủy mục tiêu!', 'success');
        loadGoals();
      } catch (error) {
        showSnackbar('Lỗi khi hủy mục tiêu', 'error');
      }
    }
  };

  const handleOpenContributeDialog = (goal) => {
    setSelectedGoal(goal);
    setContributeAmount('');
    setOpenContributeDialog(true);
  };

  const handleContribute = async () => {
    try {
      await savingsAPI.addContribution(selectedGoal.id, {
        amount: parseFloat(contributeAmount),
        notes: 'Đóng góp vào heo đất',
      });
      showSnackbar('Đóng góp thành công!', 'success');
      setOpenContributeDialog(false);
      loadGoals();
    } catch (error) {
      showSnackbar('Lỗi khi đóng góp', 'error');
    }
  };

  const showSnackbar = (message, severity = 'success') => {
    setSnackbar({ open: true, message, severity });
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount); // Hiển thị số tiền thực tế không nhân 1000
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

  // Toggle expand/collapse cho một goal
  const handleToggleExpand = (goalId) => {
    setExpandedGoals(prev => ({
      ...prev,
      [goalId]: !prev[goalId]
    }));
  };

  // Phân tích AI cho mục tiêu mới
  const analyzeGoalWithAI = async () => {
    if (!formData.name || !formData.targetAmount || !formData.targetDate) {
      showSnackbar('Vui lòng điền đầy đủ thông tin mục tiêu', 'warning');
      return;
    }

    setIsAnalyzing(true);
    try {
      // Lấy dữ liệu thu chi thực tế từ Fixed/Supplementary Income và Recurring/Incidental Expenses
      const now = new Date();
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
      const startDate = firstDay.toISOString().split('T')[0];
      const endDate = lastDay.toISOString().split('T')[0];

      // Lấy tất cả dữ liệu song song - Thêm timestamp để tránh cache
      const timestamp = new Date().getTime();
      const [fixedIncomeRes, suppIncomeRes, recurringExpenseRes, incidentalExpenseRes] = await Promise.all([
        fixedIncomeAPI.getAll(),
        supplementaryIncomeAPI.getByDateRange(startDate, endDate),
        recurringExpenseAPI.getAll(),
        incidentalExpenseAPI.getByDateRange(startDate, endDate),
      ]);

      // Tính tổng thu nhập từ fixed income (active) và supplementary income trong tháng
      const fixedIncomes = (fixedIncomeRes.data || []).filter(fi => fi.isActive);
      const suppIncomes = suppIncomeRes.data || [];

      const totalFixedIncome = fixedIncomes.reduce((sum, fi) => sum + (fi.amount || 0), 0);
      const totalSuppIncome = suppIncomes.reduce((sum, si) => sum + (si.amount || 0), 0);
      const actualMonthlyIncome = totalFixedIncome + totalSuppIncome;

      // Tính tổng chi tiêu từ recurring expenses (active) và incidental expenses trong tháng
      const recurringExpensesData = (recurringExpenseRes.data || []).filter(re => re.isActive);
      const incidentalExpenses = incidentalExpenseRes.data || [];

      const totalRecurringExpense = recurringExpensesData.reduce((sum, re) => sum + (re.amount || 0), 0);
      const totalIncidentalExpense = incidentalExpenses.reduce((sum, ie) => sum + (ie.amount || 0), 0);
      const actualMonthlyExpense = totalRecurringExpense + totalIncidentalExpense;

      // Các mục tiêu hiện có
      const existingGoals = goals
        .filter(g => g.status === 'ACTIVE')
        .map(g => ({
          name: g.name,
          monthlyRequired: g.currentMonthRequiredAmount || 0
        }));

      // Tính số ngày còn lại
      const targetDate = new Date(formData.targetDate);
      const today = new Date();
      const daysRemaining = Math.ceil((targetDate - today) / (1000 * 60 * 60 * 24));

      // Tính "Tiết kiệm tháng này" cho mục tiêu mới
      const daysInMonth = lastDay.getDate();
      const currentDay = now.getDate();
      const daysUntilEndOfMonth = daysInMonth - currentDay + 1; // Bao gồm hôm nay
      const dailyRequired = formData.targetAmount / daysRemaining;
      const currentMonthRequired = dailyRequired * daysUntilEndOfMonth;

      // Tính tổng "Tiết kiệm tháng này" của các mục tiêu hiện có
      const totalExistingCurrentMonthRequired = goals
        .filter(g => g.status === 'ACTIVE')
        .reduce((sum, g) => sum + (g.currentMonthRequiredAmount || 0), 0);

      // Gọi API phân tích với dữ liệu đầy đủ
      const analysisResponse = await aiAPI.analyzeSavingsGoal({
        goalName: formData.name,
        targetAmount: parseFloat(formData.targetAmount),
        targetDate: formData.targetDate,
        daysRemaining: daysRemaining,
        recurringIncome: [],
        recurringExpenses: [],
        actualMonthlyIncome: actualMonthlyIncome,
        actualMonthlyExpense: actualMonthlyExpense,
        existingGoals: existingGoals
      });

      // Thêm thông tin "Tiết kiệm tháng này" vào kết quả phân tích
      const analysisData = analysisResponse.data.data;
      analysisData.currentMonthRequired = currentMonthRequired;
      analysisData.dailyRequired = dailyRequired;

      // Ghi đè "Tiết kiệm hiện tại/tháng" và "Còn dư/tháng" theo "Tiết kiệm tháng này"
      analysisData.monthlyExistingGoals = totalExistingCurrentMonthRequired;
      analysisData.monthlySurplus = actualMonthlyIncome - actualMonthlyExpense - totalExistingCurrentMonthRequired;

      setAiAnalysis(analysisData);
      showSnackbar('AI đã phân tích xong mục tiêu của bạn!', 'success');
    } catch (error) {
      console.error('Error analyzing goal:', error);
      showSnackbar('Không thể phân tích mục tiêu. Vui lòng thử lại.', 'error');
    } finally {
      setIsAnalyzing(false);
    }
  };

  // Filter goals theo tab
  const activeGoals = goals.filter(g => g.status === 'ACTIVE' || g.status === 'OVERDUE');
  const completedGoals = goals.filter(g => g.status === 'COMPLETED');
  const displayedGoals = mainTab === 0 ? activeGoals : completedGoals;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Heo Đất Ảo 🐷
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Tiết kiệm cho mục tiêu của bạn
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Thêm Mục Tiêu
        </Button>
      </Box>

      {/* Tabs để chọn xem mục tiêu đang tiết kiệm hay đã hoàn thành */}
      <Tabs value={mainTab} onChange={(e, newValue) => setMainTab(newValue)} sx={{ mb: 2 }}>
        <Tab
          label={`Đang Tiết Kiệm (${activeGoals.length})`}
          icon={<SavingsIcon />}
          iconPosition="start"
        />
        <Tab
          label={`Đã Hoàn Thành (${completedGoals.length})`}
          icon={<TrophyIcon />}
          iconPosition="start"
        />
      </Tabs>

      {displayedGoals.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          {mainTab === 0 ? (
            <>
              <SavingsIcon sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                Chưa có mục tiêu đang tiết kiệm
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Tạo heo đất ảo để bắt đầu tiết kiệm cho mục tiêu của bạn
              </Typography>
            </>
          ) : (
            <>
              <TrophyIcon sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                Chưa có mục tiêu nào hoàn thành
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Hãy cố gắng hoàn thành mục tiêu tiết kiệm của bạn
              </Typography>
            </>
          )}
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Tạo Heo Đất Đầu Tiên
          </Button>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          {displayedGoals.map((goal) => {
            const percentage = Math.min(goal.percentageCompleted || 0, 100);
            const isCompleted = goal.status === 'COMPLETED';
            const isActive = goal.status === 'ACTIVE';

            return (
              <Grid item xs={12} sm={6} md={4} key={goal.id}>
                <Card>
                  <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                    {/* Header: Tên + Actions */}
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                      <Box sx={{ flex: 1 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {goal.name}
                          </Typography>
                          {isCompleted && <TrophyIcon fontSize="small" color="warning" />}
                        </Box>
                        <Box>
                          <Chip
                            label={STATUS_LABELS[goal.status]}
                            color={STATUS_COLORS[goal.status]}
                            size="small"
                            sx={{ height: 20, fontSize: '0.7rem' }}
                          />
                        </Box>
                      </Box>
                      <Box>
                        <IconButton
                          size="small"
                          onClick={() => handleToggleExpand(goal.id)}
                        >
                          {expandedGoals[goal.id] ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                        </IconButton>
                        {isActive && (
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => handleOpenDialog(goal)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        )}
                        {isActive && (
                          <IconButton
                            size="small"
                            color="warning"
                            onClick={() => handleCancel(goal.id)}
                          >
                            <CancelIcon fontSize="small" />
                          </IconButton>
                        )}
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(goal.id)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>

                    {/* Progress Bar - Always visible */}
                    <Box sx={{ mb: 1.5 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          {formatCurrency(goal.currentAmount || 0)}
                        </Typography>
                        <Typography variant="caption" fontWeight="bold" color={isCompleted ? 'success.main' : 'primary.main'}>
                          {percentage.toFixed(1)}%
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatCurrency(goal.targetAmount)}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={percentage}
                        color={isCompleted ? 'success' : 'primary'}
                        sx={{ height: 6, borderRadius: 3 }}
                      />
                    </Box>

                    {/* Collapsible Details */}
                    <Collapse in={expandedGoals[goal.id]} timeout="auto" unmountOnExit>
                      {goal.description && (
                        <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                          {goal.description}
                        </Typography>
                      )}

                      <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                        Còn lại: {formatCurrency(goal.remainingAmount || 0)}
                      </Typography>

                      <Divider sx={{ my: 1 }} />

                      <Grid container spacing={1}>
                        <Grid item xs={12}>
                          <Paper sx={{ p: 1, bgcolor: 'success.light', color: 'success.contrastText' }}>
                            <Typography variant="caption" display="block" sx={{ fontSize: '0.65rem' }}>
                              Tiết kiệm tháng này
                            </Typography>
                            <Typography variant="body2" fontWeight="bold" sx={{ fontSize: '0.85rem' }}>
                              {formatCurrency(goal.currentMonthRequiredAmount || 0)}
                            </Typography>
                          </Paper>
                        </Grid>
                        <Grid item xs={12}>
                          <Paper sx={{ p: 1, bgcolor: 'primary.light', color: 'primary.contrastText' }}>
                            <Typography variant="caption" display="block" sx={{ fontSize: '0.65rem' }}>
                              Tiết kiệm mỗi ngày
                            </Typography>
                            <Typography variant="body2" fontWeight="bold" sx={{ fontSize: '0.85rem' }}>
                              {formatCurrency(goal.dailyRequiredAmount || 0)}
                            </Typography>
                          </Paper>
                        </Grid>
                      </Grid>

                      <Box sx={{ mt: 1 }}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.7rem' }}>
                          Hạn: {new Date(goal.targetDate).toLocaleDateString('vi-VN')}
                          {goal.daysRemaining !== undefined && ` (${goal.daysRemaining} ngày)`}
                        </Typography>
                      </Box>

                      {isActive && (
                        <Button
                          fullWidth
                          size="small"
                          variant="contained"
                          startIcon={<TrendingUpIcon fontSize="small" />}
                          onClick={() => handleOpenContributeDialog(goal)}
                          sx={{ mt: 1, py: 0.5 }}
                        >
                          Đóng Góp
                        </Button>
                      )}

                      {isCompleted && (
                        <Alert severity="success" sx={{ mt: 1, py: 0 }}>
                          <Typography variant="caption">
                            🎉 Hoàn thành mục tiêu!
                          </Typography>
                        </Alert>
                      )}
                    </Collapse>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Dialog Thêm/Sửa Mục Tiêu */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingGoal ? 'Chỉnh Sửa Mục Tiêu' :
           dialogTab === 0 ? 'Thêm Mục Tiêu Mới - Bước 1/2' :
           'Thêm Mục Tiêu Mới - Bước 2/2: Phân Tích AI'}
        </DialogTitle>
        <DialogContent>
          {/* Bước 1: Form nhập liệu */}
          {(editingGoal || dialogTab === 0) && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Tên Mục Tiêu"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="VD: Mua laptop mới, Du lịch Đà Lạt..."
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Mô Tả"
                  multiline
                  rows={2}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Mô tả ngắn về mục tiêu của bạn"
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Số Tiền Mục Tiêu (VNĐ)"
                  value={formData.targetAmount ? formatCurrencyInput(formData.targetAmount.toString()) : ''}
                  onChange={(e) => {
                    const rawValue = parseCurrencyInput(e.target.value);
                    setFormData({ ...formData, targetAmount: rawValue });
                  }}
                  placeholder="Ví dụ: 1,000,000"
                  helperText="Nhập số tiền theo định dạng Việt Nam (VD: 1 triệu = 1,000,000)"
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Ngày Hoàn Thành"
                  type="date"
                  value={formData.targetDate}
                  onChange={(e) => setFormData({ ...formData, targetDate: e.target.value })}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>

              {!editingGoal && (
                <Grid item xs={12}>
                  <Alert severity="info">
                    💡 Sau khi điền đầy đủ thông tin, click <strong>"Tiếp Theo"</strong> để xem phân tích AI về khả thi của mục tiêu!
                  </Alert>
                </Grid>
              )}
            </Grid>
          )}

          {/* Bước 2: Phân tích AI */}
          {!editingGoal && dialogTab === 1 && (
            <Box sx={{ mt: 1 }}>
              {/* Loading indicator for AI analysis */}
              {isAnalyzing && (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <CircularProgress size={60} sx={{ color: 'primary.main' }} />
                  <Typography variant="h6" sx={{ mt: 2, fontWeight: 600, color: 'text.primary' }}>
                    🤖 AI đang phân tích mục tiêu của bạn
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    Vui lòng đợi trong giây lát...
                  </Typography>
                </Box>
              )}

              {/* Hiển thị kết quả phân tích AI */}
              {aiAnalysis && !isAnalyzing && (
                <Box
                  sx={{
                    bgcolor: aiAnalysis.feasibility === 'FEASIBLE' ? '#e8f5e9' :
                             aiAnalysis.feasibility === 'NOT_FEASIBLE' ? '#ffebee' :
                             '#fff3e0',
                    p: 1.5,
                    borderRadius: 2,
                    border: '2px solid',
                    borderColor: aiAnalysis.feasibility === 'FEASIBLE' ? '#66bb6a' :
                                 aiAnalysis.feasibility === 'NOT_FEASIBLE' ? '#ef5350' :
                                 '#ffa726'
                  }}
                >
                  {/* Trạng thái khả thi - Hiển thị đầu tiên */}
                  <Paper
                    elevation={3}
                    sx={{
                      p: 2,
                      mb: 2,
                      bgcolor: aiAnalysis.feasibility === 'FEASIBLE' ? '#4caf50' :
                               aiAnalysis.feasibility === 'NOT_FEASIBLE' ? '#f44336' :
                               '#ff9800',
                      color: 'white',
                      textAlign: 'center',
                      borderRadius: 2
                    }}
                  >
                    <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5 }}>
                      {aiAnalysis.feasibility === 'FEASIBLE' && '✅ MỤC TIÊU KHẢ THI'}
                      {aiAnalysis.feasibility === 'NOT_FEASIBLE' && '❌ MỤC TIÊU KHÔNG KHẢ THI'}
                      {aiAnalysis.feasibility === 'CHALLENGING' && '⚠️ MỤC TIÊU THÁCH THỨC'}
                    </Typography>
                    <Typography variant="body2">
                      {aiAnalysis.feasibility === 'FEASIBLE' && 'Bạn có thể đạt được mục tiêu này với kế hoạch tài chính hiện tại'}
                      {aiAnalysis.feasibility === 'NOT_FEASIBLE' && 'Mục tiêu này vượt quá khả năng tài chính của bạn'}
                      {aiAnalysis.feasibility === 'CHALLENGING' && 'Mục tiêu có thể đạt được nhưng cần nỗ lực và kỷ luật cao'}
                    </Typography>
                  </Paper>

                  {/* Phân tích chi tiết từ AI */}
                  <Paper
                    elevation={0}
                    sx={{
                      p: 1.5,
                      mb: 1,
                      bgcolor: 'background.paper',
                      borderRadius: 2,
                      border: '1px solid',
                      borderColor: 'grey.200'
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                      <PsychologyIcon sx={{ color: 'primary.main', fontSize: 26 }} />
                      <Typography variant="h6" fontWeight={700} color="text.primary">
                        Phân Tích Chi Tiết
                      </Typography>
                    </Box>
                    <Box
                      sx={{
                        p: 1.5,
                        bgcolor: 'background.paper',
                        borderRadius: 2,
                        whiteSpace: 'pre-line',
                        maxHeight: '300px',
                        overflowY: 'auto',
                        border: '1px solid',
                        borderColor: 'grey.200',
                        boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.06)'
                      }}
                    >
                      <Typography
                        variant="body1"
                        sx={{
                          lineHeight: 1.7,
                          color: 'text.primary',
                          fontSize: '1.05rem'
                        }}
                      >
                        {aiAnalysis.aiAnalysis}
                      </Typography>
                    </Box>
                  </Paper>

                  {/* Tổng quan tài chính */}
                  <Paper
                    elevation={0}
                    sx={{
                      p: 1.5,
                      mb: 1,
                      bgcolor: 'background.paper',
                      borderRadius: 2,
                      border: '1px solid',
                      borderColor: 'grey.200'
                    }}
                  >
                    <Typography variant="h5" fontWeight={700} color="text.primary" sx={{ mb: 1 }}>
                      📈 Tổng Quan Tài Chính
                    </Typography>
                    <Grid container spacing={1}>
                      <Grid item xs={12} sm={6}>
                        <Paper
                          sx={{
                            p: 1,
                            bgcolor: 'success.50',
                            border: '2px solid',
                            borderColor: 'success.200',
                            borderRadius: 2,
                            textAlign: 'center'
                          }}
                        >
                          <Typography variant="caption" color="success.800" fontWeight={600} display="block" sx={{ mb: 0.2 }}>
                            💰 Thu nhập/tháng
                          </Typography>
                          <Typography variant="h6" color="success.900" fontWeight={700}>
                            {formatCurrency(aiAnalysis.monthlyIncome || 0)}
                          </Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Paper
                          sx={{
                            p: 1,
                            bgcolor: 'error.50',
                            border: '2px solid',
                            borderColor: 'error.200',
                            borderRadius: 2,
                            textAlign: 'center'
                          }}
                        >
                          <Typography variant="caption" color="error.800" fontWeight={600} display="block" sx={{ mb: 0.2 }}>
                            💸 Chi tiêu/tháng
                          </Typography>
                          <Typography variant="h6" color="error.900" fontWeight={700}>
                            {formatCurrency(aiAnalysis.monthlyExpenses || 0)}
                          </Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Paper
                          sx={{
                            p: 1,
                            bgcolor: 'info.50',
                            border: '2px solid',
                            borderColor: 'info.200',
                            borderRadius: 2,
                            textAlign: 'center'
                          }}
                        >
                          <Typography variant="caption" color="info.800" fontWeight={600} display="block" sx={{ mb: 0.2 }}>
                            🎯 Tiết kiệm hiện tại/tháng
                          </Typography>
                          <Typography variant="h6" color="info.900" fontWeight={700}>
                            {formatCurrency(aiAnalysis.monthlyExistingGoals || 0)}
                          </Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Paper
                          sx={{
                            p: 1,
                            bgcolor: aiAnalysis.monthlySurplus >= 0 ? 'success.50' : 'warning.50',
                            border: '2px solid',
                            borderColor: aiAnalysis.monthlySurplus >= 0 ? 'success.200' : 'warning.200',
                            borderRadius: 2,
                            textAlign: 'center'
                          }}
                        >
                          <Typography
                            variant="caption"
                            color={aiAnalysis.monthlySurplus >= 0 ? 'success.800' : 'warning.800'}
                            fontWeight={600}
                            display="block"
                            sx={{ mb: 0.2 }}
                          >
                            💵 Còn dư/tháng
                          </Typography>
                          <Typography
                            variant="h6"
                            color={aiAnalysis.monthlySurplus >= 0 ? 'success.900' : 'warning.900'}
                            fontWeight={700}
                          >
                            {formatCurrency(aiAnalysis.monthlySurplus || 0)}
                          </Typography>
                        </Paper>
                      </Grid>
                    </Grid>
                  </Paper>

                  {/* Mục tiêu mới - Yêu cầu tiết kiệm */}
                  <Paper
                    elevation={0}
                    sx={{
                      p: 1.5,
                      bgcolor: 'primary.50',
                      borderRadius: 2,
                      border: '2px solid',
                      borderColor: 'primary.200'
                    }}
                  >
                    <Typography variant="h5" fontWeight={700} color="primary.900" sx={{ mb: 1 }}>
                      🎯 Yêu Cầu Tiết Kiệm Cho Mục Tiêu Mới ({(() => {
                        const targetDate = new Date(formData.targetDate);
                        const today = new Date();
                        const daysRemaining = Math.ceil((targetDate - today) / (1000 * 60 * 60 * 24));
                        return daysRemaining > 0 ? `${daysRemaining} ngày` : '0 ngày';
                      })()})
                    </Typography>
                    <Grid container spacing={1}>
                      <Grid item xs={12} sm={6}>
                        <Paper
                          sx={{
                            p: 1,
                            bgcolor: 'background.paper',
                            borderRadius: 2,
                            border: '2px solid',
                            borderColor: 'primary.300',
                            textAlign: 'center'
                          }}
                        >
                          <Typography variant="caption" color="text.secondary" fontWeight={600} display="block" sx={{ mb: 0.2 }}>
                            🐷 Tiết kiệm tháng này
                          </Typography>
                          <Typography variant="h5" color="primary.main" fontWeight={700}>
                            {formatCurrency(aiAnalysis.currentMonthRequired || 0)}
                          </Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Paper
                          sx={{
                            p: 1,
                            bgcolor: 'background.paper',
                            borderRadius: 2,
                            border: '2px solid',
                            borderColor: 'primary.300',
                            textAlign: 'center'
                          }}
                        >
                          <Typography variant="caption" color="text.secondary" fontWeight={600} display="block" sx={{ mb: 0.2 }}>
                            📅 Tiết kiệm mỗi ngày
                          </Typography>
                          <Typography variant="h5" color="primary.main" fontWeight={700}>
                            {formatCurrency(aiAnalysis.dailyRequired || 0)}
                          </Typography>
                        </Paper>
                      </Grid>
                    </Grid>
                  </Paper>
                </Box>
              )}

              {!aiAnalysis && !isAnalyzing && (
                <Alert severity="warning" sx={{ borderRadius: 2 }}>
                  <Typography variant="body1" fontWeight={600}>
                    ⚠️ Không thể tải phân tích AI
                  </Typography>
                  <Typography variant="body2">
                    Vui lòng thử lại hoặc kiểm tra kết nối mạng
                  </Typography>
                </Alert>
              )}

              {/* Cảnh báo khi mục tiêu không khả thi */}
              {aiAnalysis && !isAnalyzing && aiAnalysis.feasibility === 'NOT_FEASIBLE' && (
                <Alert severity="error" sx={{ mt: 2, borderRadius: 2 }}>
                  <Typography variant="body1" fontWeight={700}>
                    🚫 Không thể thêm mục tiêu này!
                  </Typography>
                  <Typography variant="body2">
                    Mục tiêu vượt quá khả năng tài chính của bạn. Vui lòng điều chỉnh số tiền hoặc thời gian để mục tiêu khả thi hơn.
                  </Typography>
                </Alert>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Hủy</Button>
          {!editingGoal && dialogTab === 1 && (
            <Button onClick={() => setDialogTab(0)}>
              ← Quay Lại
            </Button>
          )}
          {!editingGoal && dialogTab === 0 ? (
            <Button
              variant="contained"
              onClick={() => setDialogTab(1)}
              disabled={!formData.name || !formData.targetAmount || !formData.targetDate}
            >
              Tiếp Theo →
            </Button>
          ) : (
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={
                !formData.name ||
                !formData.targetAmount ||
                !formData.targetDate ||
                (!editingGoal && aiAnalysis && aiAnalysis.feasibility === 'NOT_FEASIBLE')
              }
            >
              {editingGoal ? 'Cập Nhật' : 'Thêm'}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Dialog Đóng Góp */}
      <Dialog open={openContributeDialog} onClose={() => setOpenContributeDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>
          Đóng Góp Vào: {selectedGoal?.name}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Alert severity="info" sx={{ mb: 2 }}>
              Hiện tại: {formatCurrency(selectedGoal?.currentAmount || 0)} / {formatCurrency(selectedGoal?.targetAmount || 0)}
            </Alert>
            <TextField
              fullWidth
              label="Số Tiền Đóng Góp (VNĐ)"
              value={contributeAmount ? formatCurrencyInput(contributeAmount.toString()) : ''}
              onChange={(e) => {
                const rawValue = parseCurrencyInput(e.target.value);
                setContributeAmount(rawValue);
              }}
              placeholder="Ví dụ: 1,000,000"
              helperText="Nhập số tiền theo định dạng Việt Nam (VD: 1 triệu = 1,000,000)"
              autoFocus
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenContributeDialog(false)}>Hủy</Button>
          <Button
            variant="contained"
            onClick={handleContribute}
            disabled={!contributeAmount || parseFloat(contributeAmount) <= 0}
          >
            Đóng Góp
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar Thông Báo */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default SavingsGoals;
