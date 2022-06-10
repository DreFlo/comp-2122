package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JasminEmitter implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        return toJasmin(ollirResult, true);
    }

    public JasminResult toJasmin(OllirResult ollirResult, boolean optimize) {
        String jasminCode;
        try {
            jasminCode = new OllirToJasmin(ollirResult, optimize).getCode();
        } catch (OllirErrorException e) {
            throw new RuntimeException(e);
        }

        if (optimize) {
            RedundantJMVInstructionRemover redundantJMVInstructionRemover = new RedundantJMVInstructionRemover(jasminCode);

            jasminCode = redundantJMVInstructionRemover.getOptimizedCode();

            System.out.println(jasminCode);
        }

        return new JasminResult(ollirResult, jasminCode, ollirResult.getReports());
    }
}
