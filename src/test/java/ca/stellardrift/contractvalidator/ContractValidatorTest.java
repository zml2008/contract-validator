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
      + "class TestValid {\n"
      + "  @Contract(\"null -> null; !null -> _\")\n"
      + "  public static String toStrOrNull(final Object x) { return x == null ? null : x.toString(); }\n"
      + "}"));
  }

  @Test
  void testNullFailsOnPrimitives() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestNullFails {\n"
      + "  @Contract(\"null -> fail\")\n"
      + "  public void test(final int x) {}\n"
      + "}"))
      .getMessage().contains("Constraint 'null' is only applicable to non-primitive types"));
  }

  @Test
  void testNoArgMethodsPermitted() {
    assertDoesNotThrow(() -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestNoArg {\n"
      + "  @Contract(\"-> new\")\n"
      + "  public Object test() { return new Object(); }\n"
      + "}"));
  }

  @Test
  void testTooManyParametersFails() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestTooManyParameters {\n"
      + "  @Contract(\"_, !null -> param2 \")\n"
      + "  public String test(final int x) { return \"\"; }\n"
      + "}")).getMessage().contains("Clause specified 2 parameter(s), but method 'test' had only 1 parameter(s)"));

  }

  @Test
  void testNotEnoughParametersFails() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestNotEnoughParameters {\n"
      + "  @Contract(\"_ -> param2 \")\n"
      + "  public String test(final int x, final String y) { return \"\"; }\n"
      + "}")).getMessage().contains("Clause only specified 1 parameter(s), but method 'test' had 2 parameter(s)"));
  }

  @Test
  void testBooleanValueRequiresBooleanParameter() {
    final String message1 = assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestBooleanRequiresBoolean {\n"
      + "  @Contract(\"true -> !null \")\n"
      + "  public String test(final int x) { return \"\"; }\n"
      + "}")).getMessage();
    assertTrue(message1.contains("Constraint 'true' is only applicable to boolean values, but it was used to refer to a 'int'"));

    // These can't be tested together, that fails when compiling with javac 8 (but not with newer versions!).
    final String message2 = assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestBooleanRequiresBoolean {\n"
      + "  @Contract(\"false -> !null \")\n"
      + "  public String test(final double x) { return \"\"; }\n"
      + "}")).getMessage();
    assertTrue(message2.contains("Constraint 'false' is only applicable to boolean values, but it was used to refer to a 'double'"));
  }

  @Test
  void testReferencesParam0() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestReferencesParam0 {\n"
      + "  @Contract(\"_ -> param0 \")\n"
      + "  public String test(final int x) { return \"\"; }\n"
      + "}")).getMessage().contains("Return value referenced parameter 0, but parameter numbers start from 1"));
  }

  @Test
  void testReferencesParamTooHigh() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestReferencesParamTooHigh {\n"
      + "  @Contract(\"_ -> param2 \")\n"
      + "  public static String test(final int x) { return \"\"; }\n"
      + "}")).getMessage().contains("Return value was declared to affect parameter 2, but only 1 parameter"));
  }

  @Test
  void testReferencesParamNonInteger() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestReferencesParam0 {\n"
      + "  @Contract(\"_ -> param999999999999 \")\n"
      + "  public String test(final int x) { return \"\"; }\n"
      + "}")).getMessage().contains("was not a valid integer"));
  }

  @Test
  void testThisInStaticMethod() {
    assertTrue(assertThrows(ReflectException.class, () -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestThisInStaticMethod {\n"
      + "  @Contract(\"_ -> this\")\n"
      + "  public static TestThisInStaticMethod test(final int x) { return null; }\n"
      + "}")).getMessage().contains("Effect keyword 'this' can only be referenced from a non-static context"));
  }

  @Test
  void testThisInNonStaticMethod() {
    assertDoesNotThrow(() -> this.compile("import org.jetbrains.annotations.Contract;\n"
      + "\n"
      + "class TestThisInNonStaticMethod {\n"
      + "  @Contract(\"_ -> this\")\n"
      + "  public TestThisInNonStaticMethod test(final int x) { return this; }\n"
      + "}"));
  }

}
