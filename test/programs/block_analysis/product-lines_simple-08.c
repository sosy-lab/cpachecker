extern void abort(void);
extern void __assert_fail(const char*, const char*, unsigned, const char*);
void reach_error() { __assert_fail("0","",0,""); }

extern int __VERIFIER_nondet_int(void);

/* ================== STATE ================== */
int waterLevel = 1;
int methaneLevelCritical = 0;
int pumpRunning = 0;
int systemActive = 1;

/* ================== ENV ================== */
void lowerWaterLevel(void) {
  if (waterLevel > 0) waterLevel--;
}

void waterRise(void) {
  if (waterLevel < 2) waterLevel++;
}

void changeMethaneLevel(void) {
  methaneLevelCritical = !methaneLevelCritical;
}

int getWaterLevel(void) { return waterLevel; }

int isHighWaterSensorDry(void) {
  return waterLevel < 2;
}

/* ================== SYSTEM ================== */
int isHighWaterLevel(void) {
  return !isHighWaterSensorDry();
}

void activatePump(void) {
  if (!methaneLevelCritical)
    pumpRunning = 1;
}

void deactivatePump(void) {
  pumpRunning = 0;
}

int isPumpRunning(void) {
  return pumpRunning;
}

void processEnvironment(void) {
  if (!pumpRunning) {
    if (isHighWaterLevel()) {
      activatePump();
    }
  }
}

void stopSystem(void) {
  if (pumpRunning) deactivatePump();
  systemActive = 0;
}

/* ================== SPEC ================== */
void __automaton_fail(void) {
  ERROR: {reach_error(); abort();}
}

void __utac_acc__Specification4_spec__1(void) {
  if (getWaterLevel() == 0) {
    if (isPumpRunning()) {
      __automaton_fail();
    }
  }
}

/* ================== TIME ================== */
void timeShift(void) {
  if (pumpRunning) lowerWaterLevel();
  if (systemActive) processEnvironment();
  __utac_acc__Specification4_spec__1();
}

/* ================== TEST ================== */
void cleanup(void) {
  int i = 0;
  int __cil_tmp2;

  timeShift();

  while (1) {
    while_1_continue: ;
    __cil_tmp2 = 4 - 1;
    if (i < __cil_tmp2) {
    } else {
      goto while_1_break;
    }
    timeShift();
    i = i + 1;
  }
  while_1_break: ;
}

void test(void) {
  int splverifierCounter = 0;
  int tmp, tmp___0, tmp___1, tmp___2;

  while (1) {
    while_0_continue: ;
    if (splverifierCounter < 4) {
    } else {
      goto while_0_break;
    }

    tmp = __VERIFIER_nondet_int();
    if (tmp) waterRise();

    tmp___0 = __VERIFIER_nondet_int();
    if (tmp___0) changeMethaneLevel();

    tmp___2 = __VERIFIER_nondet_int();
    if (tmp___2) {
    } else {
      tmp___1 = __VERIFIER_nondet_int();
      if (tmp___1) stopSystem();
    }

    timeShift();
  }
  while_0_break: ;

  cleanup();
}

/* ================== MAIN ================== */
int main(void) {
  test();
  return 0;
}
