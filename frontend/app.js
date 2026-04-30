// ===== ML Dashboard Frontend Application Logic =====
const API = 'http://localhost:8080';
let isDataLoaded = false;
let barChartInst = null;
let pieChartInst = null;

// ===== INITIALIZATION =====
document.addEventListener('DOMContentLoaded', () => {
    checkServerStatus();
    setupUploadZone();
    updateSlider();
    toggleConfig();
});

// ===== SERVER STATUS =====
async function checkServerStatus() {
    const dot = document.querySelector('.status-dot');
    const text = document.querySelector('.status-text');
    try {
        const res = await fetch(`${API}/api/status`);
        const data = await res.json();
        dot.classList.remove('offline');
        dot.classList.add('online');
        text.textContent = data.dataLoaded ? `Aktif • ${data.recordCount} kayıt` : 'Aktif • Veri Yok';
        if (data.dataLoaded) isDataLoaded = true;
    } catch {
        dot.classList.remove('online');
        dot.classList.add('offline');
        text.textContent = 'Aktif';
    }
}

// ===== FILE UPLOAD ZONE =====
function setupUploadZone() {
    const zone = document.getElementById('uploadZone');
    const input = document.getElementById('fileUpload');
    zone.addEventListener('click', () => input.click());
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragover'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('dragover'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            input.files = e.dataTransfer.files;
            showFileInfo(e.dataTransfer.files[0].name);
        }
    });
    input.addEventListener('change', () => {
        if (input.files.length > 0) showFileInfo(input.files[0].name);
    });
}

function showFileInfo(name) {
    document.getElementById('uploadZone').style.display = 'none';
    document.getElementById('fileInfo').style.display = 'flex';
    document.getElementById('fileName').textContent = name;
}

function clearFile() {
    document.getElementById('fileUpload').value = '';
    document.getElementById('uploadZone').style.display = '';
    document.getElementById('fileInfo').style.display = 'none';
    document.getElementById('loadStats').style.display = 'none';
    setStatus('loadStatus', '', '');
}

// ===== DATA LOADING =====
async function loadData() {
    const input = document.getElementById('fileUpload');
    if (input.files.length === 0) {
        setStatus('loadStatus', '⚠ Please select an .xlsx file first.', 'error');
        return;
    }
    const btn = document.getElementById('loadBtn');
    setBtnLoading(btn, true);
    setStatus('loadStatus', '⟳ Uploading and preprocessing...', 'loading');

    try {
        const fd = new FormData();
        fd.append('file', input.files[0]);
        const res = await fetch(`${API}/api/load`, { method: 'POST', body: fd });
        const data = await res.json();
        if (data.error) throw new Error(data.error);
        isDataLoaded = true;

        setStatus('loadStatus', `✓ Dataset loaded and preprocessed successfully`, 'success');

        // Show detailed stats
        const statsEl = document.getElementById('loadStats');
        statsEl.style.display = 'grid';
        statsEl.innerHTML = `
            <div class="load-stat-item">
                <span class="stat-value">${data.recordCount.toLocaleString()}</span>
                <span class="stat-label">Records</span>
            </div>
            <div class="load-stat-item">
                <span class="stat-value">${data.skipped}</span>
                <span class="stat-label">Skipped</span>
            </div>
            <div class="load-stat-item">
                <span class="stat-value">${data.categories.length}</span>
                <span class="stat-label">Categories</span>
            </div>
            <div class="load-stat-item" style="grid-column: 1 / -1;">
                <span class="stat-value" style="font-size: 0.85rem;">${data.categories.join(' • ')}</span>
                <span class="stat-label">Detected Classes</span>
            </div>
        `;

        checkServerStatus();
    } catch (e) {
        setStatus('loadStatus', `✕ ${e.message || 'Failed to load. Is the backend running?'}`, 'error');
    } finally {
        setBtnLoading(btn, false);
    }
}

// ===== TRAINING =====
async function trainModel() {
    // Check if data is loaded (also re-check with server)
    if (!isDataLoaded) {
        try {
            const st = await fetch(`${API}/api/status`);
            const stData = await st.json();
            if (stData.dataLoaded) { isDataLoaded = true; }
            else { setStatus('trainWarning', '⚠ Please load a dataset first!', 'error'); return; }
        } catch { setStatus('trainWarning', '⚠ Backend unreachable. Start the server first.', 'error'); return; }
    }

    const algo = document.querySelector('input[name="algo"]:checked').value;
    const k = parseInt(document.getElementById('kValue').value) || 5;
    const maxDepth = parseInt(document.getElementById('maxDepth').value) || 10;
    const criterion = document.querySelector('input[name="criterion"]:checked').value;
    const trainRatio = parseInt(document.getElementById('trainRatio').value) / 100;

    const payload = { algorithm: algo, k, maxDepth, criterion, trainRatio };
    const btn = document.getElementById('trainBtn');
    setBtnLoading(btn, true);
    setStatus('trainWarning', '⟳ Training models... this may take a moment.', 'loading');

    try {
        const res = await fetch(`${API}/api/train`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.error) throw new Error(data.error);
        setStatus('trainWarning', '', '');
        displayResults(data.results);
    } catch (e) {
        setStatus('trainWarning', `✕ Training failed: ${e.message}`, 'error');
    } finally {
        setBtnLoading(btn, false);
    }
}

// ===== DISPLAY RESULTS =====
function displayResults(results) {
    // Show all result panels
    ['resultsPanel', 'cmPanel', 'chartsPanel'].forEach(id => {
        document.getElementById(id).style.display = '';
    });

    // Result cards
    let cards = '';
    results.forEach((r, idx) => {
        cards += `<div class="result-card">
            <div class="card-glow"></div>
            <h3>${escHtml(r.algorithm)}</h3>
            <div class="accuracy-display">${r.accuracy.toFixed(2)}%</div>
            <div class="metric-row"><span class="label">Training Time</span><span class="value">${r.trainTimeMs} ms</span></div>
            <div class="metric-row"><span class="label">Prediction Time</span><span class="value">${r.predictTimeMs} ms</span></div>
            <div class="metric-row"><span class="label">Total Time</span><span class="value">${r.trainTimeMs + r.predictTimeMs} ms</span></div>
        </div>`;
    });
    document.getElementById('cardsContainer').innerHTML = cards;

    // Comparison table (when both algorithms selected)
    const cmp = document.getElementById('comparisonTableContainer');
    if (results.length > 1) {
        const accWin = results[0].accuracy >= results[1].accuracy ? [' class="winner"', ''] : ['', ' class="winner"'];
        const trainWin = results[0].trainTimeMs <= results[1].trainTimeMs ? [' class="winner"', ''] : ['', ' class="winner"'];
        const predWin = results[0].predictTimeMs <= results[1].predictTimeMs ? [' class="winner"', ''] : ['', ' class="winner"'];

        cmp.innerHTML = `<table class="comparison-table">
            <tr><th>Metric</th><th>${escHtml(results[0].algorithm)}</th><th>${escHtml(results[1].algorithm)}</th></tr>
            <tr><td>Accuracy</td><td${accWin[0]}>${results[0].accuracy.toFixed(2)}%</td><td${accWin[1]}>${results[1].accuracy.toFixed(2)}%</td></tr>
            <tr><td>Training Time</td><td${trainWin[0]}>${results[0].trainTimeMs} ms</td><td${trainWin[1]}>${results[1].trainTimeMs} ms</td></tr>
            <tr><td>Prediction Time</td><td${predWin[0]}>${results[0].predictTimeMs} ms</td><td${predWin[1]}>${results[1].predictTimeMs} ms</td></tr>
            <tr><td>Total Time</td><td>${results[0].trainTimeMs + results[0].predictTimeMs} ms</td><td>${results[1].trainTimeMs + results[1].predictTimeMs} ms</td></tr>
        </table>`;
    } else { cmp.innerHTML = ''; }

    // Confusion matrices
    let cmHtml = '';
    results.forEach(r => {
        const classes = [...new Set([
            ...Object.keys(r.confusionMatrix),
            ...Object.values(r.confusionMatrix).flatMap(v => Object.keys(v))
        ])].sort();

        // Find max value for gradient scaling
        let maxVal = 0;
        classes.forEach(actual => {
            classes.forEach(pred => {
                const val = (r.confusionMatrix[actual] && r.confusionMatrix[actual][pred]) || 0;
                if (val > maxVal) maxVal = val;
            });
        });

        cmHtml += `<div class="cm-card"><h3>${escHtml(r.algorithm)}</h3><table class="cm-table"><tr><th>Actual ╲ Pred</th>`;
        classes.forEach(c => cmHtml += `<th>${escHtml(c)}</th>`);
        cmHtml += '</tr>';
        classes.forEach(actual => {
            cmHtml += `<tr><th>${escHtml(actual)}</th>`;
            classes.forEach(pred => {
                const val = (r.confusionMatrix[actual] && r.confusionMatrix[actual][pred]) || 0;
                const isDiag = actual === pred;
                let bg, txtColor;
                if (val === 0) { bg = '#f8f9fa'; txtColor = '#94a3b8'; }
                else if (isDiag) { bg = `rgba(22,163,74,${Math.min(0.85, 0.1 + (val / maxVal) * 0.75)})`; txtColor = val / maxVal > 0.3 ? '#fff' : '#1e293b'; }
                else { bg = `rgba(220,38,38,${Math.min(0.7, 0.06 + (val / maxVal) * 0.64)})`; txtColor = val / maxVal > 0.25 ? '#fff' : '#1e293b'; }
                cmHtml += `<td style="background:${bg};color:${txtColor}" title="${escHtml(actual)} → ${escHtml(pred)}: ${val}">${val}</td>`;
            });
            cmHtml += '</tr>';
        });
        cmHtml += '</table></div>';
    });
    document.getElementById('cmContainer').innerHTML = cmHtml;

    // Charts
    renderCharts(results);

    // Smooth scroll to results
    setTimeout(() => {
        document.getElementById('resultsPanel').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
}

// ===== CHARTS =====
function renderCharts(results) {
    const palette = [
        '#4f46e5', '#0284c7', '#16a34a', '#d97706', '#dc2626',
        '#7c3aed', '#0891b2', '#65a30d', '#ea580c', '#e11d48',
        '#2563eb', '#059669', '#ca8a04', '#9333ea', '#0d9488'
    ];
    const labels = [...new Set(results.flatMap(r => Object.keys(r.perClassAccuracy)))].sort();

    Chart.defaults.color = '#64748b';
    Chart.defaults.font.family = "'Inter', sans-serif";

    // Bar chart: per-class accuracy
    const barColors = ['#4f46e5', '#0284c7'];
    const barDatasets = results.map((r, i) => ({
        label: r.algorithm,
        data: labels.map(l => r.perClassAccuracy[l] || 0),
        backgroundColor: barColors[i] + 'cc',
        borderColor: barColors[i],
        borderWidth: 1,
        borderRadius: 3,
    }));

    if (barChartInst) barChartInst.destroy();
    barChartInst = new Chart(document.getElementById('barChart'), {
        type: 'bar',
        data: { labels, datasets: barDatasets },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: { font: { size: 11 }, usePointStyle: true, pointStyleWidth: 8 }
                },
                tooltip: {
                    callbacks: { label: ctx => `${ctx.dataset.label}: ${ctx.raw.toFixed(2)}%` }
                }
            },
            scales: {
                y: { beginAtZero: true, max: 100,
                    ticks: { font: { size: 10 }, callback: v => v + '%' },
                    grid: { color: '#e2e8f0' }
                },
                x: {
                    ticks: { font: { size: 9 }, maxRotation: 45 },
                    grid: { display: false }
                }
            }
        }
    });

    // Doughnut chart: category distribution
    const cm = results[0].confusionMatrix;
    const pieData = labels.map(l => {
        let sum = 0;
        if (cm[l]) Object.values(cm[l]).forEach(v => sum += v);
        return sum;
    });

    if (pieChartInst) pieChartInst.destroy();
    pieChartInst = new Chart(document.getElementById('pieChart'), {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data: pieData,
                backgroundColor: palette.slice(0, labels.length),
                borderColor: '#ffffff',
                borderWidth: 2,
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            cutout: '55%',
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        font: { size: 10 },
                        padding: 10, usePointStyle: true, pointStyleWidth: 8
                    }
                },
                tooltip: {
                    callbacks: {
                        label: ctx => {
                            const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
                            const pct = ((ctx.raw / total) * 100).toFixed(1);
                            return `${ctx.label}: ${ctx.raw} (${pct}%)`;
                        }
                    }
                }
            }
        }
    });
}

// ===== UI HELPERS =====
function toggleConfig() {
    const algo = document.querySelector('input[name="algo"]:checked').value;
    document.getElementById('knnConfig').style.display = (algo === 'KNN' || algo === 'BOTH') ? '' : 'none';
    document.getElementById('dtConfig').style.display = (algo === 'DT' || algo === 'BOTH') ? '' : 'none';
}

function updateSlider() {
    const val = document.getElementById('trainRatio').value;
    document.getElementById('trainPct').textContent = val + '%';
    document.getElementById('testPct').textContent = (100 - val) + '%';
}

function stepValue(id, delta) {
    const el = document.getElementById(id);
    const min = parseInt(el.min) || 1;
    const max = parseInt(el.max) || 100;
    el.value = Math.max(min, Math.min(max, parseInt(el.value) + delta));
}

function setStatus(id, msg, cls) {
    const el = document.getElementById(id);
    el.textContent = msg;
    el.className = 'status-msg' + (cls ? ' ' + cls : '');
}

function setBtnLoading(btn, loading) {
    btn.querySelector('.btn-text').style.display = loading ? 'none' : '';
    btn.querySelector('.btn-loader').style.display = loading ? 'inline-block' : 'none';
    btn.disabled = loading;
}

function escHtml(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}
