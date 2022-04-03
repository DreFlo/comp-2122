import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import static pt.up.fe.specs.util.SpecsIo.getResource;

public class ExampleTest {

    @Test
    public void testExpression() {
        /*
            HOW TO DO TESTS

        1 - Get String
            1.1 Create - String string = "something"
            1.2 Get from File - String string = SpecsIo.getResource("fixtures/public/HelloWorld.jmm")

        2 - Parse It
            var parserResult = TestUtils.parse("import io;\n");

        3 - Check for error
            If it has to pass - TestUtils.noErrors(parserResult.getReports());
            If it has to fail - TestUtils.mustFail(parserResult.getReports());
        */

        // Passed Files: HelloWorld
        //String test = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        //var parserResult = TestUtils.parse(test);
        //var parserResult = TestUtils.parse("ioPlus.printHelloWorld();", "Statement");
        var parserResult = TestUtils.parse("abcjfclasscjvj", "ID");
        TestUtils.noErrors(parserResult.getReports());

        /*
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
        */



    }

}
