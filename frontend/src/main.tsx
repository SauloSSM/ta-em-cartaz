import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import './shared/styles/global.css';

const root = document.getElementById('root');

if (root === null) {
  throw new Error('Root element was not found.');
}

createRoot(root).render(
  <StrictMode>
    <a href="#main-content" className="tc-skip-link">
      Pular para o conteúdo principal
    </a>
    <App />
  </StrictMode>,
);
