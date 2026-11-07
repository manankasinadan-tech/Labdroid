# LABDROID — Spécification Officielle des Extensions .labd
> **Version du standard :** 1.0.0  
> **Espace de travail Kitchen :** `/data/data/com.labdroid.kitchen/`  
> **Architecture cible native :** `arm64-v8a`

---

## 1. Philosophie & Principes Fondamentaux

Une extension `.labd` n'est pas un simple script d'automatisation. C'est un **sous-système autonome et portable** capable d'embarquer :
- Des binaires natifs compilés en C/C++/Rust pour `arm64-v8a`.
- Des scripts d'orchestration POSIX robustes (`sh`, `bash`).
- Des déclarations de métadonnées et d'actions directes dans l'interface utilisateur Material 3 de LABDROID.

### L'Illusion Native (Intégration Transparente)
Lorsqu'un paquet `.labd` est installé :
1. Il apparaît **instantanément dans la liste unifiée** de l'écran d'Accueil, avec la même rigueur visuelle que les modules natifs (FORGE, Kitchen Explorer, Native Tools).
2. Ses exécutables sont automatiquement centralisés dans `/data/data/com.labdroid.kitchen/bin/` et marqués comme exécutables (`chmod +x`).
3. Ses actions s'interfacent avec le **Sélecteur de Fichiers Popup M3** et la **Console de Logs temps réel**.

---

## 2. Structure Canonique d'une Archive .labd

Une extension `.labd` est une archive compressée standard au format **ZIP** renommée avec l'extension `.labd`.

```text
mon_extension.labd (Archive ZIP)
├── manifest.json                  # [OBLIGATOIRE] Déclaration du module
├── main.sh                        # [OBLIGATOIRE] Point d'entrée principal
├── bin/                           # [OPTIONNEL] Binaires natifs arm64-v8a
│   ├── mon_outil_binaire
│   └── helper_tool
├── res/                           # [OPTIONNEL] Ressources, icônes, configurations
│   ├── config.prop
│   └── partition_rules.xml
└── README.md                      # [RECOMMANDÉ] Documentation du développeur
```

---

## 3. Contrat du Manifeste (`manifest.json`)

Le fichier `manifest.json` à la racine de l'archive définit le comportement, les permissions et les actions de l'extension.

### Schéma JSON Complet

```json
{
  "$schema": "https://labdroid.dev/schemas/v1/labd-manifest.json",
  "id": "erofs_pro_tools",
  "name": "EROFS Pro Optimizer",
  "version": "1.2.0",
  "author": "ROM Master Lab",
  "description": "Moteur avancé de compression et manipulation d'images EROFS haute densité pour Android 14+.",
  "minApi": 24,
  "targetArch": "arm64-v8a",
  "entrypoint": "main.sh",
  "icon": "storage",
  "permissions": [
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"
  ],
  "binaries": [
    "mkfs.erofs",
    "erofsfuse"
  ],
  "actions": [
    {
      "id": "inspect_erofs",
      "label": "Analyser l'image EROFS",
      "command": "sh main.sh --inspect",
      "requiresRoot": false,
      "outputToConsole": true
    },
    {
      "id": "compress_ultra",
      "label": "Compression Ultra LZ4HC",
      "command": "sh main.sh --compress-lz4hc",
      "requiresRoot": true,
      "outputToConsole": true
    }
  ]
}
```

### Description des Champs

| Champ | Type | Obligatoire | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | **Oui** | Identifiant unique (minuscules, chiffres, tirets bas). Ex: `erofs_tools`. |
| `name` | `String` | **Oui** | Nom affiché sur la carte unifiée de l'accueil LABDROID. |
| `version` | `String` | **Oui** | Version sémantique (ex: `1.0.0`). |
| `author` | `String` | **Oui** | Nom ou équipe de l'auteur. |
| `description` | `String` | Non | Résumé fonctionnel du module. |
| `minApi` | `Integer`| Non | API Android minimale requise (par défaut: `24`). |
| `targetArch`| `String` | Non | Architecture matérielle supportée (recommandé: `arm64-v8a`). |
| `entrypoint`| `String` | **Oui** | Script initial exécuté lors du lancement (ex: `main.sh`). |
| `icon` | `String` | Non | Nom d'icône Material (`build`, `storage`, `extension`, `folder`). |
| `binaries` | `Array` | Non | Liste des exécutables dans `bin/` à déployer dans `/data/data/com.labdroid.kitchen/bin/`. |
| `actions` | `Array` | Non | Boutons d'action rapide dans l'interface de l'extension. |

---

## 4. Cycle de Vie & Déploiement

1. **Recherche & Sélection :** L'utilisateur clique sur le bouton `+` dans la TopAppBar de LABDROID. Le sélecteur de fichiers popup interne filtre les fichiers `.labd` dans `/sdcard/`.
2. **Décompression & Validation :** L'archive est vérifiée dans un dossier temporaire. Si `manifest.json` est valide, elle est déployée dans `/data/data/com.labdroid.kitchen/extensions/<id>/`.
3. **Déploiement des Binaires :** Les fichiers du répertoire `bin/` sont copiés vers `/data/data/com.labdroid.kitchen/bin/` et reçoivent les permissions `chmod 755`.
4. **Enregistrement UI :** L'extension est injectée dans le flux de la liste d'accueil sans redémarrage nécessaire.

---

## 5. Guide de Création Pas-à-Pas d'un .labd

### Étape 1 : Créer le dossier de développement
```bash
mkdir mon_extension
cd mon_extension
mkdir -p bin res
```

### Étape 2 : Écrire le script `main.sh`
```bash
#!/system/bin/sh
echo "=== Extension Mon Extension v1.0 ==="
echo "Dossier de travail : $(pwd)"
echo "Architecture hôte : $(uname -m)"
```
Rendre le script exécutable :
```bash
chmod +x main.sh
```

### Étape 3 : Créer le fichier `manifest.json`
Rédiger le fichier conformément au schéma de la section 3.

### Étape 4 : Compiler l'archive `.labd`
```bash
zip -r mon_extension.labd manifest.json main.sh bin res README.md
```

### Étape 5 : Installer dans LABDROID
Copier `mon_extension.labd` sur votre smartphone (`/sdcard/`) ou dans l'émulateur, ouvrir LABDROID et appuyer sur le bouton `+` en haut à droite.

---

## 6. Variables d'Environnement & Chemins Kitchen

Tous les scripts et binaires exécutés par LABDROID reçoivent un environnement standardisé :

```bash
KITCHEN_HOME="/data/data/com.labdroid.kitchen/home"
PROJECTS_DIR="/data/data/com.labdroid.kitchen/home/PROJET"
BIN_DIR="/data/data/com.labdroid.kitchen/bin"
OUTPUT_DIR="/sdcard/LABDROID/REPACKED"
PATH="$BIN_DIR:$PATH"
```

Les journaux écrits sur `stdout` et `stderr` sont capturés en direct par la console temps réel avec coloration syntaxique automatique selon les préfixes `[INFO]`, `[OK]`, `[WARN]`, `[ERR]`.
