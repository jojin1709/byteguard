/**
 * Sentinel Java Agent // ByteGuard
 * Interactive Documentation & Simulation Engine
 * Author: JOJIN JOHN
 */

document.addEventListener('DOMContentLoaded', () => {
  initMatrixCanvas();
  initPlayground();
  initPipeline();
  initCopyButtons();
});

/* ─── 1. Bytecode & Matrix Particle Canvas ──────────────────────────────── */
function initMatrixCanvas() {
  const canvas = document.getElementById('matrixCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  let width = (canvas.width = window.innerWidth);
  let height = (canvas.height = window.innerHeight);

  window.addEventListener('resize', () => {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  });

  const opcodes = [
    'INVOKESTATIC', 'LDC', 'ARETURN', 'DUP', 'ASTORE', 'LLOAD', 'LSUB', 
    'IFNONNULL', 'COMPUTE_FRAMES', '0xCAFEBABE', 'ClassNode', 'AdviceAdapter',
    'Loader.transform', 'nanoTime', 'recordScan', 'Instrumentation'
  ];

  const particles = [];
  const particleCount = Math.min(Math.floor(width / 35), 45);

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      text: opcodes[Math.floor(Math.random() * opcodes.length)],
      speed: 0.3 + Math.random() * 0.7,
      opacity: 0.15 + Math.random() * 0.35,
      fontSize: 11 + Math.random() * 5
    });
  }

  function render() {
    ctx.clearRect(0, 0, width, height);

    particles.forEach(p => {
      ctx.font = `${p.fontSize}px 'JetBrains Mono', monospace`;
      ctx.fillStyle = `rgba(56, 189, 248, ${p.opacity})`;
      ctx.fillText(p.text, p.x, p.y);

      p.y += p.speed;
      if (p.y > height + 20) {
        p.y = -20;
        p.x = Math.random() * width;
        p.text = opcodes[Math.floor(Math.random() * opcodes.length)];
      }
    });

    requestAnimationFrame(render);
  }

  render();
}

/* ─── 2. Interactive Playground Engine ─────────────────────────────────── */
const SIMULATION_DATA = {
  REPLACE: {
    badge: 'LDC + ARETURN Tree Injection',
    original: [
      { num: '01', text: 'package demo.target;' },
      { num: '02', text: '' },
      { num: '03', text: 'public class TargetService {' },
      { num: '04', text: '    public String message() {' },
      { num: '05', text: '        return "Original message";', type: 'diff-mod' },
      { num: '06', text: '    }' },
      { num: '07', text: '    public int add(int a, int b) {' },
      { num: '08', text: '        return a + b;' },
      { num: '09', text: '    }' },
      { num: '10', text: '}' }
    ],
    transformed: [
      { num: '01', text: '// Transformed In-Memory by Sentinel ASM Tree API' },
      { num: '02', text: 'public class TargetService {' },
      { num: '03', text: '    public String message() {' },
      { num: '04', text: '        // [ASM Tree] InsnList replacement' },
      { num: '05', text: '        // Opcode: LDC "Transformed message"', type: 'diff-add' },
      { num: '06', text: '        // Opcode: ARETURN', type: 'diff-add' },
      { num: '07', text: '        return "Transformed message";', type: 'diff-add' },
      { num: '08', text: '    }' },
      { num: '09', text: '    public int add(int a, int b) { // Preserved' },
      { num: '10', text: '        return a + b;' },
      { num: '11', text: '    }' },
      { num: '12', text: '}' }
    ],
    logs: [
      '[Sentinel] Agent initialized (retransformation support: true)',
      '[Sentinel] Inspecting: demo.target.TargetService',
      '[Sentinel] Transforming TargetService.message() via Tree API',
      '[Sentinel] Stack-map frames recomputed via ClassWriter.COMPUTE_FRAMES',
      '[Sentinel] Transformed: demo.target.TargetService [mode=REPLACE]',
      '[Demo Execution] TargetService.message() returned: "Transformed message"'
    ]
  },
  TRACE: {
    badge: 'AdviceAdapter + System.nanoTime() Timing',
    original: [
      { num: '01', text: 'package demo.target;' },
      { num: '02', text: '' },
      { num: '03', text: 'public class TargetService {' },
      { num: '04', text: '    public String message() {' },
      { num: '05', text: '        return "Original message";' },
      { num: '06', text: '    }' },
      { num: '07', text: '}' }
    ],
    transformed: [
      { num: '01', text: '// Transformed In-Memory via ASM AdviceAdapter' },
      { num: '02', text: 'public class TargetService {' },
      { num: '03', text: '    public String message() {' },
      { num: '04', text: '        long startTime = System.nanoTime();', type: 'diff-add' },
      { num: '05', text: '        System.out.println("[Sentinel] ENTER message()");', type: 'diff-add' },
      { num: '06', text: '        String result = "Original message";' },
      { num: '07', text: '        long elapsed = (System.nanoTime() - startTime)/1_000_000;', type: 'diff-add' },
      { num: '08', text: '        System.out.println("[Sentinel] EXIT message() - returned: " + result + " - took " + elapsed + "ms");', type: 'diff-add' },
      { num: '09', text: '        return result;' },
      { num: '10', text: '    }' },
      { num: '11', text: '}' }
    ],
    logs: [
      '[Sentinel] Agent initialized (retransformation support: true)',
      '[Sentinel] Inspecting: demo.target.TargetService',
      '[Sentinel] Injected nanoTime probes & return value capture',
      '[Sentinel] Transformed: demo.target.TargetService [mode=TRACE]',
      '[Sentinel] ENTER message()',
      '[Sentinel] EXIT  message() — returned: Original message — took 1ms',
      '[Demo Execution] TargetService.message() returned: "Original message"'
    ]
  },
  COUNT: {
    badge: 'AtomicLong Global Invocation Metrics',
    original: [
      { num: '01', text: 'package demo.target;' },
      { num: '02', text: '' },
      { num: '03', text: 'public class TargetService {' },
      { num: '04', text: '    public String message() {' },
      { num: '05', text: '        return "Original message";' },
      { num: '06', text: '    }' },
      { num: '07', text: '}' }
    ],
    transformed: [
      { num: '01', text: '// Transformed In-Memory via ASM AdviceAdapter' },
      { num: '02', text: 'public class TargetService {' },
      { num: '03', text: '    public String message() {' },
      { num: '04', text: '        long count = AgentStats.recordInvocation();', type: 'diff-add' },
      { num: '05', text: '        System.out.println("[Sentinel] message() call #" + count);', type: 'diff-add' },
      { num: '06', text: '        return "Original message";' },
      { num: '07', text: '    }' },
      { num: '08', text: '}' }
    ],
    logs: [
      '[Sentinel] Agent initialized (retransformation support: true)',
      '[Sentinel] Injected AgentStats.recordInvocation() probe',
      '[Sentinel] Transformed: demo.target.TargetService [mode=COUNT]',
      '[Sentinel] message() call #1',
      '[Sentinel] message() call #2',
      '[Sentinel] message() call #3',
      '[Demo Execution] 3 invocations tracked atomically across threads'
    ]
  },
  NULL_CHECK: {
    badge: 'Post-Return Null Guard & Warning Alert',
    original: [
      { num: '01', text: 'package demo.target;' },
      { num: '02', text: '' },
      { num: '03', text: 'public class TargetService {' },
      { num: '04', text: '    public String message() {' },
      { num: '05', text: '        return null; // Risky nullable return', type: 'diff-warn' },
      { num: '06', text: '    }' },
      { num: '07', text: '}' }
    ],
    transformed: [
      { num: '01', text: '// Transformed In-Memory via ASM AdviceAdapter' },
      { num: '02', text: 'public class TargetService {' },
      { num: '03', text: '    public String message() {' },
      { num: '04', text: '        System.out.println("[Sentinel] NULL_CHECK active on message()");', type: 'diff-add' },
      { num: '05', text: '        String ret = null;' },
      { num: '06', text: '        // Injected bytecode: DUP -> IFNONNULL -> Log Warning', type: 'diff-warn' },
      { num: '07', text: '        if (ret == null) {', type: 'diff-warn' },
      { num: '08', text: '            System.err.println("[Sentinel] NULL_CHECK WARNING: message() returned null!");', type: 'diff-warn' },
      { num: '09', text: '        }' },
      { num: '10', text: '        return ret; // Original return preserved', type: 'diff-add' },
      { num: '11', text: '    }' },
      { num: '12', text: '}' }
    ],
    logs: [
      '[Sentinel] Agent initialized (retransformation support: true)',
      '[Sentinel] Injected DUP + IFNONNULL null-check hook on ARETURN',
      '[Sentinel] Transformed: demo.target.TargetService [mode=NULL_CHECK]',
      '[Sentinel] NULL_CHECK active on message()',
      '[Sentinel] NULL_CHECK WARNING: message() returned null!'
    ]
  }
};

let currentMode = 'REPLACE';

function initPlayground() {
  const modePills = document.querySelectorAll('.mode-pill');
  modePills.forEach(pill => {
    pill.addEventListener('click', () => {
      modePills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      currentMode = pill.dataset.mode;
      renderPlaygroundCode();
    });
  });

  const runBtn = document.getElementById('runSimBtn');
  if (runBtn) {
    runBtn.addEventListener('click', runSimulationAnimation);
  }

  renderPlaygroundCode();
}

function renderPlaygroundCode() {
  const data = SIMULATION_DATA[currentMode];
  const origEditor = document.getElementById('pgOrigCode');
  const transEditor = document.getElementById('pgTransCode');
  const modeBadge = document.getElementById('pgModeBadge');

  if (modeBadge) modeBadge.textContent = data.badge;

  if (origEditor) {
    origEditor.innerHTML = data.original.map(line => `
      <div class="diff-line ${line.type || ''}">
        <span class="diff-num">${line.num}</span>
        <span class="diff-text">${escapeHtml(line.text)}</span>
      </div>
    `).join('');
  }

  if (transEditor) {
    transEditor.innerHTML = data.transformed.map(line => `
      <div class="diff-line ${line.type || ''}">
        <span class="diff-num">${line.num}</span>
        <span class="diff-text">${escapeHtml(line.text)}</span>
      </div>
    `).join('');
  }
}

function runSimulationAnimation() {
  const runBtn = document.getElementById('runSimBtn');
  const statusEl = document.getElementById('pgStatus');
  const data = SIMULATION_DATA[currentMode];

  runBtn.disabled = true;
  runBtn.innerHTML = `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
      <circle cx="12" cy="12" r="10" stroke-opacity="0.25"></circle>
      <path d="M12 2a10 10 0 0 1 10 10"></path>
    </svg> Instrumenting Bytecode...
  `;

  let i = 0;
  statusEl.textContent = 'Injecting bytecode into classloader...';

  const terminalOutput = document.getElementById('simTermBody');
  if (terminalOutput) terminalOutput.innerHTML = '';

  const interval = setInterval(() => {
    if (i < data.logs.length) {
      if (terminalOutput) {
        const logLine = document.createElement('div');
        logLine.className = 'term-line';
        logLine.innerHTML = `<span class="term-success">✓</span> <span class="term-cmd">${escapeHtml(data.logs[i])}</span>`;
        terminalOutput.appendChild(logLine);
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
      }
      i++;
    } else {
      clearInterval(interval);
      runBtn.disabled = false;
      runBtn.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="5 3 19 12 5 21 5 3"></polygon>
        </svg> Execute Simulation
      `;
      statusEl.textContent = `✓ Transformation complete [mode=${currentMode}]`;
      showToast(`Bytecode successfully mutated in ${currentMode} mode!`);
    }
  }, 220);
}

/* ─── 3. Architecture Pipeline Click Flow ──────────────────────────────── */
function initPipeline() {
  const steps = document.querySelectorAll('.pipeline-step');
  steps.forEach(step => {
    step.addEventListener('click', () => {
      steps.forEach(s => s.classList.remove('active'));
      step.classList.add('active');
    });
  });
}

/* ─── 4. Copy to Clipboard & Toast ─────────────────────────────────────── */
function initCopyButtons() {
  document.querySelectorAll('[data-copy]').forEach(btn => {
    btn.addEventListener('click', () => {
      const textToCopy = btn.getAttribute('data-copy');
      navigator.clipboard.writeText(textToCopy).then(() => {
        showToast('Copied to clipboard!');
      }).catch(() => {
        showToast('Failed to copy');
      });
    });
  });
}

function showToast(msg) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.innerHTML = `
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#38bdf8" stroke-width="2.5">
      <polyline points="20 6 9 17 4 12"></polyline>
    </svg>
    <span>${msg}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
