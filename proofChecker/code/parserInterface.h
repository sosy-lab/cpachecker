#ifndef parserInterface_h
#define parserInterface_h

typedef unsigned UINT;

unsigned get_init_state(); 
void assert_equal(unsigned, unsigned);
unsigned compute_successor(unsigned, const char*);
unsigned propagate_value(const char*, unsigned);

#endif
