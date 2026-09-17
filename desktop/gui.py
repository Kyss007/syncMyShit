#!/usr/bin/env python3
"""
syncMyShit - Desktop Graphical User Interface (Linux & Windows)
A modern, dark-themed GUI for synchronizing retro emulator save files.
Dedicated to the Public Domain (The Unlicense)
"""

import os
import sys
import threading
import time
import subprocess
from datetime import datetime
from pathlib import Path
from typing import Optional, List, Dict

import tkinter as tk
from tkinter import ttk, messagebox, filedialog

from config import ConfigManager
from emulator_registry import build_emulator_database, detect_installed_emulators
from drive_sync import FolderSyncProvider
from process_monitor import ProcessMonitor
from sync_engine import SyncEngine


# ─────────────────────────────────────────────────────────────────────────────
# Theme & Color Palette
# ─────────────────────────────────────────────────────────────────────────────
BG_DARK = "#12131a"
BG_CARD = "#1c1d27"
BG_CARD_LIGHTER = "#262837"
BG_INPUT = "#181922"
ACCENT_PRIMARY = "#6366f1"       # Indigo
ACCENT_PRIMARY_HOVER = "#4f46e5"
ACCENT_SUCCESS = "#10b981"       # Emerald
ACCENT_WARNING = "#f59e0b"       # Amber
ACCENT_DANGER = "#ef4444"        # Red
TEXT_PRIMARY = "#f3f4f6"
TEXT_SECONDARY = "#9ca3af"
TEXT_MUTED = "#6b7280"
BORDER_COLOR = "#2e3042"


class SyncMyShitGUI(tk.Tk):
    def __init__(self):
        super().__init__()

        self.title("syncMyShit Desktop - Save Sync")
        self.geometry("820x680")
        self.minsize(700, 560)
        self.configure(bg=BG_DARK)

        self.config = ConfigManager()
        self.engine = SyncEngine(keep_backups=int(self.config.get("keep_backups_count", 5)))
        self.detected_emulators: List[Dict[str, object]] = []

        self.watcher_thread: Optional[threading.Thread] = None
        self.watcher_monitor: Optional[ProcessMonitor] = None
        self.is_watching = False
        self.is_syncing = False

        self._setup_styles()
        self._build_ui()
        self._refresh_emulators()

        # Center on screen
        self.update_idletasks()
        w = self.winfo_width()
        h = self.winfo_height()
        ws = self.winfo_screenwidth()
        hs = self.winfo_screenheight()
        x = (ws // 2) - (w // 2)
        y = (hs // 2) - (h // 2)
        self.geometry(f"+{x}+{y}")

        # Initial log
        self.log("🚀 syncMyShit Desktop started. Ready to sync saves!", "info")

    def _setup_styles(self):
        style = ttk.Style(self)
        style.theme_use("clam")

        # General frame style
        style.configure("TFrame", background=BG_DARK)
        style.configure("Card.TFrame", background=BG_CARD, relief="flat")
        style.configure("CardLight.TFrame", background=BG_CARD_LIGHTER, relief="flat")

        # Labels
        style.configure("TLabel", background=BG_DARK, foreground=TEXT_PRIMARY, font=("Segoe UI", 10))
        style.configure("Title.TLabel", background=BG_DARK, foreground=TEXT_PRIMARY, font=("Segoe UI", 16, "bold"))
        style.configure("Subtitle.TLabel", background=BG_DARK, foreground=TEXT_SECONDARY, font=("Segoe UI", 9))
        style.configure("CardTitle.TLabel", background=BG_CARD, foreground=TEXT_PRIMARY, font=("Segoe UI", 11, "bold"))
        style.configure("CardSub.TLabel", background=BG_CARD, foreground=TEXT_SECONDARY, font=("Segoe UI", 9))
        style.configure("Badge.TLabel", background=ACCENT_PRIMARY, foreground="#ffffff", font=("Segoe UI", 9, "bold"), padding=(6, 2))
        style.configure("StatusSuccess.TLabel", background=BG_CARD, foreground=ACCENT_SUCCESS, font=("Segoe UI", 10, "bold"))
        style.configure("StatusWatching.TLabel", background=BG_CARD, foreground=ACCENT_WARNING, font=("Segoe UI", 10, "bold"))

        # Primary Buttons
        style.configure(
            "Primary.TButton",
            background=ACCENT_PRIMARY,
            foreground="#ffffff",
            font=("Segoe UI", 10, "bold"),
            padding=(14, 8),
            borderwidth=0,
            focuscolor="none"
        )
        style.map("Primary.TButton", background=[("active", ACCENT_PRIMARY_HOVER), ("disabled", BORDER_COLOR)])

        # Secondary Buttons
        style.configure(
            "Secondary.TButton",
            background=BG_CARD_LIGHTER,
            foreground=TEXT_PRIMARY,
            font=("Segoe UI", 9),
            padding=(10, 6),
            borderwidth=0,
            focuscolor="none"
        )
        style.map("Secondary.TButton", background=[("active", BORDER_COLOR)])

        # Accent Success Button
        style.configure(
            "Success.TButton",
            background=ACCENT_SUCCESS,
            foreground="#ffffff",
            font=("Segoe UI", 10, "bold"),
            padding=(14, 8),
            borderwidth=0,
            focuscolor="none"
        )
        style.map("Success.TButton", background=[("active", "#059669")])

        # Accent Warning Button
        style.configure(
            "Warning.TButton",
            background=ACCENT_WARNING,
            foreground="#111827",
            font=("Segoe UI", 10, "bold"),
            padding=(14, 8),
            borderwidth=0,
            focuscolor="none"
        )
        style.map("Warning.TButton", background=[("active", "#d97706")])

    def _build_ui(self):
        main_container = tk.Frame(self, bg=BG_DARK, padx=18, pady=16)
        main_container.pack(fill="both", expand=True)

        # ── Header ──────────────────────────────────────────────────────────
        header_frame = tk.Frame(main_container, bg=BG_DARK)
        header_frame.pack(fill="x", pady=(0, 14))

        title_box = tk.Frame(header_frame, bg=BG_DARK)
        title_box.pack(side="left")
        ttk.Label(title_box, text="🎮 syncMyShit Desktop", style="Title.TLabel").pack(anchor="w")
        ttk.Label(title_box, text="Automagic Retro Emulator Cloud Save Sync  •  v1.0.13", style="Subtitle.TLabel").pack(anchor="w")

        self.status_badge = tk.Label(
            header_frame,
            text="● READY",
            bg="#1f2937",
            fg=ACCENT_SUCCESS,
            font=("Segoe UI", 9, "bold"),
            padx=10,
            pady=4,
            relief="flat"
        )
        self.status_badge.pack(side="right", anchor="center")

        # ── Sync Folder / Cloud Target Card ─────────────────────────────────
        folder_card = tk.Frame(main_container, bg=BG_CARD, padx=14, pady=12, highlightbackground=BORDER_COLOR, highlightthickness=1)
        folder_card.pack(fill="x", pady=(0, 12))

        f_header = tk.Frame(folder_card, bg=BG_CARD)
        f_header.pack(fill="x", pady=(0, 6))
        ttk.Label(f_header, text="Cloud & Sync Target Folder", style="CardTitle.TLabel").pack(side="left")
        ttk.Label(f_header, text="Points to your Google Drive, Syncthing, Nextcloud, or local sync directory", style="CardSub.TLabel").pack(side="left", padx=8)

        f_row = tk.Frame(folder_card, bg=BG_CARD)
        f_row.pack(fill="x")

        self.folder_var = tk.StringVar(value=self._get_active_sync_folder())
        self.folder_entry = tk.Entry(
            f_row,
            textvariable=self.folder_var,
            font=("Segoe UI", 9),
            bg=BG_INPUT,
            fg=TEXT_PRIMARY,
            insertbackground=TEXT_PRIMARY,
            relief="flat",
            highlightbackground=BORDER_COLOR,
            highlightthickness=1,
            state="readonly"
        )
        self.folder_entry.pack(side="left", fill="x", expand=True, ipady=6, padx=(0, 8))

        ttk.Button(f_row, text="Browse...", style="Secondary.TButton", command=self._browse_sync_folder).pack(side="left", padx=(0, 6))
        ttk.Button(f_row, text="Open Folder", style="Secondary.TButton", command=self._open_sync_folder).pack(side="left")

        # ── Quick Action Buttons ────────────────────────────────────────────
        actions_frame = tk.Frame(main_container, bg=BG_DARK)
        actions_frame.pack(fill="x", pady=(0, 12))

        self.sync_btn = ttk.Button(actions_frame, text="⚡ Sync Saves Now", style="Primary.TButton", command=self.on_sync_clicked)
        self.sync_btn.pack(side="left", padx=(0, 8))

        self.scan_btn = ttk.Button(actions_frame, text="🔍 Scan Saves", style="Secondary.TButton", command=self.on_scan_clicked)
        self.scan_btn.pack(side="left", padx=(0, 8))

        self.watch_btn = ttk.Button(actions_frame, text="👁 Start Auto-Watcher", style="Success.TButton", command=self.on_watch_toggle_clicked)
        self.watch_btn.pack(side="left", padx=(0, 8))

        ttk.Button(actions_frame, text="➕ Add Custom Path", style="Secondary.TButton", command=self.on_add_custom_path_clicked).pack(side="left", padx=(0, 8))
        ttk.Button(actions_frame, text="🔄 Refresh", style="Secondary.TButton", command=self._refresh_emulators).pack(side="right")

        # ── Middle Section: Notebook (Emulators / Settings) ──────────────────
        notebook_frame = tk.Frame(main_container, bg=BG_CARD, highlightbackground=BORDER_COLOR, highlightthickness=1)
        notebook_frame.pack(fill="both", expand=True, pady=(0, 12))

        # Detected Emulators Treeview
        tree_header = tk.Frame(notebook_frame, bg=BG_CARD, padx=12, pady=8)
        tree_header.pack(fill="x")
        self.emu_count_lbl = ttk.Label(tree_header, text="Detected Emulators (0)", style="CardTitle.TLabel")
        self.emu_count_lbl.pack(side="left")

        tree_container = tk.Frame(notebook_frame, bg=BG_CARD)
        tree_container.pack(fill="both", expand=True, padx=10, pady=(0, 10))

        cols = ("name", "system", "path")
        self.tree = ttk.Treeview(tree_container, columns=cols, show="headings", height=6)
        self.tree.heading("name", text="Emulator")
        self.tree.heading("system", text="System")
        self.tree.heading("path", text="Save Directory Path")

        self.tree.column("name", width=160, minwidth=120)
        self.tree.column("system", width=150, minwidth=110)
        self.tree.column("path", width=420, minwidth=250)

        tree_scroll = ttk.Scrollbar(tree_container, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=tree_scroll.set)
        self.tree.pack(side="left", fill="both", expand=True)
        tree_scroll.pack(side="right", fill="y")

        # ── Live Activity Log ───────────────────────────────────────────────
        log_card = tk.Frame(main_container, bg=BG_CARD, padx=10, pady=8, highlightbackground=BORDER_COLOR, highlightthickness=1)
        log_card.pack(fill="both", expand=True)

        log_header = tk.Frame(log_card, bg=BG_CARD)
        log_header.pack(fill="x", pady=(0, 4))
        ttk.Label(log_header, text="Activity Log", style="CardTitle.TLabel").pack(side="left")
        ttk.Button(log_header, text="Clear", style="Secondary.TButton", command=self._clear_log).pack(side="right")

        log_container = tk.Frame(log_card, bg=BG_INPUT)
        log_container.pack(fill="both", expand=True)

        self.log_text = tk.Text(
            log_container,
            bg=BG_INPUT,
            fg=TEXT_PRIMARY,
            font=("Consolas" if sys.platform == "win32" else "Monospace", 9),
            relief="flat",
            wrap="word",
            state="disabled",
            insertbackground=TEXT_PRIMARY
        )
        log_scroll = ttk.Scrollbar(log_container, orient="vertical", command=self.log_text.yview)
        self.log_text.configure(yscrollcommand=log_scroll.set)
        self.log_text.pack(side="left", fill="both", expand=True)
        log_scroll.pack(side="right", fill="y")

        # Log color tags
        self.log_text.tag_config("info", foreground="#60a5fa")      # Blue
        self.log_text.tag_config("success", foreground="#34d399")   # Green
        self.log_text.tag_config("warning", foreground="#fbbf24")   # Yellow
        self.log_text.tag_config("error", foreground="#f87171")     # Red
        self.log_text.tag_config("dim", foreground=TEXT_MUTED)

    # ─────────────────────────────────────────────────────────────────────────
    # Helper & Config methods
    # ─────────────────────────────────────────────────────────────────────────
    def _get_active_sync_folder(self) -> str:
        f = self.config.get("local_sync_folder")
        if not f:
            f = str(self.config.config_dir / "syncMyShit")
            self.config.set("local_sync_folder", f)
            self.config.set("sync_mode", "local_folder")
        Path(f).mkdir(parents=True, exist_ok=True)
        return f

    def _browse_sync_folder(self):
        cur = self.folder_var.get()
        chosen = filedialog.askdirectory(initialdir=cur, title="Select Sync Folder (Google Drive, Syncthing, etc.)")
        if chosen:
            p = str(Path(chosen).resolve())
            self.folder_var.set(p)
            self.config.set("local_sync_folder", p)
            self.config.set("sync_mode", "local_folder")
            self.log(f"📁 Sync folder changed to: {p}", "info")

    def _open_sync_folder(self):
        folder = self.folder_var.get()
        if not os.path.exists(folder):
            os.makedirs(folder, exist_ok=True)
        try:
            if sys.platform == "win32":
                os.startfile(folder)
            elif sys.platform == "darwin":
                subprocess.Popen(["open", folder])
            else:
                subprocess.Popen(["xdg-open", folder])
        except Exception as e:
            self.log(f"Could not open folder: {e}", "error")

    def _clear_log(self):
        self.log_text.config(state="normal")
        self.log_text.delete("1.0", tk.END)
        self.log_text.config(state="disabled")

    def log(self, message: str, tag: str = "info"):
        now = datetime.now().strftime("%H:%M:%S")
        self.log_text.config(state="normal")
        self.log_text.insert(tk.END, f"[{now}] ", "dim")
        self.log_text.insert(tk.END, f"{message}\n", tag)
        self.log_text.see(tk.END)
        self.log_text.config(state="disabled")

    def _refresh_emulators(self):
        self.detected_emulators = detect_installed_emulators()
        custom_paths = self.config.get("custom_paths", [])

        # Clear tree
        for item in self.tree.get_children():
            self.tree.delete(item)

        count = len(self.detected_emulators) + len(custom_paths)
        self.emu_count_lbl.config(text=f"Detected Emulators & Custom Games ({count})")

        for emu in self.detected_emulators:
            for p in emu["paths"]:
                self.tree.insert("", tk.END, values=(emu["name"], emu["category"], p))

        for cp in custom_paths:
            self.tree.insert("", tk.END, values=(cp["name"], "Custom Game / Port", cp["path"]))

        self.log(f"🔍 Discovered {len(self.detected_emulators)} emulators and {len(custom_paths)} custom game paths on your system.", "info")

    # ─────────────────────────────────────────────────────────────────────────
    # Actions
    # ─────────────────────────────────────────────────────────────────────────
    def on_sync_clicked(self):
        if self.is_syncing:
            return
        self.is_syncing = True
        self.sync_btn.config(state="disabled")
        self.status_badge.config(text="● SYNCING...", fg="#60a5fa")

        def _run():
            try:
                target_folder = Path(self.folder_var.get())
                provider = FolderSyncProvider(target_folder, self.engine)

                total_synced = 0
                for emu in self.detected_emulators:
                    paths = [Path(p) for p in emu["paths"]]
                    logs = provider.sync_emulator(emu["id"], paths, emu["extensions"])
                    if logs:
                        for l in logs:
                            # Clean rich tags
                            clean = l.replace("[green]", "").replace("[/green]", "").replace("[cyan]", "").replace("[/cyan]", "")
                            self.after(0, lambda msg=f"{emu['name']}: {clean}": self.log(msg, "success"))
                            total_synced += 1

                # Sync custom paths
                for cp in self.config.get("custom_paths", []):
                    p = Path(cp["path"])
                    if p.exists():
                        logs = provider.sync_emulator(cp["name"].lower().replace(" ", "_"), [p], [])
                        for l in logs:
                            clean = l.replace("[green]", "").replace("[/green]", "").replace("[cyan]", "").replace("[/cyan]", "")
                            self.after(0, lambda msg=f"{cp['name']}: {clean}": self.log(msg, "success"))
                            total_synced += 1

                self.after(0, lambda: self.log(f"✔ Sync finished successfully! ({total_synced} operations performed)", "success"))
            except Exception as e:
                self.after(0, lambda: self.log(f"Sync error: {e}", "error"))
            finally:
                self.after(0, self._on_sync_done)

        threading.Thread(target=_run, daemon=True).start()

    def _on_sync_done(self):
        self.is_syncing = False
        self.sync_btn.config(state="normal")
        if self.is_watching:
            self.status_badge.config(text="● WATCHING", fg=ACCENT_WARNING)
        else:
            self.status_badge.config(text="● READY", fg=ACCENT_SUCCESS)

    def on_scan_clicked(self):
        self.log("🔍 Scanning save files across detected emulators...", "info")

        def _scan():
            total_saves = 0
            for emu in self.detected_emulators:
                paths = [Path(p) for p in emu["paths"]]
                files = []
                for p in paths:
                    files.extend(self.engine.scan_directory(p, emu["extensions"]))
                if files:
                    total_saves += len(files)
                    self.after(0, lambda e=emu, c=len(files): self.log(f"📁 {e['name']}: {c} save files found", "info"))
                    for f in files[:3]:
                        sz_kb = f["size"] / 1024.0
                        self.after(0, lambda name=f['name'], sz=sz_kb: self.log(f"    • {name} ({sz:.1f} KB)", "dim"))
                    if len(files) > 3:
                        self.after(0, lambda rem=len(files)-3: self.log(f"    ... and {rem} more saves", "dim"))

            self.after(0, lambda: self.log(f"Scan complete. Total saves found: {total_saves}", "success"))

        threading.Thread(target=_scan, daemon=True).start()

    def on_watch_toggle_clicked(self):
        if not self.is_watching:
            # Start watcher
            self.is_watching = True
            self.watch_btn.config(text="⏹ Stop Auto-Watcher", style="Warning.TButton")
            self.status_badge.config(text="● WATCHING", fg=ACCENT_WARNING)
            self.log("👁 Auto-Sync Watcher daemon started! Monitoring game processes in background...", "warning")

            target_folder = Path(self.folder_var.get())
            provider = FolderSyncProvider(target_folder, self.engine)

            targets = {}
            for emu in self.detected_emulators:
                targets[emu["id"]] = emu["process_names"]
            emu_map = {e["id"]: e for e in self.detected_emulators}

            def on_launch(emu_id: str, pid: str):
                emu = emu_map.get(emu_id)
                name = emu["name"] if emu else emu_id
                self.after(0, lambda: self.log(f"▶ Emulator Started: {name} (PID {pid}). Pulling newest cloud saves...", "warning"))
                if emu:
                    logs = provider.sync_emulator(emu_id, [Path(p) for p in emu["paths"]], emu["extensions"])
                    for l in logs:
                        clean = l.replace("[green]", "").replace("[/green]", "").replace("[cyan]", "").replace("[/cyan]", "")
                        self.after(0, lambda msg=clean: self.log(f"  {msg}", "info"))

            def on_exit(emu_id: str, pid: str):
                emu = emu_map.get(emu_id)
                name = emu["name"] if emu else emu_id
                self.after(0, lambda: self.log(f"⏹ Emulator Closed: {name} (PID {pid}). Backing up new saves to cloud...", "warning"))
                if emu:
                    logs = provider.sync_emulator(emu_id, [Path(p) for p in emu["paths"]], emu["extensions"])
                    for l in logs:
                        clean = l.replace("[green]", "").replace("[/green]", "").replace("[cyan]", "").replace("[/cyan]", "")
                        self.after(0, lambda msg=clean: self.log(f"  {msg}", "success"))

            self.watcher_monitor = ProcessMonitor(
                emulator_targets=targets,
                on_emulator_launched=on_launch,
                on_emulator_exited=on_exit,
                poll_interval=float(self.config.get("poll_interval_seconds", 3))
            )

            def _watch_loop():
                while self.is_watching:
                    if self.watcher_monitor:
                        self.watcher_monitor.poll_once()
                    time.sleep(float(self.config.get("poll_interval_seconds", 3)))

            self.watcher_thread = threading.Thread(target=_watch_loop, daemon=True)
            self.watcher_thread.start()
        else:
            # Stop watcher
            self.is_watching = False
            self.watch_btn.config(text="👁 Start Auto-Watcher", style="Success.TButton")
            self.status_badge.config(text="● READY", fg=ACCENT_SUCCESS)
            self.log("⏹ Auto-Sync Watcher daemon stopped.", "info")

    def on_add_custom_path_clicked(self):
        dialog = tk.Toplevel(self)
        dialog.title("Add Custom Game / Emulator Path")
        dialog.geometry("500x220")
        dialog.configure(bg=BG_CARD)
        dialog.transient(self)
        dialog.grab_set()

        # Center dialog
        dialog.update_idletasks()
        x = self.winfo_x() + (self.winfo_width() // 2) - 250
        y = self.winfo_y() + (self.winfo_height() // 2) - 110
        dialog.geometry(f"+{x}+{y}")

        pad = tk.Frame(dialog, bg=BG_CARD, padx=16, pady=16)
        pad.pack(fill="both", expand=True)

        ttk.Label(pad, text="Game or Emulator Name:", style="CardSub.TLabel").pack(anchor="w", pady=(0, 2))
        name_entry = tk.Entry(pad, font=("Segoe UI", 10), bg=BG_INPUT, fg=TEXT_PRIMARY, relief="flat", highlightbackground=BORDER_COLOR, highlightthickness=1)
        name_entry.pack(fill="x", ipady=4, pady=(0, 10))

        ttk.Label(pad, text="Save Folder Path:", style="CardSub.TLabel").pack(anchor="w", pady=(0, 2))
        p_row = tk.Frame(pad, bg=BG_CARD)
        p_row.pack(fill="x", pady=(0, 16))

        path_var = tk.StringVar()
        path_entry = tk.Entry(p_row, textvariable=path_var, font=("Segoe UI", 10), bg=BG_INPUT, fg=TEXT_PRIMARY, relief="flat", highlightbackground=BORDER_COLOR, highlightthickness=1)
        path_entry.pack(side="left", fill="x", expand=True, ipady=4, padx=(0, 6))

        def _browse():
            d = filedialog.askdirectory(title="Select Game Save Directory")
            if d:
                path_var.set(d)

        ttk.Button(p_row, text="Browse...", style="Secondary.TButton", command=_browse).pack(side="left")

        def _save():
            name = name_entry.get().strip()
            path_str = path_var.get().strip()
            if not name or not path_str:
                messagebox.showerror("Error", "Please provide both a name and a save folder path.", parent=dialog)
                return
            p = Path(path_str).expanduser().resolve()
            if not p.exists():
                messagebox.showerror("Error", f"Folder does not exist:\n{p}", parent=dialog)
                return
            self.config.add_custom_path(name, str(p))
            self._refresh_emulators()
            self.log(f"✔ Added custom path: {name} -> {p}", "success")
            dialog.destroy()

        b_row = tk.Frame(pad, bg=BG_CARD)
        b_row.pack(fill="x")
        ttk.Button(b_row, text="Save", style="Primary.TButton", command=_save).pack(side="right")
        ttk.Button(b_row, text="Cancel", style="Secondary.TButton", command=dialog.destroy).pack(side="right", padx=(0, 8))


def run_gui():
    app = SyncMyShitGUI()
    app.mainloop()


if __name__ == "__main__":
    run_gui()
