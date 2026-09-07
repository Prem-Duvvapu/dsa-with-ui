import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import App from './App.jsx';

/**
 * Top-level route table.
 *
 * /problem/:id  →  the main app with that problem selected
 * /             →  redirect to the default problem
 * *             →  redirect to /
 *
 * BrowserRouter lives in main.jsx so this component (and App) can use
 * useParams / useNavigate without a second wrapper.
 */
export default function AppRouter() {
  return (
    <Routes>
      <Route path="/problem/:id" element={<App />} />
      <Route path="/" element={<Navigate to="/problem/two-sum" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
