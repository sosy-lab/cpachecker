// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The ESBMC project
//
// SPDX-License-Identifier: Apache-2.0

// Original file:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/queue_longest.i?ref_type=tags

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "queue_longest.c", 4, __extension__ __PRETTY_FUNCTION__); })); }

typedef struct __pthread_internal_slist
{
  struct __pthread_internal_slist *__next;
} __pthread_slist_t;
struct __pthread_mutex_s
{
  int __lock ;
  unsigned int __count;
  int __owner;
  int __kind;
 
  unsigned int __nusers;
  __extension__ union
  {
    struct { short __espins; short __eelision; } __elision_data;
    __pthread_slist_t __list;
  };
 
};
typedef unsigned long int pthread_t;
typedef union
{
  char __size[4];
  int __align;
} pthread_mutexattr_t;
union pthread_attr_t
{
  char __size[36];
  long int __align;
};
typedef union pthread_attr_t pthread_attr_t;
typedef union
{
  struct __pthread_mutex_s __data;
  char __size[24];
  long int __align;
} pthread_mutex_t;

extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern int pthread_join (pthread_t __th, void **__thread_return);
extern int pthread_mutex_init (pthread_mutex_t *__mutex,
          const pthread_mutexattr_t *__mutexattr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_lock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_unlock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

typedef struct {
    int element[(800)];
    int head;
    int tail;
    int amount;
} QType;
pthread_mutex_t m;
int __VERIFIER_nondet_int();
int stored_elements[(800)];
_Bool enqueue_flag, dequeue_flag;
QType queue;
void init(QType *q)
{
  q->head=0;
  q->tail=0;
  q->amount=0;
}
int empty(QType * q)
{
  if (q->head == q->tail)
  {
    printf("queue is empty\n");
    return (-1);
  }
  else
    return 0;
}
int full(QType * q)
{
  if (q->amount == (800))
  {
 printf("queue is full\n");
 return (-2);
  }
  else
    return 0;
}
int enqueue(QType *q, int x)
{
  q->element[q->tail] = x;
  q->amount++;
  if (q->tail == (800))
  {
    q->tail = 1;
  }
  else
  {
    q->tail++;
  }
  return 0;
}
int dequeue(QType *q)
{
  int x;
  x = q->element[q->head];
  q->amount--;
  if (q->head == (800))
  {
    q->head = 1;
  }
  else
    q->head++;
  return x;
}
void *t1(void *arg)
{
  int value, i;
  pthread_mutex_lock(&m);
  value = __VERIFIER_nondet_int();
  if (enqueue(&queue,value)) {
    goto ERROR;
  }
  stored_elements[0]=value;
  if (empty(&queue)) {
    goto ERROR;
  }
  pthread_mutex_unlock(&m);
  for(i=0; i<((800)-1); i++)
  {
    pthread_mutex_lock(&m);
    if (enqueue_flag)
    {
      value = __VERIFIER_nondet_int();
      enqueue(&queue,value);
      stored_elements[i+1]=value;
      enqueue_flag=(0);
      dequeue_flag=(1);
    }
    pthread_mutex_unlock(&m);
  }
  return ((void *)0);
 ERROR:{reach_error();abort();}
}
void *t2(void *arg)
{
  int i;
  for(i=0; i<(800); i++)
  {
    pthread_mutex_lock(&m);
    if (dequeue_flag)
    {
      if (!dequeue(&queue)==stored_elements[i]) {
        ERROR:{reach_error();abort();}
      }
      dequeue_flag=(0);
      enqueue_flag=(1);
    }
    pthread_mutex_unlock(&m);
  }
  return ((void *)0);
}
int main(void)
{
  pthread_t id1, id2;
  enqueue_flag=(1);
  dequeue_flag=(0);
  init(&queue);
  if (!empty(&queue)==(-1)) {
    ERROR:{reach_error();abort();}
  }
  pthread_mutex_init(&m, 0);
  pthread_create(&id1, ((void *)0), t1, &queue);
  pthread_create(&id2, ((void *)0), t2, &queue);
  pthread_join(id1, ((void *)0));
  pthread_join(id2, ((void *)0));
  return 0;
}
