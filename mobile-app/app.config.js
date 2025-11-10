export default {
  expo: {
    name: "RapidPhotoUpload",
    slug: "rapid-photo-upload",
    version: "1.0.0",
    orientation: "portrait",
    userInterfaceStyle: "light",
    icon: "./assets/icon.png",
    splash: {
      image: "./assets/splash.png",
      resizeMode: "contain",
      backgroundColor: "#ffffff"
    },
    extra: {
      apiUrl: process.env.API_BASE_URL || "http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1",
      wsUrl: process.env.WS_URL || "ws://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/ws",
      eas: {
        projectId: "e89de57f-302a-48f7-88a1-1a211028f07f"
      }
    },
    ios: {
      supportsTablet: true,
      bundleIdentifier: "com.rapidphotoupload.app",
      buildNumber: "5",
      infoPlist: {
        NSCameraUsageDescription: "This app needs camera access to take photos for upload.",
        NSPhotoLibraryUsageDescription: "This app needs photo library access to select photos for upload.",
        NSAppTransportSecurity: {
          NSAllowsArbitraryLoads: false,
          NSExceptionDomains: {
            "rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com": {
              NSTemporaryExceptionAllowsInsecureHTTPLoads: true,
              NSIncludesSubdomains: true,
              NSTemporaryExceptionMinimumTLSVersion: "TLSv1.0"
            }
          }
        }
      }
    },
    android: {
      package: "com.rapidphotoupload.app",
      permissions: [
        "CAMERA",
        "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE"
      ]
    },
    web: {},
    plugins: [
      "expo-image-picker",
      "expo-file-system"
    ]
  }
};
