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
	(ptr)->next = (ptr); (ptr)->prev = (ptr); \
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

__VERIFIER_BUILTIN_PLOT("i1");

    ___SL_ASSERT(head);

__VERIFIER_BUILTIN_PLOT("i2");

    ___SL_ASSERT(head->next != head);


__VERIFIER_BUILTIN_PLOT("i3");

    ___SL_ASSERT(head->prev != head);

__VERIFIER_BUILTIN_PLOT("i4");

    // go one step backward
    head = head->prev;

__VERIFIER_BUILTIN_PLOT("i5");

    ___SL_ASSERT(head);

__VERIFIER_BUILTIN_PLOT("i6");
    ___SL_ASSERT(head->next != head);

__VERIFIER_BUILTIN_PLOT("i7");
    ___SL_ASSERT(head->prev != head);

__VERIFIER_BUILTIN_PLOT("i8");


    // resolve root
    const struct node *node = list_entry(head, struct node, linkage);

__VERIFIER_BUILTIN_PLOT("i9");

    ___SL_ASSERT(node);

__VERIFIER_BUILTIN_PLOT("i10");

    ___SL_ASSERT(node->nested.next == &node->nested);


__VERIFIER_BUILTIN_PLOT("i11");

    ___SL_ASSERT(node->nested.prev == &node->nested);

__VERIFIER_BUILTIN_PLOT("i12");

    ___SL_ASSERT(node->nested.next != &node->linkage);

__VERIFIER_BUILTIN_PLOT("i13");

    ___SL_ASSERT(node->nested.prev != &node->linkage);

__VERIFIER_BUILTIN_PLOT("i14");


    // check some properties
    ___SL_ASSERT(node != (const struct node *)head);

__VERIFIER_BUILTIN_PLOT("i15");

    ___SL_ASSERT(node != (const struct node *)&node->linkage);

__VERIFIER_BUILTIN_PLOT("i16");

    ___SL_ASSERT(node == (const struct node *)&node->value);

__VERIFIER_BUILTIN_PLOT("i17");

    ___SL_ASSERT(head == node->linkage.next->prev);

__VERIFIER_BUILTIN_PLOT("i18");

    ___SL_ASSERT(head == node->linkage.prev->next);

__VERIFIER_BUILTIN_PLOT("i19");

    // check traversal
    for (head = head->next; &node->linkage != head; head = head->next);
    ___SL_ASSERT(list_entry(head, struct node, linkage) == node);


__VERIFIER_BUILTIN_PLOT("i20");

}

static inline void __list_add(struct list_head *new,
			      struct list_head *prev,
			      struct list_head *next)
{


__VERIFIER_BUILTIN_PLOT("a0");
	next->prev = new;
__VERIFIER_BUILTIN_PLOT("a1");
	new->next = next;
__VERIFIER_BUILTIN_PLOT("a2");
	new->prev = prev;
__VERIFIER_BUILTIN_PLOT("a3");
	prev->next = new;
__VERIFIER_BUILTIN_PLOT("a4");
}

static inline void __list_del(struct list_head *prev, struct list_head *next)
{
	next->prev = prev;
	prev->next = next;
}

static inline void list_add(struct list_head *new, struct list_head *head)
{

__VERIFIER_BUILTIN_PLOT("aa0");

	__list_add(new, head, head->next);

__VERIFIER_BUILTIN_PLOT("aa1");

}

static inline void list_move(struct list_head *list, struct list_head *head)
{
        __list_del(list->prev, list->next);
        list_add(list, head);
}

static void gl_insert(int value)
{
    __VERIFIER_BUILTIN_PLOT("in1");

    struct node *node = malloc(sizeof *node);

__VERIFIER_BUILTIN_PLOT("in2");

    if (!node)
        abort();

__VERIFIER_BUILTIN_PLOT("in3");

    node->value = value;

__VERIFIER_BUILTIN_PLOT("in4");

    list_add(&node->linkage, &gl_list);

__VERIFIER_BUILTIN_PLOT("in5");

    INIT_LIST_HEAD(&node->nested);

__VERIFIER_BUILTIN_PLOT("in6");

}

static void gl_read()
{

    int c = 0;
    int value = 0;

__VERIFIER_BUILTIN_PLOT("r0");

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

	__VERIFIER_BUILTIN_PLOT("r1");

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

__VERIFIER_BUILTIN_PLOT("m0");

    gl_read();

__VERIFIER_BUILTIN_PLOT("m1");

    inspect(&gl_list);

    gl_sort();
    inspect(&gl_list);

    gl_destroy();

    return 0;
}
