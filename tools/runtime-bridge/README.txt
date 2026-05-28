GuideNH runtime bridge verification tools

Files in this directory help verify the real GuideNH runtime bridge against a live `runClient25` client.

Typical flow:
1. Copy `runtime-bridge-config.sample.json` to a local JSON file and adjust values if needed.
2. Run `powershell -ExecutionPolicy Bypass -File .\tools\runtime-bridge\verify-runtime-bridge.ps1`.
3. The script updates `run/client_new/config/guidenh/guidenh.cfg`, launches or reuses `runClient25`, waits for the bridge, runs raw protocol checks, compiles `guide-vsc`, and runs its live runtime verification.

Notes:
- These scripts are UTF-8 files.
- The JSON config file is intended for local use and does not need to be committed.
- The raw protocol verification checks handshake, capabilities, document validation, and several semantic queries.
- The `guide-vsc` verification reuses its real runtime client and provider logic to validate live completions and hovers against the game bridge.
