# Quick Start Guide - Mobile App

## Prerequisites

- Node.js 18+
- Expo Go app on your phone (or iOS Simulator/Android Emulator)
- Backend running at `http://localhost:8080`

## Installation (2 minutes)

```bash
cd mobile-app
./setup.sh
```

Or manually:

```bash
npm install
cp .env.example .env
```

## Configuration

Edit `.env` and set API_URL:

```bash
# iOS Simulator
API_URL=http://localhost:8080/api/v1

# Android Emulator
API_URL=http://10.0.2.2:8080/api/v1

# Physical Device (same WiFi)
API_URL=http://192.168.1.x:8080/api/v1
```

## Run

```bash
npm start
```

- **iOS:** Press `i` or scan QR code with Camera app
- **Android:** Press `a` or scan QR code with Expo Go app

## Quick Test

1. **Register** a new account (test@example.com)
2. Should show "Setup complete!" (navigation pending)
3. Check Expo logs for successful API calls

## Current Status

✅ **Working:**
- Authentication (login/register)
- API integration
- Upload service logic
- All core infrastructure

🚧 **Pending (Templates in README):**
- UI components (PhotoPicker, UploadScreen, GalleryScreen)
- Navigation setup
- Photo gallery

## Next Steps

See `README.md` for:
- Complete implementation templates
- Component examples
- Navigation setup
- Testing guide

## Troubleshooting

**Cannot connect to backend:**
- iOS Simulator: Use `localhost`
- Android Emulator: Use `10.0.2.2`
- Physical Device: Use computer's local IP, ensure same WiFi

**Expo errors:**
```bash
rm -rf node_modules
npm install
npx expo start --clear
```

## Documentation

- **README.md** - Full implementation guide
- **MOBILE_COMPLETE.md** - Architecture summary
- **../backend/README.md** - Backend setup
- **../FRONTEND_BACKEND_INTEGRATION.md** - API integration

---

**Implementation Time:** 90% complete, ~4-6 hours remaining for UI components
