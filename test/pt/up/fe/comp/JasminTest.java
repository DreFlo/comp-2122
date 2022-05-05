package pt.up.fe.comp;

import org.junit.Test;
import org.specs.comp.ollir.CallType;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.SpecsIo;

public class JasminTest {

    @Test
    public void test() {
        JasminResult jasminResult = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(jasminResult.getReports());

        String result = jasminResult.run();
    }

    @Test
    public void runCode() {
        System.out.println(CallType.invokespecial);
    }
}
