# Podkidnoy Poker Android APK

This branch is a self-contained Android client build based on the v1.4 game. It is isolated from main.

The Android APK is a first-party WebView client. The multiplayer game rules, rooms,
bid order (clockwise), joker trades, and scores are handled by the Node.js backend
from the separately supplied v1.4 ZIP project. Android does not bundle that backend.

After installing the debug APK, select "Сервер" and enter your multiplayer backend
HTTPS URL. Local HTTP is permitted only to LAN addresses in the debug build.

GitHub Actions: Build Podkidnoy Poker APK. Artifacts expire after 30 days.
