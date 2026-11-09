import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

// ======================= Student: GET /api/student/elimination =======================
class StudentEliminationPage extends StatelessWidget {
  const StudentEliminationPage({super.key});

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      load: () async => maps(await api.get('/api/student/elimination')),
      builder: (context, rows, reload) {
        if (rows.isEmpty) return ListView(children: const [EmptyState('Aucune matière')]);
        return PageList([
          Text("Le nombre d'absences autorisées dans chaque matière. Au-delà, vous êtes éliminé(e) de la matière.", style: TextStyle(color: Theme.of(context).hintColor)),
          for (final r in rows) _studentCard(context, r),
        ]);
      },
    );
  }

  Widget _studentCard(BuildContext context, JMap r) {
    final eliminated = r['eliminated'] == true;
    final waived = r['waived'] == true;
    final max = asInt(r['max']);
    final remaining = asInt(r['remaining']);
    final Widget status;
    if (eliminated) {
      status = const Badge2('Éliminé(e)', color: EduColors.red);
    } else if (waived) {
      status = const Badge2('Élimination levée', color: Color(0xFFD97706));
    } else if (remaining != null) {
      status = remaining == 0
          ? const Badge2('Limite atteinte', color: Color(0xFFEA580C))
          : Badge2('${tr('Encore')} $remaining ${tr('absence(s) possible(s)')}', color: EduColors.emerald);
    } else {
      status = const Badge2('Pas de limite');
    }
    return Panel(
      title: str(r['name']) ?? '',
      trailing: Badge2(str(r['code']) ?? '', color: EduColors.primary),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Absences : ${r['absences'] ?? 0} · Retards : ${r['late'] ?? 0} · Autorisées : ${max ?? '—'}'),
        const SizedBox(height: 8),
        status,
      ]),
    );
  }
}

/// Red banner shown at the top of the student shell when the student is eliminated from a subject.
class EliminationBanner extends StatefulWidget {
  final VoidCallback onOpen;
  const EliminationBanner({super.key, required this.onOpen});
  @override
  State<EliminationBanner> createState() => _EliminationBannerState();
}

class _EliminationBannerState extends State<EliminationBanner> {
  List<String> names = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final rows = maps(await apiOf(context).get('/api/student/elimination'));
      if (mounted) setState(() => names = [for (final r in rows) if (r['eliminated'] == true) str(r['name']) ?? '']);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    if (names.isEmpty) return const SizedBox.shrink();
    return Material(
      color: const Color(0xFFFEF2F2),
      child: InkWell(
        onTap: widget.onOpen,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(children: [
            const Icon(Icons.warning_amber_rounded, color: EduColors.red),
            const SizedBox(width: 10),
            Expanded(
              child: Text("${tr('Vous êtes éliminé(e) de :')} ${names.join(', ')} ${tr("(trop d'absences).")}", style: const TextStyle(color: Color(0xFF991B1B), fontSize: 13, fontWeight: FontWeight.w600)),
            ),
            const Icon(Icons.chevron_right, color: EduColors.red),
          ]),
        ),
      ),
    );
  }
}

// ======================= Administration (ELIMINATION_MANAGE) =======================
class AdminEliminationPage extends StatefulWidget {
  const AdminEliminationPage({super.key});
  @override
  State<AdminEliminationPage> createState() => _AdminEliminationPageState();
}

class _AdminEliminationPageState extends State<AdminEliminationPage> {
  int tab = 0;
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        child: SegmentedButton<int>(
          showSelectedIcon: false,
          segments: const [ButtonSegment(value: 0, label: Text('Seuils')), ButtonSegment(value: 1, label: Text('Éliminations'))],
          selected: {tab},
          onSelectionChanged: (s) => setState(() { tab = s.first; k = UniqueKey(); }),
        ),
      ),
      Expanded(child: tab == 0 ? _PolicyTab(key: k, onChanged: refresh) : _ListTab(key: k)),
    ]);
  }
}

class _PolicyTab extends StatelessWidget {
  final VoidCallback onChanged;
  const _PolicyTab({super.key, required this.onChanged});

  Future<void> _edit(BuildContext context, {required String title, required String path, required int? max, required int? lates}) async {
    final api = apiOf(context);
    final maxC = TextEditingController(text: max == null || max == 0 ? '' : '$max');
    final latesC = TextEditingController(text: '${lates ?? 0}');
    final ok = await showFormSheet(
      context,
      title: title,
      fields: (_) => [
        Field(maxC, 'Absences autorisées', keyboard: TextInputType.number),
        Field(latesC, 'Retards = 1 absence (0 = ignorés)', keyboard: TextInputType.number),
      ],
      submit: () async {
        final m = int.tryParse(maxC.text.trim()) ?? 0;
        final l = int.tryParse(latesC.text.trim()) ?? 0;
        if (m < 0 || m > 99 || l < 0 || l > 20) throw ApiException(0, 'Valeurs invalides (absences : 1 à 99, retards : 0 à 20).');
        await api.put(path, body: {'maxAbsences': m, 'latesPerAbsence': l});
      },
    );
    if (ok == true) {
      if (context.mounted) toast(context, 'Seuil enregistré : les éliminations sont recalculées.');
      onChanged();
    }
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<JMap>(
      load: () async => Map<String, dynamic>.from(await api.get('/api/admin/elimination/policy') as Map),
      builder: (context, p, reload) {
        final def = p['defaultPolicy'] as Map?;
        final defMax = asInt(def?['maxAbsences']);
        final defLates = asInt(def?['latesPerAbsence']);
        final subjects = maps(p['subjects']);
        return PageList([
          Panel(
            title: 'Seuil par défaut (toutes les matières)',
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text("Laissez vide pour désactiver l'élimination partout où une matière n'a pas son propre seuil.", style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
              const SizedBox(height: 8),
              Text('Absences autorisées : ${defMax == null || defMax == 0 ? '—' : defMax} · Retards = 1 absence : ${defLates ?? 0}', style: const TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 10),
              FilledButton.tonalIcon(
                onPressed: () => _edit(context, title: 'Seuil par défaut', path: '/api/admin/elimination/policy/default', max: defMax, lates: defLates),
                icon: const Icon(Icons.tune),
                label: const Text('Modifier'),
              ),
            ]),
          ),
          if (subjects.isEmpty) const EmptyState('Aucune matière'),
          for (final s in subjects)
            Builder(builder: (context) {
              final o = s['override'] as Map?;
              final oMax = asInt(o?['maxAbsences']);
              final oLates = asInt(o?['latesPerAbsence']);
              return Panel(
                title: str(s['name']) ?? '',
                trailing: Badge2(str(s['code']) ?? '', color: EduColors.primary),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(o == null ? 'Seuil par défaut' : 'Seuil propre : ${oMax ?? '—'} · Retards = 1 absence : ${oLates ?? 0}'),
                  const SizedBox(height: 8),
                  Wrap(spacing: 8, runSpacing: 8, children: [
                    OutlinedButton.icon(
                      onPressed: () => _edit(context, title: str(s['name']) ?? '', path: '/api/admin/elimination/policy/subjects/${s['subjectId']}', max: oMax, lates: oLates),
                      icon: const Icon(Icons.edit_outlined),
                      label: const Text('Modifier'),
                    ),
                    if (o != null)
                      OutlinedButton.icon(
                        onPressed: () async {
                          try {
                            await api.delete('/api/admin/elimination/policy/subjects/${s['subjectId']}');
                            onChanged();
                          } catch (e) {
                            if (context.mounted) toastError(context, e);
                          }
                        },
                        icon: const Icon(Icons.undo),
                        label: const Text('Retirer'),
                      ),
                  ]),
                ]),
              );
            }),
        ]);
      },
    );
  }
}

class _ListTab extends StatefulWidget {
  const _ListTab({super.key});
  @override
  State<_ListTab> createState() => _ListTabState();
}

class _ListTabState extends State<_ListTab> {
  Key k = UniqueKey();
  void refresh() => setState(() => k = UniqueKey());

  Future<void> _waive(JMap r) async {
    final api = apiOf(context);
    final reason = TextEditingController();
    final ok = await showFormSheet(
      context,
      title: "Lever l'élimination",
      fields: (_) => [Field(reason, "Motif de la levée de l'élimination (facultatif)", maxLines: 2)],
      submit: () => api.post('/api/admin/elimination/${r['id']}/waive', body: {'reason': reason.text.trim()}),
    );
    if (ok == true) refresh();
  }

  Future<void> _restore(JMap r) async {
    try {
      await apiOf(context).post('/api/admin/elimination/${r['id']}/restore');
      refresh();
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<JMap>>(
      key: k,
      load: () async => maps(await api.get('/api/admin/elimination')),
      builder: (context, rows, reload) => PageList([
        if (rows.isEmpty) const EmptyState('Aucun étudiant éliminé', icon: Icons.verified_outlined),
        for (final r in rows)
          Panel(
            title: str(r['studentName']) ?? '',
            trailing: r['waived'] == true ? const Badge2('Élimination levée', color: Color(0xFFD97706)) : const Badge2('Éliminé(e)', color: EduColors.red),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(str(r['studentNumber']) ?? '', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
              Text('${str(r['subjectName']) ?? ''} (${str(r['subjectCode']) ?? ''})'),
              Text('Absences : ${r['absences']} / ${r['max']}', style: const TextStyle(fontWeight: FontWeight.w700)),
              if (r['waived'] == true)
                Text('Levée par ${str(r['waivedBy']) ?? ''}${(str(r['waiveReason']) ?? '').isNotEmpty ? ' : ${r['waiveReason']}' : ''}', style: const TextStyle(color: Color(0xFF92400E), fontSize: 12.5)),
              const SizedBox(height: 8),
              r['waived'] == true
                  ? OutlinedButton.icon(onPressed: () => _restore(r), icon: const Icon(Icons.restore), label: const Text('Rétablir'))
                  : OutlinedButton.icon(onPressed: () => _waive(r), icon: const Icon(Icons.lock_open_outlined), label: const Text("Lever l'élimination")),
            ]),
          ),
      ]),
    );
  }
}
