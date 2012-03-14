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
int __MONITOR_STATE_state   = 0;

void __initialize__(void) ;

#line 4 "s3_srvr.cil.c"
extern void ssl3_init_finished_mac() ;

#line 5
extern int ssl3_send_server_certificate() ;

#line 6
extern int ssl3_get_finished() ;

#line 7
extern int ssl3_send_change_cipher_spec() ;

#line 8
extern int ssl3_send_finished() ;

#line 9
extern int ssl3_setup_buffers() ;

#line 10
extern int ssl_init_wbio_buffer() ;

#line 11
extern int ssl3_get_client_hello() ;

#line 12
extern int ssl3_check_client_hello() ;

#line 13
extern int ssl3_send_server_hello() ;

#line 14
extern int ssl3_send_server_key_exchange() ;

#line 15
extern int ssl3_send_certificate_request() ;

#line 16
extern int ssl3_send_server_done() ;

#line 17
extern int ssl3_get_client_key_exchange() ;

#line 18
extern int ssl3_get_client_certificate() ;

#line 19
extern int ssl3_get_cert_verify() ;

#line 20
extern int ssl3_send_hello_request() ;

#line 222 "s3_srvr_1.cil.c"
extern int ( /* missing proto */  __VERIFIER_nondet_int)() ;

#line 4 "s3_srvr_1.cil.c"
int ssl3_accept(int initial_state ) 
{ int s__info_callback ;
  int s__in_handshake ;
  int s__state ;
  int s__new_session ;
  int s__server ;
  int s__version ;
  int s__type ;
  int s__init_num ;
  int s__hit ;
  int s__rwstate ;
  int s__init_buf___0 ;
  int s__debug ;
  int s__shutdown ;
  int s__cert ;
  int s__options ;
  int s__verify_mode ;
  int s__session__peer ;
  int s__cert__pkeys__AT0__privatekey ;
  int s__ctx__info_callback ;
  int s__ctx__stats__sess_accept_renegotiate ;
  int s__ctx__stats__sess_accept ;
  int s__ctx__stats__sess_accept_good ;
  int s__s3__tmp__cert_request ;
  int s__s3__tmp__reuse_message ;
  int s__s3__tmp__use_rsa_tmp ;
  int s__s3__tmp__new_cipher ;
  int s__s3__tmp__new_cipher__algorithms ;
  int s__s3__tmp__next_state___0 ;
  int s__s3__tmp__new_cipher__algo_strength ;
  int s__session__cipher ;
  int buf ;
  unsigned long l ;
  unsigned long Time ;
  unsigned long tmp ;
  int cb ;
  long num1 ;
  int ret ;
  int new_state ;
  int state ;
  int skip ;
  int got_new_session ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  long tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int __cil_tmp55 ;
  unsigned long __cil_tmp56 ;
  unsigned long __cil_tmp57 ;
  unsigned long __cil_tmp58 ;
  unsigned long __cil_tmp59 ;
  int __cil_tmp60 ;
  unsigned long __cil_tmp61 ;

  {
  {
#line 61
  s__state = initial_state;
#line 62
  Time = tmp;
#line 63
  cb = 0;
#line 64
  ret = -1;
#line 65
  skip = 0;
#line 66
  got_new_session = 0;
  }
#line 67
  if (s__info_callback != 0) {
    {
#line 68
    cb = s__info_callback;
    }
  } else {
#line 70
    if (s__ctx__info_callback != 0) {
      {
#line 71
      cb = s__ctx__info_callback;
      }
    }
  }
  {
#line 76
  s__in_handshake ++;
  }
#line 77
  if (tmp___1 + 12288) {
#line 78
    if (tmp___2 + 16384) {

    }
  }
#line 86
  if (s__cert == 0) {
#line 87
    return (-1);
  }
#line 92
  while (1) {
    {
#line 94
    state = s__state;
    }
#line 95
    if (s__state == 12292) {
      goto switch_1_12292;
    } else {
#line 98
      if (s__state == 16384) {
        goto switch_1_16384;
      } else {
#line 101
        if (s__state == 8192) {
          goto switch_1_8192;
        } else {
#line 104
          if (s__state == 24576) {
            goto switch_1_24576;
          } else {
#line 107
            if (s__state == 8195) {
              goto switch_1_8195;
            } else {
#line 110
              if (s__state == 8480) {
                goto switch_1_8480;
              } else {
#line 113
                if (s__state == 8481) {
                  goto switch_1_8481;
                } else {
#line 116
                  if (s__state == 8482) {
                    goto switch_1_8482;
                  } else {
#line 119
                    if (s__state == 8464) {
                      goto switch_1_8464;
                    } else {
#line 122
                      if (s__state == 8465) {
                        goto switch_1_8465;
                      } else {
#line 125
                        if (s__state == 8466) {
                          goto switch_1_8466;
                        } else {
#line 128
                          if (s__state == 8496) {
                            goto switch_1_8496;
                          } else {
#line 131
                            if (s__state == 8497) {
                              goto switch_1_8497;
                            } else {
#line 134
                              if (s__state == 8512) {
                                goto switch_1_8512;
                              } else {
#line 137
                                if (s__state == 8513) {
                                  goto switch_1_8513;
                                } else {
#line 140
                                  if (s__state == 8528) {
                                    goto switch_1_8528;
                                  } else {
#line 143
                                    if (s__state == 8529) {
                                      goto switch_1_8529;
                                    } else {
#line 146
                                      if (s__state == 8544) {
                                        goto switch_1_8544;
                                      } else {
#line 149
                                        if (s__state == 8545) {
                                          goto switch_1_8545;
                                        } else {
#line 152
                                          if (s__state == 8560) {
                                            goto switch_1_8560;
                                          } else {
#line 155
                                            if (s__state == 8561) {
                                              goto switch_1_8561;
                                            } else {
#line 158
                                              if (s__state == 8448) {
                                                goto switch_1_8448;
                                              } else {
#line 161
                                                if (s__state == 8576) {
                                                  goto switch_1_8576;
                                                } else {
#line 164
                                                  if (s__state == 8577) {
                                                    goto switch_1_8577;
                                                  } else {
#line 167
                                                    if (s__state == 8592) {
                                                      goto switch_1_8592;
                                                    } else {
#line 170
                                                      if (s__state == 8593) {
                                                        goto switch_1_8593;
                                                      } else {
#line 173
                                                        if (s__state == 8608) {
                                                          goto switch_1_8608;
                                                        } else {
#line 176
                                                          if (s__state == 8609) {
                                                            goto switch_1_8609;
                                                          } else {
#line 179
                                                            if (s__state == 8640) {
                                                              goto switch_1_8640;
                                                            } else {
#line 182
                                                              if (s__state ==
                                                                  8641) {
                                                                goto switch_1_8641;
                                                              } else {
#line 185
                                                                if (s__state ==
                                                                    8656) {
                                                                  goto switch_1_8656;
                                                                } else {
#line 188
                                                                  if (s__state ==
                                                                      8657) {
                                                                    goto switch_1_8657;
                                                                  } else {
#line 191
                                                                    if (s__state ==
                                                                        8672) {
                                                                      goto switch_1_8672;
                                                                    } else {
#line 194
                                                                      if (s__state ==
                                                                          8673) {
                                                                        goto switch_1_8673;
                                                                      } else {
#line 197
                                                                        if (s__state ==
                                                                            3) {
                                                                          goto switch_1_3;
                                                                        } else {
                                                                          goto switch_1_default;
#line 202
                                                                          if (0) {
                                                                            switch_1_12292: 
                                                                            {
#line 204
                                                                            s__new_session = 1;
                                                                            }
                                                                            switch_1_16384: 
                                                                            {

                                                                            }
                                                                            switch_1_8192: 
                                                                            {

                                                                            }
                                                                            switch_1_24576: 
                                                                            {

                                                                            }
                                                                            switch_1_8195: 
                                                                            {
#line 209
                                                                            s__server = 1;
                                                                            }
#line 210
                                                                            if (cb !=
                                                                                0) {

                                                                            }
                                                                            {
#line 215
                                                                            __cil_tmp55 = s__version *
                                                                                          8;
                                                                            }
#line 215
                                                                            if (__cil_tmp55 !=
                                                                                3) {
#line 216
                                                                              return (-1);
                                                                            }
                                                                            {
#line 220
                                                                            s__type = 8192;
                                                                            }
#line 221
                                                                            if (s__init_buf___0 ==
                                                                                0) {
                                                                              {
#line 222
                                                                              tmp___3 = __VERIFIER_nondet_int();
                                                                              }
#line 223
                                                                              if (! tmp___3) {
                                                                                {
#line 224
                                                                                ret = -1;
                                                                                }
                                                                                goto end;
                                                                              }
                                                                              {
#line 229
                                                                              s__init_buf___0 = buf;
                                                                              }
                                                                            }
                                                                            {
#line 233
                                                                            tmp___4 = ssl3_setup_buffers();
                                                                            }
#line 234
                                                                            if (! tmp___4) {
                                                                              {
#line 235
                                                                              ret = -1;
                                                                              }
                                                                              goto end;
                                                                            }
                                                                            {
#line 240
                                                                            s__init_num = 0;
                                                                            }
#line 241
                                                                            if (s__state !=
                                                                                12292) {
                                                                              {
#line 242
                                                                              tmp___5 = ssl_init_wbio_buffer();
                                                                              }
#line 243
                                                                              if (! tmp___5) {
                                                                                {
#line 244
                                                                                ret = -1;
                                                                                }
                                                                                goto end;
                                                                              }
                                                                              {
#line 249
                                                                              s__state = 8464;
#line 250
                                                                              s__ctx__stats__sess_accept ++;
                                                                              }
                                                                            } else {
                                                                              {
#line 252
                                                                              s__ctx__stats__sess_accept_renegotiate ++;
#line 253
                                                                              s__state = 8480;
                                                                              }
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8480: 
                                                                            {

                                                                            }
                                                                            switch_1_8481: 
                                                                            {
#line 258
                                                                            s__shutdown = 0;
#line 259
                                                                            ret = (int )(& ssl3_send_hello_request);
                                                                            }
#line 260
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 265
                                                                            s__s3__tmp__next_state___0 = 8482;
#line 266
                                                                            s__state = 8448;
#line 267
                                                                            s__init_num = 0;
#line 268
                                                                            ssl3_init_finished_mac();
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8482: 
                                                                            {
#line 270
                                                                            s__state = 3;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8464: 
                                                                            {

                                                                            }
                                                                            switch_1_8465: 
                                                                            {

                                                                            }
                                                                            switch_1_8466: 
                                                                            {
#line 275
                                                                            s__shutdown = 0;
                                                                            {
#line 19 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 20
                                                                            if (__MONITOR_STATE_state ==
                                                                                0) {
#line 21
                                                                              __MONITOR_STATE_state = 1;
                                                                            }
#line 23
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 276 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_get_client_hello();
                                                                            }
#line 282
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 287
                                                                            got_new_session = 1;
#line 288
                                                                            s__state = 8496;
#line 289
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8496: 
                                                                            {

                                                                            }
                                                                            switch_1_8497: 
                                                                            {

                                                                            {
#line 33 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 34
                                                                            if (__MONITOR_STATE_state ==
                                                                                1) {
#line 35
                                                                              __MONITOR_STATE_state = 2;
                                                                            }
#line 37
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 293 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_send_server_hello();
                                                                            }
#line 299
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
#line 304
                                                                            if (s__hit) {
                                                                              {
#line 305
                                                                              s__state = 8656;
                                                                              }
                                                                            } else {
                                                                              {
#line 307
                                                                              s__state = 8512;
                                                                              }
                                                                            }
                                                                            {
#line 309
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8512: 
                                                                            {

                                                                            }
                                                                            switch_1_8513: 
                                                                            {
#line 313
                                                                            __cil_tmp56 = (unsigned long )s__s3__tmp__new_cipher__algorithms;
                                                                            }
#line 313
                                                                            if (__cil_tmp56 +
                                                                                256UL) {
                                                                              {
#line 314
                                                                              skip = 1;
                                                                              }
                                                                            } else {
                                                                              {

                                                                              {
#line 47 "spec.work"
                                                                              __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 48
                                                                              if (__MONITOR_STATE_state ==
                                                                                  2) {
#line 49
                                                                                __MONITOR_STATE_state = 3;
                                                                              }
#line 51
                                                                              __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                              {

                                                                              }
                                                                              }
#line 316 "s3_srvr_1.cil.c"
                                                                              ret = ssl3_send_server_certificate();
                                                                              }
#line 317
                                                                              if (ret <=
                                                                                  0) {
                                                                                goto end;
                                                                              }
                                                                            }
                                                                            {
#line 323
                                                                            s__state = 8528;
#line 324
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8528: 
                                                                            {

                                                                            }
                                                                            switch_1_8529: 
                                                                            {
#line 328
                                                                            l = (unsigned long )s__s3__tmp__new_cipher__algorithms;
#line 329
                                                                            __cil_tmp57 = (unsigned long )s__options;
                                                                            }
#line 329
                                                                            if (__cil_tmp57 +
                                                                                2097152UL) {
                                                                              {
#line 330
                                                                              s__s3__tmp__use_rsa_tmp = 1;
                                                                              }
                                                                            } else {
                                                                              {
#line 332
                                                                              s__s3__tmp__use_rsa_tmp = 0;
                                                                              }
                                                                            }
#line 334
                                                                            if (s__s3__tmp__use_rsa_tmp) {
                                                                              goto _L___0;
                                                                            } else {
#line 337
                                                                              if (l +
                                                                                  30UL) {
                                                                                goto _L___0;
                                                                              } else {
#line 340
                                                                                if (l +
                                                                                    1UL) {
#line 341
                                                                                  if (s__cert__pkeys__AT0__privatekey ==
                                                                                      0) {
                                                                                    goto _L___0;
                                                                                  } else {
                                                                                    {
#line 344
                                                                                    __cil_tmp58 = (unsigned long )s__s3__tmp__new_cipher__algo_strength;
                                                                                    }
#line 344
                                                                                    if (__cil_tmp58 +
                                                                                        2UL) {
                                                                                      {
#line 345
                                                                                      tmp___6 = __VERIFIER_nondet_int();
#line 346
                                                                                      __cil_tmp59 = (unsigned long )s__s3__tmp__new_cipher__algo_strength;
                                                                                      }
#line 346
                                                                                      if (__cil_tmp59 +
                                                                                          4UL) {
                                                                                        {
#line 347
                                                                                        tmp___7 = 512;
                                                                                        }
                                                                                      } else {
                                                                                        {
#line 349
                                                                                        tmp___7 = 1024;
                                                                                        }
                                                                                      }
                                                                                      {
#line 351
                                                                                      __cil_tmp60 = tmp___6 *
                                                                                                    8;
                                                                                      }
#line 351
                                                                                      if (__cil_tmp60 >
                                                                                          tmp___7) {
                                                                                        _L___0: 
                                                                                        {

                                                                                        {
#line 61 "spec.work"
                                                                                        __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 62
                                                                                        if (__MONITOR_STATE_state ==
                                                                                            3) {
#line 63
                                                                                          __MONITOR_STATE_state = 4;
                                                                                        }
#line 65
                                                                                        __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                                        {

                                                                                        }
                                                                                        }
#line 353 "s3_srvr_1.cil.c"
                                                                                        ret = ssl3_send_server_key_exchange();
                                                                                        }
#line 354
                                                                                        if (ret <=
                                                                                            0) {
                                                                                          goto end;
                                                                                        }
                                                                                      } else {
                                                                                        {
#line 360
                                                                                        skip = 1;
                                                                                        }
                                                                                      }
                                                                                    } else {
                                                                                      {
#line 363
                                                                                      skip = 1;
                                                                                      }
                                                                                    }
                                                                                  }
                                                                                } else {
                                                                                  {
#line 367
                                                                                  skip = 1;
                                                                                  }
                                                                                }
                                                                              }
                                                                            }
                                                                            {
#line 371
                                                                            s__state = 8544;
#line 372
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8544: 
                                                                            {

                                                                            }
                                                                            switch_1_8545: 
                                                                            {

                                                                            }
#line 376
                                                                            if (s__verify_mode +
                                                                                1) {
#line 377
                                                                              if (s__session__peer !=
                                                                                  0) {
#line 378
                                                                                if (s__verify_mode +
                                                                                    4) {
                                                                                  {
#line 379
                                                                                  skip = 1;
#line 380
                                                                                  s__s3__tmp__cert_request = 0;
#line 381
                                                                                  s__state = 8560;
                                                                                  }
                                                                                } else {
                                                                                  goto _L___2;
                                                                                }
                                                                              } else {
                                                                                _L___2: 
                                                                                {
#line 387
                                                                                __cil_tmp61 = (unsigned long )s__s3__tmp__new_cipher__algorithms;
                                                                                }
#line 387
                                                                                if (__cil_tmp61 +
                                                                                    256UL) {
#line 388
                                                                                  if (s__verify_mode +
                                                                                      2) {
                                                                                    goto _L___1;
                                                                                  } else {
                                                                                    {
#line 391
                                                                                    skip = 1;
#line 392
                                                                                    s__s3__tmp__cert_request = 0;
#line 393
                                                                                    s__state = 8560;
                                                                                    }
                                                                                  }
                                                                                } else {
                                                                                  _L___1: 
                                                                                  {
#line 397
                                                                                  s__s3__tmp__cert_request = 1;
                                                                                  {
#line 75 "spec.work"
                                                                                  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 76
                                                                                  if (__MONITOR_STATE_state ==
                                                                                      4) {
#line 77
                                                                                    __MONITOR_STATE_state = 5;
                                                                                  }
#line 79
                                                                                  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                                  {

                                                                                  }
                                                                                  }
#line 398 "s3_srvr_1.cil.c"
                                                                                  ret = ssl3_send_certificate_request();
                                                                                  }
#line 399
                                                                                  if (ret <=
                                                                                      0) {
                                                                                    goto end;
                                                                                  }
                                                                                  {
#line 404
                                                                                  s__state = 8448;
#line 405
                                                                                  s__s3__tmp__next_state___0 = 8576;
#line 406
                                                                                  s__init_num = 0;
                                                                                  }
                                                                                }
                                                                              }
                                                                            } else {
                                                                              {
#line 410
                                                                              skip = 1;
#line 411
                                                                              s__s3__tmp__cert_request = 0;
#line 412
                                                                              s__state = 8560;
                                                                              }
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8560: 
                                                                            {

                                                                            }
                                                                            switch_1_8561: 
                                                                            {
#line 417
                                                                            ret = ssl3_send_server_done();
                                                                            }
#line 418
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 423
                                                                            s__s3__tmp__next_state___0 = 8576;
#line 424
                                                                            s__state = 8448;
#line 425
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8448: 
                                                                            {
#line 428
                                                                            num1 = (long )__VERIFIER_nondet_int();
                                                                            }
#line 429
                                                                            if (num1 >
                                                                                0L) {
                                                                              {
#line 430
                                                                              s__rwstate = 2;
#line 431
                                                                              num1 = tmp___8;
                                                                              }
#line 432
                                                                              if (num1 <=
                                                                                  0L) {
                                                                                {
#line 433
                                                                                ret = -1;
                                                                                }
                                                                                goto end;
                                                                              }
                                                                              {
#line 438
                                                                              s__rwstate = 1;
                                                                              }
                                                                            }
                                                                            {
#line 442
                                                                            s__state = s__s3__tmp__next_state___0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8576: 
                                                                            {

                                                                            }
                                                                            switch_1_8577: 
                                                                            {

                                                                            {
#line 89 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 90
                                                                            if (__MONITOR_STATE_state ==
                                                                                5) {
#line 91
                                                                              __MONITOR_STATE_state = 6;
                                                                            }
#line 93
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 446 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_check_client_hello();
                                                                            }
#line 447
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
#line 452
                                                                            if (ret ==
                                                                                2) {
                                                                              {
#line 453
                                                                              s__state = 8466;
                                                                              }
                                                                            } else {
                                                                              {

                                                                              {
#line 103 "spec.work"
                                                                              __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 104
                                                                              if (__MONITOR_STATE_state ==
                                                                                  6) {
#line 105
                                                                                __MONITOR_STATE_state = 7;
                                                                              }
#line 107
                                                                              __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                              {

                                                                              }
                                                                              }
#line 455 "s3_srvr_1.cil.c"
                                                                              ret = ssl3_get_client_certificate();
                                                                              }
#line 456
                                                                              if (ret <=
                                                                                  0) {
                                                                                goto end;
                                                                              }
                                                                              {
#line 461
                                                                              s__init_num = 0;
#line 462
                                                                              s__state = 8592;
                                                                              }
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8592: 
                                                                            {

                                                                            }
                                                                            switch_1_8593: 
                                                                            {

                                                                            {
#line 117 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 118
                                                                            if (__MONITOR_STATE_state ==
                                                                                7) {
#line 119
                                                                              __MONITOR_STATE_state = 8;
                                                                            }
#line 121
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 467 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_get_client_key_exchange();
                                                                            }
#line 468
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 473
                                                                            s__state = 8608;
#line 474
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8608: 
                                                                            {

                                                                            }
                                                                            switch_1_8609: 
                                                                            {

                                                                            {
#line 131 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 132
                                                                            if (__MONITOR_STATE_state ==
                                                                                8) {
#line 133
                                                                              __MONITOR_STATE_state = 9;
                                                                            }
#line 135
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 478 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_get_cert_verify();
                                                                            }
#line 479
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 484
                                                                            s__state = 8640;
#line 485
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8640: 
                                                                            {

                                                                            }
                                                                            switch_1_8641: 
                                                                            {

                                                                            {
#line 145 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 146
                                                                            if (__MONITOR_STATE_state ==
                                                                                9) {
#line 147
                                                                              __MONITOR_STATE_state = 10;
                                                                            } else {
#line 149
                                                                              if (__MONITOR_STATE_state ==
                                                                                  12) {
#line 150
                                                                                __MONITOR_STATE_state = 13;
                                                                              } else {
#line 152
                                                                                if (__MONITOR_STATE_state ==
                                                                                    15) {
#line 153
                                                                                  __MONITOR_STATE_state = 16;
                                                                                } else {
#line 155
                                                                                  if (__MONITOR_STATE_state ==
                                                                                      18) {
#line 156
                                                                                    __MONITOR_STATE_state = 19;
                                                                                  } else {
#line 158
                                                                                    if (__MONITOR_STATE_state ==
                                                                                        21) {
#line 159
                                                                                      error_fn();
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
#line 489 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_get_finished();
                                                                            }
#line 495
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
#line 500
                                                                            if (s__hit) {
                                                                              {
#line 501
                                                                              s__state = 3;
                                                                              }
                                                                            } else {
                                                                              {
#line 503
                                                                              s__state = 8656;
                                                                              }
                                                                            }
                                                                            {
#line 505
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8656: 
                                                                            {

                                                                            }
                                                                            switch_1_8657: 
                                                                            {
#line 509
                                                                            s__session__cipher = s__s3__tmp__new_cipher;
#line 510
                                                                            tmp___9 = __VERIFIER_nondet_int();
                                                                            }
#line 511
                                                                            if (! tmp___9) {
                                                                              {
#line 512
                                                                              ret = -1;
                                                                              }
                                                                              goto end;
                                                                            }
                                                                            {

                                                                            {
#line 171 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 172
                                                                            if (__MONITOR_STATE_state ==
                                                                                10) {
#line 173
                                                                              __MONITOR_STATE_state = 11;
                                                                            } else {
#line 175
                                                                              if (__MONITOR_STATE_state ==
                                                                                  13) {
#line 176
                                                                                __MONITOR_STATE_state = 14;
                                                                              } else {
#line 178
                                                                                if (__MONITOR_STATE_state ==
                                                                                    16) {
#line 179
                                                                                  __MONITOR_STATE_state = 17;
                                                                                } else {
#line 181
                                                                                  if (__MONITOR_STATE_state ==
                                                                                      19) {
#line 182
                                                                                    __MONITOR_STATE_state = 20;
                                                                                  }
                                                                                }
                                                                              }
                                                                            }
#line 184
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 517 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_send_change_cipher_spec();
                                                                            }
#line 518
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 528
                                                                            s__state = 8672;
#line 529
                                                                            s__init_num = 0;
#line 530
                                                                            tmp___10 = __VERIFIER_nondet_int();
                                                                            }
#line 531
                                                                            if (! tmp___10) {
                                                                              {
#line 532
                                                                              ret = -1;
                                                                              }
                                                                              goto end;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_8672: 
                                                                            {

                                                                            }
                                                                            switch_1_8673: 
                                                                            {

                                                                            {
#line 194 "spec.work"
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 195
                                                                            if (__MONITOR_STATE_state ==
                                                                                11) {
#line 196
                                                                              __MONITOR_STATE_state = 12;
                                                                            } else {
#line 198
                                                                              if (__MONITOR_STATE_state ==
                                                                                  14) {
#line 199
                                                                                __MONITOR_STATE_state = 15;
                                                                              } else {
#line 201
                                                                                if (__MONITOR_STATE_state ==
                                                                                    17) {
#line 202
                                                                                  __MONITOR_STATE_state = 18;
                                                                                } else {
#line 204
                                                                                  if (__MONITOR_STATE_state ==
                                                                                      20) {
#line 205
                                                                                    __MONITOR_STATE_state = 21;
                                                                                  }
                                                                                }
                                                                              }
                                                                            }
#line 207
                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                            {

                                                                            }
                                                                            }
#line 540 "s3_srvr_1.cil.c"
                                                                            ret = ssl3_send_finished();
                                                                            }
#line 541
                                                                            if (ret <=
                                                                                0) {
                                                                              goto end;
                                                                            }
                                                                            {
#line 555
                                                                            s__state = 8448;
                                                                            }
#line 556
                                                                            if (s__hit) {
                                                                              {
#line 557
                                                                              s__s3__tmp__next_state___0 = 8640;
                                                                              }
                                                                            } else {
                                                                              {
#line 559
                                                                              s__s3__tmp__next_state___0 = 3;
                                                                              }
                                                                            }
                                                                            {
#line 561
                                                                            s__init_num = 0;
                                                                            }
                                                                            goto switch_1_break;
                                                                            switch_1_3: 
                                                                            {
#line 564
                                                                            s__init_buf___0 = 0;
#line 565
                                                                            s__init_num = 0;
                                                                            }
#line 566
                                                                            if (got_new_session) {
                                                                              {
#line 567
                                                                              s__new_session = 0;
#line 568
                                                                              s__ctx__stats__sess_accept_good ++;
                                                                              }
#line 569
                                                                              if (cb !=
                                                                                  0) {

                                                                              }
                                                                            }
                                                                            {
#line 577
                                                                            ret = 1;
                                                                            }
                                                                            goto end;
                                                                            switch_1_default: 
                                                                            {
#line 580
                                                                            ret = -1;
                                                                            }
                                                                            goto end;
                                                                          } else {
                                                                            switch_1_break: 
                                                                            {

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
                }
              }
            }
          }
        }
      }
    }
#line 621
    if (! s__s3__tmp__reuse_message) {
#line 622
      if (! skip) {
#line 623
        if (s__debug) {
          {
#line 624
          ret = __VERIFIER_nondet_int();
          }
#line 625
          if (ret <= 0) {
            goto end;
          }
        }
#line 633
        if (cb != 0) {
#line 634
          if (s__state != state) {
            {
#line 635
            new_state = s__state;
#line 636
            s__state = state;
#line 637
            s__state = new_state;
            }
          }
        }
      }
    }
    {
#line 650
    skip = 0;
    }
  }
  {

  }
  end: 
  {
#line 655
  s__in_handshake --;
  }
#line 656
  if (cb != 0) {

  }
#line 661
  return (ret);
}
}

#line 669
extern int ( /* missing proto */  printf)() ;

#line 667 "s3_srvr_1.cil.c"
void ERR(void) 
{ 

  {
  {
#line 669
  printf("error");
  }
#line 670
  return;
}
}

#line 666 "s3_srvr_1.cil.c"
int entry(void) 
{ int s ;
  int tmp ;

  {
  {
#line 672
  s = 8464;
#line 673
  tmp = ssl3_accept(s);
  }
#line 675
  return (tmp);
}
}

void __initialize__(void) 
{ 

  {

}
}

