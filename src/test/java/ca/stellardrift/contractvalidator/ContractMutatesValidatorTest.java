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

public class ContractMutatesValidatorTest extends AbstractTest {
  @Test
  void testMutatesCannotReferenceThisWhenStatic() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class Test {\n"
      + "  @Contract(mutates=\"this\")\n"
      + "  public static void test(final int x) {}\n"
      + "}")).getMessage().contains("'this' cannot be mutated from a static context"));
  }

  @Test
  void testMutatesMustIncludeParameterIndexWhenMultiplePresent() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestMutatesParam {\n"
      + "  @Contract(mutates=\"param\")\n"
      + "  public static void test(final int x, final double y) {}\n"
      + "}")).getMessage().contains("An unqualified 'param' was used"));
  }

  @Test
  void testMutatesFailsWhenParameterOutOfBounds() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestMutatesParam {\n"
      + "  @Contract(mutates=\"param3\")\n"
      + "  public static void test(final int x, final double y) {}\n"
      + "}")).getMessage().contains("Method 'test' was declared to mutate parameter 3, but only 2 parameters exist"));
  }

  @Test
  void testMutatesFailsWhenParameterZero() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestMutatesParam {\n"
      + "  @Contract(mutates=\"param0\")\n"
      + "  public static void test(final int x, final double y) {}\n"
      + "}")).getMessage().contains("Mutates clause referenced parameter 0"));
  }

  @Test
  void testParseErrorsPropogated() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestMutatesParam {\n"
      + "  @Contract(mutates=\"paramHi\")\n"
      + "  public static void test(final int x, final double y) {}\n"
      + "}")).getMessage().contains("token recognition error"));
  }

  @Test
  void testRepeatedWarns() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestMutatesParam {\n"
      + "  @Contract(mutates=\"this,param1,this\")\n"
      + "  public static void test(final int x, final double y) {}\n" // unfortunately due to jOOR limitations, we can only capture output when a full error occurs
      + "}")).getMessage().contains("specifier 'this' seen multiple times"));
  }

  @Test
  void testValidCompilesSuccessfully() {
    assertDoesNotThrow(() -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestMutatesParam {\n"
      + "  @Contract(mutates=\"this,param1\")\n"
      + "  public void test(final int x, final double y) {}\n" // unfortunately due to jOOR limitations, we can only capture output when a full error occurs
      + "}"));
  }

}
