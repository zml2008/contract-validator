grammar JbContract;

@header {
package ca.stellardrift.contractvalidator;
}

// Contract(value="<contract>")
contract: clause (';' clause)* EOF;
clause: args '->' effect;
args: constraint (',' constraint)*;
constraint: ANY          # OtherConstraint
          | NULL         # NonPrimitiveConstraint
          | NONNULL      # NonPrimitiveConstraint
          | FALSE        # BooleanConstraint
          | TRUE         # BooleanConstraint
          ;
effect: constraint       # OtherEffect
      | PARAM num=NUMBER # ParamEffect
      | FAIL             # OtherEffect
      | THIS             # NonStaticEffect
      | NEW              # OtherEffect
      ;

// Contract(mutates="<mutates>")
mutates: mutateElement (',' mutateElement)* EOF;
mutateElement: THIS             # MutatesThis
             | PARAM num=NUMBER # MutatesParam
             | PARAM            # MutatesParam
             ;


ANY: '_';
NONNULL: '!null';
NULL: 'null';
FALSE: 'false';
TRUE: 'true';

FAIL: 'fail';
THIS: 'this';
NEW: 'new';

PARAM: 'param';
NUMBER: [0-9]+;

WS: [ \r\n\t]+ -> skip;
