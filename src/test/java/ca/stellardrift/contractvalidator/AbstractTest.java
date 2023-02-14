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

import org.intellij.lang.annotations.Language;
import org.joor.CompileOptions;
import org.joor.Reflect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public class AbstractTest {
  private static final String PACKAGE_BASE = "contracttest.";

  private String testName;
  private int compileIndex;

  @BeforeEach
  void setupContext(final TestInfo info) {
    this.testName = info.getDisplayName().replace(' ', '_');
    if (this.testName.length() > 1 && !Character.isUpperCase(this.testName.charAt(0))) {
      this.testName = Character.toUpperCase(this.testName.charAt(0)) + this.testName.substring(1);
    }
    this.compileIndex = 0;
  }

  protected Reflect compile(@Language("JAVA") final String classFile) {
    final String className = PACKAGE_BASE + "In" + this.testName + (++this.compileIndex);
    return Reflect.compile(
      className,
      classFile,
      new CompileOptions().processors(new ContractValidatorProcessor())
    );
  }

}
