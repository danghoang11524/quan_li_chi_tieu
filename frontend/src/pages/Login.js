import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Container, Paper, TextField, Button, Typography, Box, Alert } from '@mui/material';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const [credentials, setCredentials] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  // Automatically clear any old tokens when visiting login page
  useEffect(() => {
    localStorage.removeItem('token');
    sessionStorage.clear();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(credentials);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.');
    }
  };

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h4" align="center" gutterBottom>
            Hệ Thống Quản Lý Chi Tiêu 💰
          </Typography>
          <Typography variant="h6" align="center" gutterBottom>
            Đăng Nhập
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Tên đăng nhập hoặc Email"
              margin="normal"
              value={credentials.usernameOrEmail}
              onChange={(e) => setCredentials({ ...credentials, usernameOrEmail: e.target.value })}
              required
            />
            <TextField
              fullWidth
              label="Mật khẩu"
              type="password"
              margin="normal"
              value={credentials.password}
              onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
              required
            />
            <Button type="submit" fullWidth variant="contained" sx={{ mt: 3 }}>
              Đăng Nhập
            </Button>
            <Box sx={{ mt: 2, textAlign: 'center' }}>
              <Typography variant="body2">
                Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
              </Typography>
            </Box>
          </form>
        </Paper>
      </Box>
    </Container>
  );
};

export default Login;
