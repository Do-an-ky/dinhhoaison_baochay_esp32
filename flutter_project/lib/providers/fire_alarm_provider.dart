import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:firebase_database/firebase_database.dart';
import 'package:vibration/vibration.dart';
import '../models/alarm_state.dart';
import '../models/sensor_data.dart';

class FireAlarmProvider extends ChangeNotifier {
  final FirebaseDatabase _database = FirebaseDatabase.instance;
  StreamSubscription<DatabaseEvent>? _subscription;

  String _selectedDeviceId = 'phong_bep';
  String get selectedDeviceId => _selectedDeviceId;

  SensorData _sensorData = SensorData(
    temperature: 28.5,
    humidity: 65.0,
    gasSmokeLevel: 180,
    flameStatus: false,
    alarmState: AlarmState.normal,
    timestamp: DateTime.now().millisecondsSinceEpoch,
  );
  SensorData get sensorData => _sensorData;

  bool _isLoading = true;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  FireAlarmProvider() {
    initRealtimeListener();
  }

  void selectDevice(String deviceId) {
    if (_selectedDeviceId != deviceId) {
      _selectedDeviceId = deviceId;
      initRealtimeListener();
    }
  }

  void initRealtimeListener() {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    _subscription?.cancel();

    try {
      final ref = _database.ref('devices/$_selectedDeviceId');
      _subscription = ref.onValue.listen(
        (DatabaseEvent event) {
          _isLoading = false;
          final value = event.snapshot.value;
          if (value != null && value is Map) {
            _sensorData = SensorData.fromJson(Map<dynamic, dynamic>.from(value));
            _handleAlarmTrigger(_sensorData.alarmState);
          } else {
            // No data received from node
            _sensorData = _sensorData.copyWith(alarmState: AlarmState.disconnected);
          }
          notifyListeners();
        },
        onError: (error) {
          _isLoading = false;
          _errorMessage = error.toString();
          _sensorData = _sensorData.copyWith(alarmState: AlarmState.disconnected);
          notifyListeners();
        },
      );
    } catch (e) {
      _isLoading = false;
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  void _handleAlarmTrigger(AlarmState state) {
    if (state == AlarmState.fireEmergency) {
      // Trigger device vibration as requested by accessibility and safety specifications
      Vibration.hasVibrator().then((hasVibrator) {
        if (hasVibrator == true) {
          Vibration.vibrate(pattern: [0, 400, 200, 400], repeat: 1);
        }
      });
    }
  }

  // UC-03: Xác nhận đã xử lý (Acknowledge)
  Future<void> acknowledgeAlert([String? acknowledgedBy]) async {
    try {
      final ref = _database.ref('devices/$_selectedDeviceId');
      await ref.update({
        'acknowledged': true,
        'acknowledged_by': acknowledgedBy ?? 'Người dùng',
        'buzzer_active': false,
      });
      _sensorData = _sensorData.copyWith(
        acknowledged: true,
        acknowledgedBy: acknowledgedBy ?? 'Người dùng',
        buzzerActive: false,
      );
      notifyListeners();
    } catch (e) {
      _errorMessage = 'Không thể gửi lệnh xác nhận: $e';
      notifyListeners();
    }
  }

  // Tắt còi từ xa (Mute Buzzer on ESP32)
  Future<void> muteBuzzer() async {
    try {
      final ref = _database.ref('devices/$_selectedDeviceId');
      await ref.update({
        'buzzer_active': false,
      });
      _sensorData = _sensorData.copyWith(buzzerActive: false);
      notifyListeners();
    } catch (e) {
      _errorMessage = 'Không thể gửi lệnh tắt còi: $e';
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}
