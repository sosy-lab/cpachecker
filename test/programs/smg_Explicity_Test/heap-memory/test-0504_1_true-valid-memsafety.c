extern void __VERIFIER_error() __attribute__ ((__noreturn__));

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

    while (c1 < 2 && __VERIFIER_nondet_int()) {
        c1++;

        struct T2* x = malloc(sizeof(struct T2));
        if (!x)
            abort();
            
        x->next = NULL;
        x->prev = NULL;
        x->head.next = &x->head;
        x->head.prev = &x->head;
        x->head.data = c1 * 2;
        
        struct T* y = NULL;
        
        while (c2 < 3 && __VERIFIER_nondet_int()) {
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

        if (!first) {
            first = x;
            last = x;
        } else {
            last->next = x;
            x->prev = last;
            last = x;
        }
    }

    while (c1 > 0) {
	c1--;
        struct T2* x = first;
        first = first->next;

        struct T* y = x->head.next;
        while (y != &x->head) {
            struct T* z = y;
            y = y->next;
            
            if(z->data < 16) {
              free(z);
            }
        }
        free(x);
    }

    return 0;
}
