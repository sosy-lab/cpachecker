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

#line 4 "token_ring.04.cil.c"
void error(void) 
{ 

  {
#line 7 "token_ring.04.cil.c"
  return;
}
}

#line 8 "token_ring.04.cil.c"
int m_pc   = 0;

#line 9 "token_ring.04.cil.c"
int t1_pc   = 0;

#line 10 "token_ring.04.cil.c"
int t2_pc   = 0;

#line 11 "token_ring.04.cil.c"
int t3_pc   = 0;

#line 12 "token_ring.04.cil.c"
int t4_pc   = 0;

#line 13 "token_ring.04.cil.c"
int m_st  ;

#line 14 "token_ring.04.cil.c"
int t1_st  ;

#line 15 "token_ring.04.cil.c"
int t2_st  ;

#line 16 "token_ring.04.cil.c"
int t3_st  ;

#line 17 "token_ring.04.cil.c"
int t4_st  ;

#line 18 "token_ring.04.cil.c"
int m_i  ;

#line 19 "token_ring.04.cil.c"
int t1_i  ;

#line 20 "token_ring.04.cil.c"
int t2_i  ;

#line 21 "token_ring.04.cil.c"
int t3_i  ;

#line 22 "token_ring.04.cil.c"
int t4_i  ;

#line 23 "token_ring.04.cil.c"
int M_E   = 2;

#line 24 "token_ring.04.cil.c"
int T1_E   = 2;

#line 25 "token_ring.04.cil.c"
int T2_E   = 2;

#line 26 "token_ring.04.cil.c"
int T3_E   = 2;

#line 27 "token_ring.04.cil.c"
int T4_E   = 2;

#line 28 "token_ring.04.cil.c"
int E_M   = 2;

#line 29 "token_ring.04.cil.c"
int E_1   = 2;

#line 30 "token_ring.04.cil.c"
int E_2   = 2;

#line 31 "token_ring.04.cil.c"
int E_3   = 2;

#line 32 "token_ring.04.cil.c"
int E_4   = 2;

#line 33
int is_master_triggered(void) ;

#line 34
int is_transmit1_triggered(void) ;

#line 35
int is_transmit2_triggered(void) ;

#line 36
int is_transmit3_triggered(void) ;

#line 37
int is_transmit4_triggered(void) ;

#line 38
void immediate_notify(void) ;

#line 39 "token_ring.04.cil.c"
int token  ;

#line 40
extern int __VERIFIER_nondet_int() ;

#line 41 "token_ring.04.cil.c"
int local  ;

#line 42 "token_ring.04.cil.c"
void master(void) 
{ 

  {
#line 46
  if (m_pc == 0) {
    goto M_ENTRY;
  } else {
#line 49
    if (m_pc == 1) {
      goto M_WAIT;
    }
  }
  M_ENTRY: 
  {

  }
#line 57
  while (1) {
    {
#line 60
    token = __VERIFIER_nondet_int();
#line 61
    local = token;
#line 62
    E_1 = 1;
#line 63
    immediate_notify();
#line 64
    E_1 = 2;
#line 65
    m_pc = 1;
#line 66
    m_st = 2;
    }
    goto return_label;
    M_WAIT: 
    {

    }
#line 71
    if (token != local + 4) {
      {
#line 73
      error();
      }
    }
  }
  {

  }
  return_label: 
#line 83
  return;
}
}

#line 109
extern int ( /* missing proto */  t1_started)() ;

#line 86 "token_ring.04.cil.c"
void transmit1(void) 
{ 

  {
#line 90
  if (t1_pc == 0) {
    goto T1_ENTRY;
  } else {
#line 93
    if (t1_pc == 1) {
      goto T1_WAIT;
    }
  }
  T1_ENTRY: 
  {

  }
#line 101
  while (1) {
    {
#line 103
    t1_pc = 1;
#line 104
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
#line 20
          if (__MONITOR_STATE_executed_threads == 14) {
#line 21
            error_fn();
          } else {
#line 20
            if (__MONITOR_STATE_executed_threads == 123) {
#line 21
              error_fn();
            } else {
#line 20
              if (__MONITOR_STATE_executed_threads == 124) {
#line 21
                error_fn();
              } else {
#line 20
                if (__MONITOR_STATE_executed_threads == 134) {
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
                        if (__MONITOR_STATE_executed_threads == 4) {
#line 33
                          __MONITOR_STATE_executed_threads = 14;
                        } else {
#line 35
                          if (__MONITOR_STATE_executed_threads == 23) {
#line 36
                            __MONITOR_STATE_executed_threads = 123;
                          } else {
#line 38
                            if (__MONITOR_STATE_executed_threads == 24) {
#line 39
                              __MONITOR_STATE_executed_threads = 124;
                            } else {
#line 41
                              if (__MONITOR_STATE_executed_threads == 34) {
#line 42
                                __MONITOR_STATE_executed_threads = 134;
                              } else {
#line 44
                                if (__MONITOR_STATE_executed_threads == 234) {
#line 45
                                  __MONITOR_STATE_executed_threads = 0;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
#line 47
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 109 "token_ring.04.cil.c"
    t1_started();
#line 110
    token ++;
#line 111
    E_2 = 1;
#line 112
    immediate_notify();
#line 113
    E_2 = 2;
    }
  }
  {

  }
  return_label: 
#line 120
  return;
}
}

#line 146
extern int ( /* missing proto */  t2_started)() ;

#line 123 "token_ring.04.cil.c"
void transmit2(void) 
{ 

  {
#line 127
  if (t2_pc == 0) {
    goto T2_ENTRY;
  } else {
#line 130
    if (t2_pc == 1) {
      goto T2_WAIT;
    }
  }
  T2_ENTRY: 
  {

  }
#line 138
  while (1) {
    {
#line 140
    t2_pc = 1;
#line 141
    t2_st = 2;
    }
    goto return_label;
    T2_WAIT: 
    {

    {
#line 57 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 58
    if (__MONITOR_STATE_executed_threads == 2) {
#line 59
      error_fn();
    } else {
#line 58
      if (__MONITOR_STATE_executed_threads == 12) {
#line 59
        error_fn();
      } else {
#line 58
        if (__MONITOR_STATE_executed_threads == 23) {
#line 59
          error_fn();
        } else {
#line 58
          if (__MONITOR_STATE_executed_threads == 24) {
#line 59
            error_fn();
          } else {
#line 58
            if (__MONITOR_STATE_executed_threads == 123) {
#line 59
              error_fn();
            } else {
#line 58
              if (__MONITOR_STATE_executed_threads == 124) {
#line 59
                error_fn();
              } else {
#line 58
                if (__MONITOR_STATE_executed_threads == 234) {
#line 59
                  error_fn();
                } else {
#line 61
                  if (__MONITOR_STATE_executed_threads == 0) {
#line 62
                    __MONITOR_STATE_executed_threads = 2;
                  } else {
#line 64
                    if (__MONITOR_STATE_executed_threads == 1) {
#line 65
                      __MONITOR_STATE_executed_threads = 12;
                    } else {
#line 67
                      if (__MONITOR_STATE_executed_threads == 3) {
#line 68
                        __MONITOR_STATE_executed_threads = 23;
                      } else {
#line 70
                        if (__MONITOR_STATE_executed_threads == 4) {
#line 71
                          __MONITOR_STATE_executed_threads = 24;
                        } else {
#line 73
                          if (__MONITOR_STATE_executed_threads == 13) {
#line 74
                            __MONITOR_STATE_executed_threads = 123;
                          } else {
#line 76
                            if (__MONITOR_STATE_executed_threads == 14) {
#line 77
                              __MONITOR_STATE_executed_threads = 124;
                            } else {
#line 79
                              if (__MONITOR_STATE_executed_threads == 34) {
#line 80
                                __MONITOR_STATE_executed_threads = 234;
                              } else {
#line 82
                                if (__MONITOR_STATE_executed_threads == 134) {
#line 83
                                  __MONITOR_STATE_executed_threads = 0;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
#line 85
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 146 "token_ring.04.cil.c"
    t2_started();
#line 147
    token ++;
#line 148
    E_3 = 1;
#line 149
    immediate_notify();
#line 150
    E_3 = 2;
    }
  }
  {

  }
  return_label: 
#line 157
  return;
}
}

#line 183
extern int ( /* missing proto */  t3_started)() ;

#line 160 "token_ring.04.cil.c"
void transmit3(void) 
{ 

  {
#line 164
  if (t3_pc == 0) {
    goto T3_ENTRY;
  } else {
#line 167
    if (t3_pc == 1) {
      goto T3_WAIT;
    }
  }
  T3_ENTRY: 
  {

  }
#line 175
  while (1) {
    {
#line 177
    t3_pc = 1;
#line 178
    t3_st = 2;
    }
    goto return_label;
    T3_WAIT: 
    {

    {
#line 95 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 96
    if (__MONITOR_STATE_executed_threads == 3) {
#line 97
      error_fn();
    } else {
#line 96
      if (__MONITOR_STATE_executed_threads == 13) {
#line 97
        error_fn();
      } else {
#line 96
        if (__MONITOR_STATE_executed_threads == 23) {
#line 97
          error_fn();
        } else {
#line 96
          if (__MONITOR_STATE_executed_threads == 34) {
#line 97
            error_fn();
          } else {
#line 96
            if (__MONITOR_STATE_executed_threads == 123) {
#line 97
              error_fn();
            } else {
#line 96
              if (__MONITOR_STATE_executed_threads == 134) {
#line 97
                error_fn();
              } else {
#line 96
                if (__MONITOR_STATE_executed_threads == 234) {
#line 97
                  error_fn();
                } else {
#line 99
                  if (__MONITOR_STATE_executed_threads == 0) {
#line 100
                    __MONITOR_STATE_executed_threads = 3;
                  } else {
#line 102
                    if (__MONITOR_STATE_executed_threads == 1) {
#line 103
                      __MONITOR_STATE_executed_threads = 13;
                    } else {
#line 105
                      if (__MONITOR_STATE_executed_threads == 2) {
#line 106
                        __MONITOR_STATE_executed_threads = 23;
                      } else {
#line 108
                        if (__MONITOR_STATE_executed_threads == 4) {
#line 109
                          __MONITOR_STATE_executed_threads = 34;
                        } else {
#line 111
                          if (__MONITOR_STATE_executed_threads == 12) {
#line 112
                            __MONITOR_STATE_executed_threads = 123;
                          } else {
#line 114
                            if (__MONITOR_STATE_executed_threads == 14) {
#line 115
                              __MONITOR_STATE_executed_threads = 134;
                            } else {
#line 117
                              if (__MONITOR_STATE_executed_threads == 24) {
#line 118
                                __MONITOR_STATE_executed_threads = 234;
                              } else {
#line 120
                                if (__MONITOR_STATE_executed_threads == 124) {
#line 121
                                  __MONITOR_STATE_executed_threads = 0;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
#line 123
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 183 "token_ring.04.cil.c"
    t3_started();
#line 184
    token ++;
#line 185
    E_4 = 1;
#line 186
    immediate_notify();
#line 187
    E_4 = 2;
    }
  }
  {

  }
  return_label: 
#line 194
  return;
}
}

#line 220
extern int ( /* missing proto */  t4_started)() ;

#line 197 "token_ring.04.cil.c"
void transmit4(void) 
{ 

  {
#line 201
  if (t4_pc == 0) {
    goto T4_ENTRY;
  } else {
#line 204
    if (t4_pc == 1) {
      goto T4_WAIT;
    }
  }
  T4_ENTRY: 
  {

  }
#line 212
  while (1) {
    {
#line 214
    t4_pc = 1;
#line 215
    t4_st = 2;
    }
    goto return_label;
    T4_WAIT: 
    {

    {
#line 133 "spec.work"
    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 134
    if (__MONITOR_STATE_executed_threads == 4) {
#line 135
      error_fn();
    } else {
#line 134
      if (__MONITOR_STATE_executed_threads == 14) {
#line 135
        error_fn();
      } else {
#line 134
        if (__MONITOR_STATE_executed_threads == 24) {
#line 135
          error_fn();
        } else {
#line 134
          if (__MONITOR_STATE_executed_threads == 34) {
#line 135
            error_fn();
          } else {
#line 134
            if (__MONITOR_STATE_executed_threads == 124) {
#line 135
              error_fn();
            } else {
#line 134
              if (__MONITOR_STATE_executed_threads == 134) {
#line 135
                error_fn();
              } else {
#line 134
                if (__MONITOR_STATE_executed_threads == 234) {
#line 135
                  error_fn();
                } else {
#line 137
                  if (__MONITOR_STATE_executed_threads == 0) {
#line 138
                    __MONITOR_STATE_executed_threads = 4;
                  } else {
#line 140
                    if (__MONITOR_STATE_executed_threads == 1) {
#line 141
                      __MONITOR_STATE_executed_threads = 14;
                    } else {
#line 143
                      if (__MONITOR_STATE_executed_threads == 2) {
#line 144
                        __MONITOR_STATE_executed_threads = 24;
                      } else {
#line 146
                        if (__MONITOR_STATE_executed_threads == 3) {
#line 147
                          __MONITOR_STATE_executed_threads = 34;
                        } else {
#line 149
                          if (__MONITOR_STATE_executed_threads == 12) {
#line 150
                            __MONITOR_STATE_executed_threads = 124;
                          } else {
#line 152
                            if (__MONITOR_STATE_executed_threads == 13) {
#line 153
                              __MONITOR_STATE_executed_threads = 134;
                            } else {
#line 155
                              if (__MONITOR_STATE_executed_threads == 23) {
#line 156
                                __MONITOR_STATE_executed_threads = 234;
                              } else {
#line 158
                                if (__MONITOR_STATE_executed_threads == 123) {
#line 159
                                  __MONITOR_STATE_executed_threads = 0;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
#line 161
    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
    {

    }
    }
#line 220 "token_ring.04.cil.c"
    t4_started();
#line 221
    token ++;
#line 222
    E_M = 1;
#line 223
    immediate_notify();
#line 224
    E_M = 2;
    }
  }
  {

  }
  return_label: 
#line 231
  return;
}
}

#line 234 "token_ring.04.cil.c"
int is_master_triggered(void) 
{ int __retres1 ;

  {
#line 238
  if (m_pc == 1) {
#line 239
    if (E_M == 1) {
      {
#line 240
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 248
  __retres1 = 0;
  }
  return_label: 
#line 250
  return (__retres1);
}
}

#line 253 "token_ring.04.cil.c"
int is_transmit1_triggered(void) 
{ int __retres1 ;

  {
#line 257
  if (t1_pc == 1) {
#line 258
    if (E_1 == 1) {
      {
#line 259
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 267
  __retres1 = 0;
  }
  return_label: 
#line 269
  return (__retres1);
}
}

#line 272 "token_ring.04.cil.c"
int is_transmit2_triggered(void) 
{ int __retres1 ;

  {
#line 276
  if (t2_pc == 1) {
#line 277
    if (E_2 == 1) {
      {
#line 278
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 286
  __retres1 = 0;
  }
  return_label: 
#line 288
  return (__retres1);
}
}

#line 291 "token_ring.04.cil.c"
int is_transmit3_triggered(void) 
{ int __retres1 ;

  {
#line 295
  if (t3_pc == 1) {
#line 296
    if (E_3 == 1) {
      {
#line 297
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 305
  __retres1 = 0;
  }
  return_label: 
#line 307
  return (__retres1);
}
}

#line 310 "token_ring.04.cil.c"
int is_transmit4_triggered(void) 
{ int __retres1 ;

  {
#line 314
  if (t4_pc == 1) {
#line 315
    if (E_4 == 1) {
      {
#line 316
      __retres1 = 1;
      }
      goto return_label;
    }
  }
  {
#line 324
  __retres1 = 0;
  }
  return_label: 
#line 326
  return (__retres1);
}
}

#line 329 "token_ring.04.cil.c"
void update_channels(void) 
{ 

  {
#line 334
  return;
}
}

#line 337 "token_ring.04.cil.c"
void init_threads(void) 
{ 

  {
#line 341
  if (m_i == 1) {
    {
#line 342
    m_st = 0;
    }
  } else {
    {
#line 344
    m_st = 2;
    }
  }
#line 346
  if (t1_i == 1) {
    {
#line 347
    t1_st = 0;
    }
  } else {
    {
#line 349
    t1_st = 2;
    }
  }
#line 351
  if (t2_i == 1) {
    {
#line 352
    t2_st = 0;
    }
  } else {
    {
#line 354
    t2_st = 2;
    }
  }
#line 356
  if (t3_i == 1) {
    {
#line 357
    t3_st = 0;
    }
  } else {
    {
#line 359
    t3_st = 2;
    }
  }
#line 361
  if (t4_i == 1) {
    {
#line 362
    t4_st = 0;
    }
  } else {
    {
#line 364
    t4_st = 2;
    }
  }
#line 367
  return;
}
}

#line 370 "token_ring.04.cil.c"
int exists_runnable_thread(void) 
{ int __retres1 ;

  {
#line 374
  if (m_st == 0) {
    {
#line 375
    __retres1 = 1;
    }
    goto return_label;
  } else {
#line 378
    if (t1_st == 0) {
      {
#line 379
      __retres1 = 1;
      }
      goto return_label;
    } else {
#line 382
      if (t2_st == 0) {
        {
#line 383
        __retres1 = 1;
        }
        goto return_label;
      } else {
#line 386
        if (t3_st == 0) {
          {
#line 387
          __retres1 = 1;
          }
          goto return_label;
        } else {
#line 390
          if (t4_st == 0) {
            {
#line 391
            __retres1 = 1;
            }
            goto return_label;
          }
        }
      }
    }
  }
  {
#line 400
  __retres1 = 0;
  }
  return_label: 
#line 402
  return (__retres1);
}
}

#line 405 "token_ring.04.cil.c"
void eval(void) 
{ int tmp ;
  int tmp_ndt_1 ;
  int tmp_ndt_2 ;
  int tmp_ndt_3 ;
  int tmp_ndt_4 ;
  int tmp_ndt_5 ;

  {
#line 411
  while (1) {
    {
#line 414
    tmp = exists_runnable_thread();
    }
#line 416
    if (! tmp) {
      goto while_5_break;
    }
#line 421
    if (m_st == 0) {
      {
#line 423
      tmp_ndt_1 = __VERIFIER_nondet_int();
      }
#line 424
      if (tmp_ndt_1) {
        {
#line 426
        m_st = 1;
#line 427
        master();
        }
      }
    }
#line 435
    if (t1_st == 0) {
      {
#line 437
      tmp_ndt_2 = __VERIFIER_nondet_int();
      }
#line 438
      if (tmp_ndt_2) {
        {
#line 440
        t1_st = 1;
#line 441
        transmit1();
        }
      }
    }
#line 449
    if (t2_st == 0) {
      {
#line 451
      tmp_ndt_3 = __VERIFIER_nondet_int();
      }
#line 452
      if (tmp_ndt_3) {
        {
#line 454
        t2_st = 1;
#line 455
        transmit2();
        }
      }
    }
#line 463
    if (t3_st == 0) {
      {
#line 465
      tmp_ndt_4 = __VERIFIER_nondet_int();
      }
#line 466
      if (tmp_ndt_4) {
        {
#line 468
        t3_st = 1;
#line 469
        transmit3();
        }
      }
    }
#line 477
    if (t4_st == 0) {
      {
#line 479
      tmp_ndt_5 = __VERIFIER_nondet_int();
      }
#line 480
      if (tmp_ndt_5) {
        {
#line 482
        t4_st = 1;
#line 483
        transmit4();
        }
      }
    }
  }
  while_5_break: 
  {

  }
#line 495
  return;
}
}

#line 498 "token_ring.04.cil.c"
void fire_delta_events(void) 
{ 

  {
#line 502
  if (M_E == 0) {
    {
#line 503
    M_E = 1;
    }
  }
#line 507
  if (T1_E == 0) {
    {
#line 508
    T1_E = 1;
    }
  }
#line 512
  if (T2_E == 0) {
    {
#line 513
    T2_E = 1;
    }
  }
#line 517
  if (T3_E == 0) {
    {
#line 518
    T3_E = 1;
    }
  }
#line 522
  if (T4_E == 0) {
    {
#line 523
    T4_E = 1;
    }
  }
#line 527
  if (E_M == 0) {
    {
#line 528
    E_M = 1;
    }
  }
#line 532
  if (E_1 == 0) {
    {
#line 533
    E_1 = 1;
    }
  }
#line 537
  if (E_2 == 0) {
    {
#line 538
    E_2 = 1;
    }
  }
#line 542
  if (E_3 == 0) {
    {
#line 543
    E_3 = 1;
    }
  }
#line 547
  if (E_4 == 0) {
    {
#line 548
    E_4 = 1;
    }
  }
#line 553
  return;
}
}

#line 556 "token_ring.04.cil.c"
void reset_delta_events(void) 
{ 

  {
#line 560
  if (M_E == 1) {
    {
#line 561
    M_E = 2;
    }
  }
#line 565
  if (T1_E == 1) {
    {
#line 566
    T1_E = 2;
    }
  }
#line 570
  if (T2_E == 1) {
    {
#line 571
    T2_E = 2;
    }
  }
#line 575
  if (T3_E == 1) {
    {
#line 576
    T3_E = 2;
    }
  }
#line 580
  if (T4_E == 1) {
    {
#line 581
    T4_E = 2;
    }
  }
#line 585
  if (E_M == 1) {
    {
#line 586
    E_M = 2;
    }
  }
#line 590
  if (E_1 == 1) {
    {
#line 591
    E_1 = 2;
    }
  }
#line 595
  if (E_2 == 1) {
    {
#line 596
    E_2 = 2;
    }
  }
#line 600
  if (E_3 == 1) {
    {
#line 601
    E_3 = 2;
    }
  }
#line 605
  if (E_4 == 1) {
    {
#line 606
    E_4 = 2;
    }
  }
#line 611
  return;
}
}

#line 614 "token_ring.04.cil.c"
void activate_threads(void) 
{ int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;

  {
  {
#line 623
  tmp = is_master_triggered();
  }
#line 625
  if (tmp) {
    {
#line 626
    m_st = 0;
    }
  }
  {
#line 631
  tmp___0 = is_transmit1_triggered();
  }
#line 633
  if (tmp___0) {
    {
#line 634
    t1_st = 0;
    }
  }
  {
#line 639
  tmp___1 = is_transmit2_triggered();
  }
#line 641
  if (tmp___1) {
    {
#line 642
    t2_st = 0;
    }
  }
  {
#line 647
  tmp___2 = is_transmit3_triggered();
  }
#line 649
  if (tmp___2) {
    {
#line 650
    t3_st = 0;
    }
  }
  {
#line 655
  tmp___3 = is_transmit4_triggered();
  }
#line 657
  if (tmp___3) {
    {
#line 658
    t4_st = 0;
    }
  }
#line 663
  return;
}
}

#line 666 "token_ring.04.cil.c"
void immediate_notify(void) 
{ 

  {
  {
#line 671
  activate_threads();
  }
#line 674
  return;
}
}

#line 677 "token_ring.04.cil.c"
void fire_time_events(void) 
{ 

  {
  {
#line 681
  M_E = 1;
  }
#line 683
  return;
}
}

#line 686 "token_ring.04.cil.c"
void reset_time_events(void) 
{ 

  {
#line 690
  if (M_E == 1) {
    {
#line 691
    M_E = 2;
    }
  }
#line 695
  if (T1_E == 1) {
    {
#line 696
    T1_E = 2;
    }
  }
#line 700
  if (T2_E == 1) {
    {
#line 701
    T2_E = 2;
    }
  }
#line 705
  if (T3_E == 1) {
    {
#line 706
    T3_E = 2;
    }
  }
#line 710
  if (T4_E == 1) {
    {
#line 711
    T4_E = 2;
    }
  }
#line 715
  if (E_M == 1) {
    {
#line 716
    E_M = 2;
    }
  }
#line 720
  if (E_1 == 1) {
    {
#line 721
    E_1 = 2;
    }
  }
#line 725
  if (E_2 == 1) {
    {
#line 726
    E_2 = 2;
    }
  }
#line 730
  if (E_3 == 1) {
    {
#line 731
    E_3 = 2;
    }
  }
#line 735
  if (E_4 == 1) {
    {
#line 736
    E_4 = 2;
    }
  }
#line 741
  return;
}
}

#line 744 "token_ring.04.cil.c"
void init_model(void) 
{ 

  {
  {
#line 748
  m_i = 1;
#line 749
  t1_i = 1;
#line 750
  t2_i = 1;
#line 751
  t3_i = 1;
#line 752
  t4_i = 1;
  }
#line 754
  return;
}
}

#line 757 "token_ring.04.cil.c"
int stop_simulation(void) 
{ int tmp ;
  int __retres2 ;

  {
  {
#line 763
  tmp = exists_runnable_thread();
  }
#line 765
  if (tmp) {
    {
#line 766
    __retres2 = 0;
    }
    goto return_label;
  }
  {
#line 771
  __retres2 = 1;
  }
  return_label: 
#line 773
  return (__retres2);
}
}

#line 776 "token_ring.04.cil.c"
void start_simulation(void) 
{ int kernel_st ;
  int tmp ;
  int tmp___0 ;

  {
  {
#line 783
  kernel_st = 0;
#line 784
  update_channels();
#line 785
  init_threads();
#line 786
  fire_delta_events();
#line 787
  activate_threads();
#line 788
  reset_delta_events();
  }
#line 791
  while (1) {
    {
#line 794
    kernel_st = 1;
#line 795
    eval();
#line 798
    kernel_st = 2;
#line 799
    update_channels();
#line 802
    kernel_st = 3;
#line 803
    fire_delta_events();
#line 804
    activate_threads();
#line 805
    reset_delta_events();
#line 808
    tmp = exists_runnable_thread();
    }
#line 810
    if (tmp == 0) {
      {
#line 812
      kernel_st = 4;
#line 813
      fire_time_events();
#line 814
      activate_threads();
#line 815
      reset_time_events();
      }
    }
    {
#line 821
    tmp___0 = stop_simulation();
    }
#line 823
    if (tmp___0) {
      goto while_6_break;
    }
  }
  while_6_break: 
  {

  }
#line 832
  return;
}
}

#line 835 "token_ring.04.cil.c"
int entry(void) 
{ int __retres1 ;

  {
  {
#line 840
  init_model();
#line 841
  start_simulation();
#line 843
  __retres1 = 0;
  }
#line 844
  return (__retres1);
}
}

void __initialize__(void) 
{ 

  {

}
}

