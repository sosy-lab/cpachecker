#include "parserInterface.h"
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <map>
#include <string>

std::map<std::string, unsigned> labelToState;

void assert_equal(unsigned state1, unsigned state2) {
	if(state1 != state2 && state1 != UNREACHABLE && state2 != UNREACHABLE) {
		printf("ERROR: found state %i at branch end but expected state %i\n", state1, state2);
		exit(EXIT_FAILURE);
	}
}

#include "monitor.impl"

unsigned propagate_value(const char* label, unsigned state) {
	if(state == UNREACHABLE) {
		return state;
	}
	auto iterator = labelToState.find(label);
	if(iterator == labelToState.end()) {
		labelToState[label] = state;		
	} else {
		unsigned foundState = iterator->second;
		if(foundState != state) {
			printf("ERROR: found state %i at label but expected state %i\n", foundState, state);
			exit(EXIT_FAILURE);
		}
	}
	return state;
}


