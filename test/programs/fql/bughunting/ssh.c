extern int __VERIFIER_nondet_int();

int main()
{ 
	int s__init_buf___0 = __VERIFIER_nondet_int() ; // -3
	int s__s3__flags = __VERIFIER_nondet_int() ; // -3
	
	int s__state = 4096 ;
	int buf ;
	int ret = -1 ;
	
	while (1) {
		
		if (s__state == 4096) {
			goto switch_1_4096;
		} else {
			if (s__state == 20480) {
				goto switch_1_20480;
			} else {
				if (s__state == 4368) {
					goto switch_1_4368;
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
																										goto end;
																										
																										switch_1_4096: ;
																										switch_1_20480: ;
																										if (s__init_buf___0 == 0) {
																											buf = __VERIFIER_nondet_int();
																											if (buf == 0) {
																												goto end;
																											}
																											s__init_buf___0 = buf;
																										}
																										switch_1_4368: ;
																										goto switch_1_4560;
																										
																										switch_1_4401: ;
																										switch_1_4416: ;
																										switch_1_4417: ;
																										switch_1_4432: ;
																										switch_1_4433: ;
																										switch_1_4448: ;
																										switch_1_4449: ;
																										switch_1_4464: ;
																										switch_1_4465: ;
																										switch_1_4466: ;
																										switch_1_4467: ;
																										switch_1_4480: ;
																										switch_1_4481: ;
																										s__state = 4512;
																										goto switch_1_break;
																										
																										switch_1_4496: ;
																										switch_1_4497: ;
																										switch_1_4512: ;
																										switch_1_4513: ;
																										ret = __VERIFIER_nondet_int();
																										if (ret <= 0) {
																											goto end;
																										}
																										switch_1_4528: ;
																										switch_1_4529: ;
																										s__state = 4352;
																										if (s__s3__flags != -3) {
																											goto end;
																										}
																										goto switch_1_break;
																										
																										switch_1_4560: ;
																										switch_1_4561: ;
																										s__state = 4512;
																										goto switch_1_break;
																										
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
	
	end: 
	return (ret);
}


