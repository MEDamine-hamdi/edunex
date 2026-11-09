import 'dart:async';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../shared/notifications_page.dart';
import '../shared/timetable_pages.dart';

// ======================= Timetables =======================
class AdminTimetablesPage extends StatefulWidget {
  const AdminTimetablesPage({super.key});
  @override
  State<AdminTimetablesPage> createState() => _AdminTimetablesPageState();
}

class _AdminTimetablesPageState extends State<AdminTimetablesPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _create(context), icon: const Icon(Icons.add), label: const Text('Nouvel emploi du temps')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/admin/timetables')),
        builder: (context, items, reload) => PageList([
          Row(children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => Scaffold(appBar: AppBar(title: const Text('Semaine')), body: const _AdminWeekView()))),
                icon: const Icon(Icons.view_week_outlined),
                label: const Text('Vue semaine'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const TimetableImportPage())).then((_) => refresh()),
                icon: const Icon(Icons.picture_as_pdf_outlined),
                label: const Text('Importer un PDF'),
              ),
            ),
          ]),
          if (items.isEmpty) const EmptyState('Aucun emploi du temps'),
          for (final t in items)
            ListTileCard(
              leading: const Icon(Icons.calendar_month_outlined, color: EduColors.primary),
              title: Text('${t['academicYear']} · ${t['semester']}'),
              subtitle: Text('${t['sessionCount']} séance(s)'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TimetableSessionsPage(timetable: t))).then((_) => refresh()),
            ),
        ]),
      ),
    );
  }

  Future<void> _create(BuildContext context) async {
    final api = apiOf(context);
    final year = TextEditingController();
    final sem = TextEditingController();
    final ok = await showFormSheet(context,
        title: 'Nouvel emploi du temps',
        fields: (_) => [Field(year, 'Année universitaire', hint: '2025-2026'), Field(sem, 'Semestre', hint: 'S1')],
        submit: () => api.post('/api/admin/timetables', body: {'academicYear': year.text.trim(), 'semester': sem.text.trim()}));
    if (ok == true) refresh();
  }
}

class _AdminWeekView extends StatefulWidget {
  const _AdminWeekView();
  @override
  State<_AdminWeekView> createState() => _AdminWeekViewState();
}

class _AdminWeekViewState extends State<_AdminWeekView> {
  List<JMap> classes = [];
  int? classId;
  @override
  void initState() {
    super.initState();
    apiOf(context).get('/api/admin/classes').then((v) {
      if (mounted) setState(() => classes = maps(v));
    }).catchError((_) {});
  }

  @override
  Widget build(BuildContext context) => Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: DropdownButtonFormField<int?>(
            value: classId,
            isExpanded: true,
            decoration: InputDecoration(labelText: tr('Classe'), isDense: true),
            items: [const DropdownMenuItem<int?>(value: null, child: Text('Toutes les classes')), for (final c in classes) DropdownMenuItem<int?>(value: asInt(c['id']), child: Text('${c['name']}'))],
            onChanged: (v) => setState(() => classId = v),
          ),
        ),
        Expanded(child: WeekTimetableView(path: '/api/admin/timetable/week', query: {'classId': classId}, mode: WeekMode.readOnly)),
      ]);
}

class TimetableSessionsPage extends StatefulWidget {
  final JMap timetable;
  const TimetableSessionsPage({super.key, required this.timetable});
  @override
  State<TimetableSessionsPage> createState() => _TimetableSessionsPageState();
}

class _TimetableSessionsPageState extends State<TimetableSessionsPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());
  int get id => asInt(widget.timetable['id'])!;

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      appBar: AppBar(title: Text('${widget.timetable['academicYear']} · ${widget.timetable['semester']}')),
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _edit(context, null), icon: const Icon(Icons.add), label: const Text('Séance')),
      body: AsyncBody<List<SessionItem>>(
        key: k,
        load: () async => listOf(await api.get('/api/admin/timetables/$id/sessions'), SessionItem.fromJson),
        builder: (context, items, reload) {
          items.sort((a, b) {
            final d = dayOrder.indexOf(a.dayOfWeek).compareTo(dayOrder.indexOf(b.dayOfWeek));
            return d != 0 ? d : a.startTime.compareTo(b.startTime);
          });
          return PageList([
            if (items.isEmpty) const EmptyState('Aucune séance'),
            for (final s in items)
              ListTileCard(
                title: Text('${dayFr[s.dayOfWeek] ?? s.dayOfWeek} ${hhmm(s.startTime)}–${hhmm(s.endTime)}'),
                subtitle: Text('${s.subjectName ?? ''} · ${s.className ?? ''}\n${s.professorName ?? ''}${(s.room ?? '').isNotEmpty ? ' · Salle ${s.room}' : ''}'),
                trailing: PopupMenuButton<String>(
                  onSelected: (v) async {
                    if (v == 'edit') {
                      _edit(context, s);
                    } else {
                      if (!await confirmDialog(context, 'Supprimer la séance', 'Supprimer cette séance ?', danger: true, ok: 'Supprimer')) return;
                      try {
                        await api.delete('/api/admin/sessions/${s.id}');
                        refresh();
                      } catch (e) {
                        if (context.mounted) toastError(context, e);
                      }
                    }
                  },
                  itemBuilder: (_) => const [PopupMenuItem(value: 'edit', child: Text('Modifier')), PopupMenuItem(value: 'delete', child: Text('Supprimer'))],
                ),
              ),
          ]);
        },
      ),
    );
  }

  Future<void> _edit(BuildContext context, SessionItem? s) async {
    final api = apiOf(context);
    List<JMap> classes, subjects, profs;
    try {
      classes = maps(await api.get('/api/admin/classes'));
      subjects = maps(await api.get('/api/admin/subjects'));
      profs = maps(await api.get('/api/admin/professors'));
    } catch (e) {
      toastError(context, e);
      return;
    }
    String day = s?.dayOfWeek ?? 'MONDAY';
    TimeOfDay start = _parse(s?.startTime) ?? const TimeOfDay(hour: 8, minute: 0);
    TimeOfDay end = _parse(s?.endTime) ?? const TimeOfDay(hour: 10, minute: 0);
    int? classId = s?.classId, subjectId = s?.subjectId, profId = s?.professorId;
    final room = TextEditingController(text: s?.room ?? '');
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: s == null ? 'Nouvelle séance' : 'Modifier la séance',
      fields: (set) => [
        Dropdown<String>(label: 'Jour', value: day, items: [for (final d in dayOrder) DropdownMenuItem(value: d, child: Text(dayFr[d]!))], onChanged: (v) => set(() => day = v ?? day)),
        Row(children: [
          Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: start); if (t != null) set(() => start = t); }, child: Text('Début ${hhmmOf(start).substring(0, 5)}'))),
          const SizedBox(width: 8),
          Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: end); if (t != null) set(() => end = t); }, child: Text('Fin ${hhmmOf(end).substring(0, 5)}'))),
        ]),
        const SizedBox(height: 12),
        Field(room, 'Salle'),
        Dropdown<int>(label: 'Classe', value: classId, items: [for (final c in classes) DropdownMenuItem(value: asInt(c['id']), child: Text('${c['name']}'))], onChanged: (v) => set(() => classId = v)),
        Dropdown<int>(label: 'Matière', value: subjectId, items: [for (final c in subjects) DropdownMenuItem(value: asInt(c['id']), child: Text('${c['name']}'))], onChanged: (v) => set(() => subjectId = v)),
        Dropdown<int>(label: 'Professeur', value: profId, items: [for (final c in profs) DropdownMenuItem(value: asInt(c['id']), child: Text('${c['firstName']} ${c['lastName']}'))], onChanged: (v) => set(() => profId = v)),
      ],
      submit: () async {
        final body = {
          'dayOfWeek': day,
          'startTime': hhmmOf(start),
          'endTime': hhmmOf(end),
          'room': room.text.trim(),
          'classId': classId,
          'subjectId': subjectId,
          'professorId': profId,
        };
        if (s == null) {
          await api.post('/api/admin/timetables/$id/sessions', body: body);
        } else {
          await api.put('/api/admin/sessions/${s.id}', body: body);
        }
      },
    );
    if (ok == true) refresh();
  }

  TimeOfDay? _parse(String? t) {
    if (t == null || t.length < 5) return null;
    return TimeOfDay(hour: int.tryParse(t.substring(0, 2)) ?? 8, minute: int.tryParse(t.substring(3, 5)) ?? 0);
  }
}

/// PDF timetable import: POST /api/admin/timetable-imports, poll GET /{id}, confirm POST /{id}/confirm.
class TimetableImportPage extends StatefulWidget {
  const TimetableImportPage({super.key});
  @override
  State<TimetableImportPage> createState() => _TimetableImportPageState();
}

class _TimetableImportPageState extends State<TimetableImportPage> {
  List<JMap> timetables = [], classes = [];
  int? timetableId, classId;
  PlatformFile? file;
  JMap? job;
  String? jobId;
  Timer? timer;
  bool busy = false;
  String? error;
  final include = <int, bool>{};

  @override
  void initState() {
    super.initState();
    final api = apiOf(context);
    Future(() async {
      try {
        timetables = maps(await api.get('/api/admin/timetables'));
        classes = maps(await api.get('/api/admin/classes'));
      } catch (e) {
        error = e.toString();
      }
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  Future<void> _start() async {
    if (file == null || timetableId == null) {
      setState(() => error = 'Choisissez un emploi du temps et un fichier PDF.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    final api = apiOf(context);
    try {
      final r = Map<String, dynamic>.from(await api.multipart('/api/admin/timetable-imports',
          fields: {'timetableId': '$timetableId', if (classId != null) 'classId': '$classId'},
          files: [UploadFile('file', file!.name, file!.bytes!, contentType: 'application/pdf')]) as Map);
      jobId = str(r['id']);
      job = r;
      timer = Timer.periodic(const Duration(seconds: 2), (_) => _poll());
    } catch (e) {
      error = e.toString();
      busy = false;
    }
    if (mounted) setState(() {});
  }

  Future<void> _poll() async {
    try {
      final r = Map<String, dynamic>.from(await apiOf(context).get('/api/admin/timetable-imports/$jobId') as Map);
      if (!mounted) return;
      setState(() => job = r);
      if (r['status'] != 'PROCESSING') {
        timer?.cancel();
        busy = false;
        final rows = maps(r['rows']);
        for (final row in rows) {
          include[asInt(row['id'])!] = row['include'] == true;
        }
        setState(() {});
      }
    } catch (e) {
      timer?.cancel();
      if (mounted) setState(() {
        error = e.toString();
        busy = false;
      });
    }
  }

  Future<void> _confirm() async {
    final rows = maps(job!['rows']).where((r) => include[asInt(r['id'])] == true && r['classId'] != null && r['subjectId'] != null && r['professorId'] != null).toList();
    if (rows.isEmpty) {
      setState(() => error = 'Aucune ligne complète sélectionnée.');
      return;
    }
    final tt = timetables.firstWhere((t) => asInt(t['id']) == timetableId);
    setState(() => busy = true);
    try {
      final r = Map<String, dynamic>.from(await apiOf(context).post('/api/admin/timetable-imports/$jobId/confirm', body: {
        'timetableId': timetableId,
        'academicYear': tt['academicYear'],
        'semester': tt['semester'],
        'rows': [
          for (final x in rows)
            {'classId': x['classId'], 'subjectId': x['subjectId'], 'professorId': x['professorId'], 'day': x['day'], 'start': x['start'], 'end': x['end'], 'room': x['room']}
        ],
      }) as Map);
      if (mounted) {
        toast(context, '${r['created']} séance(s) créée(s), ${r['failed']} échec(s).');
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) setState(() {
        error = e.toString();
        busy = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final done = job != null && job!['status'] == 'DONE';
    return Scaffold(
      appBar: AppBar(title: const Text('Importer un emploi du temps')),
      body: PageList([
        if (job == null) ...[
          Panel(
            child: Column(children: [
              Dropdown<int>(label: 'Emploi du temps cible', value: timetableId, items: [for (final t in timetables) DropdownMenuItem(value: asInt(t['id']), child: Text('${t['academicYear']} · ${t['semester']}'))], onChanged: (v) => setState(() => timetableId = v)),
              Dropdown<int?>(label: 'Classe (facultatif)', value: classId, items: [const DropdownMenuItem<int?>(value: null, child: Text('Détection automatique')), for (final c in classes) DropdownMenuItem<int?>(value: asInt(c['id']), child: Text('${c['name']}'))], onChanged: (v) => setState(() => classId = v)),
              OutlinedButton.icon(
                onPressed: () async {
                  final r = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: const ['pdf'], withData: true);
                  if (r?.files.single.bytes != null) setState(() => file = r!.files.single);
                },
                icon: const Icon(Icons.picture_as_pdf_outlined),
                label: Text(file?.name ?? 'Choisir le PDF'),
              ),
              const SizedBox(height: 12),
              SizedBox(width: double.infinity, child: FilledButton(onPressed: busy ? null : _start, child: const Text('Analyser le document'))),
            ]),
          ),
        ] else if (!done && job!['status'] == 'PROCESSING')
          Panel(
            child: Column(children: [
              const CircularProgressIndicator(),
              const SizedBox(height: 12),
              Text('Analyse en cours… page ${asInt(job!['donePages']) ?? 0} / ${asInt(job!['totalPages']) ?? 0}'),
            ]),
          )
        else if (job!['status'] == 'FAILED')
          Panel(child: Text(str(job!['message']) ?? 'L\'analyse a échoué.', style: const TextStyle(color: EduColors.red)))
        else ...[
          if ((str(job!['message']) ?? '').isNotEmpty) Panel(child: Text(str(job!['message'])!)),
          for (final w in (job!['warnings'] as List? ?? const [])) Text('⚠ $w', style: const TextStyle(color: EduColors.orange)),
          for (final r in maps(job!['rows']))
            Panel(
              child: CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                value: include[asInt(r['id'])] ?? false,
                onChanged: (r['classId'] == null || r['subjectId'] == null || r['professorId'] == null) ? null : (v) => setState(() => include[asInt(r['id'])!] = v ?? false),
                title: Text('${str(r['day'])} ${str(r['start'])}–${str(r['end'])} · ${str(r['subjectName']) ?? str(r['rawSubject']) ?? ''}'),
                subtitle: Text([
                  'Classe : ${str(r['className']) ?? '⚠ ${str(r['rawClass']) ?? '?'}'}',
                  'Prof : ${str(r['professorName']) ?? '⚠ ${str(r['rawProfessor']) ?? '?'}'}',
                  if ((str(r['room']) ?? '').isNotEmpty) 'Salle ${r['room']}',
                  if ((str(r['conflict']) ?? '').isNotEmpty) 'Conflit : ${r['conflict']}',
                  for (final i in (r['issues'] as List? ?? const [])) '$i',
                ].join('\n')),
              ),
            ),
          if (maps(job!['rows']).isNotEmpty) FilledButton(onPressed: busy ? null : _confirm, child: const Text('Créer les séances sélectionnées')),
        ],
        if (error != null) Text(error!, style: const TextStyle(color: EduColors.red, fontWeight: FontWeight.w600)),
      ]),
    );
  }
}

// ======================= Grades =======================
class AdminGradesPage extends StatefulWidget {
  const AdminGradesPage({super.key});
  @override
  State<AdminGradesPage> createState() => _AdminGradesPageState();
}

class _AdminGradesPageState extends State<AdminGradesPage> {
  Key k = UniqueKey();
  int? subjectId;
  List<JMap> subjects = [];

  @override
  void initState() {
    super.initState();
    apiOf(context).get('/api/admin/subjects').then((v) {
      if (mounted) setState(() => subjects = maps(v));
    }).catchError((_) {});
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _edit(context, null), icon: const Icon(Icons.add), label: const Text('Saisir une note')),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: DropdownButtonFormField<int?>(
            value: subjectId,
            isExpanded: true,
            decoration: InputDecoration(labelText: tr('Matière'), isDense: true),
            items: [const DropdownMenuItem<int?>(value: null, child: Text('Toutes les matières')), for (final s in subjects) DropdownMenuItem<int?>(value: asInt(s['id']), child: Text('${s['name']}'))],
            onChanged: (v) => setState(() {
              subjectId = v;
              k = UniqueKey();
            }),
          ),
        ),
        Expanded(
          child: AsyncBody<List<Grade>>(
            key: k,
            load: () async => listOf(await api.get('/api/admin/grades', query: {'subjectId': subjectId}), Grade.fromJson),
            builder: (context, items, reload) => PageList([
              if (items.isEmpty) const EmptyState('Aucune note'),
              for (final g in items)
                ListTileCard(
                  title: Text(g.studentName ?? ''),
                  subtitle: Text('${g.subjectName ?? ''} · ${gradeTypeFr[g.evaluationType] ?? g.evaluationType ?? ''} · ${fmtDate(g.date)}'),
                  trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                    Text(num1(g.value), style: TextStyle(fontWeight: FontWeight.w900, fontSize: 16, color: (g.value ?? 0) >= 10 ? EduColors.emerald : EduColors.red)),
                    PopupMenuButton<String>(
                      onSelected: (v) async {
                        if (v == 'edit') {
                          _edit(context, g);
                        } else {
                          if (!await confirmDialog(context, 'Supprimer la note', 'Supprimer cette note ?', danger: true, ok: 'Supprimer')) return;
                          try {
                            await api.delete('/api/admin/grades/${g.id}');
                            await reload();
                          } catch (e) {
                            if (context.mounted) toastError(context, e);
                          }
                        }
                      },
                      itemBuilder: (_) => const [PopupMenuItem(value: 'edit', child: Text('Modifier')), PopupMenuItem(value: 'delete', child: Text('Supprimer'))],
                    ),
                  ]),
                ),
            ]),
          ),
        ),
      ]),
    );
  }

  Future<void> _edit(BuildContext context, Grade? g) async {
    final api = apiOf(context);
    int? sId = g?.subjectId ?? subjectId;
    int? stId = g?.studentId;
    List<JMap> roster = [];
    String type = g?.evaluationType ?? 'EXAM';
    DateTime date = g?.date ?? DateTime.now();
    final value = TextEditingController(text: g?.value?.toString() ?? '');
    if (g == null && subjects.isEmpty) {
      toast(context, 'Aucune matière disponible.', error: true);
      return;
    }
    Future<void> loadRoster(StateSetter set) async {
      if (sId == null) return;
      try {
        final r = maps(await api.get('/api/admin/subjects/$sId/students'));
        set(() => roster = r);
      } catch (_) {}
    }

    bool first = true;
    final ok = await showFormSheet(
      context,
      title: g == null ? 'Saisir une note' : 'Modifier la note',
      fields: (set) {
        if (first && g == null) {
          first = false;
          loadRoster(set);
        }
        return [
          if (g == null) ...[
            Dropdown<int>(
              label: 'Matière',
              value: sId,
              items: [for (final s in subjects) DropdownMenuItem(value: asInt(s['id']), child: Text('${s['name']}'))],
              onChanged: (v) {
                set(() {
                  sId = v;
                  stId = null;
                  roster = [];
                });
                loadRoster(set);
              },
            ),
            Dropdown<int>(
              label: 'Étudiant',
              value: stId,
              items: [for (final r in roster) DropdownMenuItem(value: asInt(r['studentId']), child: Text('${r['studentName']} (${str(r['studentNumber']) ?? ''})', overflow: TextOverflow.ellipsis))],
              onChanged: (v) => set(() => stId = v),
            ),
          ] else
            Padding(padding: const EdgeInsets.only(bottom: 12), child: Text('${g.studentName} · ${g.subjectName}', style: const TextStyle(fontWeight: FontWeight.w700))),
          Field(value, 'Note (0 – 20)', keyboard: const TextInputType.numberWithOptions(decimal: true)),
          Dropdown<String>(label: 'Type d\'évaluation', value: type, items: [for (final e in gradeTypeFr.entries) DropdownMenuItem(value: e.key, child: Text(e.value))], onChanged: (v) => set(() => type = v ?? type)),
          OutlinedButton.icon(
            onPressed: () async {
              final d = await pickDate(context, initial: date);
              if (d != null) set(() => date = d);
            },
            icon: const Icon(Icons.event),
            label: Text(fmtDate(date)),
          ),
          const SizedBox(height: 12),
        ];
      },
      submit: () async {
        final v = double.tryParse(value.text.replaceAll(',', '.'));
        if (g == null) {
          await api.post('/api/admin/grades', body: {'studentId': stId, 'subjectId': sId, 'value': v, 'evaluationType': type, 'date': isoDate(date)});
        } else {
          await api.put('/api/admin/grades/${g.id}', body: {'value': v, 'evaluationType': type, 'date': isoDate(date)});
        }
      },
    );
    if (ok == true) setState(() => k = UniqueKey());
  }
}

// ======================= Announcements =======================
class AdminAnnouncementsPage extends StatefulWidget {
  const AdminAnnouncementsPage({super.key});
  @override
  State<AdminAnnouncementsPage> createState() => _AdminAnnouncementsPageState();
}

class _AdminAnnouncementsPageState extends State<AdminAnnouncementsPage> {
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _publish(context), icon: const Icon(Icons.campaign_outlined), label: const Text('Publier')),
      body: AsyncBody<List<Announcement>>(
        key: k,
        load: () async => listOf(await api.get('/api/admin/announcements'), Announcement.fromJson),
        builder: (context, items, reload) => PageList([
          if (items.isEmpty) const EmptyState('Aucune annonce publiée', icon: Icons.campaign_outlined),
          for (final a in items) AnnouncementCard(a, showAudience: true),
        ]),
      ),
    );
  }

  Future<void> _publish(BuildContext context) async {
    final api = apiOf(context);
    List<JMap> classes = [];
    try {
      classes = maps(await api.get('/api/admin/classes'));
    } catch (_) {}
    final title = TextEditingController();
    final content = TextEditingController();
    String audience = 'ALL';
    int? classId;
    final files = <PlatformFile>[];
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Nouvelle annonce',
      action: 'Publier',
      fields: (set) => [
        Field(title, 'Titre'),
        Field(content, 'Contenu', maxLines: 5),
        Dropdown<String>(label: 'Destinataires', value: audience, items: [for (final e in audienceFr.entries) DropdownMenuItem(value: e.key, child: Text(e.value))], onChanged: (v) => set(() => audience = v ?? 'ALL')),
        if (audience == 'CLASS')
          Dropdown<int>(label: 'Classe', value: classId, items: [for (final c in classes) DropdownMenuItem(value: asInt(c['id']), child: Text('${c['name']}'))], onChanged: (v) => set(() => classId = v)),
        Wrap(spacing: 6, children: [
          for (final f in files) InputChip(label: Text(f.name, overflow: TextOverflow.ellipsis), onDeleted: () => set(() => files.remove(f))),
        ]),
        OutlinedButton.icon(
          onPressed: files.length >= 5
              ? null
              : () async {
                  final r = await FilePicker.platform.pickFiles(allowMultiple: true, withData: true);
                  if (r == null) return;
                  for (final f in r.files) {
                    if (f.size > 10 * 1024 * 1024) {
                      if (context.mounted) toast(context, '« ${f.name} » dépasse 10 Mo.', error: true);
                    } else if (files.length < 5 && f.bytes != null) {
                      files.add(f);
                    }
                  }
                  set(() {});
                },
          icon: const Icon(Icons.attach_file),
          label: const Text('Joindre des fichiers (5 max, 10 Mo)'),
        ),
        const SizedBox(height: 12),
      ],
      submit: () async {
        final data = {'title': title.text.trim(), 'content': content.text.trim(), 'audience': audience, 'classId': audience == 'CLASS' ? classId : null};
        if (files.isEmpty) {
          await api.post('/api/admin/announcements', body: data);
        } else {
          await api.multipart('/api/admin/announcements/with-files', jsonParts: {'data': data}, files: [for (final f in files) UploadFile('files', f.name, f.bytes!)]);
        }
      },
    );
    if (ok == true) setState(() => k = UniqueKey());
  }
}

// ======================= Knowledge base =======================
class AdminKnowledgePage extends StatefulWidget {
  const AdminKnowledgePage({super.key});
  @override
  State<AdminKnowledgePage> createState() => _AdminKnowledgePageState();
}

class _AdminKnowledgePageState extends State<AdminKnowledgePage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.end, children: [
        FloatingActionButton.small(heroTag: 'link', onPressed: () => _link(context), child: const Icon(Icons.link)),
        const SizedBox(height: 10),
        FloatingActionButton.extended(heroTag: 'file', onPressed: () => _upload(context), icon: const Icon(Icons.upload_file), label: const Text('Ajouter un fichier')),
      ]),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/admin/knowledge')),
        builder: (context, items, reload) => PageList([
          const Text('Documents utilisés par l\'assistant IA pour répondre aux questions sur l\'établissement.'),
          if (items.isEmpty) const EmptyState('Aucun document', icon: Icons.psychology_outlined),
          for (final d in items)
            Panel(
              title: str(d['title']) ?? '',
              trailing: Badge2(str(d['status']) ?? '', color: d['status'] == 'INDEXED' ? EduColors.emerald : (d['status'] == 'FAILED' ? EduColors.red : EduColors.orange)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('${str(d['sourceKind']) == 'LINK' ? 'Lien' : str(d['type']) ?? ''} · ${d['chunkCount']} extraits · ${fmtSize(asInt(d['size']))}\n${str(d['uploadedBy']) ?? ''} · ${fmtDateTime(asDate(d['uploadedAt']))}',
                    style: TextStyle(fontSize: 12.5, color: Theme.of(context).hintColor)),
                if ((str(d['url']) ?? '').isNotEmpty) Text(str(d['url'])!, style: const TextStyle(fontSize: 12.5, color: EduColors.primary)),
                if ((str(d['lastError']) ?? '').isNotEmpty) Text(str(d['lastError'])!, style: const TextStyle(fontSize: 12.5, color: EduColors.red)),
                const SizedBox(height: 8),
                Wrap(spacing: 8, children: [
                  OutlinedButton.icon(
                    onPressed: () async {
                      try {
                        await api.post('/api/admin/knowledge/${d['id']}/reindex');
                        refresh();
                      } catch (e) {
                        if (context.mounted) toastError(context, e);
                      }
                    },
                    icon: const Icon(Icons.refresh),
                    label: const Text('Réindexer'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () async {
                      if (!await confirmDialog(context, 'Supprimer', 'Supprimer « ${d['title']} » ?', danger: true, ok: 'Supprimer')) return;
                      try {
                        await api.delete('/api/admin/knowledge/${d['id']}');
                        refresh();
                      } catch (e) {
                        if (context.mounted) toastError(context, e);
                      }
                    },
                    icon: const Icon(Icons.delete_outline, color: EduColors.red),
                    label: const Text('Supprimer', style: TextStyle(color: EduColors.red)),
                  ),
                ]),
              ]),
            ),
        ]),
      ),
    );
  }

  Future<void> _upload(BuildContext context) async {
    final api = apiOf(context);
    final r = await FilePicker.platform.pickFiles(withData: true, type: FileType.custom, allowedExtensions: const ['pdf', 'docx', 'pptx', 'xlsx', 'odt', 'odp', 'txt', 'md']);
    final f = r?.files.single;
    if (f == null || f.bytes == null) return;
    final t = TextEditingController(text: f.name);
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Ajouter un document',
      action: 'Téléverser',
      fields: (_) => [Text(f.name), const SizedBox(height: 10), Field(t, 'Titre')],
      submit: () => api.multipart('/api/admin/knowledge', fields: {'title': t.text.trim()}, files: [UploadFile('file', f.name, f.bytes!)]),
    );
    if (ok == true) refresh();
  }

  Future<void> _link(BuildContext context) async {
    final api = apiOf(context);
    final t = TextEditingController();
    final u = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: 'Ajouter un lien',
      action: 'Ajouter',
      fields: (_) => [Field(t, 'Titre (facultatif)'), Field(u, 'Adresse (https://…)', keyboard: TextInputType.url)],
      submit: () => api.post('/api/admin/knowledge/link', body: {'title': t.text.trim(), 'url': u.text.trim()}),
    );
    if (ok == true) refresh();
  }
}

// ======================= Document requests (agent side) =======================
class AdminRequestsPage extends StatefulWidget {
  const AdminRequestsPage({super.key});
  @override
  State<AdminRequestsPage> createState() => _AdminRequestsPageState();
}

class _AdminRequestsPageState extends State<AdminRequestsPage> {
  Key k = UniqueKey();
  String? status;
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const RequestTypesPage())),
        icon: const Icon(Icons.tune),
        label: const Text('Types de documents'),
      ),
      body: Column(children: [
        SizedBox(
          height: 52,
          child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.fromLTRB(16, 10, 16, 0), children: [
            Padding(padding: const EdgeInsets.only(right: 8), child: ChoiceChip(label: const Text('Toutes'), selected: status == null, onSelected: (_) => setState(() { status = null; k = UniqueKey(); }))),
            for (final e in requestStatusFr.entries)
              Padding(padding: const EdgeInsets.only(right: 8), child: ChoiceChip(label: Text(e.value), selected: status == e.key, onSelected: (_) => setState(() { status = e.key; k = UniqueKey(); }))),
          ]),
        ),
        Expanded(
          child: AsyncBody<List<RequestView>>(
            key: k,
            load: () async => listOf(await api.get('/api/admin/requests', query: {'status': status}), RequestView.fromJson),
            builder: (context, items, reload) => PageList([
              if (items.isEmpty) const EmptyState('Aucune demande', icon: Icons.description_outlined),
              for (final r in items)
                Panel(
                  title: r.type,
                  trailing: Badge2(requestStatusFr[r.status] ?? r.status, color: statusColor(r.status)),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('${r.requesterName ?? ''} (${r.requesterRole == 'ROLE_PROFESSOR' ? 'Professeur' : 'Étudiant'}) · ${r.requesterEmail ?? ''}', style: const TextStyle(fontWeight: FontWeight.w600)),
                    Text('${r.copies} exemplaire(s) · ${fmtDate(r.createdAt)}', style: TextStyle(fontSize: 12.5, color: Theme.of(context).hintColor)),
                    if ((r.reason ?? '').isNotEmpty) Text('Motif : ${r.reason}'),
                    if ((r.details ?? '').isNotEmpty) Text('Détails : ${r.details}'),
                    if ((r.adminNote ?? '').isNotEmpty) Text('Note : ${r.adminNote} (${r.handledBy ?? ''})'),
                    const SizedBox(height: 8),
                    Wrap(spacing: 8, children: [
                      FilledButton.tonalIcon(onPressed: () => _update(context, r), icon: const Icon(Icons.edit_note), label: const Text('Traiter')),
                      if (r.hasFile)
                        OutlinedButton.icon(onPressed: () => openProtectedFile(context, '/api/admin/requests/${r.id}/file', name: r.fileName ?? 'document'), icon: const Icon(Icons.download), label: Text(r.fileName ?? 'Fichier')),
                      OutlinedButton.icon(onPressed: () => _attach(context, r), icon: const Icon(Icons.attach_file), label: Text(r.hasFile ? 'Remplacer' : 'Joindre')),
                      if (r.hasFile)
                        OutlinedButton.icon(
                          onPressed: () async {
                            try {
                              await api.delete('/api/admin/requests/${r.id}/file');
                              refresh();
                            } catch (e) {
                              if (context.mounted) toastError(context, e);
                            }
                          },
                          icon: const Icon(Icons.delete_outline, color: EduColors.red),
                          label: const Text('Retirer le fichier', style: TextStyle(color: EduColors.red)),
                        ),
                    ]),
                  ]),
                ),
            ]),
          ),
        ),
      ]),
    );
  }

  Future<void> _update(BuildContext context, RequestView r) async {
    final api = apiOf(context);
    String st = r.status;
    final note = TextEditingController(text: r.adminNote ?? '');
    final ok = await showFormSheet(
      context,
      title: 'Traiter la demande',
      fields: (set) => [
        Dropdown<String>(label: 'Statut', value: st, items: [for (final e in requestStatusFr.entries) DropdownMenuItem(value: e.key, child: Text(e.value))], onChanged: (v) => set(() => st = v ?? st)),
        Field(note, 'Message pour le demandeur', maxLines: 3),
      ],
      submit: () => api.patch('/api/admin/requests/${r.id}', body: {'status': st, 'note': note.text.trim()}),
    );
    if (ok == true) refresh();
  }

  Future<void> _attach(BuildContext context, RequestView r) async {
    final res = await FilePicker.platform.pickFiles(withData: true);
    final f = res?.files.single;
    if (f == null || f.bytes == null) return;
    try {
      await apiOf(context).multipart('/api/admin/requests/${r.id}/file', files: [UploadFile('file', f.name, f.bytes!)]);
      refresh();
    } catch (e) {
      if (context.mounted) toastError(context, e);
    }
  }
}

class RequestTypesPage extends StatefulWidget {
  const RequestTypesPage({super.key});
  @override
  State<RequestTypesPage> createState() => _RequestTypesPageState();
}

class _RequestTypesPageState extends State<RequestTypesPage> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());
  static const aud = {'STUDENT': 'Étudiants', 'PROFESSOR': 'Professeurs', 'BOTH': 'Tous'};

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Types de documents')),
      floatingActionButton: FloatingActionButton.extended(onPressed: () => _edit(context, null), icon: const Icon(Icons.add), label: const Text('Nouveau type')),
      body: AsyncBody<List<JMap>>(
        key: k,
        load: () async => maps(await api.get('/api/admin/request-types')),
        builder: (context, items, reload) => PageList([
          if (items.isEmpty) const EmptyState('Aucun type'),
          for (final t in items)
            ListTileCard(
              title: Text('${t['name']}'),
              subtitle: Text('${aud[t['audience']] ?? t['audience']} · max ${t['maxCopies']} ex.${t['requiresDetails'] == true ? ' · détails requis' : ''}'),
              trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                Badge2(t['active'] == true ? 'Actif' : 'Inactif', color: t['active'] == true ? EduColors.emerald : EduColors.slate500),
                PopupMenuButton<String>(
                  onSelected: (v) async {
                    if (v == 'edit') {
                      _edit(context, t);
                    } else {
                      if (!await confirmDialog(context, 'Supprimer', 'Supprimer ce type ?', danger: true, ok: 'Supprimer')) return;
                      try {
                        await api.delete('/api/admin/request-types/${t['id']}');
                        refresh();
                      } catch (e) {
                        if (context.mounted) toastError(context, e);
                      }
                    }
                  },
                  itemBuilder: (_) => const [PopupMenuItem(value: 'edit', child: Text('Modifier')), PopupMenuItem(value: 'delete', child: Text('Supprimer'))],
                ),
              ]),
            ),
        ]),
      ),
    );
  }

  Future<void> _edit(BuildContext context, JMap? t) async {
    final api = apiOf(context);
    final name = TextEditingController(text: str(t?['name']) ?? '');
    final copies = TextEditingController(text: '${asInt(t?['maxCopies']) ?? 1}');
    String audience = str(t?['audience']) ?? 'BOTH';
    bool active = t?['active'] != false;
    bool details = t?['requiresDetails'] == true;
    final ok = await showFormSheet(
      context,
      title: t == null ? 'Nouveau type' : 'Modifier le type',
      fields: (set) => [
        Field(name, 'Nom'),
        Dropdown<String>(label: 'Destinataires', value: aud.containsKey(audience) ? audience : 'BOTH', items: [for (final e in aud.entries) DropdownMenuItem(value: e.key, child: Text(e.value))], onChanged: (v) => set(() => audience = v ?? audience)),
        Field(copies, 'Nombre maximum d\'exemplaires', keyboard: TextInputType.number),
        SwitchListTile(contentPadding: EdgeInsets.zero, title: const Text('Actif'), value: active, onChanged: (v) => set(() => active = v)),
        SwitchListTile(contentPadding: EdgeInsets.zero, title: const Text('Détails obligatoires'), value: details, onChanged: (v) => set(() => details = v)),
      ],
      submit: () async {
        final body = {'name': name.text.trim(), 'audience': audience, 'active': active, 'requiresDetails': details, 'maxCopies': int.tryParse(copies.text.trim()) ?? 1};
        if (t == null) {
          await api.post('/api/admin/request-types', body: body);
        } else {
          await api.put('/api/admin/request-types/${t['id']}', body: body);
        }
      },
    );
    if (ok == true) refresh();
  }
}

// ======================= Teacher absences =======================
class AdminAbsencesPage extends StatefulWidget {
  const AdminAbsencesPage({super.key});
  @override
  State<AdminAbsencesPage> createState() => _AdminAbsencesPageState();
}

class _AdminAbsencesPageState extends State<AdminAbsencesPage> {
  List<JMap> classes = [], profs = [];
  int? classId, professorId;

  @override
  void initState() {
    super.initState();
    final api = apiOf(context);
    Future(() async {
      try {
        classes = maps(await api.get('/api/admin/classes'));
        profs = maps(await api.get('/api/admin/professors'));
      } catch (_) {}
      if (mounted) setState(() {});
    });
  }

  @override
  Widget build(BuildContext context) => Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: Row(children: [
            Expanded(
              child: DropdownButtonFormField<int?>(
                value: classId,
                isExpanded: true,
                decoration: InputDecoration(labelText: tr('Classe'), isDense: true),
                items: [const DropdownMenuItem<int?>(value: null, child: Text('Toutes')), for (final c in classes) DropdownMenuItem<int?>(value: asInt(c['id']), child: Text('${c['name']}', overflow: TextOverflow.ellipsis))],
                onChanged: (v) => setState(() => classId = v),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: DropdownButtonFormField<int?>(
                value: professorId,
                isExpanded: true,
                decoration: InputDecoration(labelText: tr('Professeur'), isDense: true),
                items: [const DropdownMenuItem<int?>(value: null, child: Text('Tous')), for (final p in profs) DropdownMenuItem<int?>(value: asInt(p['id']), child: Text('${p['firstName']} ${p['lastName']}', overflow: TextOverflow.ellipsis))],
                onChanged: (v) => setState(() => professorId = v),
              ),
            ),
          ]),
        ),
        Expanded(
          child: WeekTimetableView(path: '/api/admin/timetable/week', query: {'classId': classId, 'professorId': professorId}, mode: WeekMode.admin),
        ),
      ]);
}
