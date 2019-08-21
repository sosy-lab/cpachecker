#include <assert.h>
#include <malloc.h>

#ifdef BLAST_AUTO_1
/* double add */
int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;
#else
int VERDICT_SAFE;
int CURRENTLY_SAFE;
#endif

int int_nondet(void);

void * guard_malloc_counter  = 0;

void * __getMemory(int size)
{
  assert(size > 0);
  guard_malloc_counter++;
  if (!int_nondet())
	return 0;					
  return (void *) guard_malloc_counter;
}

void *my_malloc(int size) {
  return __getMemory(size);	
}


struct list_head {
	struct list_head *prev, *next;
};

struct list_head *elem = NULL;

static void list_add(struct list_head *new, struct list_head *head) {
  assert(new!=elem);
  if(int_nondet())
	elem = new;
}

static void list_del(struct list_head *entry) {
  if(entry==elem)
	elem=NULL;
}

static struct list_head head;

int main() {
  struct list_head *dev1, *dev2;
  dev1 = my_malloc(10);//sizeof(*dev1));
  dev2 = my_malloc(10);//sizeof(*dev2));
  if(dev1!=NULL && dev2!=NULL) {	  
    list_add(dev2, &head);
    list_add(dev1, &head);
    list_del(dev2);
    list_add(dev2, &head);
#ifdef BLAST_AUTO_1
    //BUG:
    list_add(dev1, &head);
#endif
  }
  return 0;
}

