import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {

    @Test
    public void test() {
        /*
            HOW TO DO TESTS

        1 - Get String
            1.1 Create - String string = "something"
            1.2 Get from File - String string = SpecsIo.getResource("fixtures/public/HelloWorld.jmm")

        2 - Parse It
            Regular file - var parserResult = TestUtils.parse("import io;\n");
            Starting with specific rule - var parserResult = TestUtils.parse("import io;\n", "ImportDeclaration");

        3 - Check for error
            If it has to pass - TestUtils.noErrors(parserResult.getReports());
            If it has to fail - TestUtils.mustFail(parserResult.getReports());
        */

        String test = SpecsIo.getResource("fixtures/public/WhileAndIf.jmm");
        var parserResult = TestUtils.parse(test);
        TestUtils.noErrors(parserResult.getReports());

    }

}
