import { useState, useEffect, useRef } from 'react';
import {
  Box,
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  TextField,
  Alert,
  IconButton,
  Collapse,
  Tabs,
  Tab,
  Paper,
  CircularProgress,
  List,
  ListItem,
  Avatar,
} from '@mui/material';
import {
  WbSunny as SunnyIcon,
  TipsAndUpdates as TipsIcon,
  Refresh as RefreshIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  Psychology as AIIcon,
  Send as SendIcon,
  Person as PersonIcon,
  SmartToy as BotIcon,
} from '@mui/icons-material';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

function Insights() {
  const [tabValue, setTabValue] = useState(0);
  const [weatherAdvice, setWeatherAdvice] = useState('');
  const [spendingTips, setSpendingTips] = useState([]);
  const [financialTips, setFinancialTips] = useState([]);
  const [error, setError] = useState('');
  const [location, setLocation] = useState('Hanoi');
  const [expandedTip, setExpandedTip] = useState(null);

  // AI Coach state
  const [chatMessages, setChatMessages] = useState([]);
  const [userMessage, setUserMessage] = useState('');
  const [isAILoading, setIsAILoading] = useState(false);
  const chatEndRef = useRef(null);

  const userId = localStorage.getItem('userId') || '1';

  useEffect(() => {
    loadWeatherAdvice();
    loadSpendingTips();
    loadFinancialTips();
  }, []);

  const loadWeatherAdvice = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(
        `${API_URL}/insights/weather/advice?location=${location}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setWeatherAdvice(response.data);
    } catch (err) {
      console.error('Error loading weather advice:', err);
    }
  };

  const loadSpendingTips = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(
        `${API_URL}/insights/spending-habits/suggestions?location=${location}`,
        { headers: {
          Authorization: `Bearer ${token}`,
          'X-User-Id': userId
        } }
      );
      setSpendingTips(response.data);
    } catch (err) {
      console.error('Error loading spending tips:', err);
    }
  };

  const loadFinancialTips = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(
        `${API_URL}/insights/tips/daily?count=5`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setFinancialTips(response.data);
    } catch (err) {
      console.error('Error loading financial tips:', err);
    }
  };

  const getCategoryColor = (category) => {
    const colors = {
      SAVING: '#4CAF50',
      INVESTING: '#2196F3',
      BUDGETING: '#FF9800',
      DEBT_MANAGEMENT: '#F44336',
      EMERGENCY_FUND: '#9C27B0',
      SHOPPING: '#E91E63',
      GENERAL: '#607D8B',
      INSURANCE: '#00BCD4',
      TAX: '#FF5722',
    };
    return colors[category] || '#757575';
  };

  const getCategoryLabel = (category) => {
    const labels = {
      SAVING: 'Tiết kiệm',
      INVESTING: 'Đầu tư',
      BUDGETING: 'Ngân sách',
      DEBT_MANAGEMENT: 'Quản lý nợ',
      EMERGENCY_FUND: 'Quỹ khẩn cấp',
      SHOPPING: 'Mua sắm',
      GENERAL: 'Chung',
      INSURANCE: 'Bảo hiểm',
      TAX: 'Thuế',
    };
    return labels[category] || category;
  };

  // AI Coach functions
  const sendMessageToAI = async () => {
    if (!userMessage.trim() || isAILoading) return;

    const newUserMessage = {
      role: 'user',
      content: userMessage,
      timestamp: new Date().toISOString()
    };

    setChatMessages(prev => [...prev, newUserMessage]);
    setUserMessage('');
    setIsAILoading(true);

    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(
        `${API_URL}/insights/ai-coach/chat`,
        { message: userMessage },
        { headers: {
          Authorization: `Bearer ${token}`,
          'X-User-Id': userId
        } }
      );

      const aiMessage = {
        role: 'assistant',
        content: response.data.response,
        timestamp: new Date().toISOString()
      };

      setChatMessages(prev => [...prev, aiMessage]);
    } catch (err) {
      console.error('Error sending message to AI:', err);
      setError('Không thể kết nối với AI Coach. Vui lòng thử lại.');
    } finally {
      setIsAILoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessageToAI();
    }
  };

  useEffect(() => {
    if (chatEndRef.current) {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [chatMessages]);

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: 'primary.main' }}>
          💡 Thông Tin & Gợi Ý Tài Chính
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Nhận gợi ý thông minh dựa trên thời tiết, thói quen chi tiêu và các tips tài chính hữu ích
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)}>
          <Tab icon={<SunnyIcon />} label="Thời Tiết & Gợi Ý" />
          <Tab icon={<TipsIcon />} label="Tips Tài Chính" />
          <Tab icon={<AIIcon />} label="AI Coach Tài Chính" />
        </Tabs>
      </Box>

      {/* Tab 1: Weather & Spending Tips */}
      {tabValue === 0 && (
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <SunnyIcon color="primary" />
                    Gợi ý dựa trên thời tiết
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <TextField
                      size="small"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      placeholder="Thành phố"
                      sx={{ width: 150 }}
                    />
                    <IconButton onClick={loadWeatherAdvice} color="primary">
                      <RefreshIcon />
                    </IconButton>
                  </Box>
                </Box>
                <Paper sx={{ p: 2, bgcolor: 'background.default', whiteSpace: 'pre-line' }}>
                  <Typography>{weatherAdvice || 'Đang tải...'}</Typography>
                </Paper>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  📊 Gợi ý theo thói quen chi tiêu
                </Typography>
                {spendingTips.map((tip, index) => (
                  <Paper key={index} sx={{ p: 2, mb: 2, bgcolor: 'background.default' }}>
                    <Typography sx={{ whiteSpace: 'pre-line' }}>{tip}</Typography>
                  </Paper>
                ))}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Tab 2: Financial Tips */}
      {tabValue === 1 && (
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">
                💰 Tips Tài Chính Hàng Ngày
              </Typography>
              <Button
                startIcon={<RefreshIcon />}
                onClick={loadFinancialTips}
                variant="outlined"
              >
                Tips mới
              </Button>
            </Box>
          </Grid>

          {financialTips.map((tip) => (
            <Grid item xs={12} md={6} key={tip.id}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography variant="h6" sx={{ flex: 1 }}>
                      {tip.title}
                    </Typography>
                    <Chip
                      label={getCategoryLabel(tip.category)}
                      size="small"
                      sx={{
                        bgcolor: getCategoryColor(tip.category),
                        color: 'white',
                      }}
                    />
                  </Box>

                  <Collapse in={expandedTip === tip.id} collapsedSize={80}>
                    <Typography variant="body2" color="text.secondary">
                      {tip.content}
                    </Typography>
                  </Collapse>

                  <Button
                    size="small"
                    onClick={() => setExpandedTip(expandedTip === tip.id ? null : tip.id)}
                    endIcon={expandedTip === tip.id ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                    sx={{ mt: 1 }}
                  >
                    {expandedTip === tip.id ? 'Thu gọn' : 'Xem thêm'}
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Tab 3: AI Coach */}
      {tabValue === 2 && (
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Card sx={{ height: '70vh', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', p: 0 }}>
                {/* Header */}
                <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white' }}>
                  <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <AIIcon />
                    AI Coach Tài Chính Cá Nhân
                  </Typography>
                  <Typography variant="caption">
                    Hỏi tôi bất cứ điều gì về tài chính của bạn!
                  </Typography>
                </Box>

                {/* Chat Messages */}
                <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2, bgcolor: '#f5f5f5' }}>
                  {chatMessages.length === 0 ? (
                    <Box sx={{ textAlign: 'center', mt: 4 }}>
                      <AIIcon sx={{ fontSize: 80, color: 'primary.main', opacity: 0.3 }} />
                      <Typography variant="h6" color="text.secondary" sx={{ mt: 2 }}>
                        Chào bạn! Tôi là AI Coach tài chính của bạn.
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        Hãy hỏi tôi về chi tiêu, tiết kiệm hay đầu tư của bạn!
                      </Typography>
                      <Box sx={{ mt: 3, display: 'flex', gap: 1, flexWrap: 'wrap', justifyContent: 'center' }}>
                        {[
                          'Tháng này tôi tiêu nhiều không?',
                          'Tôi nên tiết kiệm hay đầu tư?',
                          'Phân tích chi tiêu của tôi',
                          'Cho tôi lời khuyên tài chính'
                        ].map((suggestion, idx) => (
                          <Chip
                            key={idx}
                            label={suggestion}
                            onClick={() => {
                              setUserMessage(suggestion);
                            }}
                            sx={{ cursor: 'pointer' }}
                          />
                        ))}
                      </Box>
                    </Box>
                  ) : (
                    <List sx={{ p: 0 }}>
                      {chatMessages.map((msg, idx) => (
                        <ListItem
                          key={idx}
                          sx={{
                            flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                            alignItems: 'flex-start',
                            gap: 1,
                          }}
                        >
                          <Avatar
                            sx={{
                              bgcolor: msg.role === 'user' ? 'primary.main' : 'success.main',
                            }}
                          >
                            {msg.role === 'user' ? <PersonIcon /> : <BotIcon />}
                          </Avatar>
                          <Paper
                            sx={{
                              p: 2,
                              maxWidth: '70%',
                              bgcolor: msg.role === 'user' ? 'primary.light' : 'white',
                              color: msg.role === 'user' ? 'white' : 'text.primary',
                            }}
                          >
                            <Typography variant="body1" sx={{ whiteSpace: 'pre-line' }}>
                              {msg.content}
                            </Typography>
                            <Typography variant="caption" sx={{ opacity: 0.7, mt: 1, display: 'block' }}>
                              {new Date(msg.timestamp).toLocaleTimeString('vi-VN')}
                            </Typography>
                          </Paper>
                        </ListItem>
                      ))}
                      {isAILoading && (
                        <ListItem sx={{ alignItems: 'flex-start', gap: 1 }}>
                          <Avatar sx={{ bgcolor: 'success.main' }}>
                            <BotIcon />
                          </Avatar>
                          <Paper sx={{ p: 2 }}>
                            <CircularProgress size={20} />
                            <Typography variant="body2" sx={{ ml: 1, display: 'inline' }}>
                              Đang suy nghĩ...
                            </Typography>
                          </Paper>
                        </ListItem>
                      )}
                      <div ref={chatEndRef} />
                    </List>
                  )}
                </Box>

                {/* Input Area */}
                <Box sx={{ p: 2, bgcolor: 'white', borderTop: 1, borderColor: 'divider' }}>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <TextField
                      fullWidth
                      multiline
                      maxRows={3}
                      placeholder="Nhập câu hỏi của bạn..."
                      value={userMessage}
                      onChange={(e) => setUserMessage(e.target.value)}
                      onKeyPress={handleKeyPress}
                      disabled={isAILoading}
                      variant="outlined"
                      size="small"
                    />
                    <Button
                      variant="contained"
                      onClick={sendMessageToAI}
                      disabled={!userMessage.trim() || isAILoading}
                      sx={{ minWidth: 60 }}
                    >
                      <SendIcon />
                    </Button>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </Container>
  );
}

export default Insights;
