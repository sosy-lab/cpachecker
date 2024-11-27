typedef unsigned char __u_char;
typedef unsigned short int __u_short;
typedef unsigned int __u_int;
typedef unsigned long int __u_long;
typedef signed char __int8_t;
typedef unsigned char __uint8_t;
typedef signed short int __int16_t;
typedef unsigned short int __uint16_t;
typedef signed int __int32_t;
typedef unsigned int __uint32_t;
typedef signed long int __int64_t;
typedef unsigned long int __uint64_t;
typedef __int8_t __int_least8_t;
typedef __uint8_t __uint_least8_t;
typedef __int16_t __int_least16_t;
typedef __uint16_t __uint_least16_t;
typedef __int32_t __int_least32_t;
typedef __uint32_t __uint_least32_t;
typedef __int64_t __int_least64_t;
typedef __uint64_t __uint_least64_t;
typedef long int __quad_t;
typedef unsigned long int __u_quad_t;
typedef long int __intmax_t;
typedef unsigned long int __uintmax_t;
typedef unsigned long int __dev_t;
typedef unsigned int __uid_t;
typedef unsigned int __gid_t;
typedef unsigned long int __ino_t;
typedef unsigned long int __ino64_t;
typedef unsigned int __mode_t;
typedef unsigned long int __nlink_t;
typedef long int __off_t;
typedef long int __off64_t;
typedef int __pid_t;
struct __anon_type___fsid_t {
  int __val[2];
} ;
typedef struct __anon_type___fsid_t __fsid_t;
typedef long int __clock_t;
typedef unsigned long int __rlim_t;
typedef unsigned long int __rlim64_t;
typedef unsigned int __id_t;
typedef long int __time_t;
typedef unsigned int __useconds_t;
typedef long int __suseconds_t;
typedef long int __suseconds64_t;
typedef int __daddr_t;
typedef int __key_t;
typedef int __clockid_t;
typedef void *__timer_t;
typedef long int __blksize_t;
typedef long int __blkcnt_t;
typedef long int __blkcnt64_t;
typedef unsigned long int __fsblkcnt_t;
typedef unsigned long int __fsblkcnt64_t;
typedef unsigned long int __fsfilcnt_t;
typedef unsigned long int __fsfilcnt64_t;
typedef long int __fsword_t;
typedef long int __ssize_t;
typedef long int __syscall_slong_t;
typedef unsigned long int __syscall_ulong_t;
typedef __off64_t __loff_t;
typedef char *__caddr_t;
typedef long int __intptr_t;
typedef unsigned int __socklen_t;
typedef int __sig_atomic_t;
typedef unsigned long int size_t;
typedef __time_t time_t;
struct timespec {
  __time_t tv_sec;
  __syscall_slong_t tv_nsec;
} ;
typedef __pid_t pid_t;
struct sched_param {
  int sched_priority;
} ;
typedef unsigned long int __cpu_mask;
struct __anon_type_cpu_set_t {
  __cpu_mask __bits[32UL];
} ;
typedef struct __anon_type_cpu_set_t cpu_set_t;
int __sched_cpucount(size_t __setsize, const cpu_set_t *__setp);
cpu_set_t *__sched_cpualloc(size_t __count);
void __sched_cpufree(cpu_set_t *__set);
int sched_setparam(__pid_t __pid, const struct sched_param *__param);
int sched_getparam(__pid_t __pid, struct sched_param *__param);
int sched_setscheduler(__pid_t __pid, int __policy, const struct sched_param *__param);
int sched_getscheduler(__pid_t __pid);
int sched_yield();
int sched_get_priority_max(int __algorithm);
int sched_get_priority_min(int __algorithm);
int sched_rr_get_interval(__pid_t __pid, struct timespec *__t);
typedef __clock_t clock_t;
struct tm {
  int tm_sec;
  int tm_min;
  int tm_hour;
  int tm_mday;
  int tm_mon;
  int tm_year;
  int tm_wday;
  int tm_yday;
  int tm_isdst;
  long int tm_gmtoff;
  const char *tm_zone;
} ;
typedef __clockid_t clockid_t;
typedef __timer_t timer_t;
struct itimerspec {
  struct timespec it_interval;
  struct timespec it_value;
} ;
struct sigevent ;
struct __locale_struct {
  struct __locale_data *__locales[13];
  const unsigned short int *__ctype_b;
  const int *__ctype_tolower;
  const int *__ctype_toupper;
  const char *__names[13];
} ;
typedef struct __locale_struct *__locale_t;
typedef __locale_t locale_t;
clock_t clock();
time_t time(time_t *__timer);
double difftime(time_t __time1, time_t __time0);
time_t mktime(struct tm *__tp);
size_t strftime(char *__s, size_t __maxsize, const char *__format, const struct tm *__tp);
size_t strftime_l(char *__s, size_t __maxsize, const char *__format, const struct tm *__tp, locale_t __loc);
struct tm *gmtime(const time_t *__timer);
struct tm *localtime(const time_t *__timer);
struct tm *gmtime_r(const time_t *__timer, struct tm *__tp);
struct tm *localtime_r(const time_t *__timer, struct tm *__tp);
char *asctime(const struct tm *__tp);
char *ctime(const time_t *__timer);
char *asctime_r(const struct tm *__tp, char *__buf);
char *ctime_r(const time_t *__timer, char *__buf);
void tzset();
time_t timegm(struct tm *__tp);
time_t timelocal(struct tm *__tp);
int dysize(int __year);
int nanosleep(const struct timespec *__requested_time, struct timespec *__remaining);
int clock_getres(clockid_t __clock_id, struct timespec *__res);
int clock_gettime(clockid_t __clock_id, struct timespec *__tp);
int clock_settime(clockid_t __clock_id, const struct timespec *__tp);
int clock_nanosleep(clockid_t __clock_id, int __flags, const struct timespec *__req, struct timespec *__rem);
int clock_getcpuclockid(pid_t __pid, clockid_t *__clock_id);
int timer_create(clockid_t __clock_id, struct sigevent *__evp, timer_t *__timerid);
int timer_delete(timer_t __timerid);
int timer_settime(timer_t __timerid, int __flags, const struct itimerspec *__value, struct itimerspec *__ovalue);
int timer_gettime(timer_t __timerid, struct itimerspec *__value);
int timer_getoverrun(timer_t __timerid);
int timespec_get(struct timespec *__ts, int __base);
struct __anon_type_0 {
  unsigned int __low;
  unsigned int __high;
} ;
union __anon_type___atomic_wide_counter {
  unsigned long long int __value64;
  struct __anon_type_0 __value32;
} ;
typedef union __anon_type___atomic_wide_counter __atomic_wide_counter;
struct __pthread_internal_list {
  struct __pthread_internal_list *__prev;
  struct __pthread_internal_list *__next;
} ;
typedef struct __pthread_internal_list __pthread_list_t;
struct __pthread_internal_slist {
  struct __pthread_internal_slist *__next;
} ;
typedef struct __pthread_internal_slist __pthread_slist_t;
struct __pthread_mutex_s {
  int __lock;
  unsigned int __count;
  int __owner;
  unsigned int __nusers;
  int __kind;
  short __spins;
  short __elision;
  __pthread_list_t __list;
} ;
struct __pthread_rwlock_arch_t {
  unsigned int __readers;
  unsigned int __writers;
  unsigned int __wrphase_futex;
  unsigned int __writers_futex;
  unsigned int __pad3;
  unsigned int __pad4;
  int __cur_writer;
  int __shared;
  signed char __rwelision;
  unsigned char __pad1[7];
  unsigned long int __pad2;
  unsigned int __flags;
} ;
struct __pthread_cond_s {
  __atomic_wide_counter __wseq;
  __atomic_wide_counter __g1_start;
  unsigned int __g_refs[2];
  unsigned int __g_size[2];
  unsigned int __g1_orig_size;
  unsigned int __wrefs;
  unsigned int __g_signals[2];
} ;
typedef unsigned int __tss_t;
typedef unsigned long int __thrd_t;
struct __anon_type___once_flag {
  int __data;
} ;
typedef struct __anon_type___once_flag __once_flag;
typedef unsigned long int pthread_t;
union __anon_type_pthread_mutexattr_t {
  char __size[4];
  int __align;
} ;
typedef union __anon_type_pthread_mutexattr_t pthread_mutexattr_t;
union __anon_type_pthread_condattr_t {
  char __size[4];
  int __align;
} ;
typedef union __anon_type_pthread_mutexattr_t pthread_condattr_t;
typedef unsigned int pthread_key_t;
typedef int pthread_once_t;
union pthread_attr_t {
  char __size[56];
  long int __align;
} ;
typedef union pthread_attr_t pthread_attr_t;
union __anon_type_pthread_mutex_t {
  struct __pthread_mutex_s __data;
  char __size[40];
  long int __align;
} ;
typedef union __anon_type_pthread_mutex_t pthread_mutex_t;
union __anon_type_pthread_cond_t {
  struct __pthread_cond_s __data;
  char __size[48];
  long long int __align;
} ;
typedef union __anon_type_pthread_cond_t pthread_cond_t;
union __anon_type_pthread_rwlock_t {
  struct __pthread_rwlock_arch_t __data;
  char __size[56];
  long int __align;
} ;
typedef union __anon_type_pthread_rwlock_t pthread_rwlock_t;
union __anon_type_pthread_rwlockattr_t {
  char __size[8];
  long int __align;
} ;
typedef union __anon_type_pthread_rwlockattr_t pthread_rwlockattr_t;
typedef volatile int pthread_spinlock_t;
union __anon_type_pthread_barrier_t {
  char __size[32];
  long int __align;
} ;
typedef union __anon_type_pthread_barrier_t pthread_barrier_t;
union __anon_type_pthread_barrierattr_t {
  char __size[4];
  int __align;
} ;
typedef union __anon_type_pthread_mutexattr_t pthread_barrierattr_t;
typedef long int __jmp_buf[8];
struct __anon_type___sigset_t {
  unsigned long int __val[32UL];
} ;
typedef struct __anon_type___sigset_t __sigset_t;
struct __jmp_buf_tag {
  __jmp_buf __jmpbuf;
  int __mask_was_saved;
  __sigset_t __saved_mask;
} ;
enum __anon_type_1 {
  PTHREAD_CREATE_JOINABLE = 0,
  PTHREAD_CREATE_DETACHED = 1
} ;
enum __anon_type_2 {
  PTHREAD_MUTEX_TIMED_NP = 0,
  PTHREAD_MUTEX_RECURSIVE_NP = 1,
  PTHREAD_MUTEX_ERRORCHECK_NP = 2,
  PTHREAD_MUTEX_ADAPTIVE_NP = 3,
  PTHREAD_MUTEX_NORMAL = 0,
  PTHREAD_MUTEX_RECURSIVE = 1,
  PTHREAD_MUTEX_ERRORCHECK = 2,
  PTHREAD_MUTEX_DEFAULT = 0
} ;
enum __anon_type_3 {
  PTHREAD_MUTEX_STALLED = 0,
  PTHREAD_MUTEX_STALLED_NP = 0,
  PTHREAD_MUTEX_ROBUST = 1,
  PTHREAD_MUTEX_ROBUST_NP = 1
} ;
enum __anon_type_4 {
  PTHREAD_PRIO_NONE = 0,
  PTHREAD_PRIO_INHERIT = 1,
  PTHREAD_PRIO_PROTECT = 2
} ;
enum __anon_type_5 {
  PTHREAD_RWLOCK_PREFER_READER_NP = 0,
  PTHREAD_RWLOCK_PREFER_WRITER_NP = 1,
  PTHREAD_RWLOCK_PREFER_WRITER_NONRECURSIVE_NP = 2,
  PTHREAD_RWLOCK_DEFAULT_NP = 0
} ;
enum __anon_type_6 {
  PTHREAD_INHERIT_SCHED = 0,
  PTHREAD_EXPLICIT_SCHED = 1
} ;
enum __anon_type_7 {
  PTHREAD_SCOPE_SYSTEM = 0,
  PTHREAD_SCOPE_PROCESS = 1
} ;
enum __anon_type_8 {
  PTHREAD_PROCESS_PRIVATE = 0,
  PTHREAD_PROCESS_SHARED = 1
} ;
struct _pthread_cleanup_buffer {
  void (*__routine)(void *);
  void *__arg;
  int __canceltype;
  struct _pthread_cleanup_buffer *__prev;
} ;
enum __anon_type_9 {
  PTHREAD_CANCEL_ENABLE = 0,
  PTHREAD_CANCEL_DISABLE = 1
} ;
enum __anon_type_10 {
  PTHREAD_CANCEL_DEFERRED = 0,
  PTHREAD_CANCEL_ASYNCHRONOUS = 1
} ;
int pthread_create(pthread_t *__newthread, const pthread_attr_t *__attr, void *(*__start_routine)(void *), void *__arg);
void pthread_exit(void *__retval);
int pthread_join(pthread_t __th, void **__thread_return);
int pthread_detach(pthread_t __th);
pthread_t pthread_self();
int pthread_equal(pthread_t __thread1, pthread_t __thread2);
int pthread_attr_init(pthread_attr_t *__attr);
int pthread_attr_destroy(pthread_attr_t *__attr);
int pthread_attr_getdetachstate(const pthread_attr_t *__attr, int *__detachstate);
int pthread_attr_setdetachstate(pthread_attr_t *__attr, int __detachstate);
int pthread_attr_getguardsize(const pthread_attr_t *__attr, size_t *__guardsize);
int pthread_attr_setguardsize(pthread_attr_t *__attr, size_t __guardsize);
int pthread_attr_getschedparam(const pthread_attr_t *__attr, struct sched_param *__param);
int pthread_attr_setschedparam(pthread_attr_t *__attr, const struct sched_param *__param);
int pthread_attr_getschedpolicy(const pthread_attr_t *__attr, int *__policy);
int pthread_attr_setschedpolicy(pthread_attr_t *__attr, int __policy);
int pthread_attr_getinheritsched(const pthread_attr_t *__attr, int *__inherit);
int pthread_attr_setinheritsched(pthread_attr_t *__attr, int __inherit);
int pthread_attr_getscope(const pthread_attr_t *__attr, int *__scope);
int pthread_attr_setscope(pthread_attr_t *__attr, int __scope);
int pthread_attr_getstackaddr(const pthread_attr_t *__attr, void **__stackaddr);
int pthread_attr_setstackaddr(pthread_attr_t *__attr, void *__stackaddr);
int pthread_attr_getstacksize(const pthread_attr_t *__attr, size_t *__stacksize);
int pthread_attr_setstacksize(pthread_attr_t *__attr, size_t __stacksize);
int pthread_attr_getstack(const pthread_attr_t *__attr, void **__stackaddr, size_t *__stacksize);
int pthread_attr_setstack(pthread_attr_t *__attr, void *__stackaddr, size_t __stacksize);
int pthread_setschedparam(pthread_t __target_thread, int __policy, const struct sched_param *__param);
int pthread_getschedparam(pthread_t __target_thread, int *__policy, struct sched_param *__param);
int pthread_setschedprio(pthread_t __target_thread, int __prio);
int pthread_once(pthread_once_t *__once_control, void (*__init_routine)());
int pthread_setcancelstate(int __state, int *__oldstate);
int pthread_setcanceltype(int __type, int *__oldtype);
int pthread_cancel(pthread_t __th);
void pthread_testcancel();
struct __cancel_jmp_buf_tag {
  __jmp_buf __cancel_jmp_buf;
  int __mask_was_saved;
} ;
struct __anon_type___pthread_unwind_buf_t {
  struct __cancel_jmp_buf_tag __cancel_jmp_buf[1];
  void *__pad[4];
} ;
typedef struct __anon_type___pthread_unwind_buf_t __pthread_unwind_buf_t;
struct __pthread_cleanup_frame {
  void (*__cancel_routine)(void *);
  void *__cancel_arg;
  int __do_it;
  int __cancel_type;
} ;
void __pthread_register_cancel(__pthread_unwind_buf_t *__buf);
void __pthread_unregister_cancel(__pthread_unwind_buf_t *__buf);
void __pthread_unwind_next(__pthread_unwind_buf_t *__buf);
int __sigsetjmp_cancel(struct __cancel_jmp_buf_tag __env[1], int __savemask);
int pthread_mutex_init(pthread_mutex_t *__mutex, const pthread_mutexattr_t *__mutexattr);
int pthread_mutex_destroy(pthread_mutex_t *__mutex);
int pthread_mutex_trylock(pthread_mutex_t *__mutex);
int pthread_mutex_lock(pthread_mutex_t *__mutex);
int pthread_mutex_timedlock(pthread_mutex_t *__mutex, const struct timespec *__abstime);
int pthread_mutex_unlock(pthread_mutex_t *__mutex);
int pthread_mutex_getprioceiling(const pthread_mutex_t *__mutex, int *__prioceiling);
int pthread_mutex_setprioceiling(pthread_mutex_t *__mutex, int __prioceiling, int *__old_ceiling);
int pthread_mutex_consistent(pthread_mutex_t *__mutex);
int pthread_mutexattr_init(pthread_mutexattr_t *__attr);
int pthread_mutexattr_destroy(pthread_mutexattr_t *__attr);
int pthread_mutexattr_getpshared(const pthread_mutexattr_t *__attr, int *__pshared);
int pthread_mutexattr_setpshared(pthread_mutexattr_t *__attr, int __pshared);
int pthread_mutexattr_gettype(const pthread_mutexattr_t *__attr, int *__kind);
int pthread_mutexattr_settype(pthread_mutexattr_t *__attr, int __kind);
int pthread_mutexattr_getprotocol(const pthread_mutexattr_t *__attr, int *__protocol);
int pthread_mutexattr_setprotocol(pthread_mutexattr_t *__attr, int __protocol);
int pthread_mutexattr_getprioceiling(const pthread_mutexattr_t *__attr, int *__prioceiling);
int pthread_mutexattr_setprioceiling(pthread_mutexattr_t *__attr, int __prioceiling);
int pthread_mutexattr_getrobust(const pthread_mutexattr_t *__attr, int *__robustness);
int pthread_mutexattr_setrobust(pthread_mutexattr_t *__attr, int __robustness);
int pthread_rwlock_init(pthread_rwlock_t *__rwlock, const pthread_rwlockattr_t *__attr);
int pthread_rwlock_destroy(pthread_rwlock_t *__rwlock);
int pthread_rwlock_rdlock(pthread_rwlock_t *__rwlock);
int pthread_rwlock_tryrdlock(pthread_rwlock_t *__rwlock);
int pthread_rwlock_timedrdlock(pthread_rwlock_t *__rwlock, const struct timespec *__abstime);
int pthread_rwlock_wrlock(pthread_rwlock_t *__rwlock);
int pthread_rwlock_trywrlock(pthread_rwlock_t *__rwlock);
int pthread_rwlock_timedwrlock(pthread_rwlock_t *__rwlock, const struct timespec *__abstime);
int pthread_rwlock_unlock(pthread_rwlock_t *__rwlock);
int pthread_rwlockattr_init(pthread_rwlockattr_t *__attr);
int pthread_rwlockattr_destroy(pthread_rwlockattr_t *__attr);
int pthread_rwlockattr_getpshared(const pthread_rwlockattr_t *__attr, int *__pshared);
int pthread_rwlockattr_setpshared(pthread_rwlockattr_t *__attr, int __pshared);
int pthread_rwlockattr_getkind_np(const pthread_rwlockattr_t *__attr, int *__pref);
int pthread_rwlockattr_setkind_np(pthread_rwlockattr_t *__attr, int __pref);
int pthread_cond_init(pthread_cond_t *__cond, const pthread_condattr_t *__cond_attr);
int pthread_cond_destroy(pthread_cond_t *__cond);
int pthread_cond_signal(pthread_cond_t *__cond);
int pthread_cond_broadcast(pthread_cond_t *__cond);
int pthread_cond_wait(pthread_cond_t *__cond, pthread_mutex_t *__mutex);
int pthread_cond_timedwait(pthread_cond_t *__cond, pthread_mutex_t *__mutex, const struct timespec *__abstime);
int pthread_condattr_init(pthread_condattr_t *__attr);
int pthread_condattr_destroy(pthread_condattr_t *__attr);
int pthread_condattr_getpshared(const pthread_condattr_t *__attr, int *__pshared);
int pthread_condattr_setpshared(pthread_condattr_t *__attr, int __pshared);
int pthread_condattr_getclock(const pthread_condattr_t *__attr, __clockid_t *__clock_id);
int pthread_condattr_setclock(pthread_condattr_t *__attr, __clockid_t __clock_id);
int pthread_spin_init(pthread_spinlock_t *__lock, int __pshared);
int pthread_spin_destroy(pthread_spinlock_t *__lock);
int pthread_spin_lock(pthread_spinlock_t *__lock);
int pthread_spin_trylock(pthread_spinlock_t *__lock);
int pthread_spin_unlock(pthread_spinlock_t *__lock);
int pthread_barrier_init(pthread_barrier_t *__barrier, const pthread_barrierattr_t *__attr, unsigned int __count);
int pthread_barrier_destroy(pthread_barrier_t *__barrier);
int pthread_barrier_wait(pthread_barrier_t *__barrier);
int pthread_barrierattr_init(pthread_barrierattr_t *__attr);
int pthread_barrierattr_destroy(pthread_barrierattr_t *__attr);
int pthread_barrierattr_getpshared(const pthread_barrierattr_t *__attr, int *__pshared);
int pthread_barrierattr_setpshared(pthread_barrierattr_t *__attr, int __pshared);
int pthread_key_create(pthread_key_t *__key, void (*__destr_function)(void *));
int pthread_key_delete(pthread_key_t __key);
void *pthread_getspecific(pthread_key_t __key);
int pthread_setspecific(pthread_key_t __key, const void *__pointer);
int pthread_getcpuclockid(pthread_t __thread_id, __clockid_t *__clock_id);
int pthread_atfork(void (*__prepare)(), void (*__parent)(), void (*__child)());
void __assert_fail(const char *__assertion, const char *__file, unsigned int __line, const char *__function);
void *task1(void *arg);
void *task2(void *arg);
int main();

int x = 0;
void *task1(void *arg) {
    x = 0;
    x++;
}
void *task2(void *arg) {
    x++;
    x++;
}
int main() {
    pthread_t id1, id2;
    pthread_create(&id1, (void *) 0, task1, (void *) 0);
    pthread_create(&id2, (void *) 0, task2, (void *) 0);
    pthread_join(id1, (void *) 0);
    pthread_join(id2, (void *) 0);
    if (x < 2) {__assert_fail("0", "simple_two.i", 24, __extension__ __PRETTY_FUNCTION__);}
    return 0;
}
