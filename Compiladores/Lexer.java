import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private String input;
    private int pos;
    private int line;
    private int column;

    // Lista de palavras reservada
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

            // Ignora espaços em branco
            if (Character.isWhitespace(current)) {
                consumeWhitespace();
                continue;
            }

            // Identificadores ou palavras-chave: começam com letra ou sublinhado
            if (Character.isLetter(current) || current == '_') {
                tokens.add(tokenizeIdentifierOrKeyword());
                continue;
            }

            // Números: dígitos
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

            // Operadores, separadores ou tokens desconhecidos
            tokens.add(tokenizeOperatorSeparatorOrUnknown());
        }

        return tokens;
    }

    // Retorna o caractere atual sem avançar a posição
    private char peek() {
        return input.charAt(pos);
    }

    // Retorna o próximo caractere
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

    // Token para identificadores e palavras-chave
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

    // Token para números
    private Token tokenizeNumber() {
        int startPos = pos;
        int startCol = column;
        boolean hasDot = false;
        while (pos < input.length() && (Character.isDigit(peek()) || (!hasDot && peek() == '.'))) {
            if (peek() == '.') {
                 // Certifica-se de que o ponto é seguido por um dígito para ser um decimal
                 // e que não seja o último caractere da entrada.
                if (pos + 1 < input.length() && Character.isDigit(peekNext())) {
                    hasDot = true;
                } else {
                    // Se o ponto não for seguido por um dígito, ele não faz parte deste número.
                    // Pode ser um operador de acesso a membro ou um erro, dependendo da linguagem.
                    // Para este lexer, paramos de consumir o número aqui.
                    break;
                }
            }
            advance();
        }
        String lexeme = input.substring(startPos, pos);
        return new Token(TokenType.NUMBER, lexeme, line, startCol);
    }

    // Token para literais de string
    private Token tokenizeString() {
        int startLine = line; // Guarda a linha inicial caso a string se estenda por múltiplas linhas
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); // ignora a aspa de abertura
        while (pos < input.length() && peek() != '\"') {
            char currentChar = advance();
            if (currentChar == '\\') { // Trata caracteres de escape simples
                if (pos < input.length()) {
                    char escapedChar = advance(); // Consome o caractere após a barra invertida
                    switch (escapedChar) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case '\"': sb.append('\"'); break;
                        case '\\': sb.append('\\'); break;
                        // Adicionar outros escapes se necessário
                        default:
                            sb.append('\\').append(escapedChar); // Mantém a sequência de escape não reconhecida
                            break;
                    }
                } else {
                    sb.append('\\'); // Barra invertida no final da string (pode ser erro)
                }
            } else {
                sb.append(currentChar);
            }
        }
        if (pos < input.length() && peek() == '\"') {
            advance(); // ignora a aspa de fechamento
        } else {
            // String não terminada - poderia lançar um erro ou criar um token de erro.
            // System.err.println("Warning: Unterminated string literal at line " + startLine + ", col " + startCol);
        }
        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    // Token para comentários de linha (// comentário)
    private Token tokenizeSingleLineComment() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); // consome '/'
        advance(); // consome segundo '/'
        while (pos < input.length() && peek() != '\n') {
            sb.append(advance());
        }
        // Não avança sobre o '\n' aqui, para que a contagem de linhas seja tratada corretamente por advance()
        return new Token(TokenType.COMMENT, sb.toString().trim(), line, startCol);
    }

    // Token para comentários multi-linha (/* comentário */)
    private Token tokenizeMultiLineComment() {
        int startLine = line; // Guarda a linha inicial do comentário
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); // consome '/'
        advance(); // consome '*'
        while (pos < input.length()) {
            if (peek() == '*' && peekNext() == '/') {
                advance(); // consome '*'
                advance(); // consome '/'
                return new Token(TokenType.COMMENT, sb.toString().trim(), startLine, startCol);
            }
            sb.append(advance());
        }
        // Comentário multilinha não terminado - poderia lançar um erro.
        // System.err.println("Warning: Unterminated multi-line comment starting at line " + startLine + ", col " + startCol);
        return new Token(TokenType.COMMENT, sb.toString().trim(), startLine, startCol); // Ou TokenType.UNKNOWN
    }

    // Tokeniza operadores, separadores ou marca como UNKNOWN
    private Token tokenizeOperatorSeparatorOrUnknown() {
        int startCol = column;
        char current = peek();

        String lexeme;
        TokenType type;

        // Verifica operadores de múltiplos caracteres primeiro
        if (pos + 1 < input.length()) {
            String twoCharOp = input.substring(pos, pos + 2);
            switch (twoCharOp) {
                case "==":
                case "!=":
                case "<=":
                case ">=":
                // Adicione outros operadores de múltiplos caracteres como "&&", "||" se necessário
                    advance(); // consome o primeiro char
                    advance(); // consome o segundo char
                    lexeme = twoCharOp;
                    type = TokenType.OPERATOR;
                    return new Token(type, lexeme, line, startCol);
            }
        }

        // Operadores ou separadores de um único caractere
        advance(); // Consome o caractere
        lexeme = String.valueOf(current);

        switch (current) {
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '=':
            case '<':
            case '>':
            case '!': // (ex: NOT lógico)
            case '&': // (ex: AND bit-a-bit, endereço-de)
            case '|': // (ex: OR bit-a-bit)
            case '^': // (ex: XOR bit-a-bit)
                type = TokenType.OPERATOR;
                break;
            case '(':
            case ')':
            case '{':
            case '}':
            case '[':
            case ']':
            case ';':
            case ',':
            case '.':
                type = TokenType.SEPARATOR;
                break;
            default:
                type = TokenType.UNKNOWN;
                break;
        }
        return new Token(type, lexeme, line, startCol);
    }
}