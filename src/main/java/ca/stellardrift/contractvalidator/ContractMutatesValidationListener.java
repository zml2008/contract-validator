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

import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

/**
 * Validate the {@code mutates} field of Contract annotations.
 *
 * <p>rules:</p>
 * <ul>
 * <li>error: `this` can only be used on non-static methods</li>
 * <li>error: `param&lt;n&gt;` can only be used for 1-(number of parameters)</li>
 * <li>warn: param used multiple times</li>
 * </ul>
 */
class ContractMutatesValidationListener extends JbContractBaseListener {

  private Set<String> seenParams = null;
  private final ExecutableElement targetElement;
  private final ErrorConsumer messager;

  ContractMutatesValidationListener(final ExecutableElement targetElement, final ErrorConsumer messager) {
    this.targetElement = targetElement;
    this.messager = messager;
  }

  @Override
  public void enterMutatesParam(final JbContractParser.MutatesParamContext ctx) {
    this.validateSeenOnlyOnce(ctx.getText());

    if (ctx.num == null) {
      this.validateSeenOnlyOnce(ctx.getText() + "1"); // param is shorthand for param1
      // the no-number equivalent can only be used when there is exactly one parameter
      if (this.targetElement.getParameters().size() != 1) {
        this.messager.print(Diagnostic.Kind.ERROR, "An unqualified 'param' was used when there was not exactly one parameter.");
      }
    } else {
      final int paramNum;
      try {
        paramNum = Integer.parseInt(ctx.num.getText());
      } catch (final NumberFormatException ex) {
        this.messager.print(Diagnostic.Kind.ERROR, String.format("Number '%s' was not a valid integer", ctx.num.getText()));
        return;
      }

      final int possibleParameters = this.targetElement.getParameters().size();
      if (paramNum > possibleParameters) {
        this.messager.print(Diagnostic.Kind.ERROR, String.format(
          "Method '%s' was declared to mutate parameter %d, but only %d parameters exist",
          this.targetElement.getSimpleName(),
          paramNum,
          possibleParameters
        ));
      } else if (paramNum < 1) {
        this.messager.print(Diagnostic.Kind.ERROR, "Mutates clause referenced parameter 0, but parameter numbers start from 1");
      }
    }
  }

  @Override
  public void enterMutatesThis(final JbContractParser.MutatesThisContext ctx) {
    this.validateSeenOnlyOnce(ctx.getText());
    if (targetElement.getModifiers().contains(Modifier.STATIC)) {
      this.messager.print(Diagnostic.Kind.ERROR, "Invalid contract: 'this' cannot be mutated from a static context");
    }
  }

  private void validateSeenOnlyOnce(final String token) {
    if (seenParams == null) {
      seenParams = new HashSet<>();
    }

    if (!seenParams.add(token)) {
      this.messager.print(
        Diagnostic.Kind.WARNING,
        String.format("Invalid mutates clause: specifier '%s' seen multiple times", token)
      );
    }
  }

}
