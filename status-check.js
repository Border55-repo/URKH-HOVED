(() => {
  const button = document.getElementById("statusCheck");
  const output = document.getElementById("statusResult");
  if (!button || !output) return;
  const storageWorks = () => {
    const key = "__urkh_status__";
    try { localStorage.setItem(key, "ok"); const ok = localStorage.getItem(key) === "ok"; localStorage.removeItem(key); return ok; } catch { return false; }
  };
  button.addEventListener("click", async () => {
    button.disabled = true;
    output.dataset.state = "";
    output.textContent = "Kontrollerer …";
    const checks = [
      { label: "Nettverk", ok: navigator.onLine, detail: navigator.onLine ? "tilkoblet" : "frakoblet – appen kan fortsatt brukes lokalt" },
      { label: "Lokal lagring", ok: storageWorks(), detail: "prosjektlagring på enheten" },
      { label: "Opptaksstøtte", ok: Boolean(navigator.mediaDevices?.getUserMedia), detail: "tilgjengelig i nettleseren" }
    ];
    try {
      const response = await fetch("VERSION", { cache: "no-store" });
      const version = response.ok ? (await response.text()).trim() : "";
      checks.push({ label: "Appversjon", ok: version === "2.5.0", detail: version ? `versjon ${version}` : "kunne ikke leses" });
    } catch { checks.push({ label: "Appversjon", ok: false, detail: "kunne ikke kontrolleres uten nett" }); }
    if ("serviceWorker" in navigator) {
      const registration = await navigator.serviceWorker.getRegistration().catch(() => null);
      checks.push({ label: "Offline-støtte", ok: Boolean(registration), detail: registration ? "aktiv" : "aktiveres etter første innlasting" });
    } else checks.push({ label: "Offline-støtte", ok: false, detail: "ikke støttet av nettleseren" });
    const failures = checks.filter(item => !item.ok);
    output.dataset.state = failures.length === 0 ? "ok" : failures.length < checks.length ? "warning" : "error";
    const title = document.createElement("strong");
    title.textContent = failures.length === 0 ? "Alt ser bra ut." : `${checks.length - failures.length} av ${checks.length} kontroller bestått.`;
    const list = document.createElement("ul");
    checks.forEach(item => { const li = document.createElement("li"); li.textContent = `${item.ok ? "✓" : "!"} ${item.label}: ${item.detail}`; list.appendChild(li); });
    output.replaceChildren(title, list);
    button.disabled = false;
  });
})();
