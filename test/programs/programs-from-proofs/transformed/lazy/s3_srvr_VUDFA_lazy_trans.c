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
int __return_10485;
int __return_10482;
int __return_10481;
int __return_10479;
int __return_10478;
int __return_10474;
int __return_10476;
int __return_10473;
int __return_10466;
int __return_10465;
int __return_10464;
int __return_10463;
int __return_10462;
int __return_10486;
int __return_10488;
int __return_10480;
int __return_10477;
int __return_10470;
int __return_10469;
int __return_10472;
int __return_10468;
int __return_10467;
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
goto label_524;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_524;
}
else 
{
label_524:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_10485 = -1;
goto label_10486;
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
goto label_10364;
}
else 
{
label_10364:; 
 __return_10482 = ret;
goto label_10486;
}
}
else 
{
got_new_session = 1;
s__state = 8496;
label_1414:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_1423;
}
else 
{
if (s__debug == 0)
{
label_1430:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1423;
}
else 
{
goto label_1423;
}
}
else 
{
goto label_1423;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1436:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10368;
}
else 
{
label_10368:; 
 __return_10481 = ret;
goto label_10486;
}
}
else 
{
goto label_1430;
}
}
}
}
else 
{
label_1423:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_1436;
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
goto label_1497;
}
else 
{
if (s__debug == 0)
{
label_1512:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1497;
}
else 
{
goto label_1497;
}
}
else 
{
goto label_1497;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1527:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10376;
}
else 
{
label_10376:; 
 __return_10479 = ret;
goto label_10486;
}
}
else 
{
goto label_1512;
}
}
}
}
else 
{
label_1497:; 
skip = 0;
state = s__state;
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_1527;
}
else 
{
goto label_1585;
}
}
else 
{
skip = 1;
label_1585:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_1600;
}
else 
{
if (s__debug == 0)
{
label_1607:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1600;
}
else 
{
goto label_1600;
}
}
else 
{
goto label_1600;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1613:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10380;
}
else 
{
label_10380:; 
 __return_10478 = ret;
goto label_10486;
}
}
else 
{
goto label_1607;
}
}
}
}
else 
{
label_1600:; 
skip = 0;
state = s__state;
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_1659;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_1659:; 
if (!(s__s3__tmp__use_rsa_tmp == 0))
{
label_1666:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_1613;
}
else 
{
goto label_1704;
}
}
else 
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_1707;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_1666;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_1706:; 
label_1707:; 
s__state = 8544;
s__init_num = 0;
label_5915:; 
label_5917:; 
label_6132:; 
label_6138:; 
if (s__s3__tmp__reuse_message == 0)
{
label_6147:; 
if (skip == 0)
{
label_6164:; 
if (s__debug == 0)
{
label_6179:; 
if (cb != 0)
{
label_6220:; 
if (s__state != state)
{
label_6235:; 
new_state = s__state;
label_6249:; 
s__state = state;
label_6253:; 
s__state = new_state;
label_6259:; 
goto label_6149;
}
else 
{
label_6237:; 
goto label_6149;
}
}
else 
{
label_6222:; 
goto label_6149;
}
}
else 
{
label_6181:; 
ret = __VERIFIER_nondet_int();
label_6191:; 
if (ret <= 0)
{
label_6197:; 
label_6209:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10392;
}
else 
{
label_10392:; 
 __return_10474 = ret;
goto label_10486;
}
}
else 
{
label_6198:; 
goto label_6179;
}
}
}
else 
{
label_6165:; 
goto label_6149;
}
}
else 
{
label_6149:; 
skip = 0;
label_6269:; 
label_9311:; 
label_9313:; 
label_9315:; 
state = s__state;
label_9318:; 
label_9320:; 
label_9322:; 
label_9324:; 
label_9326:; 
label_9328:; 
label_9330:; 
label_9332:; 
label_9334:; 
label_9336:; 
label_9338:; 
label_9340:; 
label_9342:; 
label_9344:; 
label_9346:; 
label_9348:; 
label_9350:; 
label_9352:; 
label_9354:; 
label_9356:; 
label_9358:; 
label_9360:; 
if ((s__verify_mode + 1) == 0)
{
label_9363:; 
skip = 1;
label_9418:; 
s__s3__tmp__cert_request = 0;
label_9421:; 
s__state = 8560;
label_5781:; 
goto label_5769;
}
else 
{
label_9364:; 
if (s__session__peer != 0)
{
label_9368:; 
if ((s__verify_mode + 4) == 0)
{
label_9375:; 
goto label_9372;
}
else 
{
label_9376:; 
skip = 1;
label_9379:; 
s__s3__tmp__cert_request = 0;
label_9382:; 
s__state = 8560;
label_5713:; 
label_5716:; 
label_5718:; 
label_5769:; 
label_5785:; 
label_6134:; 
label_6136:; 
if (s__s3__tmp__reuse_message == 0)
{
label_6153:; 
if (skip == 0)
{
label_6159:; 
if (s__debug == 0)
{
label_6185:; 
if (cb != 0)
{
label_6214:; 
if (s__state != state)
{
label_6240:; 
new_state = s__state;
label_6247:; 
s__state = state;
label_6255:; 
s__state = new_state;
label_6257:; 
goto label_6155;
}
else 
{
label_6242:; 
goto label_6155;
}
}
else 
{
label_6216:; 
goto label_6155;
}
}
else 
{
label_6187:; 
ret = __VERIFIER_nondet_int();
label_6189:; 
if (ret <= 0)
{
label_6201:; 
label_6207:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10388;
}
else 
{
label_10388:; 
 __return_10476 = ret;
goto label_10486;
}
}
else 
{
label_6202:; 
goto label_6185;
}
}
}
else 
{
label_6160:; 
goto label_6155;
}
}
else 
{
label_6155:; 
skip = 0;
label_6266:; 
label_9424:; 
label_9426:; 
label_9428:; 
state = s__state;
label_9431:; 
label_9433:; 
label_9435:; 
label_9437:; 
label_9439:; 
label_9441:; 
label_9443:; 
label_9445:; 
label_9447:; 
label_9449:; 
label_9451:; 
label_9453:; 
label_9455:; 
label_9457:; 
label_9459:; 
label_9461:; 
label_9463:; 
label_9465:; 
label_9467:; 
label_9469:; 
label_9471:; 
label_9473:; 
label_9475:; 
label_9477:; 
ret = ssl3_send_server_done();
label_9479:; 
if (ret <= 0)
{
label_9482:; 
goto label_6207;
}
else 
{
label_9483:; 
s__s3__tmp__next_state___0 = 8576;
label_10299:; 
s__state = 8448;
label_6651:; 
s__init_num = 0;
label_6655:; 
goto label_6657;
}
}
}
}
else 
{
label_9370:; 
label_9372:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
label_9386:; 
if ((__cil_tmp61 + 256UL) == 0)
{
label_9389:; 
goto label_9396;
}
else 
{
label_9390:; 
if ((s__verify_mode + 2) == 0)
{
label_9393:; 
skip = 1;
label_9399:; 
s__s3__tmp__cert_request = 0;
label_9402:; 
s__state = 8560;
label_5742:; 
label_5745:; 
goto label_5718;
}
else 
{
label_9394:; 
label_9396:; 
s__s3__tmp__cert_request = 1;
label_9407:; 
ret = ssl3_send_certificate_request();
label_9409:; 
if (ret <= 0)
{
label_9412:; 
goto label_6209;
}
else 
{
label_9413:; 
s__state = 8448;
label_5760:; 
s__s3__tmp__next_state___0 = 8576;
label_7998:; 
s__init_num = 0;
label_8001:; 
label_8003:; 
label_8005:; 
label_6657:; 
label_7076:; 
label_7078:; 
if (s__s3__tmp__reuse_message == 0)
{
label_7097:; 
if (skip == 0)
{
label_7103:; 
if (s__debug == 0)
{
label_7133:; 
if (cb != 0)
{
label_7163:; 
if (s__state != state)
{
label_7193:; 
new_state = s__state;
label_7201:; 
s__state = state;
label_7209:; 
s__state = new_state;
label_7211:; 
goto label_7099;
}
else 
{
label_7195:; 
goto label_7099;
}
}
else 
{
label_7165:; 
goto label_7099;
}
}
else 
{
label_7135:; 
ret = __VERIFIER_nondet_int();
label_7137:; 
if (ret <= 0)
{
label_7149:; 
label_7156:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10396;
}
else 
{
label_10396:; 
 __return_10473 = ret;
goto label_10486;
}
}
else 
{
label_7150:; 
goto label_7133;
}
}
}
else 
{
label_7104:; 
goto label_7099;
}
}
else 
{
label_7099:; 
skip = 0;
label_7221:; 
label_8381:; 
label_8383:; 
label_8385:; 
state = s__state;
label_8388:; 
label_8390:; 
label_8392:; 
label_8394:; 
label_8396:; 
label_8398:; 
label_8400:; 
label_8402:; 
label_8404:; 
label_8406:; 
label_8408:; 
label_8410:; 
label_8412:; 
label_8414:; 
label_8416:; 
label_8418:; 
label_8420:; 
label_8422:; 
label_8424:; 
label_8426:; 
label_8428:; 
label_8430:; 
label_8432:; 
label_8434:; 
label_8436:; 
num1 = __VERIFIER_nondet_int();
label_8438:; 
if (num1 > 0L)
{
label_8441:; 
s__rwstate = 2;
label_8445:; 
num1 = tmp___8;
label_8447:; 
if (num1 <= 0L)
{
label_8450:; 
ret = -1;
label_8462:; 
goto label_7156;
}
else 
{
label_8451:; 
s__rwstate = 1;
label_8454:; 
goto label_8442;
}
}
else 
{
label_8442:; 
s__state = s__s3__tmp__next_state___0;
label_8457:; 
label_8459:; 
label_8465:; 
label_8467:; 
if (s__s3__tmp__reuse_message == 0)
{
label_8471:; 
if (skip == 0)
{
label_8477:; 
if (s__debug == 0)
{
label_8483:; 
if (cb != 0)
{
label_8498:; 
if (s__state != state)
{
label_8504:; 
new_state = s__state;
label_8509:; 
s__state = state;
label_8511:; 
s__state = new_state;
label_8513:; 
goto label_8473;
}
else 
{
label_8506:; 
goto label_8473;
}
}
else 
{
label_8500:; 
goto label_8473;
}
}
else 
{
label_8485:; 
ret = __VERIFIER_nondet_int();
label_8487:; 
if (ret <= 0)
{
label_8490:; 
label_8494:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10420;
}
else 
{
label_10420:; 
 __return_10466 = ret;
goto label_10486;
}
}
else 
{
label_8491:; 
goto label_8483;
}
}
}
else 
{
label_8478:; 
goto label_8473;
}
}
else 
{
label_8473:; 
skip = 0;
label_8517:; 
label_8519:; 
label_8521:; 
label_8523:; 
state = s__state;
label_8526:; 
label_8528:; 
label_8530:; 
label_8532:; 
label_8534:; 
label_8536:; 
label_8538:; 
label_8540:; 
label_8542:; 
label_8544:; 
label_8546:; 
label_8548:; 
label_8550:; 
label_8552:; 
label_8554:; 
label_8556:; 
label_8558:; 
label_8560:; 
label_8562:; 
label_8564:; 
label_8566:; 
label_8568:; 
label_8570:; 
label_8572:; 
label_8574:; 
label_8576:; 
label_8578:; 
ret = ssl3_check_client_hello();
label_8580:; 
if (ret <= 0)
{
label_8583:; 
goto label_8494;
}
else 
{
label_8584:; 
if (ret == 2)
{
label_8588:; 
s__state = 8466;
label_8605:; 
label_8607:; 
label_8609:; 
label_8616:; 
label_8618:; 
if (s__s3__tmp__reuse_message == 0)
{
label_8630:; 
if (skip == 0)
{
label_8636:; 
if (s__debug == 0)
{
label_8654:; 
if (cb != 0)
{
label_8678:; 
if (s__state != state)
{
label_8696:; 
new_state = s__state;
label_8702:; 
s__state = state;
label_8708:; 
s__state = new_state;
label_8710:; 
goto label_8632;
}
else 
{
label_8698:; 
goto label_8632;
}
}
else 
{
label_8680:; 
goto label_8632;
}
}
else 
{
label_8656:; 
ret = __VERIFIER_nondet_int();
label_8658:; 
if (ret <= 0)
{
label_8667:; 
label_8672:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10424;
}
else 
{
label_10424:; 
 __return_10465 = ret;
goto label_10486;
}
}
else 
{
label_8668:; 
goto label_8654;
}
}
}
else 
{
label_8637:; 
goto label_8632;
}
}
else 
{
label_8632:; 
skip = 0;
label_8717:; 
label_9264:; 
label_9266:; 
label_9268:; 
state = s__state;
label_9271:; 
label_9273:; 
label_9275:; 
label_9277:; 
label_9279:; 
label_9281:; 
label_9283:; 
label_9285:; 
label_9287:; 
label_9289:; 
label_9291:; 
label_9293:; 
label_9295:; 
s__shutdown = 0;
label_9298:; 
ret = ssl3_get_client_hello();
label_9300:; 
if (ret <= 0)
{
label_9303:; 
goto label_8672;
}
else 
{
label_9304:; 
got_new_session = 1;
label_9307:; 
s__state = 8496;
goto label_1414;
}
}
}
else 
{
label_8589:; 
ret = ssl3_get_client_certificate();
label_8591:; 
if (ret <= 0)
{
label_8594:; 
goto label_8494;
}
else 
{
label_8595:; 
s__init_num = 0;
label_8598:; 
s__state = 8592;
label_8600:; 
label_8602:; 
label_8611:; 
label_8614:; 
label_8620:; 
if (s__s3__tmp__reuse_message == 0)
{
label_8624:; 
if (skip == 0)
{
label_8641:; 
if (s__debug == 0)
{
label_8648:; 
if (cb != 0)
{
label_8684:; 
if (s__state != state)
{
label_8691:; 
new_state = s__state;
label_8704:; 
s__state = state;
label_8706:; 
s__state = new_state;
label_8712:; 
goto label_8626;
}
else 
{
label_8693:; 
goto label_8626;
}
}
else 
{
label_8686:; 
goto label_8626;
}
}
else 
{
label_8650:; 
ret = __VERIFIER_nondet_int();
label_8660:; 
if (ret <= 0)
{
label_8663:; 
label_8674:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10428;
}
else 
{
label_10428:; 
 __return_10464 = ret;
goto label_10486;
}
}
else 
{
label_8664:; 
goto label_8648;
}
}
}
else 
{
label_8642:; 
goto label_8626;
}
}
else 
{
label_8626:; 
skip = 0;
label_8720:; 
label_8722:; 
label_8724:; 
label_8726:; 
state = s__state;
label_8729:; 
label_8731:; 
label_8733:; 
label_8735:; 
label_8737:; 
label_8739:; 
label_8741:; 
label_8743:; 
label_8745:; 
label_8747:; 
label_8749:; 
label_8751:; 
label_8753:; 
label_8755:; 
label_8757:; 
label_8759:; 
label_8761:; 
label_8763:; 
label_8765:; 
label_8767:; 
label_8769:; 
label_8771:; 
label_8773:; 
label_8775:; 
label_8777:; 
label_8779:; 
label_8781:; 
label_8783:; 
label_8785:; 
ret = ssl3_get_client_key_exchange();
label_8787:; 
if (ret <= 0)
{
label_8790:; 
goto label_8674;
}
else 
{
label_8791:; 
s__state = 8608;
label_8793:; 
s__init_num = 0;
label_8796:; 
label_8798:; 
label_8801:; 
label_8803:; 
if (s__s3__tmp__reuse_message == 0)
{
label_8807:; 
if (skip == 0)
{
label_8813:; 
if (s__debug == 0)
{
label_8819:; 
if (cb != 0)
{
label_8834:; 
if (s__state != state)
{
label_8840:; 
new_state = s__state;
label_8845:; 
s__state = state;
label_8847:; 
s__state = new_state;
label_8849:; 
goto label_8809;
}
else 
{
label_8842:; 
goto label_8809;
}
}
else 
{
label_8836:; 
goto label_8809;
}
}
else 
{
label_8821:; 
ret = __VERIFIER_nondet_int();
label_8823:; 
if (ret <= 0)
{
label_8826:; 
label_8830:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10432;
}
else 
{
label_10432:; 
 __return_10463 = ret;
goto label_10486;
}
}
else 
{
label_8827:; 
goto label_8819;
}
}
}
else 
{
label_8814:; 
goto label_8809;
}
}
else 
{
label_8809:; 
skip = 0;
label_8853:; 
label_8855:; 
label_8857:; 
label_8859:; 
state = s__state;
label_8862:; 
label_8864:; 
label_8866:; 
label_8868:; 
label_8870:; 
label_8872:; 
label_8874:; 
label_8876:; 
label_8878:; 
label_8880:; 
label_8882:; 
label_8884:; 
label_8886:; 
label_8888:; 
label_8890:; 
label_8892:; 
label_8894:; 
label_8896:; 
label_8898:; 
label_8900:; 
label_8902:; 
label_8904:; 
label_8906:; 
label_8908:; 
label_8910:; 
label_8912:; 
label_8914:; 
label_8916:; 
label_8918:; 
label_8920:; 
label_8922:; 
ret = ssl3_get_cert_verify();
label_8924:; 
if (ret <= 0)
{
label_8927:; 
goto label_8830;
}
else 
{
label_8928:; 
s__state = 8640;
label_8930:; 
s__init_num = 0;
label_8933:; 
label_8935:; 
label_8938:; 
label_8940:; 
if (s__s3__tmp__reuse_message == 0)
{
label_8944:; 
if (skip == 0)
{
label_8950:; 
if (s__debug == 0)
{
label_8956:; 
if (cb != 0)
{
label_8971:; 
if (s__state != state)
{
label_8977:; 
new_state = s__state;
label_8982:; 
s__state = state;
label_8984:; 
s__state = new_state;
label_8986:; 
goto label_8946;
}
else 
{
label_8979:; 
goto label_8946;
}
}
else 
{
label_8973:; 
goto label_8946;
}
}
else 
{
label_8958:; 
ret = __VERIFIER_nondet_int();
label_8960:; 
if (ret <= 0)
{
label_8963:; 
label_8967:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10436;
}
else 
{
label_10436:; 
 __return_10462 = ret;
goto label_10486;
}
}
else 
{
label_8964:; 
goto label_8956;
}
}
}
else 
{
label_8951:; 
goto label_8946;
}
}
else 
{
label_8946:; 
skip = 0;
label_8990:; 
label_8992:; 
label_8994:; 
label_8996:; 
state = s__state;
label_8999:; 
label_9001:; 
label_9003:; 
label_9005:; 
label_9007:; 
label_9009:; 
label_9011:; 
label_9013:; 
label_9015:; 
label_9017:; 
label_9019:; 
label_9021:; 
label_9023:; 
label_9025:; 
label_9027:; 
label_9029:; 
label_9031:; 
label_9033:; 
label_9035:; 
label_9037:; 
label_9039:; 
label_9041:; 
label_9043:; 
label_9045:; 
label_9047:; 
label_9049:; 
label_9051:; 
label_9053:; 
label_9055:; 
label_9057:; 
label_9059:; 
label_9061:; 
label_9063:; 
ret = ssl3_get_finished();
label_9065:; 
if (ret <= 0)
{
label_9068:; 
goto label_8967;
}
else 
{
label_9069:; 
if (s__hit == 0)
{
label_9073:; 
s__state = 8656;
label_9081:; 
label_9083:; 
s__init_num = 0;
label_9086:; 
goto label_1487;
}
else 
{
label_9075:; 
s__state = 3;
label_9077:; 
label_9079:; 
s__init_num = 0;
label_9089:; 
label_9091:; 
label_9095:; 
label_9097:; 
if (s__s3__tmp__reuse_message == 0)
{
label_9101:; 
if (skip == 0)
{
label_9107:; 
if (s__debug == 0)
{
label_9113:; 
if (cb != 0)
{
label_9128:; 
if (s__state != state)
{
label_9134:; 
new_state = s__state;
label_9139:; 
s__state = state;
label_9141:; 
s__state = new_state;
label_9143:; 
goto label_9103;
}
else 
{
label_9136:; 
goto label_9103;
}
}
else 
{
label_9130:; 
goto label_9103;
}
}
else 
{
label_9115:; 
ret = __VERIFIER_nondet_int();
label_9117:; 
if (ret <= 0)
{
label_9120:; 
label_9124:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10440;
}
else 
{
label_10440:; 
 __return_10486 = ret;
label_10486:; 
}
tmp = __return_10486;
 __return_10488 = tmp;
return 1;
}
else 
{
label_9121:; 
goto label_9113;
}
}
}
else 
{
label_9108:; 
goto label_9103;
}
}
else 
{
label_9103:; 
skip = 0;
label_9147:; 
label_9149:; 
label_9151:; 
label_9153:; 
state = s__state;
label_9156:; 
label_9158:; 
label_9160:; 
label_9162:; 
label_9164:; 
label_9166:; 
label_9168:; 
label_9170:; 
label_9172:; 
label_9174:; 
label_9176:; 
label_9178:; 
label_9180:; 
label_9182:; 
label_9184:; 
label_9186:; 
label_9188:; 
label_9190:; 
label_9192:; 
label_9194:; 
label_9196:; 
label_9198:; 
label_9200:; 
label_9202:; 
label_9204:; 
label_9206:; 
label_9208:; 
label_9210:; 
label_9212:; 
label_9214:; 
label_9216:; 
label_9218:; 
label_9220:; 
label_9222:; 
label_9224:; 
label_9226:; 
label_9228:; 
label_9230:; 
s__init_buf___0 = 0;
label_9233:; 
s__init_num = 0;
label_9236:; 
if (got_new_session == 0)
{
label_9240:; 
ret = 1;
label_9261:; 
goto label_9124;
}
else 
{
label_9241:; 
s__new_session = 0;
label_9244:; 
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
label_9246:; 
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
label_9248:; 
__CPAchecker_TMP_3;
label_9250:; 
if (cb != 0)
{
label_9254:; 
goto label_9240;
}
else 
{
label_9256:; 
goto label_9240;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
tmp___6 = __VERIFIER_nondet_int();
__cil_tmp59 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp59 + 4UL) == 0)
{
tmp___7 = 1024;
goto label_1685;
}
else 
{
tmp___7 = 512;
label_1685:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_1666;
}
else 
{
skip = 1;
label_1704:; 
goto label_1706;
}
}
}
}
}
}
else 
{
goto label_1666;
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
label_1487:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_1501;
}
else 
{
if (s__debug == 0)
{
label_1516:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_1501;
}
else 
{
goto label_1501;
}
}
else 
{
goto label_1501;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_1526:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10372;
}
else 
{
label_10372:; 
 __return_10480 = ret;
goto label_10486;
}
}
else 
{
goto label_1516;
}
}
}
}
else 
{
label_1501:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_1526;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_1526;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_5182:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10384;
}
else 
{
label_10384:; 
 __return_10477 = ret;
goto label_10486;
}
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5192;
}
else 
{
if (s__debug == 0)
{
label_5199:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5192;
}
else 
{
goto label_5192;
}
}
else 
{
goto label_5192;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_5182;
}
else 
{
goto label_5199;
}
}
}
}
else 
{
label_5192:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_5182;
}
else 
{
s__state = 8448;
if (s__hit == 0)
{
label_6468:; 
s__s3__tmp__next_state___0 = 3;
label_6475:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7089;
}
else 
{
if (s__debug == 0)
{
label_7124:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7089;
}
else 
{
goto label_7089;
}
}
else 
{
goto label_7089;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7158:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10404;
}
else 
{
label_10404:; 
 __return_10470 = ret;
goto label_10486;
}
}
else 
{
goto label_7124;
}
}
}
}
else 
{
label_7089:; 
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
goto label_7158;
}
else 
{
s__rwstate = 1;
goto label_8038;
}
}
else 
{
label_8038:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8057;
}
else 
{
if (s__debug == 0)
{
label_8064:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8057;
}
else 
{
goto label_8057;
}
}
else 
{
goto label_8057;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8070:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10408;
}
else 
{
label_10408:; 
 __return_10469 = ret;
goto label_10486;
}
}
else 
{
goto label_8064;
}
}
}
}
else 
{
label_8057:; 
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
goto label_8134;
}
else 
{
goto label_8134;
}
}
else 
{
label_8134:; 
ret = 1;
goto label_8070;
}
}
}
}
}
else 
{
label_6470:; 
s__s3__tmp__next_state___0 = 8640;
label_6472:; 
label_6474:; 
s__init_num = 0;
label_6480:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_7093;
}
else 
{
if (s__debug == 0)
{
label_7128:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_7093;
}
else 
{
goto label_7093;
}
}
else 
{
goto label_7093;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_7157:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10400;
}
else 
{
label_10400:; 
 __return_10472 = ret;
goto label_10486;
}
}
else 
{
goto label_7128;
}
}
}
}
else 
{
label_7093:; 
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
goto label_7157;
}
else 
{
s__rwstate = 1;
goto label_8180;
}
}
else 
{
label_8180:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8199;
}
else 
{
if (s__debug == 0)
{
label_8206:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8199;
}
else 
{
goto label_8199;
}
}
else 
{
goto label_8199;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8212:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10412;
}
else 
{
label_10412:; 
 __return_10468 = ret;
goto label_10486;
}
}
else 
{
goto label_8206;
}
}
}
}
else 
{
label_8199:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_8212;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_1487;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_8288;
}
else 
{
if (s__debug == 0)
{
label_8295:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_8288;
}
else 
{
goto label_8288;
}
}
else 
{
goto label_8288;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_8301:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_10416;
}
else 
{
label_10416:; 
 __return_10467 = ret;
goto label_10486;
}
}
else 
{
goto label_8295;
}
}
}
}
else 
{
label_8288:; 
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
goto label_8365;
}
else 
{
goto label_8365;
}
}
else 
{
label_8365:; 
ret = 1;
goto label_8301;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
