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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.util.BitSet;

import javax.tools.Diagnostic;

class ParseErrorListener extends BaseErrorListener {

  private final String input;
  private final ErrorConsumer messager;

  public ParseErrorListener(final String input, final ErrorConsumer messager) {
    this.input = input;
    this.messager = messager;
  }

  @Override
  public void syntaxError(
    final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line, final int charPositionInLine, final String msg,
    final RecognitionException e
  ) {
    final StringBuilder errorMessage = new StringBuilder(msg.length() + (this.input.length() * 2));

    errorMessage
      .append(msg)
      .append(System.lineSeparator())
      .append(System.lineSeparator())
      .append("at: ")
      .append(this.input)
      .append(System.lineSeparator());

    // then a pointer
    for (int i = 0; i < (4 + charPositionInLine); ++i) {
      errorMessage.append(' ');
    }
    errorMessage.append('^');

    messager.print(Diagnostic.Kind.ERROR, errorMessage);
  }

  @Override
  public void reportAmbiguity(
    final Parser recognizer, final DFA dfa, final int startIndex, final int stopIndex, final boolean exact, final BitSet ambigAlts,
    final ATNConfigSet configs
  ) {
    this.messager.print(Diagnostic.Kind.WARNING, "Ambiguity occurred from " + startIndex + " to " + stopIndex);
    super.reportAmbiguity(recognizer, dfa, startIndex, stopIndex, exact, ambigAlts, configs);
  }

}
