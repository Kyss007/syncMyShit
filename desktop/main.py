#!/usr/bin/env python3
"""
syncMyShit - Desktop Save Sync Client (Linux & Windows)
Automagic retro emulator cloud save sync across Android, Linux, and Windows.
Dedicated to the Public Domain (The Unlicense)
"""

import argparse
import os
import sys
import time
import webbrowser
from pathlib import Path

from config import ConfigManager
from drive_sync import GoogleOAuthManager, GoogleDriveSyncProvider
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
            for tag in ["[cyan]", "[/cyan]", "[green]", "[/green]", "[yellow]", "[/yellow]", "[red]", "[/red]", "[bold]", "[/bold]", "[dim]", "[/dim]"]:
                text = text.replace(tag, "")
            print(text)
    console = DummyConsole()


def has_display() -> bool:
    """Returns True if a graphical desktop display environment is active."""
    if sys.platform == "win32":
        return True
    if sys.platform == "darwin":
        return True
    return bool(os.environ.get("DISPLAY") or os.environ.get("WAYLAND_DISPLAY"))


def cmd_status(config: ConfigManager, engine: SyncEngine):
    oauth_mgr = GoogleOAuthManager(config)
    console.print("\n[bold cyan]🎮 syncMyShit Desktop Status (v1.0.15)[/bold cyan]")
    detected = detect_installed_emulators()
    custom_paths = config.get("custom_paths", [])

    console.print(f"Config Directory: [cyan]{config.config_dir}[/cyan]")
    if oauth_mgr.is_authenticated():
        email = oauth_mgr.get_user_email()
        console.print(f"Google Drive: [bold green]Connected[/bold green] (Account: [cyan]{email}[/cyan], Folder: [cyan]syncMyShit/[/cyan])")
    else:
        console.print("Google Drive: [bold red]Not Connected[/bold red] - Run '[cyan]syncmyshit login[/cyan]' to connect your Google account.")

    console.print(f"\n[bold]Detected Emulators ({len(detected)} found):[/bold]")
    if not detected:
        console.print("[yellow]No standard emulators automatically detected yet. Use 'add-path' to add custom save folders.[/yellow]")
    else:
        for emu in detected:
            console.print(f"  • [bold green]{emu['name']}[/bold green] ({emu['category']}) -> Cloud: [cyan]{emu.get('drive_folder', emu['id'])}[/cyan]")
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
            console.print(f"\n[bold green]{emu['name']}[/bold green] ({len(files)} saves found):")
            for f in files[:5]:
                sz_kb = f["size"] / 1024.0
                console.print(f"  ├── {f['relative']} [dim]({sz_kb:.1f} KB)[/dim]")
            if len(files) > 5:
                console.print(f"  └── [dim]... and {len(files) - 5} more files[/dim]")
            total_files += len(files)

    for cp in config.get("custom_paths", []):
        p = Path(cp["path"])
        if p.exists():
            files = engine.scan_directory(p, [])
            if files:
                console.print(f"\n[bold green]{cp['name']}[/bold green] ({len(files)} saves):")
                for f in files:
                    sz_kb = f["size"] / 1024.0
                    console.print(f"  ├── {f['relative']} [dim]({sz_kb:.1f} KB)[/dim]")
                total_files += len(files)

    console.print(f"\n[bold]Total Save Files Found:[/bold] [cyan]{total_files}[/cyan]\n")


def cmd_login(config: ConfigManager):
    oauth_mgr = GoogleOAuthManager(config)
    console.print("\n[bold cyan]🔑 Google Drive Sign-In[/bold cyan]")
    try:
        auth_url = oauth_mgr.start_auth_flow()
        console.print("Opening browser for Google authorization...")
        console.print(f"If browser does not open automatically, visit:\n[cyan]{auth_url}[/cyan]\n")
        try:
            webbrowser.open(auth_url)
        except Exception:
            pass

        console.print("[dim]Waiting for browser authorization... (Or paste the authorization code below)[/dim]")
        # Give user option to wait or paste code
        for _ in range(30):
            time.sleep(2)
            if oauth_mgr.is_authenticated():
                console.print(f"[bold green]✔ Successfully connected to Google Drive as {oauth_mgr.get_user_email()}![/bold green]\n")
                return

        code_input = input("\nPaste authorization code or callback URL (or press Enter to cancel): ").strip()
        if code_input:
            if "code=" in code_input:
                import urllib.parse
                parsed = urllib.parse.urlparse(code_input)
                qs = urllib.parse.parse_qs(parsed.query)
                code_input = qs.get("code", [code_input])[0]
            tokens = oauth_mgr.exchange_code(code_input)
            console.print(f"[bold green]✔ Successfully connected to Google Drive as {tokens.get('email', 'User')}![/bold green]\n")
    except Exception as e:
        console.print(f"[bold red]Login failed:[/bold red] {e}\n")


def cmd_logout(config: ConfigManager):
    oauth_mgr = GoogleOAuthManager(config)
    oauth_mgr.sign_out()
    console.print("[green]✔ Disconnected and signed out of Google Drive.[/green]\n")


def cmd_sync(config: ConfigManager, engine: SyncEngine):
    oauth_mgr = GoogleOAuthManager(config)
    if not oauth_mgr.is_authenticated():
        console.print("[bold red]Error:[/bold red] Not connected to Google Drive. Run '[cyan]syncmyshit login[/cyan]' first.")
        return

    console.print(f"\n[bold cyan]⚡ Running Google Drive Save Sync ({oauth_mgr.get_user_email()})...[/bold cyan]")
    provider = GoogleDriveSyncProvider(oauth_mgr, engine)
    total_ops = 0

    detected = detect_installed_emulators()
    for emu in detected:
        paths = [Path(p) for p in emu["paths"]]
        try:
            logs = provider.sync_emulator(
                emu["id"],
                paths,
                emu["extensions"],
                drive_folder=emu.get("drive_folder")
            )
            if logs:
                console.print(f"[bold]{emu['name']}:[/bold]")
                for log in logs:
                    console.print(f"  {log}")
                    total_ops += 1
        except Exception as e:
            console.print(f"[red]Error syncing {emu['name']}:[/red] {e}")

    for cp in config.get("custom_paths", []):
        p = Path(cp["path"])
        if p.exists():
            try:
                logs = provider.sync_emulator(
                    cp["name"].lower().replace(" ", "_"),
                    [p],
                    [],
                    drive_folder=cp["name"]
                )
                if logs:
                    console.print(f"[bold]{cp['name']}:[/bold]")
                    for log in logs:
                        console.print(f"  {log}")
                        total_ops += 1
            except Exception as e:
                console.print(f"[red]Error syncing {cp['name']}:[/red] {e}")

    if total_ops == 0:
        console.print("[green]✔ All saves are already up-to-date with Google Drive![/green]\n")
    else:
        console.print(f"[green]✔ Sync complete! ({total_ops} files updated on Google Drive)[/green]\n")


def cmd_watch(config: ConfigManager, engine: SyncEngine):
    oauth_mgr = GoogleOAuthManager(config)
    if not oauth_mgr.is_authenticated():
        console.print("[bold red]Error:[/bold red] Not connected to Google Drive. Run '[cyan]syncmyshit login[/cyan]' first.")
        return

    console.print(f"\n[bold cyan]👁 syncMyShit Automagic Daemon Running ({oauth_mgr.get_user_email()})[/bold cyan]")
    console.print("[dim]Monitoring running emulator processes in background... Press Ctrl+C to stop.[/dim]\n")

    detected = detect_installed_emulators()
    targets = {}
    for emu in detected:
        targets[emu["id"]] = emu["process_names"]

    provider = GoogleDriveSyncProvider(oauth_mgr, engine)
    emu_map = {e["id"]: e for e in detected}

    def on_launch(emu_id: str, pid: str):
        emu = emu_map.get(emu_id)
        name = emu["name"] if emu else emu_id
        console.print(f"\n[bold green]▶ Emulator Started:[/bold green] {name} (PID {pid})")
        console.print("  [cyan]Running Pre-Play Google Drive Check...[/cyan]")
        if emu:
            try:
                logs = provider.sync_emulator(
                    emu_id,
                    [Path(p) for p in emu["paths"]],
                    emu["extensions"],
                    drive_folder=emu.get("drive_folder")
                )
                for l in logs:
                    console.print(f"    {l}")
            except Exception as e:
                console.print(f"    [red]Error:[/red] {e}")

    def on_exit(emu_id: str, pid: str):
        emu = emu_map.get(emu_id)
        name = emu["name"] if emu else emu_id
        console.print(f"\n[bold yellow]⏹ Emulator Closed:[/bold yellow] {name} (PID {pid})")
        console.print("  [green]Running Post-Play Save Sync to Google Drive...[/green]")
        if emu:
            try:
                logs = provider.sync_emulator(
                    emu_id,
                    [Path(p) for p in emu["paths"]],
                    emu["extensions"],
                    drive_folder=emu.get("drive_folder")
                )
                for l in logs:
                    console.print(f"    {l}")
            except Exception as e:
                console.print(f"    [red]Error:[/red] {e}")

    monitor = ProcessMonitor(
        emulator_targets=targets,
        on_emulator_launched=on_launch,
        on_emulator_exited=on_exit,
        poll_interval=float(config.get("poll_interval_seconds", 3))
    )

    try:
        monitor.run_forever()
    except KeyboardInterrupt:
        console.print("\n[yellow]Stopping daemon watcher...[/yellow]")
        monitor.stop()


def cmd_add_path(config: ConfigManager, name: str, path: str):
    p = Path(path).resolve()
    if not p.exists():
        console.print(f"[bold red]Error:[/bold red] Path does not exist: {p}")
        return
    config.add_custom_path(name, str(p))
    console.print(f"[green]✔ Added custom save path:[/green] {name} -> {p}")


def run_interactive_cli(config: ConfigManager, engine: SyncEngine):
    oauth_mgr = GoogleOAuthManager(config)
    while True:
        email = oauth_mgr.get_user_email() if oauth_mgr.is_authenticated() else "Not Connected"
        console.print("\n[bold cyan]🎮 syncMyShit - Automagic Cloud Save Sync[/bold cyan]")
        console.print(f"[dim]Google Drive Account: {email}[/dim]")
        print("1. ⚡ Sync Saves Now (Google Drive)")
        print("2. 🔍 Scan Emulator Save Files")
        print("3. 👁 Run Auto-Sync Background Watcher")
        print("4. 🔑 Sign In to Google Drive")
        print("5. 🚪 Sign Out of Google Drive")
        print("6. ➕ Add Custom Save Path")
        print("7. 🖥 Open Graphical Interface (GUI)")
        print("0. ❌ Exit")

        try:
            choice = input("Select an option (0-7): ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nGoodbye!")
            break

        if choice == "1":
            cmd_sync(config, engine)
        elif choice == "2":
            cmd_scan(config, engine)
        elif choice == "3":
            cmd_watch(config, engine)
        elif choice == "4":
            cmd_login(config)
        elif choice == "5":
            cmd_logout(config)
        elif choice == "6":
            name = input("Enter game/emulator name: ").strip()
            path_str = input("Enter save directory path: ").strip()
            if name and path_str:
                cmd_add_path(config, name, path_str)
        elif choice == "7":
            if has_display():
                from gui import run_gui
                run_gui()
                break
            else:
                print("Graphical display is not available in this terminal environment.")
        elif choice == "0":
            print("Goodbye!")
            break
        else:
            print("Invalid option. Please choose 0 to 7.")


def main():
    parser = argparse.ArgumentParser(
        prog="syncmyshit",
        description="syncMyShit - Automagic Retro Game Google Drive Save Sync (Linux, Windows, Android)"
    )
    parser.add_argument("--gui", action="store_true", help="Force open graphical user interface (GUI)")
    parser.add_argument("--cli", action="store_true", help="Force interactive terminal mode")

    subparsers = parser.add_subparsers(dest="command", help="Available commands")
    subparsers.add_parser("gui", help="Open graphical user interface")
    subparsers.add_parser("status", help="Show detected emulators and Google Drive status")
    subparsers.add_parser("login", help="Authenticate with Google Drive via browser")
    subparsers.add_parser("logout", help="Sign out from Google Drive")
    subparsers.add_parser("scan", help="Scan and list save files found in emulators")
    subparsers.add_parser("sync", help="Run save sync with Google Drive immediately")
    subparsers.add_parser("watch", help="Run background watcher daemon for automagic sync")

    add_p = subparsers.add_parser("add-path", help="Add custom game/emulator save path")
    add_p.add_argument("name", help="Name of game or emulator")
    add_p.add_argument("path", help="Directory path containing saves")

    args = parser.parse_args()

    config = ConfigManager()
    engine = SyncEngine(keep_backups=int(config.get("keep_backups_count", 5)))

    if args.command == "status":
        cmd_status(config, engine)
    elif args.command == "login":
        cmd_login(config)
    elif args.command == "logout":
        cmd_logout(config)
    elif args.command == "scan":
        cmd_scan(config, engine)
    elif args.command == "sync":
        cmd_sync(config, engine)
    elif args.command == "watch":
        cmd_watch(config, engine)
    elif args.command == "add-path":
        cmd_add_path(config, args.name, args.path)
    elif args.command == "gui" or args.gui:
        from gui import run_gui
        run_gui()
    elif args.cli:
        run_interactive_cli(config, engine)
    else:
        if has_display():
            try:
                from gui import run_gui
                run_gui()
            except Exception as e:
                print(f"Notice: Failed to launch GUI ({e}). Falling back to interactive CLI.")
                run_interactive_cli(config, engine)
        else:
            run_interactive_cli(config, engine)


if __name__ == "__main__":
    main()
