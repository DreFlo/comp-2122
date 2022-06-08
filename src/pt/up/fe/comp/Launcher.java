package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length < 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        boolean optimize = false;
        if (args.length == 2) {
            optimize = Objects.equals(args[1].trim(), "-o");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", optimize ? "true" : "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();

        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);

        // Check if there are parsing errors
        TestUtils.noErrors(analysisResult.getReports());

        // Instantiate JmmOptimization
        JmmOptimizer optimizer = new JmmOptimizer();

        OllirResult ollirResult = optimizer.toOllir(analysisResult);

        TestUtils.noErrors(ollirResult.getReports());

        // Instantiate JmmBackend
        JasminEmitter jasminEmitter = new JasminEmitter();

        JasminResult jasminResult = jasminEmitter.toJasmin(ollirResult, optimize);

        TestUtils.noErrors(jasminResult.getReports());

        File file = new File("./libs-jmm/compiled");
        jasminResult.compile(file);
    }

}
