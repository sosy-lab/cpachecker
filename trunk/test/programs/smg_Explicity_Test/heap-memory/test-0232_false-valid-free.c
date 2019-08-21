extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct item {
    struct item *next;
    struct item *data;
};

static void append(struct item **plist)
{
    struct item *item = malloc(sizeof *item);
    item->next = *plist;

    // shared data
    item->data = (item->next)
        ? item->next->data
        : malloc(sizeof *item);

    *plist = item;
}

int main()
{
    struct item *list = NULL;

    int c = 0;

    // create a long singly-linked list
    do { 
         c++;
        append(&list);
   } while (c < 3 && __VERIFIER_nondet_int());

    // remove the first item
    if (list) {
        struct item *next = list->next;

        // free shared data
        free(list->data);

        free(list);
        list = next;
    }

    // shared data is already freed when entering the loop

    while (list) {
        struct item *next = list->next;
        if (!next)
            // this causes a double free
            free(list->data);

        free(list);
        list = next;
    }

    return 0;
}
