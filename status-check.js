(() => {
  const button = document.getElementById("statusBtn");
  if (!button) return;

  async function runStatusCheck() {
    button.disabled = true;
    const originalLabel = button.textContent;
    button.textContent = "Kontrollerer …";
    const checks = [];

    checks.push({ label: "Nettverk", ok: navigator.onLine });

    try {
      const key = "urkh-status-check";
      localStorage.setItem(key, "ok");
      const ok = localStorage.getItem(key) === "ok";
      localStorage.removeItem(key);
      checks.push({ label: "Lokal lagring", ok });
    } catch {
      checks.push({ label: "Lokal lagring", ok: false });
    }

    try {
      const response = await fetch("manifest.webmanifest", { cache: "no-store" });
      checks.push({ label: "Appressurser", ok: response.ok });
    } catch {
      checks.push({ label: "Appressurser", ok: false });
    }

    checks.push({ label: "Installerbar app", ok: "serviceWorker" in navigator });

    const failed = checks.filter((check) => !check.ok);
    const summary = failed.length
      ? `Status: ${checks.length - failed.length} av ${checks.length} kontroller er godkjent. Sjekk: ${failed.map((check) => check.label).join(", ")}.`
      : `Status: Alle ${checks.length} kontroller er godkjent.`;

    if (typeof toast === "function") toast(summary);
    else window.alert(summary);

    button.textContent = originalLabel;
    button.disabled = false;
  }

  button.addEventListener("click", runStatusCheck);
  window.runUrkhStatusCheck = runStatusCheck;
})();
