"use strict";
(() => {
  const CURRENT = '1.3.7';
  const DAY = 24 * 60 * 60 * 1000;
  const KEY = 'bbi-legend-updates-v1';
  function parts(value) {
    const match = /^v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/.exec(value);
    return match ? match.slice(1).map(Number) : null;
  }
  function newer(candidate, current) {
    const a = parts(candidate), b = parts(current);
    if (!a || !b) return false;
    for (let i = 0; i < 3; i++) { if (a[i] !== b[i]) return a[i] > b[i]; }
    return false;
  }
  function releaseInfo(raw) {
    if (!raw || raw.draft || raw.prerelease || !parts(raw.tag_name)) return null;
    return {version: raw.tag_name, url: 'https://github.com/bbimedia/BBI-Legend-TV/releases/tag/' + encodeURIComponent(raw.tag_name)};
  }
  if (typeof module !== 'undefined' && module.exports) module.exports = {newer, releaseInfo};
  if (typeof document === 'undefined') return;
  const $ = selector => document.querySelector(selector);
  let stored = {};
  try { stored = JSON.parse(localStorage.getItem(KEY)) || {}; } catch (_) {}
  if (typeof stored !== 'object' || Array.isArray(stored)) stored = {};
  let enabled = stored.enabled !== false;
  let checkedAt = Number(stored.checkedAt) || 0;
  let release = releaseInfo(stored.release);
  let dismissed = String(stored.dismissed || '');
  let running = false;
  function save() {
    try { localStorage.setItem(KEY, JSON.stringify({enabled, checkedAt, dismissed,
      release: release ? {tag_name: release.version} : null})); } catch (_) {}
  }
  function show(manual = false) {
    const available = release && newer(release.version, CURRENT);
    $('#update-banner').classList.toggle('hidden', !available || (!manual && dismissed === release.version));
    if (available) {
      $('#update-title').textContent = 'Đã có BBI Legend TV ' + release.version;
      $('#update-link').href = release.url;
    }
  }
  async function check(manual = false) {
    if (running) return;
    if (!manual && (!enabled || (checkedAt <= Date.now() && Date.now() - checkedAt < DAY))) return;
    running = true;
    $('#update-check').disabled = true;
    $('#update-status').textContent = 'Đang kiểm tra GitHub Releases…';
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch('https://api.github.com/repos/bbimedia/BBI-Legend-TV/releases/latest', {
        signal: controller.signal, credentials: 'omit', referrerPolicy: 'no-referrer',
        headers: {Accept: 'application/vnd.github+json'}
      });
      if (!response.ok) throw new Error('GitHub unavailable');
      const next = releaseInfo(await response.json());
      if (!next) throw new Error('Unsupported release');
      release = next;
      checkedAt = Date.now();
      save();
      // Turning automatic checks off while a request is pending takes effect immediately.
      if (manual || enabled) show(manual);
      $('#update-status').textContent = newer(release.version, CURRENT)
        ? 'Có bản mới: ' + release.version + '. Xem thông báo đầu trang để tải.'
        : 'Bạn đang dùng bản mới nhất (' + CURRENT + ').';
    } catch (_) {
      // Throttle failed automatic requests too; manual retry always remains available.
      checkedAt = Date.now(); save();
      $('#update-status').textContent = 'Chưa kiểm tra được. Kiểm tra mạng hoặc thử lại sau; bạn vẫn dùng app bình thường.';
    } finally {
      clearTimeout(timer); running = false; $('#update-check').disabled = false;
    }
  }
  $('#update-auto').checked = enabled;
  $('#update-auto').addEventListener('change', () => {
    enabled = $('#update-auto').checked; save();
    if (enabled) { show(); check(); } else $('#update-banner').classList.add('hidden');
  });
  $('#update-check').addEventListener('click', () => check(true));
  $('#update-dismiss').addEventListener('click', () => {
    dismissed = release ? release.version : ''; save(); $('#update-banner').classList.add('hidden');
  });
  if (enabled) { show(); check(); }
  setInterval(() => check(), 60 * 60 * 1000);
})();
