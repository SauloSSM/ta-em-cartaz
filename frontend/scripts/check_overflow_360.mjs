import { chromium } from 'playwright';
import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const distDir = path.resolve(__dirname, '../dist');

const server = http.createServer((req, res) => {
  const url = req.url || '/';
  if (url.startsWith('/api/v1/auth/session')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ authenticated: false }));
    return;
  }
  if (url.startsWith('/api/v1/events')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ events: [] }));
    return;
  }
  let filePath = path.join(distDir, url === '/' ? 'index.html' : url.split('?')[0]);
  if (!fs.existsSync(filePath)) filePath = path.join(distDir, 'index.html');
  const ext = path.extname(filePath);
  const mimeTypes = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css' };
  fs.readFile(filePath, (err, content) => {
    res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' });
    res.end(content);
  });
});

server.listen(4174, async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto('http://localhost:4174', { waitUntil: 'networkidle' });

  const overflowingElements = await page.evaluate(() => {
    const docWidth = document.documentElement.clientWidth;
    const elements = Array.from(document.querySelectorAll('*'));
    return elements
      .filter((el) => {
        const rect = el.getBoundingClientRect();
        return rect.right > docWidth || rect.width > docWidth;
      })
      .map((el) => ({
        tag: el.tagName,
        className: el.className,
        id: el.id,
        rect: el.getBoundingClientRect(),
      }));
  });

  console.log('Overflowing elements on 360px:', JSON.stringify(overflowingElements, null, 2));

  await browser.close();
  server.close();
  process.exit(0);
});
