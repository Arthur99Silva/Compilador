public enum TokenType {
    KEYWORD,        // Palavras reservadas da linguagem (ex.: int, float, if, etc.)
    IDENTIFIER,     // Identificadores (nomes de variáveis, funções, etc.)
    NUMBER,         // Números (inteiros e decimais)
    OPERATOR,       // Operadores (ex.: +, -, =, ==, etc.)
    SEPARATOR,      // Separadores (ex.: ;, (, ), {, }, etc.)
    STRING_LITERAL, // Literais de string
    COMMENT,        // Comentários (tanto de linha quanto multi-linha)
    WHITESPACE,     // Espaços em branco (pode ser descartado)
    UNKNOWN         // Qualquer token não reconhecido
}
