import 'package:flutter/material.dart' hide Text;

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

String _explain(Object e) {
  if (e is! ApiException) return 'Une erreur est survenue.';
  final m = e.message;
  if (e.status == 503) return "Le service d'IA est momentanément indisponible. Réessayez dans un instant.";
  if (e.status == 429) return 'Trop de quiz générés : patientez quelques minutes.';
  if (e.status == 400 && m.contains('too little readable text')) {
    return 'Les documents de ce cours contiennent trop peu de texte lisible (les documents scannés ne peuvent pas être lus).';
  }
  if (e.status == 400 && m.contains('no document')) return 'Ce cours ne contient aucun document : le quiz ne peut pas être généré.';
  if (e.status == 403) return "Vous n'avez pas accès à ce cours.";
  return m;
}

/// Button shown on each course card: "Cours terminé : passer le quiz".
/// GET /api/student/courses/{id}/quiz-info
class FinishCourseButton extends StatefulWidget {
  final int courseId;
  final String title;
  const FinishCourseButton({super.key, required this.courseId, required this.title});
  @override
  State<FinishCourseButton> createState() => _FinishCourseButtonState();
}

class _FinishCourseButtonState extends State<FinishCourseButton> {
  JMap? info;
  Key k = UniqueKey();

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final i = Map<String, dynamic>.from(await apiOf(context).get('/api/student/courses/${widget.courseId}/quiz-info') as Map);
      if (mounted) setState(() => info = i);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final done = info?['completed'] == true;
    final score = asInt(info?['lastScore']);
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Wrap(spacing: 10, runSpacing: 6, crossAxisAlignment: WrapCrossAlignment.center, children: [
        FilledButton.icon(
          style: FilledButton.styleFrom(backgroundColor: EduColors.emerald),
          onPressed: () async {
            await Navigator.push(context, MaterialPageRoute(builder: (_) => CourseQuizPage(courseId: widget.courseId, title: widget.title)));
            _load();
          },
          icon: const Icon(Icons.quiz_outlined),
          label: Text(done ? 'Refaire un quiz' : 'Cours terminé : passer le quiz'),
        ),
        if (done)
          Text(score != null ? '${tr('Cours terminé')} · ${tr('dernier quiz')} $score/${info?['lastTotal']}' : 'Cours terminé', style: const TextStyle(color: EduColors.emerald, fontWeight: FontWeight.w700, fontSize: 12)),
      ]),
    );
  }
}

class CourseQuizPage extends StatefulWidget {
  final int courseId;
  final String title;
  const CourseQuizPage({super.key, required this.courseId, required this.title});
  @override
  State<CourseQuizPage> createState() => _CourseQuizPageState();
}

class _CourseQuizPageState extends State<CourseQuizPage> {
  List<JMap> history = [];
  JMap? quiz;
  JMap? result;
  List<int?> answers = [];
  bool busy = false;
  bool loading = true;
  String? error;

  String get base => '/api/student/courses/${widget.courseId}';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final h = maps(await apiOf(context).get('$base/quizzes'));
      if (mounted) setState(() { history = h; loading = false; });
    } catch (e) {
      if (mounted) setState(() { loading = false; error = _explain(e); });
    }
  }

  void _setQuiz(JMap q) {
    quiz = q;
    answers = List<int?>.filled(maps(q['questions']).length, null);
    result = null;
  }

  Future<void> _start() async {
    final api = apiOf(context);
    setState(() { busy = true; error = null; });
    try {
      await api.post('$base/complete');
      final q = Map<String, dynamic>.from(await api.post('$base/quizzes') as Map);
      if (mounted) setState(() { _setQuiz(q); busy = false; });
    } catch (e) {
      if (mounted) setState(() { busy = false; error = _explain(e); });
    }
  }

  Future<void> _open(int id) async {
    setState(() => error = null);
    try {
      final s = Map<String, dynamic>.from(await apiOf(context).get('/api/student/quizzes/$id') as Map);
      if (!mounted) return;
      setState(() {
        if (s['quiz'] != null) {
          _setQuiz(Map<String, dynamic>.from(s['quiz'] as Map));
        } else {
          quiz = null;
          result = Map<String, dynamic>.from(s['result'] as Map);
        }
      });
    } catch (e) {
      if (mounted) setState(() => error = _explain(e));
    }
  }

  Future<void> _submit() async {
    final skipped = answers.where((a) => a == null).length;
    if (skipped > 0 && !await confirmDialog(context, 'Valider mes réponses', '$skipped ${tr('question(s) sans réponse. Valider quand même ?')}', ok: 'Valider')) return;
    if (!mounted) return;
    setState(() { busy = true; error = null; });
    try {
      final r = Map<String, dynamic>.from(await apiOf(context).post('/api/student/quizzes/${quiz!['id']}/submit', body: {'answers': answers}) as Map);
      if (!mounted) return;
      setState(() { result = r; quiz = null; busy = false; });
      _load();
    } catch (e) {
      if (mounted) setState(() { busy = false; error = _explain(e); });
    }
  }

  Widget _errorBox() => Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: const Color(0xFFFEF2F2), borderRadius: BorderRadius.circular(12), border: Border.all(color: const Color(0xFFFCA5A5))),
        child: Text(error!, style: const TextStyle(color: Color(0xFF991B1B))),
      );

  @override
  Widget build(BuildContext context) {
    final List<Widget> body;
    if (loading) {
      body = const [Center(child: Padding(padding: EdgeInsets.all(40), child: CircularProgressIndicator()))];
    } else if (busy && quiz == null && result == null) {
      body = [
        if (error != null) _errorBox(),
        const Panel(child: Row(children: [
          SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2)),
          SizedBox(width: 12),
          Expanded(child: Text("L'IA prépare votre quiz à partir du cours, cela prend quelques secondes...")),
        ])),
      ];
    } else if (quiz != null) {
      body = _quizBody();
    } else if (result != null) {
      body = _resultBody();
    } else {
      body = _homeBody();
    }
    return Scaffold(
      appBar: AppBar(title: Text('${tr('Quiz')} : ${widget.title}')),
      body: PageList(body),
    );
  }

  List<Widget> _homeBody() => [
        if (error != null) _errorBox(),
        Panel(
          title: 'Vous avez terminé ce cours ?',
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text("Le quiz est généré par l'IA à partir du texte des documents du cours. Chaque question a 4 réponses possibles ; vous voyez la correction et une explication à la fin."),
            const SizedBox(height: 12),
            FilledButton.icon(onPressed: _start, icon: const Icon(Icons.auto_awesome), label: const Text('Cours terminé : générer mon quiz')),
          ]),
        ),
        if (history.isNotEmpty)
          Panel(
            title: 'Mes quiz',
            child: Column(children: [
              for (final h in history)
                ListTile(
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  title: Text(fmtDateTime(asDate(h['createdAt']))),
                  subtitle: h['submitted'] == true ? Text('${h['score']}/${h['total']}') : const Text('Non terminé'),
                  trailing: TextButton(onPressed: () => _open(asInt(h['id'])!), child: Text(h['submitted'] == true ? 'Voir la correction' : 'Reprendre')),
                ),
            ]),
          ),
      ];

  List<Widget> _quizBody() {
    final questions = maps(quiz!['questions']);
    final answered = answers.where((a) => a != null).length;
    return [
      if (error != null) _errorBox(),
      Text('$answered / ${questions.length} ${tr('réponses')}', style: TextStyle(color: Theme.of(context).hintColor)),
      for (final q in questions)
        Builder(builder: (context) {
          final qi = asInt(q['index'])!;
          final options = (q['options'] as List).map((e) => e.toString()).toList();
          return Panel(
            title: '${qi + 1}. ${str(q['question']) ?? ''}',
            child: Column(children: [
              for (var i = 0; i < options.length; i++)
                InkWell(
                  borderRadius: BorderRadius.circular(12),
                  onTap: () => setState(() => answers[qi] = i),
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 6),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: answers[qi] == i ? EduColors.primary.withValues(alpha: 0.08) : null,
                      border: Border.all(color: answers[qi] == i ? EduColors.primary : Theme.of(context).dividerColor),
                    ),
                    child: Row(children: [
                      Icon(answers[qi] == i ? Icons.radio_button_checked : Icons.radio_button_unchecked, color: answers[qi] == i ? EduColors.primary : Theme.of(context).hintColor, size: 20),
                      const SizedBox(width: 10),
                      Expanded(child: Text(options[i])),
                    ]),
                  ),
                ),
            ]),
          );
        }),
      FilledButton(
        onPressed: busy ? null : _submit,
        child: busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Text('Valider mes réponses'),
      ),
    ];
  }

  List<Widget> _resultBody() {
    final score = asInt(result!['score']) ?? 0;
    final total = asInt(result!['total']) ?? 0;
    final good = score >= total / 2;
    final msg = score == total ? 'Parfait : vous maîtrisez ce cours.' : good ? 'Bien : relisez les points manqués ci-dessous.' : 'Relisez le cours, puis refaites un quiz.';
    return [
      if (error != null) _errorBox(),
      Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(color: good ? const Color(0xFFECFDF5) : const Color(0xFFFFF7ED), borderRadius: BorderRadius.circular(16), border: Border.all(color: good ? const Color(0xFFA7F3D0) : const Color(0xFFFED7AA))),
        child: Column(children: [
          Text('Votre score', style: TextStyle(color: Theme.of(context).hintColor, fontWeight: FontWeight.w700)),
          Text('$score / $total', style: const TextStyle(fontSize: 36, fontWeight: FontWeight.w900, color: EduColors.slate900)),
          Text(msg, textAlign: TextAlign.center, style: const TextStyle(color: EduColors.slate700)),
        ]),
      ),
      for (final it in maps(result!['items']))
        Builder(builder: (context) {
          final options = (it['options'] as List).map((e) => e.toString()).toList();
          final correct = asInt(it['correct']);
          final chosen = asInt(it['chosen']);
          return Panel(
            title: '${(asInt(it['index']) ?? 0) + 1}. ${str(it['question']) ?? ''}',
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              for (var i = 0; i < options.length; i++)
                Container(
                  margin: const EdgeInsets.only(bottom: 6),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: i == correct ? const Color(0xFFECFDF5) : i == chosen ? const Color(0xFFFEF2F2) : null,
                    border: Border.all(color: i == correct ? const Color(0xFF34D399) : i == chosen ? const Color(0xFFFCA5A5) : Theme.of(context).dividerColor),
                  ),
                  child: Text('${options[i]}${i == correct ? '  ✓' : i == chosen ? '  ✗ ${tr('votre réponse')}' : ''}', style: TextStyle(color: i == correct ? const Color(0xFF065F46) : i == chosen ? const Color(0xFF991B1B) : null)),
                ),
              if (chosen == null) Text('Sans réponse', style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12)),
              if ((str(it['explanation']) ?? '').isNotEmpty)
                Container(
                  margin: const EdgeInsets.only(top: 6),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(color: Theme.of(context).colorScheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(10)),
                  child: Text(str(it['explanation'])!),
                ),
            ]),
          );
        }),
      Wrap(spacing: 8, runSpacing: 8, children: [
        FilledButton(onPressed: busy ? null : _start, child: const Text('Refaire un nouveau quiz')),
        OutlinedButton(onPressed: () => setState(() { result = null; }), child: const Text('Mes quiz')),
      ]),
    ];
  }
}
