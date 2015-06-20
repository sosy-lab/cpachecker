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
int __return_39180;
int __return_39372;
int __return_39178;
int __return_39040;
int __return_39176;
int __return_39041;
int __return_39174;
int __return_39045;
int __return_39044;
int __return_39164;
int __return_39053;
int __return_39052;
int __return_39162;
int __return_39056;
int __return_39160;
int __return_39059;
int __return_39158;
int __return_39062;
int __return_39156;
int __return_39065;
int __return_39154;
int __return_39068;
int __return_39152;
int __return_39071;
int __return_39070;
int __return_39150;
int __return_39072;
int __return_39148;
int __return_39073;
int __return_39146;
int __return_39144;
int __return_39075;
int __return_39142;
int __return_39076;
int __return_39140;
int __return_39077;
int __return_39141;
int __return_39143;
int __return_39145;
int __return_39074;
int __return_39147;
int __return_39149;
int __return_39151;
int __return_39069;
int __return_39153;
int __return_39067;
int __return_39066;
int __return_39155;
int __return_39064;
int __return_39063;
int __return_39157;
int __return_39061;
int __return_39060;
int __return_39159;
int __return_39058;
int __return_39057;
int __return_39161;
int __return_39055;
int __return_39054;
int __return_39163;
int __return_39360;
int __return_39138;
int __return_39081;
int __return_39080;
int __return_39136;
int __return_39085;
int __return_39084;
int __return_39134;
int __return_39089;
int __return_39088;
int __return_39132;
int __return_39093;
int __return_39092;
int __return_39130;
int __return_39097;
int __return_39096;
int __return_39128;
int __return_39101;
int __return_39100;
int __return_39099;
int __return_39098;
int __return_39129;
int __return_39095;
int __return_39094;
int __return_39131;
int __return_39091;
int __return_39090;
int __return_39133;
int __return_39087;
int __return_39086;
int __return_39135;
int __return_39083;
int __return_39082;
int __return_39137;
int __return_39079;
int __return_39078;
int __return_39139;
int __return_39051;
int __return_39050;
int __return_39165;
int __return_39043;
int __return_39042;
int __return_39172;
int __return_39046;
int __return_39170;
int __return_39168;
int __return_39048;
int __return_39166;
int __return_39049;
int __return_39167;
int __return_39362;
int __return_39169;
int __return_39047;
int __return_39171;
int __return_39364;
int __return_39173;
int __return_39366;
int __return_39175;
int __return_39368;
int __return_39177;
int __return_39370;
int __return_39179;
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
goto label_341;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_341;
}
else 
{
label_341:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_39180 = -1;
}
else 
{
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_719;
}
else 
{
if (s__state == 16384)
{
label_719:; 
goto label_721;
}
else 
{
if (s__state == 8192)
{
label_721:; 
goto label_723;
}
else 
{
if (s__state == 24576)
{
label_723:; 
goto label_725;
}
else 
{
if (s__state == 8195)
{
label_725:; 
s__server = 1;
if (cb != 0)
{
goto label_730;
}
else 
{
label_730:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39178 = -1;
goto label_39179;
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
goto label_38373;
}
else 
{
s__init_buf___0 = buf;
goto label_742;
}
}
else 
{
label_742:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38373;
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
goto label_38373;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_762;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_762:; 
goto label_764;
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
goto label_698;
}
else 
{
if (s__state == 8481)
{
label_698:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38373;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_764;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_764;
}
else 
{
if (s__state == 8464)
{
goto label_677;
}
else 
{
if (s__state == 8465)
{
label_677:; 
goto label_679;
}
else 
{
if (s__state == 8466)
{
label_679:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_796:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39039;
}
else 
{
label_39039:; 
 __return_39040 = ret;
}
tmp = __return_39040;
goto label_39186;
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
goto label_796;
}
else 
{
goto label_792;
}
}
else 
{
label_792:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_822;
}
else 
{
goto label_822;
}
}
else 
{
goto label_822;
}
}
}
else 
{
goto label_822;
}
}
else 
{
label_822:; 
skip = 0;
label_11521:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_11896;
}
else 
{
if (s__state == 16384)
{
label_11896:; 
goto label_11898;
}
else 
{
if (s__state == 8192)
{
label_11898:; 
goto label_11900;
}
else 
{
if (s__state == 24576)
{
label_11900:; 
goto label_11902;
}
else 
{
if (s__state == 8195)
{
label_11902:; 
s__server = 1;
if (cb != 0)
{
goto label_11907;
}
else 
{
label_11907:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39176 = -1;
goto label_39177;
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
goto label_38375;
}
else 
{
s__init_buf___0 = buf;
goto label_11919;
}
}
else 
{
label_11919:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38375;
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
goto label_38375;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_11939;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_11939:; 
goto label_11941;
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
goto label_11875;
}
else 
{
if (s__state == 8481)
{
label_11875:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38375;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_11941;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_11941;
}
else 
{
if (s__state == 8464)
{
goto label_11862;
}
else 
{
if (s__state == 8465)
{
label_11862:; 
return 1;
}
else 
{
if (s__state == 8496)
{
goto label_11843;
}
else 
{
if (s__state == 8497)
{
label_11843:; 
ret = ssl3_send_server_hello();
if (ret <= 0)
{
label_11973:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39037;
}
else 
{
label_39037:; 
 __return_39041 = ret;
}
tmp = __return_39041;
goto label_39188;
}
else 
{
if (s__hit == 0)
{
s__state = 8512;
goto label_11853;
}
else 
{
s__state = 8656;
label_11853:; 
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
goto label_11973;
}
else 
{
goto label_11969;
}
}
else 
{
label_11969:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_11999;
}
else 
{
goto label_11999;
}
}
else 
{
goto label_11999;
}
}
}
else 
{
goto label_11999;
}
}
else 
{
label_11999:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_12428;
}
else 
{
if (s__state == 16384)
{
label_12428:; 
goto label_12430;
}
else 
{
if (s__state == 8192)
{
label_12430:; 
goto label_12432;
}
else 
{
if (s__state == 24576)
{
label_12432:; 
goto label_12434;
}
else 
{
if (s__state == 8195)
{
label_12434:; 
s__server = 1;
if (cb != 0)
{
goto label_12439;
}
else 
{
label_12439:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39174 = -1;
goto label_39175;
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
goto label_38377;
}
else 
{
s__init_buf___0 = buf;
goto label_12451;
}
}
else 
{
label_12451:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38377;
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
goto label_38377;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_12471;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_12471:; 
goto label_12473;
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
goto label_12407;
}
else 
{
if (s__state == 8481)
{
label_12407:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38377;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_12473;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_12473;
}
else 
{
if (s__state == 8464)
{
goto label_12386;
}
else 
{
if (s__state == 8465)
{
label_12386:; 
goto label_12388;
}
else 
{
if (s__state == 8466)
{
label_12388:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_12556:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39029;
}
else 
{
label_39029:; 
 __return_39045 = ret;
}
tmp = __return_39045;
goto label_39186;
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
goto label_12556;
}
else 
{
goto label_12546;
}
}
else 
{
label_12546:; 
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
goto label_12359;
}
else 
{
if (s__state == 8513)
{
label_12359:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38377;
}
else 
{
goto label_12369;
}
}
else 
{
skip = 1;
label_12369:; 
s__state = 8528;
s__init_num = 0;
goto label_12473;
}
}
else 
{
if (s__state == 8528)
{
goto label_12291;
}
else 
{
if (s__state == 8529)
{
label_12291:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_12300;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_12300:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_12347;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_12334;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_12341:; 
label_12347:; 
s__state = 8544;
s__init_num = 0;
goto label_12473;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_12323;
}
else 
{
tmp___7 = 512;
label_12323:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_12334;
}
else 
{
skip = 1;
goto label_12341;
}
}
}
}
}
}
else 
{
goto label_12334;
}
}
else 
{
label_12334:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_12554:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39031;
}
else 
{
label_39031:; 
 __return_39044 = ret;
}
tmp = __return_39044;
goto label_39200;
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
goto label_12554;
}
else 
{
goto label_12544;
}
}
else 
{
label_12544:; 
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
goto label_12239;
}
else 
{
if (s__state == 8545)
{
label_12239:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_12284;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_12253;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_12276:; 
label_12284:; 
goto label_12473;
}
}
else 
{
label_12253:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_12267;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_12276;
}
else 
{
label_12267:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38377;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_12276;
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
goto label_12224;
}
else 
{
if (s__state == 8561)
{
label_12224:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38377;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_12473;
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
goto label_38377;
}
else 
{
s__rwstate = 1;
goto label_12213;
}
}
else 
{
label_12213:; 
s__state = s__s3__tmp__next_state___0;
goto label_12473;
}
}
else 
{
if (s__state == 8576)
{
goto label_12181;
}
else 
{
if (s__state == 8577)
{
label_12181:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38377;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_12197;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38377;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_12197:; 
goto label_12473;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_12167;
}
else 
{
if (s__state == 8593)
{
label_12167:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38377;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_12473;
}
}
else 
{
if (s__state == 8608)
{
goto label_12154;
}
else 
{
if (s__state == 8609)
{
label_12154:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38377;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_12473:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38377;
}
else 
{
goto label_12548;
}
}
else 
{
label_12548:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12623;
}
else 
{
goto label_12623;
}
}
else 
{
goto label_12623;
}
}
}
else 
{
goto label_12623;
}
}
else 
{
label_12623:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_21573;
}
else 
{
if (s__state == 16384)
{
label_21573:; 
goto label_21575;
}
else 
{
if (s__state == 8192)
{
label_21575:; 
goto label_21577;
}
else 
{
if (s__state == 24576)
{
label_21577:; 
goto label_21579;
}
else 
{
if (s__state == 8195)
{
label_21579:; 
s__server = 1;
if (cb != 0)
{
goto label_21584;
}
else 
{
label_21584:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39164 = -1;
goto label_39165;
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
goto label_38387;
}
else 
{
s__init_buf___0 = buf;
goto label_21596;
}
}
else 
{
label_21596:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38387;
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
goto label_38387;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_21616;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_21616:; 
goto label_21618;
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
goto label_21552;
}
else 
{
if (s__state == 8481)
{
label_21552:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38387;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_21618;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_21618;
}
else 
{
if (s__state == 8464)
{
goto label_21531;
}
else 
{
if (s__state == 8465)
{
label_21531:; 
goto label_21533;
}
else 
{
if (s__state == 8466)
{
label_21533:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_21701:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39013;
}
else 
{
label_39013:; 
 __return_39053 = ret;
}
tmp = __return_39053;
goto label_39186;
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
goto label_21701;
}
else 
{
goto label_21691;
}
}
else 
{
label_21691:; 
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
goto label_21504;
}
else 
{
if (s__state == 8513)
{
label_21504:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38387;
}
else 
{
goto label_21514;
}
}
else 
{
skip = 1;
label_21514:; 
s__state = 8528;
s__init_num = 0;
goto label_21618;
}
}
else 
{
if (s__state == 8528)
{
goto label_21436;
}
else 
{
if (s__state == 8529)
{
label_21436:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_21445;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_21445:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_21492;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_21479;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_21486:; 
label_21492:; 
s__state = 8544;
s__init_num = 0;
goto label_21618;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_21468;
}
else 
{
tmp___7 = 512;
label_21468:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_21479;
}
else 
{
skip = 1;
goto label_21486;
}
}
}
}
}
}
else 
{
goto label_21479;
}
}
else 
{
label_21479:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_21699:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39015;
}
else 
{
label_39015:; 
 __return_39052 = ret;
}
tmp = __return_39052;
goto label_39200;
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
goto label_21699;
}
else 
{
goto label_21689;
}
}
else 
{
label_21689:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21764;
}
else 
{
goto label_21764;
}
}
else 
{
goto label_21764;
}
}
}
else 
{
goto label_21764;
}
}
else 
{
label_21764:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_22190;
}
else 
{
if (s__state == 16384)
{
label_22190:; 
goto label_22192;
}
else 
{
if (s__state == 8192)
{
label_22192:; 
goto label_22194;
}
else 
{
if (s__state == 24576)
{
label_22194:; 
goto label_22196;
}
else 
{
if (s__state == 8195)
{
label_22196:; 
s__server = 1;
if (cb != 0)
{
goto label_22201;
}
else 
{
label_22201:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39162 = -1;
goto label_39163;
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
goto label_38389;
}
else 
{
s__init_buf___0 = buf;
goto label_22213;
}
}
else 
{
label_22213:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38389;
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
goto label_38389;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_22233;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_22233:; 
goto label_22235;
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
goto label_22169;
}
else 
{
if (s__state == 8481)
{
label_22169:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38389;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_22235;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_22235;
}
else 
{
if (s__state == 8464)
{
goto label_22148;
}
else 
{
if (s__state == 8465)
{
label_22148:; 
goto label_22150;
}
else 
{
if (s__state == 8466)
{
label_22150:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_22301:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39007;
}
else 
{
label_39007:; 
 __return_39056 = ret;
}
tmp = __return_39056;
goto label_39186;
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
goto label_22301;
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
goto label_22121;
}
else 
{
if (s__state == 8513)
{
label_22121:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38389;
}
else 
{
goto label_22131;
}
}
else 
{
skip = 1;
label_22131:; 
s__state = 8528;
s__init_num = 0;
goto label_22235;
}
}
else 
{
if (s__state == 8528)
{
goto label_22063;
}
else 
{
if (s__state == 8529)
{
label_22063:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_22072;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_22072:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_22112;
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
label_22108:; 
label_22112:; 
s__state = 8544;
s__init_num = 0;
goto label_22235;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_22095;
}
else 
{
tmp___7 = 512;
label_22095:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_22108;
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
goto label_22011;
}
else 
{
if (s__state == 8545)
{
label_22011:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22056;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_22025;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_22048:; 
label_22056:; 
goto label_22235;
}
}
else 
{
label_22025:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_22039;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22048;
}
else 
{
label_22039:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38389;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_22048;
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
goto label_21996;
}
else 
{
if (s__state == 8561)
{
label_21996:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38389;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_22235;
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
goto label_38389;
}
else 
{
s__rwstate = 1;
goto label_21985;
}
}
else 
{
label_21985:; 
s__state = s__s3__tmp__next_state___0;
goto label_22235;
}
}
else 
{
if (s__state == 8576)
{
goto label_21953;
}
else 
{
if (s__state == 8577)
{
label_21953:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38389;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_21969;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38389;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_21969:; 
goto label_22235;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_21939;
}
else 
{
if (s__state == 8593)
{
label_21939:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38389;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_22235;
}
}
else 
{
if (s__state == 8608)
{
goto label_21926;
}
else 
{
if (s__state == 8609)
{
label_21926:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38389;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_22235:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38389;
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
goto label_22355;
}
else 
{
goto label_22355;
}
}
else 
{
goto label_22355;
}
}
}
else 
{
goto label_22355;
}
}
else 
{
label_22355:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_22777;
}
else 
{
if (s__state == 16384)
{
label_22777:; 
goto label_22779;
}
else 
{
if (s__state == 8192)
{
label_22779:; 
goto label_22781;
}
else 
{
if (s__state == 24576)
{
label_22781:; 
goto label_22783;
}
else 
{
if (s__state == 8195)
{
label_22783:; 
s__server = 1;
if (cb != 0)
{
goto label_22788;
}
else 
{
label_22788:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39160 = -1;
goto label_39161;
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
goto label_38391;
}
else 
{
s__init_buf___0 = buf;
goto label_22800;
}
}
else 
{
label_22800:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38391;
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
goto label_38391;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_22820;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_22820:; 
goto label_22822;
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
goto label_22756;
}
else 
{
if (s__state == 8481)
{
label_22756:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38391;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_22822;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_22822;
}
else 
{
if (s__state == 8464)
{
goto label_22735;
}
else 
{
if (s__state == 8465)
{
label_22735:; 
goto label_22737;
}
else 
{
if (s__state == 8466)
{
label_22737:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_22888:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39001;
}
else 
{
label_39001:; 
 __return_39059 = ret;
}
tmp = __return_39059;
goto label_39186;
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
goto label_22888;
}
else 
{
goto label_22880;
}
}
else 
{
label_22880:; 
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
goto label_22708;
}
else 
{
if (s__state == 8513)
{
label_22708:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38391;
}
else 
{
goto label_22718;
}
}
else 
{
skip = 1;
label_22718:; 
s__state = 8528;
s__init_num = 0;
goto label_22822;
}
}
else 
{
if (s__state == 8528)
{
goto label_22650;
}
else 
{
if (s__state == 8529)
{
label_22650:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_22659;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_22659:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_22699;
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
label_22695:; 
label_22699:; 
s__state = 8544;
s__init_num = 0;
goto label_22822;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_22682;
}
else 
{
tmp___7 = 512;
label_22682:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_22695;
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
goto label_22598;
}
else 
{
if (s__state == 8545)
{
label_22598:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22643;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_22612;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_22635:; 
label_22643:; 
goto label_22822;
}
}
else 
{
label_22612:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_22626;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22635;
}
else 
{
label_22626:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38391;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_22635;
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
goto label_22583;
}
else 
{
if (s__state == 8561)
{
label_22583:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38391;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_22822;
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
goto label_38391;
}
else 
{
s__rwstate = 1;
goto label_22572;
}
}
else 
{
label_22572:; 
s__state = s__s3__tmp__next_state___0;
goto label_22822;
}
}
else 
{
if (s__state == 8576)
{
goto label_22540;
}
else 
{
if (s__state == 8577)
{
label_22540:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38391;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_22556;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38391;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_22556:; 
goto label_22822;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_22526;
}
else 
{
if (s__state == 8593)
{
label_22526:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38391;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_22822;
}
}
else 
{
if (s__state == 8608)
{
goto label_22513;
}
else 
{
if (s__state == 8609)
{
label_22513:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38391;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_22822:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38391;
}
else 
{
goto label_22882;
}
}
else 
{
label_22882:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22942;
}
else 
{
goto label_22942;
}
}
else 
{
goto label_22942;
}
}
}
else 
{
goto label_22942;
}
}
else 
{
label_22942:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_23364;
}
else 
{
if (s__state == 16384)
{
label_23364:; 
goto label_23366;
}
else 
{
if (s__state == 8192)
{
label_23366:; 
goto label_23368;
}
else 
{
if (s__state == 24576)
{
label_23368:; 
goto label_23370;
}
else 
{
if (s__state == 8195)
{
label_23370:; 
s__server = 1;
if (cb != 0)
{
goto label_23375;
}
else 
{
label_23375:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39158 = -1;
goto label_39159;
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
goto label_38393;
}
else 
{
s__init_buf___0 = buf;
goto label_23387;
}
}
else 
{
label_23387:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38393;
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
goto label_38393;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_23407;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_23407:; 
goto label_23409;
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
goto label_23343;
}
else 
{
if (s__state == 8481)
{
label_23343:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38393;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_23409;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_23409;
}
else 
{
if (s__state == 8464)
{
goto label_23322;
}
else 
{
if (s__state == 8465)
{
label_23322:; 
goto label_23324;
}
else 
{
if (s__state == 8466)
{
label_23324:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_23475:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38995;
}
else 
{
label_38995:; 
 __return_39062 = ret;
}
tmp = __return_39062;
goto label_39186;
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
goto label_23475;
}
else 
{
goto label_23467;
}
}
else 
{
label_23467:; 
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
goto label_23295;
}
else 
{
if (s__state == 8513)
{
label_23295:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38393;
}
else 
{
goto label_23305;
}
}
else 
{
skip = 1;
label_23305:; 
s__state = 8528;
s__init_num = 0;
goto label_23409;
}
}
else 
{
if (s__state == 8528)
{
goto label_23237;
}
else 
{
if (s__state == 8529)
{
label_23237:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_23246;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_23246:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_23286;
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
label_23282:; 
label_23286:; 
s__state = 8544;
s__init_num = 0;
goto label_23409;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_23269;
}
else 
{
tmp___7 = 512;
label_23269:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_23282;
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
goto label_23185;
}
else 
{
if (s__state == 8545)
{
label_23185:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_23230;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_23199;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_23222:; 
label_23230:; 
goto label_23409;
}
}
else 
{
label_23199:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_23213;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_23222;
}
else 
{
label_23213:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38393;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_23222;
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
goto label_23170;
}
else 
{
if (s__state == 8561)
{
label_23170:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38393;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_23409;
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
goto label_38393;
}
else 
{
s__rwstate = 1;
goto label_23159;
}
}
else 
{
label_23159:; 
s__state = s__s3__tmp__next_state___0;
goto label_23409;
}
}
else 
{
if (s__state == 8576)
{
goto label_23127;
}
else 
{
if (s__state == 8577)
{
label_23127:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38393;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_23143;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38393;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_23143:; 
goto label_23409;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_23113;
}
else 
{
if (s__state == 8593)
{
label_23113:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38393;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_23409;
}
}
else 
{
if (s__state == 8608)
{
goto label_23100;
}
else 
{
if (s__state == 8609)
{
label_23100:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38393;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_23409:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38393;
}
else 
{
goto label_23469;
}
}
else 
{
label_23469:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_23529;
}
else 
{
goto label_23529;
}
}
else 
{
goto label_23529;
}
}
}
else 
{
goto label_23529;
}
}
else 
{
label_23529:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_23951;
}
else 
{
if (s__state == 16384)
{
label_23951:; 
goto label_23953;
}
else 
{
if (s__state == 8192)
{
label_23953:; 
goto label_23955;
}
else 
{
if (s__state == 24576)
{
label_23955:; 
goto label_23957;
}
else 
{
if (s__state == 8195)
{
label_23957:; 
s__server = 1;
if (cb != 0)
{
goto label_23962;
}
else 
{
label_23962:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39156 = -1;
goto label_39157;
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
goto label_38395;
}
else 
{
s__init_buf___0 = buf;
goto label_23974;
}
}
else 
{
label_23974:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38395;
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
goto label_38395;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_23994;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_23994:; 
goto label_23996;
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
goto label_23930;
}
else 
{
if (s__state == 8481)
{
label_23930:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38395;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_23996;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_23996;
}
else 
{
if (s__state == 8464)
{
goto label_23909;
}
else 
{
if (s__state == 8465)
{
label_23909:; 
goto label_23911;
}
else 
{
if (s__state == 8466)
{
label_23911:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_24062:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38989;
}
else 
{
label_38989:; 
 __return_39065 = ret;
}
tmp = __return_39065;
goto label_39186;
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
goto label_24062;
}
else 
{
goto label_24054;
}
}
else 
{
label_24054:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24114;
}
else 
{
goto label_24114;
}
}
else 
{
goto label_24114;
}
}
}
else 
{
goto label_24114;
}
}
else 
{
label_24114:; 
skip = 0;
goto label_11521;
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
goto label_23882;
}
else 
{
if (s__state == 8513)
{
label_23882:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38395;
}
else 
{
goto label_23892;
}
}
else 
{
skip = 1;
label_23892:; 
s__state = 8528;
s__init_num = 0;
goto label_23996;
}
}
else 
{
if (s__state == 8528)
{
goto label_23824;
}
else 
{
if (s__state == 8529)
{
label_23824:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_23833;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_23833:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_23873;
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
label_23869:; 
label_23873:; 
s__state = 8544;
s__init_num = 0;
goto label_23996;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_23856;
}
else 
{
tmp___7 = 512;
label_23856:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_23869;
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
goto label_23772;
}
else 
{
if (s__state == 8545)
{
label_23772:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_23817;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_23786;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_23809:; 
label_23817:; 
goto label_23996;
}
}
else 
{
label_23786:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_23800;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_23809;
}
else 
{
label_23800:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38395;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_23809;
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
goto label_23757;
}
else 
{
if (s__state == 8561)
{
label_23757:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38395;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_23996;
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
goto label_38395;
}
else 
{
s__rwstate = 1;
goto label_23746;
}
}
else 
{
label_23746:; 
s__state = s__s3__tmp__next_state___0;
goto label_23996;
}
}
else 
{
if (s__state == 8576)
{
goto label_23714;
}
else 
{
if (s__state == 8577)
{
label_23714:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38395;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_23730;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38395;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_23730:; 
goto label_23996;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_23700;
}
else 
{
if (s__state == 8593)
{
label_23700:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38395;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_23996;
}
}
else 
{
if (s__state == 8608)
{
goto label_23687;
}
else 
{
if (s__state == 8609)
{
label_23687:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38395;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_23996:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38395;
}
else 
{
goto label_24056;
}
}
else 
{
label_24056:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24116;
}
else 
{
goto label_24116;
}
}
else 
{
goto label_24116;
}
}
}
else 
{
goto label_24116;
}
}
else 
{
label_24116:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_24539;
}
else 
{
if (s__state == 16384)
{
label_24539:; 
goto label_24541;
}
else 
{
if (s__state == 8192)
{
label_24541:; 
goto label_24543;
}
else 
{
if (s__state == 24576)
{
label_24543:; 
goto label_24545;
}
else 
{
if (s__state == 8195)
{
label_24545:; 
s__server = 1;
if (cb != 0)
{
goto label_24550;
}
else 
{
label_24550:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39154 = -1;
goto label_39155;
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
goto label_38397;
}
else 
{
s__init_buf___0 = buf;
goto label_24562;
}
}
else 
{
label_24562:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38397;
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
goto label_38397;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_24582;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_24582:; 
goto label_24584;
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
goto label_24518;
}
else 
{
if (s__state == 8481)
{
label_24518:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38397;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_24584;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_24584;
}
else 
{
if (s__state == 8464)
{
goto label_24497;
}
else 
{
if (s__state == 8465)
{
label_24497:; 
goto label_24499;
}
else 
{
if (s__state == 8466)
{
label_24499:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_24650:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38983;
}
else 
{
label_38983:; 
 __return_39068 = ret;
}
tmp = __return_39068;
goto label_39186;
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
goto label_24650;
}
else 
{
goto label_24642;
}
}
else 
{
label_24642:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24702;
}
else 
{
goto label_24702;
}
}
else 
{
goto label_24702;
}
}
}
else 
{
goto label_24702;
}
}
else 
{
label_24702:; 
skip = 0;
goto label_11521;
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
goto label_24470;
}
else 
{
if (s__state == 8513)
{
label_24470:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38397;
}
else 
{
goto label_24480;
}
}
else 
{
skip = 1;
label_24480:; 
s__state = 8528;
s__init_num = 0;
goto label_24584;
}
}
else 
{
if (s__state == 8528)
{
goto label_24412;
}
else 
{
if (s__state == 8529)
{
label_24412:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_24421;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_24421:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_24461;
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
label_24457:; 
label_24461:; 
s__state = 8544;
s__init_num = 0;
goto label_24584;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_24444;
}
else 
{
tmp___7 = 512;
label_24444:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_24457;
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
goto label_24360;
}
else 
{
if (s__state == 8545)
{
label_24360:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24405;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_24374;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_24397:; 
label_24405:; 
goto label_24584;
}
}
else 
{
label_24374:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_24388;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24397;
}
else 
{
label_24388:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38397;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_24397;
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
goto label_24345;
}
else 
{
if (s__state == 8561)
{
label_24345:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38397;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_24584;
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
goto label_38397;
}
else 
{
s__rwstate = 1;
goto label_24334;
}
}
else 
{
label_24334:; 
s__state = s__s3__tmp__next_state___0;
goto label_24584;
}
}
else 
{
if (s__state == 8576)
{
goto label_24302;
}
else 
{
if (s__state == 8577)
{
label_24302:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38397;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_24318;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38397;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_24318:; 
goto label_24584;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24288;
}
else 
{
if (s__state == 8593)
{
label_24288:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38397;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_24584;
}
}
else 
{
if (s__state == 8608)
{
goto label_24275;
}
else 
{
if (s__state == 8609)
{
label_24275:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38397;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_24584:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38397;
}
else 
{
goto label_24644;
}
}
else 
{
label_24644:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24704;
}
else 
{
goto label_24704;
}
}
else 
{
goto label_24704;
}
}
}
else 
{
goto label_24704;
}
}
else 
{
label_24704:; 
skip = 0;
label_24714:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_25127;
}
else 
{
if (s__state == 16384)
{
label_25127:; 
goto label_25129;
}
else 
{
if (s__state == 8192)
{
label_25129:; 
goto label_25131;
}
else 
{
if (s__state == 24576)
{
label_25131:; 
goto label_25133;
}
else 
{
if (s__state == 8195)
{
label_25133:; 
s__server = 1;
if (cb != 0)
{
goto label_25138;
}
else 
{
label_25138:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39152 = -1;
goto label_39153;
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
goto label_38399;
}
else 
{
s__init_buf___0 = buf;
goto label_25150;
}
}
else 
{
label_25150:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38399;
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
goto label_38399;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_25170;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_25170:; 
goto label_25172;
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
goto label_25106;
}
else 
{
if (s__state == 8481)
{
label_25106:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38399;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_25172;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_25172;
}
else 
{
if (s__state == 8464)
{
goto label_25085;
}
else 
{
if (s__state == 8465)
{
label_25085:; 
goto label_25087;
}
else 
{
if (s__state == 8466)
{
label_25087:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_25238:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38977;
}
else 
{
label_38977:; 
 __return_39071 = ret;
}
tmp = __return_39071;
goto label_39186;
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
goto label_25238;
}
else 
{
goto label_25230;
}
}
else 
{
label_25230:; 
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
goto label_25058;
}
else 
{
if (s__state == 8513)
{
label_25058:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38399;
}
else 
{
goto label_25068;
}
}
else 
{
skip = 1;
label_25068:; 
s__state = 8528;
s__init_num = 0;
goto label_25172;
}
}
else 
{
if (s__state == 8528)
{
goto label_25000;
}
else 
{
if (s__state == 8529)
{
label_25000:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_25009;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_25009:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_25049;
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
label_25045:; 
label_25049:; 
s__state = 8544;
s__init_num = 0;
goto label_25172;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_25032;
}
else 
{
tmp___7 = 512;
label_25032:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_25045;
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
goto label_24948;
}
else 
{
if (s__state == 8545)
{
label_24948:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24993;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_24962;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_24985:; 
label_24993:; 
goto label_25172;
}
}
else 
{
label_24962:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_24976;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24985;
}
else 
{
label_24976:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38399;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_24985;
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
goto label_24933;
}
else 
{
if (s__state == 8561)
{
label_24933:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38399;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_25172;
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
goto label_38399;
}
else 
{
s__rwstate = 1;
goto label_24922;
}
}
else 
{
label_24922:; 
s__state = s__s3__tmp__next_state___0;
goto label_25172;
}
}
else 
{
if (s__state == 8576)
{
goto label_24890;
}
else 
{
if (s__state == 8577)
{
label_24890:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38399;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_24906;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38399;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_24906:; 
goto label_25172;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24876;
}
else 
{
if (s__state == 8593)
{
label_24876:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38399;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_25172;
}
}
else 
{
if (s__state == 8608)
{
goto label_24863;
}
else 
{
if (s__state == 8609)
{
label_24863:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38399;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_25172:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38399;
}
else 
{
goto label_25232;
}
}
else 
{
label_25232:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_25292;
}
else 
{
goto label_25292;
}
}
else 
{
goto label_25292;
}
}
}
else 
{
goto label_25292;
}
}
else 
{
label_25292:; 
skip = 0;
goto label_24714;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_24845;
}
else 
{
if (s__state == 8641)
{
label_24845:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_25236:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38979;
}
else 
{
label_38979:; 
 __return_39070 = ret;
}
tmp = __return_39070;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_24855;
}
else 
{
s__state = 3;
label_24855:; 
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
goto label_25236;
}
else 
{
goto label_25228;
}
}
else 
{
label_25228:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_25288;
}
else 
{
goto label_25288;
}
}
else 
{
goto label_25288;
}
}
}
else 
{
goto label_25288;
}
}
else 
{
label_25288:; 
skip = 0;
label_25299:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_25674;
}
else 
{
if (s__state == 16384)
{
label_25674:; 
goto label_25676;
}
else 
{
if (s__state == 8192)
{
label_25676:; 
goto label_25678;
}
else 
{
if (s__state == 24576)
{
label_25678:; 
goto label_25680;
}
else 
{
if (s__state == 8195)
{
label_25680:; 
s__server = 1;
if (cb != 0)
{
goto label_25685;
}
else 
{
label_25685:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39150 = -1;
goto label_39151;
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
goto label_38401;
}
else 
{
s__init_buf___0 = buf;
goto label_25697;
}
}
else 
{
label_25697:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38401;
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
goto label_38401;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_25717;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_25717:; 
goto label_25719;
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
goto label_25653;
}
else 
{
if (s__state == 8481)
{
label_25653:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38401;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_25719;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_25719;
}
else 
{
if (s__state == 8464)
{
goto label_25640;
}
else 
{
if (s__state == 8465)
{
label_25640:; 
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
goto label_25620;
}
else 
{
if (s__state == 8513)
{
label_25620:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_25719;
}
else 
{
if (s__state == 8528)
{
goto label_25562;
}
else 
{
if (s__state == 8529)
{
label_25562:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_25571;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_25571:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_25611;
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
label_25607:; 
label_25611:; 
s__state = 8544;
s__init_num = 0;
goto label_25719;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_25594;
}
else 
{
tmp___7 = 512;
label_25594:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_25607;
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
goto label_25520;
}
else 
{
if (s__state == 8545)
{
label_25520:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25555;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_25534;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_25546:; 
label_25555:; 
goto label_25719;
}
}
else 
{
label_25534:; 
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
goto label_25546;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_25505;
}
else 
{
if (s__state == 8561)
{
label_25505:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38401;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_25719;
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
goto label_38401;
}
else 
{
s__rwstate = 1;
goto label_25494;
}
}
else 
{
label_25494:; 
s__state = s__s3__tmp__next_state___0;
goto label_25719;
}
}
else 
{
if (s__state == 8576)
{
goto label_25462;
}
else 
{
if (s__state == 8577)
{
label_25462:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38401;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_25478;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38401;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_25478:; 
goto label_25719;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_25448;
}
else 
{
if (s__state == 8593)
{
label_25448:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38401;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_25719;
}
}
else 
{
if (s__state == 8608)
{
goto label_25435;
}
else 
{
if (s__state == 8609)
{
label_25435:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38401;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_25719:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38401;
}
else 
{
goto label_25749;
}
}
else 
{
label_25749:; 
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
goto label_25405;
}
else 
{
if (s__state == 8657)
{
label_25405:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38401;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_25751;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_25751:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38975;
}
else 
{
label_38975:; 
 __return_39072 = ret;
}
tmp = __return_39072;
goto label_39190;
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
goto label_25751;
}
else 
{
goto label_25747;
}
}
else 
{
label_25747:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_25777;
}
else 
{
goto label_25777;
}
}
else 
{
goto label_25777;
}
}
}
else 
{
goto label_25777;
}
}
else 
{
label_25777:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_32352;
}
else 
{
if (s__state == 16384)
{
label_32352:; 
goto label_32354;
}
else 
{
if (s__state == 8192)
{
label_32354:; 
goto label_32356;
}
else 
{
if (s__state == 24576)
{
label_32356:; 
goto label_32358;
}
else 
{
if (s__state == 8195)
{
label_32358:; 
s__server = 1;
if (cb != 0)
{
goto label_32363;
}
else 
{
label_32363:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39148 = -1;
goto label_39149;
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
goto label_38403;
}
else 
{
s__init_buf___0 = buf;
goto label_32375;
}
}
else 
{
label_32375:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38403;
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
goto label_38403;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_32395;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_32395:; 
goto label_32397;
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
goto label_32331;
}
else 
{
if (s__state == 8481)
{
label_32331:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38403;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_32397;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_32397;
}
else 
{
if (s__state == 8464)
{
goto label_32318;
}
else 
{
if (s__state == 8465)
{
label_32318:; 
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
goto label_32298;
}
else 
{
if (s__state == 8513)
{
label_32298:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_32397;
}
else 
{
if (s__state == 8528)
{
goto label_32240;
}
else 
{
if (s__state == 8529)
{
label_32240:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_32249;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_32249:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_32289;
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
label_32285:; 
label_32289:; 
s__state = 8544;
s__init_num = 0;
goto label_32397;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_32272;
}
else 
{
tmp___7 = 512;
label_32272:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_32285;
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
goto label_32198;
}
else 
{
if (s__state == 8545)
{
label_32198:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_32233;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_32212;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_32224:; 
label_32233:; 
goto label_32397;
}
}
else 
{
label_32212:; 
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
goto label_32224;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_32183;
}
else 
{
if (s__state == 8561)
{
label_32183:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38403;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_32397;
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
goto label_38403;
}
else 
{
s__rwstate = 1;
goto label_32172;
}
}
else 
{
label_32172:; 
s__state = s__s3__tmp__next_state___0;
goto label_32397;
}
}
else 
{
if (s__state == 8576)
{
goto label_32140;
}
else 
{
if (s__state == 8577)
{
label_32140:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38403;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_32156;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38403;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_32156:; 
goto label_32397;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_32126;
}
else 
{
if (s__state == 8593)
{
label_32126:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38403;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_32397;
}
}
else 
{
if (s__state == 8608)
{
goto label_32113;
}
else 
{
if (s__state == 8609)
{
label_32113:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38403;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_32397:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38403;
}
else 
{
goto label_32427;
}
}
else 
{
label_32427:; 
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
goto label_32096;
}
else 
{
if (s__state == 8657)
{
label_32096:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38403;
}
else 
{
if (s__state == 8672)
{
goto label_32077;
}
else 
{
if (s__state == 8673)
{
label_32077:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_32429:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38973;
}
else 
{
label_38973:; 
 __return_39073 = ret;
}
tmp = __return_39073;
goto label_39192;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_32088;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_32088:; 
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
goto label_32429;
}
else 
{
goto label_32425;
}
}
else 
{
label_32425:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_32455;
}
else 
{
goto label_32455;
}
}
else 
{
goto label_32455;
}
}
}
else 
{
goto label_32455;
}
}
else 
{
label_32455:; 
skip = 0;
label_32461:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_32836;
}
else 
{
if (s__state == 16384)
{
label_32836:; 
goto label_32838;
}
else 
{
if (s__state == 8192)
{
label_32838:; 
goto label_32840;
}
else 
{
if (s__state == 24576)
{
label_32840:; 
goto label_32842;
}
else 
{
if (s__state == 8195)
{
label_32842:; 
s__server = 1;
if (cb != 0)
{
goto label_32847;
}
else 
{
label_32847:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39146 = -1;
goto label_39147;
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
goto label_38405;
}
else 
{
s__init_buf___0 = buf;
goto label_32859;
}
}
else 
{
label_32859:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38405;
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
goto label_38405;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_32879;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_32879:; 
goto label_32881;
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
goto label_32815;
}
else 
{
if (s__state == 8481)
{
label_32815:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38405;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_32881;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_32881;
}
else 
{
if (s__state == 8464)
{
goto label_32802;
}
else 
{
if (s__state == 8465)
{
label_32802:; 
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
goto label_32782;
}
else 
{
if (s__state == 8513)
{
label_32782:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_32881;
}
else 
{
if (s__state == 8528)
{
goto label_32724;
}
else 
{
if (s__state == 8529)
{
label_32724:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_32733;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_32733:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_32773;
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
label_32769:; 
label_32773:; 
s__state = 8544;
s__init_num = 0;
goto label_32881;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_32756;
}
else 
{
tmp___7 = 512;
label_32756:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_32769;
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
goto label_32682;
}
else 
{
if (s__state == 8545)
{
label_32682:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_32717;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_32696;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_32708:; 
label_32717:; 
goto label_32881;
}
}
else 
{
label_32696:; 
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
goto label_32708;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_32667;
}
else 
{
if (s__state == 8561)
{
label_32667:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38405;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_32881;
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
goto label_38405;
}
else 
{
s__rwstate = 1;
goto label_32656;
}
}
else 
{
label_32656:; 
s__state = s__s3__tmp__next_state___0;
goto label_32881;
}
}
else 
{
if (s__state == 8576)
{
goto label_32624;
}
else 
{
if (s__state == 8577)
{
label_32624:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38405;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_32640;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38405;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_32640:; 
goto label_32881;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_32610;
}
else 
{
if (s__state == 8593)
{
label_32610:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38405;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_32881;
}
}
else 
{
if (s__state == 8608)
{
goto label_32597;
}
else 
{
if (s__state == 8609)
{
label_32597:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38405;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_32881:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38405;
}
else 
{
goto label_32911;
}
}
else 
{
label_32911:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_32941;
}
else 
{
goto label_32941;
}
}
else 
{
goto label_32941;
}
}
}
else 
{
goto label_32941;
}
}
else 
{
label_32941:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_33321;
}
else 
{
if (s__state == 16384)
{
label_33321:; 
goto label_33323;
}
else 
{
if (s__state == 8192)
{
label_33323:; 
goto label_33325;
}
else 
{
if (s__state == 24576)
{
label_33325:; 
goto label_33327;
}
else 
{
if (s__state == 8195)
{
label_33327:; 
s__server = 1;
if (cb != 0)
{
goto label_33332;
}
else 
{
label_33332:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39144 = -1;
goto label_39145;
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
goto label_38407;
}
else 
{
s__init_buf___0 = buf;
goto label_33344;
}
}
else 
{
label_33344:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38407;
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
goto label_38407;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_33364;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_33364:; 
goto label_33366;
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
goto label_33300;
}
else 
{
if (s__state == 8481)
{
label_33300:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38407;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_33366;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_33366;
}
else 
{
if (s__state == 8464)
{
goto label_33287;
}
else 
{
if (s__state == 8465)
{
label_33287:; 
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
goto label_33267;
}
else 
{
if (s__state == 8513)
{
label_33267:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_33366;
}
else 
{
if (s__state == 8528)
{
goto label_33209;
}
else 
{
if (s__state == 8529)
{
label_33209:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_33218;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_33218:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_33258;
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
label_33254:; 
label_33258:; 
s__state = 8544;
s__init_num = 0;
goto label_33366;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_33241;
}
else 
{
tmp___7 = 512;
label_33241:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_33254;
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
goto label_33167;
}
else 
{
if (s__state == 8545)
{
label_33167:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_33202;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_33181;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_33193:; 
label_33202:; 
goto label_33366;
}
}
else 
{
label_33181:; 
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
goto label_33193;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_33152;
}
else 
{
if (s__state == 8561)
{
label_33152:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38407;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_33366;
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
goto label_38407;
}
else 
{
s__rwstate = 1;
goto label_33141;
}
}
else 
{
label_33141:; 
s__state = s__s3__tmp__next_state___0;
goto label_33366;
}
}
else 
{
if (s__state == 8576)
{
goto label_33109;
}
else 
{
if (s__state == 8577)
{
label_33109:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38407;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_33125;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38407;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_33125:; 
goto label_33366;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_33095;
}
else 
{
if (s__state == 8593)
{
label_33095:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38407;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_33366;
}
}
else 
{
if (s__state == 8608)
{
goto label_33082;
}
else 
{
if (s__state == 8609)
{
label_33082:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38407;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_33366:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38407;
}
else 
{
goto label_33396;
}
}
else 
{
label_33396:; 
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
goto label_33064;
}
else 
{
if (s__state == 8641)
{
label_33064:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_33398:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38969;
}
else 
{
label_38969:; 
 __return_39075 = ret;
}
tmp = __return_39075;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_33074;
}
else 
{
s__state = 3;
label_33074:; 
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
goto label_33398;
}
else 
{
goto label_33394;
}
}
else 
{
label_33394:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_33424;
}
else 
{
goto label_33424;
}
}
else 
{
goto label_33424;
}
}
}
else 
{
goto label_33424;
}
}
else 
{
label_33424:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_33805;
}
else 
{
if (s__state == 16384)
{
label_33805:; 
goto label_33807;
}
else 
{
if (s__state == 8192)
{
label_33807:; 
goto label_33809;
}
else 
{
if (s__state == 24576)
{
label_33809:; 
goto label_33811;
}
else 
{
if (s__state == 8195)
{
label_33811:; 
s__server = 1;
if (cb != 0)
{
goto label_33816;
}
else 
{
label_33816:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39142 = -1;
goto label_39143;
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
goto label_38409;
}
else 
{
s__init_buf___0 = buf;
goto label_33828;
}
}
else 
{
label_33828:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38409;
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
goto label_38409;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_33848;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_33848:; 
goto label_33850;
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
goto label_33784;
}
else 
{
if (s__state == 8481)
{
label_33784:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38409;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_33850;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_33850;
}
else 
{
if (s__state == 8464)
{
goto label_33771;
}
else 
{
if (s__state == 8465)
{
label_33771:; 
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
goto label_33751;
}
else 
{
if (s__state == 8513)
{
label_33751:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_33850;
}
else 
{
if (s__state == 8528)
{
goto label_33693;
}
else 
{
if (s__state == 8529)
{
label_33693:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_33702;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_33702:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_33742;
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
label_33738:; 
label_33742:; 
s__state = 8544;
s__init_num = 0;
goto label_33850;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_33725;
}
else 
{
tmp___7 = 512;
label_33725:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_33738;
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
goto label_33651;
}
else 
{
if (s__state == 8545)
{
label_33651:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_33686;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_33665;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_33677:; 
label_33686:; 
goto label_33850;
}
}
else 
{
label_33665:; 
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
goto label_33677;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_33636;
}
else 
{
if (s__state == 8561)
{
label_33636:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38409;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_33850;
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
goto label_38409;
}
else 
{
s__rwstate = 1;
goto label_33625;
}
}
else 
{
label_33625:; 
s__state = s__s3__tmp__next_state___0;
goto label_33850;
}
}
else 
{
if (s__state == 8576)
{
goto label_33593;
}
else 
{
if (s__state == 8577)
{
label_33593:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38409;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_33609;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38409;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_33609:; 
goto label_33850;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_33579;
}
else 
{
if (s__state == 8593)
{
label_33579:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38409;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_33850;
}
}
else 
{
if (s__state == 8608)
{
goto label_33566;
}
else 
{
if (s__state == 8609)
{
label_33566:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38409;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_33850:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38409;
}
else 
{
goto label_33880;
}
}
else 
{
label_33880:; 
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
goto label_33536;
}
else 
{
if (s__state == 8657)
{
label_33536:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38409;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_33882;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_33882:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38967;
}
else 
{
label_38967:; 
 __return_39076 = ret;
}
tmp = __return_39076;
goto label_39190;
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
goto label_33882;
}
else 
{
goto label_33878;
}
}
else 
{
label_33878:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_33908;
}
else 
{
goto label_33908;
}
}
else 
{
goto label_33908;
}
}
}
else 
{
goto label_33908;
}
}
else 
{
label_33908:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_34290;
}
else 
{
if (s__state == 16384)
{
label_34290:; 
goto label_34292;
}
else 
{
if (s__state == 8192)
{
label_34292:; 
goto label_34294;
}
else 
{
if (s__state == 24576)
{
label_34294:; 
goto label_34296;
}
else 
{
if (s__state == 8195)
{
label_34296:; 
s__server = 1;
if (cb != 0)
{
goto label_34301;
}
else 
{
label_34301:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39140 = -1;
goto label_39141;
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
goto label_38411;
}
else 
{
s__init_buf___0 = buf;
goto label_34313;
}
}
else 
{
label_34313:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38411;
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
goto label_38411;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_34333;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_34333:; 
goto label_34335;
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
goto label_34269;
}
else 
{
if (s__state == 8481)
{
label_34269:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38411;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_34335;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_34335;
}
else 
{
if (s__state == 8464)
{
goto label_34256;
}
else 
{
if (s__state == 8465)
{
label_34256:; 
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
goto label_34236;
}
else 
{
if (s__state == 8513)
{
label_34236:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_34335;
}
else 
{
if (s__state == 8528)
{
goto label_34178;
}
else 
{
if (s__state == 8529)
{
label_34178:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_34187;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_34187:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_34227;
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
label_34223:; 
label_34227:; 
s__state = 8544;
s__init_num = 0;
goto label_34335;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_34210;
}
else 
{
tmp___7 = 512;
label_34210:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_34223;
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
goto label_34136;
}
else 
{
if (s__state == 8545)
{
label_34136:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_34171;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_34150;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_34162:; 
label_34171:; 
goto label_34335;
}
}
else 
{
label_34150:; 
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
goto label_34162;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_34121;
}
else 
{
if (s__state == 8561)
{
label_34121:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38411;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_34335;
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
goto label_38411;
}
else 
{
s__rwstate = 1;
goto label_34110;
}
}
else 
{
label_34110:; 
s__state = s__s3__tmp__next_state___0;
goto label_34335;
}
}
else 
{
if (s__state == 8576)
{
goto label_34078;
}
else 
{
if (s__state == 8577)
{
label_34078:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38411;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_34094;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38411;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_34094:; 
goto label_34335;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_34064;
}
else 
{
if (s__state == 8593)
{
label_34064:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38411;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_34335;
}
}
else 
{
if (s__state == 8608)
{
goto label_34051;
}
else 
{
if (s__state == 8609)
{
label_34051:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38411;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_34335:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38411;
}
else 
{
goto label_34365;
}
}
else 
{
label_34365:; 
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
goto label_34034;
}
else 
{
if (s__state == 8657)
{
label_34034:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38411;
}
else 
{
if (s__state == 8672)
{
goto label_34015;
}
else 
{
if (s__state == 8673)
{
label_34015:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_34367:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38965;
}
else 
{
label_38965:; 
 __return_39077 = ret;
}
tmp = __return_39077;
goto label_39192;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_34026;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_34026:; 
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
goto label_34367;
}
else 
{
goto label_34363;
}
}
else 
{
label_34363:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_34393;
}
else 
{
goto label_34393;
}
}
else 
{
goto label_34393;
}
}
}
else 
{
goto label_34393;
}
}
else 
{
label_34393:; 
skip = 0;
goto label_32461;
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
goto label_34007;
}
else 
{
goto label_34007;
}
}
else 
{
label_34007:; 
ret = 1;
goto label_38411;
}
}
else 
{
ret = -1;
label_38411:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38877;
}
else 
{
label_38877:; 
 __return_39141 = ret;
label_39141:; 
}
tmp = __return_39141;
goto label_39190;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_33523;
}
else 
{
goto label_33523;
}
}
else 
{
label_33523:; 
ret = 1;
goto label_38409;
}
}
else 
{
ret = -1;
label_38409:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38879;
}
else 
{
label_38879:; 
 __return_39143 = ret;
label_39143:; 
}
tmp = __return_39143;
goto label_39196;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_33052;
}
else 
{
if (s__state == 8657)
{
label_33052:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38407;
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
goto label_33039;
}
else 
{
goto label_33039;
}
}
else 
{
label_33039:; 
ret = 1;
goto label_38407;
}
}
else 
{
ret = -1;
label_38407:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38881;
}
else 
{
label_38881:; 
 __return_39145 = ret;
label_39145:; 
}
tmp = __return_39145;
goto label_39192;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_32579;
}
else 
{
if (s__state == 8641)
{
label_32579:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_32913:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38971;
}
else 
{
label_38971:; 
 __return_39074 = ret;
}
tmp = __return_39074;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_32589;
}
else 
{
s__state = 3;
label_32589:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_32913;
}
else 
{
goto label_32909;
}
}
else 
{
label_32909:; 
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
goto label_32567;
}
else 
{
if (s__state == 8657)
{
label_32567:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38405;
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
goto label_32554;
}
else 
{
goto label_32554;
}
}
else 
{
label_32554:; 
ret = 1;
goto label_38405;
}
}
else 
{
ret = -1;
label_38405:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38883;
}
else 
{
label_38883:; 
 __return_39147 = ret;
label_39147:; 
}
tmp = __return_39147;
goto label_39192;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_32069;
}
else 
{
goto label_32069;
}
}
else 
{
label_32069:; 
ret = 1;
goto label_38403;
}
}
else 
{
ret = -1;
label_38403:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38885;
}
else 
{
label_38885:; 
 __return_39149 = ret;
label_39149:; 
}
tmp = __return_39149;
goto label_39190;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_25392;
}
else 
{
goto label_25392;
}
}
else 
{
label_25392:; 
ret = 1;
goto label_38401;
}
}
else 
{
ret = -1;
label_38401:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38887;
}
else 
{
label_38887:; 
 __return_39151 = ret;
label_39151:; 
}
tmp = __return_39151;
goto label_39196;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_24820;
}
else 
{
if (s__state == 8657)
{
label_24820:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38399;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_25234;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_25234:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38981;
}
else 
{
label_38981:; 
 __return_39069 = ret;
}
tmp = __return_39069;
goto label_39190;
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
goto label_25234;
}
else 
{
goto label_25226;
}
}
else 
{
label_25226:; 
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
goto label_24807;
}
else 
{
goto label_24807;
}
}
else 
{
label_24807:; 
ret = 1;
goto label_38399;
}
}
else 
{
ret = -1;
label_38399:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38889;
}
else 
{
label_38889:; 
 __return_39153 = ret;
label_39153:; 
}
tmp = __return_39153;
goto label_39200;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_24257;
}
else 
{
if (s__state == 8641)
{
label_24257:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_24648:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38985;
}
else 
{
label_38985:; 
 __return_39067 = ret;
}
tmp = __return_39067;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_24267;
}
else 
{
s__state = 3;
label_24267:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_24648;
}
else 
{
goto label_24640;
}
}
else 
{
label_24640:; 
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
goto label_24232;
}
else 
{
if (s__state == 8657)
{
label_24232:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38397;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_24646;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_24646:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38987;
}
else 
{
label_38987:; 
 __return_39066 = ret;
}
tmp = __return_39066;
goto label_39190;
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
goto label_24646;
}
else 
{
goto label_24638;
}
}
else 
{
label_24638:; 
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
goto label_24219;
}
else 
{
goto label_24219;
}
}
else 
{
label_24219:; 
ret = 1;
goto label_38397;
}
}
else 
{
ret = -1;
label_38397:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38891;
}
else 
{
label_38891:; 
 __return_39155 = ret;
label_39155:; 
}
tmp = __return_39155;
goto label_39200;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_23669;
}
else 
{
if (s__state == 8641)
{
label_23669:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_24060:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38991;
}
else 
{
label_38991:; 
 __return_39064 = ret;
}
tmp = __return_39064;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_23679;
}
else 
{
s__state = 3;
label_23679:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_24060;
}
else 
{
goto label_24052;
}
}
else 
{
label_24052:; 
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
goto label_23644;
}
else 
{
if (s__state == 8657)
{
label_23644:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38395;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_24058;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_24058:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38993;
}
else 
{
label_38993:; 
 __return_39063 = ret;
}
tmp = __return_39063;
goto label_39190;
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
goto label_24058;
}
else 
{
goto label_24050;
}
}
else 
{
label_24050:; 
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
goto label_23631;
}
else 
{
goto label_23631;
}
}
else 
{
label_23631:; 
ret = 1;
goto label_38395;
}
}
else 
{
ret = -1;
label_38395:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38893;
}
else 
{
label_38893:; 
 __return_39157 = ret;
label_39157:; 
}
tmp = __return_39157;
goto label_39200;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_23082;
}
else 
{
if (s__state == 8641)
{
label_23082:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_23473:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38997;
}
else 
{
label_38997:; 
 __return_39061 = ret;
}
tmp = __return_39061;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_23092;
}
else 
{
s__state = 3;
label_23092:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_23473;
}
else 
{
goto label_23465;
}
}
else 
{
label_23465:; 
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
goto label_23057;
}
else 
{
if (s__state == 8657)
{
label_23057:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38393;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_23471;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_23471:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38999;
}
else 
{
label_38999:; 
 __return_39060 = ret;
}
tmp = __return_39060;
goto label_39190;
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
goto label_23471;
}
else 
{
goto label_23463;
}
}
else 
{
label_23463:; 
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
goto label_23044;
}
else 
{
goto label_23044;
}
}
else 
{
label_23044:; 
ret = 1;
goto label_38393;
}
}
else 
{
ret = -1;
label_38393:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38895;
}
else 
{
label_38895:; 
 __return_39159 = ret;
label_39159:; 
}
tmp = __return_39159;
goto label_39200;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_22495;
}
else 
{
if (s__state == 8641)
{
label_22495:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_22886:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39003;
}
else 
{
label_39003:; 
 __return_39058 = ret;
}
tmp = __return_39058;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_22505;
}
else 
{
s__state = 3;
label_22505:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22886;
}
else 
{
goto label_22878;
}
}
else 
{
label_22878:; 
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
goto label_22470;
}
else 
{
if (s__state == 8657)
{
label_22470:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38391;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_22884;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_22884:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39005;
}
else 
{
label_39005:; 
 __return_39057 = ret;
}
tmp = __return_39057;
goto label_39190;
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
goto label_22884;
}
else 
{
goto label_22876;
}
}
else 
{
label_22876:; 
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
goto label_22457;
}
else 
{
goto label_22457;
}
}
else 
{
label_22457:; 
ret = 1;
goto label_38391;
}
}
else 
{
ret = -1;
label_38391:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38897;
}
else 
{
label_38897:; 
 __return_39161 = ret;
label_39161:; 
}
tmp = __return_39161;
goto label_39200;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_21908;
}
else 
{
if (s__state == 8641)
{
label_21908:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_22299:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39009;
}
else 
{
label_39009:; 
 __return_39055 = ret;
}
tmp = __return_39055;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_21918;
}
else 
{
s__state = 3;
label_21918:; 
s__init_num = 0;
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
goto label_21883;
}
else 
{
if (s__state == 8657)
{
label_21883:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38389;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_22297;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_22297:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39011;
}
else 
{
label_39011:; 
 __return_39054 = ret;
}
tmp = __return_39054;
goto label_39190;
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
goto label_22297;
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
goto label_21870;
}
else 
{
goto label_21870;
}
}
else 
{
label_21870:; 
ret = 1;
goto label_38389;
}
}
else 
{
ret = -1;
label_38389:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38899;
}
else 
{
label_38899:; 
 __return_39163 = ret;
label_39163:; 
}
tmp = __return_39163;
label_39200:; 
 __return_39360 = tmp;
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
goto label_21384;
}
else 
{
if (s__state == 8545)
{
label_21384:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_21429;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_21398;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_21421:; 
label_21429:; 
goto label_21618;
}
}
else 
{
label_21398:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_21412;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_21421;
}
else 
{
label_21412:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38387;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_21421;
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
goto label_21369;
}
else 
{
if (s__state == 8561)
{
label_21369:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38387;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_21618;
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
goto label_38387;
}
else 
{
s__rwstate = 1;
goto label_21358;
}
}
else 
{
label_21358:; 
s__state = s__s3__tmp__next_state___0;
goto label_21618;
}
}
else 
{
if (s__state == 8576)
{
goto label_21326;
}
else 
{
if (s__state == 8577)
{
label_21326:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38387;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_21342;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38387;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_21342:; 
goto label_21618;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_21312;
}
else 
{
if (s__state == 8593)
{
label_21312:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38387;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_21618;
}
}
else 
{
if (s__state == 8608)
{
goto label_21299;
}
else 
{
if (s__state == 8609)
{
label_21299:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38387;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_21618:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38387;
}
else 
{
goto label_21693;
}
}
else 
{
label_21693:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21768;
}
else 
{
goto label_21768;
}
}
else 
{
goto label_21768;
}
}
}
else 
{
goto label_21768;
}
}
else 
{
label_21768:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_34833;
}
else 
{
if (s__state == 16384)
{
label_34833:; 
goto label_34835;
}
else 
{
if (s__state == 8192)
{
label_34835:; 
goto label_34837;
}
else 
{
if (s__state == 24576)
{
label_34837:; 
goto label_34839;
}
else 
{
if (s__state == 8195)
{
label_34839:; 
s__server = 1;
if (cb != 0)
{
goto label_34844;
}
else 
{
label_34844:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39138 = -1;
goto label_39139;
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
goto label_38413;
}
else 
{
s__init_buf___0 = buf;
goto label_34856;
}
}
else 
{
label_34856:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38413;
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
goto label_38413;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_34876;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_34876:; 
goto label_34878;
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
goto label_34812;
}
else 
{
if (s__state == 8481)
{
label_34812:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38413;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_34878;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_34878;
}
else 
{
if (s__state == 8464)
{
goto label_34791;
}
else 
{
if (s__state == 8465)
{
label_34791:; 
goto label_34793;
}
else 
{
if (s__state == 8466)
{
label_34793:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_34961:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38957;
}
else 
{
label_38957:; 
 __return_39081 = ret;
}
tmp = __return_39081;
goto label_39186;
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
goto label_34961;
}
else 
{
goto label_34951;
}
}
else 
{
label_34951:; 
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
goto label_34764;
}
else 
{
if (s__state == 8513)
{
label_34764:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38413;
}
else 
{
goto label_34774;
}
}
else 
{
skip = 1;
label_34774:; 
s__state = 8528;
s__init_num = 0;
goto label_34878;
}
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
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_34752;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_34739;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_34746:; 
label_34752:; 
s__state = 8544;
s__init_num = 0;
goto label_34878;
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
goto label_34739;
}
else 
{
skip = 1;
goto label_34746;
}
}
}
}
}
}
else 
{
goto label_34739;
}
}
else 
{
label_34739:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_34959:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38959;
}
else 
{
label_38959:; 
 __return_39080 = ret;
}
tmp = __return_39080;
goto label_39200;
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
goto label_34959;
}
else 
{
goto label_34949;
}
}
else 
{
label_34949:; 
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
goto label_34644;
}
else 
{
if (s__state == 8545)
{
label_34644:; 
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
goto label_34658;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_34681:; 
label_34689:; 
goto label_34878;
}
}
else 
{
label_34658:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_34672;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_34681;
}
else 
{
label_34672:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38413;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_34681;
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
goto label_34629;
}
else 
{
if (s__state == 8561)
{
label_34629:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38413;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_34878;
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
goto label_38413;
}
else 
{
s__rwstate = 1;
goto label_34618;
}
}
else 
{
label_34618:; 
s__state = s__s3__tmp__next_state___0;
goto label_34878;
}
}
else 
{
if (s__state == 8576)
{
goto label_34586;
}
else 
{
if (s__state == 8577)
{
label_34586:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38413;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_34602;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38413;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_34602:; 
goto label_34878;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_34572;
}
else 
{
if (s__state == 8593)
{
label_34572:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38413;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_34878;
}
}
else 
{
if (s__state == 8608)
{
goto label_34559;
}
else 
{
if (s__state == 8609)
{
label_34559:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38413;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_34878:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38413;
}
else 
{
goto label_34953;
}
}
else 
{
label_34953:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_35028;
}
else 
{
goto label_35028;
}
}
else 
{
goto label_35028;
}
}
}
else 
{
goto label_35028;
}
}
else 
{
label_35028:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_35462;
}
else 
{
if (s__state == 16384)
{
label_35462:; 
goto label_35464;
}
else 
{
if (s__state == 8192)
{
label_35464:; 
goto label_35466;
}
else 
{
if (s__state == 24576)
{
label_35466:; 
goto label_35468;
}
else 
{
if (s__state == 8195)
{
label_35468:; 
s__server = 1;
if (cb != 0)
{
goto label_35473;
}
else 
{
label_35473:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39136 = -1;
goto label_39137;
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
goto label_38415;
}
else 
{
s__init_buf___0 = buf;
goto label_35485;
}
}
else 
{
label_35485:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38415;
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
goto label_38415;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_35505;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_35505:; 
goto label_35507;
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
goto label_35441;
}
else 
{
if (s__state == 8481)
{
label_35441:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38415;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_35507;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_35507;
}
else 
{
if (s__state == 8464)
{
goto label_35420;
}
else 
{
if (s__state == 8465)
{
label_35420:; 
goto label_35422;
}
else 
{
if (s__state == 8466)
{
label_35422:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_35590:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38949;
}
else 
{
label_38949:; 
 __return_39085 = ret;
}
tmp = __return_39085;
goto label_39186;
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
goto label_35590;
}
else 
{
goto label_35580;
}
}
else 
{
label_35580:; 
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
goto label_35393;
}
else 
{
if (s__state == 8513)
{
label_35393:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38415;
}
else 
{
goto label_35403;
}
}
else 
{
skip = 1;
label_35403:; 
s__state = 8528;
s__init_num = 0;
goto label_35507;
}
}
else 
{
if (s__state == 8528)
{
goto label_35325;
}
else 
{
if (s__state == 8529)
{
label_35325:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_35334;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_35334:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_35381;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_35368;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_35375:; 
label_35381:; 
s__state = 8544;
s__init_num = 0;
goto label_35507;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_35357;
}
else 
{
tmp___7 = 512;
label_35357:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_35368;
}
else 
{
skip = 1;
goto label_35375;
}
}
}
}
}
}
else 
{
goto label_35368;
}
}
else 
{
label_35368:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_35588:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38951;
}
else 
{
label_38951:; 
 __return_39084 = ret;
}
tmp = __return_39084;
goto label_39200;
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
goto label_35588;
}
else 
{
goto label_35578;
}
}
else 
{
label_35578:; 
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
goto label_35273;
}
else 
{
if (s__state == 8545)
{
label_35273:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35318;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_35287;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_35310:; 
label_35318:; 
goto label_35507;
}
}
else 
{
label_35287:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_35301;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35310;
}
else 
{
label_35301:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38415;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_35310;
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
goto label_35258;
}
else 
{
if (s__state == 8561)
{
label_35258:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38415;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_35507;
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
goto label_38415;
}
else 
{
s__rwstate = 1;
goto label_35247;
}
}
else 
{
label_35247:; 
s__state = s__s3__tmp__next_state___0;
goto label_35507;
}
}
else 
{
if (s__state == 8576)
{
goto label_35215;
}
else 
{
if (s__state == 8577)
{
label_35215:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38415;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_35231;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38415;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_35231:; 
goto label_35507;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_35201;
}
else 
{
if (s__state == 8593)
{
label_35201:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38415;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_35507;
}
}
else 
{
if (s__state == 8608)
{
goto label_35188;
}
else 
{
if (s__state == 8609)
{
label_35188:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38415;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_35507:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38415;
}
else 
{
goto label_35582;
}
}
else 
{
label_35582:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_35657;
}
else 
{
goto label_35657;
}
}
else 
{
goto label_35657;
}
}
}
else 
{
goto label_35657;
}
}
else 
{
label_35657:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_36091;
}
else 
{
if (s__state == 16384)
{
label_36091:; 
goto label_36093;
}
else 
{
if (s__state == 8192)
{
label_36093:; 
goto label_36095;
}
else 
{
if (s__state == 24576)
{
label_36095:; 
goto label_36097;
}
else 
{
if (s__state == 8195)
{
label_36097:; 
s__server = 1;
if (cb != 0)
{
goto label_36102;
}
else 
{
label_36102:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39134 = -1;
goto label_39135;
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
goto label_38417;
}
else 
{
s__init_buf___0 = buf;
goto label_36114;
}
}
else 
{
label_36114:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38417;
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
goto label_38417;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_36134;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_36134:; 
goto label_36136;
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
goto label_36070;
}
else 
{
if (s__state == 8481)
{
label_36070:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38417;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_36136;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_36136;
}
else 
{
if (s__state == 8464)
{
goto label_36049;
}
else 
{
if (s__state == 8465)
{
label_36049:; 
goto label_36051;
}
else 
{
if (s__state == 8466)
{
label_36051:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_36219:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38941;
}
else 
{
label_38941:; 
 __return_39089 = ret;
}
tmp = __return_39089;
goto label_39186;
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
goto label_36219;
}
else 
{
goto label_36209;
}
}
else 
{
label_36209:; 
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
goto label_36022;
}
else 
{
if (s__state == 8513)
{
label_36022:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38417;
}
else 
{
goto label_36032;
}
}
else 
{
skip = 1;
label_36032:; 
s__state = 8528;
s__init_num = 0;
goto label_36136;
}
}
else 
{
if (s__state == 8528)
{
goto label_35954;
}
else 
{
if (s__state == 8529)
{
label_35954:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_35963;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_35963:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_36010;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_35997;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_36004:; 
label_36010:; 
s__state = 8544;
s__init_num = 0;
goto label_36136;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_35986;
}
else 
{
tmp___7 = 512;
label_35986:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_35997;
}
else 
{
skip = 1;
goto label_36004;
}
}
}
}
}
}
else 
{
goto label_35997;
}
}
else 
{
label_35997:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_36217:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38943;
}
else 
{
label_38943:; 
 __return_39088 = ret;
}
tmp = __return_39088;
goto label_39200;
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
goto label_36217;
}
else 
{
goto label_36207;
}
}
else 
{
label_36207:; 
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
goto label_35902;
}
else 
{
if (s__state == 8545)
{
label_35902:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35947;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_35916;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_35939:; 
label_35947:; 
goto label_36136;
}
}
else 
{
label_35916:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_35930;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_35939;
}
else 
{
label_35930:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38417;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_35939;
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
goto label_35887;
}
else 
{
if (s__state == 8561)
{
label_35887:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38417;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_36136;
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
goto label_38417;
}
else 
{
s__rwstate = 1;
goto label_35876;
}
}
else 
{
label_35876:; 
s__state = s__s3__tmp__next_state___0;
goto label_36136;
}
}
else 
{
if (s__state == 8576)
{
goto label_35844;
}
else 
{
if (s__state == 8577)
{
label_35844:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38417;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_35860;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38417;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_35860:; 
goto label_36136;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_35830;
}
else 
{
if (s__state == 8593)
{
label_35830:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38417;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_36136;
}
}
else 
{
if (s__state == 8608)
{
goto label_35817;
}
else 
{
if (s__state == 8609)
{
label_35817:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38417;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_36136:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38417;
}
else 
{
goto label_36211;
}
}
else 
{
label_36211:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36286;
}
else 
{
goto label_36286;
}
}
else 
{
goto label_36286;
}
}
}
else 
{
goto label_36286;
}
}
else 
{
label_36286:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_36720;
}
else 
{
if (s__state == 16384)
{
label_36720:; 
goto label_36722;
}
else 
{
if (s__state == 8192)
{
label_36722:; 
goto label_36724;
}
else 
{
if (s__state == 24576)
{
label_36724:; 
goto label_36726;
}
else 
{
if (s__state == 8195)
{
label_36726:; 
s__server = 1;
if (cb != 0)
{
goto label_36731;
}
else 
{
label_36731:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39132 = -1;
goto label_39133;
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
goto label_38419;
}
else 
{
s__init_buf___0 = buf;
goto label_36743;
}
}
else 
{
label_36743:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38419;
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
goto label_38419;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_36763;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_36763:; 
goto label_36765;
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
goto label_36699;
}
else 
{
if (s__state == 8481)
{
label_36699:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38419;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_36765;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_36765;
}
else 
{
if (s__state == 8464)
{
goto label_36678;
}
else 
{
if (s__state == 8465)
{
label_36678:; 
goto label_36680;
}
else 
{
if (s__state == 8466)
{
label_36680:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_36848:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38933;
}
else 
{
label_38933:; 
 __return_39093 = ret;
}
tmp = __return_39093;
goto label_39186;
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
goto label_36848;
}
else 
{
goto label_36838;
}
}
else 
{
label_36838:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36913;
}
else 
{
goto label_36913;
}
}
else 
{
goto label_36913;
}
}
}
else 
{
goto label_36913;
}
}
else 
{
label_36913:; 
skip = 0;
goto label_11521;
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
goto label_36651;
}
else 
{
if (s__state == 8513)
{
label_36651:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38419;
}
else 
{
goto label_36661;
}
}
else 
{
skip = 1;
label_36661:; 
s__state = 8528;
s__init_num = 0;
goto label_36765;
}
}
else 
{
if (s__state == 8528)
{
goto label_36583;
}
else 
{
if (s__state == 8529)
{
label_36583:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_36592;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_36592:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_36639;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_36626;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_36633:; 
label_36639:; 
s__state = 8544;
s__init_num = 0;
goto label_36765;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_36615;
}
else 
{
tmp___7 = 512;
label_36615:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_36626;
}
else 
{
skip = 1;
goto label_36633;
}
}
}
}
}
}
else 
{
goto label_36626;
}
}
else 
{
label_36626:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_36846:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38935;
}
else 
{
label_38935:; 
 __return_39092 = ret;
}
tmp = __return_39092;
goto label_39200;
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
goto label_36846;
}
else 
{
goto label_36836;
}
}
else 
{
label_36836:; 
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
goto label_36531;
}
else 
{
if (s__state == 8545)
{
label_36531:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_36576;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_36545;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_36568:; 
label_36576:; 
goto label_36765;
}
}
else 
{
label_36545:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_36559;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_36568;
}
else 
{
label_36559:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38419;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_36568;
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
goto label_36516;
}
else 
{
if (s__state == 8561)
{
label_36516:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38419;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_36765;
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
goto label_38419;
}
else 
{
s__rwstate = 1;
goto label_36505;
}
}
else 
{
label_36505:; 
s__state = s__s3__tmp__next_state___0;
goto label_36765;
}
}
else 
{
if (s__state == 8576)
{
goto label_36473;
}
else 
{
if (s__state == 8577)
{
label_36473:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38419;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_36489;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38419;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_36489:; 
goto label_36765;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_36459;
}
else 
{
if (s__state == 8593)
{
label_36459:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38419;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_36765;
}
}
else 
{
if (s__state == 8608)
{
goto label_36446;
}
else 
{
if (s__state == 8609)
{
label_36446:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38419;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_36765:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38419;
}
else 
{
goto label_36840;
}
}
else 
{
label_36840:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_36915;
}
else 
{
goto label_36915;
}
}
else 
{
goto label_36915;
}
}
}
else 
{
goto label_36915;
}
}
else 
{
label_36915:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_37350;
}
else 
{
if (s__state == 16384)
{
label_37350:; 
goto label_37352;
}
else 
{
if (s__state == 8192)
{
label_37352:; 
goto label_37354;
}
else 
{
if (s__state == 24576)
{
label_37354:; 
goto label_37356;
}
else 
{
if (s__state == 8195)
{
label_37356:; 
s__server = 1;
if (cb != 0)
{
goto label_37361;
}
else 
{
label_37361:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39130 = -1;
goto label_39131;
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
goto label_38421;
}
else 
{
s__init_buf___0 = buf;
goto label_37373;
}
}
else 
{
label_37373:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38421;
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
goto label_38421;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_37393;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_37393:; 
goto label_37395;
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
goto label_37329;
}
else 
{
if (s__state == 8481)
{
label_37329:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38421;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_37395;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_37395;
}
else 
{
if (s__state == 8464)
{
goto label_37308;
}
else 
{
if (s__state == 8465)
{
label_37308:; 
goto label_37310;
}
else 
{
if (s__state == 8466)
{
label_37310:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_37478:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38925;
}
else 
{
label_38925:; 
 __return_39097 = ret;
}
tmp = __return_39097;
goto label_39186;
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
goto label_37478;
}
else 
{
goto label_37468;
}
}
else 
{
label_37468:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_37543;
}
else 
{
goto label_37543;
}
}
else 
{
goto label_37543;
}
}
}
else 
{
goto label_37543;
}
}
else 
{
label_37543:; 
skip = 0;
goto label_11521;
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
goto label_37281;
}
else 
{
if (s__state == 8513)
{
label_37281:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38421;
}
else 
{
goto label_37291;
}
}
else 
{
skip = 1;
label_37291:; 
s__state = 8528;
s__init_num = 0;
goto label_37395;
}
}
else 
{
if (s__state == 8528)
{
goto label_37213;
}
else 
{
if (s__state == 8529)
{
label_37213:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_37222;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_37222:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_37269;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_37256;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_37263:; 
label_37269:; 
s__state = 8544;
s__init_num = 0;
goto label_37395;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_37245;
}
else 
{
tmp___7 = 512;
label_37245:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_37256;
}
else 
{
skip = 1;
goto label_37263;
}
}
}
}
}
}
else 
{
goto label_37256;
}
}
else 
{
label_37256:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_37476:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38927;
}
else 
{
label_38927:; 
 __return_39096 = ret;
}
tmp = __return_39096;
goto label_39200;
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
goto label_37476;
}
else 
{
goto label_37466;
}
}
else 
{
label_37466:; 
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
goto label_37161;
}
else 
{
if (s__state == 8545)
{
label_37161:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_37206;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_37175;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_37198:; 
label_37206:; 
goto label_37395;
}
}
else 
{
label_37175:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_37189;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_37198;
}
else 
{
label_37189:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38421;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_37198;
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
goto label_37146;
}
else 
{
if (s__state == 8561)
{
label_37146:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38421;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_37395;
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
goto label_38421;
}
else 
{
s__rwstate = 1;
goto label_37135;
}
}
else 
{
label_37135:; 
s__state = s__s3__tmp__next_state___0;
goto label_37395;
}
}
else 
{
if (s__state == 8576)
{
goto label_37103;
}
else 
{
if (s__state == 8577)
{
label_37103:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38421;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_37119;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38421;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_37119:; 
goto label_37395;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_37089;
}
else 
{
if (s__state == 8593)
{
label_37089:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38421;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_37395;
}
}
else 
{
if (s__state == 8608)
{
goto label_37076;
}
else 
{
if (s__state == 8609)
{
label_37076:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38421;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_37395:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38421;
}
else 
{
goto label_37470;
}
}
else 
{
label_37470:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_37545;
}
else 
{
goto label_37545;
}
}
else 
{
goto label_37545;
}
}
}
else 
{
goto label_37545;
}
}
else 
{
label_37545:; 
skip = 0;
label_37557:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_37980;
}
else 
{
if (s__state == 16384)
{
label_37980:; 
goto label_37982;
}
else 
{
if (s__state == 8192)
{
label_37982:; 
goto label_37984;
}
else 
{
if (s__state == 24576)
{
label_37984:; 
goto label_37986;
}
else 
{
if (s__state == 8195)
{
label_37986:; 
s__server = 1;
if (cb != 0)
{
goto label_37991;
}
else 
{
label_37991:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39128 = -1;
goto label_39129;
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
goto label_38423;
}
else 
{
s__init_buf___0 = buf;
goto label_38003;
}
}
else 
{
label_38003:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38423;
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
goto label_38423;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_38023;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_38023:; 
goto label_38025;
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
goto label_37959;
}
else 
{
if (s__state == 8481)
{
label_37959:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38423;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_38025;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_38025;
}
else 
{
if (s__state == 8464)
{
goto label_37938;
}
else 
{
if (s__state == 8465)
{
label_37938:; 
goto label_37940;
}
else 
{
if (s__state == 8466)
{
label_37940:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_38108:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38917;
}
else 
{
label_38917:; 
 __return_39101 = ret;
}
tmp = __return_39101;
goto label_39186;
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
goto label_38108;
}
else 
{
goto label_38098;
}
}
else 
{
label_38098:; 
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
goto label_37911;
}
else 
{
if (s__state == 8513)
{
label_37911:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_38423;
}
else 
{
goto label_37921;
}
}
else 
{
skip = 1;
label_37921:; 
s__state = 8528;
s__init_num = 0;
goto label_38025;
}
}
else 
{
if (s__state == 8528)
{
goto label_37843;
}
else 
{
if (s__state == 8529)
{
label_37843:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_37852;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_37852:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_37899;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_37886;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_37893:; 
label_37899:; 
s__state = 8544;
s__init_num = 0;
goto label_38025;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_37875;
}
else 
{
tmp___7 = 512;
label_37875:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_37886;
}
else 
{
skip = 1;
goto label_37893;
}
}
}
}
}
}
else 
{
goto label_37886;
}
}
else 
{
label_37886:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_38106:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38919;
}
else 
{
label_38919:; 
 __return_39100 = ret;
}
tmp = __return_39100;
goto label_39200;
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
goto label_38106;
}
else 
{
goto label_38096;
}
}
else 
{
label_38096:; 
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
goto label_37791;
}
else 
{
if (s__state == 8545)
{
label_37791:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_37836;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_37805;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_37828:; 
label_37836:; 
goto label_38025;
}
}
else 
{
label_37805:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_37819;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_37828;
}
else 
{
label_37819:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_38423;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_37828;
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
goto label_37776;
}
else 
{
if (s__state == 8561)
{
label_37776:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38423;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_38025;
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
goto label_38423;
}
else 
{
s__rwstate = 1;
goto label_37765;
}
}
else 
{
label_37765:; 
s__state = s__s3__tmp__next_state___0;
goto label_38025;
}
}
else 
{
if (s__state == 8576)
{
goto label_37733;
}
else 
{
if (s__state == 8577)
{
label_37733:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38423;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_37749;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38423;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_37749:; 
goto label_38025;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_37719;
}
else 
{
if (s__state == 8593)
{
label_37719:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38423;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_38025;
}
}
else 
{
if (s__state == 8608)
{
goto label_37706;
}
else 
{
if (s__state == 8609)
{
label_37706:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38423;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_38025:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38423;
}
else 
{
goto label_38100;
}
}
else 
{
label_38100:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_38175;
}
else 
{
goto label_38175;
}
}
else 
{
goto label_38175;
}
}
}
else 
{
goto label_38175;
}
}
else 
{
label_38175:; 
skip = 0;
goto label_37557;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_37688;
}
else 
{
if (s__state == 8641)
{
label_37688:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_38104:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38921;
}
else 
{
label_38921:; 
 __return_39099 = ret;
}
tmp = __return_39099;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_37698;
}
else 
{
s__state = 3;
label_37698:; 
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
goto label_38104;
}
else 
{
goto label_38094;
}
}
else 
{
label_38094:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_38169;
}
else 
{
goto label_38169;
}
}
else 
{
goto label_38169;
}
}
}
else 
{
goto label_38169;
}
}
else 
{
label_38169:; 
skip = 0;
goto label_25299;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_37663;
}
else 
{
if (s__state == 8657)
{
label_37663:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38423;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_38102;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_38102:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38923;
}
else 
{
label_38923:; 
 __return_39098 = ret;
}
tmp = __return_39098;
goto label_39190;
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
goto label_38102;
}
else 
{
goto label_38092;
}
}
else 
{
label_38092:; 
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
goto label_37650;
}
else 
{
goto label_37650;
}
}
else 
{
label_37650:; 
ret = 1;
goto label_38423;
}
}
else 
{
ret = -1;
label_38423:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38865;
}
else 
{
label_38865:; 
 __return_39129 = ret;
label_39129:; 
}
tmp = __return_39129;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_37058;
}
else 
{
if (s__state == 8641)
{
label_37058:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_37474:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38929;
}
else 
{
label_38929:; 
 __return_39095 = ret;
}
tmp = __return_39095;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_37068;
}
else 
{
s__state = 3;
label_37068:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_37474;
}
else 
{
goto label_37464;
}
}
else 
{
label_37464:; 
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
goto label_37033;
}
else 
{
if (s__state == 8657)
{
label_37033:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38421;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_37472;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_37472:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38931;
}
else 
{
label_38931:; 
 __return_39094 = ret;
}
tmp = __return_39094;
goto label_39190;
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
goto label_37472;
}
else 
{
goto label_37462;
}
}
else 
{
label_37462:; 
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
goto label_37020;
}
else 
{
goto label_37020;
}
}
else 
{
label_37020:; 
ret = 1;
goto label_38421;
}
}
else 
{
ret = -1;
label_38421:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38867;
}
else 
{
label_38867:; 
 __return_39131 = ret;
label_39131:; 
}
tmp = __return_39131;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_36428;
}
else 
{
if (s__state == 8641)
{
label_36428:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_36844:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38937;
}
else 
{
label_38937:; 
 __return_39091 = ret;
}
tmp = __return_39091;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_36438;
}
else 
{
s__state = 3;
label_36438:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_36844;
}
else 
{
goto label_36834;
}
}
else 
{
label_36834:; 
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
goto label_36403;
}
else 
{
if (s__state == 8657)
{
label_36403:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38419;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_36842;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_36842:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38939;
}
else 
{
label_38939:; 
 __return_39090 = ret;
}
tmp = __return_39090;
goto label_39190;
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
goto label_36842;
}
else 
{
goto label_36832;
}
}
else 
{
label_36832:; 
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
goto label_36390;
}
else 
{
goto label_36390;
}
}
else 
{
label_36390:; 
ret = 1;
goto label_38419;
}
}
else 
{
ret = -1;
label_38419:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38869;
}
else 
{
label_38869:; 
 __return_39133 = ret;
label_39133:; 
}
tmp = __return_39133;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_35799;
}
else 
{
if (s__state == 8641)
{
label_35799:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_36215:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38945;
}
else 
{
label_38945:; 
 __return_39087 = ret;
}
tmp = __return_39087;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_35809;
}
else 
{
s__state = 3;
label_35809:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_36215;
}
else 
{
goto label_36205;
}
}
else 
{
label_36205:; 
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
goto label_35774;
}
else 
{
if (s__state == 8657)
{
label_35774:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38417;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_36213;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_36213:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38947;
}
else 
{
label_38947:; 
 __return_39086 = ret;
}
tmp = __return_39086;
goto label_39190;
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
goto label_36213;
}
else 
{
goto label_36203;
}
}
else 
{
label_36203:; 
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
goto label_35761;
}
else 
{
goto label_35761;
}
}
else 
{
label_35761:; 
ret = 1;
goto label_38417;
}
}
else 
{
ret = -1;
label_38417:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38871;
}
else 
{
label_38871:; 
 __return_39135 = ret;
label_39135:; 
}
tmp = __return_39135;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_35170;
}
else 
{
if (s__state == 8641)
{
label_35170:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_35586:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38953;
}
else 
{
label_38953:; 
 __return_39083 = ret;
}
tmp = __return_39083;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_35180;
}
else 
{
s__state = 3;
label_35180:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_35586;
}
else 
{
goto label_35576;
}
}
else 
{
label_35576:; 
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
goto label_35145;
}
else 
{
if (s__state == 8657)
{
label_35145:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38415;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_35584;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_35584:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38955;
}
else 
{
label_38955:; 
 __return_39082 = ret;
}
tmp = __return_39082;
goto label_39190;
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
goto label_35584;
}
else 
{
goto label_35574;
}
}
else 
{
label_35574:; 
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
goto label_35132;
}
else 
{
goto label_35132;
}
}
else 
{
label_35132:; 
ret = 1;
goto label_38415;
}
}
else 
{
ret = -1;
label_38415:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38873;
}
else 
{
label_38873:; 
 __return_39137 = ret;
label_39137:; 
}
tmp = __return_39137;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_34541;
}
else 
{
if (s__state == 8641)
{
label_34541:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_34957:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38961;
}
else 
{
label_38961:; 
 __return_39079 = ret;
}
tmp = __return_39079;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_34551;
}
else 
{
s__state = 3;
label_34551:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34957;
}
else 
{
goto label_34947;
}
}
else 
{
label_34947:; 
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
goto label_34516;
}
else 
{
if (s__state == 8657)
{
label_34516:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38413;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_34955;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_34955:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38963;
}
else 
{
label_38963:; 
 __return_39078 = ret;
}
tmp = __return_39078;
goto label_39190;
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
goto label_34955;
}
else 
{
goto label_34945;
}
}
else 
{
label_34945:; 
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
goto label_34503;
}
else 
{
goto label_34503;
}
}
else 
{
label_34503:; 
ret = 1;
goto label_38413;
}
}
else 
{
ret = -1;
label_38413:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38875;
}
else 
{
label_38875:; 
 __return_39139 = ret;
label_39139:; 
}
tmp = __return_39139;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_21281;
}
else 
{
if (s__state == 8641)
{
label_21281:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_21697:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39017;
}
else 
{
label_39017:; 
 __return_39051 = ret;
}
tmp = __return_39051;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_21291;
}
else 
{
s__state = 3;
label_21291:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_21697;
}
else 
{
goto label_21687;
}
}
else 
{
label_21687:; 
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
goto label_21256;
}
else 
{
if (s__state == 8657)
{
label_21256:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38387;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_21695;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_21695:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39019;
}
else 
{
label_39019:; 
 __return_39050 = ret;
}
tmp = __return_39050;
goto label_39190;
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
goto label_21695;
}
else 
{
goto label_21685;
}
}
else 
{
label_21685:; 
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
goto label_21243;
}
else 
{
goto label_21243;
}
}
else 
{
label_21243:; 
ret = 1;
goto label_38387;
}
}
else 
{
ret = -1;
label_38387:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38901;
}
else 
{
label_38901:; 
 __return_39165 = ret;
label_39165:; 
}
tmp = __return_39165;
goto label_39188;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_12136;
}
else 
{
if (s__state == 8641)
{
label_12136:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_12552:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39033;
}
else 
{
label_39033:; 
 __return_39043 = ret;
}
tmp = __return_39043;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_12146;
}
else 
{
s__state = 3;
label_12146:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_12552;
}
else 
{
goto label_12542;
}
}
else 
{
label_12542:; 
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
goto label_12111;
}
else 
{
if (s__state == 8657)
{
label_12111:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38377;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_12550;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_12550:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39035;
}
else 
{
label_39035:; 
 __return_39042 = ret;
}
tmp = __return_39042;
goto label_39190;
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
goto label_12550;
}
else 
{
goto label_12540;
}
}
else 
{
label_12540:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12615;
}
else 
{
goto label_12615;
}
}
else 
{
goto label_12615;
}
}
}
else 
{
goto label_12615;
}
}
else 
{
label_12615:; 
skip = 0;
label_17297:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_17673;
}
else 
{
if (s__state == 16384)
{
label_17673:; 
goto label_17675;
}
else 
{
if (s__state == 8192)
{
label_17675:; 
goto label_17677;
}
else 
{
if (s__state == 24576)
{
label_17677:; 
goto label_17679;
}
else 
{
if (s__state == 8195)
{
label_17679:; 
s__server = 1;
if (cb != 0)
{
goto label_17684;
}
else 
{
label_17684:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39172 = -1;
goto label_39173;
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
goto label_38379;
}
else 
{
s__init_buf___0 = buf;
goto label_17696;
}
}
else 
{
label_17696:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38379;
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
goto label_38379;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_17716;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_17716:; 
goto label_17718;
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
goto label_17652;
}
else 
{
if (s__state == 8481)
{
label_17652:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38379;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_17718;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_17718;
}
else 
{
if (s__state == 8464)
{
goto label_17639;
}
else 
{
if (s__state == 8465)
{
label_17639:; 
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
goto label_17619;
}
else 
{
if (s__state == 8513)
{
label_17619:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_17718;
}
else 
{
if (s__state == 8528)
{
goto label_17561;
}
else 
{
if (s__state == 8529)
{
label_17561:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_17570;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_17570:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_17610;
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
label_17606:; 
label_17610:; 
s__state = 8544;
s__init_num = 0;
goto label_17718;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_17593;
}
else 
{
tmp___7 = 512;
label_17593:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_17606;
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
goto label_17519;
}
else 
{
if (s__state == 8545)
{
label_17519:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_17554;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_17533;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_17545:; 
label_17554:; 
goto label_17718;
}
}
else 
{
label_17533:; 
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
goto label_17545;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_17504;
}
else 
{
if (s__state == 8561)
{
label_17504:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38379;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_17718;
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
goto label_38379;
}
else 
{
s__rwstate = 1;
goto label_17493;
}
}
else 
{
label_17493:; 
s__state = s__s3__tmp__next_state___0;
goto label_17718;
}
}
else 
{
if (s__state == 8576)
{
goto label_17461;
}
else 
{
if (s__state == 8577)
{
label_17461:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38379;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_17477;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38379;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_17477:; 
goto label_17718;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_17447;
}
else 
{
if (s__state == 8593)
{
label_17447:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38379;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_17718;
}
}
else 
{
if (s__state == 8608)
{
goto label_17434;
}
else 
{
if (s__state == 8609)
{
label_17434:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38379;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_17718:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38379;
}
else 
{
goto label_17748;
}
}
else 
{
label_17748:; 
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
goto label_17417;
}
else 
{
if (s__state == 8657)
{
label_17417:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38379;
}
else 
{
if (s__state == 8672)
{
goto label_17398;
}
else 
{
if (s__state == 8673)
{
label_17398:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_17750:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39027;
}
else 
{
label_39027:; 
 __return_39046 = ret;
}
tmp = __return_39046;
goto label_39192;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_17409;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_17409:; 
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
goto label_17750;
}
else 
{
goto label_17746;
}
}
else 
{
label_17746:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17776;
}
else 
{
goto label_17776;
}
}
else 
{
goto label_17776;
}
}
}
else 
{
goto label_17776;
}
}
else 
{
label_17776:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_18157;
}
else 
{
if (s__state == 16384)
{
label_18157:; 
goto label_18159;
}
else 
{
if (s__state == 8192)
{
label_18159:; 
goto label_18161;
}
else 
{
if (s__state == 24576)
{
label_18161:; 
goto label_18163;
}
else 
{
if (s__state == 8195)
{
label_18163:; 
s__server = 1;
if (cb != 0)
{
goto label_18168;
}
else 
{
label_18168:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39170 = -1;
goto label_39171;
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
goto label_38381;
}
else 
{
s__init_buf___0 = buf;
goto label_18180;
}
}
else 
{
label_18180:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38381;
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
goto label_38381;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_18200;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_18200:; 
goto label_18202;
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
goto label_18136;
}
else 
{
if (s__state == 8481)
{
label_18136:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38381;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_18202;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_18202;
}
else 
{
if (s__state == 8464)
{
goto label_18123;
}
else 
{
if (s__state == 8465)
{
label_18123:; 
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
goto label_18103;
}
else 
{
if (s__state == 8513)
{
label_18103:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_18202;
}
else 
{
if (s__state == 8528)
{
goto label_18045;
}
else 
{
if (s__state == 8529)
{
label_18045:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_18054;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_18054:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_18094;
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
label_18090:; 
label_18094:; 
s__state = 8544;
s__init_num = 0;
goto label_18202;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_18077;
}
else 
{
tmp___7 = 512;
label_18077:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_18090;
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
goto label_18003;
}
else 
{
if (s__state == 8545)
{
label_18003:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_18038;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_18017;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_18029:; 
label_18038:; 
goto label_18202;
}
}
else 
{
label_18017:; 
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
goto label_18029;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_17988;
}
else 
{
if (s__state == 8561)
{
label_17988:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38381;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_18202;
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
goto label_38381;
}
else 
{
s__rwstate = 1;
goto label_17977;
}
}
else 
{
label_17977:; 
s__state = s__s3__tmp__next_state___0;
goto label_18202;
}
}
else 
{
if (s__state == 8576)
{
goto label_17945;
}
else 
{
if (s__state == 8577)
{
label_17945:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38381;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_17961;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38381;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_17961:; 
goto label_18202;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_17931;
}
else 
{
if (s__state == 8593)
{
label_17931:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38381;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_18202;
}
}
else 
{
if (s__state == 8608)
{
goto label_17918;
}
else 
{
if (s__state == 8609)
{
label_17918:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38381;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_18202:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38381;
}
else 
{
goto label_18232;
}
}
else 
{
label_18232:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18262;
}
else 
{
goto label_18262;
}
}
else 
{
goto label_18262;
}
}
}
else 
{
goto label_18262;
}
}
else 
{
label_18262:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_18642;
}
else 
{
if (s__state == 16384)
{
label_18642:; 
goto label_18644;
}
else 
{
if (s__state == 8192)
{
label_18644:; 
goto label_18646;
}
else 
{
if (s__state == 24576)
{
label_18646:; 
goto label_18648;
}
else 
{
if (s__state == 8195)
{
label_18648:; 
s__server = 1;
if (cb != 0)
{
goto label_18653;
}
else 
{
label_18653:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39168 = -1;
goto label_39169;
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
goto label_38383;
}
else 
{
s__init_buf___0 = buf;
goto label_18665;
}
}
else 
{
label_18665:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38383;
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
goto label_38383;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_18685;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_18685:; 
goto label_18687;
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
goto label_18621;
}
else 
{
if (s__state == 8481)
{
label_18621:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38383;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_18687;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_18687;
}
else 
{
if (s__state == 8464)
{
goto label_18608;
}
else 
{
if (s__state == 8465)
{
label_18608:; 
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
goto label_18588;
}
else 
{
if (s__state == 8513)
{
label_18588:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_18687;
}
else 
{
if (s__state == 8528)
{
goto label_18530;
}
else 
{
if (s__state == 8529)
{
label_18530:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_18539;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_18539:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_18579;
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
label_18575:; 
label_18579:; 
s__state = 8544;
s__init_num = 0;
goto label_18687;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_18562;
}
else 
{
tmp___7 = 512;
label_18562:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_18575;
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
goto label_18488;
}
else 
{
if (s__state == 8545)
{
label_18488:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_18523;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_18502;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_18514:; 
label_18523:; 
goto label_18687;
}
}
else 
{
label_18502:; 
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
goto label_18514;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_18473;
}
else 
{
if (s__state == 8561)
{
label_18473:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38383;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_18687;
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
goto label_38383;
}
else 
{
s__rwstate = 1;
goto label_18462;
}
}
else 
{
label_18462:; 
s__state = s__s3__tmp__next_state___0;
goto label_18687;
}
}
else 
{
if (s__state == 8576)
{
goto label_18430;
}
else 
{
if (s__state == 8577)
{
label_18430:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38383;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_18446;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38383;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_18446:; 
goto label_18687;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_18416;
}
else 
{
if (s__state == 8593)
{
label_18416:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38383;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_18687;
}
}
else 
{
if (s__state == 8608)
{
goto label_18403;
}
else 
{
if (s__state == 8609)
{
label_18403:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38383;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_18687:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38383;
}
else 
{
goto label_18717;
}
}
else 
{
label_18717:; 
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
goto label_18385;
}
else 
{
if (s__state == 8641)
{
label_18385:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_18719:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39023;
}
else 
{
label_39023:; 
 __return_39048 = ret;
}
tmp = __return_39048;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_18395;
}
else 
{
s__state = 3;
label_18395:; 
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
goto label_18719;
}
else 
{
goto label_18715;
}
}
else 
{
label_18715:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18745;
}
else 
{
goto label_18745;
}
}
else 
{
goto label_18745;
}
}
}
else 
{
goto label_18745;
}
}
else 
{
label_18745:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_19126;
}
else 
{
if (s__state == 16384)
{
label_19126:; 
goto label_19128;
}
else 
{
if (s__state == 8192)
{
label_19128:; 
goto label_19130;
}
else 
{
if (s__state == 24576)
{
label_19130:; 
goto label_19132;
}
else 
{
if (s__state == 8195)
{
label_19132:; 
s__server = 1;
if (cb != 0)
{
goto label_19137;
}
else 
{
label_19137:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_39166 = -1;
goto label_39167;
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
goto label_38385;
}
else 
{
s__init_buf___0 = buf;
goto label_19149;
}
}
else 
{
label_19149:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_38385;
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
goto label_38385;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_19169;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_19169:; 
goto label_19171;
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
goto label_19105;
}
else 
{
if (s__state == 8481)
{
label_19105:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_38385;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_19171;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_19171;
}
else 
{
if (s__state == 8464)
{
goto label_19092;
}
else 
{
if (s__state == 8465)
{
label_19092:; 
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
goto label_19072;
}
else 
{
if (s__state == 8513)
{
label_19072:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_19171;
}
else 
{
if (s__state == 8528)
{
goto label_19014;
}
else 
{
if (s__state == 8529)
{
label_19014:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_19023;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_19023:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_19063;
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
label_19059:; 
label_19063:; 
s__state = 8544;
s__init_num = 0;
goto label_19171;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_19046;
}
else 
{
tmp___7 = 512;
label_19046:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_19059;
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
goto label_18972;
}
else 
{
if (s__state == 8545)
{
label_18972:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_19007;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_18986;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_18998:; 
label_19007:; 
goto label_19171;
}
}
else 
{
label_18986:; 
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
goto label_18998;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_18957;
}
else 
{
if (s__state == 8561)
{
label_18957:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38385;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_19171;
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
goto label_38385;
}
else 
{
s__rwstate = 1;
goto label_18946;
}
}
else 
{
label_18946:; 
s__state = s__s3__tmp__next_state___0;
goto label_19171;
}
}
else 
{
if (s__state == 8576)
{
goto label_18914;
}
else 
{
if (s__state == 8577)
{
label_18914:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38385;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_18930;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38385;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_18930:; 
goto label_19171;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_18900;
}
else 
{
if (s__state == 8593)
{
label_18900:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38385;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_19171;
}
}
else 
{
if (s__state == 8608)
{
goto label_18887;
}
else 
{
if (s__state == 8609)
{
label_18887:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38385;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_19171:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38385;
}
else 
{
goto label_19201;
}
}
else 
{
label_19201:; 
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
goto label_18857;
}
else 
{
if (s__state == 8657)
{
label_18857:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_38385;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_19203;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_19203:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39021;
}
else 
{
label_39021:; 
 __return_39049 = ret;
}
tmp = __return_39049;
goto label_39190;
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
goto label_19203;
}
else 
{
goto label_19199;
}
}
else 
{
label_19199:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19229;
}
else 
{
goto label_19229;
}
}
else 
{
goto label_19229;
}
}
}
else 
{
goto label_19229;
}
}
else 
{
label_19229:; 
skip = 0;
goto label_17297;
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
goto label_18844;
}
else 
{
goto label_18844;
}
}
else 
{
label_18844:; 
ret = 1;
goto label_38385;
}
}
else 
{
ret = -1;
label_38385:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38903;
}
else 
{
label_38903:; 
 __return_39167 = ret;
label_39167:; 
}
tmp = __return_39167;
label_39196:; 
 __return_39362 = tmp;
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
goto label_18373;
}
else 
{
if (s__state == 8657)
{
label_18373:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38383;
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
goto label_18360;
}
else 
{
goto label_18360;
}
}
else 
{
label_18360:; 
ret = 1;
goto label_38383;
}
}
else 
{
ret = -1;
label_38383:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38905;
}
else 
{
label_38905:; 
 __return_39169 = ret;
label_39169:; 
}
tmp = __return_39169;
goto label_39192;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_17900;
}
else 
{
if (s__state == 8641)
{
label_17900:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_18234:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_39025;
}
else 
{
label_39025:; 
 __return_39047 = ret;
}
tmp = __return_39047;
goto label_39196;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_17910;
}
else 
{
s__state = 3;
label_17910:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_18234;
}
else 
{
goto label_18230;
}
}
else 
{
label_18230:; 
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
goto label_17888;
}
else 
{
if (s__state == 8657)
{
label_17888:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38381;
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
goto label_17875;
}
else 
{
goto label_17875;
}
}
else 
{
label_17875:; 
ret = 1;
goto label_38381;
}
}
else 
{
ret = -1;
label_38381:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38907;
}
else 
{
label_38907:; 
 __return_39171 = ret;
label_39171:; 
}
tmp = __return_39171;
label_39192:; 
 __return_39364 = tmp;
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
goto label_17390;
}
else 
{
goto label_17390;
}
}
else 
{
label_17390:; 
ret = 1;
goto label_38379;
}
}
else 
{
ret = -1;
label_38379:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38909;
}
else 
{
label_38909:; 
 __return_39173 = ret;
label_39173:; 
}
tmp = __return_39173;
label_39190:; 
 __return_39366 = tmp;
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
goto label_12098;
}
else 
{
goto label_12098;
}
}
else 
{
label_12098:; 
ret = 1;
goto label_38377;
}
}
else 
{
ret = -1;
label_38377:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38911;
}
else 
{
label_38911:; 
 __return_39175 = ret;
label_39175:; 
}
tmp = __return_39175;
label_39188:; 
 __return_39368 = tmp;
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
goto label_11829;
}
else 
{
if (s__state == 8513)
{
label_11829:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_11941;
}
else 
{
if (s__state == 8528)
{
goto label_11771;
}
else 
{
if (s__state == 8529)
{
label_11771:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_11780;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_11780:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_11820;
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
label_11816:; 
label_11820:; 
s__state = 8544;
s__init_num = 0;
goto label_11941;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_11803;
}
else 
{
tmp___7 = 512;
label_11803:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_11816;
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
goto label_11729;
}
else 
{
if (s__state == 8545)
{
label_11729:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_11764;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_11743;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_11755:; 
label_11764:; 
goto label_11941;
}
}
else 
{
label_11743:; 
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
goto label_11755;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_11714;
}
else 
{
if (s__state == 8561)
{
label_11714:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38375;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_11941;
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
goto label_38375;
}
else 
{
s__rwstate = 1;
goto label_11703;
}
}
else 
{
label_11703:; 
s__state = s__s3__tmp__next_state___0;
goto label_11941;
}
}
else 
{
if (s__state == 8576)
{
goto label_11671;
}
else 
{
if (s__state == 8577)
{
label_11671:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38375;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_11687;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38375;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_11687:; 
goto label_11941;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_11657;
}
else 
{
if (s__state == 8593)
{
label_11657:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38375;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_11941;
}
}
else 
{
if (s__state == 8608)
{
goto label_11644;
}
else 
{
if (s__state == 8609)
{
label_11644:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38375;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_11941:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38375;
}
else 
{
goto label_11971;
}
}
else 
{
label_11971:; 
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
goto label_11627;
}
else 
{
if (s__state == 8657)
{
label_11627:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38375;
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
goto label_11614;
}
else 
{
goto label_11614;
}
}
else 
{
label_11614:; 
ret = 1;
goto label_38375;
}
}
else 
{
ret = -1;
label_38375:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38913;
}
else 
{
label_38913:; 
 __return_39177 = ret;
label_39177:; 
}
tmp = __return_39177;
label_39186:; 
 __return_39370 = tmp;
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
goto label_657;
}
else 
{
if (s__state == 8513)
{
label_657:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_764;
}
else 
{
if (s__state == 8528)
{
goto label_599;
}
else 
{
if (s__state == 8529)
{
label_599:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_608;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_608:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_648;
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
label_644:; 
label_648:; 
s__state = 8544;
s__init_num = 0;
goto label_764;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_631;
}
else 
{
tmp___7 = 512;
label_631:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_644;
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
goto label_557;
}
else 
{
if (s__state == 8545)
{
label_557:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_592;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_571;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_583:; 
label_592:; 
goto label_764;
}
}
else 
{
label_571:; 
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
goto label_583;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_542;
}
else 
{
if (s__state == 8561)
{
label_542:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_38373;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_764;
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
goto label_38373;
}
else 
{
s__rwstate = 1;
goto label_531;
}
}
else 
{
label_531:; 
s__state = s__s3__tmp__next_state___0;
goto label_764;
}
}
else 
{
if (s__state == 8576)
{
goto label_499;
}
else 
{
if (s__state == 8577)
{
label_499:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_38373;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_515;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_38373;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_515:; 
goto label_764;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_485;
}
else 
{
if (s__state == 8593)
{
label_485:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_38373;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_764;
}
}
else 
{
if (s__state == 8608)
{
goto label_472;
}
else 
{
if (s__state == 8609)
{
label_472:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_38373;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_764:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_38373;
}
else 
{
goto label_794;
}
}
else 
{
label_794:; 
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
goto label_455;
}
else 
{
if (s__state == 8657)
{
label_455:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_38373;
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
goto label_442;
}
else 
{
goto label_442;
}
}
else 
{
label_442:; 
ret = 1;
goto label_38373;
}
}
else 
{
ret = -1;
label_38373:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_38915;
}
else 
{
label_38915:; 
 __return_39179 = ret;
label_39179:; 
}
tmp = __return_39179;
goto label_39182;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_39180;
label_39182:; 
 __return_39372 = tmp;
return 1;
}
}
}
}
