#ifndef parserInterface_h
#define parserInterface_h

typedef unsigned UINT;

unsigned new_node(); 
void create_edge(unsigned source, unsigned target, const char* op);
void register_node(const char*, unsigned);
unsigned resolve_label(const char*);
void traverse_cfa(unsigned root, const char* function);

#endif
