import 'package:flutter/material.dart';

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

IconData docIcon(String type) {
  final t = type.toLowerCase();
  if (t.contains('pdf')) return Icons.picture_as_pdf_outlined;
  if (t.contains('ppt') || t.contains('presentation')) return Icons.slideshow_outlined;
  if (t.contains('xls') || t.contains('sheet')) return Icons.table_chart_outlined;
  if (t.contains('doc') || t.contains('word')) return Icons.description_outlined;
  if (t.contains('image') || t.contains('png') || t.contains('jpg')) return Icons.image_outlined;
  return Icons.insert_drive_file_outlined;
}

Widget sessionTile(BuildContext context, SessionItem s, {bool showProfessor = true}) => ListTileCard(
      leading: Container(
        width: 62,
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(color: EduColors.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(12)),
        child: Column(children: [
          Text((dayFr[s.dayOfWeek] ?? s.dayOfWeek).substring(0, 3).toUpperCase(),
              style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: EduColors.primary)),
          const SizedBox(height: 2),
          Text(hhmm(s.startTime), style: const TextStyle(fontWeight: FontWeight.w900)),
        ]),
      ),
      title: Text(s.subjectName ?? ''),
      subtitle: Text([
        '${hhmm(s.startTime)}â€“${hhmm(s.endTime)}',
        if ((s.room ?? '').isNotEmpty) 'Salle ${s.room}',
        if ((s.className ?? '').isNotEmpty) s.className!,
        if (showProfessor && (s.professorName ?? '').isNotEmpty) s.professorName!,
      ].join(' Â· ')),
    );

class DocumentTile extends StatelessWidget {
  final DocInfo d;
  final Widget? trailing;
  const DocumentTile(this.d, {super.key, this.trailing});
  @override
  Widget build(BuildContext context) => ListTile(
        contentPadding: EdgeInsets.zero,
        dense: true,
        leading: Icon(docIcon(d.type), color: EduColors.primary),
        title: Text(d.title, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text('${fmtSize(d.size)} Â· ${fmtDate(d.uploadedAt)}'),
        trailing: trailing ?? const Icon(Icons.download_outlined),
        onTap: () => openProtectedFile(context, '/api/documents/${d.id}/download', name: d.title),
      );
}

// ======================= Dashboard =======================
class StudentDashboardPage extends StatelessWidget {
  const StudentDashboardPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      load: () async => Map<String, dynamic>.from(await api.get('/api/student/dashboard') as Map),
      builder: (context, d, reload) {
        final sessions = listOf(d['upcomingSessions'], SessionItem.fromJson);
        final courses = maps(d['latestCourses']);
        final avg = asDouble(d['overallAverage']);
        return PageList([
          StatGrid([
            StatCard(label: 'MatiÃ¨res', value: '${asInt(d['subjects']) ?? 0}', icon: Icons.category_outlined),
            StatCard(label: 'Moyenne gÃ©nÃ©rale', value: avg == null ? 'â€”' : avg.toStringAsFixed(2), icon: Icons.grade_outlined, color: EduColors.emerald),
            StatCard(label: 'Absences', value: '${asInt(d['absences']) ?? 0}', icon: Icons.event_busy_outlined, color: EduColors.red),
            StatCard(label: 'Retards', value: '${asInt(d['lates']) ?? 0}', icon: Icons.schedule, color: EduColors.orange),
          ]),
          Panel(
            title: 'Prochaines sÃ©ances',
            child: sessions.isEmpty
                ? const EmptyState('Aucune sÃ©ance Ã  venir')
                : Column(children: [for (final s in sessions) Padding(padding: const EdgeInsets.only(bottom: 8), child: sessionTile(context, s))]),
          ),
          Panel(
            title: 'Derniers cours',
            child: courses.isEmpty
                ? const EmptyState('Aucun cours publiÃ©')
                : Column(children: [
                    for (final c in courses)
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.menu_book_outlined, color: EduColors.primary),
                        title: Text(str(c['title']) ?? '', style: const TextStyle(fontWeight: FontWeight.w700)),
                        subtitle: Text('${str(c['subjectName']) ?? ''} Â· ${fmtDate(asDate(c['createdAt']))}'),
                      ),
                  ]),
          ),
        ]);
      },
    );
  }
}

// ======================= Subjects =======================
class StudentSubjectsPage extends StatelessWidget {
  const StudentSubjectsPage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<SubjectRef>>(
      load: () async => listOf(await api.get('/api/student/subjects'), SubjectRef.fromJson),
      builder: (context, items, reload) {
        if (items.isEmpty) return ListView(children: const [EmptyState('Aucune matiÃ¨re')]);
        return PageList([
          for (final s in items)
            ListTileCard(
              leading: Avatar((s.code ?? s.name).substring(0, 1).toUpperCase()),
              title: Text(s.name),
              subtitle: Text(s.code ?? ''),
            ),
        ]);
      },
    );
  }
}

// ======================= Courses =======================
class StudentCoursesPage extends StatefulWidget {
  const StudentCoursesPage({super.key});
  @override
  State<StudentCoursesPage> createState() => _StudentCoursesPageState();
}

class _StudentCoursesPageState extends State<StudentCoursesPage> {
  String? subject;
  final search = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<Course>>(
      load: () async => listOf(await api.get('/api/student/courses'), Course.fromJson),
      builder: (context, all, reload) {
        final subjects = {for (final c in all) if (c.subjectName != null) c.subjectName!}.toList()..sort();
        final q = search.text.trim().toLowerCase();
        final items = all
            .where((c) => (subject == null || c.subjectName == subject) && (q.isEmpty || c.title.toLowerCase().contains(q)))
            .toList();
        return PageList([
          TextField(
            controller: search,
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(hintText: 'Rechercher un cours', prefixIcon: Icon(Icons.search)),
          ),
          if (subjects.isNotEmpty)
            SizedBox(
              height: 38,
              child: ListView(scrollDirection: Axis.horizontal, children: [
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(label: const Text('Toutes'), selected: subject == null, onSelected: (_) => setState(() => subject = null)),
                ),
                for (final s in subjects)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(label: Text(s), selected: subject == s, onSelected: (_) => setState(() => subject = s)),
                  ),
              ]),
            ),
          if (items.isEmpty) const EmptyState('Aucun cours', icon: Icons.menu_book_outlined),
          for (final c in items) CourseCard(c),
        ]);
      },
    );
  }
}

class CourseCard extends StatelessWidget {
  final Course c;
  final Widget Function(DocInfo)? docTrailing;
  final Widget? actions;
  const CourseCard(this.c, {super.key, this.docTrailing, this.actions});
  @override
  Widget build(BuildContext context) {
    return Panel(
      title: c.title,
      trailing: c.subjectName == null ? null : Badge2(c.subjectName!, color: EduColors.primary),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if ((c.description ?? '').isNotEmpty) Padding(padding: const EdgeInsets.only(bottom: 6), child: Text(c.description!)),
        Text('${c.professorName ?? ''} Â· ${fmtDate(c.createdAt)}',
            style: TextStyle(fontSize: 12, color: Theme.of(context).hintColor)),
        const SizedBox(height: 8),
        if (c.documents.isEmpty)
          Text('Aucun document', style: TextStyle(color: Theme.of(context).hintColor))
        else
          for (final d in c.documents) DocumentTile(d, trailing: docTrailing?.call(d)),
        if (actions != null) actions!,
      ]),
    );
  }
}

// ======================= Attendance =======================
class StudentAttendancePage extends StatelessWidget {
  const StudentAttendancePage({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<(List<JMap>, List<AttendanceRecord>)>(
      load: () async => (
        maps(await api.get('/api/student/attendance/stats')),
        listOf(await api.get('/api/student/attendance'), AttendanceRecord.fromJson)
      ),
      builder: (context, d, reload) {
        final (stats, records) = d;
        const label = {'PRESENT': 'PrÃ©sent', 'ABSENT': 'Absent', 'LATE': 'En retard'};
        return PageList([
          Panel(
            title: 'Par matiÃ¨re',
            child: stats.isEmpty
                ? const EmptyState('Aucune donnÃ©e')
                : Column(children: [
                    for (final s in stats)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 6),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Row(children: [
                            Expanded(child: Text(str(s['subjectName']) ?? '', style: const TextStyle(fontWeight: FontWeight.w700))),
                            Badge2('${(asDouble(s['absenceRate']) ?? 0).toStringAsFixed(1)} % d\'absence',
                                color: (asDouble(s['absenceRate']) ?? 0) > 20 ? EduColors.red : EduColors.emerald),
                          ]),
                          const SizedBox(height: 4),
                          Text('PrÃ©sences ${asInt(s['present'])} Â· Absences ${asInt(s['absent'])} Â· Retards ${asInt(s['late'])} Â· Total ${asInt(s['total'])}',
                              style: TextStyle(fontSize: 12.5, color: Theme.of(context).hintColor)),
                        ]),
                      ),
                  ]),
          ),
          Panel(
            title: 'Historique',
            child: records.isEmpty
                ? const EmptyState('Aucun enregistrement')
                : Column(children: [
                    for (final r in records)
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        dense: true,
                        title: Text(r.subjectName ?? '', style: const TextStyle(fontWeight: FontWeight.w600)),
                        subtitle: Text('${fmtDate(r.date)}${(r.justification ?? '').isNotEmpty ? ' Â· ${r.justification}' : ''}'),
                        trailing: Badge2(label[r.status] ?? r.status ?? '', color: statusColor(r.status ?? '')),
                      ),
                  ]),
          ),
        ]);
      },
    );
  }
}

// ======================= Grades =======================
class StudentGradesPage extends StatefulWidget {
  const StudentGradesPage({super.key});
  @override
  State<StudentGradesPage> createState() => _StudentGradesPageState();
}

class _StudentGradesPageState extends State<StudentGradesPage> {
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<(JMap, List<Grade>)>(
      load: () async => (
        Map<String, dynamic>.from(await api.get('/api/student/grades/summary') as Map),
        listOf(await api.get('/api/student/grades'), Grade.fromJson)
      ),
      builder: (context, d, reload) {
        final (summary, grades) = d;
        final subjects = maps(summary['subjects']);
        final avg = asDouble(summary['overallAverage']);
        return PageList([
          Card(
            child: Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                gradient: const LinearGradient(colors: [EduColors.violet, EduColors.primary]),
              ),
              child: Row(children: [
                const Icon(Icons.emoji_events_outlined, color: Colors.white, size: 36),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Text('Moyenne gÃ©nÃ©rale', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.w600)),
                    Text(avg == null ? 'â€”' : '${avg.toStringAsFixed(2)} / 20',
                        style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w900)),
                  ]),
                ),
              ]),
            ),
          ),
          Panel(
            title: 'Moyennes par matiÃ¨re',
            child: BarList([
              for (final s in subjects)
                if (asDouble(s['average']) != null) (str(s['subjectName']) ?? '', asDouble(s['average'])!)
            ], max: 20, fmt: (v) => v.toStringAsFixed(2)),
          ),
          Panel(
            title: 'DÃ©tail des notes',
            child: grades.isEmpty
                ? const EmptyState('Aucune note')
                : Column(children: [
                    for (final g in grades)
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        dense: true,
                        title: Text(g.subjectName ?? '', style: const TextStyle(fontWeight: FontWeight.w600)),
                        subtitle: Text('${gradeTypeFr[g.evaluationType] ?? g.evaluationType ?? ''} Â· ${fmtDate(g.date)}'),
                        trailing: Text(num1(g.value),
                            style: TextStyle(
                                fontWeight: FontWeight.w900,
                                fontSize: 16,
                                color: (g.value ?? 0) >= 10 ? EduColors.emerald : EduColors.red)),
                      ),
                  ]),
          ),
        ]);
      },
    );
  }
}