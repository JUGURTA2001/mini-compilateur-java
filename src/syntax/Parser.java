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
        // 1. Consommer tous les modificateurs disponibles
        List<Token> modifiers = new ArrayList<>();
        while (isModifier(current().type)) {
            modifiers.add(current());
            advance();
        }

        Token token = current();

        // 2. Reconnaître la déclaration de classe
        if (token.type == Token.TokenType.CLASS) {
            return parseClass(modifiers);
        }

        // 3. Reconnaître la déclaration de méthode
        if (isReturnType(token.type)) {
            if (isLikelyMethod(modifiers, token)) {
                return parseMethod(modifiers);
            } else {
                return parseDeclaration();
            }
        }

        // 4. Instructions usuelles
        switch (token.type) {
            case WHILE: return parseWhile();
            case IF: return parseIf();
            case STRING:  
            case CHAR:
            case INT:  
            case DOUBLE:
            case BOOLEAN:
            case JUGURTA:
            case TOUATI:
           
                if (!modifiers.isEmpty() || token.type == Token.TokenType.VOID) {
                    return parseMethod(modifiers);
                } else {
                    return parseDeclaration();
                }
            case IDENTIFIER: 
                // Vérifier si c'est un appel de méthode ou une assignation
                if (peek().type == Token.TokenType.LPAREN) {
                    return parseMethodCall();
                } else {
                    return parseAssignment();
                }
            case LBRACE: return parseBlock();
            default:
                errors.add("Instruction non reconnue: " + token.value + " à la ligne " + token.line);
                advance();
                return null;
        }
    }

    // Nouvelle méthode pour détecter si c'est une méthode
    private boolean isLikelyMethod(List<Token> modifiers, Token typeToken) {
        // Si on a des modificateurs, c'est une méthode
        if (!modifiers.isEmpty()) {
            return true;
        }
        
        // Si c'est 'void', c'est une méthode
        if (typeToken.type == Token.TokenType.VOID) {
            return true;
        }
        
        // Regarder les tokens suivants pour détecter une signature de méthode
        int savedPosition = position;
        try {
            advance(); // passer le type
            
            // Si le prochain token est un identifiant suivi de '(', c'est une méthode
            if (current().type == Token.TokenType.IDENTIFIER) {
                advance();
                if (current().type == Token.TokenType.LPAREN) {
                    return true;
                }
            }
            return false;
        } finally {
            position = savedPosition;
        }
    }

    private boolean isModifier(Token.TokenType type) {
        return type == Token.TokenType.PUBLIC || 
               type == Token.TokenType.STATIC ||
               type == Token.TokenType.PRIVATE ||
               type == Token.TokenType.PROTECTED ||
               type == Token.TokenType.FINAL;
    }

    private boolean isReturnType(Token.TokenType type) {
        return type == Token.TokenType.VOID ||
               type == Token.TokenType.INT ||
               type == Token.TokenType.STRING ||
               type == Token.TokenType.DOUBLE ||
               type == Token.TokenType.CHAR ||
               type == Token.TokenType.BOOLEAN ||
               type == Token.TokenType.JUGURTA ||
               type == Token.TokenType.TOUATI;
    }

    private Token peek() {
        if (position + 1 < tokens.size()) {
            return tokens.get(position + 1);
        }
        return tokens.get(tokens.size() - 1);
    }

    private Token peekNext() {
        if (position + 2 < tokens.size()) {
            return tokens.get(position + 2);
        }
        return tokens.get(tokens.size() - 1);
    }

    private ASTNode parseClass(List<Token> modifiers) {
        consume(Token.TokenType.CLASS, "Expected 'class' keyword");
        Token nameToken = consume(Token.TokenType.IDENTIFIER, "Expected class name");
        ASTNode classNode = new ASTNode("CLASS");
        classNode.value = nameToken.value;
        classNode.line = nameToken.line;
        for (Token mod : modifiers) {
            classNode.addChild(new ASTNode("MODIFIER", mod.value, mod.line));
        }
        
        consume(Token.TokenType.LBRACE, "Expected '{' to start class body");
        while (!isAtEnd() && current().type != Token.TokenType.RBRACE) {
            ASTNode statement = parseStatement();
            if (statement != null) {
                classNode.addChild(statement);
            }
        }
        consume(Token.TokenType.RBRACE, "Expected '}' to close class body");
        return classNode;
    }

    private ASTNode parseMethod(List<Token> modifiers) {
        Token returnType = current();
        advance(); // consomme le type
        Token nameToken = consume(Token.TokenType.IDENTIFIER, "Expected method name");
        ASTNode methodNode = new ASTNode("METHOD");
        methodNode.value = nameToken.value;
        methodNode.line = nameToken.line;
        methodNode.addChild(new ASTNode("RETURN_TYPE", returnType.value, returnType.line));
        for (Token mod : modifiers) {
            methodNode.addChild(new ASTNode("MODIFIER", mod.value, mod.line));
        }
        consume(Token.TokenType.LPAREN, "Expected '(' for method parameters");
        // ignore les paramètres
        while (!isAtEnd() && current().type != Token.TokenType.RPAREN) advance();
        consume(Token.TokenType.RPAREN, "Expected ')' after method parameters");
        consume(Token.TokenType.LBRACE, "Expected '{' to start method body");
        while (!isAtEnd() && current().type != Token.TokenType.RBRACE) {
            ASTNode statement = parseStatement();
            if (statement != null) {
                methodNode.addChild(statement);
            }
        }
        consume(Token.TokenType.RBRACE, "Expected '}' to close method body");
        return methodNode;
    }

    // Nouvelle méthode pour parser les appels de méthode
    private ASTNode parseMethodCall() {
        Token firstToken = current();
        StringBuilder methodName = new StringBuilder();
        
        // Construire le nom complet de la méthode (peut être System.out.println)
        while (current().type == Token.TokenType.IDENTIFIER) {
            methodName.append(current().value);
            advance();
            if (current().type == Token.TokenType.DOT) {
                methodName.append(".");
                advance();
            } else {
                break;
            }
        }
        
        ASTNode methodCall = new ASTNode("METHOD_CALL");
        methodCall.value = methodName.toString();
        methodCall.line = firstToken.line;
        
        consume(Token.TokenType.LPAREN, "Expected '(' after method name");
        
        // Parser les arguments (simplifié)
        while (!isAtEnd() && current().type != Token.TokenType.RPAREN) {
            ASTNode arg = parseExpression();
            if (arg != null) {
                methodCall.addChild(new ASTNode("ARGUMENT").addChild(arg));
            }
            if (current().type == Token.TokenType.COMMA) {
                advance();
            }
        }
        
        consume(Token.TokenType.RPAREN, "Expected ')' after method arguments");
        
        // Consommer le point-virgule s'il est présent
        if (current().type == Token.TokenType.SEMICOLON) {
            advance();
        } else {
            errors.add("Expected ';' after method call at line " + firstToken.line);
        }
        
        return methodCall;
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

        if (token.type == Token.TokenType.STRING_LITERAL) {
            advance();
            ASTNode stringNode = new ASTNode("STRING_LITERAL");
            stringNode.value = token.value;
            return stringNode;
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
        //System.out.println("DEBUG: parseDeclaration() started with token: " + current().value);
        Token typeToken = current();
        advance();

        // Vérifier qu'on a bien un identifiant
        if (current().type != Token.TokenType.IDENTIFIER) {
            errors.add("Expected identifier but found '" + current().value + "' at line " + current().line);
            return null;
        }
        
        Token idToken = current();
        advance();
        
        ASTNode declaration = new ASTNode("DECLARATION");
        declaration.line = typeToken.line;
        declaration.value = typeToken.value + " " + idToken.value;

        // Gestion de l'initialisation optionnelle
        if (current().type == Token.TokenType.EQUAL) {
            advance();
            ASTNode init = parseExpression();
            if (init != null) {
                declaration.addChild(init);
            }
        }

        // Gestion du point-virgule final - AMÉLIORATION
        if (current().type == Token.TokenType.SEMICOLON) {
            advance();
        } else {
            // Message d'erreur plus précis avec la bonne ligne
            int errorLine = Math.max(typeToken.line, idToken.line);
            errors.add("Expected ';' after declaration at line " + errorLine + " but found '" + current().value + "'");
            synchronizeToNextStatement();
        }
        
        //System.out.println("DEBUG: parseDeclaration() ending, next token: " + current().value);
        return declaration;
    }

    // Nouvelle méthode de resynchronisation améliorée
    private void synchronizeToNextStatement() {
        int startPosition = position;
        
        while (!isAtEnd()) {
            Token token = current();
            
            // Tokens qui marquent le début d'une nouvelle instruction
            if (token.type == Token.TokenType.SEMICOLON ||
                token.type == Token.TokenType.INT || 
                token.type == Token.TokenType.DOUBLE ||
                token.type == Token.TokenType.STRING ||
                token.type == Token.TokenType.WHILE ||
                token.type == Token.TokenType.IF ||
                token.type == Token.TokenType.IDENTIFIER ||
                token.type == Token.TokenType.RBRACE ||
                token.type == Token.TokenType.PUBLIC ||
                token.type == Token.TokenType.PRIVATE) {
                
                // Si on trouve un point-virgule, on le consomme
                if (token.type == Token.TokenType.SEMICOLON) {
                    advance();
                }
                return;
            }
            
            // Limiter la recherche pour éviter de sauter trop loin
            if (position > startPosition + 10) {
                return;
            }
            
            advance();
        }
    }

    // Conserver l'ancienne méthode pour la compatibilité
    private void synchronize() {
        synchronizeToNextStatement();
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
            
            if (current().type == Token.TokenType.SEMICOLON) {
                advance();
            } else {
                errors.add("Expected ';' after assignment at line " + idToken.line);
            }
            return assignment;
        }

        if (current().type == Token.TokenType.PLUS_PLUS) {
            advance();
            ASTNode increment = new ASTNode("INCREMENT");
            increment.value = idToken.value;
            if (current().type == Token.TokenType.SEMICOLON) {
                advance();
            } else {
                errors.add("Expected ';' after increment at line " + idToken.line);
            }
            return increment;
        }

        if (current().type == Token.TokenType.MINUS_MINUS) {
            advance();
            ASTNode decrement = new ASTNode("DECREMENT");
            decrement.value = idToken.value;
            if (current().type == Token.TokenType.SEMICOLON) {
                advance();
            } else {
                errors.add("Expected ';' after decrement at line " + idToken.line);
            }
            return decrement;
        }

        errors.add("Assignement invalide: " + idToken.value + " at line " + idToken.line);
        return null;
    }

    private ASTNode parseBlock() {
        Token lbrace = consume(Token.TokenType.LBRACE, "Expected '{'");
        ASTNode block = new ASTNode("BLOCK");
        block.line = lbrace.line;

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

        Token currentToken = current();
        errors.add(errorMsg + " mais trouvé '" + currentToken.value + "' à la ligne " + currentToken.line);
        advance();
        return currentToken;
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
        return position >= tokens.size() || current().type == Token.TokenType.EOF;
    }

    private void printErrors() {
        System.out.println("\n=== ERREURS DÉTECTÉES ===");
        for (String error : errors) {
            System.out.println("❌ " + error);
        }
        System.out.println("========================\n");
    }

    public List<String> getErrors() {
        return errors;
    }
}