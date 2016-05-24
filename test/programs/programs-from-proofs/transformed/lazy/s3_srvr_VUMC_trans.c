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
int __return_7063;
int __return_7075;
int __return_7061;
int __return_7060;
int __return_7058;
int __return_7057;
int __return_7056;
int __return_7055;
int __return_7054;
int __return_7053;
int __return_7051;
int __return_7050;
int __return_7049;
int __return_7048;
int __return_7052;
int __return_7059;
int __return_7047;
int __return_7045;
int __return_7044;
int __return_7046;
int __return_7043;
int __return_7042;
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
goto label_4630;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_4630;
}
else 
{
label_4630:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_7063 = -1;
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
goto label_6945;
}
else 
{
label_6945:; 
 __return_7061 = ret;
}
tmp = __return_7061;
goto label_7064;
}
else 
{
got_new_session = 1;
s__state = 8496;
label_4668:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_4678;
}
else 
{
if (s__debug == 0)
{
label_4685:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_4678;
}
else 
{
goto label_4678;
}
}
else 
{
goto label_4678;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_4691:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6949;
}
else 
{
label_6949:; 
 __return_7060 = ret;
goto label_7058;
}
}
else 
{
goto label_4685;
}
}
}
}
else 
{
label_4678:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_4691;
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
goto label_4752;
}
else 
{
if (s__debug == 0)
{
label_4767:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_4752;
}
else 
{
goto label_4752;
}
}
else 
{
goto label_4752;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_4782:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6957;
}
else 
{
label_6957:; 
 __return_7058 = ret;
label_7058:; 
}
tmp = __return_7058;
goto label_7064;
}
else 
{
goto label_4767;
}
}
}
}
else 
{
label_4752:; 
skip = 0;
state = s__state;
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_4782;
}
else 
{
goto label_4840;
}
}
else 
{
skip = 1;
label_4840:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_4855;
}
else 
{
if (s__debug == 0)
{
label_4862:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_4855;
}
else 
{
goto label_4855;
}
}
else 
{
goto label_4855;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_4868:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6961;
}
else 
{
label_6961:; 
 __return_7057 = ret;
}
tmp = __return_7057;
goto label_7064;
}
else 
{
goto label_4862;
}
}
}
}
else 
{
label_4855:; 
skip = 0;
state = s__state;
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_4914;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_4914:; 
if (!(s__s3__tmp__use_rsa_tmp == 0))
{
label_4921:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_4868;
}
else 
{
label_4961:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_4976;
}
else 
{
if (s__debug == 0)
{
label_4983:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_4976;
}
else 
{
goto label_4976;
}
}
else 
{
goto label_4976;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_4989:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6965;
}
else 
{
label_6965:; 
 __return_7056 = ret;
}
tmp = __return_7056;
goto label_7064;
}
else 
{
goto label_4983;
}
}
}
}
else 
{
label_4976:; 
skip = 0;
state = s__state;
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_5073;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_5037;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_5046:; 
label_5073:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5094;
}
else 
{
if (s__debug == 0)
{
label_5109:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5094;
}
else 
{
goto label_5094;
}
}
else 
{
goto label_5094;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5119:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6969;
}
else 
{
label_6969:; 
 __return_7055 = ret;
}
tmp = __return_7055;
goto label_7064;
}
else 
{
goto label_5109;
}
}
}
}
else 
{
label_5094:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_5119;
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
goto label_5720;
}
else 
{
if (s__debug == 0)
{
label_5727:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5720;
}
else 
{
goto label_5720;
}
}
else 
{
goto label_5720;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5733:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6887:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6973;
}
else 
{
label_6973:; 
 __return_7054 = ret;
}
tmp = __return_7054;
goto label_7064;
}
else 
{
goto label_5727;
}
}
}
}
else 
{
label_5720:; 
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
goto label_5733;
}
else 
{
s__rwstate = 1;
goto label_5780;
}
}
else 
{
label_5780:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5799;
}
else 
{
if (s__debug == 0)
{
label_5806:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5799;
}
else 
{
goto label_5799;
}
}
else 
{
goto label_5799;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5812:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6886:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6977;
}
else 
{
label_6977:; 
 __return_7053 = ret;
goto label_7048;
}
}
else 
{
goto label_5806;
}
}
}
}
else 
{
label_5799:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_5812;
}
else 
{
if (!(ret == 2))
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_5812;
}
else 
{
s__init_num = 0;
s__state = 8592;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5885;
}
else 
{
if (s__debug == 0)
{
label_5900:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5885;
}
else 
{
goto label_5885;
}
}
else 
{
goto label_5885;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5915:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6884:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6985;
}
else 
{
label_6985:; 
 __return_7051 = ret;
goto label_7048;
}
}
else 
{
goto label_5900;
}
}
}
}
else 
{
label_5885:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_5915;
}
else 
{
s__state = 8608;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5992;
}
else 
{
if (s__debug == 0)
{
label_5999:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5992;
}
else 
{
goto label_5992;
}
}
else 
{
goto label_5992;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6005:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6883:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6989;
}
else 
{
label_6989:; 
 __return_7050 = ret;
goto label_7048;
}
}
else 
{
goto label_5999;
}
}
}
}
else 
{
label_5992:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_6005;
}
else 
{
s__state = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6069;
}
else 
{
if (s__debug == 0)
{
label_6076:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6069;
}
else 
{
goto label_6069;
}
}
else 
{
goto label_6069;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6082:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6882:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6993;
}
else 
{
label_6993:; 
 __return_7049 = ret;
goto label_7048;
}
}
else 
{
goto label_6076;
}
}
}
}
else 
{
label_6069:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_6082;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_4742;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6158;
}
else 
{
if (s__debug == 0)
{
label_6165:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6158;
}
else 
{
goto label_6158;
}
}
else 
{
goto label_6158;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6171:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6881:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6997;
}
else 
{
label_6997:; 
 __return_7048 = ret;
label_7048:; 
}
tmp = __return_7048;
goto label_7064;
}
else 
{
goto label_6165;
}
}
}
}
else 
{
label_6158:; 
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
goto label_6235;
}
else 
{
goto label_6235;
}
}
else 
{
label_6235:; 
ret = 1;
goto label_6171;
}
}
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
goto label_5889;
}
else 
{
if (s__debug == 0)
{
label_5904:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5889;
}
else 
{
goto label_5889;
}
}
else 
{
goto label_5889;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5914:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_6885:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6981;
}
else 
{
label_6981:; 
 __return_7052 = ret;
goto label_7048;
}
}
else 
{
goto label_5904;
}
}
}
}
else 
{
label_5889:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_5914;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_4668;
}
}
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
label_5037:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_5053;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_5046;
}
else 
{
label_5053:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_4989;
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
goto label_5090;
}
else 
{
if (s__debug == 0)
{
label_5105:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5090;
}
else 
{
goto label_5090;
}
}
else 
{
goto label_5090;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5120:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6887;
}
else 
{
goto label_5105;
}
}
}
}
else 
{
label_5090:; 
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
goto label_5120;
}
else 
{
s__rwstate = 1;
goto label_5182;
}
}
else 
{
label_5182:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5201;
}
else 
{
if (s__debug == 0)
{
label_5208:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5201;
}
else 
{
goto label_5201;
}
}
else 
{
goto label_5201;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5214:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6886;
}
else 
{
goto label_5208;
}
}
}
}
else 
{
label_5201:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_5214;
}
else 
{
if (!(ret == 2))
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_5214;
}
else 
{
s__init_num = 0;
s__state = 8592;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5287;
}
else 
{
if (s__debug == 0)
{
label_5302:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5287;
}
else 
{
goto label_5287;
}
}
else 
{
goto label_5287;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5317:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6884;
}
else 
{
goto label_5302;
}
}
}
}
else 
{
label_5287:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_5317;
}
else 
{
s__state = 8608;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5394;
}
else 
{
if (s__debug == 0)
{
label_5401:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5394;
}
else 
{
goto label_5394;
}
}
else 
{
goto label_5394;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5407:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6883;
}
else 
{
goto label_5401;
}
}
}
}
else 
{
label_5394:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_5407;
}
else 
{
s__state = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5471;
}
else 
{
if (s__debug == 0)
{
label_5478:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5471;
}
else 
{
goto label_5471;
}
}
else 
{
goto label_5471;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5484:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6882;
}
else 
{
goto label_5478;
}
}
}
}
else 
{
label_5471:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_5484;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_4742;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5560;
}
else 
{
if (s__debug == 0)
{
label_5567:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5560;
}
else 
{
goto label_5560;
}
}
else 
{
goto label_5560;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5573:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6881;
}
else 
{
goto label_5567;
}
}
}
}
else 
{
label_5560:; 
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
goto label_5637;
}
else 
{
goto label_5637;
}
}
else 
{
label_5637:; 
ret = 1;
goto label_5573;
}
}
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
goto label_5291;
}
else 
{
if (s__debug == 0)
{
label_5306:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5291;
}
else 
{
goto label_5291;
}
}
else 
{
goto label_5291;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5316:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_6885;
}
else 
{
goto label_5306;
}
}
}
}
else 
{
label_5291:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_5316;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_4668;
}
}
}
}
}
}
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
goto label_4961;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_4921;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_4953:; 
goto label_4961;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_4940;
}
else 
{
tmp___7 = 512;
label_4940:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_4921;
}
else 
{
skip = 1;
goto label_4953;
}
}
}
}
}
}
else 
{
goto label_4921;
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
label_4742:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_4756;
}
else 
{
if (s__debug == 0)
{
label_4771:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_4756;
}
else 
{
goto label_4756;
}
}
else 
{
goto label_4756;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_4781:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_6953;
}
else 
{
label_6953:; 
 __return_7059 = ret;
goto label_7058;
}
}
else 
{
goto label_4771;
}
}
}
}
else 
{
label_4756:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_4781;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_4781;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_6335:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_7001;
}
else 
{
label_7001:; 
 __return_7047 = ret;
}
tmp = __return_7047;
goto label_7064;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6345;
}
else 
{
if (s__debug == 0)
{
label_6352:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6345;
}
else 
{
goto label_6345;
}
}
else 
{
goto label_6345;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_6335;
}
else 
{
goto label_6352;
}
}
}
}
else 
{
label_6345:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_6335;
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
goto label_6441;
}
else 
{
if (s__debug == 0)
{
label_6456:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6441;
}
else 
{
goto label_6441;
}
}
else 
{
goto label_6441;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6471:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_7009;
}
else 
{
label_7009:; 
 __return_7045 = ret;
label_7045:; 
}
tmp = __return_7045;
goto label_7064;
}
else 
{
goto label_6456;
}
}
}
}
else 
{
label_6441:; 
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
goto label_6471;
}
else 
{
s__rwstate = 1;
goto label_6533;
}
}
else 
{
label_6533:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6552;
}
else 
{
if (s__debug == 0)
{
label_6559:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6552;
}
else 
{
goto label_6552;
}
}
else 
{
goto label_6552;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6565:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_7013;
}
else 
{
label_7013:; 
 __return_7044 = ret;
goto label_7042;
}
}
else 
{
goto label_6559;
}
}
}
}
else 
{
label_6552:; 
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
goto label_6629;
}
else 
{
goto label_6629;
}
}
else 
{
label_6629:; 
ret = 1;
goto label_6565;
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
goto label_6445;
}
else 
{
if (s__debug == 0)
{
label_6460:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6445;
}
else 
{
goto label_6445;
}
}
else 
{
goto label_6445;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6470:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_7005;
}
else 
{
label_7005:; 
 __return_7046 = ret;
goto label_7045;
}
}
else 
{
goto label_6460;
}
}
}
}
else 
{
label_6445:; 
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
goto label_6470;
}
else 
{
s__rwstate = 1;
goto label_6675;
}
}
else 
{
label_6675:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6694;
}
else 
{
if (s__debug == 0)
{
label_6701:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6694;
}
else 
{
goto label_6694;
}
}
else 
{
goto label_6694;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6707:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_7017;
}
else 
{
label_7017:; 
 __return_7043 = ret;
goto label_7042;
}
}
else 
{
goto label_6701;
}
}
}
}
else 
{
label_6694:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_6707;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_4742;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_6783;
}
else 
{
if (s__debug == 0)
{
label_6790:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_6783;
}
else 
{
goto label_6783;
}
}
else 
{
goto label_6783;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_6796:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_7021;
}
else 
{
label_7021:; 
 __return_7042 = ret;
label_7042:; 
}
tmp = __return_7042;
goto label_7064;
}
else 
{
goto label_6790;
}
}
}
}
else 
{
label_6783:; 
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
goto label_6860;
}
else 
{
goto label_6860;
}
}
else 
{
label_6860:; 
ret = 1;
goto label_6796;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_7063;
label_7064:; 
 __return_7075 = tmp;
return 1;
}
}
}
}
