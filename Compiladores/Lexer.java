import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private String input;
    private int pos;
    private int line;
    private int column;
    
    // Lista de palavras reservadas da linguagem C (exemplo simplificado)
    private static final String[] keywords = {
        "int", "float", "double", "char", "if", "else", "while", "for", "return", "void", "static"
    };
    
    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }
    
    // Método principal que gera a lista de tokens
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        
        while (pos < input.length()) {
            char current = peek();
            
            // Ignora espaços em branco (eles podem ser descartados ou gerenciados se necessário)
            if (Character.isWhitespace(current)) {
                consumeWhitespace();
                continue;
            }
            
            // Identificadores ou palavras-chave: começam com letra ou sublinhado
            if (Character.isLetter(current) || current == '_') {
                tokens.add(tokenizeIdentifierOrKeyword());
                continue;
            }
            
            // Números: dígitos (podendo ter ponto decimal)
            if (Character.isDigit(current)) {
                tokens.add(tokenizeNumber());
                continue;
            }
            
            // Literais de string: iniciam e terminam com aspas duplas
            if (current == '\"') {
                tokens.add(tokenizeString());
                continue;
            }
            
            // Comentários: verificação para comentário de linha ou multi-linha
            if (current == '/') {
                if (peekNext() == '/') {
                    tokens.add(tokenizeSingleLineComment());
                    continue;
                } else if (peekNext() == '*') {
                    tokens.add(tokenizeMultiLineComment());
                    continue;
                }
            }
            
            // Operadores e separadores: caso geral para outros símbolos
            tokens.add(tokenizeOperatorOrSeparator());
        }
        
        return tokens;
    }
    
    // Retorna o caractere atual sem avançar a posição
    private char peek() {
        return input.charAt(pos);
    }
    
    // Retorna o próximo caractere (para verificação de operadores compostos)
    private char peekNext() {
        if (pos + 1 < input.length()) {
            return input.charAt(pos + 1);
        }
        return '\0';
    }
    
    // Avança um caractere e atualiza linha e coluna
    private char advance() {
        char current = input.charAt(pos);
        pos++;
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }
    
    // Consome todos os caracteres de espaço em branco
    private void consumeWhitespace() {
        while (pos < input.length() && Character.isWhitespace(peek())) {
            advance();
        }
    }
    
    // Tokeniza identificadores e palavras-chave
    private Token tokenizeIdentifierOrKeyword() {
        int startPos = pos;
        int startCol = column;
        while (pos < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            advance();
        }
        String lexeme = input.substring(startPos, pos);
        TokenType type = isKeyword(lexeme) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        return new Token(type, lexeme, line, startCol);
    }
    
    // Verifica se o lexema está na lista de palavras reservadas
    private boolean isKeyword(String lexeme) {
        for (String kw : keywords) {
            if (kw.equals(lexeme)) {
                return true;
            }
        }
        return false;
    }
    
    // Tokeniza números (inteiros e decimais)
    private Token tokenizeNumber() {
        int startPos = pos;
        int startCol = column;
        boolean hasDot = false;
        while (pos < input.length() && (Character.isDigit(peek()) || (!hasDot && peek() == '.'))) {
            if (peek() == '.') {
                hasDot = true;
            }
            advance();
        }
        String lexeme = input.substring(startPos, pos);
        return new Token(TokenType.NUMBER, lexeme, line, startCol);
    }
    
    // Tokeniza literais de string
    private Token tokenizeString() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); // ignora a aspa de abertura
        while (pos < input.length() && peek() != '\"') {
            // Aqui pode-se tratar caracteres de escape, se necessário
            sb.append(advance());
        }
        advance(); // ignora a aspa de fechamento
        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, startCol);
    }
    
    // Tokeniza comentários de linha (// comentário)
    private Token tokenizeSingleLineComment() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); // consome '/'
        advance(); // consome segundo '/'
        while (pos < input.length() && peek() != '\n') {
            sb.append(advance());
        }
        return new Token(TokenType.COMMENT, sb.toString(), line, startCol);
    }
    
    // Tokeniza comentários multi-linha (/* comentário */)
    private Token tokenizeMultiLineComment() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); // consome '/'
        advance(); // consome '*'
        while (pos < input.length() && !(peek() == '*' && peekNext() == '/')) {
            sb.append(advance());
        }
        // Consome os caracteres de fechamento "*/"
        if (pos < input.length()) {
            advance(); // consome '*'
            advance(); // consome '/'
        }
        return new Token(TokenType.COMMENT, sb.toString(), line, startCol);
    }
    
    // Tokeniza operadores e separadores (ex.: ;, (, ), +, -, etc.)
    private Token tokenizeOperatorOrSeparator() {
        int startCol = column;
        char current = advance();
        String lexeme = String.valueOf(current);
        // Exemplo: para operadores compostos como ==, !=, <=, >=
        if ((current == '=' || current == '!' || current == '<' || current == '>') && peek() == '=') {
            lexeme += advance();
        }
        // Poderiam ser adicionados mais casos para operadores como "++", "--", "&&", "||", etc.
        return new Token(TokenType.OPERATOR, lexeme, line, startCol);
    }
}
