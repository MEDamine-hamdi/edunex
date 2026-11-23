import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

const _adminFields = <(String, String, TextInputType?)>[
  ('firstName', 'Prénom', null),
  ('lastName', 'Nom', null),
  ('email', 'E-mail', TextInputType.emailAddress),
  ('cin', 'CIN (8 chiffres)', TextInputType.phone),
  ('phone', 'Téléphone', TextInputType.phone),
  ('employeeNumber', "Numéro d'employé", null),
  ('service', 'Service', null),
];

/// Creates an administrator through [endpoint]: a temporary password is generated and e-mailed.
Future<bool> createAdminAccount(BuildContext context, String endpoint) async {
  final api = apiOf(context);
  final c = {for (final f in _adminFields) f.$1: TextEditingController()};
  final ok = await showFormSheet(
    context,
    title: 'Nouvel administrateur',
    action: 'Créer le compte',
    fields: (_) => [
      const Text("Aucun mot de passe à saisir : un mot de passe provisoire est généré et envoyé par e-mail. La personne devra le remplacer à sa première connexion."),
      const SizedBox(height: 10),
      for (final f in _adminFields) Field(c[f.$1]!, f.$2, keyboard: f.$3),
    ],
    submit: () async {
      try {
        await api.post(endpoint, body: {for (final f in _adminFields) f.$1: c[f.$1]!.text.trim().isEmpty ? null : c[f.$1]!.text.trim()});
      } on ApiException catch (e) {
        if (e.status == 409) throw ApiException(409, "Cette adresse e-mail (ou ce numéro) est déjà utilisée.");
        if (e.status == 503) throw ApiException(503, "L'envoi d'e-mails n'est pas disponible : le compte n'a pas été créé.");
        rethrow;
      }
    },
  );
  return ok == true;
}

// ======================= Platform: universities =======================
class PlatformUniversitiesPage extends StatefulWidget {
  const PlatformUniversitiesPage({super.key});
  @override
  State<PlatformUniversitiesPage> createState() => _PlatformUniversitiesPageState();
}

class _PlatformUniversitiesPageState extends State<PlatformUniversitiesPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  Future<void> _create() async {
    final api = apiOf(context);
    final name = TextEditingController();
    final code = TextEditingController();
    final city = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: 'Nouvelle université',
      action: "Créer l'université",
      fields: (_) => [
        Field(name, 'Nom'),
        Field(code, 'Code (ex. UTUN)'),
        Field(city, 'Ville'),
      ],
      submit: () async {
        if (name.text.trim().isEmpty || code.text.trim().isEmpty) throw ApiException(0, 'Le nom et le code sont obligatoires.');
        await api.post('/api/platform/universities', body: {'name': name.text.trim(), 'code': code.text.trim(), 'city': city.text.trim().isEmpty ? null : city.text.trim()});
      },
    );
    if (ok == true) refresh();
  }

  Future<void> _toggle(JMap u) async {
    final suspend = u['status'] == 'ACTIVE';
    final name = str(u['name']) ?? '';
    if (!await confirmDialog(context, suspend ? "Suspendre l'université" : "Réactiver l'université", suspend ? 'Suspendre « $name » ? Ses utilisateurs ne pourront plus se connecter.' : 'Réactiver « $name » ?', danger: suspend, ok: suspend ? 'Suspendre' : 'Réactiver')) return;
    if (!mounted) return;
    try {
      await apiOf(context).put('/api/platform/universities/${u['id']}/status', body: {'status': suspend ? 'SUSPENDED' : 'ACTIVE'});
      refresh();
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: _create, icon: const Icon(Icons.add), label: const Text('Nouvelle université')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/platform/universities')),
        builder: (context, rows, reload) => PageList([
          const Text("Chaque université ne voit que ses propres données."),
          if (rows.isEmpty) const EmptyState('Aucune université', icon: Icons.account_balance_outlined),
          for (final u in rows)
            Panel(
              title: str(u['name']) ?? '',
              trailing: Badge2(u['status'] == 'ACTIVE' ? 'Active' : 'Suspendue', color: u['status'] == 'ACTIVE' ? EduColors.emerald : EduColors.red),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('${str(u['code']) ?? ''}${(str(u['city']) ?? '').isNotEmpty ? ' · ${u['city']}' : ''}', style: TextStyle(color: Theme.of(context).hintColor)),
                const SizedBox(height: 4),
                Text('${tr('Administrateurs')} : ${u['agents']} · ${tr('Professeurs')} : ${u['professors']} · ${tr('Étudiants')} : ${u['students']}'),
                const SizedBox(height: 8),
                Wrap(spacing: 8, runSpacing: 8, children: [
                  OutlinedButton.icon(
                    onPressed: () async {
                      await Navigator.push(context, MaterialPageRoute(builder: (_) => _UniversityAdminsPage(id: asInt(u['id'])!, name: str(u['name']) ?? '')));
                      refresh();
                    },
                    icon: const Icon(Icons.admin_panel_settings_outlined),
                    label: const Text('Administrateurs'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => _toggle(u),
                    icon: Icon(u['status'] == 'ACTIVE' ? Icons.pause_circle_outline : Icons.play_circle_outline),
                    label: Text(u['status'] == 'ACTIVE' ? 'Suspendre' : 'Réactiver'),
                  ),
                ]),
              ]),
            ),
        ]),
      ),
    );
  }
}

class _UniversityAdminsPage extends StatefulWidget {
  final int id;
  final String name;
  const _UniversityAdminsPage({required this.id, required this.name});
  @override
  State<_UniversityAdminsPage> createState() => _UniversityAdminsPageState();
}

class _UniversityAdminsPageState extends State<_UniversityAdminsPage> {
  Key k = UniqueKey();
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    final base = '/api/platform/universities/${widget.id}/admins';
    return Scaffold(
      appBar: AppBar(title: Text(widget.name)),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          if (await createAdminAccount(context, base)) setState(() => k = UniqueKey());
        },
        icon: const Icon(Icons.person_add_alt),
        label: const Text('Nouvel administrateur'),
      ),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get(base)),
        builder: (context, rows, reload) => PageList([
          if (rows.isEmpty) const EmptyState('Aucun administrateur : créez le premier.', icon: Icons.admin_panel_settings_outlined),
          for (final a in rows) _AdminTile(a, onToggle: () async {
            try {
              await api.put('$base/${a['id']}/status', body: {'status': a['status'] == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'});
              setState(() => k = UniqueKey());
            } catch (e) {
              if (context.mounted) toastError(context, e);
            }
          }),
        ]),
      ),
    );
  }
}

class _AdminTile extends StatelessWidget {
  final JMap a;
  final VoidCallback onToggle;
  const _AdminTile(this.a, {required this.onToggle});
  @override
  Widget build(BuildContext context) {
    final active = a['status'] == 'ACTIVE';
    return ListTileCard(
      leading: Avatar('${(str(a['firstName']) ?? '?')[0]}${(str(a['lastName']) ?? '?')[0]}'.toUpperCase(), color: EduColors.primary),
      title: Text('${a['firstName']} ${a['lastName']}'),
      subtitle: Text('${str(a['email']) ?? ''}\n${str(a['employeeNumber']) ?? ''}${(str(a['service']) ?? '').isNotEmpty ? ' · ${a['service']}' : ''}'),
      trailing: TextButton(onPressed: onToggle, child: Text(active ? 'Désactiver' : 'Activer')),
    );
  }
}

// ======================= University administrator: GET/POST /api/admin/agents =======================
class UniversityAdminsPage extends StatefulWidget {
  const UniversityAdminsPage({super.key});
  @override
  State<UniversityAdminsPage> createState() => _UniversityAdminsHereState();
}

class _UniversityAdminsHereState extends State<UniversityAdminsPage> {
  Key k = UniqueKey();
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          if (await createAdminAccount(context, '/api/admin/agents')) setState(() => k = UniqueKey());
        },
        icon: const Icon(Icons.person_add_alt),
        label: const Text('Ajouter un administrateur'),
      ),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/admin/agents')),
        builder: (context, rows, reload) => PageList([
          const Text("Les administrateurs de votre université. Les professeurs et les étudiants se créent depuis leurs pages."),
          for (final a in rows) _AdminTile(a, onToggle: () async {
            try {
              await api.put('/api/admin/agents/${a['id']}/status', body: {'status': a['status'] == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'});
              setState(() => k = UniqueKey());
            } catch (e) {
              if (context.mounted) toastError(context, e);
            }
          }),
        ]),
      ),
    );
  }
}
