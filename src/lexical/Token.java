package lexical;

public class Token {
    public enum TokenType {
        // Mots-clés
        WHILE, IF, ELSE, INT, STRING, CLASS, PUBLIC, STATIC, 
        RETURN,VOID,MAIN,ARGS,BOOLEAN,CHAR,SYSTEM,OUT,PRINT,PRINTLN,
        DOUBLE,JUGURTA,TOUATI,FINAL,PROTECTED,PRIVATE,
        
        // Opérateurs
        EQUAL, EQUAL_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL, NOT_EQUAL,
        PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
        PLUS_PLUS, MINUS_MINUS,
        // Délimiteurs
        LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON, COMMA,DOT,RBRACKET,LBRACKET,
        // Autres
        IDENTIFIER, NUMBER, STRING_LITERAL, COMMENT,
        EOF, ERROR
    }

    public TokenType type;
    public String value;
    public int line;
    public int column;

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("[%s: '%s' @%d:%d]", type, value, line, column);
    }
}