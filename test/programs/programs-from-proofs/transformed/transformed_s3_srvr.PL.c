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
int __return_46459;
int __return_46617;
int __return_46457;
int __return_46348;
int __return_46455;
int __return_46349;
int __return_46453;
int __return_46353;
int __return_46352;
int __return_46443;
int __return_46361;
int __return_46360;
int __return_46441;
int __return_46364;
int __return_46439;
int __return_46367;
int __return_46437;
int __return_46370;
int __return_46435;
int __return_46373;
int __return_46431;
int __return_46377;
int __return_46376;
int __return_46429;
int __return_46378;
int __return_46430;
int __return_46375;
int __return_46432;
int __return_46372;
int __return_46433;
int __return_46374;
int __return_46434;
int __return_46371;
int __return_46436;
int __return_46369;
int __return_46368;
int __return_46438;
int __return_46366;
int __return_46365;
int __return_46440;
int __return_46363;
int __return_46362;
int __return_46442;
int __return_46605;
int __return_46427;
int __return_46382;
int __return_46381;
int __return_46425;
int __return_46386;
int __return_46385;
int __return_46423;
int __return_46390;
int __return_46389;
int __return_46421;
int __return_46394;
int __return_46393;
int __return_46419;
int __return_46398;
int __return_46397;
int __return_46396;
int __return_46395;
int __return_46420;
int __return_46392;
int __return_46391;
int __return_46422;
int __return_46388;
int __return_46387;
int __return_46424;
int __return_46384;
int __return_46383;
int __return_46426;
int __return_46380;
int __return_46379;
int __return_46428;
int __return_46359;
int __return_46358;
int __return_46444;
int __return_46351;
int __return_46350;
int __return_46451;
int __return_46354;
int __return_46449;
int __return_46447;
int __return_46356;
int __return_46445;
int __return_46357;
int __return_46446;
int __return_46607;
int __return_46448;
int __return_46355;
int __return_46450;
int __return_46609;
int __return_46452;
int __return_46611;
int __return_46454;
int __return_46613;
int __return_46456;
int __return_46615;
int __return_46458;
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
goto label_34438;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_34438;
}
else 
{
label_34438:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_46459 = -1;
}
else 
{
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_34816;
}
else 
{
if (s__state == 16384)
{
label_34816:; 
goto label_34818;
}
else 
{
if (s__state == 8192)
{
label_34818:; 
goto label_34820;
}
else 
{
if (s__state == 24576)
{
label_34820:; 
goto label_34822;
}
else 
{
if (s__state == 8195)
{
label_34822:; 
s__server = 1;
if (cb != 0)
{
goto label_34827;
}
else 
{
label_34827:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46457 = -1;
goto label_46458;
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
goto label_45812;
}
else 
{
s__init_buf___0 = buf;
goto label_34839;
}
}
else 
{
label_34839:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45812;
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
goto label_45812;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_34859;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_34859:; 
goto label_34861;
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
goto label_34795;
}
else 
{
if (s__state == 8481)
{
label_34795:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45812;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_34861;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_34861;
}
else 
{
if (s__state == 8464)
{
goto label_34774;
}
else 
{
if (s__state == 8465)
{
label_34774:; 
goto label_34776;
}
else 
{
if (s__state == 8466)
{
label_34776:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_34893:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46347;
}
else 
{
label_46347:; 
 __return_46348 = ret;
}
tmp = __return_46348;
goto label_46465;
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
goto label_34893;
}
else 
{
goto label_34889;
}
}
else 
{
label_34889:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_34919;
}
else 
{
goto label_34919;
}
}
else 
{
goto label_34919;
}
}
}
else 
{
goto label_34919;
}
}
else 
{
label_34919:; 
skip = 0;
label_34925:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_35300;
}
else 
{
if (s__state == 16384)
{
label_35300:; 
goto label_35302;
}
else 
{
if (s__state == 8192)
{
label_35302:; 
goto label_35304;
}
else 
{
if (s__state == 24576)
{
label_35304:; 
goto label_35306;
}
else 
{
if (s__state == 8195)
{
label_35306:; 
s__server = 1;
if (cb != 0)
{
goto label_35311;
}
else 
{
label_35311:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46455 = -1;
goto label_46456;
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
goto label_45814;
}
else 
{
s__init_buf___0 = buf;
goto label_35323;
}
}
else 
{
label_35323:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45814;
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
goto label_45814;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_35343;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_35343:; 
goto label_35345;
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
goto label_35279;
}
else 
{
if (s__state == 8481)
{
label_35279:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45814;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_35345;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_35345;
}
else 
{
if (s__state == 8464)
{
goto label_35266;
}
else 
{
if (s__state == 8465)
{
label_35266:; 
return 1;
}
else 
{
if (s__state == 8496)
{
goto label_35247;
}
else 
{
if (s__state == 8497)
{
label_35247:; 
ret = ssl3_send_server_hello();
if (ret <= 0)
{
label_35377:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46345;
}
else 
{
label_46345:; 
 __return_46349 = ret;
}
tmp = __return_46349;
goto label_46467;
}
else 
{
if (s__hit == 0)
{
s__state = 8512;
goto label_35257;
}
else 
{
s__state = 8656;
label_35257:; 
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
goto label_35377;
}
else 
{
goto label_35373;
}
}
else 
{
label_35373:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_35403;
}
else 
{
goto label_35403;
}
}
else 
{
goto label_35403;
}
}
}
else 
{
goto label_35403;
}
}
else 
{
label_35403:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_35832;
}
else 
{
if (s__state == 16384)
{
label_35832:; 
goto label_35834;
}
else 
{
if (s__state == 8192)
{
label_35834:; 
goto label_35836;
}
else 
{
if (s__state == 24576)
{
label_35836:; 
goto label_35838;
}
else 
{
if (s__state == 8195)
{
label_35838:; 
s__server = 1;
if (cb != 0)
{
goto label_35843;
}
else 
{
label_35843:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46453 = -1;
goto label_46454;
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
goto label_45816;
}
else 
{
s__init_buf___0 = buf;
goto label_35855;
}
}
else 
{
label_35855:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45816;
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
goto label_45816;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_35875;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_35875:; 
goto label_35877;
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
goto label_35811;
}
else 
{
if (s__state == 8481)
{
label_35811:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45816;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_35877;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_35877;
}
else 
{
if (s__state == 8464)
{
goto label_35790;
}
else 
{
if (s__state == 8465)
{
label_35790:; 
goto label_35792;
}
else 
{
if (s__state == 8466)
{
label_35792:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_35960:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46337;
}
else 
{
label_46337:; 
 __return_46353 = ret;
}
tmp = __return_46353;
goto label_46465;
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
goto label_35960;
}
else 
{
goto label_35950;
}
}
else 
{
label_35950:; 
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
goto label_35763;
}
else 
{
if (s__state == 8513)
{
label_35763:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45816;
}
else 
{
goto label_35773;
}
}
else 
{
skip = 1;
label_35773:; 
s__state = 8528;
s__init_num = 0;
goto label_35877;
}
}
else 
{
if (s__state == 8528)
{
goto label_35695;
}
else 
{
if (s__state == 8529)
{
label_35695:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_35704;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_35704:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_35751;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_35738;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_35745:; 
label_35751:; 
s__state = 8544;
s__init_num = 0;
goto label_35877;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_35727;
}
else 
{
tmp___7 = 512;
label_35727:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_35738;
}
else 
{
skip = 1;
goto label_35745;
}
}
}
}
}
}
else 
{
goto label_35738;
}
}
else 
{
label_35738:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_35958:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46339;
}
else 
{
label_46339:; 
 __return_46352 = ret;
}
tmp = __return_46352;
goto label_46479;
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
goto label_35958;
}
else 
{
goto label_35948;
}
}
else 
{
label_35948:; 
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
goto label_35643;
}
else 
{
if (s__state == 8545)
{
label_35643:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35688;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_35657;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_35680:; 
label_35688:; 
goto label_35877;
}
}
else 
{
label_35657:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_35671;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35680;
}
else 
{
label_35671:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45816;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_35680;
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
goto label_35628;
}
else 
{
if (s__state == 8561)
{
label_35628:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45816;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_35877;
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
goto label_45816;
}
else 
{
s__rwstate = 1;
goto label_35617;
}
}
else 
{
label_35617:; 
s__state = s__s3__tmp__next_state___0;
goto label_35877;
}
}
else 
{
if (s__state == 8576)
{
goto label_35585;
}
else 
{
if (s__state == 8577)
{
label_35585:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45816;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_35601;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45816;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_35601:; 
goto label_35877;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_35571;
}
else 
{
if (s__state == 8593)
{
label_35571:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45816;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_35877;
}
}
else 
{
if (s__state == 8608)
{
goto label_35558;
}
else 
{
if (s__state == 8609)
{
label_35558:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45816;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_35877:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45816;
}
else 
{
goto label_35952;
}
}
else 
{
label_35952:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36027;
}
else 
{
goto label_36027;
}
}
else 
{
goto label_36027;
}
}
}
else 
{
goto label_36027;
}
}
else 
{
label_36027:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_38403;
}
else 
{
if (s__state == 16384)
{
label_38403:; 
goto label_38405;
}
else 
{
if (s__state == 8192)
{
label_38405:; 
goto label_38407;
}
else 
{
if (s__state == 24576)
{
label_38407:; 
goto label_38409;
}
else 
{
if (s__state == 8195)
{
label_38409:; 
s__server = 1;
if (cb != 0)
{
goto label_38414;
}
else 
{
label_38414:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46443 = -1;
goto label_46444;
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
goto label_45826;
}
else 
{
s__init_buf___0 = buf;
goto label_38426;
}
}
else 
{
label_38426:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45826;
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
goto label_45826;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_38446;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_38446:; 
goto label_38448;
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
goto label_38382;
}
else 
{
if (s__state == 8481)
{
label_38382:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45826;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_38448;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_38448;
}
else 
{
if (s__state == 8464)
{
goto label_38361;
}
else 
{
if (s__state == 8465)
{
label_38361:; 
goto label_38363;
}
else 
{
if (s__state == 8466)
{
label_38363:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_38531:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46321;
}
else 
{
label_46321:; 
 __return_46361 = ret;
}
tmp = __return_46361;
goto label_46465;
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
goto label_38531;
}
else 
{
goto label_38521;
}
}
else 
{
label_38521:; 
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
goto label_38334;
}
else 
{
if (s__state == 8513)
{
label_38334:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45826;
}
else 
{
goto label_38344;
}
}
else 
{
skip = 1;
label_38344:; 
s__state = 8528;
s__init_num = 0;
goto label_38448;
}
}
else 
{
if (s__state == 8528)
{
goto label_38266;
}
else 
{
if (s__state == 8529)
{
label_38266:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_38275;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_38275:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_38322;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_38309;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_38316:; 
label_38322:; 
s__state = 8544;
s__init_num = 0;
goto label_38448;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_38298;
}
else 
{
tmp___7 = 512;
label_38298:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_38309;
}
else 
{
skip = 1;
goto label_38316;
}
}
}
}
}
}
else 
{
goto label_38309;
}
}
else 
{
label_38309:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_38529:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46323;
}
else 
{
label_46323:; 
 __return_46360 = ret;
}
tmp = __return_46360;
goto label_46479;
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
goto label_38529;
}
else 
{
goto label_38519;
}
}
else 
{
label_38519:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_38594;
}
else 
{
goto label_38594;
}
}
else 
{
goto label_38594;
}
}
}
else 
{
goto label_38594;
}
}
else 
{
label_38594:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_39020;
}
else 
{
if (s__state == 16384)
{
label_39020:; 
goto label_39022;
}
else 
{
if (s__state == 8192)
{
label_39022:; 
goto label_39024;
}
else 
{
if (s__state == 24576)
{
label_39024:; 
goto label_39026;
}
else 
{
if (s__state == 8195)
{
label_39026:; 
s__server = 1;
if (cb != 0)
{
goto label_39031;
}
else 
{
label_39031:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46441 = -1;
goto label_46442;
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
goto label_45828;
}
else 
{
s__init_buf___0 = buf;
goto label_39043;
}
}
else 
{
label_39043:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45828;
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
goto label_45828;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_39063;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_39063:; 
goto label_39065;
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
goto label_38999;
}
else 
{
if (s__state == 8481)
{
label_38999:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45828;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_39065;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_39065;
}
else 
{
if (s__state == 8464)
{
goto label_38978;
}
else 
{
if (s__state == 8465)
{
label_38978:; 
goto label_38980;
}
else 
{
if (s__state == 8466)
{
label_38980:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_39131:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46315;
}
else 
{
label_46315:; 
 __return_46364 = ret;
}
tmp = __return_46364;
goto label_46465;
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
goto label_39131;
}
else 
{
goto label_39123;
}
}
else 
{
label_39123:; 
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
goto label_38951;
}
else 
{
if (s__state == 8513)
{
label_38951:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45828;
}
else 
{
goto label_38961;
}
}
else 
{
skip = 1;
label_38961:; 
s__state = 8528;
s__init_num = 0;
goto label_39065;
}
}
else 
{
if (s__state == 8528)
{
goto label_38893;
}
else 
{
if (s__state == 8529)
{
label_38893:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_38902;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_38902:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_38942;
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
label_38938:; 
label_38942:; 
s__state = 8544;
s__init_num = 0;
goto label_39065;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_38925;
}
else 
{
tmp___7 = 512;
label_38925:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_38938;
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
goto label_38841;
}
else 
{
if (s__state == 8545)
{
label_38841:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_38886;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_38855;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_38878:; 
label_38886:; 
goto label_39065;
}
}
else 
{
label_38855:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_38869;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_38878;
}
else 
{
label_38869:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45828;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_38878;
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
goto label_38826;
}
else 
{
if (s__state == 8561)
{
label_38826:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45828;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_39065;
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
goto label_45828;
}
else 
{
s__rwstate = 1;
goto label_38815;
}
}
else 
{
label_38815:; 
s__state = s__s3__tmp__next_state___0;
goto label_39065;
}
}
else 
{
if (s__state == 8576)
{
goto label_38783;
}
else 
{
if (s__state == 8577)
{
label_38783:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45828;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_38799;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45828;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_38799:; 
goto label_39065;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_38769;
}
else 
{
if (s__state == 8593)
{
label_38769:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45828;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_39065;
}
}
else 
{
if (s__state == 8608)
{
goto label_38756;
}
else 
{
if (s__state == 8609)
{
label_38756:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45828;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_39065:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45828;
}
else 
{
goto label_39125;
}
}
else 
{
label_39125:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_39185;
}
else 
{
goto label_39185;
}
}
else 
{
goto label_39185;
}
}
}
else 
{
goto label_39185;
}
}
else 
{
label_39185:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_39607;
}
else 
{
if (s__state == 16384)
{
label_39607:; 
goto label_39609;
}
else 
{
if (s__state == 8192)
{
label_39609:; 
goto label_39611;
}
else 
{
if (s__state == 24576)
{
label_39611:; 
goto label_39613;
}
else 
{
if (s__state == 8195)
{
label_39613:; 
s__server = 1;
if (cb != 0)
{
goto label_39618;
}
else 
{
label_39618:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46439 = -1;
goto label_46440;
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
goto label_45830;
}
else 
{
s__init_buf___0 = buf;
goto label_39630;
}
}
else 
{
label_39630:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45830;
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
goto label_45830;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_39650;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_39650:; 
goto label_39652;
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
goto label_39586;
}
else 
{
if (s__state == 8481)
{
label_39586:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45830;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_39652;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_39652;
}
else 
{
if (s__state == 8464)
{
goto label_39565;
}
else 
{
if (s__state == 8465)
{
label_39565:; 
goto label_39567;
}
else 
{
if (s__state == 8466)
{
label_39567:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_39718:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46309;
}
else 
{
label_46309:; 
 __return_46367 = ret;
}
tmp = __return_46367;
goto label_46465;
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
goto label_39718;
}
else 
{
goto label_39710;
}
}
else 
{
label_39710:; 
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
goto label_39538;
}
else 
{
if (s__state == 8513)
{
label_39538:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45830;
}
else 
{
goto label_39548;
}
}
else 
{
skip = 1;
label_39548:; 
s__state = 8528;
s__init_num = 0;
goto label_39652;
}
}
else 
{
if (s__state == 8528)
{
goto label_39480;
}
else 
{
if (s__state == 8529)
{
label_39480:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_39489;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_39489:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_39529;
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
label_39525:; 
label_39529:; 
s__state = 8544;
s__init_num = 0;
goto label_39652;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_39512;
}
else 
{
tmp___7 = 512;
label_39512:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_39525;
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
goto label_39428;
}
else 
{
if (s__state == 8545)
{
label_39428:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_39473;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_39442;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_39465:; 
label_39473:; 
goto label_39652;
}
}
else 
{
label_39442:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_39456;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_39465;
}
else 
{
label_39456:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45830;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_39465;
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
goto label_39413;
}
else 
{
if (s__state == 8561)
{
label_39413:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45830;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_39652;
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
goto label_45830;
}
else 
{
s__rwstate = 1;
goto label_39402;
}
}
else 
{
label_39402:; 
s__state = s__s3__tmp__next_state___0;
goto label_39652;
}
}
else 
{
if (s__state == 8576)
{
goto label_39370;
}
else 
{
if (s__state == 8577)
{
label_39370:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45830;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_39386;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45830;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_39386:; 
goto label_39652;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_39356;
}
else 
{
if (s__state == 8593)
{
label_39356:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45830;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_39652;
}
}
else 
{
if (s__state == 8608)
{
goto label_39343;
}
else 
{
if (s__state == 8609)
{
label_39343:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45830;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_39652:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45830;
}
else 
{
goto label_39712;
}
}
else 
{
label_39712:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_39772;
}
else 
{
goto label_39772;
}
}
else 
{
goto label_39772;
}
}
}
else 
{
goto label_39772;
}
}
else 
{
label_39772:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_40194;
}
else 
{
if (s__state == 16384)
{
label_40194:; 
goto label_40196;
}
else 
{
if (s__state == 8192)
{
label_40196:; 
goto label_40198;
}
else 
{
if (s__state == 24576)
{
label_40198:; 
goto label_40200;
}
else 
{
if (s__state == 8195)
{
label_40200:; 
s__server = 1;
if (cb != 0)
{
goto label_40205;
}
else 
{
label_40205:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46437 = -1;
goto label_46438;
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
goto label_45832;
}
else 
{
s__init_buf___0 = buf;
goto label_40217;
}
}
else 
{
label_40217:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45832;
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
goto label_45832;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_40237;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_40237:; 
goto label_40239;
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
goto label_40173;
}
else 
{
if (s__state == 8481)
{
label_40173:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45832;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_40239;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_40239;
}
else 
{
if (s__state == 8464)
{
goto label_40152;
}
else 
{
if (s__state == 8465)
{
label_40152:; 
goto label_40154;
}
else 
{
if (s__state == 8466)
{
label_40154:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_40305:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46303;
}
else 
{
label_46303:; 
 __return_46370 = ret;
}
tmp = __return_46370;
goto label_46465;
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
goto label_40305;
}
else 
{
goto label_40297;
}
}
else 
{
label_40297:; 
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
goto label_40125;
}
else 
{
if (s__state == 8513)
{
label_40125:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45832;
}
else 
{
goto label_40135;
}
}
else 
{
skip = 1;
label_40135:; 
s__state = 8528;
s__init_num = 0;
goto label_40239;
}
}
else 
{
if (s__state == 8528)
{
goto label_40067;
}
else 
{
if (s__state == 8529)
{
label_40067:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_40076;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_40076:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_40116;
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
label_40112:; 
label_40116:; 
s__state = 8544;
s__init_num = 0;
goto label_40239;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_40099;
}
else 
{
tmp___7 = 512;
label_40099:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_40112;
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
goto label_40015;
}
else 
{
if (s__state == 8545)
{
label_40015:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_40060;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_40029;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_40052:; 
label_40060:; 
goto label_40239;
}
}
else 
{
label_40029:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_40043;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_40052;
}
else 
{
label_40043:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45832;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_40052;
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
goto label_40000;
}
else 
{
if (s__state == 8561)
{
label_40000:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45832;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_40239;
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
goto label_45832;
}
else 
{
s__rwstate = 1;
goto label_39989;
}
}
else 
{
label_39989:; 
s__state = s__s3__tmp__next_state___0;
goto label_40239;
}
}
else 
{
if (s__state == 8576)
{
goto label_39957;
}
else 
{
if (s__state == 8577)
{
label_39957:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45832;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_39973;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45832;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_39973:; 
goto label_40239;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_39943;
}
else 
{
if (s__state == 8593)
{
label_39943:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45832;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_40239;
}
}
else 
{
if (s__state == 8608)
{
goto label_39930;
}
else 
{
if (s__state == 8609)
{
label_39930:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45832;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_40239:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45832;
}
else 
{
goto label_40299;
}
}
else 
{
label_40299:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_40359;
}
else 
{
goto label_40359;
}
}
else 
{
goto label_40359;
}
}
}
else 
{
goto label_40359;
}
}
else 
{
label_40359:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_40781;
}
else 
{
if (s__state == 16384)
{
label_40781:; 
goto label_40783;
}
else 
{
if (s__state == 8192)
{
label_40783:; 
goto label_40785;
}
else 
{
if (s__state == 24576)
{
label_40785:; 
goto label_40787;
}
else 
{
if (s__state == 8195)
{
label_40787:; 
s__server = 1;
if (cb != 0)
{
goto label_40792;
}
else 
{
label_40792:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46435 = -1;
goto label_46436;
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
goto label_45834;
}
else 
{
s__init_buf___0 = buf;
goto label_40804;
}
}
else 
{
label_40804:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45834;
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
goto label_45834;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_40824;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_40824:; 
goto label_40826;
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
goto label_40760;
}
else 
{
if (s__state == 8481)
{
label_40760:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45834;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_40826;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_40826;
}
else 
{
if (s__state == 8464)
{
goto label_40739;
}
else 
{
if (s__state == 8465)
{
label_40739:; 
goto label_40741;
}
else 
{
if (s__state == 8466)
{
label_40741:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_40892:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46297;
}
else 
{
label_46297:; 
 __return_46373 = ret;
}
tmp = __return_46373;
goto label_46465;
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
goto label_40892;
}
else 
{
goto label_40884;
}
}
else 
{
label_40884:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_40944;
}
else 
{
goto label_40944;
}
}
else 
{
goto label_40944;
}
}
}
else 
{
goto label_40944;
}
}
else 
{
label_40944:; 
skip = 0;
goto label_34925;
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
goto label_40712;
}
else 
{
if (s__state == 8513)
{
label_40712:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45834;
}
else 
{
goto label_40722;
}
}
else 
{
skip = 1;
label_40722:; 
s__state = 8528;
s__init_num = 0;
goto label_40826;
}
}
else 
{
if (s__state == 8528)
{
goto label_40654;
}
else 
{
if (s__state == 8529)
{
label_40654:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_40663;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_40663:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_40703;
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
label_40699:; 
label_40703:; 
s__state = 8544;
s__init_num = 0;
goto label_40826;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_40686;
}
else 
{
tmp___7 = 512;
label_40686:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_40699;
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
goto label_40602;
}
else 
{
if (s__state == 8545)
{
label_40602:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_40647;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_40616;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_40639:; 
label_40647:; 
goto label_40826;
}
}
else 
{
label_40616:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_40630;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_40639;
}
else 
{
label_40630:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45834;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_40639;
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
goto label_40587;
}
else 
{
if (s__state == 8561)
{
label_40587:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45834;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_40826;
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
goto label_45834;
}
else 
{
s__rwstate = 1;
goto label_40576;
}
}
else 
{
label_40576:; 
s__state = s__s3__tmp__next_state___0;
goto label_40826;
}
}
else 
{
if (s__state == 8576)
{
goto label_40544;
}
else 
{
if (s__state == 8577)
{
label_40544:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45834;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_40560;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45834;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_40560:; 
goto label_40826;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_40530;
}
else 
{
if (s__state == 8593)
{
label_40530:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45834;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_40826;
}
}
else 
{
if (s__state == 8608)
{
goto label_40517;
}
else 
{
if (s__state == 8609)
{
label_40517:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45834;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_40826:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45834;
}
else 
{
goto label_40886;
}
}
else 
{
label_40886:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_40946;
}
else 
{
goto label_40946;
}
}
else 
{
goto label_40946;
}
}
}
else 
{
goto label_40946;
}
}
else 
{
label_40946:; 
skip = 0;
label_41442:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_41855;
}
else 
{
if (s__state == 16384)
{
label_41855:; 
goto label_41857;
}
else 
{
if (s__state == 8192)
{
label_41857:; 
goto label_41859;
}
else 
{
if (s__state == 24576)
{
label_41859:; 
goto label_41861;
}
else 
{
if (s__state == 8195)
{
label_41861:; 
s__server = 1;
if (cb != 0)
{
goto label_41866;
}
else 
{
label_41866:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46431 = -1;
goto label_46432;
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
goto label_45838;
}
else 
{
s__init_buf___0 = buf;
goto label_41878;
}
}
else 
{
label_41878:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45838;
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
goto label_45838;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_41898;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_41898:; 
goto label_41900;
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
goto label_41834;
}
else 
{
if (s__state == 8481)
{
label_41834:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45838;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_41900;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_41900;
}
else 
{
if (s__state == 8464)
{
goto label_41813;
}
else 
{
if (s__state == 8465)
{
label_41813:; 
goto label_41815;
}
else 
{
if (s__state == 8466)
{
label_41815:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_41966:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46289;
}
else 
{
label_46289:; 
 __return_46377 = ret;
}
tmp = __return_46377;
goto label_46465;
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
goto label_41966;
}
else 
{
goto label_41958;
}
}
else 
{
label_41958:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_42018;
}
else 
{
goto label_42018;
}
}
else 
{
goto label_42018;
}
}
}
else 
{
goto label_42018;
}
}
else 
{
label_42018:; 
skip = 0;
goto label_34925;
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
goto label_41786;
}
else 
{
if (s__state == 8513)
{
label_41786:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45838;
}
else 
{
goto label_41796;
}
}
else 
{
skip = 1;
label_41796:; 
s__state = 8528;
s__init_num = 0;
goto label_41900;
}
}
else 
{
if (s__state == 8528)
{
goto label_41728;
}
else 
{
if (s__state == 8529)
{
label_41728:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_41737;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_41737:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_41777;
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
label_41773:; 
label_41777:; 
s__state = 8544;
s__init_num = 0;
goto label_41900;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_41760;
}
else 
{
tmp___7 = 512;
label_41760:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_41773;
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
goto label_41676;
}
else 
{
if (s__state == 8545)
{
label_41676:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_41721;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_41690;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_41713:; 
label_41721:; 
goto label_41900;
}
}
else 
{
label_41690:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_41704;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_41713;
}
else 
{
label_41704:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45838;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_41713;
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
goto label_41661;
}
else 
{
if (s__state == 8561)
{
label_41661:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45838;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_41900;
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
goto label_45838;
}
else 
{
s__rwstate = 1;
goto label_41650;
}
}
else 
{
label_41650:; 
s__state = s__s3__tmp__next_state___0;
goto label_41900;
}
}
else 
{
if (s__state == 8576)
{
goto label_41618;
}
else 
{
if (s__state == 8577)
{
label_41618:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45838;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_41634;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45838;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_41634:; 
goto label_41900;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_41604;
}
else 
{
if (s__state == 8593)
{
label_41604:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45838;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_41900;
}
}
else 
{
if (s__state == 8608)
{
goto label_41591;
}
else 
{
if (s__state == 8609)
{
label_41591:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45838;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_41900:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45838;
}
else 
{
goto label_41960;
}
}
else 
{
label_41960:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_42020;
}
else 
{
goto label_42020;
}
}
else 
{
goto label_42020;
}
}
}
else 
{
goto label_42020;
}
}
else 
{
label_42020:; 
skip = 0;
goto label_41442;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_41573;
}
else 
{
if (s__state == 8641)
{
label_41573:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_41964:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46291;
}
else 
{
label_46291:; 
 __return_46376 = ret;
}
tmp = __return_46376;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_41583;
}
else 
{
s__state = 3;
label_41583:; 
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
goto label_41964;
}
else 
{
goto label_41956;
}
}
else 
{
label_41956:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_42016;
}
else 
{
goto label_42016;
}
}
else 
{
goto label_42016;
}
}
}
else 
{
goto label_42016;
}
}
else 
{
label_42016:; 
skip = 0;
label_42027:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_42402;
}
else 
{
if (s__state == 16384)
{
label_42402:; 
goto label_42404;
}
else 
{
if (s__state == 8192)
{
label_42404:; 
goto label_42406;
}
else 
{
if (s__state == 24576)
{
label_42406:; 
goto label_42408;
}
else 
{
if (s__state == 8195)
{
label_42408:; 
s__server = 1;
if (cb != 0)
{
goto label_42413;
}
else 
{
label_42413:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46429 = -1;
goto label_46430;
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
goto label_45840;
}
else 
{
s__init_buf___0 = buf;
goto label_42425;
}
}
else 
{
label_42425:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45840;
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
goto label_45840;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_42445;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_42445:; 
goto label_42447;
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
goto label_42381;
}
else 
{
if (s__state == 8481)
{
label_42381:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45840;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_42447;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_42447;
}
else 
{
if (s__state == 8464)
{
goto label_42368;
}
else 
{
if (s__state == 8465)
{
label_42368:; 
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
goto label_42348;
}
else 
{
if (s__state == 8513)
{
label_42348:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_42447;
}
else 
{
if (s__state == 8528)
{
goto label_42290;
}
else 
{
if (s__state == 8529)
{
label_42290:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_42299;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_42299:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_42339;
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
label_42335:; 
label_42339:; 
s__state = 8544;
s__init_num = 0;
goto label_42447;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_42322;
}
else 
{
tmp___7 = 512;
label_42322:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_42335;
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
goto label_42248;
}
else 
{
if (s__state == 8545)
{
label_42248:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_42283;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_42262;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_42274:; 
label_42283:; 
goto label_42447;
}
}
else 
{
label_42262:; 
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
goto label_42274;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_42233;
}
else 
{
if (s__state == 8561)
{
label_42233:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45840;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_42447;
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
goto label_45840;
}
else 
{
s__rwstate = 1;
goto label_42222;
}
}
else 
{
label_42222:; 
s__state = s__s3__tmp__next_state___0;
goto label_42447;
}
}
else 
{
if (s__state == 8576)
{
goto label_42190;
}
else 
{
if (s__state == 8577)
{
label_42190:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45840;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_42206;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45840;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_42206:; 
goto label_42447;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_42176;
}
else 
{
if (s__state == 8593)
{
label_42176:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45840;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_42447;
}
}
else 
{
if (s__state == 8608)
{
goto label_42163;
}
else 
{
if (s__state == 8609)
{
label_42163:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45840;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_42447:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45840;
}
else 
{
goto label_42477;
}
}
else 
{
label_42477:; 
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
goto label_42133;
}
else 
{
if (s__state == 8657)
{
label_42133:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45840;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_42479;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_42479:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46287;
}
else 
{
label_46287:; 
 __return_46378 = ret;
}
tmp = __return_46378;
goto label_46469;
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
goto label_42479;
}
else 
{
goto label_42475;
}
}
else 
{
label_42475:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_42505;
}
else 
{
goto label_42505;
}
}
else 
{
goto label_42505;
}
}
}
else 
{
goto label_42505;
}
}
else 
{
label_42505:; 
skip = 0;
goto label_36034;
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
goto label_42120;
}
else 
{
goto label_42120;
}
}
else 
{
label_42120:; 
ret = 1;
goto label_45840;
}
}
else 
{
ret = -1;
label_45840:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46217;
}
else 
{
label_46217:; 
 __return_46430 = ret;
label_46430:; 
}
tmp = __return_46430;
goto label_46475;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_41548;
}
else 
{
if (s__state == 8657)
{
label_41548:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45838;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_41962;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_41962:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46293;
}
else 
{
label_46293:; 
 __return_46375 = ret;
}
tmp = __return_46375;
goto label_46469;
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
goto label_41962;
}
else 
{
goto label_41954;
}
}
else 
{
label_41954:; 
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
goto label_41535;
}
else 
{
goto label_41535;
}
}
else 
{
label_41535:; 
ret = 1;
goto label_45838;
}
}
else 
{
ret = -1;
label_45838:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46219;
}
else 
{
label_46219:; 
 __return_46432 = ret;
label_46432:; 
}
tmp = __return_46432;
goto label_46479;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_40499;
}
else 
{
if (s__state == 8641)
{
label_40499:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_40890:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46299;
}
else 
{
label_46299:; 
 __return_46372 = ret;
}
tmp = __return_46372;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_40509;
}
else 
{
s__state = 3;
label_40509:; 
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
goto label_40890;
}
else 
{
goto label_40882;
}
}
else 
{
label_40882:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_40942;
}
else 
{
goto label_40942;
}
}
else 
{
goto label_40942;
}
}
}
else 
{
goto label_40942;
}
}
else 
{
label_40942:; 
skip = 0;
label_40953:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_41328;
}
else 
{
if (s__state == 16384)
{
label_41328:; 
goto label_41330;
}
else 
{
if (s__state == 8192)
{
label_41330:; 
goto label_41332;
}
else 
{
if (s__state == 24576)
{
label_41332:; 
goto label_41334;
}
else 
{
if (s__state == 8195)
{
label_41334:; 
s__server = 1;
if (cb != 0)
{
goto label_41339;
}
else 
{
label_41339:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46433 = -1;
goto label_46434;
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
goto label_45836;
}
else 
{
s__init_buf___0 = buf;
goto label_41351;
}
}
else 
{
label_41351:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45836;
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
goto label_45836;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_41371;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_41371:; 
goto label_41373;
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
goto label_41307;
}
else 
{
if (s__state == 8481)
{
label_41307:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45836;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_41373;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_41373;
}
else 
{
if (s__state == 8464)
{
goto label_41294;
}
else 
{
if (s__state == 8465)
{
label_41294:; 
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
goto label_41274;
}
else 
{
if (s__state == 8513)
{
label_41274:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_41373;
}
else 
{
if (s__state == 8528)
{
goto label_41216;
}
else 
{
if (s__state == 8529)
{
label_41216:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_41225;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_41225:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_41265;
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
label_41261:; 
label_41265:; 
s__state = 8544;
s__init_num = 0;
goto label_41373;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_41248;
}
else 
{
tmp___7 = 512;
label_41248:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_41261;
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
goto label_41174;
}
else 
{
if (s__state == 8545)
{
label_41174:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_41209;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_41188;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_41200:; 
label_41209:; 
goto label_41373;
}
}
else 
{
label_41188:; 
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
goto label_41200;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_41159;
}
else 
{
if (s__state == 8561)
{
label_41159:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45836;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_41373;
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
goto label_45836;
}
else 
{
s__rwstate = 1;
goto label_41148;
}
}
else 
{
label_41148:; 
s__state = s__s3__tmp__next_state___0;
goto label_41373;
}
}
else 
{
if (s__state == 8576)
{
goto label_41116;
}
else 
{
if (s__state == 8577)
{
label_41116:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45836;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_41132;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45836;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_41132:; 
goto label_41373;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_41102;
}
else 
{
if (s__state == 8593)
{
label_41102:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45836;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_41373;
}
}
else 
{
if (s__state == 8608)
{
goto label_41089;
}
else 
{
if (s__state == 8609)
{
label_41089:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45836;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_41373:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45836;
}
else 
{
goto label_41403;
}
}
else 
{
label_41403:; 
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
goto label_41059;
}
else 
{
if (s__state == 8657)
{
label_41059:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45836;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_41405;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_41405:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46295;
}
else 
{
label_46295:; 
 __return_46374 = ret;
}
tmp = __return_46374;
goto label_46469;
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
goto label_41405;
}
else 
{
goto label_41401;
}
}
else 
{
label_41401:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_41431;
}
else 
{
goto label_41431;
}
}
else 
{
goto label_41431;
}
}
}
else 
{
goto label_41431;
}
}
else 
{
label_41431:; 
skip = 0;
goto label_36034;
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
goto label_41046;
}
else 
{
goto label_41046;
}
}
else 
{
label_41046:; 
ret = 1;
goto label_45836;
}
}
else 
{
ret = -1;
label_45836:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46221;
}
else 
{
label_46221:; 
 __return_46434 = ret;
label_46434:; 
}
tmp = __return_46434;
goto label_46475;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_40474;
}
else 
{
if (s__state == 8657)
{
label_40474:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45834;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_40888;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_40888:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46301;
}
else 
{
label_46301:; 
 __return_46371 = ret;
}
tmp = __return_46371;
goto label_46469;
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
goto label_40888;
}
else 
{
goto label_40880;
}
}
else 
{
label_40880:; 
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
goto label_40461;
}
else 
{
goto label_40461;
}
}
else 
{
label_40461:; 
ret = 1;
goto label_45834;
}
}
else 
{
ret = -1;
label_45834:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46223;
}
else 
{
label_46223:; 
 __return_46436 = ret;
label_46436:; 
}
tmp = __return_46436;
goto label_46479;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_39912;
}
else 
{
if (s__state == 8641)
{
label_39912:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_40303:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46305;
}
else 
{
label_46305:; 
 __return_46369 = ret;
}
tmp = __return_46369;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_39922;
}
else 
{
s__state = 3;
label_39922:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_40303;
}
else 
{
goto label_40295;
}
}
else 
{
label_40295:; 
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
goto label_39887;
}
else 
{
if (s__state == 8657)
{
label_39887:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45832;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_40301;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_40301:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46307;
}
else 
{
label_46307:; 
 __return_46368 = ret;
}
tmp = __return_46368;
goto label_46469;
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
goto label_40301;
}
else 
{
goto label_40293;
}
}
else 
{
label_40293:; 
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
goto label_39874;
}
else 
{
goto label_39874;
}
}
else 
{
label_39874:; 
ret = 1;
goto label_45832;
}
}
else 
{
ret = -1;
label_45832:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46225;
}
else 
{
label_46225:; 
 __return_46438 = ret;
label_46438:; 
}
tmp = __return_46438;
goto label_46479;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_39325;
}
else 
{
if (s__state == 8641)
{
label_39325:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_39716:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46311;
}
else 
{
label_46311:; 
 __return_46366 = ret;
}
tmp = __return_46366;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_39335;
}
else 
{
s__state = 3;
label_39335:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_39716;
}
else 
{
goto label_39708;
}
}
else 
{
label_39708:; 
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
goto label_39300;
}
else 
{
if (s__state == 8657)
{
label_39300:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45830;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_39714;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_39714:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46313;
}
else 
{
label_46313:; 
 __return_46365 = ret;
}
tmp = __return_46365;
goto label_46469;
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
goto label_39714;
}
else 
{
goto label_39706;
}
}
else 
{
label_39706:; 
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
goto label_39287;
}
else 
{
goto label_39287;
}
}
else 
{
label_39287:; 
ret = 1;
goto label_45830;
}
}
else 
{
ret = -1;
label_45830:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46227;
}
else 
{
label_46227:; 
 __return_46440 = ret;
label_46440:; 
}
tmp = __return_46440;
goto label_46479;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_38738;
}
else 
{
if (s__state == 8641)
{
label_38738:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_39129:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46317;
}
else 
{
label_46317:; 
 __return_46363 = ret;
}
tmp = __return_46363;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_38748;
}
else 
{
s__state = 3;
label_38748:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_39129;
}
else 
{
goto label_39121;
}
}
else 
{
label_39121:; 
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
goto label_38713;
}
else 
{
if (s__state == 8657)
{
label_38713:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45828;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_39127;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_39127:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46319;
}
else 
{
label_46319:; 
 __return_46362 = ret;
}
tmp = __return_46362;
goto label_46469;
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
goto label_39127;
}
else 
{
goto label_39119;
}
}
else 
{
label_39119:; 
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
goto label_38700;
}
else 
{
goto label_38700;
}
}
else 
{
label_38700:; 
ret = 1;
goto label_45828;
}
}
else 
{
ret = -1;
label_45828:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46229;
}
else 
{
label_46229:; 
 __return_46442 = ret;
label_46442:; 
}
tmp = __return_46442;
label_46479:; 
 __return_46605 = tmp;
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
goto label_38214;
}
else 
{
if (s__state == 8545)
{
label_38214:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_38259;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_38228;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_38251:; 
label_38259:; 
goto label_38448;
}
}
else 
{
label_38228:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_38242;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_38251;
}
else 
{
label_38242:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45826;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_38251;
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
goto label_38199;
}
else 
{
if (s__state == 8561)
{
label_38199:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45826;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_38448;
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
goto label_45826;
}
else 
{
s__rwstate = 1;
goto label_38188;
}
}
else 
{
label_38188:; 
s__state = s__s3__tmp__next_state___0;
goto label_38448;
}
}
else 
{
if (s__state == 8576)
{
goto label_38156;
}
else 
{
if (s__state == 8577)
{
label_38156:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45826;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_38172;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45826;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_38172:; 
goto label_38448;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_38142;
}
else 
{
if (s__state == 8593)
{
label_38142:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45826;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_38448;
}
}
else 
{
if (s__state == 8608)
{
goto label_38129;
}
else 
{
if (s__state == 8609)
{
label_38129:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45826;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_38448:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45826;
}
else 
{
goto label_38523;
}
}
else 
{
label_38523:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_38598;
}
else 
{
goto label_38598;
}
}
else 
{
goto label_38598;
}
}
}
else 
{
goto label_38598;
}
}
else 
{
label_38598:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_42942;
}
else 
{
if (s__state == 16384)
{
label_42942:; 
goto label_42944;
}
else 
{
if (s__state == 8192)
{
label_42944:; 
goto label_42946;
}
else 
{
if (s__state == 24576)
{
label_42946:; 
goto label_42948;
}
else 
{
if (s__state == 8195)
{
label_42948:; 
s__server = 1;
if (cb != 0)
{
goto label_42953;
}
else 
{
label_42953:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46427 = -1;
goto label_46428;
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
goto label_45842;
}
else 
{
s__init_buf___0 = buf;
goto label_42965;
}
}
else 
{
label_42965:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45842;
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
goto label_45842;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_42985;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_42985:; 
goto label_42987;
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
goto label_42921;
}
else 
{
if (s__state == 8481)
{
label_42921:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45842;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_42987;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_42987;
}
else 
{
if (s__state == 8464)
{
goto label_42900;
}
else 
{
if (s__state == 8465)
{
label_42900:; 
goto label_42902;
}
else 
{
if (s__state == 8466)
{
label_42902:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_43070:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46279;
}
else 
{
label_46279:; 
 __return_46382 = ret;
}
tmp = __return_46382;
goto label_46465;
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
goto label_43070;
}
else 
{
goto label_43060;
}
}
else 
{
label_43060:; 
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
goto label_42873;
}
else 
{
if (s__state == 8513)
{
label_42873:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45842;
}
else 
{
goto label_42883;
}
}
else 
{
skip = 1;
label_42883:; 
s__state = 8528;
s__init_num = 0;
goto label_42987;
}
}
else 
{
if (s__state == 8528)
{
goto label_42805;
}
else 
{
if (s__state == 8529)
{
label_42805:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_42814;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_42814:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_42861;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_42848;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_42855:; 
label_42861:; 
s__state = 8544;
s__init_num = 0;
goto label_42987;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_42837;
}
else 
{
tmp___7 = 512;
label_42837:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_42848;
}
else 
{
skip = 1;
goto label_42855;
}
}
}
}
}
}
else 
{
goto label_42848;
}
}
else 
{
label_42848:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_43068:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46281;
}
else 
{
label_46281:; 
 __return_46381 = ret;
}
tmp = __return_46381;
goto label_46479;
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
goto label_43068;
}
else 
{
goto label_43058;
}
}
else 
{
label_43058:; 
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
goto label_42753;
}
else 
{
if (s__state == 8545)
{
label_42753:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_42798;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_42767;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_42790:; 
label_42798:; 
goto label_42987;
}
}
else 
{
label_42767:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_42781;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_42790;
}
else 
{
label_42781:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45842;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_42790;
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
goto label_42738;
}
else 
{
if (s__state == 8561)
{
label_42738:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45842;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_42987;
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
goto label_45842;
}
else 
{
s__rwstate = 1;
goto label_42727;
}
}
else 
{
label_42727:; 
s__state = s__s3__tmp__next_state___0;
goto label_42987;
}
}
else 
{
if (s__state == 8576)
{
goto label_42695;
}
else 
{
if (s__state == 8577)
{
label_42695:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45842;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_42711;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45842;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_42711:; 
goto label_42987;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_42681;
}
else 
{
if (s__state == 8593)
{
label_42681:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45842;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_42987;
}
}
else 
{
if (s__state == 8608)
{
goto label_42668;
}
else 
{
if (s__state == 8609)
{
label_42668:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45842;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_42987:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45842;
}
else 
{
goto label_43062;
}
}
else 
{
label_43062:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_43137;
}
else 
{
goto label_43137;
}
}
else 
{
goto label_43137;
}
}
}
else 
{
goto label_43137;
}
}
else 
{
label_43137:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_43571;
}
else 
{
if (s__state == 16384)
{
label_43571:; 
goto label_43573;
}
else 
{
if (s__state == 8192)
{
label_43573:; 
goto label_43575;
}
else 
{
if (s__state == 24576)
{
label_43575:; 
goto label_43577;
}
else 
{
if (s__state == 8195)
{
label_43577:; 
s__server = 1;
if (cb != 0)
{
goto label_43582;
}
else 
{
label_43582:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46425 = -1;
goto label_46426;
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
goto label_45844;
}
else 
{
s__init_buf___0 = buf;
goto label_43594;
}
}
else 
{
label_43594:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45844;
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
goto label_45844;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_43614;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_43614:; 
goto label_43616;
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
goto label_43550;
}
else 
{
if (s__state == 8481)
{
label_43550:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45844;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_43616;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_43616;
}
else 
{
if (s__state == 8464)
{
goto label_43529;
}
else 
{
if (s__state == 8465)
{
label_43529:; 
goto label_43531;
}
else 
{
if (s__state == 8466)
{
label_43531:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_43699:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46271;
}
else 
{
label_46271:; 
 __return_46386 = ret;
}
tmp = __return_46386;
goto label_46465;
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
goto label_43699;
}
else 
{
goto label_43689;
}
}
else 
{
label_43689:; 
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
goto label_43502;
}
else 
{
if (s__state == 8513)
{
label_43502:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45844;
}
else 
{
goto label_43512;
}
}
else 
{
skip = 1;
label_43512:; 
s__state = 8528;
s__init_num = 0;
goto label_43616;
}
}
else 
{
if (s__state == 8528)
{
goto label_43434;
}
else 
{
if (s__state == 8529)
{
label_43434:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_43443;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_43443:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_43490;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_43477;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_43484:; 
label_43490:; 
s__state = 8544;
s__init_num = 0;
goto label_43616;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_43466;
}
else 
{
tmp___7 = 512;
label_43466:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_43477;
}
else 
{
skip = 1;
goto label_43484;
}
}
}
}
}
}
else 
{
goto label_43477;
}
}
else 
{
label_43477:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_43697:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46273;
}
else 
{
label_46273:; 
 __return_46385 = ret;
}
tmp = __return_46385;
goto label_46479;
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
goto label_43697;
}
else 
{
goto label_43687;
}
}
else 
{
label_43687:; 
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
goto label_43382;
}
else 
{
if (s__state == 8545)
{
label_43382:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_43427;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_43396;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_43419:; 
label_43427:; 
goto label_43616;
}
}
else 
{
label_43396:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_43410;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_43419;
}
else 
{
label_43410:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45844;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_43419;
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
goto label_43367;
}
else 
{
if (s__state == 8561)
{
label_43367:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45844;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_43616;
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
goto label_45844;
}
else 
{
s__rwstate = 1;
goto label_43356;
}
}
else 
{
label_43356:; 
s__state = s__s3__tmp__next_state___0;
goto label_43616;
}
}
else 
{
if (s__state == 8576)
{
goto label_43324;
}
else 
{
if (s__state == 8577)
{
label_43324:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45844;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_43340;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45844;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_43340:; 
goto label_43616;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_43310;
}
else 
{
if (s__state == 8593)
{
label_43310:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45844;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_43616;
}
}
else 
{
if (s__state == 8608)
{
goto label_43297;
}
else 
{
if (s__state == 8609)
{
label_43297:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45844;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_43616:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45844;
}
else 
{
goto label_43691;
}
}
else 
{
label_43691:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_43766;
}
else 
{
goto label_43766;
}
}
else 
{
goto label_43766;
}
}
}
else 
{
goto label_43766;
}
}
else 
{
label_43766:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_44200;
}
else 
{
if (s__state == 16384)
{
label_44200:; 
goto label_44202;
}
else 
{
if (s__state == 8192)
{
label_44202:; 
goto label_44204;
}
else 
{
if (s__state == 24576)
{
label_44204:; 
goto label_44206;
}
else 
{
if (s__state == 8195)
{
label_44206:; 
s__server = 1;
if (cb != 0)
{
goto label_44211;
}
else 
{
label_44211:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46423 = -1;
goto label_46424;
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
goto label_45846;
}
else 
{
s__init_buf___0 = buf;
goto label_44223;
}
}
else 
{
label_44223:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45846;
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
goto label_45846;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_44243;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_44243:; 
goto label_44245;
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
goto label_44179;
}
else 
{
if (s__state == 8481)
{
label_44179:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45846;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_44245;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_44245;
}
else 
{
if (s__state == 8464)
{
goto label_44158;
}
else 
{
if (s__state == 8465)
{
label_44158:; 
goto label_44160;
}
else 
{
if (s__state == 8466)
{
label_44160:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_44328:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46263;
}
else 
{
label_46263:; 
 __return_46390 = ret;
}
tmp = __return_46390;
goto label_46465;
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
goto label_44328;
}
else 
{
goto label_44318;
}
}
else 
{
label_44318:; 
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
goto label_44131;
}
else 
{
if (s__state == 8513)
{
label_44131:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45846;
}
else 
{
goto label_44141;
}
}
else 
{
skip = 1;
label_44141:; 
s__state = 8528;
s__init_num = 0;
goto label_44245;
}
}
else 
{
if (s__state == 8528)
{
goto label_44063;
}
else 
{
if (s__state == 8529)
{
label_44063:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_44072;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_44072:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_44119;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_44106;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_44113:; 
label_44119:; 
s__state = 8544;
s__init_num = 0;
goto label_44245;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_44095;
}
else 
{
tmp___7 = 512;
label_44095:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_44106;
}
else 
{
skip = 1;
goto label_44113;
}
}
}
}
}
}
else 
{
goto label_44106;
}
}
else 
{
label_44106:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_44326:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46265;
}
else 
{
label_46265:; 
 __return_46389 = ret;
}
tmp = __return_46389;
goto label_46479;
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
goto label_44326;
}
else 
{
goto label_44316;
}
}
else 
{
label_44316:; 
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
goto label_44011;
}
else 
{
if (s__state == 8545)
{
label_44011:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_44056;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_44025;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_44048:; 
label_44056:; 
goto label_44245;
}
}
else 
{
label_44025:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_44039;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_44048;
}
else 
{
label_44039:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45846;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_44048;
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
goto label_43996;
}
else 
{
if (s__state == 8561)
{
label_43996:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45846;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_44245;
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
goto label_45846;
}
else 
{
s__rwstate = 1;
goto label_43985;
}
}
else 
{
label_43985:; 
s__state = s__s3__tmp__next_state___0;
goto label_44245;
}
}
else 
{
if (s__state == 8576)
{
goto label_43953;
}
else 
{
if (s__state == 8577)
{
label_43953:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45846;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_43969;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45846;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_43969:; 
goto label_44245;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_43939;
}
else 
{
if (s__state == 8593)
{
label_43939:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45846;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_44245;
}
}
else 
{
if (s__state == 8608)
{
goto label_43926;
}
else 
{
if (s__state == 8609)
{
label_43926:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45846;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_44245:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45846;
}
else 
{
goto label_44320;
}
}
else 
{
label_44320:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_44395;
}
else 
{
goto label_44395;
}
}
else 
{
goto label_44395;
}
}
}
else 
{
goto label_44395;
}
}
else 
{
label_44395:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_44829;
}
else 
{
if (s__state == 16384)
{
label_44829:; 
goto label_44831;
}
else 
{
if (s__state == 8192)
{
label_44831:; 
goto label_44833;
}
else 
{
if (s__state == 24576)
{
label_44833:; 
goto label_44835;
}
else 
{
if (s__state == 8195)
{
label_44835:; 
s__server = 1;
if (cb != 0)
{
goto label_44840;
}
else 
{
label_44840:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46421 = -1;
goto label_46422;
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
goto label_45848;
}
else 
{
s__init_buf___0 = buf;
goto label_44852;
}
}
else 
{
label_44852:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45848;
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
goto label_45848;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_44872;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_44872:; 
goto label_44874;
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
goto label_44808;
}
else 
{
if (s__state == 8481)
{
label_44808:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45848;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_44874;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_44874;
}
else 
{
if (s__state == 8464)
{
goto label_44787;
}
else 
{
if (s__state == 8465)
{
label_44787:; 
goto label_44789;
}
else 
{
if (s__state == 8466)
{
label_44789:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_44957:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46255;
}
else 
{
label_46255:; 
 __return_46394 = ret;
}
tmp = __return_46394;
goto label_46465;
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
goto label_44957;
}
else 
{
goto label_44947;
}
}
else 
{
label_44947:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_45022;
}
else 
{
goto label_45022;
}
}
else 
{
goto label_45022;
}
}
}
else 
{
goto label_45022;
}
}
else 
{
label_45022:; 
skip = 0;
goto label_34925;
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
goto label_44760;
}
else 
{
if (s__state == 8513)
{
label_44760:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45848;
}
else 
{
goto label_44770;
}
}
else 
{
skip = 1;
label_44770:; 
s__state = 8528;
s__init_num = 0;
goto label_44874;
}
}
else 
{
if (s__state == 8528)
{
goto label_44692;
}
else 
{
if (s__state == 8529)
{
label_44692:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_44701;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_44701:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_44748;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_44735;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_44742:; 
label_44748:; 
s__state = 8544;
s__init_num = 0;
goto label_44874;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_44724;
}
else 
{
tmp___7 = 512;
label_44724:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_44735;
}
else 
{
skip = 1;
goto label_44742;
}
}
}
}
}
}
else 
{
goto label_44735;
}
}
else 
{
label_44735:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_44955:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46257;
}
else 
{
label_46257:; 
 __return_46393 = ret;
}
tmp = __return_46393;
goto label_46479;
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
goto label_44955;
}
else 
{
goto label_44945;
}
}
else 
{
label_44945:; 
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
goto label_44640;
}
else 
{
if (s__state == 8545)
{
label_44640:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_44685;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_44654;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_44677:; 
label_44685:; 
goto label_44874;
}
}
else 
{
label_44654:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_44668;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_44677;
}
else 
{
label_44668:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45848;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_44677;
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
goto label_44625;
}
else 
{
if (s__state == 8561)
{
label_44625:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45848;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_44874;
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
goto label_45848;
}
else 
{
s__rwstate = 1;
goto label_44614;
}
}
else 
{
label_44614:; 
s__state = s__s3__tmp__next_state___0;
goto label_44874;
}
}
else 
{
if (s__state == 8576)
{
goto label_44582;
}
else 
{
if (s__state == 8577)
{
label_44582:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45848;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_44598;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45848;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_44598:; 
goto label_44874;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_44568;
}
else 
{
if (s__state == 8593)
{
label_44568:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45848;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_44874;
}
}
else 
{
if (s__state == 8608)
{
goto label_44555;
}
else 
{
if (s__state == 8609)
{
label_44555:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45848;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_44874:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45848;
}
else 
{
goto label_44949;
}
}
else 
{
label_44949:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_45024;
}
else 
{
goto label_45024;
}
}
else 
{
goto label_45024;
}
}
}
else 
{
goto label_45024;
}
}
else 
{
label_45024:; 
skip = 0;
label_45037:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_45460;
}
else 
{
if (s__state == 16384)
{
label_45460:; 
goto label_45462;
}
else 
{
if (s__state == 8192)
{
label_45462:; 
goto label_45464;
}
else 
{
if (s__state == 24576)
{
label_45464:; 
goto label_45466;
}
else 
{
if (s__state == 8195)
{
label_45466:; 
s__server = 1;
if (cb != 0)
{
goto label_45471;
}
else 
{
label_45471:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46419 = -1;
goto label_46420;
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
goto label_45850;
}
else 
{
s__init_buf___0 = buf;
goto label_45483;
}
}
else 
{
label_45483:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45850;
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
goto label_45850;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_45503;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_45503:; 
goto label_45505;
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
goto label_45439;
}
else 
{
if (s__state == 8481)
{
label_45439:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45850;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_45505;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_45505;
}
else 
{
if (s__state == 8464)
{
goto label_45418;
}
else 
{
if (s__state == 8465)
{
label_45418:; 
goto label_45420;
}
else 
{
if (s__state == 8466)
{
label_45420:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_45588:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46247;
}
else 
{
label_46247:; 
 __return_46398 = ret;
}
tmp = __return_46398;
goto label_46465;
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
goto label_45588;
}
else 
{
goto label_45578;
}
}
else 
{
label_45578:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_45653;
}
else 
{
goto label_45653;
}
}
else 
{
goto label_45653;
}
}
}
else 
{
goto label_45653;
}
}
else 
{
label_45653:; 
skip = 0;
goto label_34925;
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
goto label_45391;
}
else 
{
if (s__state == 8513)
{
label_45391:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_45850;
}
else 
{
goto label_45401;
}
}
else 
{
skip = 1;
label_45401:; 
s__state = 8528;
s__init_num = 0;
goto label_45505;
}
}
else 
{
if (s__state == 8528)
{
goto label_45323;
}
else 
{
if (s__state == 8529)
{
label_45323:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_45332;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_45332:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_45379;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_45366;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_45373:; 
label_45379:; 
s__state = 8544;
s__init_num = 0;
goto label_45505;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_45355;
}
else 
{
tmp___7 = 512;
label_45355:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_45366;
}
else 
{
skip = 1;
goto label_45373;
}
}
}
}
}
}
else 
{
goto label_45366;
}
}
else 
{
label_45366:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_45586:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46249;
}
else 
{
label_46249:; 
 __return_46397 = ret;
}
tmp = __return_46397;
goto label_46479;
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
goto label_45586;
}
else 
{
goto label_45576;
}
}
else 
{
label_45576:; 
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
goto label_45271;
}
else 
{
if (s__state == 8545)
{
label_45271:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_45316;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_45285;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_45308:; 
label_45316:; 
goto label_45505;
}
}
else 
{
label_45285:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_45299;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_45308;
}
else 
{
label_45299:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_45850;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_45308;
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
goto label_45256;
}
else 
{
if (s__state == 8561)
{
label_45256:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45850;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_45505;
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
goto label_45850;
}
else 
{
s__rwstate = 1;
goto label_45245;
}
}
else 
{
label_45245:; 
s__state = s__s3__tmp__next_state___0;
goto label_45505;
}
}
else 
{
if (s__state == 8576)
{
goto label_45213;
}
else 
{
if (s__state == 8577)
{
label_45213:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45850;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_45229;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45850;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_45229:; 
goto label_45505;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_45199;
}
else 
{
if (s__state == 8593)
{
label_45199:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45850;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_45505;
}
}
else 
{
if (s__state == 8608)
{
goto label_45186;
}
else 
{
if (s__state == 8609)
{
label_45186:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45850;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_45505:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45850;
}
else 
{
goto label_45580;
}
}
else 
{
label_45580:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_45655;
}
else 
{
goto label_45655;
}
}
else 
{
goto label_45655;
}
}
}
else 
{
goto label_45655;
}
}
else 
{
label_45655:; 
skip = 0;
goto label_45037;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_45168;
}
else 
{
if (s__state == 8641)
{
label_45168:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_45584:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46251;
}
else 
{
label_46251:; 
 __return_46396 = ret;
}
tmp = __return_46396;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_45178;
}
else 
{
s__state = 3;
label_45178:; 
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
goto label_45584;
}
else 
{
goto label_45574;
}
}
else 
{
label_45574:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_45649;
}
else 
{
goto label_45649;
}
}
else 
{
goto label_45649;
}
}
}
else 
{
goto label_45649;
}
}
else 
{
label_45649:; 
skip = 0;
goto label_42027;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_45143;
}
else 
{
if (s__state == 8657)
{
label_45143:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45850;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_45582;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_45582:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46253;
}
else 
{
label_46253:; 
 __return_46395 = ret;
}
tmp = __return_46395;
goto label_46469;
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
goto label_45582;
}
else 
{
goto label_45572;
}
}
else 
{
label_45572:; 
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
goto label_45130;
}
else 
{
goto label_45130;
}
}
else 
{
label_45130:; 
ret = 1;
goto label_45850;
}
}
else 
{
ret = -1;
label_45850:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46207;
}
else 
{
label_46207:; 
 __return_46420 = ret;
label_46420:; 
}
tmp = __return_46420;
goto label_46467;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_44537;
}
else 
{
if (s__state == 8641)
{
label_44537:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_44953:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46259;
}
else 
{
label_46259:; 
 __return_46392 = ret;
}
tmp = __return_46392;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_44547;
}
else 
{
s__state = 3;
label_44547:; 
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
goto label_44953;
}
else 
{
goto label_44943;
}
}
else 
{
label_44943:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_45018;
}
else 
{
goto label_45018;
}
}
else 
{
goto label_45018;
}
}
}
else 
{
goto label_45018;
}
}
else 
{
label_45018:; 
skip = 0;
goto label_40953;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_44512;
}
else 
{
if (s__state == 8657)
{
label_44512:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45848;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_44951;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_44951:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46261;
}
else 
{
label_46261:; 
 __return_46391 = ret;
}
tmp = __return_46391;
goto label_46469;
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
goto label_44951;
}
else 
{
goto label_44941;
}
}
else 
{
label_44941:; 
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
goto label_44499;
}
else 
{
goto label_44499;
}
}
else 
{
label_44499:; 
ret = 1;
goto label_45848;
}
}
else 
{
ret = -1;
label_45848:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46209;
}
else 
{
label_46209:; 
 __return_46422 = ret;
label_46422:; 
}
tmp = __return_46422;
goto label_46467;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_43908;
}
else 
{
if (s__state == 8641)
{
label_43908:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_44324:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46267;
}
else 
{
label_46267:; 
 __return_46388 = ret;
}
tmp = __return_46388;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_43918;
}
else 
{
s__state = 3;
label_43918:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_44324;
}
else 
{
goto label_44314;
}
}
else 
{
label_44314:; 
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
goto label_43883;
}
else 
{
if (s__state == 8657)
{
label_43883:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45846;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_44322;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_44322:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46269;
}
else 
{
label_46269:; 
 __return_46387 = ret;
}
tmp = __return_46387;
goto label_46469;
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
goto label_44322;
}
else 
{
goto label_44312;
}
}
else 
{
label_44312:; 
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
goto label_43870;
}
else 
{
goto label_43870;
}
}
else 
{
label_43870:; 
ret = 1;
goto label_45846;
}
}
else 
{
ret = -1;
label_45846:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46211;
}
else 
{
label_46211:; 
 __return_46424 = ret;
label_46424:; 
}
tmp = __return_46424;
goto label_46467;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_43279;
}
else 
{
if (s__state == 8641)
{
label_43279:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_43695:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46275;
}
else 
{
label_46275:; 
 __return_46384 = ret;
}
tmp = __return_46384;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_43289;
}
else 
{
s__state = 3;
label_43289:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_43695;
}
else 
{
goto label_43685;
}
}
else 
{
label_43685:; 
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
goto label_43254;
}
else 
{
if (s__state == 8657)
{
label_43254:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45844;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_43693;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_43693:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46277;
}
else 
{
label_46277:; 
 __return_46383 = ret;
}
tmp = __return_46383;
goto label_46469;
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
goto label_43693;
}
else 
{
goto label_43683;
}
}
else 
{
label_43683:; 
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
goto label_43241;
}
else 
{
goto label_43241;
}
}
else 
{
label_43241:; 
ret = 1;
goto label_45844;
}
}
else 
{
ret = -1;
label_45844:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46213;
}
else 
{
label_46213:; 
 __return_46426 = ret;
label_46426:; 
}
tmp = __return_46426;
goto label_46467;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_42650;
}
else 
{
if (s__state == 8641)
{
label_42650:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_43066:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46283;
}
else 
{
label_46283:; 
 __return_46380 = ret;
}
tmp = __return_46380;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_42660;
}
else 
{
s__state = 3;
label_42660:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_43066;
}
else 
{
goto label_43056;
}
}
else 
{
label_43056:; 
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
goto label_42625;
}
else 
{
if (s__state == 8657)
{
label_42625:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45842;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_43064;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_43064:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46285;
}
else 
{
label_46285:; 
 __return_46379 = ret;
}
tmp = __return_46379;
goto label_46469;
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
goto label_43064;
}
else 
{
goto label_43054;
}
}
else 
{
label_43054:; 
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
goto label_42612;
}
else 
{
goto label_42612;
}
}
else 
{
label_42612:; 
ret = 1;
goto label_45842;
}
}
else 
{
ret = -1;
label_45842:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46215;
}
else 
{
label_46215:; 
 __return_46428 = ret;
label_46428:; 
}
tmp = __return_46428;
goto label_46467;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_38111;
}
else 
{
if (s__state == 8641)
{
label_38111:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_38527:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46325;
}
else 
{
label_46325:; 
 __return_46359 = ret;
}
tmp = __return_46359;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_38121;
}
else 
{
s__state = 3;
label_38121:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38527;
}
else 
{
goto label_38517;
}
}
else 
{
label_38517:; 
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
goto label_38086;
}
else 
{
if (s__state == 8657)
{
label_38086:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45826;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_38525;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_38525:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46327;
}
else 
{
label_46327:; 
 __return_46358 = ret;
}
tmp = __return_46358;
goto label_46469;
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
goto label_38525;
}
else 
{
goto label_38515;
}
}
else 
{
label_38515:; 
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
goto label_38073;
}
else 
{
goto label_38073;
}
}
else 
{
label_38073:; 
ret = 1;
goto label_45826;
}
}
else 
{
ret = -1;
label_45826:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46231;
}
else 
{
label_46231:; 
 __return_46444 = ret;
label_46444:; 
}
tmp = __return_46444;
goto label_46467;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_35540;
}
else 
{
if (s__state == 8641)
{
label_35540:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_35956:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46341;
}
else 
{
label_46341:; 
 __return_46351 = ret;
}
tmp = __return_46351;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_35550;
}
else 
{
s__state = 3;
label_35550:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_35956;
}
else 
{
goto label_35946;
}
}
else 
{
label_35946:; 
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
goto label_35515;
}
else 
{
if (s__state == 8657)
{
label_35515:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45816;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_35954;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_35954:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46343;
}
else 
{
label_46343:; 
 __return_46350 = ret;
}
tmp = __return_46350;
goto label_46469;
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
goto label_35954;
}
else 
{
goto label_35944;
}
}
else 
{
label_35944:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36019;
}
else 
{
goto label_36019;
}
}
else 
{
goto label_36019;
}
}
}
else 
{
goto label_36019;
}
}
else 
{
label_36019:; 
skip = 0;
label_36034:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_36410;
}
else 
{
if (s__state == 16384)
{
label_36410:; 
goto label_36412;
}
else 
{
if (s__state == 8192)
{
label_36412:; 
goto label_36414;
}
else 
{
if (s__state == 24576)
{
label_36414:; 
goto label_36416;
}
else 
{
if (s__state == 8195)
{
label_36416:; 
s__server = 1;
if (cb != 0)
{
goto label_36421;
}
else 
{
label_36421:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46451 = -1;
goto label_46452;
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
goto label_45818;
}
else 
{
s__init_buf___0 = buf;
goto label_36433;
}
}
else 
{
label_36433:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45818;
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
goto label_45818;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_36453;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_36453:; 
goto label_36455;
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
goto label_36389;
}
else 
{
if (s__state == 8481)
{
label_36389:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45818;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_36455;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_36455;
}
else 
{
if (s__state == 8464)
{
goto label_36376;
}
else 
{
if (s__state == 8465)
{
label_36376:; 
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
goto label_36356;
}
else 
{
if (s__state == 8513)
{
label_36356:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_36455;
}
else 
{
if (s__state == 8528)
{
goto label_36298;
}
else 
{
if (s__state == 8529)
{
label_36298:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_36307;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_36307:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_36347;
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
label_36343:; 
label_36347:; 
s__state = 8544;
s__init_num = 0;
goto label_36455;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_36330;
}
else 
{
tmp___7 = 512;
label_36330:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_36343;
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
goto label_36256;
}
else 
{
if (s__state == 8545)
{
label_36256:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_36291;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_36270;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_36282:; 
label_36291:; 
goto label_36455;
}
}
else 
{
label_36270:; 
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
goto label_36282;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_36241;
}
else 
{
if (s__state == 8561)
{
label_36241:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45818;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_36455;
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
goto label_45818;
}
else 
{
s__rwstate = 1;
goto label_36230;
}
}
else 
{
label_36230:; 
s__state = s__s3__tmp__next_state___0;
goto label_36455;
}
}
else 
{
if (s__state == 8576)
{
goto label_36198;
}
else 
{
if (s__state == 8577)
{
label_36198:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45818;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_36214;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45818;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_36214:; 
goto label_36455;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_36184;
}
else 
{
if (s__state == 8593)
{
label_36184:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45818;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_36455;
}
}
else 
{
if (s__state == 8608)
{
goto label_36171;
}
else 
{
if (s__state == 8609)
{
label_36171:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45818;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_36455:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45818;
}
else 
{
goto label_36485;
}
}
else 
{
label_36485:; 
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
goto label_36154;
}
else 
{
if (s__state == 8657)
{
label_36154:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_45818;
}
else 
{
if (s__state == 8672)
{
goto label_36135;
}
else 
{
if (s__state == 8673)
{
label_36135:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_36487:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46335;
}
else 
{
label_46335:; 
 __return_46354 = ret;
}
tmp = __return_46354;
goto label_46471;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_36146;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_36146:; 
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
goto label_36487;
}
else 
{
goto label_36483;
}
}
else 
{
label_36483:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36513;
}
else 
{
goto label_36513;
}
}
else 
{
goto label_36513;
}
}
}
else 
{
goto label_36513;
}
}
else 
{
label_36513:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_36894;
}
else 
{
if (s__state == 16384)
{
label_36894:; 
goto label_36896;
}
else 
{
if (s__state == 8192)
{
label_36896:; 
goto label_36898;
}
else 
{
if (s__state == 24576)
{
label_36898:; 
goto label_36900;
}
else 
{
if (s__state == 8195)
{
label_36900:; 
s__server = 1;
if (cb != 0)
{
goto label_36905;
}
else 
{
label_36905:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46449 = -1;
goto label_46450;
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
goto label_45820;
}
else 
{
s__init_buf___0 = buf;
goto label_36917;
}
}
else 
{
label_36917:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45820;
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
goto label_45820;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_36937;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_36937:; 
goto label_36939;
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
goto label_36873;
}
else 
{
if (s__state == 8481)
{
label_36873:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45820;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_36939;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_36939;
}
else 
{
if (s__state == 8464)
{
goto label_36860;
}
else 
{
if (s__state == 8465)
{
label_36860:; 
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
goto label_36840;
}
else 
{
if (s__state == 8513)
{
label_36840:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_36939;
}
else 
{
if (s__state == 8528)
{
goto label_36782;
}
else 
{
if (s__state == 8529)
{
label_36782:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_36791;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_36791:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_36831;
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
label_36827:; 
label_36831:; 
s__state = 8544;
s__init_num = 0;
goto label_36939;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_36814;
}
else 
{
tmp___7 = 512;
label_36814:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_36827;
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
goto label_36740;
}
else 
{
if (s__state == 8545)
{
label_36740:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_36775;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_36754;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_36766:; 
label_36775:; 
goto label_36939;
}
}
else 
{
label_36754:; 
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
goto label_36766;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_36725;
}
else 
{
if (s__state == 8561)
{
label_36725:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45820;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_36939;
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
goto label_45820;
}
else 
{
s__rwstate = 1;
goto label_36714;
}
}
else 
{
label_36714:; 
s__state = s__s3__tmp__next_state___0;
goto label_36939;
}
}
else 
{
if (s__state == 8576)
{
goto label_36682;
}
else 
{
if (s__state == 8577)
{
label_36682:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45820;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_36698;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45820;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_36698:; 
goto label_36939;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_36668;
}
else 
{
if (s__state == 8593)
{
label_36668:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45820;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_36939;
}
}
else 
{
if (s__state == 8608)
{
goto label_36655;
}
else 
{
if (s__state == 8609)
{
label_36655:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45820;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_36939:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45820;
}
else 
{
goto label_36969;
}
}
else 
{
label_36969:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36999;
}
else 
{
goto label_36999;
}
}
else 
{
goto label_36999;
}
}
}
else 
{
goto label_36999;
}
}
else 
{
label_36999:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_37379;
}
else 
{
if (s__state == 16384)
{
label_37379:; 
goto label_37381;
}
else 
{
if (s__state == 8192)
{
label_37381:; 
goto label_37383;
}
else 
{
if (s__state == 24576)
{
label_37383:; 
goto label_37385;
}
else 
{
if (s__state == 8195)
{
label_37385:; 
s__server = 1;
if (cb != 0)
{
goto label_37390;
}
else 
{
label_37390:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46447 = -1;
goto label_46448;
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
goto label_45822;
}
else 
{
s__init_buf___0 = buf;
goto label_37402;
}
}
else 
{
label_37402:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45822;
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
goto label_45822;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_37422;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_37422:; 
goto label_37424;
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
goto label_37358;
}
else 
{
if (s__state == 8481)
{
label_37358:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45822;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_37424;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_37424;
}
else 
{
if (s__state == 8464)
{
goto label_37345;
}
else 
{
if (s__state == 8465)
{
label_37345:; 
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
goto label_37325;
}
else 
{
if (s__state == 8513)
{
label_37325:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_37424;
}
else 
{
if (s__state == 8528)
{
goto label_37267;
}
else 
{
if (s__state == 8529)
{
label_37267:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_37276;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_37276:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_37316;
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
label_37312:; 
label_37316:; 
s__state = 8544;
s__init_num = 0;
goto label_37424;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_37299;
}
else 
{
tmp___7 = 512;
label_37299:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_37312;
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
goto label_37225;
}
else 
{
if (s__state == 8545)
{
label_37225:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_37260;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_37239;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_37251:; 
label_37260:; 
goto label_37424;
}
}
else 
{
label_37239:; 
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
goto label_37251;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_37210;
}
else 
{
if (s__state == 8561)
{
label_37210:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45822;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_37424;
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
goto label_45822;
}
else 
{
s__rwstate = 1;
goto label_37199;
}
}
else 
{
label_37199:; 
s__state = s__s3__tmp__next_state___0;
goto label_37424;
}
}
else 
{
if (s__state == 8576)
{
goto label_37167;
}
else 
{
if (s__state == 8577)
{
label_37167:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45822;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_37183;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45822;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_37183:; 
goto label_37424;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_37153;
}
else 
{
if (s__state == 8593)
{
label_37153:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45822;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_37424;
}
}
else 
{
if (s__state == 8608)
{
goto label_37140;
}
else 
{
if (s__state == 8609)
{
label_37140:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45822;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_37424:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45822;
}
else 
{
goto label_37454;
}
}
else 
{
label_37454:; 
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
goto label_37122;
}
else 
{
if (s__state == 8641)
{
label_37122:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_37456:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46331;
}
else 
{
label_46331:; 
 __return_46356 = ret;
}
tmp = __return_46356;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_37132;
}
else 
{
s__state = 3;
label_37132:; 
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
goto label_37456;
}
else 
{
goto label_37452;
}
}
else 
{
label_37452:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_37482;
}
else 
{
goto label_37482;
}
}
else 
{
goto label_37482;
}
}
}
else 
{
goto label_37482;
}
}
else 
{
label_37482:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_37863;
}
else 
{
if (s__state == 16384)
{
label_37863:; 
goto label_37865;
}
else 
{
if (s__state == 8192)
{
label_37865:; 
goto label_37867;
}
else 
{
if (s__state == 24576)
{
label_37867:; 
goto label_37869;
}
else 
{
if (s__state == 8195)
{
label_37869:; 
s__server = 1;
if (cb != 0)
{
goto label_37874;
}
else 
{
label_37874:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_46445 = -1;
goto label_46446;
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
goto label_45824;
}
else 
{
s__init_buf___0 = buf;
goto label_37886;
}
}
else 
{
label_37886:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_45824;
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
goto label_45824;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_37906;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_37906:; 
goto label_37908;
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
goto label_37842;
}
else 
{
if (s__state == 8481)
{
label_37842:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_45824;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_37908;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_37908;
}
else 
{
if (s__state == 8464)
{
goto label_37829;
}
else 
{
if (s__state == 8465)
{
label_37829:; 
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
goto label_37809;
}
else 
{
if (s__state == 8513)
{
label_37809:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_37908;
}
else 
{
if (s__state == 8528)
{
goto label_37751;
}
else 
{
if (s__state == 8529)
{
label_37751:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_37760;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_37760:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_37800;
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
label_37796:; 
label_37800:; 
s__state = 8544;
s__init_num = 0;
goto label_37908;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_37783;
}
else 
{
tmp___7 = 512;
label_37783:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_37796;
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
goto label_37709;
}
else 
{
if (s__state == 8545)
{
label_37709:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_37744;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_37723;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_37735:; 
label_37744:; 
goto label_37908;
}
}
else 
{
label_37723:; 
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
goto label_37735;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_37694;
}
else 
{
if (s__state == 8561)
{
label_37694:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45824;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_37908;
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
goto label_45824;
}
else 
{
s__rwstate = 1;
goto label_37683;
}
}
else 
{
label_37683:; 
s__state = s__s3__tmp__next_state___0;
goto label_37908;
}
}
else 
{
if (s__state == 8576)
{
goto label_37651;
}
else 
{
if (s__state == 8577)
{
label_37651:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45824;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_37667;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45824;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_37667:; 
goto label_37908;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_37637;
}
else 
{
if (s__state == 8593)
{
label_37637:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45824;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_37908;
}
}
else 
{
if (s__state == 8608)
{
goto label_37624;
}
else 
{
if (s__state == 8609)
{
label_37624:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45824;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_37908:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45824;
}
else 
{
goto label_37938;
}
}
else 
{
label_37938:; 
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
goto label_37594;
}
else 
{
if (s__state == 8657)
{
label_37594:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_45824;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_37940;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_37940:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46329;
}
else 
{
label_46329:; 
 __return_46357 = ret;
}
tmp = __return_46357;
goto label_46469;
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
goto label_37940;
}
else 
{
goto label_37936;
}
}
else 
{
label_37936:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_37966;
}
else 
{
goto label_37966;
}
}
else 
{
goto label_37966;
}
}
}
else 
{
goto label_37966;
}
}
else 
{
label_37966:; 
skip = 0;
goto label_36034;
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
goto label_37581;
}
else 
{
goto label_37581;
}
}
else 
{
label_37581:; 
ret = 1;
goto label_45824;
}
}
else 
{
ret = -1;
label_45824:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46233;
}
else 
{
label_46233:; 
 __return_46446 = ret;
label_46446:; 
}
tmp = __return_46446;
label_46475:; 
 __return_46607 = tmp;
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
goto label_37110;
}
else 
{
if (s__state == 8657)
{
label_37110:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_45822;
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
goto label_37097;
}
else 
{
goto label_37097;
}
}
else 
{
label_37097:; 
ret = 1;
goto label_45822;
}
}
else 
{
ret = -1;
label_45822:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46235;
}
else 
{
label_46235:; 
 __return_46448 = ret;
label_46448:; 
}
tmp = __return_46448;
goto label_46471;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_36637;
}
else 
{
if (s__state == 8641)
{
label_36637:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_36971:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46333;
}
else 
{
label_46333:; 
 __return_46355 = ret;
}
tmp = __return_46355;
goto label_46475;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_36647;
}
else 
{
s__state = 3;
label_36647:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_36971;
}
else 
{
goto label_36967;
}
}
else 
{
label_36967:; 
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
goto label_36625;
}
else 
{
if (s__state == 8657)
{
label_36625:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_45820;
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
goto label_36612;
}
else 
{
goto label_36612;
}
}
else 
{
label_36612:; 
ret = 1;
goto label_45820;
}
}
else 
{
ret = -1;
label_45820:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46237;
}
else 
{
label_46237:; 
 __return_46450 = ret;
label_46450:; 
}
tmp = __return_46450;
label_46471:; 
 __return_46609 = tmp;
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
goto label_36127;
}
else 
{
goto label_36127;
}
}
else 
{
label_36127:; 
ret = 1;
goto label_45818;
}
}
else 
{
ret = -1;
label_45818:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46239;
}
else 
{
label_46239:; 
 __return_46452 = ret;
label_46452:; 
}
tmp = __return_46452;
label_46469:; 
 __return_46611 = tmp;
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
goto label_35502;
}
else 
{
goto label_35502;
}
}
else 
{
label_35502:; 
ret = 1;
goto label_45816;
}
}
else 
{
ret = -1;
label_45816:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46241;
}
else 
{
label_46241:; 
 __return_46454 = ret;
label_46454:; 
}
tmp = __return_46454;
label_46467:; 
 __return_46613 = tmp;
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
goto label_35233;
}
else 
{
if (s__state == 8513)
{
label_35233:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_35345;
}
else 
{
if (s__state == 8528)
{
goto label_35175;
}
else 
{
if (s__state == 8529)
{
label_35175:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_35184;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_35184:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_35224;
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
label_35220:; 
label_35224:; 
s__state = 8544;
s__init_num = 0;
goto label_35345;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_35207;
}
else 
{
tmp___7 = 512;
label_35207:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_35220;
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
goto label_35133;
}
else 
{
if (s__state == 8545)
{
label_35133:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35168;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_35147;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_35159:; 
label_35168:; 
goto label_35345;
}
}
else 
{
label_35147:; 
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
goto label_35159;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_35118;
}
else 
{
if (s__state == 8561)
{
label_35118:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45814;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_35345;
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
goto label_45814;
}
else 
{
s__rwstate = 1;
goto label_35107;
}
}
else 
{
label_35107:; 
s__state = s__s3__tmp__next_state___0;
goto label_35345;
}
}
else 
{
if (s__state == 8576)
{
goto label_35075;
}
else 
{
if (s__state == 8577)
{
label_35075:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45814;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_35091;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45814;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_35091:; 
goto label_35345;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_35061;
}
else 
{
if (s__state == 8593)
{
label_35061:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45814;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_35345;
}
}
else 
{
if (s__state == 8608)
{
goto label_35048;
}
else 
{
if (s__state == 8609)
{
label_35048:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45814;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_35345:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45814;
}
else 
{
goto label_35375;
}
}
else 
{
label_35375:; 
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
goto label_35031;
}
else 
{
if (s__state == 8657)
{
label_35031:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_45814;
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
goto label_35018;
}
else 
{
goto label_35018;
}
}
else 
{
label_35018:; 
ret = 1;
goto label_45814;
}
}
else 
{
ret = -1;
label_45814:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46243;
}
else 
{
label_46243:; 
 __return_46456 = ret;
label_46456:; 
}
tmp = __return_46456;
label_46465:; 
 __return_46615 = tmp;
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
goto label_34754;
}
else 
{
if (s__state == 8513)
{
label_34754:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_34861;
}
else 
{
if (s__state == 8528)
{
goto label_34696;
}
else 
{
if (s__state == 8529)
{
label_34696:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_34705;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_34705:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_34745;
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
label_34741:; 
label_34745:; 
s__state = 8544;
s__init_num = 0;
goto label_34861;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_34728;
}
else 
{
tmp___7 = 512;
label_34728:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_34741;
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
goto label_34654;
}
else 
{
if (s__state == 8545)
{
label_34654:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_34689;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_34668;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_34680:; 
label_34689:; 
goto label_34861;
}
}
else 
{
label_34668:; 
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
goto label_34680;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_34639;
}
else 
{
if (s__state == 8561)
{
label_34639:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_45812;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_34861;
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
goto label_45812;
}
else 
{
s__rwstate = 1;
goto label_34628;
}
}
else 
{
label_34628:; 
s__state = s__s3__tmp__next_state___0;
goto label_34861;
}
}
else 
{
if (s__state == 8576)
{
goto label_34596;
}
else 
{
if (s__state == 8577)
{
label_34596:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_45812;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_34612;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_45812;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_34612:; 
goto label_34861;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_34582;
}
else 
{
if (s__state == 8593)
{
label_34582:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_45812;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_34861;
}
}
else 
{
if (s__state == 8608)
{
goto label_34569;
}
else 
{
if (s__state == 8609)
{
label_34569:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_45812;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_34861:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_45812;
}
else 
{
goto label_34891;
}
}
else 
{
label_34891:; 
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
goto label_34552;
}
else 
{
if (s__state == 8657)
{
label_34552:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_45812;
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
goto label_34539;
}
else 
{
goto label_34539;
}
}
else 
{
label_34539:; 
ret = 1;
goto label_45812;
}
}
else 
{
ret = -1;
label_45812:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_46245;
}
else 
{
label_46245:; 
 __return_46458 = ret;
label_46458:; 
}
tmp = __return_46458;
goto label_46461;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_46459;
label_46461:; 
 __return_46617 = tmp;
return 1;
}
}
}
}
