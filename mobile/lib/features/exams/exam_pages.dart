import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

const examActionFr = {
  'VOLUNTEERED': "S'est porté volontaire",
  'ASSIGNED': "Affecté par l'administration",
  'WITHDRAWN': "S'est désisté",
  'REMOVED': "Retiré par l'administration",
  'EXAM_CANCELLED': 'Examen annulé',
};
const examTypes = ['Examen', 'Devoir surveillé', 'Rattrapage', 'Autre'];

String _kindFr(String? k) => k == 'VACATAIRE' ? 'Vacataire' : 'Permanent';
Color _kindColor(String? k) => k == 'VACATAIRE' ? const Color(0xFFD97706) : EduColors.emerald;
String _names(dynamic classes) => maps(classes).map((c) => str(c['name']) ?? '').join(', ');
String _time(dynamic t) => hhmm(t?.toString());
String _range(JMap x) => '${_time(x['startTime'])} - ${_time(x['endTime'])}';
DateTime _day(dynamic v) {
  final d = asDate(v) ?? DateTime.now();
  return DateTime(d.year, d.month, d.day);
}

String _longDate(DateTime d) => '${tr(dayFr[dayOrder[d.weekday - 1]] ?? '')} ${fmtDate(d)}';

// ======================= Shared plan view (week or list) =======================
class ExamPlanItem {
  final String key;
  final DateTime date;
  final String start;
  final String end;
  final String title;
  final String detail;
  final String? room;
  final bool cancelled;
  final String? tag;
  const ExamPlanItem({
    required this.key,
    required this.date,
    required this.start,
    required this.end,
    required this.title,
    required this.detail,
    this.room,
    this.cancelled = false,
    this.tag,
  });
}

class ExamPlanView extends StatefulWidget {
  final List<ExamPlanItem> items;
  final String empty;
  const ExamPlanView({super.key, required this.items, required this.empty});
  @override
  State<ExamPlanView> createState() => _ExamPlanViewState();
}

class _ExamPlanViewState extends State<ExamPlanView> {
  bool week = true;
  bool past = false;
  late DateTime monday = _mondayOf(DateTime.now());

  static DateTime _mondayOf(DateTime d) => DateTime(d.year, d.month, d.day).subtract(Duration(days: d.weekday - 1));

  Map<String, List<ExamPlanItem>> get byDate {
    final m = <String, List<ExamPlanItem>>{};
    for (final i in widget.items) {
      m.putIfAbsent(isoDate(i.date), () => []).add(i);
    }
    for (final v in m.values) {
      v.sort((a, b) => a.start.compareTo(b.start));
    }
    return m;
  }

  @override
  Widget build(BuildContext context) {
    final map = byDate;
    final today = isoDate(DateTime.now());
    final children = <Widget>[
      SegmentedButton<bool>(
        showSelectedIcon: false,
        segments: const [ButtonSegment(value: true, label: Text('Semaine')), ButtonSegment(value: false, label: Text('Liste'))],
        selected: {week},
        onSelectionChanged: (s) => setState(() => week = s.first),
      ),
    ];
    if (week) {
      final days = [for (var i = 0; i < 7; i++) monday.add(Duration(days: i))];
      children.add(Row(children: [
        IconButton(onPressed: () => setState(() => monday = monday.subtract(const Duration(days: 7))), icon: const Icon(Icons.chevron_left)),
        Expanded(
          child: InkWell(
            onTap: () => setState(() => monday = _mondayOf(DateTime.now())),
            child: Text('${fmtDate(days.first)} - ${fmtDate(days.last)}', textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w800)),
          ),
        ),
        IconButton(onPressed: () => setState(() => monday = monday.add(const Duration(days: 7))), icon: const Icon(Icons.chevron_right)),
      ]));
      for (final d in days) {
        final list = map[isoDate(d)] ?? const <ExamPlanItem>[];
        children.add(Panel(
          title: _longDate(d),
          trailing: isoDate(d) == today ? const Badge2("Aujourd'hui", color: EduColors.primary) : null,
          child: list.isEmpty
              ? Text('Aucun examen', style: TextStyle(color: Theme.of(context).hintColor))
              : Column(children: [for (final it in list) _PlanCard(it)]),
        ));
      }
    } else {
      final dates = map.keys.where((d) => past || d.compareTo(today) >= 0).toList()..sort();
      children.add(CheckboxListTile(
        contentPadding: EdgeInsets.zero,
        controlAffinity: ListTileControlAffinity.leading,
        value: past,
        onChanged: (v) => setState(() => past = v ?? false),
        title: const Text('Afficher les examens passés'),
      ));
      if (dates.isEmpty) children.add(EmptyState(widget.empty, icon: Icons.event_note_outlined));
      for (final d in dates) {
        children.add(Panel(title: _longDate(DateTime.parse(d)), child: Column(children: [for (final it in map[d]!) _PlanCard(it)])));
      }
    }
    return PageList(children);
  }
}

class _PlanCard extends StatelessWidget {
  final ExamPlanItem it;
  const _PlanCard(this.it);
  @override
  Widget build(BuildContext context) {
    final c = it.cancelled ? EduColors.red : const Color(0xFF7C3AED);
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(color: c.withValues(alpha: 0.08), borderRadius: BorderRadius.circular(10)),
      clipBehavior: Clip.antiAlias,
      child: IntrinsicHeight(
        child: Row(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Container(width: 4, color: c),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(10),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('${hhmm(it.start)} - ${hhmm(it.end)}', style: TextStyle(fontWeight: FontWeight.w800, decoration: it.cancelled ? TextDecoration.lineThrough : null)),
                Text(it.title, style: const TextStyle(fontWeight: FontWeight.w600)),
                Text('${it.detail}${(it.room ?? '').isNotEmpty ? ' · ${tr('Salle')} ${it.room}' : ''}', style: TextStyle(fontSize: 12, color: Theme.of(context).hintColor)),
                if (it.tag != null) Text(it.tag!, style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: Color(0xFF6D28D9))),
                if (it.cancelled) const Text('Examen annulé', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: EduColors.red)),
              ]),
            ),
          ),
        ]),
      ),
    );
  }
}

Widget _historyList(BuildContext context, List<JMap> rows, {bool showProfessor = false}) {
  if (rows.isEmpty) return const EmptyState('Aucun historique', icon: Icons.history);
  return Column(children: [
    for (final h in rows)
      Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: ListTileCard(
          title: Text('${showProfessor ? '${str(h['professor']) ?? ''} · ' : ''}${tr(examActionFr[str(h['action'])] ?? str(h['action']) ?? '')}'),
          subtitle: Text([
            fmtDateTime(asDate(h['at'])),
            if ((str(h['exam']) ?? '').isNotEmpty) str(h['exam'])!,
            if ((str(h['actor']) ?? '').isNotEmpty) '${tr('Par')} ${h['actor']}',
            if ((str(h['reason']) ?? '').isNotEmpty) str(h['reason'])!,
          ].join('\n')),
        ),
      ),
  ]);
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
        empty: 'Aucun examen à venir.',
        items: [
          for (final r in rows)
            ExamPlanItem(
              key: '${r['id']}',
              date: _day(r['date']),
              start: str(r['startTime']) ?? '',
              end: str(r['endTime']) ?? '',
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

// ======================= Professor: /api/professor/exams/* =======================
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
      await _load();
    } catch (e) {
      if (mounted) toast(context, e is ApiException && e.isForbidden ? 'Seuls les vacataires choisissent leurs examens.' : e.toString(), error: true);
    }
  }

  Future<void> _withdraw(JMap exam) async {
    if (!await confirmDialog(context, 'Se désister', 'Vous désister de cette surveillance ?', danger: true, ok: 'Se désister')) return;
    if (!mounted) return;
    try {
      await apiOf(context).delete('/api/professor/exams/${exam['id']}/volunteer');
      await _load();
    } catch (e) {
      if (mounted) toast(context, e is ApiException && e.status == 409 ? "Trop tard pour vous désister : contactez l'administration." : e.toString(), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (error != null) return ErrorState(error!, () { setState(() { loading = true; error = null; }); _load(); });
    final isVac = kind == 'VACATAIRE';
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(isVac ? 'Vous êtes vacataire : vous choisissez les examens que vous surveillez.' : "Vous êtes permanent : l'administration fixe vos surveillances.", style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: SegmentedButton<int>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: 0, label: Text('Mon planning')),
                ButtonSegment(value: 1, label: Text('À surveiller')),
                ButtonSegment(value: 2, label: Text('Historique')),
              ],
              selected: {tab},
              onSelectionChanged: (s) => setState(() => tab = s.first),
            ),
          ),
        ]),
      ),
      Expanded(child: RefreshIndicator(onRefresh: _load, child: _body(context, isVac))),
    ]);
  }

  Widget _body(BuildContext context, bool isVac) {
    if (tab == 1) {
      if (!isVac) {
        return ListView(padding: const EdgeInsets.all(16), children: [
          Panel(child: const Text("En tant que professeur permanent, vous ne choisissez pas vos surveillances : l'administration vous affecte aux examens (voir « Mon planning »). Vous êtes prévenu(e) par notification et par e-mail.")),
        ]);
      }
      return PageList([
        if (available.isEmpty) const EmptyState('Aucun examen à surveiller pour le moment.', icon: Icons.event_available_outlined),
        for (final a in available) _availableCard(context, a),
      ]);
    }
    if (tab == 2) return PageList([_historyList(context, history)]);
    final upcomingVol = mine.where((m) => m['past'] != true && m['mode'] == 'VOLUNTEER').toList();
    return ListView(padding: EdgeInsets.zero, children: [
      SizedBox(
        height: MediaQuery.of(context).size.height * 0.62,
        child: ExamPlanView(
          empty: 'Aucune surveillance à venir.',
          items: [
            for (final m in mine)
              ExamPlanItem(
                key: '${m['exam']['id']}',
                date: _day(m['exam']['date']),
                start: str(m['exam']['startTime']) ?? '',
                end: str(m['exam']['endTime']) ?? '',
                title: '${m['exam']['subjectName']} (${m['exam']['examType']})',
                detail: _names(m['exam']['classes']),
                room: str(m['exam']['room']),
                cancelled: m['exam']['status'] == 'CANCELLED',
                tag: m['mode'] == 'VOLUNTEER' ? 'Surveillance choisie' : "Surveillance imposée par l'administration",
              ),
          ],
        ),
      ),
      if (upcomingVol.isNotEmpty)
        Padding(
          padding: const EdgeInsets.all(16),
          child: Panel(
            title: 'Mes surveillances choisies',
            child: Column(children: [
              for (final m in upcomingVol)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text('${m['exam']['subjectName']} (${m['exam']['examType']})'),
                  subtitle: Text('${_longDate(_day(m['exam']['date']))} · ${_range(Map<String, dynamic>.from(m['exam'] as Map))}'),
                  trailing: m['canWithdraw'] == true
                      ? TextButton(onPressed: () => _withdraw(Map<String, dynamic>.from(m['exam'] as Map)), child: const Text('Se désister', style: TextStyle(color: EduColors.red)))
                      : const SizedBox(width: 110, child: Text("Désistement impossible : contactez l'administration", style: TextStyle(fontSize: 10.5))),
                ),
            ]),
          ),
        ),
    ]);
  }

  Widget _availableCard(BuildContext context, JMap a) {
    final x = Map<String, dynamic>.from(a['exam'] as Map);
    final sup = maps(x['supervisors']).length;
    return Panel(
      title: '${str(x['subjectName']) ?? ''} (${str(x['examType']) ?? ''})',
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('${_longDate(_day(x['date']))} · ${_range(x)} · ${tr('Salle')} ${str(x['room']) ?? '-'}'),
        Text('${_names(x['classes'])} · $sup/${x['needed']} ${tr('surveillants')}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
        const SizedBox(height: 8),
        a['canJoin'] == true
            ? FilledButton(onPressed: () => _join(x), child: const Text('Je surveille'))
            : Badge2(str(a['blocked']) ?? '', color: EduColors.slate500),
      ]),
    );
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
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: tab == 0 ? FloatingActionButton.extended(onPressed: () => _form(context, null), icon: const Icon(Icons.add), label: const Text('Nouvel examen')) : null,
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: SizedBox(
            width: double.infinity,
            child: SegmentedButton<int>(
              showSelectedIcon: false,
              segments: const [ButtonSegment(value: 0, label: Text('Examens')), ButtonSegment(value: 1, label: Text('Surveillants')), ButtonSegment(value: 2, label: Text('Historique'))],
              selected: {tab},
              onSelectionChanged: (s) => setState(() { tab = s.first; k = UniqueKey(); }),
            ),
          ),
        ),
        Expanded(child: switch (tab) { 0 => _ExamsTab(key: k, onChanged: refresh), 1 => _SupervisorsTab(key: k), _ => _HistoryTab(key: k) }),
      ]),
    );
  }

  Future<void> _form(BuildContext context, JMap? exam) async {
    final api = apiOf(context);
    JMap lookups;
    try {
      lookups = Map<String, dynamic>.from(await api.get('/api/admin/exams/lookups') as Map);
    } catch (e) {
      if (context.mounted) toastError(context, e);
      return;
    }
    final subjects = maps(lookups['subjects']);
    final classes = maps(lookups['classes']);
    int? subjectId = asInt(exam?['subjectId']);
    final classIds = <int>{...maps(exam?['classes']).map((c) => asInt(c['id'])!)};
    String type = examTypes.contains(str(exam?['examType'])) ? str(exam?['examType'])! : 'Examen';
    DateTime? date = exam == null ? null : _day(exam['date']);
    TimeOfDay start = const TimeOfDay(hour: 8, minute: 30);
    TimeOfDay end = const TimeOfDay(hour: 10, minute: 30);
    if (exam != null) {
      final s = _time(exam['startTime']).split(':');
      final e = _time(exam['endTime']).split(':');
      start = TimeOfDay(hour: int.parse(s[0]), minute: int.parse(s[1]));
      end = TimeOfDay(hour: int.parse(e[0]), minute: int.parse(e[1]));
    }
    final room = TextEditingController(text: str(exam?['room']) ?? '');
    final needed = TextEditingController(text: '${exam?['needed'] ?? 2}');
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: exam == null ? 'Nouvel examen' : "Modifier l'examen",
      fields: (set) {
        final subject = subjects.where((s) => asInt(s['id']) == subjectId).firstOrNull;
        final allowed = [for (final c in (subject?['classIds'] as List? ?? const [])) (c as num).toInt()];
        return [
          Dropdown<int>(
            label: 'Matière',
            value: subjectId,
            items: [for (final s in subjects) DropdownMenuItem(value: asInt(s['id']), child: Text('${s['name']} (${s['code']})', overflow: TextOverflow.ellipsis))],
            onChanged: (v) => set(() { subjectId = v; classIds.clear(); }),
          ),
          Dropdown<String>(label: 'Type', value: type, items: [for (final t in examTypes) DropdownMenuItem(value: t, child: Text(t))], onChanged: (v) => set(() => type = v ?? type)),
          Field(needed, 'Surveillants nécessaires', keyboard: TextInputType.number),
          Row(children: [
            Expanded(child: OutlinedButton(onPressed: () async { final d = await pickDate(context, initial: date, first: exam == null ? DateTime.now() : null); if (d != null) set(() => date = d); }, child: Text(date == null ? 'Date' : fmtDate(date)))),
          ]),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: start); if (t != null) set(() => start = t); }, child: Text(hhmmOf(start).substring(0, 5)))),
            const SizedBox(width: 8),
            Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: end); if (t != null) set(() => end = t); }, child: Text(hhmmOf(end).substring(0, 5)))),
          ]),
          const SizedBox(height: 12),
          Field(room, 'Salle'),
          const Text('Classes concernées', style: TextStyle(fontWeight: FontWeight.w800)),
          if (subject == null) const Text("Choisissez d'abord une matière."),
          if (subject != null && allowed.isEmpty) const Text('Aucune classe ne suit cette matière.'),
          for (final cid in allowed)
            CheckboxListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading,
              value: classIds.contains(cid),
              onChanged: (v) => set(() => v == true ? classIds.add(cid) : classIds.remove(cid)),
              title: Text(str(classes.where((c) => asInt(c['id']) == cid).firstOrNull?['name']) ?? '$cid'),
            ),
          const SizedBox(height: 8),
        ];
      },
      submit: () async {
        if (subjectId == null) throw ApiException(0, 'Choisissez la matière.');
        if (classIds.isEmpty) throw ApiException(0, 'Choisissez au moins une classe.');
        if (date == null) throw ApiException(0, "Choisissez la date de l'examen.");
        final body = {
          'subjectId': subjectId,
          'classIds': classIds.toList(),
          'examType': type,
          'date': isoDate(date!),
          'startTime': hhmmOf(start),
          'endTime': hhmmOf(end),
          'room': room.text.trim().isEmpty ? null : room.text.trim(),
          'supervisorsNeeded': int.tryParse(needed.text.trim()) ?? 0,
        };
        if (exam == null) {
          await api.post('/api/admin/exams', body: body);
        } else {
          await api.put('/api/admin/exams/${exam['id']}', body: body);
        }
      },
    );
    if (ok == true) {
      if (context.mounted) toast(context, 'Examen enregistré : les étudiants (et les vacataires, pour un nouvel examen) sont prévenus.');
      refresh();
    }
  }
}

class _ExamsTab extends StatefulWidget {
  final VoidCallback onChanged;
  const _ExamsTab({super.key, required this.onChanged});
  @override
  State<_ExamsTab> createState() => _ExamsTabState();
}

class _ExamsTabState extends State<_ExamsTab> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  void _fail(Object e) {
    if (!mounted) return;
    toast(context, e is ApiException && e.status == 409 ? 'Conflit : $e' : e.toString(), error: true);
  }

  Future<void> _edit(JMap x) async {
    final state = context.findAncestorStateOfType<_AdminExamsPageState>();
    await state?._form(context, x);
  }

  Future<void> _cancel(JMap x) async {
    final api = apiOf(context);
    final reason = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: "Annuler l'examen",
      action: 'Annuler',
      fields: (_) => [Text(str(x['subjectName']) ?? ''), Field(reason, 'Motif (facultatif)', maxLines: 2)],
      submit: () => api.post('/api/admin/exams/${x['id']}/cancel', body: {'reason': reason.text.trim()}),
    );
    if (ok == true) refresh();
  }

  Future<void> _assign(JMap x, List<JMap> profs) async {
    final api = apiOf(context);
    final taken = maps(x['supervisors']).map((s) => asInt(s['professorId'])).toSet();
    final options = profs.where((p) => !taken.contains(asInt(p['id']))).toList();
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
          items: [for (final p in options) DropdownMenuItem(value: asInt(p['id']), child: Text('${p['name']} (${_kindFr(str(p['kind'])).toLowerCase()})', overflow: TextOverflow.ellipsis))],
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
      if (mounted) toast(context, (warning ?? '').isNotEmpty ? 'Affecté. Attention : $warning.' : 'Surveillant affecté et prévenu.');
      refresh();
    }
  }

  Future<void> _remove(JMap x, JMap s) async {
    final api = apiOf(context);
    final reason = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: 'Retirer',
      action: 'Retirer',
      fields: (_) => [Text(str(s['name']) ?? ''), Field(reason, 'Motif du retrait (facultatif)', maxLines: 2)],
      submit: () => api.delete('/api/admin/exams/${x['id']}/supervisors/${s['professorId']}?reason=${Uri.encodeQueryComponent(reason.text.trim())}'),
    );
    if (ok == true) refresh();
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<(List<JMap>, List<JMap>)>(
      key: k,
      load: () async => (maps(await api.get('/api/admin/exams')), maps(await api.get('/api/admin/exams/supervisors'))),
      builder: (context, d, reload) {
        final (exams, profs) = d;
        final today = _day(DateTime.now());
        return PageList([
          if (exams.isEmpty) const EmptyState('Aucun examen planifié.', icon: Icons.event_note_outlined),
          for (final x in exams) _examCard(context, x, profs, today),
          const SizedBox(height: 70),
        ]);
      },
    );
  }

  Widget _examCard(BuildContext context, JMap x, List<JMap> profs, DateTime today) {
    final cancelled = x['status'] == 'CANCELLED';
    final sups = maps(x['supervisors']);
    final needed = asInt(x['needed']) ?? 0;
    final missing = needed - sups.length;
    final over = _day(x['date']).isBefore(today);
    return Panel(
      title: '${str(x['subjectName']) ?? ''} (${str(x['examType']) ?? ''})',
      trailing: cancelled
          ? const Badge2('Annulé', color: EduColors.red)
          : Badge2('${sups.length}/$needed ${tr('surveillants')}${missing > 0 ? ' · ${tr('il en manque')} $missing' : ''}', color: missing > 0 ? const Color(0xFFEA580C) : EduColors.emerald),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('${_longDate(_day(x['date']))} · ${_range(x)} · ${tr('Salle')} ${str(x['room']) ?? '-'}'),
        Text('${_names(x['classes'])} · ${x['students']} ${tr('étudiants')}', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
        if (cancelled && (str(x['cancelReason']) ?? '').isNotEmpty) Text('Motif : ${x['cancelReason']}', style: const TextStyle(color: EduColors.red)),
        if (!cancelled) ...[
          const SizedBox(height: 8),
          if (sups.isEmpty) Text('Aucun surveillant.', style: TextStyle(color: Theme.of(context).hintColor)),
          Wrap(spacing: 6, runSpacing: 6, children: [
            for (final s in sups)
              InputChip(
                label: Text('${str(s['name']) ?? ''} · ${_kindFr(str(s['kind']))} · ${s['mode'] == 'VOLUNTEER' ? 'volontaire' : 'affecté'}', style: const TextStyle(fontSize: 12)),
                avatar: CircleAvatar(radius: 5, backgroundColor: _kindColor(str(s['kind']))),
                onDeleted: over ? null : () => _remove(x, s),
              ),
          ]),
          if (!over)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Wrap(spacing: 8, runSpacing: 8, children: [
                if (missing > 0) FilledButton.tonalIcon(onPressed: () => _assign(x, profs), icon: const Icon(Icons.person_add_alt), label: const Text('Affecter')),
                OutlinedButton.icon(onPressed: () => _edit(x), icon: const Icon(Icons.edit_outlined), label: const Text('Modifier')),
                OutlinedButton.icon(onPressed: () => _cancel(x), icon: const Icon(Icons.event_busy, color: EduColors.red), label: const Text('Annuler', style: TextStyle(color: EduColors.red))),
              ]),
            ),
        ],
      ]),
    );
  }
}

class _SupervisorsTab extends StatelessWidget {
  const _SupervisorsTab({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      load: () async => maps(await api.get('/api/admin/exams/supervisors')),
      builder: (context, rows, reload) => StatefulBuilder(builder: (context, set) {
        return PageList([
          Text("Un professeur vacataire choisit lui-même les examens qu'il surveille. Un professeur permanent est affecté par l'administration et ne peut pas refuser. Un professeur sans choix enregistré est considéré comme permanent.", style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
          for (final p in rows)
            Panel(
              title: str(p['name']) ?? '',
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(str(p['email']) ?? '', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
                Text('Surveillances à venir : ${p['upcoming']} · Passées : ${p['past']}'),
                const SizedBox(height: 8),
                Dropdown<String>(
                  label: 'Statut',
                  value: str(p['kind']) ?? 'PERMANENT',
                  items: const [DropdownMenuItem(value: 'PERMANENT', child: Text('Permanent')), DropdownMenuItem(value: 'VACATAIRE', child: Text('Vacataire'))],
                  onChanged: (v) async {
                    if (v == null) return;
                    try {
                      await api.put('/api/admin/exams/supervisors/${p['id']}/kind', body: {'kind': v});
                      set(() => p['kind'] = v);
                    } catch (e) {
                      if (context.mounted) toastError(context, e);
                    }
                  },
                ),
              ]),
            ),
        ]);
      }),
    );
  }
}

class _HistoryTab extends StatefulWidget {
  const _HistoryTab({super.key});
  @override
  State<_HistoryTab> createState() => _HistoryTabState();
}

class _HistoryTabState extends State<_HistoryTab> {
  int? professorId;
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<(List<JMap>, List<JMap>)>(
      key: k,
      load: () async => (
        maps(await api.get('/api/admin/exams/history', query: {if (professorId != null) 'professorId': professorId})),
        maps(await api.get('/api/admin/exams/supervisors')),
      ),
      builder: (context, d, reload) {
        final (rows, profs) = d;
        return PageList([
          Dropdown<int?>(
            label: 'Professeur',
            value: professorId,
            items: [
              const DropdownMenuItem<int?>(value: null, child: Text('Tous les professeurs')),
              for (final p in profs) DropdownMenuItem<int?>(value: asInt(p['id']), child: Text(str(p['name']) ?? '')),
            ],
            onChanged: (v) => setState(() { professorId = v; k = UniqueKey(); }),
          ),
          _historyList(context, rows, showProfessor: true),
        ]);
      },
    );
  }
}
