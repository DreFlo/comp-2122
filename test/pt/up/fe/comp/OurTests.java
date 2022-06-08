package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

public class OurTests {

    @Test
    public void arrayOpTest() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("OurTests/arrayOp.jmm"));

        TestUtils.mustFail(result.getReports());
    }

    @Test
    public void binOpBoolTest() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("OurTests/binOpBool.jmm"));

        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void binOpIntTest() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("OurTests/binOpInt.jmm"));

        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void ifConditionTest() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("OurTests/ifCondition.jmm"));

        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void lengthTest() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("OurTests/length.jmm"));

        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void notTest() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("OurTests/not.jmm"));

        TestUtils.noErrors(result.getReports());
    }
}
