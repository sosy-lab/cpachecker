extern int __VERIFIER_nondet_int();

int main()
{ 
	int s__init_buf___0 = __VERIFIER_nondet_int() ; // -1
	int s__s3__flags = __VERIFIER_nondet_int() ; // -3
	unsigned long tmp = __VERIFIER_nondet_int(); // 5

	int s__state = 12292 ;
	int s__s3__tmp__next_state___0 ;
	int buf ;
	int ret = -1 ;
	int new_state ;
	int state ;
	int skip = 0 ;
	int blastFlag = 0;

	while (1) {

		while_0_continue: /* CIL Label */ ;

		state = s__state;

		if (s__state == 12292) {
			goto switch_1_12292;
		} else {
			if (s__state == 16384) {
				goto switch_1_16384;
			} else {
				if (s__state == 4096) {
					goto switch_1_4096;
				} else {
					if (s__state == 20480) {
						goto switch_1_20480;
					} else {
						if (s__state == 4099) {
							goto switch_1_4099;
						} else {
							if (s__state == 4368) {
								goto switch_1_4368;
							} else {
								if (s__state == 4369) {
									goto switch_1_4369;
								} else {
									if (s__state == 4384) {
										goto switch_1_4384;
									} else {
										if (s__state == 4385) {
											goto switch_1_4385;
										} else {
											if (s__state == 4400) {
												goto switch_1_4400;
											} else {
												if (s__state == 4401) {
													goto switch_1_4401;
												} else {
													if (s__state == 4416) {
														goto switch_1_4416;
													} else {
														if (s__state == 4417) {
															goto switch_1_4417;
														} else {
															if (s__state == 4432) {
																goto switch_1_4432;
															} else {
																if (s__state == 4433) {
																	goto switch_1_4433;
																} else {
																	if (s__state == 4448) {
																		goto switch_1_4448;
																	} else {
																		if (s__state == 4449) {
																			goto switch_1_4449;
																		} else {
																			if (s__state == 4464) {
																				goto switch_1_4464;
																			} else {
																				if (s__state == 4465) {
																					goto switch_1_4465;
																				} else {
																					if (s__state == 4466) {
																						goto switch_1_4466;
																					} else {
																						if (s__state == 4467) {
																							goto switch_1_4467;
																						} else {
																							if (s__state == 4480) {
																								goto switch_1_4480;
																							} else {
																								if (s__state == 4481) {
																									goto switch_1_4481;
																								} else {
																									if (s__state == 4496) {
																										goto switch_1_4496;
																									} else {
																										if (s__state == 4497) {
																											goto switch_1_4497;
																										} else {
																											if (s__state == 4512) {
																												goto switch_1_4512;
																											} else {
																												if (s__state == 4513) {
																													goto switch_1_4513;
																												} else {
																													if (s__state == 4528) {
																														goto switch_1_4528;
																													} else {
																														if (s__state == 4529) {
																															goto switch_1_4529;
																														} else {
																															if (s__state == 4560) {
																																goto switch_1_4560;
																															} else {
																																if (s__state == 4561) {
																																	goto switch_1_4561;
																																} else {
																																	if (s__state == 4352) {
																																		goto switch_1_4352;
																																	} else {
																																		if (s__state == 3) {
																																			goto switch_1_3;
																																		} else {
																																			goto switch_1_default;
																																			if (0) {
																																				switch_1_12292: 
																																				s__state = 4096;
																																				switch_1_16384: ;
																																				switch_1_4096: ;
																																				switch_1_20480: ;
																																				switch_1_4099: 
																																				if (s__init_buf___0 == 0) {
																																					buf = __VERIFIER_nondet_int();
																																					if (buf == 0) {
																																						ret = -1;
																																						goto end;
																																					}
																																					s__init_buf___0 = buf;
																																				}
																																				s__state = 4368;
																																				goto switch_1_break;
																																				switch_1_4368: ;
																																				switch_1_4369: 
																																				ret = __VERIFIER_nondet_int();
																																				if (blastFlag == 0) {
																																					blastFlag = 1;
																																				}
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4384;

																																				goto switch_1_break;
																																				switch_1_4384: ;
																																				switch_1_4385: 
																																				ret = __VERIFIER_nondet_int();
																																				if (blastFlag == 1) {
																																					blastFlag = 2;
																																				}
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4560;
																																				goto switch_1_break;
																																				switch_1_4400: ;
																																				switch_1_4401: ;
																																				skip = 1;
																																				s__state = 4416;
																																				goto switch_1_break;
																																				switch_1_4416: ;
																																				switch_1_4417: 
																																				ret = __VERIFIER_nondet_int();
																																				if (blastFlag == 3) {
																																					blastFlag = 4;
																																				}
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4432;
																																				goto switch_1_break;
																																				switch_1_4432: ;
																																				switch_1_4433: 
																																				ret = __VERIFIER_nondet_int();
																																				if (blastFlag == 4) {
																																					goto ERROR;
																																				}
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4448;
																																				goto switch_1_break;
																																				switch_1_4448: ;
																																				switch_1_4449: 
																																				ret = __VERIFIER_nondet_int();
																																				if (blastFlag == 4) {
																																					blastFlag = 5;
																																				}
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				if (2) {
																																					s__state = 4464;
																																				} else {
																																					s__state = 4480;
																																				}
																																				goto switch_1_break;
																																				switch_1_4464: ;
																																				switch_1_4465: ;
																																				switch_1_4466: ;
																																				switch_1_4467: 
																																				ret = __VERIFIER_nondet_int();
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4480;
																																				goto switch_1_break;
																																				switch_1_4480: ;
																																				switch_1_4481: 
																																				ret = __VERIFIER_nondet_int();
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4512;
																																				goto switch_1_break;
																																				switch_1_4496: ;
																																				switch_1_4497: 
																																				ret = __VERIFIER_nondet_int();
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4512;
																																				goto switch_1_break;
																																				switch_1_4512: ;
																																				switch_1_4513: 
																																				ret = __VERIFIER_nondet_int();
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4528;
																																				goto switch_1_break;
																																				switch_1_4528: ;
																																				switch_1_4529: 
																																				ret = __VERIFIER_nondet_int();
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4352;
																																				s__s3__flags = s__s3__flags + 5;
																																				s__s3__tmp__next_state___0 = 3;
																																				if (s__s3__flags != 2L) {
																																					s__state = 3;
																																					s__s3__flags = s__s3__flags + 4L;
																																				}
																																				goto switch_1_break;
																																				switch_1_4560: ;
																																				switch_1_4561: 
																																				ret = __VERIFIER_nondet_int();
																																				if (ret <= 0) {
																																					goto end;
																																				}
																																				s__state = 4512;
																																				goto switch_1_break;
																																				switch_1_4352:
																																				s__state = s__s3__tmp__next_state___0;
																																				goto switch_1_break;
																																				switch_1_3: 
																																				if (s__init_buf___0 != 0) {
																																					s__init_buf___0 = 0;
																																				}
																																				ret = 1;

																																				goto end;
																																				switch_1_default: 
																																				ret = -1;
																																				goto end;
																																			} else {
																																				switch_1_break: ;
																																			}
																																		}
																																	}
																																}
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		skip = 0;
	}

	while_0_break: /* CIL Label */ ;

	end: 
	return (ret);

	ERROR: 
	return (-1);
}


