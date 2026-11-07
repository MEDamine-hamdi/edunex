import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../../core/api/api_client.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

/// EduNex AI assistant: /api/assistant/* (chat, chat-file, sessions, messages, scope-info).
class AssistantPage extends StatefulWidget {
  const AssistantPage({super.key});
  @override
  State<AssistantPage> createState() => _AssistantPageState();
}

class _AssistantPageState extends State<AssistantPage> {
  final input = TextEditingController();
  final scroll = ScrollController();

  List<SubjectRef> subjects = [];
  List<Course> courses = [];
  bool ready = false;
  Object? initError;

  String scopeKind = 'GENERAL'; // GENERAL | SUBJECT | COURSE
  int? subjectId;
  int? courseId;

  int? sessionId;
  final messages = <ChatMessage>[];
  bool sending = false;
  PlatformFile? attached;
  JMap? scopeInfo;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final api = apiOf(context);
    try {
      subjects = listOf(await api.get('/api/student/subjects'), SubjectRef.fromJson);
      courses = listOf(await api.get('/api/student/courses'), Course.fromJson);
    } catch (e) {
      initError = e;
    }
    ready = true;
    if (mounted) {
      setState(() {});
      _loadScopeInfo();
    }
  }

  Map<String, dynamic> get _scopeQuery => {
        if (scopeKind == 'SUBJECT' && subjectId != null) 'subjectId': subjectId,
        if (scopeKind == 'COURSE' && courseId != null) 'courseId': courseId,
      };

  Future<void> _loadScopeInfo() async {
    try {
      final r = await apiOf(context).get('/api/assistant/scope-info', query: _scopeQuery);
      if (mounted) setState(() => scopeInfo = Map<String, dynamic>.from(r as Map));
    } catch (_) {
      if (mounted) setState(() => scopeInfo = null);
    }
  }

  void _newConversation() {
    setState(() {
      sessionId = null;
      messages.clear();
      attached = null;
    });
  }

  void _setScope(String kind, {int? subject, int? course}) {
    setState(() {
      scopeKind = kind;
      subjectId = subject;
      courseId = course;
      sessionId = null;
      messages.clear();
      scopeInfo = null;
    });
    _loadScopeInfo();
  }

  Future<void> _send() async {
    final text = input.text.trim();
    if (text.isEmpty || sending) return;
    if (text.length > 2000) {
      toast(context, 'Message trop long (2000 caractÃ¨res maximum).', error: true);
      return;
    }
    if ((scopeKind == 'SUBJECT' && subjectId == null) || (scopeKind == 'COURSE' && courseId == null)) {
      toast(context, 'Choisissez d\'abord une matiÃ¨re ou un cours.', error: true);
      return;
    }
    final api = apiOf(context);
    final file = attached;
    setState(() {
      sending = true;
      messages.add(ChatMessage.fromJson({'role': 'USER', 'content': text, 'attachmentName': file?.name}));
      input.clear();
      attached = null;
    });
    _toBottom();
    try {
      dynamic res;
      if (file != null) {
        res = await api.multipart('/api/assistant/chat-file', fields: {
          'message': text,
          if (sessionId != null) 'sessionId': '$sessionId',
          if (scopeKind == 'SUBJECT' && subjectId != null) 'subjectId': '$subjectId',
          if (scopeKind == 'COURSE' && courseId != null) 'courseId': '$courseId',
        }, files: [UploadFile('file', file.name, file.bytes!)]);
      } else {
        res = await api.post('/api/assistant/chat',
            body: {
              'sessionId': sessionId,
              'subjectId': scopeKind == 'SUBJECT' ? subjectId : null,
              'courseId': scopeKind == 'COURSE' ? courseId : null,
              'message': text,
            },
            timeout: const Duration(minutes: 2));
      }
      final r = Map<String, dynamic>.from(res as Map);
      sessionId = asInt(r['sessionId']);
      messages.add(ChatMessage.fromJson(Map<String, dynamic>.from(r['assistantMessage'] as Map)));
    } catch (e) {
      final status = e is ApiException ? e.status : 0;
      final text = e is ApiException ? e.message : e.toString();
      if (mounted) {
        messages.add(ChatMessage.fromJson({
          'role': 'ASSISTANT',
          'content': status == 503
              ? 'L\'assistant IA est momentanÃ©ment indisponible. RÃ©essayez dans un instant.'
              : status == 429
                  ? 'Vous envoyez trop de messages. Patientez un peu avant de rÃ©essayer.'
                  : text,
        }));
      }
    }
    if (mounted) {
      setState(() => sending = false);
      _toBottom();
    }
  }

  void _toBottom() => WidgetsBinding.instance.addPostFrameCallback((_) {
        if (scroll.hasClients) scroll.animateTo(scroll.position.maxScrollExtent + 200, duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
      });

  Future<void> _pickFile() async {
    final r = await FilePicker.platform.pickFiles(
      withData: true,
      type: FileType.custom,
      allowedExtensions: const ['pdf', 'docx', 'pptx', 'xlsx', 'odt', 'odp', 'txt', 'md'],
    );
    final f = r?.files.single;
    if (f == null || f.bytes == null) return;
    if (f.size > 10 * 1024 * 1024) {
      if (mounted) toast(context, 'Fichier trop volumineux (10 Mo maximum).', error: true);
      return;
    }
    setState(() => attached = f);
  }

  Future<void> _history() async {
    final api = apiOf(context);
    final picked = await showModalBottomSheet<ChatSession>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (ctx) => SizedBox(
        height: MediaQuery.of(ctx).size.height * 0.7,
        child: StatefulBuilder(builder: (ctx, set) {
          return FutureBuilder<List<ChatSession>>(
            future: api.get('/api/assistant/sessions', query: _scopeQuery).then((v) => listOf(v, ChatSession.fromJson)),
            builder: (ctx, snap) {
              if (snap.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
              if (snap.hasError) return ErrorState(snap.error!, () => set(() {}));
              final items = snap.data!;
              if (items.isEmpty) return const EmptyState('Aucune conversation pour ce contexte', icon: Icons.chat_bubble_outline);
              return ListView(padding: const EdgeInsets.all(16), children: [
                for (final s in items)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: ListTileCard(
                      title: Text(s.title, maxLines: 2, overflow: TextOverflow.ellipsis),
                      subtitle: Text(fmtDateTime(s.updatedAt)),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete_outline, color: EduColors.red),
                        onPressed: () async {
                          if (!await confirmDialog(ctx, 'Supprimer la conversation', 'Supprimer Â« ${s.title} Â» ?', danger: true, ok: 'Supprimer')) return;
                          try {
                            await api.delete('/api/assistant/sessions/${s.id}');
                            if (sessionId == s.id) _newConversation();
                            set(() {});
                          } catch (e) {
                            if (ctx.mounted) toastError(ctx, e);
                          }
                        },
                      ),
                      onTap: () => Navigator.pop(ctx, s),
                    ),
                  ),
              ]);
            },
          );
        }),
      ),
    );
    if (picked == null || !mounted) return;
    try {
      final list = listOf(await api.get('/api/assistant/sessions/${picked.id}/messages'), ChatMessage.fromJson);
      setState(() {
        sessionId = picked.id;
        messages
          ..clear()
          ..addAll(list);
      });
      _toBottom();
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  Future<void> _openSource(SourceRef s) async {
    final path = s.kind == 'INSTITUTE'
        ? '/api/institute/documents/${s.documentId}/download'
        : '/api/documents/${s.documentId}/download';
    await openProtectedFile(context, path, name: s.documentTitle ?? 'source');
  }

  @override
  Widget build(BuildContext context) {
    if (!ready) return const Center(child: CircularProgressIndicator());
    if (initError != null) return ErrorState(initError!, () {
      setState(() => ready = false);
      _init();
    });
    return Column(children: [
      _scopeBar(context),
      if (scopeInfo != null) _scopeNotice(context),
      Expanded(
        child: messages.isEmpty
            ? const EmptyState('Posez une question sur vos cours ou sur le rÃ¨glement de l\'Ã©tablissement.', icon: Icons.auto_awesome_outlined)
            : ListView.builder(
                controller: scroll,
                padding: const EdgeInsets.all(16),
                itemCount: messages.length + (sending ? 1 : 0),
                itemBuilder: (_, i) {
                  if (i == messages.length) {
                    return const Align(
                      alignment: Alignment.centerLeft,
                      child: Padding(padding: EdgeInsets.all(12), child: SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2))),
                    );
                  }
                  return _bubble(context, messages[i]);
                },
              ),
      ),
      _composer(context),
    ]);
  }

  Widget _scopeBar(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
      decoration: BoxDecoration(border: Border(bottom: BorderSide(color: Theme.of(context).dividerColor))),
      child: Column(children: [
        Row(children: [
          Expanded(
            child: SegmentedButton<String>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: 'GENERAL', label: Text('GÃ©nÃ©ral')),
                ButtonSegment(value: 'SUBJECT', label: Text('MatiÃ¨re')),
                ButtonSegment(value: 'COURSE', label: Text('Cours')),
              ],
              selected: {scopeKind},
              onSelectionChanged: (s) => _setScope(s.first),
            ),
          ),
          IconButton(tooltip: 'Historique', onPressed: _history, icon: const Icon(Icons.history)),
          IconButton(tooltip: 'Nouvelle conversation', onPressed: _newConversation, icon: const Icon(Icons.add_comment_outlined)),
        ]),
        if (scopeKind == 'SUBJECT')
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: DropdownButtonFormField<int>(
              value: subjectId,
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'MatiÃ¨re', isDense: true),
              items: [for (final s in subjects) DropdownMenuItem(value: s.id, child: Text(s.label, overflow: TextOverflow.ellipsis))],
              onChanged: (v) => _setScope('SUBJECT', subject: v),
            ),
          ),
        if (scopeKind == 'COURSE')
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: DropdownButtonFormField<int>(
              value: courseId,
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Cours', isDense: true),
              items: [for (final c in courses) DropdownMenuItem(value: c.id, child: Text('${c.title} Â· ${c.subjectName ?? ''}', overflow: TextOverflow.ellipsis))],
              onChanged: (v) => _setScope('COURSE', course: v),
            ),
          ),
      ]),
    );
  }

  Widget _scopeNotice(BuildContext context) {
    final info = scopeInfo!;
    final available = info['available'] == true;
    final docs = asInt(info['documents']) ?? 0;
    final indexed = asInt(info['indexedDocuments']) ?? 0;
    final text = available
        ? '${str(info['label']) ?? ''} Â· $indexed/$docs document(s) indexÃ©(s)'
        : 'Aucun document indexÃ© pour Â« ${str(info['label']) ?? ''} Â» : l\'assistant rÃ©pondra sans sources de cours.';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      color: (available ? EduColors.primary : EduColors.orange).withValues(alpha: 0.1),
      child: Text(text, style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: available ? EduColors.primary : EduColors.orange)),
    );
  }

  Widget _bubble(BuildContext context, ChatMessage m) {
    final user = m.isUser;
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Align(
      alignment: user ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.86),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: user ? EduColors.primary : (dark ? const Color(0xFF18181B) : Colors.white),
          border: user ? null : Border.all(color: Theme.of(context).dividerColor),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(user ? 16 : 4),
            bottomRight: Radius.circular(user ? 4 : 16),
          ),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (m.attachmentName != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                Icon(Icons.attach_file, size: 14, color: user ? Colors.white70 : null),
                const SizedBox(width: 4),
                Flexible(child: Text(m.attachmentName!, style: TextStyle(fontSize: 12, color: user ? Colors.white70 : null))),
              ]),
            ),
          SelectableText(m.content, style: TextStyle(height: 1.45, color: user ? Colors.white : null)),
          if (!user && m.sources.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text('Sources', style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w800, color: Theme.of(context).hintColor)),
            const SizedBox(height: 4),
            Wrap(spacing: 6, runSpacing: 6, children: [
              for (final s in m.sources)
                ActionChip(
                  visualDensity: VisualDensity.compact,
                  avatar: CircleAvatar(
                    radius: 10,
                    backgroundColor: EduColors.primary,
                    child: Text('${s.n}', style: const TextStyle(fontSize: 10, color: Colors.white)),
                  ),
                  label: Text(
                    [
                      s.documentTitle ?? 'Document',
                      if ((s.courseTitle ?? '').isNotEmpty) s.courseTitle!,
                      if (s.kind == 'INSTITUTE') 'Guide de l\'Ã©tablissement',
                    ].join(' Â· '),
                    style: const TextStyle(fontSize: 11.5),
                    overflow: TextOverflow.ellipsis,
                  ),
                  onPressed: s.documentId == null ? null : () => _openSource(s),
                ),
            ]),
          ],
        ]),
      ),
    );
  }

  Widget _composer(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(8, 8, 8, 8),
        decoration: BoxDecoration(border: Border(top: BorderSide(color: Theme.of(context).dividerColor))),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          if (attached != null)
            Align(
              alignment: Alignment.centerLeft,
              child: Padding(
                padding: const EdgeInsets.only(left: 8, bottom: 6),
                child: InputChip(
                  avatar: const Icon(Icons.attach_file, size: 16),
                  label: Text(attached!.name, overflow: TextOverflow.ellipsis),
                  onDeleted: () => setState(() => attached = null),
                ),
              ),
            ),
          Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
            IconButton(onPressed: sending ? null : _pickFile, icon: const Icon(Icons.attach_file)),
            Expanded(
              child: TextField(
                controller: input,
                minLines: 1,
                maxLines: 5,
                maxLength: 2000,
                textInputAction: TextInputAction.newline,
                decoration: const InputDecoration(hintText: 'Posez votre questionâ€¦', counterText: '', isDense: true),
              ),
            ),
            const SizedBox(width: 6),
            IconButton.filled(
              onPressed: sending ? null : _send,
              icon: const Icon(Icons.send_rounded),
              style: IconButton.styleFrom(backgroundColor: EduColors.primary, foregroundColor: Colors.white),
            ),
          ]),
        ]),
      ),
    );
  }
}