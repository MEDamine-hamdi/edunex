import 'package:flutter/material.dart' hide Text;
import 'package:provider/provider.dart';

import '../../core/auth/auth_controller.dart';
import '../../core/i18n/language_switcher.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

/// Shown right after login while the account still uses the temporary password
/// e-mailed at creation (backend flag `mustChangePassword`, PUT /api/profile/first-password).
class ForcedPasswordPage extends StatefulWidget {
  const ForcedPasswordPage({super.key});
  @override
  State<ForcedPasswordPage> createState() => _ForcedPasswordPageState();
}

class _ForcedPasswordPageState extends State<ForcedPasswordPage> {
  final current = TextEditingController();
  final next = TextEditingController();
  final confirm = TextEditingController();
  bool busy = false;
  String? error;

  Future<void> _submit() async {
    if (next.text != confirm.text) {
      setState(() => error = 'Les deux mots de passe ne sont pas identiques.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    final auth = context.read<AuthController>();
    try {
      await auth.api.put('/api/profile/first-password', body: {'currentPassword': current.text, 'newPassword': next.text});
      auth.passwordChanged();
    } catch (e) {
      final m = e.toString();
      setState(() {
        busy = false;
        error = m.contains('Incorrect password')
            ? 'Le mot de passe provisoire est incorrect.'
            : m.contains('must be different')
                ? 'Le nouveau mot de passe doit être différent du mot de passe provisoire.'
                : m;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthController>().user!;
    return Scaffold(
      appBar: AppBar(
        title: const EduLogo(size: 30),
        actions: const [LanguageSwitcher(compact: true)],
      ),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        Text('Bienvenue ${user.firstName}', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
        const SizedBox(height: 8),
        const Text('Pour sécuriser votre compte, remplacez le mot de passe provisoire reçu par e-mail avant de continuer. '
            'Le nouveau mot de passe doit contenir au moins 10 caractères, avec des lettres et des chiffres.'),
        const SizedBox(height: 20),
        Field(current, 'Mot de passe provisoire', obscure: true),
        Field(next, 'Nouveau mot de passe', obscure: true),
        Field(confirm, 'Confirmer le nouveau mot de passe', obscure: true),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(error!, style: const TextStyle(color: EduColors.red, fontWeight: FontWeight.w600)),
          ),
        FilledButton(
          onPressed: busy ? null : _submit,
          child: busy
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
              : const Text('Enregistrer et continuer'),
        ),
        TextButton(onPressed: () => context.read<AuthController>().logout(), child: const Text('Se déconnecter')),
      ]),
    );
  }
}
