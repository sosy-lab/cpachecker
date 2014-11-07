extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>
#include <string.h>

static void *calloc_model(size_t nmemb, size_t size) {
    void *ptr = malloc(nmemb * size);
    return memset(ptr, 0, nmemb * size);
}

extern int __VERIFIER_nondet_int(void);

struct L2 {
    void        *proto;
    struct L2   *next;
};

struct L1 {
    struct L1   *next;
    struct L2   *l2;
};

static void l2_insert(struct L2 **list)
{
    struct L2 *item = calloc_model(1U, sizeof *item);
    if (!item)
        abort();

    item->proto = malloc(119U);
    if (!item->proto)
        abort();

    item->next = *list;
    *list = item;
}

static void l2_destroy(struct L2 *list)
{
    do {
        struct L2 *next = list->next;
        free(list->proto);
        free(list);
        list = next;
    }
    while (list);
}

static void l1_insert(struct L1 **list)
{
    struct L1 *item = calloc_model(1U, sizeof *item);
    if (!item)
        abort();

    int c = 3;

    do {
        c++;
        l2_insert(&item->l2);
   } while (c < 3);

    item->next = *list;
    *list = item;
}

int main()
{
    static struct L1 *list;
    int c = 0;


    do {
        l1_insert(&list);
        c++;
     }
    while ( c < 3 && __VERIFIER_nondet_int());

    do {
        struct L1 *next = list->next;

        l2_destroy(list->l2);

        free(list);
        list = next;
    }
    while (list);
}
