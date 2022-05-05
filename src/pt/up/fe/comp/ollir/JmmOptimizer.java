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
                "\n" +
                "public HelloWorld extends BoardBase {\n" +
                "    .method public static main(args.array.String).V {\n" +
                "        invokestatic(ioPlus, \"printHelloWorld\").V;\n" +
                "    }\n" +
                "\n" +
                "    .method public yellow(yel.bet, arr.array.int).int {\n" +
                "        a.int :=.int new(int).int;\n" +
                "        a.int :=.int 1.int;\n" +
                "        ret.int a.int;\n" +
                "    }\n" +
                "}";*/

        System.out.println("OLLIR CODE:\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

}
