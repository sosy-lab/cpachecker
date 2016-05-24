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
int __return_8749;
int __return_8761;
int __return_8747;
int __return_8746;
int __return_8744;
int __return_8743;
int __return_8742;
int __return_8741;
int __return_8740;
int __return_8739;
int __return_8737;
int __return_8736;
int __return_8735;
int __return_8734;
int __return_8738;
int __return_8745;
int __return_8733;
int __return_8731;
int __return_8730;
int __return_8732;
int __return_8729;
int __return_8728;
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
goto label_522;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_522;
}
else 
{
label_522:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_8749 = -1;
}
else 
{
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8631;
}
else 
{
label_8631:; 
 __return_8747 = ret;
}
tmp = __return_8747;
goto label_8750;
}
else 
{
got_new_session = 1;
s__state = 8496;
label_6355:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6364;
}
else 
{
if (s__debug == 0)
{
label_6371:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6364;
}
else 
{
goto label_6364;
}
}
else 
{
goto label_6364;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6377:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8635;
}
else 
{
label_8635:; 
 __return_8746 = ret;
goto label_8744;
}
}
else 
{
goto label_6371;
}
}
}
}
else 
{
label_6364:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_6377;
}
else 
{
if (s__hit == 0)
{
s__state = 8512;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6438;
}
else 
{
if (s__debug == 0)
{
label_6453:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6438;
}
else 
{
goto label_6438;
}
}
else 
{
goto label_6438;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6468:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8643;
}
else 
{
label_8643:; 
 __return_8744 = ret;
label_8744:; 
}
tmp = __return_8744;
goto label_8750;
}
else 
{
goto label_6453;
}
}
}
}
else 
{
label_6438:; 
skip = 0;
state = s__state;
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_6468;
}
else 
{
goto label_6526;
}
}
else 
{
skip = 1;
label_6526:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6541;
}
else 
{
if (s__debug == 0)
{
label_6548:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6541;
}
else 
{
goto label_6541;
}
}
else 
{
goto label_6541;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6554:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8647;
}
else 
{
label_8647:; 
 __return_8743 = ret;
}
tmp = __return_8743;
goto label_8750;
}
else 
{
goto label_6548;
}
}
}
}
else 
{
label_6541:; 
skip = 0;
state = s__state;
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_6600;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_6600:; 
if (!(s__s3__tmp__use_rsa_tmp == 0))
{
label_6607:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_6554;
}
else 
{
label_6647:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6662;
}
else 
{
if (s__debug == 0)
{
label_6669:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6662;
}
else 
{
goto label_6662;
}
}
else 
{
goto label_6662;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6675:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8651;
}
else 
{
label_8651:; 
 __return_8742 = ret;
}
tmp = __return_8742;
goto label_8750;
}
else 
{
goto label_6669;
}
}
}
}
else 
{
label_6662:; 
skip = 0;
state = s__state;
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_6759;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_6723;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_6732:; 
label_6759:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6780;
}
else 
{
if (s__debug == 0)
{
label_6795:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6780;
}
else 
{
goto label_6780;
}
}
else 
{
goto label_6780;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6805:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8655;
}
else 
{
label_8655:; 
 __return_8741 = ret;
}
tmp = __return_8741;
goto label_8750;
}
else 
{
goto label_6795;
}
}
}
}
else 
{
label_6780:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_6805;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7406;
}
else 
{
if (s__debug == 0)
{
label_7413:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7406;
}
else 
{
goto label_7406;
}
}
else 
{
goto label_7406;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7419:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8573:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8659;
}
else 
{
label_8659:; 
 __return_8740 = ret;
}
tmp = __return_8740;
goto label_8750;
}
else 
{
goto label_7413;
}
}
}
}
else 
{
label_7406:; 
skip = 0;
state = s__state;
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_7419;
}
else 
{
s__rwstate = 1;
goto label_7466;
}
}
else 
{
label_7466:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7485;
}
else 
{
if (s__debug == 0)
{
label_7492:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7485;
}
else 
{
goto label_7485;
}
}
else 
{
goto label_7485;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7498:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8572:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8663;
}
else 
{
label_8663:; 
 __return_8739 = ret;
goto label_8734;
}
}
else 
{
goto label_7492;
}
}
}
}
else 
{
label_7485:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_7498;
}
else 
{
if (!(ret == 2))
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_7498;
}
else 
{
s__init_num = 0;
s__state = 8592;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7571;
}
else 
{
if (s__debug == 0)
{
label_7586:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7571;
}
else 
{
goto label_7571;
}
}
else 
{
goto label_7571;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7601:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8570:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8671;
}
else 
{
label_8671:; 
 __return_8737 = ret;
goto label_8734;
}
}
else 
{
goto label_7586;
}
}
}
}
else 
{
label_7571:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_7601;
}
else 
{
s__state = 8608;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7678;
}
else 
{
if (s__debug == 0)
{
label_7685:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7678;
}
else 
{
goto label_7678;
}
}
else 
{
goto label_7678;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7691:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8569:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8675;
}
else 
{
label_8675:; 
 __return_8736 = ret;
goto label_8734;
}
}
else 
{
goto label_7685;
}
}
}
}
else 
{
label_7678:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_7691;
}
else 
{
s__state = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7755;
}
else 
{
if (s__debug == 0)
{
label_7762:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7755;
}
else 
{
goto label_7755;
}
}
else 
{
goto label_7755;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7768:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8568:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8679;
}
else 
{
label_8679:; 
 __return_8735 = ret;
goto label_8734;
}
}
else 
{
goto label_7762;
}
}
}
}
else 
{
label_7755:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_7768;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_6428;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7844;
}
else 
{
if (s__debug == 0)
{
label_7851:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7844;
}
else 
{
goto label_7844;
}
}
else 
{
goto label_7844;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7857:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8567:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8683;
}
else 
{
label_8683:; 
 __return_8734 = ret;
label_8734:; 
}
tmp = __return_8734;
goto label_8750;
}
else 
{
goto label_7851;
}
}
}
}
else 
{
label_7844:; 
skip = 0;
state = s__state;
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
goto label_7921;
}
else 
{
goto label_7921;
}
}
else 
{
label_7921:; 
ret = 1;
goto label_7857;
}
}
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
s__state = 8466;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7575;
}
else 
{
if (s__debug == 0)
{
label_7590:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7575;
}
else 
{
goto label_7575;
}
}
else 
{
goto label_7575;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7600:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_8571:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8667;
}
else 
{
label_8667:; 
 __return_8738 = ret;
goto label_8734;
}
}
else 
{
goto label_7590;
}
}
}
}
else 
{
label_7575:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_7600;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_6355;
}
}
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
label_6723:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_6739;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_6732;
}
else 
{
label_6739:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_6675;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6776;
}
else 
{
if (s__debug == 0)
{
label_6791:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6776;
}
else 
{
goto label_6776;
}
}
else 
{
goto label_6776;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6806:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8573;
}
else 
{
goto label_6791;
}
}
}
}
else 
{
label_6776:; 
skip = 0;
state = s__state;
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_6806;
}
else 
{
s__rwstate = 1;
goto label_6868;
}
}
else 
{
label_6868:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6887;
}
else 
{
if (s__debug == 0)
{
label_6894:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6887;
}
else 
{
goto label_6887;
}
}
else 
{
goto label_6887;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6900:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8572;
}
else 
{
goto label_6894;
}
}
}
}
else 
{
label_6887:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_6900;
}
else 
{
if (!(ret == 2))
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_6900;
}
else 
{
s__init_num = 0;
s__state = 8592;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6973;
}
else 
{
if (s__debug == 0)
{
label_6988:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6973;
}
else 
{
goto label_6973;
}
}
else 
{
goto label_6973;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7003:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8570;
}
else 
{
goto label_6988;
}
}
}
}
else 
{
label_6973:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_7003;
}
else 
{
s__state = 8608;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7080;
}
else 
{
if (s__debug == 0)
{
label_7087:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7080;
}
else 
{
goto label_7080;
}
}
else 
{
goto label_7080;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7093:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8569;
}
else 
{
goto label_7087;
}
}
}
}
else 
{
label_7080:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_7093;
}
else 
{
s__state = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7157;
}
else 
{
if (s__debug == 0)
{
label_7164:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7157;
}
else 
{
goto label_7157;
}
}
else 
{
goto label_7157;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7170:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8568;
}
else 
{
goto label_7164;
}
}
}
}
else 
{
label_7157:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_7170;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_6428;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7246;
}
else 
{
if (s__debug == 0)
{
label_7253:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7246;
}
else 
{
goto label_7246;
}
}
else 
{
goto label_7246;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7259:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8567;
}
else 
{
goto label_7253;
}
}
}
}
else 
{
label_7246:; 
skip = 0;
state = s__state;
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
goto label_7323;
}
else 
{
goto label_7323;
}
}
else 
{
label_7323:; 
ret = 1;
goto label_7259;
}
}
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
s__state = 8466;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6977;
}
else 
{
if (s__debug == 0)
{
label_6992:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6977;
}
else 
{
goto label_6977;
}
}
else 
{
goto label_6977;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7002:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_8571;
}
else 
{
goto label_6992;
}
}
}
}
else 
{
label_6977:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_7002;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_6355;
}
}
}
}
}
}
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
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_6647;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_6607;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_6639:; 
goto label_6647;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_6626;
}
else 
{
tmp___7 = 512;
label_6626:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_6607;
}
else 
{
skip = 1;
goto label_6639;
}
}
}
}
}
}
else 
{
goto label_6607;
}
}
}
}
}
}
}
else 
{
s__state = 8656;
s__init_num = 0;
label_6428:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6442;
}
else 
{
if (s__debug == 0)
{
label_6457:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6442;
}
else 
{
goto label_6442;
}
}
else 
{
goto label_6442;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6467:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8639;
}
else 
{
label_8639:; 
 __return_8745 = ret;
goto label_8744;
}
}
else 
{
goto label_6457;
}
}
}
}
else 
{
label_6442:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_6467;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_6467;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_8021:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8687;
}
else 
{
label_8687:; 
 __return_8733 = ret;
}
tmp = __return_8733;
goto label_8750;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8031;
}
else 
{
if (s__debug == 0)
{
label_8038:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8031;
}
else 
{
goto label_8031;
}
}
else 
{
goto label_8031;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_8021;
}
else 
{
goto label_8038;
}
}
}
}
else 
{
label_8031:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_8021;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8127;
}
else 
{
if (s__debug == 0)
{
label_8142:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8127;
}
else 
{
goto label_8127;
}
}
else 
{
goto label_8127;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8157:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8695;
}
else 
{
label_8695:; 
 __return_8731 = ret;
label_8731:; 
}
tmp = __return_8731;
goto label_8750;
}
else 
{
goto label_8142;
}
}
}
}
else 
{
label_8127:; 
skip = 0;
state = s__state;
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_8157;
}
else 
{
s__rwstate = 1;
goto label_8219;
}
}
else 
{
label_8219:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8238;
}
else 
{
if (s__debug == 0)
{
label_8245:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8238;
}
else 
{
goto label_8238;
}
}
else 
{
goto label_8238;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8251:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8699;
}
else 
{
label_8699:; 
 __return_8730 = ret;
goto label_8728;
}
}
else 
{
goto label_8245;
}
}
}
}
else 
{
label_8238:; 
skip = 0;
state = s__state;
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
goto label_8315;
}
else 
{
goto label_8315;
}
}
else 
{
label_8315:; 
ret = 1;
goto label_8251;
}
}
}
}
}
else 
{
s__s3__tmp__next_state___0 = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8131;
}
else 
{
if (s__debug == 0)
{
label_8146:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8131;
}
else 
{
goto label_8131;
}
}
else 
{
goto label_8131;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8156:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8691;
}
else 
{
label_8691:; 
 __return_8732 = ret;
goto label_8731;
}
}
else 
{
goto label_8146;
}
}
}
}
else 
{
label_8131:; 
skip = 0;
state = s__state;
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_8156;
}
else 
{
s__rwstate = 1;
goto label_8361;
}
}
else 
{
label_8361:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8380;
}
else 
{
if (s__debug == 0)
{
label_8387:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8380;
}
else 
{
goto label_8380;
}
}
else 
{
goto label_8380;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8393:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8703;
}
else 
{
label_8703:; 
 __return_8729 = ret;
goto label_8728;
}
}
else 
{
goto label_8387;
}
}
}
}
else 
{
label_8380:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_8393;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_6428;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8469;
}
else 
{
if (s__debug == 0)
{
label_8476:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8469;
}
else 
{
goto label_8469;
}
}
else 
{
goto label_8469;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8482:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_8707;
}
else 
{
label_8707:; 
 __return_8728 = ret;
label_8728:; 
}
tmp = __return_8728;
goto label_8750;
}
else 
{
goto label_8476;
}
}
}
}
else 
{
label_8469:; 
skip = 0;
state = s__state;
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
goto label_8546;
}
else 
{
goto label_8546;
}
}
else 
{
label_8546:; 
ret = 1;
goto label_8482;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_8749;
label_8750:; 
 __return_8761 = tmp;
return 1;
}
}
}
}
