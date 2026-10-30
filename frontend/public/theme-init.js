// Executed before React/CSS to avoid flashing the opposite theme. Preferences only.
try {
  const theme = localStorage.getItem("ledgerflow-theme") || "system";
  document.documentElement.dataset.theme =
    theme === "system"
      ? matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light"
      : theme === "dark"
        ? "dark"
        : "light";
} catch {
  document.documentElement.dataset.theme = "light";
}
