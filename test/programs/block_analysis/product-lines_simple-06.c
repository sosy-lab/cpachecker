extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);

void reach_error() {
    __assert_fail("0", "simplified.c", 0, "reach_error");
}

/* nondet input */
extern int __VERIFIER_nondet_int(void);

/* --- Environment --- */
int waterLevel = 1;
int methaneLevelCritical = 0;

/* --- System state --- */
int pumpRunning = 0;
int systemActive = 1;

/* --- Error condition --- */
void check_spec() {
    if (waterLevel == 0 && pumpRunning) {
        reach_error();
        abort();
    }
}

/* --- Environment updates --- */
void lowerWaterLevel() {
    if (waterLevel > 0) waterLevel--;
}

void waterRise() {
    if (waterLevel < 2) waterLevel++;
}

void changeMethaneLevel() {
    methaneLevelCritical = !methaneLevelCritical;
}

/* --- Sensors --- */
int isMethaneAlarm() {
    return methaneLevelCritical;
}

int isHighWaterLevel() {
    return waterLevel >= 2;
}

/* --- Pump control --- */
void activatePump() {
    if (!isMethaneAlarm()) {
        pumpRunning = 1;
    }
}

void deactivatePump() {
    pumpRunning = 0;
}

/* --- System logic --- */
void processEnvironment() {
    if (pumpRunning && isMethaneAlarm()) {
        deactivatePump();
    } else if (!pumpRunning && isHighWaterLevel()) {
        activatePump();
    }
}

/* --- Time step --- */
void timeShift() {
    if (pumpRunning) {
        lowerWaterLevel();
    }

    if (systemActive) {
        processEnvironment();
    }

    check_spec();  // <-- violation check
}

/* --- Scenario --- */
void test() {
    int i = 0;
    while (i < 4) {
        if (__VERIFIER_nondet_int()) waterRise();
        if (__VERIFIER_nondet_int()) changeMethaneLevel();

        if (__VERIFIER_nondet_int()) {
            systemActive = 1;
        } else if (__VERIFIER_nondet_int()) {
            systemActive = 0;
            pumpRunning = 0;
        }

        timeShift();
        i++;
    }
}

/* --- Main --- */
int main() {
    test();
    return 0;
}
