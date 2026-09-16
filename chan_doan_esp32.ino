/*
  Code chan doan - dung de tim nguyen nhan MQ2 va coi khong hoat dong
  Khong lien quan gi den logic canh bao, chi test rieng tung phan
*/

#define MQ2_AO_PIN   34
#define FLAME_DO_PIN 27
#define BUZZER_PIN   25

unsigned long lastPrint = 0;
unsigned long lastBuzzerToggle = 0;
bool buzzerState = false;

void setup() {
  Serial.begin(115200);
  delay(500);
  pinMode(FLAME_DO_PIN, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  Serial.println("Bat dau chan doan...");
  Serial.println("Coi se tu bat/tat moi 2 giay de test doc lap, khong lien quan cam bien");
}

void loop() {
  // In gia tri MQ2 tho lien tuc de xem MQ2 co phan ung voi khoi khong
  if (millis() - lastPrint >= 300) {
    lastPrint = millis();
    int smokeRaw = analogRead(MQ2_AO_PIN);
    int flameRaw = digitalRead(FLAME_DO_PIN);
    Serial.print("MQ2 raw: ");
    Serial.print(smokeRaw);
    Serial.print(" | Flame DO raw: ");
    Serial.println(flameRaw);
  }

  // Tu dong bat/tat coi moi 2 giay, khong lien quan gi cam bien
  // Neu coi van khong keu -> chac chan loi day/coi, khong phai loi code
  if (millis() - lastBuzzerToggle >= 2000) {
    lastBuzzerToggle = millis();
    buzzerState = !buzzerState;
    digitalWrite(BUZZER_PIN, buzzerState ? HIGH : LOW);
    Serial.println(buzzerState ? ">>> Coi dang duoc bat (HIGH)" : ">>> Coi dang duoc tat (LOW)");
  }
}
