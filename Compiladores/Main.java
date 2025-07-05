// File: Compiladores/Main.java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "Compiladores/codigo.txt";
        String codeToTest = "";

        try {
            codeToTest = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + filePath);
            e.printStackTrace();
            return;
        }

        System.out.println("Código Fonte para Análise:\n" + codeToTest + "\n");

        Lexer lexer = new Lexer(codeToTest);
        List<Token> tokens = lexer.tokenize();

        System.out.println("Tokens Gerados pelo Lexer:");
        for (Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("\n--- Saída do Parser (Análise Sintática e Semântica) ---");

        try {
            CodeGenerator codeGenerator = new CodeGenerator();
            Parser parser = new Parser(tokens, codeGenerator);
            parser.parseProgram();
            
            System.out.println("\n--- Código Gerado (em C) ---");
            System.out.println(codeGenerator.getGeneratedCode());

        } catch (RuntimeException e) {
            // Imprime a pilha de erros para facilitar a depuração
            e.printStackTrace();
        }
    }
}