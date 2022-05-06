package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());

        String ollirCode = ollirGenerator.getCode();

        /*String ollirCode = "import ioPlus;\n" +
                "public HelloWorld {\n" +
                "    .method public static main(args.array.String).V {\n" +
                "        invokestatic(ioPlus, \"printHelloWorld\").V;\n" +
                "        ret.V;\n" +
                "    }\n" +
                "    .method public yellow(yel.bet, arr.array.int).int {\n" +
                "        a.int :=.int 5.int;\n" +
                "        t0.bool :=.bool 3.int <.int 2.int;\n" +
                "        t1.bool :=.bool 4.int <.int 3.int;\n" +
                "        c.bool :=.bool t0.bool &&.bool t1.bool;\n" +
                "        d.bool :=.bool !.bool c.bool;\n" +
                "        ret.int a.int;\n" +
                "    }\n" +
                "}";*/

        System.out.println("OLLIR CODE:\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

}
