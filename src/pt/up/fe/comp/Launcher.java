package pt.up.fe.comp;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));
        File inputFile = new File("");

        List<String> boolStrings = Arrays.asList("true", "false");

        // Create config (with default values)
        Map<String, String> config = new HashMap<>();
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // parse arguments
        if (args.length == 1) {
            // read the code
            inputFile = new File(args[0]);
            config.put("inputFile", args[0]);
        }
        else if(args.length > 1 && args.length % 2 == 0){
            // check if '-i' option exists (input file)
            Boolean iExists = false;
            for(var a : args){
                if(a.equals("-i"))
                    iExists = true;
            }

            if(iExists) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("-i")){
                        i++;
                        inputFile = new File(args[i]);
                        config.put("inputFile", args[i]);
                    }
                    else if(args[i].equals("-r")){
                        // only used to check if it is a number (throws exception if not)
                        Integer.parseInt(args[i]);

                        config.put("registerAllocation", args[i]);
                    }
                    else if (args[i].equals("-o")) {
                        i++;
                        if(!boolStrings.contains(args[i])){
                            throw new RuntimeException("Invalid option -r value inputted");
                        }

                        config.put("optimize", args[i]);
                    }
                    else if(args[i].equals("-d")){
                        i++;
                        if(!boolStrings.contains(args[i])){
                            throw new RuntimeException("Invalid option -r value inputted");
                        }

                        config.put("debug", args[i]);
                    }
                    else{
                        throw new RuntimeException("Invalid option inputted");
                    }
                }
            }
            else{
                throw new RuntimeException("Must pass option '-i' for input file");
            }
        }
        else{
            throw new RuntimeException("Wrong number of arguments inputted");
        }

        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

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

        // Check if there are analysis errors
        TestUtils.noErrors(analysisResult);

        // Instantiate JmmOptimization
        JmmOptimizer optimizer = new JmmOptimizer();

        // Optimization stage
        var optimizationResults1 = optimizer.optimize(analysisResult);
        var optimizationResults2 = optimizer.toOllir(optimizationResults1);
        var optimizationResults3 = optimizer.optimize(optimizationResults2);

        // Check if there are optimization errors
        TestUtils.noErrors(optimizationResults3);

        // Instantiate JasminBackend
        var jasminEmitter = new JasminEmitter();

        // Jasmin  stage
        var jasminResults = jasminEmitter.toJasmin(optimizationResults3);

        jasminResults.run();
        
        // Check if there are jasmin errors
        TestUtils.noErrors(jasminResults);

        // ... add remaining stages
    }

}
