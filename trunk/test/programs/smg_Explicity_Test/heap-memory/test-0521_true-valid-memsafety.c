extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct node {
    int             value;
    struct node     *next;
};

struct list {
    struct node     *slist;
    struct list     *next;
};

struct iterator {
    struct list     *list;
    struct node     *node;
};

int main()
{
    struct list *data = NULL;
    int c = 0;

    // ------------------------------------------------------------------------

    // seq_read(&data);
    // static void seq_read(struct list **data)
    { 
      while (c < 2 && __VERIFIER_nondet_int())
        // seq_insert(data, value);
        // static void seq_insert(struct list **data, int value)
        {
          struct node *node = malloc(sizeof *node);
          if (!node)
              abort();
          c++;
          node->next = NULL;
          node->value = __VERIFIER_nondet_int();

          struct list *item = malloc(sizeof *item);
          if (!item)
              abort();

          item->slist = node;
          item->next = data;
          data = item;
        }
    }

    // ------------------------------------------------------------------------

    // seq_write(data);
    // static void seq_write(struct list *data)
    { 
      struct iterator iter;
      
      // setup_iterator(&iter, data);
      // static void setup_iterator(struct iterator *iter, struct list *list)
      { 
        struct list *list = data;

        if ((iter.list = list))
          iter.node = list->slist;
      }

      struct node *node;

      // node = get_next(&iter)
      // static struct node* get_next(struct iterator *iter)
      {
        if (!iter.list)
          node = NULL;
        else {
          struct node *current = iter.node;
          if ((iter.node = current->next))
            node = current;
          else {
            if ((iter.list = iter.list->next))
              iter.node = iter.list->slist;

            node = current;
          }
        }
      }

      while ((node)) 
      {
      
        // node = get_next(&iter)
        // static struct node* get_next(struct iterator *iter)
        {
          if (!iter.list)
            node = NULL;
          else {
            struct node *current = iter.node;
            if ((iter.node = current->next))
              node = current;
            else {
              if ((iter.list = iter.list->next))
                iter.node = iter.list->slist;

              node = current;
            }
          }
        }
      }

    }

    // ------------------------------------------------------------------------
    // seq_sort(&data);
    // static void seq_sort(struct list **data)
    {
      struct list *list = data;

      // do O(log N) iterations
      while (list && list->next) {
        // list = seq_sort_core(list);
        // static struct list* seq_sort_core(struct list *data)

        struct list *dst = NULL;

        while (list) {
          struct list *next = list->next;
          if (!next) {
            // take any odd/even padding as it is
            list->next = dst;
            dst = list;
            break;
          }

          // ...........................................................................

          // take the current sub-list and the next one and merge them into one
          // merge_pair(&list->slist, list->slist, next->slist);
          // static void merge_pair(struct node **dst,
          //                        struct node *sub1,
          //                        struct node *sub2)
          { struct node **dst = &list->slist;
            struct node *sub1 = list->slist;
            struct node *sub2 = next->slist;
        
            // merge two sorted sub-lists into one
            while (sub1 || sub2) {
              // if (!sub2 || (sub1 && sub1->value < sub2->value)) 
              //   merge_single_node(&dst, &sub1);
              //  else
              //   merge_single_node(&dst, &sub2);

              // static void merge_single_node(struct node ***dst,
              //                               struct node **data)
              struct node ***pdst = &dst;
              struct node **pdata;
//              if (!sub2 || (sub1 && sub1->value < sub2->value)) 
              if (!sub2 || (sub1 && __VERIFIER_nondet_int())) 
                pdata = &sub1;
              else
                pdata = &sub2;

              // merge_single_node(&dst, &sub1);

              // pick up the current item and jump to the next one
              struct node *node = *pdata;
              *pdata = node->next;
              node->next = NULL;

              // insert the item into dst and move cursor
              **pdst = node;
              *pdst = &node->next;
            }
          }

          // ...........................................................................

          list->next = dst;
          dst = list;

          // free the just processed sub-list and jump to the next pair
          list = next->next;
          free(next);
        }

        list = dst;
      }

      data = list;
    }

    // ------------------------------------------------------------------------
    // seq_write(data);
    // static void seq_write(struct list *data)
    { 
      struct iterator iter;
      
      // setup_iterator(&iter, data);
      // static void setup_iterator(struct iterator *iter, struct list *list)
      { 
        struct list *list = data;

        if ((iter.list = list))
          iter.node = list->slist;
      }

      struct node *node;

      // node = get_next(&iter)
      // static struct node* get_next(struct iterator *iter)
      {
        if (!iter.list)
          node = NULL;
        else {
          struct node *current = iter.node;
          if ((iter.node = current->next))
            node = current;
          else {
            if ((iter.list = iter.list->next))
              iter.node = iter.list->slist;

            node = current;
          }
        }
      }

      while ((node)) 
      {
      
        // node = get_next(&iter)
        // static struct node* get_next(struct iterator *iter)
        {
          if (!iter.list)
            node = NULL;
          else {
            struct node *current = iter.node;
            if ((iter.node = current->next))
              node = current;
            else {
              if ((iter.list = iter.list->next))
                iter.node = iter.list->slist;

              node = current;
            }
          }
        }
      }

    }

    // ------------------------------------------------------------------------

    // seq_destroy(data);
    // static void seq_destroy(struct list *data)
    {
      while (data) {
        struct list *next = data->next;

        struct node *node = data->slist;
        while (node) {
            struct node *snext = node->next;
            free(node);
            node = snext;
        }

        free(data);
        data = next;
      }
    }

    // ------------------------------------------------------------------------

    return 0;
}
