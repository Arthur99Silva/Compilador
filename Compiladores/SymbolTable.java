// File: Compiladores/SymbolTable.java
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Symbol> symbols = new HashMap<>();
    private SymbolTable parent; // Para escopos aninhados

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    // Adiciona um símbolo à tabela. Retorna falso se o símbolo já existir neste escopo.
    public boolean put(String name, Symbol symbol) {
        if (symbols.containsKey(name)) {
            return false;
        }
        symbols.put(name, symbol);
        return true;
    }

    // Busca por um símbolo na tabela atual e, se não encontrar, nas tabelas-pai.
    public Symbol get(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol == null && parent != null) {
            return parent.get(name);
        }
        return symbol;
    }

    public SymbolTable getParent() {
        return parent;
    }
}