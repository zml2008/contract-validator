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

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

class ContractValueValidationListener extends JbContractBaseListener {

  private final ExecutableElement targetElement;
  private final ErrorConsumer messager;

  // state
  private boolean inArgs;
  private int argIdx;

  ContractValueValidationListener(final ExecutableElement targetElement, final ErrorConsumer messager) {
    this.targetElement = targetElement;
    this.messager = messager;
  }

  // Track the argument state

  @Override
  public void exitEveryRule(final ParserRuleContext ctx) {
    if (this.inArgs && ctx instanceof JbContractParser.ConstraintContext) {
      this.argIdx++;
    }
  }

  @Override
  public void enterArgs(final JbContractParser.ArgsContext ctx) {
    this.inArgs = true;
    final int providedArgs = ctx.constraint().size();
    final int paramCount = this.targetElement.getParameters().size();
    if (providedArgs < paramCount) {
      this.messager.print(Diagnostic.Kind.ERROR, String.format(
        "Clause only specified %d parameter(s), but method '%s' had %d parameter(s)",
        providedArgs,
        this.targetElement.getSimpleName(),
        paramCount
      ));
    } else if (providedArgs > paramCount) {
      this.messager.print(Diagnostic.Kind.ERROR, String.format(
        "Clause specified %d parameter(s), but method '%s' had only %d parameter(s)",
        providedArgs,
        this.targetElement.getSimpleName(),
        paramCount
      ));
    }
  }

  @Override
  public void exitArgs(final JbContractParser.ArgsContext ctx) {
    this.inArgs = false;
    this.argIdx = 0;
  }

  private TypeMirror activeParameter() {
    if (this.inArgs) { // a parameter
      final List<? extends VariableElement> params = this.targetElement.getParameters();
      if (this.argIdx >= params.size()) {
        return null;
      }
      return params.get(this.argIdx).asType();
    } else { // return type
      return this.targetElement.getReturnType();
    }
  }

  // Validate each constraint and effect

  @Override
  public void enterNonPrimitiveConstraint(final JbContractParser.NonPrimitiveConstraintContext ctx) {
    // the active argument must not be a primitive
    final TypeMirror activeParameter = this.activeParameter();
    if (activeParameter != null && activeParameter.getKind().isPrimitive()) {
      this.messager.print(Diagnostic.Kind.ERROR, String.format(
        "Constraint '%s' is only applicable to non-primitive types, but it was used to refer to a '%s'",
        ctx.getText(),
        activeParameter
      ));
    }
  }

  @Override
  public void enterBooleanConstraint(final JbContractParser.BooleanConstraintContext ctx) {
    // the active argument must be a boolean
    final TypeMirror activeParameter = this.activeParameter();
    if (activeParameter != null && activeParameter.getKind() != TypeKind.BOOLEAN) {
      this.messager.print(Diagnostic.Kind.ERROR, String.format(
        "Constraint '%s' is only applicable to boolean values, but it was used to refer to a '%s'",
        ctx.getText(),
        activeParameter
      ));
    }
  }

  @Override
  public void enterParamEffect(final JbContractParser.ParamEffectContext ctx) {
    // we must be a valid parameter number
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
        "Return value was declared to affect parameter %d, but only %d parameters exist",
        paramNum,
        possibleParameters
      ));
    } else if (paramNum < 1) {
      this.messager.print(Diagnostic.Kind.ERROR, "Return value referenced parameter 0, but parameter numbers start from 1");
    }
  }

  @Override
  public void enterNonStaticEffect(final JbContractParser.NonStaticEffectContext ctx) {
    // cannot be in a static context
    if (this.targetElement.getModifiers().contains(Modifier.STATIC)) {
      this.messager.print(Diagnostic.Kind.ERROR, String.format(
        "Effect keyword '%s' can only be referenced from a non-static context, but method '%s' was static.",
        ctx.getText(),
        this.targetElement.getSimpleName()
      ));
    }
  }

}
