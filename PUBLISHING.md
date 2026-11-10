# Publishing Telos Utils to Modrinth

Everything you need is in the `release/` folder:

- `release/telos-utils-1.1.0.jar`: the file you upload
- `release/icon.png`: the project icon

## 0. Before you start

- [ ] Check that Telos Realms allows a client-side DPS meter. Look at their rules or ask in their Discord. The mod only reads what's on your screen and sends nothing to the server.
- [ ] Take 2–3 screenshots in-game for the gallery:
  - the HUD during a boss fight (with Boss HP showing)
  - the settings menu (press **O**)
  - optional: the HUD editor

  Press **F2** in Minecraft. Screenshots are saved in the profile's `screenshots` folder.

## 1. Create the project

1. Go to <https://modrinth.com> and sign in (or create an account).
2. Click the **+** button at the top right, then **New project**.
3. Fill in:

| Field | Value |
|---|---|
| Name | `Telos Utils` |
| URL | `telos-utils` (or anything free) |
| Visibility | Public |
| Summary | `Client-side utilities for Telos Realms: live DPS meter and boss HP display.` |

4. Click **Create project**.

## 2. Set up the project page

On the project's settings pages:

**General**
- Icon: upload `release/icon.png`
- Environment: **Client-side only**. (If the form asks separately, set Client: Required, Server: Unsupported.)

**Description**
- Paste the whole contents of `README.md`.

**Tags**
- Categories: **Utility**, **Game Mechanics**

**License**
- Choose **MIT License**.

**Links (optional)**
- Source code: your GitHub repo, if you upload the code there. Recommended, because people trust open-source mods more.
- Issues: the repo's issues page.

**Gallery**
- Upload your screenshots, and mark the best one as **Featured**.

## 3. Upload the first version

Open the **Versions** tab and click **Create a version**:

| Field | Value |
|---|---|
| File | `release/telos-utils-1.1.0.jar` |
| Version name | `1.1.0` |
| Version number | `1.1.0` |
| Release channel | Release (or **Beta** if you want to test with others first) |
| Loaders | Fabric |
| Game versions | 26.1.2 |
| Dependencies | **Fabric API**: Required |
| Changelog | Paste the newest section from `CHANGELOG.md` |

Click **Create**.

## 4. Submit for review

Go back to the project page and click **Submit for review**. Modrinth's moderators check new projects, usually within a few days. The most common reasons a project gets sent back:

- The description is too short or unclear (the README covers this).
- Environment or license info is missing.

You'll get a notification when it's approved or if they ask for changes.

## Releasing an update later

1. Change the code, then raise `mod_version` in `gradle.properties` (for example `1.0.1`).
2. Add a new section at the top of `CHANGELOG.md`.
3. Build: run `gradlew build` (or the Gradle **build** task in IntelliJ). The jar appears in `build/libs/`.
4. On Modrinth, go to **Versions**, click **Create a version**, upload the new jar and paste the changelog.
