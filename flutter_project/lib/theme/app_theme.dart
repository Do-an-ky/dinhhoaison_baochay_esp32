import 'package:flutter/material.dart';

class AppColors {
  // Semantic alarm status colors as defined in the system requirements
  static const Color statusNormal = Color(0xFF1E8449);       // Xanh lá (An toàn)
  static const Color statusWarning = Color(0xFFCA6F1E);      // Cam (Cảnh báo khói)
  static const Color statusEmergency = Color(0xFFC0392B);    // Đỏ (Báo cháy khẩn cấp)
  static const Color statusDisconnected = Color(0xFF7F8C8D); // Xám (Mất kết nối)

  // Containers
  static const Color statusNormalContainer = Color(0xFFE8F5E9);
  static const Color statusWarningContainer = Color(0xFFFFF3E0);
  static const Color statusEmergencyContainer = Color(0xFFFFEBEE);
  static const Color statusDisconnectedContainer = Color(0xFFECEFF1);
}

class AppTheme {
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF00629E),
        brightness: Brightness.light,
      ),
      scaffoldBackgroundColor: const Color(0xFFFBFDFA),
      textTheme: const TextTheme(
        headlineMedium: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
        titleLarge: TextStyle(fontSize: 19, fontWeight: FontWeight.bold),
        titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        bodyMedium: TextStyle(fontSize: 14, fontWeight: FontWeight.normal),
        bodySmall: TextStyle(fontSize: 12, fontWeight: FontWeight.normal),
      ),
      cardTheme: CardTheme(
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
    );
  }

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF98CBFF),
        brightness: Brightness.dark,
      ),
      scaffoldBackgroundColor: const Color(0xFF121416),
      cardTheme: CardTheme(
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
    );
  }
}
