import 'package:flutter/material.dart' hide Text;

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

// ======================= Dashboard (GET /api/admin/stats, needs STATS_VIEW_UNIVERSITY) =======================
class AdminDashboardPage extends StatelessWidget {
  const AdminDashboardPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      load: () async => Map<String, dynamic>.from(await api.get('/api/admin/stats') as Map),
      builder: (context, d, reload) {
        final c = Map<String, dynamic>.from(d['counts'] as Map? ?? {});
        final att = Map<String, dynamic>.from(d['attendance'] as Map? ?? {});
        final gr = Map<String, dynamic>.from(d['grades'] as Map? ?? {});
        final avg = asDouble(gr['average']);
        return PageList([
          StatGrid([
            StatCard(label: 'Étudiants', value: '${asInt(c['students']) ?? 0}', icon: Icons.people_outline),
            StatCard(label: 'Étudiants actifs', value: '${asInt(c['activeStudents']) ?? 0}', icon: Icons.verified_user_outlined, color: EduColors.emerald),
            StatCard(label: 'Professeurs', value: '${asInt(c['professors']) ?? 0}', icon: Icons.school_outlined, color: EduColors.indigo),
            StatCard(label: 'Classes', value: '${asInt(c['classes']) ?? 0}', icon: Icons.groups_outlined, color: EduColors.violet),
            StatCard(label: 'Matières', value: '${asInt(c['subjects']) ?? 0}', icon: Icons.category_outlined, color: EduColors.sky),
            StatCard(label: 'Inscriptions actives', value: '${asInt(c['activeEnrollments']) ?? 0}', icon: Icons.how_to_reg_outlined, color: EduColors.orange),
            StatCard(label: 'Cours', value: '${asInt(c['courses']) ?? 0}', icon: Icons.menu_book_outlined),
            StatCard(label: 'Documents', value: '${asInt(c['documents']) ?? 0}', icon: Icons.description_outlined, color: EduColors.violet),
          ]),
          Panel(
            title: 'Inscriptions par classe',
            child: BarList([for (final r in maps(d['enrollmentsByClass'])) (str(r['className']) ?? '', asDouble(r['count']) ?? 0)]),
          ),
          Panel(
            title: 'Présences',
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Taux d\'absence global : ${(asDouble(att['absenceRate']) ?? 0).toStringAsFixed(1)} %',
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              Text('Présents ${asInt(att['present']) ?? 0} · Absents ${asInt(att['absent']) ?? 0} · Retards ${asInt(att['late']) ?? 0}',
                  style: TextStyle(color: Theme.of(context).hintColor)),
              const SizedBox(height: 10),
              BarList([for (final r in maps(att['bySubject'])) (str(r['subjectName']) ?? '', asDouble(r['absenceRate']) ?? 0)],
                  color: EduColors.red, fmt: (v) => '${v.toStringAsFixed(1)} %'),
            ]),
          ),
          Panel(
            title: 'Notes · moyenne ${avg == null ? '—' : avg.toStringAsFixed(2)} (${asInt(gr['count']) ?? 0} notes)',
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Répartition', style: TextStyle(fontWeight: FontWeight.w700)),
              BarList([for (final r in maps(gr['distribution'])) (str(r['label']) ?? '', asDouble(r['count']) ?? 0)], color: EduColors.indigo),
              const SizedBox(height: 10),
              const Text('Moyenne par matière', style: TextStyle(fontWeight: FontWeight.w700)),
              BarList([
                for (final r in maps(gr['bySubject']))
                  if (asDouble(r['average']) != null) (str(r['subjectName']) ?? '', asDouble(r['average'])!)
              ], max: 20, color: EduColors.emerald, fmt: (v) => v.toStringAsFixed(2)),
            ]),
          ),
        ]);
      },
    );
  }
}

// ======================= Generic account list (professors / students) =======================
class _AccountsPage extends StatefulWidget {
  final String path;
  final String createLabel;
  final List<(String, String, TextInputType?)> createFields;
  final String Function(JMap) subtitle;
  final bool isProfessor;
  const _AccountsPage({required this.path, required this.createLabel, required this.createFields, required this.subtitle, this.isProfessor = false});
  @override
  State<_AccountsPage> createState() => _AccountsPageState();
}

class _AccountsPageState extends State<_AccountsPage> {
  Key k = UniqueKey();
  final search = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _create(context), icon: const Icon(Icons.person_add_alt), label: Text(widget.createLabel)),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get(widget.path)),
        builder: (context, all, reload) {
          final q = search.text.trim().toLowerCase();
          final items = all.where((u) => q.isEmpty || '${u['firstName']} ${u['lastName']} ${u['email']} ${widget.subtitle(u)}'.toLowerCase().contains(q)).toList();
          return PageList([
            TextField(controller: search, onChanged: (_) => setState(() {}), decoration: InputDecoration(hintText: tr('Rechercher'), prefixIcon: Icon(Icons.search))),
            if (items.isEmpty) const EmptyState('Aucun compte'),
            for (final u in items)
              ListTileCard(
                leading: Avatar('${(str(u['firstName']) ?? '?')[0]}${(str(u['lastName']) ?? '?')[0]}'.toUpperCase()),
                title: Text('${u['firstName']} ${u['lastName']}'),
                subtitle: Text('${str(u['email']) ?? ''}\n${widget.subtitle(u)}'),
                trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                  PopupMenuButton<String>(
                    child: Badge2(statusFr[u['status']] ?? str(u['status']) ?? '', color: statusColor(str(u['status']) ?? '')),
                    onSelected: (s) async {
                      try {
                        await api.patch('${widget.path}/${u['id']}/status', body: {'status': s});
                        setState(() => k = UniqueKey());
                      } catch (e) {
                        if (context.mounted) toastError(context, e);
                      }
                    },
                    itemBuilder: (_) => [
                      for (final s in const ['ACTIVE', 'INACTIVE', 'SUSPENDED'])
                        if (s != u['status']) PopupMenuItem(value: s, child: Text('Passer à « ${statusFr[s]} »')),
                    ],
                  ),
                  PopupMenuButton<String>(
                    onSelected: (v) => v == 'cin' ? _editCin(context, u) : _editType(context, u),
                    itemBuilder: (_) => [
                      const PopupMenuItem(value: 'cin', child: Text('Modifier le CIN')),
                      if (widget.isProfessor) const PopupMenuItem(value: 'type', child: Text('Changer le type')),
                    ],
                  ),
                ]),
              ),
          ]);
        },
      ),
    );
  }

  Future<void> _editCin(BuildContext context, JMap u) async {
    final api = apiOf(context);
    final c = TextEditingController(text: str(u['cin']) ?? '');
    final ok = await showFormSheet(
      context,
      title: 'Modifier le CIN',
      fields: (_) => [Field(c, 'CIN (8 chiffres)', keyboard: TextInputType.phone)],
      submit: () => api.put('/api/admin/users/${u['id']}/cin', body: {'cin': c.text.trim()}),
    );
    if (ok == true) setState(() => k = UniqueKey());
  }

  Future<void> _editType(BuildContext context, JMap u) async {
    final api = apiOf(context);
    String type = str(u['professorType']) == 'VACATAIRE' ? 'VACATAIRE' : 'PERMANENT';
    final ok = await showFormSheet(
      context,
      title: 'Type de professeur',
      fields: (set) => [
        Dropdown<String>(
          label: 'Type',
          value: type,
          items: const [
            DropdownMenuItem(value: 'PERMANENT', child: Text('Permanent')),
            DropdownMenuItem(value: 'VACATAIRE', child: Text('Vacataire')),
          ],
          onChanged: (v) => set(() => type = v ?? type),
        ),
      ],
      submit: () => api.put('/api/admin/professors/${u['id']}/type', body: {'type': type}),
    );
    if (ok == true) setState(() => k = UniqueKey());
  }

  Future<void> _create(BuildContext context) async {
    final api = apiOf(context);
    final ctrls = {for (final f in widget.createFields) f.$1: TextEditingController()};
    String professorType = 'PERMANENT';
    final ok = await showFormSheet(
      context,
      title: widget.createLabel,
      action: 'Créer le compte',
      fields: (set) => [
        for (final f in widget.createFields) Field(ctrls[f.$1]!, f.$2, keyboard: f.$3),
        if (widget.isProfessor)
          Dropdown<String>(
            label: 'Type',
            value: professorType,
            items: const [
              DropdownMenuItem(value: 'PERMANENT', child: Text('Permanent')),
              DropdownMenuItem(value: 'VACATAIRE', child: Text('Vacataire')),
            ],
            onChanged: (v) => set(() => professorType = v ?? professorType),
          ),
      ],
      submit: () => api.post(widget.path, body: {
        for (final f in widget.createFields)
          f.$1: f.$3 == TextInputType.number ? int.tryParse(ctrls[f.$1]!.text.trim()) : ctrls[f.$1]!.text.trim(),
        if (widget.isProfessor) 'professorType': professorType,
      }),
    );
    if (ok == true) {
      setState(() => k = UniqueKey());
      if (context.mounted) toast(context, 'Compte créé. Les identifiants sont envoyés par e-mail par le serveur.');
    }
  }
}

class AdminProfessorsPage extends StatelessWidget {
  const AdminProfessorsPage({super.key});
  @override
  Widget build(BuildContext context) => _AccountsPage(
        path: '/api/admin/professors',
        createLabel: 'Nouveau professeur',
        createFields: const [
          ('firstName', 'Prénom', null),
          ('lastName', 'Nom', null),
          ('email', 'E-mail', TextInputType.emailAddress),
          ('phone', 'Téléphone', TextInputType.phone),
          ('employeeNumber', 'Matricule', null),
          ('department', 'Département', null),
          ('specialty', 'Spécialité', null),
          ('cin', 'CIN (8 chiffres)', TextInputType.phone),
        ],
        isProfessor: true,
        subtitle: (u) => '${str(u['employeeNumber']) ?? ''} · ${str(u['department']) ?? ''} · ${str(u['specialty']) ?? ''} · ${str(u['professorType']) == 'VACATAIRE' ? 'Vacataire' : 'Permanent'} · CIN ${str(u['cin']) ?? '—'}',
      );
}

class AdminStudentsPage extends StatelessWidget {
  const AdminStudentsPage({super.key});
  @override
  Widget build(BuildContext context) => _AccountsPage(
        path: '/api/admin/students',
        createLabel: 'Nouvel étudiant',
        createFields: const [
          ('firstName', 'Prénom', null),
          ('lastName', 'Nom', null),
          ('email', 'E-mail', TextInputType.emailAddress),
          ('phone', 'Téléphone', TextInputType.phone),
          ('studentNumber', 'N° étudiant', null),
          ('enrollmentYear', 'Année d\'inscription', TextInputType.number),
          ('level', 'Niveau', null),
          ('specialty', 'Spécialité', null),
          ('cin', 'CIN (8 chiffres)', TextInputType.phone),
        ],
        subtitle: (u) => '${str(u['studentNumber']) ?? ''} · ${str(u['level']) ?? ''} · ${str(u['specialty']) ?? ''} · CIN ${str(u['cin']) ?? '—'}',
      );
}

// ======================= Subjects =======================
class AdminSubjectsPage extends StatefulWidget {
  const AdminSubjectsPage({super.key});
  @override
  State<AdminSubjectsPage> createState() => _AdminSubjectsPageState();
}

class _AdminSubjectsPageState extends State<AdminSubjectsPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _edit(context, null), icon: const Icon(Icons.add), label: const Text('Nouvelle matière')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/admin/subjects')),
        builder: (context, items, reload) => PageList([
          if (items.isEmpty) const EmptyState('Aucune matière'),
          for (final s in items)
            Panel(
              title: '${s['name']}',
              trailing: Badge2(str(s['code']) ?? '', color: EduColors.primary),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('Coefficient ${s['coefficient'] ?? '—'} · ${s['credits'] ?? '—'} crédits', style: TextStyle(color: Theme.of(context).hintColor)),
                const SizedBox(height: 8),
                Wrap(spacing: 6, runSpacing: 6, children: [
                  for (final p in maps(s['professors'])) Badge2('${p['firstName']} ${p['lastName']}', color: EduColors.emerald),
                  if (maps(s['professors']).isEmpty) Text('Aucun professeur', style: TextStyle(color: Theme.of(context).hintColor)),
                ]),
                const SizedBox(height: 8),
                Wrap(spacing: 8, children: [
                  OutlinedButton.icon(onPressed: () => _edit(context, s), icon: const Icon(Icons.edit_outlined), label: const Text('Modifier')),
                  OutlinedButton.icon(onPressed: () => _professors(context, s), icon: const Icon(Icons.school_outlined), label: const Text('Professeurs')),
                ]),
              ]),
            ),
        ]),
      ),
    );
  }

  Future<void> _edit(BuildContext context, JMap? s) async {
    final api = apiOf(context);
    final name = TextEditingController(text: str(s?['name']) ?? '');
    final code = TextEditingController(text: str(s?['code']) ?? '');
    final coef = TextEditingController(text: str(s?['coefficient']) ?? '1');
    final credits = TextEditingController(text: str(s?['credits']) ?? '');
    final ok = await showFormSheet(
      context,
      title: s == null ? 'Nouvelle matière' : 'Modifier la matière',
      fields: (_) => [
        Field(name, 'Nom'),
        Field(code, 'Code'),
        Field(coef, 'Coefficient', keyboard: const TextInputType.numberWithOptions(decimal: true)),
        Field(credits, 'Crédits', keyboard: TextInputType.number),
      ],
      submit: () async {
        final body = {
          'name': name.text.trim(),
          'code': code.text.trim(),
          'coefficient': double.tryParse(coef.text.replaceAll(',', '.')),
          'credits': int.tryParse(credits.text.trim()),
        };
        if (s == null) {
          await api.post('/api/admin/subjects', body: body);
        } else {
          await api.put('/api/admin/subjects/${s['id']}', body: body);
        }
      },
    );
    if (ok == true) refresh();
  }

  Future<void> _professors(BuildContext context, JMap s) async {
    final api = apiOf(context);
    List<JMap> profs;
    try {
      profs = maps(await api.get('/api/admin/professors')).where((p) => p['status'] == 'ACTIVE').toList();
    } catch (e) {
      toastError(context, e);
      return;
    }
    final selected = {for (final p in maps(s['professors'])) asInt(p['id'])!};
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Professeurs de « ${s['name']} »',
      fields: (set) => [
        for (final p in profs)
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: selected.contains(asInt(p['id'])),
            title: Text('${p['firstName']} ${p['lastName']}'),
            onChanged: (v) => set(() => v == true ? selected.add(asInt(p['id'])!) : selected.remove(asInt(p['id']))),
          ),
      ],
      submit: () => api.put('/api/admin/subjects/${s['id']}/professors', body: {'ids': selected.toList()}),
    );
    if (ok == true) refresh();
  }
}

// ======================= Classes =======================
class AdminClassesPage extends StatefulWidget {
  const AdminClassesPage({super.key});
  @override
  State<AdminClassesPage> createState() => _AdminClassesPageState();
}

class _AdminClassesPageState extends State<AdminClassesPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _edit(context, null), icon: const Icon(Icons.add), label: const Text('Nouvelle classe')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/admin/classes')),
        builder: (context, items, reload) => PageList([
          if (items.isEmpty) const EmptyState('Aucune classe'),
          for (final c in items)
            ListTileCard(
              leading: Avatar((str(c['name']) ?? '?').substring(0, 1).toUpperCase(), color: EduColors.violet),
              title: Text('${c['name']}'),
              subtitle: Text('${str(c['level']) ?? ''} · ${str(c['academicYear']) ?? ''} · ${maps(c['subjects']).length} matière(s)'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () async {
                await Navigator.push(context, MaterialPageRoute(builder: (_) => ClassManagementPage(classInfo: c)));
                refresh();
              },
            ),
        ]),
      ),
    );
  }

  Future<void> _edit(BuildContext context, JMap? c) async {
    final api = apiOf(context);
    final name = TextEditingController(text: str(c?['name']) ?? '');
    final level = TextEditingController(text: str(c?['level']) ?? '');
    final year = TextEditingController(text: str(c?['academicYear']) ?? '');
    final ok = await showFormSheet(
      context,
      title: c == null ? 'Nouvelle classe' : 'Modifier la classe',
      fields: (_) => [Field(name, 'Nom'), Field(level, 'Niveau'), Field(year, 'Année universitaire', hint: '2025-2026')],
      submit: () async {
        final body = {'name': name.text.trim(), 'level': level.text.trim(), 'academicYear': year.text.trim()};
        if (c == null) {
          await api.post('/api/admin/classes', body: body);
        } else {
          await api.put('/api/admin/classes/${c['id']}', body: body);
        }
      },
    );
    if (ok == true) refresh();
  }
}

/// Class management: /api/admin/class-management/{id}/overview (groups, subjects+professors, students)
class ClassManagementPage extends StatefulWidget {
  final JMap classInfo;
  const ClassManagementPage({super.key, required this.classInfo});
  @override
  State<ClassManagementPage> createState() => _ClassManagementPageState();
}

class _ClassManagementPageState extends State<ClassManagementPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());
  int get id => asInt(widget.classInfo['id'])!;

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      appBar: AppBar(title: Text(str(widget.classInfo['name']) ?? 'Classe'), actions: [
        IconButton(tooltip: tr('Modifier la classe'), icon: const Icon(Icons.edit_outlined), onPressed: () => _editClass(context)),
      ]),
      body: AsyncBody<JMap>(
        key: k,
        load: () async => Map<String, dynamic>.from(await api.get('/api/admin/class-management/$id/overview') as Map),
        builder: (context, o, reload) {
          final groups = maps(o['groups']);
          final subjects = maps(o['subjects']);
          final students = maps(o['students']);
          return PageList([
            Panel(
              title: 'Groupes',
              trailing: TextButton.icon(onPressed: () => _addGroup(context), icon: const Icon(Icons.add), label: const Text('Groupe')),
              child: groups.isEmpty
                  ? const EmptyState('Aucun groupe')
                  : Column(children: [
                      for (final g in groups)
                        ListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          title: Text('${g['name']}'),
                          subtitle: Text('${g['students']} étudiant(s)'),
                          trailing: PopupMenuButton<String>(
                            onSelected: (v) => v == 'rename' ? _renameGroup(context, g) : _deleteGroup(context, g),
                            itemBuilder: (_) => const [
                              PopupMenuItem(value: 'rename', child: Text('Renommer')),
                              PopupMenuItem(value: 'delete', child: Text('Supprimer')),
                            ],
                          ),
                        ),
                    ]),
            ),
            Panel(
              title: 'Matières et professeurs',
              trailing: TextButton.icon(onPressed: () => _assignSubjects(context, subjects), icon: const Icon(Icons.edit_outlined), label: const Text('Matières')),
              child: subjects.isEmpty
                  ? const EmptyState('Aucune matière')
                  : Column(children: [
                      for (final s in subjects)
                        ListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          title: Text('${s['name']}'),
                          subtitle: Text(maps(s['professors']).isEmpty ? 'Aucun professeur' : maps(s['professors']).map((p) => p['name']).join(', ')),
                          trailing: IconButton(icon: const Icon(Icons.person_search_outlined), onPressed: () => _setProfessors(context, s)),
                        ),
                    ]),
            ),
            Panel(
              title: 'Étudiants (${students.length})',
              trailing: TextButton.icon(onPressed: () => _addStudents(context, groups), icon: const Icon(Icons.person_add_alt), label: const Text('Ajouter')),
              child: students.isEmpty
                  ? const EmptyState('Aucun étudiant inscrit')
                  : Column(children: [
                      for (final s in students)
                        ListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          title: Text('${s['name']}'),
                          subtitle: Text('${str(s['studentNumber']) ?? ''} · ${str(s['groupName']) ?? 'Sans groupe'}'),
                          trailing: PopupMenuButton<String>(
                            onSelected: (v) => v == 'group' ? _changeGroup(context, s, groups) : _removeStudent(context, s),
                            itemBuilder: (_) => const [
                              PopupMenuItem(value: 'group', child: Text('Changer de groupe')),
                              PopupMenuItem(value: 'remove', child: Text('Retirer de la classe')),
                            ],
                          ),
                        ),
                    ]),
            ),
          ]);
        },
      ),
    );
  }

  Future<void> _editClass(BuildContext context) async {
    final api = apiOf(context);
    final c = widget.classInfo;
    final name = TextEditingController(text: str(c['name']) ?? '');
    final level = TextEditingController(text: str(c['level']) ?? '');
    final year = TextEditingController(text: str(c['academicYear']) ?? '');
    final ok = await showFormSheet(context,
        title: 'Modifier la classe',
        fields: (_) => [Field(name, 'Nom'), Field(level, 'Niveau'), Field(year, 'Année universitaire')],
        submit: () => api.put('/api/admin/classes/$id', body: {'name': name.text.trim(), 'level': level.text.trim(), 'academicYear': year.text.trim()}));
    if (ok == true) refresh();
  }

  Future<void> _addGroup(BuildContext context) async {
    final api = apiOf(context);
    final n = TextEditingController();
    final ok = await showFormSheet(context,
        title: 'Nouveau groupe', fields: (_) => [Field(n, 'Nom du groupe')], submit: () => api.post('/api/admin/classes/$id/sections', body: {'name': n.text.trim()}));
    if (ok == true) refresh();
  }

  Future<void> _renameGroup(BuildContext context, JMap g) async {
    final api = apiOf(context);
    final n = TextEditingController(text: str(g['name']) ?? '');
    final ok = await showFormSheet(context,
        title: 'Renommer le groupe', fields: (_) => [Field(n, 'Nom du groupe')], submit: () => api.put('/api/admin/class-management/$id/groups/${g['id']}', body: {'name': n.text.trim()}));
    if (ok == true) refresh();
  }

  Future<void> _deleteGroup(BuildContext context, JMap g) async {
    if (!await confirmDialog(context, 'Supprimer le groupe', 'Supprimer « ${g['name']} » ?', danger: true, ok: 'Supprimer')) return;
    try {
      await apiOf(context).delete('/api/admin/class-management/$id/groups/${g['id']}');
      refresh();
    } catch (e) {
      if (context.mounted) toastError(context, e);
    }
  }

  Future<void> _assignSubjects(BuildContext context, List<JMap> current) async {
    final api = apiOf(context);
    List<JMap> all;
    try {
      all = maps(await api.get('/api/admin/subjects'));
    } catch (e) {
      toastError(context, e);
      return;
    }
    final sel = {for (final s in current) asInt(s['id'])!};
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Matières de la classe',
      fields: (set) => [
        for (final s in all)
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: sel.contains(asInt(s['id'])),
            title: Text('${s['name']}'),
            subtitle: Text(str(s['code']) ?? ''),
            onChanged: (v) => set(() => v == true ? sel.add(asInt(s['id'])!) : sel.remove(asInt(s['id']))),
          ),
      ],
      submit: () => api.put('/api/admin/classes/$id/subjects', body: {'ids': sel.toList()}),
    );
    if (ok == true) refresh();
  }

  Future<void> _setProfessors(BuildContext context, JMap s) async {
    final api = apiOf(context);
    List<JMap> profs;
    try {
      profs = maps(await api.get('/api/admin/class-management/professors'));
    } catch (e) {
      toastError(context, e);
      return;
    }
    final sel = {for (final p in maps(s['professors'])) asInt(p['id'])!};
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Professeurs · ${s['name']}',
      fields: (set) => [
        for (final p in profs)
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: sel.contains(asInt(p['id'])),
            title: Text('${p['name']}'),
            onChanged: (v) => set(() => v == true ? sel.add(asInt(p['id'])!) : sel.remove(asInt(p['id']))),
          ),
      ],
      submit: () => api.put('/api/admin/class-management/$id/subjects/${s['id']}/professors', body: {'ids': sel.toList()}),
    );
    if (ok == true) refresh();
  }

  Future<void> _addStudents(BuildContext context, List<JMap> groups) async {
    final api = apiOf(context);
    final q = TextEditingController();
    List<JMap> cands = [];
    final sel = <int>{};
    int? groupId;
    bool loaded = false;
    Future<void> load(StateSetter set) async {
      try {
        cands = maps(await api.get('/api/admin/class-management/$id/candidates', query: {'q': q.text.trim()}));
      } catch (_) {}
      set(() => loaded = true);
    }

    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Ajouter des étudiants',
      action: 'Ajouter',
      fields: (set) {
        if (!loaded) {
          loaded = true;
          load(set);
        }
        return [
          TextField(
            controller: q,
            onSubmitted: (_) => load(set),
            decoration: InputDecoration(
              hintText: tr('Rechercher un étudiant'),
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(icon: const Icon(Icons.arrow_forward), onPressed: () => load(set)),
            ),
          ),
          const SizedBox(height: 8),
          if (groups.isNotEmpty)
            Dropdown<int?>(
              label: 'Groupe (facultatif)',
              value: groupId,
              items: [const DropdownMenuItem<int?>(value: null, child: Text('Sans groupe')), for (final g in groups) DropdownMenuItem<int?>(value: asInt(g['id']), child: Text('${g['name']}'))],
              onChanged: (v) => set(() => groupId = v),
            ),
          SizedBox(
            height: 260,
            child: ListView(children: [
              for (final c in cands)
                CheckboxListTile(
                  contentPadding: EdgeInsets.zero,
                  dense: true,
                  value: sel.contains(asInt(c['id'])),
                  title: Text('${c['name']}'),
                  subtitle: Text('${str(c['studentNumber']) ?? ''} · ${str(c['email']) ?? ''}'),
                  onChanged: (v) => set(() => v == true ? sel.add(asInt(c['id'])!) : sel.remove(asInt(c['id']))),
                ),
            ]),
          ),
        ];
      },
      submit: () async {
        final r = await api.post('/api/admin/class-management/$id/students', body: {'studentIds': sel.toList(), 'groupId': groupId}) as Map;
        if (context.mounted) toast(context, '${r['added']} ajouté(s), ${r['skipped']} ignoré(s).');
      },
    );
    if (ok == true) refresh();
  }

  Future<void> _changeGroup(BuildContext context, JMap s, List<JMap> groups) async {
    final api = apiOf(context);
    int? groupId = asInt(s['groupId']);
    final ok = await showFormSheet(
      context,
      title: 'Groupe de ${s['name']}',
      fields: (set) => [
        Dropdown<int?>(
          label: 'Groupe',
          value: groupId,
          items: [const DropdownMenuItem<int?>(value: null, child: Text('Sans groupe')), for (final g in groups) DropdownMenuItem<int?>(value: asInt(g['id']), child: Text('${g['name']}'))],
          onChanged: (v) => set(() => groupId = v),
        ),
      ],
      submit: () => api.put('/api/admin/class-management/$id/students/${s['studentId']}/group', body: {'groupId': groupId}),
    );
    if (ok == true) refresh();
  }

  Future<void> _removeStudent(BuildContext context, JMap s) async {
    if (!await confirmDialog(context, 'Retirer l\'étudiant', 'Retirer ${s['name']} de cette classe ?', danger: true, ok: 'Retirer')) return;
    try {
      await apiOf(context).delete('/api/admin/class-management/$id/students/${s['studentId']}');
      refresh();
    } catch (e) {
      if (context.mounted) toastError(context, e);
    }
  }
}

// ======================= Enrollments =======================
class AdminEnrollmentsPage extends StatefulWidget {
  const AdminEnrollmentsPage({super.key});
  @override
  State<AdminEnrollmentsPage> createState() => _AdminEnrollmentsPageState();
}

class _AdminEnrollmentsPageState extends State<AdminEnrollmentsPage> {
  Key k = UniqueKey();
  int? classId;
  List<JMap> classes = [];
  bool loadedClasses = false;
  static const enrFr = {'ACTIVE': 'Active', 'COMPLETED': 'Terminée', 'CANCELLED': 'Annulée'};

  @override
  void initState() {
    super.initState();
    apiOf(context).get('/api/admin/classes').then((v) {
      if (mounted) setState(() {
        classes = maps(v);
        loadedClasses = true;
      });
    }).catchError((_) {
      if (mounted) setState(() => loadedClasses = true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _create(context), icon: const Icon(Icons.add), label: const Text('Inscrire')),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: DropdownButtonFormField<int?>(
            value: classId,
            isExpanded: true,
            decoration: InputDecoration(labelText: tr('Filtrer par classe'), isDense: true),
            items: [const DropdownMenuItem<int?>(value: null, child: Text('Toutes les classes')), for (final c in classes) DropdownMenuItem<int?>(value: asInt(c['id']), child: Text('${c['name']}'))],
            onChanged: (v) => setState(() {
              classId = v;
              k = UniqueKey();
            }),
          ),
        ),
        Expanded(
          child: AsyncBody<List<JMap>>(
            key: k,
            load: () async => maps(await api.get('/api/admin/enrollments', query: {'classId': classId})),
            builder: (context, items, reload) => PageList([
              if (items.isEmpty) const EmptyState('Aucune inscription'),
              for (final e in items)
                ListTileCard(
                  title: Text(str(e['studentName']) ?? ''),
                  subtitle: Text('${str(e['className']) ?? ''}${(str(e['sectionName']) ?? '').isNotEmpty ? ' · ${e['sectionName']}' : ''} · ${str(e['academicYear']) ?? ''}\nInscrit le ${fmtDate(asDate(e['enrollmentDate']))}'),
                  trailing: PopupMenuButton<String>(
                    child: Badge2(enrFr[e['status']] ?? str(e['status']) ?? '', color: statusColor(str(e['status']) ?? '')),
                    onSelected: (s) async {
                      try {
                        await api.patch('/api/admin/enrollments/${e['id']}/status', body: {'status': s});
                        await reload();
                      } catch (err) {
                        if (context.mounted) toastError(context, err);
                      }
                    },
                    itemBuilder: (_) => [for (final s in enrFr.keys) if (s != e['status']) PopupMenuItem(value: s, child: Text('Passer à « ${enrFr[s]} »'))],
                  ),
                ),
            ]),
          ),
        ),
      ]),
    );
  }

  Future<void> _create(BuildContext context) async {
    final api = apiOf(context);
    List<JMap> students;
    try {
      students = maps(await api.get('/api/admin/students')).where((s) => s['status'] == 'ACTIVE').toList();
    } catch (e) {
      toastError(context, e);
      return;
    }
    int? studentId;
    int? cls = classId;
    int? sectionId;
    List<JMap> sections = [];
    DateTime date = DateTime.now();
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Nouvelle inscription',
      action: 'Inscrire',
      fields: (set) => [
        Dropdown<int>(
          label: 'Étudiant',
          value: studentId,
          items: [for (final s in students) DropdownMenuItem(value: asInt(s['id']), child: Text('${s['firstName']} ${s['lastName']} (${str(s['studentNumber']) ?? ''})', overflow: TextOverflow.ellipsis))],
          onChanged: (v) => set(() => studentId = v),
        ),
        Dropdown<int>(
          label: 'Classe',
          value: cls,
          items: [for (final c in classes) DropdownMenuItem(value: asInt(c['id']), child: Text('${c['name']}'))],
          onChanged: (v) async {
            set(() {
              cls = v;
              sectionId = null;
              sections = [];
            });
            try {
              final s = maps(await api.get('/api/admin/classes/$v/sections'));
              set(() => sections = s);
            } catch (_) {}
          },
        ),
        if (sections.isNotEmpty)
          Dropdown<int?>(
            label: 'Groupe (facultatif)',
            value: sectionId,
            items: [const DropdownMenuItem<int?>(value: null, child: Text('Sans groupe')), for (final s in sections) DropdownMenuItem<int?>(value: asInt(s['id']), child: Text('${s['name']}'))],
            onChanged: (v) => set(() => sectionId = v),
          ),
        OutlinedButton.icon(
          onPressed: () async {
            final d = await pickDate(context, initial: date);
            if (d != null) set(() => date = d);
          },
          icon: const Icon(Icons.event),
          label: Text('Date d\'inscription : ${fmtDate(date)}'),
        ),
        const SizedBox(height: 12),
      ],
      submit: () => api.post('/api/admin/enrollments', body: {
        'studentId': studentId,
        'classId': cls,
        'sectionId': sectionId,
        'enrollmentDate': isoDate(date),
      }),
    );
    if (ok == true) setState(() => k = UniqueKey());
  }
}
