extern int __VERIFIER_nondet_int();
extern int input();
enum { reset, fail, approach, exit, lower, msg1, msg2, raise, retry, down,
	blocked, up, R, S, E, F, G, H, P, A, Q, B, C, D
};
int ControllerCurrentLocation = E;
int GateCurrentLocation = A;
int ControllerCurrentAction = approach;
int GateCurrentAction;
int __Clock_y = 0;
int __Clock_x = 0;
int b;
int c;
int a;
int __SELECTED_FEATURE_Root;
int __SELECTED_FEATURE_Type;
int __SELECTED_FEATURE_Fast;
int __SELECTED_FEATURE_Medium;
int __SELECTED_FEATURE_Slow;
int __SELECTED_FEATURE_Error;

int validate()
{
	if ((__SELECTED_FEATURE_Root)
	    && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Error)
	    && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Error)
	    && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Type)
	    && (!__SELECTED_FEATURE_Root || __SELECTED_FEATURE_Type)
	    && (__SELECTED_FEATURE_Type || !__SELECTED_FEATURE_Fast)
	    && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Fast)
	    && (__SELECTED_FEATURE_Type || !__SELECTED_FEATURE_Medium)
	    && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Medium)
	    && (__SELECTED_FEATURE_Type || !__SELECTED_FEATURE_Slow)
	    && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Slow)) {
		return 1;
	}
	return 0;
}

void invoke(int action)
{
	if (action == reset) {
		ControllerCurrentAction = reset;
	}
	if (action == approach) {
		ControllerCurrentAction = approach;
	}
	if (action == exit) {
		ControllerCurrentAction = exit;
	}
	if (action == lower) {
		GateCurrentAction = lower;
	}
	if (action == blocked) {
		GateCurrentAction = blocked;
	}
	if (action == raise) {
		GateCurrentAction = raise;
	}
}

void step()
{
	int stepsize = __VERIFIER_nondet_int();
	__Clock_y += stepsize;
	__Clock_x += stepsize;
}

void methodController()
{
	switch (ControllerCurrentLocation) {
	case R:

		if (ControllerCurrentAction == reset) {
			if (__Clock_y <= 1) {
 G1:				ControllerCurrentLocation = F;
				__Clock_y = 0;
			}
		}
		step();
		break;
	case S:
 G2:		ControllerCurrentLocation = R;
		invoke(fail);
		break;
	case E:
		if (ControllerCurrentAction == approach) {
			if (__Clock_y <= 1) {
 G3:				ControllerCurrentLocation = F;
				__Clock_y = 0;
			}
		}
		step();
		break;
	case F:
		if (__Clock_y <= 1) {
			step();
			if (ControllerCurrentAction == exit) {
 G4:				ControllerCurrentLocation = R;
			}
			if (__Clock_y > 0) {
				if (__Clock_y <= 90) {
 G5:					ControllerCurrentLocation = G;
					invoke(lower);
				}
			}
		}
		step();
		break;
	case G:
		if (__Clock_y <= 90) {
			step();
			if (__SELECTED_FEATURE_Error && __SELECTED_FEATURE_Fast) {
				if (__Clock_y >= b) {
 G6:					ControllerCurrentLocation = S;
					invoke(msg1);
				}
			}
			if (__SELECTED_FEATURE_Error
			    && !__SELECTED_FEATURE_Slow) {
				if (__Clock_y >= c && __Clock_y >= b) {
 G7:					ControllerCurrentLocation = S;
					invoke(msg2);
				}
			}
			if (__SELECTED_FEATURE_Error
			    && !__SELECTED_FEATURE_Fast) {
				if (__Clock_y < b) {
 G8:					ControllerCurrentLocation = S;
					invoke(msg1);
				}
			}
			if (ControllerCurrentAction == exit) {
 G9:				ControllerCurrentLocation = H;
				__Clock_y = 0;
			}
		}
		step();
		break;
	case H:
		if (__Clock_y <= 1) {
 G10:			ControllerCurrentLocation = E;
			invoke(raise);
		}
		step();
		break;
	}
}

void methodGate()
{
	switch (GateCurrentLocation) {
	case P:
		if (__Clock_x >= 35) {
 G11:			GateCurrentLocation = Q;
			invoke(retry);
		}
		step();
		break;
	case A:
		if (GateCurrentAction == lower) {
			if (__Clock_x <= 30) {
 G12:				GateCurrentLocation = B;
				__Clock_x = 0;
			}
		}
		step();
		break;
	case Q:
 G13:		GateCurrentLocation = C;
		invoke(down);
		break;
	case B:
		if (__Clock_x <= 30) {
			step();
			if (__Clock_x >= 30) {
				if (GateCurrentAction == blocked) {
 G14:					GateCurrentLocation = P;
				}
			}
			if (__Clock_x >= a) {
 G15:				GateCurrentLocation = C;
				__Clock_x = 0;
				invoke(down);
			}
		}
		step();
		break;
	case C:
		if (__Clock_x >= 30) {
			if (GateCurrentAction == raise) {
				if (__Clock_x <= 40) {
 G16:					GateCurrentLocation = D;
					__Clock_x = 0;
				}
			}
		}
		step();
		break;
	case D:
		if (__Clock_x <= 40) {
			step();
			if (__Clock_x >= a) {
 G17:				GateCurrentLocation = A;
				invoke(up);
			}
		}
		step();
		break;
	}
}

int main()
{
	__SELECTED_FEATURE_Root = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Type = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Fast = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Medium = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Slow = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Error = __VERIFIER_nondet_int();
	b = input();
	c = input();
	a = input();

	if (validate()) {
		if (__SELECTED_FEATURE_Fast) {
			a = 10;
		}
		if (__SELECTED_FEATURE_Medium) {
			a = 15;
		}
		if (__SELECTED_FEATURE_Slow) {
			a = 20;
		}
	}
	while (1) {
		methodController();
		methodGate();

	}
}
