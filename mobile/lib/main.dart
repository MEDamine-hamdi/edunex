import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/api/api_client.dart';
import 'core/auth/auth_controller.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/theme_controller.dart';
import 'features/auth/login_page.dart';
import 'features/shell/role_shell.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('fr');
  final api = ApiClient();
  final auth = AuthController(api);
  final prefs = await SharedPreferences.getInstance();
  final mode = ThemeController(prefs);
  await auth.boot();
  runApp(MultiProvider(
    providers: [
      Provider<ApiClient>.value(value: api),
      ChangeNotifierProvider<AuthController>.value(value: auth),
      ChangeNotifierProvider<ThemeController>.value(value: mode),
    ],
    child: const EduNexApp(),
  ));
}

class EduNexApp extends StatelessWidget {
  const EduNexApp({super.key});
  @override
  Widget build(BuildContext context) {
    final theme = context.watch<ThemeController>();
    final auth = context.watch<AuthController>();
    return MaterialApp(
      title: 'EduNex',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      themeMode: theme.mode,
      locale: const Locale('fr'),
      supportedLocales: const [Locale('fr')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: auth.signedIn ? RoleShell(key: ValueKey(auth.user!.id), user: auth.user!) : const LoginPage(),
    );
  }
}