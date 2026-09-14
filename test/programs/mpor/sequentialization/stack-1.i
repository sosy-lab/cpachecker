// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: 2020 The ESBMC project
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "stack-1.c", 3, __extension__ __PRETTY_FUNCTION__); })); }
extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}

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

unsigned int __VERIFIER_nondet_uint();
static int top=0;
static unsigned int arr[(5)];
pthread_mutex_t m;
_Bool flag=(0);
void error(void)
{
  ERROR: {reach_error();abort();}
  return;
}
void inc_top(void)
{
  top++;
}
void dec_top(void)
{
  top--;
}
int get_top(void)
{
  return top;
}
int stack_empty(void)
{
  return (top==0) ? (1) : (0);
}
int push(unsigned int *stack, int x)
{
  if (top==(5))
  {
    printf("stack overflow\n");
    return (-1);
  }
  else
  {
    stack[get_top()] = x;
    inc_top();
  }
  return 0;
}
int pop(unsigned int *stack)
{
  if (top==0)
  {
    printf("stack underflow\n");
    return (-2);
  }
  else
  {
    dec_top();
    return stack[get_top()];
  }
  return 0;
}
void *t1(void *arg)
{
  int i;
  unsigned int tmp;
  for(i=0; i<(5); i++)
  {
    pthread_mutex_lock(&m);
    tmp = __VERIFIER_nondet_uint();
    assume_abort_if_not(tmp < (5));
    if ((push(arr,tmp)==(-1)))
      error();
    pthread_mutex_unlock(&m);
  }
  return 0;
}
void *t2(void *arg)
{
  int i;
  for(i=0; i<(5); i++)
  {
    pthread_mutex_lock(&m);
    if (top>0)
    {
      if ((pop(arr)==(-2)))
        error();
    }
    pthread_mutex_unlock(&m);
  }
  return 0;
}
int main(void)
{
  pthread_t id1, id2;
  pthread_mutex_init(&m, 0);
  pthread_create(&id1, ((void *)0), t1, ((void *)0));
  pthread_create(&id2, ((void *)0), t2, ((void *)0));
  pthread_join(id1, ((void *)0));
  pthread_join(id2, ((void *)0));
  return 0;
}
