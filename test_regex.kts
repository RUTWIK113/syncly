fun main() {
    println(Regex("""\d{1,2}[/\.-]\d{1,2}(?:[/\.-]\d{2,4})?""").matches("12-12-2024"))
}
