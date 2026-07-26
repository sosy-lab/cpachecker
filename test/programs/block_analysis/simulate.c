extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
extern int __VERIFIER_nondet_int(void);

void reach_error() { __assert_fail("0", "", 0, ""); }
void __automaton_fail(void) { reach_error(); abort(); }

/* state */
int waterLevel = 1;
int pumpRunning = 0;
int methaneCritical = 0;

/* environment */
void lowerWaterLevel(void) { if (waterLevel > 0) waterLevel--; }
void waterRise(void) { if (waterLevel < 2) waterLevel++; }
void changeMethaneLevel(void) { methaneCritical = !methaneCritical; }

int isLowWater(void) { return waterLevel == 0; }
int isMethaneAlarm(void) { return methaneCritical; }

/* system logic */
void activatePump(void) {
    if (!isMethaneAlarm())
        pumpRunning = 1;
}

void deactivatePump(void) { pumpRunning = 0; }

void processEnvironment(void) {
    if (pumpRunning && isMethaneAlarm())
        deactivatePump();
    if (!pumpRunning && waterLevel > 1)
        activatePump();
}

/* specification */
void spec(void) {
    if (isLowWater() && pumpRunning)
        __automaton_fail();
}

/* transition */
void timeShift(void) {
    if (pumpRunning)
        lowerWaterLevel();

    processEnvironment();
    spec();
}

/* scenario */
void test(void) {
    int i = 0;

    while (i < 4) {
        if (__VERIFIER_nondet_int())
            waterRise();

        if (__VERIFIER_nondet_int())
            changeMethaneLevel();

        if (__VERIFIER_nondet_int())
            activatePump();
        else if (__VERIFIER_nondet_int())
            deactivatePump();

        timeShift();
        i++;
    }

    /* ensure violation */
    methaneCritical = 0;
    pumpRunning = 1;
    waterLevel = 1;

    timeShift();  // -> 0
    timeShift();  // -> violation
}

int main(void) {
    test();
    return 0;
}
