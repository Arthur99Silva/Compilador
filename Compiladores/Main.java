// File: Compiladores/Main.java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "Compiladores/codigo.txt"; // Caminho relativo para o arquivo de código
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
            // Agora o Parser tem a lógica semântica embutida
            Parser parser = new Parser(tokens);
            parser.parseProgram();
        } catch (RuntimeException e) {
            System.err.println("Falha na Análise: " + e.getMessage());
        }
    }
}