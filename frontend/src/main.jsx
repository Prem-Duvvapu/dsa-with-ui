import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import AppRouter from './AppRouter.jsx';
import ErrorBoundary from './components/ErrorBoundary.jsx';
import './index.css';

/**
 * The app had boundaries around the two canvases and nothing above them, so a throw in the
 * sidebar, the controls or App itself rendered a white page with no way back. The canvas
 * boundaries stay: they lose one visualization instead of the whole workspace, and this
 * only catches what gets past them.
 */
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AppRouter />
      </BrowserRouter>
    </ErrorBoundary>
  </React.StrictMode>
);
