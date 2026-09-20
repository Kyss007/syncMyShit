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
import { FC, useCallback, useEffect, useState } from "react";
import {
  FaCheckCircle,
  FaExclamationCircle,
  FaGamepad,
  FaGoogle,
  FaSignOutAlt,
  FaSyncAlt,
  FaTimes,
  FaTrashAlt,
  FaKey,
} from "react-icons/fa";

/* ── theme tokens (arcade handheld) ─────────────────────────── */
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

function qrImageUrl(data: string): string {
  return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=8&data=${encodeURIComponent(data)}`;
}

const Content: FC = () => {
  const [email, setEmail] = useState("");
  const [authed, setAuthed] = useState(false);
  const [waiting, setWaiting] = useState(false);
  const [lanUrl, setLanUrl] = useState("");
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
  const [showPaste, setShowPaste] = useState(false);
  const [busyLogin, setBusyLogin] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const st = await apiGetStatus();
      if (!st?.success && st?.error) return;
      setAuthed(!!st.is_authenticated);
      setEmail(st.email || "");
      setWaiting(!!st.is_waiting && !st.is_authenticated);
      setLanUrl(st.lan_url || "");
      setAutoSync(!!st.auto_sync);
      setMonitoring(!!st.is_monitoring);

      const scan = await apiScan();
      if (scan.success) {
        setEmulators(scan.emulators || []);
        setTotalSaves(scan.total_saves || 0);
      }
      const act = await apiGetActivity();
      if (act.success) setLogs(act.logs || []);
    } catch (e) {
      console.warn("[syncMyShit] refresh", e);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Poll while waiting for phone OAuth
  useEffect(() => {
    if (!waiting) return;
    const t = setInterval(async () => {
      try {
        const st = await apiGetStatus();
        if (st.is_authenticated) {
          setWaiting(false);
          setLanUrl("");
          setAuthed(true);
          setEmail(st.email || "");
          toaster.toast({
            title: "Drive linked",
            body: st.email || "Google Drive connected",
            duration: 4000,
          });
          await refresh();
        } else if (st.lan_url) {
          setLanUrl(st.lan_url);
        }
      } catch (_) {}
    }, 2000);
    return () => clearInterval(t);
  }, [waiting, refresh]);

  const startLogin = async () => {
    setBusyLogin(true);
    try {
      const res = await apiStartLogin();
      if (res.success && res.lan_url) {
        setWaiting(true);
        setLanUrl(res.lan_url);
        toaster.toast({
          title: "Scan with your phone",
          body: "Same Wi‑Fi. Open Google, then paste the localhost URL back.",
          duration: 6000,
        });
      } else {
        toaster.toast({
          title: "Login failed",
          body: res.error || "Could not start login",
          duration: 5000,
        });
      }
    } catch (e: any) {
      toaster.toast({ title: "Login failed", body: String(e?.message || e), duration: 5000 });
    } finally {
      setBusyLogin(false);
    }
  };

  const cancelLogin = async () => {
    setWaiting(false);
    setLanUrl("");
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
        setShowPaste(false);
        setWaiting(false);
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
      toaster.toast({ title: "Signed out", body: "Google Drive disconnected", duration: 3000 });
      await refresh();
    } catch (_) {}
  };

  return (
    <div style={{ paddingBottom: 12 }}>
      {/* Brand strip */}
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
                : waiting
                  ? "Scan the QR. Sign in on your phone. Paste the localhost URL back."
                  : "Scan the QR. Bribe Google. Sync your shit."}
            </div>
          </div>
        </PanelSectionRow>

        {!authed && !waiting && (
          <PanelSectionRow>
            <ButtonItem layout="below" onClick={startLogin} disabled={busyLogin}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                <FaGoogle size={13} />
                <span>Link Google Drive</span>
              </div>
            </ButtonItem>
          </PanelSectionRow>
        )}

        {waiting && lanUrl && (
          <>
            <PanelSectionRow>
              <div style={{ width: "100%", textAlign: "center", padding: "8px 0" }}>
                <img
                  src={qrImageUrl(lanUrl)}
                  alt="Login QR"
                  width={200}
                  height={200}
                  style={{
                    borderRadius: 4,
                    border: `2px solid ${C.cyan}`,
                    background: "#fff",
                  }}
                />
                <div
                  style={{
                    marginTop: 8,
                    fontSize: 11,
                    color: C.cyan,
                    wordBreak: "break-all",
                    fontFamily: "Consolas, monospace",
                  }}
                >
                  {lanUrl}
                </div>
                <div style={{ marginTop: 6, fontSize: 11, color: C.muted }}>
                  Phone must be on the same Wi‑Fi as the Deck
                </div>
              </div>
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

        {!authed && (
          <>
            <PanelSectionRow>
              <ButtonItem
                layout="below"
                onClick={async () => {
                  const next = !showPaste;
                  setShowPaste(next);
                  if (next && !waiting) {
                    await startLogin();
                  }
                }}
              >
                <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                  <FaKey size={11} />
                  <span>{showPaste ? "Hide paste field" : "Paste code on Deck instead"}</span>
                </div>
              </ButtonItem>
            </PanelSectionRow>
            {showPaste && (
              <>
                <PanelSectionRow>
                  <TextField
                    label="Auth URL / code"
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
                <ButtonItem
                  onClick={() => doSync(emu.id)}
                  disabled={syncing || !authed}
                >
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
            <span style={{ color: C.cyan, fontWeight: 700 }}>v2.0.0</span>
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
