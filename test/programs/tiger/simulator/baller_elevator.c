#include <stdio.h>
//#include <string.h>
#include <stdlib.h>

//#define PRINTING //Allows for printing of Mails. Quite usefull for debuging etc.
#define LOCAL //Activate for local random number generator and valid program configurations for each run. Deactivate when using CPA

#ifdef LOCAL
int __VERIFIER_nondet_int() {
	return rand();
}
#endif

#ifndef LOCAL
extern int __VERIFIER_nondet_int();
#endif

int helper = 0;
int scenario;

int __SELECTED_FEATURE_base;
int __SELECTED_FEATURE_weight;
int __SELECTED_FEATURE_empty;
int __SELECTED_FEATURE_twothirdsfull;
int __SELECTED_FEATURE_executivefloor;
int __SELECTED_FEATURE_overloaded;
int __GUIDSL_ROOT_PRODUCTION;

void select_features(void);
void select_helpers(void);
int valid_product(void);
void bigMacCall(void);
void angelinaCall(void);
void cleanup(void);
int areDoorsOpen(void);
int getCurrentFloorID(void);
int isFloorCalling(int floorID);
void resetCallOnFloor(int floorID);
void callOnFloor(int floorID);
int isPersonOnFloor(int person, int floor);
void initPersonOnFloor(int person, int floor);
void removePersonFromFloor(int person, int floor);
int isTopFloor(int floorID);
void initFloors(void);
int getWeight(int person);
int getDestination(int person);
void enterElevator(int p);
void timeShift(void);
int isBlocked(void);
void printState(void);
int isEmpty(void);
int isAnyLiftButtonPressed(void);
int buttonForFloorIsPressed(int floorID);
void initTopDown(void);
void initBottomUp(void);
int isIdle(void);
int isExecutiveFloorCalling(void);
int isExecutiveFloor(int floorID);
int getOrigin(int person);
int getCurrentHeading(void);
void bobCall(void);
void threeTS(void);

int weight = 0;
int maximumWeight = 100;
int executiveFloor = 4;
int blocked = 0;
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
int cleanupTimeShifts = 12;

void __automaton_fail(void)
{

	{
		goto error;
	error: helper = helper + 1;
		return;
	}
}

int landingButtons_spc1_0;
int landingButtons_spc1_1;
int landingButtons_spc1_2;
int landingButtons_spc1_3;
int landingButtons_spc1_4;

__inline void __utac_acc__Specification1_spec__1(void)
{
	landingButtons_spc1_0 = 0;
	landingButtons_spc1_1 = 0;
	landingButtons_spc1_2 = 0;
	landingButtons_spc1_3 = 0;
	landingButtons_spc1_4 = 0;
	return;
}

__inline void __utac_acc__Specification1_spec__2(int floor)
{
	if (floor == 0) {
		landingButtons_spc1_0 = 1;
	}
	else {
		if (floor == 1) {
			landingButtons_spc1_1 = 1;
		}
		else {
			if (floor == 2) {
				landingButtons_spc1_2 = 1;
			}
			else {
				if (floor == 3) {
					landingButtons_spc1_3 = 1;
				}
				else {
					if (floor == 4) {
						landingButtons_spc1_4 = 1;
					}
					else {

					}
				}
			}
		}
	}
	return;
}

__inline void __utac_acc__Specification1_spec__3(void)
{
	int floor;
	floor = getCurrentFloorID();
	if (floor == 0) {
		if (landingButtons_spc1_0) {
			if (areDoorsOpen()) {
				landingButtons_spc1_0 = 0;
			}
			else {
				goto _L___6;
			}
		}
		else {
			goto _L___6;
		}
	}
	else {
	_L___6: /* CIL Label */
		if (floor == 1) {
			if (landingButtons_spc1_1) {
				if (areDoorsOpen()) {
					landingButtons_spc1_1 = 0;
				}
				else {
					goto _L___4;
				}
			}
			else {
				goto _L___4;
			}
		}
		else {
		_L___4: /* CIL Label */
			if (floor == 2) {
				if (landingButtons_spc1_2) {
					if (areDoorsOpen()) {
						landingButtons_spc1_2 = 0;
					}
					else {
						goto _L___2;
					}
				}
				else {
					goto _L___2;
				}
			}
			else {
			_L___2: /* CIL Label */
				if (floor == 3) {
					if (landingButtons_spc1_3) {
						if (areDoorsOpen()) {
							landingButtons_spc1_3 = 0;
						}
						else {
							goto _L___0;
						}
					}
					else {
						goto _L___0;
					}
				}
				else {
				_L___0: /* CIL Label */
					if (floor == 4) {
						if (landingButtons_spc1_4) {
							if (areDoorsOpen()) {
								landingButtons_spc1_4 = 0;
							}
							else {

							}
						}
						else {

						}
					}
					else {

					}
				}
			}
		}
	}
	return;
}

__inline void __utac_acc__Specification1_spec__4(void)
{
	if (landingButtons_spc1_0) {
		__automaton_fail();
	}
	else {
		if (landingButtons_spc1_1) {
			__automaton_fail();
		}
		else {
			if (landingButtons_spc1_2) {
				__automaton_fail();
			}
			else {
				if (landingButtons_spc1_3) {
					__automaton_fail();
				}
				else {
					if (landingButtons_spc1_4) {
						__automaton_fail();
					}
					else {

					}
				}
			}
		}
	}
	return;
}

int floorButtons_spc2_0;
int floorButtons_spc2_1;
int floorButtons_spc2_2;
int floorButtons_spc2_3;
int floorButtons_spc2_4;

__inline void __utac_acc__Specification2_spec__1(void)
{
	floorButtons_spc2_0 = 0;
	floorButtons_spc2_1 = 0;
	floorButtons_spc2_2 = 0;
	floorButtons_spc2_3 = 0;
	floorButtons_spc2_4 = 0;
	return;
}

__inline void __utac_acc__Specification2_spec__2(int floor)
{
	if (floor == 0) {
		floorButtons_spc2_0 = 1;
	}
	else {
		if (floor == 1) {
			floorButtons_spc2_1 = 1;
		}
		else {
			if (floor == 2) {
				floorButtons_spc2_2 = 1;
			}
			else {
				if (floor == 3) {
					floorButtons_spc2_3 = 1;
				}
				else {
					if (floor == 4) {
						floorButtons_spc2_4 = 1;
					}
					else {

					}
				}
			}
		}
	}
	return;
}

__inline void __utac_acc__Specification2_spec__3(void)
{
	int floor;
	floor = getCurrentFloorID();
	if (floor == 0) {
		if (floorButtons_spc2_0) {
			if (areDoorsOpen()) {
				floorButtons_spc2_0 = 0;
			}
			else {
				goto _L___6;
			}
		}
		else {
			goto _L___6;
		}
	}
	else {
	_L___6: /* CIL Label */
		if (floor == 1) {
			if (floorButtons_spc2_1) {
				if (areDoorsOpen()) {
					floorButtons_spc2_1 = 0;
				}
				else {
					goto _L___4;
				}
			}
			else {
				goto _L___4;
			}
		}
		else {
		_L___4: /* CIL Label */
			if (floor == 2) {
				if (floorButtons_spc2_2) {
					if (areDoorsOpen()) {
						floorButtons_spc2_2 = 0;
					}
					else {
						goto _L___2;
					}
				}
				else {
					goto _L___2;
				}
			}
			else {
			_L___2: /* CIL Label */
				if (floor == 3) {
					if (floorButtons_spc2_3) {
						if (areDoorsOpen()) {
							floorButtons_spc2_3 = 0;
						}
						else {
							goto _L___0;
						}
					}
					else {
						goto _L___0;
					}
				}
				else {
				_L___0: /* CIL Label */
					if (floor == 4) {
						if (floorButtons_spc2_4) {
							if (areDoorsOpen()) {
								floorButtons_spc2_4 = 0;
							}
							else {

							}
						}
						else {

						}
					}
					else {

					}
				}
			}
		}
	}
	return;
}

__inline void __utac_acc__Specification2_spec__4(void)
{
	if (floorButtons_spc2_0) {
		__automaton_fail();
	}
	else {
		if (floorButtons_spc2_1) {
			__automaton_fail();
		}
		else {
			if (floorButtons_spc2_2) {
				__automaton_fail();
			}
			else {
				if (floorButtons_spc2_3) {
					__automaton_fail();
				}
				else {
					if (floorButtons_spc2_4) {
						__automaton_fail();
					}
					else {

					}
				}
			}
		}
	}
	return;
}

int expectedDirection = 0;

void __utac_acc__Specification3_spec__1(void)
{
	int currentFloorID___0;
	expectedDirection = 0;
	currentFloorID___0 = getCurrentFloorID();
	if (getCurrentHeading() == 1) {
		if (currentFloorID___0 < 0) {
			if (buttonForFloorIsPressed(0)) {
				expectedDirection = 1;
			}
			else {
				goto _L___2;
			}
		}
		else {
		_L___2: /* CIL Label */
			if (currentFloorID___0 < 1) {
				if (buttonForFloorIsPressed(1)) {
					expectedDirection = 1;
				}
				else {
					goto _L___1;
				}
			}
			else {
			_L___1: /* CIL Label */
				if (currentFloorID___0 < 2) {
					if (buttonForFloorIsPressed(2)) {
						expectedDirection = 1;
					}
					else {
						goto _L___0;
					}
				}
				else {
				_L___0: /* CIL Label */
					if (currentFloorID___0 < 3) {
						if (buttonForFloorIsPressed(3)) {
							expectedDirection = 1;
						}
						else {
							goto _L;
						}
					}
					else {
					_L: /* CIL Label */
						if (currentFloorID___0 < 4) {
							if (buttonForFloorIsPressed(4)) {
								expectedDirection = 1;
							}
							else {

							}
						}
						else {

						}
					}
				}
			}
		}
	}
	else {
		if (currentFloorID___0 > 0) {
			if (buttonForFloorIsPressed(0)) {
				expectedDirection = -1;
			}
			else {
				goto _L___6;
			}
		}
		else {
		_L___6: /* CIL Label */
			if (currentFloorID___0 > 1) {
				if (buttonForFloorIsPressed(1)) {
					expectedDirection = -1;
				}
				else {
					goto _L___5;
				}
			}
			else {
			_L___5: /* CIL Label */
				if (currentFloorID___0 > 2) {
					if (buttonForFloorIsPressed(2)) {
						expectedDirection = -1;
					}
					else {
						goto _L___4;
					}
				}
				else {
				_L___4: /* CIL Label */
					if (currentFloorID___0 > 3) {
						if (buttonForFloorIsPressed(3)) {
							expectedDirection = -1;
						}
						else {
							goto _L___3;
						}
					}
					else {
					_L___3: /* CIL Label */
						if (currentFloorID___0 > 4) {
							if (buttonForFloorIsPressed(4)) {
								expectedDirection = -1;
							}
							else {

							}
						}
						else {

						}
					}
				}
			}
		}
	}
	return;
}

void __utac_acc__Specification3_spec__2(void)
{
	if (expectedDirection == -1) {
		if (getCurrentHeading() == 1) {
			__automaton_fail();
		}
		else {
			goto _L;
		}
	}
	else {
	_L: /* CIL Label */
		if (expectedDirection == 1) {
			if (getCurrentHeading() == 0) {
				__automaton_fail();
			}
			else {

			}
		}
		else {

		}
	}
	return;
}

int floorButtons_spc9_0;
int floorButtons_spc9_1;
int floorButtons_spc9_2;
int floorButtons_spc9_3;
int floorButtons_spc9_4;

void __utac_acc__Specification9_spec__1(void)
{
	floorButtons_spc9_0 = 0;
	floorButtons_spc9_1 = 0;
	floorButtons_spc9_2 = 0;
	floorButtons_spc9_3 = 0;
	floorButtons_spc9_4 = 0;
	return;
}

__inline void __utac_acc__Specification9_spec__2(int floor)
{
		if (floor == 0) {
			floorButtons_spc9_0 = 1;
		}
		else {
			if (floor == 1) {
				floorButtons_spc9_1 = 1;
			}
			else {
				if (floor == 2) {
					floorButtons_spc9_2 = 1;
				}
				else {
					if (floor == 3) {
						floorButtons_spc9_3 = 1;
					}
					else {
						if (floor == 4) {
							floorButtons_spc9_4 = 1;
						}
						else {

						}
					}
				}
			}
		}
		return;
}

__inline void __utac_acc__Specification9_spec__3(void)
{
	int floor;
	floor = getCurrentFloorID();
	if (isEmpty()) {
		floorButtons_spc9_0 = 0;
		floorButtons_spc9_1 = 0;
		floorButtons_spc9_2 = 0;
		floorButtons_spc9_3 = 0;
		floorButtons_spc9_4 = 0;
	}
	else {
		if (areDoorsOpen()) {
			if (floor == 0) {
				if (floorButtons_spc9_0) {
					floorButtons_spc9_0 = 0;
				}
				else {
					goto _L___2;
				}
			}
			else {
			_L___2: /* CIL Label */
				if (floor == 1) {
					if (floorButtons_spc9_1) {
						floorButtons_spc9_1 = 0;
					}
					else {
						goto _L___1;
					}
				}
				else {
				_L___1: /* CIL Label */
					if (floor == 2) {
						if (floorButtons_spc9_2) {
							floorButtons_spc9_2 = 0;
						}
						else {
							goto _L___0;
						}
					}
					else {
					_L___0: /* CIL Label */
						if (floor == 3) {
							if (floorButtons_spc9_3) {
								floorButtons_spc9_3 = 0;
							}
							else {
								goto _L;
							}
						}
						else {
						_L: /* CIL Label */
							if (floor == 4) {
								if (floorButtons_spc9_4) {
									floorButtons_spc9_4 = 0;
								}
								else {

								}
							}
							else {

							}
						}
					}
				}
			}
		}
		else {

		}
	}
	return;
}

void __utac_acc__Specification9_spec__4(void)
{
	if (floorButtons_spc9_0) {
		__automaton_fail();
	}
	else {
		if (floorButtons_spc9_1) {
			__automaton_fail();
		}
		else {
			if (floorButtons_spc9_2) {
				__automaton_fail();
			}
			else {
				if (floorButtons_spc9_3) {
					__automaton_fail();
				}
				else {
					if (floorButtons_spc9_4) {
						__automaton_fail();
					}
					else {

					}
				}
			}
		}
	}
	return;
}

int prevDir_spec13 = 0;

__inline void __utac_acc__Specification13_spec__1(void)
{
	prevDir_spec13 = getCurrentHeading();
	return;
}

__inline void __utac_acc__Specification13_spec__2(void)
{
	int __cil_tmp7;
	int __cil_tmp8;
	__cil_tmp7 = maximumWeight * 2;
	__cil_tmp8 = __cil_tmp7 / 3;
	if (weight > __cil_tmp8) {
		if (prevDir_spec13 == 1) {
			if (existInLiftCallsInDirection(0)) {
				if (existInLiftCallsInDirection(1)) {

				}
				else {
					if (getCurrentHeading() == 1) {
						__automaton_fail();
					}
					else {

					}
				}
			}
			else {

			}
		}
		else {
			if (prevDir_spec13 == 0) {
				if (existInLiftCallsInDirection(1)) {
					if (existInLiftCallsInDirection(0)) {

					}
					else {
						if (getCurrentHeading() == 0) {
							__automaton_fail();
						}
						else {

						}
					}
				}
				else {

				}
			}
			else {

			}
		}
	}
	else {

	}
	return;
}

__inline void __utac_acc__Specification14_spec__1(void)
{
	if (isExecutiveFloorCalling()) {
		if (isExecutiveFloor(getCurrentFloorID())) {

		}
		else {
			if (areDoorsOpen()) {
				__automaton_fail();
			}
			else {

			}
		}
	}
	else {

	}
	return;
}

void select_features(void)
{
	__SELECTED_FEATURE_base = 1;
	__SELECTED_FEATURE_weight = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_empty = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_twothirdsfull = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_executivefloor = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_overloaded = __VERIFIER_nondet_int();
	return;
}

void select_helpers(void)
{
		__GUIDSL_ROOT_PRODUCTION = 1;
		return;
}

int valid_product(void)
{
	int retValue_acc;
	int tmp;
	if (!__SELECTED_FEATURE_overloaded) {
		goto _L___0;
	}
	else {
		if (__SELECTED_FEATURE_weight) {
		_L___0: /* CIL Label */
			if (!__SELECTED_FEATURE_twothirdsfull) {
				goto _L;
			}
			else {
				if (__SELECTED_FEATURE_weight) {
				_L: /* CIL Label */
					if (__SELECTED_FEATURE_base) {
						tmp = 1;
					}
					else {
						tmp = 0;
					}
				}
				else {
					tmp = 0;
				}
			}
		}
		else {
			tmp = 0;
		}
	}
	retValue_acc = tmp;
	if (__SELECTED_FEATURE_base) {
	feat_base:helper = helper + 1;
	}
	if (__SELECTED_FEATURE_weight) {
	feat_weight: helper = helper + 1;
	}
	if (__SELECTED_FEATURE_empty) {
	feat_empty: helper = helper + 1;
	}
	if (__SELECTED_FEATURE_twothirdsfull) {
	feat_twothirds: helper = helper + 1;
	}
	if (__SELECTED_FEATURE_executivefloor) {
	feat_executive: helper = helper + 1;
	}
	if (__SELECTED_FEATURE_overloaded) {
	feat_overload: helper = helper + 1;
	}
	return (retValue_acc);
}

void test(void)
{
	if (scenario == 1) {
		bigMacCall();
		angelinaCall();
		cleanup();
	}
	if (scenario == 2) {
		bigMacCall();
		cleanup();
	}
	if (scenario == 3) {
		initTopDown();
		bobCall();
		threeTS();
		bobCall();
		cleanup();
	}
	if (scenario == 4) {
		aliceCall();
		angelinaCall();
		threeTS();
		bobCall();
		cleanup();
	}
	if (scenario == 5) {
		bigMacCall(); //14
		threeTS();
		bobCall();
		cleanup();
	}
	return;
}

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

void initFloors(void)
{
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
	return;
}

int isFloorCalling(int floorID)
{
	int retValue_acc;
	if (floorID == 0) {
		retValue_acc = calls_0;
		return (retValue_acc);
	}
	else {
		if (floorID == 1) {
			retValue_acc = calls_1;
			return (retValue_acc);
		}
		else {
			if (floorID == 2) {
				retValue_acc = calls_2;
				return (retValue_acc);
			}
			else {
				if (floorID == 3) {
					retValue_acc = calls_3;
					return (retValue_acc);
				}
				else {
					if (floorID == 4) {
						retValue_acc = calls_4;
						return (retValue_acc);
					}
					else {

					}
				}
			}
		}
	}
	retValue_acc = 0;
	return (retValue_acc);
}

void resetCallOnFloor(int floorID)
{
	if (floorID == 0) {
		calls_0 = 0;
	}
	else {
		if (floorID == 1) {
			calls_1 = 0;
		}
		else {
			if (floorID == 2) {
				calls_2 = 0;
			}
			else {
				if (floorID == 3) {
					calls_3 = 0;
				}
				else {
					if (floorID == 4) {
						calls_4 = 0;
					}
					else {

					}
				}
			}
		}
	}
	return;
}

void callOnFloor(int floorID)
{
	if (scenario == 1) {
		__utac_acc__Specification1_spec__2(floorID);
	}
	if (floorID == 0) {
		calls_0 = 1;
	}
	else {
		if (floorID == 1) {
			calls_1 = 1;
		}
		else {
			if (floorID == 2) {
				calls_2 = 1;
			}
			else {
				if (floorID == 3) {
					calls_3 = 1;
				}
				else {
					if (floorID == 4) {
						calls_4 = 1;
					}
					else {

					}
				}
			}
		}
	}
	return;
}

int isPersonOnFloor(int person, int floor)
{
	int retValue_acc;
	if (floor == 0) {
		if (person == 0) {
			retValue_acc = personOnFloor_0_0;
			return (retValue_acc);
		}
		else {
			if (person == 1) {
				retValue_acc = personOnFloor_1_0;
				return (retValue_acc);
			}
			else {
				if (person == 2) {
					retValue_acc = personOnFloor_2_0;
					return (retValue_acc);
				}
				else {
					if (person == 3) {
						retValue_acc = personOnFloor_3_0;
						return (retValue_acc);
					}
					else {
						if (person == 4) {
							retValue_acc = personOnFloor_4_0;
							return (retValue_acc);
						}
						else {
							if (person == 5) {
								retValue_acc = personOnFloor_5_0;
								return (retValue_acc);
							}
							else {

							}
						}
					}
				}
			}
		}
	}
	else {
		if (floor == 1) {
			if (person == 0) {
				retValue_acc = personOnFloor_0_1;
				return (retValue_acc);
			}
			else {
				if (person == 1) {
					retValue_acc = personOnFloor_1_1;
					return (retValue_acc);
				}
				else {
					if (person == 2) {
						retValue_acc = personOnFloor_2_1;
						return (retValue_acc);
					}
					else {
						if (person == 3) {
							retValue_acc = personOnFloor_3_1;
							return (retValue_acc);
						}
						else {
							if (person == 4) {
								retValue_acc = personOnFloor_4_1;
								return (retValue_acc);
							}
							else {
								if (person == 5) {
									retValue_acc = personOnFloor_5_1;
									return (retValue_acc);
								}
								else {

								}
							}
						}
					}
				}
			}
		}
		else {
			if (floor == 2) {
				if (person == 0) {
					retValue_acc = personOnFloor_0_2;
					return (retValue_acc);
				}
				else {
					if (person == 1) {
						retValue_acc = personOnFloor_1_2;
						return (retValue_acc);
					}
					else {
						if (person == 2) {
							retValue_acc = personOnFloor_2_2;
							return (retValue_acc);
						}
						else {
							if (person == 3) {
								retValue_acc = personOnFloor_3_2;
								return (retValue_acc);
							}
							else {
								if (person == 4) {
									retValue_acc = personOnFloor_4_2;
									return (retValue_acc);
								}
								else {
									if (person == 5) {
										retValue_acc = personOnFloor_5_2;
										return (retValue_acc);
									}
									else {

									}
								}
							}
						}
					}
				}
			}
			else {
				if (floor == 3) {
					if (person == 0) {
						retValue_acc = personOnFloor_0_3;
						return (retValue_acc);
					}
					else {
						if (person == 1) {
							retValue_acc = personOnFloor_1_3;
							return (retValue_acc);
						}
						else {
							if (person == 2) {
								retValue_acc = personOnFloor_2_3;
								return (retValue_acc);
							}
							else {
								if (person == 3) {
									retValue_acc = personOnFloor_3_3;
									return (retValue_acc);
								}
								else {
									if (person == 4) {
										retValue_acc = personOnFloor_4_3;
										return (retValue_acc);
									}
									else {
										if (person == 5) {
											retValue_acc = personOnFloor_5_3;
											return (retValue_acc);
										}
										else {

										}
									}
								}
							}
						}
					}
				}
				else {
					if (floor == 4) {
						if (person == 0) {
							retValue_acc = personOnFloor_0_4;
							return (retValue_acc);
						}
						else {
							if (person == 1) {
								retValue_acc = personOnFloor_1_4;
								return (retValue_acc);
							}
							else {
								if (person == 2) {
									retValue_acc = personOnFloor_2_4;
									return (retValue_acc);
								}
								else {
									if (person == 3) {
										retValue_acc = personOnFloor_3_4;
										return (retValue_acc);
									}
									else {
										if (person == 4) {
											retValue_acc = personOnFloor_4_4;
											return (retValue_acc);
										}
										else {
											if (person == 5) {
												retValue_acc = personOnFloor_5_4;
												return (retValue_acc);
											}
											else {

											}
										}
									}
								}
							}
						}
					}
					else {

					}
				}
			}
		}
	}
	retValue_acc = 0;
	return (retValue_acc);
}

void initPersonOnFloor(int person, int floor)
{
	if (floor == 0) {
		if (person == 0) {
			personOnFloor_0_0 = 1;
		}
		else {
			if (person == 1) {
				personOnFloor_1_0 = 1;
			}
			else {
				if (person == 2) {
					personOnFloor_2_0 = 1;
				}
				else {
					if (person == 3) {
						personOnFloor_3_0 = 1;
					}
					else {
						if (person == 4) {
							personOnFloor_4_0 = 1;
						}
						else {
							if (person == 5) {
								personOnFloor_5_0 = 1;
							}
							else {

							}
						}
					}
				}
			}
		}
	}
	else {
		if (floor == 1) {
			if (person == 0) {
				personOnFloor_0_1 = 1;
			}
			else {
				if (person == 1) {
					personOnFloor_1_1 = 1;
				}
				else {
					if (person == 2) {
						personOnFloor_2_1 = 1;
					}
					else {
						if (person == 3) {
							personOnFloor_3_1 = 1;
						}
						else {
							if (person == 4) {
								personOnFloor_4_1 = 1;
							}
							else {
								if (person == 5) {
									personOnFloor_5_1 = 1;
								}
								else {

								}
							}
						}
					}
				}
			}
		}
		else {
			if (floor == 2) {
				if (person == 0) {
					personOnFloor_0_2 = 1;
				}
				else {
					if (person == 1) {
						personOnFloor_1_2 = 1;
					}
					else {
						if (person == 2) {
							personOnFloor_2_2 = 1;
						}
						else {
							if (person == 3) {
								personOnFloor_3_2 = 1;
							}
							else {
								if (person == 4) {
									personOnFloor_4_2 = 1;
								}
								else {
									if (person == 5) {
										personOnFloor_5_2 = 1;
									}
									else {

									}
								}
							}
						}
					}
				}
			}
			else {
				if (floor == 3) {
					if (person == 0) {
						personOnFloor_0_3 = 1;
					}
					else {
						if (person == 1) {
							personOnFloor_1_3 = 1;
						}
						else {
							if (person == 2) {
								personOnFloor_2_3 = 1;
							}
							else {
								if (person == 3) {
									personOnFloor_3_3 = 1;
								}
								else {
									if (person == 4) {
										personOnFloor_4_3 = 1;
									}
									else {
										if (person == 5) {
											personOnFloor_5_3 = 1;
										}
										else {

										}
									}
								}
							}
						}
					}
				}
				else {
					if (floor == 4) {
						if (person == 0) {
							personOnFloor_0_4 = 1;
						}
						else {
							if (person == 1) {
								personOnFloor_1_4 = 1;
							}
							else {
								if (person == 2) {
									personOnFloor_2_4 = 1;
								}
								else {
									if (person == 3) {
										personOnFloor_3_4 = 1;
									}
									else {
										if (person == 4) {
											personOnFloor_4_4 = 1;
										}
										else {
											if (person == 5) {
												personOnFloor_5_4 = 1;
											}
											else {

											}
										}
									}
								}
							}
						}
					}
					else {

					}
				}
			}
		}
	}
	callOnFloor(floor);
	return;
}

void removePersonFromFloor(int person, int floor)
{
	if (floor == 0) {
		if (person == 0) {
			personOnFloor_0_0 = 0;
		}
		else {
			if (person == 1) {
				personOnFloor_1_0 = 0;
			}
			else {
				if (person == 2) {
					personOnFloor_2_0 = 0;
				}
				else {
					if (person == 3) {
						personOnFloor_3_0 = 0;
					}
					else {
						if (person == 4) {
							personOnFloor_4_0 = 0;
						}
						else {
							if (person == 5) {
								personOnFloor_5_0 = 0;
							}
							else {

							}
						}
					}
				}
			}
		}
	}
	else {
		if (floor == 1) {
			if (person == 0) {
				personOnFloor_0_1 = 0;
			}
			else {
				if (person == 1) {
					personOnFloor_1_1 = 0;
				}
				else {
					if (person == 2) {
						personOnFloor_2_1 = 0;
					}
					else {
						if (person == 3) {
							personOnFloor_3_1 = 0;
						}
						else {
							if (person == 4) {
								personOnFloor_4_1 = 0;
							}
							else {
								if (person == 5) {
									personOnFloor_5_1 = 0;
								}
								else {

								}
							}
						}
					}
				}
			}
		}
		else {
			if (floor == 2) {
				if (person == 0) {
					personOnFloor_0_2 = 0;
				}
				else {
					if (person == 1) {
						personOnFloor_1_2 = 0;
					}
					else {
						if (person == 2) {
							personOnFloor_2_2 = 0;
						}
						else {
							if (person == 3) {
								personOnFloor_3_2 = 0;
							}
							else {
								if (person == 4) {
									personOnFloor_4_2 = 0;
								}
								else {
									if (person == 5) {
										personOnFloor_5_2 = 0;
									}
									else {

									}
								}
							}
						}
					}
				}
			}
			else {
				if (floor == 3) {
					if (person == 0) {
						personOnFloor_0_3 = 0;
					}
					else {
						if (person == 1) {
							personOnFloor_1_3 = 0;
						}
						else {
							if (person == 2) {
								personOnFloor_2_3 = 0;
							}
							else {
								if (person == 3) {
									personOnFloor_3_3 = 0;
								}
								else {
									if (person == 4) {
										personOnFloor_4_3 = 0;
									}
									else {
										if (person == 5) {
											personOnFloor_5_3 = 0;
										}
										else {

										}
									}
								}
							}
						}
					}
				}
				else {
					if (floor == 4) {
						if (person == 0) {
							personOnFloor_0_4 = 0;
						}
						else {
							if (person == 1) {
								personOnFloor_1_4 = 0;
							}
							else {
								if (person == 2) {
									personOnFloor_2_4 = 0;
								}
								else {
									if (person == 3) {
										personOnFloor_3_4 = 0;
									}
									else {
										if (person == 4) {
											personOnFloor_4_4 = 0;
										}
										else {
											if (person == 5) {
												personOnFloor_5_4 = 0;
											}
											else {

											}
										}
									}
								}
							}
						}
					}
					else {

					}
				}
			}
		}
	}
	resetCallOnFloor(floor);
	return;
}

int isTopFloor(int floorID)
{
	int retValue_acc;
	retValue_acc = floorID == 4;
	return (retValue_acc);
}

void initTopDown(void)
{
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
	return;
}

void initBottomUp(void)
{
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
	return;
}

int isBlocked__before__overloaded(void)
{
	int retValue_acc;
	retValue_acc = 0;
	return (retValue_acc);
}

int isBlocked__role__overloaded(void)
{
	int retValue_acc;
	retValue_acc = blocked;
	return (retValue_acc);
}

int isBlocked(void)
{
	int retValue_acc;
	if (__SELECTED_FEATURE_overloaded) {
		retValue_acc = isBlocked__role__overloaded();
		return (retValue_acc);
	}
	else {
		retValue_acc = isBlocked__before__overloaded();
		return (retValue_acc);
	}
}

void enterElevator__before__weight(int p)
{
	if (p == 0) {
		persons_0 = 1;
	}
	else {
		if (p == 1) {
			persons_1 = 1;
		}
		else {
			if (p == 2) {
				persons_2 = 1;
			}
			else {
				if (p == 3) {
					persons_3 = 1;
				}
				else {
					if (p == 4) {
						persons_4 = 1;
					}
					else {
						if (p == 5) {
							persons_5 = 1;
						}
						else {

						}
					}
				}
			}
		}
	}
	return;
}

void enterElevator__role__weight(int p)
{
	int tmp;
	enterElevator__before__weight(p);
	tmp = getWeight(p);
	weight = weight + tmp;
	return;
}

void enterElevator(int p)
{
	if (__SELECTED_FEATURE_weight) {
		enterElevator__role__weight(p);
		return;
	}
	else {
		enterElevator__before__weight(p);
		return;
	}
}

void leaveElevator__before__weight(int p)
{
	if (p == 0) {
		persons_0 = 0;
	}
	else {
		if (p == 1) {
			persons_1 = 0;
		}
		else {
			if (p == 2) {
				persons_2 = 0;
			}
			else {
				if (p == 3) {
					persons_3 = 0;
				}
				else {
					if (p == 4) {
						persons_4 = 0;
					}
					else {
						if (p == 5) {
							persons_5 = 0;
						}
						else {

						}
					}
				}
			}
		}
	}
	return;
}

void leaveElevator__role__weight(int p)
{
	int tmp;
	leaveElevator__before__weight(p);
	tmp = getWeight(p);
	weight = weight - tmp;
	return;
}

void leaveElevator__before__empty(int p)
{
	if (__SELECTED_FEATURE_weight) {
		leaveElevator__role__weight(p);
		return;
	}
	else {
		leaveElevator__before__weight(p);
		return;
	}
}

void leaveElevator__role__empty(int p)
{
	leaveElevator__before__empty(p);
	if (isEmpty()) {
		floorButtons_0 = 0;
		floorButtons_1 = 0;
		floorButtons_2 = 0;
		floorButtons_3 = 0;
		floorButtons_4 = 0;
	}
	else {

	}
	return;
}

void leaveElevator(int p)
{
	if (__SELECTED_FEATURE_empty) {
		leaveElevator__role__empty(p);
		return;
	}
	else {
		leaveElevator__before__empty(p);
		return;
	}
}

void pressInLiftFloorButton(int floorID)
{
	if (scenario == 2) {
		__utac_acc__Specification2_spec__2(floorID);
		__utac_acc__Specification9_spec__2(floorID);
	}
	if (floorID == 0) {
		floorButtons_0 = 1;
	}
	else {
		if (floorID == 1) {
			floorButtons_1 = 1;
		}
		else {
			if (floorID == 2) {
				floorButtons_2 = 1;
			}
			else {
				if (floorID == 3) {
					floorButtons_3 = 1;
				}
				else {
					if (floorID == 4) {
						floorButtons_4 = 1;
					}
					else {

					}
				}
			}
		}
	}
	return;
}

void resetFloorButton(int floorID)
{
		if (floorID == 0) {
			floorButtons_0 = 0;
		}
		else {
			if (floorID == 1) {
				floorButtons_1 = 0;
			}
			else {
				if (floorID == 2) {
					floorButtons_2 = 0;
				}
				else {
					if (floorID == 3) {
						floorButtons_3 = 0;
					}
					else {
						if (floorID == 4) {
							floorButtons_4 = 0;
						}
						else {

						}
					}
				}
			}
		}
		return;
}

int getCurrentFloorID(void)
{
	int retValue_acc;
	retValue_acc = currentFloorID;
	return (retValue_acc);
}

int areDoorsOpen(void)
{
	int retValue_acc;
	retValue_acc = doorState;
	return (retValue_acc);
}

int buttonForFloorIsPressed(int floorID)
{
	int retValue_acc;
	if (floorID == 0) {
		retValue_acc = floorButtons_0;
		return (retValue_acc);
	}
	else {
		if (floorID == 1) {
			retValue_acc = floorButtons_1;
			return (retValue_acc);
		}
		else {
			if (floorID == 2) {
				retValue_acc = floorButtons_2;
				return (retValue_acc);
			}
			else {
				if (floorID == 3) {
					retValue_acc = floorButtons_3;
					return (retValue_acc);
				}
				else {
					if (floorID == 4) {
						retValue_acc = floorButtons_4;
						return (retValue_acc);
					}
					else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			}
		}
	}
}

int getCurrentHeading(void)
{
	int retValue_acc;
	retValue_acc = currentHeading;
	return (retValue_acc);
}

int isEmpty(void)
{
	int retValue_acc;
	if (persons_0 == 1) {
		retValue_acc = 0;
		return (retValue_acc);
	}
	else {
		if (persons_1 == 1) {
			retValue_acc = 0;
			return (retValue_acc);
		}
		else {
			if (persons_2 == 1) {
				retValue_acc = 0;
				return (retValue_acc);
			}
			else {
				if (persons_3 == 1) {
					retValue_acc = 0;
					return (retValue_acc);
				}
				else {
					if (persons_4 == 1) {
						retValue_acc = 0;
						return (retValue_acc);
					}
					else {
						if (persons_5 == 1) {
							retValue_acc = 0;
							return (retValue_acc);
						}
						else {

						}
					}
				}
			}
		}
	}
	retValue_acc = 1;
	return (retValue_acc);
}

int anyStopRequested(void)
{
	int retValue_acc;
	if (isFloorCalling(0)) {
		retValue_acc = 1;
		return (retValue_acc);
	}
	else {
		if (floorButtons_0) {
			retValue_acc = 1;
			return (retValue_acc);
		}
		else {
			if (isFloorCalling(1)) {
				retValue_acc = 1;
				return (retValue_acc);
			}
			else {
				if (floorButtons_1) {
					retValue_acc = 1;
					return (retValue_acc);
				}
				else {
					if (isFloorCalling(2)) {
						retValue_acc = 1;
						return (retValue_acc);
					}
					else {
						if (floorButtons_2) {
							retValue_acc = 1;
							return (retValue_acc);
						}
						else {
							if (isFloorCalling(3)) {
								retValue_acc = 1;
								return (retValue_acc);
							}
							else {
								if (floorButtons_3) {
									retValue_acc = 1;
									return (retValue_acc);
								}
								else {
									if (isFloorCalling(4)) {
										retValue_acc = 1;
										return (retValue_acc);
									}
									else {
										if (floorButtons_4) {
											retValue_acc = 1;
											return (retValue_acc);
										}
										else {

										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	retValue_acc = 0;
	return (retValue_acc);
}

int isIdle(void)
{
	int retValue_acc;
	int tmp;
	tmp = anyStopRequested();
	retValue_acc = tmp == 0;
	return (retValue_acc);
}

int stopRequestedInDirection__before__twothirdsfull(int dir, int respectFloorCalls, int respectInLiftCalls)
{
	int retValue_acc;
	if (dir == 1) {
		if (isTopFloor(currentFloorID)) {
			retValue_acc = 0;
			return (retValue_acc);
		}
		else {

		}
		if (currentFloorID < 0) {
			if (respectFloorCalls) {
				if (isFloorCalling(0)) {
					retValue_acc = 1;
					return (retValue_acc);
				}
				else {
					goto _L___16;
				}
			}
			else {
				goto _L___16;
			}
		}
		else {
		_L___16: /* CIL Label */
			if (currentFloorID < 0) {
				if (respectInLiftCalls) {
					if (floorButtons_0) {
						retValue_acc = 1;
						return (retValue_acc);
					}
					else {
						goto _L___14;
					}
				}
				else {
					goto _L___14;
				}
			}
			else {
			_L___14: /* CIL Label */
				if (currentFloorID < 1) {
					if (respectFloorCalls) {
						if (isFloorCalling(1)) {
							retValue_acc = 1;
							return (retValue_acc);
						}
						else {
							goto _L___12;
						}
					}
					else {
						goto _L___12;
					}
				}
				else {
				_L___12: /* CIL Label */
					if (currentFloorID < 1) {
						if (respectInLiftCalls) {
							if (floorButtons_1) {
								retValue_acc = 1;
								return (retValue_acc);
							}
							else {
								goto _L___10;
							}
						}
						else {
							goto _L___10;
						}
					}
					else {
					_L___10: /* CIL Label */
						if (currentFloorID < 2) {
							if (respectFloorCalls) {
								if (isFloorCalling(2)) {
									retValue_acc = 1;
									return (retValue_acc);
								}
								else {
									goto _L___8;
								}
							}
							else {
								goto _L___8;
							}
						}
						else {
						_L___8: /* CIL Label */
							if (currentFloorID < 2) {
								if (respectInLiftCalls) {
									if (floorButtons_2) {
										retValue_acc = 1;
										return (retValue_acc);
									}
									else {
										goto _L___6;
									}
								}
								else {
									goto _L___6;
								}
							}
							else {
							_L___6: /* CIL Label */
								if (currentFloorID < 3) {
									if (respectFloorCalls) {
										if (isFloorCalling(3)) {
											retValue_acc = 1;
											return (retValue_acc);
										}
										else {
											goto _L___4;
										}
									}
									else {
										goto _L___4;
									}
								}
								else {
								_L___4: /* CIL Label */
									if (currentFloorID < 3) {
										if (respectInLiftCalls) {
											if (floorButtons_3) {
												retValue_acc = 1;
												return (retValue_acc);
											}
											else {
												goto _L___2;
											}
										}
										else {
											goto _L___2;
										}
									}
									else {
									_L___2: /* CIL Label */
										if (currentFloorID < 4) {
											if (respectFloorCalls) {
												if (isFloorCalling(4)) {
													retValue_acc = 1;
													return (retValue_acc);
												}
												else {
													goto _L___0;
												}
											}
											else {
												goto _L___0;
											}
										}
										else {
										_L___0: /* CIL Label */
											if (currentFloorID < 4) {
												if (respectInLiftCalls) {
													if (floorButtons_4) {
														retValue_acc = 1;
														return (retValue_acc);
													}
													else {
														retValue_acc = 0;
														return (retValue_acc);
													}
												}
												else {
													retValue_acc = 0;
													return (retValue_acc);
												}
											}
											else {
												retValue_acc = 0;
												return (retValue_acc);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	else {
		if (currentFloorID == 0) {
			retValue_acc = 0;
			return (retValue_acc);
		}
		else {

		}
		if (currentFloorID > 0) {
			if (respectFloorCalls) {
				if (isFloorCalling(0)) {
					retValue_acc = 1;
					return (retValue_acc);
				}
				else {
					goto _L___34;
				}
			}
			else {
				goto _L___34;
			}
		}
		else {
		_L___34: /* CIL Label */
			if (currentFloorID > 0) {
				if (respectInLiftCalls) {
					if (floorButtons_0) {
						retValue_acc = 1;
						return (retValue_acc);
					}
					else {
						goto _L___32;
					}
				}
				else {
					goto _L___32;
				}
			}
			else {
			_L___32: /* CIL Label */
				if (currentFloorID > 1) {
					if (respectFloorCalls) {
						if (isFloorCalling(1)) {
							retValue_acc = 1;
							return (retValue_acc);
						}
						else {
							goto _L___30;
						}
					}
					else {
						goto _L___30;
					}
				}
				else {
				_L___30: /* CIL Label */
					if (currentFloorID > 1) {
						if (respectInLiftCalls) {
							if (floorButtons_1) {
								retValue_acc = 1;
								return (retValue_acc);
							}
							else {
								goto _L___28;
							}
						}
						else {
							goto _L___28;
						}
					}
					else {
					_L___28: /* CIL Label */
						if (currentFloorID > 2) {
							if (respectFloorCalls) {
								if (isFloorCalling(2)) {
									retValue_acc = 1;
									return (retValue_acc);
								}
								else {
									goto _L___26;
								}
							}
							else {
								goto _L___26;
							}
						}
						else {
						_L___26: /* CIL Label */
							if (currentFloorID > 2) {
								if (respectInLiftCalls) {
									if (floorButtons_2) {
										retValue_acc = 1;
										return (retValue_acc);
									}
									else {
										goto _L___24;
									}
								}
								else {
									goto _L___24;
								}
							}
							else {
							_L___24: /* CIL Label */
								if (currentFloorID > 3) {
									if (respectFloorCalls) {
										if (isFloorCalling(3)) {
											retValue_acc = 1;
											return (retValue_acc);
										}
										else {
											goto _L___22;
										}
									}
									else {
										goto _L___22;
									}
								}
								else {
								_L___22: /* CIL Label */
									if (currentFloorID > 3) {
										if (respectInLiftCalls) {
											if (floorButtons_3) {
												retValue_acc = 1;
												return (retValue_acc);
											}
											else {
												goto _L___20;
											}
										}
										else {
											goto _L___20;
										}
									}
									else {
									_L___20: /* CIL Label */
										if (currentFloorID > 4) {
											if (respectFloorCalls) {
												if (isFloorCalling(4)) {
													retValue_acc = 1;
													return (retValue_acc);
												}
												else {
													goto _L___18;
												}
											}
											else {
												goto _L___18;
											}
										}
										else {
										_L___18: /* CIL Label */
											if (currentFloorID > 4) {
												if (respectInLiftCalls) {
													if (floorButtons_4) {
														retValue_acc = 1;
														return (retValue_acc);
													}
													else {
														retValue_acc = 0;
														return (retValue_acc);
													}
												}
												else {
													retValue_acc = 0;
													return (retValue_acc);
												}
											}
											else {
												retValue_acc = 0;
												return (retValue_acc);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	return (retValue_acc);
}

int stopRequestedInDirection__role__twothirdsfull(int dir, int respectFloorCalls, int respectInLiftCalls)
{
	int retValue_acc;
	int overload;
	int buttonPressed;
	int tmp;
	int __cil_tmp8;
	int __cil_tmp9;
	__cil_tmp8 = maximumWeight * 2;
	__cil_tmp9 = __cil_tmp8 / 3;
	overload = weight > __cil_tmp9;
	tmp = isAnyLiftButtonPressed();
	buttonPressed = tmp;
	if (overload) {
		if (buttonPressed) {
			retValue_acc = stopRequestedInDirection__before__twothirdsfull(dir, 0, respectInLiftCalls);
			return (retValue_acc);
		}
		else {
			retValue_acc = stopRequestedInDirection__before__twothirdsfull(dir, respectFloorCalls, respectInLiftCalls);
			return (retValue_acc);
		}
	}
	else {
		retValue_acc = stopRequestedInDirection__before__twothirdsfull(dir, respectFloorCalls, respectInLiftCalls);
		return (retValue_acc);
	}
}

int stopRequestedInDirection__before__executivefloor(int dir, int respectFloorCalls, int respectInLiftCalls)
{
	int retValue_acc;
	if (__SELECTED_FEATURE_twothirdsfull) {
		retValue_acc = stopRequestedInDirection__role__twothirdsfull(dir, respectFloorCalls, respectInLiftCalls);
		return (retValue_acc);
	}
	else {
		retValue_acc = stopRequestedInDirection__before__twothirdsfull(dir, respectFloorCalls, respectInLiftCalls);
		return (retValue_acc);
	}
}

int stopRequestedInDirection__role__executivefloor(int dir, int respectFloorCalls, int respectInLiftCalls)
{
	int retValue_acc;
	int tmp;
	int __cil_tmp7;
	int __cil_tmp8;
	if (isExecutiveFloorCalling()) {
		tmp = getCurrentFloorID();
		__cil_tmp7 = dir == 1;
		__cil_tmp8 = tmp < executiveFloor;
		retValue_acc = __cil_tmp8 == __cil_tmp7;
		return (retValue_acc);
	}
	else {
		retValue_acc = stopRequestedInDirection__before__executivefloor(dir, respectFloorCalls, respectInLiftCalls);
		return (retValue_acc);
	}
}

int stopRequestedInDirection(int dir, int respectFloorCalls, int respectInLiftCalls)
{
	int retValue_acc;
	if (__SELECTED_FEATURE_executivefloor) {
		retValue_acc = stopRequestedInDirection__role__executivefloor(dir, respectFloorCalls, respectInLiftCalls);
		return (retValue_acc);
	}
	else {
		retValue_acc = stopRequestedInDirection__before__executivefloor(dir, respectFloorCalls, respectInLiftCalls);
		return (retValue_acc);
	}
}

int isAnyLiftButtonPressed(void)
{
	int retValue_acc;
	if (floorButtons_0) {
		retValue_acc = 1;
		return (retValue_acc);
	}
	else {
		if (floorButtons_1) {
			retValue_acc = 1;
			return (retValue_acc);
		}
		else {
			if (floorButtons_2) {
				retValue_acc = 1;
				return (retValue_acc);
			}
			else {
				if (floorButtons_3) {
					retValue_acc = 1;
					return (retValue_acc);
				}
				else {
					if (floorButtons_4) {
						retValue_acc = 1;
						return (retValue_acc);
					}
					else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			}
		}
	}
	return (retValue_acc);
}

void continueInDirection(int dir)
{
	currentHeading = dir;
	if (currentHeading == 1) {
		if (isTopFloor(currentFloorID)) {
			currentHeading = 0;
		}
		else {

		}
	}
	else {
		if (currentFloorID == 0) {
			currentHeading = 1;
		}
		else {

		}
	}
	if (currentHeading == 1) {
		currentFloorID = currentFloorID + 1;
	}
	else {
		currentFloorID = currentFloorID - 1;
	}
	return;
}

int stopRequestedAtCurrentFloor__before__twothirdsfull(void)
{
	int retValue_acc;
	if (isFloorCalling(currentFloorID)) {
		retValue_acc = 1;
		return (retValue_acc);
	}
	else {
		if (buttonForFloorIsPressed(currentFloorID)) {
			retValue_acc = 1;
			return (retValue_acc);
		}
		else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

int stopRequestedAtCurrentFloor__role__twothirdsfull(void)
{
	int retValue_acc;
	int tmp;
	int tmp___0;
	int __cil_tmp4;
	int __cil_tmp5;
	__cil_tmp4 = maximumWeight * 2;
	__cil_tmp5 = __cil_tmp4 / 3;
	if (weight > __cil_tmp5) {
		tmp = getCurrentFloorID();
		tmp___0 = buttonForFloorIsPressed(tmp);
		retValue_acc = tmp___0 == 1;
		return (retValue_acc);
	}
	else {
		retValue_acc = stopRequestedAtCurrentFloor__before__twothirdsfull();
		return (retValue_acc);
	}
}

int stopRequestedAtCurrentFloor__before__executivefloor(void)
{
	int retValue_acc;
	if (__SELECTED_FEATURE_twothirdsfull) {
		retValue_acc = stopRequestedAtCurrentFloor__role__twothirdsfull();
		return (retValue_acc);
	}
	else {
		retValue_acc = stopRequestedAtCurrentFloor__before__twothirdsfull();
		return (retValue_acc);
	}
}

int stopRequestedAtCurrentFloor__role__executivefloor(void)
{
	int retValue_acc;
	if (isExecutiveFloorCalling()) {
		if (executiveFloor == getCurrentFloorID()) {
			retValue_acc = stopRequestedAtCurrentFloor__before__executivefloor();
			return (retValue_acc);
		}
		else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
	else {
		retValue_acc = stopRequestedAtCurrentFloor__before__executivefloor();
		return (retValue_acc);
	}
}

int stopRequestedAtCurrentFloor(void)
{
	int retValue_acc;
	if (__SELECTED_FEATURE_executivefloor) {
		retValue_acc = stopRequestedAtCurrentFloor__role__executivefloor();
		return (retValue_acc);
	}
	else {
		retValue_acc = stopRequestedAtCurrentFloor__before__executivefloor();
		return (retValue_acc);
	}
}

int getReverseHeading(int ofHeading)
{
	int retValue_acc;
	if (ofHeading == 0) {
		retValue_acc = 1;
		return (retValue_acc);
	}
	else {
		retValue_acc = 0;
		return (retValue_acc);
	}
	return (retValue_acc);
}

void processWaitingOnFloor(int floorID)
{
	if (isPersonOnFloor(0, floorID)) {
		removePersonFromFloor(0, floorID);
		pressInLiftFloorButton(getDestination(0));
		enterElevator(0);
	}
	else {

	}
	if (isPersonOnFloor(1, floorID)) {
		removePersonFromFloor(1, floorID);
		pressInLiftFloorButton(getDestination(1));
		enterElevator(1);
	}
	else {

	}
	if (isPersonOnFloor(2, floorID)) {
		removePersonFromFloor(2, floorID);
		pressInLiftFloorButton(getDestination(2));
		enterElevator(2);
	}
	else {

	}
	if (isPersonOnFloor(3, floorID)) {
		removePersonFromFloor(3, floorID);
		pressInLiftFloorButton(getDestination(3));
		enterElevator(3);
	}
	else {

	}
	if (isPersonOnFloor(4, floorID)) {
		removePersonFromFloor(4, floorID);
		pressInLiftFloorButton(getDestination(4));
		enterElevator(4);
	}
	else {

	}
	if (isPersonOnFloor(5, floorID)) {
		removePersonFromFloor(5, floorID);
		pressInLiftFloorButton(getDestination(5));
		enterElevator(5);
	}
	else {

	}
	resetCallOnFloor(floorID);
	return;
}

void timeShift__before__overloaded(void)
{
	if (stopRequestedAtCurrentFloor()) {
		doorState = 1;
		if (persons_0) {
			if (getDestination(0) == currentFloorID) {
				leaveElevator(0);
			}
			else {

			}
		}
		else {

		}
		if (persons_1) {
			if (getDestination(1) == currentFloorID) {
				leaveElevator(1);
			}
			else {

			}
		}
		else {

		}
		if (persons_2) {
			if (getDestination(2) == currentFloorID) {
				leaveElevator(2);
			}
			else {

			}
		}
		else {

		}
		if (persons_3) {
			if (getDestination(3) == currentFloorID) {
				leaveElevator(3);
			}
			else {

			}
		}
		else {

		}
		if (persons_4) {
			if (getDestination(4) == currentFloorID) {
				leaveElevator(4);
			}
			else {

			}
		}
		else {

		}
		if (persons_5) {
			if (getDestination(5) == currentFloorID) {
				leaveElevator(5);
			}
			else {

			}
		}
		else {

		}
		processWaitingOnFloor(currentFloorID);
		resetFloorButton(currentFloorID);
	}
	else {
		if (doorState == 1) {
			doorState = 0;
		}
		else {

		}
		if (stopRequestedInDirection(currentHeading, 1, 1)) {
			continueInDirection(currentHeading);
		}
		else {
			if (stopRequestedInDirection(getReverseHeading(currentHeading), 1, 1)) {
				continueInDirection(getReverseHeading(currentHeading));
			}
			else {
				continueInDirection(currentHeading);
			}
		}
	}
	return;
}

void timeShift__role__overloaded(void)
{
	if (areDoorsOpen()) {
		if (weight > maximumWeight) {
			blocked = 1;
		}
		else {
			blocked = 0;
			timeShift__before__overloaded();
		}
	}
	else {
		blocked = 0;
		timeShift__before__overloaded();
	}
	return;
}

void timeShift(void)
{
	if (scenario == 3) {
		__utac_acc__Specification3_spec__1();
	}
	if (scenario == 4) {
		__utac_acc__Specification13_spec__1();
	}
	if (__SELECTED_FEATURE_overloaded) {
		timeShift__role__overloaded();
		if (scenario == 1) {
			__utac_acc__Specification1_spec__3();
		}
		if (scenario == 2) {
			__utac_acc__Specification2_spec__3();
			__utac_acc__Specification9_spec__3();
		}
		if (scenario == 3) {
		__utac_acc__Specification3_1: __utac_acc__Specification3_spec__2();
		}
		if (scenario == 4) {
		__utac_acc__Specification13_1: __utac_acc__Specification13_spec__2();
		}
		if (scenario == 5) {
		__utac_acc__Specification14_1: __utac_acc__Specification14_spec__1();
		}
		return;
	}
	else {
		timeShift__before__overloaded();
		if (scenario == 1) {
			__utac_acc__Specification1_spec__3();
		}
		if (scenario == 2) {
			__utac_acc__Specification2_spec__3();
			__utac_acc__Specification9_spec__3();
		}
		if (scenario == 3) {
		__utac_acc__Specification3_2: __utac_acc__Specification3_spec__2();
		}
		if (scenario == 4) {
		__utac_acc__Specification13_2: __utac_acc__Specification13_spec__2();
		}
		if (scenario == 5) {
		__utac_acc__Specification14_2: __utac_acc__Specification14_spec__1();
		}
		return;
	}
}

void printState__before__overloaded(void)
{
	int tmp;
	int tmp___0;
	int tmp___1;
	int tmp___2;
	int tmp___3;
	char const   * __restrict  __cil_tmp6;
	char const   * __restrict  __cil_tmp7;
	char const   * __restrict  __cil_tmp8;
	char const   * __restrict  __cil_tmp9;
	char const   * __restrict  __cil_tmp10;
	char const   * __restrict  __cil_tmp11;
	char const   * __restrict  __cil_tmp12;
	char const   * __restrict  __cil_tmp13;
	char const   * __restrict  __cil_tmp14;
	char const   * __restrict  __cil_tmp15;
	char const   * __restrict  __cil_tmp16;
	char const   * __restrict  __cil_tmp17;
	char const   * __restrict  __cil_tmp18;
	char const   * __restrict  __cil_tmp19;
	char const   * __restrict  __cil_tmp20;
	char const   * __restrict  __cil_tmp21;
	char const   * __restrict  __cil_tmp22;
	char const   * __restrict  __cil_tmp23;
	char const   * __restrict  __cil_tmp24;
	char const   * __restrict  __cil_tmp25;
	char const   * __restrict  __cil_tmp26;
	__cil_tmp6 = (char const   * __restrict)"Elevator ";
	printf(__cil_tmp6);
	if (doorState) {
		__cil_tmp7 = (char const   * __restrict)"[_]";
		printf(__cil_tmp7);
	}
	else {
		__cil_tmp8 = (char const   * __restrict)"[] ";
		printf(__cil_tmp8);
	}
	__cil_tmp9 = (char const   * __restrict)" at ";
	printf(__cil_tmp9);
	__cil_tmp10 = (char const   * __restrict)"%i";
	printf(__cil_tmp10, currentFloorID);
	__cil_tmp11 = (char const   * __restrict)" heading ";
	printf(__cil_tmp11);
	if (currentHeading) {
		__cil_tmp12 = (char const   * __restrict)"up";
		printf(__cil_tmp12);
	}
	else {
		__cil_tmp13 = (char const   * __restrict)"down";
		printf(__cil_tmp13);
	}
	__cil_tmp14 = (char const   * __restrict)" IL_p:";
	printf(__cil_tmp14);
	if (floorButtons_0) {
		__cil_tmp15 = (char const   * __restrict)" %i";
		printf(__cil_tmp15, 0);
	}
	else {

	}
	if (floorButtons_1) {
		__cil_tmp16 = (char const   * __restrict)" %i";
		printf(__cil_tmp16, 1);
	}
	else {

	}
	if (floorButtons_2) {
		__cil_tmp17 = (char const   * __restrict)" %i";
		printf(__cil_tmp17, 2);
	}
	else {

	}
	if (floorButtons_3) {
		__cil_tmp18 = (char const   * __restrict)" %i";
		printf(__cil_tmp18, 3);
	}
	else {

	}
	if (floorButtons_4) {
		__cil_tmp19 = (char const   * __restrict)" %i";
		printf(__cil_tmp19, 4);
	}
	else {

	}
	__cil_tmp20 = (char const   * __restrict)" F_p:";
	printf(__cil_tmp20);
	tmp = isFloorCalling(0);
	if (tmp) {
		__cil_tmp21 = (char const   * __restrict)" %i";
		printf(__cil_tmp21, 0);
	}
	else {

	}
	tmp___0 = isFloorCalling(1);
	if (tmp___0) {
		__cil_tmp22 = (char const   * __restrict)" %i";
		printf(__cil_tmp22, 1);
	}
	else {

	}
	tmp___1 = isFloorCalling(2);
	if (tmp___1) {
		__cil_tmp23 = (char const   * __restrict)" %i";
		printf(__cil_tmp23, 2);
	}
	else {

	}
	tmp___2 = isFloorCalling(3);
	if (tmp___2) {
		__cil_tmp24 = (char const   * __restrict)" %i";
		printf(__cil_tmp24, 3);
	}
	else {

	}
	tmp___3 = isFloorCalling(4);
	if (tmp___3) {
		__cil_tmp25 = (char const   * __restrict)" %i";
		printf(__cil_tmp25, 4);
	}
	else {

	}
	__cil_tmp26 = (char const   * __restrict)"\n";
	printf(__cil_tmp26);
	return;
}

void printState__role__overloaded(void)
{
	int tmp;
	char const   * __restrict  __cil_tmp2;
	tmp = isBlocked();
	if (tmp) {
		__cil_tmp2 = (char const   * __restrict)"Blocked ";
		printf(__cil_tmp2);
	}
	else {

	}
	printState__before__overloaded();
	return;
}

void printState(void)
{
	if (__SELECTED_FEATURE_overloaded) {
		printState__role__overloaded();
		return;
	}
	else {
		printState__before__overloaded();
		return;
	}
}

int existInLiftCallsInDirection(int d)
{
	int retValue_acc;
	int i;
	int i___0;
	if (d == 1) {
		i = 0;
		i = currentFloorID + 1;
		while (1) {
		while_3_continue: /* CIL Label */;
			if (i < 5) {

			}
			else {
				goto while_3_break;
			}
			if (i == 0) {
				if (floorButtons_0) {
					retValue_acc = 1;
					return (retValue_acc);
				}
				else {
					goto _L___2;
				}
			}
			else {
			_L___2: /* CIL Label */
				if (i == 1) {
					if (floorButtons_1) {
						retValue_acc = 1;
						return (retValue_acc);
					}
					else {
						goto _L___1;
					}
				}
				else {
				_L___1: /* CIL Label */
					if (i == 2) {
						if (floorButtons_2) {
							retValue_acc = 1;
							return (retValue_acc);
						}
						else {
							goto _L___0;
						}
					}
					else {
					_L___0: /* CIL Label */
						if (i == 3) {
							if (floorButtons_3) {
								retValue_acc = 1;
								return (retValue_acc);
							}
							else {
								goto _L;
							}
						}
						else {
						_L: /* CIL Label */
							if (i == 4) {
								if (floorButtons_4) {
									retValue_acc = 1;
									return (retValue_acc);
								}
								else {

								}
							}
							else {

							}
						}
					}
				}
			}
			i = i + 1;
		}
	while_3_break: /* CIL Label */;
	}
	else {
		if (d == 0) {
			i___0 = 0;
			i___0 = currentFloorID - 1;
			while (1) {
			while_4_continue: /* CIL Label */;
				if (i___0 >= 0) {

				}
				else {
					goto while_4_break;
				}
				i___0 = currentFloorID + 1;
				{
					while (1) {
					while_5_continue: /* CIL Label */;
						if (i___0 < 5) {

						}
						else {
							goto while_5_break;
						}
						if (i___0 == 0) {
							if (floorButtons_0) {
								retValue_acc = 1;
								return (retValue_acc);
							}
							else {
								goto _L___6;
							}
						}
						else {
						_L___6: /* CIL Label */
							if (i___0 == 1) {
								if (floorButtons_1) {
									retValue_acc = 1;
									return (retValue_acc);
								}
								else {
									goto _L___5;
								}
							}
							else {
							_L___5: /* CIL Label */
								if (i___0 == 2) {
									if (floorButtons_2) {
										retValue_acc = 1;
										return (retValue_acc);
									}
									else {
										goto _L___4;
									}
								}
								else {
								_L___4: /* CIL Label */
									if (i___0 == 3) {
										if (floorButtons_3) {
											retValue_acc = 1;
											return (retValue_acc);
										}
										else {
											goto _L___3;
										}
									}
									else {
									_L___3: /* CIL Label */
										if (i___0 == 4) {
											if (floorButtons_4) {
												retValue_acc = 1;
												return (retValue_acc);
											}
											else {

											}
										}
										else {

										}
									}
								}
							}
						}
						i___0 = i___0 + 1;
					}
				while_5_break: /* CIL Label */;
				}
				i___0 = i___0 - 1;
			}
		while_4_break: /* CIL Label */;
		}
		else {

		}
	}
	retValue_acc = 0;
	return (retValue_acc);
}

int isExecutiveFloorCalling(void)
{
	int retValue_acc;
	retValue_acc = isFloorCalling(executiveFloor);
	return (retValue_acc);
}

int isExecutiveFloor(int floorID)
{
	int retValue_acc;
	retValue_acc = executiveFloor == floorID;
	return (retValue_acc);
}

void spec1(void)
{
	int tmp;
	int tmp___0;
	int i;
	int tmp___1;

	initBottomUp();
	tmp = getOrigin(5);
	initPersonOnFloor(5, tmp);
	printState();
	tmp___0 = getOrigin(2);
	initPersonOnFloor(2, tmp___0);
	printState();
	i = 0;
	while (1) {
	while_6_continue: /* CIL Label */;
		if (i < cleanupTimeShifts) {
			tmp___1 = isBlocked();
			if (tmp___1 != 1) {

			}
			else {
				goto while_6_break;
			}
		}
		else {
			goto while_6_break;
		}
		timeShift();
		printState();
		i = i + 1;
	}
while_6_break: /* CIL Label */;
	return;
}

void spec14(void)
{
	int tmp;
	int tmp___0;
	int i;
	int tmp___1;
	initTopDown();
	tmp = getOrigin(5);
	initPersonOnFloor(5, tmp);
	printState();
	timeShift();
	timeShift();
	timeShift();
	timeShift();
	tmp___0 = getOrigin(0);
	initPersonOnFloor(0, tmp___0);
	printState();
	i = 0;
	while (1) {
	while_7_continue: /* CIL Label */;
		if (i < cleanupTimeShifts) {
			tmp___1 = isBlocked();
			if (tmp___1 != 1) {

			}
			else {
				goto while_7_break;
			}
		}
		else {
			goto while_7_break;
		}
		timeShift();
		printState();
		i = i + 1;
	}
while_7_break: /* CIL Label */;
	return;
}

//extern  __attribute__((__nothrow__, __noreturn__)) void exit(int __status);

void bobCall(void)
{
	initPersonOnFloor(0, getOrigin(0));
	return;
}

void aliceCall(void)
{
	initPersonOnFloor(1, getOrigin(1));
	return;
}

void angelinaCall(void)
{
	initPersonOnFloor(2, getOrigin(2));
	return;
}

void chuckCall(void)
{
	initPersonOnFloor(3, getOrigin(3));
	return;
}

void monicaCall(void)
{
	initPersonOnFloor(4, getOrigin(4));
	return;
}

void bigMacCall(void)
{
	initPersonOnFloor(5, getOrigin(5));
	return;
}

void threeTS(void)
{
	timeShift();
	timeShift();
	timeShift();
	return;
}

void cleanup(void)
{
	int i;
	int __cil_tmp4;
	timeShift();
	i = 0;
	while (1) {
	while_8_continue: /* CIL Label */;
		__cil_tmp4 = cleanupTimeShifts - 1;
		if (i < __cil_tmp4) {
			if (isBlocked() != 1) {

			}
			else {
				goto while_8_break;
			}
		}
		else {
			goto while_8_break;
		}
		if (isIdle()) {
			return;
		}
		else {
			timeShift();
		}
		i = i + 1;
	}
while_8_break: /* CIL Label */;
	return;
}

void randomSequenceOfActions(void)
{
	int maxLength;
	int tmp;
	int counter;
	int action;
	int tmp___0;
	int origin;
	int tmp___1;
	int tmp___2;
	maxLength = 4;
	tmp = __VERIFIER_nondet_int();
	if (tmp) {
		initTopDown();
	}
	else {
		initBottomUp();
	}
	counter = 0;
	{
		while (1) {
		while_9_continue: /* CIL Label */;
			if (counter < maxLength) {

			}
			else {
				goto while_9_break;
			}
			counter = counter + 1;
			tmp___0 = __VERIFIER_nondet_int();
			action = tmp___0;
			if (action < 6) {
				tmp___1 = getOrigin(action);
				origin = tmp___1;
				initPersonOnFloor(action, origin);
			}
			else {
				if (action == 6) {
					timeShift();
				}
				else {
					if (action == 7) {
						timeShift();
						timeShift();
						timeShift();
					}
					else {

					}
				}
			}
			tmp___2 = isBlocked();
			if (tmp___2) {
				return;
			}
			else {

			}
		}
	while_9_break: /* CIL Label */;
	}
	cleanup();
	return;
}

void runTest_Simple(void)
{
	bigMacCall();
	angelinaCall();
	cleanup();
	return;
}

void Specification1(void)
{
	bigMacCall();
	angelinaCall();
	cleanup();
	return;
}

void Specification2(void)
{
	bigMacCall();
	cleanup();
	return;
}

void Specification3(void)
{
	bobCall();
	timeShift();
	timeShift();
	timeShift();
	timeShift();
	timeShift();
	bobCall();
	cleanup();
	return;
}

void setup(void)
{
		return;
}

void runTest(void)
{
	if (scenario == 1) {
		__utac_acc__Specification1_spec__1();
	}
	if (scenario == 2) {
		__utac_acc__Specification2_spec__1();
		__utac_acc__Specification9_spec__1();
	}	
	test();
	if (scenario == 1) {
	__utac_acc__Specification1: __utac_acc__Specification1_spec__4();
	}
	if (scenario == 2) {
	__utac_acc__Specification2: __utac_acc__Specification2_spec__4();
	__utac_acc__Specification9: __utac_acc__Specification9_spec__4();
	}
	return;
}

//By Hauke
void select_scenario(void) {
	scenario = __VERIFIER_nondet_int();
}

//By Hauke
int valid_scenario(void) {
	if (scenario == 1){
		return 1;
	}
	else if (scenario == 2){
		return 1;
	}
	else if (scenario == 3){
		return 1;
	}
	else if (scenario == 4){
		if (__SELECTED_FEATURE_twothirdsfull) {
			return 1;
		} 
		else {
			return 0;
		}
	}
	else if (scenario == 5){
		if (__SELECTED_FEATURE_executivefloor){
			return 1;
		}
		else {
			return 0;
		}
	}
	else {
		return 0;
	}
}

int main(void)
{
	int retValue_acc;
	select_helpers();
	select_features();
	select_scenario();
	if (valid_product()) {
		if (valid_scenario()) {
			setup();
			runTest();
		}
	}
	else {

	}
	retValue_acc = 0;
	return (retValue_acc);
}

int getWeight(int person)
{
	int retValue_acc;

	if (person == 0) {
		retValue_acc = 40;
		return (retValue_acc);
	}
	else {
		if (person == 1) {
			retValue_acc = 40;
			return (retValue_acc);
		}
		else {
			if (person == 2) {
				retValue_acc = 40;
				return (retValue_acc);
			}
			else {
				if (person == 3) {
					retValue_acc = 40;
					return (retValue_acc);
				}
				else {
					if (person == 4) {
						retValue_acc = 30;
						return (retValue_acc);
					}
					else {
						if (person == 5) {
							retValue_acc = 150;
							return (retValue_acc);
						}
						else {
							retValue_acc = 0;
							return (retValue_acc);
						}
					}
				}
			}
		}
	}
}

int getOrigin(int person)
{
	int retValue_acc;
	if (person == 0) {
		retValue_acc = 4;
		return (retValue_acc);
	}
	else {
		if (person == 1) {
			retValue_acc = 3;
			return (retValue_acc);
		}
		else {
			if (person == 2) {
				retValue_acc = 2;
				return (retValue_acc);
			}
			else {
				if (person == 3) {
					retValue_acc = 1;
					return (retValue_acc);
				}
				else {
					if (person == 4) {
						retValue_acc = 0;
						return (retValue_acc);
					}
					else {
						if (person == 5) {
							retValue_acc = 1;
							return (retValue_acc);
						}
						else {
							retValue_acc = 0;
							return (retValue_acc);
						}
					}
				}
			}
		}
	}
}

int getDestination(int person)
{
	int retValue_acc;
	if (person == 0) {
		retValue_acc = 0;
		return (retValue_acc);
	}
	else {
		if (person == 1) {
			retValue_acc = 0;
			return (retValue_acc);
		}
		else {
			if (person == 2) {
				retValue_acc = 1;
				return (retValue_acc);
			}
			else {
				if (person == 3) {
					retValue_acc = 3;
					return (retValue_acc);
				}
				else {
					if (person == 4) {
						retValue_acc = 1;
						return (retValue_acc);
					}
					else {
						if (person == 5) {
							retValue_acc = 3;
							return (retValue_acc);
						}
						else {
							retValue_acc = 0;
							return (retValue_acc);
						}
					}
				}
			}
		}
	}
}