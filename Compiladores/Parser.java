// File: Compiladores/Parser.java
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;

    public Parser(List<Token> rawTokens) {
        this.tokens = new ArrayList<>();
        // Filtra WHITESPACE e COMMENT, e alerta sobre UNKNOWN
        for (Token token : rawTokens) {
            if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.COMMENT) { //
                if (token.getType() == TokenType.UNKNOWN) { //
                    System.err.println("Alerta do Lexer: Token DESCONHECIDO encontrado: " + token +
                                       ". Isso provavelmente causará um erro de parsing.");
                }
                this.tokens.add(token);
            }
        }

        this.currentTokenIndex = 0;
        this.currentToken = this.tokens.isEmpty() ? null : this.tokens.get(0);

        if (this.currentToken == null && !rawTokens.isEmpty() && this.tokens.isEmpty()) {
            // System.out.println("Analisando um programa vazio (todos os tokens eram espaço em branco ou comentários).");
        }
    }

    private void advance() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        } else {
            currentToken = null; // Fim dos tokens
        }
    }

    private Token expect(TokenType expectedType) { //
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada. Esperado " + expectedType);
        }
        Token token = currentToken; //
        if (token.getType() == expectedType) { //
            advance();
            return token;
        } else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado na linha " + token.getLine() + ", col " + token.getColumn() + //
                                       ". Esperado " + expectedType + " mas foi " + token.getType() + " ('" + token.getLexeme() + "')"); //
        }
    }

    private Token expect(TokenType expectedType, String lexeme) { //
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada. Esperado " + expectedType + " '" + lexeme + "'");
        }
        Token token = currentToken; //
        if (token.getType() == expectedType && token.getLexeme().equals(lexeme)) { //
            advance();
            return token;
        } else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado na linha " + token.getLine() + ", col " + token.getColumn() + //
                                       ". Esperado " + expectedType + " '" + lexeme + "' mas foi " +
                                       token.getType() + " ('" + token.getLexeme() + "')"); //
        }
    }

    public void parseProgram() {
        boolean parsedAtLeastOneTopLevelConstruct = false;
        while (currentToken != null && currentToken.getType() != TokenType.UNKNOWN) { //
            if (isTypeSpecifier(currentToken)) {
                parseFunctionDefinition();
                parsedAtLeastOneTopLevelConstruct = true;
            } else {
                throw new RuntimeException("Erro de Sintaxe: Token inesperado no nível superior: " + currentToken +
                                           ". Esperado um especificador de tipo (ex: 'int', 'float') para iniciar uma função ou declaração global.");
            }
        }

        if (currentToken != null && currentToken.getType() == TokenType.UNKNOWN) { //
             throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado após construções válidas: " + currentToken);
        }
        
        if (!parsedAtLeastOneTopLevelConstruct && tokens.isEmpty()){
             System.out.println("Análise sintática concluída: Programa está vazio ou contém apenas comentários/espaços em branco.");
             return; 
        } else if (!parsedAtLeastOneTopLevelConstruct && !tokens.isEmpty()) {
             if (currentToken != null) { 
                 throw new RuntimeException("Erro de Sintaxe: Token(s) remanescentes encontrados após o fim esperado do programa, começando com: " + currentToken);
            } else {
                 throw new RuntimeException("Erro de Sintaxe: Tokens não consumidos no final do programa, mas nenhuma construção principal foi identificada.");
            }
        }
        
        System.out.println("Análise sintática concluída com sucesso.");
    }

    private boolean isTypeSpecifier(Token token) { //
        if (token == null || token.getType() != TokenType.KEYWORD) return false; //
        String lexeme = token.getLexeme(); //
        // Palavras-chave relevantes de Lexer.java
        return lexeme.equals("int") || lexeme.equals("float") || lexeme.equals("void") ||
               lexeme.equals("char") || lexeme.equals("double") || lexeme.equals("static");
    }

    private void parseTypeSpecifier() {
        if (currentToken != null && isTypeSpecifier(currentToken)) {
            advance();
        } else {
            String got = currentToken != null ? currentToken.toString() : "fim da entrada";
            throw new RuntimeException("Erro de Sintaxe: Esperado especificador de tipo (ex: int, float) mas foi: " + got);
        }
    }

    private void parseFunctionDefinition() {
        parseTypeSpecifier();
        expect(TokenType.IDENTIFIER); // Nome da Função //
        expect(TokenType.SEPARATOR, "("); //

        if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(")"))) { //
            parseParameterList();
        }

        expect(TokenType.SEPARATOR, ")"); //
        parseBlockStatement();
    }

    private void parseParameterList() {
        parseParameter();
        while (currentToken != null && currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(",")) { //
            expect(TokenType.SEPARATOR, ","); //
            parseParameter();
        }
    }

    private void parseParameter() {
        parseTypeSpecifier();
        expect(TokenType.IDENTIFIER); // Nome do Parâmetro //
    }

    private void parseBlockStatement() {
        expect(TokenType.SEPARATOR, "{"); //
        while (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("}"))) { //
            if (currentToken.getType() == TokenType.UNKNOWN) { //
                 throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado dentro de um bloco: " + currentToken);
            }
            parseStatement();
        }
        expect(TokenType.SEPARATOR, "}"); //
    }

    // --- MÉTODO parseStatement MODIFICADO ---
    private void parseStatement() {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada, esperada uma instrução.");
        }
         if (currentToken.getType() == TokenType.UNKNOWN) { //
            throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado onde uma instrução era esperada: " + currentToken);
        }

        if (isTypeSpecifier(currentToken)) {
            parseDeclarationStatement();
        } else if (currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equals("return")) { //
            parseReturnStatement();
        } else if (currentToken.getType() == TokenType.IDENTIFIER) { //
            // Lookahead para distinguir entre atribuição (x = ...) e chamada de função/expressão (func(...))
            // Token idToken = currentToken; // Não precisamos guardar o idToken aqui se não formos usá-lo diretamente
            Token nextToken = (currentTokenIndex + 1 < tokens.size()) ? tokens.get(currentTokenIndex + 1) : null;

            if (nextToken != null && nextToken.getType() == TokenType.OPERATOR && nextToken.getLexeme().equals("=")) { //
                // É uma instrução de atribuição: IDENTIFIER = expression;
                expect(TokenType.IDENTIFIER); // Consome o IDENTIFIER //
                expect(TokenType.OPERATOR, "="); // Consome o '=' //
                parseExpression(); // Analisa a expressão à direita
                expect(TokenType.SEPARATOR, ";"); //
            } else {
                // Caso contrário, assume-se que seja uma chamada de função ou uma expressão simples
                // que termina em ';'
                parseExpressionStatement();
            }
        } else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("{")) { //
            parseBlockStatement();
        } else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(";")) { // Instrução vazia //
            expect(TokenType.SEPARATOR, ";"); //
        } else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado no início da instrução: " + currentToken);
        }
    }

    private void parseDeclarationStatement() {
        parseTypeSpecifier();
        expect(TokenType.IDENTIFIER); // Nome da Variável //
        if (currentToken != null && currentToken.getType() == TokenType.OPERATOR && currentToken.getLexeme().equals("=")) { //
            expect(TokenType.OPERATOR, "="); //
            parseExpression();
        }
        expect(TokenType.SEPARATOR, ";"); //
    }

    private void parseReturnStatement() {
        expect(TokenType.KEYWORD, "return"); //
        if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(";"))) { //
            parseExpression();
        }
        expect(TokenType.SEPARATOR, ";"); //
    }

    private void parseExpressionStatement() {
        parseExpression();
        expect(TokenType.SEPARATOR, ";"); //
    }

    // --- NOVO MÉTODO parseTerm ---
    private void parseTerm() {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada, esperado um termo na expressão.");
        }
        if (currentToken.getType() == TokenType.UNKNOWN) { //
            throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado onde um termo era esperado: " + currentToken);
        }

        if (currentToken.getType() == TokenType.IDENTIFIER) { //
            // Token idToken = currentToken; // Não precisamos guardar, apenas consumir ou verificar próximo
            advance(); // Consome o IDENTIFIER

            // Verifica se é uma chamada de função: IDENTIFIER ( argumentos )
            if (currentToken != null && currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("(")) { //
                expect(TokenType.SEPARATOR, "("); // Consome '(' //
                if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(")"))) { //
                    parseArgumentList(); 
                }
                expect(TokenType.SEPARATOR, ")"); // Consome ')' //
            }
            // Se não for uma chamada de função, o IDENTIFIER já foi consumido e atua como um termo.
        } else if (currentToken.getType() == TokenType.NUMBER) { //
            expect(TokenType.NUMBER); //
        } else if (currentToken.getType() == TokenType.STRING_LITERAL) { //
            expect(TokenType.STRING_LITERAL); //
        }
        // Para suportar expressões parentesizadas (ex: (5 + 10) * 2):
        // else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("(")) {
        //    expect(TokenType.SEPARATOR, "(");
        //    parseExpression(); // Analisa a expressão dentro dos parênteses
        //    expect(TokenType.SEPARATOR, ")");
        // }
        else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado no termo da expressão: " + currentToken +
                                       ". Esperado IDENTIFICADOR, NÚMERO, LITERAL_STRING ou '(' para expressão parentesizada.");
        }
    }

    // --- MÉTODO parseExpression MODIFICADO ---
    private void parseExpression() {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada, esperada uma expressão.");
        }
        if (currentToken.getType() == TokenType.UNKNOWN) { //
            throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado na expressão: " + currentToken);
        }

        parseTerm(); // Analisa o primeiro termo

        // Loop para operadores binários simples (sem precedência complexa ou associatividade)
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR) { //
            String lexeme = currentToken.getLexeme(); //
            // Suporta apenas alguns operadores aritméticos básicos por enquanto
            if (lexeme.equals("+") || lexeme.equals("-") || lexeme.equals("*") || lexeme.equals("/")) {
                advance(); // Consome o operador
                parseTerm(); // Analisa o próximo termo
            } else {
                // Se não for um dos operadores aritméticos esperados para continuar a expressão, interrompe.
                // Outros operadores como '=', '<', '==' seriam tratados por regras de nível superior
                // ou por uma gramática de expressão mais elaborada.
                break;
            }
        }
    }

    private void parseArgumentList() {
        parseExpression(); // Primeiro argumento
        while (currentToken != null && currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(",")) { //
            expect(TokenType.SEPARATOR, ","); //
            parseExpression(); // Próximos argumentos
        }
    }
}