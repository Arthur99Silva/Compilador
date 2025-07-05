// Compiladores/CodeGenerator.java
public class CodeGenerator {
    private final StringBuilder code;
    private final StringBuilder globals; // Para variáveis globais

    public CodeGenerator() {
        this.code = new StringBuilder();
        this.globals = new StringBuilder();
        
        // Inclui os cabeçalhos C necessários
        code.append("#include <stdio.h>\n\n");
    }

    /**
     * Retorna o código final, juntando as globais com o resto do código.
     */
    public String getGeneratedCode() {
        return globals.toString() + "\n" + code.toString();
    }
    
    /**
     * Gera a declaração de uma variável global.
     * Ex: int x;
     */
    public void generateGlobalVariableDeclaration(String varName, String varType) {
        String cType = convertTypeToC(varType);
        globals.append(cType).append(" ").append(varName).append(";\n");
    }

    /**
     * Gera a declaração de uma variável global com atribuição.
     * Ex: int x = 10;
     */
    public void generateGlobalVariableDeclarationWithAssignment(String varName, String varType, String expression) {
        String cType = convertTypeToC(varType);
        globals.append(cType).append(" ").append(varName).append(" = ").append(expression).append(";\n");
    }

    /**
     * Gera uma instrução de expressão (ex: uma chamada de função isolada).
     * Ex: inicializa();
     */
    public void generateExpressionStatement(String expression) {
        code.append("    ").append(expression).append(";\n");
    }

    /**
     * Gera o início de uma função em C.
     */
    public void generateFunctionStart(String functionName, String returnType) {
        String cType = convertTypeToC(returnType);
        // Simplificado para não lidar com parâmetros por enquanto
        code.append(cType).append(" ").append(functionName).append("() {\n");
    }

    /**
     * Gera o final de uma função em C.
     */
    public void generateFunctionEnd() {
        code.append("}\n\n");
    }

    /**
     * Gera a declaração de uma variável local em C.
     */
    public void generateVariableDeclaration(String varName, String varType) {
        String cType = convertTypeToC(varType);
        code.append("    ").append(cType).append(" ").append(varName).append(";\n");
    }

    /**
     * Gera a declaração de uma variável local com atribuição.
     */
    public void generateVariableDeclarationWithAssignment(String varName, String varType, String expression) {
        String cType = convertTypeToC(varType);
        code.append("    ").append(cType).append(" ").append(varName).append(" = ").append(expression).append(";\n");
    }

    /**
     * Gera uma atribuição em C.
     */
    public void generateAssignment(String varName, String expression) {
        code.append("    ").append(varName).append(" = ").append(expression).append(";\n");
    }
    
    /**
     * Gera um comando de retorno em C.
     */
    public void generateReturnStatement(String expression) {
        code.append("    return ").append(expression).append(";\n");
    }

    /**
     * Converte os tipos da linguagem para os tipos correspondentes em C.
     */
    private String convertTypeToC(String type) {
        switch (type) {
            case "int":
            case "float":
            case "double":
            case "char":
            case "void":
                return type;
            default:
                // Tipo padrão ou tratamento de erro
                return "int"; 
        }
    }
}