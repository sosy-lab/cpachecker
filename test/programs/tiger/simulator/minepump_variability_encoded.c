# 1 "Environment.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Environment.c"
# 1 "Environment.h" 1
# 1 "featureselect.h" 1






int __SELECTED_FEATURE_base;

int __SELECTED_FEATURE_highWaterSensor;

int __SELECTED_FEATURE_lowWaterSensor;

int __SELECTED_FEATURE_methaneQuery;

int __SELECTED_FEATURE_methaneAlarm;

int __SELECTED_FEATURE_stopCommand;

int __SELECTED_FEATURE_startCommand;

int __GUIDSL_ROOT_PRODUCTION;


int select_one();
void select_features();
void select_helpers();
int valid_product();
# 2 "Environment.h" 2


void lowerWaterLevel() ;

void waterRise();

void changeMethaneLevel();

int isMethaneLevelCritical();

int getWaterLevel();


void printEnvironment();
int isHighWaterSensorDry() ;
int isLowWaterSensorDry() ;
# 2 "Environment.c" 2







  int waterLevel = 1;


  int methaneLevelCritical = 0;


 void lowerWaterLevel() {
  if (waterLevel > 0) {
   waterLevel = waterLevel-1;
  }
 }


 void waterRise() {
  if (waterLevel < 2) {
   waterLevel = waterLevel+1;
  }
 }


 void changeMethaneLevel() {
  if (methaneLevelCritical) {
   methaneLevelCritical = 0;
  } else {
   methaneLevelCritical = 1;
  }
 }


 int isMethaneLevelCritical() {
  return methaneLevelCritical;
 }



 void printEnvironment() {
# 53 "Environment.c"
 }


 int getWaterLevel() {
  return waterLevel;
 }
 int isHighWaterSensorDry() {


  if (waterLevel < 2) {
   return 1;
  } else {
   return 0;
  }
 }
 int isLowWaterSensorDry() {
  return waterLevel == 0;
 }
# 1 "featureselect.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "featureselect.c"
# 1 "featureselect.h" 1






int __SELECTED_FEATURE_base;

int __SELECTED_FEATURE_highWaterSensor;

int __SELECTED_FEATURE_lowWaterSensor;

int __SELECTED_FEATURE_methaneQuery;

int __SELECTED_FEATURE_methaneAlarm;

int __SELECTED_FEATURE_stopCommand;

int __SELECTED_FEATURE_startCommand;

int __GUIDSL_ROOT_PRODUCTION;


int select_one();
void select_features();
void select_helpers();
int valid_product();
# 2 "featureselect.c" 2






extern int __VERIFIER_nondet_int(void);
int select_one() {if (__VERIFIER_nondet_int()) return 1; else return 0;}


void select_features() {
 __SELECTED_FEATURE_base = 1;
 __SELECTED_FEATURE_highWaterSensor = select_one();
 __SELECTED_FEATURE_lowWaterSensor = select_one();
 __SELECTED_FEATURE_methaneQuery = select_one();
 __SELECTED_FEATURE_methaneAlarm = select_one();
 __SELECTED_FEATURE_stopCommand = select_one();
 __SELECTED_FEATURE_startCommand = select_one();
}


void select_helpers() {
 __GUIDSL_ROOT_PRODUCTION = 1;
}

int valid_product() {
  return ( __SELECTED_FEATURE_base ) ;
}
# 1 "MinePump.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "MinePump.c"
# 1 "Environment.h" 1
# 1 "featureselect.h" 1






int __SELECTED_FEATURE_base;

int __SELECTED_FEATURE_highWaterSensor;

int __SELECTED_FEATURE_lowWaterSensor;

int __SELECTED_FEATURE_methaneQuery;

int __SELECTED_FEATURE_methaneAlarm;

int __SELECTED_FEATURE_stopCommand;

int __SELECTED_FEATURE_startCommand;

int __GUIDSL_ROOT_PRODUCTION;


int select_one();
void select_features();
void select_helpers();
int valid_product();
# 2 "Environment.h" 2


void lowerWaterLevel() ;

void waterRise();

void changeMethaneLevel();

int isMethaneLevelCritical();

int getWaterLevel();


void printEnvironment();
int isHighWaterSensorDry() ;
int isLowWaterSensorDry() ;
# 2 "MinePump.c" 2

# 1 "MinePump.h" 1



void timeShift();

void activatePump();

void deactivatePump();

int isPumpRunning();


void printPump();
void stopSystem();
void startSystem();
# 4 "MinePump.c" 2



int pumpRunning = 0;

int systemActive = 1;


 void timeShift() {
  if (pumpRunning)
   lowerWaterLevel();
  if (systemActive)
   processEnvironment();
 }

 void processEnvironment__before__highWaterSensor() {

 }

 void processEnvironment__role__highWaterSensor() {
  if (!pumpRunning && isHighWaterLevel()) {
   activatePump();
  } else {
   processEnvironment__before__highWaterSensor();
  }
 }

 void
processEnvironment__before__lowWaterSensor() {
    if (__SELECTED_FEATURE_highWaterSensor) {
        processEnvironment__role__highWaterSensor();
    } else {
        processEnvironment__before__highWaterSensor();
    }
}



 void processEnvironment__role__lowWaterSensor() {
  if (pumpRunning && isLowWaterLevel()) {
   deactivatePump();
  } else {
   processEnvironment__before__lowWaterSensor();
  }
 }

 void
processEnvironment__before__methaneAlarm() {
    if (__SELECTED_FEATURE_lowWaterSensor) {
        processEnvironment__role__lowWaterSensor();
    } else {
        processEnvironment__before__lowWaterSensor();
    }
}



 void processEnvironment__role__methaneAlarm() {
  if (pumpRunning && isMethaneAlarm()) {
   deactivatePump();
  } else {
   processEnvironment__before__methaneAlarm();
  }
 }
 void
processEnvironment() {
    if (__SELECTED_FEATURE_methaneAlarm) {
        processEnvironment__role__methaneAlarm();
    } else {
        processEnvironment__before__methaneAlarm();
    }
}




 void activatePump__before__methaneQuery() {
  pumpRunning = 1;
 }


 void activatePump__role__methaneQuery() {
  if (!isMethaneAlarm()) {
   activatePump__before__methaneQuery();
  } else {

  }
 }
 void
activatePump() {
    if (__SELECTED_FEATURE_methaneQuery) {
        activatePump__role__methaneQuery();
    } else {
        activatePump__before__methaneQuery();
    }
}




 void deactivatePump() {
  pumpRunning = 0;
 }


 int isMethaneAlarm() {
  return isMethaneLevelCritical();
 }


 int isPumpRunning() {
  return pumpRunning;
 }


 void printPump() {
# 135 "MinePump.c"
 }
 int isHighWaterLevel() {
  return ! isHighWaterSensorDry();
 }
 int isLowWaterLevel() {
  return isLowWaterSensorDry();


 }
 void stopSystem() {
  if (pumpRunning) {
   deactivatePump();
  }

  systemActive = 0;
 }
 void startSystem() {

  systemActive = 1;
 }
# 1 "scenario.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "scenario.c"
# 1 "featureselect.h" 1






int __SELECTED_FEATURE_base;

int __SELECTED_FEATURE_highWaterSensor;

int __SELECTED_FEATURE_lowWaterSensor;

int __SELECTED_FEATURE_methaneQuery;

int __SELECTED_FEATURE_methaneAlarm;

int __SELECTED_FEATURE_stopCommand;

int __SELECTED_FEATURE_startCommand;

int __GUIDSL_ROOT_PRODUCTION;


int select_one();
void select_features();
void select_helpers();
int valid_product();
# 2 "scenario.c" 2

void test() {
    int op1 = 0;
    int op2 = 0;
    int op3 = 0;
    int op4 = 0;
    int op5 = 0;
    int splverifierCounter = 0;
    while(splverifierCounter < 4) {
        op1 = 0;
        op2 = 0;
        op3 = 0;
        op4 = 0;
        op5 = 0;
        splverifierCounter = splverifierCounter + 1;
        if (!op1 && get_nondet()) {
                waterRise();
            op1 = 1;
        }
        else if (!op2 && get_nondet()) {
                changeMethaneLevel();
            op2 = 1;
        }
        else if (!op3 && get_nondet()) {
            if (__SELECTED_FEATURE_startCommand)
                startSystem();
            op3 = 1;
        }
        else if (!op4 && get_nondet()) {
            if (__SELECTED_FEATURE_stopCommand)
                stopSystem();
            op4 = 1;
        }
        else if (!op5 && get_nondet()) {
                timeShift();
            op5 = 1;
        }
        else break;
    }
        cleanup();
}
# 1 "Test.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Test.c"




int cleanupTimeShifts = 4;


int get_nondet() {
    int nd;
    return nd;
}


void cleanup() {


 timeShift();
 int i;
 for (i = 0; i < cleanupTimeShifts - 1; i++) {
  timeShift();
 }
}
# 54 "Test.c"
 void Specification2() {
  timeShift();printPump();
  timeShift();printPump();
  timeShift();printPump();
  waterRise();printPump();
  timeShift();printPump();
  changeMethaneLevel();printPump();
  timeShift();printPump();
  cleanup();
 }

void setup() {

}




void runTest() {


 test();
}

int
main (void)
{
  select_helpers();
  select_features();
  if (valid_product()) {
      setup();
      runTest();
  }
  return 0;

}
