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
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Chip,
  Alert,
  Snackbar,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Lightbulb as LightbulbIcon,
} from '@mui/icons-material';
import { budgetAPI } from '../services/api';

const BUDGET_PERIODS = {
  DAILY: 'Hàng ngày',
  WEEKLY: 'Hàng tuần',
  MONTHLY: 'Hàng tháng',
  YEARLY: 'Hàng năm',
  CUSTOM: 'Tùy chỉnh',
};

const CATEGORIES = [
  'Ăn uống',
  'Di chuyển',
  'Giải trí',
  'Mua sắm',
  'Hóa đơn',
  'Y tế',
  'Giáo dục',
  'Khác',
];

const Budgets = () => {
  const [budgets, setBudgets] = useState([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingBudget, setEditingBudget] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [openAIDialog, setOpenAIDialog] = useState(false);
  const [aiSuggestion, setAISuggestion] = useState('');
  const [loadingAI, setLoadingAI] = useState(false);
  const [aiCheckResult, setAICheckResult] = useState(null);
  const [loadingCheck, setLoadingCheck] = useState(false);

  const [formData, setFormData] = useState({
    category: '',
    amount: '',
    period: 'MONTHLY',
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    alertThreshold: 80,
  });

  useEffect(() => {
    loadBudgets();
  }, []);

  // Auto-check budget with AI when form changes (with debounce)
  useEffect(() => {
    // Only check for new budgets (not editing)
    if (editingBudget) return;

    // Only check if all required fields are filled
    if (!formData.category || !formData.amount || !formData.startDate || !formData.endDate) {
      setAICheckResult(null);
      return;
    }

    // Debounce: wait 1 second after user stops typing
    const timer = setTimeout(() => {
      handleCheckBudgetWithAI();
    }, 1000);

    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formData.category, formData.amount, formData.period, formData.startDate, formData.endDate, editingBudget]);

  const loadBudgets = async () => {
    try {
      const response = await budgetAPI.getAll();
      setBudgets(response.data.data || []);
    } catch (error) {
      showSnackbar('Lỗi khi tải ngân sách', 'error');
    }
  };

  const handleOpenDialog = (budget = null) => {
    if (budget) {
      setEditingBudget(budget);
      setFormData({
        category: budget.category,
        amount: budget.amount,
        period: budget.period,
        startDate: budget.startDate,
        endDate: budget.endDate,
        alertThreshold: budget.alertThreshold || 80,
      });
    } else {
      setEditingBudget(null);
      const today = new Date();
      const endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      setFormData({
        category: '',
        amount: '',
        period: 'MONTHLY',
        startDate: today.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0],
        alertThreshold: 80,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingBudget(null);
    setAICheckResult(null);
  };

  const handleSubmit = async () => {
    try {
      const data = {
        ...formData,
        amount: parseFloat(formData.amount),
        alertThreshold: parseFloat(formData.alertThreshold),
      };

      if (editingBudget) {
        await budgetAPI.update(editingBudget.id, data);
        showSnackbar('Cập nhật ngân sách thành công!', 'success');
      } else {
        await budgetAPI.create(data);
        showSnackbar('Thêm ngân sách thành công!', 'success');
      }

      handleCloseDialog();
      loadBudgets();
    } catch (error) {
      showSnackbar('Lỗi khi lưu ngân sách', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa ngân sách này?')) {
      try {
        await budgetAPI.delete(id);
        showSnackbar('Xóa ngân sách thành công!', 'success');
        loadBudgets();
      } catch (error) {
        showSnackbar('Lỗi khi xóa ngân sách', 'error');
      }
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

  const getBudgetStatus = (budget) => {
    const percentage = budget.percentageUsed || 0;
    if (percentage >= 100) return { color: 'error', icon: <WarningIcon />, text: 'Vượt ngân sách' };
    if (percentage >= budget.alertThreshold) return { color: 'warning', icon: <WarningIcon />, text: 'Cảnh báo' };
    return { color: 'success', icon: <CheckCircleIcon />, text: 'Ổn định' };
  };

  const handleGetAISuggestion = async () => {
    setLoadingAI(true);
    setOpenAIDialog(true);
    setAISuggestion('Đang phân tích và tạo gợi ý...');

    try {
      // Use checkBeforeCreate with minimal data to get general suggestions
      const response = await budgetAPI.checkBeforeCreate({
        category: 'Tổng quan',
        amount: 0,
        period: 'MONTHLY',
        startDate: new Date().toISOString().split('T')[0],
        endDate: new Date(new Date().setMonth(new Date().getMonth() + 1)).toISOString().split('T')[0],
      });

      // The response is JSON string, need to parse it
      try {
        const result = JSON.parse(response.data.data);
        // Format the suggestion nicely
        let suggestionText = result.recommendation || 'Không có gợi ý từ AI';
        if (result.tips && result.tips.length > 0) {
          suggestionText += '\n\n**Gợi ý:**\n' + result.tips.map((tip, i) => `${i + 1}. ${tip}`).join('\n');
        }
        setAISuggestion(suggestionText);
      } catch (parseError) {
        // If not JSON, use as is
        setAISuggestion(response.data.data || 'Không có gợi ý từ AI');
      }
    } catch (error) {
      console.error('Error getting AI suggestion:', error);
      setAISuggestion('Không thể lấy gợi ý từ AI. Vui lòng thử lại sau.');
    } finally {
      setLoadingAI(false);
    }
  };

  const handleCheckBudgetWithAI = async (showError = false) => {
    // Validate form data first (silently for auto-check)
    if (!formData.category || !formData.amount || !formData.startDate || !formData.endDate) {
      if (showError) {
        showSnackbar('Vui lòng điền đầy đủ thông tin ngân sách trước khi kiểm tra', 'warning');
      }
      return;
    }

    setLoadingCheck(true);
    setAICheckResult(null);

    try {
      const response = await budgetAPI.checkBeforeCreate({
        category: formData.category,
        amount: parseFloat(formData.amount),
        period: formData.period,
        startDate: formData.startDate,
        endDate: formData.endDate,
      });

      const result = JSON.parse(response.data.data);
      setAICheckResult(result);
    } catch (error) {
      console.error('Error checking budget with AI:', error);
      // Only show error if explicitly requested (not for auto-check)
      if (showError) {
        showSnackbar('Không thể kết nối với AI. Vui lòng thử lại sau.', 'error');
      }
    } finally {
      setLoadingCheck(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Quản Lý Ngân Sách</Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<LightbulbIcon />}
            onClick={handleGetAISuggestion}
            disabled={loadingAI}
          >
            Gợi ý AI
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Thêm Ngân Sách
          </Button>
        </Box>
      </Box>

      {budgets.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            Chưa có ngân sách nào
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Tạo ngân sách để theo dõi chi tiêu của bạn
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Thêm Ngân Sách Đầu Tiên
          </Button>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {budgets.map((budget) => {
            const status = getBudgetStatus(budget);
            const percentage = Math.min(budget.percentageUsed || 0, 100);

            return (
              <Grid item xs={12} md={6} lg={4} key={budget.id}>
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="h6" gutterBottom>
                          {budget.category}
                        </Typography>
                        <Chip
                          label={BUDGET_PERIODS[budget.period]}
                          size="small"
                          sx={{ mr: 1 }}
                        />
                        <Chip
                          label={status.text}
                          color={status.color}
                          size="small"
                          icon={status.icon}
                        />
                      </Box>
                      <Box>
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleOpenDialog(budget)}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(budget.id)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>

                    <Box sx={{ mb: 2 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2" color="text.secondary">
                          Đã chi: {formatCurrency(budget.spentAmount || 0)}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Ngân sách: {formatCurrency(budget.amount)}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={percentage}
                        color={status.color}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                        <Typography variant="body2" fontWeight="bold" color={status.color + '.main'}>
                          {percentage.toFixed(1)}%
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Còn lại: {formatCurrency(budget.remainingAmount || 0)}
                        </Typography>
                      </Box>
                    </Box>

                    <Box sx={{ borderTop: 1, borderColor: 'divider', pt: 1 }}>
                      <Typography variant="caption" color="text.secondary">
                        {new Date(budget.startDate).toLocaleDateString('vi-VN')} - {new Date(budget.endDate).toLocaleDateString('vi-VN')}
                      </Typography>
                    </Box>

                    {budget.isOverBudget && (
                      <Alert severity="error" sx={{ mt: 2 }}>
                        Bạn đã vượt ngân sách {formatCurrency(Math.abs(budget.remainingAmount || 0))}!
                      </Alert>
                    )}

                    {budget.shouldAlert && !budget.isOverBudget && (
                      <Alert severity="warning" sx={{ mt: 2 }}>
                        Đã sử dụng {percentage.toFixed(1)}% ngân sách!
                      </Alert>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Dialog Thêm/Sửa Ngân Sách */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingBudget ? 'Chỉnh Sửa Ngân Sách' : 'Thêm Ngân Sách Mới'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Danh Mục</InputLabel>
                <Select
                  value={formData.category}
                  label="Danh Mục"
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                >
                  {CATEGORIES.map((cat) => (
                    <MenuItem key={cat} value={cat}>{cat}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Số Tiền Ngân Sách"
                type="number"
                value={formData.amount}
                onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                InputProps={{
                  inputProps: { min: 0, step: 0.01 }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Chu Kỳ</InputLabel>
                <Select
                  value={formData.period}
                  label="Chu Kỳ"
                  onChange={(e) => {
                    const period = e.target.value;
                    setFormData({ ...formData, period });

                    // Tự động tính endDate theo period
                    const start = new Date(formData.startDate);
                    let end = new Date(start);

                    switch (period) {
                      case 'DAILY':
                        end.setDate(start.getDate() + 1);
                        break;
                      case 'WEEKLY':
                        end.setDate(start.getDate() + 7);
                        break;
                      case 'MONTHLY':
                        end.setMonth(start.getMonth() + 1);
                        break;
                      case 'YEARLY':
                        end.setFullYear(start.getFullYear() + 1);
                        break;
                      default:
                        break;
                    }

                    if (period !== 'CUSTOM') {
                      setFormData({ ...formData, period, endDate: end.toISOString().split('T')[0] });
                    }
                  }}
                >
                  {Object.entries(BUDGET_PERIODS).map(([key, value]) => (
                    <MenuItem key={key} value={key}>{value}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Ngày Bắt Đầu"
                type="date"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Ngày Kết Thúc"
                type="date"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Ngưỡng Cảnh Báo (%)"
                type="number"
                value={formData.alertThreshold}
                onChange={(e) => setFormData({ ...formData, alertThreshold: e.target.value })}
                helperText="Bạn sẽ nhận cảnh báo khi đạt ngưỡng này"
                InputProps={{
                  inputProps: { min: 0, max: 100 }
                }}
              />
            </Grid>

            {/* AI Analysis Result - Auto displayed when form is complete */}
            {!editingBudget && loadingCheck && (
              <Grid item xs={12}>
                <Alert severity="info" sx={{ mt: 2 }}>
                  <Typography variant="body2">
                    🤖 AI đang phân tích ngân sách của bạn...
                  </Typography>
                </Alert>
              </Grid>
            )}

            {!editingBudget && aiCheckResult && (
              <Grid item xs={12}>
                <Alert
                  severity={aiCheckResult.shouldCreate ? 'success' : 'warning'}
                  sx={{ mt: 2 }}
                >
                  <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                    {aiCheckResult.recommendation}
                  </Typography>
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    {aiCheckResult.reason}
                  </Typography>

                  {aiCheckResult.suggestedAmount && (
                    <Box sx={{ mt: 1, p: 1, bgcolor: 'background.paper', borderRadius: 1 }}>
                      <Typography variant="body2" color="primary" fontWeight="bold">
                        💡 Gợi ý: {formatCurrency(aiCheckResult.suggestedAmount)}
                      </Typography>
                    </Box>
                  )}

                  {aiCheckResult.warnings && aiCheckResult.warnings.length > 0 && (
                    <Box sx={{ mt: 1 }}>
                      <Typography variant="body2" fontWeight="bold" color="warning.main">
                        ⚠️ Cảnh báo:
                      </Typography>
                      <ul style={{ margin: '4px 0', paddingLeft: '20px' }}>
                        {aiCheckResult.warnings.map((warning, index) => (
                          <li key={index}>
                            <Typography variant="body2">{warning}</Typography>
                          </li>
                        ))}
                      </ul>
                    </Box>
                  )}

                  {aiCheckResult.tips && aiCheckResult.tips.length > 0 && (
                    <Box sx={{ mt: 1 }}>
                      <Typography variant="body2" fontWeight="bold" color="info.main">
                        💡 Lời khuyên:
                      </Typography>
                      <ul style={{ margin: '4px 0', paddingLeft: '20px' }}>
                        {aiCheckResult.tips.map((tip, index) => (
                          <li key={index}>
                            <Typography variant="body2">{tip}</Typography>
                          </li>
                        ))}
                      </ul>
                    </Box>
                  )}
                </Alert>
              </Grid>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Hủy</Button>
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={!formData.category || !formData.amount || !formData.startDate || !formData.endDate}
          >
            {editingBudget ? 'Cập Nhật' : 'Thêm'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Gợi ý AI */}
      <Dialog
        open={openAIDialog}
        onClose={() => setOpenAIDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <LightbulbIcon color="primary" />
          Gợi ý Ngân Sách Thông Minh
        </DialogTitle>
        <DialogContent>
          {loadingAI ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}>
              <Typography>Đang phân tích và tạo gợi ý...</Typography>
            </Box>
          ) : (
            <Box sx={{ mt: 2 }}>
              <Alert severity="info" sx={{ mb: 2 }}>
                Dựa trên các ngân sách hiện tại và thói quen chi tiêu của bạn
              </Alert>
              <Typography
                component="div"
                sx={{
                  whiteSpace: 'pre-wrap',
                  lineHeight: 1.8,
                  '& strong': { fontWeight: 'bold' },
                }}
              >
                {aiSuggestion.split('\n').map((line, index) => (
                  <Box key={index} component="p" sx={{ mb: line.trim() === '' ? 2 : 0.5 }}>
                    {line.trim().startsWith('**') && line.trim().endsWith('**') ? (
                      <strong>{line.replace(/\*\*/g, '')}</strong>
                    ) : line.trim().startsWith('-') || line.trim().match(/^\d+\./) ? (
                      <Box component="span" sx={{ display: 'block', pl: 2 }}>
                        {line}
                      </Box>
                    ) : (
                      line
                    )}
                  </Box>
                ))}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenAIDialog(false)}>Đóng</Button>
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

export default Budgets;
