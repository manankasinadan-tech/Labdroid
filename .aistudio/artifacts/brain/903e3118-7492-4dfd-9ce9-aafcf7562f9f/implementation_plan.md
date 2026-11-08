# Plan de Correction : Crash au REPACK & Persistance du Thème

## Problèmes Identifiés

1. **Fermeture forcée de l'application (Crash / ANR) lors du REPACK** :
   - Dans `ForgeEngine.repackProject`, la méthode utilisait `rawRepackFile.copyTo(outputFile, overwrite = true)` en Java direct sur un fichier de partition de plus de 3 Go. Sur Android, `copyTo` alloue des tampons mémoire en espace utilisateur JVM, provoquant des erreurs `OutOfMemoryError` ou des `SecurityException / EACCES` sur `/sdcard/` en raison de Scoped Storage.
   - Le calcul de l'empreinte SHA-256 par `FileInputStream` Java avec un buffer de 8 Ko bloquait le thread pendant des dizaines de secondes, déclenchant l'alerte « Forcer la fermeture / L'application ne répond pas » (ANR).
   - Absence de bloc `try { ... } catch (t: Throwable)` global autour de l'opération de REPACK, ce qui propageait toute exception non capturée jusqu'au crash brutal du processus Android.
   - Les scripts wrappers dans `BinaryManager.kt` (`make_ext4fs`, `img2simg`) ne géraient pas les arguments d'options (`-s`, `-S`, `-C`, `-l`), entraînant des paramètres erronés.

2. **Perte du thème sélectionné au redémarrage de l'application** :
   - Dans `MainActivity.kt`, l'état `themeMode` était stocké uniquement en mémoire volatile Jetpack Compose (`remember { mutableStateOf(AppThemeMode.SYSTEM) }`).
   - À chaque fermeture et réouverture de l'application, l'état revenait systématiquement à `SYSTEM`.

---

## Solutions & Modifications Prévues

### 1. Persistance Réelle du Thème (`ThemePreferences`)
- Création d'un module persistant `ThemePreferences` s'appuyant sur les `SharedPreferences` Android (`labdroid_settings`).
- Chargement synchrone du thème sauvegardé au démarrage dans `MainActivity.onCreate()`.
- Enregistrement immédiat dans `SharedPreferences` à chaque modification depuis `SettingsScreen`.

### 2. Sécurisation et Optimisation Kernel du REPACK (`ForgeEngine`)
- **Isolation d'exception intégrale** : Englober toute l'opération `repackProject` dans un bloc `try / catch (t: Throwable)` robuste. En cas d'erreur ou d'espace disque insuffisant, journalisation immédiate en `[ERROR]` sur la `ForgeConsole` et restitution propre d'un `Result.failure`, sans jamais crasher l'application.
- **Copie au niveau Kernel via Root** : Remplacer `copyTo` JVM par `RootService.executeCommand("mkdir -p ... && cp ... && chmod 664 ...", useRoot = true)`. La copie s'effectue directement en espace noyau sans consommer la mémoire RAM du processus et sans restriction Scoped Storage.
- **Calcul SHA-256 Natif Ultra-Rapide** : Utilisation prioritaire de `toybox sha256sum` ou `sha256sum` via shell root (durée : < 2s sur 3 Go) avec repli Java bufferisé par blocs de 1 Mo en arrière-plan.
- **Résilience des Wrappers de Binaires** : Amélioration des scripts dans `BinaryManager` pour extraire automatiquement les fichiers cibles et répertoires sources même en présence d'options (`-s`, `-S`, `-C`, `-l`, `-a`).
- **Protection de l'UI** : Ajout d'une capture `try / catch` dans le scope coroutine de `ForgeScreen.kt`.

### 3. Validation
- Compilation complète avec `compile_applet`.
- Exécution des tests unitaires Robolectric avec `gradle :app:testDebugUnitTest`.
