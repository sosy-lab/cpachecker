// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: 2020 The ESBMC project
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }

#include <stdio.h> 
#include <pthread.h>

int  num;

pthread_mutex_t  m;
pthread_cond_t  empty, full;

void * thread1(void * arg)
{
  pthread_mutex_lock(&m);

  while (num > 0) 
    pthread_cond_wait(&empty, &m);
  
  num++;

  pthread_mutex_unlock(&m);
  pthread_cond_signal(&full);

  return 0;
}


void * thread2(void * arg)
{
  pthread_mutex_lock(&m);

  while (num == 0) 
    pthread_cond_wait(&full, &m);

  num--;
  
  pthread_mutex_unlock(&m);

  pthread_cond_signal(&empty);

  return 0;
}


int main()
{
  pthread_t  t1, t2;

  num = 1;

  pthread_mutex_init(&m, 0);
  pthread_cond_init(&empty, 0);
  pthread_cond_init(&full, 0);
  
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);
  
  pthread_join(t1, 0);
  pthread_join(t2, 0);

  if (num!=1)
  {
    ERROR: {reach_error();abort();}
    ;
  }

  return 0;
  
}
