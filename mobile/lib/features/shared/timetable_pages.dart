import 'package:flutter/material.dart' hide Text;

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

DateTime mondayOf(DateTime d) {
  final day = DateTime(d.year, d.month, d.day);
  return day.subtract(Duration(days: day.weekday - 1));
}

/// Student / professor week view: /api/{role}/timetable/week?date=YYYY-MM-DD
/// Professors may declare or withdraw an absence (POST/DELETE /api/professor/absences).
class WeekTimetablePage extends StatelessWidget {
  final String role; // student | professor
  const WeekTimetablePage({super.key, required this.role});
  @override
  Widget build(BuildContext context) => WeekTimetableView(
        path: '/api/$role/timetable/week',
        mode: role == 'professor' ? WeekMode.professor : WeekMode.readOnly,
      );
}

enum WeekMode { readOnly, professor, admin }

class WeekTimetableView extends StatefulWidget {
  final String path;
  final Map<String, dynamic> query;
  final WeekMode mode;
  const WeekTimetableView({super.key, required this.path, this.query = const {}, this.mode = WeekMode.readOnly});
  @override
  State<WeekTimetableView> createState() => _WeekTimetableViewState();
}

class _WeekTimetableViewState extends State<WeekTimetableView> {
  DateTime week = mondayOf(DateTime.now());
  Key k = UniqueKey();

  @override
  void didUpdateWidget(covariant WeekTimetableView old) {
    super.didUpdateWidget(old);
    if (old.query.toString() != widget.query.toString()) k = UniqueKey();
  }

  void _shift(int days) => setState(() {
        week = week.add(Duration(days: days));
        k = UniqueKey();
      });

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    final end = week.add(const Duration(days: 6));
    return Column(children: [
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Row(children: [
          IconButton(onPressed: () => _shift(-7), icon: const Icon(Icons.chevron_left)),
          Expanded(
            child: InkWell(
              onTap: () => setState(() {
                week = mondayOf(DateTime.now());
                k = UniqueKey();
              }),
              child: Column(children: [
                Text('${fmtDate(week)} – ${fmtDate(end)}', style: const TextStyle(fontWeight: FontWeight.w800)),
                Text('Toucher pour revenir à aujourd\'hui', style: TextStyle(fontSize: 11, color: Theme.of(context).hintColor)),
              ]),
            ),
          ),
          IconButton(onPressed: () => _shift(7), icon: const Icon(Icons.chevron_right)),
        ]),
      ),
      Expanded(
        child: AsyncBody<List<WeekSession>>(
          key: k,
          load: () async => listOf(
              await api.get(widget.path, query: {...widget.query, 'date': isoDate(week)}), WeekSession.fromJson),
          builder: (context, list, reload) {
            if (list.isEmpty) {
              return ListView(children: const [EmptyState('Aucune séance cette semaine', icon: Icons.event_available_outlined)]);
            }
            list.sort((a, b) {
              final c = (a.date ?? DateTime(0)).compareTo(b.date ?? DateTime(0));
              return c != 0 ? c : a.startTime.compareTo(b.startTime);
            });
            final byDay = <String, List<WeekSession>>{};
            for (final s in list) {
              byDay.putIfAbsent(s.date == null ? s.dayOfWeek : isoDate(s.date!), () => []).add(s);
            }
            final today = isoDate(DateTime.now());
            return PageList([
              for (final e in byDay.entries) ...[
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Row(children: [
                    Text(
                      '${dayFr[e.value.first.dayOfWeek] ?? e.value.first.dayOfWeek} ${e.value.first.date != null ? fmtDate(e.value.first.date) : ''}',
                      style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 15),
                    ),
                    if (e.key == today) ...[const SizedBox(width: 8), const Badge2("Aujourd'hui", color: EduColors.primary)],
                  ]),
                ),
                for (final s in e.value) _SessionCard(s, widget.mode, reload),
              ]
            ]);
          },
        ),
      ),
    ]);
  }
}

class _SessionCard extends StatelessWidget {
  final WeekSession s;
  final WeekMode mode;
  final Future<void> Function() reload;
  const _SessionCard(this.s, this.mode, this.reload);

  @override
  Widget build(BuildContext context) {
    final absent = s.absence != null;
    final color = absent ? EduColors.red : EduColors.primary;
    final api = apiOf(context);
    final base = mode == WeekMode.admin ? '/api/admin/absences' : '/api/professor/absences';
    final showActions = mode != WeekMode.readOnly;
    return Container(
      decoration: BoxDecoration(
        border: Border(left: BorderSide(color: color, width: 4)),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Text('${hhmm(s.startTime)} – ${hhmm(s.endTime)}', style: TextStyle(color: color, fontWeight: FontWeight.w900)),
              const Spacer(),
              if (absent) const Badge2('Enseignant absent', color: EduColors.red),
            ]),
            const SizedBox(height: 4),
            Text(s.subjectName ?? '', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15.5)),
            const SizedBox(height: 2),
            Text(
              [
                if ((s.className ?? '').isNotEmpty) s.className!,
                if ((s.room ?? '').isNotEmpty) 'Salle ${s.room}',
                if (mode != WeekMode.professor && (s.professorName ?? '').isNotEmpty) s.professorName!,
              ].join(' · '),
              style: TextStyle(fontSize: 12.5, color: Theme.of(context).hintColor),
            ),
            if (absent && (s.absence!['reason'] ?? '').toString().isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text('Motif : ${s.absence!['reason']}', style: const TextStyle(fontSize: 12.5)),
              ),
            if (showActions && (absent || s.canDeclare)) ...[
              const SizedBox(height: 10),
              if (absent)
                OutlinedButton.icon(
                  onPressed: () async {
                    // professors may only withdraw absences they declared; the backend enforces it
                    try {
                      await api.delete('$base/${s.absenceId}');
                      await reload();
                    } catch (e) {
                      if (context.mounted) toastError(context, e);
                    }
                  },
                  icon: const Icon(Icons.undo),
                  label: const Text('Annuler l\'absence'),
                )
              else
                OutlinedButton.icon(
                  onPressed: () => _declare(context, api, base),
                  icon: const Icon(Icons.person_off_outlined),
                  label: Text(mode == WeekMode.admin ? 'Marquer l\'enseignant absent' : 'Déclarer mon absence'),
                ),
            ],
          ]),
        ),
      ),
    );
  }

  Future<void> _declare(BuildContext context, dynamic api, String base) async {
    final reason = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: 'Déclarer une absence',
      fields: (_) => [
        Text('${s.subjectName} · ${fmtDate(s.date)} · ${hhmm(s.startTime)}–${hhmm(s.endTime)}'),
        const SizedBox(height: 10),
        Field(reason, 'Motif (facultatif)', maxLines: 2),
      ],
      submit: () => api.post(base, body: {
        'sessionId': s.sessionId,
        'date': isoDate(s.date ?? DateTime.now()),
        'reason': reason.text.trim(),
      }),
    );
    if (ok == true) await reload();
  }
}
