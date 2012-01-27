#include "parserInterface.h"

unsigned get_init_state() {return 0;}
void assert_equal(unsigned state1, unsigned state2) {;}
unsigned compute_successor(unsigned state, const char* ident) {printf("Seen function call %s()\n", ident); return 0;}
unsigned propagate_value(const char* label, unsigned state) {return 0;}


