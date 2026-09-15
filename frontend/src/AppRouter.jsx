import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import App from './App.jsx';
import Dashboard from './components/Dashboard.jsx';

/**
 * Top-level route table.
 *
 * /problem/:id  →  the main app with that problem selected
 * /             →  the dashboard: a continue-where-you-left-off link for a returning
 *                   visitor, a start link for a first-time one. Used to redirect straight
 *                   into /problem/two-sum, so every visitor landed on the same problem
 *                   with no sense of where they'd been - see Dashboard.jsx.
 * *             →  redirect to /
 *
 * BrowserRouter lives in main.jsx so this component (and App) can use
 * useParams / useNavigate without a second wrapper.
 */
export default function AppRouter() {
  return (
    <Routes>
      <Route path="/problem/:id" element={<App />} />
      <Route path="/" element={<Dashboard />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
