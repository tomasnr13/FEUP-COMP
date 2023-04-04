package pt.up.fe.comp.ast;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {

    START,
    IMPORT_DECLARATION,
    CLASS_DECLARATION,
    ASSIGNMENT,
    METHOD_DEF,
    METHOD_CALL,
    MAIN_METHOD,
    STATEMENT,
    IF,
    WHILE,
    EXPRESSION,
    ARRAY,
    THIS,
    RETURN;

    private final String name;

    private AstNode() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    @Override
    public String toString() {
        return name;
    }
}