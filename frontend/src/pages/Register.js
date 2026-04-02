import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Container, Paper, TextField, Button, Typography, Box, Alert } from '@mui/material';
import { useAuth } from '../context/AuthContext';

const Register = () => {
  const [userData, setUserData] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
  });
  const [error, setError] = useState('');
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await register(userData);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.');
    }
  };

  const handleChange = (field) => (e) => {
    setUserData({ ...userData, [field]: e.target.value });
  };

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h4" align="center" gutterBottom>
            Đăng Ký Tài Khoản 📝
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Tên đăng nhập"
              margin="normal"
              value={userData.username}
              onChange={handleChange('username')}
              required
            />
            <TextField
              fullWidth
              label="Email"
              type="email"
              margin="normal"
              value={userData.email}
              onChange={handleChange('email')}
              required
            />
            <TextField
              fullWidth
              label="Họ và tên"
              margin="normal"
              value={userData.fullName}
              onChange={handleChange('fullName')}
            />
            <TextField
              fullWidth
              label="Mật khẩu"
              type="password"
              margin="normal"
              value={userData.password}
              onChange={handleChange('password')}
              required
            />
            <Button type="submit" fullWidth variant="contained" sx={{ mt: 3 }}>
              Đăng Ký
            </Button>
            <Box sx={{ mt: 2, textAlign: 'center' }}>
              <Typography variant="body2">
                Đã có tài khoản? <Link to="/login">Đăng nhập ngay</Link>
              </Typography>
            </Box>
          </form>
        </Paper>
      </Box>
    </Container>
  );
};

export default Register;
