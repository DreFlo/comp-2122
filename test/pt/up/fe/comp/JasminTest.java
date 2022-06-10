package pt.up.fe.comp;

import org.junit.Test;
import org.specs.comp.ollir.CallType;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;
import java.util.Scanner;

public class JasminTest {

    @Test
    public void test() {
        JasminResult jasminResult = TestUtils.backend(SpecsIo.getResource("OurTests/postJVMOptimizationLogicCheck.jmm"));
        TestUtils.noErrors(jasminResult.getReports());

        jasminResult.run();
    }

    @Test
    public void runCode() {
        System.out.println(CallType.invokespecial);
    }
}
