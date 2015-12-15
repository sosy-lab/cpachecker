// Testmodel C-Source File Name: Test_Model_Variant00005 ID: Test_Model_Variant00005_ID

//CPATiger functions
extern int input();
extern int __VERIFIER_nondet_int(void);

//Definition of event variables
//Variables representing that events are currently triggered and can be consumed by transitions
int internal_event_pumpStart;
int internal_event_pumpStop;
int output_event_pumpRunning;
int output_event_pumpNotRunning;
int input_event_waterRises;
int internal_event_levelMsg;
int input_event_timeoutPumpRunningElapsed;
int input_event_waterLower;
int input_event_startCmd;
int input_event_stopCmd;

//Definition of event trigger variables
//Variables representing the triggering of the corresponding events
int internal_event_pumpStart_trigger;
int internal_event_pumpStop_trigger;
int output_event_pumpRunning_trigger;
int output_event_pumpNotRunning_trigger;
int input_event_waterRises_trigger;
int internal_event_levelMsg_trigger;
int input_event_timeoutPumpRunningElapsed_trigger;
int input_event_waterLower_trigger;
int input_event_startCmd_trigger;
int input_event_stopCmd_trigger;

//Definition of attribute variables
int running;
int ready;
int lowLevel;
int normalLevel;
int highLevel;
int stop;

//Definition of region variables
int state_MinePump_Root_State_region_Pump_Ctrl_Region_active;
int state_MinePump_Root_State_region_Water_Monitoring_Region_active;
int state_MinePump_Root_State_region_MinePump_System_Region_active;
int state_Mine_Pump_System_Controller_region_Water_Level_Control_Region_active;
int state_Mine_Pump_System_Controller_region_Command_Ctrl_Region_active;

//Definiton running variables for automata
int running_Region_MinePump_Root_Region;
int running_Region_Pump_Ctrl_Region;
int running_Region_Water_Monitoring_Region;
int running_Region_MinePump_System_Region;
int running_Region_Water_Level_Control_Region;
int running_Region_Command_Ctrl_Region;

//Definition of function getNumber for guessing a value by CPATiger
int getNumber() {
	 return input();
}

//Definition of function select_helpers for setting environment variables simulated by by CPATiger
void select_helpers() {
	//Handle input events
	if(input_event_waterRises == 0){
		input_event_waterRises = getNumber();
	}

	if(input_event_timeoutPumpRunningElapsed == 0){
		input_event_timeoutPumpRunningElapsed = getNumber();
	}

	if(input_event_waterLower == 0){
		input_event_waterLower = getNumber();
	}

	if(input_event_startCmd == 0){
		input_event_startCmd = getNumber();
	}

	if(input_event_stopCmd == 0){
		input_event_stopCmd = getNumber();
	}


	//Handle input variables
}

//Definition of function setup for initializing all variables
void setup() {
	//Init event trigger
	internal_event_pumpStart_trigger = 0;
	internal_event_pumpStop_trigger = 0;
	output_event_pumpRunning_trigger = 0;
	output_event_pumpNotRunning_trigger = 0;
	input_event_waterRises_trigger = 0;
	internal_event_levelMsg_trigger = 0;
	input_event_timeoutPumpRunningElapsed_trigger = 0;
	input_event_waterLower_trigger = 0;
	input_event_startCmd_trigger = 0;
	input_event_stopCmd_trigger = 0;

	//Init events
	internal_event_pumpStart = 0;
	internal_event_pumpStop = 0;
	output_event_pumpRunning = 0;
	output_event_pumpNotRunning = 0;
	input_event_waterRises = 0;
	internal_event_levelMsg = 0;
	input_event_timeoutPumpRunningElapsed = 0;
	input_event_waterLower = 0;
	input_event_startCmd = 0;
	input_event_stopCmd = 0;

	//Init attributes
	running = 0;
	ready = 0;
	lowLevel = 0;
	normalLevel = 1;
	highLevel = 0;
	stop = 0;

	//Init region vars to be inactive
	state_MinePump_Root_State_region_Pump_Ctrl_Region_active = 0;
	state_MinePump_Root_State_region_Water_Monitoring_Region_active = 0;
	state_MinePump_Root_State_region_MinePump_System_Region_active = 0;
	state_Mine_Pump_System_Controller_region_Water_Level_Control_Region_active = 0;
	state_Mine_Pump_System_Controller_region_Command_Ctrl_Region_active = 0;

	//Init running vars for automata
	running_Region_MinePump_Root_Region = 0;
	running_Region_Pump_Ctrl_Region = 0;
	running_Region_Water_Monitoring_Region = 0;
	running_Region_MinePump_System_Region = 0;
	running_Region_Water_Level_Control_Region = 0;
	running_Region_Command_Ctrl_Region = 0;
}

//Enumeration for event broadcast
typedef enum Event_Broadcast {
	PUMPSTART,
	PUMPSTOP,
	PUMPRUNNING,
	PUMPNOTRUNNING,
	WATERRISES,
	LEVELMSG,
	TIMEOUTPUMPRUNNINGELAPSED,
	WATERLOWER,
	STARTCMD,
	STOPCMD
} Event_Broadcast;

//Definition of function broadcast representing an broadcast action
void broadcast(Event_Broadcast event) {
	 switch (event) {
	 case STOPCMD:
		input_event_stopCmd_trigger = 1;
		break;
	 case LEVELMSG:
		internal_event_levelMsg_trigger = 1;
		break;
	 case PUMPNOTRUNNING:
		output_event_pumpNotRunning_trigger = 1;
		break;
	 case PUMPSTART:
		internal_event_pumpStart_trigger = 1;
		break;
	 case PUMPSTOP:
		internal_event_pumpStop_trigger = 1;
		break;
	 case WATERRISES:
		input_event_waterRises_trigger = 1;
		break;
	 case STARTCMD:
		input_event_startCmd_trigger = 1;
		break;
	 case WATERLOWER:
		input_event_waterLower_trigger = 1;
		break;
	 case TIMEOUTPUMPRUNNINGELAPSED:
		input_event_timeoutPumpRunningElapsed_trigger = 1;
		break;
	 case PUMPRUNNING:
		output_event_pumpRunning_trigger = 1;
		break;
	 default: //do nothing
		break;
	}
}

//Definition of function consumeAndBroadcastEvents for set triggered events as consumed (= false) and set broadcasted events as triggered
void consumeAndBroadcastEvents() {
	//internal_event_pumpStart
	//consumed
	if(internal_event_pumpStart == 1) {
		internal_event_pumpStart = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(internal_event_pumpStart_trigger == 1) {
		internal_event_pumpStart_trigger = 0;
		internal_event_pumpStart = 1;
	}

	//internal_event_pumpStop
	//consumed
	if(internal_event_pumpStop == 1) {
		internal_event_pumpStop = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(internal_event_pumpStop_trigger == 1) {
		internal_event_pumpStop_trigger = 0;
		internal_event_pumpStop = 1;
	}

	//output_event_pumpRunning
	//consumed
	if(output_event_pumpRunning == 1) {
		output_event_pumpRunning = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(output_event_pumpRunning_trigger == 1) {
		output_event_pumpRunning_trigger = 0;
		output_event_pumpRunning = 1;
	}

	//output_event_pumpNotRunning
	//consumed
	if(output_event_pumpNotRunning == 1) {
		output_event_pumpNotRunning = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(output_event_pumpNotRunning_trigger == 1) {
		output_event_pumpNotRunning_trigger = 0;
		output_event_pumpNotRunning = 1;
	}

	//input_event_waterRises
	//consumed
	if(input_event_waterRises == 1) {
		input_event_waterRises = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_waterRises_trigger == 1) {
		input_event_waterRises_trigger = 0;
		input_event_waterRises = 1;
	}

	//internal_event_levelMsg
	//consumed
	if(internal_event_levelMsg == 1) {
		internal_event_levelMsg = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(internal_event_levelMsg_trigger == 1) {
		internal_event_levelMsg_trigger = 0;
		internal_event_levelMsg = 1;
	}

	//input_event_timeoutPumpRunningElapsed
	//consumed
	if(input_event_timeoutPumpRunningElapsed == 1) {
		input_event_timeoutPumpRunningElapsed = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_timeoutPumpRunningElapsed_trigger == 1) {
		input_event_timeoutPumpRunningElapsed_trigger = 0;
		input_event_timeoutPumpRunningElapsed = 1;
	}

	//input_event_waterLower
	//consumed
	if(input_event_waterLower == 1) {
		input_event_waterLower = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_waterLower_trigger == 1) {
		input_event_waterLower_trigger = 0;
		input_event_waterLower = 1;
	}

	//input_event_startCmd
	//consumed
	if(input_event_startCmd == 1) {
		input_event_startCmd = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_startCmd_trigger == 1) {
		input_event_startCmd_trigger = 0;
		input_event_startCmd = 1;
	}

	//input_event_stopCmd
	//consumed
	if(input_event_stopCmd == 1) {
		input_event_stopCmd = 0;
	}
	//if event was send in execution step via transition broadcast, the event is newly triggered
	if(input_event_stopCmd_trigger == 1) {
		input_event_stopCmd_trigger = 0;
		input_event_stopCmd = 1;
	}

}

//Definition of MinePump_Root_Region ---------------------------
enum stateGroups_MinePump_Root_Region{
	MINEPUMP_ROOT_REGION_GROUPLESS_STATES
} stateGroup_MinePump_Root_Region;

enum states_MinePump_Root_Region{
	MINEPUMP_ROOT_REGION_START_NODE,
	MINEPUMP_ROOT_STATE
} state_MinePump_Root_Region;

void run_MinePump_Root_Region() {
	// Initialize automata for first execution
	if(running_Region_MinePump_Root_Region == 0) {
		stateGroup_MinePump_Root_Region = MINEPUMP_ROOT_REGION_GROUPLESS_STATES;
		state_MinePump_Root_Region = MINEPUMP_ROOT_REGION_START_NODE;
		running_Region_MinePump_Root_Region = 1;
	}

	switch(stateGroup_MinePump_Root_Region) {
		case MINEPUMP_ROOT_REGION_GROUPLESS_STATES:
			LABEL_stateGroup_MINEPUMP_ROOT_REGION_GROUPLESS_STATES:
			switch(state_MinePump_Root_Region) {
				case MINEPUMP_ROOT_REGION_START_NODE:
					LABEL_state_MinePump_Root_Region_START_NODE__source:
					if(1) {
						LABEL_transition_default_MinePump_Root_Region__MinePump_Root_Region_START_NODE__MinePump_Root_State:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_MinePump_Root_Region = MINEPUMP_ROOT_REGION_GROUPLESS_STATES;
						LABEL_state_MinePump_Root_State__target__default_MinePump_Root_Region: state_MinePump_Root_Region = MINEPUMP_ROOT_STATE;

						//Activate sub regions of target state
						state_MinePump_Root_State_region_Pump_Ctrl_Region_active = 1;
						state_MinePump_Root_State_region_Water_Monitoring_Region_active = 1;
						state_MinePump_Root_State_region_MinePump_System_Region_active = 1;
					}

					break;
				case MINEPUMP_ROOT_STATE:
					LABEL_state_MinePump_Root_State__source:
					break;
				default: // states switch default statement
					break;
			}
			break;
		default: // states switch default statement
			break;
	}
}

//Definition of Pump_Ctrl_Region ---------------------------
enum stateGroups_Pump_Ctrl_Region{
	PUMP_CTRL_REGION_GROUPLESS_STATES
} stateGroup_Pump_Ctrl_Region;

enum states_Pump_Ctrl_Region{
	PUMP_CTRL_START_NODE,
	PUMP_OFF,
	PUMP_ON
} state_Pump_Ctrl_Region;

void run_Pump_Ctrl_Region() {
	// Initialize automata for first execution
	if(running_Region_Pump_Ctrl_Region == 0) {
		stateGroup_Pump_Ctrl_Region = PUMP_CTRL_REGION_GROUPLESS_STATES;
		state_Pump_Ctrl_Region = PUMP_CTRL_START_NODE;
		running_Region_Pump_Ctrl_Region = 1;
	}

	switch(stateGroup_Pump_Ctrl_Region) {
		case PUMP_CTRL_REGION_GROUPLESS_STATES:
			LABEL_stateGroup_PUMP_CTRL_REGION_GROUPLESS_STATES:
			switch(state_Pump_Ctrl_Region) {
				case PUMP_CTRL_START_NODE:
					LABEL_state_Pump_Ctrl_START_NODE__source:
					if(1) {
						LABEL_transition_default_Pump_Ctrl_Region__Pump_Ctrl_START_NODE__Pump_Off:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Pump_Ctrl_Region = PUMP_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_Off__target__default_Pump_Ctrl_Region: state_Pump_Ctrl_Region = PUMP_OFF;

					}

					break;
				case PUMP_OFF:
					LABEL_state_Pump_Off__source:
					if(internal_event_pumpStart == 1) {
						LABEL_transition_t_pump_Start__Pump_Off__Pump_On:

						//Handle transition actions
						broadcast(PUMPRUNNING);
						running = 1;

						//Set Target Group and State
						stateGroup_Pump_Ctrl_Region = PUMP_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_On__target__t_pump_Start: state_Pump_Ctrl_Region = PUMP_ON;

					}

					break;
				case PUMP_ON:
					LABEL_state_Pump_On__source:
					if(internal_event_pumpStop == 1) {
						LABEL_transition_t_pump_Stop__Pump_On__Pump_Off:

						//Handle transition actions
						broadcast(PUMPNOTRUNNING);
						running = 0;

						//Set Target Group and State
						stateGroup_Pump_Ctrl_Region = PUMP_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_Off__target__t_pump_Stop: state_Pump_Ctrl_Region = PUMP_OFF;

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

//Definition of Water_Monitoring_Region ---------------------------
enum stateGroups_Water_Monitoring_Region{
	WATER_MONITORING_REGION_GROUPLESS_STATES
} stateGroup_Water_Monitoring_Region;

enum states_Water_Monitoring_Region{
	WATER_MONITORING_START_NODE,
	NORMAL,
	HIGH,
	LOW
} state_Water_Monitoring_Region;

void run_Water_Monitoring_Region() {
	// Initialize automata for first execution
	if(running_Region_Water_Monitoring_Region == 0) {
		stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
		state_Water_Monitoring_Region = WATER_MONITORING_START_NODE;
		running_Region_Water_Monitoring_Region = 1;
	}

	switch(stateGroup_Water_Monitoring_Region) {
		case WATER_MONITORING_REGION_GROUPLESS_STATES:
			LABEL_stateGroup_WATER_MONITORING_REGION_GROUPLESS_STATES:
			switch(state_Water_Monitoring_Region) {
				case WATER_MONITORING_START_NODE:
					LABEL_state_Water_Monitoring_START_NODE__source:
					if(1) {
						LABEL_transition_default_Water_Monitoring_Region__Water_Monitoring_START_NODE__Normal:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_Normal__target__default_Water_Monitoring_Region: state_Water_Monitoring_Region = NORMAL;

					}

					break;
				case NORMAL:
					LABEL_state_Normal__source:
					if(input_event_waterRises == 1) {
						LABEL_transition_t_waterRises_normal_high__Normal__High:

						//Handle transition actions
						broadcast(LEVELMSG);
						highLevel = 1;
						normalLevel = 0;

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_High__target__t_waterRises_normal_high: state_Water_Monitoring_Region = HIGH;

					}

					else if(input_event_waterLower == 1) {
						LABEL_transition_t_waterLower_normal_low__Normal__Low:

						//Handle transition actions
						normalLevel = 0;
						lowLevel = 1;
						broadcast(LEVELMSG);

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_Low__target__t_waterLower_normal_low: state_Water_Monitoring_Region = LOW;

					}

					break;
				case HIGH:
					LABEL_state_High__source:
					if(input_event_waterRises == 1) {
						LABEL_transition_t_waterRises_high__High__High:

						//Handle transition actions
						broadcast(LEVELMSG);

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_High__target__t_waterRises_high: state_Water_Monitoring_Region = HIGH;

					}

					else if(input_event_waterLower == 1) {
						LABEL_transition_t_waterLower_high_normal__High__Normal:

						//Handle transition actions
						broadcast(LEVELMSG);
						highLevel = 0;
						normalLevel = 1;

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_Normal__target__t_waterLower_high_normal: state_Water_Monitoring_Region = NORMAL;

					}

					break;
				case LOW:
					LABEL_state_Low__source:
					if(input_event_waterLower == 1) {
						LABEL_transition_t_waterLower_low__Low__Low:

						//Handle transition actions
						broadcast(LEVELMSG);

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_Low__target__t_waterLower_low: state_Water_Monitoring_Region = LOW;

					}

					else if(input_event_waterRises == 1) {
						LABEL_transition_t_waterRises_low_normal__Low__Normal:

						//Handle transition actions
						broadcast(LEVELMSG);
						lowLevel = 0;
						normalLevel = 1;

						//Set Target Group and State
						stateGroup_Water_Monitoring_Region = WATER_MONITORING_REGION_GROUPLESS_STATES;
						LABEL_state_Normal__target__t_waterRises_low_normal: state_Water_Monitoring_Region = NORMAL;

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

//Definition of MinePump_System_Region ---------------------------
enum stateGroups_MinePump_System_Region{
	MINEPUMP_SYSTEM_REGION_GROUPLESS_STATES
} stateGroup_MinePump_System_Region;

enum states_MinePump_System_Region{
	MINEPUMP_SYSTEM_START_NODE,
	MINE_PUMP_SYSTEM_CONTROLLER
} state_MinePump_System_Region;

void run_MinePump_System_Region() {
	// Initialize automata for first execution
	if(running_Region_MinePump_System_Region == 0) {
		stateGroup_MinePump_System_Region = MINEPUMP_SYSTEM_REGION_GROUPLESS_STATES;
		state_MinePump_System_Region = MINEPUMP_SYSTEM_START_NODE;
		running_Region_MinePump_System_Region = 1;
	}

	switch(stateGroup_MinePump_System_Region) {
		case MINEPUMP_SYSTEM_REGION_GROUPLESS_STATES:
			LABEL_stateGroup_MINEPUMP_SYSTEM_REGION_GROUPLESS_STATES:
			switch(state_MinePump_System_Region) {
				case MINEPUMP_SYSTEM_START_NODE:
					LABEL_state_MinePump_System_START_NODE__source:
					if(1) {
						LABEL_transition_default_MinePump_System__MinePump_System_START_NODE__Mine_Pump_System_Controller:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_MinePump_System_Region = MINEPUMP_SYSTEM_REGION_GROUPLESS_STATES;
						LABEL_state_Mine_Pump_System_Controller__target__default_MinePump_System: state_MinePump_System_Region = MINE_PUMP_SYSTEM_CONTROLLER;

						//Activate sub regions of target state
						state_Mine_Pump_System_Controller_region_Water_Level_Control_Region_active = 1;
						state_Mine_Pump_System_Controller_region_Command_Ctrl_Region_active = 1;
					}

					break;
				case MINE_PUMP_SYSTEM_CONTROLLER:
					LABEL_state_Mine_Pump_System_Controller__source:
					break;
				default: // states switch default statement
					break;
			}
			break;
		default: // states switch default statement
			break;
	}
}

//Definition of Water_Level_Control_Region ---------------------------
enum stateGroups_Water_Level_Control_Region{
	WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES
} stateGroup_Water_Level_Control_Region;

enum states_Water_Level_Control_Region{
	WATER_LEVEL_INIT_STATE,
	WATER_LEVEL_IDLE,
	PUMP_TIMEOUT_ELAPSED,
	LEVELMSG_RECEIVED,
	HIGH_LEVEL_DETECTED,
	PUMP_SET_READY,
	PUMP_STARTED
} state_Water_Level_Control_Region;

void run_Water_Level_Control_Region() {
	// Initialize automata for first execution
	if(running_Region_Water_Level_Control_Region == 0) {
		stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
		state_Water_Level_Control_Region = WATER_LEVEL_INIT_STATE;
		running_Region_Water_Level_Control_Region = 1;
	}

	switch(stateGroup_Water_Level_Control_Region) {
		case WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES:
			LABEL_stateGroup_WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES:
			switch(state_Water_Level_Control_Region) {
				case WATER_LEVEL_INIT_STATE:
					LABEL_state_Water_Level_Init_State__source:
					if(1) {
						LABEL_transition_Water_Level_Default__Water_Level_Init_State__Water_Level_Idle:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Water_Level_Idle__target__Water_Level_Default: state_Water_Level_Control_Region = WATER_LEVEL_IDLE;

					}

					break;
				case WATER_LEVEL_IDLE:
					LABEL_state_Water_Level_Idle__source:
					if(internal_event_levelMsg == 1) {
						LABEL_transition_t_levelMsg__Water_Level_Idle__LevelMsg_Received:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_LevelMsg_Received__target__t_levelMsg: state_Water_Level_Control_Region = LEVELMSG_RECEIVED;

					}

					break;
				case PUMP_TIMEOUT_ELAPSED:
					LABEL_state_pump_timeout_elapsed__source:
					if((running == 0)) {
						LABEL_transition_t_timeout_pump_stop__pump_timeout_elapsed__Water_Level_Idle:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Water_Level_Idle__target__t_timeout_pump_stop: state_Water_Level_Control_Region = WATER_LEVEL_IDLE;

					}

					break;
				case LEVELMSG_RECEIVED:
					LABEL_state_LevelMsg_Received__source:
					if(input_event_timeoutPumpRunningElapsed == 1 && ((running == 1))) {
						LABEL_transition_t_pump_timeout_elapsed__LevelMsg_Received__pump_timeout_elapsed:

						//Handle transition actions
						broadcast(PUMPSTOP);

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_pump_timeout_elapsed__target__t_pump_timeout_elapsed: state_Water_Level_Control_Region = PUMP_TIMEOUT_ELAPSED;

					}

					else if((highLevel == 1)) {
						LABEL_transition_t_levelmsg_highlevel_true__LevelMsg_Received__High_Level_Detected:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_High_Level_Detected__target__t_levelmsg_highlevel_true: state_Water_Level_Control_Region = HIGH_LEVEL_DETECTED;

					}

					break;
				case HIGH_LEVEL_DETECTED:
					LABEL_state_High_Level_Detected__source:
					if((ready == 0 && running == 0)) {
						LABEL_transition_t_high_set_ready__High_Level_Detected__Pump_Set_Ready:

						//Handle transition actions
						ready = 1;

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_Set_Ready__target__t_high_set_ready: state_Water_Level_Control_Region = PUMP_SET_READY;

					}

					else if((ready == 1 && running == 0)) {
						LABEL_transition_t_high_ready_startPump__High_Level_Detected__Pump_Started:

						//Handle transition actions
						broadcast(PUMPSTART);

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_Started__target__t_high_ready_startPump: state_Water_Level_Control_Region = PUMP_STARTED;

					}

					else if((running == 1 || stop == 1)) {
						LABEL_transition_t_running_high_idle_b__High_Level_Detected__Water_Level_Idle:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Water_Level_Idle__target__t_running_high_idle_b: state_Water_Level_Control_Region = WATER_LEVEL_IDLE;

					}

					break;
				case PUMP_SET_READY:
					LABEL_state_Pump_Set_Ready__source:
					if(1) {
						LABEL_transition_t_pump_ready_startPump__Pump_Set_Ready__Pump_Started:

						//Handle transition actions
						broadcast(PUMPSTART);

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_Started__target__t_pump_ready_startPump: state_Water_Level_Control_Region = PUMP_STARTED;

					}

					break;
				case PUMP_STARTED:
					LABEL_state_Pump_Started__source:
					if((running == 1)) {
						LABEL_transition_t_pumpStarted_idle__Pump_Started__Water_Level_Idle:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Water_Level_Control_Region = WATER_LEVEL_CONTROL_REGION_GROUPLESS_STATES;
						LABEL_state_Water_Level_Idle__target__t_pumpStarted_idle: state_Water_Level_Control_Region = WATER_LEVEL_IDLE;

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

//Definition of Command_Ctrl_Region ---------------------------
enum stateGroups_Command_Ctrl_Region{
	COMMAND_CTRL_REGION_GROUPLESS_STATES
} stateGroup_Command_Ctrl_Region;

enum states_Command_Ctrl_Region{
	START_COMMAND_RECEIVED,
	PUMP_SET_READY_START_CMD,
	STOP_COMMAND_RECEIVED,
	STOP_PUMP_BY_COMMAND,
	COMMAND_CTRL_INIT,
	COMMAND_CTRL_IDLE
} state_Command_Ctrl_Region;

void run_Command_Ctrl_Region() {
	// Initialize automata for first execution
	if(running_Region_Command_Ctrl_Region == 0) {
		stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
		state_Command_Ctrl_Region = COMMAND_CTRL_INIT;
		running_Region_Command_Ctrl_Region = 1;
	}

	switch(stateGroup_Command_Ctrl_Region) {
		case COMMAND_CTRL_REGION_GROUPLESS_STATES:
			LABEL_stateGroup_COMMAND_CTRL_REGION_GROUPLESS_STATES:
			switch(state_Command_Ctrl_Region) {
				case START_COMMAND_RECEIVED:
					LABEL_state_Start_Command_Received__source:
					if((running == 1 || ready == 1)) {
						LABEL_transition_t_start_cmd_running_or_ready__Start_Command_Received__Command_Ctrl_Idle:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Command_Ctrl_Idle__target__t_start_cmd_running_or_ready: state_Command_Ctrl_Region = COMMAND_CTRL_IDLE;

					}

					else if((running == 0 || ready == 0)) {
						LABEL_transition_t_set_stop_false__Start_Command_Received__Pump_Set_Ready_Start_Cmd:

						//Handle transition actions
						stop = 0;

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Pump_Set_Ready_Start_Cmd__target__t_set_stop_false: state_Command_Ctrl_Region = PUMP_SET_READY_START_CMD;

					}

					break;
				case PUMP_SET_READY_START_CMD:
					LABEL_state_Pump_Set_Ready_Start_Cmd__source:
					if(1) {
						LABEL_transition_t_start_cmd_set_ready__Pump_Set_Ready_Start_Cmd__Command_Ctrl_Idle:

						//Handle transition actions
						ready = 1;

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Command_Ctrl_Idle__target__t_start_cmd_set_ready: state_Command_Ctrl_Region = COMMAND_CTRL_IDLE;

					}

					break;
				case STOP_COMMAND_RECEIVED:
					LABEL_state_Stop_Command_Received__source:
					if((running == 0 || ready == 0)) {
						LABEL_transition_t_stop_not_running_or_not_ready__Stop_Command_Received__Command_Ctrl_Idle:

						//Handle transition actions
						stop = 1;

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Command_Ctrl_Idle__target__t_stop_not_running_or_not_ready: state_Command_Ctrl_Region = COMMAND_CTRL_IDLE;

					}

					else if((running == 1)) {
						LABEL_transition_t_stop_running_pump_stop__Stop_Command_Received__Stop_Pump_By_Command:

						//Handle transition actions
						broadcast(PUMPSTOP);

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Stop_Pump_By_Command__target__t_stop_running_pump_stop: state_Command_Ctrl_Region = STOP_PUMP_BY_COMMAND;

					}

					break;
				case STOP_PUMP_BY_COMMAND:
					LABEL_state_Stop_Pump_By_Command__source:
					if((running == 0)) {
						LABEL_transition_t_pump_not_running_stop_true__Stop_Pump_By_Command__Command_Ctrl_Idle:

						//Handle transition actions
						stop = 1;
						ready = 0;

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Command_Ctrl_Idle__target__t_pump_not_running_stop_true: state_Command_Ctrl_Region = COMMAND_CTRL_IDLE;

					}

					break;
				case COMMAND_CTRL_INIT:
					LABEL_state_Command_Ctrl_Init__source:
					if(1) {
						LABEL_transition_Command_Ctrl_Default__Command_Ctrl_Init__Command_Ctrl_Idle:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Command_Ctrl_Idle__target__Command_Ctrl_Default: state_Command_Ctrl_Region = COMMAND_CTRL_IDLE;

					}

					break;
				case COMMAND_CTRL_IDLE:
					LABEL_state_Command_Ctrl_Idle__source:
					if(input_event_startCmd == 1) {
						LABEL_transition_t_start_Cmd__Command_Ctrl_Idle__Start_Command_Received:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Start_Command_Received__target__t_start_Cmd: state_Command_Ctrl_Region = START_COMMAND_RECEIVED;

					}

					else if(input_event_stopCmd == 1) {
						LABEL_transition_t_stop_Cmd__Command_Ctrl_Idle__Stop_Command_Received:

						//Handle transition actions

						//Set Target Group and State
						stateGroup_Command_Ctrl_Region = COMMAND_CTRL_REGION_GROUPLESS_STATES;
						LABEL_state_Stop_Command_Received__target__t_stop_Cmd: state_Command_Ctrl_Region = STOP_COMMAND_RECEIVED;

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
	int i = 0;
	while (i < 10) {
		i++;
		//Simulate Environment
		select_helpers();

		//list automata in depth-first order and for each sub automata use active variable
		//Root Automata is always running
		run_MinePump_Root_Region();
		if(state_MinePump_Root_State_region_Pump_Ctrl_Region_active == 1)
			run_Pump_Ctrl_Region();
		if(state_MinePump_Root_State_region_Water_Monitoring_Region_active == 1)
			run_Water_Monitoring_Region();
		if(state_MinePump_Root_State_region_MinePump_System_Region_active == 1)
			run_MinePump_System_Region();
		if(state_Mine_Pump_System_Controller_region_Water_Level_Control_Region_active == 1)
			run_Water_Level_Control_Region();
		if(state_Mine_Pump_System_Controller_region_Command_Ctrl_Region_active == 1)
			run_Command_Ctrl_Region();

		//handle event broadcast and event consumption
		consumeAndBroadcastEvents();
	}

	return 0;
}
