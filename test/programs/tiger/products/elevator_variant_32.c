# 1 "Elevator.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Elevator.c"
# 1 "Elevator.h" 1






# 1 "featureselect.h" 1
# 9 "featureselect.h"
int select_one();

void select_features();

void select_helpers();

int valid_product();
# 8 "Elevator.h" 2







# 1 "Person.h" 1
# 10 "Person.h"
int getWeight(int person);

int getOrigin(int person);

int getDestination(int person);

void enterElevator(int person);
# 16 "Elevator.h" 2

# 1 "Floor.h" 1
# 12 "Floor.h"
int isFloorCalling(int floorID);

void resetCallOnFloor(int floorID);

void callOnFloor(int floorID);

int isPersonOnFloor(int person, int floor);

void initPersonOnFloor(int person, int floor);

void removePersonFromFloor(int person, int floor);

int isTopFloor(int floorID);

void processWaitingPersons(int floorID);

void initFloors();
# 18 "Elevator.h" 2


void timeShift();

int isBlocked();

void printState();

int isEmpty();

int isAnyLiftButtonPressed();

int buttonForFloorIsPressed(int floorID);


void initTopDown();

void initBottomUp();


int areDoorsOpen();

int getCurrentFloorID();

int isIdle();







 int executiveFloor;


 int isExecutiveFloorCalling();

 int isExecutiveFloor(int floorID);


int blocked;
# 2 "Elevator.c" 2
# 13 "Elevator.c"
int currentHeading = 1;

int currentFloorID = 0;

int persons_0;

int persons_1;

int persons_2;

int persons_3;

int persons_4;

int persons_5;


int doorState = 1;

int floorButtons_0;

int floorButtons_1;

int floorButtons_2;

int floorButtons_3;

int floorButtons_4;







void initTopDown() {
 currentFloorID = 4;
 currentHeading = 0;
 floorButtons_0 = 0;
 floorButtons_1 = 0;
 floorButtons_2 = 0;
 floorButtons_3 = 0;
 floorButtons_4 = 0;
 persons_0 = 0;
 persons_1 = 0;
 persons_2 = 0;
 persons_3 = 0;
 persons_4 = 0;
 persons_5 = 0;
 initFloors();
}

void initBottomUp() {
 currentFloorID = 0;
 currentHeading = 1;
 floorButtons_0 = 0;
 floorButtons_1 = 0;
 floorButtons_2 = 0;
 floorButtons_3 = 0;
 floorButtons_4 = 0;
 persons_0 = 0;
 persons_1 = 0;
 persons_2 = 0;
 persons_3 = 0;
 persons_4 = 0;
 persons_5 = 0;
 initFloors();
}


 int isBlocked () {
  return blocked;
 }


void enterElevator__wrappee__base (int p) {
 if (p == 0) persons_0 = 1;
 else if (p == 1) persons_1 = 1;
 else if (p == 2) persons_2 = 1;
 else if (p == 3) persons_3 = 1;
 else if (p == 4) persons_4 = 1;
 else if (p == 5) persons_5 = 1;


}


void enterElevator(int p) {
 enterElevator__wrappee__base(p);
 weight = weight + getWeight(p);
}

void leaveElevator__wrappee__base (int p) {
 if (p == 0) persons_0 = 0;
 else if (p == 1) persons_1 = 0;
 else if (p == 2) persons_2 = 0;
 else if (p == 3) persons_3 = 0;
 else if (p == 4) persons_4 = 0;
 else if (p == 5) persons_5 = 0;

}

void leaveElevator__wrappee__weight (int p) {
 leaveElevator__wrappee__base(p);
 weight = weight - getWeight(p);
}


void leaveElevator(int p) {
 leaveElevator__wrappee__weight(p);

 if (isEmpty()) {
  floorButtons_0 = 0;
  floorButtons_1 = 0;
  floorButtons_2 = 0;
  floorButtons_3 = 0;
  floorButtons_4 = 0;
 }
}

void pressInLiftFloorButton(int floorID) {
 if (floorID == 0) floorButtons_0 = 1;
 else if (floorID == 1) floorButtons_1 = 1;
 else if (floorID == 2) floorButtons_2 = 1;
 else if (floorID == 3) floorButtons_3 = 1;
 else if (floorID == 4) floorButtons_4 = 1;
}

void resetFloorButton(int floorID) {
 if (floorID == 0) floorButtons_0 = 0;
 else if (floorID == 1) floorButtons_1 = 0;
 else if (floorID == 2) floorButtons_2 = 0;
 else if (floorID == 3) floorButtons_3 = 0;
 else if (floorID == 4) floorButtons_4 = 0;
}

int getCurrentFloorID() {
 return currentFloorID;
}

int areDoorsOpen() {
 return doorState;
}

int buttonForFloorIsPressed(int floorID) {
 if (floorID == 0) return floorButtons_0;
 else if (floorID == 1) return floorButtons_1;
 else if (floorID == 2) return floorButtons_2;
 else if (floorID == 3) return floorButtons_3;
 else if (floorID == 4) return floorButtons_4;
 else return 0;
}

int getCurrentHeading() {
 return currentHeading;
}

int isEmpty() {
 if (persons_0 == 1) return 0;
 else if (persons_1 == 1) return 0;
 else if (persons_2 == 1) return 0;
 else if (persons_3 == 1) return 0;
 else if (persons_4 == 1) return 0;
 else if (persons_5 == 1) return 0;
 return 1;
}


int anyStopRequested () {
 if (isFloorCalling(0)) return 1;
 else if (floorButtons_0) return 1;
 else if (isFloorCalling(1)) return 1;
 else if (floorButtons_1) return 1;
 else if (isFloorCalling(2)) return 1;
 else if (floorButtons_2) return 1;
 else if (isFloorCalling(3)) return 1;
 else if (floorButtons_3) return 1;
 else if (isFloorCalling(4)) return 1;
 else if (floorButtons_4) return 1;
 return 0;
}

int isIdle() {
 return (anyStopRequested() == 0);
}

 int stopRequestedInDirection__wrappee__empty (int dir, int respectFloorCalls, int respectInLiftCalls) {
  if (dir == 1) {
   if (isTopFloor(currentFloorID)) return 0;
# 210 "Elevator.c"
   if (currentFloorID < 0 && respectFloorCalls && isFloorCalling(0)) return 1;
   else if (currentFloorID < 0 && respectInLiftCalls && floorButtons_0) return 1;

   else if (currentFloorID < 1 && respectFloorCalls && isFloorCalling(1)) return 1;
   else if (currentFloorID < 1 && respectInLiftCalls && floorButtons_1) return 1;

   else if (currentFloorID < 2 && respectFloorCalls && isFloorCalling(2)) return 1;
   else if (currentFloorID < 2 && respectInLiftCalls && floorButtons_2) return 1;

   else if (currentFloorID < 3 && respectFloorCalls && isFloorCalling(3)) return 1;
   else if (currentFloorID < 3 && respectInLiftCalls && floorButtons_3) return 1;

   else if (currentFloorID < 4 && respectFloorCalls && isFloorCalling(4)) return 1;
   else if (currentFloorID < 4 && respectInLiftCalls && floorButtons_4) return 1;
   else return 0;
  } else {
   if (currentFloorID == 0) return 0;
# 235 "Elevator.c"
   if (currentFloorID > 0 && respectFloorCalls && isFloorCalling(0)) return 1;
   else if (currentFloorID > 0 && respectInLiftCalls && floorButtons_0) return 1;

   else if (currentFloorID > 1 && respectFloorCalls && isFloorCalling(1)) return 1;
   else if (currentFloorID > 1 && respectInLiftCalls && floorButtons_1) return 1;

   else if (currentFloorID > 2 && respectFloorCalls && isFloorCalling(2)) return 1;
   else if (currentFloorID > 2 && respectInLiftCalls && floorButtons_2) return 1;

   else if (currentFloorID > 3 && respectFloorCalls && isFloorCalling(3)) return 1;
   else if (currentFloorID > 3 &&respectInLiftCalls && floorButtons_3) return 1;

   else if (currentFloorID > 4 && respectFloorCalls && isFloorCalling(4)) return 1;
   else if (currentFloorID > 4 && respectInLiftCalls && floorButtons_4) return 1;
   else return 0;
  }
 }

 int stopRequestedInDirection__wrappee__twothirdsfull (int dir, int respectFloorCalls, int respectInLiftCalls) {
  int overload =weight > maximumWeight*2/3;
  int buttonPressed =isAnyLiftButtonPressed();
  if (overload && buttonPressed) {
   return stopRequestedInDirection__wrappee__empty(dir, 0, respectInLiftCalls);
  } else return stopRequestedInDirection__wrappee__empty(dir, respectFloorCalls, respectInLiftCalls);
 }


 int stopRequestedInDirection (int dir, int respectFloorCalls, int respectInLiftCalls) {
  if (isExecutiveFloorCalling()) {
   return ((getCurrentFloorID() < executiveFloor) == (dir == 1));
  } else {
   return stopRequestedInDirection__wrappee__twothirdsfull(dir, respectFloorCalls, respectInLiftCalls);
  }
 }

 int isAnyLiftButtonPressed() {
  if (floorButtons_0) return 1;
  else if (floorButtons_1) return 1;
  else if (floorButtons_2) return 1;
  else if (floorButtons_3) return 1;
  else if (floorButtons_4) return 1;
  else return 0;
 }

 void continueInDirection(int dir) {
  currentHeading = dir;
  if (currentHeading == 1) {
   if (isTopFloor(currentFloorID)) {

    currentHeading = 0;
   }
  } else {
   if (currentFloorID == 0) {

    currentHeading = 1;
   }
  }
  if (currentHeading == 1) {
   currentFloorID = currentFloorID + 1;
  } else {
   currentFloorID = currentFloorID - 1;
  }
 }

 int stopRequestedAtCurrentFloor__wrappee__empty () {
  if (isFloorCalling(currentFloorID)) {
   return 1;
  } else if (buttonForFloorIsPressed(currentFloorID)) {
   return 1;
  } else {
   return 0;
  }
 }

 int stopRequestedAtCurrentFloor__wrappee__twothirdsfull () {
  if (weight > maximumWeight*2/3) {
   return buttonForFloorIsPressed(getCurrentFloorID()) == 1;
  } else return stopRequestedAtCurrentFloor__wrappee__empty();
 }


 int stopRequestedAtCurrentFloor() {
  if (isExecutiveFloorCalling() && ! (executiveFloor == getCurrentFloorID())) {
   return 0;
  } else return stopRequestedAtCurrentFloor__wrappee__twothirdsfull();
 }

 int getReverseHeading(int ofHeading) {
  if (ofHeading==0) {
   return 1;
  } else return 0;
 }


void processWaitingOnFloor(int floorID) {
  if (isPersonOnFloor(0,floorID)) {
   removePersonFromFloor(0, floorID);
   pressInLiftFloorButton(getDestination(0));
   enterElevator(0);
  }
  if (isPersonOnFloor(1,floorID)) {
   removePersonFromFloor(1, floorID);
   pressInLiftFloorButton(getDestination(1));
   enterElevator(1);
  }
  if (isPersonOnFloor(2,floorID)) {
   removePersonFromFloor(2, floorID);
   pressInLiftFloorButton(getDestination(2));
   enterElevator(2);
  }
  if (isPersonOnFloor(3,floorID)) {
   removePersonFromFloor(3, floorID);
   pressInLiftFloorButton(getDestination(3));
   enterElevator(3);
  }
  if (isPersonOnFloor(4,floorID)) {
   removePersonFromFloor(4, floorID);
   pressInLiftFloorButton(getDestination(4));
   enterElevator(4);
  }
  if (isPersonOnFloor(5,floorID)) {
   removePersonFromFloor(5, floorID);
   pressInLiftFloorButton(getDestination(5));
   enterElevator(5);
  }
  resetCallOnFloor(floorID);
 }


 void timeShift__wrappee__executivefloor () {


  if (stopRequestedAtCurrentFloor()) {

   doorState = 1;

   if (persons_0 && getDestination(0) == currentFloorID) leaveElevator(0);
   if (persons_1 && getDestination(1) == currentFloorID) leaveElevator(1);
   if (persons_2 && getDestination(2) == currentFloorID) leaveElevator(2);
   if (persons_3 && getDestination(3) == currentFloorID) leaveElevator(3);
   if (persons_4 && getDestination(4) == currentFloorID) leaveElevator(4);
   if (persons_5 && getDestination(5) == currentFloorID) leaveElevator(5);
   processWaitingOnFloor(currentFloorID);
   resetFloorButton(currentFloorID);
  } else {
   if (doorState == 1) {
    doorState = 0;

   }
   if (stopRequestedInDirection(currentHeading, 1, 1)) {


    continueInDirection(currentHeading);
   } else if (stopRequestedInDirection(getReverseHeading(currentHeading), 1, 1)) {


    continueInDirection(getReverseHeading(currentHeading));
   } else {


    continueInDirection(currentHeading);
   }
  }
 }



 void timeShift() {
  if (areDoorsOpen() && weight > maximumWeight) {
   blocked = 1;
  } else {
   blocked = 0;
   timeShift__wrappee__executivefloor();
  }
 }

 void printState__wrappee__executivefloor () {
# 435 "Elevator.c"
 }

 void printState() {

  printState__wrappee__executivefloor();
 }


 int existInLiftCallsInDirection(int d) {
   if (d == 1) {
     int i = 0;
    for (i = currentFloorID + 1; i < 5; i++) {
      if (i==0 && floorButtons_0) return 1;
      else if (i==1 && floorButtons_1) return 1;
      else if (i==2 && floorButtons_2) return 1;
      else if (i==3 && floorButtons_3) return 1;
      else if (i==4 && floorButtons_4) return 1;
    }
   } else if (d == 0) {
     int i = 0;
    for (i = currentFloorID - 1; i >= 0; i--)
    for (i = currentFloorID + 1; i < 5; i++) {
      if (i==0 && floorButtons_0) return 1;
      else if (i==1 && floorButtons_1) return 1;
      else if (i==2 && floorButtons_2) return 1;
      else if (i==3 && floorButtons_3) return 1;
      else if (i==4 && floorButtons_4) return 1;
    }
   }
   return 0;
 }
int weight = 0;

int maximumWeight = 100;

 int executiveFloor = 4;


 int isExecutiveFloorCalling() {
  return isFloorCalling(executiveFloor);
 }

 int isExecutiveFloor(int floorID) {
  return (executiveFloor == floorID);
 }
int blocked = 0;
# 1 "featureselect.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "featureselect.c"
# 1 "featureselect.h" 1
# 9 "featureselect.h"
int select_one();

void select_features();

void select_helpers();

int valid_product();
# 2 "featureselect.c" 2






extern int __VERIFIER_nondet_int(void);

int select_one() {if (__VERIFIER_nondet_int()) return 1; else return 0;}


void select_features() {

}



void select_helpers() {

}


int valid_product() {
  return 1;
}
# 1 "Floor.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Floor.c"



# 1 "Floor.h" 1






# 1 "featureselect.h" 1
# 9 "featureselect.h"
int select_one();

void select_features();

void select_helpers();

int valid_product();
# 8 "Floor.h" 2




int isFloorCalling(int floorID);

void resetCallOnFloor(int floorID);

void callOnFloor(int floorID);

int isPersonOnFloor(int person, int floor);

void initPersonOnFloor(int person, int floor);

void removePersonFromFloor(int person, int floor);

int isTopFloor(int floorID);

void processWaitingPersons(int floorID);

void initFloors();
# 5 "Floor.c" 2


int calls_0;

int calls_1;

int calls_2;

int calls_3;

int calls_4;


int personOnFloor_0_0;

int personOnFloor_0_1;

int personOnFloor_0_2;

int personOnFloor_0_3;

int personOnFloor_0_4;

int personOnFloor_1_0;

int personOnFloor_1_1;

int personOnFloor_1_2;

int personOnFloor_1_3;

int personOnFloor_1_4;

int personOnFloor_2_0;

int personOnFloor_2_1;

int personOnFloor_2_2;

int personOnFloor_2_3;

int personOnFloor_2_4;

int personOnFloor_3_0;

int personOnFloor_3_1;

int personOnFloor_3_2;

int personOnFloor_3_3;

int personOnFloor_3_4;

int personOnFloor_4_0;

int personOnFloor_4_1;

int personOnFloor_4_2;

int personOnFloor_4_3;

int personOnFloor_4_4;

int personOnFloor_5_0;

int personOnFloor_5_1;

int personOnFloor_5_2;

int personOnFloor_5_3;

int personOnFloor_5_4;


void initFloors() {
 calls_0 = 0;
 calls_1 = 0;
 calls_2 = 0;
 calls_3 = 0;
 calls_4 = 0;
 personOnFloor_0_0 = 0;
 personOnFloor_0_1 = 0;
 personOnFloor_0_2 = 0;
 personOnFloor_0_3 = 0;
 personOnFloor_0_4 = 0;
 personOnFloor_1_0 = 0;
 personOnFloor_1_1 = 0;
 personOnFloor_1_2 = 0;
 personOnFloor_1_3 = 0;
 personOnFloor_1_4 = 0;
 personOnFloor_2_0 = 0;
 personOnFloor_2_1 = 0;
 personOnFloor_2_2 = 0;
 personOnFloor_2_3 = 0;
 personOnFloor_2_4 = 0;
 personOnFloor_3_0 = 0;
 personOnFloor_3_1 = 0;
 personOnFloor_3_2 = 0;
 personOnFloor_3_3 = 0;
 personOnFloor_3_4 = 0;
 personOnFloor_4_0 = 0;
 personOnFloor_4_1 = 0;
 personOnFloor_4_2 = 0;
 personOnFloor_4_3 = 0;
 personOnFloor_4_4 = 0;
 personOnFloor_5_0 = 0;
 personOnFloor_5_1 = 0;
 personOnFloor_5_2 = 0;
 personOnFloor_5_3 = 0;
 personOnFloor_5_4 = 0;
}


int isFloorCalling(int floorID) {
 if (floorID == 0) return calls_0;
 else if (floorID == 1) return calls_1;
 else if (floorID == 2) return calls_2;
 else if (floorID == 3) return calls_3;
 else if (floorID == 4) return calls_4;
 return 0;
}


void resetCallOnFloor(int floorID) {
 if (floorID == 0) calls_0 = 0;
 else if (floorID == 1) calls_1 = 0;
 else if (floorID == 2) calls_2 = 0;
 else if (floorID == 3) calls_3 = 0;
 else if (floorID == 4) calls_4 = 0;
}


void callOnFloor(int floorID) {
 if (floorID == 0) calls_0 = 1;
 else if (floorID == 1) calls_1 = 1;
 else if (floorID == 2) calls_2 = 1;
 else if (floorID == 3) calls_3 = 1;
 else if (floorID == 4) calls_4 = 1;
}


int isPersonOnFloor(int person, int floor) {
 if (floor == 0) {
  if (person == 0) return personOnFloor_0_0;
  else if (person == 1) return personOnFloor_1_0;
  else if (person == 2) return personOnFloor_2_0;
  else if (person == 3) return personOnFloor_3_0;
  else if (person == 4) return personOnFloor_4_0;
  else if (person == 5) return personOnFloor_5_0;
 } else if (floor == 1) {
  if (person == 0) return personOnFloor_0_1;
  else if (person == 1) return personOnFloor_1_1;
  else if (person == 2) return personOnFloor_2_1;
  else if (person == 3) return personOnFloor_3_1;
  else if (person == 4) return personOnFloor_4_1;
  else if (person == 5) return personOnFloor_5_1;
 } else if (floor == 2) {
  if (person == 0) return personOnFloor_0_2;
  else if (person == 1) return personOnFloor_1_2;
  else if (person == 2) return personOnFloor_2_2;
  else if (person == 3) return personOnFloor_3_2;
  else if (person == 4) return personOnFloor_4_2;
  else if (person == 5) return personOnFloor_5_2;
 } else if (floor == 3) {
  if (person == 0) return personOnFloor_0_3;
  else if (person == 1) return personOnFloor_1_3;
  else if (person == 2) return personOnFloor_2_3;
  else if (person == 3) return personOnFloor_3_3;
  else if (person == 4) return personOnFloor_4_3;
  else if (person == 5) return personOnFloor_5_3;
 } else if (floor == 4) {
  if (person == 0) return personOnFloor_0_4;
  else if (person == 1) return personOnFloor_1_4;
  else if (person == 2) return personOnFloor_2_4;
  else if (person == 3) return personOnFloor_3_4;
  else if (person == 4) return personOnFloor_4_4;
  else if (person == 5) return personOnFloor_5_4;
 }
 return 0;
}

void initPersonOnFloor(int person, int floor) {
 if (floor == 0) {
  if (person == 0) personOnFloor_0_0 = 1;
  else if (person == 1) personOnFloor_1_0 = 1;
  else if (person == 2) personOnFloor_2_0 = 1;
  else if (person == 3) personOnFloor_3_0 = 1;
  else if (person == 4) personOnFloor_4_0 = 1;
  else if (person == 5) personOnFloor_5_0 = 1;
 } else if (floor == 1) {
  if (person == 0) personOnFloor_0_1 = 1;
  else if (person == 1) personOnFloor_1_1 = 1;
  else if (person == 2) personOnFloor_2_1 = 1;
  else if (person == 3) personOnFloor_3_1 = 1;
  else if (person == 4) personOnFloor_4_1 = 1;
  else if (person == 5) personOnFloor_5_1 = 1;
 } else if (floor == 2) {
  if (person == 0) personOnFloor_0_2 = 1;
  else if (person == 1) personOnFloor_1_2 = 1;
  else if (person == 2) personOnFloor_2_2 = 1;
  else if (person == 3) personOnFloor_3_2 = 1;
  else if (person == 4) personOnFloor_4_2 = 1;
  else if (person == 5) personOnFloor_5_2 = 1;
 } else if (floor == 3) {
  if (person == 0) personOnFloor_0_3 = 1;
  else if (person == 1) personOnFloor_1_3 = 1;
  else if (person == 2) personOnFloor_2_3 = 1;
  else if (person == 3) personOnFloor_3_3 = 1;
  else if (person == 4) personOnFloor_4_3 = 1;
  else if (person == 5) personOnFloor_5_3 = 1;
 } else if (floor == 4) {
  if (person == 0) personOnFloor_0_4 = 1;
  else if (person == 1) personOnFloor_1_4 = 1;
  else if (person == 2) personOnFloor_2_4 = 1;
  else if (person == 3) personOnFloor_3_4 = 1;
  else if (person == 4) personOnFloor_4_4 = 1;
  else if (person == 5) personOnFloor_5_4 = 1;
 }
 callOnFloor(floor);
}

void removePersonFromFloor(int person, int floor) {
 if (floor == 0) {
  if (person == 0) personOnFloor_0_0= 0;
  else if (person == 1) personOnFloor_1_0= 0;
  else if (person == 2) personOnFloor_2_0= 0;
  else if (person == 3) personOnFloor_3_0= 0;
  else if (person == 4) personOnFloor_4_0= 0;
  else if (person == 5) personOnFloor_5_0= 0;
 } else if (floor == 1) {
  if (person == 0) personOnFloor_0_1= 0;
  else if (person == 1) personOnFloor_1_1= 0;
  else if (person == 2) personOnFloor_2_1= 0;
  else if (person == 3) personOnFloor_3_1= 0;
  else if (person == 4) personOnFloor_4_1= 0;
  else if (person == 5) personOnFloor_5_1= 0;
 } else if (floor == 2) {
  if (person == 0) personOnFloor_0_2= 0;
  else if (person == 1) personOnFloor_1_2= 0;
  else if (person == 2) personOnFloor_2_2= 0;
  else if (person == 3) personOnFloor_3_2= 0;
  else if (person == 4) personOnFloor_4_2= 0;
  else if (person == 5) personOnFloor_5_2= 0;
 } else if (floor == 3) {
  if (person == 0) personOnFloor_0_3= 0;
  else if (person == 1) personOnFloor_1_3= 0;
  else if (person == 2) personOnFloor_2_3= 0;
  else if (person == 3) personOnFloor_3_3= 0;
  else if (person == 4) personOnFloor_4_3= 0;
  else if (person == 5) personOnFloor_5_3= 0;
 } else if (floor == 4) {
  if (person == 0) personOnFloor_0_4= 0;
  else if (person == 1) personOnFloor_1_4= 0;
  else if (person == 2) personOnFloor_2_4= 0;
  else if (person == 3) personOnFloor_3_4= 0;
  else if (person == 4) personOnFloor_4_4= 0;
  else if (person == 5) personOnFloor_5_4= 0;
 }
 resetCallOnFloor(floor);
}

int isTopFloor(int floorID) {
 return floorID == 5 -1;
}
# 1 "Person.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Person.c"



# 1 "Person.h" 1






# 1 "featureselect.h" 1
# 9 "featureselect.h"
int select_one();

void select_features();

void select_helpers();

int valid_product();
# 8 "Person.h" 2


int getWeight(int person);

int getOrigin(int person);

int getDestination(int person);

void enterElevator(int person);
# 5 "Person.c" 2
# 18 "Person.c"
int getWeight(int person) {
 if (person == 0) {
  return 40;
 } else if (person == 1) {
  return 40;
 } else if (person == 2) {
  return 40;
 } else if (person == 3) {
  return 40;
 } else if (person == 4) {
  return 30;
 } else if (person == 5) {
  return 150;
 } else {
  return 0;
 }
}


int getOrigin(int person) {
 if (person == 0) {
  return 4;
 } else if (person == 1) {
  return 3;
 } else if (person == 2) {
  return 2;
 } else if (person == 3) {
  return 1;
 } else if (person == 4) {
  return 0;
 } else if (person == 5) {
  return 1;
 } else {
  return 0;
 }
}

int getDestination(int person) {
 if (person == 0) {
  return 0;
 } else if (person == 1) {
  return 0;
 } else if (person == 2) {
  return 1;
 } else if (person == 3) {
  return 3;
 } else if (person == 4) {
  return 1;
 } else if (person == 5) {
  return 3;
 } else {
  return 0;
 }
}
# 1 "scenario.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "scenario.c"
void test() {
    if (get_nondet()) {
        initTopDown();
    } else {
        initBottomUp();
    }
    int op1 = 0;
    int op2 = 0;
    int op3 = 0;
    int op4 = 0;
    int op5 = 0;
    int op6 = 0;
    int op7 = 0;
    int op8 = 0;
    int splverifierCounter = 0;
    while(splverifierCounter < 4) {
        op1 = 0;
        op2 = 0;
        op3 = 0;
        op4 = 0;
        op5 = 0;
        op6 = 0;
        op7 = 0;
        op8 = 0;
        splverifierCounter = splverifierCounter + 1;
        if (!op1 && get_nondet()) {
            bobCall();
            op1 = 1;
        }
        else if (!op2 && get_nondet()) {
            aliceCall();
            op2 = 1;
        }
        else if (!op3 && get_nondet()) {
            angelinaCall();
            op3 = 1;
        }
        else if (!op4 && get_nondet()) {
            chuckCall();
            op4 = 1;
        }
        else if (!op5 && get_nondet()) {
            monicaCall();
            op5 = 1;
        }
        else if (!op6 && get_nondet()) {
            bigMacCall();
            op6 = 1;
        }
        else if (!op7 && get_nondet()) {
            timeShift();
            op7 = 1;
        }
        else if (!op8 && get_nondet()) {
            threeTS();
            op8 = 1;
        }
        else break;
    }
    cleanup();
}
# 1 "Test.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Test.c"
# 10 "Test.c"
int cleanupTimeShifts = 12;


extern int input();

int get_nondet() {
    int nd;
    nd = input();
    return nd;
}


int get_nondetMinMax07() {
    int nd;
    nd = input();
    if (nd==0) {
 return 0;
    } else if (nd==1) {
 return 1;
    } else if (nd==2) {
 return 2;
    } else if (nd==3) {
 return 3;
    } else if (nd==4) {
 return 4;
    } else if (nd==5) {
 return 5;
    } else if (nd==6) {
 return 6;
    } else {
 return 7;
    }
}


void bobCall() { initPersonOnFloor(0, getOrigin(0)); }

void aliceCall() { initPersonOnFloor(1, getOrigin(1)); }

void angelinaCall() { initPersonOnFloor(2, getOrigin(2)); }

void chuckCall() { initPersonOnFloor(3, getOrigin(3)); }

void monicaCall() { initPersonOnFloor(4, getOrigin(4)); }

void bigMacCall() { initPersonOnFloor(5, getOrigin(5)); }

void threeTS() { timeShift(); timeShift(); timeShift(); }

void cleanup() {


 timeShift();
 int i;
 for (i = 0; i < cleanupTimeShifts-1 && isBlocked()!=1; i++) {
  if (isIdle())
   return;
  else
   timeShift();

 }
}


void randomSequenceOfActions() {
  int maxLength = 4;
  if (get_nondet()) {

   initTopDown();


  } else {
   initBottomUp();



  }
  int counter = 0;
  while (counter < maxLength) {
   counter++;
   int action = get_nondetMinMax07();





   if (action < 6) {
    int origin = getOrigin(action);
    initPersonOnFloor(action, origin);
   } else if (action == 6) {
    timeShift();
   } else if (action == 7) {

    timeShift();
    timeShift();
    timeShift();

   }


   if (isBlocked()) {
    return;
   }
  }
  cleanup();
 }



void runTest_Simple() {
 bigMacCall();
 angelinaCall();
 cleanup();
}



 void Specification1() {
  bigMacCall();
  angelinaCall();
  cleanup();
 }


 void Specification2() {
  bigMacCall();
  cleanup();
 }

 void Specification3() {
  bobCall();
  timeShift();
  timeShift();
  timeShift();
  timeShift();

  timeShift();




  bobCall();
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
