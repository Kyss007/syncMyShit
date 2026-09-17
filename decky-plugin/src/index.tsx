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
  FaCloud,
  FaFolder,
  FaGamepad,
  FaHistory,
  FaSyncAlt,
  FaTrashAlt,
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

// Callable bindings to Python backend (Plugin class)
const apiGetStatus = callable<[], StatusResponse>("get_status");
const apiScanSaves = callable<[], ScanResponse>("scan_saves");
const apiRunSync = callable<[emulatorId?: string], SyncResponse>("run_sync");
const apiToggleWatcher = callable<[enabled: boolean], { success: boolean; auto_sync: boolean }>("toggle_watcher");
const apiSetSyncFolder = callable<[path: string], { success: boolean; sync_folder?: string; error?: string }>("set_sync_folder");
const apiGetRecentLogs = callable<[], LogsResponse>("get_recent_logs");
const apiClearLogs = callable<[], { success: boolean }>("clear_logs");

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
  const [loadingInitial, setLoadingInitial] = useState<boolean>(true);

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
    } finally {
      setLoadingInitial(false);
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

  return (
    <div>
      {/* Overview & Quick Sync */}
      <PanelSection title="Quick Sync">
        <PanelSectionRow>
          <ButtonItem
            layout="below"
            onClick={handleFullSync}
            disabled={syncing}
          >
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px" }}>
              <FaSyncAlt className={syncing && syncTargetId === "all" ? "fa-spin" : ""} />
              <span>{syncing && syncTargetId === "all" ? "Syncing Saves..." : "Sync All Saves Now"}</span>
            </div>
          </ButtonItem>
        </PanelSectionRow>

        <PanelSectionRow>
          <Field
            label="Detected Saves"
            description={`${totalSaves} save files across ${emulators.length} emulators`}
          >
            <span style={{ color: "#22c55e", fontWeight: "bold" }}>Ready</span>
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
            description="Automatically uploads saves when an emulator process closes."
            checked={status?.auto_sync ?? true}
            onChange={handleToggleWatcher}
          />
        </PanelSectionRow>
      </PanelSection>

      {/* Detected Emulators List */}
      <PanelSection title={`Emulators (${emulators.length})`}>
        {emulators.length === 0 ? (
          <PanelSectionRow>
            <Field
              label="No Emulators Found"
              description="Make sure your emulators or EmuDeck are installed."
            />
          </PanelSectionRow>
        ) : (
          emulators.map((emu) => {
            const isThisSyncing = syncing && syncTargetId === emu.id;
            return (
              <PanelSectionRow key={emu.id}>
                <div style={{ display: "flex", flexDirection: "column", width: "100%", gap: "4px" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: "14px" }}>{emu.name}</div>
                      <div style={{ fontSize: "11px", color: "#94a3b8" }}>
                        {emu.category} • {emu.save_count} save{emu.save_count === 1 ? "" : "s"}
                      </div>
                    </div>
                    <ButtonItem
                      layout="inline"
                      onClick={() => handleSingleSync(emu)}
                      disabled={syncing}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                        <FaSyncAlt className={isThisSyncing ? "fa-spin" : ""} size={12} />
                        <span>{isThisSyncing ? "Syncing..." : "Sync"}</span>
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
          <Field
            label="Active Folder"
            description={status?.sync_folder || "None"}
          >
            <FaFolder style={{ color: "#eab308" }} />
          </Field>
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
          <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", width: "100%", paddingTop: "4px" }}>
            <ButtonItem
              layout="inline"
              onClick={() => handleApplyFolder("~/GoogleDrive/syncMyShit")}
            >
              Google Drive
            </ButtonItem>
            <ButtonItem
              layout="inline"
              onClick={() => handleApplyFolder("~/Syncthing/syncMyShit")}
            >
              Syncthing
            </ButtonItem>
            <ButtonItem
              layout="inline"
              onClick={() => handleApplyFolder("~/Nextcloud/syncMyShit")}
            >
              Nextcloud
            </ButtonItem>
            <ButtonItem
              layout="inline"
              onClick={() => handleApplyFolder("~/.config/syncMyShit/cloud_sync")}
            >
              Default Internal
            </ButtonItem>
          </div>
        </PanelSectionRow>
      </PanelSection>

      {/* Recent Activity Log */}
      <PanelSection title="Recent Activity">
        {logs.length === 0 ? (
          <PanelSectionRow>
            <div style={{ fontSize: "12px", color: "#94a3b8", padding: "4px 0" }}>
              No recent sync activity yet.
            </div>
          </PanelSectionRow>
        ) : (
          logs.slice(0, 8).map((entry, idx) => (
            <PanelSectionRow key={idx}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%", fontSize: "12px" }}>
                <div>
                  <span style={{ color: entry.type === "upload" ? "#4ade80" : "#38bdf8", fontWeight: "bold" }}>
                    [{entry.type.toUpperCase()}]
                  </span>{" "}
                  <span>{entry.emulator}: {entry.message}</span>
                </div>
                <div style={{ color: "#64748b", fontSize: "10px", marginLeft: "8px", whiteSpace: "nowrap" }}>
                  {entry.time}
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
                <FaTrashAlt size={12} />
                <span>Clear Activity Log</span>
              </div>
            </ButtonItem>
          </PanelSectionRow>
        )}
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
