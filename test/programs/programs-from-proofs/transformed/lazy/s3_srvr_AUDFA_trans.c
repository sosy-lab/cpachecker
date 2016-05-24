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
int __return_1661;
int __return_1672;
int __return_1659;
int __return_1657;
int __return_1656;
int __return_1655;
int __return_1654;
int __return_1652;
int __return_1658;
int __return_1653;
int __return_1660;
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
goto label_462;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_462;
}
else 
{
label_462:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_1661 = -1;
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
goto label_516;
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
label_537:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_533;
}
else 
{
goto label_533;
}
}
else 
{
goto label_533;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_543:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1628;
}
else 
{
label_1628:; 
 __return_1659 = ret;
}
tmp = __return_1659;
goto label_1662;
}
else 
{
goto label_537;
}
}
}
else 
{
goto label_533;
}
}
else 
{
label_533:; 
skip = 0;
state = s__state;
if (s__state == 8496)
{
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_543;
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
label_643:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1632;
}
else 
{
label_1632:; 
 __return_1657 = ret;
}
tmp = __return_1657;
goto label_1662;
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
if (s__state == 8512)
{
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_643;
}
else 
{
goto label_717;
}
}
else 
{
skip = 1;
label_717:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_733:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_729;
}
else 
{
goto label_729;
}
}
else 
{
goto label_729;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_739:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1634;
}
else 
{
label_1634:; 
 __return_1656 = ret;
}
tmp = __return_1656;
goto label_1662;
}
else 
{
goto label_733;
}
}
}
else 
{
goto label_729;
}
}
else 
{
label_729:; 
skip = 0;
state = s__state;
if (s__state == 8528)
{
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_803;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_803:; 
if (s__s3__tmp__use_rsa_tmp == 0)
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_843;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_808;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_842:; 
label_843:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_857:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_853;
}
else 
{
goto label_853;
}
}
else 
{
goto label_853;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_863:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1636;
}
else 
{
label_1636:; 
 __return_1655 = ret;
}
tmp = __return_1655;
goto label_1662;
}
else 
{
goto label_857;
}
}
}
else 
{
goto label_853;
}
}
else 
{
label_853:; 
skip = 0;
state = s__state;
if (s__state == 8544)
{
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_956;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_926;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_933:; 
label_956:; 
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_980:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_971;
}
else 
{
goto label_971;
}
}
else 
{
goto label_971;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_990:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1638;
}
else 
{
label_1638:; 
 __return_1654 = ret;
}
tmp = __return_1654;
goto label_1662;
}
else 
{
goto label_980;
}
}
}
else 
{
goto label_971;
}
}
else 
{
label_971:; 
skip = 0;
state = s__state;
if (s__state == 8560)
{
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_990;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_1437;
}
}
else 
{
ret = -1;
goto label_990;
}
}
}
}
else 
{
label_926:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_940;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_933;
}
else 
{
label_940:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_863;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
label_1437:; 
label_1440:; 
label_1442:; 
if (s__s3__tmp__reuse_message == 0)
{
label_1445:; 
if (skip == 0)
{
label_1449:; 
if (s__debug == 0)
{
label_1454:; 
if (cb != 0)
{
label_1467:; 
if (s__state != state)
{
label_1472:; 
new_state = s__state;
label_1476:; 
s__state = state;
label_1478:; 
s__state = new_state;
label_1480:; 
goto label_1446;
}
else 
{
label_1473:; 
goto label_1446;
}
}
else 
{
label_1468:; 
goto label_1446;
}
}
else 
{
label_1455:; 
ret = __VERIFIER_nondet_int();
label_1457:; 
if (ret <= 0)
{
label_1460:; 
label_1464:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1642;
}
else 
{
label_1642:; 
 __return_1652 = ret;
}
tmp = __return_1652;
goto label_1662;
}
else 
{
label_1461:; 
goto label_1454;
}
}
}
else 
{
label_1450:; 
goto label_1446;
}
}
else 
{
label_1446:; 
skip = 0;
label_1483:; 
label_1485:; 
label_1487:; 
label_1489:; 
state = s__state;
label_1491:; 
label_1493:; 
label_1495:; 
label_1497:; 
label_1499:; 
label_1501:; 
label_1503:; 
label_1505:; 
label_1507:; 
label_1509:; 
label_1511:; 
label_1513:; 
label_1515:; 
label_1517:; 
label_1519:; 
label_1521:; 
label_1523:; 
label_1525:; 
label_1527:; 
label_1529:; 
label_1531:; 
label_1533:; 
if (s__state == 8448)
{
label_1536:; 
label_1572:; 
label_1574:; 
num1 = __VERIFIER_nondet_int();
label_1576:; 
if (num1 > 0L)
{
label_1579:; 
s__rwstate = 2;
label_1582:; 
num1 = tmp___8;
label_1584:; 
if (num1 <= 0L)
{
label_1587:; 
ret = -1;
label_1596:; 
goto label_1464;
}
else 
{
label_1588:; 
s__rwstate = 1;
label_1590:; 
goto label_1580;
}
}
else 
{
label_1580:; 
s__state = s__s3__tmp__next_state___0;
label_1593:; 
goto label_1437;
}
}
else 
{
label_1537:; 
label_1539:; 
label_1541:; 
label_1543:; 
label_1545:; 
label_1547:; 
label_1549:; 
label_1551:; 
label_1553:; 
label_1555:; 
label_1557:; 
label_1559:; 
label_1561:; 
label_1563:; 
label_1565:; 
label_1567:; 
ret = -1;
label_1569:; 
goto label_1464;
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
goto label_863;
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
goto label_824;
}
else 
{
tmp___7 = 512;
label_824:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_808;
}
else 
{
skip = 1;
label_840:; 
goto label_842;
}
}
}
}
}
}
else 
{
goto label_808;
}
}
else 
{
label_808:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_739;
}
else 
{
goto label_840;
}
}
}
}
else 
{
ret = -1;
goto label_739;
}
}
}
}
else 
{
ret = -1;
goto label_643;
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
label_632:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_623;
}
else 
{
goto label_623;
}
}
else 
{
goto label_623;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_642:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1630;
}
else 
{
label_1630:; 
 __return_1658 = ret;
}
tmp = __return_1658;
goto label_1662;
}
else 
{
goto label_632;
}
}
}
else 
{
goto label_623;
}
}
else 
{
label_623:; 
skip = 0;
state = s__state;
if (s__state == 8656)
{
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_642;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_642;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_1349:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1640;
}
else 
{
label_1640:; 
 __return_1653 = ret;
}
tmp = __return_1653;
goto label_1662;
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (skip == 0)
{
if (s__debug == 0)
{
label_1360:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1356;
}
else 
{
goto label_1356;
}
}
else 
{
goto label_1356;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_1349;
}
else 
{
goto label_1360;
}
}
}
else 
{
goto label_1356;
}
}
else 
{
label_1356:; 
skip = 0;
state = s__state;
if (s__state == 8672)
{
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_1349;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
s__s3__tmp__next_state___0 = 3;
goto label_1432;
}
else 
{
s__s3__tmp__next_state___0 = 8640;
label_1432:; 
s__init_num = 0;
goto label_1437;
}
}
}
else 
{
ret = -1;
goto label_1349;
}
}
}
}
}
}
else 
{
ret = -1;
goto label_642;
}
}
}
}
}
else 
{
ret = -1;
goto label_543;
}
}
}
}
else 
{
ret = -1;
label_516:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_1626;
}
else 
{
label_1626:; 
 __return_1660 = ret;
}
tmp = __return_1660;
goto label_1662;
}
}
tmp = __return_1661;
label_1662:; 
 __return_1672 = tmp;
return 1;
}
}
}
}
