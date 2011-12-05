#define bool int
#define true 1
#define false 0

bool crit1;
bool crit2;

void error_fn() {
	ERROR: goto ERROR;
}

void __MONITOR_END_TRANSITION() {

}

void __MONITOR_START_TRANSITION() {

}

int checkProgramInvariant() {
	return !(crit1 && crit2);
}
