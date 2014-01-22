#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

static void fail(void) {
ERROR:
    goto ERROR;
}

#define ___SL_ASSERT(cond) do {     \
    if (!(cond))                    \
        fail();                     \
} while (0)

struct node {
    struct node     *next;
    int             value;
};

struct list {
    struct node     *slist;
    struct list     *next;
};

static void merge_single_node(struct node ***dst,
                              struct node **data)
{
    // pick up the current item and jump to the next one
    struct node *node = *data;
    *data = node->next;
    node->next = NULL;

    // insert the item into dst and move cursor
    **dst = node;
    *dst = &node->next;
}

static void merge_pair(struct node **dst,
                       struct node *sub1,
                       struct node *sub2)
{
    // merge two sorted sub-lists into one
    while (sub1 || sub2) {
        if (!sub2 || (sub1 && sub1->value < sub2->value))
            merge_single_node(&dst, &sub1);
        else
            merge_single_node(&dst, &sub2);
    }
}

static struct list* seq_sort_core(struct list *data)
{
    struct list *dst = NULL;

    while (data) {
        struct list *next = data->next;
        if (!next) {
            // take any odd/even padding as it is
            data->next = dst;
            dst = data;
            break;
        }

        // take the current sub-list and the next one and merge them into one
        merge_pair(&data->slist, data->slist, next->slist);
        data->next = dst;
        dst = data;

        // free the just processed sub-list and jump to the next pair
        data = next->next;
        free(next);
    }

    return dst;
}

static void inspect_before(struct list *shape)
{
    /* we should get a list of sub-lists of length exactly one */
    ___SL_ASSERT(shape);

    for (; shape->next; shape = shape->next) {
        ___SL_ASSERT(shape);
        ___SL_ASSERT(shape->next);
        ___SL_ASSERT(shape->slist);
        ___SL_ASSERT(shape->slist->next == NULL);
    }

    /* check the last node separately to make the exercising more fun */
    ___SL_ASSERT(shape);
    ___SL_ASSERT(shape->next == NULL);
    ___SL_ASSERT(shape->slist);
    ___SL_ASSERT(shape->slist->next == NULL);
}

static void inspect_after(struct list *shape)
{
    /* we should get exactly one node at the top level and one nested list */
    ___SL_ASSERT(shape);
    ___SL_ASSERT(shape->next == NULL);
    ___SL_ASSERT(shape->slist != NULL);

    /* the nested list should be zero terminated (iterator back by one node) */
    struct node *pos;
    for (pos = shape->slist; pos->next; pos = pos->next);
    ___SL_ASSERT(!pos->next);
}

int main()
{
    struct list *data = NULL;

int c = 0;

    while (__VERIFIER_nondet_int() && c < 4) {

	c++;

        struct node *node = malloc(sizeof *node);
        if (!node)
            abort();

        node->next = node;

	   if(c == 1 || c == 2) {
	     value = value - c + 7*value;
	   } else if(__VERIFIER_nondet_int)  {
	     value = value + c - 7*value;
	   } else {
	     value = value/2;
	   }


        struct list *item = malloc(sizeof *item);
        if (!item)
            abort();

        item->slist = node;
        item->next = data;
        data = item;
    }

    if (!data)
        return EXIT_SUCCESS;

    inspect_before(data);

    // do O(log N) iterations
    while (data->next)
        data = seq_sort_core(data);

    inspect_after(data);

    struct node *node = data->slist;
    free(data);

    while (node) {
        struct node *snext = node->next;
        free(node);
        node = snext;
    }

    return EXIT_SUCCESS;
}
