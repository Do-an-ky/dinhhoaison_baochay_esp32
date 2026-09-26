import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/alarm_state.dart';
import '../../providers/fire_alarm_provider.dart';
import '../alert/alert_detail_screen.dart';
import 'widgets/gauge_widget.dart';
import 'widgets/status_banner.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<FireAlarmProvider>();
    final data = provider.sensorData;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFFB83A2C),
        foregroundColor: Colors.white,
        centerTitle: true,
        title: const Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.local_fire_department, color: Colors.white, size: 22),
            SizedBox(width: 8),
            Text(
              'Cảnh báo cháy nổ',
              style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 19),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: Badge(
              isLabelVisible: data.alarmState != AlarmState.normal,
              backgroundColor: Colors.white,
              textColor: data.alarmState.color,
              label: Text(data.alarmState.symbol, style: const TextStyle(fontWeight: FontWeight.bold)),
              child: const Icon(Icons.notifications_outlined, color: Colors.white),
            ),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const AlertDetailScreen()),
              );
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          provider.initRealtimeListener();
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Device Selector Dropdown
              Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 14.0, vertical: 8.0),
                  child: Row(
                    children: [
                      const CircleAvatar(
                        radius: 17,
                        child: Icon(Icons.sensors, size: 19),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: DropdownButtonHideUnderline(
                          child: DropdownButton<String>(
                            value: provider.selectedDeviceId,
                            isExpanded: true,
                            items: const [
                              DropdownMenuItem(value: 'phong_bep', child: Text('Node 1 - Phòng Bếp (ESP32)')),
                              DropdownMenuItem(value: 'nha_kho', child: Text('Node 2 - Nhà Kho (ESP32)')),
                              DropdownMenuItem(value: 'phong_khach', child: Text('Node 3 - Phòng Khách (ESP32)')),
                            ],
                            onChanged: (val) {
                              if (val != null) provider.selectDevice(val);
                            },
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 14),

              // 2. Active Buzzer Quick Alert (if triggered)
              if (data.buzzerActive) ...[
                Card(
                  color: const Color(0xFFC0392B),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  child: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Row(
                      children: [
                        const Icon(Icons.volume_up, color: Colors.white, size: 26),
                        const SizedBox(width: 10),
                        const Expanded(
                          child: Text(
                            'Còi hú ESP32 đang kích hoạt!',
                            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                          ),
                        ),
                        ElevatedButton.icon(
                          onPressed: () => provider.muteBuzzer(),
                          icon: const Icon(Icons.volume_off, size: 16),
                          label: const Text('Tắt còi'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.white,
                            foregroundColor: const Color(0xFFC0392B),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 14),
              ],

              // 3. Two Half-Circle Gauges (Side by Side matching the photo)
              Row(
                children: [
                  Expanded(
                    child: GaugeWidget(
                      title: 'Nhiệt độ',
                      value: data.temperature,
                      unit: '°C',
                      minValue: 0,
                      maxValue: 100,
                      warningThreshold: 50,
                      dangerThreshold: 65,
                      icon: Icons.thermostat,
                      gaugeColor: const Color(0xFF27AE60),
                      trackColor: const Color(0xFFE8F5E9),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: GaugeWidget(
                      title: 'Mức khói/gas',
                      value: data.gasSmokeLevel.toDouble(),
                      unit: 'ppm',
                      minValue: 0,
                      maxValue: 1000,
                      warningThreshold: 400,
                      dangerThreshold: 600,
                      icon: Icons.sensors,
                      gaugeColor: const Color(0xFF2980B9),
                      trackColor: const Color(0xFFE1F5FE),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // 4. Secondary Metrics: Humidity & Flame Status
              Row(
                children: [
                  Expanded(
                    child: Card(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      child: Padding(
                        padding: const EdgeInsets.all(14.0),
                        child: Row(
                          children: [
                            const CircleAvatar(
                              backgroundColor: Color(0xFFE1F5FE),
                              child: Icon(Icons.water_drop, color: Color(0xFF0288D1)),
                            ),
                            const SizedBox(width: 10),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Độ ẩm', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                                Text('${data.humidity}%', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Card(
                      color: data.flameStatus ? Colors.red[50] : null,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      child: Padding(
                        padding: const EdgeInsets.all(14.0),
                        child: Row(
                          children: [
                            CircleAvatar(
                              backgroundColor: data.flameStatus ? Colors.red[100] : Colors.green[100],
                              child: Icon(
                                Icons.local_fire_department,
                                color: data.flameStatus ? Colors.red : Colors.green,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Cảm biến lửa', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                                Text(
                                  data.flameStatus ? 'CÓ LỬA!' : 'An toàn',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                    color: data.flameStatus ? Colors.red : Colors.green,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // 5. Overall Semantic Status Card at the bottom (as in photo)
              StatusBanner(
                alarmState: data.alarmState,
                timestamp: data.timestamp,
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const AlertDetailScreen()),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
