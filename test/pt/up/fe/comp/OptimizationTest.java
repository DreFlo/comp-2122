package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

public class OptimizationTest {

    @Test
    public void test() {
        OllirResult ollirResult = TestUtils.optimize(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(ollirResult);
    }
}
