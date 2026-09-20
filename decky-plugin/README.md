# syncMyShit — Decky Plugin v2.1

Cloud save sync for Steam Deck Gaming Mode.

## Sign in (Desktop Mode — once)

1. Switch to **Desktop Mode** → open **Konsole**
2. Run:
   ```bash
   ~/homebrew/plugins/syncMyShit/login-desktop.sh
   ```
3. **First run only:** create a Google Cloud OAuth client of type **Desktop app**
   (Credentials → Create → Desktop app), paste the Client ID when asked.
   Do **not** reuse the Android client ID — Google returns `invalid_request`.
4. Finish Google sign-in in the browser
5. Return to **Gaming Mode** → Decky → syncMyShit

## Sync (Game Mode)

Press `...` → Decky → syncMyShit → **Sync All Saves**

## Install / update

```bash
curl -sSL https://raw.githubusercontent.com/Kyss007/syncMyShit/main/update-decky.sh | bash
```
