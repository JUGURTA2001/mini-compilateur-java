package syntax;

import lexical.*;
import models.*;
import java.util.*;

public class Parser {
    private List<Token> tokens;
    private int position = 0;
    private List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ASTNode parse() {
        try {
            ASTNode program = parseProgram();
            if (!errors.isEmpty()) {
                printErrors();
            }
            return program;
        } catch (Exception e) {
            errors.add("Erreur fatale: " + e.getMessage());
            printErrors();
            return null;
        }
    }

    private ASTNode parseProgram() {
        ASTNode root = new ASTNode("PROGRAM");
        while (!isAtEnd() && current().type != Token.TokenType.EOF) {
            ASTNode statement = parseStatement();
            if (statement != null) {
                root.addChild(statement);
            }
        }
        return root;
    }

    private ASTNode parseStatement() {
        Token token = current();

        switch (token.type) {
            case WHILE:
                return parseWhile();
            case IF:
                return parseIf();
            case INT:
            case STRING:
                return parseDeclaration();
            case IDENTIFIER:
                return parseAssignment();
            case LBRACE:
                return parseBlock();
            default:
                advance();
                errors.add("Instruction non reconnue: " + token.value + " à la ligne " + token.line);
                return null;
        }
    }

    private ASTNode parseWhile() {
        Token whileToken = consume(Token.TokenType.WHILE, "Expected 'while'");
        ASTNode whileNode = new ASTNode("WHILE");
        whileNode.line = whileToken.line;

        consume(Token.TokenType.LPAREN, "Expected '(' après 'while'");
        
        ASTNode condition = parseCondition();
        whileNode.addChild(new ASTNode("CONDITION").addChild(condition));

        consume(Token.TokenType.RPAREN, "Expected ')' pour fermer la condition");

        ASTNode body = parseStatement();
        if (body != null) {
            whileNode.addChild(new ASTNode("BODY").addChild(body));
        }

        return whileNode;
    }

    private ASTNode parseCondition() {
        ASTNode left = parseExpression();
        
        if (isComparisonOperator()) {
            Token op = current();
            advance();
            ASTNode right = parseExpression();
            
            ASTNode comparison = new ASTNode("COMPARISON");
            comparison.value = op.value;
            comparison.addChild(left);
            comparison.addChild(right);
            return comparison;
        }
        
        return left;
    }

    private boolean isComparisonOperator() {
        Token token = current();
        return token.type == Token.TokenType.EQUAL_EQUAL ||
               token.type == Token.TokenType.NOT_EQUAL ||
               token.type == Token.TokenType.LESS ||
               token.type == Token.TokenType.GREATER ||
               token.type == Token.TokenType.LESS_EQUAL ||
               token.type == Token.TokenType.GREATER_EQUAL;
    }

    private ASTNode parseExpression() {
        ASTNode left = parseTerm();

        while (current().type == Token.TokenType.PLUS || current().type == Token.TokenType.MINUS) {
            Token op = current();
            advance();
            ASTNode right = parseTerm();
            
            ASTNode binary = new ASTNode("BINARY_OP");
            binary.value = op.value;
            binary.addChild(left);
            binary.addChild(right);
            left = binary;
        }

        return left;
    }

    private ASTNode parseTerm() {
        ASTNode left = parseFactor();

        while (current().type == Token.TokenType.MULTIPLY || 
               current().type == Token.TokenType.DIVIDE || 
               current().type == Token.TokenType.MODULO) {
            Token op = current();
            advance();
            ASTNode right = parseFactor();
            
            ASTNode binary = new ASTNode("BINARY_OP");
            binary.value = op.value;
            binary.addChild(left);
            binary.addChild(right);
            left = binary;
        }

        return left;
    }

    private ASTNode parseFactor() {
        Token token = current();

        if (token.type == Token.TokenType.NUMBER) {
            advance();
            ASTNode numberNode = new ASTNode("NUMBER");
            numberNode.value = token.value;
            return numberNode;
        }

        if (token.type == Token.TokenType.IDENTIFIER) {
            advance();
            ASTNode idNode = new ASTNode("IDENTIFIER");
            idNode.value = token.value;
            
            // Vérifier ++, --
            if (current().type == Token.TokenType.PLUS_PLUS) {
                advance();
                ASTNode postInc = new ASTNode("POST_INCREMENT");
                postInc.addChild(idNode);
                return postInc;
            }
            if (current().type == Token.TokenType.MINUS_MINUS) {
                advance();
                ASTNode postDec = new ASTNode("POST_DECREMENT");
                postDec.addChild(idNode);
                return postDec;
            }
            
            return idNode;
        }

        if (token.type == Token.TokenType.LPAREN) {
            advance();
            ASTNode expr = parseCondition();
            consume(Token.TokenType.RPAREN, "Expected ')'");
            return expr;
        }

        errors.add("Expression invalide: " + token.value + " à la ligne " + token.line);
        advance();
        return new ASTNode("ERROR");
    }

    private ASTNode parseIf() {
        Token ifToken = consume(Token.TokenType.IF, "Expected 'if'");
        ASTNode ifNode = new ASTNode("IF");
        ifNode.line = ifToken.line;

        consume(Token.TokenType.LPAREN, "Expected '(' après 'if'");
        ASTNode condition = parseCondition();
        ifNode.addChild(new ASTNode("CONDITION").addChild(condition));
        consume(Token.TokenType.RPAREN, "Expected ')'");

        ASTNode thenBody = parseStatement();
        if (thenBody != null) {
            ifNode.addChild(new ASTNode("THEN").addChild(thenBody));
        }

        if (current().type == Token.TokenType.ELSE) {
            advance();
            ASTNode elseBody = parseStatement();
            if (elseBody != null) {
                ifNode.addChild(new ASTNode("ELSE").addChild(elseBody));
            }
        }

        return ifNode;
    }

    private ASTNode parseDeclaration() {
        Token typeToken = current();
        advance();

        Token idToken = consume(Token.TokenType.IDENTIFIER, "Expected identifier");
        
        ASTNode declaration = new ASTNode("DECLARATION");
        declaration.line = typeToken.line;
        declaration.value = typeToken.value + " " + idToken.value;

        if (current().type == Token.TokenType.EQUAL) {
            advance();
            ASTNode init = parseExpression();
            declaration.addChild(init);
        }

        consume(Token.TokenType.SEMICOLON, "Expected ';'");
        return declaration;
    }

    private ASTNode parseAssignment() {
        Token idToken = consume(Token.TokenType.IDENTIFIER, "Expected identifier");
        
        if (current().type == Token.TokenType.EQUAL) {
            advance();
            ASTNode value = parseExpression();
            
            ASTNode assignment = new ASTNode("ASSIGNMENT");
            assignment.line = idToken.line;
            assignment.value = idToken.value;
            assignment.addChild(value);
            
            consume(Token.TokenType.SEMICOLON, "Expected ';'");
            return assignment;
        }

        if (current().type == Token.TokenType.PLUS_PLUS) {
            advance();
            ASTNode increment = new ASTNode("INCREMENT");
            increment.value = idToken.value;
            consume(Token.TokenType.SEMICOLON, "Expected ';'");
            return increment;
        }

        if (current().type == Token.TokenType.MINUS_MINUS) {
            advance();
            ASTNode decrement = new ASTNode("DECREMENT");
            decrement.value = idToken.value;
            consume(Token.TokenType.SEMICOLON, "Expected ';'");
            return decrement;
        }

        errors.add("Assignement invalide: " + idToken.value);
        return null;
    }

    private ASTNode parseBlock() {
        consume(Token.TokenType.LBRACE, "Expected '{'");
        ASTNode block = new ASTNode("BLOCK");

        while (!isAtEnd() && current().type != Token.TokenType.RBRACE) {
            ASTNode statement = parseStatement();
            if (statement != null) {
                block.addChild(statement);
            }
        }

        consume(Token.TokenType.RBRACE, "Expected '}'");
        return block;
    }

    private Token consume(Token.TokenType type, String errorMsg) {
        if (current().type == type) {
            Token token = current();
            advance();
            return token;
        }

        Token current = current();
        errors.add(errorMsg + " mais trouvé '" + current.value + "' à la ligne " + current.line);
        advance();
        return current;
    }

    private Token current() {
        if (position < tokens.size()) {
            return tokens.get(position);
        }
        return tokens.get(tokens.size() - 1);
    }

    private void advance() {
        if (!isAtEnd()) {
            position++;
        }
    }

    private boolean isAtEnd() {
        return current().type == Token.TokenType.EOF;
    }

    private void printErrors() {
        System.out.println("\n=== ERREURS DÉTECTÉES ===");
        for (String error : errors) {
            System.out.println("❌ " + error);
        }
        System.out.println("========================\n");
    }
}