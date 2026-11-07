import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../student/student_pages.dart';

// ======================= Dashboard =======================
class ProfessorDashboardPage extends StatelessWidget {
  const ProfessorDashboardPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      load: () async => Map<String, dynamic>.from(await api.get('/api/professor/dashboard') as Map),
      builder: (context, d, reload) {
        final sessions = listOf(d['upcomingSessions'], SessionItem.fromJson);
        return PageList([
          StatGrid([
            StatCard(label: 'MatiÃ¨res', value: '${asInt(d['subjects']) ?? 0}', icon: Icons.category_outlined),
            StatCard(label: 'Cours', value: '${asInt(d['courses']) ?? 0}', icon: Icons.menu_book_outlined, color: EduColors.indigo),
            StatCard(label: 'Documents', value: '${asInt(d['documents']) ?? 0}', icon: Icons.description_outlined, color: EduColors.violet),
            StatCard(label: 'Classes', value: '${asInt(d['classes']) ?? 0}', icon: Icons.groups_outlined, color: EduColors.emerald),
            StatCard(label: 'Ã‰tudiants', value: '${asInt(d['students']) ?? 0}', icon: Icons.people_outline, color: EduColors.sky),
          ]),
          Panel(
            title: 'Prochaines sÃ©ances',
            child: sessions.isEmpty
                ? const EmptyState('Aucune sÃ©ance Ã  venir')
                : Column(children: [
                    for (final s in sessions) Padding(padding: const EdgeInsets.only(bottom: 8), child: sessionTile(context, s, showProfessor: false))
                  ]),
          ),
        ]);
      },
    );
  }
}

// ======================= Subjects =======================
class ProfessorSubjectsPage extends StatelessWidget {
  const ProfessorSubjectsPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<SubjectRef>>(
      load: () async => listOf(await api.get('/api/professor/subjects'), SubjectRef.fromJson),
      builder: (context, items, reload) {
        if (items.isEmpty) return ListView(children: const [EmptyState('Aucune matiÃ¨re affectÃ©e')]);
        return PageList([
          for (final s in items)
            ListTileCard(
              leading: Avatar((s.code ?? s.name).substring(0, 1).toUpperCase()),
              title: Text(s.name),
              subtitle: Text(s.code ?? ''),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (_) => Scaffold(appBar: AppBar(title: Text(s.name)), body: _RosterView(subjectId: s.id)))),
            ),
        ]);
      },
    );
  }
}

class _RosterView extends StatelessWidget {
  final int subjectId;
  const _RosterView({required this.subjectId});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      load: () async => maps(await api.get('/api/professor/subjects/$subjectId/students')),
      builder: (context, items, reload) => PageList([
        if (items.isEmpty) const EmptyState('Aucun Ã©tudiant'),
        for (final s in items)
          ListTileCard(
            leading: Avatar((str(s['studentName']) ?? '?').substring(0, 1).toUpperCase()),
            title: Text(str(s['studentName']) ?? ''),
            subtitle: Text('${str(s['studentNumber']) ?? ''} Â· ${str(s['className']) ?? ''}'),
          ),
      ]),
    );
  }
}

// ======================= Classes =======================
class ProfessorClassesPage extends StatelessWidget {
  const ProfessorClassesPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      load: () async => maps(await api.get('/api/professor/classes')),
      builder: (context, items, reload) {
        if (items.isEmpty) return ListView(children: const [EmptyState('Aucune classe')]);
        return PageList([
          for (final c in items)
            Panel(
              title: str(c['name']) ?? '',
              trailing: Badge2(str(c['academicYear']) ?? '', color: EduColors.indigo),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(str(c['level']) ?? '', style: TextStyle(color: Theme.of(context).hintColor)),
                const SizedBox(height: 8),
                Wrap(spacing: 6, runSpacing: 6, children: [
                  for (final s in maps(c['subjects'])) Badge2(str(s['name']) ?? '', color: EduColors.primary),
                ]),
                const SizedBox(height: 8),
                ExpansionTile(
                  tilePadding: EdgeInsets.zero,
                  title: Text('Ã‰tudiants (${maps(c['students']).length})', style: const TextStyle(fontWeight: FontWeight.w700)),
                  children: [
                    for (final s in maps(c['students']))
                      ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(str(s['name']) ?? ''),
                        subtitle: Text('${str(s['studentNumber']) ?? ''}${(str(s['section']) ?? '').isNotEmpty ? ' Â· ${s['section']}' : ''}'),
                      ),
                  ],
                ),
              ]),
            ),
        ]);
      },
    );
  }
}

// ======================= Courses =======================
class ProfessorCoursesPage extends StatefulWidget {
  const ProfessorCoursesPage({super.key});
  @override
  State<ProfessorCoursesPage> createState() => _ProfessorCoursesPageState();
}

class _ProfessorCoursesPageState extends State<ProfessorCoursesPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _editCourse(context, null),
        icon: const Icon(Icons.add),
        label: const Text('Nouveau cours'),
      ),
      body: AsyncBody<List<Course>>(
        key: k,
        load: () async => listOf(await api.get('/api/professor/courses'), Course.fromJson),
        builder: (context, items, reload) {
          if (items.isEmpty) return ListView(children: const [EmptyState('Aucun cours. CrÃ©ez votre premier cours.', icon: Icons.menu_book_outlined)]);
          return PageList([
            for (final c in items)
              CourseCard(
                c,
                docTrailing: (d) => PopupMenuButton<String>(
                  onSelected: (v) => _docAction(context, d, v),
                  itemBuilder: (_) => const [
                    PopupMenuItem(value: 'open', child: Text('Ouvrir')),
                    PopupMenuItem(value: 'rename', child: Text('Renommer')),
                    PopupMenuItem(value: 'delete', child: Text('Supprimer')),
                  ],
                ),
                actions: Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Wrap(spacing: 8, children: [
                    OutlinedButton.icon(onPressed: () => _upload(context, c), icon: const Icon(Icons.upload_file), label: const Text('Ajouter un document')),
                    OutlinedButton.icon(onPressed: () => _editCourse(context, c), icon: const Icon(Icons.edit_outlined), label: const Text('Modifier')),
                    OutlinedButton.icon(
                      onPressed: () => _deleteCourse(context, c),
                      icon: const Icon(Icons.delete_outline, color: EduColors.red),
                      label: const Text('Supprimer', style: TextStyle(color: EduColors.red)),
                    ),
                  ]),
                ),
              ),
          ]);
        },
      ),
    );
  }

  Future<void> _docAction(BuildContext context, DocInfo d, String v) async {
    final api = apiOf(context);
    if (v == 'open') {
      await openProtectedFile(context, '/api/documents/${d.id}/download', name: d.title);
    } else if (v == 'rename') {
      final t = TextEditingController(text: d.title);
      final ok = await showFormSheet(context,
          title: 'Renommer le document',
          fields: (_) => [Field(t, 'Titre')],
          submit: () => api.put('/api/professor/documents/${d.id}', body: {'title': t.text.trim()}));
      if (ok == true) refresh();
    } else if (v == 'delete') {
      if (!await confirmDialog(context, 'Supprimer le document', 'Supprimer Â« ${d.title} Â» ?', danger: true, ok: 'Supprimer')) return;
      try {
        await api.delete('/api/professor/documents/${d.id}');
        refresh();
      } catch (e) {
        if (context.mounted) toastError(context, e);
      }
    }
  }

  Future<void> _upload(BuildContext context, Course c) async {
    final api = apiOf(context);
    final r = await FilePicker.platform.pickFiles(withData: true);
    final f = r?.files.single;
    if (f == null || f.bytes == null) return;
    if (f.size > 25 * 1024 * 1024) {
      if (context.mounted) toast(context, 'Fichier trop volumineux (25 Mo maximum).', error: true);
      return;
    }
    final t = TextEditingController(text: f.name);
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Ajouter un document',
      action: 'TÃ©lÃ©verser',
      fields: (_) => [Text(f.name), const SizedBox(height: 10), Field(t, 'Titre')],
      submit: () => api.multipart('/api/professor/courses/${c.id}/documents',
          fields: {'title': t.text.trim()}, files: [UploadFile('file', f.name, f.bytes!)]),
    );
    if (ok == true) refresh();
  }

  Future<void> _deleteCourse(BuildContext context, Course c) async {
    if (!await confirmDialog(context, 'Supprimer le cours', 'Supprimer Â« ${c.title} Â» et ses documents ?', danger: true, ok: 'Supprimer')) return;
    try {
      await apiOf(context).delete('/api/professor/courses/${c.id}');
      refresh();
    } catch (e) {
      if (context.mounted) toastError(context, e);
    }
  }

  Future<void> _editCourse(BuildContext context, Course? c) async {
    final api = apiOf(context);
    List<SubjectRef> subjects;
    try {
      subjects = listOf(await api.get('/api/professor/subjects'), SubjectRef.fromJson);
    } catch (e) {
      toastError(context, e);
      return;
    }
    if (subjects.isEmpty) {
      if (context.mounted) toast(context, 'Aucune matiÃ¨re ne vous est affectÃ©e.', error: true);
      return;
    }
    final title = TextEditingController(text: c?.title ?? '');
    final desc = TextEditingController(text: c?.description ?? '');
    int? subjectId = c?.subjectId ?? subjects.first.id;
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: c == null ? 'Nouveau cours' : 'Modifier le cours',
      fields: (set) => [
        Field(title, 'Titre'),
        Field(desc, 'Description', maxLines: 3),
        Dropdown<int>(
          label: 'MatiÃ¨re',
          value: subjectId,
          items: [for (final s in subjects) DropdownMenuItem(value: s.id, child: Text(s.label))],
          onChanged: (v) => set(() => subjectId = v),
        ),
      ],
      submit: () async {
        final body = {'title': title.text.trim(), 'description': desc.text.trim(), 'subjectId': subjectId};
        if (c == null) {
          await api.post('/api/professor/courses', body: body);
        } else {
          await api.put('/api/professor/courses/${c.id}', body: body);
        }
      },
    );
    if (ok == true) refresh();
  }
}

// ======================= Attendance =======================
class ProfessorAttendancePage extends StatefulWidget {
  const ProfessorAttendancePage({super.key});
  @override
  State<ProfessorAttendancePage> createState() => _ProfessorAttendancePageState();
}

class _ProfessorAttendancePageState extends State<ProfessorAttendancePage> {
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      key: k,
      load: () async => Map<String, dynamic>.from(await api.get('/api/professor/attendance/current') as Map),
      builder: (context, d, reload) {
        final open = maps(d['open']);
        final upcoming = maps(d['upcoming']);
        return PageList([
          Panel(
            title: 'SÃ©ances ouvertes Ã  l\'appel',
            child: open.isEmpty
                ? const EmptyState('Aucune sÃ©ance ouverte pour le moment', icon: Icons.how_to_reg_outlined)
                : Column(children: [for (final s in open) _sessionRow(context, s, true)]),
          ),
          Panel(
            title: 'SÃ©ances Ã  venir',
            child: upcoming.isEmpty
                ? const EmptyState('Aucune sÃ©ance Ã  venir aujourd\'hui')
                : Column(children: [for (final s in upcoming) _sessionRow(context, s, false)]),
          ),
          Panel(
            title: 'Historique et bilan',
            child: FilledButton.tonalIcon(
              onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const _AttendanceSummaryPicker())),
              icon: const Icon(Icons.bar_chart),
              label: const Text('Bilan des prÃ©sences par matiÃ¨re'),
            ),
          ),
        ]);
      },
    );
  }

  Widget _sessionRow(BuildContext context, JMap s, bool open) => ListTileCard(
        title: Text('${str(s['subjectName'])} Â· ${str(s['className'])}'),
        subtitle: Text('${hhmm(str(s['startTime']))}â€“${hhmm(str(s['endTime']))}'
            '${(str(s['room']) ?? '').isNotEmpty ? ' Â· Salle ${s['room']}' : ''}'
            '${open ? '' : ' Â· ouvre Ã  ${hhmm(str(s['opensAt']))}'}'),
        trailing: open ? const Icon(Icons.chevron_right) : const Icon(Icons.lock_clock_outlined),
        onTap: open
            ? () => Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (_) => Scaffold(
                        appBar: AppBar(title: Text(str(s['subjectName']) ?? 'Appel')),
                        body: TakeAttendanceView(subjectId: asInt(s['subjectId'])!))))
            : null,
      );
}

class TakeAttendanceView extends StatefulWidget {
  final int subjectId;
  const TakeAttendanceView({super.key, required this.subjectId});
  @override
  State<TakeAttendanceView> createState() => _TakeAttendanceViewState();
}

class _TakeAttendanceViewState extends State<TakeAttendanceView> {
  final status = <int, String>{};
  final justif = <int, TextEditingController>{};
  bool busy = false;
  bool loaded = false;
  Object? error;
  List<JMap> roster = [];
  late final DateTime date = DateTime.now();

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = apiOf(context);
    try {
      roster = maps(await api.get('/api/professor/subjects/${widget.subjectId}/students'));
      final existing = listOf(
          await api.get('/api/professor/subjects/${widget.subjectId}/attendance', query: {'date': isoDate(date)}),
          AttendanceRecord.fromJson);
      for (final r in roster) {
        status[asInt(r['studentId'])!] = 'PRESENT';
      }
      for (final e in existing) {
        if (e.studentId != null) {
          status[e.studentId!] = e.status ?? 'PRESENT';
          justif[e.studentId!] = TextEditingController(text: e.justification ?? '');
        }
      }
    } catch (e) {
      error = e;
    }
    if (mounted) setState(() => loaded = true);
  }

  Future<void> _save() async {
    setState(() => busy = true);
    try {
      await apiOf(context).put('/api/professor/subjects/${widget.subjectId}/attendance', body: {
        'date': isoDate(date),
        'records': [
          for (final r in roster)
            {
              'studentId': asInt(r['studentId']),
              'status': status[asInt(r['studentId'])] ?? 'PRESENT',
              'justification': justif[asInt(r['studentId'])]?.text.trim(),
            }
        ],
      });
      if (mounted) {
        toast(context, 'Appel enregistrÃ©.');
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) toastError(context, e);
    }
    if (mounted) setState(() => busy = false);
  }

  @override
  Widget build(BuildContext context) {
    if (!loaded) return const Center(child: CircularProgressIndicator());
    if (error != null) return ErrorState(error!, () {
      setState(() {
        loaded = false;
        error = null;
      });
      _load();
    });
    return Column(children: [
      Expanded(
        child: PageList([
          Text('Appel du ${fmtDate(date)}', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
          Row(children: [
            TextButton(onPressed: () => setState(() { for (final r in roster) { status[asInt(r['studentId'])!] = 'PRESENT'; } }), child: const Text('Tous prÃ©sents')),
          ]),
          for (final r in roster)
            Builder(builder: (context) {
              final id = asInt(r['studentId'])!;
              final st = status[id] ?? 'PRESENT';
              return Panel(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(str(r['studentName']) ?? '', style: const TextStyle(fontWeight: FontWeight.w800)),
                  Text(str(r['studentNumber']) ?? '', style: TextStyle(fontSize: 12, color: Theme.of(context).hintColor)),
                  const SizedBox(height: 8),
                  SegmentedButton<String>(
                    showSelectedIcon: false,
                    segments: const [
                      ButtonSegment(value: 'PRESENT', label: Text('PrÃ©sent')),
                      ButtonSegment(value: 'LATE', label: Text('Retard')),
                      ButtonSegment(value: 'ABSENT', label: Text('Absent')),
                    ],
                    selected: {st},
                    onSelectionChanged: (s) => setState(() => status[id] = s.first),
                  ),
                  if (st != 'PRESENT') ...[
                    const SizedBox(height: 8),
                    TextField(
                      controller: justif.putIfAbsent(id, () => TextEditingController()),
                      decoration: const InputDecoration(labelText: 'Justification (facultatif)'),
                    ),
                  ],
                ]),
              );
            }),
        ]),
      ),
      SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: busy ? null : _save,
              icon: const Icon(Icons.save_outlined),
              label: const Text('Enregistrer l\'appel'),
            ),
          ),
        ),
      ),
    ]);
  }
}

class _AttendanceSummaryPicker extends StatelessWidget {
  const _AttendanceSummaryPicker();
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Bilan des prÃ©sences')),
      body: AsyncBody<List<SubjectRef>>(
        load: () async => listOf(await api.get('/api/professor/subjects'), SubjectRef.fromJson),
        builder: (context, subjects, reload) => PageList([
          for (final s in subjects)
            ListTileCard(
              title: Text(s.name),
              subtitle: Text(s.code ?? ''),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (_) => Scaffold(appBar: AppBar(title: Text(s.name)), body: _SummaryView(subjectId: s.id)))),
            ),
        ]),
      ),
    );
  }
}

class _SummaryView extends StatelessWidget {
  final int subjectId;
  const _SummaryView({required this.subjectId});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      load: () async => maps(await api.get('/api/professor/subjects/$subjectId/attendance/summary')),
      builder: (context, items, reload) => PageList([
        if (items.isEmpty) const EmptyState('Aucune donnÃ©e'),
        for (final s in items)
          ListTileCard(
            title: Text(str(s['studentName']) ?? ''),
            subtitle: Text('${str(s['studentNumber']) ?? ''}\nPrÃ©sences ${asInt(s['present'])} Â· Absences ${asInt(s['absent'])} Â· Retards ${asInt(s['late'])} / ${asInt(s['total'])}'),
          ),
      ]),
    );
  }
}