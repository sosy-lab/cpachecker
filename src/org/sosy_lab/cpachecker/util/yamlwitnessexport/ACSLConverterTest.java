package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.SMGInvariant;

public class ACSLConverterTest {

  @Test
  public void testPointerValidityConversion() {
    SMGInvariant inv = new SMGInvariant(SMGInvariant.InvariantType.POINTER_VALIDITY, SMGInvariant.Property.MEMORY_SAFETY, "ptr", null, 0, null);
    ACSLConverter converter = new ACSLConverter();
    String expression = converter.convertToACSL(inv);
    assertThat(expression).isEqualTo("\\valid(ptr)");
  }

  @Test
  public void testAllocationStatusConversion() {
    SMGInvariant inv = new SMGInvariant(SMGInvariant.InvariantType.ALLOCATION_STATUS, SMGInvariant.Property.MEMORY_SAFETY, "ptr", null, 0, null);
    ACSLConverter converter = new ACSLConverter();
    String expression = converter.convertToACSL(inv);
    assertThat(expression).isEqualTo("\\allocated(ptr)");
  }

  @Test
  public void testBufferBoundsConversion() {
    SMGInvariant inv = new SMGInvariant(SMGInvariant.InvariantType.BUFFER_BOUNDS, SMGInvariant.Property.MEMORY_SAFETY, "buf", null, 32, null);
    ACSLConverter converter = new ACSLConverter();
    String expression = converter.convertToACSL(inv);
    assertThat(expression).isEqualTo("\\valid(buf+(0..31))");
  }

  @Test
  public void testTemporalSafetyConversion() {
    SMGInvariant inv = new SMGInvariant(SMGInvariant.InvariantType.TEMPORAL_SAFETY, SMGInvariant.Property.MEMORY_SAFETY, "ptr", "use_after_free", 0, "old_state");
    ACSLConverter converter = new ACSLConverter();
    String expression = converter.convertToACSL(inv);
    assertThat(expression).contains("\\at(use_after_free, old_state)");
  }

  @Test
  public void testUnknownInvariantTypeHandled() {
    SMGInvariant inv = new SMGInvariant(null, SMGInvariant.Property.MEMORY_SAFETY, "ptr", null, 0, null);
    ACSLConverter converter = new ACSLConverter();
    String expression = converter.convertToACSL(inv);
    assertThat(expression).isNotNull();
  }
}
