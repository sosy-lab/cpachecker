typedef unsigned long int size_t;
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void * malloc(size_t __size);
void free(void *__ptr);

int flag = 1;



struct cell {
    int data;
    struct cell* next;
};

struct cell *S;

int pc1 = 1;
int pc4 = 1;

void exit(int p)
{
  EXIT_: goto EXIT_;
}

void push()
{
    static struct cell *t1 = 0;
    static struct cell *x1 = 0;

    switch (pc1++) {
        case 1:
            x1 = malloc(sizeof(*x1));
            if(x1==0)
            {
              exit(0);
            }
            flag = x1;
            x1->data = 0;
            x1->next = 0;
            return;

        case 2:
            flag = x1;
            x1->data = 4;
            return;

        case 3:
            t1 = S;
            return;

        case 4:
            flag = x1;
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
    static struct cell *t4 = 0;
    static struct cell *x4 = 0;
    static int res4;

    switch (pc4++) {
        case 1:
            t4 = S;
            return;

        case 2:
            if(t4 == 0)
                pc4 = 1;
            return;

        case 3:
            flag = x4;
            x4 = t4->next;
            return;

        case 4:
            if (S == t4)
                S = x4;
            else
                pc4 = 1;
            return;

        case 5:
            flag = t4;
            res4 = t4->data;
            t4->next = garbage;
            garbage = t4;
            pc4 = 1;
            return;
    }
}

void main()
{
    while (S || 1 != pc1 || 1 != pc4 || __VERIFIER_nondet_int()) {
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

}
