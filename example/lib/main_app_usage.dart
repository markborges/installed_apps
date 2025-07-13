import 'package:flutter/material.dart';
import 'package:installed_apps/app_usage.dart';
import 'package:installed_apps/installed_apps.dart';

void main() => runApp(AppUsageApp());

class AppUsageApp extends StatefulWidget {
  @override
  AppUsageAppState createState() => AppUsageAppState();
}

class AppUsageAppState extends State<AppUsageApp> {
  List<AppUsageInfo> _infos = [];

  @override
  void initState() {
    super.initState();
  }

  void getUsageStats() async {
    try {
      // List<AppUsageInfo> infoList = await AppUsage().getAppUsageAsList();
      Map<String, AppUsageInfo> infoList = await AppUsage().getAppUsageAsMap();

      List<AppUsageInfo> usage = [];
      for (var key in infoList.keys) {
        usage.add(infoList[key]!);
      }
      setState(() => _infos = usage);
    } catch (exception) {
      print(exception);
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('App Usage Example'),
          backgroundColor: Colors.green,
        ),
        body: ListView.builder(
            itemCount: _infos.length,
            itemBuilder: (context, index) {
              return ListTile(
                  onTap: () {
                    InstalledApps.openAppUsage(_infos[index].packageName);
                  },
                  title: Text(_infos[index].appName),
                  trailing: Text(_infos[index].usage.toString()));
            }),
        floatingActionButton: FloatingActionButton(
            onPressed: getUsageStats, child: Icon(Icons.file_download)),
      ),
    );
  }
}
