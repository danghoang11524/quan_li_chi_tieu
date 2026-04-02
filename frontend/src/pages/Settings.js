import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Avatar,
  Grid,
  Divider,
  Alert,
  Snackbar,
  Tab,
  Tabs,
} from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../services/api';

function TabPanel({ children, value, index }) {
  return (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const Settings = () => {
  const { user, loadUser } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  // Profile form
  const [profileForm, setProfileForm] = useState({
    email: '',
    fullName: '',
    phoneNumber: '',
    currency: 'VND',
    monthlyIncome: '',
    avatarUrl: '',
  });

  // Password form
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [loading, setLoading] = useState(false);

  // Load user data from AuthContext instead of making API call
  useEffect(() => {
    if (user) {
      setProfileForm({
        email: user.email || '',
        fullName: user.fullName || '',
        phoneNumber: user.phoneNumber || '',
        currency: user.currency || 'VND',
        monthlyIncome: user.monthlyIncome || '',
        avatarUrl: user.avatarUrl || '',
      });
    }
  }, [user]);

  const handleProfileChange = (e) => {
    setProfileForm({ ...profileForm, [e.target.name]: e.target.value });
  };

  const handlePasswordChange = (e) => {
    setPasswordForm({ ...passwordForm, [e.target.name]: e.target.value });
  };

  const handleAvatarUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      showSnackbar('Vui lòng chọn file ảnh', 'error');
      return;
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
      showSnackbar('Kích thước ảnh phải nhỏ hơn 5MB', 'error');
      return;
    }

    setLoading(true);
    try {
      const response = await authAPI.uploadAvatar(file);
      const avatarUrl = response.data.data.avatarUrl;

      // Update profile with new avatar URL
      setProfileForm({ ...profileForm, avatarUrl });

      // Also save to backend immediately
      await authAPI.updateProfile({ avatarUrl });

      showSnackbar('Ảnh đại diện đã được cập nhật', 'success');
    } catch (error) {
      console.error('Error uploading avatar:', error);
      showSnackbar('Không thể tải ảnh lên', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await authAPI.updateProfile(profileForm);
      showSnackbar('Cập nhật thông tin thành công!', 'success');
      // Reload user data from server
      await loadUser();
    } catch (error) {
      console.error('Error updating profile:', error);
      showSnackbar(
        error.response?.data?.message || 'Không thể cập nhật thông tin',
        'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      showSnackbar('Mật khẩu mới không khớp!', 'error');
      return;
    }

    if (passwordForm.newPassword.length < 6) {
      showSnackbar('Mật khẩu phải có ít nhất 6 ký tự!', 'error');
      return;
    }

    setLoading(true);
    try {
      await authAPI.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      showSnackbar('Đổi mật khẩu thành công!', 'success');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (error) {
      console.error('Error changing password:', error);
      showSnackbar(
        error.response?.data?.message || 'Không thể đổi mật khẩu',
        'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const showSnackbar = (message, severity) => {
    setSnackbar({ open: true, message, severity });
  };

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3}>
        <Box sx={{ p: 3 }}>
          <Typography variant="h4" gutterBottom>
            Cài đặt
          </Typography>

          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="Thông tin cá nhân" />
            <Tab label="Đổi mật khẩu" />
          </Tabs>

          <TabPanel value={tabValue} index={0}>
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
              <Avatar
                src={profileForm.avatarUrl || undefined}
                sx={{ width: 100, height: 100, mb: 2 }}
              >
                {user?.username?.charAt(0).toUpperCase()}
              </Avatar>
              <Typography variant="h6">{user?.username}</Typography>

              <input
                accept="image/*"
                style={{ display: 'none' }}
                id="avatar-upload"
                type="file"
                onChange={handleAvatarUpload}
              />
              <label htmlFor="avatar-upload">
                <Button
                  variant="outlined"
                  component="span"
                  disabled={loading}
                  sx={{ mt: 2 }}
                >
                  Tải ảnh lên
                </Button>
              </label>
            </Box>

            <form onSubmit={handleProfileSubmit}>
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="URL Ảnh đại diện"
                    name="avatarUrl"
                    value={profileForm.avatarUrl}
                    onChange={handleProfileChange}
                    placeholder="https://example.com/avatar.jpg"
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Email"
                    name="email"
                    type="email"
                    value={profileForm.email}
                    onChange={handleProfileChange}
                    required
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Họ và tên"
                    name="fullName"
                    value={profileForm.fullName}
                    onChange={handleProfileChange}
                  />
                </Grid>

                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Số điện thoại"
                    name="phoneNumber"
                    value={profileForm.phoneNumber}
                    onChange={handleProfileChange}
                  />
                </Grid>

                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    select
                    label="Tiền tệ"
                    name="currency"
                    value={profileForm.currency}
                    onChange={handleProfileChange}
                    SelectProps={{ native: true }}
                  >
                    <option value="VND">VND - Việt Nam Đồng</option>
                    <option value="USD">USD - US Dollar</option>
                    <option value="EUR">EUR - Euro</option>
                  </TextField>
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Thu nhập hàng tháng (VND)"
                    name="monthlyIncome"
                    type="number"
                    value={profileForm.monthlyIncome}
                    onChange={handleProfileChange}
                  />
                </Grid>

                <Grid item xs={12}>
                  <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    fullWidth
                    disabled={loading}
                  >
                    {loading ? 'Đang cập nhật...' : 'Cập nhật thông tin'}
                  </Button>
                </Grid>
              </Grid>
            </form>
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            <form onSubmit={handlePasswordSubmit}>
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <Alert severity="info">
                    Mật khẩu phải có ít nhất 6 ký tự
                  </Alert>
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Mật khẩu hiện tại"
                    name="currentPassword"
                    type="password"
                    value={passwordForm.currentPassword}
                    onChange={handlePasswordChange}
                    required
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Mật khẩu mới"
                    name="newPassword"
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={handlePasswordChange}
                    required
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Xác nhận mật khẩu mới"
                    name="confirmPassword"
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={handlePasswordChange}
                    required
                  />
                </Grid>

                <Grid item xs={12}>
                  <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    fullWidth
                    disabled={loading}
                  >
                    {loading ? 'Đang đổi mật khẩu...' : 'Đổi mật khẩu'}
                  </Button>
                </Grid>
              </Grid>
            </form>
          </TabPanel>
        </Box>
      </Paper>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Settings;
