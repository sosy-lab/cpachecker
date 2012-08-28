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

#line 4 "token_ring.03.cil.c"
extern void t1_started() ;

#line 5
extern void t2_started() ;

#line 6
extern void t3_started() ;

#line 8 "token_ring.03.cil.c"
void error(void) 
{ 

  {
#line 11
  return;
}
}

#line 12 "token_ring.03.cil.c"
int m_pc   = 0;

#line 13 "token_ring.03.cil.c"
int t1_pc   = 0;

#line 14 "token_ring.03.cil.c"
int t2_pc   = 0;

#line 15 "token_ring.03.cil.c"
int t3_pc   = 0;

#line 16 "token_ring.03.cil.c"
int m_st  ;

#line 17 "token_ring.03.cil.c"
int t1_st  ;

#line 18 "token_ring.03.cil.c"
int t2_st  ;

#line 19 "token_ring.03.cil.c"
int t3_st  ;

#line 20 "token_ring.03.cil.c"
int m_i  ;

#line 21 "token_ring.03.cil.c"
int t1_i  ;

#line 22 "token_ring.03.cil.c"
int t2_i  ;

#line 23 "token_ring.03.cil.c"
int t3_i  ;

#line 24 "token_ring.03.cil.c"
int M_E   = 2;

#line 25 "token_ring.03.cil.c"
int T1_E   = 2;

#line 26 "token_ring.03.cil.c"
int T2_E   = 2;

#line 27 "token_ring.03.cil.c"
int T3_E   = 2;

#line 28 "token_ring.03.cil.c"
int E_M   = 2;

#line 29 "token_ring.03.cil.c"
int E_1   = 2;

#line 30 "token_ring.03.cil.c"
int E_2   = 2;

#line 31 "token_ring.03.cil.c"
int E_3   = 2;

#line 32
int is_master_triggered(void) ;

#line 33
int is_transmit1_triggered(void) ;

#line 34
int is_transmit2_triggered(void) ;

#line 35
int is_transmit3_triggered(void) ;

#line 36
void immediate_notify(void) ;

#line 37 "token_ring.03.cil.c"
int token  ;

#line 38
extern int __VERIFIER_nondet_int() ;

#line 39 "token_ring.03.cil.c"
int local  ;

#line 40 "token_ring.03.cil.c"
void master(void) 
{ 

  {
#line 44
  if (m_pc == 0) {
    goto M_ENTRY;
  } else {
#line 47
    if (m_pc == 1) {
      goto M_WAIT;
    }
  }
  M_ENTRY: 
  {

  }
#line 55
  while (1) {
    {
#line 58
    token = __VERIFIER_nondet_int();
#line 59
    local = token;
#line 60
    E_1 = 1;
#line 61
    immediate_notify();
#line 62
    E_1 = 2;
#line 63
    m_pc = 1;
#line 64
    m_st = 2;
    }
    goto return_label;
    M_WAIT: 
    {

    }
#line 69
    if (token != local + 3) {
      {
#line 71
      error();
      }
    }
  }
  {

  }
  return_label: 
#line 81
  return;
}
}

#line 84 "token_ring.03.cil.c"
void transmit1(void) 
{ 

  {
#line 88
  if (t1_pc == 0) {
    goto T1_ENTRY;
  } else {
#line 91
    if (t1_pc == 1) {
      goto T1_WAIT;
    }
  }
  T1_ENTRY: 
  {

  }
#line 99
  while (1) {
    {
#line 101
    t1_pc = 1;
#line 102
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
#line 20
      if (__MONITOR_STATE_executed_threads == 12) {
#line 21
        error_fn();
      } else {
#line 20
        if (__MONITOR_STATE_executed_threads == 13) {
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
              __MONITOR_STATE_executed_threads = 12;
            } else {
#line 29
              if (__MONITOR_STATE_executed_threads == 3) {
#line 30
                __MONITOR_STATE_executed_threads = 13;
              } else {
#line 32
                if (__MONITOR_STATE_executed_threads == 23) {
#line 33
                  __MONITOR_STATE_executed_threads = 0;
                }
              }
            }
          }
        }
      }
    }
#line 35
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 107 "token_ring.03.cil.c"
    t1_started();
#line 108
    token ++;
#line 109
    E_2 = 1;
#line 110
    immediate_notify();
#line 111
    E_2 = 2;
    }
  }
  {

  }
  return_label: 
#line 118
  return;
}
}

#line 121 "token_ring.03.cil.c"
void transmit2(void) 
{ 

  {
#line 125
  if (t2_pc == 0) {
    goto T2_ENTRY;
  } else {
#line 128
    if (t2_pc == 1) {
      goto T2_WAIT;
    }
  }
  T2_ENTRY: 
  {

  }
#line 136
  while (1) {
    {
#line 138
    t2_pc = 1;
#line 139
    t2_st = 2;
    }
    goto return_label;
    T2_WAIT: 
    {

    {
#line 45 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 46
    if (__MONITOR_STATE_executed_threads == 2) {
#line 47
      error_fn();
    } else {
#line 46
      if (__MONITOR_STATE_executed_threads == 12) {
#line 47
        error_fn();
      } else {
#line 46
        if (__MONITOR_STATE_executed_threads == 23) {
#line 47
          error_fn();
        } else {
#line 49
          if (__MONITOR_STATE_executed_threads == 0) {
#line 50
            __MONITOR_STATE_executed_threads = 2;
          } else {
#line 52
            if (__MONITOR_STATE_executed_threads == 1) {
#line 53
              __MONITOR_STATE_executed_threads = 12;
            } else {
#line 55
              if (__MONITOR_STATE_executed_threads == 3) {
#line 56
                __MONITOR_STATE_executed_threads = 23;
              } else {
#line 58
                if (__MONITOR_STATE_executed_threads == 13) {
#line 59
                  __MONITOR_STATE_executed_threads = 0;
                }
              }
            }
          }
        }
      }
    }
#line 61
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 144 "token_ring.03.cil.c"
    t2_started();
#line 145
    token ++;
#line 146
    E_3 = 1;
#line 147
    immediate_notify();
#line 148
    E_3 = 2;
    }
  }
  {

  }
  return_label: 
#line 155
  return;
}
}

#line 158 "token_ring.03.cil.c"
void transmit3(void) 
{ 

  {
#line 162
  if (t3_pc == 0) {
    goto T3_ENTRY;
  } else {
#line 165
    if (t3_pc == 1) {
      goto T3_WAIT;
    }
  }
  T3_ENTRY: 
  {

  }
#line 173
  while (1) {
    {
#line 175
    t3_pc = 1;
#line 176
    t3_st = 2;
    }
    goto return_label;
    T3_WAIT: 
    {

    {
#line 71 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 72
    if (__MONITOR_STATE_executed_threads == 3) {
#line 73
      error_fn();
    } else {
#line 72
      if (__MONITOR_STATE_executed_threads == 13) {
#line 73
        error_fn();
      } else {
#line 72
        if (__MONITOR_STATE_executed_threads == 23) {
#line 73
          error_fn();
        } else {
#line 75
          if (__MONITOR_STATE_executed_threads == 0) {
#line 76
            __MONITOR_STATE_executed_threads = 3;
          } else {
#line 78
            if (__MONITOR_STATE_executed_threads == 1) {
#line 79
              __MONITOR_STATE_executed_threads = 13;
            } else {
#line 81
              if (__MONITOR_STATE_executed_threads == 2) {
#line 82
                __MONITOR_STATE_executed_threads = 23;
              } else {
#line 84
                if (__MONITOR_STATE_executed_threads == 12) {
#line 85
                  __MONITOR_STATE_executed_threads = 0;
                }
              }
            }
          }
        }
      }
    }
#line 87
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 181 "token_ring.03.cil.c"
    t3_started();
#line 182
    token ++;
#line 183
    E_M = 1;
#line 184
    immediate_notify();
#line 185
    E_M = 2;
    }
  }
  {

  }
  return_label: 
#line 192
  return;
}
}

#line 195 "token_ring.03.cil.c"
int is_master_triggered(void) 
{ int __retres1 ;

  {
#line 199
  if (m_pc == 1) {
#line 200
    if (E_M == 1) {
      {
#line 201
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 209
  __retres1 = 0;
  }
  return_label: 
#line 211
  return (__retres1);
}
}

#line 214 "token_ring.03.cil.c"
int is_transmit1_triggered(void) 
{ int __retres1 ;

  {
#line 218
  if (t1_pc == 1) {
#line 219
    if (E_1 == 1) {
      {
#line 220
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 228
  __retres1 = 0;
  }
  return_label: 
#line 230
  return (__retres1);
}
}

#line 233 "token_ring.03.cil.c"
int is_transmit2_triggered(void) 
{ int __retres1 ;

  {
#line 237
  if (t2_pc == 1) {
#line 238
    if (E_2 == 1) {
      {
#line 239
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 247
  __retres1 = 0;
  }
  return_label: 
#line 249
  return (__retres1);
}
}

#line 252 "token_ring.03.cil.c"
int is_transmit3_triggered(void) 
{ int __retres1 ;

  {
#line 256
  if (t3_pc == 1) {
#line 257
    if (E_3 == 1) {
      {
#line 258
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 266
  __retres1 = 0;
  }
  return_label: 
#line 268
  return (__retres1);
}
}

#line 271 "token_ring.03.cil.c"
void update_channels(void) 
{ 

  {
#line 276
  return;
}
}

#line 279 "token_ring.03.cil.c"
void init_threads(void) 
{ 

  {
#line 283
  if (m_i == 1) {
    {
#line 284
    m_st = 0;
    }
  } else {
    {
#line 286
    m_st = 2;
    }
  }
#line 288
  if (t1_i == 1) {
    {
#line 289
    t1_st = 0;
    }
  } else {
    {
#line 291
    t1_st = 2;
    }
  }
#line 293
  if (t2_i == 1) {
    {
#line 294
    t2_st = 0;
    }
  } else {
    {
#line 296
    t2_st = 2;
    }
  }
#line 298
  if (t3_i == 1) {
    {
#line 299
    t3_st = 0;
    }
  } else {
    {
#line 301
    t3_st = 2;
    }
  }
#line 304
  return;
}
}

#line 307 "token_ring.03.cil.c"
int exists_runnable_thread(void) 
{ int __retres1 ;

  {
#line 311
  if (m_st == 0) {
    {
#line 312
    __retres1 = 1;
    }
    goto return_label;
  } else {
#line 315
    if (t1_st == 0) {
      {
#line 316
      __retres1 = 1;
      }
      goto return_label;
    } else {
#line 319
      if (t2_st == 0) {
        {
#line 320
        __retres1 = 1;
        }
        goto return_label;
      } else {
#line 323
        if (t3_st == 0) {
          {
#line 324
          __retres1 = 1;
          }
          goto return_label;
        }
      }
    }
  }
  {
#line 332
  __retres1 = 0;
  }
  return_label: 
#line 334
  return (__retres1);
}
}

#line 337 "token_ring.03.cil.c"
void eval(void) 
{ int tmp ;
  int tmp_ndt_1 ;
  int tmp_ndt_2 ;
  int tmp_ndt_3 ;
  int tmp_ndt_4 ;

  {
#line 343
  while (1) {
    {
#line 346
    tmp = exists_runnable_thread();
    }
#line 348
    if (! tmp) {
      goto while_4_break;
    }
#line 353
    if (m_st == 0) {
      {
#line 355
      tmp_ndt_1 = __VERIFIER_nondet_int();
      }
#line 356
      if (tmp_ndt_1) {
        {
#line 358
        m_st = 1;
#line 359
        master();
        }
      }
    }
#line 367
    if (t1_st == 0) {
      {
#line 369
      tmp_ndt_2 = __VERIFIER_nondet_int();
      }
#line 370
      if (tmp_ndt_2) {
        {
#line 372
        t1_st = 1;
#line 373
        transmit1();
        }
      }
    }
#line 381
    if (t2_st == 0) {
      {
#line 383
      tmp_ndt_3 = __VERIFIER_nondet_int();
      }
#line 384
      if (tmp_ndt_3) {
        {
#line 386
        t2_st = 1;
#line 387
        transmit2();
        }
      }
    }
#line 395
    if (t3_st == 0) {
      {
#line 397
      tmp_ndt_4 = __VERIFIER_nondet_int();
      }
#line 398
      if (tmp_ndt_4) {
        {
#line 400
        t3_st = 1;
#line 401
        transmit3();
        }
      }
    }
  }
  while_4_break: 
  {

  }
#line 413
  return;
}
}

#line 416 "token_ring.03.cil.c"
void fire_delta_events(void) 
{ 

  {
#line 420
  if (M_E == 0) {
    {
#line 421
    M_E = 1;
    }
  }
#line 425
  if (T1_E == 0) {
    {
#line 426
    T1_E = 1;
    }
  }
#line 430
  if (T2_E == 0) {
    {
#line 431
    T2_E = 1;
    }
  }
#line 435
  if (T3_E == 0) {
    {
#line 436
    T3_E = 1;
    }
  }
#line 440
  if (E_M == 0) {
    {
#line 441
    E_M = 1;
    }
  }
#line 445
  if (E_1 == 0) {
    {
#line 446
    E_1 = 1;
    }
  }
#line 450
  if (E_2 == 0) {
    {
#line 451
    E_2 = 1;
    }
  }
#line 455
  if (E_3 == 0) {
    {
#line 456
    E_3 = 1;
    }
  }
#line 461
  return;
}
}

#line 464 "token_ring.03.cil.c"
void reset_delta_events(void) 
{ 

  {
#line 468
  if (M_E == 1) {
    {
#line 469
    M_E = 2;
    }
  }
#line 473
  if (T1_E == 1) {
    {
#line 474
    T1_E = 2;
    }
  }
#line 478
  if (T2_E == 1) {
    {
#line 479
    T2_E = 2;
    }
  }
#line 483
  if (T3_E == 1) {
    {
#line 484
    T3_E = 2;
    }
  }
#line 488
  if (E_M == 1) {
    {
#line 489
    E_M = 2;
    }
  }
#line 493
  if (E_1 == 1) {
    {
#line 494
    E_1 = 2;
    }
  }
#line 498
  if (E_2 == 1) {
    {
#line 499
    E_2 = 2;
    }
  }
#line 503
  if (E_3 == 1) {
    {
#line 504
    E_3 = 2;
    }
  }
#line 509
  return;
}
}

#line 512 "token_ring.03.cil.c"
void activate_threads(void) 
{ int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;

  {
  {
#line 520
  tmp = is_master_triggered();
  }
#line 522
  if (tmp) {
    {
#line 523
    m_st = 0;
    }
  }
  {
#line 528
  tmp___0 = is_transmit1_triggered();
  }
#line 530
  if (tmp___0) {
    {
#line 531
    t1_st = 0;
    }
  }
  {
#line 536
  tmp___1 = is_transmit2_triggered();
  }
#line 538
  if (tmp___1) {
    {
#line 539
    t2_st = 0;
    }
  }
  {
#line 544
  tmp___2 = is_transmit3_triggered();
  }
#line 546
  if (tmp___2) {
    {
#line 547
    t3_st = 0;
    }
  }
#line 552
  return;
}
}

#line 555 "token_ring.03.cil.c"
void immediate_notify(void) 
{ 

  {
  {
#line 560
  activate_threads();
  }
#line 563
  return;
}
}

#line 566 "token_ring.03.cil.c"
void fire_time_events(void) 
{ 

  {
  {
#line 570
  M_E = 1;
  }
#line 572
  return;
}
}

#line 575 "token_ring.03.cil.c"
void reset_time_events(void) 
{ 

  {
#line 579
  if (M_E == 1) {
    {
#line 580
    M_E = 2;
    }
  }
#line 584
  if (T1_E == 1) {
    {
#line 585
    T1_E = 2;
    }
  }
#line 589
  if (T2_E == 1) {
    {
#line 590
    T2_E = 2;
    }
  }
#line 594
  if (T3_E == 1) {
    {
#line 595
    T3_E = 2;
    }
  }
#line 599
  if (E_M == 1) {
    {
#line 600
    E_M = 2;
    }
  }
#line 604
  if (E_1 == 1) {
    {
#line 605
    E_1 = 2;
    }
  }
#line 609
  if (E_2 == 1) {
    {
#line 610
    E_2 = 2;
    }
  }
#line 614
  if (E_3 == 1) {
    {
#line 615
    E_3 = 2;
    }
  }
#line 620
  return;
}
}

#line 623 "token_ring.03.cil.c"
void init_model(void) 
{ 

  {
  {
#line 627
  m_i = 1;
#line 628
  t1_i = 1;
#line 629
  t2_i = 1;
#line 630
  t3_i = 1;
  }
#line 632
  return;
}
}

#line 635 "token_ring.03.cil.c"
int stop_simulation(void) 
{ int tmp ;
  int __retres2 ;

  {
  {
#line 641
  tmp = exists_runnable_thread();
  }
#line 643
  if (tmp) {
    {
#line 644
    __retres2 = 0;
    }
    goto return_label;
  }
  {
#line 649
  __retres2 = 1;
  }
  return_label: 
#line 651
  return (__retres2);
}
}

#line 654 "token_ring.03.cil.c"
void start_simulation(void) 
{ int kernel_st ;
  int tmp ;
  int tmp___0 ;

  {
  {
#line 661
  kernel_st = 0;
#line 662
  update_channels();
#line 663
  init_threads();
#line 664
  fire_delta_events();
#line 665
  activate_threads();
#line 666
  reset_delta_events();
  }
#line 669
  while (1) {
    {
#line 672
    kernel_st = 1;
#line 673
    eval();
#line 676
    kernel_st = 2;
#line 677
    update_channels();
#line 680
    kernel_st = 3;
#line 681
    fire_delta_events();
#line 682
    activate_threads();
#line 683
    reset_delta_events();
#line 686
    tmp = exists_runnable_thread();
    }
#line 688
    if (tmp == 0) {
      {
#line 690
      kernel_st = 4;
#line 691
      fire_time_events();
#line 692
      activate_threads();
#line 693
      reset_time_events();
      }
    }
    {
#line 699
    tmp___0 = stop_simulation();
    }
#line 701
    if (tmp___0) {
      goto while_5_break;
    }
  }
  while_5_break: 
  {

  }
#line 710
  return;
}
}

#line 713 "token_ring.03.cil.c"
int entry(void) 
{ int __retres1 ;

  {
  {
#line 718
  init_model();
#line 719
  start_simulation();
#line 721
  __retres1 = 0;
  }
#line 722
  return (__retres1);
}
}

void __initialize__(void) 
{ 

  {

}
}

