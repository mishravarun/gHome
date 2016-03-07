static int counter = 0;
String content = "";
char character;
int LED = 11; //the pin we connect the LED
static int ledState = 0; // off

void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
}

void loop() {
  if (Serial.peek() != -1) {
    Serial.print("Read: ");
    content="";
    do {
        character = Serial.read();
      content.concat(character);
    } while (Serial.peek() != -1);
    Serial.print(content);
    Serial.print("\n");
    if (content == "on")
     ledState = !ledState;
    digitalWrite(LED, ledState);
  }
  delay(1000);
}
