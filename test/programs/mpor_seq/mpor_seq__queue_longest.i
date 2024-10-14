// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

// This sequentialization (transformation of a parallel program into an equivalent 
// sequential program) was created by the MPORAlgorithm implemented in CPAchecker. 
// 
// Assertion fails from the function "__SEQUENTIALIZATION_ERROR__" mark faulty sequentializations. 
// All other assertion fails are induced by faulty input programs. 
// 
// Input program file: /home/noahkoenig/Documents/Edu/24ss/ba-sosy/sv-common/queue_longest.i

// unchanged input program declarations
int __VERIFIER_nondet_int();
void abort();
void __assert_fail(const char *__assertion, const char *__file, unsigned int __line, const char *__function);
void __assert_perror_fail(int __errnum, const char *__file, unsigned int __line, const char *__function);
void __assert(const char *__assertion, const char *__file, int __line);
void reach_error();
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
typedef signed long long int __int64_t;
typedef unsigned long long int __uint64_t;
typedef long long int __quad_t;
typedef unsigned long long int __u_quad_t;
typedef long long int __intmax_t;
typedef unsigned long long int __uintmax_t;
typedef __u_quad_t __dev_t;
typedef unsigned int __uid_t;
typedef unsigned int __gid_t;
typedef unsigned long int __ino_t;
typedef __u_quad_t __ino64_t;
typedef unsigned int __mode_t;
typedef unsigned int __nlink_t;
typedef long int __off_t;
typedef __quad_t __off64_t;
typedef int __pid_t;
struct __anon_type___fsid_t {
  int __val[2];
} ;
typedef struct __anon_type___fsid_t __fsid_t;
typedef long int __clock_t;
typedef unsigned long int __rlim_t;
typedef __u_quad_t __rlim64_t;
typedef unsigned int __id_t;
typedef long int __time_t;
typedef unsigned int __useconds_t;
typedef long int __suseconds_t;
typedef int __daddr_t;
typedef int __key_t;
typedef int __clockid_t;
typedef void *__timer_t;
typedef long int __blksize_t;
typedef long int __blkcnt_t;
typedef __quad_t __blkcnt64_t;
typedef unsigned long int __fsblkcnt_t;
typedef __u_quad_t __fsblkcnt64_t;
typedef unsigned long int __fsfilcnt_t;
typedef __u_quad_t __fsfilcnt64_t;
typedef int __fsword_t;
typedef int __ssize_t;
typedef long int __syscall_slong_t;
typedef unsigned long int __syscall_ulong_t;
typedef __off64_t __loff_t;
typedef char *__caddr_t;
typedef int __intptr_t;
typedef unsigned int __socklen_t;
typedef int __sig_atomic_t;
unsigned int __bswap_32(unsigned int __bsx);
__uint64_t __bswap_64(__uint64_t __bsx);
__uint16_t __uint16_identity(__uint16_t __x);
__uint32_t __uint32_identity(__uint32_t __x);
__uint64_t __uint64_identity(__uint64_t __x);
typedef unsigned int size_t;
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
int stime(const time_t *__when);
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
struct __pthread_rwlock_arch_t {
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
} ;
struct __pthread_internal_slist {
  struct __pthread_internal_slist *__next;
} ;
typedef struct __pthread_internal_slist __pthread_slist_t;
struct __anon_type_0 {
  short __espins;
  short __eelision;
} ;
union __anon_type_1 {
  struct __anon_type_0 __elision_data;
  __pthread_slist_t __list;
} ;
struct __pthread_mutex_s {
  int __lock;
  unsigned int __count;
  int __owner;
  int __kind;
  unsigned int __nusers;
  union __anon_type_1 __anon_type_member_5;
} ;
struct __anon_type_2 {
  unsigned int __low;
  unsigned int __high;
} ;
union __anon_type_3 {
  unsigned long long int __wseq;
  struct __anon_type_2 __wseq32;
} ;
struct __anon_type_4 {
  unsigned int __low;
  unsigned int __high;
} ;
union __anon_type_5 {
  unsigned long long int __g1_start;
  struct __anon_type_4 __g1_start32;
} ;
struct __pthread_cond_s {
  union __anon_type_3 __anon_type_member_0;
  union __anon_type_5 __anon_type_member_1;
  unsigned int __g_refs[2];
  unsigned int __g_size[2];
  unsigned int __g1_orig_size;
  unsigned int __wrefs;
  unsigned int __g_signals[2];
} ;
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
  char __size[36];
  long int __align;
} ;
typedef union pthread_attr_t pthread_attr_t;
union __anon_type_pthread_mutex_t {
  struct __pthread_mutex_s __data;
  char __size[24];
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
  char __size[32];
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
  char __size[20];
  long int __align;
} ;
typedef union __anon_type_pthread_barrier_t pthread_barrier_t;
union __anon_type_pthread_barrierattr_t {
  char __size[4];
  int __align;
} ;
typedef union __anon_type_pthread_mutexattr_t pthread_barrierattr_t;
typedef int __jmp_buf[6];
enum __anon_type_6 {
  PTHREAD_CREATE_JOINABLE = 0,
  PTHREAD_CREATE_DETACHED = 1
} ;
enum __anon_type_7 {
  PTHREAD_MUTEX_TIMED_NP = 0,
  PTHREAD_MUTEX_RECURSIVE_NP = 1,
  PTHREAD_MUTEX_ERRORCHECK_NP = 2,
  PTHREAD_MUTEX_ADAPTIVE_NP = 3,
  PTHREAD_MUTEX_NORMAL = 0,
  PTHREAD_MUTEX_RECURSIVE = 1,
  PTHREAD_MUTEX_ERRORCHECK = 2,
  PTHREAD_MUTEX_DEFAULT = 0
} ;
enum __anon_type_8 {
  PTHREAD_MUTEX_STALLED = 0,
  PTHREAD_MUTEX_STALLED_NP = 0,
  PTHREAD_MUTEX_ROBUST = 1,
  PTHREAD_MUTEX_ROBUST_NP = 1
} ;
enum __anon_type_9 {
  PTHREAD_PRIO_NONE = 0,
  PTHREAD_PRIO_INHERIT = 1,
  PTHREAD_PRIO_PROTECT = 2
} ;
enum __anon_type_10 {
  PTHREAD_RWLOCK_PREFER_READER_NP = 0,
  PTHREAD_RWLOCK_PREFER_WRITER_NP = 1,
  PTHREAD_RWLOCK_PREFER_WRITER_NONRECURSIVE_NP = 2,
  PTHREAD_RWLOCK_DEFAULT_NP = 0
} ;
enum __anon_type_11 {
  PTHREAD_INHERIT_SCHED = 0,
  PTHREAD_EXPLICIT_SCHED = 1
} ;
enum __anon_type_12 {
  PTHREAD_SCOPE_SYSTEM = 0,
  PTHREAD_SCOPE_PROCESS = 1
} ;
enum __anon_type_13 {
  PTHREAD_PROCESS_PRIVATE = 0,
  PTHREAD_PROCESS_SHARED = 1
} ;
struct _pthread_cleanup_buffer {
  void (*__routine)(void *);
  void *__arg;
  int __canceltype;
  struct _pthread_cleanup_buffer *__prev;
} ;
enum __anon_type_14 {
  PTHREAD_CANCEL_ENABLE = 0,
  PTHREAD_CANCEL_DISABLE = 1
} ;
enum __anon_type_15 {
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
struct __anon_type_16 {
  __jmp_buf __cancel_jmp_buf;
  int __mask_was_saved;
} ;
struct __anon_type___pthread_unwind_buf_t {
  struct __anon_type_16 __cancel_jmp_buf[1];
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
struct __jmp_buf_tag ;
int __sigsetjmp(struct __jmp_buf_tag *__env, int __savemask);
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
struct _IO_FILE ;
typedef struct _IO_FILE __FILE;
typedef struct _IO_FILE FILE;
union __anon_type_17 {
  unsigned int __wch;
  char __wchb[4];
} ;
struct __anon_type___mbstate_t {
  int __count;
  union __anon_type_17 __value;
} ;
typedef struct __anon_type___mbstate_t __mbstate_t;
struct __anon_type__G_fpos_t {
  __off_t __pos;
  __mbstate_t __state;
} ;
typedef struct __anon_type__G_fpos_t _G_fpos_t;
struct __anon_type__G_fpos64_t {
  __off64_t __pos;
  __mbstate_t __state;
} ;
typedef struct __anon_type__G_fpos64_t _G_fpos64_t;
typedef __builtin_va_list __gnuc_va_list;
struct _IO_jump_t ;
typedef void _IO_lock_t;
struct _IO_marker {
  struct _IO_marker *_next;
  struct _IO_FILE *_sbuf;
  int _pos;
} ;
enum __codecvt_result {
  __codecvt_ok = 0,
  __codecvt_partial = 1,
  __codecvt_error = 2,
  __codecvt_noconv = 3
} ;
struct _IO_FILE {
  int _flags;
  char *_IO_read_ptr;
  char *_IO_read_end;
  char *_IO_read_base;
  char *_IO_write_base;
  char *_IO_write_ptr;
  char *_IO_write_end;
  char *_IO_buf_base;
  char *_IO_buf_end;
  char *_IO_save_base;
  char *_IO_backup_base;
  char *_IO_save_end;
  struct _IO_marker *_markers;
  struct _IO_FILE *_chain;
  int _fileno;
  int _flags2;
  __off_t _old_offset;
  unsigned short _cur_column;
  signed char _vtable_offset;
  char _shortbuf[1];
  _IO_lock_t *_lock;
  __off64_t _offset;
  void *__pad1;
  void *__pad2;
  void *__pad3;
  void *__pad4;
  size_t __pad5;
  int _mode;
  char _unused2[40UL];
} ;
typedef struct _IO_FILE _IO_FILE;
struct _IO_FILE_plus ;
typedef __ssize_t __io_read_fn(void *__cookie, char *__buf, size_t __nbytes);
typedef __ssize_t __io_write_fn(void *__cookie, const char *__buf, size_t __n);
typedef int __io_seek_fn(void *__cookie, __off64_t *__pos, int __w);
typedef int __io_close_fn(void *__cookie);
int __underflow(_IO_FILE *);
int __uflow(_IO_FILE *);
int __overflow(_IO_FILE *, int);
int _IO_getc(_IO_FILE *__fp);
int _IO_putc(int __c, _IO_FILE *__fp);
int _IO_feof(_IO_FILE *__fp);
int _IO_ferror(_IO_FILE *__fp);
int _IO_peekc_locked(_IO_FILE *__fp);
void _IO_flockfile(_IO_FILE *);
void _IO_funlockfile(_IO_FILE *);
int _IO_ftrylockfile(_IO_FILE *);
int _IO_vfscanf(_IO_FILE *, const char *, __gnuc_va_list , int *);
int _IO_vfprintf(_IO_FILE *, const char *, __gnuc_va_list );
__ssize_t _IO_padn(_IO_FILE *, int, __ssize_t );
size_t _IO_sgetn(_IO_FILE *, void *, size_t );
__off64_t _IO_seekoff(_IO_FILE *, __off64_t , int, int);
__off64_t _IO_seekpos(_IO_FILE *, __off64_t , int);
void _IO_free_backup_area(_IO_FILE *);
typedef __gnuc_va_list va_list;
typedef __off_t off_t;
typedef __ssize_t ssize_t;
typedef _G_fpos_t fpos_t;
int remove(const char *__filename);
int rename(const char *__old, const char *__new);
int renameat(int __oldfd, const char *__old, int __newfd, const char *__new);
FILE *tmpfile();
char *tmpnam(char *__s);
char *tmpnam_r(char *__s);
char *tempnam(const char *__dir, const char *__pfx);
int fclose(FILE *__stream);
int fflush(FILE *__stream);
int fflush_unlocked(FILE *__stream);
FILE *fopen(const char *__filename, const char *__modes);
FILE *freopen(const char *__filename, const char *__modes, FILE *__stream);
FILE *fdopen(int __fd, const char *__modes);
FILE *fmemopen(void *__s, size_t __len, const char *__modes);
FILE *open_memstream(char **__bufloc, size_t *__sizeloc);
void setbuf(FILE *__stream, char *__buf);
int setvbuf(FILE *__stream, char *__buf, int __modes, size_t __n);
void setbuffer(FILE *__stream, char *__buf, size_t __size);
void setlinebuf(FILE *__stream);
int fprintf(FILE *__stream, const char *__format, ...);
int printf(const char *__format, ...);
int sprintf(char *__s, const char *__format, ...);
int vfprintf(FILE *__s, const char *__format, __gnuc_va_list __arg);
int vprintf(const char *__format, __gnuc_va_list __arg);
int vsprintf(char *__s, const char *__format, __gnuc_va_list __arg);
int snprintf(char *__s, size_t __maxlen, const char *__format, ...);
int vsnprintf(char *__s, size_t __maxlen, const char *__format, __gnuc_va_list __arg);
int vdprintf(int __fd, const char *__fmt, __gnuc_va_list __arg);
int dprintf(int __fd, const char *__fmt, ...);
int fscanf(FILE *__stream, const char *__format, ...);
int scanf(const char *__format, ...);
int sscanf(const char *__s, const char *__format, ...);
int vfscanf(FILE *__s, const char *__format, __gnuc_va_list __arg);
int vscanf(const char *__format, __gnuc_va_list __arg);
int vsscanf(const char *__s, const char *__format, __gnuc_va_list __arg);
int fgetc(FILE *__stream);
int getc(FILE *__stream);
int getchar();
int getc_unlocked(FILE *__stream);
int getchar_unlocked();
int fgetc_unlocked(FILE *__stream);
int fputc(int __c, FILE *__stream);
int putc(int __c, FILE *__stream);
int putchar(int __c);
int fputc_unlocked(int __c, FILE *__stream);
int putc_unlocked(int __c, FILE *__stream);
int putchar_unlocked(int __c);
int getw(FILE *__stream);
int putw(int __w, FILE *__stream);
char *fgets(char *__s, int __n, FILE *__stream);
__ssize_t __getdelim(char **__lineptr, size_t *__n, int __delimiter, FILE *__stream);
__ssize_t getdelim(char **__lineptr, size_t *__n, int __delimiter, FILE *__stream);
__ssize_t getline(char **__lineptr, size_t *__n, FILE *__stream);
int fputs(const char *__s, FILE *__stream);
int puts(const char *__s);
int ungetc(int __c, FILE *__stream);
size_t fread(void *__ptr, size_t __size, size_t __n, FILE *__stream);
size_t fwrite(const void *__ptr, size_t __size, size_t __n, FILE *__s);
size_t fread_unlocked(void *__ptr, size_t __size, size_t __n, FILE *__stream);
size_t fwrite_unlocked(const void *__ptr, size_t __size, size_t __n, FILE *__stream);
int fseek(FILE *__stream, long int __off, int __whence);
long int ftell(FILE *__stream);
void rewind(FILE *__stream);
int fseeko(FILE *__stream, __off_t __off, int __whence);
__off_t ftello(FILE *__stream);
int fgetpos(FILE *__stream, fpos_t *__pos);
int fsetpos(FILE *__stream, const fpos_t *__pos);
void clearerr(FILE *__stream);
int feof(FILE *__stream);
int ferror(FILE *__stream);
void clearerr_unlocked(FILE *__stream);
int feof_unlocked(FILE *__stream);
int ferror_unlocked(FILE *__stream);
void perror(const char *__s);
int fileno(FILE *__stream);
int fileno_unlocked(FILE *__stream);
FILE *popen(const char *__command, const char *__modes);
int pclose(FILE *__stream);
char *ctermid(char *__s);
void flockfile(FILE *__stream);
int ftrylockfile(FILE *__stream);
void funlockfile(FILE *__stream);
struct __anon_type_QType {
  int element[800];
  int head;
  int tail;
  int amount;
} ;
typedef struct __anon_type_QType QType;
void init(QType *q);
int empty(QType *q);
int full(QType *q);
int enqueue(QType *q, int x);
int dequeue(QType *q);
void *t1(void *arg);
void *t2(void *arg);
int main();

// global variable substitutes
extern char *__g_0___tzname[2];
extern int __g_1___daylight;
extern long int __g_2___timezone;
extern char *__g_3_tzname[2];
extern int __g_4_daylight;
extern long int __g_5_timezone;
extern struct _IO_FILE_plus __g_6__IO_2_1_stdin_;
extern struct _IO_FILE_plus __g_7__IO_2_1_stdout_;
extern struct _IO_FILE_plus __g_8__IO_2_1_stderr_;
extern struct _IO_FILE *__g_9_stdin;
extern struct _IO_FILE *__g_10_stdout;
extern struct _IO_FILE *__g_11_stderr;
extern int __g_12_sys_nerr;
extern const char * const __g_13_sys_errlist[];
pthread_mutex_t __g_14_m = {  };
int __g_15_stored_elements[800] = {  };
_Bool __g_16_enqueue_flag = 0;
_Bool __g_17_dequeue_flag = 0;
QType __g_18_queue = {  };

// thread 0 local variable substitutes
pthread_t __t0_21_id1;
pthread_t __t0_22_id2;
int __t0_23___CPAchecker_TMP_0;

// thread 1 local variable substitutes
int __t1_27_value;
int __t1_28_i;
int __t1_29___CPAchecker_TMP_0;
int __t1_30___CPAchecker_TMP_1;

// thread 2 local variable substitutes
int __t2_34_i;
int __t2_35___CPAchecker_TMP_0;
int __t2_36_x;

// thread 0 parameter declarations storing function arguments
QType *__p0_19_q;
QType *__p0_20_q;

// thread 1 parameter declarations storing function arguments
QType *__p1_24_q;
int __p1_25_x;
QType *__p1_26_q;

// thread 2 parameter declarations storing function arguments
QType *__p2_33_q;

// thread local function return pc storing calling contexts
int __return_pc_t0_reach_error = 0;
int __return_pc_t0_empty = 0;
int __return_pc_t0_init = 0;
int __return_pc_t1_enqueue = 0;
int __return_pc_t1_reach_error = 0;
int __return_pc_t1_empty = 0;
int __return_pc_t2_reach_error = 0;
int __return_pc_t2_dequeue = 0;

// (p)thread simulation variables
int __t0_active = 1;
int __t1_active = 0;
int __t2_active = 0;
int __g_14_m_locked = 0;
int __t1_awaits___g_14_m = 0;
int __t2_awaits___g_14_m = 0;
int __t0_joins_t1 = 0;
int __t0_joins_t2 = 0;

// custom function declarations
int __VERIFIER_nondet_int();
void abort();
void __assert_fail(const char *__assertion, const char *__file, unsigned int __line, const char *__function);
int __mpor_seq_assume(const int cond);
int main();

void __mpor_seq_assume(const int cond) {
  if (cond == 0) {
    abort();
  }
}

int main() {
  const int NUM_THREADS = 3;
  int pc[] = { 0, 0, 0 };

  while (1) {
    int next_thread;
    next_thread = __VERIFIER_nondet_int();

    __mpor_seq_assume((0 <= next_thread && next_thread < NUM_THREADS));
    __mpor_seq_assume((pc[next_thread]) != -1);

    __mpor_seq_assume((__t0_active || next_thread != 0));
    __mpor_seq_assume((__t1_active || next_thread != 1));
    __mpor_seq_assume((__t2_active || next_thread != 2));
    __mpor_seq_assume((!((__g_14_m_locked && __t1_awaits___g_14_m)) || next_thread != 1));
    __mpor_seq_assume((!((__g_14_m_locked && __t2_awaits___g_14_m)) || next_thread != 2));
    __mpor_seq_assume((!((__t1_active && __t0_joins_t1)) || next_thread != 0));
    __mpor_seq_assume((!((__t2_active && __t0_joins_t2)) || next_thread != 0));

    if (next_thread == 0) {
      switch (pc[0]) {
        case 0: __g_16_enqueue_flag = 1; pc[0] = 439; continue;
        case 439: __g_17_dequeue_flag = 0; pc[0] = 440; continue;
        case 440: __return_pc_t0_init = 441; __p0_20_q = &__g_18_queue; pc[0] = 462; continue;
        case 441: __return_pc_t0_empty = 443; __p0_19_q = &__g_18_queue; pc[0] = 456; continue;
        case 443: if ((0 == __t0_23___CPAchecker_TMP_0) == -1) { pc[0] = 444; } else if (!((0 == __t0_23___CPAchecker_TMP_0) == -1)) { pc[0] = 450; } continue;
        case 444: __return_pc_t0_reach_error = -1; pc[0] = 446; continue;
        case 446: 4UL; pc[0] = 448; continue;
        case 448: __assert_fail("0", "queue_longest.c", 4, "__PRETTY_FUNCTION__"); pc[0] = -1; continue;
        case 450: __t1_active = 1; pc[0] = 452; continue;
        case 452: __t2_active = 1; pc[0] = 453; continue;
        case 453: if (__t1_active) { __t0_joins_t1 = 1; } else { __t0_joins_t1 = 0; pc[0] = 454; } continue;
        case 454: if (__t2_active) { __t0_joins_t2 = 1; } else { __t0_joins_t2 = 0; pc[0] = 455; } continue;
        case 455: __t0_active = 0; pc[0] = -1; continue;
        case 456: if ((__p0_19_q->head) == (__p0_19_q->tail)) { pc[0] = 458; } else if (!((__p0_19_q->head) == (__p0_19_q->tail))) { pc[0] = 461; } continue;
        case 458: printf("queue is empty\n"); pc[0] = 459; continue;
        case 459: 
            switch (__return_pc_t0_empty) {
              case 443: __t0_23___CPAchecker_TMP_0 = -1; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 775, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[0] = 460; continue;
        case 460: pc[0] = __return_pc_t0_empty; continue;
        case 461: 
            switch (__return_pc_t0_empty) {
              case 443: __t0_23___CPAchecker_TMP_0 = 0; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 782, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[0] = 460; continue;
        case 462: __p0_20_q->head = 0; pc[0] = 464; continue;
        case 464: __p0_20_q->tail = 0; pc[0] = 465; continue;
        case 465: __p0_20_q->amount = 0; pc[0] = 466; continue;
        case 466: pc[0] = __return_pc_t0_init; continue;
        default: __assert_fail("0", "mpor_seq__queue_longest.i", 789, "__SEQUENTIALIZATION_ERROR__");
      }

    } else if (next_thread == 1) {
      switch (pc[1]) {
        case 0: if (__g_14_m_locked) { __t1_awaits___g_14_m = 1; } else { __t1_awaits___g_14_m = 0; __g_14_m_locked = 1; pc[1] = 4; } continue;
        case 4: __t1_27_value = __VERIFIER_nondet_int(); pc[1] = 5; continue;
        case 5: __return_pc_t1_enqueue = 7; __p1_24_q = &__g_18_queue; __p1_25_x = __t1_27_value; pc[1] = 27; continue;
        case 7: if (__t1_29___CPAchecker_TMP_0 == 0) { pc[1] = 8; } else if (!(__t1_29___CPAchecker_TMP_0 == 0)) { pc[1] = 55; } continue;
        case 8: __g_15_stored_elements[0] = __t1_27_value; pc[1] = 9; continue;
        case 9: __return_pc_t1_empty = 11; __p1_26_q = &__g_18_queue; pc[1] = 49; continue;
        case 11: if (__t1_30___CPAchecker_TMP_1 == 0) { pc[1] = 12; } else if (!(__t1_30___CPAchecker_TMP_1 == 0)) { pc[1] = 42; } continue;
        case 12: __g_14_m_locked = 0; pc[1] = 13; continue;
        case 13: __t1_28_i = 0; pc[1] = 15; continue;
        case 15: if (__t1_28_i < 799) { pc[1] = 16; } else if (!(__t1_28_i < 799)) { pc[1] = 41; } continue;
        case 16: if (__g_14_m_locked) { __t1_awaits___g_14_m = 1; } else { __t1_awaits___g_14_m = 0; __g_14_m_locked = 1; pc[1] = 17; } continue;
        case 17: if (__g_16_enqueue_flag == 0) { pc[1] = 18; } else if (!(__g_16_enqueue_flag == 0)) { pc[1] = 21; } continue;
        case 18: __g_14_m_locked = 0; pc[1] = 19; continue;
        case 19: __t1_28_i = __t1_28_i + 1; pc[1] = 15; continue;
        case 21: __t1_27_value = __VERIFIER_nondet_int(); pc[1] = 22; continue;
        case 22: __return_pc_t1_enqueue = 23; __p1_24_q = &__g_18_queue; __p1_25_x = __t1_27_value; pc[1] = 27; continue;
        case 23: __g_15_stored_elements[__t1_28_i + 1] = __t1_27_value; pc[1] = 24; continue;
        case 24: __g_16_enqueue_flag = 0; pc[1] = 25; continue;
        case 25: __g_17_dequeue_flag = 1; pc[1] = 26; continue;
        case 26: __g_14_m_locked = 0; pc[1] = 19; continue;
        case 27: (__p1_24_q->element)[__p1_24_q->tail] = __p1_25_x; pc[1] = 29; continue;
        case 29: const int __t1_31___CPAchecker_TMP_0 = __p1_24_q->amount; __p1_24_q->amount = (__p1_24_q->amount) + 1; __t1_31___CPAchecker_TMP_0; pc[1] = 32; continue;
        case 32: if ((__p1_24_q->tail) == 800) { pc[1] = 33; } else if (!((__p1_24_q->tail) == 800)) { pc[1] = 37; } continue;
        case 33: __p1_24_q->tail = 1; pc[1] = 34; continue;
        case 34: 
            switch (__return_pc_t1_enqueue) {
              case 7: __t1_29___CPAchecker_TMP_0 = 0; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 821, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[1] = 36; continue;
        case 36: pc[1] = __return_pc_t1_enqueue; continue;
        case 37: const int __t1_32___CPAchecker_TMP_1 = __p1_24_q->tail; __p1_24_q->tail = (__p1_24_q->tail) + 1; __t1_32___CPAchecker_TMP_1; pc[1] = 40; continue;
        case 40: 
            switch (__return_pc_t1_enqueue) {
              case 7: __t1_29___CPAchecker_TMP_0 = 0; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 829, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[1] = 36; continue;
        case 41: __t1_active = 0; pc[1] = -1; continue;
        case 42: __return_pc_t1_reach_error = -1; pc[1] = 45; continue;
        case 45: 4UL; pc[1] = 47; continue;
        case 47: __assert_fail("0", "queue_longest.c", 4, "__PRETTY_FUNCTION__"); pc[1] = -1; continue;
        case 49: if ((__p1_26_q->head) == (__p1_26_q->tail)) { pc[1] = 51; } else if (!((__p1_26_q->head) == (__p1_26_q->tail))) { pc[1] = 54; } continue;
        case 51: printf("queue is empty\n"); pc[1] = 52; continue;
        case 52: 
            switch (__return_pc_t1_empty) {
              case 11: __t1_30___CPAchecker_TMP_1 = -1; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 841, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[1] = 53; continue;
        case 53: pc[1] = __return_pc_t1_empty; continue;
        case 54: 
            switch (__return_pc_t1_empty) {
              case 11: __t1_30___CPAchecker_TMP_1 = 0; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 848, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[1] = 53; continue;
        case 55: __return_pc_t1_reach_error = -1; pc[1] = 45; continue;
        default: __assert_fail("0", "mpor_seq__queue_longest.i", 852, "__SEQUENTIALIZATION_ERROR__");
      }

    } else if (next_thread == 2) {
      switch (pc[2]) {
        case 0: __t2_34_i = 0; pc[2] = 4; continue;
        case 4: if (__t2_34_i < 800) { pc[2] = 5; } else if (!(__t2_34_i < 800)) { pc[2] = 37; } continue;
        case 5: if (__g_14_m_locked) { __t2_awaits___g_14_m = 1; } else { __t2_awaits___g_14_m = 0; __g_14_m_locked = 1; pc[2] = 6; } continue;
        case 6: if (__g_17_dequeue_flag == 0) { pc[2] = 7; } else if (!(__g_17_dequeue_flag == 0)) { pc[2] = 10; } continue;
        case 7: __g_14_m_locked = 0; pc[2] = 8; continue;
        case 8: __t2_34_i = __t2_34_i + 1; pc[2] = 4; continue;
        case 10: __return_pc_t2_dequeue = 12; __p2_33_q = &__g_18_queue; pc[2] = 22; continue;
        case 12: if ((0 == __t2_35___CPAchecker_TMP_0) == (__g_15_stored_elements[__t2_34_i])) { pc[2] = 13; } else if (!((0 == __t2_35___CPAchecker_TMP_0) == (__g_15_stored_elements[__t2_34_i]))) { pc[2] = 19; } continue;
        case 13: __return_pc_t2_reach_error = -1; pc[2] = 15; continue;
        case 15: 4UL; pc[2] = 17; continue;
        case 17: __assert_fail("0", "queue_longest.c", 4, "__PRETTY_FUNCTION__"); pc[2] = -1; continue;
        case 19: __g_17_dequeue_flag = 0; pc[2] = 20; continue;
        case 20: __g_16_enqueue_flag = 1; pc[2] = 21; continue;
        case 21: __g_14_m_locked = 0; pc[2] = 8; continue;
        case 22: __t2_36_x = (__p2_33_q->element)[__p2_33_q->head]; pc[2] = 25; continue;
        case 25: const int __t2_37___CPAchecker_TMP_0 = __p2_33_q->amount; __p2_33_q->amount = (__p2_33_q->amount) - 1; __t2_37___CPAchecker_TMP_0; pc[2] = 28; continue;
        case 28: if ((__p2_33_q->head) == 800) { pc[2] = 29; } else if (!((__p2_33_q->head) == 800)) { pc[2] = 33; } continue;
        case 29: __p2_33_q->head = 1; pc[2] = 30; continue;
        case 30: 
            switch (__return_pc_t2_dequeue) {
              case 12: __t2_35___CPAchecker_TMP_0 = __t2_36_x; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 878, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[2] = 32; continue;
        case 32: pc[2] = __return_pc_t2_dequeue; continue;
        case 33: const int __t2_38___CPAchecker_TMP_1 = __p2_33_q->head; __p2_33_q->head = (__p2_33_q->head) + 1; __t2_38___CPAchecker_TMP_1; pc[2] = 36; continue;
        case 36: 
            switch (__return_pc_t2_dequeue) {
              case 12: __t2_35___CPAchecker_TMP_0 = __t2_36_x; break;
              default: __assert_fail("0", "mpor_seq__queue_longest.i", 886, "__SEQUENTIALIZATION_ERROR__");
            }
            pc[2] = 32; continue;
        case 37: __t2_active = 0; pc[2] = -1; continue;
        default: __assert_fail("0", "mpor_seq__queue_longest.i", 890, "__SEQUENTIALIZATION_ERROR__");
      }
    }
  }
  return 0;
}
