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
int __return_16169=0;
int __return_16175=0;
int __return_16089=0;
int __return_16167=0;
int __return_16091=0;
int __return_16165=0;
int __return_16095=0;
int __return_16161=0;
int __return_16097=0;
int __return_16159=0;
int __return_16099=0;
int __return_16157=0;
int __return_16101=0;
int __return_16155=0;
int __return_16103=0;
int __return_16153=0;
int __return_16105=0;
int __return_16151=0;
int __return_16107=0;
int __return_16149=0;
int __return_16109=0;
int __return_16147=0;
int __return_16111=0;
int __return_16145=0;
int __return_16113=0;
int __return_16143=0;
int __return_16115=0;
int __return_16141=0;
int __return_16093=0;
int __return_16163=0;
int __return_16117=0;
int __return_16139=0;
int __return_16121=0;
int __return_16135=0;
int __return_16123=0;
int __return_16133=0;
int __return_16119=0;
int __return_16137=0;
int __return_16125=0;
int __return_16131=0;
int __return_16127=0;
int __return_16129=0;
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
goto label_12085;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
label_12085:; 
int __CPAchecker_TMP_0 = s__in_handshake;
label_12091:; 
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_16169 = -1;
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
 __return_16089 = ret;
label_16089:; 
}
else 
{
 __return_16167 = ret;
goto label_16089;
}
tmp = __return_16089;
goto label_16171;
}
else 
{
got_new_session = 1;
s__state = 8496;
label_12149:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_12172:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12163;
}
else 
{
goto label_12163;
}
}
else 
{
goto label_12163;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12183:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16091 = ret;
goto label_16089;
}
else 
{
 __return_16165 = ret;
goto label_16089;
}
}
else 
{
goto label_12172;
}
}
}
else 
{
goto label_12163;
}
}
else 
{
label_12163:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_12183;
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
label_12302:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12283;
}
else 
{
goto label_12283;
}
}
else 
{
goto label_12283;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12327:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16095 = ret;
goto label_16089;
}
else 
{
 __return_16161 = ret;
goto label_16089;
}
}
else 
{
goto label_12302;
}
}
}
else 
{
goto label_12283;
}
}
else 
{
label_12283:; 
skip = 0;
state = s__state;
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_12327;
}
else 
{
goto label_12423;
}
}
else 
{
skip = 1;
label_12423:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_12456:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12447;
}
else 
{
goto label_12447;
}
}
else 
{
goto label_12447;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12467:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16097 = ret;
goto label_16089;
}
else 
{
 __return_16159 = ret;
goto label_16089;
}
}
else 
{
goto label_12456;
}
}
}
else 
{
goto label_12447;
}
}
else 
{
label_12447:; 
skip = 0;
state = s__state;
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_12547;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_12547:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_12625;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_12557;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_12609:; 
goto label_12625;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_12589;
}
else 
{
tmp___7 = 512;
label_12589:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_12557;
}
else 
{
skip = 1;
goto label_12609;
}
}
}
}
}
}
else 
{
goto label_12557;
}
}
else 
{
label_12557:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_12467;
}
else 
{
label_12625:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_12656:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12647;
}
else 
{
goto label_12647;
}
}
else 
{
goto label_12647;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12667:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16099 = ret;
goto label_16089;
}
else 
{
 __return_16157 = ret;
goto label_16089;
}
}
else 
{
goto label_12656;
}
}
}
else 
{
goto label_12647;
}
}
else 
{
label_12647:; 
skip = 0;
state = s__state;
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_12809;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_12749;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_12763:; 
label_12809:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_12858:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12839;
}
else 
{
goto label_12839;
}
}
else 
{
goto label_12839;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12877:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16101 = ret;
goto label_16089;
}
else 
{
 __return_16155 = ret;
goto label_16089;
}
}
else 
{
goto label_12858;
}
}
}
else 
{
goto label_12839;
}
}
else 
{
label_12839:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_12877;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13902:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13893;
}
else 
{
goto label_13893;
}
}
else 
{
goto label_13893;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13913:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15859:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16103 = ret;
goto label_16089;
}
else 
{
 __return_16153 = ret;
goto label_16089;
}
}
else 
{
goto label_13902;
}
}
}
else 
{
goto label_13893;
}
}
else 
{
label_13893:; 
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
goto label_13913;
}
else 
{
s__rwstate = 1;
goto label_13997;
}
}
else 
{
label_13997:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14034:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14025;
}
else 
{
goto label_14025;
}
}
else 
{
goto label_14025;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_14045:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15857:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16105 = ret;
goto label_16089;
}
else 
{
 __return_16151 = ret;
goto label_16089;
}
}
else 
{
goto label_14034;
}
}
}
else 
{
goto label_14025;
}
}
else 
{
label_14025:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_14045;
}
else 
{
if (ret == 2)
{
s__state = 8466;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14196:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14177;
}
else 
{
goto label_14177;
}
}
else 
{
goto label_14177;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_14215:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15855:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16107 = ret;
goto label_16089;
}
else 
{
 __return_16149 = ret;
goto label_16089;
}
}
else 
{
goto label_14196;
}
}
}
else 
{
goto label_14177;
}
}
else 
{
label_14177:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_14215;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_12149;
}
}
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_14045;
}
else 
{
s__init_num = 0;
s__state = 8592;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14192:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14173;
}
else 
{
goto label_14173;
}
}
else 
{
goto label_14173;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_14217:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15853:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16109 = ret;
goto label_16089;
}
else 
{
 __return_16147 = ret;
goto label_16089;
}
}
else 
{
goto label_14192;
}
}
}
else 
{
goto label_14173;
}
}
else 
{
label_14173:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_14217;
}
else 
{
s__state = 8608;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14356:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14347;
}
else 
{
goto label_14347;
}
}
else 
{
goto label_14347;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_14367:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15851:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16111 = ret;
goto label_16089;
}
else 
{
 __return_16145 = ret;
goto label_16089;
}
}
else 
{
goto label_14356;
}
}
}
else 
{
goto label_14347;
}
}
else 
{
label_14347:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_14367;
}
else 
{
s__state = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14488:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14479;
}
else 
{
goto label_14479;
}
}
else 
{
goto label_14479;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_14499:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15849:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16113 = ret;
goto label_16089;
}
else 
{
 __return_16143 = ret;
goto label_16089;
}
}
else 
{
goto label_14488;
}
}
}
else 
{
goto label_14479;
}
}
else 
{
label_14479:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_14499;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_12267;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14638:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14629;
}
else 
{
goto label_14629;
}
}
else 
{
goto label_14629;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_14649:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_15847:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16115 = ret;
goto label_16089;
}
else 
{
 __return_16141 = ret;
goto label_16089;
}
}
else 
{
goto label_14638;
}
}
}
else 
{
goto label_14629;
}
}
else 
{
label_14629:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_14760:; 
ret = 1;
goto label_14649;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_14760;
}
else 
{
goto label_14760;
}
}
}
}
}
}
}
}
}
}
}
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
label_12749:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_12777;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_12763;
}
else 
{
label_12777:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_12667;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_12854:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12835;
}
else 
{
goto label_12835;
}
}
else 
{
goto label_12835;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12879:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15859;
}
else 
{
goto label_12854;
}
}
}
else 
{
goto label_12835;
}
}
else 
{
label_12835:; 
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
goto label_12879;
}
else 
{
s__rwstate = 1;
goto label_12985;
}
}
else 
{
label_12985:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13022:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13013;
}
else 
{
goto label_13013;
}
}
else 
{
goto label_13013;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13033:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15857;
}
else 
{
goto label_13022;
}
}
}
else 
{
goto label_13013;
}
}
else 
{
label_13013:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_13033;
}
else 
{
if (ret == 2)
{
s__state = 8466;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13184:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13165;
}
else 
{
goto label_13165;
}
}
else 
{
goto label_13165;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13203:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15855;
}
else 
{
goto label_13184;
}
}
}
else 
{
goto label_13165;
}
}
else 
{
label_13165:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_13203;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_12149;
}
}
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_13033;
}
else 
{
s__init_num = 0;
s__state = 8592;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13180:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13161;
}
else 
{
goto label_13161;
}
}
else 
{
goto label_13161;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13205:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15853;
}
else 
{
goto label_13180;
}
}
}
else 
{
goto label_13161;
}
}
else 
{
label_13161:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_13205;
}
else 
{
s__state = 8608;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13344:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13335;
}
else 
{
goto label_13335;
}
}
else 
{
goto label_13335;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13355:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15851;
}
else 
{
goto label_13344;
}
}
}
else 
{
goto label_13335;
}
}
else 
{
label_13335:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_13355;
}
else 
{
s__state = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13476:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13467;
}
else 
{
goto label_13467;
}
}
else 
{
goto label_13467;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13487:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15849;
}
else 
{
goto label_13476;
}
}
}
else 
{
goto label_13467;
}
}
else 
{
label_13467:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_13487;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_12267;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_13626:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_13617;
}
else 
{
goto label_13617;
}
}
else 
{
goto label_13617;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_13637:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_15847;
}
else 
{
goto label_13626;
}
}
}
else 
{
goto label_13617;
}
}
else 
{
label_13617:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_13748:; 
ret = 1;
goto label_13637;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_13748;
}
else 
{
goto label_13748;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
s__state = 8656;
s__init_num = 0;
label_12267:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_12306:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_12287;
}
else 
{
goto label_12287;
}
}
else 
{
goto label_12287;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_12325:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16093 = ret;
goto label_16089;
}
else 
{
 __return_16163 = ret;
goto label_16089;
}
}
else 
{
goto label_12306;
}
}
}
else 
{
goto label_12287;
}
}
else 
{
label_12287:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_12325;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_12325;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_14933:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16117 = ret;
goto label_16089;
}
else 
{
 __return_16139 = ret;
goto label_16089;
}
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_14956:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_14947;
}
else 
{
goto label_14947;
}
}
else 
{
goto label_14947;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_14933;
}
else 
{
goto label_14956;
}
}
}
else 
{
goto label_14947;
}
}
else 
{
label_14947:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_14933;
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
if (skip == 0)
{
if (s__debug == 0)
{
label_15130:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15111;
}
else 
{
goto label_15111;
}
}
else 
{
goto label_15111;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_15155:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16121 = ret;
goto label_16089;
}
else 
{
 __return_16135 = ret;
goto label_16089;
}
}
else 
{
goto label_15130;
}
}
}
else 
{
goto label_15111;
}
}
else 
{
label_15111:; 
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
goto label_15155;
}
else 
{
s__rwstate = 1;
goto label_15261;
}
}
else 
{
label_15261:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_15298:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15289;
}
else 
{
goto label_15289;
}
}
else 
{
goto label_15289;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_15309:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16123 = ret;
goto label_16089;
}
else 
{
 __return_16133 = ret;
goto label_16089;
}
}
else 
{
goto label_15298;
}
}
}
else 
{
goto label_15289;
}
}
else 
{
label_15289:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_15420:; 
ret = 1;
goto label_15309;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_15420;
}
else 
{
goto label_15420;
}
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
if (skip == 0)
{
if (s__debug == 0)
{
label_15134:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15115;
}
else 
{
goto label_15115;
}
}
else 
{
goto label_15115;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_15153:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16119 = ret;
goto label_16089;
}
else 
{
 __return_16137 = ret;
goto label_16089;
}
}
else 
{
goto label_15134;
}
}
}
else 
{
goto label_15115;
}
}
else 
{
label_15115:; 
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
goto label_15153;
}
else 
{
s__rwstate = 1;
goto label_15503;
}
}
else 
{
label_15503:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_15540:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15531;
}
else 
{
goto label_15531;
}
}
else 
{
goto label_15531;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_15551:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16125 = ret;
goto label_16089;
}
else 
{
 __return_16131 = ret;
goto label_16089;
}
}
else 
{
goto label_15540;
}
}
}
else 
{
goto label_15531;
}
}
else 
{
label_15531:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_15551;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_12267;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_15690:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15681;
}
else 
{
goto label_15681;
}
}
else 
{
goto label_15681;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_15701:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_16127 = ret;
goto label_16089;
}
else 
{
 __return_16129 = ret;
goto label_16089;
}
}
else 
{
goto label_15690;
}
}
}
else 
{
goto label_15681;
}
}
else 
{
label_15681:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_15812:; 
ret = 1;
goto label_15701;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_15812;
}
else 
{
goto label_15812;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_16169;
label_16171:; 
 __return_16175 = tmp;
return 1;
}
else 
{
int __CPAchecker_TMP_0 = s__in_handshake;
goto label_12091;
}
}
}
}
