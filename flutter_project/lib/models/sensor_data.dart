import 'alarm_state.dart';

class SensorData {
  final double temperature;
  final double humidity;
  final int gasSmokeLevel;
  final bool flameStatus;
  final AlarmState alarmState;
  final int timestamp;
  final bool buzzerActive;
  final bool acknowledged;
  final String? acknowledgedBy;

  SensorData({
    required this.temperature,
    required this.humidity,
    required this.gasSmokeLevel,
    required this.flameStatus,
    required this.alarmState,
    required this.timestamp,
    this.buzzerActive = false,
    this.acknowledged = false,
    this.acknowledgedBy,
  });

  factory SensorData.fromJson(Map<dynamic, dynamic> json) {
    return SensorData(
      temperature: (json['temperature'] as num?)?.toDouble() ?? 0.0,
      humidity: (json['humidity'] as num?)?.toDouble() ?? 0.0,
      gasSmokeLevel: (json['gas_smoke_level'] as num?)?.toInt() ?? 0,
      flameStatus: (json['flame_status'] as bool?) ?? false,
      alarmState: AlarmState.fromString(json['alarm_state'] as String?),
      timestamp: (json['timestamp'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      buzzerActive: (json['buzzer_active'] as bool?) ?? false,
      acknowledged: (json['acknowledged'] as bool?) ?? false,
      acknowledgedBy: json['acknowledged_by'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'temperature': temperature,
      'humidity': humidity,
      'gas_smoke_level': gasSmokeLevel,
      'flame_status': flameStatus,
      'alarm_state': alarmState.displayName,
      'timestamp': timestamp,
      'buzzer_active': buzzerActive,
      'acknowledged': acknowledged,
      'acknowledged_by': acknowledgedBy,
    };
  }

  SensorData copyWith({
    double? temperature,
    double? humidity,
    int? gasSmokeLevel,
    bool? flameStatus,
    AlarmState? alarmState,
    int? timestamp,
    bool? buzzerActive,
    bool? acknowledged,
    String? acknowledgedBy,
  }) {
    return SensorData(
      temperature: temperature ?? this.temperature,
      humidity: humidity ?? this.humidity,
      gasSmokeLevel: gasSmokeLevel ?? this.gasSmokeLevel,
      flameStatus: flameStatus ?? this.flameStatus,
      alarmState: alarmState ?? this.alarmState,
      timestamp: timestamp ?? this.timestamp,
      buzzerActive: buzzerActive ?? this.buzzerActive,
      acknowledged: acknowledged ?? this.acknowledged,
      acknowledgedBy: acknowledgedBy ?? this.acknowledgedBy,
    );
  }
}
