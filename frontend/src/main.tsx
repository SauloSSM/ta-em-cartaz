import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';

const root = document.getElementById('root');

if (root === null) {
  throw new Error('Root element was not found.');
}

createRoot(root).render(
  <StrictMode>
    <a href="#main-content">Pular para o conteúdo principal</a>
    <App />
  </StrictMode>,
);
