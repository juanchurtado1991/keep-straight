# Firma de código — KeepStraight desktop (CI)

Esta guía explica cómo obtener certificados, convertirlos a **GitHub Actions secrets** y qué hace el workflow `Package desktop`.

Sin secrets configurados, CI sigue publicando instaladores **sin firmar** (SmartScreen / Gatekeeper seguirán avisando).

---

## Resumen de secrets

| Secret | Plataforma | Qué es |
|--------|------------|--------|
| `APPLE_CERT_P12` | macOS | Certificado **Developer ID Application** exportado como `.p12`, codificado en Base64 |
| `APPLE_CERT_PASSWORD` | macOS | Contraseña del `.p12` |
| `MACOS_SIGNING_IDENTITY` | macOS | Nombre del certificado (ver abajo) |
| `APPLE_ID` | macOS | Email de tu Apple ID de desarrollador |
| `APPLE_APP_SPECIFIC_PASSWORD` | macOS | Contraseña específica de app ([appleid.apple.com](https://appleid.apple.com)) |
| `APPLE_TEAM_ID` | macOS | Team ID de 10 caracteres (ej. `AB12CD34EF`) |
| `WINDOWS_CERT_PFX` | Windows | Certificado Authenticode `.pfx`, codificado en Base64 |
| `WINDOWS_CERT_PASSWORD` | Windows | Contraseña del `.pfx` |

Opcional: si solo quieres firmar sin notarizar aún, basta con los secrets de macOS de certificado + `MACOS_SIGNING_IDENTITY` (sin `APPLE_ID` / password / team).

---

## Dónde subirlos en GitHub

1. Abre tu repo en GitHub → **Settings**
2. **Secrets and variables** → **Actions**
3. **New repository secret** (uno por fila de la tabla)
4. Pega el valor **sin comillas** ni saltos de línea extra

Ruta directa: `https://github.com/TU_USUARIO/keep-straight/settings/secrets/actions`

> Los secrets son solo lectura en workflows; no se muestran en logs si el workflow está bien escrito.

---

## macOS — certificado Developer ID

### 1. Crear el certificado

1. Cuenta [Apple Developer Program](https://developer.apple.com/programs/) (99 USD/año)
2. [Certificates → +](https://developer.apple.com/account/resources/certificates/add) → **Developer ID Application**
3. Sube un CSR desde **Keychain Access → Certificate Assistant → Request a Certificate…**
4. Descarga e instala el certificado (doble clic)

### 2. Exportar `.p12`

1. Keychain Access → **login** → **My Certificates**
2. Expande **Developer ID Application: …**
3. Clic derecho → **Export** → formato **Personal Information Exchange (.p12)**
4. Elige una contraseña → será `APPLE_CERT_PASSWORD`

### 3. Codificar en Base64

**En Mac:**

```bash
base64 -i DeveloperID.p12 | pbcopy
# pega el resultado en el secret APPLE_CERT_P12
```

**En Linux:**

```bash
base64 -w 0 DeveloperID.p12 > apple-cert.b64
# copia el contenido de apple-cert.b64 al secret
```

### 4. `MACOS_SIGNING_IDENTITY`

Compose busca el certificado por **nombre común**, no por Team ID solo.

En terminal:

```bash
security find-identity -v -p codesigning
```

Usa la parte entre comillas, por ejemplo:

```text
Developer ID Application: Juan Churtado (AB12CD34EF)
```

Ese string completo va en `MACOS_SIGNING_IDENTITY`.

### 5. Notarización Apple

1. [appleid.apple.com](https://appleid.apple.com) → **Sign-In and Security** → **App-Specific Passwords** → genera una
2. Esa contraseña → secret `APPLE_APP_SPECIFIC_PASSWORD`
3. `APPLE_ID` = tu email de Apple ID
4. `APPLE_TEAM_ID` = Membership → Team ID, o:

```bash
xcrun altool --list-providers -u "tu@email.com" -p "app-specific-password"
```

### 6. App ID / bundle

El bundle ya está fijado a `com.keepstraight.desktop`. Debe existir un **App ID** con ese identificador en [Identifiers](https://developer.apple.com/account/resources/identifiers/list).

---

## Windows — Authenticode

### 1. Obtener certificado

Opciones habituales:

- **EV Code Signing** (mejor para SmartScreen desde el día 1)
- **Standard Code Signing** de DigiCert, Sectigo, SSL.com, etc.
- Certificado de prueba interno (solo QA; SmartScreen seguirá avisando)

El emisor te entrega un `.pfx` o un `.p12` usable en Windows.

### 2. Codificar `.pfx` en Base64

**PowerShell (Windows):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\codesign.pfx")) | Set-Clipboard
```

**Linux / Mac:**

```bash
base64 -w 0 codesign.pfx > windows-cert.b64
```

Pega en `WINDOWS_CERT_PFX`. La contraseña del PFX → `WINDOWS_CERT_PASSWORD`.

---

## Qué hace CI cuando hay secrets

| OS | Sin secrets | Con secrets |
|----|-------------|-------------|
| **macOS** | `packageReleaseDmg` sin firmar | Importa `.p12` → `notarizeReleaseDmg` (firma + notarización + staple) |
| **Windows** | EXE/MSI sin firmar | Tras empaquetar, `sign-windows-artifacts.ps1` con `signtool` + timestamp DigiCert |
| **Linux** | `.deb` sin cambios | Sin firma (normal en Linux) |

Workflow: `.github/workflows/package-desktop.yml`

---

## Probar en local (macOS)

Con certificado ya en el llavero:

```bash
./gradlew :desktopApp:notarizeReleaseDmg \
  -Pkeepstraight.skipApkSync=true \
  -Pcompose.desktop.mac.sign=true \
  -Pcompose.desktop.mac.signing.identity="Developer ID Application: TU NOMBRE (TEAMID)" \
  -Pcompose.desktop.mac.notarization.appleID="tu@email.com" \
  -Pcompose.desktop.mac.notarization.password="xxxx-xxxx-xxxx-xxxx" \
  -Pcompose.desktop.mac.notarization.teamID="AB12CD34EF"
```

## Probar en local (Windows)

```powershell
.\desktopApp\scripts\sign-windows-artifacts.ps1 `
  -DistDir desktopApp\build\compose\binaries\main-release\exe `
  -PfxPath C:\path\cert.pfx `
  -PfxPassword "tu-password"
```

---

## Checklist rápido

- [ ] 8 secrets creados en GitHub Actions
- [ ] App ID `com.keepstraight.desktop` en Apple Developer
- [ ] Push a `main` o **Actions → Package desktop → Run workflow**
- [ ] Release `ci-latest` con EXE/MSI/DMG firmados
- [ ] Probar instalación en una máquina limpia (Mac: sin “Open” forzado; Windows: sin SmartScreen bloqueante)

---

## Problemas frecuentes

| Síntoma | Causa probable |
|---------|----------------|
| `Could not find certificate` | `MACOS_SIGNING_IDENTITY` no coincide con Keychain / cert expirado |
| Notarization Invalid | Binarios sin firmar dentro del `.app`; revisa entitlements en `desktopApp/macos/` |
| `signtool` not found | Runner Windows sin Windows SDK (el workflow instala WiX; SDK suele venir preinstalado) |
| Secret “vacío” en CI | El paso detecta secret ausente y **omite** firma (build no falla) |

Para rotar un certificado: exporta el nuevo `.p12`/`.pfx`, actualiza el secret y vuelve a lanzar el workflow.
