// Testmodel C-Source File Name: Test_Model_Variant00006 ID: Test_Model_Variant00006_ID

//CPATiger functions
extern int input(void);
extern int __VERIFIER_nondet_int(void);

//Definition of event variables
//Variables representing that events are currently triggered and can be consumed by transitions
int internal_event_noRain;
int internal_event_rain;
int input_event_non;
int input_event_little;
int input_event_heavy;
int input_event_off;
int input_event_intvOn;
int input_event_intvOff;
int output_event_slowWipe;
int output_event_stopWipe;
int output_event_permWipe;
int input_event_manualOn;
int output_event_fastWipe;

//Definition of event trigger variables
//Variables representing the triggering of the corresponding events
int internal_event_noRain_trigger;
int internal_event_rain_trigger;
int input_event_non_trigger;
int input_event_little_trigger;
int input_event_heavy_trigger;
int input_event_off_trigger;
int input_event_intvOn_trigger;
int input_event_intvOff_trigger;
int output_event_slowWipe_trigger;
int output_event_stopWipe_trigger;
int output_event_permWipe_trigger;
int input_event_manualOn_trigger;
int output_event_fastWipe_trigger;

//Definition of attribute variables

//Definition of region variables
int state_Wiper_region_Sensor_Root_active;
int state_Wiper_region_Wiper_Root_active;

//Definiton running variables for automata
int running_Region_Root_Wiper;
int running_Region_Sensor_Root;
int running_Region_Wiper_Root;

//Definition of function getNumber for guessing a value by CPATiger
int getNumber() {
	 return input();
}

//Definition of function select_helpers for setting environment variables simulated by by CPATiger
void select_helpers() {
	//Handle input events
	if(input_event_non == 0){
		input_event_non = getNumber();
	}

	if(input_event_little == 0){
		input_event_little = getNumber();
	}

	if(input_event_heavy == 0){
		input_event_heavy = getNumber();
	}

	if(input_event_off == 0){
		input_event_off = getNumber();
	}

	if(input_event_intvOn == 0){
		input_event_intvOn = getNumber();
	}

	if(input_event_intvOff == 0){
		input_event_intvOff = getNumber();
	}

	if(input_event_manualOn == 0){
		input_event_manualOn = getNumber();
	}


	//Handle input variables
}

//Definition of function setup for initializing all variables
void setup() {
	//Init event trigger
	internal_event_noRain_trigger = 0;
	internal_event_rain_trigger = 0;
	input_event_non_trigger = 0;
	input_event_little_trigger = 0;
	input_event_heavy_trigger = 0;
	input_event_off_trigger = 0;
	input_event_intvOn_trigger = 0;
	input_event_intvOff_trigger = 0;
	output_event_slowWipe_trigger = 0;
	output_event_stopWipe_trigger = 0;
	output_event_permWipe_trigger = 0;
	input_event_manualOn_trigger = 0;
	output_event_fastWipe_trigger = 0;

	//Init events
	internal_event_noRain = 0;
	internal_event_rain = 0;
	input_event_non = 0;
	input_event_little = 0;
	input_event_heavy = 0;
	input_event_off = 0;
	input_event_intvOn = 0;
	input_event_intvOff = 0;
	output_event_slowWipe = 0;
	output_event_stopWipe = 0;
	output_event_permWipe = 0;
	input_event_manualOn = 0;
	output_event_fastWipe = 0;

	//Init attributes

	//Init region vars to be inactive
	state_Wiper_region_Sensor_Root_active = 0;
	state_Wiper_region_Wiper_Root_active = 0;

	//Init running vars for automata
	running_Region_Root_Wiper = 0;
	running_Region_Sensor_Root = 0;
	running_Region_Wiper_Root = 0;
}

//Enumeration for event broadcast
typedef enum Event_Broadcast {
	NORAIN,
	RAIN,
	NON,
	LITTLE,
	HEAVY,
	OFF,
	INTVON,
	INTVOFF,
	SLOWWIPE,
	STOPWIPE,
	PERMWIPE,
	MANUALON,
	FASTWIPE
} Event_Broadcast;

//Definition of function broadcast representing an broadcast action
void broadcast(Event_Broadcast event) {
	 switch (event) {
	 case INTVOFF:
		input_event_intvOff_trigger = 1;
		break;
	 case INTVON:
		input_event_intvOn_trigger = 1;
		break;
	 case STOPWIPE:
		output_event_stopWipe_trigger = 1;
		break;
	 case PERMWIPE:
		output_event_permWipe_trigger = 1;
		break;
	 case LITTLE:
		input_event_little_trigger = 1;
		break;
	 case NORAIN:
		internal_event_noRain_trigger = 1;
		break;
	 case NON:
		input_event_non_trigger = 1;
		break;
	 case OFF:
		input_event_off_trigger = 1;
		break;
	 case HEAVY:
		input_event_heavy_trigger = 1;
		break;
	 case RAIN:
		internal_event_rain_trigger = 1;
		break;
	 case FASTWIPE:
		output_event_fastWipe_trigger = 1;
		break;
	 case MANUALON:
		input_event_manualOn_trigger = 1;
		break;
	 case SLOWWIPE:
		output_event_slowWipe_trigger = 1;
		break;
	 default: //do nothing
		break;
	}
}

//Definition of function consumeAndBroadcastEvents for set triggered events as consumed (= false) and set broadcasted events as triggered
void consumeAndBroadcastEvents() {
	//internal_event_noRain
	//consumed
	if(internal_event_noRain == 1) {
		internal_event_noRain = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(internal_event_noRain_trigger == 1) {
		internal_event_noRain_trigger = 0;
		internal_event_noRain = 1;
	}

	//internal_event_rain
	//consumed
	if(internal_event_rain == 1) {
		internal_event_rain = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(internal_event_rain_trigger == 1) {
		internal_event_rain_trigger = 0;
		internal_event_rain = 1;
	}

	//input_event_non
	//consumed
	if(input_event_non == 1) {
		input_event_non = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_non_trigger == 1) {
		input_event_non_trigger = 0;
		input_event_non = 1;
	}

	//input_event_little
	//consumed
	if(input_event_little == 1) {
		input_event_little = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_little_trigger == 1) {
		input_event_little_trigger = 0;
		input_event_little = 1;
	}

	//input_event_heavy
	//consumed
	if(input_event_heavy == 1) {
		input_event_heavy = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_heavy_trigger == 1) {
		input_event_heavy_trigger = 0;
		input_event_heavy = 1;
	}

	//input_event_off
	//consumed
	if(input_event_off == 1) {
		input_event_off = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_off_trigger == 1) {
		input_event_off_trigger = 0;
		input_event_off = 1;
	}

	//input_event_intvOn
	//consumed
	if(input_event_intvOn == 1) {
		input_event_intvOn = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_intvOn_trigger == 1) {
		input_event_intvOn_trigger = 0;
		input_event_intvOn = 1;
	}

	//input_event_intvOff
	//consumed
	if(input_event_intvOff == 1) {
		input_event_intvOff = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_intvOff_trigger == 1) {
		input_event_intvOff_trigger = 0;
		input_event_intvOff = 1;
	}

	//output_event_slowWipe
	//consumed
	if(output_event_slowWipe == 1) {
		output_event_slowWipe = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(output_event_slowWipe_trigger == 1) {
		output_event_slowWipe_trigger = 0;
		output_event_slowWipe = 1;
	}

	//output_event_stopWipe
	//consumed
	if(output_event_stopWipe == 1) {
		output_event_stopWipe = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(output_event_stopWipe_trigger == 1) {
		output_event_stopWipe_trigger = 0;
		output_event_stopWipe = 1;
	}

	//output_event_permWipe
	//consumed
	if(output_event_permWipe == 1) {
		output_event_permWipe = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(output_event_permWipe_trigger == 1) {
		output_event_permWipe_trigger = 0;
		output_event_permWipe = 1;
	}

	//input_event_manualOn
	//consumed
	if(input_event_manualOn == 1) {
		input_event_manualOn = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_manualOn_trigger == 1) {
		input_event_manualOn_trigger = 0;
		input_event_manualOn = 1;
	}

	//output_event_fastWipe
	//consumed
	if(output_event_fastWipe == 1) {
		output_event_fastWipe = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(output_event_fastWipe_trigger == 1) {
		output_event_fastWipe_trigger = 0;
		output_event_fastWipe = 1;
	}

}

//Definition of Root_Wiper ---------------------------
enum stateGroups_Root_Wiper{
	ROOT_WIPER_GROUPLESS_STATES
} stateGroup_Root_Wiper;

enum states_Root_Wiper{
	ROOT_WIPER_SYSTEM_START_NODE,
	WIPER
} state_Root_Wiper;

void run_Root_Wiper() {
	// Initialize automata for first execution
	if(running_Region_Root_Wiper == 0) {
		stateGroup_Root_Wiper = ROOT_WIPER_GROUPLESS_STATES;
		state_Root_Wiper = ROOT_WIPER_SYSTEM_START_NODE;
		running_Region_Root_Wiper = 1;
	}

	switch(stateGroup_Root_Wiper) {
		case ROOT_WIPER_GROUPLESS_STATES:
			LABEL_stateGroup_ROOT_WIPER_GROUPLESS_STATES:
			switch(state_Root_Wiper) {
				case ROOT_WIPER_SYSTEM_START_NODE:
					LABEL_state_Root_Wiper_System_START_NODE__source:
					if(1) {
						LABEL_transition_default_Root_Wiper__Root_Wiper_System_START_NODE__Wiper:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Root_Wiper = ROOT_WIPER_GROUPLESS_STATES;
						LABEL_state_Wiper__target__default_Root_Wiper: state_Root_Wiper = WIPER;

						//Activate sub regions of target state
						state_Wiper_region_Sensor_Root_active = 1;
						state_Wiper_region_Wiper_Root_active = 1;
					}

					break;
				case WIPER:
					LABEL_state_Wiper__source:
					break;
				default: // states switch default statement
					break;
			}
			break;
		default: // states switch default statement
			break;
	}
}

//Definition of Sensor_Root ---------------------------
enum stateGroups_Sensor_Root{
	SENSOR_ROOT_GROUPLESS_STATES
} stateGroup_Sensor_Root;

enum states_Sensor_Root{
	SENSOR_ROOT_START_NODE,
	SENSORS1,
	SENSORS2
} state_Sensor_Root;

void run_Sensor_Root() {
	// Initialize automata for first execution
	if(running_Region_Sensor_Root == 0) {
		stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
		state_Sensor_Root = SENSOR_ROOT_START_NODE;
		running_Region_Sensor_Root = 1;
	}

	switch(stateGroup_Sensor_Root) {
		case SENSOR_ROOT_GROUPLESS_STATES:
			LABEL_stateGroup_SENSOR_ROOT_GROUPLESS_STATES:
			switch(state_Sensor_Root) {
				case SENSOR_ROOT_START_NODE:
					LABEL_state_Sensor_Root_START_NODE__source:
					if(1) {
						LABEL_transition_default_Sensor_Root__Sensor_Root_START_NODE__SensorS1:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS1__target__default_Sensor_Root: state_Sensor_Root = SENSORS1;

					}

					break;
				case SENSORS1:
					LABEL_state_SensorS1__source:
					if(input_event_non == 1) {
						LABEL_transition_t1__SensorS1__SensorS1:

						//Handle transition actions
						broadcast(NORAIN);

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS1__target__t1: state_Sensor_Root = SENSORS1;

					}

					else if(input_event_little == 1) {
						LABEL_transition_t2__SensorS1__SensorS2:

						//Handle transition actions
						broadcast(RAIN);

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS2__target__t2: state_Sensor_Root = SENSORS2;

					}

					else if(input_event_heavy == 1) {
						LABEL_transition_t3__SensorS1__SensorS2:

						//Handle transition actions
						broadcast(RAIN);

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS2__target__t3: state_Sensor_Root = SENSORS2;

					}

					break;
				case SENSORS2:
					LABEL_state_SensorS2__source:
					if(input_event_non == 1) {
						LABEL_transition_t4__SensorS2__SensorS1:

						//Handle transition actions
						broadcast(NORAIN);

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS1__target__t4: state_Sensor_Root = SENSORS1;

					}

					else if(input_event_little == 1) {
						LABEL_transition_t6__SensorS2__SensorS2:

						//Handle transition actions
						broadcast(RAIN);

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS2__target__t6: state_Sensor_Root = SENSORS2;

					}

					else if(input_event_heavy == 1) {
						LABEL_transition_t5__SensorS2__SensorS2:

						//Handle transition actions
						broadcast(RAIN);

						//Set Target Group and State
						stateGroup_Sensor_Root = SENSOR_ROOT_GROUPLESS_STATES;
						LABEL_state_SensorS2__target__t5: state_Sensor_Root = SENSORS2;

					}

					break;
				default: // states switch default statement
					break;
			}
			break;
		default: // states switch default statement
			break;
	}
}

//Definition of Wiper_Root ---------------------------
enum stateGroups_Wiper_Root{
	WIPER_ROOT_GROUPLESS_STATES
} stateGroup_Wiper_Root;

enum states_Wiper_Root{
	WIPER_ROOT_START_NODE,
	WIPERS1,
	WIPERS2,
	WIPERS3
} state_Wiper_Root;

void run_Wiper_Root() {
	// Initialize automata for first execution
	if(running_Region_Wiper_Root == 0) {
		stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
		state_Wiper_Root = WIPER_ROOT_START_NODE;
		running_Region_Wiper_Root = 1;
	}

	switch(stateGroup_Wiper_Root) {
		case WIPER_ROOT_GROUPLESS_STATES:
			LABEL_stateGroup_WIPER_ROOT_GROUPLESS_STATES:
			switch(state_Wiper_Root) {
				case WIPER_ROOT_START_NODE:
					LABEL_state_Wiper_Root_START_NODE__source:
					if(1) {
						LABEL_transition_default_Wiper_Root__Wiper_Root_START_NODE__WiperS1:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS1__target__default_Wiper_Root: state_Wiper_Root = WIPERS1;

					}

					break;
				case WIPERS1:
					LABEL_state_WiperS1__source:
					if(input_event_off == 1) {
						LABEL_transition_t12__WiperS1__WiperS1:

						//Handle transition actions
						broadcast(STOPWIPE);

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS1__target__t12: state_Wiper_Root = WIPERS1;

					}

					else if(input_event_intvOn == 1) {
						LABEL_transition_t13__WiperS1__WiperS2:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS2__target__t13: state_Wiper_Root = WIPERS2;

					}

					else if(input_event_manualOn == 1) {
						LABEL_transition_t19__WiperS1__WiperS3:

						//Handle transition actions
						broadcast(PERMWIPE);

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS3__target__t19: state_Wiper_Root = WIPERS3;

					}

					break;
				case WIPERS2:
					LABEL_state_WiperS2__source:
					if(input_event_intvOff == 1) {
						LABEL_transition_t14__WiperS2__WiperS1:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS1__target__t14: state_Wiper_Root = WIPERS1;

					}

					else if(internal_event_noRain == 1) {
						LABEL_transition_t15__WiperS2__WiperS2:

						//Handle transition actions
						broadcast(STOPWIPE);

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS2__target__t15: state_Wiper_Root = WIPERS2;

					}

					else if(internal_event_rain == 1) {
						LABEL_transition_t22__WiperS2__WiperS2:

						//Handle transition actions
						broadcast(FASTWIPE);

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS2__target__t22: state_Wiper_Root = WIPERS2;

					}

					break;
				case WIPERS3:
					LABEL_state_WiperS3__source:
					if(input_event_off == 1) {
						LABEL_transition_t18__WiperS3__WiperS1:

						//Handle transition actions
						broadcast(STOPWIPE);

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS1__target__t18: state_Wiper_Root = WIPERS1;

					}

					else if(input_event_intvOn == 1) {
						LABEL_transition_t20__WiperS3__WiperS2:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Wiper_Root = WIPER_ROOT_GROUPLESS_STATES;
						LABEL_state_WiperS2__target__t20: state_Wiper_Root = WIPERS2;

					}

					break;
				default: // states switch default statement
					break;
			}
			break;
		default: // states switch default statement
			break;
	}
}

//Start Automata
int main(void) {
	//Initalize variables with default values
	setup();

	//Start automata "endless" loop
	while (getNumber()) {
		//Simulate Environment
		select_helpers();

		//list automata in depth-first order and for each sub automata use active variable
		//Root Automata is always running
		run_Root_Wiper();
		if(state_Wiper_region_Sensor_Root_active == 1)
			run_Sensor_Root();
		if(state_Wiper_region_Wiper_Root_active == 1)
			run_Wiper_Root();

		//handle event broadcast and event consumption
		consumeAndBroadcastEvents();
	}

	return 0;
}
