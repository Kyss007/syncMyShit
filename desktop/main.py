#!/usr/bin/env python3
"""
syncMyShit - Desktop Save Sync Client (Linux & Windows)
Automagic retro emulator cloud save sync across Android, Linux, and Windows.
Dedicated to the Public Domain (The Unlicense)
"""

import argparse
import sys
import time
from pathlib import Path

from config import ConfigManager
from drive_sync import FolderSyncProvider, GoogleDriveProvider
from emulator_registry import build_emulator_database, detect_installed_emulators
from process_monitor import ProcessMonitor
from sync_engine import SyncEngine

try:
    from rich.console import Console
    from rich.table import Table
    from rich.panel import Panel
    console = Console()
except ImportError:
    class DummyConsole:
        def print(self, *args, **kwargs):
            text = " ".join(str(a) for a in args)
            # Strip simple rich tags
            for tag in ["[cyan]", "[/cyan]", "[green]", "[/green]", "[yellow]", "[/yellow]", "[red]", "[/red]", "[bold]", "[/bold]"]:
                text = text.replace(tag, "")
            print(text)
    console = DummyConsole()


def cmd_status(config: ConfigManager, engine: SyncEngine):
    console.print("\n[bold cyan]🎮 syncMyShit Desktop Status[/bold cyan]")
    detected = detect_installed_emulators()
    custom_paths = config.get("custom_paths", [])

    console.print(f"Config Directory: [cyan]{config.config_dir}[/cyan]")
    sync_mode = config.get("sync_mode", "local_folder")
    console.print(f"Sync Mode: [green]{sync_mode}[/green]")

    if sync_mode == "local_folder":
        folder = config.get("local_sync_folder") or str(config.config_dir / "cloud_sync")
        console.print(f"Sync Folder: [cyan]{folder}[/cyan]")

    console.print(f"\n[bold]Detected Emulators ({len(detected)} found):[/bold]")
    if not detected:
        console.print("[yellow]No standard emulators automatically detected yet. Use 'add-path' to add custom save folders.[/yellow]")
    else:
        for emu in detected:
            console.print(f"  • [bold green]{emu['name']}[/bold green] ({emu['category']})")
            for p in emu["paths"]:
                console.print(f"    └── Path: {p}")

    if custom_paths:
        console.print(f"\n[bold]Custom Save Paths ({len(custom_paths)} configured):[/bold]")
        for cp in custom_paths:
            console.print(f"  • [bold]{cp['name']}[/bold]: {cp['path']}")
    console.print("")


def cmd_scan(config: ConfigManager, engine: SyncEngine):
    console.print("\n[bold cyan]🔍 Scanning Emulator Save Files...[/bold cyan]")
    detected = detect_installed_emulators()
    total_files = 0

    for emu in detected:
        paths = [Path(p) for p in emu["paths"]]
        files = []
        for p in paths:
            files.extend(engine.scan_directory(p, emu["extensions"]))

        if files:
            console.print(f"\n[bold green]{emu['name']}[/bold green] ({len(files)} saves):")
            for f in files:
                sz_kb = f["size"] / 1024.0
                console.print(f"  ├── {f['relative']} [dim]({sz_kb:.1f} KB)[/dim]")
            total_files += len(files)

    console.print(f"\n[bold]Total Save Files Found:[/bold] [cyan]{total_files}[/cyan]\n")


def cmd_sync(config: ConfigManager, engine: SyncEngine):
    console.print("\n[bold cyan]⚡ Running Save Sync...[/bold cyan]")
    sync_mode = config.get("sync_mode", "local_folder")
    target_folder = Path(config.get("local_sync_folder") or (config.config_dir / "cloud_sync"))
    provider = FolderSyncProvider(target_folder, engine)

    detected = detect_installed_emulators()
    for emu in detected:
        paths = [Path(p) for p in emu["paths"]]
        logs = provider.sync_emulator(emu["id"], paths, emu["extensions"])
        if logs:
            console.print(f"[bold]{emu['name']}:[/bold]")
            for log in logs:
                console.print(f"  {log}")

    console.print("[green]✔ Sync complete![/green]\n")


def cmd_watch(config: ConfigManager, engine: SyncEngine):
    console.print("\n[bold cyan]👁 syncMyShit Automagic Daemon Running[/bold cyan]")
    console.print("[dim]Monitoring running emulator processes in background... Press Ctrl+C to stop.[/dim]\n")

    detected = detect_installed_emulators()
    targets = {}
    for emu in detected:
        targets[emu["id"]] = emu["process_names"]

    sync_mode = config.get("sync_mode", "local_folder")
    target_folder = Path(config.get("local_sync_folder") or (config.config_dir / "cloud_sync"))
    provider = FolderSyncProvider(target_folder, engine)

    emu_map = {e["id"]: e for e in detected}

    def on_launch(emu_id: str, pid: str):
        emu = emu_map.get(emu_id)
        name = emu["name"] if emu else emu_id
        console.print(f"\n[bold green]▶ Emulator Started:[/bold green] {name} (PID {pid})")
        console.print("  [cyan]Running Pre-Play Cloud Check...[/cyan]")
        if emu:
            logs = provider.sync_emulator(emu_id, [Path(p) for p in emu["paths"]], emu["extensions"])
            for l in logs:
                console.print(f"    {l}")

    def on_exit(emu_id: str, pid: str):
        emu = emu_map.get(emu_id)
        name = emu["name"] if emu else emu_id
        console.print(f"\n[bold yellow]⏹ Emulator Closed:[/bold yellow] {name} (PID {pid})")
        console.print("  [green]Running Post-Play Save Sync...[/green]")
        if emu:
            logs = provider.sync_emulator(emu_id, [Path(p) for p in emu["paths"]], emu["extensions"])
            for l in logs:
                console.print(f"    {l}")

    monitor = ProcessMonitor(
        emulator_targets=targets,
        on_emulator_launched=on_launch,
        on_emulator_exited=on_exit,
        poll_interval=float(config.get("poll_interval_seconds", 3))
    )

    try:
        monitor.run_forever()
    except KeyboardInterrupt:
        console.print("\n[yellow]Daemon stopped by user.[/yellow]")


def cmd_add_path(config: ConfigManager, name: str, path: str):
    p = Path(path).expanduser().resolve()
    if not p.exists():
        console.print(f"[red]Error: Path does not exist: {p}[/red]")
        return
    config.add_custom_path(name, str(p))
    console.print(f"[green]✔ Added custom path: {name} -> {p}[/green]")


def cmd_set_folder(config: ConfigManager, path: str):
    p = Path(path).expanduser().resolve()
    p.mkdir(parents=True, exist_ok=True)
    config.set("local_sync_folder", str(p))
    config.set("sync_mode", "local_folder")
    console.print(f"[green]✔ Set sync folder to: {p}[/green]")


def main():
    parser = argparse.ArgumentParser(
        prog="syncmyshit",
        description="syncMyShit - Automagic Retro Game Cloud Save Sync (Linux, Windows, Android)"
    )
    subparsers = parser.add_subparsers(dest="command", help="Available commands")

    subparsers.add_parser("status", help="Show detected emulators and sync status")
    subparsers.add_parser("scan", help="Scan and list save files found in emulators")
    subparsers.add_parser("sync", help="Run save sync immediately once")
    subparsers.add_parser("watch", help="Run background watcher daemon for automagic sync")

    add_p = subparsers.add_parser("add-path", help="Add custom game/emulator save path")
    add_p.add_argument("name", help="Name of game or emulator")
    add_p.add_argument("path", help="Directory path containing saves")

    set_f = subparsers.add_parser("set-folder", help="Set target sync folder (Syncthing, Drive, etc.)")
    set_f.add_argument("path", help="Directory path to sync saves to")

    args = parser.parse_args()

    config = ConfigManager()
    engine = SyncEngine(keep_backups=int(config.get("keep_backups_count", 5)))

    if args.command == "status" or not args.command:
        cmd_status(config, engine)
    elif args.command == "scan":
        cmd_scan(config, engine)
    elif args.command == "sync":
        cmd_sync(config, engine)
    elif args.command == "watch":
        cmd_watch(config, engine)
    elif args.command == "add-path":
        cmd_add_path(config, args.name, args.path)
    elif args.command == "set-folder":
        cmd_set_folder(config, args.path)


if __name__ == "__main__":
    main()
