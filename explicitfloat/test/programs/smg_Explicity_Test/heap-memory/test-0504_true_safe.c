#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct T {
    struct T* next;
    struct T* prev;
    int data;
};

struct T2 {
    struct T head;
    struct T2* next;
    struct T2* prev;
};

int main() {
    struct T2* first = NULL;
    struct T2* last = NULL;
    
    int c1 = 0;
    int c2 = 0;

    while (c1 < 3 && __VERIFIER_nondet_int()) {
        c1++;

        struct T2* x = malloc(sizeof(struct T2));
        if (!x)
            abort();

	  __VERIFIER_BUILTIN_PLOT("C1");
            
        x->next = NULL;
        x->prev = NULL;
        x->head.next = &x->head;
        x->head.prev = &x->head;
        x->head.data = c1 * 2;

        __VERIFIER_BUILTIN_PLOT("C2");
        
        struct T* y = NULL;

	  __VERIFIER_BUILTIN_PLOT("C3");
        
        while (c2 < 2 && __VERIFIER_nondet_int()) {
            c2++;
            y = malloc(sizeof(struct T));
            if (!y)
                abort();
                
            y->next = x->head.next;
            y->next->prev = y;
            y->prev = &x->head;
            y->data = c2*c1;
            x->head.next = y;
            y = NULL;
        }
        
        c2 = 0;

	  __VERIFIER_BUILTIN_PLOT("C4");

        if (!first) {
            first = x;
            last = x;
	       __VERIFIER_BUILTIN_PLOT("D1");
        } else {
            last->next = x;
            x->prev = last;
            last = x;
		__VERIFIER_BUILTIN_PLOT("E1");
        }
    }

    __VERIFIER_BUILTIN_PLOT("C5");

    while (c1 > 0) {

	   c1--;

        struct T2* x = first;
        first = first->next;

	__VERIFIER_BUILTIN_PLOT("C6");

        struct T* y = x->head.next;

     __VERIFIER_BUILTIN_PLOT("C7");

        while (y != &x->head) {
            struct T* z = y;
            y = y->next;

	__VERIFIER_BUILTIN_PLOT("C8");
            
            if(z->data < 9) {
              free(z);
            }
     __VERIFIER_BUILTIN_PLOT("C9");
        }
	__VERIFIER_BUILTIN_PLOT("C10");
        free(x);
    }

	__VERIFIER_BUILTIN_PLOT("C11");

    return 0;
}
