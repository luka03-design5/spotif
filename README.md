# Spotify Lyrics (Fabric, Minecraft 1.21.1)

## JAR bauen
Variante A – lokal (JDK 21 + Gradle 8.10+ installiert):
    gradle build
Die Mod liegt danach in build/libs/spotify-lyrics-1.0.0.jar

Variante B – ohne lokale Installation:
Projekt in ein neues GitHub-Repo pushen. Unter "Actions" läuft "build",
dort das Artifact "spotify-lyrics-jar" herunterladen.

## Installieren
JAR (nicht die -sources.jar) nach .minecraft/mods/ kopieren.
Benötigt: Fabric Loader + Fabric API für Minecraft 1.21.1.

## Spotify einrichten
1. developer.spotify.com/dashboard -> App erstellen, Web API aktivieren
2. Redirect URI: http://127.0.0.1:8888/callback
3. Im Spiel: /lyrics login <clientId>

Befehle: /lyrics toggle, /lyrics offset <ms>
