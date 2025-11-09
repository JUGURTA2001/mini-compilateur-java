package models;

import java.util.*;

public class ASTNode {
    public String type;
    public String value;
    public int line;
    public List<ASTNode> children = new ArrayList<>();

    public ASTNode(String type) {
        this.type = type;
        this.value = "";
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
        if (!value.isEmpty()) {
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