import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.removeItem('edt.purchase-intent.v1');
  sessionStorage.removeItem('edt.active-hold.v1');
  sessionStorage.removeItem('unrelated.preference');
});
