import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

const examTypes = ['Examen', 'Devoir surveillé', 'Rattrapage', 'Autre'];
const examActionFr = {
  'VOLUNTEERED': "S'est porté volontaire",
  'ASSIGNED': "Affecté par l'administration",
  'WITHDRAWN': "S'est désisté",
  'REMOVED': "Retiré par l'administration",
  'EXAM_CANCELLED': 'Examen annulé',
};

String _hm(dynamic t) {
  final s = (t ?? '').toString();
  return s.length >= 5 ? s.substring(0, 5) : s;
}

DateTime? _day(dynamic v) {
  final d = asDate(v);
  return d == null ? null : DateTime(d.year, d.month, d.day);
}

String _dayName(DateTime d) => dayFr[dayOrder[d.weekday - 1]] ?? '';

String _stamp(dynamic v) {
  final d = asDate(v);
  if (d == null) return '';
  return '${fmtDate(d)} ${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
}

// ---------------- shared plan view (week / list) ----------------
class PlanItem {
  final String key;
  final DateTime date;
  final String start;
  final String end;
  final String title;
  final String detail;
  final String? room;
  final String? tag;
  final bool cancelled;
  const PlanItem({required this.key, required this.date, required this.start, required this.end, required this.title, this.detail = '', this.room, this.tag, this.cancelled = false});
}

class ExamPlanView extends StatefulWidget {
  final List<PlanItem> items;
  final String empty;
  final List<Widget> footer;
  const ExamPlanView({super.key, required this.items, required this.empty, this.footer = const []});
  @override
  State<ExamPlanView> createState() => _ExamPlanViewState();
}

class _ExamPlanViewState extends State<ExamPlanView> {
  bool week = true;
  bool showPast = false;
  late DateTime weekStart = _monday(DateTime.now());

  static DateTime _monday(DateTime d) {
    final day = DateTime(d.year, d.month, d.day);
    return day.subtract(Duration(days: day.weekday - 1));
  }

  Widget _card(PlanItem it) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: it.cancelled ? const Color(0xFFFEF2F2) : scheme.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: it.cancelled ? const Color(0xFFFCA5A5) : Theme.of(context).dividerColor),
      ),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Column(children: [
          Text(_hm(it.start), style: const TextStyle(fontWeight: FontWeight.w900)),
          Text(_hm(it.end), style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12)),
        ]),
        const SizedBox(width: 12),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(it.title, style: TextStyle(fontWeight: FontWeight.w800, decoration: it.cancelled ? TextDecoration.lineThrough : null)),
            if (it.detail.isNotEmpty) Text(it.detail, style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
            if ((it.room ?? '').isNotEmpty) Text('Salle : ${it.room}', style: const TextStyle(fontSize: 12.5)),
            Wrap(spacing: 6, children: [
              if (it.cancelled) const Badge2('Annulé', color: EduColors.red),
              if (it.tag != null) Badge2(it.tag!, color: EduColors.indigo),
            ]),
          ]),
        ),
      ]),
    );
  }

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    final today0 = DateTime(today.year, today.month, today.day);
    final all = [...widget.items]..sort((a, b) {
        final c = a.date.compareTo(b.date);
        return c != 0 ? c : a.start.compareTo(b.start);
      });
    final children = <Widget>[
      SegmentedButton<bool>(
        showSelectedIcon: false,
        segments: const [ButtonSegment(value: true, label: Text('Semaine')), ButtonSegment(value: false, label: Text('Liste'))],
        selected: {week},
        onSelectionChanged: (s) => setState(() => week = s.first),
      ),
    ];
    if (week) {
      final end = weekStart.add(const Duration(days: 6));
      final inWeek = all.where((i) => !i.date.isBefore(weekStart) && !i.date.isAfter(end)).toList();
      children.add(Row(children: [
        IconButton(onPressed: () => setState(() => weekStart = weekStart.subtract(const Duration(days: 7))), icon: const Icon(Icons.chevron_left)),
        Expanded(child: Center(child: Text('${fmtDate(weekStart)} → ${fmtDate(end)}', style: const TextStyle(fontWeight: FontWeight.w800)))),
        IconButton(onPressed: () => setState(() => weekStart = weekStart.add(const Duration(days: 7))), icon: const Icon(Icons.chevron_right)),
      ]));
      if (inWeek.isEmpty) children.add(EmptyState(widget.empty, icon: Icons.event_available_outlined));
      DateTime? last;
      for (final it in inWeek) {
        if (last != it.date) {
          children.add(Padding(
            padding: const EdgeInsets.only(top: 6, bottom: 6),
            child: Text('${_dayName(it.date)} ${fmtDate(it.date)}', style: const TextStyle(fontWeight: FontWeight.w900, color: EduColors.primary)),
          ));
          last = it.date;
        }
        children.add(_card(it));
      }
    } else {
      children.add(CheckboxListTile(
        contentPadding: EdgeInsets.zero,
        controlAffinity: ListTileControlAffinity.leading,
        value: showPast,
        onChanged: (v) => setState(() => showPast = v ?? false),
        title: const Text('Afficher les examens passés'),
      ));
      final shown = all.where((i) => showPast || !i.date.isBefore(today0)).toList();
      if (shown.isEmpty) children.add(EmptyState(widget.empty, icon: Icons.event_available_outlined));
      for (final it in shown) {
        children.add(Padding(
          padding: const EdgeInsets.only(bottom: 2),
          child: Text('${_dayName(it.date)} ${fmtDate(it.date)}', style: TextStyle(fontSize: 12, color: Theme.of(context).hintColor, fontWeight: FontWeight.w700)),
        ));
        children.add(_card(it));
      }
    }
    children.addAll(widget.footer);
    return PageList(children);
  }
}

// ======================= Student: GET /api/student/exams =======================
class StudentExamsPage extends StatelessWidget {
  const StudentExamsPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      load: () async => maps(await api.get('/api/student/exams')),
      builder: (context, rows, reload) => ExamPlanView(
        empty: 'Aucun examen',
        items: [
          for (final r in rows)
            if (_day(r['date']) != null)
              PlanItem(
                key: '${r['id']}',
                date: _day(r['date'])!,
                start: _hm(r['startTime']),
                end: _hm(r['endTime']),
                title: '${str(r['subjectName']) ?? ''} (${str(r['examType']) ?? ''})',
                detail: str(r['subjectCode']) ?? '',
                room: str(r['room']),
                cancelled: r['status'] == 'CANCELLED',
              ),
        ],
      ),
    );
  }
}

// ======================= Professor =======================
class ProfessorExamsPage extends StatefulWidget {
  const ProfessorExamsPage({super.key});
  @override
  State<ProfessorExamsPage> createState() => _ProfessorExamsPageState();
}

class _ProfessorExamsPageState extends State<ProfessorExamsPage> {
  int tab = 0;
  String? kind;
  List<JMap> mine = [];
  List<JMap> available = [];
  List<JMap> history = [];
  Object? error;
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = apiOf(context);
    try {
      final k = Map<String, dynamic>.from(await api.get('/api/professor/exams/kind') as Map);
      final m = maps(await api.get('/api/professor/exams/mine'));
      final a = maps(await api.get('/api/professor/exams/available'));
      final h = maps(await api.get('/api/professor/exams/history'));
      if (mounted) setState(() { kind = str(k['kind']); mine = m; available = a; history = h; loading = false; error = null; });
    } catch (e) {
      if (mounted) setState(() { loading = false; error = e; });
    }
  }

  Future<void> _join(JMap exam) async {
    try {
      await apiOf(context).post('/api/professor/exams/${exam['id']}/volunteer');
      if (mounted) toast(context, 'Vous surveillerez cet examen. Il apparaît dans votre planning.');
      _load();
    } catch (e) {
      if (mounted) toast(context, e is ApiException && e.isForbidden ? 'Seuls les vacataires choisissent leurs examens.' : e.toString(), error: true);
    }
  }

  Future<void> _withdraw(JMap exam) async {
    if (!await confirmDialog(context, 'Se désister', 'Vous désister de cette surveillance ?', danger: true, ok: 'Se désister')) return;
    if (!mounted) return;
    try {
      await apiOf(context).delete('/api/professor/exams/${exam['id']}/volunteer');
      _load();
    } catch (e) {
      if (mounted) toast(context, e is ApiException && e.status == 409 ? "Trop tard pour vous désister : contactez l'administration." : e.toString(), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (error != null) return ErrorState(error!, () { setState(() { loading = true; error = null; }); _load(); });
    final vacataire = kind == 'VACATAIRE';
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        child: SegmentedButton<int>(
          showSelectedIcon: false,
          segments: const [
            ButtonSegment(value: 0, label: Text('Planning')),
            ButtonSegment(value: 1, label: Text('À surveiller')),
            ButtonSegment(value: 2, label: Text('Historique')),
          ],
          selected: {tab},
          onSelectionChanged: (s) => setState(() => tab = s.first),
        ),
      ),
      Expanded(
        child: switch (tab) {
          0 => _plan(vacataire),
          1 => _choose(vacataire),
          _ => _history(),
        },
      ),
    ]);
  }

  Widget _plan(bool vacataire) {
    final items = <PlanItem>[];
    for (final m in mine) {
      final e = Map<String, dynamic>.from(m['exam'] as Map);
      final d = _day(e['date']);
      if (d == null) continue;
      items.add(PlanItem(
        key: '${e['id']}',
        date: d,
        start: _hm(e['startTime']),
        end: _hm(e['endTime']),
        title: '${str(e['subjectName']) ?? ''} (${str(e['examType']) ?? ''})',
        detail: maps(e['classes']).map((c) => c['name']).join(', '),
        room: str(e['room']),
        tag: m['mode'] == 'VOLUNTEER' ? 'Surveillance choisie' : "Surveillance imposée par l'administration",
        cancelled: e['status'] == 'CANCELLED',
      ));
    }
    final volunteer = mine.where((m) => m['past'] != true && m['mode'] == 'VOLUNTEER').toList();
    return ExamPlanView(
      items: items,
      empty: 'Aucune surveillance à venir.',
      footer: [
        Text(vacataire ? 'Vous êtes vacataire : vous choisissez les examens que vous surveillez.' : "Vous êtes permanent : l'administration fixe vos surveillances.", style: TextStyle(color: Theme.of(context).hintColor)),
        if (volunteer.isNotEmpty)
          Panel(
            title: 'Mes surveillances choisies',
            child: Column(children: [
              for (final m in volunteer)
                Builder(builder: (context) {
                  final e = Map<String, dynamic>.from(m['exam'] as Map);
                  return ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    title: Text('${str(e['subjectName']) ?? ''} (${str(e['examType']) ?? ''})'),
                    subtitle: Text('${fmtDate(_day(e['date']))} · ${_hm(e['startTime'])}-${_hm(e['endTime'])}'),
                    trailing: m['canWithdraw'] == true
                        ? TextButton(onPressed: () => _withdraw(e), child: const Text('Se désister', style: TextStyle(color: EduColors.red)))
                        : const SizedBox(width: 120, child: Text('Désistement impossible : contactez l\'administration', style: TextStyle(fontSize: 11))),
                  );
                }),
            ]),
          ),
      ],
    );
  }

  Widget _choose(bool vacataire) {
    if (!vacataire) {
      return PageList([
        Panel(
          child: Text("En tant que professeur permanent, vous ne choisissez pas vos surveillances : l'administration vous affecte aux examens (voir « Planning »). Vous êtes prévenu(e) par notification et par e-mail."),
        ),
      ]);
    }
    if (available.isEmpty) return ListView(children: const [EmptyState('Aucun examen à surveiller pour le moment.', icon: Icons.event_available_outlined)]);
    return RefreshIndicator(
      onRefresh: _load,
      child: PageList([
        for (final a in available)
          Builder(builder: (context) {
            final e = Map<String, dynamic>.from(a['exam'] as Map);
            final sup = maps(e['supervisors']).length;
            return Panel(
              title: '${str(e['subjectName']) ?? ''} (${str(e['examType']) ?? ''})',
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('${_dayName(_day(e['date']) ?? DateTime.now())} ${fmtDate(_day(e['date']))} · ${_hm(e['startTime'])}-${_hm(e['endTime'])}', style: const TextStyle(fontWeight: FontWeight.w700)),
                Text('Salle : ${str(e['room']) ?? '—'}'),
                Text('${maps(e['classes']).map((c) => c['name']).join(', ')} · $sup/${e['needed']} ${tr('surveillants')}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
                const SizedBox(height: 8),
                a['canJoin'] == true
                    ? FilledButton(onPressed: () => _join(e), child: const Text('Je surveille'))
                    : Badge2(str(a['blocked']) ?? '', color: EduColors.slate500),
              ]),
            );
          }),
      ]),
    );
  }

  Widget _history() => _HistoryList(rows: history);
}

class _HistoryList extends StatelessWidget {
  final List<JMap> rows;
  final bool showProfessor;
  const _HistoryList({required this.rows, this.showProfessor = false});
  @override
  Widget build(BuildContext context) {
    if (rows.isEmpty) return ListView(children: const [EmptyState('Aucun historique', icon: Icons.history)]);
    return PageList([
      for (final h in rows)
        Panel(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(examActionFr[h['action']] ?? str(h['action']) ?? '', style: const TextStyle(fontWeight: FontWeight.w800)),
            if (showProfessor) Text(str(h['professor']) ?? ''),
            if ((str(h['exam']) ?? '').isNotEmpty) Text(str(h['exam'])!),
            if ((str(h['reason']) ?? '').isNotEmpty) Text(str(h['reason'])!, style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
            Text('${_stamp(h['at'])}${(str(h['actor']) ?? '').isNotEmpty ? ' · ${h['actor']}' : ''}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12)),
          ]),
        ),
    ]);
  }
}

// ======================= Administration (EXAMS_MANAGE) =======================
class AdminExamsPage extends StatefulWidget {
  const AdminExamsPage({super.key});
  @override
  State<AdminExamsPage> createState() => _AdminExamsPageState();
}

class _AdminExamsPageState extends State<AdminExamsPage> {
  int tab = 0;
  List<JMap> exams = [];
  List<JMap> profs = [];
  JMap lookups = {};
  List<JMap> history = [];
  String? histProf;
  Object? error;
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = apiOf(context);
    try {
      final x = maps(await api.get('/api/admin/exams'));
      final l = Map<String, dynamic>.from(await api.get('/api/admin/exams/lookups') as Map);
      final p = maps(await api.get('/api/admin/exams/supervisors'));
      if (mounted) setState(() { exams = x; lookups = l; profs = p; loading = false; error = null; });
    } catch (e) {
      if (mounted) setState(() { loading = false; error = e; });
    }
  }

  Future<void> _loadHistory() async {
    try {
      final h = maps(await apiOf(context).get('/api/admin/exams/history', query: {if (histProf != null) 'professorId': histProf}));
      if (mounted) setState(() => history = h);
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  void _fail(Object e) {
    toast(context, e is ApiException && e.status == 409 ? 'Conflit : $e' : e.toString(), error: true);
  }

  Future<void> _edit(JMap? x) async {
    final api = apiOf(context);
    final subjects = maps(lookups['subjects']);
    final classes = maps(lookups['classes']);
    int? subjectId = asInt(x?['subjectId']);
    final classIds = <int>{...maps(x?['classes']).map((c) => asInt(c['id'])!)};
    String type = examTypes.contains(str(x?['examType'])) ? str(x?['examType'])! : examTypes.first;
    DateTime? date = _day(x?['date']);
    TimeOfDay start = _tod(x?['startTime'], const TimeOfDay(hour: 8, minute: 30));
    TimeOfDay end = _tod(x?['endTime'], const TimeOfDay(hour: 10, minute: 30));
    final room = TextEditingController(text: str(x?['room']) ?? '');
    final needed = TextEditingController(text: '${x?['needed'] ?? 2}');
    final ok = await showFormSheet(
      context,
      title: x == null ? 'Nouvel examen' : "Modifier l'examen",
      fields: (set) {
        final subj = subjects.where((s) => asInt(s['id']) == subjectId).toList();
        final allowed = subj.isEmpty ? <int>[] : (subj.first['classIds'] as List).map((e) => (e as num).toInt()).toList();
        return [
          Dropdown<int>(
            label: 'Matière',
            value: subjectId,
            items: [for (final s in subjects) DropdownMenuItem(value: asInt(s['id']), child: Text('${s['name']} (${s['code']})', overflow: TextOverflow.ellipsis))],
            onChanged: (v) => set(() { subjectId = v; classIds.clear(); }),
          ),
          if (subjectId != null) ...[
            const Text('Classes', style: TextStyle(fontWeight: FontWeight.w800)),
            Wrap(spacing: 6, children: [
              for (final c in classes.where((c) => allowed.contains(asInt(c['id']))))
                FilterChip(
                  label: Text(str(c['name']) ?? ''),
                  selected: classIds.contains(asInt(c['id'])),
                  onSelected: (v) => set(() => v ? classIds.add(asInt(c['id'])!) : classIds.remove(asInt(c['id']))),
                ),
            ]),
            const SizedBox(height: 8),
          ],
          Dropdown<String>(label: 'Type', value: type, items: [for (final t in examTypes) DropdownMenuItem(value: t, child: Text(t))], onChanged: (v) => set(() => type = v ?? type)),
          OutlinedButton.icon(onPressed: () async { final d = await pickDate(context, initial: date, first: DateTime.now(), last: DateTime.now().add(const Duration(days: 730))); if (d != null) set(() => date = d); }, icon: const Icon(Icons.event), label: Text(date == null ? 'Date' : fmtDate(date))),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: start); if (t != null) set(() => start = t); }, child: Text(hhmmOf(start).substring(0, 5)))),
            const SizedBox(width: 8),
            Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: end); if (t != null) set(() => end = t); }, child: Text(hhmmOf(end).substring(0, 5)))),
          ]),
          const SizedBox(height: 12),
          Field(room, 'Salle'),
          Field(needed, 'Nombre de surveillants requis', keyboard: TextInputType.number),
        ];
      },
      submit: () async {
        final n = int.tryParse(needed.text.trim()) ?? 0;
        if (subjectId == null || classIds.isEmpty || date == null) throw ApiException(0, 'Choisissez la matière, au moins une classe et la date.');
        if (n < 1 || n > 20) throw ApiException(0, 'Le nombre de surveillants doit être compris entre 1 et 20.');
        final body = {
          'subjectId': subjectId,
          'classIds': classIds.toList(),
          'examType': type,
          'date': isoDate(date!),
          'startTime': hhmmOf(start),
          'endTime': hhmmOf(end),
          'room': room.text.trim().isEmpty ? null : room.text.trim(),
          'supervisorsNeeded': n,
        };
        if (x == null) {
          await api.post('/api/admin/exams', body: body);
        } else {
          await api.put('/api/admin/exams/${x['id']}', body: body);
        }
      },
    );
    if (ok == true) _load();
  }

  TimeOfDay _tod(dynamic v, TimeOfDay fallback) {
    final s = _hm(v);
    final p = s.split(':');
    if (p.length != 2) return fallback;
    return TimeOfDay(hour: int.tryParse(p[0]) ?? fallback.hour, minute: int.tryParse(p[1]) ?? fallback.minute);
  }

  Future<void> _assign(JMap x) async {
    final api = apiOf(context);
    final taken = maps(x['supervisors']).map((s) => asInt(s['professorId'])).toSet();
    final candidates = profs.where((p) => !taken.contains(asInt(p['id']))).toList();
    int? pid;
    String? warning;
    final ok = await showFormSheet(
      context,
      title: 'Affecter un professeur',
      action: 'Affecter',
      fields: (set) => [
        Dropdown<int>(
          label: 'Professeur',
          value: pid,
          items: [for (final p in candidates) DropdownMenuItem(value: asInt(p['id']), child: Text('${p['name']} (${p['kind'] == 'VACATAIRE' ? 'vacataire' : 'permanent'})', overflow: TextOverflow.ellipsis))],
          onChanged: (v) => set(() => pid = v),
        ),
      ],
      submit: () async {
        if (pid == null) throw ApiException(0, 'Choisissez un professeur.');
        final r = Map<String, dynamic>.from(await api.post('/api/admin/exams/${x['id']}/supervisors', body: {'professorId': pid}) as Map);
        warning = str(r['warning']);
      },
    );
    if (ok == true) {
      if (mounted) toast(context, warning != null ? 'Affecté. Attention : $warning.' : 'Surveillant affecté et prévenu.');
      _load();
    }
  }

  Future<void> _remove(JMap x, JMap s) async {
    if (!await confirmDialog(context, 'Retirer le surveillant', 'Retirer ${s['name']} de cet examen ?', danger: true, ok: 'Retirer')) return;
    if (!mounted) return;
    try {
      await apiOf(context).delete('/api/admin/exams/${x['id']}/supervisors/${s['professorId']}');
      _load();
    } catch (e) {
      if (mounted) _fail(e);
    }
  }

  Future<void> _cancel(JMap x) async {
    final api = apiOf(context);
    final reason = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: "Annuler l'examen",
      action: "Annuler l'examen",
      fields: (_) => [Field(reason, 'Motif', maxLines: 2)],
      submit: () => api.post('/api/admin/exams/${x['id']}/cancel', body: {'reason': reason.text.trim()}),
    );
    if (ok == true) _load();
  }

  Future<void> _setKind(JMap p, String kind) async {
    try {
      await apiOf(context).put('/api/admin/exams/supervisors/${p['id']}/kind', body: {'kind': kind});
      _load();
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (error != null) return ErrorState(error!, () { setState(() { loading = true; error = null; }); _load(); });
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: tab == 0 ? FloatingActionButton.extended(onPressed: () => _edit(null), icon: const Icon(Icons.add), label: const Text('Nouvel examen')) : null,
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: SegmentedButton<int>(
            showSelectedIcon: false,
            segments: const [ButtonSegment(value: 0, label: Text('Examens')), ButtonSegment(value: 1, label: Text('Surveillants')), ButtonSegment(value: 2, label: Text('Historique'))],
            selected: {tab},
            onSelectionChanged: (s) {
              setState(() => tab = s.first);
              if (tab == 2) _loadHistory();
            },
          ),
        ),
        Expanded(child: switch (tab) { 0 => _examsTab(), 1 => _profsTab(), _ => _historyTab() }),
      ]),
    );
  }

  Widget _examsTab() {
    if (exams.isEmpty) return ListView(children: const [EmptyState('Aucun examen', icon: Icons.event_note_outlined)]);
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(padding: const EdgeInsets.fromLTRB(16, 12, 16, 96), children: [
        for (final x in exams)
          Builder(builder: (context) {
            final cancelled = x['status'] == 'CANCELLED';
            final sups = maps(x['supervisors']);
            final needed = asInt(x['needed']) ?? 0;
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Panel(
                title: '${str(x['subjectName']) ?? ''} (${str(x['examType']) ?? ''})',
                trailing: cancelled ? const Badge2('Annulé', color: EduColors.red) : Badge2('${sups.length}/$needed', color: sups.length >= needed ? EduColors.emerald : const Color(0xFFD97706)),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('${_dayName(_day(x['date']) ?? DateTime.now())} ${fmtDate(_day(x['date']))} · ${_hm(x['startTime'])}-${_hm(x['endTime'])} · Salle : ${str(x['room']) ?? '—'}', style: const TextStyle(fontWeight: FontWeight.w700)),
                  Text('${maps(x['classes']).map((c) => c['name']).join(', ')} · ${x['students'] ?? 0} ${tr('étudiants')}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
                  if (cancelled && (str(x['cancelReason']) ?? '').isNotEmpty) Text('Motif : ${x['cancelReason']}', style: const TextStyle(color: EduColors.red, fontSize: 12.5)),
                  if (!cancelled) ...[
                    if (sups.length < needed)
                      Padding(padding: const EdgeInsets.only(top: 4), child: Text('${needed - sups.length} ${tr('surveillant(s) manquant(s)')}', style: const TextStyle(color: Color(0xFFD97706), fontWeight: FontWeight.w700, fontSize: 12.5))),
                    for (final s in sups)
                      ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(str(s['name']) ?? ''),
                        subtitle: Text('${s['kind'] == 'VACATAIRE' ? 'Vacataire' : 'Permanent'} · ${s['mode'] == 'VOLUNTEER' ? 'volontaire' : 'affecté'}'),
                        trailing: IconButton(icon: const Icon(Icons.person_remove_outlined, color: EduColors.red), onPressed: () => _remove(x, s)),
                      ),
                    const SizedBox(height: 6),
                    Wrap(spacing: 8, runSpacing: 8, children: [
                      FilledButton.tonalIcon(onPressed: sups.length >= needed ? null : () => _assign(x), icon: const Icon(Icons.person_add_alt), label: const Text('Affecter un professeur')),
                      OutlinedButton.icon(onPressed: () => _edit(x), icon: const Icon(Icons.edit_outlined), label: const Text('Modifier')),
                      OutlinedButton.icon(onPressed: () => _cancel(x), icon: const Icon(Icons.block, color: EduColors.red), label: const Text("Annuler l'examen", style: TextStyle(color: EduColors.red))),
                    ]),
                  ],
                ]),
              ),
            );
          }),
      ]),
    );
  }

  Widget _profsTab() {
    if (profs.isEmpty) return ListView(children: const [EmptyState('Aucun professeur')]);
    return RefreshIndicator(
      onRefresh: _load,
      child: PageList([
        for (final p in profs)
          ListTileCard(
            leading: Avatar((str(p['name']) ?? '?').split(' ').where((e) => e.isNotEmpty).take(2).map((e) => e[0].toUpperCase()).join()),
            title: Text(str(p['name']) ?? ''),
            subtitle: Text('${str(p['email']) ?? ''}\n${tr('À venir')} : ${p['upcoming'] ?? 0} · ${tr('Passées')} : ${p['past'] ?? 0}'),
            trailing: DropdownButton<String>(
              value: p['kind'] == 'VACATAIRE' ? 'VACATAIRE' : 'PERMANENT',
              underline: const SizedBox.shrink(),
              items: const [DropdownMenuItem(value: 'PERMANENT', child: Text('Permanent')), DropdownMenuItem(value: 'VACATAIRE', child: Text('Vacataire'))],
              onChanged: (v) { if (v != null) _setKind(p, v); },
            ),
          ),
      ]),
    );
  }

  Widget _historyTab() {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        child: DropdownButtonFormField<String?>(
          value: histProf,
          isExpanded: true,
          decoration: InputDecoration(labelText: tr('Filtrer par professeur'), isDense: true),
          items: [
            const DropdownMenuItem<String?>(value: null, child: Text('Tous les professeurs')),
            for (final p in profs) DropdownMenuItem<String?>(value: '${p['id']}', child: Text(str(p['name']) ?? '', overflow: TextOverflow.ellipsis)),
          ],
          onChanged: (v) { setState(() => histProf = v); _loadHistory(); },
        ),
      ),
      Expanded(child: _HistoryList(rows: history, showProfessor: true)),
    ]);
  }
}
