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
int __return_35644;
int __return_35828;
int __return_35642;
int __return_35512;
int __return_35640;
int __return_35513;
int __return_35638;
int __return_35517;
int __return_35516;
int __return_35628;
int __return_35525;
int __return_35524;
int __return_35626;
int __return_35528;
int __return_35624;
int __return_35531;
int __return_35622;
int __return_35534;
int __return_35620;
int __return_35537;
int __return_35618;
int __return_35540;
int __return_35616;
int __return_35543;
int __return_35542;
int __return_35614;
int __return_35544;
int __return_35612;
int __return_35545;
int __return_35610;
int __return_35608;
int __return_35547;
int __return_35609;
int __return_35546;
int __return_35611;
int __return_35613;
int __return_35615;
int __return_35541;
int __return_35617;
int __return_35539;
int __return_35538;
int __return_35619;
int __return_35536;
int __return_35535;
int __return_35621;
int __return_35533;
int __return_35532;
int __return_35623;
int __return_35530;
int __return_35529;
int __return_35625;
int __return_35527;
int __return_35526;
int __return_35627;
int __return_35816;
int __return_35606;
int __return_35551;
int __return_35550;
int __return_35604;
int __return_35555;
int __return_35554;
int __return_35602;
int __return_35559;
int __return_35558;
int __return_35600;
int __return_35563;
int __return_35562;
int __return_35598;
int __return_35567;
int __return_35566;
int __return_35596;
int __return_35571;
int __return_35570;
int __return_35569;
int __return_35568;
int __return_35597;
int __return_35565;
int __return_35564;
int __return_35599;
int __return_35561;
int __return_35560;
int __return_35601;
int __return_35557;
int __return_35556;
int __return_35603;
int __return_35553;
int __return_35552;
int __return_35605;
int __return_35549;
int __return_35548;
int __return_35607;
int __return_35523;
int __return_35522;
int __return_35629;
int __return_35515;
int __return_35514;
int __return_35636;
int __return_35518;
int __return_35634;
int __return_35632;
int __return_35520;
int __return_35630;
int __return_35521;
int __return_35631;
int __return_35818;
int __return_35633;
int __return_35519;
int __return_35635;
int __return_35820;
int __return_35637;
int __return_35822;
int __return_35639;
int __return_35824;
int __return_35641;
int __return_35826;
int __return_35643;
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
goto label_21292;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_21292;
}
else 
{
label_21292:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_35644 = -1;
}
else 
{
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_21670;
}
else 
{
if (s__state == 16384)
{
label_21670:; 
goto label_21672;
}
else 
{
if (s__state == 8192)
{
label_21672:; 
goto label_21674;
}
else 
{
if (s__state == 24576)
{
label_21674:; 
goto label_21676;
}
else 
{
if (s__state == 8195)
{
label_21676:; 
s__server = 1;
if (cb != 0)
{
goto label_21681;
}
else 
{
label_21681:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35642 = -1;
goto label_35643;
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
goto label_34877;
}
else 
{
s__init_buf___0 = buf;
goto label_21693;
}
}
else 
{
label_21693:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34877;
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
goto label_34877;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_21713;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_21713:; 
goto label_21715;
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
goto label_21649;
}
else 
{
if (s__state == 8481)
{
label_21649:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34877;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_21715;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_21715;
}
else 
{
if (s__state == 8464)
{
goto label_21628;
}
else 
{
if (s__state == 8465)
{
label_21628:; 
goto label_21630;
}
else 
{
if (s__state == 8466)
{
label_21630:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_21747:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35511;
}
else 
{
label_35511:; 
 __return_35512 = ret;
}
tmp = __return_35512;
goto label_35650;
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
goto label_21747;
}
else 
{
goto label_21743;
}
}
else 
{
label_21743:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_21773;
}
else 
{
goto label_21773;
}
}
else 
{
goto label_21773;
}
}
}
else 
{
goto label_21773;
}
}
else 
{
label_21773:; 
skip = 0;
label_21779:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_22154;
}
else 
{
if (s__state == 16384)
{
label_22154:; 
goto label_22156;
}
else 
{
if (s__state == 8192)
{
label_22156:; 
goto label_22158;
}
else 
{
if (s__state == 24576)
{
label_22158:; 
goto label_22160;
}
else 
{
if (s__state == 8195)
{
label_22160:; 
s__server = 1;
if (cb != 0)
{
goto label_22165;
}
else 
{
label_22165:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35640 = -1;
goto label_35641;
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
goto label_34879;
}
else 
{
s__init_buf___0 = buf;
goto label_22177;
}
}
else 
{
label_22177:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34879;
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
goto label_34879;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_22197;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_22197:; 
goto label_22199;
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
goto label_22133;
}
else 
{
if (s__state == 8481)
{
label_22133:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34879;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_22199;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_22199;
}
else 
{
if (s__state == 8464)
{
goto label_22120;
}
else 
{
if (s__state == 8465)
{
label_22120:; 
return 1;
}
else 
{
if (s__state == 8496)
{
goto label_22101;
}
else 
{
if (s__state == 8497)
{
label_22101:; 
ret = ssl3_send_server_hello();
if (ret <= 0)
{
label_22231:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35509;
}
else 
{
label_35509:; 
 __return_35513 = ret;
}
tmp = __return_35513;
goto label_35652;
}
else 
{
if (s__hit == 0)
{
s__state = 8512;
goto label_22111;
}
else 
{
s__state = 8656;
label_22111:; 
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
goto label_22231;
}
else 
{
goto label_22227;
}
}
else 
{
label_22227:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22257;
}
else 
{
goto label_22257;
}
}
else 
{
goto label_22257;
}
}
}
else 
{
goto label_22257;
}
}
else 
{
label_22257:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_22686;
}
else 
{
if (s__state == 16384)
{
label_22686:; 
goto label_22688;
}
else 
{
if (s__state == 8192)
{
label_22688:; 
goto label_22690;
}
else 
{
if (s__state == 24576)
{
label_22690:; 
goto label_22692;
}
else 
{
if (s__state == 8195)
{
label_22692:; 
s__server = 1;
if (cb != 0)
{
goto label_22697;
}
else 
{
label_22697:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35638 = -1;
goto label_35639;
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
goto label_34881;
}
else 
{
s__init_buf___0 = buf;
goto label_22709;
}
}
else 
{
label_22709:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34881;
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
goto label_34881;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_22729;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_22729:; 
goto label_22731;
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
goto label_22665;
}
else 
{
if (s__state == 8481)
{
label_22665:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34881;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_22731;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_22731;
}
else 
{
if (s__state == 8464)
{
goto label_22644;
}
else 
{
if (s__state == 8465)
{
label_22644:; 
goto label_22646;
}
else 
{
if (s__state == 8466)
{
label_22646:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_22814:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35501;
}
else 
{
label_35501:; 
 __return_35517 = ret;
}
tmp = __return_35517;
goto label_35650;
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
goto label_22814;
}
else 
{
goto label_22804;
}
}
else 
{
label_22804:; 
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
goto label_22617;
}
else 
{
if (s__state == 8513)
{
label_22617:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34881;
}
else 
{
goto label_22627;
}
}
else 
{
skip = 1;
label_22627:; 
s__state = 8528;
s__init_num = 0;
goto label_22731;
}
}
else 
{
if (s__state == 8528)
{
goto label_22549;
}
else 
{
if (s__state == 8529)
{
label_22549:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_22558;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_22558:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_22605;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_22592;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_22599:; 
label_22605:; 
s__state = 8544;
s__init_num = 0;
goto label_22731;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_22581;
}
else 
{
tmp___7 = 512;
label_22581:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_22592;
}
else 
{
skip = 1;
goto label_22599;
}
}
}
}
}
}
else 
{
goto label_22592;
}
}
else 
{
label_22592:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_22812:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35503;
}
else 
{
label_35503:; 
 __return_35516 = ret;
}
tmp = __return_35516;
goto label_35664;
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
goto label_22812;
}
else 
{
goto label_22802;
}
}
else 
{
label_22802:; 
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
goto label_22497;
}
else 
{
if (s__state == 8545)
{
label_22497:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22542;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_22511;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_22534:; 
label_22542:; 
goto label_22731;
}
}
else 
{
label_22511:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_22525;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22534;
}
else 
{
label_22525:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34881;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_22534;
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
goto label_22482;
}
else 
{
if (s__state == 8561)
{
label_22482:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34881;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_22731;
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
goto label_34881;
}
else 
{
s__rwstate = 1;
goto label_22471;
}
}
else 
{
label_22471:; 
s__state = s__s3__tmp__next_state___0;
goto label_22731;
}
}
else 
{
if (s__state == 8576)
{
goto label_22439;
}
else 
{
if (s__state == 8577)
{
label_22439:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34881;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_22455;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34881;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_22455:; 
goto label_22731;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_22425;
}
else 
{
if (s__state == 8593)
{
label_22425:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34881;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_22731;
}
}
else 
{
if (s__state == 8608)
{
goto label_22412;
}
else 
{
if (s__state == 8609)
{
label_22412:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34881;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_22731:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34881;
}
else 
{
goto label_22806;
}
}
else 
{
label_22806:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22881;
}
else 
{
goto label_22881;
}
}
else 
{
goto label_22881;
}
}
}
else 
{
goto label_22881;
}
}
else 
{
label_22881:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_25256;
}
else 
{
if (s__state == 16384)
{
label_25256:; 
goto label_25258;
}
else 
{
if (s__state == 8192)
{
label_25258:; 
goto label_25260;
}
else 
{
if (s__state == 24576)
{
label_25260:; 
goto label_25262;
}
else 
{
if (s__state == 8195)
{
label_25262:; 
s__server = 1;
if (cb != 0)
{
goto label_25267;
}
else 
{
label_25267:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35628 = -1;
goto label_35629;
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
goto label_34891;
}
else 
{
s__init_buf___0 = buf;
goto label_25279;
}
}
else 
{
label_25279:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34891;
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
goto label_34891;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_25299;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_25299:; 
goto label_25301;
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
goto label_25235;
}
else 
{
if (s__state == 8481)
{
label_25235:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34891;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_25301;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_25301;
}
else 
{
if (s__state == 8464)
{
goto label_25214;
}
else 
{
if (s__state == 8465)
{
label_25214:; 
goto label_25216;
}
else 
{
if (s__state == 8466)
{
label_25216:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_25384:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35485;
}
else 
{
label_35485:; 
 __return_35525 = ret;
}
tmp = __return_35525;
goto label_35650;
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
goto label_25384;
}
else 
{
goto label_25374;
}
}
else 
{
label_25374:; 
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
goto label_25187;
}
else 
{
if (s__state == 8513)
{
label_25187:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34891;
}
else 
{
goto label_25197;
}
}
else 
{
skip = 1;
label_25197:; 
s__state = 8528;
s__init_num = 0;
goto label_25301;
}
}
else 
{
if (s__state == 8528)
{
goto label_25119;
}
else 
{
if (s__state == 8529)
{
label_25119:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_25128;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_25128:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_25175;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_25162;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_25169:; 
label_25175:; 
s__state = 8544;
s__init_num = 0;
goto label_25301;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_25151;
}
else 
{
tmp___7 = 512;
label_25151:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_25162;
}
else 
{
skip = 1;
goto label_25169;
}
}
}
}
}
}
else 
{
goto label_25162;
}
}
else 
{
label_25162:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_25382:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35487;
}
else 
{
label_35487:; 
 __return_35524 = ret;
}
tmp = __return_35524;
goto label_35664;
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
goto label_25382;
}
else 
{
goto label_25372;
}
}
else 
{
label_25372:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_25447;
}
else 
{
goto label_25447;
}
}
else 
{
goto label_25447;
}
}
}
else 
{
goto label_25447;
}
}
else 
{
label_25447:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_25873;
}
else 
{
if (s__state == 16384)
{
label_25873:; 
goto label_25875;
}
else 
{
if (s__state == 8192)
{
label_25875:; 
goto label_25877;
}
else 
{
if (s__state == 24576)
{
label_25877:; 
goto label_25879;
}
else 
{
if (s__state == 8195)
{
label_25879:; 
s__server = 1;
if (cb != 0)
{
goto label_25884;
}
else 
{
label_25884:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35626 = -1;
goto label_35627;
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
goto label_34893;
}
else 
{
s__init_buf___0 = buf;
goto label_25896;
}
}
else 
{
label_25896:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34893;
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
goto label_34893;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_25916;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_25916:; 
goto label_25918;
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
goto label_25852;
}
else 
{
if (s__state == 8481)
{
label_25852:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34893;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_25918;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_25918;
}
else 
{
if (s__state == 8464)
{
goto label_25831;
}
else 
{
if (s__state == 8465)
{
label_25831:; 
goto label_25833;
}
else 
{
if (s__state == 8466)
{
label_25833:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_25984:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35479;
}
else 
{
label_35479:; 
 __return_35528 = ret;
}
tmp = __return_35528;
goto label_35650;
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
goto label_25984;
}
else 
{
goto label_25976;
}
}
else 
{
label_25976:; 
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
goto label_25804;
}
else 
{
if (s__state == 8513)
{
label_25804:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34893;
}
else 
{
goto label_25814;
}
}
else 
{
skip = 1;
label_25814:; 
s__state = 8528;
s__init_num = 0;
goto label_25918;
}
}
else 
{
if (s__state == 8528)
{
goto label_25746;
}
else 
{
if (s__state == 8529)
{
label_25746:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_25755;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_25755:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_25795;
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
label_25791:; 
label_25795:; 
s__state = 8544;
s__init_num = 0;
goto label_25918;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_25778;
}
else 
{
tmp___7 = 512;
label_25778:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_25791;
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
goto label_25694;
}
else 
{
if (s__state == 8545)
{
label_25694:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25739;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_25708;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_25731:; 
label_25739:; 
goto label_25918;
}
}
else 
{
label_25708:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_25722;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25731;
}
else 
{
label_25722:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34893;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_25731;
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
goto label_25679;
}
else 
{
if (s__state == 8561)
{
label_25679:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34893;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_25918;
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
goto label_34893;
}
else 
{
s__rwstate = 1;
goto label_25668;
}
}
else 
{
label_25668:; 
s__state = s__s3__tmp__next_state___0;
goto label_25918;
}
}
else 
{
if (s__state == 8576)
{
goto label_25636;
}
else 
{
if (s__state == 8577)
{
label_25636:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34893;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_25652;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34893;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_25652:; 
goto label_25918;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_25622;
}
else 
{
if (s__state == 8593)
{
label_25622:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34893;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_25918;
}
}
else 
{
if (s__state == 8608)
{
goto label_25609;
}
else 
{
if (s__state == 8609)
{
label_25609:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34893;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_25918:; 
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
goto label_25978;
}
}
else 
{
label_25978:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_26038;
}
else 
{
goto label_26038;
}
}
else 
{
goto label_26038;
}
}
}
else 
{
goto label_26038;
}
}
else 
{
label_26038:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_26460;
}
else 
{
if (s__state == 16384)
{
label_26460:; 
goto label_26462;
}
else 
{
if (s__state == 8192)
{
label_26462:; 
goto label_26464;
}
else 
{
if (s__state == 24576)
{
label_26464:; 
goto label_26466;
}
else 
{
if (s__state == 8195)
{
label_26466:; 
s__server = 1;
if (cb != 0)
{
goto label_26471;
}
else 
{
label_26471:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35624 = -1;
goto label_35625;
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
goto label_34895;
}
else 
{
s__init_buf___0 = buf;
goto label_26483;
}
}
else 
{
label_26483:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34895;
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
goto label_34895;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_26503;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_26503:; 
goto label_26505;
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
goto label_26439;
}
else 
{
if (s__state == 8481)
{
label_26439:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34895;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_26505;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_26505;
}
else 
{
if (s__state == 8464)
{
goto label_26418;
}
else 
{
if (s__state == 8465)
{
label_26418:; 
goto label_26420;
}
else 
{
if (s__state == 8466)
{
label_26420:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_26571:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35473;
}
else 
{
label_35473:; 
 __return_35531 = ret;
}
tmp = __return_35531;
goto label_35650;
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
goto label_26571;
}
else 
{
goto label_26563;
}
}
else 
{
label_26563:; 
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
goto label_26391;
}
else 
{
if (s__state == 8513)
{
label_26391:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34895;
}
else 
{
goto label_26401;
}
}
else 
{
skip = 1;
label_26401:; 
s__state = 8528;
s__init_num = 0;
goto label_26505;
}
}
else 
{
if (s__state == 8528)
{
goto label_26333;
}
else 
{
if (s__state == 8529)
{
label_26333:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_26342;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_26342:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_26382;
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
label_26378:; 
label_26382:; 
s__state = 8544;
s__init_num = 0;
goto label_26505;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_26365;
}
else 
{
tmp___7 = 512;
label_26365:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_26378;
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
goto label_26281;
}
else 
{
if (s__state == 8545)
{
label_26281:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_26326;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_26295;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_26318:; 
label_26326:; 
goto label_26505;
}
}
else 
{
label_26295:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_26309;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_26318;
}
else 
{
label_26309:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34895;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_26318;
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
goto label_26266;
}
else 
{
if (s__state == 8561)
{
label_26266:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34895;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_26505;
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
goto label_34895;
}
else 
{
s__rwstate = 1;
goto label_26255;
}
}
else 
{
label_26255:; 
s__state = s__s3__tmp__next_state___0;
goto label_26505;
}
}
else 
{
if (s__state == 8576)
{
goto label_26223;
}
else 
{
if (s__state == 8577)
{
label_26223:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34895;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_26239;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34895;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_26239:; 
goto label_26505;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_26209;
}
else 
{
if (s__state == 8593)
{
label_26209:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34895;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_26505;
}
}
else 
{
if (s__state == 8608)
{
goto label_26196;
}
else 
{
if (s__state == 8609)
{
label_26196:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34895;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_26505:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34895;
}
else 
{
goto label_26565;
}
}
else 
{
label_26565:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_26625;
}
else 
{
goto label_26625;
}
}
else 
{
goto label_26625;
}
}
}
else 
{
goto label_26625;
}
}
else 
{
label_26625:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_27047;
}
else 
{
if (s__state == 16384)
{
label_27047:; 
goto label_27049;
}
else 
{
if (s__state == 8192)
{
label_27049:; 
goto label_27051;
}
else 
{
if (s__state == 24576)
{
label_27051:; 
goto label_27053;
}
else 
{
if (s__state == 8195)
{
label_27053:; 
s__server = 1;
if (cb != 0)
{
goto label_27058;
}
else 
{
label_27058:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35622 = -1;
goto label_35623;
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
goto label_34897;
}
else 
{
s__init_buf___0 = buf;
goto label_27070;
}
}
else 
{
label_27070:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34897;
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
goto label_34897;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_27090;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_27090:; 
goto label_27092;
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
goto label_27026;
}
else 
{
if (s__state == 8481)
{
label_27026:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34897;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_27092;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_27092;
}
else 
{
if (s__state == 8464)
{
goto label_27005;
}
else 
{
if (s__state == 8465)
{
label_27005:; 
goto label_27007;
}
else 
{
if (s__state == 8466)
{
label_27007:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_27158:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35467;
}
else 
{
label_35467:; 
 __return_35534 = ret;
}
tmp = __return_35534;
goto label_35650;
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
goto label_27158;
}
else 
{
goto label_27150;
}
}
else 
{
label_27150:; 
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
goto label_26978;
}
else 
{
if (s__state == 8513)
{
label_26978:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34897;
}
else 
{
goto label_26988;
}
}
else 
{
skip = 1;
label_26988:; 
s__state = 8528;
s__init_num = 0;
goto label_27092;
}
}
else 
{
if (s__state == 8528)
{
goto label_26920;
}
else 
{
if (s__state == 8529)
{
label_26920:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_26929;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_26929:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_26969;
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
label_26965:; 
label_26969:; 
s__state = 8544;
s__init_num = 0;
goto label_27092;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_26952;
}
else 
{
tmp___7 = 512;
label_26952:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_26965;
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
goto label_26868;
}
else 
{
if (s__state == 8545)
{
label_26868:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_26913;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_26882;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_26905:; 
label_26913:; 
goto label_27092;
}
}
else 
{
label_26882:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_26896;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_26905;
}
else 
{
label_26896:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34897;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_26905;
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
goto label_26853;
}
else 
{
if (s__state == 8561)
{
label_26853:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34897;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_27092;
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
goto label_34897;
}
else 
{
s__rwstate = 1;
goto label_26842;
}
}
else 
{
label_26842:; 
s__state = s__s3__tmp__next_state___0;
goto label_27092;
}
}
else 
{
if (s__state == 8576)
{
goto label_26810;
}
else 
{
if (s__state == 8577)
{
label_26810:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34897;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_26826;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34897;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_26826:; 
goto label_27092;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_26796;
}
else 
{
if (s__state == 8593)
{
label_26796:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34897;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_27092;
}
}
else 
{
if (s__state == 8608)
{
goto label_26783;
}
else 
{
if (s__state == 8609)
{
label_26783:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34897;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_27092:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34897;
}
else 
{
goto label_27152;
}
}
else 
{
label_27152:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_27212;
}
else 
{
goto label_27212;
}
}
else 
{
goto label_27212;
}
}
}
else 
{
goto label_27212;
}
}
else 
{
label_27212:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_27634;
}
else 
{
if (s__state == 16384)
{
label_27634:; 
goto label_27636;
}
else 
{
if (s__state == 8192)
{
label_27636:; 
goto label_27638;
}
else 
{
if (s__state == 24576)
{
label_27638:; 
goto label_27640;
}
else 
{
if (s__state == 8195)
{
label_27640:; 
s__server = 1;
if (cb != 0)
{
goto label_27645;
}
else 
{
label_27645:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35620 = -1;
goto label_35621;
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
goto label_34899;
}
else 
{
s__init_buf___0 = buf;
goto label_27657;
}
}
else 
{
label_27657:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34899;
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
goto label_34899;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_27677;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_27677:; 
goto label_27679;
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
goto label_27613;
}
else 
{
if (s__state == 8481)
{
label_27613:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34899;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_27679;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_27679;
}
else 
{
if (s__state == 8464)
{
goto label_27592;
}
else 
{
if (s__state == 8465)
{
label_27592:; 
goto label_27594;
}
else 
{
if (s__state == 8466)
{
label_27594:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_27745:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35461;
}
else 
{
label_35461:; 
 __return_35537 = ret;
}
tmp = __return_35537;
goto label_35650;
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
goto label_27745;
}
else 
{
goto label_27737;
}
}
else 
{
label_27737:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_27797;
}
else 
{
goto label_27797;
}
}
else 
{
goto label_27797;
}
}
}
else 
{
goto label_27797;
}
}
else 
{
label_27797:; 
skip = 0;
goto label_21779;
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
goto label_27565;
}
else 
{
if (s__state == 8513)
{
label_27565:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34899;
}
else 
{
goto label_27575;
}
}
else 
{
skip = 1;
label_27575:; 
s__state = 8528;
s__init_num = 0;
goto label_27679;
}
}
else 
{
if (s__state == 8528)
{
goto label_27507;
}
else 
{
if (s__state == 8529)
{
label_27507:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_27516;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_27516:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_27556;
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
label_27552:; 
label_27556:; 
s__state = 8544;
s__init_num = 0;
goto label_27679;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_27539;
}
else 
{
tmp___7 = 512;
label_27539:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_27552;
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
goto label_27455;
}
else 
{
if (s__state == 8545)
{
label_27455:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_27500;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_27469;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_27492:; 
label_27500:; 
goto label_27679;
}
}
else 
{
label_27469:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_27483;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_27492;
}
else 
{
label_27483:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34899;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_27492;
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
goto label_27440;
}
else 
{
if (s__state == 8561)
{
label_27440:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34899;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_27679;
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
goto label_34899;
}
else 
{
s__rwstate = 1;
goto label_27429;
}
}
else 
{
label_27429:; 
s__state = s__s3__tmp__next_state___0;
goto label_27679;
}
}
else 
{
if (s__state == 8576)
{
goto label_27397;
}
else 
{
if (s__state == 8577)
{
label_27397:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34899;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_27413;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34899;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_27413:; 
goto label_27679;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_27383;
}
else 
{
if (s__state == 8593)
{
label_27383:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34899;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_27679;
}
}
else 
{
if (s__state == 8608)
{
goto label_27370;
}
else 
{
if (s__state == 8609)
{
label_27370:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34899;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_27679:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34899;
}
else 
{
goto label_27739;
}
}
else 
{
label_27739:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_27799;
}
else 
{
goto label_27799;
}
}
else 
{
goto label_27799;
}
}
}
else 
{
goto label_27799;
}
}
else 
{
label_27799:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_28222;
}
else 
{
if (s__state == 16384)
{
label_28222:; 
goto label_28224;
}
else 
{
if (s__state == 8192)
{
label_28224:; 
goto label_28226;
}
else 
{
if (s__state == 24576)
{
label_28226:; 
goto label_28228;
}
else 
{
if (s__state == 8195)
{
label_28228:; 
s__server = 1;
if (cb != 0)
{
goto label_28233;
}
else 
{
label_28233:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35618 = -1;
goto label_35619;
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
goto label_34901;
}
else 
{
s__init_buf___0 = buf;
goto label_28245;
}
}
else 
{
label_28245:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34901;
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
goto label_34901;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_28265;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_28265:; 
goto label_28267;
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
goto label_28201;
}
else 
{
if (s__state == 8481)
{
label_28201:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34901;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_28267;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_28267;
}
else 
{
if (s__state == 8464)
{
goto label_28180;
}
else 
{
if (s__state == 8465)
{
label_28180:; 
goto label_28182;
}
else 
{
if (s__state == 8466)
{
label_28182:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_28333:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35455;
}
else 
{
label_35455:; 
 __return_35540 = ret;
}
tmp = __return_35540;
goto label_35650;
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
goto label_28333;
}
else 
{
goto label_28325;
}
}
else 
{
label_28325:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_28385;
}
else 
{
goto label_28385;
}
}
else 
{
goto label_28385;
}
}
}
else 
{
goto label_28385;
}
}
else 
{
label_28385:; 
skip = 0;
goto label_21779;
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
goto label_28153;
}
else 
{
if (s__state == 8513)
{
label_28153:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34901;
}
else 
{
goto label_28163;
}
}
else 
{
skip = 1;
label_28163:; 
s__state = 8528;
s__init_num = 0;
goto label_28267;
}
}
else 
{
if (s__state == 8528)
{
goto label_28095;
}
else 
{
if (s__state == 8529)
{
label_28095:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_28104;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_28104:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_28144;
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
label_28140:; 
label_28144:; 
s__state = 8544;
s__init_num = 0;
goto label_28267;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_28127;
}
else 
{
tmp___7 = 512;
label_28127:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_28140;
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
goto label_28043;
}
else 
{
if (s__state == 8545)
{
label_28043:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_28088;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_28057;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_28080:; 
label_28088:; 
goto label_28267;
}
}
else 
{
label_28057:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_28071;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_28080;
}
else 
{
label_28071:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34901;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_28080;
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
goto label_28028;
}
else 
{
if (s__state == 8561)
{
label_28028:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34901;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_28267;
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
goto label_34901;
}
else 
{
s__rwstate = 1;
goto label_28017;
}
}
else 
{
label_28017:; 
s__state = s__s3__tmp__next_state___0;
goto label_28267;
}
}
else 
{
if (s__state == 8576)
{
goto label_27985;
}
else 
{
if (s__state == 8577)
{
label_27985:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34901;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_28001;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34901;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_28001:; 
goto label_28267;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_27971;
}
else 
{
if (s__state == 8593)
{
label_27971:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34901;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_28267;
}
}
else 
{
if (s__state == 8608)
{
goto label_27958;
}
else 
{
if (s__state == 8609)
{
label_27958:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34901;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_28267:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34901;
}
else 
{
goto label_28327;
}
}
else 
{
label_28327:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_28387;
}
else 
{
goto label_28387;
}
}
else 
{
goto label_28387;
}
}
}
else 
{
goto label_28387;
}
}
else 
{
label_28387:; 
skip = 0;
label_28397:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_28810;
}
else 
{
if (s__state == 16384)
{
label_28810:; 
goto label_28812;
}
else 
{
if (s__state == 8192)
{
label_28812:; 
goto label_28814;
}
else 
{
if (s__state == 24576)
{
label_28814:; 
goto label_28816;
}
else 
{
if (s__state == 8195)
{
label_28816:; 
s__server = 1;
if (cb != 0)
{
goto label_28821;
}
else 
{
label_28821:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35616 = -1;
goto label_35617;
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
goto label_34903;
}
else 
{
s__init_buf___0 = buf;
goto label_28833;
}
}
else 
{
label_28833:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34903;
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
goto label_34903;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_28853;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_28853:; 
goto label_28855;
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
goto label_28789;
}
else 
{
if (s__state == 8481)
{
label_28789:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34903;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_28855;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_28855;
}
else 
{
if (s__state == 8464)
{
goto label_28768;
}
else 
{
if (s__state == 8465)
{
label_28768:; 
goto label_28770;
}
else 
{
if (s__state == 8466)
{
label_28770:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_28921:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35449;
}
else 
{
label_35449:; 
 __return_35543 = ret;
}
tmp = __return_35543;
goto label_35650;
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
goto label_28921;
}
else 
{
goto label_28913;
}
}
else 
{
label_28913:; 
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
goto label_28741;
}
else 
{
if (s__state == 8513)
{
label_28741:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34903;
}
else 
{
goto label_28751;
}
}
else 
{
skip = 1;
label_28751:; 
s__state = 8528;
s__init_num = 0;
goto label_28855;
}
}
else 
{
if (s__state == 8528)
{
goto label_28683;
}
else 
{
if (s__state == 8529)
{
label_28683:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_28692;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_28692:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_28732;
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
label_28728:; 
label_28732:; 
s__state = 8544;
s__init_num = 0;
goto label_28855;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_28715;
}
else 
{
tmp___7 = 512;
label_28715:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_28728;
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
goto label_28631;
}
else 
{
if (s__state == 8545)
{
label_28631:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_28676;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_28645;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_28668:; 
label_28676:; 
goto label_28855;
}
}
else 
{
label_28645:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_28659;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_28668;
}
else 
{
label_28659:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34903;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_28668;
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
goto label_28616;
}
else 
{
if (s__state == 8561)
{
label_28616:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34903;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_28855;
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
goto label_34903;
}
else 
{
s__rwstate = 1;
goto label_28605;
}
}
else 
{
label_28605:; 
s__state = s__s3__tmp__next_state___0;
goto label_28855;
}
}
else 
{
if (s__state == 8576)
{
goto label_28573;
}
else 
{
if (s__state == 8577)
{
label_28573:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34903;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_28589;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34903;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_28589:; 
goto label_28855;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_28559;
}
else 
{
if (s__state == 8593)
{
label_28559:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34903;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_28855;
}
}
else 
{
if (s__state == 8608)
{
goto label_28546;
}
else 
{
if (s__state == 8609)
{
label_28546:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34903;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_28855:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34903;
}
else 
{
goto label_28915;
}
}
else 
{
label_28915:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_28975;
}
else 
{
goto label_28975;
}
}
else 
{
goto label_28975;
}
}
}
else 
{
goto label_28975;
}
}
else 
{
label_28975:; 
skip = 0;
goto label_28397;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_28528;
}
else 
{
if (s__state == 8641)
{
label_28528:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_28919:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35451;
}
else 
{
label_35451:; 
 __return_35542 = ret;
}
tmp = __return_35542;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_28538;
}
else 
{
s__state = 3;
label_28538:; 
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
goto label_28919;
}
else 
{
goto label_28911;
}
}
else 
{
label_28911:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_28971;
}
else 
{
goto label_28971;
}
}
else 
{
goto label_28971;
}
}
}
else 
{
goto label_28971;
}
}
else 
{
label_28971:; 
skip = 0;
label_28982:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_29357;
}
else 
{
if (s__state == 16384)
{
label_29357:; 
goto label_29359;
}
else 
{
if (s__state == 8192)
{
label_29359:; 
goto label_29361;
}
else 
{
if (s__state == 24576)
{
label_29361:; 
goto label_29363;
}
else 
{
if (s__state == 8195)
{
label_29363:; 
s__server = 1;
if (cb != 0)
{
goto label_29368;
}
else 
{
label_29368:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35614 = -1;
goto label_35615;
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
goto label_34905;
}
else 
{
s__init_buf___0 = buf;
goto label_29380;
}
}
else 
{
label_29380:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34905;
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
goto label_34905;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_29400;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_29400:; 
goto label_29402;
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
goto label_29336;
}
else 
{
if (s__state == 8481)
{
label_29336:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34905;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_29402;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_29402;
}
else 
{
if (s__state == 8464)
{
goto label_29323;
}
else 
{
if (s__state == 8465)
{
label_29323:; 
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
goto label_29303;
}
else 
{
if (s__state == 8513)
{
label_29303:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_29402;
}
else 
{
if (s__state == 8528)
{
goto label_29245;
}
else 
{
if (s__state == 8529)
{
label_29245:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_29254;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_29254:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_29294;
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
label_29290:; 
label_29294:; 
s__state = 8544;
s__init_num = 0;
goto label_29402;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_29277;
}
else 
{
tmp___7 = 512;
label_29277:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_29290;
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
goto label_29203;
}
else 
{
if (s__state == 8545)
{
label_29203:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_29238;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_29217;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_29229:; 
label_29238:; 
goto label_29402;
}
}
else 
{
label_29217:; 
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
goto label_29229;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_29188;
}
else 
{
if (s__state == 8561)
{
label_29188:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34905;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_29402;
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
goto label_34905;
}
else 
{
s__rwstate = 1;
goto label_29177;
}
}
else 
{
label_29177:; 
s__state = s__s3__tmp__next_state___0;
goto label_29402;
}
}
else 
{
if (s__state == 8576)
{
goto label_29145;
}
else 
{
if (s__state == 8577)
{
label_29145:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34905;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_29161;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34905;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_29161:; 
goto label_29402;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_29131;
}
else 
{
if (s__state == 8593)
{
label_29131:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34905;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_29402;
}
}
else 
{
if (s__state == 8608)
{
goto label_29118;
}
else 
{
if (s__state == 8609)
{
label_29118:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34905;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_29402:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34905;
}
else 
{
goto label_29432;
}
}
else 
{
label_29432:; 
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
goto label_29088;
}
else 
{
if (s__state == 8657)
{
label_29088:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34905;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_29434;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_29434:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35447;
}
else 
{
label_35447:; 
 __return_35544 = ret;
}
tmp = __return_35544;
goto label_35654;
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
goto label_29434;
}
else 
{
goto label_29430;
}
}
else 
{
label_29430:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_29460;
}
else 
{
goto label_29460;
}
}
else 
{
goto label_29460;
}
}
}
else 
{
goto label_29460;
}
}
else 
{
label_29460:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_29842;
}
else 
{
if (s__state == 16384)
{
label_29842:; 
goto label_29844;
}
else 
{
if (s__state == 8192)
{
label_29844:; 
goto label_29846;
}
else 
{
if (s__state == 24576)
{
label_29846:; 
goto label_29848;
}
else 
{
if (s__state == 8195)
{
label_29848:; 
s__server = 1;
if (cb != 0)
{
goto label_29853;
}
else 
{
label_29853:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35612 = -1;
goto label_35613;
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
goto label_34907;
}
else 
{
s__init_buf___0 = buf;
goto label_29865;
}
}
else 
{
label_29865:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34907;
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
goto label_34907;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_29885;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_29885:; 
goto label_29887;
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
goto label_29821;
}
else 
{
if (s__state == 8481)
{
label_29821:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34907;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_29887;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_29887;
}
else 
{
if (s__state == 8464)
{
goto label_29808;
}
else 
{
if (s__state == 8465)
{
label_29808:; 
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
goto label_29788;
}
else 
{
if (s__state == 8513)
{
label_29788:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_29887;
}
else 
{
if (s__state == 8528)
{
goto label_29730;
}
else 
{
if (s__state == 8529)
{
label_29730:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_29739;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_29739:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_29779;
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
label_29775:; 
label_29779:; 
s__state = 8544;
s__init_num = 0;
goto label_29887;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_29762;
}
else 
{
tmp___7 = 512;
label_29762:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_29775;
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
goto label_29688;
}
else 
{
if (s__state == 8545)
{
label_29688:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_29723;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_29702;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_29714:; 
label_29723:; 
goto label_29887;
}
}
else 
{
label_29702:; 
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
goto label_29714;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_29673;
}
else 
{
if (s__state == 8561)
{
label_29673:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34907;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_29887;
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
goto label_34907;
}
else 
{
s__rwstate = 1;
goto label_29662;
}
}
else 
{
label_29662:; 
s__state = s__s3__tmp__next_state___0;
goto label_29887;
}
}
else 
{
if (s__state == 8576)
{
goto label_29630;
}
else 
{
if (s__state == 8577)
{
label_29630:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34907;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_29646;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34907;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_29646:; 
goto label_29887;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_29616;
}
else 
{
if (s__state == 8593)
{
label_29616:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34907;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_29887;
}
}
else 
{
if (s__state == 8608)
{
goto label_29603;
}
else 
{
if (s__state == 8609)
{
label_29603:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34907;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_29887:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34907;
}
else 
{
goto label_29917;
}
}
else 
{
label_29917:; 
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
goto label_29586;
}
else 
{
if (s__state == 8657)
{
label_29586:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34907;
}
else 
{
if (s__state == 8672)
{
goto label_29567;
}
else 
{
if (s__state == 8673)
{
label_29567:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_29919:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35445;
}
else 
{
label_35445:; 
 __return_35545 = ret;
}
tmp = __return_35545;
goto label_35656;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_29578;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_29578:; 
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
goto label_29919;
}
else 
{
goto label_29915;
}
}
else 
{
label_29915:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_29945;
}
else 
{
goto label_29945;
}
}
else 
{
goto label_29945;
}
}
}
else 
{
goto label_29945;
}
}
else 
{
label_29945:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_30326;
}
else 
{
if (s__state == 16384)
{
label_30326:; 
goto label_30328;
}
else 
{
if (s__state == 8192)
{
label_30328:; 
goto label_30330;
}
else 
{
if (s__state == 24576)
{
label_30330:; 
goto label_30332;
}
else 
{
if (s__state == 8195)
{
label_30332:; 
s__server = 1;
if (cb != 0)
{
goto label_30337;
}
else 
{
label_30337:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35610 = -1;
goto label_35611;
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
goto label_34909;
}
else 
{
s__init_buf___0 = buf;
goto label_30349;
}
}
else 
{
label_30349:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34909;
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
goto label_34909;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_30369;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_30369:; 
goto label_30371;
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
goto label_30305;
}
else 
{
if (s__state == 8481)
{
label_30305:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34909;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_30371;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_30371;
}
else 
{
if (s__state == 8464)
{
goto label_30292;
}
else 
{
if (s__state == 8465)
{
label_30292:; 
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
goto label_30272;
}
else 
{
if (s__state == 8513)
{
label_30272:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_30371;
}
else 
{
if (s__state == 8528)
{
goto label_30214;
}
else 
{
if (s__state == 8529)
{
label_30214:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_30223;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_30223:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_30263;
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
label_30259:; 
label_30263:; 
s__state = 8544;
s__init_num = 0;
goto label_30371;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_30246;
}
else 
{
tmp___7 = 512;
label_30246:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_30259;
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
goto label_30172;
}
else 
{
if (s__state == 8545)
{
label_30172:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_30207;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_30186;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_30198:; 
label_30207:; 
goto label_30371;
}
}
else 
{
label_30186:; 
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
goto label_30198;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_30157;
}
else 
{
if (s__state == 8561)
{
label_30157:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34909;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_30371;
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
goto label_34909;
}
else 
{
s__rwstate = 1;
goto label_30146;
}
}
else 
{
label_30146:; 
s__state = s__s3__tmp__next_state___0;
goto label_30371;
}
}
else 
{
if (s__state == 8576)
{
goto label_30114;
}
else 
{
if (s__state == 8577)
{
label_30114:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34909;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_30130;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34909;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_30130:; 
goto label_30371;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_30100;
}
else 
{
if (s__state == 8593)
{
label_30100:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34909;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_30371;
}
}
else 
{
if (s__state == 8608)
{
goto label_30087;
}
else 
{
if (s__state == 8609)
{
label_30087:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34909;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_30371:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34909;
}
else 
{
goto label_30401;
}
}
else 
{
label_30401:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_30431;
}
else 
{
goto label_30431;
}
}
else 
{
goto label_30431;
}
}
}
else 
{
goto label_30431;
}
}
else 
{
label_30431:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_30811;
}
else 
{
if (s__state == 16384)
{
label_30811:; 
goto label_30813;
}
else 
{
if (s__state == 8192)
{
label_30813:; 
goto label_30815;
}
else 
{
if (s__state == 24576)
{
label_30815:; 
goto label_30817;
}
else 
{
if (s__state == 8195)
{
label_30817:; 
s__server = 1;
if (cb != 0)
{
goto label_30822;
}
else 
{
label_30822:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35608 = -1;
goto label_35609;
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
goto label_34911;
}
else 
{
s__init_buf___0 = buf;
goto label_30834;
}
}
else 
{
label_30834:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34911;
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
goto label_34911;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_30854;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_30854:; 
goto label_30856;
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
goto label_30790;
}
else 
{
if (s__state == 8481)
{
label_30790:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34911;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_30856;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_30856;
}
else 
{
if (s__state == 8464)
{
goto label_30777;
}
else 
{
if (s__state == 8465)
{
label_30777:; 
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
goto label_30757;
}
else 
{
if (s__state == 8513)
{
label_30757:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_30856;
}
else 
{
if (s__state == 8528)
{
goto label_30699;
}
else 
{
if (s__state == 8529)
{
label_30699:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_30708;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_30708:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_30748;
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
label_30744:; 
label_30748:; 
s__state = 8544;
s__init_num = 0;
goto label_30856;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_30731;
}
else 
{
tmp___7 = 512;
label_30731:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_30744;
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
goto label_30657;
}
else 
{
if (s__state == 8545)
{
label_30657:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_30692;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_30671;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_30683:; 
label_30692:; 
goto label_30856;
}
}
else 
{
label_30671:; 
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
goto label_30683;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_30642;
}
else 
{
if (s__state == 8561)
{
label_30642:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34911;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_30856;
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
goto label_34911;
}
else 
{
s__rwstate = 1;
goto label_30631;
}
}
else 
{
label_30631:; 
s__state = s__s3__tmp__next_state___0;
goto label_30856;
}
}
else 
{
if (s__state == 8576)
{
goto label_30599;
}
else 
{
if (s__state == 8577)
{
label_30599:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34911;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_30615;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34911;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_30615:; 
goto label_30856;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_30585;
}
else 
{
if (s__state == 8593)
{
label_30585:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34911;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_30856;
}
}
else 
{
if (s__state == 8608)
{
goto label_30572;
}
else 
{
if (s__state == 8609)
{
label_30572:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34911;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_30856:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34911;
}
else 
{
goto label_30886;
}
}
else 
{
label_30886:; 
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
goto label_30554;
}
else 
{
if (s__state == 8641)
{
label_30554:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_30888:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35441;
}
else 
{
label_35441:; 
 __return_35547 = ret;
}
tmp = __return_35547;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_30564;
}
else 
{
s__state = 3;
label_30564:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_30888;
}
else 
{
goto label_30884;
}
}
else 
{
label_30884:; 
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
goto label_30542;
}
else 
{
if (s__state == 8657)
{
label_30542:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34911;
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
goto label_30529;
}
else 
{
goto label_30529;
}
}
else 
{
label_30529:; 
ret = 1;
goto label_34911;
}
}
else 
{
ret = -1;
label_34911:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35357;
}
else 
{
label_35357:; 
 __return_35609 = ret;
label_35609:; 
}
tmp = __return_35609;
goto label_35656;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_30069;
}
else 
{
if (s__state == 8641)
{
label_30069:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_30403:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35443;
}
else 
{
label_35443:; 
 __return_35546 = ret;
}
tmp = __return_35546;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_30079;
}
else 
{
s__state = 3;
label_30079:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_30403;
}
else 
{
goto label_30399;
}
}
else 
{
label_30399:; 
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
goto label_30057;
}
else 
{
if (s__state == 8657)
{
label_30057:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34909;
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
goto label_30044;
}
else 
{
goto label_30044;
}
}
else 
{
label_30044:; 
ret = 1;
goto label_34909;
}
}
else 
{
ret = -1;
label_34909:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35359;
}
else 
{
label_35359:; 
 __return_35611 = ret;
label_35611:; 
}
tmp = __return_35611;
goto label_35656;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_29559;
}
else 
{
goto label_29559;
}
}
else 
{
label_29559:; 
ret = 1;
goto label_34907;
}
}
else 
{
ret = -1;
label_34907:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35361;
}
else 
{
label_35361:; 
 __return_35613 = ret;
label_35613:; 
}
tmp = __return_35613;
goto label_35654;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_29075;
}
else 
{
goto label_29075;
}
}
else 
{
label_29075:; 
ret = 1;
goto label_34905;
}
}
else 
{
ret = -1;
label_34905:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35363;
}
else 
{
label_35363:; 
 __return_35615 = ret;
label_35615:; 
}
tmp = __return_35615;
goto label_35660;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_28503;
}
else 
{
if (s__state == 8657)
{
label_28503:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34903;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_28917;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_28917:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35453;
}
else 
{
label_35453:; 
 __return_35541 = ret;
}
tmp = __return_35541;
goto label_35654;
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
goto label_28917;
}
else 
{
goto label_28909;
}
}
else 
{
label_28909:; 
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
goto label_28490;
}
else 
{
goto label_28490;
}
}
else 
{
label_28490:; 
ret = 1;
goto label_34903;
}
}
else 
{
ret = -1;
label_34903:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35365;
}
else 
{
label_35365:; 
 __return_35617 = ret;
label_35617:; 
}
tmp = __return_35617;
goto label_35664;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_27940;
}
else 
{
if (s__state == 8641)
{
label_27940:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_28331:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35457;
}
else 
{
label_35457:; 
 __return_35539 = ret;
}
tmp = __return_35539;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_27950;
}
else 
{
s__state = 3;
label_27950:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_28331;
}
else 
{
goto label_28323;
}
}
else 
{
label_28323:; 
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
goto label_27915;
}
else 
{
if (s__state == 8657)
{
label_27915:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34901;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_28329;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_28329:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35459;
}
else 
{
label_35459:; 
 __return_35538 = ret;
}
tmp = __return_35538;
goto label_35654;
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
goto label_28329;
}
else 
{
goto label_28321;
}
}
else 
{
label_28321:; 
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
goto label_27902;
}
else 
{
goto label_27902;
}
}
else 
{
label_27902:; 
ret = 1;
goto label_34901;
}
}
else 
{
ret = -1;
label_34901:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35367;
}
else 
{
label_35367:; 
 __return_35619 = ret;
label_35619:; 
}
tmp = __return_35619;
goto label_35664;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_27352;
}
else 
{
if (s__state == 8641)
{
label_27352:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_27743:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35463;
}
else 
{
label_35463:; 
 __return_35536 = ret;
}
tmp = __return_35536;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_27362;
}
else 
{
s__state = 3;
label_27362:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_27743;
}
else 
{
goto label_27735;
}
}
else 
{
label_27735:; 
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
goto label_27327;
}
else 
{
if (s__state == 8657)
{
label_27327:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34899;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_27741;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_27741:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35465;
}
else 
{
label_35465:; 
 __return_35535 = ret;
}
tmp = __return_35535;
goto label_35654;
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
goto label_27741;
}
else 
{
goto label_27733;
}
}
else 
{
label_27733:; 
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
goto label_27314;
}
else 
{
goto label_27314;
}
}
else 
{
label_27314:; 
ret = 1;
goto label_34899;
}
}
else 
{
ret = -1;
label_34899:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35369;
}
else 
{
label_35369:; 
 __return_35621 = ret;
label_35621:; 
}
tmp = __return_35621;
goto label_35664;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_26765;
}
else 
{
if (s__state == 8641)
{
label_26765:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_27156:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35469;
}
else 
{
label_35469:; 
 __return_35533 = ret;
}
tmp = __return_35533;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_26775;
}
else 
{
s__state = 3;
label_26775:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_27156;
}
else 
{
goto label_27148;
}
}
else 
{
label_27148:; 
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
goto label_26740;
}
else 
{
if (s__state == 8657)
{
label_26740:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34897;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_27154;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_27154:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35471;
}
else 
{
label_35471:; 
 __return_35532 = ret;
}
tmp = __return_35532;
goto label_35654;
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
goto label_27154;
}
else 
{
goto label_27146;
}
}
else 
{
label_27146:; 
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
goto label_26727;
}
else 
{
goto label_26727;
}
}
else 
{
label_26727:; 
ret = 1;
goto label_34897;
}
}
else 
{
ret = -1;
label_34897:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35371;
}
else 
{
label_35371:; 
 __return_35623 = ret;
label_35623:; 
}
tmp = __return_35623;
goto label_35664;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_26178;
}
else 
{
if (s__state == 8641)
{
label_26178:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_26569:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35475;
}
else 
{
label_35475:; 
 __return_35530 = ret;
}
tmp = __return_35530;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_26188;
}
else 
{
s__state = 3;
label_26188:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_26569;
}
else 
{
goto label_26561;
}
}
else 
{
label_26561:; 
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
goto label_26153;
}
else 
{
if (s__state == 8657)
{
label_26153:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34895;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_26567;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_26567:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35477;
}
else 
{
label_35477:; 
 __return_35529 = ret;
}
tmp = __return_35529;
goto label_35654;
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
goto label_26567;
}
else 
{
goto label_26559;
}
}
else 
{
label_26559:; 
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
goto label_26140;
}
else 
{
goto label_26140;
}
}
else 
{
label_26140:; 
ret = 1;
goto label_34895;
}
}
else 
{
ret = -1;
label_34895:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35373;
}
else 
{
label_35373:; 
 __return_35625 = ret;
label_35625:; 
}
tmp = __return_35625;
goto label_35664;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_25591;
}
else 
{
if (s__state == 8641)
{
label_25591:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_25982:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35481;
}
else 
{
label_35481:; 
 __return_35527 = ret;
}
tmp = __return_35527;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_25601;
}
else 
{
s__state = 3;
label_25601:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25982;
}
else 
{
goto label_25974;
}
}
else 
{
label_25974:; 
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
goto label_25566;
}
else 
{
if (s__state == 8657)
{
label_25566:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34893;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_25980;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_25980:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35483;
}
else 
{
label_35483:; 
 __return_35526 = ret;
}
tmp = __return_35526;
goto label_35654;
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
goto label_25980;
}
else 
{
goto label_25972;
}
}
else 
{
label_25972:; 
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
goto label_25553;
}
else 
{
goto label_25553;
}
}
else 
{
label_25553:; 
ret = 1;
goto label_34893;
}
}
else 
{
ret = -1;
label_34893:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35375;
}
else 
{
label_35375:; 
 __return_35627 = ret;
label_35627:; 
}
tmp = __return_35627;
label_35664:; 
 __return_35816 = tmp;
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
goto label_25067;
}
else 
{
if (s__state == 8545)
{
label_25067:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25112;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_25081;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_25104:; 
label_25112:; 
goto label_25301;
}
}
else 
{
label_25081:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_25095;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_25104;
}
else 
{
label_25095:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34891;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_25104;
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
goto label_25052;
}
else 
{
if (s__state == 8561)
{
label_25052:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34891;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_25301;
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
goto label_34891;
}
else 
{
s__rwstate = 1;
goto label_25041;
}
}
else 
{
label_25041:; 
s__state = s__s3__tmp__next_state___0;
goto label_25301;
}
}
else 
{
if (s__state == 8576)
{
goto label_25009;
}
else 
{
if (s__state == 8577)
{
label_25009:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34891;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_25025;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34891;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_25025:; 
goto label_25301;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24995;
}
else 
{
if (s__state == 8593)
{
label_24995:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34891;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_25301;
}
}
else 
{
if (s__state == 8608)
{
goto label_24982;
}
else 
{
if (s__state == 8609)
{
label_24982:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34891;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_25301:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34891;
}
else 
{
goto label_25376;
}
}
else 
{
label_25376:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_25451;
}
else 
{
goto label_25451;
}
}
else 
{
goto label_25451;
}
}
}
else 
{
goto label_25451;
}
}
else 
{
label_25451:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_31351;
}
else 
{
if (s__state == 16384)
{
label_31351:; 
goto label_31353;
}
else 
{
if (s__state == 8192)
{
label_31353:; 
goto label_31355;
}
else 
{
if (s__state == 24576)
{
label_31355:; 
goto label_31357;
}
else 
{
if (s__state == 8195)
{
label_31357:; 
s__server = 1;
if (cb != 0)
{
goto label_31362;
}
else 
{
label_31362:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35606 = -1;
goto label_35607;
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
goto label_34913;
}
else 
{
s__init_buf___0 = buf;
goto label_31374;
}
}
else 
{
label_31374:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34913;
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
goto label_34913;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_31394;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_31394:; 
goto label_31396;
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
goto label_31330;
}
else 
{
if (s__state == 8481)
{
label_31330:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34913;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_31396;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_31396;
}
else 
{
if (s__state == 8464)
{
goto label_31309;
}
else 
{
if (s__state == 8465)
{
label_31309:; 
goto label_31311;
}
else 
{
if (s__state == 8466)
{
label_31311:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_31479:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35433;
}
else 
{
label_35433:; 
 __return_35551 = ret;
}
tmp = __return_35551;
goto label_35650;
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
goto label_31479;
}
else 
{
goto label_31469;
}
}
else 
{
label_31469:; 
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
goto label_31282;
}
else 
{
if (s__state == 8513)
{
label_31282:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34913;
}
else 
{
goto label_31292;
}
}
else 
{
skip = 1;
label_31292:; 
s__state = 8528;
s__init_num = 0;
goto label_31396;
}
}
else 
{
if (s__state == 8528)
{
goto label_31214;
}
else 
{
if (s__state == 8529)
{
label_31214:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_31223;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_31223:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_31270;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_31257;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_31264:; 
label_31270:; 
s__state = 8544;
s__init_num = 0;
goto label_31396;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_31246;
}
else 
{
tmp___7 = 512;
label_31246:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_31257;
}
else 
{
skip = 1;
goto label_31264;
}
}
}
}
}
}
else 
{
goto label_31257;
}
}
else 
{
label_31257:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_31477:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35435;
}
else 
{
label_35435:; 
 __return_35550 = ret;
}
tmp = __return_35550;
goto label_35664;
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
goto label_31477;
}
else 
{
goto label_31467;
}
}
else 
{
label_31467:; 
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
goto label_31162;
}
else 
{
if (s__state == 8545)
{
label_31162:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_31207;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_31176;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_31199:; 
label_31207:; 
goto label_31396;
}
}
else 
{
label_31176:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_31190;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_31199;
}
else 
{
label_31190:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34913;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_31199;
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
goto label_31147;
}
else 
{
if (s__state == 8561)
{
label_31147:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34913;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_31396;
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
goto label_34913;
}
else 
{
s__rwstate = 1;
goto label_31136;
}
}
else 
{
label_31136:; 
s__state = s__s3__tmp__next_state___0;
goto label_31396;
}
}
else 
{
if (s__state == 8576)
{
goto label_31104;
}
else 
{
if (s__state == 8577)
{
label_31104:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34913;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_31120;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34913;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_31120:; 
goto label_31396;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_31090;
}
else 
{
if (s__state == 8593)
{
label_31090:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34913;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_31396;
}
}
else 
{
if (s__state == 8608)
{
goto label_31077;
}
else 
{
if (s__state == 8609)
{
label_31077:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34913;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_31396:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34913;
}
else 
{
goto label_31471;
}
}
else 
{
label_31471:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_31546;
}
else 
{
goto label_31546;
}
}
else 
{
goto label_31546;
}
}
}
else 
{
goto label_31546;
}
}
else 
{
label_31546:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_31980;
}
else 
{
if (s__state == 16384)
{
label_31980:; 
goto label_31982;
}
else 
{
if (s__state == 8192)
{
label_31982:; 
goto label_31984;
}
else 
{
if (s__state == 24576)
{
label_31984:; 
goto label_31986;
}
else 
{
if (s__state == 8195)
{
label_31986:; 
s__server = 1;
if (cb != 0)
{
goto label_31991;
}
else 
{
label_31991:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35604 = -1;
goto label_35605;
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
goto label_34915;
}
else 
{
s__init_buf___0 = buf;
goto label_32003;
}
}
else 
{
label_32003:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34915;
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
goto label_34915;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_32023;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_32023:; 
goto label_32025;
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
goto label_31959;
}
else 
{
if (s__state == 8481)
{
label_31959:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34915;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_32025;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_32025;
}
else 
{
if (s__state == 8464)
{
goto label_31938;
}
else 
{
if (s__state == 8465)
{
label_31938:; 
goto label_31940;
}
else 
{
if (s__state == 8466)
{
label_31940:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_32108:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35425;
}
else 
{
label_35425:; 
 __return_35555 = ret;
}
tmp = __return_35555;
goto label_35650;
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
goto label_32108;
}
else 
{
goto label_32098;
}
}
else 
{
label_32098:; 
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
goto label_31911;
}
else 
{
if (s__state == 8513)
{
label_31911:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34915;
}
else 
{
goto label_31921;
}
}
else 
{
skip = 1;
label_31921:; 
s__state = 8528;
s__init_num = 0;
goto label_32025;
}
}
else 
{
if (s__state == 8528)
{
goto label_31843;
}
else 
{
if (s__state == 8529)
{
label_31843:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_31852;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_31852:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_31899;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_31886;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_31893:; 
label_31899:; 
s__state = 8544;
s__init_num = 0;
goto label_32025;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_31875;
}
else 
{
tmp___7 = 512;
label_31875:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_31886;
}
else 
{
skip = 1;
goto label_31893;
}
}
}
}
}
}
else 
{
goto label_31886;
}
}
else 
{
label_31886:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_32106:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35427;
}
else 
{
label_35427:; 
 __return_35554 = ret;
}
tmp = __return_35554;
goto label_35664;
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
goto label_32106;
}
else 
{
goto label_32096;
}
}
else 
{
label_32096:; 
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
goto label_31791;
}
else 
{
if (s__state == 8545)
{
label_31791:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_31836;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_31805;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_31828:; 
label_31836:; 
goto label_32025;
}
}
else 
{
label_31805:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_31819;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_31828;
}
else 
{
label_31819:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34915;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_31828;
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
goto label_31776;
}
else 
{
if (s__state == 8561)
{
label_31776:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34915;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_32025;
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
goto label_34915;
}
else 
{
s__rwstate = 1;
goto label_31765;
}
}
else 
{
label_31765:; 
s__state = s__s3__tmp__next_state___0;
goto label_32025;
}
}
else 
{
if (s__state == 8576)
{
goto label_31733;
}
else 
{
if (s__state == 8577)
{
label_31733:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34915;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_31749;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34915;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_31749:; 
goto label_32025;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_31719;
}
else 
{
if (s__state == 8593)
{
label_31719:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34915;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_32025;
}
}
else 
{
if (s__state == 8608)
{
goto label_31706;
}
else 
{
if (s__state == 8609)
{
label_31706:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34915;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_32025:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34915;
}
else 
{
goto label_32100;
}
}
else 
{
label_32100:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_32175;
}
else 
{
goto label_32175;
}
}
else 
{
goto label_32175;
}
}
}
else 
{
goto label_32175;
}
}
else 
{
label_32175:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_32609;
}
else 
{
if (s__state == 16384)
{
label_32609:; 
goto label_32611;
}
else 
{
if (s__state == 8192)
{
label_32611:; 
goto label_32613;
}
else 
{
if (s__state == 24576)
{
label_32613:; 
goto label_32615;
}
else 
{
if (s__state == 8195)
{
label_32615:; 
s__server = 1;
if (cb != 0)
{
goto label_32620;
}
else 
{
label_32620:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35602 = -1;
goto label_35603;
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
goto label_34917;
}
else 
{
s__init_buf___0 = buf;
goto label_32632;
}
}
else 
{
label_32632:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34917;
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
goto label_34917;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_32652;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_32652:; 
goto label_32654;
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
goto label_32588;
}
else 
{
if (s__state == 8481)
{
label_32588:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34917;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_32654;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_32654;
}
else 
{
if (s__state == 8464)
{
goto label_32567;
}
else 
{
if (s__state == 8465)
{
label_32567:; 
goto label_32569;
}
else 
{
if (s__state == 8466)
{
label_32569:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_32737:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35417;
}
else 
{
label_35417:; 
 __return_35559 = ret;
}
tmp = __return_35559;
goto label_35650;
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
goto label_32737;
}
else 
{
goto label_32727;
}
}
else 
{
label_32727:; 
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
goto label_32540;
}
else 
{
if (s__state == 8513)
{
label_32540:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34917;
}
else 
{
goto label_32550;
}
}
else 
{
skip = 1;
label_32550:; 
s__state = 8528;
s__init_num = 0;
goto label_32654;
}
}
else 
{
if (s__state == 8528)
{
goto label_32472;
}
else 
{
if (s__state == 8529)
{
label_32472:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_32481;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_32481:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_32528;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_32515;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_32522:; 
label_32528:; 
s__state = 8544;
s__init_num = 0;
goto label_32654;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_32504;
}
else 
{
tmp___7 = 512;
label_32504:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_32515;
}
else 
{
skip = 1;
goto label_32522;
}
}
}
}
}
}
else 
{
goto label_32515;
}
}
else 
{
label_32515:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_32735:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35419;
}
else 
{
label_35419:; 
 __return_35558 = ret;
}
tmp = __return_35558;
goto label_35664;
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
goto label_32735;
}
else 
{
goto label_32725;
}
}
else 
{
label_32725:; 
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
goto label_32420;
}
else 
{
if (s__state == 8545)
{
label_32420:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_32465;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_32434;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_32457:; 
label_32465:; 
goto label_32654;
}
}
else 
{
label_32434:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_32448;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_32457;
}
else 
{
label_32448:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34917;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_32457;
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
goto label_32405;
}
else 
{
if (s__state == 8561)
{
label_32405:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34917;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_32654;
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
goto label_34917;
}
else 
{
s__rwstate = 1;
goto label_32394;
}
}
else 
{
label_32394:; 
s__state = s__s3__tmp__next_state___0;
goto label_32654;
}
}
else 
{
if (s__state == 8576)
{
goto label_32362;
}
else 
{
if (s__state == 8577)
{
label_32362:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34917;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_32378;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34917;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_32378:; 
goto label_32654;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_32348;
}
else 
{
if (s__state == 8593)
{
label_32348:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34917;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_32654;
}
}
else 
{
if (s__state == 8608)
{
goto label_32335;
}
else 
{
if (s__state == 8609)
{
label_32335:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34917;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_32654:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34917;
}
else 
{
goto label_32729;
}
}
else 
{
label_32729:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_32804;
}
else 
{
goto label_32804;
}
}
else 
{
goto label_32804;
}
}
}
else 
{
goto label_32804;
}
}
else 
{
label_32804:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_33238;
}
else 
{
if (s__state == 16384)
{
label_33238:; 
goto label_33240;
}
else 
{
if (s__state == 8192)
{
label_33240:; 
goto label_33242;
}
else 
{
if (s__state == 24576)
{
label_33242:; 
goto label_33244;
}
else 
{
if (s__state == 8195)
{
label_33244:; 
s__server = 1;
if (cb != 0)
{
goto label_33249;
}
else 
{
label_33249:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35600 = -1;
goto label_35601;
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
goto label_34919;
}
else 
{
s__init_buf___0 = buf;
goto label_33261;
}
}
else 
{
label_33261:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34919;
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
goto label_34919;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_33281;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_33281:; 
goto label_33283;
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
goto label_33217;
}
else 
{
if (s__state == 8481)
{
label_33217:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34919;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_33283;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_33283;
}
else 
{
if (s__state == 8464)
{
goto label_33196;
}
else 
{
if (s__state == 8465)
{
label_33196:; 
goto label_33198;
}
else 
{
if (s__state == 8466)
{
label_33198:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_33366:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35409;
}
else 
{
label_35409:; 
 __return_35563 = ret;
}
tmp = __return_35563;
goto label_35650;
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
goto label_33366;
}
else 
{
goto label_33356;
}
}
else 
{
label_33356:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_33431;
}
else 
{
goto label_33431;
}
}
else 
{
goto label_33431;
}
}
}
else 
{
goto label_33431;
}
}
else 
{
label_33431:; 
skip = 0;
goto label_21779;
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
goto label_33169;
}
else 
{
if (s__state == 8513)
{
label_33169:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34919;
}
else 
{
goto label_33179;
}
}
else 
{
skip = 1;
label_33179:; 
s__state = 8528;
s__init_num = 0;
goto label_33283;
}
}
else 
{
if (s__state == 8528)
{
goto label_33101;
}
else 
{
if (s__state == 8529)
{
label_33101:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_33110;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_33110:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_33157;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_33144;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_33151:; 
label_33157:; 
s__state = 8544;
s__init_num = 0;
goto label_33283;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_33133;
}
else 
{
tmp___7 = 512;
label_33133:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_33144;
}
else 
{
skip = 1;
goto label_33151;
}
}
}
}
}
}
else 
{
goto label_33144;
}
}
else 
{
label_33144:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_33364:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35411;
}
else 
{
label_35411:; 
 __return_35562 = ret;
}
tmp = __return_35562;
goto label_35664;
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
goto label_33364;
}
else 
{
goto label_33354;
}
}
else 
{
label_33354:; 
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
goto label_33049;
}
else 
{
if (s__state == 8545)
{
label_33049:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_33094;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_33063;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_33086:; 
label_33094:; 
goto label_33283;
}
}
else 
{
label_33063:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_33077;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_33086;
}
else 
{
label_33077:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34919;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_33086;
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
goto label_33034;
}
else 
{
if (s__state == 8561)
{
label_33034:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34919;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_33283;
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
goto label_34919;
}
else 
{
s__rwstate = 1;
goto label_33023;
}
}
else 
{
label_33023:; 
s__state = s__s3__tmp__next_state___0;
goto label_33283;
}
}
else 
{
if (s__state == 8576)
{
goto label_32991;
}
else 
{
if (s__state == 8577)
{
label_32991:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34919;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_33007;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34919;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_33007:; 
goto label_33283;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_32977;
}
else 
{
if (s__state == 8593)
{
label_32977:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34919;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_33283;
}
}
else 
{
if (s__state == 8608)
{
goto label_32964;
}
else 
{
if (s__state == 8609)
{
label_32964:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34919;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_33283:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34919;
}
else 
{
goto label_33358;
}
}
else 
{
label_33358:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_33433;
}
else 
{
goto label_33433;
}
}
else 
{
goto label_33433;
}
}
}
else 
{
goto label_33433;
}
}
else 
{
label_33433:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_33868;
}
else 
{
if (s__state == 16384)
{
label_33868:; 
goto label_33870;
}
else 
{
if (s__state == 8192)
{
label_33870:; 
goto label_33872;
}
else 
{
if (s__state == 24576)
{
label_33872:; 
goto label_33874;
}
else 
{
if (s__state == 8195)
{
label_33874:; 
s__server = 1;
if (cb != 0)
{
goto label_33879;
}
else 
{
label_33879:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35598 = -1;
goto label_35599;
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
goto label_34921;
}
else 
{
s__init_buf___0 = buf;
goto label_33891;
}
}
else 
{
label_33891:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34921;
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
goto label_34921;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_33911;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_33911:; 
goto label_33913;
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
goto label_33847;
}
else 
{
if (s__state == 8481)
{
label_33847:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34921;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_33913;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_33913;
}
else 
{
if (s__state == 8464)
{
goto label_33826;
}
else 
{
if (s__state == 8465)
{
label_33826:; 
goto label_33828;
}
else 
{
if (s__state == 8466)
{
label_33828:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_33996:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35401;
}
else 
{
label_35401:; 
 __return_35567 = ret;
}
tmp = __return_35567;
goto label_35650;
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
goto label_33996;
}
else 
{
goto label_33986;
}
}
else 
{
label_33986:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_34061;
}
else 
{
goto label_34061;
}
}
else 
{
goto label_34061;
}
}
}
else 
{
goto label_34061;
}
}
else 
{
label_34061:; 
skip = 0;
goto label_21779;
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
goto label_33799;
}
else 
{
if (s__state == 8513)
{
label_33799:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34921;
}
else 
{
goto label_33809;
}
}
else 
{
skip = 1;
label_33809:; 
s__state = 8528;
s__init_num = 0;
goto label_33913;
}
}
else 
{
if (s__state == 8528)
{
goto label_33731;
}
else 
{
if (s__state == 8529)
{
label_33731:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_33740;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_33740:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_33787;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_33774;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_33781:; 
label_33787:; 
s__state = 8544;
s__init_num = 0;
goto label_33913;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_33763;
}
else 
{
tmp___7 = 512;
label_33763:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_33774;
}
else 
{
skip = 1;
goto label_33781;
}
}
}
}
}
}
else 
{
goto label_33774;
}
}
else 
{
label_33774:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_33994:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35403;
}
else 
{
label_35403:; 
 __return_35566 = ret;
}
tmp = __return_35566;
goto label_35664;
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
goto label_33994;
}
else 
{
goto label_33984;
}
}
else 
{
label_33984:; 
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
goto label_33679;
}
else 
{
if (s__state == 8545)
{
label_33679:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_33724;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_33693;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_33716:; 
label_33724:; 
goto label_33913;
}
}
else 
{
label_33693:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_33707;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_33716;
}
else 
{
label_33707:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34921;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_33716;
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
goto label_33664;
}
else 
{
if (s__state == 8561)
{
label_33664:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34921;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_33913;
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
goto label_34921;
}
else 
{
s__rwstate = 1;
goto label_33653;
}
}
else 
{
label_33653:; 
s__state = s__s3__tmp__next_state___0;
goto label_33913;
}
}
else 
{
if (s__state == 8576)
{
goto label_33621;
}
else 
{
if (s__state == 8577)
{
label_33621:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34921;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_33637;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34921;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_33637:; 
goto label_33913;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_33607;
}
else 
{
if (s__state == 8593)
{
label_33607:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34921;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_33913;
}
}
else 
{
if (s__state == 8608)
{
goto label_33594;
}
else 
{
if (s__state == 8609)
{
label_33594:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34921;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_33913:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34921;
}
else 
{
goto label_33988;
}
}
else 
{
label_33988:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_34063;
}
else 
{
goto label_34063;
}
}
else 
{
goto label_34063;
}
}
}
else 
{
goto label_34063;
}
}
else 
{
label_34063:; 
skip = 0;
label_34075:; 
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_34498;
}
else 
{
if (s__state == 16384)
{
label_34498:; 
goto label_34500;
}
else 
{
if (s__state == 8192)
{
label_34500:; 
goto label_34502;
}
else 
{
if (s__state == 24576)
{
label_34502:; 
goto label_34504;
}
else 
{
if (s__state == 8195)
{
label_34504:; 
s__server = 1;
if (cb != 0)
{
goto label_34509;
}
else 
{
label_34509:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35596 = -1;
goto label_35597;
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
goto label_34923;
}
else 
{
s__init_buf___0 = buf;
goto label_34521;
}
}
else 
{
label_34521:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34923;
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
goto label_34923;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_34541;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_34541:; 
goto label_34543;
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
goto label_34477;
}
else 
{
if (s__state == 8481)
{
label_34477:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34923;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_34543;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_34543;
}
else 
{
if (s__state == 8464)
{
goto label_34456;
}
else 
{
if (s__state == 8465)
{
label_34456:; 
goto label_34458;
}
else 
{
if (s__state == 8466)
{
label_34458:; 
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
label_34626:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35393;
}
else 
{
label_35393:; 
 __return_35571 = ret;
}
tmp = __return_35571;
goto label_35650;
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
goto label_34626;
}
else 
{
goto label_34616;
}
}
else 
{
label_34616:; 
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
goto label_34429;
}
else 
{
if (s__state == 8513)
{
label_34429:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_34923;
}
else 
{
goto label_34439;
}
}
else 
{
skip = 1;
label_34439:; 
s__state = 8528;
s__init_num = 0;
goto label_34543;
}
}
else 
{
if (s__state == 8528)
{
goto label_34361;
}
else 
{
if (s__state == 8529)
{
label_34361:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_34370;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_34370:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_34417;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_34404;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_34411:; 
label_34417:; 
s__state = 8544;
s__init_num = 0;
goto label_34543;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_34393;
}
else 
{
tmp___7 = 512;
label_34393:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_34404;
}
else 
{
skip = 1;
goto label_34411;
}
}
}
}
}
}
else 
{
goto label_34404;
}
}
else 
{
label_34404:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
label_34624:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35395;
}
else 
{
label_35395:; 
 __return_35570 = ret;
}
tmp = __return_35570;
goto label_35664;
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
goto label_34624;
}
else 
{
goto label_34614;
}
}
else 
{
label_34614:; 
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
goto label_34309;
}
else 
{
if (s__state == 8545)
{
label_34309:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_34354;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_34323;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_34346:; 
label_34354:; 
goto label_34543;
}
}
else 
{
label_34323:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_34337;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_34346;
}
else 
{
label_34337:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_34923;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
goto label_34346;
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
goto label_34294;
}
else 
{
if (s__state == 8561)
{
label_34294:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34923;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_34543;
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
goto label_34923;
}
else 
{
s__rwstate = 1;
goto label_34283;
}
}
else 
{
label_34283:; 
s__state = s__s3__tmp__next_state___0;
goto label_34543;
}
}
else 
{
if (s__state == 8576)
{
goto label_34251;
}
else 
{
if (s__state == 8577)
{
label_34251:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34923;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_34267;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34923;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_34267:; 
goto label_34543;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_34237;
}
else 
{
if (s__state == 8593)
{
label_34237:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34923;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_34543;
}
}
else 
{
if (s__state == 8608)
{
goto label_34224;
}
else 
{
if (s__state == 8609)
{
label_34224:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34923;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_34543:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34923;
}
else 
{
goto label_34618;
}
}
else 
{
label_34618:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_34693;
}
else 
{
goto label_34693;
}
}
else 
{
goto label_34693;
}
}
}
else 
{
goto label_34693;
}
}
else 
{
label_34693:; 
skip = 0;
goto label_34075;
}
}
}
else 
{
if (s__state == 8640)
{
goto label_34206;
}
else 
{
if (s__state == 8641)
{
label_34206:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_34622:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35397;
}
else 
{
label_35397:; 
 __return_35569 = ret;
}
tmp = __return_35569;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_34216;
}
else 
{
s__state = 3;
label_34216:; 
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
goto label_34622;
}
else 
{
goto label_34612;
}
}
else 
{
label_34612:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_34687;
}
else 
{
goto label_34687;
}
}
else 
{
goto label_34687;
}
}
}
else 
{
goto label_34687;
}
}
else 
{
label_34687:; 
skip = 0;
goto label_28982;
}
}
}
}
else 
{
if (s__state == 8656)
{
goto label_34181;
}
else 
{
if (s__state == 8657)
{
label_34181:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34923;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_34620;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_34620:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35399;
}
else 
{
label_35399:; 
 __return_35568 = ret;
}
tmp = __return_35568;
goto label_35654;
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
goto label_34620;
}
else 
{
goto label_34610;
}
}
else 
{
label_34610:; 
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
goto label_34168;
}
else 
{
goto label_34168;
}
}
else 
{
label_34168:; 
ret = 1;
goto label_34923;
}
}
else 
{
ret = -1;
label_34923:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35345;
}
else 
{
label_35345:; 
 __return_35597 = ret;
label_35597:; 
}
tmp = __return_35597;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_33576;
}
else 
{
if (s__state == 8641)
{
label_33576:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_33992:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35405;
}
else 
{
label_35405:; 
 __return_35565 = ret;
}
tmp = __return_35565;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_33586;
}
else 
{
s__state = 3;
label_33586:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_33992;
}
else 
{
goto label_33982;
}
}
else 
{
label_33982:; 
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
goto label_33551;
}
else 
{
if (s__state == 8657)
{
label_33551:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34921;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_33990;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_33990:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35407;
}
else 
{
label_35407:; 
 __return_35564 = ret;
}
tmp = __return_35564;
goto label_35654;
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
goto label_33990;
}
else 
{
goto label_33980;
}
}
else 
{
label_33980:; 
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
goto label_33538;
}
else 
{
goto label_33538;
}
}
else 
{
label_33538:; 
ret = 1;
goto label_34921;
}
}
else 
{
ret = -1;
label_34921:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35347;
}
else 
{
label_35347:; 
 __return_35599 = ret;
label_35599:; 
}
tmp = __return_35599;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_32946;
}
else 
{
if (s__state == 8641)
{
label_32946:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_33362:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35413;
}
else 
{
label_35413:; 
 __return_35561 = ret;
}
tmp = __return_35561;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_32956;
}
else 
{
s__state = 3;
label_32956:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_33362;
}
else 
{
goto label_33352;
}
}
else 
{
label_33352:; 
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
goto label_32921;
}
else 
{
if (s__state == 8657)
{
label_32921:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34919;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_33360;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_33360:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35415;
}
else 
{
label_35415:; 
 __return_35560 = ret;
}
tmp = __return_35560;
goto label_35654;
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
goto label_33360;
}
else 
{
goto label_33350;
}
}
else 
{
label_33350:; 
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
goto label_32908;
}
else 
{
goto label_32908;
}
}
else 
{
label_32908:; 
ret = 1;
goto label_34919;
}
}
else 
{
ret = -1;
label_34919:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35349;
}
else 
{
label_35349:; 
 __return_35601 = ret;
label_35601:; 
}
tmp = __return_35601;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_32317;
}
else 
{
if (s__state == 8641)
{
label_32317:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_32733:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35421;
}
else 
{
label_35421:; 
 __return_35557 = ret;
}
tmp = __return_35557;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_32327;
}
else 
{
s__state = 3;
label_32327:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_32733;
}
else 
{
goto label_32723;
}
}
else 
{
label_32723:; 
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
goto label_32292;
}
else 
{
if (s__state == 8657)
{
label_32292:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34917;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_32731;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_32731:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35423;
}
else 
{
label_35423:; 
 __return_35556 = ret;
}
tmp = __return_35556;
goto label_35654;
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
goto label_32731;
}
else 
{
goto label_32721;
}
}
else 
{
label_32721:; 
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
goto label_32279;
}
else 
{
goto label_32279;
}
}
else 
{
label_32279:; 
ret = 1;
goto label_34917;
}
}
else 
{
ret = -1;
label_34917:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35351;
}
else 
{
label_35351:; 
 __return_35603 = ret;
label_35603:; 
}
tmp = __return_35603;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_31688;
}
else 
{
if (s__state == 8641)
{
label_31688:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_32104:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35429;
}
else 
{
label_35429:; 
 __return_35553 = ret;
}
tmp = __return_35553;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_31698;
}
else 
{
s__state = 3;
label_31698:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_32104;
}
else 
{
goto label_32094;
}
}
else 
{
label_32094:; 
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
goto label_31663;
}
else 
{
if (s__state == 8657)
{
label_31663:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34915;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_32102;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_32102:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35431;
}
else 
{
label_35431:; 
 __return_35552 = ret;
}
tmp = __return_35552;
goto label_35654;
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
goto label_32102;
}
else 
{
goto label_32092;
}
}
else 
{
label_32092:; 
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
goto label_31650;
}
else 
{
goto label_31650;
}
}
else 
{
label_31650:; 
ret = 1;
goto label_34915;
}
}
else 
{
ret = -1;
label_34915:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35353;
}
else 
{
label_35353:; 
 __return_35605 = ret;
label_35605:; 
}
tmp = __return_35605;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_31059;
}
else 
{
if (s__state == 8641)
{
label_31059:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_31475:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35437;
}
else 
{
label_35437:; 
 __return_35549 = ret;
}
tmp = __return_35549;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_31069;
}
else 
{
s__state = 3;
label_31069:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_31475;
}
else 
{
goto label_31465;
}
}
else 
{
label_31465:; 
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
goto label_31034;
}
else 
{
if (s__state == 8657)
{
label_31034:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34913;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_31473;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_31473:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35439;
}
else 
{
label_35439:; 
 __return_35548 = ret;
}
tmp = __return_35548;
goto label_35654;
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
goto label_31473;
}
else 
{
goto label_31463;
}
}
else 
{
label_31463:; 
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
goto label_31021;
}
else 
{
goto label_31021;
}
}
else 
{
label_31021:; 
ret = 1;
goto label_34913;
}
}
else 
{
ret = -1;
label_34913:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35355;
}
else 
{
label_35355:; 
 __return_35607 = ret;
label_35607:; 
}
tmp = __return_35607;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_24964;
}
else 
{
if (s__state == 8641)
{
label_24964:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_25380:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35489;
}
else 
{
label_35489:; 
 __return_35523 = ret;
}
tmp = __return_35523;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_24974;
}
else 
{
s__state = 3;
label_24974:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_25380;
}
else 
{
goto label_25370;
}
}
else 
{
label_25370:; 
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
goto label_24939;
}
else 
{
if (s__state == 8657)
{
label_24939:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34891;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_25378;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_25378:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35491;
}
else 
{
label_35491:; 
 __return_35522 = ret;
}
tmp = __return_35522;
goto label_35654;
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
goto label_25378;
}
else 
{
goto label_25368;
}
}
else 
{
label_25368:; 
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
goto label_24926;
}
else 
{
goto label_24926;
}
}
else 
{
label_24926:; 
ret = 1;
goto label_34891;
}
}
else 
{
ret = -1;
label_34891:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35377;
}
else 
{
label_35377:; 
 __return_35629 = ret;
label_35629:; 
}
tmp = __return_35629;
goto label_35652;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_22394;
}
else 
{
if (s__state == 8641)
{
label_22394:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_22810:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35505;
}
else 
{
label_35505:; 
 __return_35515 = ret;
}
tmp = __return_35515;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_22404;
}
else 
{
s__state = 3;
label_22404:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_22810;
}
else 
{
goto label_22800;
}
}
else 
{
label_22800:; 
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
goto label_22369;
}
else 
{
if (s__state == 8657)
{
label_22369:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34881;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_22808;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_22808:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35507;
}
else 
{
label_35507:; 
 __return_35514 = ret;
}
tmp = __return_35514;
goto label_35654;
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
goto label_22808;
}
else 
{
goto label_22798;
}
}
else 
{
label_22798:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_22873;
}
else 
{
goto label_22873;
}
}
else 
{
goto label_22873;
}
}
}
else 
{
goto label_22873;
}
}
else 
{
label_22873:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_23264;
}
else 
{
if (s__state == 16384)
{
label_23264:; 
goto label_23266;
}
else 
{
if (s__state == 8192)
{
label_23266:; 
goto label_23268;
}
else 
{
if (s__state == 24576)
{
label_23268:; 
goto label_23270;
}
else 
{
if (s__state == 8195)
{
label_23270:; 
s__server = 1;
if (cb != 0)
{
goto label_23275;
}
else 
{
label_23275:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35636 = -1;
goto label_35637;
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
goto label_34883;
}
else 
{
s__init_buf___0 = buf;
goto label_23287;
}
}
else 
{
label_23287:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34883;
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
goto label_34883;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_23307;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_23307:; 
goto label_23309;
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
goto label_23243;
}
else 
{
if (s__state == 8481)
{
label_23243:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34883;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_23309;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_23309;
}
else 
{
if (s__state == 8464)
{
goto label_23230;
}
else 
{
if (s__state == 8465)
{
label_23230:; 
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
goto label_23210;
}
else 
{
if (s__state == 8513)
{
label_23210:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_23309;
}
else 
{
if (s__state == 8528)
{
goto label_23152;
}
else 
{
if (s__state == 8529)
{
label_23152:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_23161;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_23161:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_23201;
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
label_23197:; 
label_23201:; 
s__state = 8544;
s__init_num = 0;
goto label_23309;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_23184;
}
else 
{
tmp___7 = 512;
label_23184:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_23197;
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
goto label_23110;
}
else 
{
if (s__state == 8545)
{
label_23110:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_23145;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_23124;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_23136:; 
label_23145:; 
goto label_23309;
}
}
else 
{
label_23124:; 
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
goto label_23136;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_23095;
}
else 
{
if (s__state == 8561)
{
label_23095:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34883;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_23309;
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
goto label_34883;
}
else 
{
s__rwstate = 1;
goto label_23084;
}
}
else 
{
label_23084:; 
s__state = s__s3__tmp__next_state___0;
goto label_23309;
}
}
else 
{
if (s__state == 8576)
{
goto label_23052;
}
else 
{
if (s__state == 8577)
{
label_23052:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34883;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_23068;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34883;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_23068:; 
goto label_23309;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_23038;
}
else 
{
if (s__state == 8593)
{
label_23038:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34883;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_23309;
}
}
else 
{
if (s__state == 8608)
{
goto label_23025;
}
else 
{
if (s__state == 8609)
{
label_23025:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34883;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_23309:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34883;
}
else 
{
goto label_23339;
}
}
else 
{
label_23339:; 
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
goto label_23008;
}
else 
{
if (s__state == 8657)
{
label_23008:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34883;
}
else 
{
if (s__state == 8672)
{
goto label_22989;
}
else 
{
if (s__state == 8673)
{
label_22989:; 
ret = ssl3_send_finished();
if (ret <= 0)
{
label_23341:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35499;
}
else 
{
label_35499:; 
 __return_35518 = ret;
}
tmp = __return_35518;
goto label_35656;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_23000;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_23000:; 
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
goto label_23341;
}
else 
{
goto label_23337;
}
}
else 
{
label_23337:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_23367;
}
else 
{
goto label_23367;
}
}
else 
{
goto label_23367;
}
}
}
else 
{
goto label_23367;
}
}
else 
{
label_23367:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_23748;
}
else 
{
if (s__state == 16384)
{
label_23748:; 
goto label_23750;
}
else 
{
if (s__state == 8192)
{
label_23750:; 
goto label_23752;
}
else 
{
if (s__state == 24576)
{
label_23752:; 
goto label_23754;
}
else 
{
if (s__state == 8195)
{
label_23754:; 
s__server = 1;
if (cb != 0)
{
goto label_23759;
}
else 
{
label_23759:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35634 = -1;
goto label_35635;
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
goto label_34885;
}
else 
{
s__init_buf___0 = buf;
goto label_23771;
}
}
else 
{
label_23771:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34885;
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
goto label_34885;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_23791;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_23791:; 
goto label_23793;
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
goto label_23727;
}
else 
{
if (s__state == 8481)
{
label_23727:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34885;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_23793;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_23793;
}
else 
{
if (s__state == 8464)
{
goto label_23714;
}
else 
{
if (s__state == 8465)
{
label_23714:; 
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
goto label_23694;
}
else 
{
if (s__state == 8513)
{
label_23694:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_23793;
}
else 
{
if (s__state == 8528)
{
goto label_23636;
}
else 
{
if (s__state == 8529)
{
label_23636:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_23645;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_23645:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_23685;
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
label_23681:; 
label_23685:; 
s__state = 8544;
s__init_num = 0;
goto label_23793;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_23668;
}
else 
{
tmp___7 = 512;
label_23668:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_23681;
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
goto label_23594;
}
else 
{
if (s__state == 8545)
{
label_23594:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_23629;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_23608;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_23620:; 
label_23629:; 
goto label_23793;
}
}
else 
{
label_23608:; 
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
goto label_23620;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_23579;
}
else 
{
if (s__state == 8561)
{
label_23579:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34885;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_23793;
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
goto label_34885;
}
else 
{
s__rwstate = 1;
goto label_23568;
}
}
else 
{
label_23568:; 
s__state = s__s3__tmp__next_state___0;
goto label_23793;
}
}
else 
{
if (s__state == 8576)
{
goto label_23536;
}
else 
{
if (s__state == 8577)
{
label_23536:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34885;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_23552;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34885;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_23552:; 
goto label_23793;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_23522;
}
else 
{
if (s__state == 8593)
{
label_23522:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34885;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_23793;
}
}
else 
{
if (s__state == 8608)
{
goto label_23509;
}
else 
{
if (s__state == 8609)
{
label_23509:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34885;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_23793:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34885;
}
else 
{
goto label_23823;
}
}
else 
{
label_23823:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_23853;
}
else 
{
goto label_23853;
}
}
else 
{
goto label_23853;
}
}
}
else 
{
goto label_23853;
}
}
else 
{
label_23853:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_24233;
}
else 
{
if (s__state == 16384)
{
label_24233:; 
goto label_24235;
}
else 
{
if (s__state == 8192)
{
label_24235:; 
goto label_24237;
}
else 
{
if (s__state == 24576)
{
label_24237:; 
goto label_24239;
}
else 
{
if (s__state == 8195)
{
label_24239:; 
s__server = 1;
if (cb != 0)
{
goto label_24244;
}
else 
{
label_24244:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35632 = -1;
goto label_35633;
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
goto label_34887;
}
else 
{
s__init_buf___0 = buf;
goto label_24256;
}
}
else 
{
label_24256:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34887;
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
goto label_34887;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_24276;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_24276:; 
goto label_24278;
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
goto label_24212;
}
else 
{
if (s__state == 8481)
{
label_24212:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34887;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_24278;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_24278;
}
else 
{
if (s__state == 8464)
{
goto label_24199;
}
else 
{
if (s__state == 8465)
{
label_24199:; 
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
goto label_24179;
}
else 
{
if (s__state == 8513)
{
label_24179:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_24278;
}
else 
{
if (s__state == 8528)
{
goto label_24121;
}
else 
{
if (s__state == 8529)
{
label_24121:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_24130;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_24130:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_24170;
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
label_24166:; 
label_24170:; 
s__state = 8544;
s__init_num = 0;
goto label_24278;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_24153;
}
else 
{
tmp___7 = 512;
label_24153:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_24166;
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
goto label_24079;
}
else 
{
if (s__state == 8545)
{
label_24079:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24114;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_24093;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_24105:; 
label_24114:; 
goto label_24278;
}
}
else 
{
label_24093:; 
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
goto label_24105;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_24064;
}
else 
{
if (s__state == 8561)
{
label_24064:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34887;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_24278;
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
goto label_34887;
}
else 
{
s__rwstate = 1;
goto label_24053;
}
}
else 
{
label_24053:; 
s__state = s__s3__tmp__next_state___0;
goto label_24278;
}
}
else 
{
if (s__state == 8576)
{
goto label_24021;
}
else 
{
if (s__state == 8577)
{
label_24021:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34887;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_24037;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34887;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_24037:; 
goto label_24278;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24007;
}
else 
{
if (s__state == 8593)
{
label_24007:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34887;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_24278;
}
}
else 
{
if (s__state == 8608)
{
goto label_23994;
}
else 
{
if (s__state == 8609)
{
label_23994:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34887;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_24278:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34887;
}
else 
{
goto label_24308;
}
}
else 
{
label_24308:; 
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
goto label_23976;
}
else 
{
if (s__state == 8641)
{
label_23976:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_24310:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35495;
}
else 
{
label_35495:; 
 __return_35520 = ret;
}
tmp = __return_35520;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_23986;
}
else 
{
s__state = 3;
label_23986:; 
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
goto label_24310;
}
else 
{
goto label_24306;
}
}
else 
{
label_24306:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_24336;
}
else 
{
goto label_24336;
}
}
else 
{
goto label_24336;
}
}
}
else 
{
goto label_24336;
}
}
else 
{
label_24336:; 
skip = 0;
state = s__state;
if (s__state == 12292)
{
s__new_session = 1;
goto label_24717;
}
else 
{
if (s__state == 16384)
{
label_24717:; 
goto label_24719;
}
else 
{
if (s__state == 8192)
{
label_24719:; 
goto label_24721;
}
else 
{
if (s__state == 24576)
{
label_24721:; 
goto label_24723;
}
else 
{
if (s__state == 8195)
{
label_24723:; 
s__server = 1;
if (cb != 0)
{
goto label_24728;
}
else 
{
label_24728:; 
__cil_tmp55 = s__version * 8;
if (__cil_tmp55 != 3)
{
 __return_35630 = -1;
goto label_35631;
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
goto label_34889;
}
else 
{
s__init_buf___0 = buf;
goto label_24740;
}
}
else 
{
label_24740:; 
tmp___4 = ssl3_setup_buffers();
if (tmp___4 == 0)
{
ret = -1;
goto label_34889;
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
goto label_34889;
}
else 
{
s__state = 8464;
int __CPAchecker_TMP_1 = s__ctx__stats__sess_accept;
s__ctx__stats__sess_accept = s__ctx__stats__sess_accept + 1;
__CPAchecker_TMP_1;
goto label_24760;
}
}
else 
{
int __CPAchecker_TMP_2 = s__ctx__stats__sess_accept_renegotiate;
s__ctx__stats__sess_accept_renegotiate = s__ctx__stats__sess_accept_renegotiate + 1;
__CPAchecker_TMP_2;
s__state = 8480;
label_24760:; 
goto label_24762;
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
goto label_24696;
}
else 
{
if (s__state == 8481)
{
label_24696:; 
s__shutdown = 0;
ret = &ssl3_send_hello_request;
if (ret <= 0)
{
goto label_34889;
}
else 
{
s__s3__tmp__next_state___0 = 8482;
s__state = 8448;
s__init_num = 0;
ssl3_init_finished_mac();
goto label_24762;
}
}
else 
{
if (s__state == 8482)
{
s__state = 3;
goto label_24762;
}
else 
{
if (s__state == 8464)
{
goto label_24683;
}
else 
{
if (s__state == 8465)
{
label_24683:; 
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
goto label_24663;
}
else 
{
if (s__state == 8513)
{
label_24663:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_24762;
}
else 
{
if (s__state == 8528)
{
goto label_24605;
}
else 
{
if (s__state == 8529)
{
label_24605:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_24614;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_24614:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_24654;
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
label_24650:; 
label_24654:; 
s__state = 8544;
s__init_num = 0;
goto label_24762;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_24637;
}
else 
{
tmp___7 = 512;
label_24637:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_24650;
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
goto label_24563;
}
else 
{
if (s__state == 8545)
{
label_24563:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_24598;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_24577;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_24589:; 
label_24598:; 
goto label_24762;
}
}
else 
{
label_24577:; 
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
goto label_24589;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_24548;
}
else 
{
if (s__state == 8561)
{
label_24548:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34889;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_24762;
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
goto label_34889;
}
else 
{
s__rwstate = 1;
goto label_24537;
}
}
else 
{
label_24537:; 
s__state = s__s3__tmp__next_state___0;
goto label_24762;
}
}
else 
{
if (s__state == 8576)
{
goto label_24505;
}
else 
{
if (s__state == 8577)
{
label_24505:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34889;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_24521;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34889;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_24521:; 
goto label_24762;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_24491;
}
else 
{
if (s__state == 8593)
{
label_24491:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34889;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_24762;
}
}
else 
{
if (s__state == 8608)
{
goto label_24478;
}
else 
{
if (s__state == 8609)
{
label_24478:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34889;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_24762:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34889;
}
else 
{
goto label_24792;
}
}
else 
{
label_24792:; 
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
goto label_24448;
}
else 
{
if (s__state == 8657)
{
label_24448:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_34889;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_24794;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_24794:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35493;
}
else 
{
label_35493:; 
 __return_35521 = ret;
}
tmp = __return_35521;
goto label_35654;
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
goto label_24794;
}
else 
{
goto label_24790;
}
}
else 
{
label_24790:; 
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
goto label_24435;
}
else 
{
goto label_24435;
}
}
else 
{
label_24435:; 
ret = 1;
goto label_34889;
}
}
else 
{
ret = -1;
label_34889:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35379;
}
else 
{
label_35379:; 
 __return_35631 = ret;
label_35631:; 
}
tmp = __return_35631;
label_35660:; 
 __return_35818 = tmp;
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
goto label_23964;
}
else 
{
if (s__state == 8657)
{
label_23964:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34887;
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
goto label_23951;
}
else 
{
goto label_23951;
}
}
else 
{
label_23951:; 
ret = 1;
goto label_34887;
}
}
else 
{
ret = -1;
label_34887:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35381;
}
else 
{
label_35381:; 
 __return_35633 = ret;
label_35633:; 
}
tmp = __return_35633;
goto label_35656;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_23491;
}
else 
{
if (s__state == 8641)
{
label_23491:; 
ret = ssl3_get_finished();
if (ret <= 0)
{
label_23825:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35497;
}
else 
{
label_35497:; 
 __return_35519 = ret;
}
tmp = __return_35519;
goto label_35660;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
goto label_23501;
}
else 
{
s__state = 3;
label_23501:; 
s__init_num = 0;
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_23825;
}
else 
{
goto label_23821;
}
}
else 
{
label_23821:; 
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
goto label_23479;
}
else 
{
if (s__state == 8657)
{
label_23479:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34885;
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
goto label_23466;
}
else 
{
goto label_23466;
}
}
else 
{
label_23466:; 
ret = 1;
goto label_34885;
}
}
else 
{
ret = -1;
label_34885:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35383;
}
else 
{
label_35383:; 
 __return_35635 = ret;
label_35635:; 
}
tmp = __return_35635;
label_35656:; 
 __return_35820 = tmp;
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
goto label_22981;
}
else 
{
goto label_22981;
}
}
else 
{
label_22981:; 
ret = 1;
goto label_34883;
}
}
else 
{
ret = -1;
label_34883:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35385;
}
else 
{
label_35385:; 
 __return_35637 = ret;
label_35637:; 
}
tmp = __return_35637;
label_35654:; 
 __return_35822 = tmp;
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
goto label_22356;
}
else 
{
goto label_22356;
}
}
else 
{
label_22356:; 
ret = 1;
goto label_34881;
}
}
else 
{
ret = -1;
label_34881:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35387;
}
else 
{
label_35387:; 
 __return_35639 = ret;
label_35639:; 
}
tmp = __return_35639;
label_35652:; 
 __return_35824 = tmp;
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
goto label_22087;
}
else 
{
if (s__state == 8513)
{
label_22087:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_22199;
}
else 
{
if (s__state == 8528)
{
goto label_22029;
}
else 
{
if (s__state == 8529)
{
label_22029:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_22038;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_22038:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_22078;
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
label_22074:; 
label_22078:; 
s__state = 8544;
s__init_num = 0;
goto label_22199;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_22061;
}
else 
{
tmp___7 = 512;
label_22061:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_22074;
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
goto label_21987;
}
else 
{
if (s__state == 8545)
{
label_21987:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_22022;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_22001;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_22013:; 
label_22022:; 
goto label_22199;
}
}
else 
{
label_22001:; 
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
goto label_22013;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_21972;
}
else 
{
if (s__state == 8561)
{
label_21972:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34879;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_22199;
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
goto label_34879;
}
else 
{
s__rwstate = 1;
goto label_21961;
}
}
else 
{
label_21961:; 
s__state = s__s3__tmp__next_state___0;
goto label_22199;
}
}
else 
{
if (s__state == 8576)
{
goto label_21929;
}
else 
{
if (s__state == 8577)
{
label_21929:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34879;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_21945;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34879;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_21945:; 
goto label_22199;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_21915;
}
else 
{
if (s__state == 8593)
{
label_21915:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34879;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_22199;
}
}
else 
{
if (s__state == 8608)
{
goto label_21902;
}
else 
{
if (s__state == 8609)
{
label_21902:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34879;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_22199:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34879;
}
else 
{
goto label_22229;
}
}
else 
{
label_22229:; 
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
goto label_21885;
}
else 
{
if (s__state == 8657)
{
label_21885:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34879;
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
goto label_21872;
}
else 
{
goto label_21872;
}
}
else 
{
label_21872:; 
ret = 1;
goto label_34879;
}
}
else 
{
ret = -1;
label_34879:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35389;
}
else 
{
label_35389:; 
 __return_35641 = ret;
label_35641:; 
}
tmp = __return_35641;
label_35650:; 
 __return_35826 = tmp;
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
goto label_21608;
}
else 
{
if (s__state == 8513)
{
label_21608:; 
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
skip = 1;
s__state = 8528;
s__init_num = 0;
goto label_21715;
}
else 
{
if (s__state == 8528)
{
goto label_21550;
}
else 
{
if (s__state == 8529)
{
label_21550:; 
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_21559;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_21559:; 
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_21599;
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
label_21595:; 
label_21599:; 
s__state = 8544;
s__init_num = 0;
goto label_21715;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_21582;
}
else 
{
tmp___7 = 512;
label_21582:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
return 1;
}
else 
{
skip = 1;
goto label_21595;
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
goto label_21508;
}
else 
{
if (s__state == 8545)
{
label_21508:; 
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_21543;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_21522;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_21534:; 
label_21543:; 
goto label_21715;
}
}
else 
{
label_21522:; 
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
goto label_21534;
}
}
}
}
else 
{
if (s__state == 8560)
{
goto label_21493;
}
else 
{
if (s__state == 8561)
{
label_21493:; 
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_34877;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_21715;
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
goto label_34877;
}
else 
{
s__rwstate = 1;
goto label_21482;
}
}
else 
{
label_21482:; 
s__state = s__s3__tmp__next_state___0;
goto label_21715;
}
}
else 
{
if (s__state == 8576)
{
goto label_21450;
}
else 
{
if (s__state == 8577)
{
label_21450:; 
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_34877;
}
else 
{
if (ret == 2)
{
s__state = 8466;
goto label_21466;
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_34877;
}
else 
{
s__init_num = 0;
s__state = 8592;
label_21466:; 
goto label_21715;
}
}
}
}
else 
{
if (s__state == 8592)
{
goto label_21436;
}
else 
{
if (s__state == 8593)
{
label_21436:; 
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_34877;
}
else 
{
s__state = 8608;
s__init_num = 0;
goto label_21715;
}
}
else 
{
if (s__state == 8608)
{
goto label_21423;
}
else 
{
if (s__state == 8609)
{
label_21423:; 
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_34877;
}
else 
{
s__state = 8640;
s__init_num = 0;
label_21715:; 
if (skip == 0)
{
if (!(s__debug == 0))
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_34877;
}
else 
{
goto label_21745;
}
}
else 
{
label_21745:; 
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
goto label_21406;
}
else 
{
if (s__state == 8657)
{
label_21406:; 
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
ret = -1;
goto label_34877;
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
goto label_21393;
}
else 
{
goto label_21393;
}
}
else 
{
label_21393:; 
ret = 1;
goto label_34877;
}
}
else 
{
ret = -1;
label_34877:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_35391;
}
else 
{
label_35391:; 
 __return_35643 = ret;
label_35643:; 
}
tmp = __return_35643;
goto label_35646;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_35644;
label_35646:; 
 __return_35828 = tmp;
return 1;
}
}
}
}
