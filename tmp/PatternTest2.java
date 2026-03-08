import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPattern;

public class PatternTest2 {
    public static void main(String[] args) {
        PathPatternParser parser = new PathPatternParser();
        PathPattern p1 = parser.parse("");
        PathPattern p2 = parser.parse("/*swagger-initializer.js");
        System.out.println("p1:" + p1.getPatternString());
        System.out.println(p1.combine(p2).getPatternString());
    }
}
