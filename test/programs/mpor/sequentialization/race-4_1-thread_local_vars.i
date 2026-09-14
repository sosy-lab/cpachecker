// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2015-2016 Daniel Liew <dan@su-root.co.uk>
// SPDX-FileCopyrightText: 2015-2020 The SV-Benchmarks Community
//
// SPDX-License-Identifier: GPL-2.0-only

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
extern int pthread_mutex_destroy (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_lock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_unlock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "race-4_1-thread_local_vars.c", 8, __extension__ __PRETTY_FUNCTION__); })); }
int __VERIFIER_nondet_int(void);
void ldv_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}}; return; }
pthread_t t1, t2;
pthread_mutex_t mutex;
int pdev;
void ath9k_flush(void) {
   pthread_mutex_lock(&mutex);
   pdev = 6;
   ldv_assert(pdev==6);
   pthread_mutex_unlock(&mutex);
}
void *thread_ath9k(void *arg) {
    while(1) {
      switch(__VERIFIER_nondet_int()) {
       case 1:
         ath9k_flush();
         break;
       case 2:
                  goto exit_thread_ath9k;
      }
    }
exit_thread_ath9k:
    return 0;
}
int ieee80211_register_hw(void) {
   if(__VERIFIER_nondet_int()) {
      pthread_create(&t2, ((void *)0), thread_ath9k, ((void *)0));
      return 0;
   }
   return -1;
}
void ieee80211_deregister_hw(void) {
   void *status;
   pthread_join(t2, &status);
   return;
}
static int ath_ahb_probe(void)
{
        int error;
        error = ieee80211_register_hw();
        if (error)
                goto rx_cleanup;
        return 0;
rx_cleanup:
        return -1;
}
void ath_ahb_disconnect() {
        ieee80211_deregister_hw();
}
int ldv_usb_state;
void *thread_usb(void *arg) {
    ldv_usb_state = 0;
    int probe_ret;
    while(1) {
      switch(__VERIFIER_nondet_int()) {
  case 0:
                if(ldv_usb_state==0) {
    probe_ret = ath_ahb_probe();
    if(probe_ret!=0)
      goto exit_thread_usb;
                  ldv_usb_state = 1;
                }
  break;
  case 1:
                if(ldv_usb_state==1) {
                   ath_ahb_disconnect();
     ldv_usb_state=0;
     pdev = 8;
     ldv_assert(pdev==8);
                }
  break;
  case 2:
                if(ldv_usb_state==0) {
                  goto exit_thread_usb;
  }
  break;
      }
    }
exit_thread_usb:
    pdev = 9;
    ldv_assert(pdev==9);
    return 0;
}
int module_init() {
   pthread_mutex_init(&mutex, ((void *)0));
   pdev = 1;
   ldv_assert(pdev==1);
   if(__VERIFIER_nondet_int()) {
      pthread_create(&t1, ((void *)0), thread_usb, ((void *)0));
      return 0;
   }
   pdev = 3;
   ldv_assert(pdev==3);
   pthread_mutex_destroy(&mutex);
   return -1;
}
void module_exit() {
   void *status;
   pthread_join(t1, &status);
   pthread_mutex_destroy(&mutex);
   pdev = 5;
   ldv_assert(pdev==5);
}
int main(void) {
    if(module_init()!=0) goto module_exit;
    module_exit();
module_exit:
    return 0;
}
