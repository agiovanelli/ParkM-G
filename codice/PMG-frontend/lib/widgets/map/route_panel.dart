import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:park_mg/utils/theme.dart';

class RoutePanel extends StatelessWidget {
  final List<RoutePanelRoute> routes;
  final int selectedIndex;
  final ValueChanged<int> onSelectIndex;
  final VoidCallback onClose;
  final VoidCallback onStart;
  final String Function(String) stripHtml;
  final ValueChanged<bool> setMapGesturesLocked;

  const RoutePanel({
    super.key,
    required this.routes,
    required this.selectedIndex,
    required this.onSelectIndex,
    required this.onClose,
    required this.onStart,
    required this.stripHtml,
    required this.setMapGesturesLocked,
  });

  @override
  Widget build(BuildContext context) {
    final route = routes[selectedIndex];
    final steps = route.steps;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.route, color: Colors.black87),
            const SizedBox(width: 10),
            const Expanded(
              child: Text(
                'Scegli il percorso',
                style: TextStyle(
                  color: Colors.black87,
                  fontWeight: FontWeight.w800,
                  fontSize: 16,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
            IconButton(
              icon: const Icon(Icons.close, color: Colors.black87),
              onPressed: onClose,
            ),
          ],
        ),
        const SizedBox(height: 10),

        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: List.generate(routes.length, (i) {
            final selected = i == selectedIndex;
            return ChoiceChip(
              label: Text('Route ${i + 1}'),
              selected: selected,
              onSelected: (_) => onSelectIndex(i),

              // colori in linea col tema (tu li hai già così)
              backgroundColor: AppColors.accentCyan,
              selectedColor: AppColors.bgDark,

              labelStyle: TextStyle(
                color: selected ? AppColors.accentCyan : Colors.white,
                fontWeight: FontWeight.w800,
              ),

              side: BorderSide(
                color: selected ? AppColors.borderField : AppColors.accentCyan,
                width: 1,
              ),

              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(999),
              ),
              elevation: selected ? 1.5 : 0,
              pressElevation: 0,
            );
          }),
        ),

        const SizedBox(height: 12),

        Text(
          route.summary,
          style: const TextStyle(
            color: Colors.black54,
            fontWeight: FontWeight.w600,
          ),
        ),

        const SizedBox(height: 12),
        Divider(color: Colors.grey.shade300, height: 1),
        const SizedBox(height: 10),

        const Text(
          'Indicazioni',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),

        Expanded(
          child: steps.isEmpty
              ? const Center(
                  child: Text(
                    'Nessun dettaglio disponibile.',
                    style: TextStyle(color: Colors.black54),
                  ),
                )
              : Listener(
                  onPointerDown: (_) => setMapGesturesLocked(true),
                  onPointerUp: (_) => setMapGesturesLocked(false),
                  onPointerCancel: (_) => setMapGesturesLocked(false),
                  child: NotificationListener<ScrollNotification>(
                    onNotification: (n) {
                      if (n is ScrollStartNotification) {
                        setMapGesturesLocked(true);
                      } else if (n is ScrollEndNotification) {
                        setMapGesturesLocked(false);
                      } else if (n is UserScrollNotification &&
                          n.direction == ScrollDirection.idle) {
                        setMapGesturesLocked(false);
                      }
                      return false;
                    },
                    child: ListView.separated(
                      padding: EdgeInsets.zero,
                      itemCount: steps.length,
                      separatorBuilder: (_, __) =>
                          Divider(color: Colors.grey.shade200, height: 12),
                      itemBuilder: (_, i) {
                        final s = steps[i] as Map<String, dynamic>;
                        final html = (s['htmlInstructions'] ?? '') as String;
                        final dist = (s['distanceText'] ?? '') as String;
                        final dur = (s['durationText'] ?? '') as String;

                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${i + 1}.',
                              style: const TextStyle(
                                color: Colors.black54,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    stripHtml(html),
                                    style: const TextStyle(
                                      color: Colors.black87,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  if (dist.isNotEmpty || dur.isNotEmpty)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 4),
                                      child: Text(
                                        [dist, dur]
                                            .where((x) => x.isNotEmpty)
                                            .join(' • '),
                                        style: const TextStyle(
                                          color: Colors.black54,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                  ),
                ),
        ),

        const SizedBox(height: 12),

        SizedBox(
          width: double.infinity,
          height: 46,
          child: ElevatedButton.icon(
            onPressed: onStart,
            icon: const Icon(Icons.navigation),
            label: const Text(
              'Avvia',
              style: TextStyle(fontWeight: FontWeight.w800),
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.accentCyan,
              foregroundColor: Colors.black,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

/// DTO “leggero” per non importare roba privata del tuo screen dentro i widget.
/// Lo costruisci in UserScreen partendo dalla tua _DirectionsRoute.
class RoutePanelRoute {
  final String summary;
  final List<dynamic> steps;
  const RoutePanelRoute({required this.summary, required this.steps});
}
