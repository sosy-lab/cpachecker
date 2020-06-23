// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

//Now the tool can't understand, that in 49 line there is a shared data

typedef int pthread_mutex_t;
typedef unsigned long int pthread_t;
typedef int pthread_attr_t;
typedef int size_t;

extern void* malloc(size_t size);
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int pthread_create(pthread_t *thread_id , pthread_attr_t const   *attr , void *(*func)(void * ) ,
                          void *arg ) ;

struct point {
  int *x;
  int y;
};

struct line {
  struct point* start;
  struct point* finish;
};

int t;
pthread_mutex_t mutex;
struct line *L2;

extern void* malloc(int size);

struct point *allocPoint(int *m) {
  struct point *C = malloc(sizeof(struct point));
  *m = 10;
  C->x = m;
  C->y = 1;
  return C;
}

struct point* getStart(struct line *l) {
  struct point *r;
  if ( l != 0) {
    r = allocPoint(&t);
    l->start = r;
    return r;
  } else {
    return 0;
  }
}

int func() {
  int *a;
  int b;
  struct point* B;
   
  pthread_mutex_lock(&mutex);
  B = getStart(L2);
  pthread_mutex_unlock(&mutex);
  if (L2->finish != 0) {
      b++;
  }
  (*(L2->start->x))--;
}

void* prepare_line1(void* arg) {
	func();
}

void* prepare_line2(void* arg) {
	func();
}

int main() {
    pthread_t thread, thread2;
    L2 = malloc(sizeof(struct line));
	pthread_create(&thread, 0, &prepare_line1, 0);
	pthread_create(&thread2, 0, &prepare_line2, 0);
	
}
