import org.junit.Test;

import pt.up.fe.comp.TestUtils;

public class ExampleTest {

    @Test
    public void testExpression() {

        // Import Tests
        TestUtils.parse("import io;\n", "ImportDeclaration");
        TestUtils.parse("import org.junit.Test;\n");
        TestUtils.parse("import io; import org.junit.Test;");

        // Type Tests
        TestUtils.parse("int alo\n");
        TestUtils.parse("int[] alo\n");
        TestUtils.parse("boolean alo\n");

        // Expression Tests
        TestUtils.parse("a + b");
        TestUtils.parse("1 < a");
        TestUtils.parse("alo && True");
        TestUtils.parse("vez * False");
        TestUtils.parse("123.length");
        TestUtils.parse("False.append(1 + a)");
        TestUtils.parse("new 123()");
        TestUtils.parse("new 123[True.length]");

        System.out.println("Resto de um bom dia!");

        var parserResult = TestUtils.parse("import io;\n");
        TestUtils.noErrors(parserResult.getReports());

        // var parserResult = TestUtils.parse("2+3\n10+20\n");
        // parserResult.getReports().get(0).getException().get().printStackTrace();
        // // System.out.println();
        // var analysisResult = TestUtils.analyse(parserResult);
    }

}
