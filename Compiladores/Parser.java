// File: Compiladores/Parser.java
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private SymbolTable symbolTable;
    private final CodeGenerator codeGenerator;

    public Parser(List<Token> rawTokens, CodeGenerator codeGenerator) {
        this.tokens = new ArrayList<>();
        for (Token token : rawTokens) {
            // Ignora espaços em branco e comentários
            if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.COMMENT) {
                if (token.getType() == TokenType.UNKNOWN) {
                    System.err.println("Alerta do Lexer: Token DESCONHECIDO encontrado: " + token);
                }
                this.tokens.add(token);
            }
        }

        this.currentTokenIndex = 0;
        this.currentToken = this.tokens.isEmpty() ? null : this.tokens.get(0);
        this.symbolTable = new SymbolTable(null); // Tabela de símbolos global
        this.codeGenerator = codeGenerator;
    }

    /**
     * Retorna um token futuro na stream sem consumi-lo.
     */
    private Token peekAhead(int offset) {
        if (currentTokenIndex + offset < tokens.size()) {
            return tokens.get(currentTokenIndex + offset);
        }
        return null;
    }

    /**
     * Ponto de entrada da análise. Analisa todo o programa.
     */
    public void parseProgram() {
        while (currentToken != null) {
            if (!isTypeSpecifier(currentToken)) {
                throw new RuntimeException("Erro de Sintaxe: Token inesperado no nível superior: " + currentToken +
                                           ". Esperado um especificador de tipo (ex: 'int', 'float').");
            }

            // Espia o token que vem depois do identificador para decidir a regra
            Token afterIdentifier = peekAhead(2);

            if (afterIdentifier != null && afterIdentifier.getType() == TokenType.SEPARATOR && afterIdentifier.getLexeme().equals("(")) {
                // Se for um '(', é uma definição de função
                parseFunctionDefinition();
            } else {
                // Caso contrário, assume-se que é uma declaração de variável global
                parseGlobalVariableDeclaration();
            }
        }
        System.out.println("Análise sintática e semântica concluída com sucesso.");
    }
    
    /**
     * Analisa uma declaração de variável no escopo global.
     */
    private void parseGlobalVariableDeclaration() {
        Token type = parseTypeSpecifier();
        Token name = expect(TokenType.IDENTIFIER);

        // SEMANTIC: Adiciona à tabela de símbolos global
        if (!symbolTable.put(name.getLexeme(), new Symbol(name.getLexeme(), type.getType()))) {
            throw new RuntimeException("Erro Semântico: Símbolo global '" + name.getLexeme() + "' na linha " + name.getLine() + " já foi declarado.");
        }

        // CODEGEN: Gera o código para a variável global
        if (currentToken != null && currentToken.getType() == TokenType.OPERATOR && currentToken.getLexeme().equals("=")) {
            expect(TokenType.OPERATOR, "=");
            String expression = parseExpression();
            codeGenerator.generateGlobalVariableDeclarationWithAssignment(name.getLexeme(), type.getLexeme(), expression);
        } else {
            codeGenerator.generateGlobalVariableDeclaration(name.getLexeme(), type.getLexeme());
        }
        expect(TokenType.SEPARATOR, ";");
    }

    /**
     * Analisa a definição completa de uma função.
     */
    private void parseFunctionDefinition() {
        Token type = parseTypeSpecifier();
        Token name = expect(TokenType.IDENTIFIER);

        // SEMANTIC: Adiciona a função à tabela de símbolos global
        if (!symbolTable.put(name.getLexeme(), new Symbol(name.getLexeme(), type.getType()))) {
            throw new RuntimeException("Erro Semântico: Função '" + name.getLexeme() + "' na linha " + name.getLine() + " já foi definida.");
        }
        
        // CODEGEN: Inicia a geração do código da função
        codeGenerator.generateFunctionStart(name.getLexeme(), type.getLexeme());

        expect(TokenType.SEPARATOR, "(");
        
        // SEMANTIC: Cria um novo escopo para a função
        symbolTable = new SymbolTable(symbolTable);

        if (currentToken != null && !currentToken.getLexeme().equals(")")) {
            parseParameterList();
        }

        expect(TokenType.SEPARATOR, ")");
        parseBlockStatement();
        
        // CODEGEN: Finaliza o bloco da função
        codeGenerator.generateFunctionEnd();
        
        // SEMANTIC: Retorna ao escopo pai (global)
        symbolTable = symbolTable.getParent();
    }

    /**
     * Analisa um bloco de código delimitado por { ... }.
     */
    private void parseBlockStatement() {
        expect(TokenType.SEPARATOR, "{");
        while (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("}"))) {
            parseStatement();
        }
        expect(TokenType.SEPARATOR, "}");
    }

    /**
     * Analisa uma única instrução dentro de um bloco.
     */
    private void parseStatement() {
        if (isTypeSpecifier(currentToken)) {
            parseDeclarationStatement();
        } else if (currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equals("return")) {
            parseReturnStatement();
        } else if (currentToken.getType() == TokenType.IDENTIFIER) {
            // Olha à frente para decidir entre atribuição e chamada de função
            Token next = peekAhead(1);
            if (next != null && next.getType() == TokenType.OPERATOR && next.getLexeme().equals("=")) {
                parseAssignmentStatement();
            } else {
                parseExpressionStatement();
            }
        } else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals("{")) {
            // Escopo aninhado
            symbolTable = new SymbolTable(symbolTable);
            parseBlockStatement();
            symbolTable = symbolTable.getParent();
        } else if (currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(";")) { // Instrução vazia
            advance();
        } else {
            throw new RuntimeException("Erro de Sintaxe: Instrução inválida começando com: " + currentToken);
        }
    }

    /**
     * Analisa uma declaração de variável local.
     */
    private void parseDeclarationStatement() {
        Token type = parseTypeSpecifier();
        Token name = expect(TokenType.IDENTIFIER);

        if (!symbolTable.put(name.getLexeme(), new Symbol(name.getLexeme(), type.getType()))) {
            throw new RuntimeException("Erro Semântico: Variável '" + name.getLexeme() + "' na linha " + name.getLine() + " já foi declarada neste escopo.");
        }

        if (currentToken.getLexeme().equals("=")) {
            expect(TokenType.OPERATOR, "=");
            String expression = parseExpression();
            codeGenerator.generateVariableDeclarationWithAssignment(name.getLexeme(), type.getLexeme(), expression);
        } else {
            codeGenerator.generateVariableDeclaration(name.getLexeme(), type.getLexeme());
        }
        expect(TokenType.SEPARATOR, ";");
    }

    /**
     * Analisa uma instrução de atribuição.
     */
    private void parseAssignmentStatement() {
        Token name = expect(TokenType.IDENTIFIER);
        if (symbolTable.get(name.getLexeme()) == null) {
            throw new RuntimeException("Erro Semântico: Símbolo '" + name.getLexeme() + "' na linha " + name.getLine() + " não foi declarado.");
        }
        expect(TokenType.OPERATOR, "=");
        String expression = parseExpression();
        codeGenerator.generateAssignment(name.getLexeme(), expression);
        expect(TokenType.SEPARATOR, ";");
    }
    
    /**
     * Analisa uma instrução de expressão (ex: chamada de função).
     */
    private void parseExpressionStatement() {
        String expression = parseExpression();
        codeGenerator.generateExpressionStatement(expression);
        expect(TokenType.SEPARATOR, ";");
    }

    /**
     * Analisa uma instrução de retorno.
     */
    private void parseReturnStatement() {
        expect(TokenType.KEYWORD, "return");
        String expression = "0"; // Padrão para "return;"
        if (currentToken != null && !(currentToken.getType() == TokenType.SEPARATOR && currentToken.getLexeme().equals(";"))) {
            expression = parseExpression();
        }
        codeGenerator.generateReturnStatement(expression);
        expect(TokenType.SEPARATOR, ";");
    }

    /**
     * Analisa uma expressão.
     */
    private String parseExpression() {
        StringBuilder expressionBuilder = new StringBuilder();
        expressionBuilder.append(parseTerm());

        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR) {
            String lexeme = currentToken.getLexeme();
            if (lexeme.equals("+") || lexeme.equals("-") || lexeme.equals("*") || lexeme.equals("/")) {
                expressionBuilder.append(" ").append(lexeme).append(" ");
                advance();
                expressionBuilder.append(parseTerm());
            } else {
                break;
            }
        }
        return expressionBuilder.toString();
    }

    /**
     * Analisa um termo (componente básico de uma expressão).
     */
    private String parseTerm() {
        Token token = currentToken;
        if (token.getType() == TokenType.IDENTIFIER) {
            if (symbolTable.get(token.getLexeme()) == null) {
                 throw new RuntimeException("Erro Semântico: Símbolo '" + token.getLexeme() + "' na linha " + token.getLine() + " não foi declarado.");
            }
            advance();
            // Verifica se é uma chamada de função
            if (currentToken != null && currentToken.getLexeme().equals("(")) {
                StringBuilder functionCall = new StringBuilder();
                functionCall.append(token.getLexeme());
                functionCall.append("(");
                advance(); // Consome o '('
                if (!currentToken.getLexeme().equals(")")) {
                    functionCall.append(parseArgumentList());
                }
                expect(TokenType.SEPARATOR, ")");
                functionCall.append(")");
                return functionCall.toString();
            }
            return token.getLexeme(); // É apenas um identificador de variável
        } else if (token.getType() == TokenType.NUMBER) {
            advance();
            return token.getLexeme();
        } else if (token.getLexeme().equals("(")) {
            advance();
            String expression = parseExpression();
            expect(TokenType.SEPARATOR, ")");
            return "(" + expression + ")";
        }
        throw new RuntimeException("Erro de Sintaxe: Termo inválido na expressão: " + token);
    }
    
    // --- Métodos de Apoio para Listas e Tipos ---

    private void parseParameterList() {
        parseParameter();
        while (currentToken != null && currentToken.getLexeme().equals(",")) {
            advance();
            parseParameter();
        }
    }

    private void parseParameter() {
        Token paramType = parseTypeSpecifier();
        Token paramName = expect(TokenType.IDENTIFIER);
        if (!symbolTable.put(paramName.getLexeme(), new Symbol(paramName.getLexeme(), paramType.getType()))) {
            throw new RuntimeException("Erro Semântico: Parâmetro '" + paramName.getLexeme() + "' já definido.");
        }
    }

    private String parseArgumentList() {
        StringBuilder args = new StringBuilder();
        args.append(parseExpression());
        while (currentToken != null && currentToken.getLexeme().equals(",")) {
            advance();
            args.append(", ").append(parseExpression());
        }
        return args.toString();
    }
    
    private boolean isTypeSpecifier(Token token) {
        if (token == null || token.getType() != TokenType.KEYWORD) return false;
        String lexeme = token.getLexeme();
        return lexeme.equals("int") || lexeme.equals("float") || lexeme.equals("void") ||
               lexeme.equals("char") || lexeme.equals("double") || lexeme.equals("static");
    }

    private Token parseTypeSpecifier() {
        if (currentToken != null && isTypeSpecifier(currentToken)) {
            Token typeToken = currentToken;
            advance();
            return typeToken;
        }
        throw new RuntimeException("Erro de Sintaxe: Esperado um especificador de tipo, mas foi: " + currentToken);
    }

    // --- Métodos Utilitários ---
    
    private void advance() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        } else {
            currentToken = null; // Fim dos tokens
        }
    }

    private Token expect(TokenType expectedType) {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada. Esperado " + expectedType);
        }
        Token token = currentToken;
        if (token.getType() == expectedType) {
            advance();
            return token;
        }
        throw new RuntimeException("Erro de Sintaxe: Token inesperado na linha " + token.getLine() + ". Esperado " + expectedType + " mas foi " + token.getType());
    }

    private Token expect(TokenType expectedType, String lexeme) {
        if (currentToken == null) {
            throw new RuntimeException("Erro de Sintaxe: Fim inesperado da entrada. Esperado '" + lexeme + "'");
        }
        Token token = currentToken;
        if (token.getType() == expectedType && token.getLexeme().equals(lexeme)) {
            advance();
            return token;
        }
        throw new RuntimeException("Erro de Sintaxe: Token inesperado na linha " + token.getLine() + ". Esperado '" + lexeme + "' mas foi '" + token.getLexeme() + "'");
    }
}