import { useState, useEffect } from 'react';
import {
  Container, Paper, Typography, Box, Grid, Card, CardContent,
  CircularProgress,
  TextField, Button, Avatar, Tabs, Tab
} from '@mui/material';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts';
import { aiAPI, fixedIncomeAPI, supplementaryIncomeAPI, recurringExpenseAPI, incidentalExpenseAPI } from '../services/api';
import { translateCategory } from '../utils/categoryTranslator';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import {
  Send as SendIcon,
  Person as PersonIcon,
  SmartToy as BotIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D', '#FFC658'];

const Statistics = () => {
  const [tabValue, setTabValue] = useState(0);
  const [period] = useState('year');
  const [loading, setLoading] = useState(true);
  const [chartData, setChartData] = useState([]);

  // Chart data
  const [categoryData, setCategoryData] = useState([]);
  const [incomeCategoryData, setIncomeCategoryData] = useState([]);

  // Tổng dữ liệu
  const [totalSummary, setTotalSummary] = useState({
    totalIncome: 0,
    totalExpense: 0,
    totalSavings: 0,
    monthlyData: []
  });

  // Chatbot
  const [chatMessages, setChatMessages] = useState([
    { sender: 'bot', message: 'Xin chào! Tôi là trợ lý tài chính AI. Bạn cần tư vấn gì không?' }
  ]);
  const [userMessage, setUserMessage] = useState('');

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  // Hàm xuất file Excel
  const exportToExcel = () => {
    try {
      // Tạo worksheet cho tổng quan
      const summaryData = [
        ['BÁO CÁO THỐNG KÊ TÀI CHÍNH'],
        [''],
        ['Tổng Thu Nhập:', totalSummary.totalIncome],
        ['Tổng Chi Tiêu:', totalSummary.totalExpense],
        ['Tổng Tiết Kiệm:', totalSummary.totalSavings],
        ['Tỷ Lệ Tiết Kiệm:', `${((totalSummary.totalSavings / totalSummary.totalIncome) * 100).toFixed(2)}%`],
        [''],
        ['CHI TIẾT THEO THÁNG']
      ];

      const monthlyDataForExcel = totalSummary.monthlyData.map(m => ({
        'Tháng': m.month,
        'Thu Nhập': m.totalIncome,
        'Chi Tiêu': m.totalExpense,
        'Tiết Kiệm': m.totalIncome - m.totalExpense
      }));

      const wsSummary = XLSX.utils.aoa_to_sheet(summaryData);
      const wsMonthly = XLSX.utils.json_to_sheet(monthlyDataForExcel);

      // Tạo worksheet cho chi tiêu theo danh mục
      const expenseData = categoryData.map(c => ({
        'Danh Mục': c.name,
        'Số Tiền': c.value
      }));
      const wsExpense = XLSX.utils.json_to_sheet(expenseData);

      // Tạo worksheet cho thu nhập theo nguồn
      const incomeData = incomeCategoryData.map(c => ({
        'Nguồn': c.name,
        'Số Tiền': c.value
      }));
      const wsIncome = XLSX.utils.json_to_sheet(incomeData);

      // Tạo workbook
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, wsSummary, 'Tổng Quan');
      XLSX.utils.book_append_sheet(wb, wsMonthly, 'Chi Tiết Tháng');
      XLSX.utils.book_append_sheet(wb, wsExpense, 'Chi Tiêu Theo Danh Mục');
      XLSX.utils.book_append_sheet(wb, wsIncome, 'Thu Nhập Theo Nguồn');

      // Xuất file
      const excelBuffer = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
      const data = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const fileName = `Bao-Cao-Thong-Ke-${new Date().toISOString().split('T')[0]}.xlsx`;
      saveAs(data, fileName);
    } catch (error) {
      console.error('Error exporting to Excel:', error);
      alert('Có lỗi khi xuất file Excel!');
    }
  };

  useEffect(() => {
    loadAllData();
  }, [period]);

  const loadAllData = async () => {
    setLoading(true);
    try {
      await Promise.all([
        loadStatistics(),
        loadAIInsights()
      ]);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStatistics = async () => {
    try {
      // Calculate date ranges based on period
      const now = new Date();
      const monthsToLoad = period === 'week' ? 1 : period === 'month' ? 6 : 12;

      const monthlyData = [];

      // Load fixed incomes and recurring expenses once (active only)
      const [fixedIncomesRes, recurringExpensesRes] = await Promise.all([
        fixedIncomeAPI.getAll(),
        recurringExpenseAPI.getAll()
      ]);

      const fixedIncomes = (fixedIncomesRes.data.data || fixedIncomesRes.data || []).filter(fi => fi.isActive);
      const recurringExpenses = (recurringExpensesRes.data.data || recurringExpensesRes.data || []).filter(re => re.isActive);

      // Load data for past months
      for (let i = monthsToLoad - 1; i >= 0; i--) {
        const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const startDate = new Date(date.getFullYear(), date.getMonth(), 1).toISOString().split('T')[0];
        const endDate = new Date(date.getFullYear(), date.getMonth() + 1, 0).toISOString().split('T')[0];

        try {
          // Load supplementary income and incidental expenses for this month
          const [suppIncomeRes, incidentalExpenseRes] = await Promise.all([
            supplementaryIncomeAPI.getByDateRange(startDate, endDate),
            incidentalExpenseAPI.getByDateRange(startDate, endDate)
          ]);

          const suppIncomes = suppIncomeRes.data.data || suppIncomeRes.data || [];
          const incidentalExpenses = incidentalExpenseRes.data.data || incidentalExpenseRes.data || [];

          // Lọc Fixed Income chỉ tính từ tháng được tạo trở đi
          const activeFixedIncomesForMonth = fixedIncomes.filter(fi => {
            if (!fi.createdAt) return true; // Nếu không có createdAt, tính luôn
            const createdDate = new Date(fi.createdAt);
            return createdDate <= new Date(endDate); // Chỉ tính fixed income được tạo trước hoặc trong tháng này
          });

          // Lọc Recurring Expense chỉ tính từ tháng được tạo trở đi
          const activeRecurringExpensesForMonth = recurringExpenses.filter(re => {
            if (!re.createdAt) return true; // Nếu không có createdAt, tính luôn
            const createdDate = new Date(re.createdAt);
            return createdDate <= new Date(endDate); // Chỉ tính expense được tạo trước hoặc trong tháng này
          });

          // Calculate total income = fixed income + supplementary income
          const totalFixedIncome = activeFixedIncomesForMonth.reduce((sum, fi) => sum + (fi.amount || 0), 0);
          const totalSuppIncome = suppIncomes.reduce((sum, si) => sum + (si.amount || 0), 0);
          const income = totalFixedIncome + totalSuppIncome;

          // Calculate total expense = recurring expense + incidental expense
          const totalRecurringExpense = activeRecurringExpensesForMonth.reduce((sum, re) => sum + (re.amount || 0), 0);
          const totalIncidentalExpense = incidentalExpenses.reduce((sum, ie) => sum + (ie.amount || 0), 0);
          const expense = totalRecurringExpense + totalIncidentalExpense;

          // Chỉ thêm tháng này nếu có giao dịch thực sự (có supplementary income hoặc incidental expense)
          const hasRealTransactions = suppIncomes.length > 0 || incidentalExpenses.length > 0;

          // Hoặc nếu là tháng hiện tại, luôn hiển thị (để thấy fixed income và recurring expenses)
          const isCurrentMonth = date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();

          if (!hasRealTransactions && !isCurrentMonth) {
            continue; // Bỏ qua tháng không có giao dịch
          }

          // Group expenses by category
          const categoryExpenses = {};

          // Add recurring expenses to category breakdown (chỉ tính expense cho tháng này)
          activeRecurringExpensesForMonth.forEach(re => {
            const category = re.category || 'Khác';
            categoryExpenses[category] = (categoryExpenses[category] || 0) + (re.amount || 0);
          });

          // Add incidental expenses to category breakdown
          incidentalExpenses.forEach(ie => {
            const category = ie.category || 'Khác';
            categoryExpenses[category] = (categoryExpenses[category] || 0) + (ie.amount || 0);
          });

          // Group income by category/source
          const categoryIncomes = {};

          // Add fixed incomes to category breakdown (chỉ tính income cho tháng này)
          activeFixedIncomesForMonth.forEach(fi => {
            const source = fi.source || 'Thu nhập cố định';
            categoryIncomes[source] = (categoryIncomes[source] || 0) + (fi.amount || 0);
          });

          // Add supplementary incomes to category breakdown
          suppIncomes.forEach(si => {
            const category = si.category || 'Thu nhập phụ';
            categoryIncomes[category] = (categoryIncomes[category] || 0) + (si.amount || 0);
          });

          const monthName = date.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit' });

          monthlyData.push({
            month: monthName,
            totalIncome: income,
            totalExpense: expense,
            categoryExpenses: categoryExpenses,
            categoryIncomes: categoryIncomes
          });

        } catch (error) {
          console.error(`Error loading data for ${date}:`, error);
        }
      }

      // Tính tổng tất cả các tháng
      const totalIncome = monthlyData.reduce((sum, m) => sum + m.totalIncome, 0);
      const totalExpense = monthlyData.reduce((sum, m) => sum + m.totalExpense, 0);
      const totalSavings = totalIncome - totalExpense;

      setTotalSummary({
        totalIncome,
        totalExpense,
        totalSavings,
        monthlyData
      });

      // Prepare chart data
      const chart = monthlyData.map(m => ({
        name: m.month,
        'Thu nhập': m.totalIncome,
        'Chi tiêu': m.totalExpense,
        'Tiết kiệm': m.totalIncome - m.totalExpense
      }));

      setChartData(chart);

    } catch (error) {
      console.error('Error loading statistics:', error);
    }
  };

  const loadAIInsights = async () => {
    try {
      // Get current month date range
      const now = new Date();
      const startDate = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
      const endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0];

      // Load all income and expense data
      const [fixedIncomesRes, suppIncomesRes, recurringExpensesRes, incidentalExpensesRes] = await Promise.all([
        fixedIncomeAPI.getAll(),
        supplementaryIncomeAPI.getByDateRange(startDate, endDate),
        recurringExpenseAPI.getAll(),
        incidentalExpenseAPI.getByDateRange(startDate, endDate)
      ]);

      const fixedIncomes = (fixedIncomesRes.data.data || fixedIncomesRes.data || []).filter(fi => fi.isActive);
      const suppIncomes = suppIncomesRes.data.data || suppIncomesRes.data || [];
      const recurringExpenses = (recurringExpensesRes.data.data || recurringExpensesRes.data || []).filter(re => re.isActive);
      const incidentalExpenses = incidentalExpensesRes.data.data || incidentalExpensesRes.data || [];

      // Calculate total income
      const totalFixedIncome = fixedIncomes.reduce((sum, fi) => sum + (fi.amount || 0), 0);
      const totalSuppIncome = suppIncomes.reduce((sum, si) => sum + (si.amount || 0), 0);
      const totalIncome = totalFixedIncome + totalSuppIncome;

      // Calculate total expense
      const totalRecurringExpense = recurringExpenses.reduce((sum, re) => sum + (re.amount || 0), 0);
      const totalIncidentalExpense = incidentalExpenses.reduce((sum, ie) => sum + (ie.amount || 0), 0);
      const totalExpense = totalRecurringExpense + totalIncidentalExpense;

      // Calculate expense category breakdown
      const categoryExpenseMap = {};
      recurringExpenses.forEach(re => {
        const category = re.category || 'Khác';
        categoryExpenseMap[category] = (categoryExpenseMap[category] || 0) + (re.amount || 0);
      });
      incidentalExpenses.forEach(ie => {
        const category = ie.category || 'Khác';
        categoryExpenseMap[category] = (categoryExpenseMap[category] || 0) + (ie.amount || 0);
      });

      const categoryExpenseArray = Object.entries(categoryExpenseMap).map(([name, value]) => ({
        name: translateCategory(name), // Chuyển sang tiếng Việt
        value: parseFloat(value.toFixed(2))
      })).sort((a, b) => b.value - a.value);

      setCategoryData(categoryExpenseArray);

      // Calculate income category breakdown
      const categoryIncomeMap = {};
      fixedIncomes.forEach(fi => {
        const source = fi.source || 'Thu nhập cố định';
        categoryIncomeMap[source] = (categoryIncomeMap[source] || 0) + (fi.amount || 0);
      });
      suppIncomes.forEach(si => {
        const category = si.category || 'Thu nhập phụ';
        categoryIncomeMap[category] = (categoryIncomeMap[category] || 0) + (si.amount || 0);
      });

      const categoryIncomeArray = Object.entries(categoryIncomeMap).map(([name, value]) => ({
        name: translateCategory(name), // Chuyển sang tiếng Việt
        value: parseFloat(value.toFixed(2))
      })).sort((a, b) => b.value - a.value);

      setIncomeCategoryData(categoryIncomeArray);

    } catch (error) {
      console.error('Error loading chart data:', error);
    }
  };

  const handleSendMessage = async () => {
    if (!userMessage.trim()) return;

    // Add user message
    setChatMessages(prev => [...prev, { sender: 'user', message: userMessage }]);
    const currentMessage = userMessage;
    setUserMessage('');

    try {
      const response = await aiAPI.chat(currentMessage);
      setChatMessages(prev => [...prev, { sender: 'bot', message: response.data.data.message }]);
    } catch (error) {
      console.error('AI Chat error:', error);
      setChatMessages(prev => [...prev, {
        sender: 'bot',
        message: 'Xin lỗi, tôi gặp lỗi. Vui lòng thử lại sau!'
      }]);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4">Thống Kê & Phân Tích 📊</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Phân tích chi tiêu và trực quan hóa dữ liệu tài chính
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <Button
            variant="contained"
            color="success"
            startIcon={<DownloadIcon />}
            onClick={exportToExcel}
          >
            Xuất Excel
          </Button>
        </Box>
      </Box>

      {/* Tổng quan dữ liệu */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'success.lighter', borderLeft: 4, borderColor: 'success.main' }}>
            <CardContent>
              <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                Tổng Thu Nhập
              </Typography>
              <Typography variant="h5" color="success.main" fontWeight="bold">
                {formatCurrency(totalSummary.totalIncome)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Tất cả các tháng
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'error.lighter', borderLeft: 4, borderColor: 'error.main' }}>
            <CardContent>
              <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                Tổng Chi Tiêu
              </Typography>
              <Typography variant="h5" color="error.main" fontWeight="bold">
                {formatCurrency(totalSummary.totalExpense)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Tất cả các tháng
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'primary.lighter', borderLeft: 4, borderColor: 'primary.main' }}>
            <CardContent>
              <Typography color="text.secondary" gutterBottom variant="body2" fontWeight="bold">
                Tổng Tiết Kiệm
              </Typography>
              <Typography variant="h5" color="primary.main" fontWeight="bold">
                {formatCurrency(totalSummary.totalSavings)}
              </Typography>
              <Typography variant="caption" color={totalSummary.totalSavings >= 0 ? 'success.main' : 'error.main'}>
                {totalSummary.totalIncome > 0 ? `${((totalSummary.totalSavings / totalSummary.totalIncome) * 100).toFixed(1)}%` : '0%'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
          <Tab label="📊 Tổng Quan & Biểu Đồ" />
          <Tab label="💬 Trợ Lý AI" />
        </Tabs>
      </Box>

      {/* Tab 1: Tổng quan & Biểu đồ */}
      {tabValue === 0 && (
        <Grid container spacing={3}>
          {/* Biểu đồ chính */}
          <Grid item xs={12}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>Biểu đồ thu chi theo thời gian</Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis tickFormatter={(value) => `${(value / 1000000).toFixed(0)}tr`} />
                  <Tooltip
                    formatter={(value, name) => [formatCurrency(value), name]}
                    labelStyle={{ color: '#000' }}
                  />
                  <Bar dataKey="Thu nhập" fill="#4caf50" name="Thu nhập" />
                  <Bar dataKey="Chi tiêu" fill="#f44336" name="Chi tiêu" />
                  <Bar dataKey="Tiết kiệm" fill="#2196f3" name="Tiết kiệm" />
                </BarChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>

          {/* Biểu đồ chi tiêu */}
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Phân Bổ Chi Tiêu Theo Danh Mục
              </Typography>
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
            </Paper>
          </Grid>

          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Top 5 Danh Mục Chi Tiêu
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={categoryData.slice(0, 5)}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis tickFormatter={(value) => `${(value / 1000000).toFixed(0)}tr`} />
                  <Tooltip
                    formatter={(value) => [formatCurrency(value), 'Chi tiêu']}
                    labelStyle={{ color: '#000' }}
                  />
                  <Bar dataKey="value" fill="#8884d8" name="Chi tiêu">
                    {categoryData.slice(0, 5).map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>

          {/* Biểu đồ thu nhập */}
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Phân Bổ Thu Nhập Theo Nguồn
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={incomeCategoryData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#82ca9d"
                    dataKey="value"
                  >
                    {incomeCategoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatCurrency(value)} />
                </PieChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>

          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Top 5 Nguồn Thu Nhập
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={incomeCategoryData.slice(0, 5)}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis tickFormatter={(value) => `${(value / 1000000).toFixed(0)}tr`} />
                  <Tooltip
                    formatter={(value) => [formatCurrency(value), 'Thu nhập']}
                    labelStyle={{ color: '#000' }}
                  />
                  <Bar dataKey="value" fill="#82ca9d" name="Thu nhập">
                    {incomeCategoryData.slice(0, 5).map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>
        </Grid>
      )}

      {/* Tab 2: Trợ lý AI */}
      {tabValue === 1 && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Trợ Lý Tài Chính AI 💬
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Hỏi tôi bất cứ điều gì về tiết kiệm, đầu tư, ngân sách, hoặc quản lý tài chính!
          </Typography>
          <Box sx={{
            height: 500,
            overflowY: 'auto',
            mb: 2,
            p: 2,
            bgcolor: 'grey.50',
            borderRadius: 1
          }}>
            {chatMessages.map((msg, index) => (
              <Box
                key={index}
                sx={{
                  display: 'flex',
                  justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                  mb: 2
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', maxWidth: '80%' }}>
                  {msg.sender === 'bot' && (
                    <Avatar sx={{ mr: 1, bgcolor: 'primary.main' }}>
                      <BotIcon />
                    </Avatar>
                  )}
                  <Paper
                    sx={{
                      p: 2,
                      bgcolor: msg.sender === 'user' ? 'primary.main' : 'white',
                      color: msg.sender === 'user' ? 'white' : 'text.primary',
                    }}
                  >
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{msg.message}</Typography>
                  </Paper>
                  {msg.sender === 'user' && (
                    <Avatar sx={{ ml: 1, bgcolor: 'secondary.main' }}>
                      <PersonIcon />
                    </Avatar>
                  )}
                </Box>
              </Box>
            ))}
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              fullWidth
              placeholder="Hỏi AI về tài chính: tiết kiệm, đầu tư, ngân sách, quản lý nợ..."
              value={userMessage}
              onChange={(e) => setUserMessage(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
            />
            <Button
              variant="contained"
              endIcon={<SendIcon />}
              onClick={handleSendMessage}
              disabled={!userMessage.trim()}
            >
              Gửi
            </Button>
          </Box>
        </Paper>
      )}
    </Container>
  );
};

export default Statistics;
