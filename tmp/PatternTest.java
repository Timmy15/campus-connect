import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPattern;

public class PatternTest {
    public static void main(String[] args) {
        PathPatternParser parser = new PathPatternParser();
        PathPattern p1 = parser.parse("/swagger-ui/**");
        PathPattern p2 = parser.parse("/*swagger-initializer.js");
        System.out.println(p1.combine(p2).getPatternString());

        PathPattern p3 = parser.parse("/webjars/**");
        PathPattern p4 = parser.parse("/*swagger-ui");
        System.out.println(p3.combine(p4).getPatternString());
    }
}
