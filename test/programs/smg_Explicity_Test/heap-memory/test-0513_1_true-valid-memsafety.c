extern void __VERIFIER_error() __attribute__ ((__noreturn__));

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
    while (c1 < 2 && __VERIFIER_nondet_int()) {
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

    if (!data)
        return 0;

    // merge subsequent pairs
    while (data->next != data) {

        item = data->data;
        item2 = data->next->data;

        lItem = data->next;
        data->next = lItem->next;
        free(lItem);

        struct DItem** dst = &data->data;
  
        while (item && item2) {

            if (item->value > item2->value) {
                *dst = item;
                item = item->next;            
                
            } else {
                *dst = item2;
                item2 = item2->next;
            }

            dst = &(*dst)->next;
        }

        if (item) {
            *dst = item;
            item = NULL;
        } else if (item2) {
            *dst = item2;
            item2 = NULL;
        }

        dst = NULL;
        data = data->next;
    }

    // release the list
    item = data->data;

    free(data);

    while (item) {
        item2 = item;
        item = item->next;

        if(item2->value > 1 && item2->value < 15 ) {
          free(item2);
        }
    }

    return 0;
}
