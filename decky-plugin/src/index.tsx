import {
  ButtonItem,
  Field,
  PanelSection,
  PanelSectionRow,
  ToggleField,
  staticClasses,
} from "@decky/ui";
import { callable, definePlugin, toaster } from "@decky/api";
import { FC, useCallback, useEffect, useState } from "react";
import {
  FaCheckCircle,
  FaExclamationCircle,
  FaGamepad,
  FaSignOutAlt,
  FaSyncAlt,
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
    email: string;
    auto_sync: boolean;
    is_monitoring: boolean;
    last_sync_timestamp: number;
    version?: string;
    login_hint?: string;
    error?: string;
  }
>("get_status");
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
    logs: Array<{ timestamp: string; status: string; message: string; file_count: number }>;
  }
>("get_activity");
const apiClearActivity = callable<[], { success: boolean }>("clear_activity");

const LOGIN_CMD = "~/homebrew/plugins/syncMyShit/login-desktop.sh";

const Content: FC = () => {
  const [email, setEmail] = useState("");
  const [authed, setAuthed] = useState(false);
  const [autoSync, setAutoSync] = useState(true);
  const [monitoring, setMonitoring] = useState(false);
  const [emulators, setEmulators] = useState<
    Array<{ id: string; name: string; category: string; save_count: number }>
  >([]);
  const [totalSaves, setTotalSaves] = useState(0);
  const [logs, setLogs] = useState<
    Array<{ timestamp: string; status: string; message: string }>
  >([]);
  const [syncing, setSyncing] = useState(false);
  const [syncTarget, setSyncTarget] = useState<string | null>(null);
  const [version, setVersion] = useState("2.1.1");
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    try {
      const st = await apiGetStatus();
      if (!st || st.success === false) {
        setError(st?.error || "Backend error");
        setAuthed(false);
        return;
      }
      setError("");
      setAuthed(!!st.is_authenticated);
      setEmail(st.email || "");
      setAutoSync(!!st.auto_sync);
      setMonitoring(!!st.is_monitoring);
      setVersion(st.version || "2.1.1");

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
      setError(String(e?.message || e));
    }
  }, []);

  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 5000);
    return () => clearInterval(t);
  }, [refresh]);

  const doSync = async (id?: string) => {
    if (!authed) {
      toaster.toast({
        title: "Not signed in",
        body: "Login in Desktop Mode first (see instructions below).",
        duration: 5000,
      });
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
        toaster.toast({ title: "Sync issue", body: res.error || "Failed", duration: 5000 });
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
      toaster.toast({ title: "Signed out", body: "Tokens cleared", duration: 3000 });
      await refresh();
    } catch (_) {}
  };

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
        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: "-0.03em", color: C.cyan }}>
          syncMyShit
        </div>
        <div style={{ fontSize: 11, color: C.muted, marginTop: 4 }}>
          Sync in Game Mode. Sign in from Desktop Mode.
        </div>
      </div>

      <PanelSection title="Google Drive">
        <PanelSectionRow>
          <div
            style={{
              width: "100%",
              padding: 12,
              background: C.panel,
              border: `1px solid ${C.line}`,
              borderLeft: `3px solid ${authed ? C.lime : C.warn}`,
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
              {authed ? (
                <FaCheckCircle color={C.lime} size={14} />
              ) : (
                <FaExclamationCircle color={C.warn} size={14} />
              )}
              <span style={{ fontWeight: 700, fontSize: 13, color: authed ? C.lime : C.warn }}>
                {authed ? email || "Connected" : "Not signed in"}
              </span>
            </div>
            {error && (
              <div style={{ fontSize: 11, color: C.coral, marginTop: 6 }}>{error}</div>
            )}
          </div>
        </PanelSectionRow>

        {!authed && (
          <PanelSectionRow>
            <div
              style={{
                width: "100%",
                padding: 12,
                background: C.panel,
                border: `1px solid ${C.line}`,
                fontSize: 12,
                color: C.text,
                lineHeight: 1.5,
              }}
            >
              <div style={{ fontWeight: 700, color: C.cyan, marginBottom: 8 }}>
                Sign in once (Desktop Mode)
              </div>
              <div style={{ color: C.muted, marginBottom: 8 }}>
                1. Switch to <span style={{ color: C.text }}>Desktop Mode</span>
                <br />
                2. Open <span style={{ color: C.text }}>Konsole</span>
                <br />
                3. Run:
              </div>
              <div
                style={{
                  fontFamily: "Consolas, monospace",
                  fontSize: 11,
                  color: C.lime,
                  background: "#021018",
                  padding: "10px 8px",
                  wordBreak: "break-all",
                  marginBottom: 8,
                }}
              >
                {LOGIN_CMD}
              </div>
              <div style={{ color: C.muted }}>
                First run asks for a Google <span style={{ color: C.text }}>Desktop</span> OAuth
                Client ID (not the Android one — that causes invalid_request).
                <br />
                Then finish Google in the browser and return here.
              </div>
            </div>
          </PanelSectionRow>
        )}

        <PanelSectionRow>
          <ButtonItem layout="below" onClick={refresh}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
              <FaSyncAlt size={12} />
              <span>Refresh status</span>
            </div>
          </ButtonItem>
        </PanelSectionRow>

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
                    ? "Sign in (Desktop Mode) to sync"
                    : "⚡ Sync All Saves"}
              </span>
            </div>
          </ButtonItem>
        </PanelSectionRow>
        <PanelSectionRow>
          <ToggleField
            label="Auto-sync on emulator exit"
            description={monitoring ? "Watching emulator processes" : autoSync ? "On when linked" : "Off"}
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
            <div style={{ fontSize: 11, color: C.muted }}>No activity yet.</div>
          </PanelSectionRow>
        ) : (
          logs.slice(0, 10).map((log, i) => (
            <PanelSectionRow key={`${log.timestamp}-${i}`}>
              <div style={{ width: "100%", fontSize: 11 }}>
                <span style={{ color: C.muted }}>{log.timestamp}</span>{" "}
                <span style={{ color: log.status === "error" ? C.coral : C.lime }}>{log.message}</span>
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

export default definePlugin(() => ({
  title: <div className={staticClasses.Title}>syncMyShit</div>,
  content: <Content />,
  icon: <FaGamepad />,
}));
