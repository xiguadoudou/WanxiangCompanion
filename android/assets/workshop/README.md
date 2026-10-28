# Authorized local port

The repository owner explicitly reported authorization from the datawxq.com developer to copy and adapt these modules. Original component code, styles and game records are preserved in `site.zip`; upstream source hashes and attribution are recorded in its `port-manifest.json`. Existing bundled dependency license comments remain intact. This notice does not declare the website source to be MIT or grant additional third-party rights.

The app loads only the packaged code under Android's virtual app-asset origin. It never loads the original website HTML or remote executable scripts. Account login, cloud sharing, and guide creation are excluded. App-owned local plans, code import/export and PNG rendering replace server actions.

`index.html`, `port.css`, and `port.js` are maintained adapters. The ZIP also contains complete literal records in `library-data.json` and 268 core portraits/icons plus tier badges. `WorkshopData` refreshes data literals without executing downloaded JavaScript. Additional art is fetched through the shared image cache when available.

Reproduce: run `python3 tools/build_local_modules.py --json-jar <org.json.jar>` from android/. After editing adapters, use `python3 tools/repack_workshop.py`. The port ZIP is source material, not an archive of the private signing workspace.

0.1.18: app-owned mint/glass theme replaces the two website shell themes. Both shell navigation sets are hidden and inert; Vue router, links, History and Android module ownership checks restrict navigation to the active module. Queries remain available for drafts and library filters.
