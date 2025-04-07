import java.util.List;

public class Main {
    public static void main(String[] args) {
        String code = "int main() {\n" +
                      "    // Comentário\n" +
                      "    int x = 10;\n" +
                      "    float y = 20.5;\n" +
                      "    printf(\"Hello, World!\");\n" +
                      "    return 0;\n" +
                      "}\n";
                      
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
