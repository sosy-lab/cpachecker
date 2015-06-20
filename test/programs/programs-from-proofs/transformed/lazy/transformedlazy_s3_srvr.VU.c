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
int __return_20452=0;
int __return_20458=0;
int __return_20368=0;
int __return_20450=0;
int __return_20370=0;
int __return_20448=0;
int __return_20374=0;
int __return_20444=0;
int __return_20376=0;
int __return_20442=0;
int __return_20378=0;
int __return_20440=0;
int __return_20380=0;
int __return_20438=0;
int __return_20382=0;
int __return_20436=0;
int __return_20384=0;
int __return_20434=0;
int __return_20386=0;
int __return_20432=0;
int __return_20388=0;
int __return_20430=0;
int __return_20390=0;
int __return_20428=0;
int __return_20392=0;
int __return_20426=0;
int __return_20394=0;
int __return_20424=0;
int __return_20396=0;
int __return_20422=0;
int __return_20398=0;
int __return_20420=0;
int __return_20400=0;
int __return_20418=0;
int __return_20372=0;
int __return_20446=0;
int __return_20402=0;
int __return_20416=0;
int __return_20404=0;
int __return_20414=0;
int __return_20406=0;
int __return_20412=0;
int __return_20408=0;
int __return_20410=0;
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
goto label_11158;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
label_11158:; 
int __CPAchecker_TMP_0 = s__in_handshake;
label_11164:; 
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_20452 = -1;
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
 __return_20368 = ret;
label_20368:; 
}
else 
{
 __return_20450 = ret;
goto label_20368;
}
tmp = __return_20368;
goto label_20454;
}
else 
{
got_new_session = 1;
s__state = 8496;
label_15856:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_15877:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15868;
}
else 
{
goto label_15868;
}
}
else 
{
goto label_15868;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_15888:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20370 = ret;
goto label_20368;
}
else 
{
 __return_20448 = ret;
goto label_20368;
}
}
else 
{
goto label_15877;
}
}
}
else 
{
goto label_15868;
}
}
else 
{
label_15868:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_15888;
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
label_16007:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15988;
}
else 
{
goto label_15988;
}
}
else 
{
goto label_15988;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16032:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20374 = ret;
goto label_20368;
}
else 
{
 __return_20444 = ret;
goto label_20368;
}
}
else 
{
goto label_16007;
}
}
}
else 
{
goto label_15988;
}
}
else 
{
label_15988:; 
skip = 0;
state = s__state;
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_16032;
}
else 
{
goto label_16128;
}
}
else 
{
skip = 1;
label_16128:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_16161:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16152;
}
else 
{
goto label_16152;
}
}
else 
{
goto label_16152;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16172:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20376 = ret;
goto label_20368;
}
else 
{
 __return_20442 = ret;
goto label_20368;
}
}
else 
{
goto label_16161;
}
}
}
else 
{
goto label_16152;
}
}
else 
{
label_16152:; 
skip = 0;
state = s__state;
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_16252;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_16252:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_16330;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_16262;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_16314:; 
goto label_16330;
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_16294;
}
else 
{
tmp___7 = 512;
label_16294:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_16262;
}
else 
{
skip = 1;
goto label_16314;
}
}
}
}
}
}
else 
{
goto label_16262;
}
}
else 
{
label_16262:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_16172;
}
else 
{
label_16330:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_16361:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16352;
}
else 
{
goto label_16352;
}
}
else 
{
goto label_16352;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16372:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20378 = ret;
goto label_20368;
}
else 
{
 __return_20440 = ret;
goto label_20368;
}
}
else 
{
goto label_16361;
}
}
}
else 
{
goto label_16352;
}
}
else 
{
label_16352:; 
skip = 0;
state = s__state;
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_16514;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_16454;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_16468:; 
label_16514:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_16563:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16544;
}
else 
{
goto label_16544;
}
}
else 
{
goto label_16544;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16582:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20380 = ret;
goto label_20368;
}
else 
{
 __return_20438 = ret;
goto label_20368;
}
}
else 
{
goto label_16563;
}
}
}
else 
{
goto label_16544;
}
}
else 
{
label_16544:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_16582;
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
label_18041:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18032;
}
else 
{
goto label_18032;
}
}
else 
{
goto label_18032;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18052:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20122:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20382 = ret;
goto label_20368;
}
else 
{
 __return_20436 = ret;
goto label_20368;
}
}
else 
{
goto label_18041;
}
}
}
else 
{
goto label_18032;
}
}
else 
{
label_18032:; 
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
goto label_18052;
}
else 
{
s__rwstate = 1;
goto label_18136;
}
}
else 
{
label_18136:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_18173:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18164;
}
else 
{
goto label_18164;
}
}
else 
{
goto label_18164;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18184:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20120:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20384 = ret;
goto label_20368;
}
else 
{
 __return_20434 = ret;
goto label_20368;
}
}
else 
{
goto label_18173;
}
}
}
else 
{
goto label_18164;
}
}
else 
{
label_18164:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_18184;
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
label_18335:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18316;
}
else 
{
goto label_18316;
}
}
else 
{
goto label_18316;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18354:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20118:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20386 = ret;
goto label_20368;
}
else 
{
 __return_20432 = ret;
goto label_20368;
}
}
else 
{
goto label_18335;
}
}
}
else 
{
goto label_18316;
}
}
else 
{
label_18316:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_18354;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_15856;
}
}
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_18184;
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
label_18331:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18312;
}
else 
{
goto label_18312;
}
}
else 
{
goto label_18312;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18356:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20116:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20388 = ret;
goto label_20368;
}
else 
{
 __return_20430 = ret;
goto label_20368;
}
}
else 
{
goto label_18331;
}
}
}
else 
{
goto label_18312;
}
}
else 
{
label_18312:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_18356;
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
label_18495:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18486;
}
else 
{
goto label_18486;
}
}
else 
{
goto label_18486;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18506:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20114:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20390 = ret;
goto label_20368;
}
else 
{
 __return_20428 = ret;
goto label_20368;
}
}
else 
{
goto label_18495;
}
}
}
else 
{
goto label_18486;
}
}
else 
{
label_18486:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_18506;
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
label_18627:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18618;
}
else 
{
goto label_18618;
}
}
else 
{
goto label_18618;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18638:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20112:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20392 = ret;
goto label_20368;
}
else 
{
 __return_20426 = ret;
goto label_20368;
}
}
else 
{
goto label_18627;
}
}
}
else 
{
goto label_18618;
}
}
else 
{
label_18618:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_18638;
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
label_18767:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18758;
}
else 
{
goto label_18758;
}
}
else 
{
goto label_18758;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_18778:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20110:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20394 = ret;
goto label_20368;
}
else 
{
 __return_20424 = ret;
goto label_20368;
}
}
else 
{
goto label_18767;
}
}
}
else 
{
goto label_18758;
}
}
else 
{
label_18758:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_18778;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_18778;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_18906:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20108:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20396 = ret;
goto label_20368;
}
else 
{
 __return_20422 = ret;
goto label_20368;
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
label_18929:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_18920;
}
else 
{
goto label_18920;
}
}
else 
{
goto label_18920;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_18906;
}
else 
{
goto label_18929;
}
}
}
else 
{
goto label_18920;
}
}
else 
{
label_18920:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_18906;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_19079:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19070;
}
else 
{
goto label_19070;
}
}
else 
{
goto label_19070;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_19090:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20106:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20398 = ret;
goto label_20368;
}
else 
{
 __return_20420 = ret;
goto label_20368;
}
}
else 
{
goto label_19079;
}
}
}
else 
{
goto label_19070;
}
}
else 
{
label_19070:; 
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
goto label_19090;
}
else 
{
s__rwstate = 1;
goto label_19174;
}
}
else 
{
label_19174:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_19211:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19202;
}
else 
{
goto label_19202;
}
}
else 
{
goto label_19202;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_19222:; 
int __CPAchecker_TMP_4 = s__in_handshake;
label_20104:; 
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20400 = ret;
goto label_20368;
}
else 
{
 __return_20418 = ret;
goto label_20368;
}
}
else 
{
goto label_19211;
}
}
}
else 
{
goto label_19202;
}
}
else 
{
label_19202:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_19333:; 
ret = 1;
goto label_19222;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_19333;
}
else 
{
goto label_19333;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_16454:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_16482;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_16468;
}
else 
{
label_16482:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_16372;
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
label_16559:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16540;
}
else 
{
goto label_16540;
}
}
else 
{
goto label_16540;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16584:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20122;
}
else 
{
goto label_16559;
}
}
}
else 
{
goto label_16540;
}
}
else 
{
label_16540:; 
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
goto label_16584;
}
else 
{
s__rwstate = 1;
goto label_16690;
}
}
else 
{
label_16690:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_16727:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16718;
}
else 
{
goto label_16718;
}
}
else 
{
goto label_16718;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16738:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20120;
}
else 
{
goto label_16727;
}
}
}
else 
{
goto label_16718;
}
}
else 
{
label_16718:; 
skip = 0;
state = s__state;
ret = ssl3_check_client_hello();
if (ret <= 0)
{
goto label_16738;
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
label_16889:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16870;
}
else 
{
goto label_16870;
}
}
else 
{
goto label_16870;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16908:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20118;
}
else 
{
goto label_16889;
}
}
}
else 
{
goto label_16870;
}
}
else 
{
label_16870:; 
skip = 0;
state = s__state;
s__shutdown = 0;
ret = ssl3_get_client_hello();
if (ret <= 0)
{
goto label_16908;
}
else 
{
got_new_session = 1;
s__state = 8496;
goto label_15856;
}
}
}
else 
{
ret = ssl3_get_client_certificate();
if (ret <= 0)
{
goto label_16738;
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
label_16885:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_16866;
}
else 
{
goto label_16866;
}
}
else 
{
goto label_16866;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16910:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20116;
}
else 
{
goto label_16885;
}
}
}
else 
{
goto label_16866;
}
}
else 
{
label_16866:; 
skip = 0;
state = s__state;
ret = ssl3_get_client_key_exchange();
if (ret <= 0)
{
goto label_16910;
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
label_17049:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17040;
}
else 
{
goto label_17040;
}
}
else 
{
goto label_17040;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_17060:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20114;
}
else 
{
goto label_17049;
}
}
}
else 
{
goto label_17040;
}
}
else 
{
label_17040:; 
skip = 0;
state = s__state;
ret = ssl3_get_cert_verify();
if (ret <= 0)
{
goto label_17060;
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
label_17181:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17172;
}
else 
{
goto label_17172;
}
}
else 
{
goto label_17172;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_17192:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20112;
}
else 
{
goto label_17181;
}
}
}
else 
{
goto label_17172;
}
}
else 
{
label_17172:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_17192;
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
label_17321:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17312;
}
else 
{
goto label_17312;
}
}
else 
{
goto label_17312;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_17332:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20110;
}
else 
{
goto label_17321;
}
}
}
else 
{
goto label_17312;
}
}
else 
{
label_17312:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_17332;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_17332;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_17460:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20108;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_17483:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17474;
}
else 
{
goto label_17474;
}
}
else 
{
goto label_17474;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_17460;
}
else 
{
goto label_17483;
}
}
}
else 
{
goto label_17474;
}
}
else 
{
label_17474:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_17460;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_17633:; 
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
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_17644:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20106;
}
else 
{
goto label_17633;
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
num1 = __VERIFIER_nondet_int();
if (num1 > 0L)
{
s__rwstate = 2;
num1 = tmp___8;
if (num1 <= 0L)
{
ret = -1;
goto label_17644;
}
else 
{
s__rwstate = 1;
goto label_17728;
}
}
else 
{
label_17728:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_17765:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_17756;
}
else 
{
goto label_17756;
}
}
else 
{
goto label_17756;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_17776:; 
int __CPAchecker_TMP_4 = s__in_handshake;
goto label_20104;
}
else 
{
goto label_17765;
}
}
}
else 
{
goto label_17756;
}
}
else 
{
label_17756:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_17887:; 
ret = 1;
goto label_17776;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_17887;
}
else 
{
goto label_17887;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_16011:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_15992;
}
else 
{
goto label_15992;
}
}
else 
{
goto label_15992;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_16030:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20372 = ret;
goto label_20368;
}
else 
{
 __return_20446 = ret;
goto label_20368;
}
}
else 
{
goto label_16011;
}
}
}
else 
{
goto label_15992;
}
}
else 
{
label_15992:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_16030;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_16030;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_19506:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20402 = ret;
goto label_20368;
}
else 
{
 __return_20416 = ret;
goto label_20368;
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
label_19529:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19520;
}
else 
{
goto label_19520;
}
}
else 
{
goto label_19520;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_19506;
}
else 
{
goto label_19529;
}
}
}
else 
{
goto label_19520;
}
}
else 
{
label_19520:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_19506;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8640;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_19679:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19670;
}
else 
{
goto label_19670;
}
}
else 
{
goto label_19670;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_19690:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20404 = ret;
goto label_20368;
}
else 
{
 __return_20414 = ret;
goto label_20368;
}
}
else 
{
goto label_19679;
}
}
}
else 
{
goto label_19670;
}
}
else 
{
label_19670:; 
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
goto label_19690;
}
else 
{
s__rwstate = 1;
goto label_19774;
}
}
else 
{
label_19774:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_19811:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19802;
}
else 
{
goto label_19802;
}
}
else 
{
goto label_19802;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_19822:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20406 = ret;
goto label_20368;
}
else 
{
 __return_20412 = ret;
goto label_20368;
}
}
else 
{
goto label_19811;
}
}
}
else 
{
goto label_19802;
}
}
else 
{
label_19802:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_19822;
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
label_19951:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_19942;
}
else 
{
goto label_19942;
}
}
else 
{
goto label_19942;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_19962:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
 __return_20408 = ret;
goto label_20368;
}
else 
{
 __return_20410 = ret;
goto label_20368;
}
}
else 
{
goto label_19951;
}
}
}
else 
{
goto label_19942;
}
}
else 
{
label_19942:; 
skip = 0;
state = s__state;
s__init_buf___0 = 0;
s__init_num = 0;
if (got_new_session == 0)
{
label_20073:; 
ret = 1;
goto label_19962;
}
else 
{
s__new_session = 0;
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
__CPAchecker_TMP_3;
if (cb != 0)
{
goto label_20073;
}
else 
{
goto label_20073;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
tmp = __return_20452;
label_20454:; 
 __return_20458 = tmp;
return 1;
}
else 
{
int __CPAchecker_TMP_0 = s__in_handshake;
goto label_11164;
}
}
}
}
