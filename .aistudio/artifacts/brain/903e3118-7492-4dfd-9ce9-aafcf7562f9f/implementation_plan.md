# Plan de Correction et d'Intégration des Binaires Natifs FORGE

Résolution du problème d'extraction sur images réelles GSI (3.2 Go), intégration de la suite complète de binaires arm64 dans `/data/data/com.labdroid.kitchen/bin/`, et mise en place d'un pipeline d'extraction/reconstruction réel avec support root loop mount et outils de décompression.

## User Review & Critical Decisions

> [!IMPORTANT]
> Les décisions critiques suivantes ont été confirmées pour corriger l'extraction réelle :
> - **Mécanisme d'extraction prioritaire** : Intégration complète de la suite d'outils et binaires arm64 autonomes (`simg2img`, `e2fsdroid`, `fsck.erofs`, `make_ext4fs`, `magiskboot`) couplée au montage loop root direct (`mount -o loop,ro`).
> - **Pack d'outils complet** : Déploiement de tous les binaires indispensables dans `/data/data/com.labdroid.kitchen/bin/` dès le démarrage.
> - **Gestion rigoureuse des erreurs** : Interdiction absolue de marquer « Succès » sur une analyse inconnue ou une image non extraite. Interruption propre avec rapport d'erreur technique détaillé.

---

## 1. Diagnostic de l'Incident Signalé

1. **Erreur d'accès bas niveau sur /sdcard/** : Sur Android 11+ / 14+, les fichiers de plus de 2 Go situés sur le stockage externe (`/sdcard/`) peuvent échouer lors d'une lecture directe par `RandomAccessFile` à cause des restrictions Scoped Storage ou des permissions SELinux de l'espace sandbox. Le test renvoyait silencieusement `Corrupted/Unreadable` et `UNKNOWN`.
2. **Comportement de substitution (Mock fallback)** : Le moteur enchaînait sur une structure par défaut de 4 fichiers au lieu de lever une exception bloquante, créant une illusion de succès sans le vrai contenu du système GSI.
3. **Absence des exécutables arm64 dans /bin/** : Les utilitaires système de manipulation d'images (`simg2img`, `e2fsdroid`, `toybox`, `erofs-utils`) n'étaient pas encore déployés physiquement dans `/data/data/com.labdroid.kitchen/bin/`.

---

## 2. Architecture Technique & Pipeline d'Extraction Réel

```
┌────────────────────────────────────────────────────────────────────────┐
│                   PIPELINE RÉEL DE DÉPAQUETAGE FORGE                   │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
       1. Analyse physique robuste (Root dd / stat / readmagic)
          ├─ Sparse Android (0xED26FF3A)
          ├─ Linux ext4 (0xEF53)
          ├─ EROFS (0xE0F5E1E2)
          └─ Android Boot Image (ANDROID!)
                                    │
                                    ▼
       2. Conversion Android Sparse -> RAW
          └─ simg2img <source.img> temp/unsparse.raw
                                    │
                                    ▼
       3. Extraction réelle du filesystem dans rootfs/
          ├─ Méthode A (Root) : mount -o loop,ro unsparse.raw /mnt_tmp
          │                     toybox cp -a -H -d -p /mnt_tmp/. rootfs/
          │                     umount /mnt_tmp
          └─ Méthode B (Binaire) : fsck.erofs --extract=rootfs/ unsparse.raw
                                    ou e2fsdroid / 7z extract
                                    │
                                    ▼
       4. Extraction des vrais contextes SELinux & Permissions
          ├─ Génération du vrai fs_config (UID, GID, modes réels)
          └─ Génération du vrai file_contexts.bin (u:object_r:...)
                                    │
                                    ▼
       5. Validation stricte d'autonomie -> database/project_meta.json
```

---

## 3. Plan d'Implémentation Étape par Étape

### Étape 1 : Déploiement Automatisé des Binaires arm64
- Déploiement et extraction automatique des exécutables et wrappers vers `/data/data/com.labdroid.kitchen/bin/` :
  - `simg2img` & `img2simg` (Gestion des images Android Sparse).
  - `make_ext4fs` & `e2fsdroid` (Fabrication et injection de contextes ext4).
  - `erofs-utils` (`mkfs.erofs`, `fsck.erofs` pour extraction et repack EROFS).
  - `magiskboot` (Dépaquetage / repaquetage de `boot.img`, `init_boot.img`).
  - `toybox` (Utilitaires POSIX avec gestion des attributs étendus et liens symboliques).
- Attribution automatique des droits d'exécution `chmod 755` sur tous les fichiers de `/data/data/com.labdroid.kitchen/bin/`.

### Étape 2 : Réécriture de l'Analyseur d'Image (`analyzeImageHeader`)
- Remplacement du simple `RandomAccessFile` par une lecture bas-niveau résiliente :
  - Si le fichier est inaccessible via Java, appel direct via shell/root (`dd if="..." bs=4096 count=1`).
  - Détection exacte des signatures Sparse, ext4, EROFS, F2FS et Boot.
  - **Arrêt immédiat en cas d'échec** : journalisation en `[ERROR]`, affichage clair de l'erreur dans la console, et rejet de l'opération (plus de faux succès).

### Étape 3 : Pipeline d'Extraction Réel (UNPACK)
- Exécution de la conversion Sparse si nécessaire (`simg2img`).
- Montage loopback root avec point de montage isolé temporaire (`/data/data/com.labdroid.kitchen/mnt_temp`).
- Copie intégrale de l'arborescence réelle avec préservation des liens symboliques, permissions et contextes.
- Collecte des contextes SELinux réels et génération des bases dans `database/`.
- Nettoyage rigoureux des fichiers temporaires après extraction.

### Étape 4 : Pipeline de Reconstruction Réel (REPACK)
- Validation des métadonnées `database/project_meta.json`.
- Calcul de la taille de bloc et d'inodes requise pour `rootfs/`.
- Recompilation de l'image partition conforme (RAW ou Sparse) dans `/sdcard/LABDROID/REPACKED/`.
- Vérification de l'intégrité de l'image finale par somme de contrôle SHA-256.

### Étape 5 : Interface & Moniteur de Binaires
- Ajout d'une section « Diagnostic des Binaires » dans l'onglet Outils/Paramètres listant l'état (`Installé / Prêt`) de chaque utilitaire binaire dans `/data/data/com.labdroid.kitchen/bin/`.
