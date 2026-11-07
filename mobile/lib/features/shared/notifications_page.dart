import 'package:flutter/material.dart';

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

const notifTypeFr = {
  'NEW_COURSE': 'Nouveau cours',
  'TIMETABLE_CHANGE': 'Emploi du temps',
  'NEW_GRADE': 'Nouvelle note',
  'ATTENDANCE_UPDATE': 'PrÃ©sence',
  'ANNOUNCEMENT': 'Annonce',
  'DOCUMENT_REQUEST': 'Demande de document',
};

const notifTypeIcon = {
  'NEW_COURSE': Icons.menu_book_outlined,
  'TIMETABLE_CHANGE': Icons.calendar_month_outlined,
  'NEW_GRADE': Icons.grade_outlined,
  'ATTENDANCE_UPDATE': Icons.event_busy_outlined,
  'ANNOUNCEMENT': Icons.campaign_outlined,
  'DOCUMENT_REQUEST': Icons.description_outlined,
};

/// Notifications + announcements visible to the signed-in user (same merge as the web notifications page).
class NotificationsPage extends StatefulWidget {
  const NotificationsPage({super.key});
  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  int tab = 0;
  Key reloadKey = UniqueKey();

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        child: SegmentedButton<int>(
          showSelectedIcon: false,
          segments: const [
            ButtonSegment(value: 0, label: Text('Notifications'), icon: Icon(Icons.notifications_none)),
            ButtonSegment(value: 1, label: Text('Annonces'), icon: Icon(Icons.campaign_outlined)),
          ],
          selected: {tab},
          onSelectionChanged: (s) => setState(() => tab = s.first),
        ),
      ),
      Expanded(child: tab == 0 ? _Notifs(key: reloadKey) : const _Announcements()),
    ]);
  }
}

class _Notifs extends StatelessWidget {
  const _Notifs({super.key});
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<NotificationItem>>(
      load: () async => listOf(await api.get('/api/notifications'), NotificationItem.fromJson),
      builder: (context, items, reload) {
        if (items.isEmpty) return ListView(children: const [EmptyState('Aucune notification', icon: Icons.notifications_none)]);
        final unread = items.where((n) => !n.read).length;
        return PageList([
          if (unread > 0)
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: () async {
                  try {
                    await api.put('/api/notifications/read-all');
                    await reload();
                  } catch (e) {
                    if (context.mounted) toastError(context, e);
                  }
                },
                icon: const Icon(Icons.done_all),
                label: Text('Tout marquer comme lu ($unread)'),
              ),
            ),
          for (final n in items)
            ListTileCard(
              leading: Container(
                padding: const EdgeInsets.all(9),
                decoration: BoxDecoration(
                    color: EduColors.primary.withValues(alpha: n.read ? 0.06 : 0.14), borderRadius: BorderRadius.circular(10)),
                child: Icon(notifTypeIcon[n.type] ?? Icons.notifications_none,
                    size: 20, color: n.read ? EduColors.slate400 : EduColors.primary),
              ),
              title: Text(n.message, maxLines: 3, overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontWeight: n.read ? FontWeight.w500 : FontWeight.w800)),
              subtitle: Text('${notifTypeFr[n.type] ?? n.type} Â· ${fmtDateTime(n.sentAt)}'),
              trailing: n.read ? null : const Icon(Icons.circle, size: 10, color: EduColors.primary),
              onTap: () async {
                await showDialog<void>(
                  context: context,
                  builder: (_) => AlertDialog(
                    title: Text(notifTypeFr[n.type] ?? 'Notification'),
                    content: Text(n.message),
                    actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Fermer'))],
                  ),
                );
                if (!n.read) {
                  try {
                    await api.put('/api/notifications/${n.id}/read');
                    await reload();
                  } catch (_) {}
                }
              },
            ),
        ]);
      },
    );
  }
}

class _Announcements extends StatelessWidget {
  const _Announcements();
  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return AsyncBody<List<Announcement>>(
      load: () async => listOf(await api.get('/api/announcements'), Announcement.fromJson),
      builder: (context, items, reload) {
        if (items.isEmpty) return ListView(children: const [EmptyState('Aucune annonce', icon: Icons.campaign_outlined)]);
        return PageList([for (final a in items) AnnouncementCard(a)]);
      },
    );
  }
}

class AnnouncementCard extends StatelessWidget {
  final Announcement a;
  final bool showAudience;
  const AnnouncementCard(this.a, {super.key, this.showAudience = false});
  @override
  Widget build(BuildContext context) {
    return Panel(
      title: a.title,
      trailing: showAudience && a.audience != null
          ? Badge2(a.audience == 'CLASS' ? (a.className ?? 'Classe') : (audienceFr[a.audience] ?? a.audience!),
              color: EduColors.primary)
          : null,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(a.content, style: const TextStyle(height: 1.45)),
        const SizedBox(height: 8),
        Text(
          '${a.publishedBy ?? ''}${a.className != null && !showAudience ? ' Â· ${a.className}' : ''} Â· ${fmtDateTime(a.publishedAt)}'
          '${showAudience && a.recipients != null ? ' Â· ${a.recipients} destinataires' : ''}',
          style: TextStyle(fontSize: 12, color: Theme.of(context).hintColor),
        ),
        if (a.attachments.isNotEmpty) ...[
          const SizedBox(height: 10),
          Wrap(spacing: 8, runSpacing: 8, children: [
            for (final f in a.attachments)
              ActionChip(
                avatar: Icon(f.image ? Icons.image_outlined : Icons.attach_file, size: 18),
                label: Text('${f.name}${f.size != null ? ' Â· ${fmtSize(f.size)}' : ''}'),
                onPressed: () => openProtectedFile(context, '/api/announcements/${a.id}/attachments/${f.id}', name: f.name),
              ),
          ]),
        ],
      ]),
    );
  }
}