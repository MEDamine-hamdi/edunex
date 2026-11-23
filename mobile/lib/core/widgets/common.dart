import 'dart:io';

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart' hide Text;
import 'package:intl/intl.dart';
import 'package:open_filex/open_filex.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../i18n/i18n.dart';
import '../i18n/text.dart';
import '../auth/auth_controller.dart';
import '../platform/file_saver_stub.dart' if (dart.library.js_interop) '../platform/file_saver_web.dart';
import '../theme/app_theme.dart';

export '../i18n/i18n.dart' show tr, dateLocale, I18n, LangScope;
export '../i18n/text.dart';

ApiClient apiOf(BuildContext c) => c.read<ApiClient>();

// ---------- logo ----------
class EduLogo extends StatelessWidget {
  final double size;
  final bool withText;
  const EduLogo({super.key, this.size = 36, this.withText = true});
  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Row(mainAxisSize: MainAxisSize.min, children: [
      Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(size * 0.28),
          gradient: const LinearGradient(
              begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [EduColors.violet, EduColors.primary]),
        ),
        child: Icon(Icons.school_rounded, color: Colors.white, size: size * 0.62),
      ),
      if (withText) ...[
        SizedBox(width: size * 0.3),
        Text('EduNex',
            style: TextStyle(
                fontSize: size * 0.58,
                fontWeight: FontWeight.w900,
                letterSpacing: -0.5,
                color: dark ? Colors.white : EduColors.slate900)),
      ]
    ]);
  }
}

// ---------- formatting ----------
String fmtDate(DateTime? d) => d == null ? '—' : DateFormat('dd MMM yyyy', dateLocale()).format(d);
String fmtDateTime(DateTime? d) => d == null ? '—' : DateFormat('dd MMM yyyy · HH:mm', dateLocale()).format(d.toLocal());
String isoDate(DateTime d) => DateFormat('yyyy-MM-dd').format(d);
String hhmm(String? t) => (t == null || t.length < 5) ? (t ?? '') : t.substring(0, 5);
String fmtSize(int? b) {
  if (b == null) return '';
  if (b < 1024) return '$b o';
  if (b < 1024 * 1024) return '${(b / 1024).toStringAsFixed(0)} Ko';
  return '${(b / 1024 / 1024).toStringAsFixed(1)} Mo';
}

String num1(num? v) => v == null ? '—' : v.toStringAsFixed(2);

const dayFr = {
  'MONDAY': 'Lundi',
  'TUESDAY': 'Mardi',
  'WEDNESDAY': 'Mercredi',
  'THURSDAY': 'Jeudi',
  'FRIDAY': 'Vendredi',
  'SATURDAY': 'Samedi',
  'SUNDAY': 'Dimanche',
};
const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const gradeTypeFr = {
  'EXAM': 'Examen',
  'QUIZ': 'Quiz',
  'ASSIGNMENT': 'Devoir',
  'PROJECT': 'Projet',
  'PRACTICAL': 'TP',
};
const statusFr = {'ACTIVE': 'Actif', 'INACTIVE': 'Inactif', 'SUSPENDED': 'Suspendu'};
const audienceFr = {
  'ALL': 'Tout le monde',
  'STUDENTS': 'Étudiants',
  'PROFESSORS': 'Professeurs',
  'ADMIN_AGENTS': 'Agents',
  'CLASS': 'Une classe',
};

Color roleColor(String role) => switch (role) {
      'ROLE_ADMIN_AGENT' => EduColors.primary,
      'ROLE_PLATFORM_ADMIN' => EduColors.violet,
      'ROLE_PROFESSOR' => EduColors.emerald,
      _ => EduColors.sky,
    };
String roleLabel(String role) => switch (role) {
      'ROLE_ADMIN_AGENT' => 'Administration',
      'ROLE_PLATFORM_ADMIN' => 'Plateforme',
      'ROLE_PROFESSOR' => 'Professeur',
      'ROLE_STUDENT' => 'Étudiant',
      _ => role,
    };

// ---------- feedback ----------
void toast(BuildContext c, String msg, {bool error = false}) {
  ScaffoldMessenger.of(c)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(msg), backgroundColor: error ? EduColors.red : null));
}

void toastError(BuildContext c, Object e) => toast(c, e.toString(), error: true);

Future<bool> confirmDialog(BuildContext c, String title, String body, {String ok = 'Confirmer', bool danger = false}) async {
  final r = await showDialog<bool>(
    context: c,
    builder: (_) => AlertDialog(
      title: Text(title),
      content: Text(body),
      actions: [
        TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Annuler')),
        FilledButton(
          style: danger ? FilledButton.styleFrom(backgroundColor: EduColors.red) : null,
          onPressed: () => Navigator.pop(c, true),
          child: Text(ok),
        ),
      ],
    ),
  );
  return r ?? false;
}

/// Downloads a protected file with the bearer token and opens it with the system viewer.
Future<void> openProtectedFile(BuildContext c, String path, {String name = 'document'}) async {
  final api = apiOf(c);
  toast(c, 'Téléchargement…');
  try {
    if (kIsWeb) {
      final f = await api.fetchFile(path, fallbackName: name);
      await saveBytesToBrowser(f.bytes, f.name, f.contentType);
      return;
    }
    final File f = await api.download(path, fallbackName: name);
    final r = await OpenFilex.open(f.path);
    if (r.type != ResultType.done && c.mounted) {
      toast(c, 'Fichier enregistré mais aucune application pour l\'ouvrir (${r.message}).', error: true);
    }
  } catch (e) {
    if (c.mounted) toastError(c, e);
  }
}

// ---------- layout primitives ----------
class Panel extends StatelessWidget {
  final String? title;
  final Widget? trailing;
  final Widget child;
  final EdgeInsets padding;
  const Panel({super.key, this.title, this.trailing, required this.child, this.padding = const EdgeInsets.all(16)});
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: padding,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (title != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Row(children: [
                Expanded(child: Text(title!, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800))),
                if (trailing != null) trailing!,
              ]),
            ),
          child,
        ]),
      ),
    );
  }
}

class StatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color color;
  const StatCard({super.key, required this.label, required this.value, required this.icon, this.color = EduColors.primary});
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(10)),
            child: Icon(icon, size: 18, color: color),
          ),
          const SizedBox(height: 10),
          Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(fontSize: 12, color: Theme.of(context).hintColor, fontWeight: FontWeight.w600)),
        ]),
      ),
    );
  }
}

class StatGrid extends StatelessWidget {
  final List<StatCard> items;
  const StatGrid(this.items, {super.key});
  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      final w = (c.maxWidth - 12) / 2;
      return Wrap(spacing: 12, runSpacing: 12, children: [for (final i in items) SizedBox(width: w, child: i)]);
    });
  }
}

class Badge2 extends StatelessWidget {
  final String text;
  final Color color;
  const Badge2(this.text, {super.key, this.color = EduColors.slate500});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
        decoration: BoxDecoration(color: color.withValues(alpha: 0.13), borderRadius: BorderRadius.circular(8)),
        child: Text(text, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: color)),
      );
}

Color statusColor(String s) => switch (s) {
      'ACTIVE' || 'PRESENT' || 'READY_FOR_PICKUP' || 'COMPLETED' => EduColors.emerald,
      'SUSPENDED' || 'ABSENT' || 'CANCELLED' => EduColors.red,
      'LATE' || 'IN_PROGRESS' => EduColors.orange,
      _ => EduColors.slate500,
    };

class EmptyState extends StatelessWidget {
  final String text;
  final IconData icon;
  const EmptyState(this.text, {super.key, this.icon = Icons.inbox_outlined});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.all(32),
        child: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(icon, size: 40, color: Theme.of(context).hintColor),
            const SizedBox(height: 10),
            Text(text, textAlign: TextAlign.center, style: TextStyle(color: Theme.of(context).hintColor)),
          ]),
        ),
      );
}

class ErrorState extends StatelessWidget {
  final Object error;
  final VoidCallback onRetry;
  const ErrorState(this.error, this.onRetry, {super.key});
  @override
  Widget build(BuildContext context) {
    final forbidden = error is ApiException && (error as ApiException).isForbidden;
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(forbidden ? Icons.lock_outline : Icons.cloud_off_outlined, size: 42, color: EduColors.red),
          const SizedBox(height: 10),
          Text(error.toString(), textAlign: TextAlign.center),
          if (!forbidden) ...[
            const SizedBox(height: 14),
            OutlinedButton.icon(onPressed: onRetry, icon: const Icon(Icons.refresh), label: const Text('Réessayer')),
          ],
        ]),
      ),
    );
  }
}

/// Loads data once, supports pull-to-refresh and reload via [AsyncBodyState.reload] / the [onReady] key.
class AsyncBody<T> extends StatefulWidget {
  final Future<T> Function() load;
  final Widget Function(BuildContext, T, Future<void> Function() reload) builder;
  final bool scroll;
  const AsyncBody({super.key, required this.load, required this.builder, this.scroll = true});
  @override
  State<AsyncBody<T>> createState() => _AsyncBodyState<T>();
}

class _AsyncBodyState<T> extends State<AsyncBody<T>> {
  T? data;
  Object? error;
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final d = await widget.load();
      if (!mounted) return;
      setState(() {
        data = d;
        error = null;
        loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = e;
        loading = false;
      });
    }
  }

  Future<void> reload() async {
    setState(() => loading = data == null);
    await _fetch();
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (error != null && data == null) {
      return RefreshIndicator(
        onRefresh: reload,
        child: ListView(children: [SizedBox(height: 320, child: ErrorState(error!, reload))]),
      );
    }
    return RefreshIndicator(onRefresh: reload, child: widget.builder(context, data as T, reload));
  }
}

/// Standard scrolling page body with gutters.
class PageList extends StatelessWidget {
  final List<Widget> children;
  const PageList(this.children, {super.key});
  @override
  Widget build(BuildContext context) => ListView.separated(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
        itemCount: children.length,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (_, i) => children[i],
      );
}

class Field extends StatelessWidget {
  final TextEditingController controller;
  final String label;
  final bool obscure;
  final TextInputType? keyboard;
  final int maxLines;
  final String? hint;
  final bool enabled;
  const Field(this.controller, this.label,
      {super.key, this.obscure = false, this.keyboard, this.maxLines = 1, this.hint, this.enabled = true});
  @override
  Widget build(BuildContext context) {
    LangScope.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextField(
        controller: controller,
        obscureText: obscure,
        keyboardType: keyboard,
        maxLines: obscure ? 1 : maxLines,
        enabled: enabled,
        decoration: InputDecoration(labelText: tr(label), hintText: hint == null ? null : tr(hint!)),
      ),
    );
  }
}

class Dropdown<T> extends StatelessWidget {
  final String label;
  final T? value;
  final List<DropdownMenuItem<T>> items;
  final ValueChanged<T?> onChanged;
  const Dropdown({super.key, required this.label, required this.value, required this.items, required this.onChanged});
  @override
  Widget build(BuildContext context) {
    LangScope.of(context);
    return Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: DropdownButtonFormField<T>(
          value: value,
          isExpanded: true,
          decoration: InputDecoration(labelText: tr(label)),
          items: items,
          onChanged: onChanged,
        ),
      );
  }
}

/// Bottom-sheet form helper: runs [submit], shows API errors in place and closes on success.
Future<bool?> showFormSheet(
  BuildContext context, {
  required String title,
  required List<Widget> Function(StateSetter) fields,
  required Future<void> Function() submit,
  String action = 'Enregistrer',
}) {
  bool busy = false;
  String? err;
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    showDragHandle: true,
    builder: (ctx) {
      return StatefulBuilder(builder: (ctx, set) {
        return Padding(
          padding: EdgeInsets.fromLTRB(20, 4, 20, MediaQuery.of(ctx).viewInsets.bottom + 20),
          child: SingleChildScrollView(
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, mainAxisSize: MainAxisSize.min, children: [
              Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
              const SizedBox(height: 14),
              ...fields(set),
              if (err != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Text(err!, style: const TextStyle(color: EduColors.red, fontWeight: FontWeight.w600)),
                ),
              FilledButton(
                onPressed: busy
                    ? null
                    : () async {
                        set(() {
                          busy = true;
                          err = null;
                        });
                        try {
                          await submit();
                          if (ctx.mounted) Navigator.pop(ctx, true);
                        } catch (e) {
                          set(() {
                            busy = false;
                            err = e.toString();
                          });
                        }
                      },
                child: busy
                    ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : Text(action),
              ),
            ]),
          ),
        );
      });
    },
  );
}

Future<DateTime?> pickDate(BuildContext c, {DateTime? initial, DateTime? first, DateTime? last}) => showDatePicker(
      context: c,
      initialDate: initial ?? DateTime.now(),
      firstDate: first ?? DateTime(2000),
      lastDate: last ?? DateTime(2100),
      locale: Locale(dateLocale()),
    );

Future<TimeOfDay?> pickTime(BuildContext c, {TimeOfDay? initial}) =>
    showTimePicker(context: c, initialTime: initial ?? const TimeOfDay(hour: 8, minute: 0));

String hhmmOf(TimeOfDay t) => '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}:00';

class ListTileCard extends StatelessWidget {
  final Widget title;
  final Widget? subtitle;
  final Widget? leading;
  final Widget? trailing;
  final VoidCallback? onTap;
  const ListTileCard({super.key, required this.title, this.subtitle, this.leading, this.trailing, this.onTap});
  @override
  Widget build(BuildContext context) => Card(
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(children: [
              if (leading != null) ...[leading!, const SizedBox(width: 12)],
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  DefaultTextStyle.merge(style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5), child: title),
                  if (subtitle != null) ...[
                    const SizedBox(height: 3),
                    DefaultTextStyle.merge(
                        style: TextStyle(fontSize: 12.5, color: Theme.of(context).hintColor), child: subtitle!),
                  ],
                ]),
              ),
              if (trailing != null) ...[const SizedBox(width: 8), trailing!],
            ]),
          ),
        ),
      );
}

class Avatar extends StatelessWidget {
  final String initials;
  final Color color;
  final double size;
  const Avatar(this.initials, {super.key, this.color = EduColors.primary, this.size = 40});
  @override
  Widget build(BuildContext context) => Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(color: color.withValues(alpha: 0.14), shape: BoxShape.circle),
        child: Text(initials, style: TextStyle(color: color, fontWeight: FontWeight.w800, fontSize: size * 0.36)),
      );
}

/// A simple horizontal bar list (web BarList equivalent).
class BarList extends StatelessWidget {
  final List<(String, double)> rows;
  final double? max;
  final Color color;
  final String Function(double)? fmt;
  const BarList(this.rows, {super.key, this.max, this.color = EduColors.primary, this.fmt});
  @override
  Widget build(BuildContext context) {
    if (rows.isEmpty) return const EmptyState('Aucune donnée');
    final m = max ?? rows.map((r) => r.$2).fold<double>(0, (a, b) => b > a ? b : a);
    return Column(children: [
      for (final r in rows)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 5),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Expanded(child: Text(r.$1, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13))),
              Text(fmt != null ? fmt!(r.$2) : r.$2.toStringAsFixed(r.$2 % 1 == 0 ? 0 : 1),
                  style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 13)),
            ]),
            const SizedBox(height: 4),
            ClipRRect(
              borderRadius: BorderRadius.circular(6),
              child: LinearProgressIndicator(
                value: m <= 0 ? 0 : (r.$2 / m).clamp(0, 1).toDouble(),
                minHeight: 7,
                color: color,
                backgroundColor: color.withValues(alpha: 0.12),
              ),
            ),
          ]),
        ),
    ]);
  }
}

AuthController authOf(BuildContext c) => c.read<AuthController>();
