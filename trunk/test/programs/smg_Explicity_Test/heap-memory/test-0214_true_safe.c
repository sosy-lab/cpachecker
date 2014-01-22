#include <stdlib.h>
#include <string.h>

extern int __VERIFIER_nondet_int(void);

int i = 0;

typedef void *list_t[2];
typedef list_t *list_p;
typedef enum {
    LIST_BEG,
    LIST_END
} end_point_t;

typedef void *item_t[2];
typedef item_t *item_p;
typedef enum {
    ITEM_PREV,
    ITEM_NEXT
} direction_t;

typedef struct {
    item_t head;
    char text[0x100 + /* terminating zero */ 1];
} *user_item_p;

int is_empty(list_p list)
{

	__VERIFIER_BUILTIN_PLOT("e1");

    int no_beg = (int)!(*list)[LIST_BEG];
    int no_end = (int)!(*list)[LIST_END];

__VERIFIER_BUILTIN_PLOT("e2");

    if (no_beg != no_end || i > 10)
        /* unreachable */
        free(list);

__VERIFIER_BUILTIN_PLOT("e3");

    return no_beg;
}

void abort() {
	_EXIT: goto _EXIT;
}

item_p create_item(end_point_t at, item_p link)
{
    user_item_p item = malloc(sizeof *item);

	__VERIFIER_BUILTIN_PLOT("c1");

    if (!item)
        abort();

__VERIFIER_BUILTIN_PLOT("c2");

    direction_t term_field, link_field;

__VERIFIER_BUILTIN_PLOT("c3");

    switch (at) {
        case LIST_BEG:
            link_field = ITEM_NEXT;
            term_field = ITEM_PREV;
            break;

        case LIST_END:
            link_field = ITEM_PREV;
            term_field = ITEM_NEXT;
            break;
    }

__VERIFIER_BUILTIN_PLOT("c4");

    item->head[link_field] = link;
    item->head[term_field] = NULL;

__VERIFIER_BUILTIN_PLOT("c5");

    item->text[0] = '\0';
    item_p head = &item->head;

__VERIFIER_BUILTIN_PLOT("c6");

    if (link)
        (*link)[term_field] = head;

__VERIFIER_BUILTIN_PLOT("c7");
 
    return head;
}

void append_one(list_p list, end_point_t to)
{

__VERIFIER_BUILTIN_PLOT("a1");

    item_p item = create_item(to, (*list)[to]);

__VERIFIER_BUILTIN_PLOT("a2");

    (*list)[to] = item;

__VERIFIER_BUILTIN_PLOT("a3");

    if (NULL == (*list)[LIST_BEG])
        (*list)[LIST_BEG] = item;

__VERIFIER_BUILTIN_PLOT("a4");

    if (NULL == (*list)[LIST_END])
        (*list)[LIST_END] = item;

__VERIFIER_BUILTIN_PLOT("a5");
}

void remove_one(list_p list, end_point_t from)
{

__VERIFIER_BUILTIN_PLOT("r1");

    if (is_empty(list))
        /* list empty, nothing to remove */
        return;

__VERIFIER_BUILTIN_PLOT("r2");

    if ((*list)[LIST_BEG] == (*list)[LIST_END]) {
        free((*list)[LIST_BEG]);

__VERIFIER_BUILTIN_PLOT("r3");

        memset(*list, 0, sizeof *list);

__VERIFIER_BUILTIN_PLOT("r4");

        return;
    }

    const direction_t next_field = (LIST_BEG == from) ? ITEM_NEXT : ITEM_PREV;
    const direction_t term_field = (LIST_END == from) ? ITEM_NEXT : ITEM_PREV;

    item_p item = (*list)[from];
    item_p next = (*item)[next_field];
    (*next)[term_field] = NULL;
    (*list)[from] = next;
    free(item);
}

end_point_t rand_end_point(void)
{
    if (i - 3 || i - 2)
        return LIST_BEG;
    else
        return LIST_END;
}

int main()
{
    static list_t list;

    int y = 0;
    int z = 0;
    int length = 0;

    while (i < 2 && __VERIFIER_nondet_int()) {
    
        while (y < 2 && __VERIFIER_nondet_int()) {
            append_one(&list, rand_end_point());
            y++;
            length++;
            }

        while (z < 3 && __VERIFIER_nondet_int()){
            remove_one(&list, rand_end_point());
            z++;
            
            if(length > 0) {
               length--;
            }
        }
            
        y = 0;
        z = 0;
        i++;
    }

__VERIFIER_BUILTIN_PLOT("m1");

    end_point_t end_point;
    direction_t direction;

    if (__VERIFIER_nondet_int()) {
        /* destroy the list from begin to end */
        end_point = LIST_BEG;
        direction = ITEM_NEXT;
    } else {
        /* destroy the list from end to begin */
        end_point = LIST_END;
        direction = ITEM_PREV;
    }    

   __VERIFIER_BUILTIN_PLOT("m2");


    /* now please destroy the list */
    item_p cursor = list[end_point];

   __VERIFIER_BUILTIN_PLOT("m3");


    while  (length > 0) {

  __VERIFIER_BUILTIN_PLOT("m4");

        item_p next = (*cursor)[direction];

__VERIFIER_BUILTIN_PLOT("m5");

        free(cursor);

__VERIFIER_BUILTIN_PLOT("m6");


        cursor = next;

__VERIFIER_BUILTIN_PLOT("m7");


        length--;

__VERIFIER_BUILTIN_PLOT("m8");


    }    

		
__VERIFIER_BUILTIN_PLOT("m9");


    return 0;
}
