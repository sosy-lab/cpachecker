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
int __return_5877;
int __return_5874;
int __return_5873;
int __return_5871;
int __return_5870;
int __return_5869;
int __return_5868;
int __return_5867;
int __return_5866;
int __return_5865;
int __return_5864;
int __return_5863;
int __return_5862;
int __return_5860;
int __return_5872;
int __return_5858;
int __return_5855;
int __return_5854;
int __return_5857;
int __return_5853;
int __return_5878;
int __return_5880;
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
goto label_3023;
}
else 
{
if (s__ctx__info_callback != 0)
{
cb = s__ctx__info_callback;
goto label_3023;
}
else 
{
label_3023:; 
int __CPAchecker_TMP_0 = s__in_handshake;
s__in_handshake = s__in_handshake + 1;
__CPAchecker_TMP_0;
if (s__cert == 0)
{
 __return_5877 = -1;
goto label_5878;
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
goto label_5755;
}
else 
{
label_5755:; 
 __return_5874 = ret;
goto label_5878;
}
}
else 
{
got_new_session = 1;
s__state = 8496;
label_3061:; 
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_3071;
}
else 
{
if (s__debug == 0)
{
label_3078:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_3071;
}
else 
{
goto label_3071;
}
}
else 
{
goto label_3071;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_3084:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5759;
}
else 
{
label_5759:; 
 __return_5873 = ret;
goto label_5878;
}
}
else 
{
goto label_3078;
}
}
}
}
else 
{
label_3071:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_hello();
if (ret <= 0)
{
goto label_3084;
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
goto label_3145;
}
else 
{
if (s__debug == 0)
{
label_3160:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_3145;
}
else 
{
goto label_3145;
}
}
else 
{
goto label_3145;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_3175:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5767;
}
else 
{
label_5767:; 
 __return_5871 = ret;
goto label_5878;
}
}
else 
{
goto label_3160;
}
}
}
}
else 
{
label_3145:; 
skip = 0;
state = s__state;
__cil_tmp56 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp56 + 256UL) == 0)
{
ret = ssl3_send_server_certificate();
if (ret <= 0)
{
goto label_3175;
}
else 
{
goto label_3233;
}
}
else 
{
skip = 1;
label_3233:; 
s__state = 8528;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_3248;
}
else 
{
if (s__debug == 0)
{
label_3255:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_3248;
}
else 
{
goto label_3248;
}
}
else 
{
goto label_3248;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_3261:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5771;
}
else 
{
label_5771:; 
 __return_5870 = ret;
goto label_5878;
}
}
else 
{
goto label_3255;
}
}
}
}
else 
{
label_3248:; 
skip = 0;
state = s__state;
l = (unsigned long)s__s3__tmp__new_cipher__algorithms;
__cil_tmp57 = (unsigned long)s__options;
if ((__cil_tmp57 + 2097152UL) == 0)
{
s__s3__tmp__use_rsa_tmp = 0;
goto label_3307;
}
else 
{
s__s3__tmp__use_rsa_tmp = 1;
label_3307:; 
if (!(s__s3__tmp__use_rsa_tmp == 0))
{
label_3314:; 
ret = ssl3_send_server_key_exchange();
if (ret <= 0)
{
goto label_3261;
}
else 
{
goto label_3352;
}
}
else 
{
if ((l + 30UL) == 0)
{
if ((l + 1UL) == 0)
{
skip = 1;
goto label_3355;
}
else 
{
if (s__cert__pkeys__AT0__privatekey == 0)
{
goto label_3314;
}
else 
{
__cil_tmp58 = (unsigned long)s__s3__tmp__new_cipher__algo_strength;
if ((__cil_tmp58 + 2UL) == 0)
{
skip = 1;
label_3354:; 
label_3355:; 
s__state = 8544;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_3369;
}
else 
{
if (s__debug == 0)
{
label_3376:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_3369;
}
else 
{
goto label_3369;
}
}
else 
{
goto label_3369;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_3382:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5775;
}
else 
{
label_5775:; 
 __return_5869 = ret;
goto label_5878;
}
}
else 
{
goto label_3376;
}
}
}
}
else 
{
label_3369:; 
skip = 0;
state = s__state;
if ((s__verify_mode + 1) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_3466;
}
else 
{
if (s__session__peer != 0)
{
if ((s__verify_mode + 4) == 0)
{
goto label_3430;
}
else 
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
label_3439:; 
label_3466:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_3487;
}
else 
{
if (s__debug == 0)
{
label_3502:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_3487;
}
else 
{
goto label_3487;
}
}
else 
{
goto label_3487;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_3512:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5779;
}
else 
{
label_5779:; 
 __return_5868 = ret;
goto label_5878;
}
}
else 
{
goto label_3502;
}
}
}
}
else 
{
label_3487:; 
skip = 0;
state = s__state;
ret = ssl3_send_server_done();
if (ret <= 0)
{
goto label_3512;
}
else 
{
s__s3__tmp__next_state___0 = 8576;
s__state = 8448;
s__init_num = 0;
goto label_4107;
}
}
}
}
else 
{
label_3430:; 
__cil_tmp61 = (unsigned long)s__s3__tmp__new_cipher__algorithms;
if ((__cil_tmp61 + 256UL) == 0)
{
goto label_3446;
}
else 
{
if ((s__verify_mode + 2) == 0)
{
skip = 1;
s__s3__tmp__cert_request = 0;
s__state = 8560;
goto label_3439;
}
else 
{
label_3446:; 
s__s3__tmp__cert_request = 1;
ret = ssl3_send_certificate_request();
if (ret <= 0)
{
goto label_3382;
}
else 
{
s__state = 8448;
s__s3__tmp__next_state___0 = 8576;
s__init_num = 0;
label_4107:; 
label_4110:; 
label_4112:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4116:; 
if (skip == 0)
{
label_4122:; 
if (s__debug == 0)
{
label_4128:; 
if (cb != 0)
{
label_4143:; 
if (s__state != state)
{
label_4149:; 
new_state = s__state;
label_4154:; 
s__state = state;
label_4156:; 
s__state = new_state;
label_4158:; 
goto label_4118;
}
else 
{
label_4151:; 
goto label_4118;
}
}
else 
{
label_4145:; 
goto label_4118;
}
}
else 
{
label_4130:; 
ret = __VERIFIER_nondet_int();
label_4132:; 
if (ret <= 0)
{
label_4135:; 
label_4139:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5783;
}
else 
{
label_5783:; 
 __return_5867 = ret;
goto label_5878;
}
}
else 
{
label_4136:; 
goto label_4128;
}
}
}
else 
{
label_4123:; 
goto label_4118;
}
}
else 
{
label_4118:; 
skip = 0;
label_4162:; 
label_4164:; 
label_4166:; 
label_4168:; 
state = s__state;
label_4171:; 
label_4173:; 
label_4175:; 
label_4177:; 
label_4179:; 
label_4181:; 
label_4183:; 
label_4185:; 
label_4187:; 
label_4189:; 
label_4191:; 
label_4193:; 
label_4195:; 
label_4197:; 
label_4199:; 
label_4201:; 
label_4203:; 
label_4205:; 
label_4207:; 
label_4209:; 
label_4211:; 
label_4213:; 
label_4215:; 
label_4217:; 
label_4219:; 
num1 = __VERIFIER_nondet_int();
label_4221:; 
if (num1 > 0L)
{
label_4224:; 
s__rwstate = 2;
label_4228:; 
num1 = tmp___8;
label_4230:; 
if (num1 <= 0L)
{
label_4233:; 
ret = -1;
label_4245:; 
goto label_4139;
}
else 
{
label_4234:; 
s__rwstate = 1;
label_4237:; 
goto label_4225;
}
}
else 
{
label_4225:; 
s__state = s__s3__tmp__next_state___0;
label_4240:; 
label_4242:; 
label_4248:; 
label_4250:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4254:; 
if (skip == 0)
{
label_4260:; 
if (s__debug == 0)
{
label_4266:; 
if (cb != 0)
{
label_4281:; 
if (s__state != state)
{
label_4287:; 
new_state = s__state;
label_4292:; 
s__state = state;
label_4294:; 
s__state = new_state;
label_4296:; 
goto label_4256;
}
else 
{
label_4289:; 
goto label_4256;
}
}
else 
{
label_4283:; 
goto label_4256;
}
}
else 
{
label_4268:; 
ret = __VERIFIER_nondet_int();
label_4270:; 
if (ret <= 0)
{
label_4273:; 
label_4277:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5787;
}
else 
{
label_5787:; 
 __return_5866 = ret;
goto label_5878;
}
}
else 
{
label_4274:; 
goto label_4266;
}
}
}
else 
{
label_4261:; 
goto label_4256;
}
}
else 
{
label_4256:; 
skip = 0;
label_4300:; 
label_4302:; 
label_4304:; 
label_4306:; 
state = s__state;
label_4309:; 
label_4311:; 
label_4313:; 
label_4315:; 
label_4317:; 
label_4319:; 
label_4321:; 
label_4323:; 
label_4325:; 
label_4327:; 
label_4329:; 
label_4331:; 
label_4333:; 
label_4335:; 
label_4337:; 
label_4339:; 
label_4341:; 
label_4343:; 
label_4345:; 
label_4347:; 
label_4349:; 
label_4351:; 
label_4353:; 
label_4355:; 
label_4357:; 
label_4359:; 
label_4361:; 
ret = ssl3_check_client_hello();
label_4363:; 
if (ret <= 0)
{
label_4366:; 
goto label_4277;
}
else 
{
label_4367:; 
if (ret == 2)
{
label_4371:; 
s__state = 8466;
label_4388:; 
label_4390:; 
label_4392:; 
label_4399:; 
label_4401:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4413:; 
if (skip == 0)
{
label_4419:; 
if (s__debug == 0)
{
label_4437:; 
if (cb != 0)
{
label_4461:; 
if (s__state != state)
{
label_4479:; 
new_state = s__state;
label_4485:; 
s__state = state;
label_4491:; 
s__state = new_state;
label_4493:; 
goto label_4415;
}
else 
{
label_4481:; 
goto label_4415;
}
}
else 
{
label_4463:; 
goto label_4415;
}
}
else 
{
label_4439:; 
ret = __VERIFIER_nondet_int();
label_4441:; 
if (ret <= 0)
{
label_4450:; 
label_4455:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5791;
}
else 
{
label_5791:; 
 __return_5865 = ret;
goto label_5878;
}
}
else 
{
label_4451:; 
goto label_4437;
}
}
}
else 
{
label_4420:; 
goto label_4415;
}
}
else 
{
label_4415:; 
skip = 0;
label_4500:; 
label_5047:; 
label_5049:; 
label_5051:; 
state = s__state;
label_5054:; 
label_5056:; 
label_5058:; 
label_5060:; 
label_5062:; 
label_5064:; 
label_5066:; 
label_5068:; 
label_5070:; 
label_5072:; 
label_5074:; 
label_5076:; 
label_5078:; 
s__shutdown = 0;
label_5081:; 
ret = ssl3_get_client_hello();
label_5083:; 
if (ret <= 0)
{
label_5086:; 
goto label_4455;
}
else 
{
label_5087:; 
got_new_session = 1;
label_5090:; 
s__state = 8496;
goto label_3061;
}
}
}
else 
{
label_4372:; 
ret = ssl3_get_client_certificate();
label_4374:; 
if (ret <= 0)
{
label_4377:; 
goto label_4277;
}
else 
{
label_4378:; 
s__init_num = 0;
label_4381:; 
s__state = 8592;
label_4383:; 
label_4385:; 
label_4394:; 
label_4397:; 
label_4403:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4407:; 
if (skip == 0)
{
label_4424:; 
if (s__debug == 0)
{
label_4431:; 
if (cb != 0)
{
label_4467:; 
if (s__state != state)
{
label_4474:; 
new_state = s__state;
label_4487:; 
s__state = state;
label_4489:; 
s__state = new_state;
label_4495:; 
goto label_4409;
}
else 
{
label_4476:; 
goto label_4409;
}
}
else 
{
label_4469:; 
goto label_4409;
}
}
else 
{
label_4433:; 
ret = __VERIFIER_nondet_int();
label_4443:; 
if (ret <= 0)
{
label_4446:; 
label_4457:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5795;
}
else 
{
label_5795:; 
 __return_5864 = ret;
goto label_5878;
}
}
else 
{
label_4447:; 
goto label_4431;
}
}
}
else 
{
label_4425:; 
goto label_4409;
}
}
else 
{
label_4409:; 
skip = 0;
label_4503:; 
label_4505:; 
label_4507:; 
label_4509:; 
state = s__state;
label_4512:; 
label_4514:; 
label_4516:; 
label_4518:; 
label_4520:; 
label_4522:; 
label_4524:; 
label_4526:; 
label_4528:; 
label_4530:; 
label_4532:; 
label_4534:; 
label_4536:; 
label_4538:; 
label_4540:; 
label_4542:; 
label_4544:; 
label_4546:; 
label_4548:; 
label_4550:; 
label_4552:; 
label_4554:; 
label_4556:; 
label_4558:; 
label_4560:; 
label_4562:; 
label_4564:; 
label_4566:; 
label_4568:; 
ret = ssl3_get_client_key_exchange();
label_4570:; 
if (ret <= 0)
{
label_4573:; 
goto label_4457;
}
else 
{
label_4574:; 
s__state = 8608;
label_4576:; 
s__init_num = 0;
label_4579:; 
label_4581:; 
label_4584:; 
label_4586:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4590:; 
if (skip == 0)
{
label_4596:; 
if (s__debug == 0)
{
label_4602:; 
if (cb != 0)
{
label_4617:; 
if (s__state != state)
{
label_4623:; 
new_state = s__state;
label_4628:; 
s__state = state;
label_4630:; 
s__state = new_state;
label_4632:; 
goto label_4592;
}
else 
{
label_4625:; 
goto label_4592;
}
}
else 
{
label_4619:; 
goto label_4592;
}
}
else 
{
label_4604:; 
ret = __VERIFIER_nondet_int();
label_4606:; 
if (ret <= 0)
{
label_4609:; 
label_4613:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5799;
}
else 
{
label_5799:; 
 __return_5863 = ret;
goto label_5878;
}
}
else 
{
label_4610:; 
goto label_4602;
}
}
}
else 
{
label_4597:; 
goto label_4592;
}
}
else 
{
label_4592:; 
skip = 0;
label_4636:; 
label_4638:; 
label_4640:; 
label_4642:; 
state = s__state;
label_4645:; 
label_4647:; 
label_4649:; 
label_4651:; 
label_4653:; 
label_4655:; 
label_4657:; 
label_4659:; 
label_4661:; 
label_4663:; 
label_4665:; 
label_4667:; 
label_4669:; 
label_4671:; 
label_4673:; 
label_4675:; 
label_4677:; 
label_4679:; 
label_4681:; 
label_4683:; 
label_4685:; 
label_4687:; 
label_4689:; 
label_4691:; 
label_4693:; 
label_4695:; 
label_4697:; 
label_4699:; 
label_4701:; 
label_4703:; 
label_4705:; 
ret = ssl3_get_cert_verify();
label_4707:; 
if (ret <= 0)
{
label_4710:; 
goto label_4613;
}
else 
{
label_4711:; 
s__state = 8640;
label_4713:; 
s__init_num = 0;
label_4716:; 
label_4718:; 
label_4721:; 
label_4723:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4727:; 
if (skip == 0)
{
label_4733:; 
if (s__debug == 0)
{
label_4739:; 
if (cb != 0)
{
label_4754:; 
if (s__state != state)
{
label_4760:; 
new_state = s__state;
label_4765:; 
s__state = state;
label_4767:; 
s__state = new_state;
label_4769:; 
goto label_4729;
}
else 
{
label_4762:; 
goto label_4729;
}
}
else 
{
label_4756:; 
goto label_4729;
}
}
else 
{
label_4741:; 
ret = __VERIFIER_nondet_int();
label_4743:; 
if (ret <= 0)
{
label_4746:; 
label_4750:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5803;
}
else 
{
label_5803:; 
 __return_5862 = ret;
goto label_5878;
}
}
else 
{
label_4747:; 
goto label_4739;
}
}
}
else 
{
label_4734:; 
goto label_4729;
}
}
else 
{
label_4729:; 
skip = 0;
label_4773:; 
label_4775:; 
label_4777:; 
label_4779:; 
state = s__state;
label_4782:; 
label_4784:; 
label_4786:; 
label_4788:; 
label_4790:; 
label_4792:; 
label_4794:; 
label_4796:; 
label_4798:; 
label_4800:; 
label_4802:; 
label_4804:; 
label_4806:; 
label_4808:; 
label_4810:; 
label_4812:; 
label_4814:; 
label_4816:; 
label_4818:; 
label_4820:; 
label_4822:; 
label_4824:; 
label_4826:; 
label_4828:; 
label_4830:; 
label_4832:; 
label_4834:; 
label_4836:; 
label_4838:; 
label_4840:; 
label_4842:; 
label_4844:; 
label_4846:; 
ret = ssl3_get_finished();
label_4848:; 
if (ret <= 0)
{
label_4851:; 
goto label_4750;
}
else 
{
label_4852:; 
if (s__hit == 0)
{
label_4856:; 
s__state = 8656;
label_4864:; 
label_4866:; 
s__init_num = 0;
label_4869:; 
goto label_3135;
}
else 
{
label_4858:; 
s__state = 3;
label_4860:; 
label_4862:; 
s__init_num = 0;
label_4872:; 
label_4874:; 
label_4878:; 
label_4880:; 
if (s__s3__tmp__reuse_message == 0)
{
label_4884:; 
if (skip == 0)
{
label_4890:; 
if (s__debug == 0)
{
label_4896:; 
if (cb != 0)
{
label_4911:; 
if (s__state != state)
{
label_4917:; 
new_state = s__state;
label_4922:; 
s__state = state;
label_4924:; 
s__state = new_state;
label_4926:; 
goto label_4886;
}
else 
{
label_4919:; 
goto label_4886;
}
}
else 
{
label_4913:; 
goto label_4886;
}
}
else 
{
label_4898:; 
ret = __VERIFIER_nondet_int();
label_4900:; 
if (ret <= 0)
{
label_4903:; 
label_4907:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5807;
}
else 
{
label_5807:; 
 __return_5860 = ret;
goto label_5878;
}
}
else 
{
label_4904:; 
goto label_4896;
}
}
}
else 
{
label_4891:; 
goto label_4886;
}
}
else 
{
label_4886:; 
skip = 0;
label_4930:; 
label_4932:; 
label_4934:; 
label_4936:; 
state = s__state;
label_4939:; 
label_4941:; 
label_4943:; 
label_4945:; 
label_4947:; 
label_4949:; 
label_4951:; 
label_4953:; 
label_4955:; 
label_4957:; 
label_4959:; 
label_4961:; 
label_4963:; 
label_4965:; 
label_4967:; 
label_4969:; 
label_4971:; 
label_4973:; 
label_4975:; 
label_4977:; 
label_4979:; 
label_4981:; 
label_4983:; 
label_4985:; 
label_4987:; 
label_4989:; 
label_4991:; 
label_4993:; 
label_4995:; 
label_4997:; 
label_4999:; 
label_5001:; 
label_5003:; 
label_5005:; 
label_5007:; 
label_5009:; 
label_5011:; 
label_5013:; 
s__init_buf___0 = 0;
label_5016:; 
s__init_num = 0;
label_5019:; 
if (got_new_session == 0)
{
label_5023:; 
ret = 1;
label_5044:; 
goto label_4907;
}
else 
{
label_5024:; 
s__new_session = 0;
label_5027:; 
int __CPAchecker_TMP_3 = s__ctx__stats__sess_accept_good;
label_5029:; 
s__ctx__stats__sess_accept_good = s__ctx__stats__sess_accept_good + 1;
label_5031:; 
__CPAchecker_TMP_3;
label_5033:; 
if (cb != 0)
{
label_5037:; 
goto label_5023;
}
else 
{
label_5039:; 
goto label_5023;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
goto label_3333;
}
else 
{
tmp___7 = 512;
label_3333:; 
__cil_tmp60 = tmp___6 * 8;
if (__cil_tmp60 > tmp___7)
{
goto label_3314;
}
else 
{
skip = 1;
label_3352:; 
goto label_3354;
}
}
}
}
}
}
else 
{
goto label_3314;
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
label_3135:; 
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_3149;
}
else 
{
if (s__debug == 0)
{
label_3164:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_3149;
}
else 
{
goto label_3149;
}
}
else 
{
goto label_3149;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_3174:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5763;
}
else 
{
label_5763:; 
 __return_5872 = ret;
goto label_5878;
}
}
else 
{
goto label_3164;
}
}
}
}
else 
{
label_3149:; 
skip = 0;
state = s__state;
s__session__cipher = s__s3__tmp__new_cipher;
tmp___9 = __VERIFIER_nondet_int();
if (tmp___9 == 0)
{
ret = -1;
goto label_3174;
}
else 
{
ret = ssl3_send_change_cipher_spec();
if (ret <= 0)
{
goto label_3174;
}
else 
{
s__state = 8672;
s__init_num = 0;
tmp___10 = __VERIFIER_nondet_int();
if (tmp___10 == 0)
{
ret = -1;
label_5152:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5811;
}
else 
{
label_5811:; 
 __return_5858 = ret;
goto label_5878;
}
}
else 
{
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5162;
}
else 
{
if (s__debug == 0)
{
label_5169:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5162;
}
else 
{
goto label_5162;
}
}
else 
{
goto label_5162;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
goto label_5152;
}
else 
{
goto label_5169;
}
}
}
}
else 
{
label_5162:; 
skip = 0;
state = s__state;
ret = ssl3_send_finished();
if (ret <= 0)
{
goto label_5152;
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
goto label_5258;
}
else 
{
if (s__debug == 0)
{
label_5273:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5258;
}
else 
{
goto label_5258;
}
}
else 
{
goto label_5258;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5288:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5819;
}
else 
{
label_5819:; 
 __return_5855 = ret;
goto label_5878;
}
}
else 
{
goto label_5273;
}
}
}
}
else 
{
label_5258:; 
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
goto label_5288;
}
else 
{
s__rwstate = 1;
goto label_5350;
}
}
else 
{
label_5350:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5369;
}
else 
{
if (s__debug == 0)
{
label_5376:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5369;
}
else 
{
goto label_5369;
}
}
else 
{
goto label_5369;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5382:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5823;
}
else 
{
label_5823:; 
 __return_5854 = ret;
goto label_5878;
}
}
else 
{
goto label_5376;
}
}
}
}
else 
{
label_5369:; 
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
goto label_5446;
}
else 
{
goto label_5446;
}
}
else 
{
label_5446:; 
ret = 1;
goto label_5382;
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
goto label_5262;
}
else 
{
if (s__debug == 0)
{
label_5277:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5262;
}
else 
{
goto label_5262;
}
}
else 
{
goto label_5262;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5287:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5815;
}
else 
{
label_5815:; 
 __return_5857 = ret;
goto label_5878;
}
}
else 
{
goto label_5277;
}
}
}
}
else 
{
label_5262:; 
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
goto label_5287;
}
else 
{
s__rwstate = 1;
goto label_5492;
}
}
else 
{
label_5492:; 
s__state = s__s3__tmp__next_state___0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5511;
}
else 
{
if (s__debug == 0)
{
label_5518:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5511;
}
else 
{
goto label_5511;
}
}
else 
{
goto label_5511;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5524:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5827;
}
else 
{
label_5827:; 
 __return_5853 = ret;
goto label_5878;
}
}
else 
{
goto label_5518;
}
}
}
}
else 
{
label_5511:; 
skip = 0;
state = s__state;
ret = ssl3_get_finished();
if (ret <= 0)
{
goto label_5524;
}
else 
{
if (s__hit == 0)
{
s__state = 8656;
s__init_num = 0;
goto label_3135;
}
else 
{
s__state = 3;
s__init_num = 0;
if (s__s3__tmp__reuse_message == 0)
{
if (!(skip == 0))
{
goto label_5600;
}
else 
{
if (s__debug == 0)
{
label_5607:; 
if (cb != 0)
{
if (s__state != state)
{
new_state = s__state;
s__state = state;
s__state = new_state;
goto label_5600;
}
else 
{
goto label_5600;
}
}
else 
{
goto label_5600;
}
}
else 
{
ret = __VERIFIER_nondet_int();
if (ret <= 0)
{
label_5613:; 
int __CPAchecker_TMP_4 = s__in_handshake;
s__in_handshake = s__in_handshake - 1;
__CPAchecker_TMP_4;
if (cb != 0)
{
goto label_5831;
}
else 
{
label_5831:; 
 __return_5878 = ret;
label_5878:; 
}
tmp = __return_5878;
 __return_5880 = tmp;
return 1;
}
else 
{
goto label_5607;
}
}
}
}
else 
{
label_5600:; 
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
goto label_5677;
}
else 
{
goto label_5677;
}
}
else 
{
label_5677:; 
ret = 1;
goto label_5613;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
