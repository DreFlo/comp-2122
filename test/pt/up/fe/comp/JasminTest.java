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
        JasminResult jasminResult = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(jasminResult.getReports());

        Scanner scanner = new Scanner(jasminResult.getJasminCode());

        int l;
        l = 1;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            System.out.println(l + ":\t" + line);
            l++;
        }

        String result = jasminResult.run();
    }

    @Test
    public void runCode() {
        System.out.println(CallType.invokespecial);
    }
}
