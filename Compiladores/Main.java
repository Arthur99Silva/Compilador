// File: Compiladores/Main.java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "/home/arthurantunes/Compilador/Compiladores/codigo.txt"; // Nome do arquivo a ser lido
        String codeToTest = "";

        try {
            // Lê todo o conteúdo do arquivo para a string
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
        System.out.println("\n--- Saída do Parser ---");

        try {
            Parser parser = new Parser(tokens);
            parser.parseProgram();
        } catch (RuntimeException e) {
            System.err.println("Falha na Análise: " + e.getMessage());
        }
    }
}