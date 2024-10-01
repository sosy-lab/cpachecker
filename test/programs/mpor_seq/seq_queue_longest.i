// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// original program declarations (non-variable)
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

// global variables
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

// thread 0 local variables
pthread_t __t0_21_id1;
pthread_t __t0_22_id2;
int __t0_23___CPAchecker_TMP_0;

// thread 1 local variables
int __t1_27_value;
int __t1_28_i;
int __t1_29___CPAchecker_TMP_0;
int __t1_30___CPAchecker_TMP_1;

// thread 2 local variables
int __t2_34_i;
int __t2_35___CPAchecker_TMP_0;
int __t2_36_x;

// thread 0 parameter variables
QType *__p0_19_q;
QType *__p0_20_q;

// thread 1 parameter variables
QType *__p1_24_q;
int __p1_25_x;
QType *__p1_26_q;

// thread 2 parameter variables
QType *__p2_33_q;

// thread local function return pc
int __return_pc_t0_init;
int __return_pc_t0_empty;
int __return_pc_t0_reach_error;
int __return_pc_t1_enqueue;
int __return_pc_t1_empty;
int __return_pc_t1_enqueue;
int __return_pc_t1_reach_error;
int __return_pc_t2_dequeue;
int __return_pc_t2_reach_error;

// pthread method replacements
int __t1_active = 0;
int __t2_active = 0;

// custom function declarations
int __VERIFIER_nondet_int();
void __mpor_seq_assume(int cond);
int __mpor_seq_any_unsigned(int array[], int size);

void __mpor_seq_assume(int cond) {
  if (!(cond)) {
    abort();
  }
}

int __mpor_seq_any_unsigned(int array[], int size) {
  int i = 0;
  while (i < size) {
    if (array[i] >= 0) {
      return 1;
    }
    i++;
  }
  return 0;
}

int main(void) {
  const int NUM_THREADS = 3;
  int pc[NUM_THREADS] = { 0, 0, 0 };
  int execute = 1;

  while (execute) {
    int next_thread = __VERIFIER_nondet_int();
    __mpor_seq_assume(0 <= next_thread && next_thread < NUM_THREADS);

    if (pc[next_thread] == -1) {
      execute = __mpor_seq_any_unsigned(pc, NUM_THREADS); 
      continue;
    }

    if (next_thread == 0) {
      switch (pc[0]) {
        case 0: pc[0] = 1; continue;
        case 1: pc[0] = 2; continue;
        case 2: pc[0] = 3; continue;
        case 3: pc[0] = 4; continue;
        case 4: pc[0] = 5; continue;
        case 5: pc[0] = 6; continue;
        case 6: pc[0] = 7; continue;
        case 7: pc[0] = 8; continue;
        case 8: pc[0] = 9; continue;
        case 9: pc[0] = 10; continue;
        case 10: pc[0] = 11; continue;
        case 11: pc[0] = 12; continue;
        case 12: pc[0] = 13; continue;
        case 13: pc[0] = 14; continue;
        case 14: pc[0] = 15; continue;
        case 15: pc[0] = 16; continue;
        case 16: pc[0] = 17; continue;
        case 17: pc[0] = 18; continue;
        case 18: pc[0] = 19; continue;
        case 19: pc[0] = 20; continue;
        case 20: pc[0] = 21; continue;
        case 21: pc[0] = 22; continue;
        case 22: pc[0] = 23; continue;
        case 23: pc[0] = 24; continue;
        case 24: pc[0] = 25; continue;
        case 25: pc[0] = 26; continue;
        case 26: pc[0] = 27; continue;
        case 27: pc[0] = 28; continue;
        case 28: pc[0] = 29; continue;
        case 29: pc[0] = 30; continue;
        case 30: pc[0] = 31; continue;
        case 31: pc[0] = 32; continue;
        case 32: pc[0] = 33; continue;
        case 33: pc[0] = 34; continue;
        case 34: pc[0] = 35; continue;
        case 35: pc[0] = 36; continue;
        case 36: pc[0] = 37; continue;
        case 37: pc[0] = 38; continue;
        case 38: pc[0] = 39; continue;
        case 39: pc[0] = 40; continue;
        case 40: pc[0] = 41; continue;
        case 41: pc[0] = 42; continue;
        case 42: pc[0] = 43; continue;
        case 43: pc[0] = 44; continue;
        case 44: pc[0] = 45; continue;
        case 45: pc[0] = 46; continue;
        case 46: pc[0] = 47; continue;
        case 47: pc[0] = 48; continue;
        case 48: pc[0] = 49; continue;
        case 49: pc[0] = 50; continue;
        case 50: pc[0] = 51; continue;
        case 51: pc[0] = 52; continue;
        case 52: pc[0] = 53; continue;
        case 53: pc[0] = 54; continue;
        case 54: pc[0] = 55; continue;
        case 55: pc[0] = 56; continue;
        case 56: pc[0] = 57; continue;
        case 57: pc[0] = 58; continue;
        case 58: pc[0] = 59; continue;
        case 59: pc[0] = 60; continue;
        case 60: pc[0] = 61; continue;
        case 61: pc[0] = 62; continue;
        case 62: pc[0] = 63; continue;
        case 63: pc[0] = 64; continue;
        case 64: pc[0] = 65; continue;
        case 65: pc[0] = 66; continue;
        case 66: pc[0] = 67; continue;
        case 67: pc[0] = 68; continue;
        case 68: pc[0] = 69; continue;
        case 69: pc[0] = 70; continue;
        case 70: pc[0] = 71; continue;
        case 71: pc[0] = 72; continue;
        case 72: pc[0] = 73; continue;
        case 73: pc[0] = 74; continue;
        case 74: pc[0] = 75; continue;
        case 75: pc[0] = 76; continue;
        case 76: pc[0] = 77; continue;
        case 77: pc[0] = 78; continue;
        case 78: pc[0] = 79; continue;
        case 79: pc[0] = 80; continue;
        case 80: pc[0] = 81; continue;
        case 81: pc[0] = 82; continue;
        case 82: pc[0] = 83; continue;
        case 83: pc[0] = 84; continue;
        case 84: pc[0] = 85; continue;
        case 85: pc[0] = 86; continue;
        case 86: pc[0] = 87; continue;
        case 87: pc[0] = 88; continue;
        case 88: pc[0] = 89; continue;
        case 89: pc[0] = 90; continue;
        case 90: pc[0] = 91; continue;
        case 91: pc[0] = 92; continue;
        case 92: pc[0] = 93; continue;
        case 93: pc[0] = 94; continue;
        case 94: pc[0] = 95; continue;
        case 95: pc[0] = 96; continue;
        case 96: pc[0] = 97; continue;
        case 97: pc[0] = 98; continue;
        case 98: pc[0] = 99; continue;
        case 99: pc[0] = 100; continue;
        case 100: pc[0] = 101; continue;
        case 101: pc[0] = 102; continue;
        case 102: pc[0] = 103; continue;
        case 103: pc[0] = 104; continue;
        case 104: pc[0] = 105; continue;
        case 105: pc[0] = 106; continue;
        case 106: pc[0] = 107; continue;
        case 107: pc[0] = 108; continue;
        case 108: pc[0] = 109; continue;
        case 109: pc[0] = 110; continue;
        case 110: pc[0] = 111; continue;
        case 111: pc[0] = 112; continue;
        case 112: pc[0] = 113; continue;
        case 113: pc[0] = 114; continue;
        case 114: pc[0] = 115; continue;
        case 115: pc[0] = 116; continue;
        case 116: pc[0] = 117; continue;
        case 117: pc[0] = 118; continue;
        case 118: pc[0] = 119; continue;
        case 119: pc[0] = 120; continue;
        case 120: pc[0] = 121; continue;
        case 121: pc[0] = 122; continue;
        case 122: pc[0] = 123; continue;
        case 123: pc[0] = 124; continue;
        case 124: pc[0] = 125; continue;
        case 125: pc[0] = 126; continue;
        case 126: pc[0] = 127; continue;
        case 127: pc[0] = 128; continue;
        case 128: pc[0] = 129; continue;
        case 129: pc[0] = 130; continue;
        case 130: pc[0] = 131; continue;
        case 131: pc[0] = 132; continue;
        case 132: pc[0] = 133; continue;
        case 133: pc[0] = 134; continue;
        case 134: pc[0] = 135; continue;
        case 135: pc[0] = 136; continue;
        case 136: pc[0] = 137; continue;
        case 137: pc[0] = 138; continue;
        case 138: pc[0] = 139; continue;
        case 139: pc[0] = 140; continue;
        case 140: pc[0] = 141; continue;
        case 141: pc[0] = 142; continue;
        case 142: pc[0] = 143; continue;
        case 143: pc[0] = 144; continue;
        case 144: pc[0] = 145; continue;
        case 145: pc[0] = 146; continue;
        case 146: pc[0] = 147; continue;
        case 147: pc[0] = 148; continue;
        case 148: pc[0] = 149; continue;
        case 149: pc[0] = 150; continue;
        case 150: pc[0] = 151; continue;
        case 151: pc[0] = 152; continue;
        case 152: pc[0] = 153; continue;
        case 153: pc[0] = 154; continue;
        case 154: pc[0] = 155; continue;
        case 155: pc[0] = 156; continue;
        case 156: pc[0] = 157; continue;
        case 157: pc[0] = 158; continue;
        case 158: pc[0] = 159; continue;
        case 159: pc[0] = 160; continue;
        case 160: pc[0] = 161; continue;
        case 161: pc[0] = 162; continue;
        case 162: pc[0] = 163; continue;
        case 163: pc[0] = 164; continue;
        case 164: pc[0] = 165; continue;
        case 165: pc[0] = 166; continue;
        case 166: pc[0] = 167; continue;
        case 167: pc[0] = 168; continue;
        case 168: pc[0] = 169; continue;
        case 169: pc[0] = 170; continue;
        case 170: pc[0] = 171; continue;
        case 171: pc[0] = 172; continue;
        case 172: pc[0] = 173; continue;
        case 173: pc[0] = 174; continue;
        case 174: pc[0] = 175; continue;
        case 175: pc[0] = 176; continue;
        case 176: pc[0] = 177; continue;
        case 177: pc[0] = 178; continue;
        case 178: pc[0] = 179; continue;
        case 179: pc[0] = 180; continue;
        case 180: pc[0] = 181; continue;
        case 181: pc[0] = 182; continue;
        case 182: pc[0] = 183; continue;
        case 183: pc[0] = 184; continue;
        case 184: pc[0] = 185; continue;
        case 185: pc[0] = 186; continue;
        case 186: pc[0] = 187; continue;
        case 187: pc[0] = 188; continue;
        case 188: pc[0] = 189; continue;
        case 189: pc[0] = 190; continue;
        case 190: pc[0] = 191; continue;
        case 191: pc[0] = 192; continue;
        case 192: pc[0] = 193; continue;
        case 193: pc[0] = 194; continue;
        case 194: pc[0] = 195; continue;
        case 195: pc[0] = 196; continue;
        case 196: pc[0] = 197; continue;
        case 197: pc[0] = 198; continue;
        case 198: pc[0] = 199; continue;
        case 199: pc[0] = 200; continue;
        case 200: pc[0] = 201; continue;
        case 201: pc[0] = 202; continue;
        case 202: pc[0] = 203; continue;
        case 203: pc[0] = 204; continue;
        case 204: pc[0] = 205; continue;
        case 205: pc[0] = 206; continue;
        case 206: pc[0] = 207; continue;
        case 207: pc[0] = 208; continue;
        case 208: pc[0] = 209; continue;
        case 209: pc[0] = 210; continue;
        case 210: pc[0] = 211; continue;
        case 211: pc[0] = 212; continue;
        case 212: pc[0] = 213; continue;
        case 213: pc[0] = 214; continue;
        case 214: pc[0] = 215; continue;
        case 215: pc[0] = 216; continue;
        case 216: pc[0] = 217; continue;
        case 217: pc[0] = 218; continue;
        case 218: pc[0] = 219; continue;
        case 219: pc[0] = 220; continue;
        case 220: pc[0] = 221; continue;
        case 221: pc[0] = 222; continue;
        case 222: pc[0] = 223; continue;
        case 223: pc[0] = 224; continue;
        case 224: pc[0] = 225; continue;
        case 225: pc[0] = 226; continue;
        case 226: pc[0] = 227; continue;
        case 227: pc[0] = 228; continue;
        case 228: pc[0] = 229; continue;
        case 229: pc[0] = 230; continue;
        case 230: pc[0] = 231; continue;
        case 231: pc[0] = 232; continue;
        case 232: pc[0] = 233; continue;
        case 233: pc[0] = 234; continue;
        case 234: pc[0] = 235; continue;
        case 235: pc[0] = 236; continue;
        case 236: pc[0] = 237; continue;
        case 237: pc[0] = 238; continue;
        case 238: pc[0] = 239; continue;
        case 239: pc[0] = 240; continue;
        case 240: pc[0] = 241; continue;
        case 241: pc[0] = 242; continue;
        case 242: pc[0] = 243; continue;
        case 243: pc[0] = 244; continue;
        case 244: pc[0] = 245; continue;
        case 245: pc[0] = 246; continue;
        case 246: pc[0] = 247; continue;
        case 247: pc[0] = 248; continue;
        case 248: pc[0] = 249; continue;
        case 249: pc[0] = 250; continue;
        case 250: pc[0] = 251; continue;
        case 251: pc[0] = 252; continue;
        case 252: pc[0] = 253; continue;
        case 253: pc[0] = 254; continue;
        case 254: pc[0] = 255; continue;
        case 255: pc[0] = 256; continue;
        case 256: pc[0] = 257; continue;
        case 257: pc[0] = 258; continue;
        case 258: pc[0] = 259; continue;
        case 259: pc[0] = 260; continue;
        case 260: pc[0] = 261; continue;
        case 261: pc[0] = 262; continue;
        case 262: pc[0] = 263; continue;
        case 263: pc[0] = 264; continue;
        case 264: pc[0] = 265; continue;
        case 265: pc[0] = 266; continue;
        case 266: pc[0] = 267; continue;
        case 267: pc[0] = 268; continue;
        case 268: pc[0] = 269; continue;
        case 269: pc[0] = 270; continue;
        case 270: pc[0] = 271; continue;
        case 271: pc[0] = 272; continue;
        case 272: pc[0] = 273; continue;
        case 273: pc[0] = 274; continue;
        case 274: pc[0] = 275; continue;
        case 275: pc[0] = 276; continue;
        case 276: pc[0] = 277; continue;
        case 277: pc[0] = 278; continue;
        case 278: pc[0] = 279; continue;
        case 279: pc[0] = 280; continue;
        case 280: pc[0] = 281; continue;
        case 281: pc[0] = 282; continue;
        case 282: pc[0] = 283; continue;
        case 283: pc[0] = 284; continue;
        case 284: pc[0] = 285; continue;
        case 285: pc[0] = 286; continue;
        case 286: pc[0] = 287; continue;
        case 287: pc[0] = 288; continue;
        case 288: pc[0] = 289; continue;
        case 289: pc[0] = 290; continue;
        case 290: pc[0] = 291; continue;
        case 291: pc[0] = 292; continue;
        case 292: pc[0] = 293; continue;
        case 293: pc[0] = 294; continue;
        case 294: pc[0] = 295; continue;
        case 295: pc[0] = 296; continue;
        case 296: pc[0] = 297; continue;
        case 297: pc[0] = 298; continue;
        case 298: pc[0] = 299; continue;
        case 299: pc[0] = 300; continue;
        case 300: pc[0] = 301; continue;
        case 301: pc[0] = 302; continue;
        case 302: pc[0] = 303; continue;
        case 303: pc[0] = 304; continue;
        case 304: pc[0] = 305; continue;
        case 305: pc[0] = 306; continue;
        case 306: pc[0] = 307; continue;
        case 307: pc[0] = 308; continue;
        case 308: pc[0] = 309; continue;
        case 309: pc[0] = 310; continue;
        case 310: pc[0] = 311; continue;
        case 311: pc[0] = 312; continue;
        case 312: pc[0] = 313; continue;
        case 313: pc[0] = 314; continue;
        case 314: pc[0] = 315; continue;
        case 315: pc[0] = 316; continue;
        case 316: pc[0] = 317; continue;
        case 317: pc[0] = 318; continue;
        case 318: pc[0] = 319; continue;
        case 319: pc[0] = 320; continue;
        case 320: pc[0] = 321; continue;
        case 321: pc[0] = 322; continue;
        case 322: pc[0] = 323; continue;
        case 323: pc[0] = 324; continue;
        case 324: pc[0] = 325; continue;
        case 325: pc[0] = 326; continue;
        case 326: pc[0] = 327; continue;
        case 327: pc[0] = 328; continue;
        case 328: pc[0] = 329; continue;
        case 329: pc[0] = 330; continue;
        case 330: pc[0] = 331; continue;
        case 331: pc[0] = 332; continue;
        case 332: pc[0] = 333; continue;
        case 333: pc[0] = 334; continue;
        case 334: pc[0] = 335; continue;
        case 335: pc[0] = 336; continue;
        case 336: pc[0] = 337; continue;
        case 337: pc[0] = 338; continue;
        case 338: pc[0] = 339; continue;
        case 339: pc[0] = 340; continue;
        case 340: pc[0] = 341; continue;
        case 341: pc[0] = 342; continue;
        case 342: pc[0] = 343; continue;
        case 343: pc[0] = 344; continue;
        case 344: pc[0] = 345; continue;
        case 345: pc[0] = 346; continue;
        case 346: pc[0] = 347; continue;
        case 347: pc[0] = 348; continue;
        case 348: pc[0] = 349; continue;
        case 349: pc[0] = 350; continue;
        case 350: pc[0] = 351; continue;
        case 351: pc[0] = 352; continue;
        case 352: pc[0] = 353; continue;
        case 353: pc[0] = 354; continue;
        case 354: pc[0] = 355; continue;
        case 355: pc[0] = 356; continue;
        case 356: pc[0] = 357; continue;
        case 357: pc[0] = 358; continue;
        case 358: pc[0] = 359; continue;
        case 359: pc[0] = 360; continue;
        case 360: pc[0] = 361; continue;
        case 361: pc[0] = 362; continue;
        case 362: pc[0] = 363; continue;
        case 363: pc[0] = 364; continue;
        case 364: pc[0] = 365; continue;
        case 365: pc[0] = 366; continue;
        case 366: pc[0] = 367; continue;
        case 367: pc[0] = 368; continue;
        case 368: pc[0] = 369; continue;
        case 369: pc[0] = 370; continue;
        case 370: pc[0] = 371; continue;
        case 371: pc[0] = 372; continue;
        case 372: pc[0] = 373; continue;
        case 373: pc[0] = 374; continue;
        case 374: pc[0] = 375; continue;
        case 375: pc[0] = 376; continue;
        case 376: pc[0] = 377; continue;
        case 377: pc[0] = 378; continue;
        case 378: pc[0] = 379; continue;
        case 379: pc[0] = 380; continue;
        case 380: pc[0] = 381; continue;
        case 381: pc[0] = 382; continue;
        case 382: pc[0] = 383; continue;
        case 383: pc[0] = 384; continue;
        case 384: pc[0] = 385; continue;
        case 385: pc[0] = 386; continue;
        case 386: pc[0] = 387; continue;
        case 387: pc[0] = 388; continue;
        case 388: pc[0] = 389; continue;
        case 389: pc[0] = 390; continue;
        case 390: pc[0] = 391; continue;
        case 391: pc[0] = 392; continue;
        case 392: pc[0] = 393; continue;
        case 393: pc[0] = 394; continue;
        case 394: pc[0] = 395; continue;
        case 395: pc[0] = 396; continue;
        case 396: pc[0] = 397; continue;
        case 397: pc[0] = 398; continue;
        case 398: pc[0] = 399; continue;
        case 399: pc[0] = 400; continue;
        case 400: pc[0] = 401; continue;
        case 401: pc[0] = 402; continue;
        case 402: pc[0] = 403; continue;
        case 403: pc[0] = 404; continue;
        case 404: pc[0] = 405; continue;
        case 405: pc[0] = 406; continue;
        case 406: pc[0] = 407; continue;
        case 407: pc[0] = 408; continue;
        case 408: pc[0] = 409; continue;
        case 409: pc[0] = 410; continue;
        case 410: pc[0] = 411; continue;
        case 411: pc[0] = 412; continue;
        case 412: pc[0] = 413; continue;
        case 413: pc[0] = 414; continue;
        case 414: pc[0] = 415; continue;
        case 415: pc[0] = 416; continue;
        case 416: pc[0] = 417; continue;
        case 417: pc[0] = 418; continue;
        case 418: pc[0] = 419; continue;
        case 419: pc[0] = 420; continue;
        case 420: pc[0] = 421; continue;
        case 421: pc[0] = 422; continue;
        case 422: pc[0] = 423; continue;
        case 423: pc[0] = 424; continue;
        case 424: pc[0] = 425; continue;
        case 425: pc[0] = 426; continue;
        case 426: pc[0] = 427; continue;
        case 427: pc[0] = 428; continue;
        case 428: pc[0] = 429; continue;
        case 429: pc[0] = 430; continue;
        case 430: pc[0] = 431; continue;
        case 431: pc[0] = 432; continue;
        case 432: pc[0] = 433; continue;
        case 433: pc[0] = 434; continue;
        case 434: pc[0] = 435; continue;
        case 435: pc[0] = 436; continue;
        case 436: pc[0] = 437; continue;
        case 437: pc[0] = 438; continue;
        case 438: __g_16_enqueue_flag = 1; pc[0] = 439; continue;
        case 439: __g_17_dequeue_flag = 0; pc[0] = 440; continue;
        case 440: __p0_20_q = &__g_18_queue;  pc[0] = 462; __return_pc_t0_init = 441;  continue;
        case 441: pc[0] = 442; continue;
        case 442: __return_pc_t0_empty = 443;  __p0_19_q = &__g_18_queue;  pc[0] = 456; continue;
        case 443: if ((0 == __t0_23___CPAchecker_TMP_0) == -1) { pc[0] = 444;  }else if (!((0 == __t0_23___CPAchecker_TMP_0) == -1)) { pc[0] = 450;  }continue;
        case 444: pc[0] = 445; continue;
        case 445: __return_pc_t0_reach_error = -1;  continue;
        case 446: pc[0] = 447; continue;
        case 447: 4UL; pc[0] = 448; continue;
        case 448: pc[0] = 449; continue;
        case 449: __assert_fail("0", "queue_longest.c", 4, "__PRETTY_FUNCTION__"); pc[0] = -1; continue;
        case 450: pthread_mutex_init(&__g_14_m, 0); pc[0] = 451; continue;
        case 451: __t1_active = 1; pc[0] = 452; continue;
        case 452: __t2_active = 1; pc[0] = 453; continue;
        case 453: pthread_join(__t0_21_id1, (void *)0); pc[0] = 454; continue;
        case 454: pthread_join(__t0_22_id2, (void *)0); pc[0] = 455; continue;
        case 455: pc[0] = -1; continue;
        case 456: pc[0] = 457; continue;
        case 457: if (!((__p0_19_q->head) == (__p0_19_q->tail))) { pc[0] = 461;  }else if ((__p0_19_q->head) == (__p0_19_q->tail)) { pc[0] = 458;  }continue;
        case 458: printf("queue is empty\n"); pc[0] = 459; continue;
        case 459: __t0_23___CPAchecker_TMP_0 = -1;  pc[0] = 460; continue;
        case 460: pc[0] = __return_pc_t0_empty;  continue;
        case 461: __t0_23___CPAchecker_TMP_0 = 0;  pc[0] = 460; continue;
        case 462: pc[0] = 463; continue;
        case 463: __p0_20_q->head = 0; pc[0] = 464; continue;
        case 464: __p0_20_q->tail = 0; pc[0] = 465; continue;
        case 465: __p0_20_q->amount = 0; pc[0] = 466; continue;
        case 466: pc[0] = 467; continue;
        case 467: pc[0] = __return_pc_t0_init;  continue;
      }

    } else if (next_thread == 1) {
      switch (pc[1]) {
        case 0: pc[1] = 1; continue;
        case 1: pc[1] = 2; continue;
        case 2: pc[1] = 3; continue;
        case 3: pthread_mutex_lock(&__g_14_m); pc[1] = 4; continue;
        case 4: __t1_27_value = __VERIFIER_nondet_int(); pc[1] = 5; continue;
        case 5: pc[1] = 6; continue;
        case 6: __return_pc_t1_enqueue = 7;  __p1_24_q = &__g_18_queue;  __p1_25_x = __t1_27_value;  pc[1] = 27; continue;
        case 7: if (__t1_29___CPAchecker_TMP_0 == 0) { pc[1] = 8;  }else if (!(__t1_29___CPAchecker_TMP_0 == 0)) { pc[1] = 55;  }continue;
        case 8: __g_15_stored_elements[0] = __t1_27_value; pc[1] = 9; continue;
        case 9: pc[1] = 10; continue;
        case 10: __return_pc_t1_empty = 11;  __p1_26_q = &__g_18_queue;  pc[1] = 49; continue;
        case 11: if (__t1_30___CPAchecker_TMP_1 == 0) { pc[1] = 12;  }else if (!(__t1_30___CPAchecker_TMP_1 == 0)) { pc[1] = 42;  }continue;
        case 12: pthread_mutex_unlock(&__g_14_m); pc[1] = 13; continue;
        case 13: pc[1] = 14; continue;
        case 14: __t1_28_i = 0; pc[1] = 15; continue;
        case 15: if (__t1_28_i < 799) { pc[1] = 16;  }else if (!(__t1_28_i < 799)) { pc[1] = 41;  }continue;
        case 16: pthread_mutex_lock(&__g_14_m); pc[1] = 17; continue;
        case 17: if (__g_16_enqueue_flag == 0) { pc[1] = 18;  }else if (!(__g_16_enqueue_flag == 0)) { pc[1] = 21;  }continue;
        case 18: pthread_mutex_unlock(&__g_14_m); pc[1] = 19; continue;
        case 19: pc[1] = 20; continue;
        case 20: __t1_28_i = __t1_28_i + 1; pc[1] = 15; continue;
        case 21: __t1_27_value = __VERIFIER_nondet_int(); pc[1] = 22; continue;
        case 22: __p1_24_q = &__g_18_queue;  __p1_25_x = __t1_27_value;  pc[1] = 27; __return_pc_t1_enqueue = 23;  continue;
        case 23: __g_15_stored_elements[__t1_28_i + 1] = __t1_27_value; pc[1] = 24; continue;
        case 24: __g_16_enqueue_flag = 0; pc[1] = 25; continue;
        case 25: __g_17_dequeue_flag = 1; pc[1] = 26; continue;
        case 26: pc[1] = 18; continue;
        case 27: pc[1] = 28; continue;
        case 28: (__p1_24_q->element)[__p1_24_q->tail] = __p1_25_x; pc[1] = 29; continue;
        case 29: const int __t1_31___CPAchecker_TMP_0 = __p1_24_q->amount; __p1_24_q->amount = (__p1_24_q->amount) + 1; __t1_31___CPAchecker_TMP_0; pc[1] = 32; continue;
        case 32: if (!((__p1_24_q->tail) == 800)) { pc[1] = 37;  }else if ((__p1_24_q->tail) == 800) { pc[1] = 33;  }continue;
        case 33: __p1_24_q->tail = 1; pc[1] = 34; continue;
        case 34: pc[1] = 35; continue;
        case 35: __t1_29___CPAchecker_TMP_0 = 0;  pc[1] = 36; continue;
        case 36: pc[1] = __return_pc_t1_enqueue;  continue;
        case 37: const int __t1_32___CPAchecker_TMP_1 = __p1_24_q->tail; __p1_24_q->tail = (__p1_24_q->tail) + 1; __t1_32___CPAchecker_TMP_1; pc[1] = 40; continue;
        case 40: pc[1] = 35; continue;
        case 41: __t1_active = 0; pc[1] = -1; continue;
        case 42: pc[1] = 43; continue;
        case 43: pc[1] = 44; continue;
        case 44: __return_pc_t1_reach_error = -1;  continue;
        case 45: pc[1] = 46; continue;
        case 46: 4UL; pc[1] = 47; continue;
        case 47: pc[1] = 48; continue;
        case 48: __assert_fail("0", "queue_longest.c", 4, "__PRETTY_FUNCTION__"); pc[1] = -1; continue;
        case 49: pc[1] = 50; continue;
        case 50: if ((__p1_26_q->head) == (__p1_26_q->tail)) { pc[1] = 51;  }else if (!((__p1_26_q->head) == (__p1_26_q->tail))) { pc[1] = 54;  }continue;
        case 51: printf("queue is empty\n"); pc[1] = 52; continue;
        case 52: __t1_30___CPAchecker_TMP_1 = -1;  pc[1] = 53; continue;
        case 53: pc[1] = __return_pc_t1_empty;  continue;
        case 54: __t1_30___CPAchecker_TMP_1 = 0;  pc[1] = 53; continue;
        case 55: pc[1] = 43; continue;
      }

    } else if (next_thread == 2) {
      switch (pc[2]) {
        case 0: pc[2] = 1; continue;
        case 1: pc[2] = 2; continue;
        case 2: pc[2] = 3; continue;
        case 3: __t2_34_i = 0; pc[2] = 4; continue;
        case 4: if (__t2_34_i < 800) { pc[2] = 5;  }else if (!(__t2_34_i < 800)) { pc[2] = 37;  }continue;
        case 5: pthread_mutex_lock(&__g_14_m); pc[2] = 6; continue;
        case 6: if (!(__g_17_dequeue_flag == 0)) { pc[2] = 10;  }else if (__g_17_dequeue_flag == 0) { pc[2] = 7;  }continue;
        case 7: pthread_mutex_unlock(&__g_14_m); pc[2] = 8; continue;
        case 8: pc[2] = 9; continue;
        case 9: __t2_34_i = __t2_34_i + 1; pc[2] = 4; continue;
        case 10: pc[2] = 11; continue;
        case 11: __return_pc_t2_dequeue = 12;  __p2_33_q = &__g_18_queue;  pc[2] = 22; continue;
        case 12: if ((0 == __t2_35___CPAchecker_TMP_0) == (__g_15_stored_elements[__t2_34_i])) { pc[2] = 13;  }else if (!((0 == __t2_35___CPAchecker_TMP_0) == (__g_15_stored_elements[__t2_34_i]))) { pc[2] = 19;  }continue;
        case 13: pc[2] = 14; continue;
        case 14: __return_pc_t2_reach_error = -1;  continue;
        case 15: pc[2] = 16; continue;
        case 16: 4UL; pc[2] = 17; continue;
        case 17: pc[2] = 18; continue;
        case 18: __assert_fail("0", "queue_longest.c", 4, "__PRETTY_FUNCTION__"); pc[2] = -1; continue;
        case 19: __g_17_dequeue_flag = 0; pc[2] = 20; continue;
        case 20: __g_16_enqueue_flag = 1; pc[2] = 21; continue;
        case 21: pc[2] = 7; continue;
        case 22: pc[2] = 23; continue;
        case 23: pc[2] = 24; continue;
        case 24: __t2_36_x = (__p2_33_q->element)[__p2_33_q->head]; pc[2] = 25; continue;
        case 25: const int __t2_37___CPAchecker_TMP_0 = __p2_33_q->amount; __p2_33_q->amount = (__p2_33_q->amount) - 1; __t2_37___CPAchecker_TMP_0; pc[2] = 28; continue;
        case 28: if (!((__p2_33_q->head) == 800)) { pc[2] = 33;  }else if ((__p2_33_q->head) == 800) { pc[2] = 29;  }continue;
        case 29: __p2_33_q->head = 1; pc[2] = 30; continue;
        case 30: pc[2] = 31; continue;
        case 31: __t2_35___CPAchecker_TMP_0 = __t2_36_x;  pc[2] = 32; continue;
        case 32: pc[2] = __return_pc_t2_dequeue;  continue;
        case 33: const int __t2_38___CPAchecker_TMP_1 = __p2_33_q->head; __p2_33_q->head = (__p2_33_q->head) + 1; __t2_38___CPAchecker_TMP_1; pc[2] = 36; continue;
        case 36: pc[2] = 31; continue;
        case 37: __t2_active = 0; pc[2] = -1; continue;
      }
    }
  }
}