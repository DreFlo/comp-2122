package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JasminEmitter implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        String jasminCode;
        try {
            jasminCode = new OllirToJasmin(ollirResult).getCode();
        } catch (OllirErrorException e) {
            throw new RuntimeException(e);
        }

        return new JasminResult(ollirResult, jasminCode, ollirResult.getReports());
    }
}
