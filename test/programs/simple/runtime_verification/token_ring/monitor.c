#line 1 "include.h"
void error_fn(void) 
{ 

  {
  ERROR: 
  goto ERROR;
}
}

#line 5 "include.h"
int __MONITOR_START_TRANSITION   = 0;

#line 6 "include.h"
int __MONITOR_END_TRANSITION   = 0;

#line 2 "spec.work"
int __BLAST_error  ;

#line 3 "spec.work"
void __error__(void) 
{ 

  {
#line 5
  __BLAST_error = 0;
  ERROR: 
  goto ERROR;
}
}

#line 8 "spec.work"
void __BLAST___error__(void) 
{ 

  {
#line 10
  __BLAST_error = 0;
  BERROR: 
  goto BERROR;
}
}

#line 13 "spec.work"
int __MONITOR_STATE_executed_threads   = 0;

void __initialize__(void) ;

#line 4 "token_ring.02.cil.c"
extern void t1_started() ;

#line 5
extern void t2_started() ;

#line 7 "token_ring.02.cil.c"
void error(void) 
{ 

  {
#line 9
  return;
}
}

#line 10 "token_ring.02.cil.c"
int m_pc   = 0;

#line 11 "token_ring.02.cil.c"
int t1_pc   = 0;

#line 12 "token_ring.02.cil.c"
int t2_pc   = 0;

#line 13 "token_ring.02.cil.c"
int m_st  ;

#line 14 "token_ring.02.cil.c"
int t1_st  ;

#line 15 "token_ring.02.cil.c"
int t2_st  ;

#line 16 "token_ring.02.cil.c"
int m_i  ;

#line 17 "token_ring.02.cil.c"
int t1_i  ;

#line 18 "token_ring.02.cil.c"
int t2_i  ;

#line 19 "token_ring.02.cil.c"
int M_E   = 2;

#line 20 "token_ring.02.cil.c"
int T1_E   = 2;

#line 21 "token_ring.02.cil.c"
int T2_E   = 2;

#line 22 "token_ring.02.cil.c"
int E_M   = 2;

#line 23 "token_ring.02.cil.c"
int E_1   = 2;

#line 24 "token_ring.02.cil.c"
int E_2   = 2;

#line 25
int is_master_triggered(void) ;

#line 26
int is_transmit1_triggered(void) ;

#line 27
int is_transmit2_triggered(void) ;

#line 28
void immediate_notify(void) ;

#line 29 "token_ring.02.cil.c"
int token  ;

#line 30
extern int __VERIFIER_nondet_int() ;

#line 31 "token_ring.02.cil.c"
int local  ;

#line 32 "token_ring.02.cil.c"
void master(void) 
{ 

  {
#line 36
  if (m_pc == 0) {
    goto M_ENTRY;
  } else {
#line 39
    if (m_pc == 1) {
      goto M_WAIT;
    }
  }
  M_ENTRY: 
  {

  }
#line 47
  while (1) {
    {
#line 50
    token = __VERIFIER_nondet_int();
#line 51
    local = token;
#line 52
    E_1 = 1;
#line 53
    immediate_notify();
#line 54
    E_1 = 2;
#line 55
    m_pc = 1;
#line 56
    m_st = 2;
    }
    goto return_label;
    M_WAIT: 
    {

    }
#line 61
    if (token != local + 2) {
      {
#line 63
      error();
      }
    }
  }
  {

  }
  return_label: 
#line 73
  return;
}
}

#line 76 "token_ring.02.cil.c"
void transmit1(void) 
{ 

  {
#line 80
  if (t1_pc == 0) {
    goto T1_ENTRY;
  } else {
#line 83
    if (t1_pc == 1) {
      goto T1_WAIT;
    }
  }
  T1_ENTRY: 
  {

  }
#line 91
  while (1) {
    {
#line 93
    t1_pc = 1;
#line 94
    t1_st = 2;
    }
    goto return_label;
    T1_WAIT: 
    {

    {
#line 19 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 20
    if (__MONITOR_STATE_executed_threads == 1) {
#line 21
      error_fn();
    } else {
#line 23
      if (__MONITOR_STATE_executed_threads == 0) {
#line 24
        __MONITOR_STATE_executed_threads = 1;
      } else {
#line 26
        if (__MONITOR_STATE_executed_threads == 2) {
#line 27
          __MONITOR_STATE_executed_threads = 0;
        }
      }
    }
#line 29
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 99 "token_ring.02.cil.c"
    t1_started();
#line 100
    token ++;
#line 101
    E_2 = 1;
#line 102
    immediate_notify();
#line 103
    E_2 = 2;
    }
  }
  {

  }
  return_label: 
#line 110
  return;
}
}

#line 113 "token_ring.02.cil.c"
void transmit2(void) 
{ 

  {
#line 117
  if (t2_pc == 0) {
    goto T2_ENTRY;
  } else {
#line 120
    if (t2_pc == 1) {
      goto T2_WAIT;
    }
  }
  T2_ENTRY: 
  {

  }
#line 128
  while (1) {
    {
#line 130
    t2_pc = 1;
#line 131
    t2_st = 2;
    }
    goto return_label;
    T2_WAIT: 
    {

    {
#line 39 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 40
    if (__MONITOR_STATE_executed_threads == 2) {
#line 41
      error_fn();
    } else {
#line 43
      if (__MONITOR_STATE_executed_threads == 1) {
#line 44
        __MONITOR_STATE_executed_threads = 0;
      } else {
#line 46
        if (__MONITOR_STATE_executed_threads == 0) {
#line 47
          __MONITOR_STATE_executed_threads = 2;
        }
      }
    }
#line 49
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 136 "token_ring.02.cil.c"
    t2_started();
#line 137
    token ++;
#line 138
    E_M = 1;
#line 139
    immediate_notify();
#line 140
    E_M = 2;
    }
  }
  {

  }
  return_label: 
#line 147
  return;
}
}

#line 150 "token_ring.02.cil.c"
int is_master_triggered(void) 
{ int __retres1 ;

  {
#line 154
  if (m_pc == 1) {
#line 155
    if (E_M == 1) {
      {
#line 156
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 164
  __retres1 = 0;
  }
  return_label: 
#line 166
  return (__retres1);
}
}

#line 169 "token_ring.02.cil.c"
int is_transmit1_triggered(void) 
{ int __retres1 ;

  {
#line 173
  if (t1_pc == 1) {
#line 174
    if (E_1 == 1) {
      {
#line 175
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 183
  __retres1 = 0;
  }
  return_label: 
#line 185
  return (__retres1);
}
}

#line 188 "token_ring.02.cil.c"
int is_transmit2_triggered(void) 
{ int __retres1 ;

  {
#line 192
  if (t2_pc == 1) {
#line 193
    if (E_2 == 1) {
      {
#line 194
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 202
  __retres1 = 0;
  }
  return_label: 
#line 204
  return (__retres1);
}
}

#line 207 "token_ring.02.cil.c"
void update_channels(void) 
{ 

  {
#line 212
  return;
}
}

#line 215 "token_ring.02.cil.c"
void init_threads(void) 
{ 

  {
#line 219
  if (m_i == 1) {
    {
#line 220
    m_st = 0;
    }
  } else {
    {
#line 222
    m_st = 2;
    }
  }
#line 224
  if (t1_i == 1) {
    {
#line 225
    t1_st = 0;
    }
  } else {
    {
#line 227
    t1_st = 2;
    }
  }
#line 229
  if (t2_i == 1) {
    {
#line 230
    t2_st = 0;
    }
  } else {
    {
#line 232
    t2_st = 2;
    }
  }
#line 235
  return;
}
}

#line 238 "token_ring.02.cil.c"
int exists_runnable_thread(void) 
{ int __retres1 ;

  {
#line 242
  if (m_st == 0) {
    {
#line 243
    __retres1 = 1;
    }
    goto return_label;
  } else {
#line 246
    if (t1_st == 0) {
      {
#line 247
      __retres1 = 1;
      }
      goto return_label;
    } else {
#line 250
      if (t2_st == 0) {
        {
#line 251
        __retres1 = 1;
        }
        goto return_label;
      }
    }
  }
  {
#line 258
  __retres1 = 0;
  }
  return_label: 
#line 260
  return (__retres1);
}
}

#line 263 "token_ring.02.cil.c"
void eval(void) 
{ int tmp ;
  int tmp_ndt_1 ;
  int tmp_ndt_2 ;
  int tmp_ndt_3 ;

  {
#line 269
  while (1) {
    {
#line 272
    tmp = exists_runnable_thread();
    }
#line 274
    if (! tmp) {
      goto while_3_break;
    }
#line 279
    if (m_st == 0) {
      {
#line 281
      tmp_ndt_1 = __VERIFIER_nondet_int();
      }
#line 282
      if (tmp_ndt_1) {
        {
#line 284
        m_st = 1;
#line 285
        master();
        }
      }
    }
#line 293
    if (t1_st == 0) {
      {
#line 295
      tmp_ndt_2 = __VERIFIER_nondet_int();
      }
#line 296
      if (tmp_ndt_2) {
        {
#line 298
        t1_st = 1;
#line 299
        transmit1();
        }
      }
    }
#line 307
    if (t2_st == 0) {
      {
#line 309
      tmp_ndt_3 = __VERIFIER_nondet_int();
      }
#line 310
      if (tmp_ndt_3) {
        {
#line 312
        t2_st = 1;
#line 313
        transmit2();
        }
      }
    }
  }
  while_3_break: 
  {

  }
#line 325
  return;
}
}

#line 328 "token_ring.02.cil.c"
void fire_delta_events(void) 
{ 

  {
#line 332
  if (M_E == 0) {
    {
#line 333
    M_E = 1;
    }
  }
#line 337
  if (T1_E == 0) {
    {
#line 338
    T1_E = 1;
    }
  }
#line 342
  if (T2_E == 0) {
    {
#line 343
    T2_E = 1;
    }
  }
#line 347
  if (E_M == 0) {
    {
#line 348
    E_M = 1;
    }
  }
#line 352
  if (E_1 == 0) {
    {
#line 353
    E_1 = 1;
    }
  }
#line 357
  if (E_2 == 0) {
    {
#line 358
    E_2 = 1;
    }
  }
#line 363
  return;
}
}

#line 366 "token_ring.02.cil.c"
void reset_delta_events(void) 
{ 

  {
#line 370
  if (M_E == 1) {
    {
#line 371
    M_E = 2;
    }
  }
#line 375
  if (T1_E == 1) {
    {
#line 376
    T1_E = 2;
    }
  }
#line 380
  if (T2_E == 1) {
    {
#line 381
    T2_E = 2;
    }
  }
#line 385
  if (E_M == 1) {
    {
#line 386
    E_M = 2;
    }
  }
#line 390
  if (E_1 == 1) {
    {
#line 391
    E_1 = 2;
    }
  }
#line 395
  if (E_2 == 1) {
    {
#line 396
    E_2 = 2;
    }
  }
#line 401
  return;
}
}

#line 404 "token_ring.02.cil.c"
void activate_threads(void) 
{ int tmp ;
  int tmp___0 ;
  int tmp___1 ;

  {
  {
#line 411
  tmp = is_master_triggered();
  }
#line 413
  if (tmp) {
    {
#line 414
    m_st = 0;
    }
  }
  {
#line 419
  tmp___0 = is_transmit1_triggered();
  }
#line 421
  if (tmp___0) {
    {
#line 422
    t1_st = 0;
    }
  }
  {
#line 427
  tmp___1 = is_transmit2_triggered();
  }
#line 429
  if (tmp___1) {
    {
#line 430
    t2_st = 0;
    }
  }
#line 435
  return;
}
}

#line 438 "token_ring.02.cil.c"
void immediate_notify(void) 
{ 

  {
  {
#line 443
  activate_threads();
  }
#line 446
  return;
}
}

#line 449 "token_ring.02.cil.c"
void fire_time_events(void) 
{ 

  {
  {
#line 453
  M_E = 1;
  }
#line 455
  return;
}
}

#line 458 "token_ring.02.cil.c"
void reset_time_events(void) 
{ 

  {
#line 462
  if (M_E == 1) {
    {
#line 463
    M_E = 2;
    }
  }
#line 467
  if (T1_E == 1) {
    {
#line 468
    T1_E = 2;
    }
  }
#line 472
  if (T2_E == 1) {
    {
#line 473
    T2_E = 2;
    }
  }
#line 477
  if (E_M == 1) {
    {
#line 478
    E_M = 2;
    }
  }
#line 482
  if (E_1 == 1) {
    {
#line 483
    E_1 = 2;
    }
  }
#line 487
  if (E_2 == 1) {
    {
#line 488
    E_2 = 2;
    }
  }
#line 493
  return;
}
}

#line 496 "token_ring.02.cil.c"
void init_model(void) 
{ 

  {
  {
#line 500
  m_i = 1;
#line 501
  t1_i = 1;
#line 502
  t2_i = 1;
  }
#line 504
  return;
}
}

#line 507 "token_ring.02.cil.c"
int stop_simulation(void) 
{ int tmp ;
  int __retres2 ;

  {
  {
#line 513
  tmp = exists_runnable_thread();
  }
#line 515
  if (tmp) {
    {
#line 516
    __retres2 = 0;
    }
    goto return_label;
  }
  {
#line 521
  __retres2 = 1;
  }
  return_label: 
#line 523
  return (__retres2);
}
}

#line 526 "token_ring.02.cil.c"
void start_simulation(void) 
{ int kernel_st ;
  int tmp ;
  int tmp___0 ;

  {
  {
#line 533
  kernel_st = 0;
#line 534
  update_channels();
#line 535
  init_threads();
#line 536
  fire_delta_events();
#line 537
  activate_threads();
#line 538
  reset_delta_events();
  }
#line 541
  while (1) {
    {
#line 544
    kernel_st = 1;
#line 545
    eval();
#line 548
    kernel_st = 2;
#line 549
    update_channels();
#line 552
    kernel_st = 3;
#line 553
    fire_delta_events();
#line 554
    activate_threads();
#line 555
    reset_delta_events();
#line 558
    tmp = exists_runnable_thread();
    }
#line 560
    if (tmp == 0) {
      {
#line 562
      kernel_st = 4;
#line 563
      fire_time_events();
#line 564
      activate_threads();
#line 565
      reset_time_events();
      }
    }
    {
#line 571
    tmp___0 = stop_simulation();
    }
#line 573
    if (tmp___0) {
      goto while_4_break;
    }
  }
  while_4_break: 
  {

  }
#line 582
  return;
}
}

#line 585 "token_ring.02.cil.c"
int entry(void) 
{ int __retres1 ;

  {
  {
#line 590
  init_model();
#line 591
  start_simulation();
#line 593
  __retres1 = 0;
  }
#line 594
  return (__retres1);
}
}

void __initialize__(void) 
{ 

  {

}
}

