import React from 'react';
import LearningNetworkNav from './components/LearningNetworkNav';
import { Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './components/Dashboard.jsx';
import { CatalogProvider } from './catalog/CatalogProvider';
import ProblemWorkspace from './workspace/ProblemWorkspace';

/**
 * Top-level route table.
 *
 * /problem/:id  →  the problem workspace (Playground, Code walkthrough, Analysis) for that id
 * /             →  the algorithm library, with the returning learner's Continue link
 * *             →  redirect to /
 *
 * BrowserRouter lives in main.jsx so the routes can use useParams / useNavigate without a
 * second wrapper. The catalogue is loaded once here and shared by every route.
 */
export default function AppRouter() {
  return (
    <CatalogProvider><LearningNetworkNav /><Routes>
      <Route path="/problem/:id" element={<ProblemWorkspace />} />
      <Route path="/" element={<Dashboard />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes></CatalogProvider>
  );
}
