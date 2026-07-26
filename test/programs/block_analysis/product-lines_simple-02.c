extern void abort(void);
extern void __assert_fail(const char*, const char*, unsigned, const char*);
void reach_error() { __assert_fail("0","",0,""); }

extern int __VERIFIER_nondet_int(void);

/* global state */
int waterLevel = 1;
int methaneCritical = 0;
int pumpRunning = 0;
int systemActive = 1;

/* environment */
void waterRise() {
  if (waterLevel < 2) waterLevel++;
}

void changeMethaneLevel() {
  methaneCritical = !methaneCritical;
}

int isMethaneLevelCritical() { return methaneCritical; }

int isHighWaterLevel() { return waterLevel == 2; }

/* system */
void activatePump() { pumpRunning = 1; }
void deactivatePump() { pumpRunning = 0; }

int isPumpRunning() { return pumpRunning; }

void processEnvironment() {
  if (!isPumpRunning()) {
    if (isHighWaterLevel())
      activatePump();
  }
}

/* specification */
void spec() {
  if (!isMethaneLevelCritical()) {
    if (isHighWaterLevel()) {
      if (!isPumpRunning()) {
        reach_error();   // still reachable
      }
    }
  }
}

/* timing */
void timeShift() {
  if (systemActive) {
    spec();                 // check too early
    processEnvironment();   // reaction too late
  }
}

int t() {return __VERIFIER_nondet_int();}

/* control */
void startSystem() { systemActive = 1; }
void stopSystem()  { systemActive = 0; deactivatePump(); }

/* test harness */
int main() {
  int i;
  for (i = 0; i < t(); i++) {
    if (t()) waterRise();
    if (t()) changeMethaneLevel();

    if (t())
      startSystem();
    else if (t())
      stopSystem();

    timeShift();
  }
  return 0;
}
