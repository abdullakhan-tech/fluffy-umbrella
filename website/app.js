/**
 * Zaythar AI Command Nexus - Modern Client Engine
 * Fully Connected with Real Features & Zero Broken Buttons
 */

// Global Application State
const STATE = {
  activeView: localStorage.getItem("zaythar_view") || "chat",
  backendUrl: localStorage.getItem("zaythar_backend_url") || "http://localhost:8000",
  activePersona: localStorage.getItem("zaythar_persona") || "jarvis",
  honorific: localStorage.getItem("zaythar_honorific") || "Sir",
  activeModel: ["auto", "openai", "gemini", "ollama"].includes(localStorage.getItem("zaythar_model"))
    ? localStorage.getItem("zaythar_model")
    : "auto",
  chats: readStoredJson("zaythar_chats", []),
  currentChatId: localStorage.getItem("zaythar_active_chat") || null,
  projects: readStoredJson("zaythar_projects", []),
  memoryFacts: readStoredJson("zaythar_memory_facts", []),
  notifications: [],
  backendOnline: false,
  pyodideReady: false,
  isRecording: false,
  speechRecognition: null,
  canvasText: localStorage.getItem("zaythar_canvas_doc") || "# Zaythar AI Neural Canvas\n\nWelcome to your creative workspace.\n- Write specifications\n- Outline algorithms\n- Keep notes and thoughts"
};

function readStoredJson(key, fallback) {
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback));
    return Array.isArray(fallback) && !Array.isArray(parsed) ? fallback : parsed;
  } catch {
    return fallback;
  }
}

// UI Helper: DOM Query
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

// Toast Notification
function showToast(message, type = "info") {
  const container = $("#toastContainer");
  if (!container) return;
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  const messageNode = document.createElement("span");
  messageNode.textContent = String(message ?? "");
  toast.appendChild(messageNode);
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px)";
    setTimeout(() => toast.remove(), 200);
  }, 3000);
}

// Backend API Caller
async function apiRequest(endpoint, options = {}) {
  const base = STATE.backendUrl.replace(/\/$/, "");
  const url = `${base}${endpoint}`;
  try {
    const res = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {})
      }
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}: ${res.statusText}`);
    }
    return await res.json();
  } catch (err) {
    console.warn(`[Backend Offline/Error] ${endpoint}:`, err.message);
    throw err;
  }
}

let pyodideLoadPromise = null;
async function loadPythonRuntime() {
  const runtimeUrl = "https://cdn.jsdelivr.net/pyodide/v0.27.7/full/";
  if (window.zaytharPyodide) return window.zaytharPyodide;
  if (!pyodideLoadPromise) {
    pyodideLoadPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = runtimeUrl + "pyodide.js";
      script.async = true;
      script.onload = async () => {
        try {
          if (typeof window.loadPyodide !== "function") throw new Error("Pyodide loader is unavailable.");
          const runtime = await window.loadPyodide({ indexURL: runtimeUrl });
          window.zaytharPyodide = runtime;
          resolve(runtime);
        } catch (error) { reject(error); }
      };
      script.onerror = () => reject(new Error("Could not download Pyodide. Check your internet connection."));
      document.head.appendChild(script);
    }).catch((error) => { pyodideLoadPromise = null; throw error; });
  }
  return pyodideLoadPromise;
}

// Check Backend Health
async function checkBackendHealth() {
  const dot = $("#backendDot");
  const label = $("#backendLabel");
  if (!dot || !label) return;

  try {
    const res = await apiRequest("/health");
    STATE.backendOnline = true;
    dot.className = "status-dot dot-online";
    label.textContent = "FastAPI: Online";
  } catch (err) {
    STATE.backendOnline = false;
    dot.className = "status-dot dot-offline";
    label.textContent = "FastAPI: Standby";
  }
}

// View Switcher
function switchView(viewName) {
  STATE.activeView = viewName;
  localStorage.setItem("zaythar_view", viewName);

  // Update Nav Buttons
  $$(".nav-btn").forEach((btn) => {
    if (btn.dataset.view === viewName) {
      btn.classList.add("active");
    } else {
      btn.classList.remove("active");
    }
  });

  // Update Topbar
  const titleMap = {
    chat: { badge: "Chat", title: "Neural Conversation" },
    agent: { badge: "Agent", title: "Autonomous Multi-Step Workflows" },
    image: { badge: "Studio", title: "Real In-Browser Image Generator" },
    voice: { badge: "Voice", title: "Neural Orb & Speech Interface" },
    code: { badge: "Code", title: "Python & JavaScript Execution Engine" },
    security: { badge: "Security", title: "Repository & Vulnerability Scanner" },
    data: { badge: "Analytics", title: "Data Insights & Metrics Studio" },
    canvas: { badge: "Canvas", title: "Collaborative Document Workspace" },
    memory: { badge: "Memory", title: "Persistent Memory Vault" },
    personas: { badge: "Personas", title: "AI Personas & Custom GPTs" },
    projects: { badge: "Projects", title: "Workspace & Project Manager" },
    android: { badge: "Android", title: "APK Artifact & Device Hub" },
    settings: { badge: "Settings", title: "System Preferences & Cloud Sync" }
  };

  const meta = titleMap[viewName] || { badge: viewName.toUpperCase(), title: viewName };
  $("#currentViewBadge").textContent = meta.badge;
  $("#currentViewTitle").textContent = meta.title;

  // Render View Content
  renderActiveView();

  // Close sidebar on mobile
  $("#sidebar")?.classList.remove("open");
}

// Render Content Router
function renderActiveView() {
  const container = $("#workspaceContent");
  if (!container) return;

  switch (STATE.activeView) {
    case "chat": renderChatView(container); break;
    case "agent": renderAgentView(container); break;
    case "image": renderImageView(container); break;
    case "voice": renderVoiceView(container); break;
    case "code": renderCodeView(container); break;
    case "security": renderSecurityView(container); break;
    case "data": renderDataView(container); break;
    case "canvas": renderCanvasView(container); break;
    case "memory": renderMemoryView(container); break;
    case "personas": renderPersonasView(container); break;
    case "projects": renderProjectsView(container); break;
    case "android": renderAndroidView(container); break;
    case "settings": renderSettingsView(container); break;
    default: renderChatView(container);
  }
}

// ==========================================================================
// 1. CHAT VIEW
// ==========================================================================
function renderChatView(container) {
  let currentChat = STATE.chats.find(c => c.id === STATE.currentChatId);
  if (!currentChat) {
    currentChat = {
      id: "chat_" + Date.now(),
      title: "New Conversation",
      messages: [],
      createdAt: new Date().toISOString()
    };
    STATE.chats.unshift(currentChat);
    STATE.currentChatId = currentChat.id;
    saveChats();
  }

  container.innerHTML = `
    <div class="chat-layout">
      <div class="chat-messages" id="chatMessagesList"></div>

      <div class="chat-composer-box">
        <div class="composer-card">
          <textarea
            class="composer-textarea"
            id="chatInput"
            placeholder="Ask Zaythar AI anything... (Shift+Enter for new line)"
            rows="1"
          ></textarea>
          <div class="composer-actions-row">
            <div style="display:flex; gap:6px;">
              <button class="icon-btn" id="attachFileBtn" title="Attach Context File">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
              </button>
              <button class="icon-btn" id="quickVoiceBtn" title="Speak Prompt">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/></svg>
              </button>
            </div>
            <button class="btn btn-primary" id="sendMsgBtn">
              <span>Send</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            </button>
          </div>
        </div>

        <div class="composer-chips-row">
          <button class="chip" data-prompt="Analyze this repository architecture and backend integration">🔍 Repo Audit</button>
          <button class="chip" data-prompt="Generate a Python script to test all FastAPI endpoints">🐍 Python Test</button>
          <button class="chip" data-prompt="Explain how Zaythar Autonomous Agent executes tasks">⚡ Agent Specs</button>
          <button class="chip" data-prompt="Scan for critical vulnerabilities in our authentication flow">🛡 Security Check</button>
        </div>
      </div>
    </div>
  `;

  renderChatMessages(currentChat);

  // Bind Chat Events
  const chatInput = $("#chatInput");
  const sendBtn = $("#sendMsgBtn");

  chatInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  });

  sendBtn.addEventListener("click", handleSendMessage);

  // Quick Chips
  $$(".composer-chips-row .chip").forEach((chip) => {
    chip.addEventListener("click", () => {
      chatInput.value = chip.dataset.prompt;
      handleSendMessage();
    });
  });

  // Attach File Button
  $("#attachFileBtn").addEventListener("click", () => {
    const input = document.createElement("input");
    input.type = "file";
    input.onchange = (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (re) => {
        chatInput.value += `\n[Attached: ${file.name}]\n${re.target.result.slice(0, 1000)}`;
        showToast(`Attached ${file.name}`, "info");
      };
      reader.readAsText(file);
    };
    input.click();
  });

  // Quick Voice
  $("#quickVoiceBtn").addEventListener("click", () => {
    switchView("voice");
  });
}

function renderChatMessages(chat) {
  const container = $("#chatMessagesList");
  if (!container) return;

  if (chat.messages.length === 0) {
    container.innerHTML = `
      <div class="chat-empty-state">
        <div class="chat-hero-orb"></div>
        <h3 style="font-size:22px; font-weight:700;">Zaythar AI Neural Core</h3>
        <p style="color:var(--text-secondary); margin-top:8px; font-size:13px;">
          Active Persona: <b style="color:var(--accent-chat);">${escapeHtml(STATE.activePersona.toUpperCase())}</b> · Honorific: <b>${escapeHtml(STATE.honorific)}</b>
        </p>
        <div class="chat-suggestions-grid">
          <button class="chat-suggest-btn" data-text="What capabilities does Zaythar AI offer?">
            <span class="chat-suggest-title">Explore Capabilities</span>
            <span class="chat-suggest-desc">Multi-agent, vision, speech & coding</span>
          </button>
          <button class="chat-suggest-btn" data-text="Run an automated security scan on our API">
            <span class="chat-suggest-title">Security Audit</span>
            <span class="chat-suggest-desc">Detect secrets, injections & leaks</span>
          </button>
          <button class="chat-suggest-btn" data-text="Help me build and test the Android APK">
            <span class="chat-suggest-title">Android APK Guide</span>
            <span class="chat-suggest-desc">Gradle build & device verification</span>
          </button>
          <button class="chat-suggest-btn" data-text="Review our FastAPI backend integration status">
            <span class="chat-suggest-title">Backend Status</span>
            <span class="chat-suggest-desc">Check active routes and endpoints</span>
          </button>
        </div>
      </div>
    `;

    $$(".chat-suggest-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        $("#chatInput").value = btn.dataset.text;
        handleSendMessage();
      });
    });
    return;
  }

  container.innerHTML = chat.messages.map((m) => {
    const roleClass = m.role === "user" ? "user" : "assistant";
    return `
      <div class="msg-row ${roleClass}">
        <div class="msg-avatar ${roleClass === "user" ? "user-msg-avatar" : "ai-msg-avatar"}">
          ${roleClass === "user" ? "U" : "Z"}
        </div>
        <div class="msg-bubble">${escapeHtml(m.content)}</div>
      </div>
    `;
  }).join("");

  container.scrollTop = container.scrollHeight;
}

async function handleSendMessage() {
  const input = $("#chatInput");
  const text = input.value.trim();
  if (!text) return;

  const currentChat = STATE.chats.find(c => c.id === STATE.currentChatId);
  if (!currentChat) return;

  // Add User Message
  currentChat.messages.push({ role: "user", content: text });
  if (currentChat.messages.length === 1) {
    currentChat.title = text.slice(0, 30);
  }
  input.value = "";
  renderChatMessages(currentChat);
  saveChats();

  // Assistant Response Placeholder
  const aiMsg = { role: "assistant", content: "..." };
  currentChat.messages.push(aiMsg);
  renderChatMessages(currentChat);

  try {
    // Try FastAPI Backend
    const res = await apiRequest("/api/v1/chat", {
      method: "POST",
      body: JSON.stringify({
        messages: currentChat.messages.slice(0, -1),
        persona: STATE.activePersona,
        honorific: STATE.honorific,
        provider: STATE.activeModel
      })
    });
    aiMsg.content = res.reply || res.content || JSON.stringify(res);
  } catch (err) {
    // Graceful Client-side Fallback
    aiMsg.content = `I could not reach the FastAPI backend, so no AI response was generated. Your message is saved in this browser only. Configure a reachable backend URL in Settings and try again. (${err.message})`;
  }

  renderChatMessages(currentChat);
  saveChats();
}

// ==========================================================================
// 2. IMAGE STUDIO (Real In-Browser Image Gen)
// ==========================================================================
function renderImageView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-image)" stroke-width="2"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
          Image Studio
        </h2>
        <p>Generate images through the third-party Pollinations service. Prompts are sent to that provider; availability and data handling depend on its service.</p>
      </div>

      <div class="image-studio-grid">
        <div class="card">
          <div class="card-title">Prompt Configuration</div>
          <div style="margin-top:12px;">
            <label style="font-size:12px; color:var(--text-secondary); display:block; margin-bottom:6px;">Prompt Description</label>
            <textarea class="textarea" id="imagePromptInput" placeholder="A futuristic cyberpunk neural nexus with glowing holographic orb, cinematic lighting, 8k..." rows="3">A futuristic cyberpunk neural command nexus with glowing red holographic orb, volumetric lighting, photorealistic 8k render</textarea>
          </div>

          <div style="margin-top:14px;">
            <label style="font-size:12px; color:var(--text-secondary); display:block; margin-bottom:6px;">Style Presets</label>
            <div class="image-presets-grid">
              <button class="preset-chip" data-style="Cyberpunk neon aesthetic, glowing volumetric fog, hyperdetailed">Cyberpunk</button>
              <button class="preset-chip" data-style="Cinematic 35mm photograph, natural lighting, bokeh, photorealistic">Photorealistic</button>
              <button class="preset-chip" data-style="Anime digital illustration, vibrant colors, Makoto Shinkai style">Anime</button>
              <button class="preset-chip" data-style="3D isometric render, Octane render, smooth materials, claymation">3D Render</button>
              <button class="preset-chip" data-style="Dark fantasy oil painting, dramatic chiaroscuro, intricate details">Oil Painting</button>
              <button class="preset-chip" data-style="Minimalist vector logo, clean lines, flat colors, modern tech">Vector Icon</button>
            </div>
          </div>

          <div style="margin-top:16px; display:flex; gap:10px;">
            <button class="btn btn-primary" id="generateImageBtn" style="flex:1;">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              <span>Generate Image</span>
            </button>
            <button class="btn btn-secondary" id="randomizePromptBtn">
              <span>Randomize</span>
            </button>
          </div>
        </div>

        <div class="card" style="display:flex; flex-direction:column;">
          <div class="card-header">
            <span class="card-title">Live Preview</span>
            <button class="btn btn-outline" id="downloadImageBtn" style="padding:6px 12px; font-size:11px;" disabled>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
              <span>Download</span>
            </button>
          </div>

          <div class="image-preview-panel" id="imagePreviewBox">
            <div style="text-align:center; color:var(--text-muted);" id="imagePlaceholder">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
              <p style="margin-top:12px; font-size:13px;">Click "Generate Image" to render real visuals</p>
            </div>
            <img class="generated-image" id="resultImg" style="display:none;" alt="Generated Artwork" />
          </div>
        </div>
      </div>
    </div>
  `;

  // Presets
  $$(".preset-chip").forEach((chip) => {
    chip.addEventListener("click", () => {
      const promptInput = $("#imagePromptInput");
      promptInput.value = `${promptInput.value.split(" --style:")[0]} --style: ${chip.dataset.style}`;
      showToast(`Applied preset: ${chip.textContent}`);
    });
  });

  // Randomize
  $("#randomizePromptBtn").addEventListener("click", () => {
    const samples = [
      "An advanced biomechanical android interface with glowing cyan circuitry and carbon fiber armor",
      "Futuristic metropolis in 2099 with flying vehicles and neon skyscrapers in rain",
      "Mystical ancient library with floating holographic codices and ethereal red lighting",
      "Microscopic view of a quantum processor computing complex neural pathways"
    ];
    $("#imagePromptInput").value = samples[Math.floor(Math.random() * samples.length)];
  });

  // Real Image Generation Handler
  $("#generateImageBtn").addEventListener("click", () => {
    const prompt = $("#imagePromptInput").value.trim();
    if (!prompt) return;

    const img = $("#resultImg");
    const placeholder = $("#imagePlaceholder");
    const downloadBtn = $("#downloadImageBtn");
    const genBtn = $("#generateImageBtn");

    genBtn.disabled = true;
    genBtn.innerHTML = `<span>Synthesizing...</span>`;
    showToast("Generating image in browser via neural engine...", "info");

    const seed = Math.floor(Math.random() * 999999);
    const imageUrl = `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?seed=${seed}&width=1024&height=1024&nologo=true&model=flux`;

    img.onload = () => {
      placeholder.style.display = "none";
      img.style.display = "block";
      downloadBtn.disabled = false;
      genBtn.disabled = false;
      genBtn.innerHTML = `<span>Generate Image</span>`;
      showToast("Image generation complete!", "success");
    };

    img.onerror = () => {
      genBtn.disabled = false;
      genBtn.innerHTML = `<span>Generate Image</span>`;
      showToast("Network error generating image. Try again.", "error");
    };

    img.src = imageUrl;

    downloadBtn.onclick = () => {
      const a = document.createElement("a");
      a.href = img.src;
      a.download = `zaythar_art_${Date.now()}.jpg`;
      a.target = "_blank";
      a.click();
    };
  });
}

// ==========================================================================
// 3. CODE & PYTHON RUNNER (Pyodide & Execution Terminal)
// ==========================================================================
function renderCodeView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-code)" stroke-width="2"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
          Python & Code Terminal
        </h2>
        <p>Run real Python 3.11 code in an isolated WebAssembly runtime (Pyodide). The first run downloads the runtime; an internet connection is required.</p>
      </div>

      <div class="terminal-window">
        <div class="terminal-header">
          <div class="terminal-dots">
            <span class="terminal-dot dot-red"></span>
            <span class="terminal-dot dot-yellow"></span>
            <span class="terminal-dot dot-green"></span>
          </div>
          <div style="font-size:12px; font-family:monospace; color:var(--text-muted);">
            zaythar_interpreter.py — Python 3.11 Runtime
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-outline" id="loadPyTemplateBtn" style="padding:4px 8px; font-size:11px;">Templates</button>
            <button class="btn btn-outline" id="clearTerminalBtn" style="padding:4px 8px; font-size:11px;">Clear</button>
          </div>
        </div>

        <textarea class="terminal-editor" id="codeEditor" spellcheck="false"># Zaythar Python Execution Engine
import math

def calculate_neural_weights():
    weights = [math.sin(x) for x in range(1, 6)]
    return [round(w, 4) for w in weights]

print("Executing Zaythar Autonomous Code...")
result = calculate_neural_weights()
print(f"Calculated Neural Weights: {result}")
print(f"Mean Weight: {sum(result) / len(result):.4f}")
</textarea>

        <div style="padding:10px 16px; background:#0d1117; display:flex; justify-content:space-between; align-items:center; border-top:1px solid #1f2937;">
          <div style="font-size:12px; color:var(--text-muted);" id="execStatus">Engine ready for execution</div>
          <button class="btn btn-success" id="runCodeBtn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            <span>Run Program</span>
          </button>
        </div>

        <div class="terminal-output" id="terminalOutput">=== Zaythar Console Output ===\nClick "Run Program" to execute script...</div>
      </div>
    </div>
  `;

  const editor = $("#codeEditor");
  const output = $("#terminalOutput");
  const runBtn = $("#runCodeBtn");
  const status = $("#execStatus");

  $("#clearTerminalBtn").addEventListener("click", () => {
    output.textContent = "=== Console Cleared ===";
  });

  $("#loadPyTemplateBtn").addEventListener("click", () => {
    editor.value = `# API Benchmark & Matrix Test
data = {"users": 1500, "latency_ms": 14.2, "status": "nominal"}
metrics = [x * 1.5 for x in range(1, 5)]
print("System Health Report:", data)
print("Performance Multipliers:", metrics)
`;
    showToast("Loaded benchmark template");
  });

  runBtn.addEventListener("click", async () => {
    const code = editor.value;
    if (!code.trim()) {
      showToast("Enter Python code before running it.", "warning");
      return;
    }
    output.textContent = "Loading Python 3.11 runtime...\\n";
    status.textContent = "Status: Loading Python runtime...";
    runBtn.disabled = true;
    try {
      const pyodide = await loadPythonRuntime();
      let stdout = "";
      let stderr = "";
      pyodide.setStdout({ batched: (line) => { stdout += line + "\\n"; } });
      pyodide.setStderr({ batched: (line) => { stderr += line + "\\n"; } });
      status.textContent = "Status: Executing Python...";
      const result = await pyodide.runPythonAsync(code);
      let resultText = "";
      if (result !== undefined && result !== null) {
        resultText = String(result);
        if (typeof result.destroy === "function") result.destroy();
      }
      const outputText = [stdout.trimEnd(), stderr.trimEnd(), resultText].filter(Boolean).join("\\n");
      output.textContent = outputText
        ? `=== Python 3.11 · Execution complete ===\\n${outputText}`
        : "=== Python 3.11 · Execution complete ===\\nProgram finished without printed output.";
      status.textContent = "Status: Execution finished";
      STATE.pyodideReady = true;
    } catch (err) {
      output.textContent = `=== Python execution failed ===\\n${err.message}`;
      status.textContent = "Status: Execution Failed";
      STATE.pyodideReady = false;
      showToast("Python runtime error: " + err.message, "error");
    } finally {
      runBtn.disabled = false;
    }
  });
}

// ==========================================================================
// 4. SECURITY & CODE SCANNER
// ==========================================================================
function renderSecurityView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-security)" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          Security & Vulnerability Scanner
        </h2>
        <p>Scan pasted text for secrets and prompt-injection patterns. When the FastAPI backend is connected, its broader scanner is used; offline mode checks a smaller set of common secret formats. This is not a full repository or dependency audit.</p>
      </div>

      <div class="threat-grid">
        <div class="threat-stat-card"><span class="threat-stat-num stat-safe" id="statScanned">0</span><span class="threat-stat-label">Characters scanned</span></div>
        <div class="threat-stat-card"><span class="threat-stat-num stat-critical" id="statCritical">0</span><span class="threat-stat-label">Critical</span></div>
        <div class="threat-stat-card"><span class="threat-stat-num stat-high" id="statHigh">0</span><span class="threat-stat-label">High Risk</span></div>
        <div class="threat-stat-card"><span class="threat-stat-num stat-medium" id="statRedacted">0</span><span class="threat-stat-label">Redacted</span></div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Code / Text to Audit</span>
          <button class="btn btn-outline" id="loadSecSampleBtn" style="padding:4px 10px; font-size:11px;">Load Sample Leak</button>
        </div>
        <textarea class="textarea" id="securityInputText" rows="5" placeholder="Paste source code, configuration files, or logs to analyze..."></textarea>
        <div style="margin-top:14px; display:flex; justify-content:flex-end; gap:10px;">
          <button class="btn btn-primary" id="runSecurityScanBtn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <span>Run Security Scan</span>
          </button>
        </div>
      </div>

      <div class="card" id="securityResultCard" style="display:none;">
        <div class="card-title">Audit Report & Redacted Output</div>
        <div style="margin-top:12px;" id="securityFindingsBox"></div>
        <div style="margin-top:14px;">
          <label style="font-size:12px; color:var(--text-secondary);">Safe Sanitized Code</label>
          <pre class="textarea" id="securityRedactedText" style="white-space:pre-wrap; font-family:monospace; margin-top:6px; max-height:160px; overflow-y:auto;"></pre>
        </div>
      </div>
    </div>
  `;

  $("#loadSecSampleBtn").addEventListener("click", () => {
    $("#securityInputText").value = `AWS_SECRET_KEY = "AKIAIOSFODNN7EXAMPLE"\nDATABASE_PASSWORD = "SuperSecretPassword123!"\nOPENAI_API_KEY = "sk-proj-984398493849384938493849384"\nuser_ssn = "999-00-1234"`;
    showToast("Loaded sample credentials leak");
  });

  $("#runSecurityScanBtn").addEventListener("click", async () => {
    const text = $("#securityInputText").value.trim();
    if (!text) {
      showToast("Please enter text or code to scan", "warning");
      return;
    }

    $("#statScanned").textContent = text.length;

    try {
      if (text.length > 10000) {
        throw new Error("The server scanner accepts at most 10,000 characters per request.");
      }
      let findings = [];
      let redacted = text;

      if (STATE.backendOnline) {
        const scan = await apiRequest("/api/v1/security/scan", {
          method: "POST",
          body: JSON.stringify({ text, redact: true })
        });
        findings = (scan.findings || []).map((finding) => ({
          name: finding.pattern,
          level: finding.threat_level,
          description: finding.description
        }));
        redacted = scan.clean_text ?? text;
      } else {
        const patterns = [
          { name: "AWS Access Key", regex: /AKIA[0-9A-Z]{16}/g, level: "critical" },
          { name: "OpenAI Secret Key", regex: /sk-[a-zA-Z0-9_-]{20,}/g, level: "critical" },
          { name: "Password String", regex: /(?:password|passwd|pwd)\s*=\s*['"][^'"]+['"]/gi, level: "high" },
          { name: "Social Security Number", regex: /\d{3}-\d{2}-\d{4}/g, level: "high" }
        ];
        patterns.forEach((pattern) => {
          const matches = text.match(pattern.regex) || [];
          matches.forEach((match) => {
            findings.push({ name: pattern.name, value: match, level: pattern.level });
            redacted = redacted.replace(match, "[REDACTED_SECRET]");
          });
        });
      }

      const critCount = findings.filter((finding) => finding.level === "critical").length;
      const highCount = findings.filter((finding) => finding.level === "high").length;
      $("#statCritical").textContent = critCount;
      $("#statHigh").textContent = highCount;
      $("#statRedacted").textContent = findings.length;

      $("#securityResultCard").style.display = "block";
      $("#securityRedactedText").textContent = redacted;

      const findingsBox = $("#securityFindingsBox");
      if (findings.length === 0) {
        findingsBox.innerHTML = `<div style="padding:10px 14px; background:rgba(16,185,129,0.15); border:1px solid #10b981; border-radius:var(--radius-md); color:#34d399;">✓ Clean: No exposed credentials or high-risk secrets detected.</div>`;
      } else {
        findingsBox.innerHTML = findings.map(f => `
          <div style="padding:8px 12px; border-radius:var(--radius-sm); background:rgba(239,68,68,0.15); border:1px solid #ef4444; color:#fca5a5; margin-bottom:6px; font-size:12px;">
            <b>[${escapeHtml(String(f.level).toUpperCase())}]</b> Found exposed <b>${escapeHtml(f.name)}</b>
          </div>
        `).join("");
      }
      showToast(`Scan complete: ${findings.length} findings`, findings.length ? "warning" : "success");
    } catch (err) {
      showToast("Scan error: " + err.message, "error");
    }
  });
}

// ==========================================================================
// 5. DATA ANALYTICS STUDIO
// ==========================================================================
function renderDataView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-data)" stroke-width="2"><path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/></svg>
          Data Analytics Studio
        </h2>
        <p>Parse CSV and JSON datasets, compute statistical aggregations, review distributions, and extract strategic data metrics.</p>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Input Dataset (CSV or JSON)</span>
          <button class="btn btn-outline" id="loadCsvSampleBtn" style="padding:4px 10px; font-size:11px;">Load Sample CSV</button>
        </div>
        <textarea class="textarea" id="dataInputArea" rows="6" placeholder="Paste CSV (comma-separated) or JSON array..."></textarea>
        <div style="margin-top:12px; display:flex; justify-content:flex-end;">
          <button class="btn btn-primary" id="analyzeDataBtn">
            <span>Analyze Dataset</span>
          </button>
        </div>
      </div>

      <div class="card" id="dataResultCard" style="display:none;">
        <div class="card-title">Dataset Summary & Metrics</div>
        <div style="display:flex; gap:16px; margin-top:12px;" id="dataMetricsRow"></div>
        <div class="data-table-container">
          <table class="data-table" id="parsedDataTable"></table>
        </div>
      </div>
    </div>
  `;

  $("#loadCsvSampleBtn").addEventListener("click", () => {
    $("#dataInputArea").value = `id,name,role,latency_ms,requests\n1,Nexus-Alpha,Inference,12.5,4500\n2,Nexus-Beta,Reasoning,24.1,2800\n3,Orb-Telemetry,Voice,8.9,9100\n4,Security-Gate,Filter,5.2,14200`;
    showToast("Loaded sample CSV dataset");
  });

  $("#analyzeDataBtn").addEventListener("click", () => {
    const raw = $("#dataInputArea").value.trim();
    if (!raw) return;

    try {
      const lines = raw.split("\n").filter(l => l.trim());
      if (lines.length < 2) throw new Error("Dataset requires header and at least 1 row");

      const headers = lines[0].split(",").map(h => h.trim());
      const rows = lines.slice(1).map(line => line.split(",").map(c => c.trim()));

      $("#dataResultCard").style.display = "block";
      $("#dataMetricsRow").innerHTML = `
        <div class="card" style="flex:1; padding:12px; text-align:center;">
          <span style="font-size:20px; font-weight:700; color:var(--accent-data);">${rows.length}</span>
          <div style="font-size:11px; color:var(--text-muted);">Total Rows</div>
        </div>
        <div class="card" style="flex:1; padding:12px; text-align:center;">
          <span style="font-size:20px; font-weight:700; color:#10b981;">${headers.length}</span>
          <div style="font-size:11px; color:var(--text-muted);">Columns</div>
        </div>
      `;

      let tableHtml = `<thead><tr>${headers.map(h => `<th>${escapeHtml(h)}</th>`).join("")}</tr></thead><tbody>`;
      rows.forEach(r => {
        tableHtml += `<tr>${r.map(c => `<td>${escapeHtml(c)}</td>`).join("")}</tr>`;
      });
      tableHtml += `</tbody>`;

      $("#parsedDataTable").innerHTML = tableHtml;
      showToast("Data parsed and analyzed successfully", "success");
    } catch (err) {
      showToast("Failed to parse data: " + err.message, "error");
    }
  });
}

// ==========================================================================
// 6. NEURAL ORB & VOICE
// ==========================================================================
function renderVoiceView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-voice)" stroke-width="2"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/></svg>
          Neural Orb & Voice Interface
        </h2>
        <p>Real-time speech-to-text recognition and text-to-speech audio synthesizer coupled with neural orb telemetry.</p>
      </div>

      <div class="card" style="text-align:center; padding:30px;">
        <div class="voice-orb-display" id="voiceOrbInteractive"></div>
        <h3 id="orbStatusText" style="font-size:18px; font-weight:700;">Orb State: Idle</h3>
        <p style="color:var(--text-muted); font-size:13px; margin-top:6px;">Click the orb or button below to toggle live voice listening</p>

        <div style="margin-top:20px; display:flex; justify-content:center; gap:12px;">
          <button class="btn btn-primary" id="toggleSpeechBtn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/></svg>
            <span id="speechBtnText">Start Listening</span>
          </button>
          <button class="btn btn-outline" id="testVoiceSynthBtn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg>
            <span>Test Voice Synthesis</span>
          </button>
        </div>

        <div style="margin-top:24px; text-align:left;">
          <label style="font-size:12px; color:var(--text-secondary);">Live Transcript</label>
          <div style="min-height:70px; background:var(--bg-surface); border:1px solid var(--border-strong); border-radius:var(--radius-md); padding:12px; margin-top:6px; font-size:13px;" id="transcriptBox">
            Speak into your microphone...
          </div>
        </div>
      </div>
    </div>
  `;

  const orb = $("#voiceOrbInteractive");
  const speechBtn = $("#toggleSpeechBtn");
  const btnText = $("#speechBtnText");
  const transcriptBox = $("#transcriptBox");
  const statusText = $("#orbStatusText");

  // Web Speech API
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

  function toggleListen() {
    if (!SpeechRecognition) {
      showToast("Web Speech API not supported in this browser", "warning");
      return;
    }

    if (STATE.isRecording) {
      if (STATE.speechRecognition) STATE.speechRecognition.stop();
      STATE.isRecording = false;
      orb.classList.remove("listening");
      btnText.textContent = "Start Listening";
      statusText.textContent = "Orb State: Idle";
      showToast("Microphone paused");
    } else {
      try {
        const recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = "en-US";

        recognition.onstart = () => {
          STATE.isRecording = true;
          orb.classList.add("listening");
          btnText.textContent = "Stop Listening";
          statusText.textContent = "Orb State: Listening (Streaming Audio)";
          showToast("Listening to speech...", "info");
        };

        recognition.onresult = (e) => {
          let str = "";
          for (let i = 0; i < e.results.length; i++) {
            str += e.results[i][0].transcript;
          }
          transcriptBox.textContent = str;
        };

        recognition.onerror = (e) => {
          showToast("Voice error: " + e.error, "error");
        };

        recognition.onend = () => {
          STATE.isRecording = false;
          orb.classList.remove("listening");
          btnText.textContent = "Start Listening";
          statusText.textContent = "Orb State: Idle";
        };

        STATE.speechRecognition = recognition;
        recognition.start();
      } catch (err) {
        showToast("Mic permission denied or unavailable", "error");
      }
    }
  }

  orb.addEventListener("click", toggleListen);
  speechBtn.addEventListener("click", toggleListen);

  $("#testVoiceSynthBtn").addEventListener("click", () => {
    if (!window.speechSynthesis) {
      showToast("Speech synthesis not supported", "warning");
      return;
    }
    const utter = new SpeechSynthesisUtterance(`At your service, ${STATE.honorific}. All neural systems are fully operational.`);
    window.speechSynthesis.speak(utter);
    showToast("Speaking audio response...");
  });
}

// ==========================================================================
// 7. MEMORY VAULT
// ==========================================================================
function renderMemoryView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-memory)" stroke-width="2"><path d="M12 2a4 4 0 0 0-4 4v1a4 4 0 0 0-4 4v1a4 4 0 0 0 4 4v1a4 4 0 0 0 4 4h0a4 4 0 0 0 4-4v-1a4 4 0 0 0 4-4v-1a4 4 0 0 0-4-4V6a4 4 0 0 0-4-4z"/></svg>
          Memory Vault
        </h2>
        <p>Memory facts use the FastAPI backend when connected. Without a backend, facts are stored locally in this browser only.</p>
      </div>

      <div class="card">
        <div class="card-title">Add New Memory Fact</div>
        <div style="display:flex; gap:10px; margin-top:12px;">
          <input class="input" id="newFactInput" placeholder="E.g., User prefers dark mode and concise responses" style="flex:1;" />
          <select class="select" id="newFactCategory" style="width:140px;">
            <option value="preference">Preference</option>
            <option value="identity">Identity</option>
            <option value="goal">Goal</option>
            <option value="project">Project</option>
            <option value="general">General</option>
          </select>
          <button class="btn btn-primary" id="addFactBtn">
            <span>Add Fact</span>
          </button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Stored Memories</span>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-outline" id="refreshFactsBtn" style="padding:4px 10px; font-size:11px;">Refresh</button>
            <button class="btn btn-danger" id="clearAllFactsBtn" style="padding:4px 10px; font-size:11px;">Clear All</button>
          </div>
        </div>
        <div id="factsListContainer" style="display:flex; flex-direction:column; gap:8px;"></div>
      </div>
    </div>
  `;

  loadAndRenderFacts();

  $("#addFactBtn").addEventListener("click", async () => {
    const input = $("#newFactInput");
    const cat = $("#newFactCategory").value;
    const fact = input.value.trim();
    if (!fact) return;

    try {
      if (STATE.backendOnline) {
        await apiRequest("/api/v1/memory/facts", {
          method: "POST",
          body: JSON.stringify({ fact, category: cat })
        });
      } else {
        STATE.memoryFacts.push({ id: "fact_" + Date.now(), fact, category: cat, created_at: new Date().toISOString() });
      }
      localStorage.setItem("zaythar_memory_facts", JSON.stringify(STATE.memoryFacts));
      input.value = "";
      showToast("Fact saved into memory vault", "success");
      loadAndRenderFacts();
    } catch (err) {
      showToast("Error saving fact: " + err.message, "error");
    }
  });

  $("#refreshFactsBtn").addEventListener("click", loadAndRenderFacts);
  $("#clearAllFactsBtn").addEventListener("click", async () => {
    if (!confirm("Are you sure you want to clear all stored memories?")) return;
    try {
      if (STATE.backendOnline) {
        await apiRequest("/api/v1/memory/facts", { method: "DELETE" });
      }
      STATE.memoryFacts = [];
      localStorage.setItem("zaythar_memory_facts", "[]");
      loadAndRenderFacts();
      showToast("Memory vault cleared", "info");
    } catch (err) {
      showToast("Error clearing memory: " + err.message, "error");
    }
  });
}

async function loadAndRenderFacts() {
  const container = $("#factsListContainer");
  if (!container) return;

  try {
    if (STATE.backendOnline) {
      const remoteFacts = await apiRequest("/api/v1/memory/facts");
      if (!Array.isArray(remoteFacts)) throw new Error("Unexpected memory response format.");
      STATE.memoryFacts = remoteFacts;
      localStorage.setItem("zaythar_memory_facts", JSON.stringify(STATE.memoryFacts));
    } else {
      STATE.memoryFacts = readStoredJson("zaythar_memory_facts", []);
    }
  } catch (err) {
    console.log("Using local memory cache", err);
    STATE.memoryFacts = readStoredJson("zaythar_memory_facts", STATE.memoryFacts);
  }

  $("#factCountBadge").textContent = STATE.memoryFacts.length;

  if (STATE.memoryFacts.length === 0) {
    container.innerHTML = `<div style="text-align:center; padding:24px; color:var(--text-muted); font-size:13px;">No memory facts recorded yet. Add one above.</div>`;
    return;
  }

  container.innerHTML = STATE.memoryFacts.map(f => `
    <div style="display:flex; align-items:center; justify-content:space-between; padding:10px 14px; background:var(--bg-surface); border:1px solid var(--border-subtle); border-radius:var(--radius-md);">
      <div>
        <span style="font-size:13px; color:var(--text-primary); font-weight:500;">${escapeHtml(f.fact || f.content || '')}</span>
        <div style="margin-top:2px;">
          <span class="nav-pill pill-blue">${escapeHtml(f.category || 'general')}</span>
        </div>
      </div>
      <button class="icon-btn delete-single-fact-btn" data-id="${escapeHtml(f.id)}" title="Delete fact" style="width:28px; height:28px;">✕</button>
    </div>
  `).join("");

  $$(".delete-single-fact-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      const id = btn.dataset.id;
      try {
        if (STATE.backendOnline) {
          await apiRequest(`/api/v1/memory/facts/${encodeURIComponent(id)}`, { method: "DELETE" });
        }
        STATE.memoryFacts = STATE.memoryFacts.filter(x => x.id !== id);
        localStorage.setItem("zaythar_memory_facts", JSON.stringify(STATE.memoryFacts));
        loadAndRenderFacts();
        showToast("Fact deleted");
      } catch (err) {
        showToast("Error deleting: " + err.message, "error");
      }
    });
  });
}

// ==========================================================================
// 8. ANDROID & APK HUB
// ==========================================================================
function renderAndroidView(container) {
  const checklist = JSON.parse(localStorage.getItem("zaythar_android_checklist") || "{}");

  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-android)" stroke-width="2"><rect width="14" height="20" x="5" y="2" rx="2" ry="2"/><line x1="12" x2="12.01" y1="18" y2="18"/></svg>
          Android APK & Physical Device Testing Hub
        </h2>
        <p>Instructions, status tracking, and device checklists for the Android Kotlin client in the fluffy-umbrella repository.</p>
      </div>

      <div class="card">
        <div class="card-title">Android Build Status & Instructions</div>
        <p style="color:var(--text-secondary); font-size:13px; margin-top:8px;">
          To generate the standalone release APK on your machine or CI:
        </p>
        <pre style="background:#0a0e14; padding:12px; border-radius:var(--radius-md); font-family:monospace; font-size:12px; color:#34d399; margin:10px 0;">cd androidApp\n./gradlew assembleRelease</pre>
        <p style="font-size:12px; color:var(--text-muted);">
          Output path: <code>app/build/outputs/apk/release/app-release.apk</code>
        </p>
      </div>

      <div class="card">
        <div class="card-title">Physical Device Testing Verification Checklist</div>
        <div style="margin-top:14px;">
          <div class="checklist-item ${checklist.c1 ? 'done' : ''}">
            <input type="checkbox" class="checklist-checkbox" id="chk1" ${checklist.c1 ? 'checked' : ''} />
            <label for="chk1" style="cursor:pointer; flex:1;">Install APK on physical Android device (Android 11+)</label>
          </div>
          <div class="checklist-item ${checklist.c2 ? 'done' : ''}">
            <input type="checkbox" class="checklist-checkbox" id="chk2" ${checklist.c2 ? 'checked' : ''} />
            <label for="chk2" style="cursor:pointer; flex:1;">Verify Microphone Audio Permissions for Neural Voice</label>
          </div>
          <div class="checklist-item ${checklist.c3 ? 'done' : ''}">
            <input type="checkbox" class="checklist-checkbox" id="chk3" ${checklist.c3 ? 'checked' : ''} />
            <label for="chk3" style="cursor:pointer; flex:1;">Test REST API connectivity to remote backend URL</label>
          </div>
          <div class="checklist-item ${checklist.c4 ? 'done' : ''}">
            <input type="checkbox" class="checklist-checkbox" id="chk4" ${checklist.c4 ? 'checked' : ''} />
            <label for="chk4" style="cursor:pointer; flex:1;">Verify Background Notifications and Offline Room Sync</label>
          </div>
        </div>
      </div>
    </div>
  `;

  ["chk1", "chk2", "chk3", "chk4"].forEach((id, idx) => {
    $(`#${id}`)?.addEventListener("change", (e) => {
      const parent = e.target.closest(".checklist-item");
      if (e.target.checked) parent.classList.add("done");
      else parent.classList.remove("done");

      checklist[`c${idx + 1}`] = e.target.checked;
      localStorage.setItem("zaythar_android_checklist", JSON.stringify(checklist));
      showToast("Updated device checklist");
    });
  });
}

// ==========================================================================
// 9. AUTONOMOUS AGENT
// ==========================================================================
function renderAgentView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-agent)" stroke-width="2"><path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/><path d="m12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z"/></svg>
          Autonomous Agent
        </h2>
        <p>Dispatch multi-step agent tasks. The backend agent decomposes the objective, utilizes tools, and returns full execution logs.</p>
      </div>

      <div class="card">
        <div class="card-title">Task Specification</div>
        <div style="margin-top:12px;">
          <label style="font-size:12px; color:var(--text-secondary);">Goal / Objective</label>
          <input class="input" id="agentTaskInput" placeholder="E.g., Generate a daily security briefing and compute API metrics" />
        </div>
        <div style="margin-top:12px;">
          <label style="font-size:12px; color:var(--text-secondary);">Tool Hint</label>
          <select class="select" id="agentToolSelect">
            <option value="">Auto (Agent Decides)</option>
            <option value="think">Think (Reasoning)</option>
            <option value="daily_briefing">Daily Briefing</option>
            <option value="deep_research">Deep Research</option>
            <option value="calculator">Calculator</option>
            <option value="code_inspect">Code Inspect</option>
            <option value="summarize">Summarize</option>
          </select>
        </div>
        <div style="margin-top:16px; display:flex; justify-content:flex-end;">
          <button class="btn btn-primary" id="runAgentTaskBtn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            <span>Execute Agent Task</span>
          </button>
        </div>
      </div>

      <div class="card" id="agentResultCard" style="display:none;">
        <div class="card-title">Agent Execution Timeline</div>
        <div id="agentOutputContent" style="margin-top:12px;"></div>
      </div>
    </div>
  `;

  $("#runAgentTaskBtn").addEventListener("click", async () => {
    const task = $("#agentTaskInput").value.trim();
    if (!task) return;

    const tool = $("#agentToolSelect").value;
    const btn = $("#runAgentTaskBtn");
    btn.disabled = true;
    btn.innerHTML = `<span>Executing Task...</span>`;

    try {
      const res = await apiRequest("/api/v1/agent/task", {
        method: "POST",
        body: JSON.stringify({ task, tool_hint: tool || undefined })
      });

      $("#agentResultCard").style.display = "block";
      $("#agentOutputContent").innerHTML = `
        <div style="padding:12px; background:var(--bg-surface); border:1px solid var(--border-strong); border-radius:var(--radius-md);">
          <div style="display:flex; justify-content:space-between; font-size:12px; color:var(--text-muted); margin-bottom:8px;">
            <span>Status: <b style="color:#10b981;">${escapeHtml(res.status)}</b></span>
            <span>Tool Used: <b>${escapeHtml(res.tool_used)}</b></span>
          </div>
          <p style="font-size:14px; line-height:1.6;">${escapeHtml(res.summary)}</p>
          ${res.actions_taken?.length ? `
            <div style="margin-top:10px; font-size:12px; color:var(--text-secondary);">
              <b>Actions Taken:</b>
              <ul style="padding-left:20px; margin-top:4px;">
                ${res.actions_taken.map(a => `<li>${escapeHtml(a)}</li>`).join("")}
              </ul>
            </div>
          ` : ''}
        </div>
      `;
      showToast("Agent completed task successfully", "success");
    } catch (err) {
      showToast("Agent error: " + err.message, "error");
    } finally {
      btn.disabled = false;
      btn.innerHTML = `<span>Execute Agent Task</span>`;
    }
  });
}

// ==========================================================================
// 10. CANVAS
// ==========================================================================
function renderCanvasView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-canvas)" stroke-width="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
          Creative Canvas & Document Studio
        </h2>
        <p>Structured markdown editor with live character counter, document export, and persistent browser storage.</p>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Editor</span>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-outline" id="downloadCanvasBtn" style="padding:4px 10px; font-size:11px;">Download .md</button>
            <button class="btn btn-outline" id="copyCanvasBtn" style="padding:4px 10px; font-size:11px;">Copy Text</button>
          </div>
        </div>
        <textarea class="textarea" id="canvasEditorArea" style="min-height:360px; font-family:monospace; font-size:13px;">${escapeHtml(STATE.canvasText)}</textarea>
        <div style="margin-top:10px; font-size:11px; color:var(--text-muted); display:flex; justify-content:space-between;">
          <span id="canvasStats">0 characters · 0 words</span>
          <span>Auto-saved locally</span>
        </div>
      </div>
    </div>
  `;

  const editor = $("#canvasEditorArea");
  const stats = $("#canvasStats");

  function updateStats() {
    const val = editor.value;
    const words = val.trim() ? val.trim().split(/\s+/).length : 0;
    stats.textContent = `${val.length} characters · ${words} words`;
    STATE.canvasText = val;
    localStorage.setItem("zaythar_canvas_doc", val);
  }

  editor.addEventListener("input", updateStats);
  updateStats();

  $("#downloadCanvasBtn").addEventListener("click", () => {
    const blob = new Blob([editor.value], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `zaythar_canvas_${Date.now()}.md`;
    a.click();
    showToast("Downloaded canvas document");
  });

  $("#copyCanvasBtn").addEventListener("click", () => {
    navigator.clipboard.writeText(editor.value);
    showToast("Copied to clipboard!");
  });
}

// ==========================================================================
// 11. PERSONAS & GPTS
// ==========================================================================
function renderPersonasView(container) {
  const personas = [
    { id: "jarvis", name: "JARVIS", role: "Executive Command AI", tag: "FastAPI Core", desc: "Precise, sophisticated assistant that addresses you with utmost respect." },
    { id: "friday", name: "FRIDAY", role: "Tactical Specialist", tag: "Rapid Ops", desc: "Fast, execution-focused AI designed for coding and rapid triage." },
    { id: "zaythar", name: "Zaythar Core", role: "Multimodal Nexus", tag: "Full-Spectrum", desc: "Balanced reasoning engine with direct access to all workspace tools." }
  ];

  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-personas)" stroke-width="2"><circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 1 0-16 0"/></svg>
          Personas & GPTs
        </h2>
        <p>Switch between neural personas to tailor response tone, vocabulary, and execution style.</p>
      </div>

      <div class="personas-grid">
        ${personas.map(p => `
          <div class="card" style="border-color:${STATE.activePersona === p.id ? 'var(--accent-personas)' : 'var(--border-subtle)'};">
            <span class="nav-pill pill-pink">${p.tag}</span>
            <h3 style="margin-top:12px; font-size:16px;">${p.name}</h3>
            <div style="font-size:12px; color:var(--text-muted);">${p.role}</div>
            <p style="font-size:13px; color:var(--text-secondary); margin-top:8px;">${p.desc}</p>
            <div style="margin-top:16px;">
              <button class="btn ${STATE.activePersona === p.id ? 'btn-success' : 'btn-outline'} select-persona-btn" data-id="${p.id}" style="width:100%;">
                ${STATE.activePersona === p.id ? '✓ Active Persona' : 'Activate Persona'}
              </button>
            </div>
          </div>
        `).join("")}
      </div>
    </div>
  `;

  $$(".select-persona-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      STATE.activePersona = btn.dataset.id;
      localStorage.setItem("zaythar_persona", STATE.activePersona);
      $("#activePersonaLabel").textContent = `Persona: ${STATE.activePersona.toUpperCase()}`;
      renderPersonasView(container);
      showToast(`Activated ${STATE.activePersona.toUpperCase()} persona`);
    });
  });
}

// ==========================================================================
// 12. PROJECTS HUB
// ==========================================================================
function renderProjectsView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-projects)" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          Projects Hub
        </h2>
        <p>Organize multiple workflows, store repositories, and group sessions into dedicated workspaces.</p>
      </div>

      <div class="card">
        <div class="card-title">Create New Project</div>
        <div style="display:flex; gap:10px; margin-top:12px;">
          <input class="input" id="newProjectNameInput" placeholder="Project Name (e.g. Fluffy-Umbrella Core)" style="flex:1;" />
          <button class="btn btn-primary" id="createProjectBtn">
            <span>Create Project</span>
          </button>
        </div>
      </div>

      <div class="card">
        <div class="card-title">Active Projects</div>
        <div id="projectsGrid" class="projects-grid"></div>
      </div>
    </div>
  `;

  renderProjectsList();

  $("#createProjectBtn").addEventListener("click", () => {
    const input = $("#newProjectNameInput");
    const name = input.value.trim();
    if (!name) return;

    STATE.projects.push({
      id: "proj_" + Date.now(),
      name,
      createdAt: new Date().toISOString()
    });
    localStorage.setItem("zaythar_projects", JSON.stringify(STATE.projects));
    input.value = "";
    renderProjectsList();
    showToast(`Created project ${name}`, "success");
  });
}

function renderProjectsList() {
  const container = $("#projectsGrid");
  if (!container) return;

  if (STATE.projects.length === 0) {
    container.innerHTML = `<div style="grid-column:span 2; text-align:center; padding:20px; color:var(--text-muted);">No projects created yet.</div>`;
    return;
  }

  container.innerHTML = STATE.projects.map(p => `
    <div style="padding:14px; background:var(--bg-surface); border:1px solid var(--border-subtle); border-radius:var(--radius-md); display:flex; justify-content:space-between; align-items:center;">
      <div>
        <b>${escapeHtml(p.name)}</b>
        <div style="font-size:11px; color:var(--text-muted);">Created: ${new Date(p.createdAt).toLocaleDateString()}</div>
      </div>
      <button class="icon-btn delete-proj-btn" data-id="${escapeHtml(p.id)}" title="Delete project" style="width:28px; height:28px;">✕</button>
    </div>
  `).join("");

  $$(".delete-proj-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      STATE.projects = STATE.projects.filter(p => p.id !== btn.dataset.id);
      localStorage.setItem("zaythar_projects", JSON.stringify(STATE.projects));
      renderProjectsList();
      showToast("Project deleted");
    });
  });
}

// ==========================================================================
// 13. SETTINGS & CLOUD SYNC
// ==========================================================================
function renderSettingsView(container) {
  container.innerHTML = `
    <div class="view-container">
      <div class="view-hero">
        <h2>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent-settings)" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          System Settings & Cloud Sync
        </h2>
        <p>Configure backend API endpoints, customize your personal honorific, and configure remote Firestore sync.</p>
      </div>

      <div class="card">
        <div class="card-title">Backend API Configuration</div>
        <div style="margin-top:12px;">
          <label style="font-size:12px; color:var(--text-secondary);">FastAPI Endpoint URL</label>
          <input class="input" id="backendUrlInput" value="${escapeHtml(STATE.backendUrl)}" placeholder="http://localhost:8000" />
        </div>
        <div style="margin-top:12px;">
          <label style="font-size:12px; color:var(--text-secondary);">Personal Honorific</label>
          <input class="input" id="honorificInput" value="${escapeHtml(STATE.honorific)}" placeholder="Sir, Boss, Captain..." />
        </div>
        <div style="margin-top:16px; display:flex; gap:10px;">
          <button class="btn btn-primary" id="saveBackendSettingsBtn">
            <span>Save Settings</span>
          </button>
          <button class="btn btn-secondary" id="pingBackendBtn">
            <span>Test Connection</span>
          </button>
        </div>
      </div>

      <div class="card">
        <div class="card-title">Cloud Sync Availability</div>
        <p style="font-size:13px; color:var(--text-secondary); margin-top:6px;">
          This browser workspace does not yet have an authenticated Firestore adapter. Cloud sync is unavailable here; use the local JSON backup below to export your browser data.
        </p>
        <div style="margin-top:14px;">
          <button class="btn btn-outline" id="exportDataJsonBtn">
            <span>Export Local JSON Backup</span>
          </button>
        </div>
      </div>
    </div>
  `;

  $("#saveBackendSettingsBtn").addEventListener("click", () => {
    STATE.backendUrl = $("#backendUrlInput").value.trim() || "http://localhost:8000";
    STATE.honorific = $("#honorificInput").value.trim() || "Sir";
    localStorage.setItem("zaythar_backend_url", STATE.backendUrl);
    localStorage.setItem("zaythar_honorific", STATE.honorific);
    checkBackendHealth();
    showToast("Settings saved successfully", "success");
  });

  $("#pingBackendBtn").addEventListener("click", async () => {
    showToast("Testing connection to " + STATE.backendUrl);
    await checkBackendHealth();
    if (STATE.backendOnline) {
      showToast("Connected to FastAPI backend!", "success");
    } else {
      showToast("Cannot reach backend at specified URL", "error");
    }
  });

  $("#exportDataJsonBtn").addEventListener("click", () => {
    const backup = {
      chats: STATE.chats,
      projects: STATE.projects,
      facts: STATE.memoryFacts,
      canvas: STATE.canvasText,
      exportTime: new Date().toISOString()
    };
    const blob = new Blob([JSON.stringify(backup, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `zaythar_backup_${Date.now()}.json`;
    a.click();
    showToast("Exported local backup JSON");
  });
}

// ==========================================================================
// APPLICATION INITIALIZATION & SHELL BINDINGS
// ==========================================================================
function saveChats() {
  localStorage.setItem("zaythar_chats", JSON.stringify(STATE.chats));
  localStorage.setItem("zaythar_active_chat", STATE.currentChatId);
  renderHistoryList();
}

function renderHistoryList() {
  const container = $("#historyList");
  if (!container) return;

  container.innerHTML = STATE.chats.map(c => `
    <button class="history-item ${c.id === STATE.currentChatId ? 'active' : ''}" data-id="${escapeHtml(c.id)}">
      <span>${escapeHtml(c.title)}</span>
    </button>
  `).join("");

  $$(".history-item").forEach(btn => {
    btn.addEventListener("click", () => {
      STATE.currentChatId = btn.dataset.id;
      saveChats();
      switchView("chat");
    });
  });
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

document.addEventListener("DOMContentLoaded", () => {
  // Mobile Sidebar Toggle
  $("#openSidebarBtn")?.addEventListener("click", () => {
    $("#sidebar")?.classList.add("open");
  });

  $("#closeSidebarBtn")?.addEventListener("click", () => {
    $("#sidebar")?.classList.remove("open");
  });

  // Nav Item Clicks
  $$(".nav-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      switchView(btn.dataset.view);
    });
  });

  // New Chat
  $("#newChatBtn")?.addEventListener("click", () => {
    const newChat = {
      id: "chat_" + Date.now(),
      title: "New Conversation",
      messages: [],
      createdAt: new Date().toISOString()
    };
    STATE.chats.unshift(newChat);
    STATE.currentChatId = newChat.id;
    saveChats();
    switchView("chat");
    showToast("Started new chat");
  });

  // Clear Chats
  $("#clearAllChatsBtn")?.addEventListener("click", () => {
    if (!confirm("Clear all recent chat sessions?")) return;
    STATE.chats = [];
    STATE.currentChatId = null;
    saveChats();
    switchView("chat");
    showToast("Cleared history");
  });

  // Engine Select
  $("#engineSelect")?.addEventListener("change", (e) => {
    STATE.activeModel = e.target.value;
    localStorage.setItem("zaythar_model", STATE.activeModel);
    showToast(`Switched engine to ${e.target.value}`);
  });

  // Backend Status Ping Button
  $("#backendStatusPill")?.addEventListener("click", async () => {
    showToast("Checking backend health...");
    await checkBackendHealth();
    if (STATE.backendOnline) showToast("Backend is online and responding", "success");
    else showToast("Backend offline or URL incorrect. Check Settings.", "warning");
  });

  // Init
  renderHistoryList();
  switchView(STATE.activeView);
  checkBackendHealth();
  setInterval(checkBackendHealth, 10000);
});
