import {
  ButtonItem,
  Field,
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
  FaFolder,
  FaGamepad,
  FaSyncAlt,
  FaTrashAlt,
  FaArrowAltCircleUp,
} from "react-icons/fa";

// RPC method typings
interface StatusResponse {
  success: boolean;
  sync_folder: string;
  sync_mode: string;
  auto_sync: boolean;
  is_monitoring: boolean;
  last_sync_timestamp: number;
  detected_emulators_count: number;
  custom_paths_count: number;
  keep_backups: number;
}

interface EmulatorSavePreview {
  name: string;
  relative: string;
  size: number;
  mtime: number;
}

interface EmulatorItem {
  id: string;
  name: string;
  category: string;
  paths: string[];
  drive_folder: string;
  save_count: number;
  saves: EmulatorSavePreview[];
}

interface ScanResponse {
  success: boolean;
  total_saves: number;
  emulators: EmulatorItem[];
}

interface SyncResponse {
  success: boolean;
  operations_count: number;
  logs: string[];
  message: string;
  timestamp: number;
}

interface LogEntry {
  time: string;
  emulator: string;
  message: string;
  type: "upload" | "download";
}

interface LogsResponse {
  success: boolean;
  logs: LogEntry[];
}

interface UpdateResponse {
  success: boolean;
  message?: string;
  error?: string;
}

// Callable bindings to Python backend (Plugin class)
const apiGetStatus = callable<[], StatusResponse>("get_status");
const apiScanSaves = callable<[], ScanResponse>("scan_saves");
const apiRunSync = callable<[emulatorId?: string], SyncResponse>("run_sync");
const apiToggleWatcher = callable<[enabled: boolean], { success: boolean; auto_sync: boolean }>("toggle_watcher");
const apiSetSyncFolder = callable<[path: string], { success: boolean; sync_folder?: string; error?: string }>("set_sync_folder");
const apiGetRecentLogs = callable<[], LogsResponse>("get_recent_logs");
const apiClearLogs = callable<[], { success: boolean }>("clear_logs");
const apiUpdatePlugin = callable<[], UpdateResponse>("update_plugin");

const formatTimestamp = (ts: number): string => {
  if (!ts || ts <= 0) return "Never";
  const diffSec = Math.floor(Date.now() / 1000 - ts);
  if (diffSec < 60) return "Just now";
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return new Date(ts * 1000).toLocaleDateString();
};

const Content: FC = () => {
  const [status, setStatus] = useState<StatusResponse | null>(null);
  const [emulators, setEmulators] = useState<EmulatorItem[]>([]);
  const [totalSaves, setTotalSaves] = useState<number>(0);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [syncing, setSyncing] = useState<boolean>(false);
  const [syncTargetId, setSyncTargetId] = useState<string | null>(null);
  const [customFolder, setCustomFolder] = useState<string>("");
  const [updating, setUpdating] = useState<boolean>(false);

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
        setCustomFolder(st.sync_folder);
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

  // Run full sync
  const handleFullSync = async () => {
    setSyncing(true);
    setSyncTargetId("all");
    try {
      const res = await apiRunSync();
      toaster.toast({
        title: "syncMyShit",
        body: res.message || "Save synchronization complete!",
        duration: 4000,
      });
      await refreshData();
    } catch (err: any) {
      toaster.toast({
        title: "syncMyShit Error",
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
    setSyncing(true);
    setSyncTargetId(emu.id);
    try {
      const res = await apiRunSync(emu.id);
      toaster.toast({
        title: `syncMyShit: ${emu.name}`,
        body: res.message || `Synchronized ${emu.name} saves`,
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
            ? "Auto-sync enabled (syncs on game exit)"
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

  // Apply custom sync folder
  const handleApplyFolder = async (path: string) => {
    if (!path.trim()) return;
    try {
      const res = await apiSetSyncFolder(path.trim());
      if (res.success && res.sync_folder) {
        setCustomFolder(res.sync_folder);
        if (status) setStatus({ ...status, sync_folder: res.sync_folder });
        toaster.toast({
          title: "Sync Target Updated",
          body: `Now syncing to: ${res.sync_folder}`,
          duration: 3500,
        });
        await refreshData();
      } else {
        toaster.toast({
          title: "Folder Error",
          body: res.error || "Could not set folder",
          duration: 4000,
        });
      }
    } catch (e: any) {
      toaster.toast({
        title: "Folder Error",
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

  // Update plugin from GitHub directly inside Decky
  const handleUpdatePlugin = async () => {
    setUpdating(true);
    try {
      const res = await apiUpdatePlugin();
      if (res.success) {
        toaster.toast({
          title: "syncMyShit Updated!",
          body: res.message || "Updated to latest version! Please close and reopen QAM.",
          duration: 6000,
        });
        await refreshData();
      } else {
        toaster.toast({
          title: "Update Failed",
          body: res.error || "Could not complete update.",
          duration: 5000,
        });
      }
    } catch (err: any) {
      toaster.toast({
        title: "Update Error",
        body: String(err?.message || err),
        duration: 5000,
      });
    } finally {
      setUpdating(false);
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
      {/* Overview & Quick Sync */}
      <PanelSection title="Quick Sync">
        <PanelSectionRow>
          <ButtonItem
            layout="below"
            onClick={handleFullSync}
            disabled={syncing}
          >
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" }}>
              <FaSyncAlt className={syncing && syncTargetId === "all" ? "fa-spin" : ""} />
              <span>{syncing && syncTargetId === "all" ? "Syncing Saves..." : "Sync All Saves Now"}</span>
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
            label="Last Cloud Sync"
            description={formatTimestamp(status?.last_sync_timestamp || 0)}
          >
            <FaCheckCircle style={{ color: "#38bdf8" }} />
          </Field>
        </PanelSectionRow>

        <PanelSectionRow>
          <ToggleField
            label="Auto-Sync on Game Exit"
            description="Uploads saves when emulator closes"
            checked={status?.auto_sync ?? true}
            onChange={handleToggleWatcher}
          />
        </PanelSectionRow>
      </PanelSection>

      {/* Detected Emulators List */}
      <PanelSection title={`Emulators (${emulators.length})`}>
        {emulators.length === 0 ? (
          <PanelSectionRow>
            <div style={{ fontSize: "12px", color: "#94a3b8", padding: "4px 0", textAlign: "center" }}>
              No emulator save directories found. Check your EmuDeck or emulator paths.
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
                      disabled={syncing}
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

      {/* Cloud Target Configuration */}
      <PanelSection title="Cloud / Sync Folder">
        <PanelSectionRow>
          <div style={{ width: "100%", maxWidth: "100%", boxSizing: "border-box" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: 600 }}>
              <FaFolder style={{ color: "#eab308" }} size={13} />
              <span>Active Cloud Folder</span>
            </div>
            <div
              style={{
                fontSize: "11px",
                color: "#94a3b8",
                marginTop: "4px",
                wordBreak: "break-all",
                overflowWrap: "anywhere",
                background: "rgba(0, 0, 0, 0.3)",
                padding: "6px 8px",
                borderRadius: "4px",
                border: "1px solid rgba(255, 255, 255, 0.08)",
                lineHeight: 1.35,
              }}
            >
              {status?.sync_folder || "None configured"}
            </div>
          </div>
        </PanelSectionRow>

        <PanelSectionRow>
          <TextField
            label="Custom Sync Path"
            value={customFolder}
            onChange={(e) => setCustomFolder(e.target.value)}
          />
        </PanelSectionRow>

        <PanelSectionRow>
          <ButtonItem
            layout="below"
            onClick={() => handleApplyFolder(customFolder)}
            disabled={!customFolder || customFolder === status?.sync_folder}
          >
            Save Target Path
          </ButtonItem>
        </PanelSectionRow>

        <PanelSectionRow>
          <div style={{ width: "100%", maxWidth: "100%", boxSizing: "border-box" }}>
            <div style={{ fontSize: "11px", color: "#94a3b8", marginBottom: "6px" }}>
              Quick Presets:
            </div>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "6px",
                width: "100%",
                maxWidth: "100%",
                boxSizing: "border-box",
              }}
            >
              <ButtonItem
                layout="below"
                onClick={() => handleApplyFolder("~/GoogleDrive/syncMyShit")}
              >
                Google Drive
              </ButtonItem>
              <ButtonItem
                layout="below"
                onClick={() => handleApplyFolder("~/Syncthing/syncMyShit")}
              >
                Syncthing
              </ButtonItem>
              <ButtonItem
                layout="below"
                onClick={() => handleApplyFolder("~/Nextcloud/syncMyShit")}
              >
                Nextcloud
              </ButtonItem>
              <ButtonItem
                layout="below"
                onClick={() => handleApplyFolder("~/.config/syncMyShit/cloud_sync")}
              >
                Default Local
              </ButtonItem>
            </div>
          </div>
        </PanelSectionRow>
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

      {/* Plugin Management & Updates */}
      <PanelSection title="Plugin Management">
        <PanelSectionRow>
          <Field label="Version" description="syncMyShit Decky Plugin">
            <span style={{ color: "#38bdf8", fontWeight: 700, fontSize: "12px" }}>v1.0.15</span>
          </Field>
        </PanelSectionRow>

        <PanelSectionRow>
          <ButtonItem
            layout="below"
            onClick={handleUpdatePlugin}
            disabled={updating}
          >
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
              <FaArrowAltCircleUp className={updating ? "fa-spin" : ""} size={13} />
              <span>{updating ? "Updating Plugin..." : "⚡ Update Plugin to Latest"}</span>
            </div>
          </ButtonItem>
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
            Tip: In Desktop Mode Konsole, you can also run:
            <div style={{ color: "#94a3b8", fontFamily: "monospace", marginTop: "2px", overflowWrap: "anywhere", wordBreak: "break-all" }}>
              curl -sSL .../update-decky.sh | bash
            </div>
            <div style={{ color: "#94a3b8", fontFamily: "monospace", marginTop: "2px", overflowWrap: "anywhere", wordBreak: "break-all" }}>
              curl -sSL .../uninstall-decky.sh | bash
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
    onDismount() {},
  };
});
