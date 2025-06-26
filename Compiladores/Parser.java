// File: Compiladores/Parser.java
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private SymbolTable symbolTable; // SEMANTIC: Tabela de símbolos

    public Parser(List<Token> rawTokens) {
        this.tokens = new ArrayList<>();
        for (Token token : rawTokens) {
            // Ignora espaços em branco e comentários, mas alerta sobre tokens desconhecidos.
            if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.COMMENT) {
                if (token.getType() == TokenType.UNKNOWN) {
                    System.err.println("Alerta do Lexer: Token DESCONHECIDO encontrado: " + token +
                                       ". Isso provavelmente causará um erro de parsing.");
                }
                this.tokens.add(token);
            }
        }

        this.currentTokenIndex = 0;
        this.currentToken = this.tokens.isEmpty() ? null : this.tokens.get(0);
        this.symbolTable = new SymbolTable(null); // SEMANTIC: Inicializa a tabela de símbolos global

        if (this.currentToken == null && !rawTokens.isEmpty() && this.tokens.isEmpty()) {
            System.out.println("Analisando um programa vazio (todos os tokens eram espaço em branco ou comentários).");
        }
    }

    // Avança para o próximo token da lista.
    private void advance() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        } else {
            currentToken = null; // Fim dos tokens
        }
    }

    // Consome o token atual se ele for do tipo esperado.
    private Token expect(TokenType expectedType) {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada. Esperado " + expectedType);
        }
        Token token = currentToken;
        if (token.getType() == expectedType) {
            advance();
            return token;
        } else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado na linha " + token.getLine() + ", col " + token.getColumn() +
                                       ". Esperado " + expectedType + " mas foi " + token.getType() + " ('" + token.getLexeme() + "')");
        }
    }

    // Consome o token atual se ele for do tipo e lexema esperados.
    private Token expect(TokenType expectedType, String lexeme) {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada. Esperado " + expectedType + " '" + lexeme + "'");
        }
        Token token = currentToken;
        if (token.getType() == expectedType && token.getLexeme().equals(lexeme)) {
            advance();
            return token;
        } else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado na linha " + token.getLine() + ", col " + token.getColumn() +
                                       ". Esperado " + expectedType + " '" + lexeme + "' mas foi " +
                                       token.getType() + " ('" + token.getLexeme() + "')");
        }
    }

    // Inicia a análise do programa.
    public void parseProgram() {
        boolean parsedAtLeastOneTopLevelConstruct = false;
        while (currentToken != null && currentToken.getType() != TokenType.UNKNOWN) {
            if (isTypeSpecifier(currentToken)) {
                parseFunctionDefinition();
                parsedAtLeastOneTopLevelConstruct = true;
            } else {
                throw new RuntimeException("Erro de Sintaxe: Token inesperado no nível superior: " + currentToken +
                                           ". Esperado um especificador de tipo (ex: 'int', 'float') para iniciar uma função ou declaração global.");
            }
        }

        if (currentToken != null && currentToken.getType() == TokenType.UNKNOWN) {
             throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado após construções válidas: " + currentToken);
        }
        
        if (!parsedAtLeastOneTopLevelConstruct && tokens.isEmpty()){
             System.out.println("Análise sintática e semântica concluída: Programa está vazio ou contém apenas comentários/espaços em branco.");
             return; 
        } else if (!parsedAtLeastOneTopLevelConstruct && !tokens.isEmpty()) {
             if (currentToken != null) { 
                 throw new RuntimeException("Erro de Sintaxe: Token(s) remanescentes encontrados após o fim esperado do programa, começando com: " + currentToken);
            } else {
                 throw new RuntimeException("Erro de Sintaxe: Tokens não consumidos no final do programa, mas nenhuma construção principal foi identificada.");
            }
        }
        
        System.out.println("Análise sintática e semântica concluída com sucesso.");
    }

    // Verifica se um token é um especificador de tipo.
    private boolean isTypeSpecifier(Token token) {
        if (token == null || token.getType() != TokenType.KEYWORD) return false;
        String lexeme = token.getLexeme();
        return lexeme.equals("int") || lexeme.equals("float") || lexeme.equals("void") ||
               lexeme.equals("char") || lexeme.equals("double") || lexeme.equals("static");
    }

    // Analisa um especificador de tipo.
    private Token parseTypeSpecifier() {
        if (currentToken != null && isTypeSpecifier(currentToken)) {
            Token typeToken = currentToken;
            advance();
            return typeToken;
        } else {
            String got = currentToken != null ? currentToken.toString() : "fim da entrada";
            throw new RuntimeException("Erro de Sintaxe: Esperado especificador de tipo (ex: int, float) mas foi: " + got);
        }
    }

    // Analisa a definição de uma função.
    private void parseFunctionDefinition() {
        Token type = parseTypeSpecifier();
        Token name = expect(TokenType.IDENTIFIER);

        // SEMANTIC: Adiciona a função à tabela de símbolos global.
        if (!symbolTable.put(name.getLexeme(), new Symbol(name.getLexeme(), type.getType()))) {
            throw new RuntimeException("Erro Semântico: Função '" + name.getLexeme() + "' na linha " + name.getLine() + " já foi definida.");
        }

        expect(TokenType.SEPARATOR, "(");
        
        // SEMANTIC: Cria um novo escopo (tabela de símbolos filha) para os parâmetros e corpo da função.
        symbolTable = new SymbolTable(symbolTable);

        if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(")"))) {
            parseParameterList();
        }

        expect(TokenType.SEPARATOR, ")");
        parseBlockStatement();

        // SEMANTIC: Restaura o escopo anterior ao sair da função.
        symbolTable = symbolTable.getParent();
    }

    // Analisa a lista de parâmetros de uma função.
    private void parseParameterList() {
        parseParameter();
        while (currentToken != null && currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(",")) {
            expect(TokenType.SEPARATOR, ",");
            parseParameter();
        }
    }

    // Analisa um único parâmetro.
    private void parseParameter() {
        Token paramType = parseTypeSpecifier();
        Token paramName = expect(TokenType.IDENTIFIER);

        // SEMANTIC: Adiciona o parâmetro à tabela de símbolos do escopo atual.
        if (!symbolTable.put(paramName.getLexeme(), new Symbol(paramName.getLexeme(), paramType.getType()))) {
            throw new RuntimeException("Erro Semântico: Parâmetro '" + paramName.getLexeme() + "' na linha " + paramName.getLine() + " já foi definido nesta função.");
        }
    }

    // Analisa um bloco de código delimitado por { ... }.
    private void parseBlockStatement() {
        expect(TokenType.SEPARATOR, "{");
        while (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("}"))) {
            if (currentToken.getType() == TokenType.UNKNOWN) {
                 throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado dentro de um bloco: " + currentToken);
            }
            parseStatement();
        }
        expect(TokenType.SEPARATOR, "}");
    }

    // Analisa uma instrução.
    private void parseStatement() {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada, esperada uma instrução.");
        }
         if (currentToken.getType() == TokenType.UNKNOWN) {
            throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado onde uma instrução era esperada: " + currentToken);
        }

        if (isTypeSpecifier(currentToken)) {
            parseDeclarationStatement();
        } else if (currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equals("return")) {
            parseReturnStatement();
        } else if (currentToken.getType() == TokenType.IDENTIFIER) {
            Token identifierToken = currentToken;
            // SEMANTIC: Verifica se o símbolo existe antes de usá-lo.
            Symbol symbol = symbolTable.get(identifierToken.getLexeme());
            if (symbol == null) {
                throw new RuntimeException("Erro Semântico: Símbolo '" + identifierToken.getLexeme() + "' na linha " + identifierToken.getLine() + " não foi declarado.");
            }

            Token nextToken = (currentTokenIndex + 1 < tokens.size()) ? tokens.get(currentTokenIndex + 1) : null;

            if (nextToken != null && nextToken.getType() == TokenType.OPERATOR && nextToken.getLexeme().equals("=")) {
                // É uma atribuição, como `x = 5;`
                parseAssignmentStatement();
            } else {
                // É uma chamada de função ou outra expressão.
                parseExpressionStatement();
            }
        } else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("{")) {
            parseBlockStatement();
        } else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(";")) { // Instrução vazia
            advance();
        } else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado no início da instrução: " + currentToken);
        }
    }

    // Analisa uma declaração de variável.
    private void parseDeclarationStatement() {
        Token type = parseTypeSpecifier();
        Token name = expect(TokenType.IDENTIFIER);

        // SEMANTIC: Adiciona o novo símbolo à tabela de símbolos do escopo atual.
        if (!symbolTable.put(name.getLexeme(), new Symbol(name.getLexeme(), type.getType()))) {
            throw new RuntimeException("Erro Semântico: Variável '" + name.getLexeme() + "' na linha " + name.getLine() + " já foi declarada neste escopo.");
        }

        if (currentToken != null && currentToken.getType() == TokenType.OPERATOR && currentToken.getLexeme().equals("=")) {
            expect(TokenType.OPERATOR, "=");
            parseExpression();
            // Aqui seria o local para uma verificação de tipo da inicialização.
        }
        expect(TokenType.SEPARATOR, ";");
    }
    
    // Analisa uma instrução de atribuição.
    private void parseAssignmentStatement() {
        Token name = expect(TokenType.IDENTIFIER); // O símbolo já foi verificado em parseStatement
        expect(TokenType.OPERATOR, "=");
        parseExpression();
        expect(TokenType.SEPARATOR, ";");
        // Aqui seria o local para uma verificação de tipo entre a variável e a expressão.
    }

    // Analisa uma instrução de retorno.
    private void parseReturnStatement() {
        expect(TokenType.KEYWORD, "return");
        if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(";"))) {
            parseExpression();
            // Aqui seria o local para verificar se o tipo da expressão de retorno é compatível com o tipo de retorno da função.
        }
        expect(TokenType.SEPARATOR, ";");
    }

    // Analisa uma instrução de expressão (ex: chamada de função).
    private void parseExpressionStatement() {
        parseExpression();
        expect(TokenType.SEPARATOR, ";");
    }

    // Analisa um termo (o componente básico de uma expressão).
    private void parseTerm() {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada, esperado um termo na expressão.");
        }
        if (currentToken.getType() == TokenType.UNKNOWN) {
            throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado onde um termo era esperado: " + currentToken);
        }

        if (currentToken.getType() == TokenType.IDENTIFIER) { 
            Token idToken = currentToken;
            // SEMANTIC: Verifica se a variável/função foi declarada.
            if (symbolTable.get(idToken.getLexeme()) == null) {
                 throw new RuntimeException("Erro Semântico: Símbolo '" + idToken.getLexeme() + "' na linha " + idToken.getLine() + " não foi declarado.");
            }
            advance();

            // Verifica se é uma chamada de função.
            if (currentToken != null && currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("(")) {
                expect(TokenType.SEPARATOR, "(");
                if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(")"))) {
                    parseArgumentList(); 
                }
                expect(TokenType.SEPARATOR, ")");
            }
        } else if (currentToken.getType() == TokenType.NUMBER) {
            expect(TokenType.NUMBER);
        } else if (currentToken.getType() == TokenType.STRING_LITERAL) {
            expect(TokenType.STRING_LITERAL);
        }
        // Expressões parentesizadas (ex: (5 + 10) * 2):
        else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("(")) {
            expect(TokenType.SEPARATOR, "(");
            parseExpression();
            expect(TokenType.SEPARATOR, ")");
        }
        else {
            throw new RuntimeException("Erro de Sintaxe: Token inesperado no termo da expressão: " + currentToken +
                                       ". Esperado IDENTIFICADOR, NÚMERO, LITERAL_STRING ou '(' para expressão parentesizada.");
        }
    }

    // Analisa uma expressão.
    private void parseExpression() {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada, esperada uma expressão.");
        }
        if (currentToken.getType() == TokenType.UNKNOWN) {
            throw new RuntimeException("Erro de Sintaxe: Token DESCONHECIDO encontrado na expressão: " + currentToken);
        }

        parseTerm();

        // Continua analisando operadores binários.
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR) {
            String lexeme = currentToken.getLexeme();
            if (lexeme.equals("+") || lexeme.equals("-") || lexeme.equals("*") || lexeme.equals("/")) {
                advance();
                parseTerm();
            } else {
                // Se não for um operador de expressão simples, para.
                break;
            }
        }
    }

    // Analisa a lista de argumentos em uma chamada de função.
    private void parseArgumentList() {
        parseExpression();
        while (currentToken != null && currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(",")) {
            expect(TokenType.SEPARATOR, ",");
            parseExpression();
        }
    }
}