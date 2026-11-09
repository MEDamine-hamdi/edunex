import 'dart:async';

import 'package:flutter/material.dart' hide Text;

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

String _roleName(String? r) => switch (r) {
      'ROLE_PROFESSOR' => 'Professeur',
      'ROLE_ADMIN_AGENT' => 'Agent',
      _ => r ?? '',
    };

/// Platform administrator: give or take rights from one specific person.
/// GET /api/platform/user-rights/users?q= · GET/PUT /api/platform/user-rights/users/{id}
class PlatformUserRightsPage extends StatefulWidget {
  const PlatformUserRightsPage({super.key});
  @override
  State<PlatformUserRightsPage> createState() => _PlatformUserRightsPageState();
}

class _PlatformUserRightsPageState extends State<PlatformUserRightsPage> {
  final search = TextEditingController();
  List<JMap> hits = [];
  Object? error;
  Timer? debounce;

  @override
  void initState() {
    super.initState();
    _search();
  }

  @override
  void dispose() {
    debounce?.cancel();
    search.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    try {
      final r = maps(await apiOf(context).get('/api/platform/user-rights/users', query: {'q': search.text.trim()}));
      if (mounted) setState(() { hits = r; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
        child: TextField(
          controller: search,
          onChanged: (_) {
            debounce?.cancel();
            debounce = Timer(const Duration(milliseconds: 250), _search);
          },
          decoration: InputDecoration(hintText: tr('Rechercher un professeur ou un agent'), prefixIcon: const Icon(Icons.search)),
        ),
      ),
      Expanded(
        child: error != null && hits.isEmpty
            ? ErrorState(error!, _search)
            : hits.isEmpty
                ? ListView(children: const [EmptyState('Aucune personne trouvée', icon: Icons.person_search_outlined)])
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                    itemCount: hits.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (_, i) {
                      final h = hits[i];
                      return ListTileCard(
                        leading: Avatar(_initials(str(h['name']) ?? '')),
                        title: Text(str(h['name']) ?? ''),
                        subtitle: Text('${_roleName(str(h['role']))} · ${str(h['email']) ?? ''}'),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => _UserRightsDetailPage(id: asInt(h['id'])!, name: str(h['name']) ?? ''))),
                      );
                    },
                  ),
      ),
    ]);
  }
}

String _initials(String name) => name.split(' ').where((p) => p.isNotEmpty).take(2).map((p) => p[0].toUpperCase()).join();

class _UserRightsDetailPage extends StatefulWidget {
  final int id;
  final String name;
  const _UserRightsDetailPage({required this.id, required this.name});
  @override
  State<_UserRightsDetailPage> createState() => _UserRightsDetailPageState();
}

class _UserRightsDetailPageState extends State<_UserRightsDetailPage> {
  JMap? view;
  Object? error;
  Map<String, String> draft = {};
  bool saving = false;

  String get path => '/api/platform/user-rights/users/${widget.id}';

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _set(JMap v) {
    view = v;
    draft = {for (final r in maps(v['rights'])) str(r['code']) ?? '': str(r['override']) ?? ''};
  }

  Future<void> _load() async {
    try {
      final v = Map<String, dynamic>.from(await apiOf(context).get(path) as Map);
      if (mounted) setState(() { _set(v); error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _save() async {
    setState(() => saving = true);
    try {
      final v = Map<String, dynamic>.from(await apiOf(context).put(path, body: {
        'overrides': [for (final e in draft.entries) {'code': e.key, 'effect': e.value}],
      }) as Map);
      if (!mounted) return;
      setState(() { _set(v); saving = false; });
      toast(context, "Droits enregistrés. Ils s'appliquent dès la prochaine action de la personne.");
    } catch (e) {
      if (mounted) { setState(() => saving = false); toastError(context, e); }
    }
  }

  bool _result(JMap r) {
    final d = draft[str(r['code']) ?? ''];
    if (d == 'GRANT') return true;
    if (d == 'DENY') return false;
    return r['fromRole'] == true;
  }

  @override
  Widget build(BuildContext context) {
    final v = view;
    return Scaffold(
      appBar: AppBar(title: Text(widget.name)),
      body: error != null && v == null
          ? ErrorState(error!, () { setState(() => error = null); _load(); })
          : v == null
              ? const Center(child: CircularProgressIndicator())
              : PageList([
                  Panel(
                    child: Row(children: [
                      Avatar(_initials(str(v['name']) ?? ''), size: 48),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(str(v['name']) ?? '', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                          Text('${_roleName(str(v['role']))} · ${str(v['email']) ?? ''}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
                        ]),
                      ),
                    ]),
                  ),
                  for (final r in maps(v['rights']))
                    Panel(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [
                          Expanded(child: Text(str(r['description']) ?? '', style: const TextStyle(fontWeight: FontWeight.w700))),
                          Badge2(_result(r) ? 'Oui' : 'Non', color: _result(r) ? EduColors.emerald : EduColors.slate500),
                        ]),
                        Text('${str(r['code']) ?? ''} · rôle : ${r['fromRole'] == true ? 'oui' : 'non'}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12)),
                        const SizedBox(height: 8),
                        DropdownButtonFormField<String>(
                          value: draft[str(r['code']) ?? ''] ?? '',
                          isExpanded: true,
                          decoration: const InputDecoration(isDense: true),
                          items: [
                            const DropdownMenuItem(value: '', child: Text('Par défaut')),
                            const DropdownMenuItem(value: 'GRANT', child: Text('Autoriser')),
                            const DropdownMenuItem(value: 'DENY', child: Text('Refuser')),
                          ],
                          onChanged: (x) => setState(() => draft[str(r['code']) ?? ''] = x ?? ''),
                        ),
                      ]),
                    ),
                  FilledButton(
                    onPressed: saving ? null : _save,
                    child: saving ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Text('Enregistrer'),
                  ),
                ]),
    );
  }
}
