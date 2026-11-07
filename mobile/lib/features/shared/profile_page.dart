import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/api/api_client.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});
  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<(JMap, bool, Uint8List?)>(
      key: k,
      load: () async {
        final p = Map<String, dynamic>.from(await api.get('/api/profile') as Map);
        bool emailPref = true;
        try {
          emailPref = (await api.get('/api/profile/email-notifications') as Map)['enabled'] == true;
        } catch (_) {}
        Uint8List? photo;
        if (p['hasPhoto'] == true) {
          try {
            photo = await api.bytes('/api/profile/photo');
          } catch (_) {}
        }
        return (p, emailPref, photo);
      },
      builder: (context, d, reload) {
        final (p, emailPref, photo) = d;
        final user = context.watch<AuthController>().user!;
        final details = listOf(p['details'], (m) => (str(m['label']) ?? '', str(m['value']) ?? ''));
        return PageList([
          Panel(
            child: Column(children: [
              Stack(alignment: Alignment.bottomRight, children: [
                CircleAvatar(
                  radius: 48,
                  backgroundColor: roleColor(user.role).withValues(alpha: 0.15),
                  backgroundImage: photo != null ? MemoryImage(photo) : null,
                  child: photo == null
                      ? Text(user.initials,
                          style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900, color: roleColor(user.role)))
                      : null,
                ),
                CircleAvatar(
                  radius: 16,
                  backgroundColor: EduColors.primary,
                  child: IconButton(
                    padding: EdgeInsets.zero,
                    icon: const Icon(Icons.camera_alt, size: 16, color: Colors.white),
                    onPressed: () => _photo(context, api, hasPhoto: photo != null),
                  ),
                ),
              ]),
              const SizedBox(height: 12),
              Text('${p['firstName']} ${p['lastName']}', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
              const SizedBox(height: 4),
              Text(str(p['email']) ?? '', style: TextStyle(color: Theme.of(context).hintColor)),
              const SizedBox(height: 8),
              Wrap(spacing: 8, children: [
                Badge2(roleLabel(user.role), color: roleColor(user.role)),
                Badge2(statusFr[p['status']] ?? str(p['status']) ?? '', color: statusColor(str(p['status']) ?? '')),
              ]),
            ]),
          ),
          Panel(
            title: 'Informations',
            trailing: TextButton(onPressed: () => _edit(context, api, p), child: const Text('Modifier')),
            child: Column(children: [
              _row('TÃ©lÃ©phone', str(p['phone'])),
              _row('Adresse', str(p['address'])),
              _row('Date de naissance', p['dateOfBirth'] == null ? null : fmtDate(asDate(p['dateOfBirth']))),
              for (final (label, value) in details) _row(label, value),
            ]),
          ),
          Panel(
            title: 'SÃ©curitÃ©',
            child: Column(children: [
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.lock_outline),
                title: const Text('Changer le mot de passe'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _password(context, api),
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.alternate_email),
                title: const Text('Changer l\'adresse e-mail'),
                subtitle: const Text('Un lien de confirmation est envoyÃ© Ã  la nouvelle adresse'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _email(context, api),
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                secondary: const Icon(Icons.mark_email_unread_outlined),
                title: const Text('Notifications par e-mail'),
                value: emailPref,
                onChanged: (v) async {
                  try {
                    await api.put('/api/profile/email-notifications', body: {'enabled': v});
                    refresh();
                  } catch (e) {
                    if (context.mounted) toastError(context, e);
                  }
                },
              ),
            ]),
          ),
        ]);
      },
    );
  }

  Widget _row(String l, String? v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          SizedBox(width: 130, child: Text(l, style: TextStyle(color: Theme.of(context).hintColor))),
          Expanded(child: Text((v == null || v.isEmpty) ? 'â€”' : v, style: const TextStyle(fontWeight: FontWeight.w600))),
        ]),
      );

  Future<void> _photo(BuildContext context, ApiClient api, {required bool hasPhoto}) async {
    final choice = await showModalBottomSheet<String>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(leading: const Icon(Icons.photo_library_outlined), title: const Text('Choisir une photo'), onTap: () => Navigator.pop(context, 'pick')),
          if (hasPhoto)
            ListTile(leading: const Icon(Icons.delete_outline, color: EduColors.red), title: const Text('Supprimer la photo'), onTap: () => Navigator.pop(context, 'delete')),
        ]),
      ),
    );
    try {
      if (choice == 'pick') {
        final r = await FilePicker.platform.pickFiles(type: FileType.image, withData: true);
        final f = r?.files.single;
        if (f == null || f.bytes == null) return;
        if (f.size > 2 * 1024 * 1024) {
          if (context.mounted) toast(context, 'La photo ne doit pas dÃ©passer 2 Mo.', error: true);
          return;
        }
        await api.multipart('/api/profile/photo', files: [UploadFile('file', f.name, f.bytes!)]);
        refresh();
      } else if (choice == 'delete') {
        await api.delete('/api/profile/photo');
        refresh();
      }
    } catch (e) {
      if (context.mounted) toastError(context, e);
    }
  }

  Future<void> _edit(BuildContext context, ApiClient api, JMap p) async {
    final phone = TextEditingController(text: str(p['phone']) ?? '');
    final address = TextEditingController(text: str(p['address']) ?? '');
    DateTime? dob = asDate(p['dateOfBirth']);
    final ok = await showFormSheet(
      context,
      title: 'Modifier mes informations',
      fields: (set) => [
        Field(phone, 'TÃ©lÃ©phone', keyboard: TextInputType.phone),
        Field(address, 'Adresse'),
        OutlinedButton.icon(
          onPressed: () async {
            final d = await pickDate(context, initial: dob ?? DateTime(2000), last: DateTime.now());
            if (d != null) set(() => dob = d);
          },
          icon: const Icon(Icons.cake_outlined),
          label: Text(dob == null ? 'Date de naissance' : fmtDate(dob)),
        ),
        const SizedBox(height: 12),
      ],
      submit: () => api.put('/api/profile', body: {
        'phone': phone.text.trim(),
        'address': address.text.trim(),
        'dateOfBirth': dob == null ? null : isoDate(dob!),
      }),
    );
    if (ok == true) refresh();
  }

  Future<void> _password(BuildContext context, ApiClient api) async {
    final cur = TextEditingController();
    final next = TextEditingController();
    final auth = context.read<AuthController>();
    final ok = await showFormSheet(
      context,
      title: 'Changer le mot de passe',
      fields: (_) => [
        Field(cur, 'Mot de passe actuel', obscure: true),
        Field(next, 'Nouveau mot de passe', obscure: true, hint: '10 caractÃ¨res min., lettres et chiffres'),
      ],
      submit: () => api.put('/api/profile/password', body: {'currentPassword': cur.text, 'newPassword': next.text}),
    );
    if (ok == true) {
      // The backend revokes all earlier tokens after a password change.
      await auth.forceSignOut('Mot de passe modifiÃ©. Reconnectez-vous avec le nouveau mot de passe.');
    }
  }

  Future<void> _email(BuildContext context, ApiClient api) async {
    final mail = TextEditingController();
    final pass = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: 'Changer l\'adresse e-mail',
      action: 'Envoyer le lien',
      fields: (_) => [
        Field(mail, 'Nouvelle adresse e-mail', keyboard: TextInputType.emailAddress),
        Field(pass, 'Mot de passe actuel', obscure: true),
      ],
      submit: () => api.post('/api/profile/email-change', body: {'newEmail': mail.text.trim(), 'password': pass.text}),
    );
    if (ok == true && context.mounted) {
      toast(context, 'Un lien de confirmation a Ã©tÃ© envoyÃ© Ã  la nouvelle adresse.');
    }
  }
}