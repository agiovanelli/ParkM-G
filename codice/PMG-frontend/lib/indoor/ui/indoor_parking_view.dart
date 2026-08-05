import 'package:flutter/material.dart';

import '../graph/lane_grid_mask.dart';
import '../models/indoor_models.dart';
import '../parking_map_definition.dart';
import '../slot_map.dart';
import 'dynamic_parking_painter.dart';

class IndoorParkingView extends StatefulWidget {
  final IndoorMapDefinition def;
  final IndoorAssignment assignment;
  final int userFloor;
  final bool showGridDebug;
  final VoidCallback? onArrivedToSlot;
  final int animationDurationSeconds;

  const IndoorParkingView({
    super.key,
    required this.def,
    required this.assignment,
    this.userFloor = 1,
    this.showGridDebug = false,
    this.onArrivedToSlot,
    this.animationDurationSeconds = 6,
  });

  @override
  State<IndoorParkingView> createState() => IndoorParkingViewState();
}

class IndoorParkingViewState extends State<IndoorParkingView>
    with TickerProviderStateMixin {
  late int _floor;
  List<SlotCell2D> _path = const [];
  int _stepIndex = 0;
  late final AnimationController _targetController;
  late final Animation<double> _targetAnimation;
  late final AnimationController _userController;
  late final Animation<double> _userAnimation;
  bool _arrivalNotified = false;

  List<int> get _steps {
    final availableFloors = widget.def.floorNumbers;
    if (availableFloors.isEmpty) return const [];

    final startFloor = availableFloors.contains(widget.userFloor)
        ? widget.userFloor
        : availableFloors.first;
    final targetFloor = widget.assignment.slot.floor;

    if (startFloor == targetFloor) {
      return [startFloor];
    }

    if (targetFloor > startFloor) {
      return [
        for (int floor = startFloor; floor <= targetFloor; floor++)
          if (availableFloors.contains(floor)) floor,
      ];
    }

    return [
      for (int floor = startFloor; floor >= targetFloor; floor--)
        if (availableFloors.contains(floor)) floor,
    ];
  }

  @override
  void initState() {
    super.initState();

    _targetController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..repeat(reverse: true);

    _targetAnimation = CurvedAnimation(
      parent: _targetController,
      curve: Curves.easeInOut,
    );

    _userController = AnimationController(
      vsync: this,
      duration: Duration(seconds: widget.animationDurationSeconds),
    );

    _userController.addStatusListener(_handleAnimationStatus);
    _userAnimation = CurvedAnimation(
      parent: _userController,
      curve: Curves.linear,
    );

    final steps = _steps;
    _floor = steps.isEmpty ? widget.userFloor : steps.first;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _recompute();
      }
    });
  }

  void _handleAnimationStatus(AnimationStatus status) {
    if (status != AnimationStatus.completed || !mounted) return;

    final steps = _steps;
    final isLastStep = steps.isEmpty || _stepIndex >= steps.length - 1;

    if (isLastStep) {
      if (!_arrivalNotified) {
        _arrivalNotified = true;
        widget.onArrivedToSlot?.call();
      }
      return;
    }

    setState(() {
      _stepIndex++;
    });
    _recompute();
  }

  @override
  void didUpdateWidget(covariant IndoorParkingView oldWidget) {
    super.didUpdateWidget(oldWidget);

    final assignmentChanged =
        oldWidget.assignment.slot.slotId != widget.assignment.slot.slotId ||
        oldWidget.assignment.slot.floor != widget.assignment.slot.floor;
    final mapChanged = oldWidget.def.data.parkingId != widget.def.data.parkingId;
    final floorChanged = oldWidget.userFloor != widget.userFloor;

    if (assignmentChanged || mapChanged || floorChanged) {
      _arrivalNotified = false;
      _stepIndex = 0;
      final steps = _steps;
      _floor = steps.isEmpty ? widget.userFloor : steps.first;
      _recompute();
    }
  }

  @override
  void dispose() {
    _targetController.dispose();
    _userController
      ..removeStatusListener(_handleAnimationStatus)
      ..dispose();
    super.dispose();
  }

  void restartRouteAnimation() {
    if (!mounted) return;

    _arrivalNotified = false;
    _stepIndex = 0;
    final steps = _steps;
    _floor = steps.isEmpty ? widget.userFloor : steps.first;
    _recompute();
  }

  void _recompute() {
    final steps = _steps;
    if (steps.isEmpty) {
      setState(() {
        _path = const [];
      });
      return;
    }

    if (_stepIndex >= steps.length) {
      _stepIndex = steps.length - 1;
    }

    final shownFloor = steps[_stepIndex];
    final layout = widget.def.layouts[shownFloor];

    if (layout == null) {
      setState(() {
        _floor = shownFloor;
        _path = const [];
      });
      return;
    }

    final targetFloor = widget.assignment.slot.floor;
    final goal = shownFloor == targetFloor
        ? widget.assignment.approachCell
        : layout.rampCell;

    final route = LaneGrid(layout).buildPath(
      start: layout.entryCell,
      goal: goal,
    );

    setState(() {
      _floor = shownFloor;
      _path = route;
    });

    _userController
      ..stop()
      ..forward(from: 0);
  }

  @override
  Widget build(BuildContext context) {
    final layout = widget.def.layouts[_floor];

    if (layout == null) {
      return const Center(
        child: Text(
          'Layout del piano non disponibile',
          style: TextStyle(color: Colors.white),
        ),
      );
    }

    return Column(
      children: [
        _Header(
          parkingName: widget.def.data.name,
          floor: _floor,
          targetSlotId: widget.assignment.slot.slotId,
          floorCount: widget.def.floorNumbers.length,
        ),
        const SizedBox(height: 10),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(18),
            child: Container(
              color: const Color(0xFFECEFF1),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  return InteractiveViewer(
                    minScale: 1,
                    maxScale: 4,
                    boundaryMargin: const EdgeInsets.all(140),
                    child: SizedBox(
                      width: constraints.maxWidth,
                      height: constraints.maxHeight,
                      child: AnimatedBuilder(
                        animation: Listenable.merge([
                          _targetController,
                          _userController,
                        ]),
                        builder: (_, __) {
                          return CustomPaint(
                            painter: DynamicParkingPainter(
                              layout: layout,
                              path: _path,
                              targetSlotId:
                                  _floor == widget.assignment.slot.floor
                                      ? widget.assignment.slot.slotId
                                      : null,
                              targetAnimation: _targetAnimation.value,
                              userAnimation: _userAnimation.value,
                              showGridDebug: widget.showGridDebug,
                            ),
                          );
                        },
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        ),
        const SizedBox(height: 8),
        const _Legend(),
      ],
    );
  }
}

class _Header extends StatelessWidget {
  final String parkingName;
  final int floor;
  final String targetSlotId;
  final int floorCount;

  const _Header({
    required this.parkingName,
    required this.floor,
    required this.targetSlotId,
    required this.floorCount,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      child: Row(
        children: [
          Expanded(
            child: Text(
              parkingName.isEmpty ? 'Parcheggio' : parkingName,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w800,
                fontSize: 16,
              ),
            ),
          ),
          Text(
            'Piano $floor/$floorCount  •  Posto $targetSlotId',
            style: const TextStyle(
              color: Colors.white70,
              fontWeight: FontWeight.w700,
              fontSize: 14,
            ),
          ),
        ],
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend();

  @override
  Widget build(BuildContext context) {
    return const Wrap(
      spacing: 12,
      runSpacing: 6,
      children: [
        _LegendItem(color: Color(0xFF69B9A9), text: 'Libero'),
        _LegendItem(color: Color(0xFFB63D32), text: 'Occupato'),
        _LegendItem(color: Color(0xFFFFB74D), text: 'Prenotato'),
        _LegendItem(color: Color(0xFF42A5F5), text: 'Disabili'),
        _LegendItem(color: Color(0xFFEC7EB7), text: 'Incinta'),
        _LegendItem(color: Color(0xFF616161), text: 'Fuori servizio'),
      ],
    );
  }
}

class _LegendItem extends StatelessWidget {
  final Color color;
  final String text;

  const _LegendItem({
    required this.color,
    required this.text,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(3),
          ),
        ),
        const SizedBox(width: 4),
        Text(
          text,
          style: const TextStyle(
            color: Colors.white70,
            fontSize: 12,
          ),
        ),
      ],
    );
  }
}
