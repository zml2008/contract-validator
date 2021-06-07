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

import com.google.auto.service.AutoService;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes(ContractValidatorProcessor.CONTRACT_ANNOTATION)
public class ContractValidatorProcessor extends AbstractProcessor {

  public static final String CONTRACT_ANNOTATION = "org.jetbrains.annotations.Contract";

  @Override
  public SourceVersion getSupportedSourceVersion() {
    // We only have to be compatible with the JB annotations -- it is unlikely we will have to process arbitrary source features.
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final TypeElement contractAnnotation = this.processingEnv.getElementUtils().getTypeElement(CONTRACT_ANNOTATION);
    if (contractAnnotation == null) {
      this.processingEnv.getMessager()
        .printMessage(Diagnostic.Kind.ERROR, "Failed to resolve the Contract annotation on the compile classpath, validation cannot occur");
      return false;
    }

    this.validateAllElements(roundEnv.getElementsAnnotatedWith(contractAnnotation));

    return false; // never claim
  }

  private void validateAllElements(final Set<? extends Element> elements) {
    final Messager log = this.processingEnv.getMessager();
    for (final Element element : elements) {
      final ElementKind kind = element.getKind();
      if (kind != ElementKind.METHOD && kind != ElementKind.CONSTRUCTOR) {
        log.printMessage(
          Diagnostic.Kind.ERROR, "A @Contract annotation was found on an element that was neither a method or a constructor.", element);
        continue;
      }
      final ExecutableElement executable = (ExecutableElement) element;

      final AnnotationMirror contractAnnotation = getAnnotationOfType(element, CONTRACT_ANNOTATION);
      if (contractAnnotation == null) {
        log.printMessage(Diagnostic.Kind.ERROR, "Could not actually find annotation", element);
        continue;
      }

      validateContractValue(executable, contractAnnotation);
      validateContractMutates(executable, contractAnnotation);
    }
  }

  private void validateContractValue(final ExecutableElement containingElement, final AnnotationMirror contractAnnotation) {
    this.tryAndParse(containingElement, contractAnnotation, "value", ContractValueValidationListener::new, JbContractParser::contract);
  }

  // a comma-separated list of this, param, or param<n> with no repetitions
  private void validateContractMutates(final ExecutableElement containingElement, final AnnotationMirror contractAnnotation) {
    this.tryAndParse(containingElement, contractAnnotation, "mutates", ContractMutatesValidationListener::new, JbContractParser::mutates);
  }

  private void tryAndParse(
    final ExecutableElement containing,
    final AnnotationMirror annotation,
    final String annotationField,
    final BiFunction<ExecutableElement, ErrorConsumer, JbContractListener> listenerMaker,
    final Function<JbContractParser, ? extends ParseTree> rootNode
  ) {
    final AnnotationValue annotationValue = getAnnotationValue(annotation, annotationField);
    if (annotationValue == null) {
      return;
    }

    final Object unknownValue = annotationValue.getValue();
    if (!(unknownValue instanceof String)) {
      this.processingEnv.getMessager().printMessage(
        Diagnostic.Kind.WARNING,
        String.format(
          "Found an annotation value for field %s, but it was a %s instead of the expected String type.",
          annotationField,
          unknownValue.getClass()
        ),
        containing,
        annotation,
        annotationValue
      );
      return;
    }

    final String valueText = (String) unknownValue;

    // Now parse
    final ErrorConsumer errorConsumer =
      (kind, message) -> this.processingEnv.getMessager().printMessage(kind, message, containing, annotation, annotationValue);
    final ParseErrorListener handler = new ParseErrorListener(valueText, errorConsumer);

    // TODO: the error messages here can be a bit spammy, look at how to improve them sometime maybe?
    // or: just bail faster. maybe suppress lexer errors, when a parser error happens, constrain the error location to be no further than the last
    // successful token
    final CharStream stream = CharStreams.fromString(valueText);
    final JbContractLexer lexer = new JbContractLexer(stream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(handler);

    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final JbContractParser parser = new JbContractParser(tokens);

    ParseTree parsed;
    // try with faster SLL(*)
    parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
    // no errors or recovery
    parser.removeErrorListeners();
    parser.setErrorHandler(new BailErrorStrategy());
    try {
      parsed = rootNode.apply(parser);
    } catch (final ParseCancellationException ex) {
      // Reset
      tokens.seek(0);
      parser.reset();
      parser.addErrorListener(handler);
      parser.setErrorHandler(new DefaultErrorStrategy());

      // try again with full LL(*)
      parser.getInterpreter().setPredictionMode(PredictionMode.LL);
      parsed = rootNode.apply(parser);
    }

    // If parsing was possible, report any validation errors
    ParseTreeWalker.DEFAULT.walk(listenerMaker.apply(containing, errorConsumer), parsed);
  }

  private AnnotationMirror getAnnotationOfType(final Element element, final String type) {
    for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
      if (this.processingEnv.getElementUtils().getBinaryName((TypeElement) annotation.getAnnotationType().asElement()).contentEquals(type)) {
        return annotation;
      }
    }
    return null;
  }

  private AnnotationValue getAnnotationValue(final AnnotationMirror annotation, final String field) {
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(field)) {
        return entry.getValue();
      }
    }
    return null;
  }

}
