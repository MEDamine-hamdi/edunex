import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/auth/auth_controller.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/theme_controller.dart';
import '../../core/widgets/common.dart';
import '../admin/admin_pages.dart';
import '../admin/admin_pages2.dart';
import '../assistant/assistant_page.dart';
import '../platform/platform_pages.dart';
import '../professor/professor_pages.dart';
import '../shared/notifications_page.dart';
import '../shared/profile_page.dart';
import '../shared/requests_page.dart';
import '../shared/timetable_pages.dart';
import '../student/student_pages.dart';

class NavDest {
  final String label;
  final IconData icon;
  final Widget Function() build;
  const NavDest(this.label, this.icon, this.build);
  String get short => label.startsWith('Emploi') ? 'Planning' : label;
}

List<NavDest> destinationsFor(String role) {
  final notifications = NavDest('Notifications', Icons.notifications_none, () => const NotificationsPage());
  final profile = NavDest('Mon profil', Icons.person_outline, () => const ProfilePage());
  switch (role) {
    case 'ROLE_STUDENT':
      return [
        NavDest('Accueil', Icons.dashboard_outlined, () => const StudentDashboardPage()),
        NavDest('Mes cours', Icons.menu_book_outlined, () => const StudentCoursesPage()),
        NavDest('Emploi du temps', Icons.calendar_month_outlined, () => const WeekTimetablePage(role: 'student')),
        NavDest('Assistant IA', Icons.auto_awesome_outlined, () => const AssistantPage()),
        NavDest('Mes matiÃ¨res', Icons.category_outlined, () => const StudentSubjectsPage()),
        NavDest('Absences', Icons.event_busy_outlined, () => const StudentAttendancePage()),
        NavDest('Notes', Icons.grade_outlined, () => const StudentGradesPage()),
        NavDest('Demandes de documents', Icons.description_outlined, () => const RequestsPage()),
        notifications,
        profile,
      ];
    case 'ROLE_PROFESSOR':
      return [
        NavDest('Accueil', Icons.dashboard_outlined, () => const ProfessorDashboardPage()),
        NavDest('Mes cours', Icons.menu_book_outlined, () => const ProfessorCoursesPage()),
        NavDest('PrÃ©sences', Icons.how_to_reg_outlined, () => const ProfessorAttendancePage()),
        NavDest('Emploi du temps', Icons.calendar_month_outlined, () => const WeekTimetablePage(role: 'professor')),
        NavDest('Mes matiÃ¨res', Icons.category_outlined, () => const ProfessorSubjectsPage()),
        NavDest('Mes classes', Icons.groups_outlined, () => const ProfessorClassesPage()),
        NavDest('Demandes de documents', Icons.description_outlined, () => const RequestsPage()),
        notifications,
        profile,
      ];
    case 'ROLE_ADMIN_AGENT':
      return [
        NavDest('Accueil', Icons.dashboard_outlined, () => const AdminDashboardPage()),
        NavDest('Emplois du temps', Icons.calendar_month_outlined, () => const AdminTimetablesPage()),
        NavDest('Annonces', Icons.campaign_outlined, () => const AdminAnnouncementsPage()),
        NavDest('Demandes', Icons.description_outlined, () => const AdminRequestsPage()),
        NavDest('Professeurs', Icons.school_outlined, () => const AdminProfessorsPage()),
        NavDest('Ã‰tudiants', Icons.people_outline, () => const AdminStudentsPage()),
        NavDest('Classes', Icons.groups_outlined, () => const AdminClassesPage()),
        NavDest('MatiÃ¨res', Icons.category_outlined, () => const AdminSubjectsPage()),
        NavDest('Inscriptions', Icons.how_to_reg_outlined, () => const AdminEnrollmentsPage()),
        NavDest('Notes', Icons.grade_outlined, () => const AdminGradesPage()),
        NavDest('Base de connaissances', Icons.psychology_outlined, () => const AdminKnowledgePage()),
        NavDest('Absences des enseignants', Icons.person_off_outlined, () => const AdminAbsencesPage()),
        notifications,
        profile,
      ];
    default: // ROLE_PLATFORM_ADMIN
      return [
        NavDest('Accueil', Icons.dashboard_outlined, () => const PlatformDashboardPage()),
        NavDest('Agents', Icons.badge_outlined, () => const PlatformAgentsPage()),
        NavDest('Journal', Icons.history, () => const PlatformLogsPage()),
        NavDest('Notifications', Icons.notifications_none, () => const NotificationsPage()),
        NavDest('RÃ´les', Icons.admin_panel_settings_outlined, () => const PlatformRolesPage()),
        NavDest('Permissions', Icons.key_outlined, () => const PlatformPermissionsPage()),
        NavDest('ParamÃ¨tres', Icons.settings_outlined, () => const PlatformSettingsPage()),
        NavDest('E-mails', Icons.mail_outline, () => const PlatformMailPage()),
        profile,
      ];
  }
}

class RoleShell extends StatefulWidget {
  final SessionUser user;
  const RoleShell({super.key, required this.user});
  @override
  State<RoleShell> createState() => _RoleShellState();
}

class _RoleShellState extends State<RoleShell> {
  int index = 0;
  int unread = 0;
  Timer? timer;
  late final List<NavDest> dests = destinationsFor(widget.user.role);
  static const primaryCount = 4;

  @override
  void initState() {
    super.initState();
    _poll();
    timer = Timer.periodic(const Duration(seconds: 60), (_) => _poll());
  }

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  Future<void> _poll() async {
    try {
      final r = await apiOf(context).get('/api/notifications/unread-count') as Map;
      if (mounted) setState(() => unread = (r['count'] as num?)?.toInt() ?? 0);
    } catch (_) {}
  }

  void _openNotifications() {
    Navigator.push(context, MaterialPageRoute(builder: (_) => Scaffold(
          appBar: AppBar(title: const Text('Notifications')),
          body: const NotificationsPage(),
        ))).then((_) => _poll());
  }

  @override
  Widget build(BuildContext context) {
    final u = widget.user;
    final isMore = index >= primaryCount;
    final title = isMore ? 'Plus' : dests[index].label;
    return Scaffold(
      appBar: AppBar(
        title: index == 0 ? const EduLogo(size: 30) : Text(title),
        actions: [
          IconButton(
            tooltip: 'Notifications',
            onPressed: _openNotifications,
            icon: Badge(
              isLabelVisible: unread > 0,
              label: Text(unread > 99 ? '99+' : '$unread'),
              child: const Icon(Icons.notifications_none),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Center(child: Badge2(roleLabel(u.role), color: roleColor(u.role))),
          ),
        ],
      ),
      body: isMore ? _MoreMenu(dests: dests.sublist(primaryCount), user: u) : KeyedSubtree(key: ValueKey(index), child: dests[index].build()),
      bottomNavigationBar: NavigationBar(
        selectedIndex: isMore ? primaryCount : index,
        onDestinationSelected: (i) => setState(() => index = i),
        destinations: [
          for (final d in dests.take(primaryCount))
            NavigationDestination(icon: Icon(d.icon), label: d.short),
          const NavigationDestination(icon: Icon(Icons.apps_rounded), label: 'Plus'),
        ],
      ),
    );
  }
}

class _MoreMenu extends StatelessWidget {
  final List<NavDest> dests;
  final SessionUser user;
  const _MoreMenu({required this.dests, required this.user});
  @override
  Widget build(BuildContext context) {
    return ListView(padding: const EdgeInsets.all(16), children: [
      Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(children: [
            Avatar(user.initials, color: roleColor(user.role), size: 48),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(user.fullName, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                Text(user.email, style: TextStyle(color: Theme.of(context).hintColor, fontSize: 12.5)),
              ]),
            ),
          ]),
        ),
      ),
      const SizedBox(height: 12),
      for (final d in dests)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: ListTileCard(
            leading: Icon(d.icon, color: EduColors.primary),
            title: Text(d.label),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => Scaffold(appBar: AppBar(title: Text(d.label)), body: d.build())),
            ),
          ),
        ),
      const SizedBox(height: 8),
      _ThemeRow(),
      const SizedBox(height: 8),
      OutlinedButton.icon(
        onPressed: () async {
          if (await confirmDialog(context, 'DÃ©connexion', 'Voulez-vous vous dÃ©connecter ?', ok: 'Se dÃ©connecter')) {
            if (context.mounted) await context.read<AuthController>().logout();
          }
        },
        icon: const Icon(Icons.logout, color: EduColors.red),
        label: const Text('Se dÃ©connecter', style: TextStyle(color: EduColors.red)),
      ),
    ]);
  }
}

class _ThemeRow extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final t = context.watch<ThemeController>();
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(children: [
          const Icon(Icons.brightness_6_outlined),
          const SizedBox(width: 10),
          const Expanded(child: Text('ThÃ¨me', style: TextStyle(fontWeight: FontWeight.w700))),
          SegmentedButton<ThemeMode>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: ThemeMode.system, label: Text('Auto')),
              ButtonSegment(value: ThemeMode.light, label: Text('Clair')),
              ButtonSegment(value: ThemeMode.dark, label: Text('Sombre')),
            ],
            selected: {t.mode},
            onSelectionChanged: (s) => t.set(s.first),
          ),
        ]),
      ),
    );
  }
}