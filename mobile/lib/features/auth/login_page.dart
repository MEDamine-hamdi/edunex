import 'dart:async';

import 'package:flutter/material.dart' hide Text;
import 'package:provider/provider.dart';

import '../../core/api/api_client.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/i18n/language_switcher.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final email = TextEditingController();
  final password = TextEditingController();
  bool remember = true;
  bool busy = false;
  bool show = false;
  String? error;
  int lockSeconds = 0;
  Timer? timer;

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  void _startLock(int s) {
    timer?.cancel();
    setState(() => lockSeconds = s);
    timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (!mounted) return t.cancel();
      setState(() => lockSeconds = lockSeconds - 1);
      if (lockSeconds <= 0) t.cancel();
    });
  }

  Future<void> _submit() async {
    if (email.text.trim().isEmpty || password.text.isEmpty) {
      setState(() => error = 'Saisissez votre e-mail et votre mot de passe.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await context.read<AuthController>().login(email.text, password.text, remember: remember);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        busy = false;
        error = e.status == 401 ? 'E-mail ou mot de passe incorrect.' : e.message;
      });
      if (e.status == 429 && e.retryAfterSeconds != null) _startLock(e.retryAfterSeconds!);
    }
  }

  Future<void> _server() async {
    final auth = context.read<AuthController>();
    final c = TextEditingController(text: auth.api.baseUrl);
    await showFormSheet(
      context,
      title: 'Adresse du serveur',
      fields: (_) => [
        const Text('URL de l\'API EduNex (backend Spring Boot).'),
        const SizedBox(height: 10),
        Field(c, 'URL du serveur', keyboard: TextInputType.url, hint: 'https://…'),
      ],
      submit: () async {
        final u = c.text.trim();
        if (!u.startsWith('http://') && !u.startsWith('https://')) {
          throw ApiException(0, 'L\'adresse doit commencer par http:// ou https://');
        }
        await auth.setBaseUrl(u);
      },
    );
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthController>();
    final dark = Theme.of(context).brightness == Brightness.dark;
    final locked = lockSeconds > 0;
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: dark
              ? null
              : const LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [Color(0xFFFEF3C7), Color(0xFFFFE4E6), Color(0xFFE0E7FF)],
                ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 440),
                child: Column(children: [
                  const Align(alignment: AlignmentDirectional.centerEnd, child: LanguageSwitcher(compact: true)),
                  const EduLogo(size: 52),
                  const SizedBox(height: 8),
                  Text('La plateforme de votre établissement',
                      style: TextStyle(color: Theme.of(context).hintColor, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 24),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(22),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                        const Text('Connexion', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900)),
                        const SizedBox(height: 4),
                        Text('Accédez à votre espace EduNex',
                            style: TextStyle(color: Theme.of(context).hintColor)),
                        const SizedBox(height: 20),
                        if (auth.notice != null)
                          Container(
                            margin: const EdgeInsets.only(bottom: 14),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                                color: EduColors.orange.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(12)),
                            child: Text(auth.notice!, style: const TextStyle(color: EduColors.orange, fontWeight: FontWeight.w600)),
                          ),
                        TextField(
                          controller: email,
                          keyboardType: TextInputType.emailAddress,
                          autofillHints: const [AutofillHints.username],
                          decoration: InputDecoration(labelText: tr('Adresse e-mail'), prefixIcon: Icon(Icons.mail_outline)),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: password,
                          obscureText: !show,
                          autofillHints: const [AutofillHints.password],
                          onSubmitted: (_) => busy || locked ? null : _submit(),
                          decoration: InputDecoration(
                            labelText: tr('Mot de passe'),
                            prefixIcon: const Icon(Icons.lock_outline),
                            suffixIcon: IconButton(
                              icon: Icon(show ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                              onPressed: () => setState(() => show = !show),
                            ),
                          ),
                        ),
                        const SizedBox(height: 6),
                        Row(children: [
                          Checkbox(value: remember, onChanged: (v) => setState(() => remember = v ?? true)),
                          const Expanded(child: Text('Rester connecté')),
                          TextButton(
                            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ForgotPasswordPage())),
                            child: const Text('Mot de passe oublié ?'),
                          ),
                        ]),
                        if (error != null)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 10),
                            child: Text(error!, style: const TextStyle(color: EduColors.red, fontWeight: FontWeight.w600)),
                          ),
                        if (locked)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 10),
                            child: Text('Réessayez dans ${lockSeconds ~/ 60}:${(lockSeconds % 60).toString().padLeft(2, '0')}',
                                style: const TextStyle(color: EduColors.orange, fontWeight: FontWeight.w700)),
                          ),
                        FilledButton(
                          style: FilledButton.styleFrom(backgroundColor: EduColors.indigo),
                          onPressed: busy || locked ? null : _submit,
                          child: busy
                              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                              : const Text('Se connecter'),
                        ),
                      ]),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextButton.icon(
                    onPressed: _server,
                    icon: const Icon(Icons.dns_outlined, size: 18),
                    label: Text('Serveur : ${auth.api.baseUrl}', overflow: TextOverflow.ellipsis),
                  ),
                ]),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class ForgotPasswordPage extends StatefulWidget {
  const ForgotPasswordPage({super.key});
  @override
  State<ForgotPasswordPage> createState() => _ForgotPasswordPageState();
}

class _ForgotPasswordPageState extends State<ForgotPasswordPage> {
  final email = TextEditingController();
  final token = TextEditingController();
  final pass = TextEditingController();
  bool sent = false;
  bool busy = false;
  String? error;

  /// The e-mail contains a link with ?token=...; accept either the raw token or the whole link.
  String _token(String raw) {
    final t = raw.trim();
    final m = RegExp(r'token=([^&\s]+)').firstMatch(t);
    return m != null ? Uri.decodeComponent(m.group(1)!) : t;
  }

  Future<void> _run(Future<void> Function() f) async {
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await f();
    } catch (e) {
      error = e.toString();
    }
    if (mounted) setState(() => busy = false);
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Mot de passe oublié')),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        if (!sent) ...[
          const Text('Saisissez votre adresse e-mail : si un compte existe, vous recevrez un lien de réinitialisation.'),
          const SizedBox(height: 16),
          Field(email, 'Adresse e-mail', keyboard: TextInputType.emailAddress),
          FilledButton(
            onPressed: busy
                ? null
                : () => _run(() async {
                      await api.post('/api/auth/forgot-password', body: {'email': email.text.trim()}, auth: false);
                      sent = true;
                    }),
            child: const Text('Envoyer le lien'),
          ),
          TextButton(onPressed: () => setState(() => sent = true), child: const Text('J\'ai déjà reçu un lien')),
        ] else ...[
          const Text('Collez le lien (ou le code) reçu par e-mail puis choisissez un nouveau mot de passe '
              '(10 caractères minimum, avec lettres et chiffres).'),
          const SizedBox(height: 16),
          Field(token, 'Lien ou code reçu par e-mail'),
          Field(pass, 'Nouveau mot de passe', obscure: true),
          FilledButton(
            onPressed: busy
                ? null
                : () => _run(() async {
                      await api.post('/api/auth/reset-password',
                          body: {'token': _token(token.text), 'newPassword': pass.text}, auth: false);
                      if (mounted) {
                        toast(context, 'Mot de passe modifié. Vous pouvez vous connecter.');
                        Navigator.pop(context);
                      }
                    }),
            child: const Text('Réinitialiser'),
          ),
        ],
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(error!, style: const TextStyle(color: EduColors.red, fontWeight: FontWeight.w600)),
          ),
      ]),
    );
  }
}
