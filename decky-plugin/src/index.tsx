import {
  ButtonItem,
  Field,
  Navigation,
  PanelSection,
  PanelSectionRow,
  staticClasses,
  TextField,
  ToggleField,
} from "@decky/ui";
import { callable, definePlugin, toaster } from "@decky/api";
import { useEffect, useState, FC } from "react";

import {
  FaCheckCircle,
  FaExclamationCircle,
  FaGamepad,
  FaGoogle,
  FaSyncAlt,
  FaTrashAlt,
  FaSignOutAlt,
  FaKey,
  FaTimes,
  FaExternalLinkAlt,
  FaCopy,
} from "react-icons/fa";



// RPC method typings
interface StatusResponse {
  success: boolean;
  is_authenticated: boolean;
  is_authenticating?: boolean;
  auth_url?: string;
  mobile_url?: string;
  email: string;
  last_sync_timestamp: number;
  auto_sync: boolean;
  is_monitoring: boolean;
}

interface EmulatorItem {
  id: string;
  name: string;
  category: string;
  save_path: string;
  exists: boolean;
  save_count: number;
  last_sync?: number;
}

interface ScanResponse {
  success: boolean;
  emulators: EmulatorItem[];
  total_saves: number;
}

interface SyncResponse {
  success: boolean;
  uploaded: number;
  downloaded: number;
  conflicts_resolved: number;
  error?: string;
}

interface LogEntry {
  timestamp: string;
  status: "success" | "error" | "conflict";
  message: string;
  file_count: number;
  type: "upload" | "download";
}

interface LogsResponse {
  success: boolean;
  logs: LogEntry[];
}

// Callable bindings to Python backend (Plugin class)
const apiGetStatus = callable<[], StatusResponse>("get_status");
const apiScanSaves = callable<[], ScanResponse>("scan_saves");
const apiRunSync = callable<[emulatorId?: string], SyncResponse>("run_sync");
const apiToggleWatcher = callable<[enabled: boolean], { success: boolean; auto_sync: boolean }>("toggle_watcher");
const apiGetRecentLogs = callable<[], LogsResponse>("get_recent_logs");
const apiClearLogs = callable<[], { success: boolean }>("clear_logs");
const apiStartGoogleLogin = callable<[], { success: boolean; auth_url?: string; mobile_url?: string; error?: string }>("start_google_login");
const apiCancelGoogleLogin = callable<[], { success: boolean }>("cancel_google_login");
const apiOpenBrowser = callable<[url: string], { success: boolean; error?: string }>("open_browser");
const apiSubmitAuthCode = callable<[code: string], { success: boolean; email?: string; error?: string }>("submit_auth_code");
const apiSignOutGoogle = callable<[], { success: boolean }>("sign_out_google");

const formatTimestamp = (ts: number): string => {
  if (!ts || ts <= 0) return "Never";
  const diffSec = Math.floor(Date.now() / 1000 - ts);
  if (diffSec < 60) return "Just now";
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return new Date(ts * 1000).toLocaleDateString();
};


// Clipboard helper for Steam Deck CEF
const copyToClipboard = async (text: string) => {
  if (!text) return;
  const win = window as any;
  let copied = false;
  try {
    if (typeof win.SteamClient?.System?.SetClipboardText === "function") {
      win.SteamClient.System.SetClipboardText(text);
      copied = true;
    }
  } catch (e) {}

  if (!copied) {
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
        copied = true;
      }
    } catch (e) {}
  }

  if (copied) {
    toaster.toast({
      title: "Copied Link!",
      body: "Google Sign-In URL copied to clipboard.",
      duration: 3000,
    });
  } else {
    toaster.toast({
      title: "Clipboard",
      body: "Could not copy automatically. URL shown on screen.",
      duration: 3000,
    });
  }
};

const Content: FC = () => {
  const [status, setStatus] = useState<StatusResponse | null>(null);
  const [emulators, setEmulators] = useState<EmulatorItem[]>([]);
  const [totalSaves, setTotalSaves] = useState<number>(0);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [syncing, setSyncing] = useState<boolean>(false);
  const [syncTargetId, setSyncTargetId] = useState<string | null>(null);
  const [loggingIn, setLoggingIn] = useState<boolean>(false);
  const [authUrl, setAuthUrl] = useState<string>("");
  const [manualCode, setManualCode] = useState<string>("");
  const [showManualCode, setShowManualCode] = useState<boolean>(false);

  // Load status and emulator scans
  const refreshData = async () => {
    try {
      const [st, sc, lg] = await Promise.all([
        apiGetStatus(),
        apiScanSaves(),
        apiGetRecentLogs(),
      ]);
      if (st.success) {
        setStatus(st);
        if (st.is_authenticated) {
          setAuthUrl("");
          setLoggingIn(false);
        } else if (st.is_authenticating && st.auth_url) {
          // Restore sign-in state if QAM was closed during login
          setLoggingIn(true);
          setAuthUrl(st.auth_url);
        }
      }
      if (sc.success) {
        setEmulators(sc.emulators);
        setTotalSaves(sc.total_saves);
      }
      if (lg.success) {
        setLogs(lg.logs);
      }
    } catch (e: any) {
      console.error("[syncMyShit] Failed to refresh data:", e);
    }
  };

  useEffect(() => {
    refreshData();
  }, []);

  // Poll every 2 s while waiting for the browser OAuth callback to land
  useEffect(() => {
    if (!loggingIn) return;
    const interval = setInterval(async () => {
      try {
        const st = await apiGetStatus();
        if (st.success && st.is_authenticated) {
          setStatus(st);
          setLoggingIn(false);
          setAuthUrl("");
          toaster.toast({
            title: "Google Drive Connected!",
            body: `Signed in as ${st.email}`,
            duration: 4500,
          });
          await refreshData();
        }
      } catch (e) {
        // ignore transient poll errors
      }
    }, 2000);
    return () => clearInterval(interval);
  }, [loggingIn]);

  // Launch the Steam browser for Google sign-in
  const openSteamBrowser = async (url: string) => {
    if (!url) return;
    const win = window as any;

    // 1. Tell backend to invoke steam steam://openurl/<url>
    apiOpenBrowser(url).catch((e) => console.warn("[syncMyShit] apiOpenBrowser failed:", e));

    // 2. Navigation.NavigateToSteamWeb (Steam client's internal web browser)
    try {
      if (typeof Navigation?.NavigateToSteamWeb === "function") {
        Navigation.NavigateToSteamWeb(url);
      }
    } catch (e) {
      console.warn("[syncMyShit] NavigateToSteamWeb failed:", e);
    }

    // 3. Navigation.NavigateToExternalWeb (Steam GamepadUI external browser modal)
    try {
      if (typeof Navigation?.NavigateToExternalWeb === "function") {
        Navigation.NavigateToExternalWeb(url);
      }
    } catch (e) {
      console.warn("[syncMyShit] NavigateToExternalWeb failed:", e);
    }

    // 4. SteamClient methods
    try {
      if (typeof win.SteamClient?.System?.OpenURLInSystemBrowser === "function") {
        win.SteamClient.System.OpenURLInSystemBrowser(url);
      } else if (typeof win.SteamClient?.System?.OpenBrowser === "function") {
        win.SteamClient.System.OpenBrowser(url);
      } else if (typeof win.SteamClient?.Shell?.OpenURL === "function") {
        win.SteamClient.Shell.OpenURL(url);
      }
    } catch (e) {}

    // 5. Fallback window.open
    try {
      window.open(url, "_blank");
    } catch (e) {}
  };

  // Open the Google sign-in page in the Steam browser
  const handleStartGoogleLogin = async () => {
    setLoggingIn(true);
    try {
      const res = await apiStartGoogleLogin();
      if (res.success && res.auth_url) {
        setAuthUrl(res.auth_url);
        await openSteamBrowser(res.auth_url);
        toaster.toast({
          title: "Opening Steam Browser",
          body: "Sign in with Google to connect your account.",
          duration: 5000,
        });
      } else {
        setLoggingIn(false);
        toaster.toast({
          title: "Sign-In Error",
          body: res.error || "Failed to start Google authentication",
          duration: 5000,
        });
      }
    } catch (err: any) {
      setLoggingIn(false);
      toaster.toast({
        title: "Sign-In Error",
        body: String(err?.message || err),
        duration: 5000,
      });
    }
  };

  // Cancel in-progress sign in
  const handleCancelLogin = async () => {
    setLoggingIn(false);
    setAuthUrl("");
    try {
      await apiCancelGoogleLogin();
    } catch (e) {}
  };

  // Submit manual authorization code / URL (fallback)
  const handleSubmitManualCode = async () => {
    if (!manualCode.trim()) return;
    try {
      const res = await apiSubmitAuthCode(manualCode.trim());
      if (res.success) {
        setManualCode("");
        setShowManualCode(false);
        setLoggingIn(false);
        setAuthUrl("");
        toaster.toast({
          title: "Google Drive Connected!",
          body: `Signed in as ${res.email || "Google Drive User"}`,
          duration: 4500,
        });
        await refreshData();
      } else {
        toaster.toast({
          title: "Authorization Error",
          body: res.error || "Invalid code or failed to exchange token",
          duration: 5000,
        });
      }
    } catch (err: any) {
      toaster.toast({
        title: "Authorization Error",
        body: String(err?.message || err),
        duration: 5000,
      });
    }
  };

  // Sign out of Google Drive
  const handleSignOut = async () => {
    try {
      await apiSignOutGoogle();
      toaster.toast({
        title: "Google Drive Disconnected",
        body: "You have signed out of Google Drive.",
        duration: 3500,
      });
      await refreshData();
    } catch (err: any) {
      toaster.toast({
        title: "Sign-Out Error",
        body: String(err?.message || err),
        duration: 4000,
      });
    }
  };

  // Run full sync
  const handleFullSync = async () => {
    if (!status?.is_authenticated) {
      toaster.toast({
        title: "Google Drive Required",
        body: "Please connect Google Drive first.",
        duration: 4000,
      });
      return;
    }
    setSyncing(true);
    setSyncTargetId("all");
    try {
      const res = await apiRunSync();
      toaster.toast({
        title: "Google Drive Sync",
        body: res.message || "Save synchronization complete!",
        duration: 4000,
      });
      await refreshData();
    } catch (err: any) {
      toaster.toast({
        title: "Sync Error",
        body: String(err?.message || err),
        duration: 5000,
      });
    } finally {
      setSyncing(false);
      setSyncTargetId(null);
    }
  };

  // Run sync for a single emulator
  const handleSingleSync = async (emu: EmulatorItem) => {
    if (!status?.is_authenticated) {
      toaster.toast({
        title: "Google Drive Required",
        body: "Please connect Google Drive first.",
        duration: 4000,
      });
      return;
    }
    setSyncing(true);
    setSyncTargetId(emu.id);
    try {
      const res = await apiRunSync(emu.id);
      toaster.toast({
        title: `Drive Sync: ${emu.name}`,
        body: res.message || `Synchronized ${emu.name} with Google Drive`,
        duration: 3500,
      });
      await refreshData();
    } catch (err: any) {
      toaster.toast({
        title: `Error: ${emu.name}`,
        body: String(err?.message || err),
        duration: 5000,
      });
    } finally {
      setSyncing(false);
      setSyncTargetId(null);
    }
  };

  // Toggle auto-sync background watcher
  const handleToggleWatcher = async (enabled: boolean) => {
    try {
      const res = await apiToggleWatcher(enabled);
      if (res.success && status) {
        setStatus({ ...status, auto_sync: res.auto_sync });
        toaster.toast({
          title: "Auto-Sync Watcher",
          body: enabled
            ? "Auto-sync enabled (syncs to Drive on game exit)"
            : "Auto-sync paused",
          duration: 3000,
        });
      }
    } catch (e: any) {
      toaster.toast({
        title: "Watcher Error",
        body: String(e),
        duration: 4000,
      });
    }
  };

  // Clear logs
  const handleClearLogs = async () => {
    try {
      await apiClearLogs();
      setLogs([]);
      toaster.toast({
        title: "Activity Log",
        body: "Cleared recent activity",
        duration: 2500,
      });
    } catch (e: any) {
      console.error(e);
    }
  };

  return (
    <div
      style={{
        width: "100%",
        maxWidth: "100%",
        boxSizing: "border-box",
        overflowX: "hidden",
        padding: "0 2px",
      }}
    >
      {/* Google Drive Account Section */}
      <PanelSection title="Google Drive Cloud">
        {status?.is_authenticated ? (
          <>
            <PanelSectionRow>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  width: "100%",
                  maxWidth: "100%",
                  boxSizing: "border-box",
                  background: "rgba(34, 197, 94, 0.1)",
                  border: "1px solid rgba(34, 197, 94, 0.3)",
                  borderRadius: "6px",
                  padding: "8px 10px",
                  gap: "4px",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <FaCheckCircle style={{ color: "#22c55e" }} size={13} />
                  <span style={{ fontWeight: 700, fontSize: "13px", color: "#22c55e" }}>
                    Connected to Google Drive
                  </span>
                </div>
                <div
                  style={{
                    fontSize: "12px",
                    color: "#f8fafc",
                    fontWeight: 600,
                    overflowWrap: "anywhere",
                    wordBreak: "break-all",
                    marginTop: "2px",
                  }}
                >
                  {status.email || "Google Account"}
                </div>
                <div style={{ fontSize: "11px", color: "#94a3b8" }}>
                  Drive Folder: <span style={{ color: "#38bdf8", fontWeight: 600 }}>syncMyShit/</span>
                </div>
              </div>
            </PanelSectionRow>

            <PanelSectionRow>
              <ButtonItem
                layout="below"
                onClick={handleSignOut}
              >
                <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                  <FaSignOutAlt size={12} />
                  <span>Disconnect Google Account</span>
                </div>
              </ButtonItem>
            </PanelSectionRow>
          </>
        ) : (
          <>
            <PanelSectionRow>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  width: "100%",
                  maxWidth: "100%",
                  boxSizing: "border-box",
                  background: loggingIn ? "rgba(56, 189, 248, 0.1)" : "rgba(239, 68, 68, 0.1)",
                  border: `1px solid ${loggingIn ? "rgba(56, 189, 248, 0.3)" : "rgba(239, 68, 68, 0.3)"}`,
                  borderRadius: "6px",
                  padding: "8px 10px",
                  gap: "4px",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  {loggingIn ? (
                    <>
                      <FaSyncAlt className="fa-spin" style={{ color: "#38bdf8" }} size={13} />
                      <span style={{ fontWeight: 700, fontSize: "13px", color: "#38bdf8" }}>
                        Waiting for Google Sign-In...
                      </span>
                    </>
                  ) : (
                    <>
                      <FaExclamationCircle style={{ color: "#ef4444" }} size={13} />
                      <span style={{ fontWeight: 700, fontSize: "13px", color: "#ef4444" }}>
                        Not Connected
                      </span>
                    </>
                  )}
                </div>
                <div style={{ fontSize: "11px", color: "#94a3b8", lineHeight: 1.35 }}>
                  {loggingIn
                    ? "Complete authorization in your browser window. Once approved, syncMyShit connects automatically."
                    : "Sign in with your Google account to sync saves across your Steam Deck and Android handhelds."}
                </div>
              </div>
            </PanelSectionRow>

            {!loggingIn ? (
              <PanelSectionRow>
                <ButtonItem
                  layout="below"
                  onClick={handleStartGoogleLogin}
                  disabled={loggingIn}
                >
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" }}>
                    <FaGoogle size={13} />
                    <span>Sign In to Google Drive</span>
                  </div>
                </ButtonItem>
              </PanelSectionRow>
            ) : (
              <>
                <PanelSectionRow>
                  <ButtonItem
                    layout="below"
                    onClick={() => openSteamBrowser(authUrl)}
                  >
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" }}>
                      <FaExternalLinkAlt size={12} />
                      <span>🌐 Open in Steam Browser</span>
                    </div>
                  </ButtonItem>
                </PanelSectionRow>

                <PanelSectionRow>
                  <ButtonItem
                    layout="below"
                    onClick={() => copyToClipboard(authUrl)}
                  >
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" }}>
                      <FaCopy size={12} />
                      <span>📋 Copy Sign-In Link</span>
                    </div>
                  </ButtonItem>
                </PanelSectionRow>

                <PanelSectionRow>
                  <ButtonItem
                    layout="below"
                    onClick={handleCancelLogin}
                  >
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                      <FaTimes size={12} />
                      <span>Cancel Sign-In</span>
                    </div>
                  </ButtonItem>
                </PanelSectionRow>
              </>
            )}

            {/* Manual Code Input Toggle */}
            <PanelSectionRow>
              <ButtonItem
                layout="below"
                onClick={() => setShowManualCode(!showManualCode)}
              >
                <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                  <FaKey size={11} />
                  <span>{showManualCode ? "Hide Manual Input" : "Paste Code / URL on Steam Deck"}</span>
                </div>
              </ButtonItem>
            </PanelSectionRow>

            {showManualCode && (
              <>
                <PanelSectionRow>
                  <TextField
                    label="Authorization Code / URL"
                    value={manualCode}
                    onChange={(e) => setManualCode(e.target.value)}
                  />
                </PanelSectionRow>
                <PanelSectionRow>
                  <ButtonItem
                    layout="below"
                    onClick={handleSubmitManualCode}
                    disabled={!manualCode.trim()}
                  >
                    Submit Code
                  </ButtonItem>
                </PanelSectionRow>
              </>
            )}
          </>
        )}
      </PanelSection>

      {/* Quick Sync Section */}
      <PanelSection title="Quick Sync">
        <PanelSectionRow>
          <ButtonItem
            layout="below"
            onClick={handleFullSync}
            disabled={syncing || !status?.is_authenticated}
          >
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" }}>
              <FaSyncAlt className={syncing && syncTargetId === "all" ? "fa-spin" : ""} />
              <span>
                {syncing && syncTargetId === "all"
                  ? "Syncing to Drive..."
                  : !status?.is_authenticated
                  ? "Sign in to Sync Saves"
                  : "⚡ Sync All Saves Now"}
              </span>
            </div>
          </ButtonItem>
        </PanelSectionRow>

        <PanelSectionRow>
          <Field
            label="Detected Saves"
            description={`${totalSaves} saves (${emulators.length} emulators)`}
          >
            <span style={{ color: "#22c55e", fontWeight: 700, fontSize: "12px" }}>Ready</span>
          </Field>
        </PanelSectionRow>

        <PanelSectionRow>
          <Field
            label="Last Drive Sync"
            description={formatTimestamp(status?.last_sync_timestamp || 0)}
          >
            <FaCheckCircle style={{ color: status?.last_sync_timestamp ? "#38bdf8" : "#64748b" }} />
          </Field>
        </PanelSectionRow>

        <PanelSectionRow>
          <ToggleField
            label="Auto-Sync on Game Exit"
            description="Uploads saves to Google Drive when emulator closes"
            checked={status?.auto_sync ?? true}
            onChange={handleToggleWatcher}
          />
        </PanelSectionRow>
      </PanelSection>

      {/* Detected Emulators List */}
      <PanelSection title={`Emulators (${emulators.length})`}>
        {emulators.length === 0 ? (
          <PanelSectionRow>
            <div style={{ fontSize: "12px", color: "#94a3b8", padding: "4px 0", textAlign: "center", width: "100%" }}>
              No emulator save directories found. Check your EmuDeck or emulator installation.
            </div>
          </PanelSectionRow>
        ) : (
          emulators.map((emu) => {
            const isThisSyncing = syncing && syncTargetId === emu.id;
            return (
              <PanelSectionRow key={emu.id}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    width: "100%",
                    maxWidth: "100%",
                    gap: "8px",
                    minWidth: 0,
                    boxSizing: "border-box",
                  }}
                >
                  <div style={{ flex: "1 1 auto", minWidth: 0, overflow: "hidden" }}>
                    <div
                      style={{
                        fontWeight: 600,
                        fontSize: "13px",
                        whiteSpace: "nowrap",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                      }}
                      title={emu.name}
                    >
                      {emu.name}
                    </div>
                    <div
                      style={{
                        fontSize: "11px",
                        color: "#94a3b8",
                        whiteSpace: "nowrap",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                      }}
                    >
                      {emu.category} • {emu.save_count} save{emu.save_count === 1 ? "" : "s"}
                    </div>
                  </div>
                  <div style={{ flex: "0 0 auto" }}>
                    <ButtonItem
                      layout="inline"
                      onClick={() => handleSingleSync(emu)}
                      disabled={syncing || !status?.is_authenticated}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12px" }}>
                        <FaSyncAlt className={isThisSyncing ? "fa-spin" : ""} size={11} />
                        <span>{isThisSyncing ? "..." : "Sync"}</span>
                      </div>
                    </ButtonItem>
                  </div>
                </div>
              </PanelSectionRow>
            );
          })
        )}
      </PanelSection>

      {/* Recent Activity Log */}
      <PanelSection title="Recent Activity">
        {logs.length === 0 ? (
          <PanelSectionRow>
            <div style={{ fontSize: "12px", color: "#94a3b8", padding: "4px 0", textAlign: "center", width: "100%" }}>
              No recent sync activity yet.
            </div>
          </PanelSectionRow>
        ) : (
          logs.slice(0, 8).map((entry, idx) => (
            <PanelSectionRow key={idx}>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  width: "100%",
                  maxWidth: "100%",
                  boxSizing: "border-box",
                  gap: "2px",
                  fontSize: "11px",
                  minWidth: 0,
                  background: "rgba(255, 255, 255, 0.03)",
                  padding: "5px 8px",
                  borderRadius: "4px",
                  borderLeft: `3px solid ${entry.type === "upload" ? "#4ade80" : "#38bdf8"}`,
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%", minWidth: 0 }}>
                  <span style={{ color: entry.type === "upload" ? "#4ade80" : "#38bdf8", fontWeight: 700, fontSize: "11px" }}>
                    [{entry.type.toUpperCase()}] {entry.emulator}
                  </span>
                  <span style={{ color: "#64748b", fontSize: "10px", whiteSpace: "nowrap", flexShrink: 0, marginLeft: "6px" }}>
                    {entry.time}
                  </span>
                </div>
                <div
                  style={{
                    color: "#cbd5e1",
                    fontSize: "11px",
                    overflowWrap: "anywhere",
                    wordBreak: "break-word",
                    lineHeight: 1.3,
                    marginTop: "2px",
                  }}
                >
                  {entry.message}
                </div>
              </div>
            </PanelSectionRow>
          ))
        )}

        {logs.length > 0 && (
          <PanelSectionRow>
            <ButtonItem
              layout="below"
              onClick={handleClearLogs}
            >
              <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                <FaTrashAlt size={11} />
                <span>Clear Activity Log</span>
              </div>
            </ButtonItem>
          </PanelSectionRow>
        )}
      </PanelSection>

      {/* Plugin Information */}
      <PanelSection title="Plugin Info">
        <PanelSectionRow>
          <Field label="Version" description="syncMyShit Decky Plugin">
            <span style={{ color: "#38bdf8", fontWeight: 700, fontSize: "12px" }}>v1.0.22</span>
          </Field>
        </PanelSectionRow>

        <PanelSectionRow>
          <div
            style={{
              fontSize: "10px",
              color: "#64748b",
              lineHeight: 1.4,
              width: "100%",
              boxSizing: "border-box",
              padding: "4px 0",
            }}
          >
            Update or uninstall via Konsole (Desktop Mode):
            <div style={{ color: "#94a3b8", fontFamily: "monospace", marginTop: "2px", overflowWrap: "anywhere", wordBreak: "break-all" }}>
              curl -sSL https://raw.githubusercontent.com/Kyss007/syncMyShit/main/update-decky.sh | bash
            </div>
            <div style={{ color: "#94a3b8", fontFamily: "monospace", marginTop: "2px", overflowWrap: "anywhere", wordBreak: "break-all" }}>
              curl -sSL https://raw.githubusercontent.com/Kyss007/syncMyShit/main/uninstall-decky.sh | bash
            </div>
          </div>
        </PanelSectionRow>
      </PanelSection>
    </div>
  );
};

export default definePlugin(() => {
  return {
    name: "syncMyShit",
    titleView: <div className={staticClasses.Title}>syncMyShit</div>,
    content: <Content />,
    icon: <FaGamepad />,
    alwaysRender: true,
    onDismount() {},
  };
});
