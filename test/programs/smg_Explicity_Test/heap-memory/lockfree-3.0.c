extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct cell {
    int data;
    struct cell* next;
};

struct cell *S;

int pc1 = 1;
int pc4 = 1;

void push()
{
    static struct cell *t1 = NULL;
    static struct cell *x1 = NULL;

    switch (pc1++) {
        case 1:
            x1 = malloc(sizeof(*x1));
            x1->data = 0;
            x1->next = NULL;
            return;

        case 2:
            x1->data = 4;
            return;

        case 3:
            t1 = S;
            return;

        case 4:
            x1->next = t1;
            return;

        case 5:
            if (S == t1)
                S = x1;
            else
                pc1 = 3;
            return;

        case 6:
            pc1 = 1;
            return;
    }
}

struct cell* garbage;

void pop()
{
    static struct cell *t4 = NULL;
    static struct cell *x4 = NULL;
    static int res4;

    switch (pc4++) {
        case 1:
            t4 = S;
            return;

        case 2:
            if(t4 == NULL)
                pc4 = 1;
            return;

        case 3:
            x4 = t4->next;
            return;

        case 4:
            if (S == t4)
                S = x4;
            else
                pc4 = 1;
            return;

        case 5:
            res4 = t4->data;
            t4->next = garbage;
            garbage = t4;
            pc4 = 1;
            return;
    }
}

int main()
{

    int c = 0;

    while ((S || 1 != pc1 || 1 != pc4) && c < 3) {
	c = c + 1;
        if (__VERIFIER_nondet_int())
            push();
        else
            pop();
    }

    while (garbage) {
        struct cell *next = garbage->next;
        free(garbage);
        garbage = next;
    }

    return !!garbage;
}
