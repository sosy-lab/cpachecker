#include <pthread.h>
#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

struct item {
  struct item *next;
  struct item *data;
};

static void append(struct item **plist) {
  struct item *elem = malloc(sizeof *elem);
  elem->next = *plist;
  elem->data = (elem->next) ? elem->next->data : malloc(sizeof *elem);
  *plist = elem;
}

void *build(void *plist) {
  struct item *list = (struct item *)plist;
  do
    append(&list);
  while (__VERIFIER_nondet_int());
  pthread_exit(NULL);
}

void *delete (void *plist) {
  struct item *list = (struct item *)plist;
  if (list) {
    struct item *next = list->next;
    free(list->data);
    free(list);
    list = next;
  }
  while (list) {
    struct item *next = list->next;
    free(list);
    list = next;
  }
  pthread_exit(NULL);
}

int main(int argc, char **argv) {
  pthread_t id1, id2;
  struct item *list = ((void *)0);
  pthread_create(&id1, NULL, build, list);
  pthread_join(id1, NULL);
  pthread_create(&id2, NULL, delete, list);
  pthread_join(id2, NULL);
  return 0;
}
