// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

extern void abort(void);
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "13-privatized_67-pthread_cond_wait_true.c", 3, __extension__ __PRETTY_FUNCTION__); })); }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }

typedef struct __pthread_internal_list
{
  struct __pthread_internal_list *__prev;
  struct __pthread_internal_list *__next;
} __pthread_list_t;
typedef struct __pthread_internal_slist
{
  struct __pthread_internal_slist *__next;
} __pthread_slist_t;
struct __pthread_mutex_s
{
  int __lock;
  unsigned int __count;
  int __owner;
  int __kind;
  unsigned int __nusers;
  __extension__ union
  {
    struct
    {
      short __espins;
      short __eelision;
    } __elision_data;
    __pthread_slist_t __list;
  };
};
struct __pthread_rwlock_arch_t
{
  unsigned int __readers;
  unsigned int __writers;
  unsigned int __wrphase_futex;
  unsigned int __writers_futex;
  unsigned int __pad3;
  unsigned int __pad4;
  unsigned char __flags;
  unsigned char __shared;
  signed char __rwelision;
  unsigned char __pad2;
  int __cur_writer;
};
struct __pthread_cond_s
{
  __extension__ union
  {
    __extension__ unsigned long long int __wseq;
    struct
    {
      unsigned int __low;
      unsigned int __high;
    } __wseq32;
  };
  __extension__ union
  {
    __extension__ unsigned long long int __g1_start;
    struct
    {
      unsigned int __low;
      unsigned int __high;
    } __g1_start32;
  };
  unsigned int __g_refs[2] ;
  unsigned int __g_size[2];
  unsigned int __g1_orig_size;
  unsigned int __wrefs;
  unsigned int __g_signals[2];
};
typedef unsigned long int pthread_t;
typedef union
{
  char __size[4];
  int __align;
} pthread_mutexattr_t;
typedef union
{
  char __size[4];
  int __align;
} pthread_condattr_t;
typedef int pthread_once_t;
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
typedef union
{
  struct __pthread_cond_s __data;
  char __size[48];
  __extension__ long long int __align;
} pthread_cond_t;
typedef union
{
  struct __pthread_rwlock_arch_t __data;
  char __size[32];
  long int __align;
} pthread_rwlock_t;
typedef union
{
   char __size[64];
   long int __align;
} pthread_rwlockattr_t;

extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern void pthread_exit (void *__retval) __attribute__ ((__noreturn__));
extern int pthread_join (pthread_t __th, void **__thread_return);

extern int pthread_once (pthread_once_t *__once_control,
    void (*__init_routine) (void)) __attribute__ ((__nonnull__ (1, 2)));

extern int pthread_mutex_init (pthread_mutex_t *__mutex,
          const pthread_mutexattr_t *__mutexattr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_destroy (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_lock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_unlock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int pthread_rwlock_init (pthread_rwlock_t *__restrict __rwlock,
    const pthread_rwlockattr_t *__restrict
    __attr) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_rwlock_destroy (pthread_rwlock_t *__rwlock)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_rwlock_rdlock (pthread_rwlock_t *__rwlock)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_rwlock_wrlock (pthread_rwlock_t *__rwlock)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_rwlock_unlock (pthread_rwlock_t *__rwlock)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int pthread_cond_init (pthread_cond_t *__restrict __cond,
         const pthread_condattr_t *__restrict __cond_attr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_cond_destroy (pthread_cond_t *__cond)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_cond_signal (pthread_cond_t *__cond)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_cond_broadcast (pthread_cond_t *__cond)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_cond_wait (pthread_cond_t *__restrict __cond,
         pthread_mutex_t *__restrict __mutex)
     __attribute__ ((__nonnull__ (1, 2)));

struct synchronization {
    pthread_mutex_t sync_mutex;
    pthread_cond_t sync_cond;
    pthread_rwlock_t sync_rwlock;
};
struct worker {
    int worker_id;
    int worker_active;
    struct synchronization worker_sync;
    pthread_once_t worker_once;
};
struct application {
    struct worker application_worker;
    pthread_mutex_t application_mutex;
    pthread_cond_t application_cond;
    struct {
        pthread_rwlock_t nested_rwlock;
        pthread_mutex_t nested_mutex;
    } application_nested;
};
struct complex_object {
    int complex_number;
    long complex_value;
    void *complex_pointer;
    pthread_mutex_t complex_mutex;
    pthread_cond_t complex_cond;
    struct {
        int internal_flags;
        pthread_rwlock_t internal_rwlock;
        pthread_once_t internal_once;
    } complex_internal;
};

pthread_cond_t global_cond = { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } };
pthread_mutex_t global_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } };
pthread_once_t global_once = 0;
pthread_rwlock_t global_rwlock = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } };

static struct synchronization sync_object = {
    { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
    { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
    { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }
};
static struct worker worker_object = {
    1,
    1,
    {
        { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
        { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
        { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }
    },
    0
};
static struct application app_object = {
    {
        42,
        1,
        {
            { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
            { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
            { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }
        },
        0
    },
    { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
    { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
    {
        { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
        { { 0, 0, 0, 0, 0, { { 0, 0 } } } }
    }
};
static struct synchronization designated_sync = {
    .sync_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
    .sync_cond = { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
    .sync_rwlock = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }
};
static struct worker designated_worker = {
    .worker_id = 100,
    .worker_active = 1,
    .worker_sync = {
        .sync_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
        .sync_cond = { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
        .sync_rwlock = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }
    },
    .worker_once = 0
};
static struct application designated_app = {
    .application_worker = {
        .worker_id = 200,
        .worker_active = 1,
        .worker_sync = {
            .sync_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
            .sync_cond = { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
            .sync_rwlock = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }
        },
        .worker_once = 0
    },
    .application_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
    .application_cond = { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
    .application_nested = {
        .nested_rwlock = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
        .nested_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } }
    }
};
static struct complex_object complex_instance = {
    .complex_number = 123,
    .complex_value = 456L,
    .complex_pointer = (void *)0,
    .complex_mutex = { { 0, 0, 0, 0, 0, { { 0, 0 } } } },
    .complex_cond = { { { 0 }, { 0 }, { 0, 0 }, { 0, 0 }, 0, 0, { 0, 0 } } },
    .complex_internal = {
        .internal_flags = 0x1234,
        .internal_rwlock = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
        .internal_once = 0
    }
};

static int once_counter = 0;

static void initialize_once(void)
{
    once_counter++;
}
static void test_mutex(pthread_mutex_t *mutex)
{
    pthread_mutex_lock(mutex);
    pthread_mutex_unlock(mutex);
}
static void test_cond(pthread_cond_t *cond)
{
    pthread_mutex_lock(&global_mutex);
    pthread_cond_signal(cond);
    pthread_mutex_unlock(&global_mutex);
    pthread_mutex_destroy(&global_mutex);
}
static void test_rwlock(pthread_rwlock_t *rwlock)
{
    pthread_rwlock_rdlock(rwlock);
    pthread_rwlock_unlock(rwlock);
}
static void test_once(pthread_once_t *once)
{
    pthread_once(once, initialize_once);
}
static void start_routine(void* arg) {
    return;
}
int main(void)
{
    pthread_t t1;
    pthread_create(&t1, (void *)0, start_routine, (void *)0);
    pthread_join(t1, (void *)0);

    test_mutex(&global_mutex);
    test_cond(&global_cond);
    test_rwlock(&global_rwlock);
    test_once(&global_once);

    test_mutex(&sync_object.sync_mutex);
    test_cond(&sync_object.sync_cond);
    test_rwlock(&sync_object.sync_rwlock);

    test_mutex(&worker_object.worker_sync.sync_mutex);
    test_cond(&worker_object.worker_sync.sync_cond);
    test_rwlock(&worker_object.worker_sync.sync_rwlock);
    test_once(&worker_object.worker_once);

    test_mutex(&app_object.application_worker.worker_sync.sync_mutex);
    test_cond(&app_object.application_worker.worker_sync.sync_cond);
    test_rwlock(&app_object.application_nested.nested_rwlock);
    test_mutex(&app_object.application_nested.nested_mutex);

    test_mutex(&designated_sync.sync_mutex);
    test_cond(&designated_sync.sync_cond);
    test_rwlock(&designated_sync.sync_rwlock);

    test_mutex(&designated_worker.worker_sync.sync_mutex);
    test_cond(&designated_worker.worker_sync.sync_cond);
    test_rwlock(&designated_worker.worker_sync.sync_rwlock);
    test_once(&designated_worker.worker_once);

    test_mutex(&designated_app.application_worker.worker_sync.sync_mutex);
    test_cond(&designated_app.application_cond);
    test_rwlock(&designated_app.application_nested.nested_rwlock);

    test_mutex(&complex_instance.complex_mutex);
    test_cond(&complex_instance.complex_cond);
    test_rwlock(&complex_instance.complex_internal.internal_rwlock);
    test_once(&complex_instance.complex_internal.internal_once);

    if (once_counter == 0) {
        reach_error();
    }

    return 0;
}
