import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  TextField,
  List,
  ListItem,
  ListItemText,
  Chip,
  Avatar,
  Divider,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  PieChart, Pie, Cell, BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Psychology as AIIcon,
  Send as SendIcon,
  Person as PersonIcon,
  SmartToy as BotIcon,
} from '@mui/icons-material';
import { transactionAPI, aiAPI, fixedIncomeAPI, supplementaryIncomeAPI, recurringExpenseAPI, incidentalExpenseAPI } from '../services/api';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D', '#FFC658'];

const AIInsights = () => {
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const [prediction, setPrediction] = useState(null);
  const [chatMessages, setChatMessages] = useState([
    { sender: 'bot', message: 'Xin chào! Tôi là trợ lý tài chính AI. Bạn cần tư vấn gì không?' }
  ]);
  const [userMessage, setUserMessage] = useState('');
  const [categoryData, setCategoryData] = useState([]);

  useEffect(() => {
    loadAnalysis();
  }, []);

  const loadAnalysis = async () => {
    setLoading(true);
    try {
      // Lấy ngày đầu và cuối tháng hiện tại
      const now = new Date();
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

      const startDate = firstDay.toISOString().split('T')[0];
      const endDate = lastDay.toISOString().split('T')[0];

      // Lấy tất cả dữ liệu song song (giống Dashboard)
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
      const totalIncome = totalFixedIncome + totalSuppIncome;

      // Tính tổng chi tiêu từ recurring expenses (active) và incidental expenses trong tháng
      const recurringExpenses = (recurringExpenseRes.data || []).filter(re => re.isActive);
      const incidentalExpenses = incidentalExpenseRes.data || [];

      const totalRecurringExpense = recurringExpenses.reduce((sum, re) => sum + (re.amount || 0), 0);
      const totalIncidentalExpense = incidentalExpenses.reduce((sum, ie) => sum + (ie.amount || 0), 0);
      const totalExpense = totalRecurringExpense + totalIncidentalExpense;

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

      const categoryDataArray = Object.entries(categoryMap).map(([name, value]) => ({
        name,
        value: parseFloat(value.toFixed(2))
      })).sort((a, b) => b.value - a.value);

      setCategoryData(categoryDataArray);

      // Prepare data for AI analysis
      const analysisData = {
        totalIncome: totalIncome,
        totalExpense: totalExpense,
        categoryBreakdown: categoryMap,
      };

      // Call AI analysis
      const analysisRes = await aiAPI.analyze(analysisData);
      setAnalysis(analysisRes.data.data);

      // Call AI prediction với dữ liệu thực tế
      const allExpenseAmounts = [
        ...recurringExpenses.map(re => re.amount || 0),
        ...incidentalExpenses.map(ie => ie.amount || 0)
      ];

      const predictionRes = await aiAPI.predict({
        pastExpenses: allExpenseAmounts,
        categoryHistory: categoryMap,
      });
      setPrediction(predictionRes.data.data);

    } catch (error) {
      console.error('Error loading AI insights:', error);
    } finally {
      setLoading(false);
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

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount); // Hiển thị số tiền thực tế không nhân 1000
  };

  const getHealthColor = (health) => {
    switch (health) {
      case 'EXCELLENT': return 'success';
      case 'GOOD': return 'info';
      case 'MODERATE': return 'warning';
      case 'POOR': return 'error';
      default: return 'default';
    }
  };

  const getHealthLabel = (health) => {
    switch (health) {
      case 'EXCELLENT': return 'Xuất sắc';
      case 'GOOD': return 'Tốt';
      case 'MODERATE': return 'Trung bình';
      case 'POOR': return 'Kém';
      default: return health;
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Phân Tích AI 🤖
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Phân tích chi tiêu thông minh và dự đoán tài chính
        </Typography>
      </Box>

      {analysis && (
        <>
          {/* Tổng quan tài chính */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Tổng Thu Nhập
                  </Typography>
                  <Typography variant="h4" color="success.main">
                    {formatCurrency(analysis.totalIncome)}
                  </Typography>
                  <TrendingUpIcon color="success" />
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Tổng Chi Tiêu
                  </Typography>
                  <Typography variant="h4" color="error.main">
                    {formatCurrency(analysis.totalExpense)}
                  </Typography>
                  <TrendingDownIcon color="error" />
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Tỷ Lệ Tiết Kiệm
                  </Typography>
                  <Typography variant="h4" color="primary.main">
                    {analysis.savingsRate?.toFixed(1)}%
                  </Typography>
                  <Chip
                    label={getHealthLabel(analysis.financialHealth)}
                    color={getHealthColor(analysis.financialHealth)}
                    sx={{ mt: 1 }}
                  />
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Biểu đồ tròn - Phân bổ chi tiêu */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
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

            {/* Top danh mục chi tiêu */}
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Top Danh Mục Chi Tiêu
                </Typography>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={categoryData.slice(0, 5)}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip formatter={(value) => formatCurrency(value)} />
                    <Bar dataKey="value" fill="#8884d8">
                      {categoryData.slice(0, 5).map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </Paper>
            </Grid>
          </Grid>

          {/* Nhận xét và Gợi ý */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <AIIcon color="primary" />
                  Nhận Xét Của AI
                </Typography>
                <List>
                  {analysis.insights?.map((insight, index) => (
                    <ListItem key={index}>
                      <ListItemText
                        primary={insight}
                        primaryTypographyProps={{ variant: 'body2' }}
                      />
                    </ListItem>
                  ))}
                </List>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <TrendingUpIcon color="success" />
                  Gợi Ý Tiết Kiệm
                </Typography>
                <List>
                  {analysis.recommendations?.map((rec, index) => (
                    <ListItem key={index}>
                      <ListItemText
                        primary={rec}
                        primaryTypographyProps={{ variant: 'body2', color: 'success.main' }}
                      />
                    </ListItem>
                  ))}
                </List>
              </Paper>
            </Grid>
          </Grid>

          {/* Dự đoán chi tiêu */}
          {prediction && (
            <Paper sx={{ p: 3, mb: 3 }}>
              <Typography variant="h6" gutterBottom>
                Dự Đoán Chi Tiêu Tháng Tới
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={4}>
                  <Alert severity="info">
                    <Typography variant="body2" gutterBottom>
                      <strong>Chi tiêu dự kiến:</strong>
                    </Typography>
                    <Typography variant="h6">
                      {formatCurrency(prediction.predictedNextMonthExpense)}
                    </Typography>
                  </Alert>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Alert severity={prediction.trend === 'INCREASING' ? 'warning' : 'success'}>
                    <Typography variant="body2" gutterBottom>
                      <strong>Xu hướng:</strong>
                    </Typography>
                    <Typography variant="h6">
                      {prediction.trend === 'INCREASING' ? '📈 Tăng' :
                       prediction.trend === 'DECREASING' ? '📉 Giảm' : '➡️ Ổn định'}
                    </Typography>
                  </Alert>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Alert severity="info">
                    <Typography variant="body2" gutterBottom>
                      <strong>Độ tin cậy:</strong>
                    </Typography>
                    <Typography variant="h6">
                      {prediction.confidenceLevel?.toFixed(0)}%
                    </Typography>
                  </Alert>
                </Grid>
              </Grid>
            </Paper>
          )}
        </>
      )}

      {/* Chatbot AI */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Trợ Lý Tài Chính AI
        </Typography>
        <Box sx={{
          height: 400,
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
              <Box sx={{ display: 'flex', alignItems: 'flex-start', maxWidth: '70%' }}>
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
                  <Typography variant="body2">{msg.message}</Typography>
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
            placeholder="Hỏi AI về tài chính của bạn..."
            value={userMessage}
            onChange={(e) => setUserMessage(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
          />
          <Button
            variant="contained"
            endIcon={<SendIcon />}
            onClick={handleSendMessage}
          >
            Gửi
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default AIInsights;
