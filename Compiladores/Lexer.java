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

    // Metodo principal que gera a lista de tokens
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < input.length()) {
            char current = peek();

            // Ignora espaços em branco
            if (Character.isWhitespace(current)) {
                consumeWhitespace();
                continue;
            }

            // Identificadores ou palavras-chave
            if (Character.isLetter(current) || current == '_') {
                tokens.add(tokenizeIdentifierOrKeyword());
                continue;
            }

            // Números
            if (Character.isDigit(current)) {
                tokens.add(tokenizeNumber());
                continue;
            }

            // Literais de string
            if (current == '\"') {
                tokens.add(tokenizeString());
                continue;
            }

            // Comentários
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

    // Retorna o caractere atual sem avançar a posicao
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

    // Token para numeros
    private Token tokenizeNumber() {
        int startPos = pos;
        int startCol = column;
        boolean hasDot = false;
        while (pos < input.length() && (Character.isDigit(peek()) || (!hasDot && peek() == '.'))) {
            if (peek() == '.') {

                if (pos + 1 < input.length() && Character.isDigit(peekNext())) {
                    hasDot = true;
                } else {

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
        int startLine = line;
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance();
        while (pos < input.length() && peek() != '\"') {
            char currentChar = advance();
            if (currentChar == '\\') {
                if (pos < input.length()) {
                    char escapedChar = advance();
                    switch (escapedChar) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case '\"': sb.append('\"'); break;
                        case '\\': sb.append('\\'); break;
                        default:
                            sb.append('\\').append(escapedChar); 
                            break;
                    }
                } else {
                    sb.append('\\');
                }
            } else {
                sb.append(currentChar);
            }
        }
        if (pos < input.length() && peek() == '\"') {
            advance(); 
        } else {
        }
        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    // Token para comentários de linha
    private Token tokenizeSingleLineComment() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance(); 
        advance();
        while (pos < input.length() && peek() != '\n') {
            sb.append(advance());
        }
        return new Token(TokenType.COMMENT, sb.toString().trim(), line, startCol);
    }

    // Token para comentários multi-linha
    private Token tokenizeMultiLineComment() {
        int startLine = line; 
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        advance();
        advance();
        while (pos < input.length()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return new Token(TokenType.COMMENT, sb.toString().trim(), startLine, startCol);
            }
            sb.append(advance());
        }

        return new Token(TokenType.COMMENT, sb.toString().trim(), startLine, startCol); //TokenType.UNKNOWN
    }

    // Tokeniza operadores, separadores ou marca como UNKNOWN
    private Token tokenizeOperatorSeparatorOrUnknown() {
        int startCol = column;
        char current = peek();

        String lexeme;
        TokenType type;

        if (pos + 1 < input.length()) {
            String twoCharOp = input.substring(pos, pos + 2);
            switch (twoCharOp) {
                case "==":
                case "!=":
                case "<=":
                case ">=":
                    advance();
                    advance();
                    lexeme = twoCharOp;
                    type = TokenType.OPERATOR;
                    return new Token(type, lexeme, line, startCol);
            }
        }

        advance();
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
            case '!':
            case '&':
            case '|':
            case '^': 
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