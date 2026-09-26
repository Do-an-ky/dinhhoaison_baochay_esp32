import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

enum AlarmState {
  normal,
  smokeWarning,
  fireEmergency,
  disconnected;

  String get displayName {
    switch (this) {
      case AlarmState.normal:
        return 'Bình thường';
      case AlarmState.smokeWarning:
        return 'Cảnh báo khói';
      case AlarmState.fireEmergency:
        return 'Báo cháy khẩn cấp';
      case AlarmState.disconnected:
        return 'Mất kết nối';
    }
  }

  String get symbol {
    switch (this) {
      case AlarmState.normal:
        return '✓';
      case AlarmState.smokeWarning:
        return '!';
      case AlarmState.fireEmergency:
        return '🔥';
      case AlarmState.disconnected:
        return '✕';
    }
  }

  Color get color {
    switch (this) {
      case AlarmState.normal:
        return AppColors.statusNormal;
      case AlarmState.smokeWarning:
        return AppColors.statusWarning;
      case AlarmState.fireEmergency:
        return AppColors.statusEmergency;
      case AlarmState.disconnected:
        return AppColors.statusDisconnected;
    }
  }

  IconData get icon {
    switch (this) {
      case AlarmState.normal:
        return Icons.check_circle;
      case AlarmState.smokeWarning:
        return Icons.warning_rounded;
      case AlarmState.fireEmergency:
        return Icons.local_fire_department;
      case AlarmState.disconnected:
        return Icons.wifi_off_rounded;
    }
  }

  String get description {
    switch (this) {
      case AlarmState.normal:
        return 'Hệ thống an toàn, các chỉ số môi trường ở mức bình thường.';
      case AlarmState.smokeWarning:
        return 'Nồng độ khói hoặc khí gas tăng cao. Cần kiểm tra khu vực!';
      case AlarmState.fireEmergency:
        return 'NGUY HIỂM! Phát hiện ngọn lửa hoặc nhiệt độ & khói vượt mức!';
      case AlarmState.disconnected:
        return 'Không nhận được dữ liệu từ vi điều khiển ESP32.';
    }
  }

  static AlarmState fromString(String? val) {
    if (val == null) return AlarmState.disconnected;
    final clean = val.toLowerCase().trim();
    if (clean.contains('chay') || clean.contains('khan cap') || clean.contains('emergency')) {
      return AlarmState.fireEmergency;
    } else if (clean.contains('khoi') || clean.contains('gas') || clean.contains('warning')) {
      return AlarmState.smokeWarning;
    } else if (clean.contains('mat ket noi') || clean.contains('offline') || clean.contains('disconnected')) {
      return AlarmState.disconnected;
    } else {
      return AlarmState.normal;
    }
  }
}
