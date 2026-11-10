// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long int pthread_t;
typedef struct
{
    int __lock;
    unsigned int __count;
    int __owner;
    int __kind;
    unsigned int __nusers;
    unsigned long int __reserved;
} pthread_mutex_t;
typedef struct
{
    char __size[36];
    long int __align;
} pthread_attr_t;
typedef struct
{
    char __size[4];
    int __align;
} pthread_mutexattr_t;
extern int pthread_create(pthread_t *__restrict thread,
                          const pthread_attr_t *__restrict attr,
                          void *(*start_routine)(void *),
                          void *__restrict arg);
extern int pthread_join(pthread_t thread, void **retval);
extern int pthread_mutex_init(pthread_mutex_t *mutex,
                              const pthread_mutexattr_t *attr);
extern int pthread_mutex_destroy(pthread_mutex_t *mutex);
extern int pthread_mutex_lock(pthread_mutex_t *mutex);
extern int pthread_mutex_unlock(pthread_mutex_t *mutex);

extern void abort(void);
extern void __assert_fail (const char *__assertion, const char *__file,
                              unsigned int __line, const char *__function)
                             __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "race-4_1-thread_local_vars.c", 8, __extension__ __PRETTY_FUNCTION__); })); }
extern int __VERIFIER_nondet_int(void);

static pthread_t coordinator_tid;
static pthread_t service_tid;

static pthread_mutex_t state_mutex;

static int resource_value;
static int service_state;
static int controller_state;

static void update_resource(void)
{
    pthread_mutex_lock(&state_mutex);
    resource_value = 14;
    pthread_mutex_unlock(&state_mutex);
}
static void *service_main(void *context)
{
    int terminate = 0;
    (void)context;
    while (!terminate) {
        int operation = __VERIFIER_nondet_int();
        switch (operation) {
        case 4:
            update_resource();
            break;
        case 9:
            terminate = 1;
            break;
        default:
            resource_value = resource_value;
            break;
        }
    }
    service_state = 0;
    return (void *)0;
}
static int start_service(void)
{
    if (__VERIFIER_nondet_int() == 0) {
        return -1;
    }
    pthread_create(&service_tid,
                   (const pthread_attr_t *)0,
                   service_main,
                   (void *)0);
    service_state = 1;
    return 0;
}
static void stop_service(void)
{
    void *return_value = (void *)0;
    pthread_join(service_tid, &return_value);
    service_state = 0;
}
static int device_open(void)
{
    int result = start_service();
    if (result != 0) {
        return -1;
    }
    controller_state = 3;
    return 0;
}
static void device_close(void)
{
    stop_service();
    controller_state = 5;
    resource_value = 22;
}
static void *controller_main(void *context)
{
    int done = 0;
    (void)context;
    controller_state = 0;
    while (!done) {
        int request = __VERIFIER_nondet_int();
        if (request == 1) {
            if (controller_state == 0) {
                if (device_open() != 0) {
                    done = 1;
                }
            }
        }
        else if (request == 2) {
            if (controller_state == 3) {
                device_close();
            }
        }
        else if (request == 3) {
            if (controller_state == 0) {
                done = 1;
            }
        }
    }
    resource_value = 26;
    return (void *)0;
}
static int subsystem_init(void)
{
    pthread_mutex_init(&state_mutex, (const pthread_mutexattr_t *)0);
    resource_value = 2;
    if (!__VERIFIER_nondet_int()) {
        resource_value = 7;
        pthread_mutex_destroy(&state_mutex);
        return -1;
    }
    pthread_create(&coordinator_tid,
                            (const pthread_attr_t *)0,
                            controller_main,
                            (void *)0);
    return 0;
}
static void subsystem_exit(void)
{
    void *return_value = (void *)0;
    pthread_join(coordinator_tid, &return_value);
    pthread_mutex_destroy(&state_mutex);
    resource_value = 35;
}
int main(void)
{
    if (subsystem_init() != 0) {
        return 0;
    }
    subsystem_exit();
    return 0;
}
