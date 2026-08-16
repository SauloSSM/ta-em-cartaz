import { chromium } from 'playwright';
import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const distDir = path.resolve(__dirname, '../dist');
const artifactDir = 'C:\\Users\\saulo\\.gemini\\antigravity-cli\\brain\\6b5823ef-7d13-4cac-a23c-2f0d10468c1a';

const sampleEvents = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    title: 'ANAVITÓRIA — TURNÊ COR',
    description: 'Show da turnê Cor no Vivo Rio',
    imageUrl: 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=800&q=80',
    category: 'Show',
    status: 'PUBLISHED',
    venueName: 'Vivo Rio',
    venueAddress: 'Rio de Janeiro — RJ',
    startsAt: '2026-09-09T20:00:00Z',
    startingPrice: 140,
    salesClosed: false,
    createdAt: '2026-08-15T10:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    title: 'FESTIVAL BALAIO 2026',
    description: 'Grande festival de música independente',
    imageUrl: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=800&q=80',
    category: 'Festival',
    status: 'PUBLISHED',
    venueName: 'Parque Municipal',
    venueAddress: 'Belo Horizonte — MG',
    startsAt: '2026-09-16T22:00:00Z',
    startingPrice: 200,
    salesClosed: false,
    createdAt: '2026-08-15T10:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    title: 'TIM MAIA EXPERIENCE',
    description: 'Tributo ao rei do soul brasileiro',
    category: 'Cultura',
    status: 'PUBLISHED',
    venueName: 'Teatro Positivo',
    venueAddress: 'Curitiba — PR',
    startsAt: '2026-09-18T19:00:00Z',
    startingPrice: 90,
    salesClosed: false,
    createdAt: '2026-08-15T10:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
  {
    id: '44444444-4444-4444-4444-444444444444',
    title: 'JÃO — SUPERTURNÊ',
    description: 'Mega show da Superturnê',
    imageUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80',
    category: 'Show',
    status: 'PUBLISHED',
    venueName: 'Pepsi On Stage',
    venueAddress: 'Porto Alegre — RS',
    startsAt: '2026-09-23T21:00:00Z',
    startingPrice: 160,
    salesClosed: true,
    createdAt: '2026-08-15T10:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
];

const mimeTypes = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
};

const server = http.createServer((req, res) => {
  const url = req.url || '/';
  
  if (url.startsWith('/api/v1/auth/session')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ authenticated: false }));
    return;
  }
  
  if (url.startsWith('/api/v1/events')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ events: sampleEvents }));
    return;
  }

  let filePath = path.join(distDir, url === '/' ? 'index.html' : url.split('?')[0]);
  if (!fs.existsSync(filePath)) {
    filePath = path.join(distDir, 'index.html');
  }

  const ext = path.extname(filePath);
  const contentType = mimeTypes[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500);
      res.end('Server Error');
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

server.listen(4173, async () => {
  const browser = await chromium.launch({
    headless: true,
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  const consoleLogs = [];
  page.on('console', (msg) => {
    consoleLogs.push({ type: msg.type(), text: msg.text() });
  });

  const viewports = [
    { name: 'home_1440x900', width: 1440, height: 900 },
    { name: 'home_1280x800', width: 1280, height: 800 },
    { name: 'home_1024x768', width: 1024, height: 768 },
    { name: 'home_768x1024', width: 768, height: 1024 },
    { name: 'home_430x932', width: 430, height: 932 },
    { name: 'home_390x844', width: 390, height: 844 },
    { name: 'home_360x800', width: 360, height: 800 },
  ];

  const results = [];

  for (const vp of viewports) {
    await page.setViewportSize({ width: vp.width, height: vp.height });
    await page.goto('http://localhost:4173', { waitUntil: 'networkidle' });

    await page.waitForSelector('.tc-event-row');

    const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
    const clientWidth = await page.evaluate(() => document.documentElement.clientWidth);
    const bodyScrollWidth = await page.evaluate(() => document.body.scrollWidth);
    const bodyClientWidth = await page.evaluate(() => document.body.clientWidth);

    const hasOverflow = scrollWidth > clientWidth || bodyScrollWidth > bodyClientWidth;

    results.push({
      viewport: `${vp.width}x${vp.height} (${vp.name})`,
      hasOverflow,
      scrollWidth,
      clientWidth,
    });

    const screenshotFile = path.join(artifactDir, `${vp.name}.png`);
    await page.screenshot({ path: screenshotFile, fullPage: true });
    console.log(`Saved screenshot: ${screenshotFile}`);
  }

  console.log('\n=== RESPONSIVE OVERFLOW AUDIT ===');
  console.table(results);

  console.log('\n=== CONSOLE LOGS AUDIT ===');
  console.log(consoleLogs.length === 0 ? 'No console warnings or errors.' : JSON.stringify(consoleLogs, null, 2));

  await browser.close();
  server.close();
  process.exit(0);
});
