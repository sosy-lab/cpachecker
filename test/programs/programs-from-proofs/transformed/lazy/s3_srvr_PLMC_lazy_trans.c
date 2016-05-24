extern int __VERIFIER_nondet_int();
extern long __VERIFIER_nondet_long(void);
void ssl3_init_finished_mac();
int ssl3_send_server_certificate();
int ssl3_get_finished();
int ssl3_send_change_cipher_spec();
int ssl3_send_finished();
int ssl3_setup_buffers();
int ssl_init_wbio_buffer();
int ssl3_get_client_hello();
int ssl3_check_client_hello();
int ssl3_send_server_hello();
int ssl3_send_server_key_exchange();
int ssl3_send_certificate_request();
int ssl3_send_server_done();
int ssl3_get_client_key_exchange();
int ssl3_get_client_certificate();
int ssl3_get_cert_verify();
int ssl3_send_hello_request();
int ssl3_accept(int initial_state );
void ERR();
int main(void);
int __return_26456;
int __return_26656;
int __return_26454;
int __return_26308;
int __return_26452;
int __return_26309;
int __return_26450;
int __return_26313;
int __return_26312;
int __return_26440;
int __return_26321;
int __return_26320;
int __return_26438;
int __return_26324;
int __return_26436;
int __return_26327;
int __return_26434;
int __return_26330;
int __return_26432;
int __return_26333;
int __return_26430;
int __return_26336;
int __return_26428;
int __return_26339;
int __return_26338;
int __return_26426;
int __return_26340;
int __return_26424;
int __return_26341;
int __return_26422;
int __return_26420;
int __return_26343;
int __return_26421;
int __return_26342;
int __return_26423;
int __return_26425;
int __return_26427;
int __return_26337;
int __return_26429;
int __return_26335;
int __return_26334;
int __return_26431;
int __return_26332;
int __return_26331;
int __return_26433;
int __return_26329;
int __return_26328;
int __return_26435;
int __return_26326;
int __return_26325;
int __return_26437;
int __return_26323;
int __return_26322;
int __return_26439;
int __return_26644;
int __return_26418;
int __return_26347;
int __return_26346;
int __return_26416;
int __return_26351;
int __return_26350;
int __return_26414;
int __return_26355;
int __return_26354;
int __return_26412;
int __return_26359;
int __return_26358;
int __return_26410;
int __return_26363;
int __return_26362;
int __return_26408;
int __return_26367;
int __return_26366;
int __return_26365;
int __return_26406;
int __return_26368;
int __return_26404;
int __return_26369;
int __return_26402;
int __return_26400;
int __return_26371;
int __return_26401;
int __return_26370;
int __return_26403;
int __return_26405;
int __return_26407;
int __return_26364;
int __return_26409;
int __return_26361;
int __return_26360;
int __return_26411;
int __return_26357;
int __return_26356;
int __return_26413;
int __return_26353;
int __return_26352;
int __return_26415;
int __return_26349;
int __return_26348;
int __return_26417;
int __return_26345;
int __return_26344;
int __return_26419;
int __return_26319;
int __return_26318;
int __return_26441;
int __return_26311;
int __return_26310;
int __return_26448;
int __return_26314;
int __return_26446;
int __return_26444;
int __return_26316;
int __return_26442;
int __return_26317;
int __return_26443;
int __return_26646;
int __return_26445;
int __return_26315;
int __return_26447;
int __return_26648;
int __return_26449;
int __return_26650;
int __return_26451;
int __return_26652;
int __return_26453;
int __return_26654;
int __return_26455;
int main()
{
int s ;
int tmp ;
s = 8464;
{
int __tmp_1 = s;
int initial_state = __tmp_1;
int s__info_callback = __VERIFIER_nondet_int();
s__info_callback = __VERIFIER_nondet_int();
int s__in_handshake = __VERIFIER_nondet_int();
s__in_handshake = __VERIFIER_nondet_int();
int s__state ;
int s__new_session ;
int s__server ;
int s__version = __VERIFIER_nondet_int();
s__version = __VERIFIER_nondet_int();
int s__type ;
int s__init_num ;
int s__hit = __VERIFIER_nondet_int();
s__hit = __VERIFIER_nondet_int();
int s__rwstate ;
int s__init_buf___0 ;
int s__debug = __VERIFIER_nondet_int();
s__debug = __VERIFIER_nondet_int();
int s__shutdown ;
int s__cert = __VERIFIER_nondet_int();
s__cert = __VERIFIER_nondet_int();
int s__options = __VERIFIER_nondet_int();
s__options = __VERIFIER_nondet_int();
int s__verify_mode = __VERIFIER_nondet_int();
s__verify_mode = __VERIFIER_nondet_int();
int s__session__peer = __VERIFIER_nondet_int();
s__session__peer = __VERIFIER_nondet_int();
int s__cert__pkeys__AT0__privatekey = __VERIFIER_nondet_int();
s__cert__pkeys__AT0__privatekey = __VERIFIER_nondet_int();
int s__ctx__info_callback = __VERIFIER_nondet_int();
s__ctx__info_callback = __VERIFIER_nondet_int();
int s__ctx__stats__sess_accept_renegotiate = __VERIFIER_nondet_int();
s__ctx__stats__sess_accept_renegotiate = __VERIFIER_nondet_int();
int s__ctx__stats__sess_accept = __VERIFIER_nondet_int();
s__ctx__stats__sess_accept = __VERIFIER_nondet_int();
int s__ctx__stats__sess_accept_good = __VERIFIER_nondet_int();
s__ctx__stats__sess_accept_good = __VERIFIER_nondet_int();
int s__s3__tmp__cert_request ;
int s__s3__tmp__reuse_message = __VERIFIER_nondet_int();
s__s3__tmp__reuse_message = __VERIFIER_nondet_int();
int s__s3__tmp__use_rsa_tmp ;
int s__s3__tmp__new_cipher = __VERIFIER_nondet_int();
s__s3__tmp__new_cipher = __VERIFIER_nondet_int();
int s__s3__tmp__new_cipher__algorithms = __VERIFIER_nondet_int();
s__s3__tmp__new_cipher__algorithms = __VERIFIER_nondet_int();
int s__s3__tmp__next_state___0 ;
int s__s3__tmp__new_cipher__algo_strength = __VERIFIER_nondet_int();
s__s3__tmp__new_cipher__algo_strength = __VERIFIER_nondet_int();
int s__session__cipher ;
int buf = __VERIFIER_nondet_int();
buf = __VERIFIER_nondet_int();
unsigned long l ;
unsigned long Time ;
unsigned long tmp = __VERIFIER_nondet_long();
tmp = __VERIFIER_nondet_long();
int cb ;
long num1 ;
int ret ;
int new_state ;
int state ;
int skip ;
int got_new_session ;
int tmp___1 = __VERIFIER_nondet_int();
tmp___1 = __VERIFIER_nondet_int();
int tmp___2 = __VERIFIER_nondet_int();
tmp___2 = __VERIFIER_nondet_int();
int tmp___3 ;
int tmp___4 ;
int tmp___5 ;
int tmp___6 ;
int tmp___7 ;
long tmp___8 = __VERIFIER_nondet_long();
tmp___8 = __VERIFIER_nondet_long();
int tmp___9 ;
int tmp___10 ;
int __cil_tmp55 ;
unsigned long __cil_tmp56 ;
unsigned long __cil_tmp57 ;
unsigned long __cil_tmp58 ;
unsigned long __cil_tmp59 ;
int __cil_tmp60 ;
unsigned long __cil_tmp61 ;
s__state = initial_state;
Time = tmp;
cb = 0;
ret = -1;
skip = 0;
got_new_session = 0;
if (s__info_callback != 0)
{
cb = s__info_callback;
goto label_4910;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_4910;
}
else 
{
label_4910:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_26456 = -1;
}
else 
{
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_5288;
}
else 
{
if (s__state == 16384)
{
label_5288:; 
goto label_5290;
}
else 
{
if (s__state == 8192)
{
label_5290:; 
goto label_5292;
}
else 
{
if (s__state == 24576)
{
label_5292:; 
goto label_5294;
}
else 
{
if (s__state == 8195)
{
label_5294:; 
s__server = 1;
if (cb != 0)
{
goto label_5299;
}
else 
{
label_5299:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26454 = -1;
goto label_26455;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25609;
}
else 
{
s__init_buf___0 = buf;
goto label_5311;
}
}
else 
{
label_5311:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25609;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25609;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_5331;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_5331:; 
goto label_5333;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_5267;
}
else 
{
if (s__state == 8481)
{
label_5267:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25609;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_5333;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_5333;
}
else 
{
if (s__state == 8464)
{
goto label_5246;
}
else 
{
if (s__state == 8465)
{
label_5246:; 
goto label_5248;
}
else 
{
if (s__state == 8466)
{
label_5248:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_5365:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26307;
}
else 
{
label_26307:; 
 __return_26308 = ret;
}
tmp = __return_26308;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_5365;
}
else 
{
goto label_5361;
}
}
else 
{
label_5361:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5391;
}
else 
{
goto label_5391;
}
}
else 
{
goto label_5391;
}
}
}
else 
{
goto label_5391;
}
}
else 
{
label_5391:; 
skip = 0;
label_5397:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_5772;
}
else 
{
if (s__state == 16384)
{
label_5772:; 
goto label_5774;
}
else 
{
if (s__state == 8192)
{
label_5774:; 
goto label_5776;
}
else 
{
if (s__state == 24576)
{
label_5776:; 
goto label_5778;
}
else 
{
if (s__state == 8195)
{
label_5778:; 
s__server = 1;
if (cb != 0)
{
goto label_5783;
}
else 
{
label_5783:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26452 = -1;
goto label_26453;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25611;
}
else 
{
s__init_buf___0 = buf;
goto label_5795;
}
}
else 
{
label_5795:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25611;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25611;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_5815;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_5815:; 
goto label_5817;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_5751;
}
else 
{
if (s__state == 8481)
{
label_5751:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25611;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_5817;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_5817;
}
else 
{
if (s__state == 8464)
{
goto label_5738;
}
else 
{
if (s__state == 8465)
{
label_5738:; 
return 1;
}
else 
{
if (s__state == 8496)
{
goto label_5719;
}
else 
{
if (s__state == 8497)
{
label_5719:; 
ret = ssl3_send_server_hello();
if (ret <= 0)
{
label_5849:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26305;
}
else 
{
label_26305:; 
 __return_26309 = ret;
}
tmp = __return_26309;
goto label_26464;
}
else 
{
if (s__hit == 0)
{
s__state = 8512;
goto label_5729;
}
else 
{
s__state = 8656;
label_5729:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_5849;
}
else 
{
goto label_5845;
}
}
else 
{
label_5845:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5875;
}
else 
{
goto label_5875;
}
}
else 
{
goto label_5875;
}
}
}
else 
{
goto label_5875;
}
}
else 
{
label_5875:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_6304;
}
else 
{
if (s__state == 16384)
{
label_6304:; 
goto label_6306;
}
else 
{
if (s__state == 8192)
{
label_6306:; 
goto label_6308;
}
else 
{
if (s__state == 24576)
{
label_6308:; 
goto label_6310;
}
else 
{
if (s__state == 8195)
{
label_6310:; 
s__server = 1;
if (cb != 0)
{
goto label_6315;
}
else 
{
label_6315:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26450 = -1;
goto label_26451;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25613;
}
else 
{
s__init_buf___0 = buf;
goto label_6327;
}
}
else 
{
label_6327:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25613;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25613;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_6347;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_6347:; 
goto label_6349;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_6283;
}
else 
{
if (s__state == 8481)
{
label_6283:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25613;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_6349;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_6349;
}
else 
{
if (s__state == 8464)
{
goto label_6262;
}
else 
{
if (s__state == 8465)
{
label_6262:; 
goto label_6264;
}
else 
{
if (s__state == 8466)
{
label_6264:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_6432:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26297;
}
else 
{
label_26297:; 
 __return_26313 = ret;
}
tmp = __return_26313;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_6432;
}
else 
{
goto label_6422;
}
}
else 
{
label_6422:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_6235;
}
else 
{
if (s__state == 8513)
{
label_6235:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25613;
}
else 
{
goto label_6245;
}
}
else 
{
skip = 1;
label_6245:; 
s__state = 8528;
s__init_num = 0;
goto label_6349;
}
}
else 
{
if (s__state == 8528)
{
goto label_6167;
}
else 
{
if (s__state == 8529)
{
label_6167:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_6176;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_6176:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_6223;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_6210;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_6217:; 
label_6223:; 
s__state = 8544;
s__init_num = 0;
goto label_6349;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_6199;
}
else 
{
tmp___7 = 512;
label_6199:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_6210;
}
else 
{
skip = 1;
goto label_6217;
}
}
}
}
}
}
else 
{
goto label_6210;
}
}
else 
{
label_6210:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_6430:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26299;
}
else 
{
label_26299:; 
 __return_26312 = ret;
}
tmp = __return_26312;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_6430;
}
else 
{
goto label_6420;
}
}
else 
{
label_6420:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_6115;
}
else 
{
if (s__state == 8545)
{
label_6115:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_6160;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_6129;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_6152:; 
label_6160:; 
goto label_6349;
}
}
else 
{
label_6129:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_6143;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_6152;
}
else 
{
label_6143:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25613;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_6152;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_6100;
}
else 
{
if (s__state == 8561)
{
label_6100:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25613;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_6349;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25613;
}
else 
{
s__rwstate = 1;
goto label_6089;
}
}
else 
{
label_6089:; 
s__state = s__s3__tmp__next_state___0;
goto label_6349;
}
}
else 
{
if (s__state == 8576)
{
goto label_6057;
}
else 
{
if (s__state == 8577)
{
label_6057:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25613;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_6073;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25613;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_6073:; 
goto label_6349;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_6043;
}
else 
{
if (s__state == 8593)
{
label_6043:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25613;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_6349;
}
}
else 
{
if (s__state == 8608)
{
goto label_6030;
}
else 
{
if (s__state == 8609)
{
label_6030:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25613;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_6349:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25613;
}
else 
{
goto label_6424;
}
}
else 
{
label_6424:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6499;
}
else 
{
goto label_6499;
}
}
else 
{
goto label_6499;
}
}
}
else 
{
goto label_6499;
}
}
else 
{
label_6499:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_10781;
}
else 
{
if (s__state == 16384)
{
label_10781:; 
goto label_10783;
}
else 
{
if (s__state == 8192)
{
label_10783:; 
goto label_10785;
}
else 
{
if (s__state == 24576)
{
label_10785:; 
goto label_10787;
}
else 
{
if (s__state == 8195)
{
label_10787:; 
s__server = 1;
if (cb != 0)
{
goto label_10792;
}
else 
{
label_10792:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26440 = -1;
goto label_26441;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25623;
}
else 
{
s__init_buf___0 = buf;
goto label_10804;
}
}
else 
{
label_10804:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25623;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25623;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_10824;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_10824:; 
goto label_10826;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_10760;
}
else 
{
if (s__state == 8481)
{
label_10760:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25623;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_10826;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_10826;
}
else 
{
if (s__state == 8464)
{
goto label_10739;
}
else 
{
if (s__state == 8465)
{
label_10739:; 
goto label_10741;
}
else 
{
if (s__state == 8466)
{
label_10741:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_10909:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26281;
}
else 
{
label_26281:; 
 __return_26321 = ret;
}
tmp = __return_26321;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_10909;
}
else 
{
goto label_10899;
}
}
else 
{
label_10899:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_10712;
}
else 
{
if (s__state == 8513)
{
label_10712:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25623;
}
else 
{
goto label_10722;
}
}
else 
{
skip = 1;
label_10722:; 
s__state = 8528;
s__init_num = 0;
goto label_10826;
}
}
else 
{
if (s__state == 8528)
{
goto label_10644;
}
else 
{
if (s__state == 8529)
{
label_10644:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_10653;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_10653:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_10700;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_10687;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_10694:; 
label_10700:; 
s__state = 8544;
s__init_num = 0;
goto label_10826;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_10676;
}
else 
{
tmp___7 = 512;
label_10676:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_10687;
}
else 
{
skip = 1;
goto label_10694;
}
}
}
}
}
}
else 
{
goto label_10687;
}
}
else 
{
label_10687:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_10907:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26283;
}
else 
{
label_26283:; 
 __return_26320 = ret;
}
tmp = __return_26320;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_10907;
}
else 
{
goto label_10897;
}
}
else 
{
label_10897:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_10972;
}
else 
{
goto label_10972;
}
}
else 
{
goto label_10972;
}
}
}
else 
{
goto label_10972;
}
}
else 
{
label_10972:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_11398;
}
else 
{
if (s__state == 16384)
{
label_11398:; 
goto label_11400;
}
else 
{
if (s__state == 8192)
{
label_11400:; 
goto label_11402;
}
else 
{
if (s__state == 24576)
{
label_11402:; 
goto label_11404;
}
else 
{
if (s__state == 8195)
{
label_11404:; 
s__server = 1;
if (cb != 0)
{
goto label_11409;
}
else 
{
label_11409:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26438 = -1;
goto label_26439;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25625;
}
else 
{
s__init_buf___0 = buf;
goto label_11421;
}
}
else 
{
label_11421:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25625;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25625;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_11441;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_11441:; 
goto label_11443;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_11377;
}
else 
{
if (s__state == 8481)
{
label_11377:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25625;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_11443;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_11443;
}
else 
{
if (s__state == 8464)
{
goto label_11356;
}
else 
{
if (s__state == 8465)
{
label_11356:; 
goto label_11358;
}
else 
{
if (s__state == 8466)
{
label_11358:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_11509:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26275;
}
else 
{
label_26275:; 
 __return_26324 = ret;
}
tmp = __return_26324;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_11509;
}
else 
{
goto label_11501;
}
}
else 
{
label_11501:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_11329;
}
else 
{
if (s__state == 8513)
{
label_11329:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25625;
}
else 
{
goto label_11339;
}
}
else 
{
skip = 1;
label_11339:; 
s__state = 8528;
s__init_num = 0;
goto label_11443;
}
}
else 
{
if (s__state == 8528)
{
goto label_11271;
}
else 
{
if (s__state == 8529)
{
label_11271:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_11280;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_11280:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_11320;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_11316:; 
label_11320:; 
s__state = 8544;
s__init_num = 0;
goto label_11443;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_11303;
}
else 
{
tmp___7 = 512;
label_11303:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_11316;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_11219;
}
else 
{
if (s__state == 8545)
{
label_11219:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_11264;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_11233;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_11256:; 
label_11264:; 
goto label_11443;
}
}
else 
{
label_11233:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_11247;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_11256;
}
else 
{
label_11247:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25625;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_11256;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_11204;
}
else 
{
if (s__state == 8561)
{
label_11204:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25625;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_11443;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25625;
}
else 
{
s__rwstate = 1;
goto label_11193;
}
}
else 
{
label_11193:; 
s__state = s__s3__tmp__next_state___0;
goto label_11443;
}
}
else 
{
if (s__state == 8576)
{
goto label_11161;
}
else 
{
if (s__state == 8577)
{
label_11161:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25625;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_11177;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25625;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_11177:; 
goto label_11443;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_11147;
}
else 
{
if (s__state == 8593)
{
label_11147:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25625;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_11443;
}
}
else 
{
if (s__state == 8608)
{
goto label_11134;
}
else 
{
if (s__state == 8609)
{
label_11134:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25625;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_11443:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25625;
}
else 
{
goto label_11503;
}
}
else 
{
label_11503:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_11563;
}
else 
{
goto label_11563;
}
}
else 
{
goto label_11563;
}
}
}
else 
{
goto label_11563;
}
}
else 
{
label_11563:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_11985;
}
else 
{
if (s__state == 16384)
{
label_11985:; 
goto label_11987;
}
else 
{
if (s__state == 8192)
{
label_11987:; 
goto label_11989;
}
else 
{
if (s__state == 24576)
{
label_11989:; 
goto label_11991;
}
else 
{
if (s__state == 8195)
{
label_11991:; 
s__server = 1;
if (cb != 0)
{
goto label_11996;
}
else 
{
label_11996:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26436 = -1;
goto label_26437;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25627;
}
else 
{
s__init_buf___0 = buf;
goto label_12008;
}
}
else 
{
label_12008:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25627;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25627;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_12028;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_12028:; 
goto label_12030;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_11964;
}
else 
{
if (s__state == 8481)
{
label_11964:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25627;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_12030;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_12030;
}
else 
{
if (s__state == 8464)
{
goto label_11943;
}
else 
{
if (s__state == 8465)
{
label_11943:; 
goto label_11945;
}
else 
{
if (s__state == 8466)
{
label_11945:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_12096:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26269;
}
else 
{
label_26269:; 
 __return_26327 = ret;
}
tmp = __return_26327;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12096;
}
else 
{
goto label_12088;
}
}
else 
{
label_12088:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_11916;
}
else 
{
if (s__state == 8513)
{
label_11916:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25627;
}
else 
{
goto label_11926;
}
}
else 
{
skip = 1;
label_11926:; 
s__state = 8528;
s__init_num = 0;
goto label_12030;
}
}
else 
{
if (s__state == 8528)
{
goto label_11858;
}
else 
{
if (s__state == 8529)
{
label_11858:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_11867;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_11867:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_11907;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_11903:; 
label_11907:; 
s__state = 8544;
s__init_num = 0;
goto label_12030;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_11890;
}
else 
{
tmp___7 = 512;
label_11890:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_11903;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_11806;
}
else 
{
if (s__state == 8545)
{
label_11806:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_11851;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_11820;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_11843:; 
label_11851:; 
goto label_12030;
}
}
else 
{
label_11820:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_11834;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_11843;
}
else 
{
label_11834:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25627;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_11843;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_11791;
}
else 
{
if (s__state == 8561)
{
label_11791:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25627;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_12030;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25627;
}
else 
{
s__rwstate = 1;
goto label_11780;
}
}
else 
{
label_11780:; 
s__state = s__s3__tmp__next_state___0;
goto label_12030;
}
}
else 
{
if (s__state == 8576)
{
goto label_11748;
}
else 
{
if (s__state == 8577)
{
label_11748:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25627;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_11764;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25627;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_11764:; 
goto label_12030;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_11734;
}
else 
{
if (s__state == 8593)
{
label_11734:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25627;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_12030;
}
}
else 
{
if (s__state == 8608)
{
goto label_11721;
}
else 
{
if (s__state == 8609)
{
label_11721:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25627;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_12030:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25627;
}
else 
{
goto label_12090;
}
}
else 
{
label_12090:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12150;
}
else 
{
goto label_12150;
}
}
else 
{
goto label_12150;
}
}
}
else 
{
goto label_12150;
}
}
else 
{
label_12150:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_12572;
}
else 
{
if (s__state == 16384)
{
label_12572:; 
goto label_12574;
}
else 
{
if (s__state == 8192)
{
label_12574:; 
goto label_12576;
}
else 
{
if (s__state == 24576)
{
label_12576:; 
goto label_12578;
}
else 
{
if (s__state == 8195)
{
label_12578:; 
s__server = 1;
if (cb != 0)
{
goto label_12583;
}
else 
{
label_12583:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26434 = -1;
goto label_26435;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25629;
}
else 
{
s__init_buf___0 = buf;
goto label_12595;
}
}
else 
{
label_12595:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25629;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25629;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_12615;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_12615:; 
goto label_12617;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_12551;
}
else 
{
if (s__state == 8481)
{
label_12551:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25629;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_12617;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_12617;
}
else 
{
if (s__state == 8464)
{
goto label_12530;
}
else 
{
if (s__state == 8465)
{
label_12530:; 
goto label_12532;
}
else 
{
if (s__state == 8466)
{
label_12532:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_12683:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26263;
}
else 
{
label_26263:; 
 __return_26330 = ret;
}
tmp = __return_26330;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12683;
}
else 
{
goto label_12675;
}
}
else 
{
label_12675:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_12503;
}
else 
{
if (s__state == 8513)
{
label_12503:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25629;
}
else 
{
goto label_12513;
}
}
else 
{
skip = 1;
label_12513:; 
s__state = 8528;
s__init_num = 0;
goto label_12617;
}
}
else 
{
if (s__state == 8528)
{
goto label_12445;
}
else 
{
if (s__state == 8529)
{
label_12445:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_12454;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_12454:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_12494;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_12490:; 
label_12494:; 
s__state = 8544;
s__init_num = 0;
goto label_12617;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_12477;
}
else 
{
tmp___7 = 512;
label_12477:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_12490;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_12393;
}
else 
{
if (s__state == 8545)
{
label_12393:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_12438;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_12407;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_12430:; 
label_12438:; 
goto label_12617;
}
}
else 
{
label_12407:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_12421;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_12430;
}
else 
{
label_12421:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25629;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_12430;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_12378;
}
else 
{
if (s__state == 8561)
{
label_12378:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25629;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_12617;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25629;
}
else 
{
s__rwstate = 1;
goto label_12367;
}
}
else 
{
label_12367:; 
s__state = s__s3__tmp__next_state___0;
goto label_12617;
}
}
else 
{
if (s__state == 8576)
{
goto label_12335;
}
else 
{
if (s__state == 8577)
{
label_12335:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25629;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_12351;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25629;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_12351:; 
goto label_12617;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_12321;
}
else 
{
if (s__state == 8593)
{
label_12321:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25629;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_12617;
}
}
else 
{
if (s__state == 8608)
{
goto label_12308;
}
else 
{
if (s__state == 8609)
{
label_12308:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25629;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_12617:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25629;
}
else 
{
goto label_12677;
}
}
else 
{
label_12677:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12737;
}
else 
{
goto label_12737;
}
}
else 
{
goto label_12737;
}
}
}
else 
{
goto label_12737;
}
}
else 
{
label_12737:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_13159;
}
else 
{
if (s__state == 16384)
{
label_13159:; 
goto label_13161;
}
else 
{
if (s__state == 8192)
{
label_13161:; 
goto label_13163;
}
else 
{
if (s__state == 24576)
{
label_13163:; 
goto label_13165;
}
else 
{
if (s__state == 8195)
{
label_13165:; 
s__server = 1;
if (cb != 0)
{
goto label_13170;
}
else 
{
label_13170:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26432 = -1;
goto label_26433;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25631;
}
else 
{
s__init_buf___0 = buf;
goto label_13182;
}
}
else 
{
label_13182:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25631;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25631;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_13202;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_13202:; 
goto label_13204;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_13138;
}
else 
{
if (s__state == 8481)
{
label_13138:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25631;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_13204;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_13204;
}
else 
{
if (s__state == 8464)
{
goto label_13117;
}
else 
{
if (s__state == 8465)
{
label_13117:; 
goto label_13119;
}
else 
{
if (s__state == 8466)
{
label_13119:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_13270:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26257;
}
else 
{
label_26257:; 
 __return_26333 = ret;
}
tmp = __return_26333;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_13270;
}
else 
{
goto label_13262;
}
}
else 
{
label_13262:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13322;
}
else 
{
goto label_13322;
}
}
else 
{
goto label_13322;
}
}
}
else 
{
goto label_13322;
}
}
else 
{
label_13322:; 
skip = 0;
goto label_5397;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_13090;
}
else 
{
if (s__state == 8513)
{
label_13090:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25631;
}
else 
{
goto label_13100;
}
}
else 
{
skip = 1;
label_13100:; 
s__state = 8528;
s__init_num = 0;
goto label_13204;
}
}
else 
{
if (s__state == 8528)
{
goto label_13032;
}
else 
{
if (s__state == 8529)
{
label_13032:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_13041;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_13041:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_13081;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_13077:; 
label_13081:; 
s__state = 8544;
s__init_num = 0;
goto label_13204;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_13064;
}
else 
{
tmp___7 = 512;
label_13064:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_13077;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_12980;
}
else 
{
if (s__state == 8545)
{
label_12980:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_13025;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_12994;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_13017:; 
label_13025:; 
goto label_13204;
}
}
else 
{
label_12994:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_13008;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_13017;
}
else 
{
label_13008:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25631;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_13017;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_12965;
}
else 
{
if (s__state == 8561)
{
label_12965:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25631;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_13204;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25631;
}
else 
{
s__rwstate = 1;
goto label_12954;
}
}
else 
{
label_12954:; 
s__state = s__s3__tmp__next_state___0;
goto label_13204;
}
}
else 
{
if (s__state == 8576)
{
goto label_12922;
}
else 
{
if (s__state == 8577)
{
label_12922:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25631;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_12938;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25631;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_12938:; 
goto label_13204;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_12908;
}
else 
{
if (s__state == 8593)
{
label_12908:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25631;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_13204;
}
}
else 
{
if (s__state == 8608)
{
goto label_12895;
}
else 
{
if (s__state == 8609)
{
label_12895:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25631;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_13204:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25631;
}
else 
{
goto label_13264;
}
}
else 
{
label_13264:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13324;
}
else 
{
goto label_13324;
}
}
else 
{
goto label_13324;
}
}
}
else 
{
goto label_13324;
}
}
else 
{
label_13324:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_13747;
}
else 
{
if (s__state == 16384)
{
label_13747:; 
goto label_13749;
}
else 
{
if (s__state == 8192)
{
label_13749:; 
goto label_13751;
}
else 
{
if (s__state == 24576)
{
label_13751:; 
goto label_13753;
}
else 
{
if (s__state == 8195)
{
label_13753:; 
s__server = 1;
if (cb != 0)
{
goto label_13758;
}
else 
{
label_13758:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26430 = -1;
goto label_26431;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25633;
}
else 
{
s__init_buf___0 = buf;
goto label_13770;
}
}
else 
{
label_13770:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25633;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25633;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_13790;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_13790:; 
goto label_13792;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_13726;
}
else 
{
if (s__state == 8481)
{
label_13726:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25633;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_13792;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_13792;
}
else 
{
if (s__state == 8464)
{
goto label_13705;
}
else 
{
if (s__state == 8465)
{
label_13705:; 
goto label_13707;
}
else 
{
if (s__state == 8466)
{
label_13707:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_13858:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26251;
}
else 
{
label_26251:; 
 __return_26336 = ret;
}
tmp = __return_26336;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_13858;
}
else 
{
goto label_13850;
}
}
else 
{
label_13850:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13910;
}
else 
{
goto label_13910;
}
}
else 
{
goto label_13910;
}
}
}
else 
{
goto label_13910;
}
}
else 
{
label_13910:; 
skip = 0;
goto label_5397;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_13678;
}
else 
{
if (s__state == 8513)
{
label_13678:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25633;
}
else 
{
goto label_13688;
}
}
else 
{
skip = 1;
label_13688:; 
s__state = 8528;
s__init_num = 0;
goto label_13792;
}
}
else 
{
if (s__state == 8528)
{
goto label_13620;
}
else 
{
if (s__state == 8529)
{
label_13620:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_13629;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_13629:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_13669;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_13665:; 
label_13669:; 
s__state = 8544;
s__init_num = 0;
goto label_13792;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_13652;
}
else 
{
tmp___7 = 512;
label_13652:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_13665;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_13568;
}
else 
{
if (s__state == 8545)
{
label_13568:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_13613;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_13582;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_13605:; 
label_13613:; 
goto label_13792;
}
}
else 
{
label_13582:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_13596;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_13605;
}
else 
{
label_13596:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25633;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_13605;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_13553;
}
else 
{
if (s__state == 8561)
{
label_13553:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25633;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_13792;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25633;
}
else 
{
s__rwstate = 1;
goto label_13542;
}
}
else 
{
label_13542:; 
s__state = s__s3__tmp__next_state___0;
goto label_13792;
}
}
else 
{
if (s__state == 8576)
{
goto label_13510;
}
else 
{
if (s__state == 8577)
{
label_13510:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25633;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_13526;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25633;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_13526:; 
goto label_13792;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_13496;
}
else 
{
if (s__state == 8593)
{
label_13496:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25633;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_13792;
}
}
else 
{
if (s__state == 8608)
{
goto label_13483;
}
else 
{
if (s__state == 8609)
{
label_13483:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25633;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_13792:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25633;
}
else 
{
goto label_13852;
}
}
else 
{
label_13852:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13912;
}
else 
{
goto label_13912;
}
}
else 
{
goto label_13912;
}
}
}
else 
{
goto label_13912;
}
}
else 
{
label_13912:; 
skip = 0;
label_16076:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_16489;
}
else 
{
if (s__state == 16384)
{
label_16489:; 
goto label_16491;
}
else 
{
if (s__state == 8192)
{
label_16491:; 
goto label_16493;
}
else 
{
if (s__state == 24576)
{
label_16493:; 
goto label_16495;
}
else 
{
if (s__state == 8195)
{
label_16495:; 
s__server = 1;
if (cb != 0)
{
goto label_16500;
}
else 
{
label_16500:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26428 = -1;
goto label_26429;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25635;
}
else 
{
s__init_buf___0 = buf;
goto label_16512;
}
}
else 
{
label_16512:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25635;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25635;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_16532;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_16532:; 
goto label_16534;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_16468;
}
else 
{
if (s__state == 8481)
{
label_16468:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25635;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_16534;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_16534;
}
else 
{
if (s__state == 8464)
{
goto label_16447;
}
else 
{
if (s__state == 8465)
{
label_16447:; 
goto label_16449;
}
else 
{
if (s__state == 8466)
{
label_16449:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_16600:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26245;
}
else 
{
label_26245:; 
 __return_26339 = ret;
}
tmp = __return_26339;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_16600;
}
else 
{
goto label_16592;
}
}
else 
{
label_16592:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_16420;
}
else 
{
if (s__state == 8513)
{
label_16420:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25635;
}
else 
{
goto label_16430;
}
}
else 
{
skip = 1;
label_16430:; 
s__state = 8528;
s__init_num = 0;
goto label_16534;
}
}
else 
{
if (s__state == 8528)
{
goto label_16362;
}
else 
{
if (s__state == 8529)
{
label_16362:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_16371;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_16371:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_16411;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_16407:; 
label_16411:; 
s__state = 8544;
s__init_num = 0;
goto label_16534;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_16394;
}
else 
{
tmp___7 = 512;
label_16394:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_16407;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_16310;
}
else 
{
if (s__state == 8545)
{
label_16310:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_16355;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_16324;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_16347:; 
label_16355:; 
goto label_16534;
}
}
else 
{
label_16324:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_16338;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_16347;
}
else 
{
label_16338:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25635;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_16347;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_16295;
}
else 
{
if (s__state == 8561)
{
label_16295:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25635;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_16534;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25635;
}
else 
{
s__rwstate = 1;
goto label_16284;
}
}
else 
{
label_16284:; 
s__state = s__s3__tmp__next_state___0;
goto label_16534;
}
}
else 
{
if (s__state == 8576)
{
goto label_16252;
}
else 
{
if (s__state == 8577)
{
label_16252:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25635;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_16268;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25635;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_16268:; 
goto label_16534;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_16238;
}
else 
{
if (s__state == 8593)
{
label_16238:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25635;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_16534;
}
}
else 
{
if (s__state == 8608)
{
goto label_16225;
}
else 
{
if (s__state == 8609)
{
label_16225:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25635;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_16534:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25635;
}
else 
{
goto label_16594;
}
}
else 
{
label_16594:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16654;
}
else 
{
goto label_16654;
}
}
else 
{
goto label_16654;
}
}
}
else 
{
goto label_16654;
}
}
else 
{
label_16654:; 
skip = 0;
goto label_16076;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_16207;
}
else 
{
if (s__state == 8641)
{
label_16207:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_16598:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26247;
}
else 
{
label_26247:; 
 __return_26338 = ret;
}
tmp = __return_26338;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_16217;
}
else 
{
s__state = 3;
label_16217:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_16598;
}
else 
{
goto label_16590;
}
}
else 
{
label_16590:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16650;
}
else 
{
goto label_16650;
}
}
else 
{
goto label_16650;
}
}
}
else 
{
goto label_16650;
}
}
else 
{
label_16650:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_17036;
}
else 
{
if (s__state == 16384)
{
label_17036:; 
goto label_17038;
}
else 
{
if (s__state == 8192)
{
label_17038:; 
goto label_17040;
}
else 
{
if (s__state == 24576)
{
label_17040:; 
goto label_17042;
}
else 
{
if (s__state == 8195)
{
label_17042:; 
s__server = 1;
if (cb != 0)
{
goto label_17047;
}
else 
{
label_17047:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26426 = -1;
goto label_26427;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25637;
}
else 
{
s__init_buf___0 = buf;
goto label_17059;
}
}
else 
{
label_17059:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25637;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25637;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_17079;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_17079:; 
goto label_17081;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_17015;
}
else 
{
if (s__state == 8481)
{
label_17015:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25637;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_17081;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_17081;
}
else 
{
if (s__state == 8464)
{
goto label_17002;
}
else 
{
if (s__state == 8465)
{
label_17002:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_16982;
}
else 
{
if (s__state == 8513)
{
label_16982:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_17081;
}
else 
{
if (s__state == 8528)
{
goto label_16924;
}
else 
{
if (s__state == 8529)
{
label_16924:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_16933;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_16933:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_16973;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_16969:; 
label_16973:; 
s__state = 8544;
s__init_num = 0;
goto label_17081;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_16956;
}
else 
{
tmp___7 = 512;
label_16956:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_16969;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_16882;
}
else 
{
if (s__state == 8545)
{
label_16882:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_16917;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_16896;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_16908:; 
label_16917:; 
goto label_17081;
}
}
else 
{
label_16896:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_16908;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_16867;
}
else 
{
if (s__state == 8561)
{
label_16867:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25637;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_17081;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25637;
}
else 
{
s__rwstate = 1;
goto label_16856;
}
}
else 
{
label_16856:; 
s__state = s__s3__tmp__next_state___0;
goto label_17081;
}
}
else 
{
if (s__state == 8576)
{
goto label_16824;
}
else 
{
if (s__state == 8577)
{
label_16824:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25637;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_16840;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25637;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_16840:; 
goto label_17081;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_16810;
}
else 
{
if (s__state == 8593)
{
label_16810:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25637;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_17081;
}
}
else 
{
if (s__state == 8608)
{
goto label_16797;
}
else 
{
if (s__state == 8609)
{
label_16797:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25637;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_17081:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25637;
}
else 
{
goto label_17111;
}
}
else 
{
label_17111:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_16767;
}
else 
{
if (s__state == 8657)
{
label_16767:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25637;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_17113;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_17113:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26243;
}
else 
{
label_26243:; 
 __return_26340 = ret;
}
tmp = __return_26340;
goto label_26466;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_17113;
}
else 
{
goto label_17109;
}
}
else 
{
label_17109:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17139;
}
else 
{
goto label_17139;
}
}
else 
{
goto label_17139;
}
}
}
else 
{
goto label_17139;
}
}
else 
{
label_17139:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_17521;
}
else 
{
if (s__state == 16384)
{
label_17521:; 
goto label_17523;
}
else 
{
if (s__state == 8192)
{
label_17523:; 
goto label_17525;
}
else 
{
if (s__state == 24576)
{
label_17525:; 
goto label_17527;
}
else 
{
if (s__state == 8195)
{
label_17527:; 
s__server = 1;
if (cb != 0)
{
goto label_17532;
}
else 
{
label_17532:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26424 = -1;
goto label_26425;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25639;
}
else 
{
s__init_buf___0 = buf;
goto label_17544;
}
}
else 
{
label_17544:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25639;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25639;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_17564;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_17564:; 
goto label_17566;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_17500;
}
else 
{
if (s__state == 8481)
{
label_17500:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25639;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_17566;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_17566;
}
else 
{
if (s__state == 8464)
{
goto label_17487;
}
else 
{
if (s__state == 8465)
{
label_17487:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_17467;
}
else 
{
if (s__state == 8513)
{
label_17467:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_17566;
}
else 
{
if (s__state == 8528)
{
goto label_17409;
}
else 
{
if (s__state == 8529)
{
label_17409:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_17418;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_17418:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_17458;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_17454:; 
label_17458:; 
s__state = 8544;
s__init_num = 0;
goto label_17566;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_17441;
}
else 
{
tmp___7 = 512;
label_17441:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_17454;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_17367;
}
else 
{
if (s__state == 8545)
{
label_17367:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_17402;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_17381;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_17393:; 
label_17402:; 
goto label_17566;
}
}
else 
{
label_17381:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_17393;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_17352;
}
else 
{
if (s__state == 8561)
{
label_17352:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25639;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_17566;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25639;
}
else 
{
s__rwstate = 1;
goto label_17341;
}
}
else 
{
label_17341:; 
s__state = s__s3__tmp__next_state___0;
goto label_17566;
}
}
else 
{
if (s__state == 8576)
{
goto label_17309;
}
else 
{
if (s__state == 8577)
{
label_17309:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25639;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_17325;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25639;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_17325:; 
goto label_17566;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_17295;
}
else 
{
if (s__state == 8593)
{
label_17295:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25639;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_17566;
}
}
else 
{
if (s__state == 8608)
{
goto label_17282;
}
else 
{
if (s__state == 8609)
{
label_17282:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25639;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_17566:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25639;
}
else 
{
goto label_17596;
}
}
else 
{
label_17596:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_17265;
}
else 
{
if (s__state == 8657)
{
label_17265:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25639;
}
else 
{
if (s__state == 8672)
{
goto label_17246;
}
else 
{
if (s__state == 8673)
{
label_17246:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_17598:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26241;
}
else 
{
label_26241:; 
 __return_26341 = ret;
}
tmp = __return_26341;
goto label_26468;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_17257;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_17257:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_17598;
}
else 
{
goto label_17594;
}
}
else 
{
label_17594:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17624;
}
else 
{
goto label_17624;
}
}
else 
{
goto label_17624;
}
}
}
else 
{
goto label_17624;
}
}
else 
{
label_17624:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_18005;
}
else 
{
if (s__state == 16384)
{
label_18005:; 
goto label_18007;
}
else 
{
if (s__state == 8192)
{
label_18007:; 
goto label_18009;
}
else 
{
if (s__state == 24576)
{
label_18009:; 
goto label_18011;
}
else 
{
if (s__state == 8195)
{
label_18011:; 
s__server = 1;
if (cb != 0)
{
goto label_18016;
}
else 
{
label_18016:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26422 = -1;
goto label_26423;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25641;
}
else 
{
s__init_buf___0 = buf;
goto label_18028;
}
}
else 
{
label_18028:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25641;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25641;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_18048;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_18048:; 
goto label_18050;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_17984;
}
else 
{
if (s__state == 8481)
{
label_17984:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25641;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_18050;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_18050;
}
else 
{
if (s__state == 8464)
{
goto label_17971;
}
else 
{
if (s__state == 8465)
{
label_17971:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_17951;
}
else 
{
if (s__state == 8513)
{
label_17951:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_18050;
}
else 
{
if (s__state == 8528)
{
goto label_17893;
}
else 
{
if (s__state == 8529)
{
label_17893:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_17902;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_17902:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_17942;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_17938:; 
label_17942:; 
s__state = 8544;
s__init_num = 0;
goto label_18050;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_17925;
}
else 
{
tmp___7 = 512;
label_17925:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_17938;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_17851;
}
else 
{
if (s__state == 8545)
{
label_17851:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_17886;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_17865;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_17877:; 
label_17886:; 
goto label_18050;
}
}
else 
{
label_17865:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_17877;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_17836;
}
else 
{
if (s__state == 8561)
{
label_17836:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25641;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_18050;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25641;
}
else 
{
s__rwstate = 1;
goto label_17825;
}
}
else 
{
label_17825:; 
s__state = s__s3__tmp__next_state___0;
goto label_18050;
}
}
else 
{
if (s__state == 8576)
{
goto label_17793;
}
else 
{
if (s__state == 8577)
{
label_17793:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25641;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_17809;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25641;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_17809:; 
goto label_18050;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_17779;
}
else 
{
if (s__state == 8593)
{
label_17779:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25641;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_18050;
}
}
else 
{
if (s__state == 8608)
{
goto label_17766;
}
else 
{
if (s__state == 8609)
{
label_17766:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25641;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_18050:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25641;
}
else 
{
goto label_18080;
}
}
else 
{
label_18080:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18110;
}
else 
{
goto label_18110;
}
}
else 
{
goto label_18110;
}
}
}
else 
{
goto label_18110;
}
}
else 
{
label_18110:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_18490;
}
else 
{
if (s__state == 16384)
{
label_18490:; 
goto label_18492;
}
else 
{
if (s__state == 8192)
{
label_18492:; 
goto label_18494;
}
else 
{
if (s__state == 24576)
{
label_18494:; 
goto label_18496;
}
else 
{
if (s__state == 8195)
{
label_18496:; 
s__server = 1;
if (cb != 0)
{
goto label_18501;
}
else 
{
label_18501:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26420 = -1;
goto label_26421;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25643;
}
else 
{
s__init_buf___0 = buf;
goto label_18513;
}
}
else 
{
label_18513:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25643;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25643;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_18533;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_18533:; 
goto label_18535;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_18469;
}
else 
{
if (s__state == 8481)
{
label_18469:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25643;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_18535;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_18535;
}
else 
{
if (s__state == 8464)
{
goto label_18456;
}
else 
{
if (s__state == 8465)
{
label_18456:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_18436;
}
else 
{
if (s__state == 8513)
{
label_18436:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_18535;
}
else 
{
if (s__state == 8528)
{
goto label_18378;
}
else 
{
if (s__state == 8529)
{
label_18378:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_18387;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_18387:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_18427;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_18423:; 
label_18427:; 
s__state = 8544;
s__init_num = 0;
goto label_18535;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_18410;
}
else 
{
tmp___7 = 512;
label_18410:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_18423;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_18336;
}
else 
{
if (s__state == 8545)
{
label_18336:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_18371;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_18350;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_18362:; 
label_18371:; 
goto label_18535;
}
}
else 
{
label_18350:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_18362;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_18321;
}
else 
{
if (s__state == 8561)
{
label_18321:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25643;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_18535;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25643;
}
else 
{
s__rwstate = 1;
goto label_18310;
}
}
else 
{
label_18310:; 
s__state = s__s3__tmp__next_state___0;
goto label_18535;
}
}
else 
{
if (s__state == 8576)
{
goto label_18278;
}
else 
{
if (s__state == 8577)
{
label_18278:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25643;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_18294;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25643;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_18294:; 
goto label_18535;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_18264;
}
else 
{
if (s__state == 8593)
{
label_18264:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25643;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_18535;
}
}
else 
{
if (s__state == 8608)
{
goto label_18251;
}
else 
{
if (s__state == 8609)
{
label_18251:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25643;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_18535:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25643;
}
else 
{
goto label_18565;
}
}
else 
{
label_18565:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_18233;
}
else 
{
if (s__state == 8641)
{
label_18233:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_18567:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26237;
}
else 
{
label_26237:; 
 __return_26343 = ret;
}
tmp = __return_26343;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_18243;
}
else 
{
s__state = 3;
label_18243:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_18567;
}
else 
{
goto label_18563;
}
}
else 
{
label_18563:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_18221;
}
else 
{
if (s__state == 8657)
{
label_18221:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25643;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_18208;
}
else 
{
goto label_18208;
}
}
else 
{
label_18208:; 
ret = 1;
goto label_25643;
}
}
else 
{
ret = -1;
label_25643:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26145;
}
else 
{
label_26145:; 
 __return_26421 = ret;
label_26421:; 
}
tmp = __return_26421;
goto label_26468;
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
else 
{
if (s__state == 8640)
{
goto label_17748;
}
else 
{
if (s__state == 8641)
{
label_17748:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_18082:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26239;
}
else 
{
label_26239:; 
 __return_26342 = ret;
}
tmp = __return_26342;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_17758;
}
else 
{
s__state = 3;
label_17758:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_18082;
}
else 
{
goto label_18078;
}
}
else 
{
label_18078:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_17736;
}
else 
{
if (s__state == 8657)
{
label_17736:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25641;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_17723;
}
else 
{
goto label_17723;
}
}
else 
{
label_17723:; 
ret = 1;
goto label_25641;
}
}
else 
{
ret = -1;
label_25641:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26147;
}
else 
{
label_26147:; 
 __return_26423 = ret;
label_26423:; 
}
tmp = __return_26423;
goto label_26468;
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
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_17238;
}
else 
{
goto label_17238;
}
}
else 
{
label_17238:; 
ret = 1;
goto label_25639;
}
}
else 
{
ret = -1;
label_25639:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26149;
}
else 
{
label_26149:; 
 __return_26425 = ret;
label_26425:; 
}
tmp = __return_26425;
goto label_26466;
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
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_16754;
}
else 
{
goto label_16754;
}
}
else 
{
label_16754:; 
ret = 1;
goto label_25637;
}
}
else 
{
ret = -1;
label_25637:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26151;
}
else 
{
label_26151:; 
 __return_26427 = ret;
label_26427:; 
}
tmp = __return_26427;
goto label_26472;
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
else 
{
if (s__state == 8656)
{
goto label_16182;
}
else 
{
if (s__state == 8657)
{
label_16182:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25635;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_16596;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_16596:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26249;
}
else 
{
label_26249:; 
 __return_26337 = ret;
}
tmp = __return_26337;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_16596;
}
else 
{
goto label_16588;
}
}
else 
{
label_16588:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_16169;
}
else 
{
goto label_16169;
}
}
else 
{
label_16169:; 
ret = 1;
goto label_25635;
}
}
else 
{
ret = -1;
label_25635:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26153;
}
else 
{
label_26153:; 
 __return_26429 = ret;
label_26429:; 
}
tmp = __return_26429;
goto label_26476;
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
else 
{
if (s__state == 8640)
{
goto label_13465;
}
else 
{
if (s__state == 8641)
{
label_13465:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_13856:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26253;
}
else 
{
label_26253:; 
 __return_26335 = ret;
}
tmp = __return_26335;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_13475;
}
else 
{
s__state = 3;
label_13475:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_13856;
}
else 
{
goto label_13848;
}
}
else 
{
label_13848:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_13440;
}
else 
{
if (s__state == 8657)
{
label_13440:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25633;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_13854;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_13854:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26255;
}
else 
{
label_26255:; 
 __return_26334 = ret;
}
tmp = __return_26334;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_13854;
}
else 
{
goto label_13846;
}
}
else 
{
label_13846:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_13427;
}
else 
{
goto label_13427;
}
}
else 
{
label_13427:; 
ret = 1;
goto label_25633;
}
}
else 
{
ret = -1;
label_25633:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26155;
}
else 
{
label_26155:; 
 __return_26431 = ret;
label_26431:; 
}
tmp = __return_26431;
goto label_26476;
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
else 
{
if (s__state == 8640)
{
goto label_12877;
}
else 
{
if (s__state == 8641)
{
label_12877:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_13268:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26259;
}
else 
{
label_26259:; 
 __return_26332 = ret;
}
tmp = __return_26332;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_12887;
}
else 
{
s__state = 3;
label_12887:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_13268;
}
else 
{
goto label_13260;
}
}
else 
{
label_13260:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_12852;
}
else 
{
if (s__state == 8657)
{
label_12852:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25631;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_13266;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_13266:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26261;
}
else 
{
label_26261:; 
 __return_26331 = ret;
}
tmp = __return_26331;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_13266;
}
else 
{
goto label_13258;
}
}
else 
{
label_13258:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_12839;
}
else 
{
goto label_12839;
}
}
else 
{
label_12839:; 
ret = 1;
goto label_25631;
}
}
else 
{
ret = -1;
label_25631:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26157;
}
else 
{
label_26157:; 
 __return_26433 = ret;
label_26433:; 
}
tmp = __return_26433;
goto label_26476;
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
else 
{
if (s__state == 8640)
{
goto label_12290;
}
else 
{
if (s__state == 8641)
{
label_12290:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_12681:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26265;
}
else 
{
label_26265:; 
 __return_26329 = ret;
}
tmp = __return_26329;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_12300;
}
else 
{
s__state = 3;
label_12300:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12681;
}
else 
{
goto label_12673;
}
}
else 
{
label_12673:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_12265;
}
else 
{
if (s__state == 8657)
{
label_12265:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25629;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_12679;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_12679:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26267;
}
else 
{
label_26267:; 
 __return_26328 = ret;
}
tmp = __return_26328;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12679;
}
else 
{
goto label_12671;
}
}
else 
{
label_12671:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_12252;
}
else 
{
goto label_12252;
}
}
else 
{
label_12252:; 
ret = 1;
goto label_25629;
}
}
else 
{
ret = -1;
label_25629:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26159;
}
else 
{
label_26159:; 
 __return_26435 = ret;
label_26435:; 
}
tmp = __return_26435;
goto label_26476;
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
else 
{
if (s__state == 8640)
{
goto label_11703;
}
else 
{
if (s__state == 8641)
{
label_11703:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_12094:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26271;
}
else 
{
label_26271:; 
 __return_26326 = ret;
}
tmp = __return_26326;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_11713;
}
else 
{
s__state = 3;
label_11713:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12094;
}
else 
{
goto label_12086;
}
}
else 
{
label_12086:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_11678;
}
else 
{
if (s__state == 8657)
{
label_11678:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25627;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_12092;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_12092:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26273;
}
else 
{
label_26273:; 
 __return_26325 = ret;
}
tmp = __return_26325;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12092;
}
else 
{
goto label_12084;
}
}
else 
{
label_12084:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_11665;
}
else 
{
goto label_11665;
}
}
else 
{
label_11665:; 
ret = 1;
goto label_25627;
}
}
else 
{
ret = -1;
label_25627:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26161;
}
else 
{
label_26161:; 
 __return_26437 = ret;
label_26437:; 
}
tmp = __return_26437;
goto label_26476;
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
else 
{
if (s__state == 8640)
{
goto label_11116;
}
else 
{
if (s__state == 8641)
{
label_11116:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_11507:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26277;
}
else 
{
label_26277:; 
 __return_26323 = ret;
}
tmp = __return_26323;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_11126;
}
else 
{
s__state = 3;
label_11126:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_11507;
}
else 
{
goto label_11499;
}
}
else 
{
label_11499:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_11091;
}
else 
{
if (s__state == 8657)
{
label_11091:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25625;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_11505;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_11505:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26279;
}
else 
{
label_26279:; 
 __return_26322 = ret;
}
tmp = __return_26322;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_11505;
}
else 
{
goto label_11497;
}
}
else 
{
label_11497:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_11078;
}
else 
{
goto label_11078;
}
}
else 
{
label_11078:; 
ret = 1;
goto label_25625;
}
}
else 
{
ret = -1;
label_25625:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26163;
}
else 
{
label_26163:; 
 __return_26439 = ret;
label_26439:; 
}
tmp = __return_26439;
label_26476:; 
 __return_26644 = tmp;
return 1;
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
}
else 
{
if (s__state == 8544)
{
goto label_10592;
}
else 
{
if (s__state == 8545)
{
label_10592:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_10637;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_10606;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_10629:; 
label_10637:; 
goto label_10826;
}
}
else 
{
label_10606:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_10620;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_10629;
}
else 
{
label_10620:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25623;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_10629;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_10577;
}
else 
{
if (s__state == 8561)
{
label_10577:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25623;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_10826;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25623;
}
else 
{
s__rwstate = 1;
goto label_10566;
}
}
else 
{
label_10566:; 
s__state = s__s3__tmp__next_state___0;
goto label_10826;
}
}
else 
{
if (s__state == 8576)
{
goto label_10534;
}
else 
{
if (s__state == 8577)
{
label_10534:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25623;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_10550;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25623;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_10550:; 
goto label_10826;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_10520;
}
else 
{
if (s__state == 8593)
{
label_10520:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25623;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_10826;
}
}
else 
{
if (s__state == 8608)
{
goto label_10507;
}
else 
{
if (s__state == 8609)
{
label_10507:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25623;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_10826:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25623;
}
else 
{
goto label_10901;
}
}
else 
{
label_10901:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_10976;
}
else 
{
goto label_10976;
}
}
else 
{
goto label_10976;
}
}
}
else 
{
goto label_10976;
}
}
else 
{
label_10976:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_19030;
}
else 
{
if (s__state == 16384)
{
label_19030:; 
goto label_19032;
}
else 
{
if (s__state == 8192)
{
label_19032:; 
goto label_19034;
}
else 
{
if (s__state == 24576)
{
label_19034:; 
goto label_19036;
}
else 
{
if (s__state == 8195)
{
label_19036:; 
s__server = 1;
if (cb != 0)
{
goto label_19041;
}
else 
{
label_19041:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26418 = -1;
goto label_26419;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25645;
}
else 
{
s__init_buf___0 = buf;
goto label_19053;
}
}
else 
{
label_19053:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25645;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25645;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_19073;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_19073:; 
goto label_19075;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_19009;
}
else 
{
if (s__state == 8481)
{
label_19009:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25645;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_19075;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_19075;
}
else 
{
if (s__state == 8464)
{
goto label_18988;
}
else 
{
if (s__state == 8465)
{
label_18988:; 
goto label_18990;
}
else 
{
if (s__state == 8466)
{
label_18990:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_19158:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26229;
}
else 
{
label_26229:; 
 __return_26347 = ret;
}
tmp = __return_26347;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19158;
}
else 
{
goto label_19148;
}
}
else 
{
label_19148:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_18961;
}
else 
{
if (s__state == 8513)
{
label_18961:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25645;
}
else 
{
goto label_18971;
}
}
else 
{
skip = 1;
label_18971:; 
s__state = 8528;
s__init_num = 0;
goto label_19075;
}
}
else 
{
if (s__state == 8528)
{
goto label_18893;
}
else 
{
if (s__state == 8529)
{
label_18893:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_18902;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_18902:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_18949;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_18936;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_18943:; 
label_18949:; 
s__state = 8544;
s__init_num = 0;
goto label_19075;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_18925;
}
else 
{
tmp___7 = 512;
label_18925:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_18936;
}
else 
{
skip = 1;
goto label_18943;
}
}
}
}
}
}
else 
{
goto label_18936;
}
}
else 
{
label_18936:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_19156:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26231;
}
else 
{
label_26231:; 
 __return_26346 = ret;
}
tmp = __return_26346;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19156;
}
else 
{
goto label_19146;
}
}
else 
{
label_19146:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_18841;
}
else 
{
if (s__state == 8545)
{
label_18841:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_18886;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_18855;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_18878:; 
label_18886:; 
goto label_19075;
}
}
else 
{
label_18855:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_18869;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_18878;
}
else 
{
label_18869:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25645;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_18878;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_18826;
}
else 
{
if (s__state == 8561)
{
label_18826:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25645;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_19075;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25645;
}
else 
{
s__rwstate = 1;
goto label_18815;
}
}
else 
{
label_18815:; 
s__state = s__s3__tmp__next_state___0;
goto label_19075;
}
}
else 
{
if (s__state == 8576)
{
goto label_18783;
}
else 
{
if (s__state == 8577)
{
label_18783:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25645;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_18799;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25645;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_18799:; 
goto label_19075;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_18769;
}
else 
{
if (s__state == 8593)
{
label_18769:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25645;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_19075;
}
}
else 
{
if (s__state == 8608)
{
goto label_18756;
}
else 
{
if (s__state == 8609)
{
label_18756:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25645;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_19075:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25645;
}
else 
{
goto label_19150;
}
}
else 
{
label_19150:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19225;
}
else 
{
goto label_19225;
}
}
else 
{
goto label_19225;
}
}
}
else 
{
goto label_19225;
}
}
else 
{
label_19225:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_19659;
}
else 
{
if (s__state == 16384)
{
label_19659:; 
goto label_19661;
}
else 
{
if (s__state == 8192)
{
label_19661:; 
goto label_19663;
}
else 
{
if (s__state == 24576)
{
label_19663:; 
goto label_19665;
}
else 
{
if (s__state == 8195)
{
label_19665:; 
s__server = 1;
if (cb != 0)
{
goto label_19670;
}
else 
{
label_19670:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26416 = -1;
goto label_26417;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25647;
}
else 
{
s__init_buf___0 = buf;
goto label_19682;
}
}
else 
{
label_19682:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25647;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25647;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_19702;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_19702:; 
goto label_19704;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_19638;
}
else 
{
if (s__state == 8481)
{
label_19638:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25647;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_19704;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_19704;
}
else 
{
if (s__state == 8464)
{
goto label_19617;
}
else 
{
if (s__state == 8465)
{
label_19617:; 
goto label_19619;
}
else 
{
if (s__state == 8466)
{
label_19619:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_19787:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26221;
}
else 
{
label_26221:; 
 __return_26351 = ret;
}
tmp = __return_26351;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19787;
}
else 
{
goto label_19777;
}
}
else 
{
label_19777:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_19590;
}
else 
{
if (s__state == 8513)
{
label_19590:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25647;
}
else 
{
goto label_19600;
}
}
else 
{
skip = 1;
label_19600:; 
s__state = 8528;
s__init_num = 0;
goto label_19704;
}
}
else 
{
if (s__state == 8528)
{
goto label_19522;
}
else 
{
if (s__state == 8529)
{
label_19522:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_19531;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_19531:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_19578;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_19565;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_19572:; 
label_19578:; 
s__state = 8544;
s__init_num = 0;
goto label_19704;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_19554;
}
else 
{
tmp___7 = 512;
label_19554:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_19565;
}
else 
{
skip = 1;
goto label_19572;
}
}
}
}
}
}
else 
{
goto label_19565;
}
}
else 
{
label_19565:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_19785:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26223;
}
else 
{
label_26223:; 
 __return_26350 = ret;
}
tmp = __return_26350;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19785;
}
else 
{
goto label_19775;
}
}
else 
{
label_19775:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_19470;
}
else 
{
if (s__state == 8545)
{
label_19470:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_19515;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_19484;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_19507:; 
label_19515:; 
goto label_19704;
}
}
else 
{
label_19484:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_19498;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_19507;
}
else 
{
label_19498:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25647;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_19507;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_19455;
}
else 
{
if (s__state == 8561)
{
label_19455:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25647;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_19704;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25647;
}
else 
{
s__rwstate = 1;
goto label_19444;
}
}
else 
{
label_19444:; 
s__state = s__s3__tmp__next_state___0;
goto label_19704;
}
}
else 
{
if (s__state == 8576)
{
goto label_19412;
}
else 
{
if (s__state == 8577)
{
label_19412:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25647;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_19428;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25647;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_19428:; 
goto label_19704;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_19398;
}
else 
{
if (s__state == 8593)
{
label_19398:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25647;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_19704;
}
}
else 
{
if (s__state == 8608)
{
goto label_19385;
}
else 
{
if (s__state == 8609)
{
label_19385:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25647;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_19704:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25647;
}
else 
{
goto label_19779;
}
}
else 
{
label_19779:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19854;
}
else 
{
goto label_19854;
}
}
else 
{
goto label_19854;
}
}
}
else 
{
goto label_19854;
}
}
else 
{
label_19854:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_20288;
}
else 
{
if (s__state == 16384)
{
label_20288:; 
goto label_20290;
}
else 
{
if (s__state == 8192)
{
label_20290:; 
goto label_20292;
}
else 
{
if (s__state == 24576)
{
label_20292:; 
goto label_20294;
}
else 
{
if (s__state == 8195)
{
label_20294:; 
s__server = 1;
if (cb != 0)
{
goto label_20299;
}
else 
{
label_20299:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26414 = -1;
goto label_26415;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25649;
}
else 
{
s__init_buf___0 = buf;
goto label_20311;
}
}
else 
{
label_20311:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25649;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25649;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_20331;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_20331:; 
goto label_20333;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_20267;
}
else 
{
if (s__state == 8481)
{
label_20267:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25649;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_20333;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_20333;
}
else 
{
if (s__state == 8464)
{
goto label_20246;
}
else 
{
if (s__state == 8465)
{
label_20246:; 
goto label_20248;
}
else 
{
if (s__state == 8466)
{
label_20248:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_20416:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26213;
}
else 
{
label_26213:; 
 __return_26355 = ret;
}
tmp = __return_26355;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_20416;
}
else 
{
goto label_20406;
}
}
else 
{
label_20406:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_20219;
}
else 
{
if (s__state == 8513)
{
label_20219:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25649;
}
else 
{
goto label_20229;
}
}
else 
{
skip = 1;
label_20229:; 
s__state = 8528;
s__init_num = 0;
goto label_20333;
}
}
else 
{
if (s__state == 8528)
{
goto label_20151;
}
else 
{
if (s__state == 8529)
{
label_20151:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_20160;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_20160:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_20207;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_20194;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_20201:; 
label_20207:; 
s__state = 8544;
s__init_num = 0;
goto label_20333;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_20183;
}
else 
{
tmp___7 = 512;
label_20183:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_20194;
}
else 
{
skip = 1;
goto label_20201;
}
}
}
}
}
}
else 
{
goto label_20194;
}
}
else 
{
label_20194:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_20414:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26215;
}
else 
{
label_26215:; 
 __return_26354 = ret;
}
tmp = __return_26354;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_20414;
}
else 
{
goto label_20404;
}
}
else 
{
label_20404:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_20099;
}
else 
{
if (s__state == 8545)
{
label_20099:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_20144;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_20113;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_20136:; 
label_20144:; 
goto label_20333;
}
}
else 
{
label_20113:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_20127;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_20136;
}
else 
{
label_20127:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25649;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_20136;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_20084;
}
else 
{
if (s__state == 8561)
{
label_20084:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25649;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_20333;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25649;
}
else 
{
s__rwstate = 1;
goto label_20073;
}
}
else 
{
label_20073:; 
s__state = s__s3__tmp__next_state___0;
goto label_20333;
}
}
else 
{
if (s__state == 8576)
{
goto label_20041;
}
else 
{
if (s__state == 8577)
{
label_20041:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25649;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_20057;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25649;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_20057:; 
goto label_20333;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_20027;
}
else 
{
if (s__state == 8593)
{
label_20027:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25649;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_20333;
}
}
else 
{
if (s__state == 8608)
{
goto label_20014;
}
else 
{
if (s__state == 8609)
{
label_20014:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25649;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_20333:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25649;
}
else 
{
goto label_20408;
}
}
else 
{
label_20408:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_20483;
}
else 
{
goto label_20483;
}
}
else 
{
goto label_20483;
}
}
}
else 
{
goto label_20483;
}
}
else 
{
label_20483:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_20917;
}
else 
{
if (s__state == 16384)
{
label_20917:; 
goto label_20919;
}
else 
{
if (s__state == 8192)
{
label_20919:; 
goto label_20921;
}
else 
{
if (s__state == 24576)
{
label_20921:; 
goto label_20923;
}
else 
{
if (s__state == 8195)
{
label_20923:; 
s__server = 1;
if (cb != 0)
{
goto label_20928;
}
else 
{
label_20928:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26412 = -1;
goto label_26413;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25651;
}
else 
{
s__init_buf___0 = buf;
goto label_20940;
}
}
else 
{
label_20940:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25651;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25651;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_20960;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_20960:; 
goto label_20962;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_20896;
}
else 
{
if (s__state == 8481)
{
label_20896:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25651;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_20962;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_20962;
}
else 
{
if (s__state == 8464)
{
goto label_20875;
}
else 
{
if (s__state == 8465)
{
label_20875:; 
goto label_20877;
}
else 
{
if (s__state == 8466)
{
label_20877:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_21045:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26205;
}
else 
{
label_26205:; 
 __return_26359 = ret;
}
tmp = __return_26359;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21045;
}
else 
{
goto label_21035;
}
}
else 
{
label_21035:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21110;
}
else 
{
goto label_21110;
}
}
else 
{
goto label_21110;
}
}
}
else 
{
goto label_21110;
}
}
else 
{
label_21110:; 
skip = 0;
goto label_5397;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_20848;
}
else 
{
if (s__state == 8513)
{
label_20848:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25651;
}
else 
{
goto label_20858;
}
}
else 
{
skip = 1;
label_20858:; 
s__state = 8528;
s__init_num = 0;
goto label_20962;
}
}
else 
{
if (s__state == 8528)
{
goto label_20780;
}
else 
{
if (s__state == 8529)
{
label_20780:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_20789;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_20789:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_20836;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_20823;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_20830:; 
label_20836:; 
s__state = 8544;
s__init_num = 0;
goto label_20962;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_20812;
}
else 
{
tmp___7 = 512;
label_20812:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_20823;
}
else 
{
skip = 1;
goto label_20830;
}
}
}
}
}
}
else 
{
goto label_20823;
}
}
else 
{
label_20823:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_21043:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26207;
}
else 
{
label_26207:; 
 __return_26358 = ret;
}
tmp = __return_26358;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21043;
}
else 
{
goto label_21033;
}
}
else 
{
label_21033:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_20728;
}
else 
{
if (s__state == 8545)
{
label_20728:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_20773;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_20742;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_20765:; 
label_20773:; 
goto label_20962;
}
}
else 
{
label_20742:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_20756;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_20765;
}
else 
{
label_20756:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25651;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_20765;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_20713;
}
else 
{
if (s__state == 8561)
{
label_20713:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25651;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_20962;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25651;
}
else 
{
s__rwstate = 1;
goto label_20702;
}
}
else 
{
label_20702:; 
s__state = s__s3__tmp__next_state___0;
goto label_20962;
}
}
else 
{
if (s__state == 8576)
{
goto label_20670;
}
else 
{
if (s__state == 8577)
{
label_20670:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25651;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_20686;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25651;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_20686:; 
goto label_20962;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_20656;
}
else 
{
if (s__state == 8593)
{
label_20656:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25651;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_20962;
}
}
else 
{
if (s__state == 8608)
{
goto label_20643;
}
else 
{
if (s__state == 8609)
{
label_20643:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25651;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_20962:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25651;
}
else 
{
goto label_21037;
}
}
else 
{
label_21037:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21112;
}
else 
{
goto label_21112;
}
}
else 
{
goto label_21112;
}
}
}
else 
{
goto label_21112;
}
}
else 
{
label_21112:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_21547;
}
else 
{
if (s__state == 16384)
{
label_21547:; 
goto label_21549;
}
else 
{
if (s__state == 8192)
{
label_21549:; 
goto label_21551;
}
else 
{
if (s__state == 24576)
{
label_21551:; 
goto label_21553;
}
else 
{
if (s__state == 8195)
{
label_21553:; 
s__server = 1;
if (cb != 0)
{
goto label_21558;
}
else 
{
label_21558:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26410 = -1;
goto label_26411;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25653;
}
else 
{
s__init_buf___0 = buf;
goto label_21570;
}
}
else 
{
label_21570:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25653;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25653;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_21590;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_21590:; 
goto label_21592;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_21526;
}
else 
{
if (s__state == 8481)
{
label_21526:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25653;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_21592;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_21592;
}
else 
{
if (s__state == 8464)
{
goto label_21505;
}
else 
{
if (s__state == 8465)
{
label_21505:; 
goto label_21507;
}
else 
{
if (s__state == 8466)
{
label_21507:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_21675:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26197;
}
else 
{
label_26197:; 
 __return_26363 = ret;
}
tmp = __return_26363;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21675;
}
else 
{
goto label_21665;
}
}
else 
{
label_21665:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21740;
}
else 
{
goto label_21740;
}
}
else 
{
goto label_21740;
}
}
}
else 
{
goto label_21740;
}
}
else 
{
label_21740:; 
skip = 0;
goto label_5397;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_21478;
}
else 
{
if (s__state == 8513)
{
label_21478:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25653;
}
else 
{
goto label_21488;
}
}
else 
{
skip = 1;
label_21488:; 
s__state = 8528;
s__init_num = 0;
goto label_21592;
}
}
else 
{
if (s__state == 8528)
{
goto label_21410;
}
else 
{
if (s__state == 8529)
{
label_21410:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_21419;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_21419:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_21466;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_21453;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_21460:; 
label_21466:; 
s__state = 8544;
s__init_num = 0;
goto label_21592;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_21442;
}
else 
{
tmp___7 = 512;
label_21442:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_21453;
}
else 
{
skip = 1;
goto label_21460;
}
}
}
}
}
}
else 
{
goto label_21453;
}
}
else 
{
label_21453:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_21673:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26199;
}
else 
{
label_26199:; 
 __return_26362 = ret;
}
tmp = __return_26362;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21673;
}
else 
{
goto label_21663;
}
}
else 
{
label_21663:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_21358;
}
else 
{
if (s__state == 8545)
{
label_21358:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_21403;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_21372;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_21395:; 
label_21403:; 
goto label_21592;
}
}
else 
{
label_21372:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_21386;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_21395;
}
else 
{
label_21386:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25653;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_21395;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_21343;
}
else 
{
if (s__state == 8561)
{
label_21343:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25653;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_21592;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25653;
}
else 
{
s__rwstate = 1;
goto label_21332;
}
}
else 
{
label_21332:; 
s__state = s__s3__tmp__next_state___0;
goto label_21592;
}
}
else 
{
if (s__state == 8576)
{
goto label_21300;
}
else 
{
if (s__state == 8577)
{
label_21300:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25653;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_21316;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25653;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_21316:; 
goto label_21592;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_21286;
}
else 
{
if (s__state == 8593)
{
label_21286:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25653;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_21592;
}
}
else 
{
if (s__state == 8608)
{
goto label_21273;
}
else 
{
if (s__state == 8609)
{
label_21273:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25653;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_21592:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25653;
}
else 
{
goto label_21667;
}
}
else 
{
label_21667:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21742;
}
else 
{
goto label_21742;
}
}
else 
{
goto label_21742;
}
}
}
else 
{
goto label_21742;
}
}
else 
{
label_21742:; 
skip = 0;
label_21754:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_22177;
}
else 
{
if (s__state == 16384)
{
label_22177:; 
goto label_22179;
}
else 
{
if (s__state == 8192)
{
label_22179:; 
goto label_22181;
}
else 
{
if (s__state == 24576)
{
label_22181:; 
goto label_22183;
}
else 
{
if (s__state == 8195)
{
label_22183:; 
s__server = 1;
if (cb != 0)
{
goto label_22188;
}
else 
{
label_22188:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26408 = -1;
goto label_26409;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25655;
}
else 
{
s__init_buf___0 = buf;
goto label_22200;
}
}
else 
{
label_22200:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25655;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25655;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_22220;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_22220:; 
goto label_22222;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_22156;
}
else 
{
if (s__state == 8481)
{
label_22156:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25655;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_22222;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_22222;
}
else 
{
if (s__state == 8464)
{
goto label_22135;
}
else 
{
if (s__state == 8465)
{
label_22135:; 
goto label_22137;
}
else 
{
if (s__state == 8466)
{
label_22137:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_22305:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26189;
}
else 
{
label_26189:; 
 __return_26367 = ret;
}
tmp = __return_26367;
goto label_26462;
}
else 
{
got_new_session = 1;
s__state = 8496;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22305;
}
else 
{
goto label_22295;
}
}
else 
{
label_22295:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_22108;
}
else 
{
if (s__state == 8513)
{
label_22108:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_25655;
}
else 
{
goto label_22118;
}
}
else 
{
skip = 1;
label_22118:; 
s__state = 8528;
s__init_num = 0;
goto label_22222;
}
}
else 
{
if (s__state == 8528)
{
goto label_22040;
}
else 
{
if (s__state == 8529)
{
label_22040:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_22049;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_22049:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_22096;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_22083;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_22090:; 
label_22096:; 
s__state = 8544;
s__init_num = 0;
goto label_22222;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_22072;
}
else 
{
tmp___7 = 512;
label_22072:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_22083;
}
else 
{
skip = 1;
goto label_22090;
}
}
}
}
}
}
else 
{
goto label_22083;
}
}
else 
{
label_22083:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_22303:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26191;
}
else 
{
label_26191:; 
 __return_26366 = ret;
}
tmp = __return_26366;
goto label_26476;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22303;
}
else 
{
goto label_22293;
}
}
else 
{
label_22293:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8544)
{
goto label_21988;
}
else 
{
if (s__state == 8545)
{
label_21988:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22033;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_22002;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_22025:; 
label_22033:; 
goto label_22222;
}
}
else 
{
label_22002:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_22016;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22025;
}
else 
{
label_22016:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_25655;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_22025;
}
}
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_21973;
}
else 
{
if (s__state == 8561)
{
label_21973:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25655;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_22222;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25655;
}
else 
{
s__rwstate = 1;
goto label_21962;
}
}
else 
{
label_21962:; 
s__state = s__s3__tmp__next_state___0;
goto label_22222;
}
}
else 
{
if (s__state == 8576)
{
goto label_21930;
}
else 
{
if (s__state == 8577)
{
label_21930:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25655;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_21946;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25655;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_21946:; 
goto label_22222;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_21916;
}
else 
{
if (s__state == 8593)
{
label_21916:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25655;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_22222;
}
}
else 
{
if (s__state == 8608)
{
goto label_21903;
}
else 
{
if (s__state == 8609)
{
label_21903:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25655;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_22222:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25655;
}
else 
{
goto label_22297;
}
}
else 
{
label_22297:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22372;
}
else 
{
goto label_22372;
}
}
else 
{
goto label_22372;
}
}
}
else 
{
goto label_22372;
}
}
else 
{
label_22372:; 
skip = 0;
goto label_21754;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_21885;
}
else 
{
if (s__state == 8641)
{
label_21885:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_22301:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26193;
}
else 
{
label_26193:; 
 __return_26365 = ret;
}
tmp = __return_26365;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_21895;
}
else 
{
s__state = 3;
label_21895:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22301;
}
else 
{
goto label_22291;
}
}
else 
{
label_22291:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22366;
}
else 
{
goto label_22366;
}
}
else 
{
goto label_22366;
}
}
}
else 
{
goto label_22366;
}
}
else 
{
label_22366:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_22755;
}
else 
{
if (s__state == 16384)
{
label_22755:; 
goto label_22757;
}
else 
{
if (s__state == 8192)
{
label_22757:; 
goto label_22759;
}
else 
{
if (s__state == 24576)
{
label_22759:; 
goto label_22761;
}
else 
{
if (s__state == 8195)
{
label_22761:; 
s__server = 1;
if (cb != 0)
{
goto label_22766;
}
else 
{
label_22766:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26406 = -1;
goto label_26407;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25657;
}
else 
{
s__init_buf___0 = buf;
goto label_22778;
}
}
else 
{
label_22778:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25657;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25657;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_22798;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_22798:; 
goto label_22800;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_22734;
}
else 
{
if (s__state == 8481)
{
label_22734:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25657;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_22800;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_22800;
}
else 
{
if (s__state == 8464)
{
goto label_22721;
}
else 
{
if (s__state == 8465)
{
label_22721:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_22701;
}
else 
{
if (s__state == 8513)
{
label_22701:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_22800;
}
else 
{
if (s__state == 8528)
{
goto label_22643;
}
else 
{
if (s__state == 8529)
{
label_22643:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_22652;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_22652:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_22692;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_22688:; 
label_22692:; 
s__state = 8544;
s__init_num = 0;
goto label_22800;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_22675;
}
else 
{
tmp___7 = 512;
label_22675:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_22688;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_22601;
}
else 
{
if (s__state == 8545)
{
label_22601:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22636;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_22615;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_22627:; 
label_22636:; 
goto label_22800;
}
}
else 
{
label_22615:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22627;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_22586;
}
else 
{
if (s__state == 8561)
{
label_22586:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25657;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_22800;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25657;
}
else 
{
s__rwstate = 1;
goto label_22575;
}
}
else 
{
label_22575:; 
s__state = s__s3__tmp__next_state___0;
goto label_22800;
}
}
else 
{
if (s__state == 8576)
{
goto label_22543;
}
else 
{
if (s__state == 8577)
{
label_22543:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25657;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_22559;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25657;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_22559:; 
goto label_22800;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_22529;
}
else 
{
if (s__state == 8593)
{
label_22529:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25657;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_22800;
}
}
else 
{
if (s__state == 8608)
{
goto label_22516;
}
else 
{
if (s__state == 8609)
{
label_22516:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25657;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_22800:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25657;
}
else 
{
goto label_22830;
}
}
else 
{
label_22830:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_22486;
}
else 
{
if (s__state == 8657)
{
label_22486:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25657;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_22832;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_22832:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26187;
}
else 
{
label_26187:; 
 __return_26368 = ret;
}
tmp = __return_26368;
goto label_26466;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22832;
}
else 
{
goto label_22828;
}
}
else 
{
label_22828:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22858;
}
else 
{
goto label_22858;
}
}
else 
{
goto label_22858;
}
}
}
else 
{
goto label_22858;
}
}
else 
{
label_22858:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_24325;
}
else 
{
if (s__state == 16384)
{
label_24325:; 
goto label_24327;
}
else 
{
if (s__state == 8192)
{
label_24327:; 
goto label_24329;
}
else 
{
if (s__state == 24576)
{
label_24329:; 
goto label_24331;
}
else 
{
if (s__state == 8195)
{
label_24331:; 
s__server = 1;
if (cb != 0)
{
goto label_24336;
}
else 
{
label_24336:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26404 = -1;
goto label_26405;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25659;
}
else 
{
s__init_buf___0 = buf;
goto label_24348;
}
}
else 
{
label_24348:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25659;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25659;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_24368;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_24368:; 
goto label_24370;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_24304;
}
else 
{
if (s__state == 8481)
{
label_24304:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25659;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_24370;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_24370;
}
else 
{
if (s__state == 8464)
{
goto label_24291;
}
else 
{
if (s__state == 8465)
{
label_24291:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_24271;
}
else 
{
if (s__state == 8513)
{
label_24271:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_24370;
}
else 
{
if (s__state == 8528)
{
goto label_24213;
}
else 
{
if (s__state == 8529)
{
label_24213:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_24222;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_24222:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_24262;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_24258:; 
label_24262:; 
s__state = 8544;
s__init_num = 0;
goto label_24370;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_24245;
}
else 
{
tmp___7 = 512;
label_24245:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_24258;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_24171;
}
else 
{
if (s__state == 8545)
{
label_24171:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24206;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_24185;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_24197:; 
label_24206:; 
goto label_24370;
}
}
else 
{
label_24185:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24197;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_24156;
}
else 
{
if (s__state == 8561)
{
label_24156:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25659;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_24370;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25659;
}
else 
{
s__rwstate = 1;
goto label_24145;
}
}
else 
{
label_24145:; 
s__state = s__s3__tmp__next_state___0;
goto label_24370;
}
}
else 
{
if (s__state == 8576)
{
goto label_24113;
}
else 
{
if (s__state == 8577)
{
label_24113:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25659;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_24129;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25659;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_24129:; 
goto label_24370;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24099;
}
else 
{
if (s__state == 8593)
{
label_24099:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25659;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_24370;
}
}
else 
{
if (s__state == 8608)
{
goto label_24086;
}
else 
{
if (s__state == 8609)
{
label_24086:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25659;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_24370:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25659;
}
else 
{
goto label_24400;
}
}
else 
{
label_24400:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_24069;
}
else 
{
if (s__state == 8657)
{
label_24069:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25659;
}
else 
{
if (s__state == 8672)
{
goto label_24050;
}
else 
{
if (s__state == 8673)
{
label_24050:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_24402:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26185;
}
else 
{
label_26185:; 
 __return_26369 = ret;
}
tmp = __return_26369;
goto label_26468;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_24061;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_24061:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_24402;
}
else 
{
goto label_24398;
}
}
else 
{
label_24398:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24428;
}
else 
{
goto label_24428;
}
}
else 
{
goto label_24428;
}
}
}
else 
{
goto label_24428;
}
}
else 
{
label_24428:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_24809;
}
else 
{
if (s__state == 16384)
{
label_24809:; 
goto label_24811;
}
else 
{
if (s__state == 8192)
{
label_24811:; 
goto label_24813;
}
else 
{
if (s__state == 24576)
{
label_24813:; 
goto label_24815;
}
else 
{
if (s__state == 8195)
{
label_24815:; 
s__server = 1;
if (cb != 0)
{
goto label_24820;
}
else 
{
label_24820:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26402 = -1;
goto label_26403;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25661;
}
else 
{
s__init_buf___0 = buf;
goto label_24832;
}
}
else 
{
label_24832:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25661;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25661;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_24852;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_24852:; 
goto label_24854;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_24788;
}
else 
{
if (s__state == 8481)
{
label_24788:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25661;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_24854;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_24854;
}
else 
{
if (s__state == 8464)
{
goto label_24775;
}
else 
{
if (s__state == 8465)
{
label_24775:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_24755;
}
else 
{
if (s__state == 8513)
{
label_24755:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_24854;
}
else 
{
if (s__state == 8528)
{
goto label_24697;
}
else 
{
if (s__state == 8529)
{
label_24697:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_24706;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_24706:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_24746;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_24742:; 
label_24746:; 
s__state = 8544;
s__init_num = 0;
goto label_24854;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_24729;
}
else 
{
tmp___7 = 512;
label_24729:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_24742;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_24655;
}
else 
{
if (s__state == 8545)
{
label_24655:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24690;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_24669;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_24681:; 
label_24690:; 
goto label_24854;
}
}
else 
{
label_24669:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24681;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_24640;
}
else 
{
if (s__state == 8561)
{
label_24640:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25661;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_24854;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25661;
}
else 
{
s__rwstate = 1;
goto label_24629;
}
}
else 
{
label_24629:; 
s__state = s__s3__tmp__next_state___0;
goto label_24854;
}
}
else 
{
if (s__state == 8576)
{
goto label_24597;
}
else 
{
if (s__state == 8577)
{
label_24597:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25661;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_24613;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25661;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_24613:; 
goto label_24854;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24583;
}
else 
{
if (s__state == 8593)
{
label_24583:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25661;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_24854;
}
}
else 
{
if (s__state == 8608)
{
goto label_24570;
}
else 
{
if (s__state == 8609)
{
label_24570:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25661;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_24854:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25661;
}
else 
{
goto label_24884;
}
}
else 
{
label_24884:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24914;
}
else 
{
goto label_24914;
}
}
else 
{
goto label_24914;
}
}
}
else 
{
goto label_24914;
}
}
else 
{
label_24914:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_25294;
}
else 
{
if (s__state == 16384)
{
label_25294:; 
goto label_25296;
}
else 
{
if (s__state == 8192)
{
label_25296:; 
goto label_25298;
}
else 
{
if (s__state == 24576)
{
label_25298:; 
goto label_25300;
}
else 
{
if (s__state == 8195)
{
label_25300:; 
s__server = 1;
if (cb != 0)
{
goto label_25305;
}
else 
{
label_25305:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26400 = -1;
goto label_26401;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25663;
}
else 
{
s__init_buf___0 = buf;
goto label_25317;
}
}
else 
{
label_25317:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25663;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25663;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_25337;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_25337:; 
goto label_25339;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_25273;
}
else 
{
if (s__state == 8481)
{
label_25273:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25663;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_25339;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_25339;
}
else 
{
if (s__state == 8464)
{
goto label_25260;
}
else 
{
if (s__state == 8465)
{
label_25260:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_25240;
}
else 
{
if (s__state == 8513)
{
label_25240:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_25339;
}
else 
{
if (s__state == 8528)
{
goto label_25182;
}
else 
{
if (s__state == 8529)
{
label_25182:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_25191;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_25191:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_25231;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_25227:; 
label_25231:; 
s__state = 8544;
s__init_num = 0;
goto label_25339;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_25214;
}
else 
{
tmp___7 = 512;
label_25214:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_25227;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_25140;
}
else 
{
if (s__state == 8545)
{
label_25140:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25175;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_25154;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_25166:; 
label_25175:; 
goto label_25339;
}
}
else 
{
label_25154:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25166;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_25125;
}
else 
{
if (s__state == 8561)
{
label_25125:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25663;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_25339;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25663;
}
else 
{
s__rwstate = 1;
goto label_25114;
}
}
else 
{
label_25114:; 
s__state = s__s3__tmp__next_state___0;
goto label_25339;
}
}
else 
{
if (s__state == 8576)
{
goto label_25082;
}
else 
{
if (s__state == 8577)
{
label_25082:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25663;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_25098;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25663;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_25098:; 
goto label_25339;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_25068;
}
else 
{
if (s__state == 8593)
{
label_25068:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25663;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_25339;
}
}
else 
{
if (s__state == 8608)
{
goto label_25055;
}
else 
{
if (s__state == 8609)
{
label_25055:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25663;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_25339:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25663;
}
else 
{
goto label_25369;
}
}
else 
{
label_25369:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_25037;
}
else 
{
if (s__state == 8641)
{
label_25037:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_25371:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26181;
}
else 
{
label_26181:; 
 __return_26371 = ret;
}
tmp = __return_26371;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_25047;
}
else 
{
s__state = 3;
label_25047:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25371;
}
else 
{
goto label_25367;
}
}
else 
{
label_25367:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_25025;
}
else 
{
if (s__state == 8657)
{
label_25025:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25663;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_25012;
}
else 
{
goto label_25012;
}
}
else 
{
label_25012:; 
ret = 1;
goto label_25663;
}
}
else 
{
ret = -1;
label_25663:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26125;
}
else 
{
label_26125:; 
 __return_26401 = ret;
label_26401:; 
}
tmp = __return_26401;
goto label_26468;
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
else 
{
if (s__state == 8640)
{
goto label_24552;
}
else 
{
if (s__state == 8641)
{
label_24552:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_24886:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26183;
}
else 
{
label_26183:; 
 __return_26370 = ret;
}
tmp = __return_26370;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_24562;
}
else 
{
s__state = 3;
label_24562:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_24886;
}
else 
{
goto label_24882;
}
}
else 
{
label_24882:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_24540;
}
else 
{
if (s__state == 8657)
{
label_24540:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25661;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_24527;
}
else 
{
goto label_24527;
}
}
else 
{
label_24527:; 
ret = 1;
goto label_25661;
}
}
else 
{
ret = -1;
label_25661:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26127;
}
else 
{
label_26127:; 
 __return_26403 = ret;
label_26403:; 
}
tmp = __return_26403;
goto label_26468;
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
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_24042;
}
else 
{
goto label_24042;
}
}
else 
{
label_24042:; 
ret = 1;
goto label_25659;
}
}
else 
{
ret = -1;
label_25659:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26129;
}
else 
{
label_26129:; 
 __return_26405 = ret;
label_26405:; 
}
tmp = __return_26405;
goto label_26466;
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
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_22473;
}
else 
{
goto label_22473;
}
}
else 
{
label_22473:; 
ret = 1;
goto label_25657;
}
}
else 
{
ret = -1;
label_25657:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26131;
}
else 
{
label_26131:; 
 __return_26407 = ret;
label_26407:; 
}
tmp = __return_26407;
goto label_26472;
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
else 
{
if (s__state == 8656)
{
goto label_21860;
}
else 
{
if (s__state == 8657)
{
label_21860:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25655;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_22299;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_22299:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26195;
}
else 
{
label_26195:; 
 __return_26364 = ret;
}
tmp = __return_26364;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22299;
}
else 
{
goto label_22289;
}
}
else 
{
label_22289:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_21847;
}
else 
{
goto label_21847;
}
}
else 
{
label_21847:; 
ret = 1;
goto label_25655;
}
}
else 
{
ret = -1;
label_25655:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26133;
}
else 
{
label_26133:; 
 __return_26409 = ret;
label_26409:; 
}
tmp = __return_26409;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_21255;
}
else 
{
if (s__state == 8641)
{
label_21255:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_21671:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26201;
}
else 
{
label_26201:; 
 __return_26361 = ret;
}
tmp = __return_26361;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_21265;
}
else 
{
s__state = 3;
label_21265:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21671;
}
else 
{
goto label_21661;
}
}
else 
{
label_21661:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_21230;
}
else 
{
if (s__state == 8657)
{
label_21230:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25653;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_21669;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_21669:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26203;
}
else 
{
label_26203:; 
 __return_26360 = ret;
}
tmp = __return_26360;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21669;
}
else 
{
goto label_21659;
}
}
else 
{
label_21659:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_21217;
}
else 
{
goto label_21217;
}
}
else 
{
label_21217:; 
ret = 1;
goto label_25653;
}
}
else 
{
ret = -1;
label_25653:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26135;
}
else 
{
label_26135:; 
 __return_26411 = ret;
label_26411:; 
}
tmp = __return_26411;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_20625;
}
else 
{
if (s__state == 8641)
{
label_20625:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_21041:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26209;
}
else 
{
label_26209:; 
 __return_26357 = ret;
}
tmp = __return_26357;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_20635;
}
else 
{
s__state = 3;
label_20635:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21041;
}
else 
{
goto label_21031;
}
}
else 
{
label_21031:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_20600;
}
else 
{
if (s__state == 8657)
{
label_20600:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25651;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_21039;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_21039:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26211;
}
else 
{
label_26211:; 
 __return_26356 = ret;
}
tmp = __return_26356;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21039;
}
else 
{
goto label_21029;
}
}
else 
{
label_21029:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_20587;
}
else 
{
goto label_20587;
}
}
else 
{
label_20587:; 
ret = 1;
goto label_25651;
}
}
else 
{
ret = -1;
label_25651:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26137;
}
else 
{
label_26137:; 
 __return_26413 = ret;
label_26413:; 
}
tmp = __return_26413;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_19996;
}
else 
{
if (s__state == 8641)
{
label_19996:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_20412:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26217;
}
else 
{
label_26217:; 
 __return_26353 = ret;
}
tmp = __return_26353;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_20006;
}
else 
{
s__state = 3;
label_20006:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_20412;
}
else 
{
goto label_20402;
}
}
else 
{
label_20402:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_19971;
}
else 
{
if (s__state == 8657)
{
label_19971:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25649;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_20410;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_20410:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26219;
}
else 
{
label_26219:; 
 __return_26352 = ret;
}
tmp = __return_26352;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_20410;
}
else 
{
goto label_20400;
}
}
else 
{
label_20400:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_19958;
}
else 
{
goto label_19958;
}
}
else 
{
label_19958:; 
ret = 1;
goto label_25649;
}
}
else 
{
ret = -1;
label_25649:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26139;
}
else 
{
label_26139:; 
 __return_26415 = ret;
label_26415:; 
}
tmp = __return_26415;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_19367;
}
else 
{
if (s__state == 8641)
{
label_19367:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_19783:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26225;
}
else 
{
label_26225:; 
 __return_26349 = ret;
}
tmp = __return_26349;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_19377;
}
else 
{
s__state = 3;
label_19377:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19783;
}
else 
{
goto label_19773;
}
}
else 
{
label_19773:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_19342;
}
else 
{
if (s__state == 8657)
{
label_19342:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25647;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_19781;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_19781:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26227;
}
else 
{
label_26227:; 
 __return_26348 = ret;
}
tmp = __return_26348;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19781;
}
else 
{
goto label_19771;
}
}
else 
{
label_19771:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_19329;
}
else 
{
goto label_19329;
}
}
else 
{
label_19329:; 
ret = 1;
goto label_25647;
}
}
else 
{
ret = -1;
label_25647:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26141;
}
else 
{
label_26141:; 
 __return_26417 = ret;
label_26417:; 
}
tmp = __return_26417;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_18738;
}
else 
{
if (s__state == 8641)
{
label_18738:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_19154:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26233;
}
else 
{
label_26233:; 
 __return_26345 = ret;
}
tmp = __return_26345;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_18748;
}
else 
{
s__state = 3;
label_18748:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19154;
}
else 
{
goto label_19144;
}
}
else 
{
label_19144:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_18713;
}
else 
{
if (s__state == 8657)
{
label_18713:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25645;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_19152;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_19152:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26235;
}
else 
{
label_26235:; 
 __return_26344 = ret;
}
tmp = __return_26344;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19152;
}
else 
{
goto label_19142;
}
}
else 
{
label_19142:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_18700;
}
else 
{
goto label_18700;
}
}
else 
{
label_18700:; 
ret = 1;
goto label_25645;
}
}
else 
{
ret = -1;
label_25645:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26143;
}
else 
{
label_26143:; 
 __return_26419 = ret;
label_26419:; 
}
tmp = __return_26419;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_10489;
}
else 
{
if (s__state == 8641)
{
label_10489:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_10905:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26285;
}
else 
{
label_26285:; 
 __return_26319 = ret;
}
tmp = __return_26319;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_10499;
}
else 
{
s__state = 3;
label_10499:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_10905;
}
else 
{
goto label_10895;
}
}
else 
{
label_10895:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_10464;
}
else 
{
if (s__state == 8657)
{
label_10464:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25623;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_10903;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_10903:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26287;
}
else 
{
label_26287:; 
 __return_26318 = ret;
}
tmp = __return_26318;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_10903;
}
else 
{
goto label_10893;
}
}
else 
{
label_10893:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_10451;
}
else 
{
goto label_10451;
}
}
else 
{
label_10451:; 
ret = 1;
goto label_25623;
}
}
else 
{
ret = -1;
label_25623:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26165;
}
else 
{
label_26165:; 
 __return_26441 = ret;
label_26441:; 
}
tmp = __return_26441;
goto label_26464;
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
else 
{
if (s__state == 8640)
{
goto label_6012;
}
else 
{
if (s__state == 8641)
{
label_6012:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_6428:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26301;
}
else 
{
label_26301:; 
 __return_26311 = ret;
}
tmp = __return_26311;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_6022;
}
else 
{
s__state = 3;
label_6022:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_6428;
}
else 
{
goto label_6418;
}
}
else 
{
label_6418:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_5987;
}
else 
{
if (s__state == 8657)
{
label_5987:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25613;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_6426;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_6426:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26303;
}
else 
{
label_26303:; 
 __return_26310 = ret;
}
tmp = __return_26310;
goto label_26466;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_6426;
}
else 
{
goto label_6416;
}
}
else 
{
label_6416:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6491;
}
else 
{
goto label_6491;
}
}
else 
{
goto label_6491;
}
}
}
else 
{
goto label_6491;
}
}
else 
{
label_6491:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_6882;
}
else 
{
if (s__state == 16384)
{
label_6882:; 
goto label_6884;
}
else 
{
if (s__state == 8192)
{
label_6884:; 
goto label_6886;
}
else 
{
if (s__state == 24576)
{
label_6886:; 
goto label_6888;
}
else 
{
if (s__state == 8195)
{
label_6888:; 
s__server = 1;
if (cb != 0)
{
goto label_6893;
}
else 
{
label_6893:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26448 = -1;
goto label_26449;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25615;
}
else 
{
s__init_buf___0 = buf;
goto label_6905;
}
}
else 
{
label_6905:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25615;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25615;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_6925;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_6925:; 
goto label_6927;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_6861;
}
else 
{
if (s__state == 8481)
{
label_6861:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25615;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_6927;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_6927;
}
else 
{
if (s__state == 8464)
{
goto label_6848;
}
else 
{
if (s__state == 8465)
{
label_6848:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_6828;
}
else 
{
if (s__state == 8513)
{
label_6828:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_6927;
}
else 
{
if (s__state == 8528)
{
goto label_6770;
}
else 
{
if (s__state == 8529)
{
label_6770:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_6779;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_6779:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_6819;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_6815:; 
label_6819:; 
s__state = 8544;
s__init_num = 0;
goto label_6927;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_6802;
}
else 
{
tmp___7 = 512;
label_6802:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_6815;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_6728;
}
else 
{
if (s__state == 8545)
{
label_6728:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_6763;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_6742;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_6754:; 
label_6763:; 
goto label_6927;
}
}
else 
{
label_6742:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_6754;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_6713;
}
else 
{
if (s__state == 8561)
{
label_6713:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25615;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_6927;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25615;
}
else 
{
s__rwstate = 1;
goto label_6702;
}
}
else 
{
label_6702:; 
s__state = s__s3__tmp__next_state___0;
goto label_6927;
}
}
else 
{
if (s__state == 8576)
{
goto label_6670;
}
else 
{
if (s__state == 8577)
{
label_6670:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25615;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_6686;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25615;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_6686:; 
goto label_6927;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_6656;
}
else 
{
if (s__state == 8593)
{
label_6656:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25615;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_6927;
}
}
else 
{
if (s__state == 8608)
{
goto label_6643;
}
else 
{
if (s__state == 8609)
{
label_6643:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25615;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_6927:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25615;
}
else 
{
goto label_6957;
}
}
else 
{
label_6957:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_6626;
}
else 
{
if (s__state == 8657)
{
label_6626:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25615;
}
else 
{
if (s__state == 8672)
{
goto label_6607;
}
else 
{
if (s__state == 8673)
{
label_6607:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_6959:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26295;
}
else 
{
label_26295:; 
 __return_26314 = ret;
}
tmp = __return_26314;
goto label_26468;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_6618;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_6618:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_6959;
}
else 
{
goto label_6955;
}
}
else 
{
label_6955:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6985;
}
else 
{
goto label_6985;
}
}
else 
{
goto label_6985;
}
}
}
else 
{
goto label_6985;
}
}
else 
{
label_6985:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_7366;
}
else 
{
if (s__state == 16384)
{
label_7366:; 
goto label_7368;
}
else 
{
if (s__state == 8192)
{
label_7368:; 
goto label_7370;
}
else 
{
if (s__state == 24576)
{
label_7370:; 
goto label_7372;
}
else 
{
if (s__state == 8195)
{
label_7372:; 
s__server = 1;
if (cb != 0)
{
goto label_7377;
}
else 
{
label_7377:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26446 = -1;
goto label_26447;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25617;
}
else 
{
s__init_buf___0 = buf;
goto label_7389;
}
}
else 
{
label_7389:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25617;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25617;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_7409;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_7409:; 
goto label_7411;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_7345;
}
else 
{
if (s__state == 8481)
{
label_7345:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25617;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_7411;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_7411;
}
else 
{
if (s__state == 8464)
{
goto label_7332;
}
else 
{
if (s__state == 8465)
{
label_7332:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_7312;
}
else 
{
if (s__state == 8513)
{
label_7312:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_7411;
}
else 
{
if (s__state == 8528)
{
goto label_7254;
}
else 
{
if (s__state == 8529)
{
label_7254:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_7263;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_7263:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_7303;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_7299:; 
label_7303:; 
s__state = 8544;
s__init_num = 0;
goto label_7411;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_7286;
}
else 
{
tmp___7 = 512;
label_7286:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_7299;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_7212;
}
else 
{
if (s__state == 8545)
{
label_7212:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_7247;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_7226;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_7238:; 
label_7247:; 
goto label_7411;
}
}
else 
{
label_7226:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_7238;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_7197;
}
else 
{
if (s__state == 8561)
{
label_7197:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25617;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_7411;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25617;
}
else 
{
s__rwstate = 1;
goto label_7186;
}
}
else 
{
label_7186:; 
s__state = s__s3__tmp__next_state___0;
goto label_7411;
}
}
else 
{
if (s__state == 8576)
{
goto label_7154;
}
else 
{
if (s__state == 8577)
{
label_7154:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25617;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_7170;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25617;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_7170:; 
goto label_7411;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_7140;
}
else 
{
if (s__state == 8593)
{
label_7140:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25617;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_7411;
}
}
else 
{
if (s__state == 8608)
{
goto label_7127;
}
else 
{
if (s__state == 8609)
{
label_7127:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25617;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_7411:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25617;
}
else 
{
goto label_7441;
}
}
else 
{
label_7441:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7471;
}
else 
{
goto label_7471;
}
}
else 
{
goto label_7471;
}
}
}
else 
{
goto label_7471;
}
}
else 
{
label_7471:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_7851;
}
else 
{
if (s__state == 16384)
{
label_7851:; 
goto label_7853;
}
else 
{
if (s__state == 8192)
{
label_7853:; 
goto label_7855;
}
else 
{
if (s__state == 24576)
{
label_7855:; 
goto label_7857;
}
else 
{
if (s__state == 8195)
{
label_7857:; 
s__server = 1;
if (cb != 0)
{
goto label_7862;
}
else 
{
label_7862:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26444 = -1;
goto label_26445;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25619;
}
else 
{
s__init_buf___0 = buf;
goto label_7874;
}
}
else 
{
label_7874:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25619;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25619;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_7894;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_7894:; 
goto label_7896;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_7830;
}
else 
{
if (s__state == 8481)
{
label_7830:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25619;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_7896;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_7896;
}
else 
{
if (s__state == 8464)
{
goto label_7817;
}
else 
{
if (s__state == 8465)
{
label_7817:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_7797;
}
else 
{
if (s__state == 8513)
{
label_7797:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_7896;
}
else 
{
if (s__state == 8528)
{
goto label_7739;
}
else 
{
if (s__state == 8529)
{
label_7739:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_7748;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_7748:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_7788;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_7784:; 
label_7788:; 
s__state = 8544;
s__init_num = 0;
goto label_7896;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_7771;
}
else 
{
tmp___7 = 512;
label_7771:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_7784;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_7697;
}
else 
{
if (s__state == 8545)
{
label_7697:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_7732;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_7711;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_7723:; 
label_7732:; 
goto label_7896;
}
}
else 
{
label_7711:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_7723;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_7682;
}
else 
{
if (s__state == 8561)
{
label_7682:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25619;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_7896;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25619;
}
else 
{
s__rwstate = 1;
goto label_7671;
}
}
else 
{
label_7671:; 
s__state = s__s3__tmp__next_state___0;
goto label_7896;
}
}
else 
{
if (s__state == 8576)
{
goto label_7639;
}
else 
{
if (s__state == 8577)
{
label_7639:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25619;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_7655;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25619;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_7655:; 
goto label_7896;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_7625;
}
else 
{
if (s__state == 8593)
{
label_7625:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25619;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_7896;
}
}
else 
{
if (s__state == 8608)
{
goto label_7612;
}
else 
{
if (s__state == 8609)
{
label_7612:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25619;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_7896:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25619;
}
else 
{
goto label_7926;
}
}
else 
{
label_7926:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_7594;
}
else 
{
if (s__state == 8641)
{
label_7594:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_7928:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26291;
}
else 
{
label_26291:; 
 __return_26316 = ret;
}
tmp = __return_26316;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_7604;
}
else 
{
s__state = 3;
label_7604:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_7928;
}
else 
{
goto label_7924;
}
}
else 
{
label_7924:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7954;
}
else 
{
goto label_7954;
}
}
else 
{
goto label_7954;
}
}
}
else 
{
goto label_7954;
}
}
else 
{
label_7954:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_8335;
}
else 
{
if (s__state == 16384)
{
label_8335:; 
goto label_8337;
}
else 
{
if (s__state == 8192)
{
label_8337:; 
goto label_8339;
}
else 
{
if (s__state == 24576)
{
label_8339:; 
goto label_8341;
}
else 
{
if (s__state == 8195)
{
label_8341:; 
s__server = 1;
if (cb != 0)
{
goto label_8346;
}
else 
{
label_8346:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_26442 = -1;
goto label_26443;
}
else 
{
s__type = 8192;
if (s__init_buf___0 == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
ret = -1;
goto label_25621;
}
else 
{
s__init_buf___0 = buf;
goto label_8358;
}
}
else 
{
label_8358:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_25621;
}
else 
{
s__init_num = 0;
if (s__state != 12292)
{
tmp___5 = ssl_init_wbio_buffer();
if (tmp___5 == 0)
{
ret = -1;
goto label_25621;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_8378;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_8378:; 
goto label_8380;
}
}
}
}
}
}
else 
{
if (s__state == 8480)
{
goto label_8314;
}
else 
{
if (s__state == 8481)
{
label_8314:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_25621;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_8380;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_8380;
}
else 
{
if (s__state == 8464)
{
goto label_8301;
}
else 
{
if (s__state == 8465)
{
label_8301:; 
return 1;
}
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_8281;
}
else 
{
if (s__state == 8513)
{
label_8281:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_8380;
}
else 
{
if (s__state == 8528)
{
goto label_8223;
}
else 
{
if (s__state == 8529)
{
label_8223:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_8232;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_8232:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_8272;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_8268:; 
label_8272:; 
s__state = 8544;
s__init_num = 0;
goto label_8380;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_8255;
}
else 
{
tmp___7 = 512;
label_8255:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_8268;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_8181;
}
else 
{
if (s__state == 8545)
{
label_8181:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_8216;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_8195;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_8207:; 
label_8216:; 
goto label_8380;
}
}
else 
{
label_8195:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_8207;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_8166;
}
else 
{
if (s__state == 8561)
{
label_8166:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25621;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_8380;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25621;
}
else 
{
s__rwstate = 1;
goto label_8155;
}
}
else 
{
label_8155:; 
s__state = s__s3__tmp__next_state___0;
goto label_8380;
}
}
else 
{
if (s__state == 8576)
{
goto label_8123;
}
else 
{
if (s__state == 8577)
{
label_8123:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25621;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_8139;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25621;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_8139:; 
goto label_8380;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_8109;
}
else 
{
if (s__state == 8593)
{
label_8109:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25621;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_8380;
}
}
else 
{
if (s__state == 8608)
{
goto label_8096;
}
else 
{
if (s__state == 8609)
{
label_8096:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25621;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_8380:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25621;
}
else 
{
goto label_8410;
}
}
else 
{
label_8410:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_8066;
}
else 
{
if (s__state == 8657)
{
label_8066:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_25621;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_8412;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_8412:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26289;
}
else 
{
label_26289:; 
 __return_26317 = ret;
}
tmp = __return_26317;
goto label_26466;
}
else 
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_8412;
}
else 
{
goto label_8408;
}
}
else 
{
label_8408:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_8053;
}
else 
{
goto label_8053;
}
}
else 
{
label_8053:; 
ret = 1;
goto label_25621;
}
}
else 
{
ret = -1;
label_25621:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26167;
}
else 
{
label_26167:; 
 __return_26443 = ret;
label_26443:; 
}
tmp = __return_26443;
label_26472:; 
 __return_26646 = tmp;
return 1;
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
else 
{
if (s__state == 8656)
{
goto label_7582;
}
else 
{
if (s__state == 8657)
{
label_7582:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25619;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_7569;
}
else 
{
goto label_7569;
}
}
else 
{
label_7569:; 
ret = 1;
goto label_25619;
}
}
else 
{
ret = -1;
label_25619:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26169;
}
else 
{
label_26169:; 
 __return_26445 = ret;
label_26445:; 
}
tmp = __return_26445;
goto label_26468;
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
else 
{
if (s__state == 8640)
{
goto label_7109;
}
else 
{
if (s__state == 8641)
{
label_7109:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_7443:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26293;
}
else 
{
label_26293:; 
 __return_26315 = ret;
}
tmp = __return_26315;
goto label_26472;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_7119;
}
else 
{
s__state = 3;
label_7119:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_7443;
}
else 
{
goto label_7439;
}
}
else 
{
label_7439:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_7097;
}
else 
{
if (s__state == 8657)
{
label_7097:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25617;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_7084;
}
else 
{
goto label_7084;
}
}
else 
{
label_7084:; 
ret = 1;
goto label_25617;
}
}
else 
{
ret = -1;
label_25617:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26171;
}
else 
{
label_26171:; 
 __return_26447 = ret;
label_26447:; 
}
tmp = __return_26447;
label_26468:; 
 __return_26648 = tmp;
return 1;
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
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_6599;
}
else 
{
goto label_6599;
}
}
else 
{
label_6599:; 
ret = 1;
goto label_25615;
}
}
else 
{
ret = -1;
label_25615:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26173;
}
else 
{
label_26173:; 
 __return_26449 = ret;
label_26449:; 
}
tmp = __return_26449;
label_26466:; 
 __return_26650 = tmp;
return 1;
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
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_5974;
}
else 
{
goto label_5974;
}
}
else 
{
label_5974:; 
ret = 1;
goto label_25613;
}
}
else 
{
ret = -1;
label_25613:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26175;
}
else 
{
label_26175:; 
 __return_26451 = ret;
label_26451:; 
}
tmp = __return_26451;
label_26464:; 
 __return_26652 = tmp;
return 1;
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
else 
{
if (s__state == 8512)
{
goto label_5705;
}
else 
{
if (s__state == 8513)
{
label_5705:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_5817;
}
else 
{
if (s__state == 8528)
{
goto label_5647;
}
else 
{
if (s__state == 8529)
{
label_5647:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_5656;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_5656:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_5696;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_5692:; 
label_5696:; 
s__state = 8544;
s__init_num = 0;
goto label_5817;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_5679;
}
else 
{
tmp___7 = 512;
label_5679:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_5692;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_5605;
}
else 
{
if (s__state == 8545)
{
label_5605:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_5640;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_5619;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_5631:; 
label_5640:; 
goto label_5817;
}
}
else 
{
label_5619:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_5631;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_5590;
}
else 
{
if (s__state == 8561)
{
label_5590:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25611;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_5817;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25611;
}
else 
{
s__rwstate = 1;
goto label_5579;
}
}
else 
{
label_5579:; 
s__state = s__s3__tmp__next_state___0;
goto label_5817;
}
}
else 
{
if (s__state == 8576)
{
goto label_5547;
}
else 
{
if (s__state == 8577)
{
label_5547:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25611;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_5563;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25611;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_5563:; 
goto label_5817;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_5533;
}
else 
{
if (s__state == 8593)
{
label_5533:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25611;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_5817;
}
}
else 
{
if (s__state == 8608)
{
goto label_5520;
}
else 
{
if (s__state == 8609)
{
label_5520:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25611;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_5817:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25611;
}
else 
{
goto label_5847;
}
}
else 
{
label_5847:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_5503;
}
else 
{
if (s__state == 8657)
{
label_5503:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25611;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_5490;
}
else 
{
goto label_5490;
}
}
else 
{
label_5490:; 
ret = 1;
goto label_25611;
}
}
else 
{
ret = -1;
label_25611:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26177;
}
else 
{
label_26177:; 
 __return_26453 = ret;
label_26453:; 
}
tmp = __return_26453;
label_26462:; 
 __return_26654 = tmp;
return 1;
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
else 
{
if (s__state == 8496)
{
return 1;
}
else 
{
if (s__state == 8512)
{
goto label_5226;
}
else 
{
if (s__state == 8513)
{
label_5226:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_5333;
}
else 
{
if (s__state == 8528)
{
goto label_5168;
}
else 
{
if (s__state == 8529)
{
label_5168:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_5177;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_5177:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_5217;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
return 1;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_5213:; 
label_5217:; 
s__state = 8544;
s__init_num = 0;
goto label_5333;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_5200;
}
else 
{
tmp___7 = 512;
label_5200:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_5213;
}
}
}
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8544)
{
goto label_5126;
}
else 
{
if (s__state == 8545)
{
label_5126:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_5161;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_5140;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_5152:; 
label_5161:; 
goto label_5333;
}
}
else 
{
label_5140:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
return 1;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_5152;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_5111;
}
else 
{
if (s__state == 8561)
{
label_5111:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_25609;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_5333;
}
}
else 
{
if (s__state == 8448)
{
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_25609;
}
else 
{
s__rwstate = 1;
goto label_5100;
}
}
else 
{
label_5100:; 
s__state = s__s3__tmp__next_state___0;
goto label_5333;
}
}
else 
{
if (s__state == 8576)
{
goto label_5068;
}
else 
{
if (s__state == 8577)
{
label_5068:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_25609;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_5084;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_25609;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_5084:; 
goto label_5333;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_5054;
}
else 
{
if (s__state == 8593)
{
label_5054:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_25609;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_5333;
}
}
else 
{
if (s__state == 8608)
{
goto label_5041;
}
else 
{
if (s__state == 8609)
{
label_5041:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_25609;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_5333:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25609;
}
else 
{
goto label_5363;
}
}
else 
{
label_5363:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
}
else 
{
return 1;
}
}
}
else 
{
if (s__state == 8640)
{
return 1;
}
else 
{
if (s__state == 8656)
{
goto label_5024;
}
else 
{
if (s__state == 8657)
{
label_5024:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_25609;
}
else 
{
if (s__state == 8672)
{
return 1;
}
else 
{
if (s__state == 3)
{
s__init_buf___0 = 0;
s__init_num = 0;
if (!(got_new_session == 0))
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_5011;
}
else 
{
goto label_5011;
}
}
else 
{
label_5011:; 
ret = 1;
goto label_25609;
}
}
else 
{
ret = -1;
label_25609:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_26179;
}
else 
{
label_26179:; 
 __return_26455 = ret;
label_26455:; 
}
tmp = __return_26455;
goto label_26458;
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
tmp = __return_26456;
label_26458:; 
 __return_26656 = tmp;
return 1;
}
}
}
}
