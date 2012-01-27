#include "parserInterface.h"

unsigned get_init_state() {return 0;}
void assert_equal(unsigned state1, unsigned state2) {;}
unsigned compute_successor(unsigned state, const char* ident) {printf("Seen function call %s()\n", ident); return 0;}
void register_value(const char* label, unsigned state) {}
unsigned get_value(const char* label) {return 0;}


