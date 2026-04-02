import React, { useEffect, useState } from 'react';
import { Grid, Paper, Typography, Box, Card, CardContent, LinearProgress, Chip } from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  AccountBalance as BalanceIcon,
  Savings as SavingsIcon,
} from '@mui/icons-material';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { transactionAPI, budgetAPI, savingsAPI, fixedIncomeAPI, supplementaryIncomeAPI, recurringExpenseAPI, incidentalExpenseAPI } from '../services/api';
import { translateCategory } from '../utils/categoryTranslator';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D'];

const Dashboard = () => {
  const [summary, setSummary] = useState(null);
  const [budgets, setBudgets] = useState([]);
  const [goals, setGoals] = useState([]);
  const [categoryData, setCategoryData] = useState([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      // Lấy ngày đầu và cuối tháng hiện tại
      const now = new Date();
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

      const startDate = firstDay.toISOString().split('T')[0];
      const endDate = lastDay.toISOString().split('T')[0];

      // Lấy tất cả dữ liệu song song
      const [budgetsRes, goalsRes, fixedIncomeRes, suppIncomeRes, recurringExpenseRes, incidentalExpenseRes] = await Promise.all([
        budgetAPI.getActive(),
        savingsAPI.getActive(),
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
      const totalIncome = totalFixedIncome + totalSuppIncome;

      // Tính tổng chi tiêu từ recurring expenses (active) và incidental expenses trong tháng
      const recurringExpenses = (recurringExpenseRes.data || []).filter(re => re.isActive);
      const incidentalExpenses = incidentalExpenseRes.data || [];

      const totalRecurringExpense = recurringExpenses.reduce((sum, re) => sum + (re.amount || 0), 0);
      const totalIncidentalExpense = incidentalExpenses.reduce((sum, ie) => sum + (ie.amount || 0), 0);
      const totalExpense = totalRecurringExpense + totalIncidentalExpense;

      // Tổng số giao dịch
      const totalTransactions = fixedIncomes.length + suppIncomes.length + recurringExpenses.length + incidentalExpenses.length;

      // Tính tổng tiền cần tiết kiệm cho các mục tiêu heo đất (currentMonthRequiredAmount)
      const activeGoals = goalsRes.data.data || [];
      const totalSavingsRequired = activeGoals
        .filter(g => g.status === 'ACTIVE')
        .reduce((sum, g) => sum + (g.currentMonthRequiredAmount || 0), 0);

      setSummary({
        totalIncome,
        totalExpense,
        totalSavingsRequired,
        balance: totalIncome - totalExpense - totalSavingsRequired, // Trừ cả tiết kiệm
        totalTransactions
      });

      setBudgets(budgetsRes.data.data || []);
      setGoals(activeGoals);

      // Tính category data từ recurring và incidental expenses
      const categoryMap = {};

      // Thêm recurring expenses
      recurringExpenses.forEach(re => {
        const cat = re.category || 'Khác';
        categoryMap[cat] = (categoryMap[cat] || 0) + (re.amount || 0);
      });

      // Thêm incidental expenses
      incidentalExpenses.forEach(ie => {
        const cat = ie.category || 'Khác';
        categoryMap[cat] = (categoryMap[cat] || 0) + (ie.amount || 0);
      });

      const categoryDataArray = Object.entries(categoryMap)
        .map(([name, value]) => ({
          name: translateCategory(name), // Chuyển sang tiếng Việt
          value: parseFloat(value.toFixed(2))
        }))
        .sort((a, b) => b.value - a.value)
        .slice(0, 5);

      setCategoryData(categoryDataArray);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount || 0); // Hiển thị số tiền thực tế không nhân 1000
  };

  const savingsRate = summary ? ((summary.totalIncome - summary.totalExpense) / summary.totalIncome * 100) : 0;

  const getCurrentMonthLabel = () => {
    const now = new Date();
    const monthNames = [
      'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
      'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
    ];
    return `${monthNames[now.getMonth()]} ${now.getFullYear()}`;
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Bảng Điều Khiển 📊
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Tổng quan tài chính tháng này - {getCurrentMonthLabel()}
      </Typography>

      {/* Tổng quan tài chính */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'success.lighter', borderLeft: 4, borderColor: 'success.main' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                    Thu Nhập Tháng Này
                  </Typography>
                  <Typography variant="h5" color="success.main" fontWeight="bold">
                    {formatCurrency(summary?.totalIncome)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {summary?.totalTransactions || 0} giao dịch
                  </Typography>
                </Box>
                <TrendingUpIcon sx={{ fontSize: 40, color: 'success.main', opacity: 0.5 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'error.lighter', borderLeft: 4, borderColor: 'error.main' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                    Chi Tiêu Tháng Này
                  </Typography>
                  <Typography variant="h5" color="error.main" fontWeight="bold">
                    {formatCurrency(summary?.totalExpense)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {categoryData.length} danh mục
                  </Typography>
                </Box>
                <TrendingDownIcon sx={{ fontSize: 40, color: 'error.main', opacity: 0.5 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'primary.lighter', borderLeft: 4, borderColor: 'primary.main' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                    Số Dư Tháng Này
                  </Typography>
                  <Typography variant="h5" color="primary.main" fontWeight="bold">
                    {formatCurrency(summary?.balance)}
                  </Typography>
                  <Typography variant="caption" color={summary?.balance >= 0 ? 'success.main' : 'error.main'}>
                    {summary?.balance >= 0 ? '✓ Dương' : '✗ Âm'}
                  </Typography>
                </Box>
                <BalanceIcon sx={{ fontSize: 40, color: 'primary.main', opacity: 0.5 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'info.lighter', borderLeft: 4, borderColor: 'info.main' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                    Tỷ Lệ Tiết Kiệm
                  </Typography>
                  <Typography variant="h5" color={savingsRate > 20 ? 'success.main' : 'warning.main'} fontWeight="bold">
                    {isNaN(savingsRate) || !isFinite(savingsRate) ? '0' : savingsRate.toFixed(1)}%
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {savingsRate > 20 ? 'Tốt' : savingsRate > 10 ? 'Trung bình' : 'Cần cải thiện'}
                  </Typography>
                </Box>
                <SavingsIcon sx={{ fontSize: 40, color: 'info.main', opacity: 0.5 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Biểu đồ */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {/* Biểu đồ tròn - Top 5 danh mục chi tiêu */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Top 5 Danh Mục Chi Tiêu Tháng Này
            </Typography>
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatCurrency(value)} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <Box sx={{ textAlign: 'center', py: 5 }}>
                <Typography color="text.secondary">
                  Chưa có dữ liệu chi tiêu
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>

        {/* Biểu đồ cột - Chi tiêu theo danh mục */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Chi Tiêu Theo Danh Mục Tháng Này
            </Typography>
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={categoryData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis tickFormatter={(value) => `${(value / 1000000).toFixed(0)}tr`} />
                  <Tooltip
                    formatter={(value) => [formatCurrency(value), 'Chi tiêu']}
                    labelStyle={{ color: '#000' }}
                  />
                  <Bar dataKey="value" fill="#8884d8" name="Chi tiêu">
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <Box sx={{ textAlign: 'center', py: 5 }}>
                <Typography color="text.secondary">
                  Chưa có dữ liệu chi tiêu
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Mục tiêu tiết kiệm */}
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Mục Tiêu Tiết Kiệm 🐷 ({goals.length})
            </Typography>
            {goals.length > 0 ? (
              goals.slice(0, 3).map((goal) => (
                <Box key={goal.id} sx={{ mb: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                    <Typography variant="body2">{goal.name}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {formatCurrency(goal.currentAmount)} / {formatCurrency(goal.targetAmount)}
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(goal.percentageCompleted || 0, 100)}
                    color={goal.status === 'COMPLETED' ? 'success' : 'primary'}
                    sx={{ height: 8, borderRadius: 4 }}
                  />
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5 }}>
                    <Typography variant="caption" color="text.secondary">
                      {goal.percentageCompleted?.toFixed(1)}% hoàn thành
                    </Typography>
                    <Typography variant="caption" color="primary.main">
                      Còn: {formatCurrency(goal.dailyRequiredAmount)}/ngày
                    </Typography>
                  </Box>
                </Box>
              ))
            ) : (
              <Box sx={{ textAlign: 'center', py: 3 }}>
                <Typography color="text.secondary">
                  Chưa có mục tiêu tiết kiệm nào
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
