// File: Compiladores/Symbol.java
public class Symbol {
    private String name;
    private TokenType type; // Em um compilador real, teríamos um sistema de tipos mais complexo

    public Symbol(String name, TokenType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "<" + name + ":" + type + ">";
    }
}