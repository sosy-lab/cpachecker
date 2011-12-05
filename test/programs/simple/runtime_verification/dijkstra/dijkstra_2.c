#define bool int
#define true 1
#define false 0

extern bool crit1;
extern bool crit2;
int turn = 1;
int flag1 = 0;
int flag2 = 0;

int program_counter_1 = 1;
int program_counter_2 = 1;

void thread1() {
	switch(program_counter_1) {
		case 1: goto L_1_1;
		case 2: goto L_1_2;
		case 3: goto L_1_3;
		case 4: goto L_1_4;
		case 5: goto L_1_5;
		case 6: goto L_1_6;
		case 7: goto L_1_7;
		case 8: goto L_1_8;
		case 9: goto L_1_9;
		case 10: goto L_1_10;
		case 11: goto L_1_11;
		case 12: goto L_1_12;
		case 13: goto L_1_13;
		default: return; 
	}

L_1_1:	flag1 = 1;
	program_counter_1 = 2;
	return;

L_1_2:	while(!(turn == 1)) {
		program_counter_1 = 3;
		return;

L_1_3:		if(turn == 1) {
			program_counter_1 = 4;
			return;

L_1_4:			if(flag1 == 0) {
				program_counter_1 = 5;
				return;

L_1_5:				turn = 1;
				program_counter_1 = 2;
				return;

			}
			program_counter_1 = 2;
			return;

		}
		program_counter_1 = 6;
		return;

L_1_6:		/*else*/ if(turn == 2) {
			program_counter_1 = 7;
			return;

L_1_7:			if(flag2 == 0) {
				program_counter_1 = 8;
				return;

L_1_8:				turn = 1;
				program_counter_1 = 2;
				return;

			}
			program_counter_1 = 2;
			return;

		}
		program_counter_1 = 2;
		return;

	}
	program_counter_1 = 9;
	return;

L_1_9:	flag1 = 2;
	program_counter_1 = 10;
	return;


L_1_10:	if(flag2 == 2) {
		program_counter_1 = 1;
		return;

	}
	program_counter_1 = 11;
	return;


L_1_11:	crit1 = true;
	program_counter_1 = 12;
	return;

L_1_12:	crit1 = false;
	program_counter_1 = 13;
	return;

	
L_1_13:	flag1 = 0;
	program_counter_1 = 14;
	return;

}

void thread2() {
	switch(program_counter_2) {
		case 1: goto L_2_1;
		case 2: goto L_2_2;
		case 3: goto L_2_3;
		case 4: goto L_2_4;
		case 5: goto L_2_5;
		case 6: goto L_2_6;
		case 7: goto L_2_7;
		case 8: goto L_2_8;
		case 9: goto L_2_9;
		case 10: goto L_2_10;
		case 11: goto L_2_11;
		case 12: goto L_2_12;
		case 13: goto L_2_13;
		default: return; 
	}

L_2_1:	flag2 = 1;
	program_counter_2 = 2;
	return;

L_2_2:	while(!(turn == 2)) {
		program_counter_2 = 3;
		return;

L_2_3:		if(turn == 1) {
			program_counter_2 = 4;
			return;

L_2_4:			if(flag1 == 0) {
				program_counter_2 = 5;
				return;

L_2_5:				turn = 2;
				program_counter_2 = 2;
				return;

			}
			program_counter_2 = 2;
			return;

		}
		program_counter_2 = 6;
		return;

L_2_6:		/*else*/ if(turn == 2) {
			program_counter_2 = 7;
			return;

L_2_7:			if(flag2 == 0) {
				program_counter_2 = 8;
				return;

L_2_8:				turn = 2;
				program_counter_2 = 2;
				return;

			}
			program_counter_2 = 2;
			return;

		}
		program_counter_2 = 2;
		return;

	}
	program_counter_2 = 9;
	return;

L_2_9:	flag2 = 2;
	program_counter_2 = 10;
	return;


L_2_10:	if(flag1 == 2) {
		program_counter_2 = 1;
		return;

	}
	program_counter_2 = 11;
	return;


L_2_11:	crit2 = true;
	program_counter_2 = 12;
	return;

L_2_12:	crit2 = false;
	program_counter_2 = 13;
	return;

	
L_2_13:	flag2 = 0;
	program_counter_2 = 14;
	return;

}

int entry() {
	crit1 = false;
	crit2 = false;

	while(true) {
		switch(nondet_int()) {
			case 0: thread1(); break;
			case 1: thread2(); break;
		}

		performed_operation();
	}
	return 1;
}



