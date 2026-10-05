public class RegexTest {
    public static void main(String[] args) {
        try {
            java.util.regex.Pattern.compile("\\d{1,2}[/\\.-]\\d{1,2}(?:[/\\.-]\\d{2,4})?");
            System.out.println("Regex 1 OK");
            java.util.regex.Pattern.compile("\\d{4}[/\\.-]\\d{1,2}[/\\.-]\\d{1,2}");
            System.out.println("Regex 2 OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
