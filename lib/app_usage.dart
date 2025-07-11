//Original Source: https://github.com/cph-cachet/flutter-plugins/blob/master/packages/app_usage/lib/app_usage.dart
import 'dart:async';

import 'package:flutter/services.dart';
import 'dart:io' show Platform;

/// Information on app usage.
class AppUsageInfo {
  late String _packageName, _appName;
  late Duration _usage;

  AppUsageInfo(
    String name,
    int usageInSeconds,
  ) {
    List<String> tokens = name.split('.');
    _packageName = name;
    _appName = tokens.last;
    _usage = Duration(seconds: usageInSeconds.toInt());
  }

  /// The name of the application
  String get appName => _appName;

  /// The name of the application package
  String get packageName => _packageName;

  /// The amount of time the application has been used
  /// in the specified interval
  Duration get usage => _usage;

  @override
  String toString() => 'App Usage: $packageName - $appName, duration: $usage';
}

/// Singleton class to get app usage statistics.
class AppUsage {
  static const MethodChannel _methodChannel =
      const MethodChannel("installed_apps");

  static final AppUsage _instance = AppUsage._();
  AppUsage._();
  factory AppUsage() => _instance;

  /// Get app usage statistics for the specified interval. Only works on Android.
  /// Returns an empty list if called on iOS.
  Future<List<AppUsageInfo>> getAppUsage(
    DateTime startDate,
    DateTime endDate,
  ) async {
    if (!Platform.isAndroid) return [];

    if (endDate.isBefore(startDate)) {
      throw ArgumentError('End date must be after start date');
    }

    // Convert dates to ms since epoch
    int end = endDate.millisecondsSinceEpoch;
    int start = startDate.millisecondsSinceEpoch;

    // Set parameters
    Map<String, int> interval = {'start': start, 'end': end};

    // Get result and parse it as a Map of <String, List<double>>
    Map usage = await _methodChannel.invokeMethod('getUsage');

    // Convert to list of AppUsageInfo
    List<AppUsageInfo> result = [];
    for (String key in usage.keys) {
      result.add(AppUsageInfo(
        key,
        usage[key],
      ));
    }

    return result;
  }
}
