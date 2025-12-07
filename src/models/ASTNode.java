package models;

import java.util.*;

public class ASTNode {
    public String type;
    public String value;
    public int line;
    public List<ASTNode> children = new ArrayList<>();

    public ASTNode(String type) {
        this.type = type;
        this.value = ""; // Ajout : jamais null
        this.line = 0;
    }
    public ASTNode(String type, String value, int line) {
        this.type = type;
        this.value = (value != null) ? value : ""; // Ajout : valeur jamais null
        this.line = line;
    }

    public ASTNode addChild(ASTNode child) {
        if (child != null) {
            children.add(child);
        }
        return this;
    }

    public void print(int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }

        System.out.print(indent + "├─ " + type);

        // Correction ici : sécurité sur la valeur null
        if (value != null && !value.isEmpty()) {
            System.out.print(" [" + value + "]");
        }

        if (line > 0) {
            System.out.print(" (@" + line + ")");
        }
        System.out.println();

        for (ASTNode child : children) {
            child.print(depth + 1);
        }
    }
}