# Licensing boundary

New original source in `android/` and `docs/android/` is GPL-3.0-or-later; full terms are `android/COPYING`. The official Gradle wrapper JAR is Apache-2.0 licensed (Gradle project); its accompanying license is in `android/gradle/wrapper/LICENSE`.

This is **not** a relicensing of the inherited repository. At upstream commit `ee6a57d971c207c933a424c0d705044a02985b0a` there is no top-level LICENSE, and GitHub reports license=null. `PKGBUILD:10-12` identifies OWL and its patches as GPL-3.0-or-later, radiotap as ISC, and omdrop-awdl tools as GPL-2.0-only, but it does not unambiguously grant a license for every original helper in this repository. Those files remain unchanged and their rights remain with their authors. Permission clarification is required before packaging those ambiguous helpers.

Pinned OWL `832d70f815c3d4a06a02117bf0fc5e868daa1ff0` has GPL-3.0 in COPYING and GPL-3.0-or-later headers in daemon/io.c and src/frame.c. Pinned omdrop-awdl `534f91a525337951ca19c311e53d333bd6d55100` declares GPL-2.0-only in LICENSE. No OWL, radiotap, omdrop-awdl, or inherited shell helper code is copied into, linked into, or packaged with this APK. The adapter seam and probe are original code. No GPL2-only/GPL3 combined binary is distributed.

No contributor/coauthor list was supplied by the runtime. Commit author is the explicitly authorized human GitHub account jduartedj, whose name/id were verified through the authenticated GitHub API. No bot or invented coauthor attribution is added.
