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
int __return_1796;
int __return_1794;
int __return_1819;
int __return_1793;
int __return_1792;
int __return_1818;
int __return_1790;
int __return_1789;
int __return_1787;
int __return_1783;
int __return_1782;
int __return_1788;
int __return_1817;
int __return_1786;
int __return_1785;
int __return_1784;
int __return_1791;
int __return_1780;
int __return_1781;
int __return_1816;
int __return_1779;
int __return_1815;
int __return_1778;
int __return_1795;
int __return_1820;
int main()
{
int s ;
int tmp ;
s = 8464;
{
int __tmp_1 = s;
int initial_state = __tmp_1;
int s__info_callback = __VERIFIER_nondet_int();
int s__in_handshake = __VERIFIER_nondet_int();
int s__state ;
int s__new_session ;
int s__server ;
int s__version = __VERIFIER_nondet_int();
int s__type ;
int s__init_num ;
int s__hit = __VERIFIER_nondet_int();
int s__rwstate ;
int s__init_buf___0 ;
int s__debug = __VERIFIER_nondet_int();
int s__shutdown ;
int s__cert = __VERIFIER_nondet_int();
int s__options = __VERIFIER_nondet_int();
int s__verify_mode = __VERIFIER_nondet_int();
int s__session__peer = __VERIFIER_nondet_int();
int s__cert__pkeys__AT0__privatekey = __VERIFIER_nondet_int();
int s__ctx__info_callback = __VERIFIER_nondet_int();
int s__ctx__stats__sess_accept_renegotiate = __VERIFIER_nondet_int();
int s__ctx__stats__sess_accept = __VERIFIER_nondet_int();
int s__ctx__stats__sess_accept_good = __VERIFIER_nondet_int();
int s__s3__tmp__cert_request ;
int s__s3__tmp__reuse_message = __VERIFIER_nondet_int();
int s__s3__tmp__use_rsa_tmp ;
int s__s3__tmp__new_cipher = __VERIFIER_nondet_int();
int s__s3__tmp__new_cipher__algorithms = __VERIFIER_nondet_int();
int s__s3__tmp__next_state___0 ;
int s__s3__tmp__new_cipher__algo_strength = __VERIFIER_nondet_int();
int s__session__cipher ;
int buf = __VERIFIER_nondet_int();
unsigned long l ;
unsigned long Time ;
unsigned long tmp = __VERIFIER_nondet_long();
int cb ;
long num1 ;
int ret ;
int new_state ;
int state ;
int skip ;
int got_new_session ;
int tmp___1 = __VERIFIER_nondet_int();
int tmp___2 = __VERIFIER_nondet_int();
int tmp___3 ;
int tmp___4 ;
int tmp___5 ;
int tmp___6 ;
int tmp___7 ;
long tmp___8 = __VERIFIER_nondet_long();
int tmp___9 ;
int tmp___10 ;
int __cil_tmp55 ;
unsigned long __cil_tmp56 ;
unsigned long __cil_tmp57 ;
unsigned long __cil_tmp58 ;
unsigned long __cil_tmp59 ;
int __cil_tmp60 ;
unsigned long __cil_tmp61 ;
int __CPAchecker_TMP_0;
int __CPAchecker_TMP_4;
int __CPAchecker_TMP_3;
int __CPAchecker_TMP_2;
int __CPAchecker_TMP_1;
s__info_callback = __VERIFIER_nondet_int();
s__in_handshake = __VERIFIER_nondet_int();
s__version = __VERIFIER_nondet_int();
s__hit = __VERIFIER_nondet_int();
s__debug = __VERIFIER_nondet_int();
s__cert = __VERIFIER_nondet_int();
s__options = __VERIFIER_nondet_int();
s__verify_mode = __VERIFIER_nondet_int();
s__session__peer = __VERIFIER_nondet_int();
s__cert__pkeys__AT0__privatekey = __VERIFIER_nondet_int();
s__ctx__info_callback = __VERIFIER_nondet_int();
s__ctx__stats__sess_accept_renegotiate = __VERIFIER_nondet_int();
s__ctx__stats__sess_accept = __VERIFIER_nondet_int();
s__ctx__stats__sess_accept_good = __VERIFIER_nondet_int();
s__s3__tmp__reuse_message = __VERIFIER_nondet_int();
s__s3__tmp__new_cipher = __VERIFIER_nondet_int();
s__s3__tmp__new_cipher__algorithms = __VERIFIER_nondet_int();
s__s3__tmp__new_cipher__algo_strength = __VERIFIER_nondet_int();
buf = __VERIFIER_nondet_int();
tmp = __VERIFIER_nondet_long();
tmp___1 = __VERIFIER_nondet_int();
tmp___2 = __VERIFIER_nondet_int();
tmp___8 = __VERIFIER_nondet_long();
s__state = initial_state;
Time = tmp;
cb = 0;
ret = -1;
skip = 0;
got_new_session = 0;
if (s__info_callback != 0)
{
cb = s__info_callback;
goto label_489;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_489;
}
else 
{
label_489:; 
__CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_1796 = -1;
goto label_1795;
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
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1727;
}
else 
{
label_1727:; 
 __return_1794 = ret;
}
tmp = __return_1794;
label_1798:; 
 __return_1819 = tmp;
return 1;
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
label_564:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_560;
}
else 
{
goto label_560;
}
}
else 
{
goto label_560;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_570:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1729;
}
else 
{
label_1729:; 
 __return_1793 = ret;
}
tmp = __return_1793;
goto label_1798;
}
else 
{
goto label_564;
}
}
}
else 
{
goto label_560;
}
}
else 
{
label_560:; 
skip = 0;
state = s__state;
if (s__state == 8496)
{
ret = ssl3_send_server_hello();
if (ret <= 0)
{
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1731;
}
else 
{
label_1731:; 
 __return_1792 = ret;
}
tmp = __return_1792;
label_1800:; 
 __return_1818 = tmp;
return 1;
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
label_657:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_648;
}
else 
{
goto label_648;
}
}
else 
{
goto label_648;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_670:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1735;
}
else 
{
label_1735:; 
 __return_1790 = ret;
}
tmp = __return_1790;
goto label_1800;
}
else 
{
goto label_657;
}
}
}
else 
{
goto label_648;
}
}
else 
{
label_648:; 
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
goto label_670;
}
else 
{
goto label_744;
}
}
else 
{
skip = 1;
label_744:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_760:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_756;
}
else 
{
goto label_756;
}
}
else 
{
goto label_756;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_766:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1737;
}
else 
{
label_1737:; 
 __return_1789 = ret;
}
tmp = __return_1789;
goto label_1800;
}
else 
{
goto label_760;
}
}
}
else 
{
goto label_756;
}
}
else 
{
label_756:; 
skip = 0;
state = s__state;
if (s__state == 8528)
{
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_830;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_830:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_870;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_835;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_861:; 
label_870:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_896:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_887;
}
else 
{
goto label_887;
}
}
else 
{
goto label_887;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_906:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1741;
}
else 
{
label_1741:; 
 __return_1787 = ret;
}
tmp = __return_1787;
goto label_1800;
}
else 
{
goto label_896;
}
}
}
else 
{
goto label_887;
}
}
else 
{
label_887:; 
skip = 0;
state = s__state;
if (s__state == 8544)
{
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_1264;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_1234;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_1241:; 
label_1264:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1288:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1279;
}
else 
{
goto label_1279;
}
}
else 
{
goto label_1279;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1298:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1749;
}
else 
{
label_1749:; 
 __return_1783 = ret;
}
tmp = __return_1783;
goto label_1800;
}
else 
{
goto label_1288;
}
}
}
else 
{
goto label_1279;
}
}
else 
{
label_1279:; 
skip = 0;
state = s__state;
if (s__state == 8560)
{
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_1298;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_1271;
}
}
else 
{
ret = -1;
goto label_1298;
}
}
}
}
else 
{
label_1234:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_1248;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_1241;
}
else 
{
label_1248:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_906;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
label_1271:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1286:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1277;
}
else 
{
goto label_1277;
}
}
else 
{
goto label_1277;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1299:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1751;
}
else 
{
label_1751:; 
 __return_1782 = ret;
}
tmp = __return_1782;
goto label_1800;
}
else 
{
goto label_1286;
}
}
}
else 
{
goto label_1277;
}
}
else 
{
label_1277:; 
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
goto label_1299;
}
else 
{
s__rwstate = 1;
goto label_1370;
}
}
else 
{
label_1370:; 
s__state = s__s3__tmp__next_state___0;
goto label_1271;
}
}
else 
{
ret = -1;
goto label_1299;
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
goto label_906;
}
}
}
else 
{
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_851;
}
else 
{
tmp___7 = 512;
label_851:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_835;
}
else 
{
skip = 1;
goto label_861;
}
}
}
}
}
}
else 
{
goto label_835;
}
}
else 
{
label_835:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1739;
}
else 
{
label_1739:; 
 __return_1788 = ret;
}
tmp = __return_1788;
label_1804:; 
 __return_1817 = tmp;
return 1;
}
else 
{
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_894:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_885;
}
else 
{
goto label_885;
}
}
else 
{
goto label_885;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_907:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1743;
}
else 
{
label_1743:; 
 __return_1786 = ret;
}
tmp = __return_1786;
goto label_1804;
}
else 
{
goto label_894;
}
}
}
else 
{
goto label_885;
}
}
else 
{
label_885:; 
skip = 0;
state = s__state;
if (s__state == 8544)
{
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_1011;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_981;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_988:; 
label_1011:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1035:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1026;
}
else 
{
goto label_1026;
}
}
else 
{
goto label_1026;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1045:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1745;
}
else 
{
label_1745:; 
 __return_1785 = ret;
}
tmp = __return_1785;
goto label_1804;
}
else 
{
goto label_1035;
}
}
}
else 
{
goto label_1026;
}
}
else 
{
label_1026:; 
skip = 0;
state = s__state;
if (s__state == 8560)
{
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_1045;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_1018;
}
}
else 
{
ret = -1;
goto label_1045;
}
}
}
}
else 
{
label_981:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_995;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_988;
}
else 
{
label_995:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_907;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
label_1018:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1033:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1024;
}
else 
{
goto label_1024;
}
}
else 
{
goto label_1024;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1046:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1747;
}
else 
{
label_1747:; 
 __return_1784 = ret;
}
tmp = __return_1784;
goto label_1804;
}
else 
{
goto label_1033;
}
}
}
else 
{
goto label_1024;
}
}
else 
{
label_1024:; 
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
goto label_1046;
}
else 
{
s__rwstate = 1;
goto label_1117;
}
}
else 
{
label_1117:; 
s__state = s__s3__tmp__next_state___0;
goto label_1018;
}
}
else 
{
ret = -1;
goto label_1046;
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
goto label_907;
}
}
}
}
}
}
else 
{
ret = -1;
goto label_766;
}
}
}
}
else 
{
ret = -1;
goto label_670;
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
label_659:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_650;
}
else 
{
goto label_650;
}
}
else 
{
goto label_650;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_669:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1733;
}
else 
{
label_1733:; 
 __return_1791 = ret;
}
tmp = __return_1791;
goto label_1800;
}
else 
{
goto label_659;
}
}
}
else 
{
goto label_650;
}
}
else 
{
label_650:; 
skip = 0;
state = s__state;
if (s__state == 8656)
{
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_669;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1755;
}
else 
{
label_1755:; 
 __return_1780 = ret;
}
tmp = __return_1780;
goto label_1811;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_1497:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1753;
}
else 
{
label_1753:; 
 __return_1781 = ret;
}
tmp = __return_1781;
label_1811:; 
 __return_1816 = tmp;
return 1;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1508:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1504;
}
else 
{
goto label_1504;
}
}
else 
{
goto label_1504;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_1497;
}
else 
{
goto label_1508;
}
}
}
else 
{
goto label_1504;
}
}
else 
{
label_1504:; 
skip = 0;
state = s__state;
if (s__state == 8672)
{
ret = ssl3_send_finished();
if (ret <= 0)
{
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1757;
}
else 
{
label_1757:; 
 __return_1779 = ret;
}
tmp = __return_1779;
label_1813:; 
 __return_1815 = tmp;
return 1;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_1580;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_1580:; 
s__init_num = 0;
label_1584:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1593:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1589;
}
else 
{
goto label_1589;
}
}
else 
{
goto label_1589;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1599:; 
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1759;
}
else 
{
label_1759:; 
 __return_1778 = ret;
}
tmp = __return_1778;
goto label_1813;
}
else 
{
goto label_1593;
}
}
}
else 
{
goto label_1589;
}
}
else 
{
label_1589:; 
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
goto label_1599;
}
else 
{
s__rwstate = 1;
goto label_1659;
}
}
else 
{
label_1659:; 
s__state = s__s3__tmp__next_state___0;
goto label_1584;
}
}
else 
{
ret = -1;
goto label_1599;
}
}
}
}
}
else 
{
ret = -1;
goto label_1497;
}
}
}
}
}
}
else 
{
ret = -1;
goto label_669;
}
}
}
}
}
else 
{
ret = -1;
goto label_570;
}
}
}
}
else 
{
ret = -1;
__CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1725;
}
else 
{
label_1725:; 
 __return_1795 = ret;
label_1795:; 
}
tmp = __return_1795;
 __return_1820 = tmp;
return 1;
}
}
}
}
}
}
