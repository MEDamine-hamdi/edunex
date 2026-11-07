import 'package:flutter/material.dart';

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

// ======================= Dashboard (GET /api/platform/stats) =======================
class PlatformDashboardPage extends StatelessWidget {
  const PlatformDashboardPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      load: () async => Map<String, dynamic>.from(await api.get('/api/platform/stats') as Map),
      builder: (context, d, reload) {
        final days = maps(d['last7Days']);
        const roles = {'ROLE_STUDENT': 'Ã‰tudiants', 'ROLE_PROFESSOR': 'Professeurs', 'ROLE_ADMIN_AGENT': 'Agents', 'ROLE_PLATFORM_ADMIN': 'Administrateurs'};
        return PageList([
          StatGrid([
            StatCard(label: 'Utilisateurs', value: '${asInt(d['totalUsers']) ?? 0}', icon: Icons.people_outline),
            StatCard(label: 'EntrÃ©es du journal', value: '${asInt(d['logEntries']) ?? 0}', icon: Icons.history, color: EduColors.indigo),
            StatCard(label: 'ParamÃ¨tres', value: '${asInt(d['settings']) ?? 0}', icon: Icons.settings_outlined, color: EduColors.violet),
          ]),
          Panel(
            title: 'Utilisateurs par rÃ´le',
            child: BarList([for (final r in maps(d['usersByRole'])) (roles[r['role']] ?? str(r['role']) ?? '', asDouble(r['count']) ?? 0)]),
          ),
          Panel(
            title: 'Utilisateurs par statut',
            child: BarList([for (final r in maps(d['usersByStatus'])) (statusFr[r['status']] ?? str(r['status']) ?? '', asDouble(r['count']) ?? 0)], color: EduColors.emerald),
          ),
          Panel(
            title: 'ActivitÃ© des 7 derniers jours',
            child: Column(children: [
              if (days.isEmpty) const EmptyState('Aucune activitÃ©'),
              for (final x in days)
                ListTile(
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  title: Text(fmtDate(asDate(x['date'])), style: const TextStyle(fontWeight: FontWeight.w700)),
                  subtitle: Text('Connexions ${asInt(x['logins'])} Â· Ã‰checs ${asInt(x['failedLogins'])} Â· Modifications ${asInt(x['changes'])}'),
                ),
            ]),
          ),
        ]);
      },
    );
  }
}

// ======================= Agents =======================
class PlatformAgentsPage extends StatefulWidget {
  const PlatformAgentsPage({super.key});
  @override
  State<PlatformAgentsPage> createState() => _PlatformAgentsPageState();
}

class _PlatformAgentsPageState extends State<PlatformAgentsPage> {
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _create(context), icon: const Icon(Icons.person_add_alt), label: const Text('Nouvel agent')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/platform/agents')),
        builder: (context, items, reload) => PageList([
          if (items.isEmpty) const EmptyState('Aucun agent'),
          for (final u in items)
            ListTileCard(
              leading: Avatar('${(str(u['firstName']) ?? '?')[0]}${(str(u['lastName']) ?? '?')[0]}'.toUpperCase(), color: EduColors.primary),
              title: Text('${u['firstName']} ${u['lastName']}'),
              subtitle: Text('${str(u['email']) ?? ''}\n${str(u['employeeNumber']) ?? ''} Â· ${str(u['service']) ?? ''}'),
              trailing: PopupMenuButton<String>(
                child: Badge2(statusFr[u['status']] ?? str(u['status']) ?? '', color: statusColor(str(u['status']) ?? '')),
                onSelected: (s) async {
                  try {
                    await api.patch('/api/platform/agents/${u['id']}/status', body: {'status': s});
                    setState(() => k = UniqueKey());
                  } catch (e) {
                    if (context.mounted) toastError(context, e);
                  }
                },
                itemBuilder: (_) => [for (final s in const ['ACTIVE', 'INACTIVE', 'SUSPENDED']) if (s != u['status']) PopupMenuItem(value: s, child: Text('Passer Ã  Â« ${statusFr[s]} Â»'))],
              ),
            ),
        ]),
      ),
    );
  }

  Future<void> _create(BuildContext context) async {
    final api = apiOf(context);
    const List<(String, String, TextInputType?)> keys = [
      ('firstName', 'PrÃ©nom', null),
      ('lastName', 'Nom', null),
      ('email', 'E-mail', TextInputType.emailAddress),
      ('phone', 'TÃ©lÃ©phone', TextInputType.phone),
      ('employeeNumber', 'Matricule', null),
      ('service', 'Service', null),
    ];
    final c = {for (final f in keys) f.$1: TextEditingController()};
    final ok = await showFormSheet(
      context,
      title: 'Nouvel agent d\'administration',
      action: 'CrÃ©er le compte',
      fields: (_) => [for (final f in keys) Field(c[f.$1]!, f.$2, keyboard: f.$3)],
      submit: () => api.post('/api/platform/agents', body: {for (final f in keys) f.$1: c[f.$1]!.text.trim()}),
    );
    if (ok == true) setState(() => k = UniqueKey());
  }
}

// ======================= Roles =======================
class PlatformRolesPage extends StatefulWidget {
  const PlatformRolesPage({super.key});
  @override
  State<PlatformRolesPage> createState() => _PlatformRolesPageState();
}

class _PlatformRolesPageState extends State<PlatformRolesPage> {
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      key: k,
      load: () async => maps(await api.get('/api/platform/roles')),
      builder: (context, items, reload) => PageList([
        for (final r in items)
          Panel(
            title: str(r['name']) ?? '',
            trailing: r['locked'] == true ? const Badge2('VerrouillÃ©', color: EduColors.slate500) : Badge2('${asInt(r['userCount'])} utilisateur(s)', color: EduColors.primary),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              if ((str(r['description']) ?? '').isNotEmpty) Text(str(r['description'])!),
              const SizedBox(height: 8),
              Wrap(spacing: 6, runSpacing: 6, children: [
                for (final p in maps(r['permissions'])) Badge2(str(p['code']) ?? '', color: EduColors.indigo),
                if (maps(r['permissions']).isEmpty) Text('Aucune permission', style: TextStyle(color: Theme.of(context).hintColor)),
              ]),
              if (r['locked'] != true) ...[
                const SizedBox(height: 8),
                OutlinedButton.icon(onPressed: () => _edit(context, r), icon: const Icon(Icons.key_outlined), label: const Text('Modifier les permissions')),
              ],
            ]),
          ),
      ]),
    );
  }

  Future<void> _edit(BuildContext context, JMap role) async {
    final api = apiOf(context);
    List<JMap> perms;
    try {
      perms = maps(await api.get('/api/platform/permissions'));
    } catch (e) {
      toastError(context, e);
      return;
    }
    final sel = {for (final p in maps(role['permissions'])) asInt(p['id'])!};
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Permissions Â· ${role['name']}',
      fields: (set) => [
        for (final p in perms)
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: sel.contains(asInt(p['id'])),
            title: Text(str(p['code']) ?? ''),
            subtitle: Text(str(p['description']) ?? ''),
            onChanged: (v) => set(() => v == true ? sel.add(asInt(p['id'])!) : sel.remove(asInt(p['id']))),
          ),
      ],
      submit: () => api.put('/api/platform/roles/${role['id']}/permissions', body: {'ids': sel.toList()}),
    );
    if (ok == true) setState(() => k = UniqueKey());
  }
}

// ======================= Permissions =======================
class PlatformPermissionsPage extends StatefulWidget {
  const PlatformPermissionsPage({super.key});
  @override
  State<PlatformPermissionsPage> createState() => _PlatformPermissionsPageState();
}

class _PlatformPermissionsPageState extends State<PlatformPermissionsPage> {
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      key: k,
      load: () async => maps(await api.get('/api/platform/permissions')),
      builder: (context, items, reload) => PageList([
        for (final p in items)
          ListTileCard(
            title: Text(str(p['code']) ?? ''),
            subtitle: Text('${str(p['description']) ?? ''}\nRÃ´les : ${(p['roles'] as List? ?? const []).join(', ')}'),
            trailing: const Icon(Icons.edit_outlined, size: 18),
            onTap: () async {
              final d = TextEditingController(text: str(p['description']) ?? '');
              final ok = await showFormSheet(
                context,
                title: str(p['code']) ?? '',
                fields: (_) => [Field(d, 'Description', maxLines: 3)],
                submit: () => api.put('/api/platform/permissions/${p['id']}', body: {'description': d.text.trim()}),
              );
              if (ok == true) setState(() => k = UniqueKey());
            },
          ),
      ]),
    );
  }
}

// ======================= Settings =======================
class PlatformSettingsPage extends StatefulWidget {
  const PlatformSettingsPage({super.key});
  @override
  State<PlatformSettingsPage> createState() => _PlatformSettingsPageState();
}

class _PlatformSettingsPageState extends State<PlatformSettingsPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _edit(context, null), icon: const Icon(Icons.add), label: const Text('ParamÃ¨tre')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/platform/settings')),
        builder: (context, items, reload) => PageList([
          if (items.isEmpty) const EmptyState('Aucun paramÃ¨tre'),
          for (final s in items)
            ListTileCard(
              title: Text(str(s['key']) ?? ''),
              subtitle: Text(str(s['value']) ?? ''),
              trailing: PopupMenuButton<String>(
                onSelected: (v) async {
                  if (v == 'edit') {
                    _edit(context, s);
                  } else {
                    if (!await confirmDialog(context, 'Supprimer', 'Supprimer Â« ${s['key']} Â» ?', danger: true, ok: 'Supprimer')) return;
                    try {
                      await api.delete('/api/platform/settings/${Uri.encodeComponent(str(s['key']) ?? '')}');
                      refresh();
                    } catch (e) {
                      if (context.mounted) toastError(context, e);
                    }
                  }
                },
                itemBuilder: (_) => const [PopupMenuItem(value: 'edit', child: Text('Modifier')), PopupMenuItem(value: 'delete', child: Text('Supprimer'))],
              ),
            ),
        ]),
      ),
    );
  }

  Future<void> _edit(BuildContext context, JMap? s) async {
    final api = apiOf(context);
    final key = TextEditingController(text: str(s?['key']) ?? '');
    final value = TextEditingController(text: str(s?['value']) ?? '');
    final ok = await showFormSheet(
      context,
      title: s == null ? 'Nouveau paramÃ¨tre' : 'Modifier le paramÃ¨tre',
      fields: (_) => [Field(key, 'ClÃ©', enabled: s == null), Field(value, 'Valeur', maxLines: 3)],
      submit: () => api.put('/api/platform/settings/${Uri.encodeComponent(key.text.trim())}', body: {'value': value.text}),
    );
    if (ok == true) refresh();
  }
}

// ======================= Mail =======================
class PlatformMailPage extends StatefulWidget {
  const PlatformMailPage({super.key});
  @override
  State<PlatformMailPage> createState() => _PlatformMailPageState();
}

class _PlatformMailPageState extends State<PlatformMailPage> {
  Key k = UniqueKey();
  final to = TextEditingController();
  bool busy = false;

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      key: k,
      load: () async => Map<String, dynamic>.from(await api.get('/api/platform/mail/status') as Map),
      builder: (context, d, reload) {
        final failures = maps(d['recentFailures']);
        final configured = d['configured'] == true;
        return PageList([
          Panel(
            child: Row(children: [
              Icon(configured ? Icons.check_circle : Icons.error_outline, color: configured ? EduColors.emerald : EduColors.red),
              const SizedBox(width: 10),
              Expanded(child: Text(configured ? 'Serveur d\'e-mails configurÃ© Â· expÃ©diteur ${str(d['senderEmail']) ?? ''}' : 'Serveur d\'e-mails non configurÃ©', style: const TextStyle(fontWeight: FontWeight.w700))),
            ]),
          ),
          StatGrid([
            StatCard(label: 'En attente', value: '${asInt(d['pending']) ?? 0}', icon: Icons.schedule, color: EduColors.orange),
            StatCard(label: 'EnvoyÃ©s', value: '${asInt(d['sent']) ?? 0}', icon: Icons.mark_email_read_outlined, color: EduColors.emerald),
            StatCard(label: 'Ã‰checs', value: '${asInt(d['failed']) ?? 0}', icon: Icons.error_outline, color: EduColors.red),
          ]),
          Panel(
            title: 'Envoyer un e-mail de test',
            child: Column(children: [
              Field(to, 'Destinataire', keyboard: TextInputType.emailAddress),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: busy
                      ? null
                      : () async {
                          setState(() => busy = true);
                          try {
                            await api.post('/api/platform/mail/test', body: {'to': to.text.trim()});
                            if (context.mounted) toast(context, 'E-mail de test envoyÃ©.');
                            setState(() => k = UniqueKey());
                          } catch (e) {
                            if (context.mounted) toastError(context, e);
                          }
                          if (mounted) setState(() => busy = false);
                        },
                  child: const Text('Envoyer'),
                ),
              ),
            ]),
          ),
          Panel(
            title: 'Derniers Ã©checs',
            child: failures.isEmpty
                ? const EmptyState('Aucun Ã©chec rÃ©cent')
                : Column(children: [
                    for (final f in failures)
                      ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text('${str(f['to'])} Â· ${str(f['subject'])}'),
                        subtitle: Text('${str(f['error']) ?? ''}\n${fmtDateTime(asDate(f['createdAt']))}'),
                      ),
                  ]),
          ),
        ]);
      },
    );
  }
}

// ======================= Activity log =======================
class PlatformLogsPage extends StatefulWidget {
  const PlatformLogsPage({super.key});
  @override
  State<PlatformLogsPage> createState() => _PlatformLogsPageState();
}

class _PlatformLogsPageState extends State<PlatformLogsPage> {
  final q = TextEditingController();
  final items = <JMap>[];
  int page = 0, totalPages = 1;
  bool loading = false;
  Object? error;

  @override
  void initState() {
    super.initState();
    _load(reset: true);
  }

  Future<void> _load({bool reset = false}) async {
    if (loading) return;
    if (reset) {
      page = 0;
      items.clear();
    }
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final r = Map<String, dynamic>.from(await apiOf(context).get('/api/platform/logs', query: {'q': q.text.trim(), 'page': page, 'size': 25}) as Map);
      items.addAll(maps(r['items']));
      totalPages = asInt(r['totalPages']) ?? 1;
    } catch (e) {
      error = e;
    }
    if (mounted) setState(() => loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        child: TextField(
          controller: q,
          textInputAction: TextInputAction.search,
          onSubmitted: (_) => _load(reset: true),
          decoration: InputDecoration(hintText: 'Rechercher (action, utilisateur, IPâ€¦)', prefixIcon: const Icon(Icons.search), suffixIcon: IconButton(icon: const Icon(Icons.arrow_forward), onPressed: () => _load(reset: true))),
        ),
      ),
      Expanded(
        child: RefreshIndicator(
          onRefresh: () => _load(reset: true),
          child: error != null && items.isEmpty
              ? ListView(children: [SizedBox(height: 300, child: ErrorState(error!, () => _load(reset: true)))])
              : PageList([
                  if (items.isEmpty && !loading) const EmptyState('Aucune entrÃ©e', icon: Icons.history),
                  for (final l in items)
                    ListTileCard(
                      title: Text(str(l['action']) ?? ''),
                      subtitle: Text('${str(l['userName']) ?? ''} Â· ${str(l['userEmail']) ?? ''}\n${str(l['entityType']) ?? ''} Â· ${str(l['ipAddress']) ?? ''} Â· ${fmtDateTime(asDate(l['timestamp']))}'),
                    ),
                  if (loading) const Center(child: Padding(padding: EdgeInsets.all(12), child: CircularProgressIndicator())),
                  if (!loading && page + 1 < totalPages)
                    OutlinedButton(
                      onPressed: () {
                        page++;
                        _load();
                      },
                      child: const Text('Charger plus'),
                    ),
                ]),
        ),
      ),
    ]);
  }
}