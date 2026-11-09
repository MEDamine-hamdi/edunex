import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/widgets/common.dart';

/// Rights given to one specific professor by the platform administrator.
/// Visible only when GET /api/users/me/permissions contains GRADES_MANAGE.
class DelegatedGradesPage extends StatefulWidget {
  const DelegatedGradesPage({super.key});
  @override
  State<DelegatedGradesPage> createState() => _DelegatedGradesPageState();
}

class _DelegatedGradesPageState extends State<DelegatedGradesPage> {
  List<JMap> subjects = [];
  List<String> types = [];
  List<JMap> students = [];
  int? subjectId;
  int? studentId;
  String? type;
  DateTime date = DateTime.now();
  final value = TextEditingController();
  final done = <String>[];
  Object? error;
  bool loading = true;
  bool busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    value.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final api = apiOf(context);
    try {
      final s = maps(await api.get('/api/delegated/grades/subjects'));
      final t = ((await api.get('/api/delegated/grades/evaluation-types')) as List).map((e) => e.toString()).toList();
      if (!mounted) return;
      setState(() { subjects = s; types = t; type = t.isEmpty ? null : t.first; loading = false; error = null; });
    } catch (e) {
      if (mounted) setState(() { loading = false; error = e; });
    }
  }

  Future<void> _pickSubject(int? id) async {
    setState(() { subjectId = id; studentId = null; students = []; });
    if (id == null) return;
    try {
      final r = maps(await apiOf(context).get('/api/delegated/grades/subjects/$id/students'));
      if (mounted) setState(() => students = r);
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  Future<void> _submit() async {
    final v = double.tryParse(value.text.replaceAll(',', '.'));
    if (subjectId == null || studentId == null || type == null || v == null || v < 0 || v > 20) {
      toast(context, 'Choisissez la matière, l\'étudiant et une note entre 0 et 20.', error: true);
      return;
    }
    setState(() => busy = true);
    try {
      await apiOf(context).post('/api/delegated/grades', body: {
        'studentId': studentId,
        'subjectId': subjectId,
        'value': v,
        'evaluationType': type,
        'date': isoDate(date),
      });
      if (!mounted) return;
      final st = students.firstWhere((s) => asInt(s['studentId']) == studentId, orElse: () => {});
      final su = subjects.firstWhere((s) => asInt(s['id']) == subjectId, orElse: () => {});
      setState(() {
        done.insert(0, '${str(st['studentName']) ?? ''} · ${str(su['name']) ?? ''} · ${gradeTypeFr[type] ?? type} · $v/20 · ${fmtDate(date)}');
        value.clear();
        busy = false;
      });
      toast(context, 'Note enregistrée.');
    } catch (e) {
      if (mounted) {
        setState(() => busy = false);
        final forbidden = e is ApiException && e.isForbidden;
        toast(context, forbidden ? "Vous n'enseignez pas cette matière." : e.toString(), error: true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (error != null) return ErrorState(error!, () { setState(() { loading = true; error = null; }); _load(); });
    return PageList([
      Text("Droit accordé par l'administration : vous pouvez saisir des notes dans les matières que vous enseignez. L'étudiant est prévenu à chaque note.", style: TextStyle(color: Theme.of(context).hintColor)),
      Panel(
        child: Column(children: [
          Dropdown<int>(
            label: 'Matière',
            value: subjectId,
            items: [for (final s in subjects) DropdownMenuItem(value: asInt(s['id']), child: Text('${s['name']} (${s['code']})', overflow: TextOverflow.ellipsis))],
            onChanged: _pickSubject,
          ),
          Dropdown<int>(
            label: 'Étudiant',
            value: studentId,
            items: [for (final s in students) DropdownMenuItem(value: asInt(s['studentId']), child: Text('${s['studentName']} (${s['studentNumber']})', overflow: TextOverflow.ellipsis))],
            onChanged: (v) => setState(() => studentId = v),
          ),
          Dropdown<String>(
            label: "Type d'évaluation",
            value: type,
            items: [for (final t in types) DropdownMenuItem(value: t, child: Text(gradeTypeFr[t] ?? t))],
            onChanged: (v) => setState(() => type = v),
          ),
          Field(value, 'Note (0 – 20)', keyboard: const TextInputType.numberWithOptions(decimal: true)),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () async {
                final d = await pickDate(context, initial: date, last: DateTime.now());
                if (d != null) setState(() => date = d);
              },
              icon: const Icon(Icons.event),
              label: Text(fmtDate(date)),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: busy ? null : _submit,
              child: busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Text('Saisir une note'),
            ),
          ),
        ]),
      ),
      Panel(
        title: 'Notes saisies pendant cette session',
        child: done.isEmpty ? const EmptyState('Aucune note') : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [for (final d in done) Padding(padding: const EdgeInsets.symmetric(vertical: 3), child: Text(d))]),
      ),
    ]);
  }
}

const _days = [
  ['MONDAY', 'Lundi'],
  ['TUESDAY', 'Mardi'],
  ['WEDNESDAY', 'Mercredi'],
  ['THURSDAY', 'Jeudi'],
  ['FRIDAY', 'Vendredi'],
  ['SATURDAY', 'Samedi'],
];

/// Visible only when GET /api/users/me/permissions contains TIMETABLE_MANAGE.
class DelegatedTimetablePage extends StatefulWidget {
  const DelegatedTimetablePage({super.key});
  @override
  State<DelegatedTimetablePage> createState() => _DelegatedTimetablePageState();
}

class _DelegatedTimetablePageState extends State<DelegatedTimetablePage> {
  List<JMap> timetables = [];
  List<JMap> classes = [];
  int? timetableId;
  int? classId;
  int? subjectId;
  int? professorId;
  String day = 'MONDAY';
  TimeOfDay start = const TimeOfDay(hour: 8, minute: 30);
  TimeOfDay end = const TimeOfDay(hour: 10, minute: 0);
  final year = TextEditingController();
  final semester = TextEditingController();
  final room = TextEditingController();
  Object? error;
  bool loading = true;
  bool busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    year.dispose();
    semester.dispose();
    room.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final l = Map<String, dynamic>.from(await apiOf(context).get('/api/delegated/timetable/lookups') as Map);
      if (mounted) setState(() { timetables = maps(l['timetables']); classes = maps(l['classes']); loading = false; error = null; });
    } catch (e) {
      if (mounted) setState(() { loading = false; error = e; });
    }
  }

  List<JMap> get subjects => maps(classes.firstWhere((c) => asInt(c['id']) == classId, orElse: () => {})['subjects']);
  List<JMap> get professors => maps(subjects.firstWhere((s) => asInt(s['id']) == subjectId, orElse: () => {})['professors']);

  Future<void> _createTimetable() async {
    if (year.text.trim().isEmpty || semester.text.trim().isEmpty) {
      toast(context, "Renseignez l'année universitaire et le semestre.", error: true);
      return;
    }
    try {
      await apiOf(context).post('/api/delegated/timetable/timetables', body: {'academicYear': year.text.trim(), 'semester': semester.text.trim()});
      year.clear();
      semester.clear();
      if (mounted) toast(context, 'Emploi du temps créé.');
      _load();
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  Future<void> _addSession() async {
    if (timetableId == null || classId == null || subjectId == null || professorId == null) {
      toast(context, "Choisissez l'emploi du temps, la classe, la matière et le professeur.", error: true);
      return;
    }
    setState(() => busy = true);
    try {
      await apiOf(context).post('/api/delegated/timetable/sessions', body: {
        'timetableId': timetableId,
        'dayOfWeek': day,
        'startTime': hhmmOf(start),
        'endTime': hhmmOf(end),
        'room': room.text.trim().isEmpty ? null : room.text.trim(),
        'classId': classId,
        'subjectId': subjectId,
        'professorId': professorId,
      });
      if (mounted) {
        setState(() => busy = false);
        toast(context, 'Séance ajoutée : la classe et le professeur sont prévenus.');
      }
    } catch (e) {
      if (mounted) {
        setState(() => busy = false);
        toast(context, e is ApiException && e.status == 409 ? "Conflit d'horaire : $e" : e.toString(), error: true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (error != null) return ErrorState(error!, () { setState(() { loading = true; error = null; }); _load(); });
    return PageList([
      Text("Droit accordé par l'administration : créez un emploi du temps et ajoutez des séances. Les conflits de salle, de professeur et de classe sont refusés.", style: TextStyle(color: Theme.of(context).hintColor)),
      Panel(
        title: 'Nouvel emploi du temps',
        child: Column(children: [
          Field(year, 'Année universitaire'),
          Field(semester, 'Semestre'),
          SizedBox(width: double.infinity, child: FilledButton(onPressed: _createTimetable, child: const Text('Créer'))),
        ]),
      ),
      Panel(
        title: 'Ajouter une séance',
        child: Column(children: [
          Dropdown<int>(
            label: 'Emploi du temps cible',
            value: timetableId,
            items: [for (final t in timetables) DropdownMenuItem(value: asInt(t['id']), child: Text(str(t['label']) ?? ''))],
            onChanged: (v) => setState(() => timetableId = v),
          ),
          Dropdown<String>(
            label: 'Jour',
            value: day,
            items: [for (final d in _days) DropdownMenuItem(value: d[0], child: Text(d[1]))],
            onChanged: (v) => setState(() => day = v ?? day),
          ),
          Row(children: [
            Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: start); if (t != null) setState(() => start = t); }, child: Text(hhmmOf(start).substring(0, 5)))),
            const SizedBox(width: 8),
            Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: end); if (t != null) setState(() => end = t); }, child: Text(hhmmOf(end).substring(0, 5)))),
          ]),
          const SizedBox(height: 12),
          Field(room, 'Salle'),
          Dropdown<int>(
            label: 'Classe',
            value: classId,
            items: [for (final c in classes) DropdownMenuItem(value: asInt(c['id']), child: Text(str(c['name']) ?? ''))],
            onChanged: (v) => setState(() { classId = v; subjectId = null; professorId = null; }),
          ),
          Dropdown<int>(
            label: 'Matière',
            value: subjectId,
            items: [for (final s in subjects) DropdownMenuItem(value: asInt(s['id']), child: Text(str(s['name']) ?? ''))],
            onChanged: (v) => setState(() { subjectId = v; professorId = null; }),
          ),
          Dropdown<int>(
            label: 'Professeur',
            value: professorId,
            items: [for (final p in professors) DropdownMenuItem(value: asInt(p['id']), child: Text(str(p['name']) ?? ''))],
            onChanged: (v) => setState(() => professorId = v),
          ),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: busy ? null : _addSession,
              child: busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Text('Ajouter une séance'),
            ),
          ),
        ]),
      ),
    ]);
  }
}
