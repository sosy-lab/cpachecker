#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct DItem {
    struct DItem* next;
    int value;
};

struct TLItem {
    struct TLItem* next;
    struct DItem* data;	
};

int main() {
    struct TLItem* data = NULL;
    struct DItem* item, * item2;
    struct TLItem *lItem;
    
    int c1 = 0;

    // fill top level list with single data items
    while (c1 < 5 && __VERIFIER_nondet_int()) {
        c1++;
        
        item = malloc(sizeof *item);
        if (!item)
            abort();

        item->next = NULL;
        
        if(c1 < 2)
        item->value = 2;
        
        if(c1 < 4)
        item->value = c1 + 2;
        
        if(c1 < 6)
        item->value = 2 * c1 + 4;
        
        lItem = malloc(sizeof *lItem);
        
        if (data) {
            lItem->next = data->next;
            data->next = lItem;
        } else {
            lItem->next = lItem;
            data = lItem;
        }

        lItem->data = item;

        item = NULL;
        lItem = NULL;
    }

	__VERIFIER_BUILTIN_PLOT("C1");

    if (!data)
        return 0;

__VERIFIER_BUILTIN_PLOT("C2");

    // merge subsequent pairs
    while (data->next != data) {

	__VERIFIER_BUILTIN_PLOT("C3");

        item = data->data;
        item2 = data->next->data;

__VERIFIER_BUILTIN_PLOT("C4");

        lItem = data->next;
        data->next = lItem->next;
        free(lItem);

        struct DItem** dst = &data->data;

__VERIFIER_BUILTIN_PLOT("C5");
  
        while (item && item2) {

__VERIFIER_BUILTIN_PLOT("C6");

            if (item->value > item2->value) {
                *dst = item;
                item = item->next;            

__VERIFIER_BUILTIN_PLOT("D1");
                
            } else {
                *dst = item2;
                item2 = item2->next;
__VERIFIER_BUILTIN_PLOT("E1");
            }

            dst = &(*dst)->next;
__VERIFIER_BUILTIN_PLOT("C7");
        }

        if (item) {
            *dst = item;
            item = NULL;
        } else if (item2) {
            *dst = item2;
            item2 = NULL;
        }

__VERIFIER_BUILTIN_PLOT("C8");

        dst = NULL;
        data = data->next;
    }

__VERIFIER_BUILTIN_PLOT("C9");

    // release the list
    item = data->data;

    free(data);

__VERIFIER_BUILTIN_PLOT("C10");

    while (item) {

__VERIFIER_BUILTIN_PLOT("C11");

        item2 = item;
        item = item->next;

__VERIFIER_BUILTIN_PLOT("C12");

        if(item2->value > 1 && item2->value < 15 ) {

__VERIFIER_BUILTIN_PLOT("G1");

          free(item2);

__VERIFIER_BUILTIN_PLOT("G2");

        }

__VERIFIER_BUILTIN_PLOT("C13");

    }

__VERIFIER_BUILTIN_PLOT("XX");

    return 0;
}
