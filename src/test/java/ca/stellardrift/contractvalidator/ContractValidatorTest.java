/*
 * Copyright (c) 2021 zml and contributors
 * This file is part of contract-validator.
 *
 * contract-validator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * contract-validator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with contract-validator.  If not, see <https://www.gnu.org/licenses/>.
 */
package ca.stellardrift.contractvalidator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joor.ReflectException;
import org.junit.jupiter.api.Test;

public class ContractValidatorTest extends AbstractTest {

  @Test
  void testValidContract() {
    assertDoesNotThrow(() -> this.compile(""
      + "import org.jetbrains.annotations.Contract;\n"
      + "class Test {\n"
      + "  @Contract(\"null -> null; !null -> _\")\n"
      + "  public static String toStrOrNull(final Object x) { return x == null ? null : x.toString(); }\n"
      + "}"));
  }

  @Test
  void testNullFailsOnPrimitives() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class Test {\n"
      + "  @Contract(\"null -> fail\")\n"
      + "  public void test(final int x) {}\n"
      + "}"))
      .getMessage().contains("Constraint 'null' is only applicable to non-primitive types"));
  }

  // todo: finish the cases here

}
