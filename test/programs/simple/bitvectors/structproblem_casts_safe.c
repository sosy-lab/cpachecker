#include <stdlib.h>

//typedef unsigned int size_t;
//extern  __attribute__((__nothrow__)) void *malloc(size_t __size )  __attribute__((__malloc__)) ;

struct node {
    struct node     *next;
    int             value;
};

struct list {
   struct node *slist ;
   struct list *next ;
};


int main() {
  int i = 3;
  int j = 4;
  void* tmp = malloc(sizeof(int*) * 2);
  int* ptrI = &i;
  int* ptrj = &j;
  //int** tmphelper = (int**)tmp;
  int* tmpfst =  *((int**)tmp);
  int* tmpsnd = *(((int**)tmp) + 1);
  tmpfst = ptrI;
  tmpsnd = ptrj;
  //int** other = (int**)tmp;
  int* snd = *((int**)tmp + 1);
  if (*tmpfst == 3 && *tmpsnd == 4) { // && *snd == 4){
    return 0;
  }

 ERROR:
  goto ERROR;

  return 1;
}

