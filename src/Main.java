import lexical.*;
import syntax.*;
import models.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args)  {
        // Exemple de code avec boucle while
       String path = "C:\\Users\\jugurta\\Desktop\\mini-compilateur-java\\tests\\WhileTest.java";
    String code = "";
    try {
        code = Files.readString(Path.of(path));
    } catch (IOException e) {
        System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        System.exit(1);
    }

        
        

        System.out.println("=== MINI-COMPILATEUR ===\n");
        System.out.println("📄 CODE À ANALYSER:");
        System.out.println(code);
        System.out.println("\n" + "=".repeat(40) + "\n");

        // ÉTAPE 1 : Analyse lexicale
        System.out.println("📍 ÉTAPE 1 - ANALYSE LEXICALE");
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        System.out.println("Tokens identifiés:");
        for (Token token : tokens) {
            if (token.type != Token.TokenType.EOF) {
                System.out.println("  " + token);
            }
        }

        System.out.println("\n" + "=".repeat(40) + "\n");

        // ÉTAPE 2 : Analyse syntaxique
        System.out.println("📍 ÉTAPE 2 - ANALYSE SYNTAXIQUE");
        Parser parser = new Parser(tokens);
        ASTNode ast = parser.parse();

        if (ast != null) {
            System.out.println("Arbre syntaxique (AST):\n");
            ast.print(0);
        }

        System.out.println("\n✅ Compilation terminée!");
    }
}