/*
  He thong canh bao chay - ESP32
  Cam bien: MQ2 (khoi), DHT11 (nhiet do & do am), cam bien lua (flame)
*/

#include <DHT.h>
#include <WiFi.h>
#include <FirebaseESP32.h>

// ==== Khai bao chan ====
#define MQ2_AO_PIN   34
#define MQ2_DO_PIN   35
#define FLAME_DO_PIN 27
#define FLAME_AO_PIN 32
#define DHT_PIN      4
#define BUZZER_PIN   25
#define RELAY_PIN    26

#define DHTTYPE DHT11
DHT dht(DHT_PIN, DHTTYPE);

// ==== Thong tin Wi-Fi va Firebase ====
#define WIFI_SSID "Son"
#define WIFI_PASSWORD "0369538588"

// LƯU Ý: Không có https:// và không có dấu / ở cuối
#define FIREBASE_HOST "doan-baochay-esp32-default-rtdb.firebaseio.com" 
#define FIREBASE_AUTH "FSljfXuhZmRouGiVsIBAihbE1ui7BQGeY0UZXhvw"

// Khai bao cac doi tuong Firebase
FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

// ==== Nguong canh bao ====
const int SMOKE_THRESHOLD = 1800;
const bool FLAME_ACTIVE_LOW = true; 

unsigned long lastRead = 0;
const unsigned long READ_INTERVAL = 2000; // Doc cam bien moi 2 giay (Cho phu hop DHT11)

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(MQ2_DO_PIN, INPUT);
  pinMode(FLAME_DO_PIN, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(RELAY_PIN, OUTPUT);

  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RELAY_PIN, LOW);

  dht.begin();

  // Ket noi Wi-Fi
  Serial.print("Dang ket noi Wi-Fi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  // Đợi kết nối Wi-Fi, nhưng tối đa chỉ đợi 10 giây (20 lần x 500ms)
  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 20) { 
    Serial.print(".");
    delay(500);
    retries++;
  }

  // ĐÂY LÀ VỊ TRÍ CỦA IF / ELSE
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nKet noi Wi-Fi thanh cong!");
    
    // Chỉ khởi tạo Firebase nếu có Wi-Fi
    config.database_url = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
  } else {
    // ĐOẠN ELSE ĐƯỢC ĐẶT Ở ĐÂY
    Serial.println("\nKhong the ket noi Wi-Fi. Tiep tuc chay Offline!");
  }
  
  Serial.println("He thong canh bao chay - khoi dong xong");
}


void loop() {
  if (millis() - lastRead >= READ_INTERVAL) {
    lastRead = millis();

    // ==== Doc cam bien ====
    int smokeValue = analogRead(MQ2_AO_PIN);
    int flameRaw = digitalRead(FLAME_DO_PIN);
    bool flameDetected = FLAME_ACTIVE_LOW ? (flameRaw == LOW) : (flameRaw == HIGH);

    float humidity = dht.readHumidity();
    float temperature = dht.readTemperature();

    // Kiem tra loi DHT11 de tranh day du lieu sai len Firebase
    if (isnan(humidity) || isnan(temperature)) {
      Serial.println("DHT11: doc loi (kiem tra lai day cam bien)");
      // Giu nguyen gia tri mac dinh de khong lam loi thuat toan
      temperature = 0.0; 
      humidity = 0.0;
    }

    // ==== Xu ly canh bao ====
    bool smokeAlert = smokeValue > SMOKE_THRESHOLD;
    bool fireAlert = smokeAlert || flameDetected;

    if (fireAlert) {
      tone(BUZZER_PIN, 1000); // Bat coi 
      digitalWrite(RELAY_PIN, HIGH);
      Serial.println(">>> CANH BAO CHAY! <<<");
    } else {
      noTone(BUZZER_PIN); // Tat coi
      digitalWrite(RELAY_PIN, LOW);
    }

    // ==== In du lieu ra Serial de theo doi ====
    Serial.print("Nhiet do: "); Serial.print(temperature);
    Serial.print(" | Do am: "); Serial.print(humidity);
    Serial.print(" | Khoi: "); Serial.print(smokeValue);
    Serial.print(" | Lua: "); Serial.println(flameDetected ? "CO" : "Khong");

    // ==== Dong goi va day du lieu len Firebase ====
    FirebaseJson json;
    json.set("temperature", temperature);
    json.set("humidity", humidity);
    json.set("gas_smoke_level", smokeValue);
    json.set("flame_status", flameDetected);
    json.set("alarm_state", fireAlert ? "Bao chay khan cap" : "Binh thuong");
    
    // Su dung ham updateNode thay vi setJSON de Firebase hoat dong muot ma hon
    if (Firebase.updateNode(firebaseData, "/phong_bep", json)) {
      Serial.println("Gửi dữ liệu lên Firebase thành công.");
    } else {
      Serial.print("Lỗi gửi Firebase: ");
      Serial.println(firebaseData.errorReason());
    }
    Serial.println("-------------------------");
  }
}
