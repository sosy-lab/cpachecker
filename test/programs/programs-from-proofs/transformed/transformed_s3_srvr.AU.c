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
int __return_1515=0;
int __return_1527=0;
int __return_1513=0;
int __return_1511=0;
int __return_1510=0;
int __return_1509=0;
int __return_1508=0;
int __return_1507=0;
int __return_1512=0;
int __return_1506=0;
int __return_1505=0;
int __return_1514=0;
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
goto label_460;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_460;
}
else 
{
label_460:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_1515 = -1;
}
else 
{
state = s__state;
if (s__state == 8464)
{
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_514;
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
if (s__debug == 0)
{
label_535:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_531;
}
else 
{
goto label_531;
}
}
else 
{
goto label_531;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_541:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1478;
}
else 
{
label_1478:; 
 __return_1513 = ret;
}
tmp = __return_1513;
goto label_1516;
}
else 
{
goto label_535;
}
}
}
else 
{
goto label_531;
}
}
else 
{
label_531:; 
skip = 0;
state = s__state;
if (s__state == 8496)
{
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_541;
}
else 
{
if (s__hit == 0)
{
s__state = 8512;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_628:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_619;
}
else 
{
goto label_619;
}
}
else 
{
goto label_619;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_641:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1482;
}
else 
{
label_1482:; 
 __return_1511 = ret;
}
tmp = __return_1511;
goto label_1516;
}
else 
{
goto label_628;
}
}
}
else 
{
goto label_619;
}
}
else 
{
label_619:; 
skip = 0;
state = s__state;
if (s__state == 8512)
{
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_641;
}
else 
{
goto label_715;
}
}
else 
{
skip = 1;
label_715:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_731:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_727;
}
else 
{
goto label_727;
}
}
else 
{
goto label_727;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_737:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1484;
}
else 
{
label_1484:; 
 __return_1510 = ret;
}
tmp = __return_1510;
goto label_1516;
}
else 
{
goto label_731;
}
}
}
else 
{
goto label_727;
}
}
else 
{
label_727:; 
skip = 0;
state = s__state;
if (s__state == 8528)
{
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_801;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_801:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_840;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_806;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_832:; 
goto label_840;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_822;
}
else 
{
tmp___7 = 512;
label_822:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_806;
}
else 
{
skip = 1;
goto label_832;
}
}
}
}
}
}
else 
{
goto label_806;
}
}
else 
{
label_806:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_737;
}
else 
{
label_840:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_855:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_851;
}
else 
{
goto label_851;
}
}
else 
{
goto label_851;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_861:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1486;
}
else 
{
label_1486:; 
 __return_1509 = ret;
}
tmp = __return_1509;
goto label_1516;
}
else 
{
goto label_855;
}
}
}
else 
{
goto label_851;
}
}
else 
{
label_851:; 
skip = 0;
state = s__state;
if (s__state == 8544)
{
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_954;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_924;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_931:; 
label_954:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_978:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_969;
}
else 
{
goto label_969;
}
}
else 
{
goto label_969;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_988:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1488;
}
else 
{
label_1488:; 
 __return_1508 = ret;
}
tmp = __return_1508;
goto label_1516;
}
else 
{
goto label_978;
}
}
}
else 
{
goto label_969;
}
}
else 
{
label_969:; 
skip = 0;
state = s__state;
if (s__state == 8560)
{
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_988;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
label_1124:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1133:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1129;
}
else 
{
goto label_1129;
}
}
else 
{
goto label_1129;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1139:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_1446:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1490;
}
else 
{
label_1490:; 
 __return_1507 = ret;
}
tmp = __return_1507;
goto label_1516;
}
else 
{
goto label_1133;
}
}
}
else 
{
goto label_1129;
}
}
else 
{
label_1129:; 
skip = 0;
state = s__state;
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
goto label_1139;
}
else 
{
s__rwstate = 1;
goto label_1199;
}
}
else 
{
label_1199:; 
s__state = s__s3__tmp__next_state___0;
goto label_1124;
}
}
else 
{
ret = -1;
goto label_1139;
}
}
}
}
else 
{
ret = -1;
goto label_988;
}
}
}
}
else 
{
label_924:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_938;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_931;
}
else 
{
label_938:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_861;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
label_961:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_976:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_967;
}
else 
{
goto label_967;
}
}
else 
{
goto label_967;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_989:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_1446;
}
else 
{
goto label_976;
}
}
}
else 
{
goto label_967;
}
}
else 
{
label_967:; 
skip = 0;
state = s__state;
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
goto label_989;
}
else 
{
s__rwstate = 1;
goto label_1060;
}
}
else 
{
label_1060:; 
s__state = s__s3__tmp__next_state___0;
goto label_961;
}
}
else 
{
ret = -1;
goto label_989;
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
ret = -1;
goto label_861;
}
}
}
}
}
}
else 
{
ret = -1;
goto label_737;
}
}
}
}
else 
{
ret = -1;
goto label_641;
}
}
}
else 
{
s__state = 8656;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_630:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_621;
}
else 
{
goto label_621;
}
}
else 
{
goto label_621;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_640:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1480;
}
else 
{
label_1480:; 
 __return_1512 = ret;
}
tmp = __return_1512;
goto label_1516;
}
else 
{
goto label_630;
}
}
}
else 
{
goto label_621;
}
}
else 
{
label_621:; 
skip = 0;
state = s__state;
if (s__state == 8656)
{
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_640;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_640;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_1271:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1492;
}
else 
{
label_1492:; 
 __return_1506 = ret;
}
tmp = __return_1506;
goto label_1516;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1282:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1278;
}
else 
{
goto label_1278;
}
}
else 
{
goto label_1278;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_1271;
}
else 
{
goto label_1282;
}
}
}
else 
{
goto label_1278;
}
}
else 
{
label_1278:; 
skip = 0;
state = s__state;
if (s__state == 8672)
{
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_1271;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_1354;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_1354:; 
s__init_num = 0;
label_1358:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1367:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1363;
}
else 
{
goto label_1363;
}
}
else 
{
goto label_1363;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1373:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1494;
}
else 
{
label_1494:; 
 __return_1505 = ret;
}
tmp = __return_1505;
goto label_1516;
}
else 
{
goto label_1367;
}
}
}
else 
{
goto label_1363;
}
}
else 
{
label_1363:; 
skip = 0;
state = s__state;
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
goto label_1373;
}
else 
{
s__rwstate = 1;
goto label_1433;
}
}
else 
{
label_1433:; 
s__state = s__s3__tmp__next_state___0;
goto label_1358;
}
}
else 
{
ret = -1;
goto label_1373;
}
}
}
}
}
else 
{
ret = -1;
goto label_1271;
}
}
}
}
}
}
else 
{
ret = -1;
goto label_640;
}
}
}
}
}
else 
{
ret = -1;
goto label_541;
}
}
}
}
else 
{
ret = -1;
label_514:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1476;
}
else 
{
label_1476:; 
 __return_1514 = ret;
}
tmp = __return_1514;
goto label_1516;
}
}
tmp = __return_1515;
label_1516:; 
 __return_1527 = tmp;
return 1;
}
}
}
}
