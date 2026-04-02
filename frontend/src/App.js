import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { AuthProvider } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import FixedIncome from './pages/FixedIncome';
import ExpenseManager from './pages/ExpenseManager';
import SavingsGoals from './pages/SavingsGoals';
import Statistics from './pages/Statistics';
import Insights from './pages/Insights';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#F97B8E', // Màu hồng đào chủ đạo
      light: '#FFB1C1',
      dark: '#E5587A',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#FF9EAA', // Màu hồng nhạt
      light: '#FFCAD4',
      dark: '#E77D8E',
    },
    success: {
      main: '#4CAF50',
    },
    warning: {
      main: '#FF9800',
    },
    error: {
      main: '#F44336',
    },
    background: {
      default: '#FFF5F7', // Nền hồng nhạt
      paper: '#FFFFFF',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontWeight: 700,
    },
    h2: {
      fontWeight: 600,
    },
    h3: {
      fontWeight: 600,
    },
    button: {
      textTransform: 'none',
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 24px',
          boxShadow: 'none',
          '&:hover': {
            boxShadow: '0 4px 12px rgba(249, 123, 142, 0.3)',
          },
        },
        contained: {
          background: 'linear-gradient(135deg, #F97B8E 0%, #FFB1C1 100%)',
          '&:hover': {
            background: 'linear-gradient(135deg, #E5587A 0%, #FF9EAA 100%)',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          boxShadow: '0 2px 12px rgba(249, 123, 142, 0.15)',
          transition: 'transform 0.2s, box-shadow 0.2s',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 8px 24px rgba(249, 123, 142, 0.25)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
        elevation1: {
          boxShadow: '0 2px 8px rgba(249, 123, 142, 0.1)',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
        colorPrimary: {
          background: 'linear-gradient(135deg, #F97B8E 0%, #FFB1C1 100%)',
          color: '#FFFFFF',
        },
      },
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="income" element={<FixedIncome />} />
              <Route path="expenses" element={<ExpenseManager />} />
              <Route path="savings" element={<SavingsGoals />} />
              <Route path="statistics" element={<Statistics />} />
              <Route path="insights" element={<Insights />} />
            </Route>
          </Routes>
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
