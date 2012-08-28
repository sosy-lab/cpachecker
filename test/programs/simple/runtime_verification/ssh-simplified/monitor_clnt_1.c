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

#line 215 "s3_clnt.cil.c"
extern int ( /* missing proto */  ssl3_setup_buffers)() ;

#line 241
extern int ( /* missing proto */  ssl3_client_hello)() ;

#line 253
extern int ( /* missing proto */  ssl3_get_server_hello)() ;

#line 269
extern int ( /* missing proto */  ssl3_get_server_certificate)() ;

#line 279
extern int ( /* missing proto */  ssl3_get_key_exchange)() ;

#line 292
extern int ( /* missing proto */  ssl3_get_certificate_request)() ;

#line 301
extern int ( /* missing proto */  ssl3_get_server_done)() ;

#line 316
extern int ( /* missing proto */  ssl3_send_client_certificate)() ;

#line 325
extern int ( /* missing proto */  ssl3_send_client_key_exchange)() ;

#line 340
extern int ( /* missing proto */  ssl3_send_client_verify)() ;

#line 350
extern int ( /* missing proto */  ssl3_send_change_cipher_spec)() ;

#line 373
extern int ( /* missing proto */  ssl3_send_finished)() ;

#line 400
extern int ( /* missing proto */  ssl3_get_finished)() ;

#line 493
extern int ( /* missing proto */  BIO_flush)() ;

#line 4 "s3_clnt.cil.c"
int ssl3_connect(int initial_state ) 
{ int s__info_callback ;
  int s__in_handshake ;
  int s__state ;
  int s__new_session ;
  int s__server ;
  int s__version ;
  int s__type ;
  int s__init_num ;
  int s__bbio ;
  int s__wbio ;
  int s__hit ;
  int s__rwstate ;
  int s__init_buf___0 ;
  int s__debug ;
  int s__shutdown ;
  int s__ctx__info_callback ;
  int s__ctx__stats__sess_connect_renegotiate ;
  int s__ctx__stats__sess_connect ;
  int s__ctx__stats__sess_hit ;
  int s__ctx__stats__sess_connect_good ;
  int s__s3__change_cipher_spec ;
  int s__s3__flags ;
  int s__s3__delay_buf_pop_ret ;
  int s__s3__tmp__cert_req ;
  int s__s3__tmp__new_compression ;
  int s__s3__tmp__reuse_message ;
  int s__s3__tmp__new_cipher ;
  int s__s3__tmp__new_cipher__algorithms ;
  int s__s3__tmp__next_state___0 ;
  int s__s3__tmp__new_compression__id ;
  int s__session__cipher ;
  int s__session__compress_meth ;
  int buf ;
  unsigned long l ;
  int num1 ;
  int cb ;
  int ret ;
  int new_state ;
  int state ;
  int skip ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int blastFlag ;
  int __cil_tmp55 ;
  long __cil_tmp56 ;
  long __cil_tmp57 ;
  long __cil_tmp58 ;
  long __cil_tmp59 ;
  long __cil_tmp60 ;
  long __cil_tmp61 ;
  long __cil_tmp62 ;
  long __cil_tmp63 ;
  long __cil_tmp64 ;

  {
  {
#line 70
  s__state = initial_state;
#line 71
  blastFlag = 0;
#line 72
  cb = 0;
#line 73
  ret = -1;
#line 74
  skip = 0;
#line 75
  tmp___0 = 0;
  }
#line 76
  if (s__info_callback != 0) {
    {
#line 77
    cb = s__info_callback;
    }
  } else {
#line 79
    if (s__ctx__info_callback != 0) {
      {
#line 80
      cb = s__ctx__info_callback;
      }
    }
  }
  {
#line 83
  s__in_handshake ++;
  }
#line 84
  if (tmp___1 - 12288) {
#line 85
    if (tmp___2 - 16384) {

    }
  }
#line 90
  while (1) {
    {
#line 92
    state = s__state;
    }
#line 93
    if (s__state == 12292) {
      goto switch_1_12292;
    } else {
#line 96
      if (s__state == 16384) {
        goto switch_1_16384;
      } else {
#line 99
        if (s__state == 4096) {
          goto switch_1_4096;
        } else {
#line 102
          if (s__state == 20480) {
            goto switch_1_20480;
          } else {
#line 105
            if (s__state == 4099) {
              goto switch_1_4099;
            } else {
#line 108
              if (s__state == 4368) {
                goto switch_1_4368;
              } else {
#line 111
                if (s__state == 4369) {
                  goto switch_1_4369;
                } else {
#line 114
                  if (s__state == 4384) {
                    goto switch_1_4384;
                  } else {
#line 117
                    if (s__state == 4385) {
                      goto switch_1_4385;
                    } else {
#line 120
                      if (s__state == 4400) {
                        goto switch_1_4400;
                      } else {
#line 123
                        if (s__state == 4401) {
                          goto switch_1_4401;
                        } else {
#line 126
                          if (s__state == 4416) {
                            goto switch_1_4416;
                          } else {
#line 129
                            if (s__state == 4417) {
                              goto switch_1_4417;
                            } else {
#line 132
                              if (s__state == 4432) {
                                goto switch_1_4432;
                              } else {
#line 135
                                if (s__state == 4433) {
                                  goto switch_1_4433;
                                } else {
#line 138
                                  if (s__state == 4448) {
                                    goto switch_1_4448;
                                  } else {
#line 141
                                    if (s__state == 4449) {
                                      goto switch_1_4449;
                                    } else {
#line 144
                                      if (s__state == 4464) {
                                        goto switch_1_4464;
                                      } else {
#line 147
                                        if (s__state == 4465) {
                                          goto switch_1_4465;
                                        } else {
#line 150
                                          if (s__state == 4466) {
                                            goto switch_1_4466;
                                          } else {
#line 153
                                            if (s__state == 4467) {
                                              goto switch_1_4467;
                                            } else {
#line 156
                                              if (s__state == 4480) {
                                                goto switch_1_4480;
                                              } else {
#line 159
                                                if (s__state == 4481) {
                                                  goto switch_1_4481;
                                                } else {
#line 162
                                                  if (s__state == 4496) {
                                                    goto switch_1_4496;
                                                  } else {
#line 165
                                                    if (s__state == 4497) {
                                                      goto switch_1_4497;
                                                    } else {
#line 168
                                                      if (s__state == 4512) {
                                                        goto switch_1_4512;
                                                      } else {
#line 171
                                                        if (s__state == 4513) {
                                                          goto switch_1_4513;
                                                        } else {
#line 174
                                                          if (s__state == 4528) {
                                                            goto switch_1_4528;
                                                          } else {
#line 177
                                                            if (s__state == 4529) {
                                                              goto switch_1_4529;
                                                            } else {
#line 180
                                                              if (s__state ==
                                                                  4560) {
                                                                goto switch_1_4560;
                                                              } else {
#line 183
                                                                if (s__state ==
                                                                    4561) {
                                                                  goto switch_1_4561;
                                                                } else {
#line 186
                                                                  if (s__state ==
                                                                      4352) {
                                                                    goto switch_1_4352;
                                                                  } else {
#line 189
                                                                    if (s__state ==
                                                                        3) {
                                                                      goto switch_1_3;
                                                                    } else {
                                                                      goto switch_1_default;
#line 193
                                                                      if (0) {
                                                                        switch_1_12292: 
                                                                        {
#line 195
                                                                        s__new_session = 1;
#line 196
                                                                        s__state = 4096;
#line 197
                                                                        s__ctx__stats__sess_connect_renegotiate ++;
                                                                        }
                                                                        switch_1_16384: 
                                                                        {

                                                                        }
                                                                        switch_1_4096: 
                                                                        {

                                                                        }
                                                                        switch_1_20480: 
                                                                        {

                                                                        }
                                                                        switch_1_4099: 
                                                                        {
#line 202
                                                                        s__server = 0;
                                                                        }
#line 203
                                                                        if (cb !=
                                                                            0) {

                                                                        }
                                                                        {
#line 207
                                                                        __cil_tmp55 = s__version -
                                                                                      65280;
                                                                        }
#line 208
                                                                        if (__cil_tmp55 !=
                                                                            768) {
                                                                          {
#line 209
                                                                          ret = -1;
                                                                          }
                                                                          goto end;
                                                                        }
                                                                        {
#line 213
                                                                        s__type = 4096;
                                                                        }
#line 214
                                                                        if (s__init_buf___0 ==
                                                                            0) {
                                                                          {
#line 215
                                                                          buf = ssl3_setup_buffers();
                                                                          }
#line 216
                                                                          if (buf ==
                                                                              0) {
                                                                            {
#line 217
                                                                            ret = -1;
                                                                            }
                                                                            goto end;
                                                                          }
#line 220
                                                                          if (! tmp___3) {
                                                                            {
#line 221
                                                                            ret = -1;
                                                                            }
                                                                            goto end;
                                                                          }
                                                                          {
#line 224
                                                                          s__init_buf___0 = buf;
                                                                          }
                                                                        }
#line 226
                                                                        if (! tmp___4) {
                                                                          {
#line 227
                                                                          ret = -1;
                                                                          }
                                                                          goto end;
                                                                        }
#line 230
                                                                        if (! tmp___5) {
                                                                          {
#line 231
                                                                          ret = -1;
                                                                          }
                                                                          goto end;
                                                                        }
                                                                        {
#line 234
                                                                        s__state = 4368;
#line 235
                                                                        s__ctx__stats__sess_connect ++;
#line 236
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4368: 
                                                                        {

                                                                        }
                                                                        switch_1_4369: 
                                                                        {
#line 240
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
#line 241 "s3_clnt.cil.c"
                                                                        ret = ssl3_client_hello();
                                                                        }
#line 242
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 245
                                                                        s__state = 4384;
#line 246
                                                                        s__init_num = 0;
                                                                        }
#line 247
                                                                        if (s__bbio !=
                                                                            s__wbio) {

                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4384: 
                                                                        {

                                                                        }
                                                                        switch_1_4385: 
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
#line 253 "s3_clnt.cil.c"
                                                                        ret = ssl3_get_server_hello();
                                                                        }
#line 254
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
#line 257
                                                                        if (s__hit) {
                                                                          {
#line 258
                                                                          s__state = 4560;
                                                                          }
                                                                        } else {
                                                                          {
#line 260
                                                                          s__state = 4400;
                                                                          }
                                                                        }
                                                                        {
#line 262
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4400: 
                                                                        {

                                                                        }
                                                                        switch_1_4401: 
                                                                        {

                                                                        }
#line 266
                                                                        if (s__s3__tmp__new_cipher__algorithms -
                                                                            256) {
                                                                          {
#line 267
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
#line 269 "s3_clnt.cil.c"
                                                                          ret = ssl3_get_server_certificate();
                                                                          }
#line 270
                                                                          if (ret <=
                                                                              0) {
                                                                            goto end;
                                                                          }
                                                                        }
                                                                        {
#line 274
                                                                        s__state = 4416;
#line 275
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4416: 
                                                                        {

                                                                        }
                                                                        switch_1_4417: 
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
#line 279 "s3_clnt.cil.c"
                                                                        ret = ssl3_get_key_exchange();
                                                                        }
#line 280
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 283
                                                                        s__state = 4432;
#line 284
                                                                        s__init_num = 0;
                                                                        }
#line 285
                                                                        if (! tmp___6) {
                                                                          {
#line 286
                                                                          ret = -1;
                                                                          }
                                                                          goto end;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4432: 
                                                                        {

                                                                        }
                                                                        switch_1_4433: 
                                                                        {

                                                                        {
#line 75 "spec.work"
                                                                        __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 76
                                                                        if (__MONITOR_STATE_state ==
                                                                            5) {
#line 77
                                                                          error_fn();
                                                                        }
#line 79
                                                                        __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                        {

                                                                        }
                                                                        }
#line 292 "s3_clnt.cil.c"
                                                                        ret = ssl3_get_certificate_request();
                                                                        }
#line 293
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 296
                                                                        s__state = 4448;
#line 297
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4448: 
                                                                        {

                                                                        }
                                                                        switch_1_4449: 
                                                                        {

                                                                        {
#line 89 "spec.work"
                                                                        __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 90
                                                                        if (__MONITOR_STATE_state ==
                                                                            4) {
#line 91
                                                                          __MONITOR_STATE_state = 5;
                                                                        }
#line 93
                                                                        __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                        {

                                                                        }
                                                                        }
#line 301 "s3_clnt.cil.c"
                                                                        ret = ssl3_get_server_done();
                                                                        }
#line 302
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
#line 305
                                                                        if (s__s3__tmp__cert_req) {
                                                                          {
#line 306
                                                                          s__state = 4464;
                                                                          }
                                                                        } else {
                                                                          {
#line 308
                                                                          s__state = 4480;
                                                                          }
                                                                        }
                                                                        {
#line 310
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4464: 
                                                                        {

                                                                        }
                                                                        switch_1_4465: 
                                                                        {

                                                                        }
                                                                        switch_1_4466: 
                                                                        {

                                                                        }
                                                                        switch_1_4467: 
                                                                        {
#line 316
                                                                        ret = ssl3_send_client_certificate();
                                                                        }
#line 317
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 320
                                                                        s__state = 4480;
#line 321
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4480: 
                                                                        {

                                                                        }
                                                                        switch_1_4481: 
                                                                        {
#line 325
                                                                        ret = ssl3_send_client_key_exchange();
                                                                        }
#line 326
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 329
                                                                        l = (unsigned long )s__s3__tmp__new_cipher__algorithms;
                                                                        }
#line 330
                                                                        if (s__s3__tmp__cert_req ==
                                                                            1) {
                                                                          {
#line 331
                                                                          s__state = 4496;
                                                                          }
                                                                        } else {
                                                                          {
#line 333
                                                                          s__state = 4512;
#line 334
                                                                          s__s3__change_cipher_spec = 0;
                                                                          }
                                                                        }
                                                                        {
#line 336
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4496: 
                                                                        {

                                                                        }
                                                                        switch_1_4497: 
                                                                        {
#line 340
                                                                        ret = ssl3_send_client_verify();
                                                                        }
#line 341
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 344
                                                                        s__state = 4512;
#line 345
                                                                        s__init_num = 0;
#line 346
                                                                        s__s3__change_cipher_spec = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4512: 
                                                                        {

                                                                        }
                                                                        switch_1_4513: 
                                                                        {
#line 350
                                                                        ret = ssl3_send_change_cipher_spec();
                                                                        }
#line 351
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 354
                                                                        s__state = 4528;
#line 355
                                                                        s__init_num = 0;
#line 356
                                                                        s__session__cipher = s__s3__tmp__new_cipher;
                                                                        }
#line 357
                                                                        if (s__s3__tmp__new_compression ==
                                                                            0) {
                                                                          {
#line 358
                                                                          s__session__compress_meth = 0;
                                                                          }
                                                                        } else {
                                                                          {
#line 360
                                                                          s__session__compress_meth = s__s3__tmp__new_compression__id;
                                                                          }
                                                                        }
#line 362
                                                                        if (! tmp___7) {
                                                                          {
#line 363
                                                                          ret = -1;
                                                                          }
                                                                          goto end;
                                                                        }
#line 366
                                                                        if (! tmp___8) {
                                                                          {
#line 367
                                                                          ret = -1;
                                                                          }
                                                                          goto end;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4528: 
                                                                        {

                                                                        }
                                                                        switch_1_4529: 
                                                                        {
#line 373
                                                                        ret = ssl3_send_finished();
                                                                        }
#line 374
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
                                                                        {
#line 377
                                                                        s__state = 4352;
#line 378
                                                                        __cil_tmp56 = (long )s__s3__flags;
#line 379
                                                                        __cil_tmp57 = __cil_tmp56 +
                                                                                      5L;
#line 380
                                                                        s__s3__flags = (int )__cil_tmp57;
                                                                        }
#line 381
                                                                        if (s__hit) {
                                                                          {
#line 382
                                                                          s__s3__tmp__next_state___0 = 3;
#line 384
                                                                          __cil_tmp58 = (long )s__s3__flags;
                                                                          }
#line 385
                                                                          if (__cil_tmp58 -
                                                                              2L) {
                                                                            {
#line 386
                                                                            s__state = 3;
#line 387
                                                                            __cil_tmp59 = (long )s__s3__flags;
#line 388
                                                                            __cil_tmp60 = __cil_tmp59 +
                                                                                          4L;
#line 389
                                                                            s__s3__flags = (int )__cil_tmp60;
#line 390
                                                                            s__s3__delay_buf_pop_ret = 0;
                                                                            }
                                                                          }
                                                                        } else {
                                                                          {
#line 394
                                                                          s__s3__tmp__next_state___0 = 4560;
                                                                          }
                                                                        }
                                                                        {
#line 396
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4560: 
                                                                        {

                                                                        }
                                                                        switch_1_4561: 
                                                                        {
#line 400
                                                                        ret = ssl3_get_finished();
                                                                        }
#line 401
                                                                        if (ret <=
                                                                            0) {
                                                                          goto end;
                                                                        }
#line 404
                                                                        if (s__hit) {
                                                                          {
#line 405
                                                                          s__state = 4512;
                                                                          }
                                                                        } else {
                                                                          {
#line 407
                                                                          s__state = 3;
                                                                          }
                                                                        }
                                                                        {
#line 409
                                                                        s__init_num = 0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_4352: 
                                                                        {
#line 413
                                                                        __cil_tmp61 = (long )num1;
                                                                        }
#line 414
                                                                        if (__cil_tmp61 >
                                                                            0L) {
                                                                          {
#line 415
                                                                          s__rwstate = 2;
#line 416
                                                                          num1 = tmp___9;
#line 418
                                                                          __cil_tmp62 = (long )num1;
                                                                          }
#line 419
                                                                          if (__cil_tmp62 <=
                                                                              0L) {
                                                                            {
#line 420
                                                                            ret = -1;
                                                                            }
                                                                            goto end;
                                                                          }
                                                                          {
#line 424
                                                                          s__rwstate = 1;
                                                                          }
                                                                        }
                                                                        {
#line 427
                                                                        s__state = s__s3__tmp__next_state___0;
                                                                        }
                                                                        goto switch_1_break;
                                                                        switch_1_3: 
#line 430
                                                                        if (s__init_buf___0 !=
                                                                            0) {
                                                                          {
#line 431
                                                                          s__init_buf___0 = 0;
                                                                          }
                                                                        }
                                                                        {
#line 434
                                                                        __cil_tmp63 = (long )s__s3__flags;
#line 435
                                                                        __cil_tmp64 = __cil_tmp63 -
                                                                                      4L;
                                                                        }
#line 436
                                                                        if (! __cil_tmp64) {

                                                                        }
                                                                        {
#line 440
                                                                        s__init_num = 0;
#line 441
                                                                        s__new_session = 0;
                                                                        }
#line 442
                                                                        if (s__hit) {
                                                                          {
#line 443
                                                                          s__ctx__stats__sess_hit ++;
                                                                          }
                                                                        }
                                                                        {
#line 445
                                                                        ret = 1;
#line 446
                                                                        s__ctx__stats__sess_connect_good ++;
                                                                        }
#line 447
                                                                        if (cb !=
                                                                            0) {

                                                                        }
                                                                        goto end;
                                                                        switch_1_default: 
                                                                        {
#line 452
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
#line 490
    if (! s__s3__tmp__reuse_message) {
#line 491
      if (! skip) {
#line 492
        if (s__debug) {
          {
#line 493
          ret = BIO_flush();
          }
#line 494
          if (ret <= 0) {
            goto end;
          }
        }
#line 498
        if (cb != 0) {
#line 499
          if (s__state != state) {
            {
#line 500
            new_state = s__state;
#line 501
            s__state = state;
#line 502
            s__state = new_state;
            }
          }
        }
      }
    }
    {
#line 507
    skip = 0;
    }
  }
  {

  }
  end: 
  {
#line 513
  s__in_handshake --;
  }
#line 514
  if (cb != 0) {

  }
#line 517
  return (ret);
#line 519
  return (-1);
}
}

#line 522 "s3_clnt.cil.c"
int entry(void) 
{ int s ;

  {
  {
#line 527
  s = 12292;
#line 528
  ssl3_connect(12292);
  }
#line 530
  return (0);
}
}

void __initialize__(void) 
{ 

  {

}
}

