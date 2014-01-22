#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>

extern int __VERIFIER_nondet_int(void);

static void fail(void) {
ERROR:
    goto ERROR;
}

#define ___SL_ASSERT(cond) do {     \
    if (!(cond))                    \
        fail();                     \
} while (0)

struct list_head {
	struct list_head *next, *prev;
};

#define LIST_HEAD_INIT(name) { &(name), &(name) }

#define LIST_HEAD(name) \
	struct list_head name = LIST_HEAD_INIT(name)

#define INIT_LIST_HEAD(ptr) do { \
	(ptr)->next = (ptr); (ptr)->next = (ptr); \
} while (0)

#define list_entry(ptr, type, member) \
	((type *)((char *)(ptr)-(unsigned long)(&((type *)0)->member)))

struct node {
    int                         value;
    struct list_head            linkage;
    struct list_head            nested;
};

LIST_HEAD(gl_list);

static void inspect(const struct list_head *head)
{
    // check the head
    ___SL_ASSERT(head);
    ___SL_ASSERT(head->next != head);
    ___SL_ASSERT(head->prev != head);

    // go one step backward
    head = head->prev;
    ___SL_ASSERT(head);
    ___SL_ASSERT(head->next != head);
    ___SL_ASSERT(head->prev != head);

    // resolve root
    const struct node *node = list_entry(head, struct node, linkage);
    ___SL_ASSERT(node);
    ___SL_ASSERT(node->nested.next == &node->nested);
    ___SL_ASSERT(node->nested.prev == &node->nested);
    ___SL_ASSERT(node->nested.next != &node->linkage);
    ___SL_ASSERT(node->nested.prev != &node->linkage);

    // check some properties
    ___SL_ASSERT(node != (const struct node *)head);
    ___SL_ASSERT(node != (const struct node *)&node->linkage);
    ___SL_ASSERT(node == (const struct node *)&node->value);
    ___SL_ASSERT(head == node->linkage.next->prev);
    ___SL_ASSERT(head == node->linkage.prev->next);

    // check traversal
    for (head = head->next; &node->linkage != head; head = head->next);
    ___SL_ASSERT(list_entry(head, struct node, linkage) == node);
}

static inline void __list_add(struct list_head *new,
			      struct list_head *prev,
			      struct list_head *next)
{
	next->prev = new;
	new->next = next;
	new->prev = prev;
	prev->next = new;
}

static inline void __list_del(struct list_head *prev, struct list_head *next)
{
	next->prev = prev;
	prev->next = next;
}

static inline void list_add(struct list_head *new, struct list_head *head)
{
	__list_add(new, head, head->next);
}

static inline void list_move(struct list_head *list, struct list_head *head)
{
        __list_del(list->prev, list->next);
        list_add(list, head);
}

static void gl_insert(int value)
{
    struct node *node = malloc(sizeof *node);
    if (!node)
        abort();

    node->value = value;
    list_add(&node->linkage, &gl_list);
    INIT_LIST_HEAD(&node->nested);
}

static void gl_read()
{

    int c = 1;
    int value = 0;

    do {
	c++;

	if(c == 1 || c == 2) {
	value = value - c + 7*value;
	} else if(__VERIFIER_nondet_int)  {
	value = value + c - 7*value;
	} else {
	value = value/2;
	}

        gl_insert(value);
    }
    while (__VERIFIER_nondet_int() && c < 5);
}

static void gl_destroy()
{
    struct list_head *next;
    while (&gl_list != (next = gl_list.next)) {
        gl_list.next = next->next;
        free(list_entry(next, struct node, linkage));
	}
}

static int val_from_node(struct list_head *head) {
    struct node *entry = list_entry(head, struct node, linkage);
    return entry->value;
}

static bool gl_sort_pass()
{
    bool any_change = false;

    struct list_head *pos0 = gl_list.next;
    struct list_head *pos1;
    while (&gl_list != (pos1 = pos0->next)) {
        const int val0 = val_from_node(pos0);
        const int val1 = val_from_node(pos1);
        if (val0 <= val1) {
            // jump to next
            pos0 = pos1;
            continue;
        }

        any_change = true;
        list_move(pos0, pos1);
    }

    return any_change;
}

static void gl_sort()
{
    while (gl_sort_pass())
        ;
}

int main()
{
    gl_read();
    inspect(&gl_list);

    gl_sort();
    inspect(&gl_list);

    gl_destroy();

    return 0;
}
