import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/api/api_client.dart';
import 'core/auth/auth_controller.dart';
import 'core/i18n/i18n.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/theme_controller.dart';
import 'features/auth/forced_password_page.dart';
import 'features/auth/login_page.dart';
import 'features/shell/role_shell.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  for (final l in const ['fr', 'en', 'ar']) {
    await initializeDateFormatting(l);
  }
  final api = ApiClient();
  final auth = AuthController(api);
  final prefs = await SharedPreferences.getInstance();
  final mode = ThemeController(prefs);
  final lang = AppLang(prefs);
  await auth.boot();
  runApp(MultiProvider(
    providers: [
      Provider<ApiClient>.value(value: api),
      ChangeNotifierProvider<AuthController>.value(value: auth),
      ChangeNotifierProvider<ThemeController>.value(value: mode),
      ChangeNotifierProvider<AppLang>.value(value: lang),
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
    final lang = context.watch<AppLang>();
    return LangScope(
      code: lang.code,
      child: MaterialApp(
      title: 'EduTEK',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      themeMode: theme.mode,
      locale: Locale(lang.code),
      supportedLocales: const [Locale('fr'), Locale('en'), Locale('ar')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: !auth.signedIn
          ? const LoginPage()
          : auth.user!.mustChangePassword
              ? const ForcedPasswordPage()
              : RoleShell(key: ValueKey(auth.user!.id), user: auth.user!),
      ),
    );
  }
}
