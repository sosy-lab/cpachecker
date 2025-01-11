/*-----------------------------------------------------------------------------
 * mutex.c - Concurrent program using locking to access a shared variable
 *-----------------------------------------------------------------------------
 * Author: Frank Schüssele
 *   Date: 2023-07-11
 *---------------------------------------------------------------------------*/

/*-----------------------------------------------------------------------------
 * Declarations of POSIX thread data types
 *---------------------------------------------------------------------------*/
typedef unsigned long int pthread_t;

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

typedef union
{
  char __size[4];
  int __align;
} pthread_condattr_t;

typedef unsigned int pthread_key_t;

typedef int pthread_once_t;

typedef union pthread_attr_t
{
  char __size[36];
  long int __align;
} pthread_attr_t;

typedef union
{
  struct __pthread_mutex_s __data;
  char __size[24];
  long int __align;
} pthread_mutex_t;

typedef union
{
  char __size[4];
  int __align;
} pthread_mutexattr_t;

/*-----------------------------------------------------------------------------
 * Declarations of POSIX thread functions
 *---------------------------------------------------------------------------*/
extern int pthread_create (pthread_t *__restrict __newthread,
                           const pthread_attr_t *__restrict __attr,
                           void *(*__start_routine) (void *),
                           void *__restrict __arg)
    __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));

extern int pthread_mutex_init (pthread_mutex_t *__mutex,
                               const pthread_mutexattr_t *__mutexattr)
    __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));

extern int pthread_mutex_destroy (pthread_mutex_t *__mutex)
    __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));

extern int pthread_mutex_lock (pthread_mutex_t *__mutex)
    __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int pthread_mutex_unlock (pthread_mutex_t *__mutex)
    __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

/*-----------------------------------------------------------------------------
 * SV-Comp-specific function declarations
 *---------------------------------------------------------------------------*/
extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

extern void reach_error();

/*-----------------------------------------------------------------------------
 * Concurrent program using a POSIX thread mutex to implement locking
 *---------------------------------------------------------------------------*/
int used;
pthread_mutex_t m;

void* producer()
{
  while (1) {
    pthread_mutex_lock(&m);
    used++;
    used--;
    pthread_mutex_unlock(&m);
  }

  return 0;
}

int main()
{
  pthread_t tid;

  pthread_mutex_init(&m, 0);
  pthread_create(&tid, 0, producer, 0);

  pthread_mutex_lock(&m);
  if (used != 0) reach_error();
  pthread_mutex_unlock(&m);

  pthread_mutex_destroy(&m);
  return 0;
}
