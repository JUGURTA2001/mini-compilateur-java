package lexical;

import java.util.*;

public class Lexer {
    private String input;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private List<Token> tokens = new ArrayList<>();

    private static final Map<String, Token.TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("while", Token.TokenType.WHILE);
        KEYWORDS.put("if", Token.TokenType.IF);
        KEYWORDS.put("else", Token.TokenType.ELSE);
        KEYWORDS.put("int", Token.TokenType.INT);
        KEYWORDS.put("String", Token.TokenType.STRING);
        KEYWORDS.put("class", Token.TokenType.CLASS);
        KEYWORDS.put("public", Token.TokenType.PUBLIC);
        KEYWORDS.put("static", Token.TokenType.STATIC);
        KEYWORDS.put("return", Token.TokenType.RETURN);
    }

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        while (position < input.length()) {
            char current = input.charAt(position);

            // Ignorer espaces et tabulations
            if (Character.isWhitespace(current)) {
                if (current == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                position++;
                continue;
            }

            // Commentaires
            if (current == '/' && peek() == '/') {
                skipLineComment();
                continue;
            }

            if (current == '/' && peek() == '*') {
                skipBlockComment();
                continue;
            }

            // Nombres
            if (Character.isDigit(current)) {
                readNumber();
                continue;
            }

            // Identifiants et mots-clés
            if (Character.isLetter(current) || current == '_') {
                readIdentifier();
                continue;
            }

            // Chaînes de caractères
            if (current == '"') {
                readString();
                continue;
            }

            // Opérateurs et délimiteurs
            if (!readOperator()) {
                tokens.add(new Token(Token.TokenType.ERROR, String.valueOf(current), line, column));
                position++;
                column++;
            }
        }

        tokens.add(new Token(Token.TokenType.EOF, "", line, column));
        return tokens;
    }

    private char peek() {
        if (position + 1 < input.length()) {
            return input.charAt(position + 1);
        }
        return '\0';
    }

    private char peekAhead(int offset) {
        if (position + offset < input.length()) {
            return input.charAt(position + offset);
        }
        return '\0';
    }

    private void skipLineComment() {
        while (position < input.length() && input.charAt(position) != '\n') {
            position++;
        }
    }

    private void skipBlockComment() {
        position += 2;
        while (position + 1 < input.length()) {
            if (input.charAt(position) == '*' && input.charAt(position + 1) == '/') {
                position += 2;
                return;
            }
            if (input.charAt(position) == '\n') {
                line++;
                column = 1;
            }
            position++;
        }
    }

    private void readNumber() {
        StringBuilder sb = new StringBuilder();
        int startLine = line, startColumn = column;

        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            sb.append(input.charAt(position));
            position++;
            column++;
        }

        tokens.add(new Token(Token.TokenType.NUMBER, sb.toString(), startLine, startColumn));
    }

    private void readIdentifier() {
        StringBuilder sb = new StringBuilder();
        int startLine = line, startColumn = column;

        while (position < input.length() &&
               (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) {
            sb.append(input.charAt(position));
            position++;
            column++;
        }

        String word = sb.toString();
        Token.TokenType type = KEYWORDS.getOrDefault(word, Token.TokenType.IDENTIFIER);
        tokens.add(new Token(type, word, startLine, startColumn));
    }

    private void readString() {
        StringBuilder sb = new StringBuilder();
        int startLine = line, startColumn = column;
        position++; // Sauter le '"'
        column++;

        while (position < input.length() && input.charAt(position) != '"') {
            if (input.charAt(position) == '\\') {
                position++;
                column++;
                if (position < input.length()) {
                    sb.append(input.charAt(position));
                }
            } else {
                sb.append(input.charAt(position));
            }
            position++;
            column++;
        }

        if (position < input.length()) {
            position++; // Sauter le '"' de fermeture
            column++;
        }

        tokens.add(new Token(Token.TokenType.STRING_LITERAL, sb.toString(), startLine, startColumn));
    }

    private boolean readOperator() {
        int startColumn = column;
        char current = input.charAt(position);

        // Opérateurs doubles
        if (current == '=' && peek() == '=') {
            tokens.add(new Token(Token.TokenType.EQUAL_EQUAL, "==", line, startColumn));
            position += 2;
            column += 2;
            return true;
        }
        if (current == '!' && peek() == '=') {
            tokens.add(new Token(Token.TokenType.NOT_EQUAL, "!=", line, startColumn));
            position += 2;
            column += 2;
            return true;
        }
        if (current == '<' && peek() == '=') {
            tokens.add(new Token(Token.TokenType.LESS_EQUAL, "<=", line, startColumn));
            position += 2;
            column += 2;
            return true;
        }
        if (current == '>' && peek() == '=') {
            tokens.add(new Token(Token.TokenType.GREATER_EQUAL, ">=", line, startColumn));
            position += 2;
            column += 2;
            return true;
        }
        if (current == '+' && peek() == '+') {
            tokens.add(new Token(Token.TokenType.PLUS_PLUS, "++", line, startColumn));
            position += 2;
            column += 2;
            return true;
        }
        if (current == '-' && peek() == '-') {
            tokens.add(new Token(Token.TokenType.MINUS_MINUS, "--", line, startColumn));
            position += 2;
            column += 2;
            return true;
        }

        // Opérateurs simples
        switch (current) {
            case '=':
                tokens.add(new Token(Token.TokenType.EQUAL, "=", line, startColumn));
                break;
            case '<':
                tokens.add(new Token(Token.TokenType.LESS, "<", line, startColumn));
                break;
            case '>':
                tokens.add(new Token(Token.TokenType.GREATER, ">", line, startColumn));
                break;
            case '+':
                tokens.add(new Token(Token.TokenType.PLUS, "+", line, startColumn));
                break;
            case '-':
                tokens.add(new Token(Token.TokenType.MINUS, "-", line, startColumn));
                break;
            case '*':
                tokens.add(new Token(Token.TokenType.MULTIPLY, "*", line, startColumn));
                break;
            case '/':
                tokens.add(new Token(Token.TokenType.DIVIDE, "/", line, startColumn));
                break;
            case '%':
                tokens.add(new Token(Token.TokenType.MODULO, "%", line, startColumn));
                break;
            case '(':
                tokens.add(new Token(Token.TokenType.LPAREN, "(", line, startColumn));
                break;
            case ')':
                tokens.add(new Token(Token.TokenType.RPAREN, ")", line, startColumn));
                break;
            case '{':
                tokens.add(new Token(Token.TokenType.LBRACE, "{", line, startColumn));
                break;
            case '}':
                tokens.add(new Token(Token.TokenType.RBRACE, "}", line, startColumn));
                break;
            case ';':
                tokens.add(new Token(Token.TokenType.SEMICOLON, ";", line, startColumn));
                break;
            case ',':
                tokens.add(new Token(Token.TokenType.COMMA, ",", line, startColumn));
                break;
            default:
                return false;
        }

        position++;
        column++;
        return true;
    }
}