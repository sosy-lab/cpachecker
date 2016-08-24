#include <assert.h>
#include <malloc.h>

//void *malloc(int);

int VERDICT_SAFE;
int CURRENTLY_SAFE;

struct list_head {
	struct list_head *prev, *next;
	int inserted;
};

static void list_add(struct list_head *new, struct list_head *head) {
  assert(new->inserted==0);
  new->inserted = 1;
}

static void list_del(struct list_head *entry) {
  assert(entry->inserted==1);
  entry->inserted = 0;
}

static struct list_head head;

int main() {
  struct list_head *dev;
  dev = malloc(sizeof(*dev));
  if(dev!=NULL) {
	  dev->inserted=0;
	  list_add(dev, &head);
	  list_del(dev);
	  list_add(dev, &head);
  }
  return 0;
}

