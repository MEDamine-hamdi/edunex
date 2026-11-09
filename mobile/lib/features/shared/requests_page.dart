import 'package:flutter/material.dart' hide Text;

import '../../core/models.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

/// Student / professor document requests: /api/requests/*
class RequestsPage extends StatefulWidget {
  const RequestsPage({super.key});
  @override
  State<RequestsPage> createState() => _RequestsPageState();
}

class _RequestsPageState extends State<RequestsPage> {
  Key k = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final api = apiOf(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _create(context),
        icon: const Icon(Icons.add),
        label: const Text('Nouvelle demande'),
      ),
      body: AsyncBody<List<RequestView>>(
        key: k,
        load: () async => listOf(await api.get('/api/requests/mine'), RequestView.fromJson),
        builder: (context, items, reload) {
          if (items.isEmpty) {
            return ListView(children: const [EmptyState('Aucune demande de document pour le moment', icon: Icons.description_outlined)]);
          }
          return PageList([
            for (final r in items)
              Panel(
                title: r.type,
                trailing: Badge2(requestStatusFr[r.status] ?? r.status, color: statusColor(r.status)),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  _Steps(r.status),
                  const SizedBox(height: 10),
                  Text('${r.copies} exemplaire(s) · déposée le ${fmtDate(r.createdAt)}',
                      style: TextStyle(fontSize: 12.5, color: Theme.of(context).hintColor)),
                  if ((r.reason ?? '').isNotEmpty) Padding(padding: const EdgeInsets.only(top: 6), child: Text('Motif : ${r.reason}')),
                  if ((r.details ?? '').isNotEmpty) Padding(padding: const EdgeInsets.only(top: 4), child: Text('Détails : ${r.details}')),
                  if ((r.adminNote ?? '').isNotEmpty)
                    Container(
                      margin: const EdgeInsets.only(top: 8),
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(color: EduColors.primary.withValues(alpha: 0.08), borderRadius: BorderRadius.circular(10)),
                      child: Text('Message de l\'administration : ${r.adminNote}'),
                    ),
                  const SizedBox(height: 10),
                  Wrap(spacing: 8, children: [
                    if (r.hasFile)
                      FilledButton.icon(
                        style: FilledButton.styleFrom(minimumSize: const Size(0, 40)),
                        onPressed: () => openProtectedFile(context, '/api/requests/${r.id}/file', name: r.fileName ?? 'document'),
                        icon: const Icon(Icons.download),
                        label: Text(r.fileName ?? 'Télécharger'),
                      ),
                    if (r.status == 'FILED')
                      OutlinedButton.icon(
                        onPressed: () async {
                          if (!await confirmDialog(context, 'Retirer la demande', 'Retirer cette demande ?', danger: true)) return;
                          try {
                            await api.delete('/api/requests/${r.id}');
                            await reload();
                          } catch (e) {
                            if (context.mounted) toastError(context, e);
                          }
                        },
                        icon: const Icon(Icons.close, color: EduColors.red),
                        label: const Text('Retirer', style: TextStyle(color: EduColors.red)),
                      ),
                  ]),
                ]),
              ),
          ]);
        },
      ),
    );
  }

  Future<void> _create(BuildContext context) async {
    final api = apiOf(context);
    List<JMap> options;
    try {
      options = maps(await api.get('/api/requests/type-options'));
    } catch (e) {
      toastError(context, e);
      return;
    }
    if (options.isEmpty) {
      toast(context, 'Aucun type de document n\'est disponible pour le moment.', error: true);
      return;
    }
    JMap? type = options.first;
    int copies = 1;
    final reason = TextEditingController();
    final details = TextEditingController();
    if (!context.mounted) return;
    final ok = await showFormSheet(
      context,
      title: 'Nouvelle demande',
      action: 'Déposer la demande',
      fields: (set) {
        final max = asInt(type?['maxCopies']) ?? 1;
        if (copies > max) copies = max;
        return [
          Dropdown<JMap>(
            label: 'Type de document',
            value: type,
            items: [for (final o in options) DropdownMenuItem(value: o, child: Text(str(o['name']) ?? ''))],
            onChanged: (v) => set(() => type = v),
          ),
          Row(children: [
            const Expanded(child: Text('Nombre d\'exemplaires')),
            IconButton(onPressed: copies > 1 ? () => set(() => copies--) : null, icon: const Icon(Icons.remove_circle_outline)),
            Text('$copies', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
            IconButton(onPressed: copies < max ? () => set(() => copies++) : null, icon: const Icon(Icons.add_circle_outline)),
          ]),
          Field(reason, 'Motif', maxLines: 2),
          Field(details, type?['requiresDetails'] == true ? 'Détails (obligatoire)' : 'Détails (facultatif)', maxLines: 3),
        ];
      },
      submit: () => api.post('/api/requests', body: {
        'type': type?['name'],
        'copies': copies,
        'reason': reason.text.trim(),
        'details': details.text.trim(),
      }),
    );
    if (ok == true) setState(() => k = UniqueKey());
  }
}

class _Steps extends StatelessWidget {
  final String status;
  const _Steps(this.status);
  @override
  Widget build(BuildContext context) {
    const order = ['FILED', 'IN_PROGRESS', 'READY_FOR_PICKUP'];
    final current = order.indexOf(status);
    return Row(children: [
      for (var i = 0; i < order.length; i++) ...[
        Expanded(
          child: Column(children: [
            Container(
              height: 6,
              decoration: BoxDecoration(
                color: i <= current ? EduColors.primary : EduColors.slate200,
                borderRadius: BorderRadius.circular(4),
              ),
            ),
            const SizedBox(height: 4),
            Text(requestStatusFr[order[i]]!,
                style: TextStyle(fontSize: 10.5, fontWeight: i == current ? FontWeight.w800 : FontWeight.w500)),
          ]),
        ),
        if (i < order.length - 1) const SizedBox(width: 6),
      ]
    ]);
  }
}
