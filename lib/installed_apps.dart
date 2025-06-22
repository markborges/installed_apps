import 'package:flutter/services.dart';
import 'dart:async';
import 'package:installed_apps/app_info.dart';

/// Enumeration for installation and uninstallation event types
enum IUEventType { installed, uninstalled }

/// Class representing an installation or uninstallation event of an application
class IUEvent {
  final IUEventType type;
  final String packageName;

  IUEvent({required this.type, required this.packageName});

  @override
  String toString() {
    return 'AppIUEvent {type: $type, packageName: $packageName}';
  }
}

/// Class that provides functionality to listen to application installation and uninstallation events
class AppIUEvents {
  final EventChannel _eventChannel = const EventChannel(
    'com.abian.app_install_events/app_monitor',
  );

  final StreamController<IUEvent> _appEventController =
      StreamController<IUEvent>.broadcast();

  /// Get a stream of installation and uninstallation events
  Stream<IUEvent> get appEvents => _appEventController.stream;

  StreamSubscription<IUEvent>? _eventSubscription;

  /// Constructor that automatically starts listening to events.
  AppIUEvents() {
    startListening();
  }

  /// Start listening to installation and uninstallation events
  void startListening() {
    _eventSubscription = _eventChannel
        .receiveBroadcastStream()
        .map((event) {
          final parts = event.split(':');
          final type = parts[0] == 'AppInstalled'
              ? IUEventType.installed
              : IUEventType.uninstalled;
          final packageName = parts[1];

          return IUEvent(type: type, packageName: packageName);
        })
        .listen((appEvent) {
          _appEventController.add(appEvent);
        });
  }

  /// Stop listening to events
  void dispose() {
    _eventSubscription?.cancel();
    _eventSubscription = null;
  }
}

/// A utility class for interacting with installed apps on the device.
class InstalledApps {
  static const MethodChannel _channel = const MethodChannel('installed_apps');

  /// Retrieves a list of installed apps on the device.
  ///
  /// [excludeSystemApps] specifies whether to exclude system apps from the list.
  /// [withLaunchIntentOnly] specifies whether to include only apps that can be launched
  /// [withIcon] specifies whether to include app icons in the list.
  /// [packageNamePrefix] is an optional parameter to filter apps with package names starting with a specific prefix.
  /// [platformType] is an optional parameter to set the app type. Default is [AppPlatformType.flutter].
  ///
  /// Returns a list of [AppInfo] objects representing the installed apps.
  static Future<List<AppInfo>> getInstalledApps([
    bool excludeSystemApps = false,
    bool withLaunchIntentOnly = true,
    bool withIcon = false,
    String packageNamePrefix = "",
    BuiltWith platformType = BuiltWith.flutter,
  ]) async {
    dynamic apps = await _channel.invokeMethod("getInstalledApps", {
      "exclude_system_apps": excludeSystemApps,
      "with_launch_intent_only": withLaunchIntentOnly,
      "with_icon": withIcon,
      "package_name_prefix": packageNamePrefix,
      "platform_type": platformType.name,
    });
    return AppInfo.parseList(apps);
  }

  /// Launches an app with the specified package name.
  ///
  /// [packageName] is the package name of the app to launch.
  ///
  /// Returns a boolean indicating whether the operation was successful.
  static Future<bool?> startApp(String packageName) async {
    return _channel.invokeMethod("startApp", {"package_name": packageName});
  }

  /// Opens the settings screen (App Info) of an app with the specified package name.
  ///
  /// [packageName] is the package name of the app whose settings screen should be opened.
  static openSettings(String packageName) {
    _channel.invokeMethod("openSettings", {"package_name": packageName});
  }

  /// Displays a toast message on the device.
  ///
  /// [message] is the message to display.
  /// [isShortLength] specifies whether the toast should be short or long in duration.
  static toast(String message, bool isShortLength) {
    _channel.invokeMethod("toast", {
      "message": message,
      "short_length": isShortLength,
    });
  }

  /// Retrieves information about an app with the specified package name.
  ///
  /// [packageName] is the package name of the app to retrieve information for.
  ///
  /// Returns [AppInfo] for the given package name, or null if not found.
  static Future<AppInfo?> getAppInfo(
    String packageName,
    BuiltWith? platformType,
  ) async {
    var app = await _channel.invokeMethod("getAppInfo", {
      "package_name": packageName,
      "platform_type": platformType?.name ?? '',
    });
    if (app == null) {
      return null;
    } else {
      return AppInfo.create(app);
    }
  }

  /// Checks if an app with the specified package name is a system app.
  ///
  /// [packageName] is the package name of the app to check.
  ///
  /// Returns a boolean indicating whether the app is a system app.
  static Future<bool?> isSystemApp(String packageName) async {
    return _channel.invokeMethod("isSystemApp", {"package_name": packageName});
  }

  /// Uninstalls an app with the specified package name.
  ///
  /// [packageName] is the package name of the app to uninstall.
  ///
  /// Returns a boolean indicating whether the uninstallation was successful.
  static Future<bool?> uninstallApp(String packageName) async {
    return _channel.invokeMethod("uninstallApp", {"package_name": packageName});
  }

  /// Checks if an app with the specified package name is installed on the device.
  ///
  /// [packageName] is the package name of the app to check.
  ///
  /// Returns a boolean indicating whether the app is installed.
  static Future<bool?> isAppInstalled(String packageName) async {
    return _channel.invokeMethod("isAppInstalled", {
      "package_name": packageName,
    });
  }
}
