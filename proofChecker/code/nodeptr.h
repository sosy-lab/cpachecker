
#ifndef NODEPTR_H

#define NODEPTR_H


typedef struct NODEPTR_struct *NODEPTR;

#define NULLNODEPTR ((NODEPTR) 0)

#endif

#ifdef MONITOR

#define DAPTO_RESULTNODEPTR(n) DAPTO_RESULT_PTR(n)

#define DAPTO_ARGNODEPTR(n) DAPTO_ARG_PTR(n, NODEPTR)

#endif
