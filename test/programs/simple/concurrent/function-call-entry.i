// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

typedef unsigned long int pthread_t;
union pthread_attr_t
{
    char __size[36];
    long int __align;
};
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
                          const pthread_attr_t *__restrict __attr,
                          void *(*__start_routine)(void *),
                          void *__restrict __arg);
extern int pthread_join(pthread_t __th, void **__thread_return);

static int writer_active = 0;
static int reader_count = 0;
static int data_value;
static int observed_value;
static int copied_value;

static void require_valid(int condition)
{
    if (!condition) {
        abort();
    }
}
static void acquire_writer(void)
{
    require_valid(writer_active == 0 && reader_count == 0);
    writer_active = 1;
}
static void acquire_reader(void)
{
    require_valid(writer_active == 0);
    reader_count++;
}
static void *writer_thread(void *arg)
{
    acquire_writer();
    __VERIFIER_atomic_begin();
    data_value = 12;
    __VERIFIER_atomic_end();
    __VERIFIER_atomic_begin();
    writer_active = 0;
    __VERIFIER_atomic_end();
    return (void *)0;
}
static void *reader_thread(void *arg)
{
    int local_value;
    acquire_reader();
    __VERIFIER_atomic_begin();
    local_value = data_value;
    __VERIFIER_atomic_end();
    __VERIFIER_atomic_begin();
    copied_value = local_value;
    __VERIFIER_atomic_end();
    if (!(copied_value == data_value)) {
        abort();
    }
    __VERIFIER_atomic_begin();
    reader_count = reader_count - 1;
    __VERIFIER_atomic_end();
    return (void *)0;
}
int main(void)
{
    pthread_t writer_id;
    pthread_t reader_id;
    pthread_create(&writer_id, (const pthread_attr_t *)0,
                   writer_thread, (void *)0);
    pthread_create(&reader_id, (const pthread_attr_t *)0,
                   reader_thread, (void *)0);
    pthread_join(writer_id, (void **)0);
    pthread_join(reader_id, (void **)0);
    return 0;
}
