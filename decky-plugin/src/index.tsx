import {
  ButtonItem,
  Field,
  PanelSection,
  PanelSectionRow,
  TextField,
  ToggleField,
  staticClasses,
} from "@decky/ui";
import { callable, definePlugin, toaster } from "@decky/api";
import { FC, useCallback, useEffect, useRef, useState } from "react";
import QRCode from "qrcode";
import {
  FaCheckCircle,
  FaCopy,
  FaExclamationCircle,
  FaGamepad,
  FaGoogle,
  FaSignOutAlt,
  FaSyncAlt,
  FaTimes,
  FaTrashAlt,
} from "react-icons/fa";

const C = {
  cyan: "#2ee6ff",
  lime: "#3dff9a",
  coral: "#ff5c7a",
  ink: "#061018",
  panel: "rgba(13, 31, 42, 0.92)",
  line: "#1e3a48",
  muted: "#8aa4b0",
  text: "#eaf6fb",
  warn: "#ffc857",
};

const apiGetStatus = callable<
  [],
  {
    success: boolean;
    is_authenticated: boolean;
    is_waiting?: boolean;
    email: string;
    lan_url?: string;
    auth_url?: string;
    auto_sync: boolean;
    is_monitoring: boolean;
    last_sync_timestamp: number;
    version?: string;
    error?: string;
  }
>("get_status");

const apiStartLogin = callable<
  [],
  { success: boolean; lan_url?: string; auth_url?: string; error?: string }
>("start_login");
const apiCancelLogin = callable<[], { success: boolean }>("cancel_login");
const apiSubmitCode = callable<[string], { success: boolean; email?: string; error?: string }>(
  "submit_code"
);
const apiSignOut = callable<[], { success: boolean }>("sign_out");
const apiScan = callable<
  [],
  {
    success: boolean;
    emulators: Array<{
      id: string;
      name: string;
      category: string;
      save_path: string;
      exists: boolean;
      save_count: number;
      drive_folder: string;
    }>;
    total_saves: number;
  }
>("scan");
const apiRunSync = callable<
  [string?],
  { success: boolean; uploaded: number; downloaded: number; error?: string }
>("run_sync");
const apiToggleAuto = callable<
  [boolean],
  { success: boolean; auto_sync: boolean; is_monitoring: boolean }
>("toggle_auto_sync");
const apiGetActivity = callable<
  [],
  {
    success: boolean;
    logs: Array<{
      timestamp: string;
      status: string;
      message: string;
      file_count: number;
      type: string;
    }>;
  }
>("get_activity");
const apiClearActivity = callable<[], { success: boolean }>("clear_activity");

async function makeQrSvg(data: string): Promise<string> {
  return QRCode.toString(data, {
    type: "svg",
    errorCorrectionLevel: "M",
    margin: 2,
    width: 200,
    color: { dark: "#061018", light: "#ffffff" },
  });
}

async function copyText(text: string): Promise<boolean> {
  const win = window as any;
  try {
    if (typeof win.SteamClient?.System?.SetClipboardText === "function") {
      win.SteamClient.System.SetClipboardText(text);
      return true;
    }
  } catch (_) {}
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (_) {}
  return false;
}

const Content: FC = () => {
  const [email, setEmail] = useState("");
  const [authed, setAuthed] = useState(false);
  const [waiting, setWaiting] = useState(false);
  const [lanUrl, setLanUrl] = useState("");
  const [qrSvg, setQrSvg] = useState("");
  const [loginError, setLoginError] = useState("");
  const [autoSync, setAutoSync] = useState(true);
  const [monitoring, setMonitoring] = useState(false);
  const [emulators, setEmulators] = useState<
    Array<{
      id: string;
      name: string;
      category: string;
      save_path: string;
      save_count: number;
    }>
  >([]);
  const [totalSaves, setTotalSaves] = useState(0);
  const [logs, setLogs] = useState<
    Array<{ timestamp: string; status: string; message: string; file_count: number }>
  >([]);
  const [syncing, setSyncing] = useState(false);
  const [syncTarget, setSyncTarget] = useState<string | null>(null);
  const [manualCode, setManualCode] = useState("");
  const [busyLogin, setBusyLogin] = useState(false);
  const [backendOk, setBackendOk] = useState(true);
  const [version, setVersion] = useState("?");
  const autoStarted = useRef(false);

  const setLanAndQr = useCallback(async (url: string) => {
    setLanUrl(url);
    if (!url) {
      setQrSvg("");
      return;
    }
    try {
      const svg = await makeQrSvg(url);
      setQrSvg(svg);
    } catch (e) {
      console.warn("[syncMyShit] QR SVG failed", e);
      setQrSvg("");
    }
  }, []);

  const refresh = useCallback(async () => {
    try {
      const st = await apiGetStatus();
      if (!st || (st.success === false && st.error)) {
        setBackendOk(false);
        setLoginError(st?.error || "Backend not responding");
        return;
      }
      setBackendOk(true);
      setVersion(st.version || "2.0.1");
      setAuthed(!!st.is_authenticated);
      setEmail(st.email || "");
      const wait = !!st.is_waiting && !st.is_authenticated;
      setWaiting(wait);
      if (st.lan_url) await setLanAndQr(st.lan_url);
      else if (!wait) await setLanAndQr("");
      setAutoSync(!!st.auto_sync);
      setMonitoring(!!st.is_monitoring);

      try {
        const scan = await apiScan();
        if (scan.success) {
          setEmulators(scan.emulators || []);
          setTotalSaves(scan.total_saves || 0);
        }
      } catch (_) {}
      try {
        const act = await apiGetActivity();
        if (act.success) setLogs(act.logs || []);
      } catch (_) {}
    } catch (e: any) {
      setBackendOk(false);
      setLoginError(String(e?.message || e));
      console.warn("[syncMyShit] refresh", e);
    }
  }, [setLanAndQr]);

  const startLogin = useCallback(async (silent = false) => {
    setBusyLogin(true);
    setLoginError("");
    try {
      const res = await apiStartLogin();
      if (res.success && res.lan_url) {
        setWaiting(true);
        await setLanAndQr(res.lan_url);
        if (!silent) {
          toaster.toast({
            title: "Scan this QR",
            body: "Same Wi‑Fi. Phone signs in, then paste the localhost URL.",
            duration: 6000,
          });
        }
      } else {
        const err = res.error || "Could not start login server";
        setLoginError(err);
        setWaiting(false);
        if (!silent) {
          toaster.toast({ title: "Login failed", body: err, duration: 6000 });
        }
      }
    } catch (e: any) {
      const err = String(e?.message || e);
      setLoginError(err);
      setBackendOk(false);
      if (!silent) {
        toaster.toast({ title: "Login failed", body: err, duration: 6000 });
      }
    } finally {
      setBusyLogin(false);
    }
  }, [setLanAndQr]);

  useEffect(() => {
    (async () => {
      await refresh();
      if (autoStarted.current) return;
      autoStarted.current = true;
      try {
        const st = await apiGetStatus();
        if (st && !st.is_authenticated) {
          await startLogin(true);
        }
      } catch (_) {}
    })();
  }, [refresh, startLogin]);

  useEffect(() => {
    if (!waiting) return;
    const t = setInterval(async () => {
      try {
        const st = await apiGetStatus();
        if (st.is_authenticated) {
          setWaiting(false);
          await setLanAndQr("");
          setAuthed(true);
          setEmail(st.email || "");
          toaster.toast({
            title: "Drive linked",
            body: st.email || "Google Drive connected",
            duration: 4000,
          });
          await refresh();
        } else if (st.lan_url) {
          await setLanAndQr(st.lan_url);
        }
      } catch (_) {}
    }, 2000);
    return () => clearInterval(t);
  }, [waiting, refresh, setLanAndQr]);

  const cancelLogin = async () => {
    setWaiting(false);
    await setLanAndQr("");
    try {
      await apiCancelLogin();
    } catch (_) {}
  };

  const submitManual = async () => {
    if (!manualCode.trim()) return;
    try {
      const res = await apiSubmitCode(manualCode.trim());
      if (res.success) {
        setManualCode("");
        setWaiting(false);
        await setLanAndQr("");
        toaster.toast({ title: "Drive linked", body: res.email || "Connected", duration: 4000 });
        await refresh();
      } else {
        toaster.toast({ title: "Code rejected", body: res.error || "Try again", duration: 5000 });
      }
    } catch (e: any) {
      toaster.toast({ title: "Code rejected", body: String(e?.message || e), duration: 5000 });
    }
  };

  const doSync = async (id?: string) => {
    if (!authed) {
      toaster.toast({ title: "Sign in first", body: "Link Google Drive to sync.", duration: 4000 });
      return;
    }
    setSyncing(true);
    setSyncTarget(id || "all");
    try {
      const res = await apiRunSync(id || "");
      if (res.success) {
        toaster.toast({
          title: "Sync done",
          body: `↑ ${res.uploaded}  ↓ ${res.downloaded}`,
          duration: 4500,
        });
      } else {
        toaster.toast({
          title: "Sync issue",
          body: res.error || "Something went sideways",
          duration: 5000,
        });
      }
      await refresh();
    } catch (e: any) {
      toaster.toast({ title: "Sync failed", body: String(e?.message || e), duration: 5000 });
    } finally {
      setSyncing(false);
      setSyncTarget(null);
    }
  };

  const toggleAuto = async (v: boolean) => {
    setAutoSync(v);
    try {
      const res = await apiToggleAuto(v);
      setMonitoring(!!res.is_monitoring);
    } catch (_) {}
  };

  const signOut = async () => {
    try {
      await apiSignOut();
      setAuthed(false);
      setEmail("");
      setWaiting(false);
      await setLanAndQr("");
      autoStarted.current = false;
      toaster.toast({ title: "Signed out", body: "Google Drive disconnected", duration: 3000 });
      await refresh();
      await startLogin(true);
    } catch (_) {}
  };

  const showLoginPanel = !authed;

  return (
    <div style={{ paddingBottom: 12 }}>
      <div
        style={{
          margin: "0 0 10px",
          padding: "14px 12px 12px",
          background: `linear-gradient(135deg, ${C.ink} 0%, #0a2a33 55%, #0a2418 100%)`,
          borderBottom: `2px solid ${C.cyan}`,
          fontFamily: '"Trebuchet MS", "Segoe UI", sans-serif',
        }}
      >
        <div
          style={{
            fontSize: 22,
            fontWeight: 800,
            letterSpacing: "-0.03em",
            color: C.cyan,
            lineHeight: 1.1,
          }}
        >
          syncMyShit
        </div>
        <div style={{ fontSize: 11, color: C.muted, marginTop: 4, lineHeight: 1.35 }}>
          Deck ↔ Phone ↔ Drive. Zero save drama.
        </div>
      </div>

      <PanelSection title="Google Drive">
        <PanelSectionRow>
          <div
            style={{
              width: "100%",
              padding: "12px",
              background: C.panel,
              border: `1px solid ${C.line}`,
              borderLeft: `3px solid ${authed ? C.lime : waiting ? C.warn : C.coral}`,
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
              {authed ? (
                <FaCheckCircle color={C.lime} size={14} />
              ) : waiting ? (
                <FaSyncAlt className="fa-spin" color={C.warn} size={14} />
              ) : (
                <FaExclamationCircle color={C.coral} size={14} />
              )}
              <span
                style={{
                  fontWeight: 700,
                  fontSize: 13,
                  color: authed ? C.lime : waiting ? C.warn : C.coral,
                }}
              >
                {authed ? email || "Connected" : waiting ? "Waiting for phone…" : "Not linked"}
              </span>
            </div>
            <div style={{ fontSize: 11, color: C.muted, lineHeight: 1.4 }}>
              {authed
                ? "Saves land in Drive folder syncMyShit — same as Android."
                : "1) Scan QR  2) Google on phone  3) Paste the 127.0.0.1 URL back on phone"}
            </div>
            {!backendOk && (
              <div style={{ marginTop: 8, fontSize: 11, color: C.coral }}>
                Backend error: {loginError || "unknown"}
              </div>
            )}
            {loginError && backendOk && (
              <div style={{ marginTop: 8, fontSize: 11, color: C.coral }}>{loginError}</div>
            )}
          </div>
        </PanelSectionRow>

        {showLoginPanel && (
          <>
            {(qrSvg || lanUrl) && (
              <PanelSectionRow>
                <div style={{ width: "100%", textAlign: "center", padding: "8px 0" }}>
                  {qrSvg ? (
                    <div
                      style={{
                        display: "inline-block",
                        padding: 8,
                        background: "#fff",
                        borderRadius: 4,
                        border: `2px solid ${C.cyan}`,
                      }}
                      dangerouslySetInnerHTML={{ __html: qrSvg }}
                    />
                  ) : (
                    <div style={{ color: C.warn, fontSize: 12, padding: 12 }}>
                      QR rendering failed — use the URL below on your phone browser.
                    </div>
                  )}
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 12,
                      color: C.cyan,
                      wordBreak: "break-all",
                      fontFamily: "Consolas, monospace",
                      fontWeight: 700,
                    }}
                  >
                    {lanUrl || "…"}
                  </div>
                  <div style={{ marginTop: 6, fontSize: 11, color: C.muted }}>
                    Phone must be on the same Wi‑Fi as the Deck
                  </div>
                </div>
              </PanelSectionRow>
            )}

            {!lanUrl && (
              <PanelSectionRow>
                <ButtonItem layout="below" onClick={() => startLogin(false)} disabled={busyLogin}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                    <FaGoogle size={13} />
                    <span>{busyLogin ? "Starting…" : "Show login QR"}</span>
                  </div>
                </ButtonItem>
              </PanelSectionRow>
            )}

            {lanUrl && (
              <>
                <PanelSectionRow>
                  <ButtonItem
                    layout="below"
                    onClick={async () => {
                      const ok = await copyText(lanUrl);
                      toaster.toast({
                        title: ok ? "Copied" : "Copy failed",
                        body: ok ? "Open this URL on your phone" : lanUrl,
                        duration: 4000,
                      });
                    }}
                  >
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                      <FaCopy size={12} />
                      <span>Copy login link</span>
                    </div>
                  </ButtonItem>
                </PanelSectionRow>
                <PanelSectionRow>
                  <ButtonItem layout="below" onClick={() => startLogin(false)} disabled={busyLogin}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                      <FaSyncAlt size={12} />
                      <span>Refresh QR</span>
                    </div>
                  </ButtonItem>
                </PanelSectionRow>
                <PanelSectionRow>
                  <ButtonItem layout="below" onClick={cancelLogin}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                      <FaTimes size={12} />
                      <span>Cancel</span>
                    </div>
                  </ButtonItem>
                </PanelSectionRow>
              </>
            )}

            <PanelSectionRow>
              <TextField
                label="Or paste code / 127.0.0.1 URL here"
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
              />
            </PanelSectionRow>
            <PanelSectionRow>
              <ButtonItem layout="below" onClick={submitManual} disabled={!manualCode.trim()}>
                Connect with pasted code
              </ButtonItem>
            </PanelSectionRow>
          </>
        )}

        {authed && (
          <PanelSectionRow>
            <ButtonItem layout="below" onClick={signOut}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                <FaSignOutAlt size={12} />
                <span>Sign out</span>
              </div>
            </ButtonItem>
          </PanelSectionRow>
        )}
      </PanelSection>

      <PanelSection title="Sync">
        <PanelSectionRow>
          <ButtonItem layout="below" onClick={() => doSync()} disabled={syncing || !authed}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
              <FaSyncAlt className={syncing && syncTarget === "all" ? "fa-spin" : ""} />
              <span>
                {syncing && syncTarget === "all"
                  ? "Syncing…"
                  : !authed
                    ? "Sign in to sync"
                    : "⚡ Sync All Saves"}
              </span>
            </div>
          </ButtonItem>
        </PanelSectionRow>
        <PanelSectionRow>
          <ToggleField
            label="Auto-sync on emulator exit"
            description={
              monitoring
                ? "Watching emulator processes"
                : autoSync
                  ? "Starts after you link Drive"
                  : "Off"
            }
            checked={autoSync}
            onChange={toggleAuto}
          />
        </PanelSectionRow>
        <PanelSectionRow>
          <Field label="Detected saves" description={`${emulators.length} emulators`}>
            <span style={{ color: C.cyan, fontWeight: 700 }}>{totalSaves}</span>
          </Field>
        </PanelSectionRow>
      </PanelSection>

      {emulators.length > 0 && (
        <PanelSection title="Emulators">
          {emulators.map((emu) => (
            <PanelSectionRow key={emu.id}>
              <div
                style={{
                  width: "100%",
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  padding: "6px 0",
                  borderBottom: `1px solid ${C.line}`,
                }}
              >
                <FaGamepad color={C.muted} size={12} style={{ flexShrink: 0 }} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12, fontWeight: 700, color: C.text }}>{emu.name}</div>
                  <div style={{ fontSize: 10, color: C.muted }}>
                    {emu.save_count} file{emu.save_count === 1 ? "" : "s"} · {emu.category}
                  </div>
                </div>
                <ButtonItem onClick={() => doSync(emu.id)} disabled={syncing || !authed}>
                  {syncing && syncTarget === emu.id ? "…" : "Sync"}
                </ButtonItem>
              </div>
            </PanelSectionRow>
          ))}
        </PanelSection>
      )}

      <PanelSection title="Activity">
        {logs.length === 0 ? (
          <PanelSectionRow>
            <div style={{ fontSize: 11, color: C.muted, padding: "4px 0" }}>
              Nothing yet — sync something and it shows up here.
            </div>
          </PanelSectionRow>
        ) : (
          logs.slice(0, 10).map((log, i) => (
            <PanelSectionRow key={`${log.timestamp}-${i}`}>
              <div style={{ width: "100%", fontSize: 11, padding: "3px 0" }}>
                <span style={{ color: C.muted }}>{log.timestamp}</span>{" "}
                <span style={{ color: log.status === "error" ? C.coral : C.lime }}>
                  {log.message}
                </span>
              </div>
            </PanelSectionRow>
          ))
        )}
        {logs.length > 0 && (
          <PanelSectionRow>
            <ButtonItem
              layout="below"
              onClick={async () => {
                await apiClearActivity();
                setLogs([]);
              }}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                <FaTrashAlt size={11} />
                <span>Clear activity</span>
              </div>
            </ButtonItem>
          </PanelSectionRow>
        )}
      </PanelSection>

      <PanelSection title="About">
        <PanelSectionRow>
          <Field label="Version" description="Decky plugin">
            <span style={{ color: C.cyan, fontWeight: 700 }}>v{version}</span>
          </Field>
        </PanelSectionRow>
      </PanelSection>
    </div>
  );
};

export default definePlugin(() => {
  return {
    title: <div className={staticClasses.Title}>syncMyShit</div>,
    content: <Content />,
    icon: <FaGamepad />,
  };
});
