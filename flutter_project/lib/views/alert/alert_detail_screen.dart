import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/alarm_state.dart';
import '../../providers/fire_alarm_provider.dart';

class AlertDetailScreen extends StatelessWidget {
  const AlertDetailScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<FireAlarmProvider>();
    final data = provider.sensorData;
    final dateFormat = DateFormat('HH:mm:ss - dd/MM/yyyy');
    final formattedTime = dateFormat.format(DateTime.fromMillisecondsSinceEpoch(data.timestamp));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Chi Tiết Cảnh Báo', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 1. Severity Level Banner
            Card(
              color: data.alarmState.color,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              elevation: 4,
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 52,
                          height: 52,
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.25),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(data.alarmState.icon, color: Colors.white, size: 30),
                        ),
                        const SizedBox(width: 14),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'MỨC ĐỘ NGUY HIỂM',
                              style: TextStyle(
                                color: Colors.white.withOpacity(0.85),
                                fontSize: 11,
                                letterSpacing: 1,
                              ),
                            ),
                            Text(
                              '${data.alarmState.symbol} ${data.alarmState.displayName}',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 22,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Text(
                      data.alarmState.description,
                      style: const TextStyle(color: Colors.white, fontSize: 15, height: 1.4),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        const Icon(Icons.access_time, color: Colors.white70, size: 16),
                        const SizedBox(width: 6),
                        Text(
                          'Thời gian: $formattedTime',
                          style: const TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // 2. Snapshot Data Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Thông số tại thời điểm cảnh báo',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 14),
                    _SnapshotRow(
                      label: 'Nhiệt độ (DHT11)',
                      value: '${data.temperature} °C',
                      isDanger: data.temperature >= 60,
                    ),
                    const Divider(),
                    _SnapshotRow(
                      label: 'Nồng độ khói/gas (MQ-2)',
                      value: '${data.gasSmokeLevel} ppm',
                      isDanger: data.gasSmokeLevel >= 400,
                    ),
                    const Divider(),
                    _SnapshotRow(
                      label: 'Cảm biến ngọn lửa (Flame)',
                      value: data.flameStatus ? 'Phát hiện ngọn lửa!' : 'Không có lửa',
                      isDanger: data.flameStatus,
                    ),
                    const Divider(),
                    _SnapshotRow(
                      label: 'Độ ẩm không khí',
                      value: '${data.humidity} %',
                      isDanger: false,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // 3. Acknowledged info if processed
            if (data.acknowledged) ...[
              Card(
                color: Colors.green[50],
                child: Padding(
                  padding: const EdgeInsets.all(14.0),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle, color: Colors.green, size: 24),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Đã xác nhận xử lý',
                              style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green),
                            ),
                            Text(
                              'Bởi ${data.acknowledgedBy ?? 'Chủ hộ'} lúc $formattedTime',
                              style: TextStyle(fontSize: 12, color: Colors.grey[700]),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
            ],

            const Text(
              'Thao tác khẩn cấp',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),

            // Button 1: "Xác nhận đã xử lý" (Min 48x48dp target)
            SizedBox(
              height: 54, // > 48dp
              child: ElevatedButton.icon(
                onPressed: () {
                  provider.acknowledgeAlert('Chủ hộ (Mobile App)');
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Đã xác nhận sự cố an toàn!')),
                  );
                },
                icon: const Icon(Icons.check_circle_outline, size: 22),
                label: const Text(
                  'Xác nhận đã xử lý',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF1E8449),
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
              ),
            ),
            const SizedBox(height: 12),

            // Button 2: "Tắt còi từ xa" (Call remote API / RTDB buzzer_active = false)
            SizedBox(
              height: 54, // > 48dp
              child: OutlinedButton.icon(
                onPressed: () {
                  provider.muteBuzzer();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Đã gửi lệnh tắt còi đến ESP32!')),
                  );
                },
                icon: const Icon(Icons.volume_off, size: 22, color: Color(0xFFC0392B)),
                label: const Text(
                  'Tắt còi từ xa',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFC0392B),
                  ),
                ),
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: Color(0xFFC0392B), width: 1.5),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SnapshotRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isDanger;

  const _SnapshotRow({
    required this.label,
    required this.value,
    required this.isDanger,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 14)),
          Text(
            value,
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: isDanger ? const Color(0xFFC0392B) : null,
            ),
          ),
        ],
      ),
    );
  }
}
