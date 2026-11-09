import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

const internshipKinds = ['PFE', 'PFA', "Stage d'été", "Stage d'initiation", 'Autre'];
const jurorRoleFr = {'PRESIDENT': 'Président', 'RAPPORTEUR': 'Rapporteur', 'MEMBER': 'Membre'};

String _person(dynamic p) => p is Map ? (str(p['name']) ?? '') : '';

Widget _resultBadge(num? grade, String status) {
  if (grade == null) return const Badge2('À venir', color: EduColors.slate500);
  return Badge2('$grade/20', color: grade >= 10 ? EduColors.emerald : EduColors.red);
}

Widget _internshipCard(BuildContext context, JMap r, {Widget? actions}) {
  final student = r['student'] as Map?;
  final sup = r['supervisor'] as Map?;
  return Panel(
    title: str(r['title']) ?? '',
    trailing: Badge2(str(r['kind']) ?? '—', color: EduColors.indigo),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('Étudiant : ${_person(student)}${(str(student?['number']) ?? '').isNotEmpty ? ' (${student!['number']})' : ''}'),
      Text('Encadrant : ${_person(sup)}'),
      if ((str(r['company']) ?? '').isNotEmpty)
        Text('Entreprise : ${r['company']}${(str(r['companySupervisor']) ?? '').isNotEmpty ? ' · ${r['companySupervisor']}' : ''}'),
      Text('${fmtDate(asDate(r['startDate']))} → ${fmtDate(asDate(r['endDate']))}', style: TextStyle(color: Theme.of(context).hintColor)),
      if (r['defenseId'] != null)
        Padding(
          padding: const EdgeInsets.only(top: 6),
          child: Text('Soutenance : ${fmtDateTime(asDate(r['defenseAt']))}', style: const TextStyle(fontWeight: FontWeight.w700)),
        ),
      if (actions != null) Padding(padding: const EdgeInsets.only(top: 8), child: actions),
    ]),
  );
}

Widget _defenseCard(BuildContext context, JMap d, VoidCallback onOpen) {
  final jury = maps(d['jury']);
  return ListTileCard(
    title: Text(str(d['title']) ?? ''),
    subtitle: Text(
      '${_person(d['student'])} · ${fmtDateTime(asDate(d['scheduledAt']))}'
      '${(str(d['room']) ?? '').isNotEmpty ? ' · Salle ${d['room']}' : ''}\n'
      'Encadrant : ${_person(d['supervisor'])}'
      '${jury.isNotEmpty ? '\nJury : ${jury.map((j) => j['name']).join(', ')}' : ''}',
    ),
    trailing: _resultBadge(asDouble(d['grade']), str(d['status']) ?? ''),
    onTap: onOpen,
  );
}

void _openDefense(BuildContext context, int id, {bool manager = false}) {
  Navigator.push(
    context,
    MaterialPageRoute(builder: (_) => Scaffold(appBar: AppBar(title: const Text('Soutenance')), body: DefenseDetailView(id: id, manager: manager))),
  );
}

// ======================= Student / professor =======================
/// GET /api/internships/mine and /api/defenses/mine (student, supervisor or jury member).
class InternshipsMinePage extends StatelessWidget {
  const InternshipsMinePage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<(List<JMap>, List<JMap>)>(
      load: () async => (maps(await api.get('/api/internships/mine')), maps(await api.get('/api/defenses/mine'))),
      builder: (context, d, reload) {
        final (internships, defenses) = d;
        return PageList([
          const Text('Stages', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
          if (internships.isEmpty) const EmptyState('Aucun stage', icon: Icons.work_outline),
          for (final r in internships) _internshipCard(context, r),
          const Text('Soutenances', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
          if (defenses.isEmpty) const EmptyState('Aucune soutenance', icon: Icons.event_outlined),
          for (final x in defenses) _defenseCard(context, x, () => _openDefense(context, asInt(x['id'])!)),
        ]);
      },
    );
  }
}

/// GET /api/defenses/{id}; reports/records via /api/defenses/{id}/report|record.
class DefenseDetailView extends StatefulWidget {
  final int id;
  final bool manager;
  const DefenseDetailView({super.key, required this.id, this.manager = false});
  @override
  State<DefenseDetailView> createState() => _DefenseDetailViewState();
}

class _DefenseDetailViewState extends State<DefenseDetailView> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      key: k,
      load: () async => Map<String, dynamic>.from(await api.get('/api/defenses/${widget.id}') as Map),
      builder: (context, d, reload) {
        final jury = maps(d['jury']);
        final grade = asDouble(d['grade']);
        return PageList([
          Panel(
            title: str(d['title']) ?? '',
            trailing: _resultBadge(grade, str(d['status']) ?? ''),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('${fmtDateTime(asDate(d['scheduledAt']))}${(str(d['room']) ?? '').isNotEmpty ? ' · Salle ${d['room']}' : ''}', style: const TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              Text('Étudiant : ${_person(d['student'])}'),
              Text('Encadrant : ${_person(d['supervisor'])}'),
              if ((str(d['company']) ?? '').isNotEmpty) Text('Entreprise : ${d['company']}'),
              if ((str(d['kind']) ?? '').isNotEmpty) Text('Type : ${d['kind']}'),
              if (d['startDate'] != null) Text('${fmtDate(asDate(d['startDate']))} → ${fmtDate(asDate(d['endDate']))}'),
            ]),
          ),
          Panel(
            title: 'Jury',
            child: jury.isEmpty
                ? const EmptyState('Aucun jury')
                : Column(children: [
                    for (final j in jury)
                      ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(str(j['name']) ?? ''),
                        subtitle: Text(str(j['email']) ?? ''),
                        trailing: Badge2(jurorRoleFr[j['role']] ?? str(j['role']) ?? '', color: EduColors.primary),
                      ),
                  ]),
          ),
          if (grade != null)
            Panel(
              title: 'Résultat',
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('$grade / 20${(str(d['mention']) ?? '').isNotEmpty ? ' · ${d['mention']}' : ''}', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
                if ((str(d['decision']) ?? '').isNotEmpty) Text('${d['decision']}'),
                if ((str(d['comments']) ?? '').isNotEmpty) Padding(padding: const EdgeInsets.only(top: 6), child: Text('${d['comments']}')),
              ]),
            ),
          Panel(
            title: 'Documents',
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              _fileRow(context, api, d, 'report', 'Rapport', d['hasReport'] == true, str(d['reportName'])),
              _fileRow(context, api, d, 'record', 'Procès-verbal', d['hasRecord'] == true, str(d['recordName'])),
            ]),
          ),
          if (widget.manager)
            Panel(
              title: 'Gestion',
              child: Wrap(spacing: 8, runSpacing: 8, children: [
                FilledButton.tonalIcon(onPressed: () => _result(context, api, d), icon: const Icon(Icons.grade_outlined), label: const Text('Saisir le résultat')),
              ]),
            ),
        ]);
      },
    );
  }

  Widget _fileRow(BuildContext context, ApiClient api, JMap d, String kind, String label, bool has, String? name) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(children: [
        Expanded(child: Text(has ? '$label · ${name ?? ''}' : '$label : aucun fichier')),
        if (has)
          IconButton(icon: const Icon(Icons.download), onPressed: () => openProtectedFile(context, '/api/defenses/${widget.id}/$kind', name: name ?? kind)),
        if (widget.manager) ...[
          IconButton(
            icon: const Icon(Icons.upload_file),
            onPressed: () async {
              final r = await FilePicker.platform.pickFiles(withData: true);
              final f = r?.files.single;
              if (f == null || f.bytes == null) return;
              try {
                await api.multipart('/api/admin/defenses/${widget.id}/$kind', files: [UploadFile('file', f.name, f.bytes!)]);
                refresh();
              } catch (e) {
                if (context.mounted) toastError(context, e);
              }
            },
          ),
          if (has)
            IconButton(
              icon: const Icon(Icons.delete_outline, color: EduColors.red),
              onPressed: () async {
                try {
                  await api.delete('/api/admin/defenses/${widget.id}/$kind');
                  refresh();
                } catch (e) {
                  if (context.mounted) toastError(context, e);
                }
              },
            ),
        ],
      ]),
    );
  }

  Future<void> _result(BuildContext context, ApiClient api, JMap d) async {
    final grade = TextEditingController(text: d['grade'] == null ? '' : '${d['grade']}');
    final comments = TextEditingController(text: str(d['comments']) ?? '');
    final ok = await showFormSheet(
      context,
      title: 'Résultat de la soutenance',
      fields: (_) => [
        Field(grade, 'Note (0 – 20)', keyboard: const TextInputType.numberWithOptions(decimal: true)),
        Field(comments, 'Commentaires', maxLines: 3),
      ],
      submit: () async {
        final g = double.tryParse(grade.text.replaceAll(',', '.'));
        if (g == null || g < 0 || g > 20) throw ApiException(0, 'La note doit être comprise entre 0 et 20.');
        await api.put('/api/admin/defenses/${widget.id}/result', body: {'grade': g, 'comments': comments.text.trim()});
      },
    );
    if (ok == true) refresh();
  }
}

// ======================= Administration (INTERNSHIPS_MANAGE) =======================
class AdminInternshipsPage extends StatefulWidget {
  const AdminInternshipsPage({super.key});
  @override
  State<AdminInternshipsPage> createState() => _AdminInternshipsPageState();
}

class _AdminInternshipsPageState extends State<AdminInternshipsPage> {
  int tab = 0;
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: tab == 0
          ? FloatingActionButton.extended(onPressed: () => _editInternship(context, null), icon: const Icon(Icons.add), label: const Text('Nouveau stage'))
          : null,
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: SegmentedButton<int>(
            showSelectedIcon: false,
            segments: const [ButtonSegment(value: 0, label: Text('Stages')), ButtonSegment(value: 1, label: Text('Soutenances'))],
            selected: {tab},
            onSelectionChanged: (s) => setState(() {
              tab = s.first;
              k = UniqueKey();
            }),
          ),
        ),
        Expanded(
          child: tab == 0
              ? AsyncBody<List<JMap>>(
                  key: k,
                  load: () async => maps(await api.get('/api/admin/internships')),
                  builder: (context, items, reload) => PageList([
                    if (items.isEmpty) const EmptyState('Aucun stage', icon: Icons.work_outline),
                    for (final r in items)
                      _internshipCard(
                        context,
                        r,
                        actions: Wrap(spacing: 8, runSpacing: 8, children: [
                          OutlinedButton.icon(onPressed: () => _editInternship(context, r), icon: const Icon(Icons.edit_outlined), label: const Text('Modifier')),
                          if (r['defenseId'] == null)
                            OutlinedButton.icon(onPressed: () => _editDefense(context, internship: r), icon: const Icon(Icons.event_available_outlined), label: const Text('Planifier la soutenance'))
                          else
                            OutlinedButton.icon(onPressed: () => _openDefense(context, asInt(r['defenseId'])!, manager: true), icon: const Icon(Icons.open_in_new), label: const Text('Voir la soutenance')),
                          OutlinedButton.icon(
                            onPressed: () async {
                              if (!await confirmDialog(context, 'Supprimer le stage', 'Supprimer ce stage et sa soutenance ?', danger: true, ok: 'Supprimer')) return;
                              try {
                                await api.delete('/api/admin/internships/${r['id']}');
                                refresh();
                              } catch (e) {
                                if (context.mounted) toastError(context, e);
                              }
                            },
                            icon: const Icon(Icons.delete_outline, color: EduColors.red),
                            label: const Text('Supprimer', style: TextStyle(color: EduColors.red)),
                          ),
                        ]),
                      ),
                  ]),
                )
              : AsyncBody<List<JMap>>(
                  key: k,
                  load: () async => maps(await api.get('/api/admin/defenses')),
                  builder: (context, items, reload) => PageList([
                    if (items.isEmpty) const EmptyState('Aucune soutenance', icon: Icons.event_outlined),
                    for (final x in items) _defenseCard(context, x, () async {
                      await Navigator.push(context, MaterialPageRoute(builder: (_) => Scaffold(appBar: AppBar(title: const Text('Soutenance')), body: DefenseDetailView(id: asInt(x['id'])!, manager: true))));
                      refresh();
                    }),
                  ]),
                ),
        ),
      ]),
    );
  }

  Future<void> _editInternship(BuildContext context, JMap? r) async {
    final api = apiOf(context);
    JMap lookups;
    try {
      lookups = Map<String, dynamic>.from(await api.get('/api/admin/internships/lookups') as Map);
    } catch (e) {
      if (context.mounted) toastError(context, e);
      return;
    }
    final students = maps(lookups['students']);
    final profs = maps(lookups['professors']);
    int? studentId = asInt((r?['student'] as Map?)?['id']);
    int? supervisorId = asInt((r?['supervisor'] as Map?)?['id']);
    final company = TextEditingController(text: str(r?['company']) ?? '');
    final companySup = TextEditingController(text: str(r?['companySupervisor']) ?? '');
    final title = TextEditingController(text: str(r?['title']) ?? '');
    String kind = internshipKinds.contains(str(r?['kind'])) ? str(r?['kind'])! : 'PFE';
    DateTime? start = asDate(r?['startDate']);
    DateTime? end = asDate(r?['endDate']);
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: r == null ? 'Nouveau stage' : 'Modifier le stage',
      fields: (set) => [
        Dropdown<int>(
          label: 'Étudiant',
          value: studentId,
          items: [for (final s in students) DropdownMenuItem(value: asInt(s['id']), child: Text('${s['name']} (${str(s['number']) ?? ''})', overflow: TextOverflow.ellipsis))],
          onChanged: (v) => set(() => studentId = v),
        ),
        Dropdown<int>(
          label: 'Encadrant',
          value: supervisorId,
          items: [for (final p in profs) DropdownMenuItem(value: asInt(p['id']), child: Text('${p['name']}', overflow: TextOverflow.ellipsis))],
          onChanged: (v) => set(() => supervisorId = v),
        ),
        Field(title, 'Titre'),
        Dropdown<String>(label: 'Type', value: kind, items: [for (final x in internshipKinds) DropdownMenuItem(value: x, child: Text(x))], onChanged: (v) => set(() => kind = v ?? kind)),
        Field(company, 'Entreprise'),
        Field(companySup, 'Encadrant en entreprise'),
        Row(children: [
          Expanded(child: OutlinedButton(onPressed: () async { final d = await pickDate(context, initial: start); if (d != null) set(() => start = d); }, child: Text(start == null ? 'Début' : fmtDate(start)))),
          const SizedBox(width: 8),
          Expanded(child: OutlinedButton(onPressed: () async { final d = await pickDate(context, initial: end ?? start); if (d != null) set(() => end = d); }, child: Text(end == null ? 'Fin' : fmtDate(end)))),
        ]),
        const SizedBox(height: 12),
      ],
      submit: () async {
        final body = {
          'studentId': studentId,
          'supervisorId': supervisorId,
          'company': company.text.trim(),
          'companySupervisor': companySup.text.trim(),
          'title': title.text.trim(),
          'kind': kind,
          'startDate': start == null ? null : isoDate(start!),
          'endDate': end == null ? null : isoDate(end!),
        };
        if (r == null) {
          await api.post('/api/admin/internships', body: body);
        } else {
          await api.put('/api/admin/internships/${r['id']}', body: body);
        }
      },
    );
    if (ok == true) refresh();
  }

  Future<void> _editDefense(BuildContext context, {required JMap internship}) async {
    final api = apiOf(context);
    List<JMap> profs;
    try {
      profs = maps((await api.get('/api/admin/internships/lookups') as Map)['professors']);
    } catch (e) {
      if (context.mounted) toastError(context, e);
      return;
    }
    DateTime? date;
    TimeOfDay time = const TimeOfDay(hour: 9, minute: 0);
    final room = TextEditingController();
    int? president, rapporteur, member;
    if (!context.mounted) return;
    List<DropdownMenuItem<int>> opts() => [for (final p in profs) DropdownMenuItem(value: asInt(p['id']), child: Text('${p['name']}', overflow: TextOverflow.ellipsis))];
    final ok = await showFormSheet(
      context,
      title: 'Planifier la soutenance',
      fields: (set) => [
        Text(str(internship['title']) ?? ''),
        const SizedBox(height: 10),
        Row(children: [
          Expanded(child: OutlinedButton(onPressed: () async { final d = await pickDate(context, initial: date); if (d != null) set(() => date = d); }, child: Text(date == null ? 'Date' : fmtDate(date)))),
          const SizedBox(width: 8),
          Expanded(child: OutlinedButton(onPressed: () async { final t = await pickTime(context, initial: time); if (t != null) set(() => time = t); }, child: Text(hhmmOf(time).substring(0, 5)))),
        ]),
        const SizedBox(height: 12),
        Field(room, 'Salle'),
        Dropdown<int>(label: 'Président du jury', value: president, items: opts(), onChanged: (v) => set(() => president = v)),
        Dropdown<int>(label: 'Rapporteur', value: rapporteur, items: opts(), onChanged: (v) => set(() => rapporteur = v)),
        Dropdown<int>(label: 'Membre (facultatif)', value: member, items: opts(), onChanged: (v) => set(() => member = v)),
      ],
      submit: () async {
        if (date == null) throw ApiException(0, 'Choisissez la date de la soutenance.');
        if (president == null || rapporteur == null) throw ApiException(0, 'Le jury doit avoir un président et un rapporteur.');
        final ids = [president, rapporteur, if (member != null) member];
        if (ids.toSet().length != ids.length) throw ApiException(0, "Un professeur ne peut apparaître qu'une fois dans le jury.");
        final at = DateTime(date!.year, date!.month, date!.day, time.hour, time.minute);
        await api.post('/api/admin/defenses', body: {
          'internshipId': internship['id'],
          'scheduledAt': at.toIso8601String().split('.').first,
          'room': room.text.trim().isEmpty ? null : room.text.trim(),
          'jury': [
            {'professorId': president, 'role': 'PRESIDENT'},
            {'professorId': rapporteur, 'role': 'RAPPORTEUR'},
            if (member != null) {'professorId': member, 'role': 'MEMBER'},
          ],
        });
      },
    );
    if (ok == true) refresh();
  }
}
