package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisTest {
    @Test
    public void test() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("fixtures/public/not.jmm"));

        System.out.println(result.getSymbolTable().print());

        TestUtils.noErrors(result.getReports());
    }
}
