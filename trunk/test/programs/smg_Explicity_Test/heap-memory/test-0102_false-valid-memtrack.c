extern void __VERIFIER_error() __attribute__ ((__noreturn__));

/*
 * This source code is licensed under the GPL license, see License.GPLv2.txt
 * for details.  The list implementation is taken from the Linux kernel.
 */

#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct list_head {
    struct list_head *next, *prev;
};

#define LIST_HEAD_INIT(name) { &(name), &(name) }

#define LIST_HEAD(name) \
    struct list_head name = LIST_HEAD_INIT(name)

#define list_entry(ptr, type, member) \
	((type *)((char *)(ptr)-(unsigned long)(&((type *)0)->member)))

static inline void __list_add(struct list_head *new,
                              struct list_head *prev,
                              struct list_head *next)
{
    next->prev = new;
    new->next = next;
    new->prev = prev;
    prev->next = new;
}

static inline void list_add_tail(struct list_head *new, struct list_head *head)
{
    __list_add(new, head->prev, head);
}

struct top_list {
    struct list_head    link;
    struct list_head    sub1;
    struct list_head    sub2;
};

struct sub_list {
    int                 number;
    struct list_head    link;
};

void destroy_sub(struct list_head *head)
{
    struct sub_list *now = list_entry(head->next, struct sub_list, link);

    while (&now->link != (head)) {
        struct sub_list *next = list_entry(now->link.next, struct sub_list, link);

        free(now);
        now = next;
    }
}

void destroy_top(struct list_head *head)
{
    struct top_list *now = list_entry(head->next, struct top_list, link);

    while (&now->link != (head)) {
        struct top_list *next = list_entry(now->link.next, struct top_list, link);

        destroy_sub(&now->sub1);
        // Oops, we forgot to destroy &now->sub2...
        // Please point us to this line, so that we can fix it!
#if 0
        destroy_sub(&now->sub2);
#endif
        free(now);
        now = next;
    }
}

void insert_sub(struct list_head *head)
{
    struct sub_list *sub = malloc(sizeof(*sub));
    if (!sub)
        abort();

    sub->number = 0;

    list_add_tail(&sub->link, head);
}

void create_sub_list(struct list_head *sub)
{
    sub->prev = sub;
    sub->next = sub;

    int c = 0;   

    do {
        insert_sub(sub);
         c++;
    } while (c < 3);
}

void insert_top(struct list_head *head)
{
    struct top_list *top = malloc(sizeof(*top));
    if (!top)
        abort();

    create_sub_list(&top->sub1);
    create_sub_list(&top->sub2);

    list_add_tail(&top->link, head);
}

void create_top(struct list_head *top)
{
    int c = 0;

    do {
        insert_top(top);
	c = c + 1;
    } while (c < 3 && __VERIFIER_nondet_int());
}

int main()
{
    LIST_HEAD(top);

    create_top(&top);

    destroy_top(&top);

    return 0;
}
