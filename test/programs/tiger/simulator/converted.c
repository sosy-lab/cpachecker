extern int __VERIFIER_nondet_int();
extern int input();
enum{ reset, fail, approach, exit, lower, msg1, msg2, raise, retry, down, blocked, up, R, S, E, F, G, H, P, A, Q, B, C, D, R, S, E, F, G, H, P, A, Q, B, C, D};
	int ControllerCurrentLocation=E;
	int GateCurrentLocation=A;
	int ControllerCurrentAction;
	int GateCurrentAction;
	int __Clock_y = 0;
	int __Clock_x = 0;
	int b;
	int c;
	int a;
	int ControllerIsBlocked=0;
	int GateIsBlocked=0;
	int execute=-22;		 //spezieller Wert der sonst nie erreicht werden kann
	int __SELECTED_FEATURE_Root;
	int __SELECTED_FEATURE_Type;
	int __SELECTED_FEATURE_Fast;
	int __SELECTED_FEATURE_Medium;
	int __SELECTED_FEATURE_Slow;
	int __SELECTED_FEATURE_Error;

int validate(){
	if((__SELECTED_FEATURE_Root) && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Error) && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Error) && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Type) && (!__SELECTED_FEATURE_Root || __SELECTED_FEATURE_Type) && (__SELECTED_FEATURE_Type || !__SELECTED_FEATURE_Fast) && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Fast) && (__SELECTED_FEATURE_Type || !__SELECTED_FEATURE_Medium) && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Medium) && (__SELECTED_FEATURE_Type || !__SELECTED_FEATURE_Slow) && (__SELECTED_FEATURE_Root || !__SELECTED_FEATURE_Slow)){
		return 1;
	}
	return 0;
}
void invoke(int action){
	if(action==lower){
		if(ControllerCurrentAction != lower||GateCurrentAction != lower) {
			return;
		}
		ControllerIsBlocked=0;
		GateIsBlocked=0;
return;
	}
	if(action==raise){
		if(ControllerCurrentAction != raise||GateCurrentAction != raise) {
			return;
		}
		ControllerIsBlocked=0;
		GateIsBlocked=0;
return;
	}
}
void step(){
	int stepsize =__VERIFIER_nondet_int();
	__Clock_y+= stepsize;
	__Clock_x+= stepsize;
}

void methodController( int invokeAction){	// -1 if no Action is invoked 
 	if(ControllerIsBlocked){
		return;
	}
	switch (ControllerCurrentLocation ){
		case  R :		//Casetest for Location
		if(__Clock_y<=1){		//Invariant of next Location
			step();
			__Clock_y=0;
			ControllerCurrentAction=reset;
			G1:	ControllerCurrentLocation= F;
		}
		step();
		break;
		case  S :		//Casetest for Location
			ControllerCurrentAction=fail;
			G2:	ControllerCurrentLocation= R;
		break;
		case  E :		//Casetest for Location
		if(__Clock_y<=1){		//Invariant of next Location
			step();
			__Clock_y=0;
			ControllerCurrentAction=approach;
			G3:	ControllerCurrentLocation= F;
		}
		step();
		break;
		case  F :		//Casetest for Location
		if(__Clock_y<=1){				//own invariant
step();
			ControllerCurrentAction=exit;
			G4:	ControllerCurrentLocation= R;
		if(__Clock_y>0){		// Parameter Constraint
		step();
		if(__Clock_y<=90){		//Invariant of next Location
			step();
			if(invokeAction==-1 && ControllerCurrentAction==lower){		// is this invoked and has to be executed
			G5:	ControllerCurrentLocation= G;
			}
			else{		//has to be invoked
				ControllerCurrentAction = lower;
				ControllerIsBlocked=1;
				invoke(lower);
				return;
			}
		}
		}
		}
		step();
		break;
		case  G :		//Casetest for Location
		if(__Clock_y<=90){				//own invariant
step();
		if(__SELECTED_FEATURE_Error&&__SELECTED_FEATURE_Fast){		//Feature Constraint
		step();
		if(__Clock_y>=b){		// Parameter Constraint
		step();
			ControllerCurrentAction=msg1;
			G6:	ControllerCurrentLocation= S;
		}
		}
		if(__SELECTED_FEATURE_Error&&!__SELECTED_FEATURE_Slow){		//Feature Constraint
		step();
		if(__Clock_y>=c&&__Clock_y>=b){		// Parameter Constraint
		step();
			ControllerCurrentAction=msg2;
			G7:	ControllerCurrentLocation= S;
		}
		}
		if(__SELECTED_FEATURE_Error&&!__SELECTED_FEATURE_Fast){		//Feature Constraint
		step();
		if(__Clock_y<b){		// Parameter Constraint
		step();
			ControllerCurrentAction=msg1;
			G8:	ControllerCurrentLocation= S;
		}
		}
			__Clock_y=0;
			ControllerCurrentAction=exit;
			G9:	ControllerCurrentLocation= H;
		}
		step();
		break;
		case  H :		//Casetest for Location
		if(__Clock_y<=1){		// Parameter Constraint
		step();
			if(invokeAction==-1 && ControllerCurrentAction==raise){		// is this invoked and has to be executed
			G10:	ControllerCurrentLocation= E;
			}
			else{		//has to be invoked
				ControllerCurrentAction = raise;
				ControllerIsBlocked=1;
				invoke(raise);
				return;
			}
		}
		step();
		break;
		}
	}

void methodGate( int invokeAction){	// -1 if no Action is invoked 
 	if(GateIsBlocked){
		return;
	}
	switch (GateCurrentLocation ){
		case  P :		//Casetest for Location
		if(__Clock_x>=35){		// Parameter Constraint
		step();
			GateCurrentAction=retry;
			G11:	GateCurrentLocation= Q;
		}
		step();
		break;
		case  A :		//Casetest for Location
		if(__Clock_x<=30){		//Invariant of next Location
			step();
			__Clock_x=0;
			if(invokeAction==-1 && GateCurrentAction==lower){		// is this invoked and has to be executed
			G12:	GateCurrentLocation= B;
			}
			else{		//has to be invoked
				GateCurrentAction = lower;
				GateIsBlocked=1;
				invoke(lower);
				return;
			}
		}
		step();
		break;
		case  Q :		//Casetest for Location
			GateCurrentAction=down;
			G13:	GateCurrentLocation= C;
		break;
		case  B :		//Casetest for Location
		if(__Clock_x<=30){				//own invariant
step();
		if(__Clock_x>=30){		// Parameter Constraint
		step();
			GateCurrentAction=blocked;
			G14:	GateCurrentLocation= P;
		}
		if(__Clock_x>=a){		// Parameter Constraint
		step();
			__Clock_x=0;
			GateCurrentAction=down;
			G15:	GateCurrentLocation= C;
		}
		}
		step();
		break;
		case  C :		//Casetest for Location
		if(__Clock_x>=30){		// Parameter Constraint
		step();
		if(__Clock_x<=40){		//Invariant of next Location
			step();
			__Clock_x=0;
			if(invokeAction==-1 && GateCurrentAction==raise){		// is this invoked and has to be executed
			G16:	GateCurrentLocation= D;
			}
			else{		//has to be invoked
				GateCurrentAction = raise;
				GateIsBlocked=1;
				invoke(raise);
				return;
			}
		}
		}
		step();
		break;
		case  D :		//Casetest for Location
		if(__Clock_x<=40){				//own invariant
step();
		if(__Clock_x>=a){		// Parameter Constraint
		step();
			GateCurrentAction=up;
			G17:	GateCurrentLocation= A;
		}
		}
		step();
		break;
		}
	}
int main() {
	__SELECTED_FEATURE_Root= __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Type= __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Fast= __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Medium= __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Slow= __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Error= __VERIFIER_nondet_int();
	b = input();
	c = input();
	a = input();


if (validate()) {
		if (__SELECTED_FEATURE_Fast){
			a=10;
		}
		if (__SELECTED_FEATURE_Medium){
			a=15;
		}
		if (__SELECTED_FEATURE_Slow){
			a=20;
		}
		while(1){
			methodController(-1);
			methodGate(-1);
		}

	}
}
