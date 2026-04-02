import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Add token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle 401/403 errors with automatic token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // Don't auto-clear token for profile endpoint to avoid race conditions
      const isProfileEndpoint = originalRequest?.url?.endsWith('/auth/profile');

      if (isProfileEndpoint) {
        console.warn('Profile endpoint returned 401/403 - token may be invalid');
        return Promise.reject(error);
      }

      // Don't retry refresh-token or login/register endpoints
      if (originalRequest.url?.includes('/auth/refresh-token') ||
          originalRequest.url?.includes('/auth/login') ||
          originalRequest.url?.includes('/auth/register')) {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }

      // Try to refresh token
      const refreshToken = localStorage.getItem('refreshToken');

      if (refreshToken && !originalRequest._retry) {
        if (isRefreshing) {
          // If already refreshing, queue this request
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          })
            .then(token => {
              originalRequest.headers['Authorization'] = 'Bearer ' + token;
              return api(originalRequest);
            })
            .catch(err => Promise.reject(err));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh-token`, {
            refreshToken: refreshToken
          });

          const { token: newAccessToken, refreshToken: newRefreshToken } = response.data.data;

          localStorage.setItem('token', newAccessToken);
          if (newRefreshToken) {
            localStorage.setItem('refreshToken', newRefreshToken);
          }

          api.defaults.headers.common['Authorization'] = 'Bearer ' + newAccessToken;
          originalRequest.headers['Authorization'] = 'Bearer ' + newAccessToken;

          processQueue(null, newAccessToken);

          return api(originalRequest);
        } catch (refreshError) {
          processQueue(refreshError, null);

          // Refresh failed, clear tokens and redirect to login
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
          sessionStorage.clear();

          if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login';
          }

          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      } else {
        // No refresh token, clear and redirect
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        sessionStorage.clear();

        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/auth/register', userData),
  getProfile: () => api.get('/auth/profile'),
  updateProfile: (data) => api.put('/auth/profile', data),
  changePassword: (data) => api.put('/auth/change-password', data),
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/upload/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};

// Fixed Income API
export const fixedIncomeAPI = {
  getAll: () => api.get('/fixed-income'),
  getActive: () => api.get('/fixed-income/active'),
  getById: (id) => api.get(`/fixed-income/${id}`),
  create: (data) => api.post('/fixed-income', data),
  update: (id, data) => api.put(`/fixed-income/${id}`, data),
  delete: (id) => api.delete(`/fixed-income/${id}`),
  toggle: (id) => api.patch(`/fixed-income/${id}/toggle`),
  generate: (id) => api.post(`/fixed-income/${id}/generate`),
  generateAll: () => api.post('/fixed-income/generate-all'),
  aiCategorize: (description) => api.post('/fixed-income/ai-categorize', { description }),
};

// Supplementary Income API
export const supplementaryIncomeAPI = {
  getAll: () => api.get('/supplementary-income'),
  getById: (id) => api.get(`/supplementary-income/${id}`),
  create: (data) => api.post('/supplementary-income', data),
  update: (id, data) => api.put(`/supplementary-income/${id}`, data),
  delete: (id) => api.delete(`/supplementary-income/${id}`),
  getSummary: () => api.get('/supplementary-income/summary'),
  getByDateRange: (startDate, endDate) =>
    api.get(`/supplementary-income/date-range?startDate=${startDate}&endDate=${endDate}`),
  getByCategory: (category) => api.get(`/supplementary-income/category/${category}`),
  getSummaryByDateRange: (startDate, endDate) =>
    api.get(`/supplementary-income/summary/date-range?startDate=${startDate}&endDate=${endDate}`),
};

// Recurring Expense API
export const recurringExpenseAPI = {
  getAll: () => api.get('/recurring-expenses'),
  getActive: () => api.get('/recurring-expenses/active'),
  getById: (id) => api.get(`/recurring-expenses/${id}`),
  create: (data) => api.post('/recurring-expenses', data),
  update: (id, data) => api.put(`/recurring-expenses/${id}`, data),
  delete: (id) => api.delete(`/recurring-expenses/${id}`),
  toggle: (id) => api.patch(`/recurring-expenses/${id}/toggle`),
  generate: (id) => api.post(`/recurring-expenses/${id}/generate`),
  generateAll: () => api.post('/recurring-expenses/generate-all'),
};

// Incidental Expense API
export const incidentalExpenseAPI = {
  getAll: () => api.get('/incidental-expenses'),
  getById: (id) => api.get(`/incidental-expenses/${id}`),
  create: (data) => api.post('/incidental-expenses', data),
  update: (id, data) => api.put(`/incidental-expenses/${id}`, data),
  delete: (id) => api.delete(`/incidental-expenses/${id}`),
  getSummary: () => api.get('/incidental-expenses/summary'),
  getByDateRange: (startDate, endDate) =>
    api.get(`/incidental-expenses/date-range?startDate=${startDate}&endDate=${endDate}`),
  getByCategory: (category) => api.get(`/incidental-expenses/category/${category}`),
  getSummaryByDateRange: (startDate, endDate) =>
    api.get(`/incidental-expenses/summary/date-range?startDate=${startDate}&endDate=${endDate}`),
  aiCategorize: (description) => api.post('/incidental-expenses/ai-categorize', { description }),
};

// Transaction API
export const transactionAPI = {
  getAll: () => api.get('/transactions'),
  getById: (id) => api.get(`/transactions/${id}`),
  getByDateRange: (startDate, endDate) =>
    api.get(`/transactions/date-range?startDate=${startDate}&endDate=${endDate}`),
  create: (data) => api.post('/transactions', data),
  update: (id, data) => api.put(`/transactions/${id}`, data),
  delete: (id) => api.delete(`/transactions/${id}`),
};

// Recurring Transaction API
export const recurringTransactionAPI = {
  getAll: () => api.get('/recurring-transactions'),
  getById: (id) => api.get(`/recurring-transactions/${id}`),
  create: (data) => api.post('/recurring-transactions', data),
  update: (id, data) => api.put(`/recurring-transactions/${id}`, data),
  delete: (id) => api.delete(`/recurring-transactions/${id}`),
  toggle: (id) => api.patch(`/recurring-transactions/${id}/toggle`),
  generate: (id) => api.post(`/recurring-transactions/${id}/generate`),
};

// Budget API
export const budgetAPI = {
  getAll: () => api.get('/budgets'),
  getActive: () => api.get('/budgets/active'),
  getById: (id) => api.get(`/budgets/${id}`),
  create: (data) => api.post('/budgets', data),
  update: (id, data) => api.put(`/budgets/${id}`, data),
  delete: (id) => api.delete(`/budgets/${id}`),
  checkBeforeCreate: (data) => api.post('/budgets/check-before-create', data),
};

// Savings Goal API
export const savingsAPI = {
  getAll: () => api.get('/savings'),
  getActive: () => api.get('/savings/active'),
  getById: (id) => api.get(`/savings/${id}`),
  create: (data) => api.post('/savings', data),
  update: (id, data) => api.put(`/savings/${id}`, data),
  delete: (id) => api.delete(`/savings/${id}`),
  addContribution: (id, data) => api.post(`/savings/${id}/contribute`, data),
  cancel: (id) => api.put(`/savings/${id}/cancel`),
};

// AI API
export const aiAPI = {
  analyze: (transactionData) => api.post('/ai/analyze', transactionData),
  predict: (historicalData) => api.post('/ai/predict', historicalData),
  chat: (message) => api.post('/ai/chat', { message }),
  getStatistics: (statisticsData) => api.post('/ai/statistics', statisticsData),
  categorize: (description) => api.post('/ai/categorize', { description }),
  analyzeSavingsGoal: (goalData) => api.post('/ai/analyze-savings-goal', goalData),
};

export default api;
