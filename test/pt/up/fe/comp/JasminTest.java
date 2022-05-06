package pt.up.fe.comp;

import org.junit.Test;
import org.specs.comp.ollir.CallType;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class JasminTest {

    @Test
    public void test() {
        //JasminResult jasminResult = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        //TestUtils.noErrors(jasminResult.getReports());

        String ollirCode = "Fac {\n" +
                "\t.construct Fac().V {\n" +
                "\t\tinvokespecial(this, \"<init>\").V;\n" +
                "\t}\n" +
                "\t\n" +
                "\t.method public compFac(num.i32).i32 {\n" +
                "\t\tif ($1.num.i32 >=.bool 1.i32) goto else;\n" +
                "\t\t\tnum_aux.i32 :=.i32 1.i32;\n" +
                "\t\t\tgoto endif;\n" +
                "\t\telse:\n" +
                "\t\t\taux1.i32 :=.i32 $1.num.i32 -.i32 1.i32;\n" +
                "\t\t\taux2.i32 :=.i32 invokevirtual(this, \"compFac\", aux1.i32).i32;\n" +
                "\t\t\tnum_aux.i32 :=.i32 $1.num.i32 *.i32 aux2.i32;\n" +
                "\t\tendif:   \n" +
                "\t\t\tret.i32 num_aux.i32;\n" +
                "\t}\n" +
                "\t\n" +
                "\t.method public static main(args.array.String).V {\n" +
                "\t\taux1.Fac :=.Fac new(Fac).Fac;\n" +
                "\t\tinvokespecial(aux1.Fac,\"<init>\").V;\n" +
                "\t\taux2.i32 :=.i32 invokevirtual(aux1.Fac,\"compFac\",10.i32).i32;\n" +
                "\t\tinvokestatic(io, \"println\", aux2.i3).V;\n" +
                "\t}\n" +
                "}";



        OllirResult ollirResult = new OllirResult(null, ollirCode, Collections.emptyList());

        JasminBackend jasminBackend = new JasminEmitter();

        JasminResult jasminResult = jasminBackend.toJasmin(ollirResult);

        String result = jasminResult.run();
    }

    @Test
    public void runCode() {
        System.out.println(CallType.invokespecial);
    }
}
