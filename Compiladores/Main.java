// File: Compiladores/Main.java
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Código de exemplo para testar
        String codeWithUnknownEnd = "int main() {\n" +
                                    "    // Comentário de linha\n" +
                                    "    int x = 10;\n" +
                                    "    float y = 20.5;\n" +
                                    "    /* Comentário \n de múltiplas \n linhas */\n" +
                                    "    printf(\"Hello, World! Str \\\"內@#\\\u00E1\");\n" + // String com escapes e unicode
                                    "    if (x > y) {\n" +
                                    "        return 1;\n" +
                                    "    } else {\n" +
                                    "        return 0;\n" +
                                    "    }\n" +
                                    "}\n" +
                                    "@@@"; // Tokens desconhecidos no final

        String validCode = "void anotherFunc() {\n" +
                           "    int a = 5 + 10; // Expressão simples, não suportada pelo parser atual\n" +
                           "                  // O parser atual espera IDENTIFIER, NUMBER ou STRING_LITERAL em parseExpression\n"+
                           "                  // Para suportar '5+10', parseExpression precisaria de lógica de precedência de operador.\n"+
                           "                  // Por enquanto, vamos testar com atribuição simples:\n"+
                           "    int b = 42;\n"+
                           "    return;\n" +
                           "}\n" +
                           "int main() {\n" +
                           "    // Teste simples\n" +
                           "    int value = 123;\n" +
                           "    callFunction();\n" + // Chamada de função
                           "    return 0;\n" +
                           "}";

        String codeToTest = validCode;
        System.out.println("Código Fonte para Análise:\n" + codeToTest + "\n");

        Lexer lexer = new Lexer(codeToTest);
        List<Token> tokens = lexer.tokenize();

        System.out.println("Tokens Gerados pelo Lexer:");
        for (Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("\n--- Saída do Parser ---");

        try {
            Parser parser = new Parser(tokens);
            parser.parseProgram();
        } catch (RuntimeException e) {
            System.err.println("Falha na Análise: " + e.getMessage());
        }
    }
}