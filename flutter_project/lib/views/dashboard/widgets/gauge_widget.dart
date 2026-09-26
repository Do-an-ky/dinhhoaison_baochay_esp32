import 'dart:math';
import 'package:flutter/material.dart';
import '../../../../theme/app_theme.dart';

/// 180-degree Half-Circle Gauge Widget matching the dashboard photo design
class GaugeWidget extends StatelessWidget {
  final String title;
  final double value;
  final String unit;
  final double minValue;
  final double maxValue;
  final double warningThreshold;
  final double dangerThreshold;
  final IconData? icon;
  final Color? gaugeColor;
  final Color? trackColor;

  const GaugeWidget({
    Key? key,
    required this.title,
    required this.value,
    required this.unit,
    this.minValue = 0,
    this.maxValue = 100,
    this.warningThreshold = 50,
    this.dangerThreshold = 70,
    this.icon,
    this.gaugeColor,
    this.trackColor,
  }) : super(key: key);

  Color get _activeColor {
    if (value >= dangerThreshold) {
      return AppColors.statusEmergency;
    } else if (value >= warningThreshold) {
      return AppColors.statusWarning;
    } else {
      return gaugeColor ?? AppColors.statusNormal;
    }
  }

  Color get _effectiveTrackColor {
    return trackColor ?? Colors.grey[200]!;
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 14.0),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (icon != null) ...[
                  Icon(icon, size: 19, color: _activeColor),
                  const SizedBox(width: 5),
                ],
                Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                ),
              ],
            ),
            const SizedBox(height: 6),
            SizedBox(
              width: double.infinity,
              height: 110,
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: [
                  CustomPaint(
                    size: const Size(double.infinity, 110),
                    painter: _HalfCircleGaugePainter(
                      value: value.clamp(minValue, maxValue),
                      minValue: minValue,
                      maxValue: maxValue,
                      gaugeColor: _activeColor,
                      trackColor: _effectiveTrackColor,
                    ),
                  ),
                  Positioned(
                    bottom: 24,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          value % 1 == 0 ? value.toInt().toString() : value.toStringAsFixed(1),
                          style: TextStyle(
                            fontSize: 21,
                            fontWeight: FontWeight.w800,
                            color: _activeColor,
                          ),
                        ),
                        Text(
                          unit,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Colors.grey[700],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 4),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('${minValue.toInt()}', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.grey[600])),
                  Text('${maxValue.toInt()}', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.grey[600])),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HalfCircleGaugePainter extends CustomPainter {
  final double value;
  final double minValue;
  final double maxValue;
  final Color gaugeColor;
  final Color trackColor;

  _HalfCircleGaugePainter({
    required this.value,
    required this.minValue,
    required this.maxValue,
    required this.gaugeColor,
    required this.trackColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final bottomOffset = 10.0;
    final strokeWidth = 13.0;
    final center = Offset(size.width / 2, size.height - bottomOffset);
    final maxRWidth = (size.width - strokeWidth * 2) / 2;
    final maxRHeight = size.height - strokeWidth - bottomOffset;
    final radius = min(maxRWidth, maxRHeight);

    // 180 degrees sweep (pi radians), start at 180° (pi radians)
    const startAngle = pi;
    const sweepAngle = pi;

    final arcRect = Rect.fromCircle(center: center, radius: radius);

    // 1. Semi-Circle Background Track (Lighter fill)
    final bgPaint = Paint()
      ..color = trackColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    canvas.drawArc(arcRect, startAngle, sweepAngle, false, bgPaint);

    // 2. Active Progress Arc
    final progress = ((value - minValue) / (maxValue - minValue)).clamp(0.01, 1.0);
    final activePaint = Paint()
      ..color = gaugeColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    canvas.drawArc(arcRect, startAngle, sweepAngle * progress, false, activePaint);

    // 3. Tick Marks
    const ticks = 5;
    for (int i = 0; i <= ticks; i++) {
      final tickAngle = startAngle + (sweepAngle * (i / ticks));
      final innerR = radius - strokeWidth * 0.95;
      final outerR = radius - strokeWidth * 0.25;

      final startP = Offset(
        center.dx + innerR * cos(tickAngle),
        center.dy + innerR * sin(tickAngle),
      );
      final endP = Offset(
        center.dx + outerR * cos(tickAngle),
        center.dy + outerR * sin(tickAngle),
      );

      canvas.drawLine(
        startP,
        endP,
        Paint()
          ..color = Colors.grey[500]!
          ..strokeWidth = 1.8
          ..strokeCap = StrokeCap.round,
      );
    }

    // 4. Needle Pointer
    final needleAngle = startAngle + (sweepAngle * progress);
    final needleLength = radius * 0.72;
    final needleEnd = Offset(
      center.dx + needleLength * cos(needleAngle),
      center.dy + needleLength * sin(needleAngle),
    );

    final needlePaint = Paint()
      ..color = gaugeColor
      ..strokeWidth = 3.2
      ..strokeCap = StrokeCap.round;

    canvas.drawLine(center, needleEnd, needlePaint);
    canvas.drawCircle(center, 6.5, Paint()..color = gaugeColor);
    canvas.drawCircle(center, 3.0, Paint()..color = Colors.white);
  }

  @override
  bool shouldRepaint(covariant _HalfCircleGaugePainter oldDelegate) {
    return oldDelegate.value != value ||
        oldDelegate.gaugeColor != gaugeColor ||
        oldDelegate.trackColor != trackColor;
  }
}
