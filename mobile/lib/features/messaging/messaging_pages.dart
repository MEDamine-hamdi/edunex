import 'dart:async';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:provider/provider.dart';

import '../../core/api/api_client.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

String _initials(String name) =>
    name.split(' ').where((p) => p.isNotEmpty).take(2).map((p) => p[0].toUpperCase()).join();

String _clock(DateTime? d) {
  if (d == null) return '';
  final l = d.toLocal();
  return '${l.hour.toString().padLeft(2, '0')}:${l.minute.toString().padLeft(2, '0')}';
}

String _roleOf(String? r) => switch (r) {
      'ROLE_STUDENT' || 'STUDENT' => 'Étudiant',
      'ROLE_PROFESSOR' || 'PROFESSOR' => 'Professeur',
      'ROLE_ADMIN_AGENT' || 'ADMIN_AGENT' => 'Agent',
      _ => r ?? '',
    };

// ---------- protected images (profile / group pictures, image messages) ----------
final Map<String, Future<Uint8List>> _imageCache = {};

Future<Uint8List> _loadImage(ApiClient api, String path) =>
    _imageCache.putIfAbsent(path, () => api.bytes(path).catchError((Object e) {
          _imageCache.remove(path);
          throw e;
        }));

class ProtectedImage extends StatelessWidget {
  final String path;
  final BoxFit fit;
  final double? width;
  final double? height;
  final Widget fallback;
  const ProtectedImage(this.path, {super.key, this.fit = BoxFit.cover, this.width, this.height, required this.fallback});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Uint8List>(
      future: _loadImage(apiOf(context), path),
      builder: (context, snap) {
        if (snap.hasData) return Image.memory(snap.data!, fit: fit, width: width, height: height, gaplessPlayback: true);
        return fallback;
      },
    );
  }
}

class ChatAvatar extends StatelessWidget {
  final String? path;
  final String name;
  final double size;
  const ChatAvatar({super.key, required this.path, required this.name, this.size = 40});

  @override
  Widget build(BuildContext context) {
    final fb = Avatar(_initials(name).isEmpty ? '?' : _initials(name), size: size);
    if (path == null) return fb;
    return ClipOval(child: SizedBox(width: size, height: size, child: ProtectedImage(path!, fallback: fb)));
  }
}

String? _convPhoto(JMap c, int version) {
  final type = str(c['type']);
  if (type == 'DIRECT') {
    final uid = asInt(c['photoUserId']);
    return uid == null ? null : '/api/messaging/users/$uid/photo';
  }
  return c['hasPhoto'] == true ? '/api/messaging/conversations/${c['id']}/photo?v=$version' : null;
}

// ======================= Conversation list =======================
class MessagingPage extends StatefulWidget {
  const MessagingPage({super.key});
  @override
  State<MessagingPage> createState() => _MessagingPageState();
}

class _MessagingPageState extends State<MessagingPage> {
  List<JMap>? rows;
  Object? error;
  Timer? timer;

  @override
  void initState() {
    super.initState();
    _load();
    timer = Timer.periodic(const Duration(seconds: 8), (_) => _load(silent: true));
  }

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    try {
      final r = maps(await apiOf(context).get('/api/messaging/conversations'));
      if (mounted) setState(() { rows = r; error = null; });
    } catch (e) {
      if (mounted && !silent) setState(() => error = e);
    }
  }

  Future<void> _open(JMap c) async {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => ChatPage(conversation: c)));
    if (mounted) _load(silent: true);
  }

  Future<void> _new() async {
    final created = await Navigator.push<JMap>(context, MaterialPageRoute(builder: (_) => const NewChatPage()));
    if (created != null && mounted) {
      await _open(created);
    }
  }

  @override
  Widget build(BuildContext context) {
    Widget body;
    if (error != null && rows == null) {
      body = ErrorState(error!, () { setState(() => error = null); _load(); });
    } else if (rows == null) {
      body = const Center(child: CircularProgressIndicator());
    } else if (rows!.isEmpty) {
      body = ListView(children: const [EmptyState('Aucune conversation', icon: Icons.chat_bubble_outline)]);
    } else {
      body = RefreshIndicator(
        onRefresh: _load,
        child: ListView.separated(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
          itemCount: rows!.length,
          separatorBuilder: (_, __) => const SizedBox(height: 8),
          itemBuilder: (_, i) {
            final c = rows![i];
            final unread = asInt(c['unread']) ?? 0;
            return ListTileCard(
              leading: ChatAvatar(path: _convPhoto(c, 0), name: str(c['title']) ?? '', size: 44),
              title: Text(str(c['title']) ?? '', style: TextStyle(fontWeight: unread > 0 ? FontWeight.w900 : FontWeight.w700), maxLines: 1, overflow: TextOverflow.ellipsis),
              subtitle: Text(str(c['lastText']) ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
              trailing: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
                Text(_clock(asDate(c['lastAt'])), style: TextStyle(fontSize: 11, color: Theme.of(context).hintColor)),
                if (unread > 0) Padding(padding: const EdgeInsets.only(top: 4), child: Badge2('$unread', color: EduColors.primary)),
              ]),
              onTap: () => _open(c),
            );
          },
        ),
      );
    }
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(onPressed: _new, icon: const Icon(Icons.edit_outlined), label: const Text('Nouvelle conversation')),
      body: body,
    );
  }
}

// ======================= New conversation =======================
class NewChatPage extends StatefulWidget {
  const NewChatPage({super.key});
  @override
  State<NewChatPage> createState() => _NewChatPageState();
}

class _NewChatPageState extends State<NewChatPage> {
  final search = TextEditingController();
  List<JMap> contacts = [];
  final picked = <int, String>{};
  bool group = false;
  bool busy = false;
  String? error;
  Timer? debounce;

  @override
  void initState() {
    super.initState();
    _search();
  }

  @override
  void dispose() {
    debounce?.cancel();
    search.dispose();
    super.dispose();
  }

  void _onChanged(String _) {
    debounce?.cancel();
    debounce = Timer(const Duration(milliseconds: 250), _search);
  }

  Future<void> _search() async {
    try {
      final r = maps(await apiOf(context).get('/api/messaging/contacts', query: {'q': search.text.trim()}));
      if (mounted) setState(() { contacts = r; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    }
  }

  Future<void> _direct(JMap c) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      final d = Map<String, dynamic>.from(await apiOf(context).post('/api/messaging/conversations/direct', body: {'userId': asInt(c['id'])}) as Map);
      if (mounted) Navigator.pop(context, d);
    } catch (e) {
      if (mounted) { setState(() => busy = false); toastError(context, e); }
    }
  }

  Future<void> _createGroup() async {
    final api = apiOf(context);
    final name = TextEditingController();
    JMap? created;
    final ok = await showFormSheet(
      context,
      title: 'Nouveau groupe',
      fields: (_) => [Field(name, 'Nom du groupe')],
      action: 'Créer',
      submit: () async {
        if (name.text.trim().isEmpty) throw ApiException(0, 'Le nom du groupe est obligatoire.');
        created = Map<String, dynamic>.from(await api.post('/api/messaging/conversations/group', body: {'name': name.text.trim(), 'memberIds': picked.keys.toList()}) as Map);
      },
    );
    if (ok == true && created != null && mounted) Navigator.pop(context, created);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(group ? 'Nouveau groupe' : 'Nouvelle conversation'),
        actions: [
          TextButton.icon(
            onPressed: () => setState(() { group = !group; picked.clear(); }),
            icon: Icon(group ? Icons.person_outline : Icons.group_add_outlined),
            label: Text(group ? 'Message direct' : 'Groupe'),
          ),
        ],
      ),
      floatingActionButton: group && picked.isNotEmpty
          ? FloatingActionButton.extended(onPressed: _createGroup, icon: const Icon(Icons.check), label: Text('Créer (${picked.length})'))
          : null,
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: TextField(controller: search, onChanged: _onChanged, decoration: InputDecoration(hintText: tr('Rechercher une personne'), prefixIcon: const Icon(Icons.search))),
        ),
        if (error != null) Padding(padding: const EdgeInsets.all(12), child: Text(error!, style: const TextStyle(color: EduColors.red))),
        Expanded(
          child: contacts.isEmpty
              ? ListView(children: const [EmptyState('Aucune personne trouvée', icon: Icons.person_search_outlined)])
              : ListView.builder(
                  padding: const EdgeInsets.only(bottom: 96),
                  itemCount: contacts.length,
                  itemBuilder: (_, i) {
                    final c = contacts[i];
                    final id = asInt(c['id'])!;
                    final name = str(c['name']) ?? '';
                    return ListTile(
                      leading: ChatAvatar(path: '/api/messaging/users/$id/photo', name: name),
                      title: Text(name),
                      subtitle: Text(_roleOf(str(c['role']))),
                      trailing: group ? Checkbox(value: picked.containsKey(id), onChanged: (v) => setState(() => v == true ? picked[id] = name : picked.remove(id))) : null,
                      onTap: group ? () => setState(() => picked.containsKey(id) ? picked.remove(id) : picked[id] = name) : () => _direct(c),
                    );
                  },
                ),
        ),
      ]),
    );
  }
}

// ======================= Chat =======================
class ChatPage extends StatefulWidget {
  final JMap conversation; // ConvRow or ConvDetail (id, type, title, photoUserId, hasPhoto)
  const ChatPage({super.key, required this.conversation});
  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  late JMap conv = widget.conversation;
  int get id => asInt(conv['id'])!;
  bool get isGroup => str(conv['type']) == 'GROUP';

  List<JMap> messages = [];
  String? serverTime;
  bool loading = true;
  bool hasMore = false;
  bool sending = false;
  Object? error;
  PlatformFile? file;
  int photoVersion = 0;
  JMap? detail;
  Timer? timer;
  final input = TextEditingController();
  final scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _first();
    _loadDetail();
    timer = Timer.periodic(const Duration(seconds: 3), (_) => _poll());
  }

  @override
  void dispose() {
    timer?.cancel();
    input.dispose();
    scroll.dispose();
    super.dispose();
  }

  List<JMap> _merge(List<JMap> prev, List<JMap> incoming) {
    if (incoming.isEmpty) return prev;
    final byId = {for (final m in prev) asInt(m['id'])!: m};
    for (final m in incoming) {
      byId[asInt(m['id'])!] = m;
    }
    return byId.values.toList()..sort((a, b) => asInt(a['id'])!.compareTo(asInt(b['id'])!));
  }

  Future<void> _first() async {
    try {
      final p = Map<String, dynamic>.from(await apiOf(context).get('/api/messaging/conversations/$id/messages') as Map);
      final list = maps(p['messages']);
      if (!mounted) return;
      setState(() {
        messages = list;
        serverTime = str(p['serverTime']);
        hasMore = list.length >= 50;
        loading = false;
        error = null;
      });
      _markRead();
      _toBottom();
    } catch (e) {
      if (mounted) setState(() { loading = false; error = e; });
    }
  }

  Future<void> _loadDetail() async {
    try {
      final d = Map<String, dynamic>.from(await apiOf(context).get('/api/messaging/conversations/$id') as Map);
      if (mounted) setState(() { detail = d; conv = {...conv, ...d}; });
    } catch (_) {}
  }

  Future<void> _poll() async {
    final since = serverTime;
    if (since == null || !mounted) return;
    try {
      final p = Map<String, dynamic>.from(await apiOf(context).get('/api/messaging/conversations/$id/messages', query: {'since': since}) as Map);
      final list = maps(p['messages']);
      if (!mounted) return;
      final meId = context.read<AuthController>().user?.id;
      final nearBottom = !scroll.hasClients || scroll.position.maxScrollExtent - scroll.offset < 240;
      setState(() {
        serverTime = str(p['serverTime']);
        if (list.isNotEmpty) messages = _merge(messages, list);
      });
      if (list.any((m) => asInt(m['senderId']) != meId && m['deleted'] != true)) _markRead();
      if (list.isNotEmpty && nearBottom) _toBottom();
    } catch (_) {
      // transient error: the next refresh retries
    }
  }

  Future<void> _older() async {
    if (messages.isEmpty) return;
    try {
      final p = Map<String, dynamic>.from(await apiOf(context).get('/api/messaging/conversations/$id/messages', query: {'before': asInt(messages.first['id'])}) as Map);
      final list = maps(p['messages']);
      if (!mounted) return;
      setState(() {
        messages = _merge(messages, list);
        hasMore = list.length >= 50;
      });
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  Future<void> _markRead() async {
    try {
      await apiOf(context).post('/api/messaging/conversations/$id/read');
    } catch (_) {}
  }

  void _toBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (scroll.hasClients) scroll.jumpTo(scroll.position.maxScrollExtent);
    });
  }

  Future<void> _pick() async {
    final r = await FilePicker.platform.pickFiles(withData: true);
    final f = r?.files.single;
    if (f == null) return;
    if (f.bytes == null) return;
    if (f.size > 25 * 1024 * 1024) {
      if (mounted) toast(context, 'Fichier trop volumineux (25 Mo maximum).', error: true);
      return;
    }
    setState(() => file = f);
  }

  Future<void> _send() async {
    final text = input.text.trim();
    if ((text.isEmpty && file == null) || sending) return;
    final api = apiOf(context);
    setState(() => sending = true);
    try {
      dynamic created;
      if (file != null) {
        created = await api.multipart(
          '/api/messaging/conversations/$id/messages/file',
          fields: text.isEmpty ? const {} : {'content': text},
          files: [UploadFile('file', file!.name, file!.bytes!)],
        );
      } else {
        created = await api.post('/api/messaging/conversations/$id/messages', body: {'content': text});
      }
      if (!mounted) return;
      setState(() {
        messages = _merge(messages, [Map<String, dynamic>.from(created as Map)]);
        input.clear();
        file = null;
        sending = false;
      });
      _toBottom();
    } catch (e) {
      if (mounted) { setState(() => sending = false); toastError(context, e); }
    }
  }

  Future<void> _delete(JMap m) async {
    if (!await confirmDialog(context, 'Supprimer le message', "Supprimer ce message ? Les autres participants verront qu'un message a été supprimé.", danger: true, ok: 'Supprimer')) return;
    if (!mounted) return;
    try {
      final u = Map<String, dynamic>.from(await apiOf(context).delete('/api/messaging/messages/${m['id']}') as Map);
      if (mounted) setState(() => messages = _merge(messages, [u]));
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  Future<void> _info() async {
    final r = await Navigator.push<String>(context, MaterialPageRoute(builder: (_) => GroupInfoPage(conversationId: id, photoVersion: photoVersion)));
    if (!mounted) return;
    if (r == 'left') {
      Navigator.pop(context);
    } else if (r == 'changed') {
      setState(() => photoVersion++);
      _loadDetail();
    }
  }

  @override
  Widget build(BuildContext context) {
    final meId = context.read<AuthController>().user?.id;
    final iAmAdmin = detail?['iAmAdmin'] == true;
    final title = str(conv['title']) ?? '';
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: InkWell(
          onTap: isGroup ? _info : null,
          child: Row(children: [
            ChatAvatar(path: _convPhoto(conv, photoVersion), name: title, size: 36),
            const SizedBox(width: 10),
            Expanded(child: Text(title, maxLines: 1, overflow: TextOverflow.ellipsis)),
          ]),
        ),
        actions: [if (isGroup) IconButton(onPressed: _info, icon: const Icon(Icons.info_outline))],
      ),
      body: Column(children: [
        Expanded(
          child: loading
              ? const Center(child: CircularProgressIndicator())
              : error != null && messages.isEmpty
                  ? ErrorState(error!, () { setState(() { loading = true; error = null; }); _first(); })
                  : messages.isEmpty
                      ? ListView(children: const [EmptyState('Aucun message', icon: Icons.chat_bubble_outline)])
                      : ListView.builder(
                          controller: scroll,
                          padding: const EdgeInsets.all(12),
                          itemCount: messages.length + (hasMore ? 1 : 0),
                          itemBuilder: (_, i) {
                            if (hasMore && i == 0) {
                              return Center(child: TextButton(onPressed: _older, child: const Text('Charger plus')));
                            }
                            final m = messages[i - (hasMore ? 1 : 0)];
                            final mine = asInt(m['senderId']) == meId;
                            return _Bubble(
                              m: m,
                              mine: mine,
                              isGroup: isGroup,
                              onDelete: (!mine && !(isGroup && iAmAdmin)) || m['deleted'] == true ? null : () => _delete(m),
                            );
                          },
                        ),
        ),
        if (file != null)
          Container(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            child: Row(children: [
              const Icon(Icons.attach_file, size: 18),
              const SizedBox(width: 6),
              Expanded(child: Text('${file!.name} · ${fmtSize(file!.size)}', overflow: TextOverflow.ellipsis)),
              IconButton(icon: const Icon(Icons.close), onPressed: () => setState(() => file = null)),
            ]),
          ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(8, 6, 8, 8),
            child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
              IconButton(onPressed: sending ? null : _pick, icon: const Icon(Icons.attach_file)),
              Expanded(
                child: TextField(
                  controller: input,
                  minLines: 1,
                  maxLines: 5,
                  maxLength: 4000,
                  textInputAction: TextInputAction.newline,
                  decoration: InputDecoration(hintText: tr('Écrire un message…'), counterText: '', isDense: true),
                ),
              ),
              const SizedBox(width: 6),
              IconButton.filled(
                onPressed: sending ? null : _send,
                icon: sending ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.send),
              ),
            ]),
          ),
        ),
      ]),
    );
  }
}

class _Bubble extends StatelessWidget {
  final JMap m;
  final bool mine;
  final bool isGroup;
  final VoidCallback? onDelete;
  const _Bubble({required this.m, required this.mine, required this.isGroup, required this.onDelete});

  @override
  Widget build(BuildContext context) {
    final deleted = m['deleted'] == true;
    final scheme = Theme.of(context).colorScheme;
    final fg = deleted ? Theme.of(context).hintColor : (mine ? Colors.white : scheme.onSurface);
    final bg = deleted ? Colors.transparent : (mine ? EduColors.primary : scheme.surfaceContainerHighest);
    final api = apiOf(context);
    final id = asInt(m['id'])!;
    final content = str(m['content']) ?? '';
    return Align(
      alignment: mine ? AlignmentDirectional.centerEnd : AlignmentDirectional.centerStart,
      child: GestureDetector(
        onLongPress: onDelete,
        child: Container(
          constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.78),
          margin: const EdgeInsets.symmetric(vertical: 3),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: bg,
            borderRadius: BorderRadius.circular(16),
            border: deleted ? Border.all(color: Theme.of(context).dividerColor) : null,
          ),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            if (!mine && isGroup && !deleted)
              Text(str(m['senderName']) ?? '', style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w800, color: EduColors.primary)),
            if (deleted)
              Text(m['deletedByOther'] == true ? "Ce message a été supprimé par un administrateur du groupe" : 'Ce message a été supprimé', style: TextStyle(fontStyle: FontStyle.italic, color: fg, fontSize: 13))
            else ...[
              if (m['hasFile'] == true && str(m['fileKind']) == 'IMAGE')
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: GestureDetector(
                    onTap: () => openProtectedFile(context, '/api/messaging/messages/$id/file', name: str(m['fileName']) ?? 'image'),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxHeight: 240),
                        child: ProtectedImage('/api/messaging/messages/$id/file', fit: BoxFit.contain, fallback: const SizedBox(width: 160, height: 100, child: Center(child: CircularProgressIndicator(strokeWidth: 2)))),
                      ),
                    ),
                  ),
                ),
              if (m['hasFile'] == true && str(m['fileKind']) != 'IMAGE')
                InkWell(
                  onTap: () => openProtectedFile(context, '/api/messaging/messages/$id/file', name: str(m['fileName']) ?? 'fichier'),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.insert_drive_file_outlined, color: fg, size: 20),
                    const SizedBox(width: 6),
                    Flexible(child: Text('${str(m['fileName']) ?? ''} · ${fmtSize(asInt(m['fileSize']))}', style: TextStyle(color: fg, decoration: TextDecoration.underline))),
                  ]),
                ),
              if (content.isNotEmpty) Text(content, style: TextStyle(color: fg, fontSize: 15)),
            ],
            Align(
              alignment: AlignmentDirectional.centerEnd,
              child: Text(_clock(asDate(m['createdAt'])), style: TextStyle(fontSize: 10, color: mine && !deleted ? Colors.white70 : Theme.of(context).hintColor)),
            ),
          ]),
        ),
      ),
    );
  }
}

// ======================= Group info =======================
class GroupInfoPage extends StatefulWidget {
  final int conversationId;
  final int photoVersion;
  const GroupInfoPage({super.key, required this.conversationId, required this.photoVersion});
  @override
  State<GroupInfoPage> createState() => _GroupInfoPageState();
}

class _GroupInfoPageState extends State<GroupInfoPage> {
  JMap? detail;
  Object? error;
  bool changed = false;
  int version = 0;

  String get base => '/api/messaging/conversations/${widget.conversationId}';

  @override
  void initState() {
    super.initState();
    version = widget.photoVersion;
    _load();
  }

  Future<void> _load() async {
    try {
      final d = Map<String, dynamic>.from(await apiOf(context).get(base) as Map);
      if (mounted) setState(() { detail = d; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _run(Future<dynamic> Function() action) async {
    try {
      final r = await action();
      if (!mounted) return;
      changed = true;
      if (r is Map) {
        setState(() => detail = Map<String, dynamic>.from(r));
      } else {
        await _load();
      }
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  Future<void> _rename() async {
    final api = apiOf(context);
    final name = TextEditingController(text: str(detail?['title']) ?? '');
    final ok = await showFormSheet(
      context,
      title: 'Renommer le groupe',
      fields: (_) => [Field(name, 'Nom du groupe')],
      submit: () async {
        if (name.text.trim().isEmpty) throw ApiException(0, 'Le nom du groupe est obligatoire.');
        await api.put(base, body: {'name': name.text.trim()});
      },
    );
    if (ok == true) { changed = true; _load(); }
  }

  Future<void> _photo() async {
    final api = apiOf(context);
    final r = await FilePicker.platform.pickFiles(type: FileType.image, withData: true);
    final f = r?.files.single;
    if (f == null || f.bytes == null) return;
    if (f.size > 2 * 1024 * 1024) {
      if (mounted) toast(context, 'La photo ne doit pas dépasser 2 Mo.', error: true);
      return;
    }
    await _run(() => api.multipart('$base/photo', method: 'PUT', files: [UploadFile('file', f.name, f.bytes!)]));
    if (mounted) setState(() => version++);
  }

  Future<void> _addMembers() async {
    final existing = maps(detail?['members']).map((m) => asInt(m['id'])).toSet();
    final ids = await Navigator.push<List<int>>(context, MaterialPageRoute(builder: (_) => _PickMembersPage(exclude: existing)));
    if (ids == null || ids.isEmpty || !mounted) return;
    final api = apiOf(context);
    await _run(() => api.post('$base/members', body: {'userIds': ids}));
  }

  Future<void> _leave(int meId) async {
    if (!await confirmDialog(context, 'Quitter le groupe', 'Quitter ce groupe ?', danger: true, ok: 'Quitter')) return;
    if (!mounted) return;
    try {
      await apiOf(context).delete('$base/members/$meId');
      if (mounted) Navigator.pop(context, 'left');
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final me = context.read<AuthController>().user;
    final d = detail;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) Navigator.pop(context, changed ? 'changed' : null);
      },
      child: Scaffold(
        appBar: AppBar(title: const Text('Le groupe')),
        body: error != null && d == null
            ? ErrorState(error!, () { setState(() => error = null); _load(); })
            : d == null
                ? const Center(child: CircularProgressIndicator())
                : PageList([
                    Panel(
                      child: Column(children: [
                        ChatAvatar(path: d['hasPhoto'] == true ? '$base/photo?v=$version' : null, name: str(d['title']) ?? '', size: 72),
                        const SizedBox(height: 10),
                        Text(str(d['title']) ?? '', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
                        if (d['iAmAdmin'] == true) ...[
                          const SizedBox(height: 10),
                          Wrap(spacing: 8, runSpacing: 8, alignment: WrapAlignment.center, children: [
                            OutlinedButton.icon(onPressed: _rename, icon: const Icon(Icons.edit_outlined), label: const Text('Renommer le groupe')),
                            OutlinedButton.icon(onPressed: _photo, icon: const Icon(Icons.image_outlined), label: const Text('Changer la photo')),
                            if (d['hasPhoto'] == true)
                              OutlinedButton.icon(onPressed: () => _run(() => apiOf(context).delete('$base/photo')), icon: const Icon(Icons.delete_outline), label: const Text('Supprimer la photo')),
                          ]),
                        ],
                      ]),
                    ),
                    Panel(
                      title: 'Membres',
                      trailing: d['iAmAdmin'] == true ? TextButton.icon(onPressed: _addMembers, icon: const Icon(Icons.person_add_alt), label: const Text('Ajouter')) : null,
                      child: Column(children: [
                        for (final m in maps(d['members']))
                          ListTile(
                            dense: true,
                            contentPadding: EdgeInsets.zero,
                            leading: ChatAvatar(path: '/api/messaging/users/${m['id']}/photo', name: str(m['name']) ?? '', size: 34),
                            title: Text(str(m['name']) ?? ''),
                            subtitle: Text(_roleOf(str(m['role']))),
                            trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                              if (m['admin'] == true) const Badge2('Admin', color: EduColors.primary),
                              if (d['iAmAdmin'] == true && asInt(m['id']) != me?.id)
                                IconButton(icon: const Icon(Icons.person_remove_outlined, color: EduColors.red), onPressed: () => _run(() => apiOf(context).delete('$base/members/${m['id']}'))),
                            ]),
                          ),
                      ]),
                    ),
                    OutlinedButton.icon(
                      onPressed: me == null ? null : () => _leave(me.id),
                      icon: const Icon(Icons.logout, color: EduColors.red),
                      label: const Text('Quitter le groupe', style: TextStyle(color: EduColors.red)),
                    ),
                  ]),
      ),
    );
  }
}

class _PickMembersPage extends StatefulWidget {
  final Set<int?> exclude;
  const _PickMembersPage({required this.exclude});
  @override
  State<_PickMembersPage> createState() => _PickMembersPageState();
}

class _PickMembersPageState extends State<_PickMembersPage> {
  final search = TextEditingController();
  List<JMap> contacts = [];
  final picked = <int>{};
  Timer? debounce;

  @override
  void initState() {
    super.initState();
    _search();
  }

  @override
  void dispose() {
    debounce?.cancel();
    search.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    try {
      final r = maps(await apiOf(context).get('/api/messaging/contacts', query: {'q': search.text.trim()}));
      if (mounted) setState(() => contacts = r.where((c) => !widget.exclude.contains(asInt(c['id']))).toList());
    } catch (e) {
      if (mounted) toastError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ajouter')),
      floatingActionButton: picked.isEmpty
          ? null
          : FloatingActionButton.extended(onPressed: () => Navigator.pop(context, picked.toList()), icon: const Icon(Icons.check), label: Text('Ajouter (${picked.length})')),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: TextField(
            controller: search,
            onChanged: (_) {
              debounce?.cancel();
              debounce = Timer(const Duration(milliseconds: 250), _search);
            },
            decoration: InputDecoration(hintText: tr('Rechercher une personne'), prefixIcon: const Icon(Icons.search)),
          ),
        ),
        Expanded(
          child: contacts.isEmpty
              ? ListView(children: const [EmptyState('Aucune personne trouvée', icon: Icons.person_search_outlined)])
              : ListView.builder(
                  padding: const EdgeInsets.only(bottom: 96),
                  itemCount: contacts.length,
                  itemBuilder: (_, i) {
                    final c = contacts[i];
                    final id = asInt(c['id'])!;
                    return CheckboxListTile(
                      value: picked.contains(id),
                      onChanged: (v) => setState(() => v == true ? picked.add(id) : picked.remove(id)),
                      secondary: ChatAvatar(path: '/api/messaging/users/$id/photo', name: str(c['name']) ?? '', size: 38),
                      title: Text(str(c['name']) ?? ''),
                      subtitle: Text(_roleOf(str(c['role']))),
                    );
                  },
                ),
        ),
      ]),
    );
  }
}
